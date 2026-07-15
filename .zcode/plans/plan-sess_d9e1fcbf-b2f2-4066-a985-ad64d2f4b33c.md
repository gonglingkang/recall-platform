## 需求状态机重构 + 验收/发布时间入参支持

### 一、背景与确认结论

用户要求重构需求手动状态机，并让验收完成/发布完成支持传入时间，验收完成额外支持传入验收人。已确认：

- **新状态机**（仅改手动流转，K 联动逻辑不动）：
  ```
  讨论中(0)    -> 不涉及(1) / 进行中(2)                    [不变]
  不涉及(1)    -> 讨论中(0) / 进行中(2)                     [新增 ->进行中]
  进行中(2)    -> 讨论中(0) / 不涉及(1) / 开发完成(3)        [新增 ->讨论中]
  开发完成(3)  -> 进行中(2) / 不涉及(1) / 验收完成(4) / 发布完成(5)  [新增 ->进行中/发布完成]
  验收完成(4)  -> 发布完成(5)                               [删除 ->开发完成 回退]
  发布完成(5)  -> 终态                                     [不变]
  ```
- 验收人：存**姓名字符串**（自由文本，VARCHAR(100)，非必填）。
- 验收完成时间 / 发布完成时间：入参可选，**不传默认当天**。

### 二、数据库变更

`requirements` 表新增 `acceptance_person` 字段。需三步同步（AGENTS.md 约定）：

1. 更新 `schema.sql`：在 `acceptance_date` 后加 `acceptance_person VARCHAR(100) DEFAULT NULL COMMENT '验收人(姓名)'`。
2. 对真实库执行 ALTER（测试连的就是这个库）：
   ```sql
   ALTER TABLE requirements ADD COLUMN acceptance_person VARCHAR(100) DEFAULT NULL COMMENT '验收人(姓名)' AFTER acceptance_date;
   ```
3. 字段位置：`dev_complete_date` / `acceptance_date` / `acceptance_person` / `release_date`。

### 三、代码改动（按层）

**1. Entity — `Requirement.java`**
- 新增字段 `private String acceptancePerson;`
- 更新类注释中的状态流转说明。

**2. DTO — `RequirementStatusReq.java`**
新增 3 个可选字段（均带 `@Schema` + 校验）：
- `acceptanceDate` (LocalDate) — 验收完成时间，不传后端填当天
- `acceptancePerson` (String, @Size max=100) — 验收人
- `releaseDate` (LocalDate) — 发布完成时间，不传后端填当天
- 更新类 JavaDoc 说明新的入参。

**3. VO — `RequirementVO.java`**
- 新增 `acceptancePerson` 字段 + `@Schema`。
- `toVO` 中补该字段映射。

**4. Enum — `RequirementStatus.java`**
- 更新类 JavaDoc 中的"手动合法流转"清单为新状态机。

**5. ServiceImpl — `RequirementServiceImpl.java`（核心）**

(a) `validateManualTransition` — 重写为新状态机：
```
DISCUSSING    -> NOT_INVOLVED, IN_PROGRESS
NOT_INVOLVED  -> DISCUSSING, IN_PROGRESS      [新增 IN_PROGRESS]
IN_PROGRESS   -> DISCUSSING, NOT_INVOLVED, DEV_DONE   [新增 DISCUSSING]
DEV_DONE      -> IN_PROGRESS, NOT_INVOLVED, ACCEPTANCE_DONE, RELEASED  [新增 IN_PROGRESS, RELEASED]
ACCEPTANCE_DONE -> RELEASED                   [删除 DEV_DONE]
RELEASED      -> false
```

(b) `changeStatus` 方法体 — 调整各分支：
- `NOT_INVOLVED`：保持（填 cancelReason，清 devCompleteDate）。
- `ACCEPTANCE_DONE`：解绑K；acceptanceDate 取入参（null 兜底当天）；写入入参 acceptancePerson；**不再清 devCompleteDate**（开发完成->验收完成，开发完成时间应保留作为历史记录）。
- `RELEASED`：解绑K；releaseDate 取入参（null 兜底当天）；**保留 devCompleteDate**（发布完成是从开发完成或验收完成进入，开发完成时间是有效历史）。
- else 分支（K 活跃态间手动流转：讨论中/进行中/开发完成，仅未绑K可达）：
  - 目标为 `DEV_DONE`：devCompleteDate 为 null 时填当天（保持）。
  - 目标为 `IN_PROGRESS`/`DISCUSSING`：清 devCompleteDate（回退开发态应清开发完成时间）。
  - 清 cancelReason。

(c) `applyUpdate` — 新增 `.set(Requirement::getAcceptancePerson, r.getAcceptancePerson())`，确保 null 能落库。

(d) 关于 devCompleteDate 保留逻辑的说明：原代码进入验收完成/发布完成会清 devCompleteDate，但新状态机下验收完成/发布完成多由开发完成进入，开发完成时间是有意义的历史信息，应保留。解绑K本身不影响 devCompleteDate（解绑只是断开K联动，开发完成时间已记录）。

**6. Controller — `RequirementController.java`**
- 无需改动（`changeStatus` 已用 `@Valid @RequestBody`，新字段靠 DTO 注解自动采集）。

### 四、测试改动 — `RequirementServiceTest.java`

1. **更新原有非法流转测试**：`changeStatus_illegalTransition_shouldThrow` 原断言"讨论中->开发完成非法"，新状态机下讨论中确实不能直接到开发完成，保持。但需补充验证新增的合法流转。
2. **新增合法流转测试**：
   - 不涉及 -> 进行中（新）
   - 进行中 -> 讨论中（新）
   - 开发完成 -> 进行中（新，清 devCompleteDate）
   - 开发完成 -> 发布完成（新）
   - 验收完成 -> 发布完成（保持合法）
3. **新增非法流转测试**：
   - 验收完成 -> 开发完成（现在是非法，原来合法）
   - 验收完成 -> 进行中 / 讨论中（非法）
   - 开发完成 -> 讨论中（非法，需经进行中）
4. **新增入参测试**：
   - 验收完成传入 acceptanceDate + acceptancePerson -> 正确落库
   - 验收完成不传 acceptanceDate -> 默认当天
   - 发布完成传入 releaseDate -> 正确落库
   - 发布完成不传 releaseDate -> 默认当天
   - 验收完成/发布完成保留 devCompleteDate
5. **保持绑K约束测试**：绑K时手动改 K 活跃态仍 409（`changeStatus_boundKr_manualToActiveState_shouldThrow` 不变）。

### 五、文档同步 — `docs/需求管理接口文档-v1.0.md`

- "手动状态机"流转图更新为新状态机。
- 4.5 状态变更接口的请求体表格新增 acceptanceDate/acceptancePerson/releaseDate 三字段。
- RequirementVO 数据对象表新增 acceptancePerson。
- 状态流转速查章节更新（验收完成/发布完成保留 devCompleteDate）。

### 六、验证

1. 编译：`cd backend && ./mvnw -q -pl recall-system -am compile`
2. 跑测试：`RequirementServiceTest` + `KeyResultServiceTest`（确认 K 联动无回归）
3. 测试库 ALTER 已执行，避免 SQL 错误