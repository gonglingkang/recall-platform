# AGENTS.md

> 本文件是给 AI 编码代理（以及新加入的开发者）的项目协作指南。它不是代码规范本身（规范见 `.agents/skills/backend-code-standards/SKILL.md`），而是**如何在本项目中正确工作**的元约定。

## 项目概览

**recall-platform** 是一个个人待办 + 月度绩效 + 团队冲刺管理系统。

- **技术栈**：Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL + JWT，Maven 多模块
- **后端模块**：`backend/recall-common`（通用横切件）+ `backend/recall-system`（全部业务）
- **前端**：独立工程（本项目仓库为后端）
- **分支**：`master` 为主干

## 业务域划分

后端按业务域分包，每个域有独立的 controller / service / dao / entity / dto / vo：

| 业务域 | 包名 | 职责 | 主要表 |
|---|---|---|---|
| auth | `com.recall.*.auth` | 注册/登录/JWT | users |
| user | `com.recall.*.user` | 用户资料/密码/偏好 | users（Mapper 在 auth 域，待治理） |
| todo | `com.recall.*.todo` | 待办 CRUD/完成/置顶/回收站/延续 | todos |
| category | `com.recall.*.category` | 大分类/子分类 | categories, subcategories |
| objectives | `com.recall.*.objectives` | 月度绩效目标 O + 关键成果 K | objectives, key_results |
| sprint | `com.recall.*.sprint` | 团队冲刺任务 + 关联 K | sprint_items, sprint_key_results |
| plan | `com.recall.*.plan` | 月度总览（聚合查询） | 无自有表 |
| stats | `com.recall.*.stats` | 今日统计 | 无自有表（有 StatsMapper） |

## 必读规范

**写任何后端代码前，必须先加载 skill：`backend-code-standards`。**

skill 路径：`.agents/skills/backend-code-standards/SKILL.md`

核心红线（违反即错）：

1. **三层职责**：Controller 只接入+调 Service；Service 写业务；DAO 只做数据访问。
2. **Mapper 各有归属 Service**：每个 Mapper 必须有自己的 Service 管理其数据访问；其他 Service（不论同域异域）不得直接注入该 Mapper，须调归属 Service 接口。关联表独立建 Service 管理。判定与是否同 `dao.<域>` 包无关。
3. **entity 不出 DAO 层**：不返回 Controller/前端。Service 间内部调用可经 Service 方法返回 entity，但不透传出 Service 层。
4. **DTO 按操作拆分**：不复用通用 DTO。入参 `Req`，出参 `VO`。
5. **userId 不从 Controller 传**：一律 `UserContextHolder.getUserId()`。
6. **越权统一 404**：`loadOwned(id)` 模式，查不到或不属于当前用户都抛 NOT_FOUND。
7. **事务按实际写 SQL 条数**：单条写不加；多条写加 `@Transactional(rollbackFor=Exception.class)`。
8. **循环依赖用 `@Lazy`**：不改回字段注入（`lombok.config` 已配置 `copyableAnnotations`）。
9. **入参校验下沉 DTO 注解**：复杂业务规则校验在 Service；参数格式校验用 `@Valid` + 注解。

## 已知技术债（历史违规，非本次引入）

以下 ServiceImpl 直接注入了非自己归属的 Mapper，违反"每个 Mapper 必须有自己的 Service 管理"规范。新代码不得模仿，后续应治理：

| 文件 | 违规点 | 正确做法 |
|---|---|---|
| `SprintServiceImpl` | 直接注入 `SprintKeyResultMapper` | 为关联表建 `SprintKeyResultService`，调该 Service |
| `PlanServiceImpl` | 直接注入 4 个非归属 Mapper（Objective/KeyResult/SprintItem/Todo） | 改注入对应 Service 接口 |
| `StatsServiceImpl` | 直接注入 TodoMapper、CategoryMapper | 走 TodoService/CategoryService 或专设 StatsMapper |
| `CategoryServiceImpl` | 直接注入 TodoMapper | 级联删待办应走 TodoService |
| `AuthServiceImpl` | 直接注入 CategoryMapper | 注册时建默认分类应走 CategoryService |
| `UserServiceImpl` | 直接注入 UserMapper（user 域用了 auth 域的 Mapper） | UserMapper 应有自己的归属 Service 或迁包 |

> 新增代码若涉及这些文件，应顺手将涉及的跨域调用改为 Service 接口调用，逐步治理。

## 工作约定

### 改动前
- 写后端代码前先加载 `backend-code-standards` skill。
- 复杂改动（多文件、涉及架构决策）先进入 plan mode，与用户对齐方案再动手。
- 不确定的设计点用 `AskUserQuestion` 确认，不要自行假设。

### 改动中
- **匹配周围代码风格**：命名、注释密度、JavaDoc 格式与同包文件一致。
- **新代码遵守 skill**：即使周围历史代码违规，新代码必须按规范写。
- 遇到历史违规且本次改动会触及该文件，顺手修正（用 `TODO(P1):` 标注后续治理项，或直接改）。
- 测试必须覆盖：正常路径 + 边界 + 异常 + **权限隔离**（跨用户访问必测）。

### 改动后
- 编译通过 + 相关测试全绿才算完成。
- 如实报告测试结果，不夸大。
- 涉及接口变更的，同步更新 `docs/` 下的对接文档。

### 提交
- 仅在用户明确要求时才 commit/push。
- 不在 commit message 里夸大或捏造。

## 常用命令

```bash
# 编译
cd backend && ./mvnw -q -pl recall-system -am compile

# 跑单个测试类
cd backend && ./mvnw -pl recall-system -am test -Dtest=SprintServiceTest -Dsurefire.failIfNoSpecifiedTests=false

# 跑多个测试类
cd backend && ./mvnw -pl recall-system -am test -Dtest=SprintServiceTest,ObjectiveServiceTest -Dsurefire.failIfNoSpecifiedTests=false

# MySQL CLI（本机）
"/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe" -uroot -p123456 recall
```

## 数据库变更

- 项目无 migration 工具，`schema.sql` 是建表脚本（手动执行）。
- 改表结构时：① 更新 `schema.sql`；② 对实际数据库执行 ALTER；③ 测试库与开发库是同一个。
- 测试连的是真实 MySQL（非内存库），表结构变更必须同步 ALTER，否则测试报 SQL 语法错误。

## 文档

| 文档 | 位置 | 用途 |
|---|---|---|
| 后端代码规范 | `.agents/skills/backend-code-standards/SKILL.md` | 写后端代码前必读（用 skill 加载） |
| PRD | `docs/PRD-待办系统产品需求文档-v1.1.md` | 产品需求 |
| 绩效接口文档 | `docs/个人月度绩效接口文档-v2.0.md` | 绩效模块前端对接 |
| 取消状态说明 | `docs/月度绩效-取消状态对接说明-v2.1.md` | 取消状态增量对接 |
