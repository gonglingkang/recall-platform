---
name: backend-code-standards
description: 待办系统(recall-platform)后端 Java + Spring Boot 3 + MyBatis-Plus 项目的代码规范。用于约束后端 controller/service/dao 三层架构、每个 Mapper 必须有自己的 Service 管理且其他 Service 不得直接注入该 Mapper（与是否同业务域无关）、关联表独立建 Service 管理、聚合查询走 Service 接口、entity 不出 DAO 层、统一响应、异常处理、事务、数据隔离、安全、命名等代码编写。每当用户在本项目里新增/修改后端 Java 业务代码（写 Controller、Service、Mapper、Entity、DTO、VO、写接口逻辑、做后端功能开发）时必须遵循本规范——即使用户没有明说"按规范写"。
---

# 后端代码规范 (recall-platform)

本规范约束 `backend/` 下 recall-common 与 recall-system 模块的 Java 后端代码。技术栈：Java 17 + Spring Boot 3.2 + Maven 多模块 + MyBatis-Plus + JWT。所有规范对应 `docs/PRD-待办系统产品需求文档-v1.1.md`。

## 0. 总则（写任何后端代码前必读）

### 分层职责红线

项目采用 **controller / service / dao 三层**，每层内部按业务域分包（auth / todo / category / perf / sprint / stats / user / plan）。职责不可越界：

- **Controller**：只做 HTTP 接入（接收参数、触发 `@Valid` 校验）、调用 Service、用 `Result<T>` 包装返回。**禁止写业务逻辑、禁止直接调 Mapper**。
- **Service**：业务逻辑所在。接口与 Impl 分离（`XxxService` + `XxxServiceImpl`）。**禁止在接口里写实现**。
- **DAO (Mapper)**：只做数据访问。**禁止写业务判断**。

### 数据流转红线

- **entity 不出 DAO 层**，**禁止把 entity 直接返回给 Controller/前端**。
  - 唯一例外：**Service 之间内部调用**可经 Service 接口方法返回 entity（如 `ObjectiveService.getById(id, checkOwnership)` 供 `KeyResultService` 校验归属、取字段），但**该 entity 不得再透传至 Controller/前端**，调用方拿到后只做内部判断/取值，对外仍返回 VO。
  - 实现方式：在 Service 接口暴露 `getById(id, checkOwnership)` 之类的方法，`checkOwnership=true` 时内部完成归属校验（查不到或不属于当前用户抛 404），调用方无需重复判空。
- 入参用 **DTO**（后缀 `Req`），出参用 **VO**（后缀 `VO`），entity↔vo 转换在 Service 层用 `toVO()` 手写（本项目暂不引入 MapStruct）。
- **每个 Mapper 必须有自己的 Service 管理**，与是否同业务域无关。Mapper 的数据访问（select/insert/update/delete）只能由其归属的 Service 内部调用并经 Service 接口向外暴露；**其他 Service 不得直接注入该 Mapper**，须调用其归属 Service 的接口方法。
  - 判定标准：一个 Mapper 被哪个 Service「拥有」，取决于该 Service 是否对这张表承担主要业务责任。归属关系一经确定，该 Mapper 只能出现在其归属 ServiceImpl 的 `@RequiredArgsConstructor` 注入字段里。
  - 反例（错误）：`SprintServiceImpl` 直接注入 `SprintKeyResultMapper` 操作关联表——`SprintKeyResultMapper` 应有自己的 Service（如 `SprintKeyResultService`）封装关联数据的增删查，`SprintServiceImpl` 调该 Service，不直接碰 Mapper。
  - 反例（错误）：`KeyResultServiceImpl` 直接注入 `ObjectiveMapper`——应注入 `ObjectiveService`。
  - 反例（错误）：`PlanServiceImpl` 直接注入 `ObjectiveMapper`/`KeyResultMapper`/`SprintItemMapper`/`TodoMapper`——应分别注入对应 Service 接口。
  - 正例（正确）：`ObjectiveServiceImpl` 注入 `ObjectiveMapper`（自己拥有）、需要 K 数据时调 `KeyResultService`（不注入 `KeyResultMapper`）。
  - 例外：一个 ServiceImpl 可注入多个自己归属的 Mapper（如同时管理主表与从表），前提是这些 Mapper 都归该 Service 管、且不出现「A Service 拥有的 Mapper 出现在 B ServiceImpl」的情况。是否同 `dao.<域>` 包**不是**判定依据。
- **关联表归属**：多对多关联表（如 `sprint_key_results`）独立建 Mapper + Service 管理；需要操作关联数据的服务调这个 Service 接口，不直接注入关联表 Mapper。
- **聚合查询**（月度总览、统计等）：不直接注入多个域的 Mapper，走各域 Service 接口聚合，或经专设的查询 Mapper（归聚合 Service 自己拥有）。
- **Service 间循环依赖**用 `@Lazy` 打破，**不要改回 `@Autowired` 字段注入**。`@Lazy` 加在 `@RequiredArgsConstructor` 的 `final` 字段上；需在 `backend/lombok.config` 配置 `lombok.copyableAnnotations += org.springframework.context.annotation.Lazy`（已配置），Lombok 才会把 `@Lazy` 复制到构造参数。

### 模块依赖方向

- `recall-common`：通用横切件（Result/异常/JWT/UserContext/错误码），**不得依赖任何业务包**。
- `recall-system`：依赖 common，承载全部业务。禁止 system 反向被 common 依赖。

---

## 1. 写 Controller 时

包路径：`com.recall.controller.<业务域>/`，类名 `XxxController`。

1. **URL**：前缀统一 `/api`，RESTful 风格。资源用复数名词（`/api/todos`、`/api/categories`）。
2. **HTTP 方法语义**：GET 只读不改；POST 新增；PUT 全量更新；PATCH 部分更新/状态切换；DELETE 软删除。禁止用 GET 执行写操作。
3. **返回值**：所有接口必须返回 `Result<T>`，**禁止裸返回实体或 List**。
4. **参数校验**：Body 用 `@Valid @RequestBody`；路径变量用 `@PathVariable`；查询参数用 `@RequestParam`。校验注解写在 DTO 上，Controller 只加 `@Valid`。
5. **Controller 里禁止 try-catch**：异常交给 `GlobalExceptionHandler` 统一处理。
6. **依赖注入**：用 `@RequiredArgsConstructor` + `final` 字段，不用 `@Autowired` 字段注入。
7. **Swagger 注解**（接口文档由 springdoc-openapi 生成，访问 `/swagger-ui.html`）：
   - 类上加 `@Tag(name = "业务名", description = "说明")`，每个 Controller 一个分组。
   - 每个接口方法加 `@Operation(summary = "简述", description = "补充说明(含PRD编号)")`。
   - 路径/查询参数用 `@Parameter(description = "说明")` 标注。Body 参数靠 DTO 的 `@Schema` 自动生成，无需重复。
   - Controller 层**只**用 Swagger 注解做接口文档，不写 JavaDoc（避免重复维护）。

示例：
```java
@Tag(name = "待办", description = "待办的增删改查、完成、置顶、回收站")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TodoService todoService;

    @Operation(summary = "创建待办", description = "快速创建仅需 title（PRD 6.3.2）")
    @PostMapping
    public Result<TodoVO> create(@Valid @RequestBody TodoCreateReq req) {
        return Result.ok(todoService.create(req));
    }
}
```

---

## 2. 写 Service 时

包路径：`com.recall.service.<业务域>/`（接口）与 `com.recall.service.<业务域>.impl/`（实现）。

1. **强制接口 + Impl 分离**，即使是简单 CRUD 也不省略接口（利于 Mock 测试、统一风格）。
2. **业务校验放 Service**：参数格式校验（注解）在 DTO，复杂业务规则校验（唯一性、状态流转、权限）在 Service，不满足抛 `BusinessException`。
3. **越权访问统一返回 404**：`loadOwned(id)` 模式——查不到或不属于当前用户都抛 `NOT_FOUND`，**不暴露资源存在性**。
4. **禁止从 Controller 参数传 userId**：userId 一律从 `UserContextHolder.getUserId()` 取（防伪造）。
5. **依赖注入**：`@RequiredArgsConstructor` + `final`。出现循环依赖用 `@Lazy`，不要改回字段注入。
   - 循环依赖典型场景：`ObjectiveServiceImpl` 注入 `KeyResultService`、`KeyResultServiceImpl` 又注入 `ObjectiveService`。在**其中一侧**的注入字段加 `@Lazy` 即可打破（项目已在 `backend/lombok.config` 配置 `copyableAnnotations += @Lazy`，Lombok 会把字段上的 `@Lazy` 复制到构造参数）。
   ```java
   @RequiredArgsConstructor
   public class KeyResultServiceImpl implements KeyResultService {
       private final KeyResultMapper keyResultMapper;
       @Lazy
       private final ObjectiveService objectiveService; // 打破与 ObjectiveServiceImpl 的循环依赖
   }
   ```
   - **禁止**为绕开循环依赖而让 Service 直接调其他域 Mapper——这会把数据访问绕过归属校验，是更严重的违规。正确路径是 Service→Service 接口 + `@Lazy`。
6. **注释规范**（Service 层用基本 JavaDoc，**不用 Swagger 注解**）：
   - 每个公共方法写 JavaDoc：一行简述 + 有入参加 `@param 参数名 简明说明` + 有出参加 `@return 简明说明`。
   - 说明**简明**即可，不要长篇大论；复杂逻辑可在简述后用 `（PRD x.x）` 标注对应需求章节。
   - `void` 方法不写 `@return`；无参方法不写 `@param`。
   - 类级 JavaDoc 说明职责与核心约束（数据隔离、PRD 章节等）。
   ```java
   /**
    * 完成待办（PRD 4.2 状态机）。
    *
    * @param id  待办 ID
    * @param req 状态变更请求
    * @return 变更后的待办详情
    */
   TodoVO changeStatus(Long id, TodoStatusReq req);
   ```

### 事务规则（核心，按实际写 SQL 条数判断）

判断标准看方法内**实际落库的写操作（insert/update/delete）SQL 条数**，不看代码行数：

| 场景 | 是否加 `@Transactional` |
|---|---|
| 单条写（1 次 insert/update/delete） | **不加**。DB 自身保证原子性，事务是冗余开销 |
| 多条写（≥2 次 insert/update/delete） | **必须加** `@Transactional(rollbackFor = Exception.class)` |
| 纯只读查询 | 不加（或按需 `readOnly = true`） |

边界说明：
- `delete(wrapper)` 按条件删多条 → 算多条写 → 要加事务。
- `@TableLogic` 逻辑删除 `deleteById` 是单条 update → 单条写 → 不加事务。
- "查→判断→写"只要最终写操作只有 1 条 → 不加。
- **跨 Service 调用的写操作须并入当前方法的写 SQL 计数**：被调 Service 方法内含 insert/update/delete 的，其写条数并入调用方；合计 ≥2 条时，调用方方法**必须加 `@Transactional`**。跨 Service 调用默认共享同一事务（Spring `REQUIRED` 传播），保证跨域写原子性。例：`KeyResultServiceImpl.changeStatus` 自身 1 条 `updateById` + 跨调 `SprintService.syncStatusByKeyResult`（内含 N 条冲刺 update）→ 合计 ≥2 → `changeStatus` 必须加 `@Transactional`。
- 事务方法内**禁止跨网络调用**（避免长事务）。
- **单条写方法加 `@Transactional` 属冗余**，勿模仿历史代码中"所有写方法都加事务"的写法——那是规范定稿前的习惯，现已统一按"实际写 SQL 条数"判断。

```java
// ✅ 不加事务：仅 1 次写
public TodoVO create(TodoCreateReq req) {
    todoMapper.insert(todo);
    return toVO(todo);
}

// ✅ 必须加事务：2 次写
@Transactional(rollbackFor = Exception.class)
public LoginVO register(RegisterReq req) {
    userMapper.insert(user);      // 写1
    categoryMapper.insert(c);     // 写2
}
```

---

## 3. 写 DAO (Mapper) 时

包路径：`com.recall.dao.<业务域>/`，继承 `BaseMapper<T>`，类名 `XxxMapper`，加 `@Mapper`。

1. **查询优先用 `LambdaQueryWrapper`**，**禁止用字符串字段名**（`new QueryWrapper<>().eq("user_id", ...)` 易出错）。
2. **逻辑删除**统一用 `@TableLogic` + `deletedAt` 字段，业务层**不手写 `deleted_at IS NULL` 条件**（MP 自动处理）。
3. **需绕过逻辑删除**的查询（如回收站、物理删除）用**自定义 SQL**，写在 `resources/mapper/XxxMapper.xml`，接口方法对应。
4. **自定义 SQL 必须带 userId 条件**（用户隔离表），即使有拦截器兜底也要显式写，双保险。
5. 复杂聚合 SQL 写 XML，不用注解 SQL。

---

## 4. 写 Entity / DTO / VO 时

- **Entity**（`com.recall.entity.<业务域>/`）：继承 `BaseEntity`（自动填充 createdAt/updatedAt）。`@TableName` 指定表名，主键 `@TableId(type = IdType.AUTO)`。JSON 字段用 `@TableField(typeHandler = JacksonTypeHandler.class)` 并在 `@TableName` 加 `autoResultMap = true`。
- **DTO**（`com.recall.dto.<业务域>/`）：后缀 `Req`，按操作拆分（`TodoCreateReq`/`TodoUpdateReq`/`TodoStatusReq`），**不复用通用 DTO**（校验规则不冲突）。校验注解 + 中文 message。
- **VO**（`com.recall.vo.<业务域>/`）：后缀 `VO`，用 `@Builder` + `@Data`。**禁止包含 passwordHash 等敏感字段**。
- **枚举**（`com.recall.enums`）：无后缀。提供 `of(String)` 工厂方法，未知值抛 `IllegalArgumentException` 或给默认值。
- 字段命名驼峰，对应数据库下划线（MP 自动映射）。
- **`@Schema` 注解（强制）**：所有 DTO/VO/Entity 对象的字段都要加 `@Schema(description = "字段说明")` 描述字段含义，类本身加 `@Schema(description = "对象用途")`。这是接口文档的字段说明来源，由 Swagger 自动采集。说明简明，import `io.swagger.v3.oas.annotations.media.Schema`。
  ```java
  @Data
  @Schema(description = "待办创建请求")
  public class TodoCreateReq {
      @Schema(description = "标题")
      private String title;
  }
  ```

---

## 5. 写测试时

- 测试类 `src/test/java/com/recall/service/<业务域>/XxxServiceTest`，继承 `BaseTest`。
- 方法名 `场景_预期结果`（如 `listToday_shouldCarryOverOverdueTodos`）。
- 集成测试 `@SpringBootTest` + `@Transactional`（自动回滚，不污染库）。
- 用 `loginAsNewUser()` 注入测试用户上下文，不依赖固定账号。
- 覆盖：正常路径 + 边界 + 异常 + **权限隔离**（跨用户访问必测）。
- 断言用 JUnit5 `assertThrows` 验证异常码，**不依赖测试执行顺序**。

---

## 6. 通用约束（写任何代码都生效）

### 命名
- 包名全小写、类名大驼峰、方法/变量小驼峰、常量全大写下划线。
- 三层后缀：`XxxController` / `XxxService`+`XxxServiceImpl` / `XxxMapper`。

### 日志
- 统一 `@Slf4j` + 占位符 `{}`，**禁止字符串拼接拼日志**。
- 关键写操作留 `log.info`（派生待办、删除分类、自动延续），异常留 `log.error` 带堆栈。
- **禁止打印密码、token、完整敏感信息**。

### 注释
- 类级 JavaDoc：职责 + 对应 PRD 章节。
- 公共方法 JavaDoc：参数/返回/异常/业务约束。
- 复杂逻辑（状态机、排序规则、数据隔离）必须注释对应 PRD 条款。
- TODO 格式：`TODO(P1): 说明` 带优先级，**禁止裸 TODO**。

### 错误码
- 按业务域分码段：认证 41xx、待办 42xx、分类 43xx、绩效 44xx、冲刺 45xx。
- 新增业务错误加到 `ResultCode` 枚举，复用已有码优先，不重复造码。
- HTTP 语义：4xx 客户端、5xx 服务端。

### 安全（硬约束，不可违背）
- 非白名单接口强制 JWT，白名单清单维护在 `SecurityConfig`。
- 用户隔离表（todos/categories/subcategories/perf_*/sprint_items）的所有 select/update/delete **必须带 userId**——拦截器兜底 + 业务层显式带，双保险。
- 密码 BCrypt 哈希；登录失败不区分"账号不存在/密码错误"。
- 越权统一返回 404。

### 配置
- 自定义配置项前缀 `recall.`（如 `recall.jwt`）。
- 敏感信息（DB 密码、JWT secret）**不入 application.yml 提交**，用 `application-local.yml` 覆盖（已 gitignore）。

### 时间
- 时间戳（createdAt/updatedAt/doneAt/reminderAt）**UTC 存储**。
- 计划日期（planDate）用 `LocalDate`，按用户本地自然日（PRD 4.4）。

---

## 7. 决策记录（已定调，不再讨论）

| 决策点 | 结论 |
|---|---|
| Service 接口 | 强制接口 + Impl 分离 |
| Mapper 归属 | 每个 Mapper 必须有自己的 Service 管理；其他 Service（不论同域异域）不得直接注入该 Mapper，须调归属 Service 接口 |
| 关联表 | 独立建 Mapper + Service 管理；操作关联数据走该 Service 接口 |
| 聚合查询 | 走各域 Service 接口聚合，或经专设查询 Mapper（归聚合 Service 自己拥有） |
| entity 出 DAO | 禁止返回 Controller/前端；Service 间内部调用可经 Service 方法返回 entity，但不得透传出 Service 层 |
| 循环依赖 | 用 `@Lazy` 打破（配合 `lombok.config` 的 `copyableAnnotations`），禁止改回字段注入，禁止借道 Mapper 绕过 |
| DTO 粒度 | 按操作拆分，不复用通用 DTO |
| 对象映射 | 手写 toVO()，暂不引入 MapStruct |
| 批量操作 | 设上限（默认 100）并校验 |
| 接口文档 | 引入 springdoc-openapi（规范要求，落地待加） |
| 代码格式化 | 引入 Spotless（规范要求，落地待加） |
| 错误码 | 按业务域分码段 |

## 8. 何时偏离规范

规范是默认路径，不是教条。遇到以下情况可偏离，但**必须在代码注释里说明原因**：
- 性能优化需要绕过 ORM 直接写 SQL。
- 框架/库限制无法满足某条规则。
- 临时实现 P1/P2 功能但有技术债——用 `TODO(P1):` 标注，后续补齐。
