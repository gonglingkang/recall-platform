# Recall · 待办系统

为忙碌人群打造的轻量待办管理系统：专注今日、清晰分类、自动流转、月度规划与每日执行闭环。
详细需求见 [`docs/PRD-待办系统产品需求文档-v1.1.md`](docs/PRD-待办系统产品需求文档-v1.1.md)。

## 技术栈

| | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + TypeScript + Vue Router + Pinia + axios |
| 后端 | Java 17 + Spring Boot 3.2 + Maven 多模块 |
| 数据库 | MySQL 8 |
| 持久层 | MyBatis-Plus |
| 认证 | JWT 无状态 |

## 目录结构

```
recall-platform/
├── backend/                         # 后端 Maven 聚合工程
│   ├── pom.xml                      # 聚合 parent + 依赖版本管理
│   ├── mvnw / mvnw.cmd              # Maven Wrapper（无需本机预装 Maven）
│   ├── recall-common/               # 通用层：Result/异常/JWT/UserContext/错误码
│   └── recall-system/               # 启动模块：controller/service/dao 三层业务
│       └── src/main/
│           ├── java/com/recall/
│           │   ├── controller/{auth,todo,category,perf,sprint,stats,user,plan}/
│           │   ├── service/<业务>/{impl}/
│           │   ├── dao/<业务>/
│           │   ├── entity/<业务>/   # 9 张表实体
│           │   ├── dto/<业务>/ vo/<业务>/ enums/
│           │   ├── config/          # Security/MyBatisPlus/Cors/隔离拦截器
│           │   ├── security/        # JwtAuthFilter
│           │   └── web/advice/      # 全局异常处理
│           └── resources/
│               ├── application.yml
│               ├── db/schema.sql    # 9 表 + 索引建表脚本
│               └── mapper/
├── frontend/                        # 前端骨架（交其他模型开发）
│   └── src/{api,stores,router,views,components,assets}/
└── docs/                            # PRD
```

## 后端启动

**前置**
- JDK 17+（本机当前为 JDK 21，`JAVA_HOME` 需指向它）
- MySQL 8
- Maven（已内置 `mvnw`，无需单独安装）

**步骤**

1. 初始化数据库（执行建表脚本，会创建 `recall` 库与全部表）：
   ```bash
   mysql -uroot -p < backend/recall-system/src/main/resources/db/schema.sql
   ```
2. 按需修改 `backend/recall-system/src/main/resources/application.yml` 的数据源账号密码。
3. 编译 / 启动：
   ```bash
   cd backend
   ./mvnw spring-boot:run          # 或 ./mvnw package 后 java -jar 启动
   ```
   服务监听 `http://localhost:8080`。

**核心已实现（P0）**
- 注册 / 登录 / 退出 / 当前用户（JWT，密码 BCrypt）
- 待办：今日（触发次日自动延续 PRD 4.5）、指定日期、按分类、搜索、CRUD、完成/撤销、置顶、软删除
- 待办排序（PRD 4.6）、状态机（PRD 4.2）
- 数据隔离拦截器（PRD 3.2 / 13.3，强制 userId 过滤）
- 今日概览统计

**接口骨架已就绪、业务逻辑待补全（P1/P2）**
- 分类管理、月度绩效、团队冲刺、月度总览、回收站恢复/彻底删除、派生待办等
  （Service/Controller 接口签名已建好，调用会返回 `503 待实现`，补全 Impl 即可）
- 接口清单见 PRD 第 11 章。

## 前端启动

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

前端为骨架，路由、请求封装、登录守卫已就绪，各页面 UI 待实现（详见 `frontend/README.md`）。

## 约定

- 接口前缀 `/api`，RESTful，统一响应 `{ code, message, data }`，`code===200` 为成功。
- 鉴权：请求头 `Authorization: Bearer <token>`，401 由前端拦截跳登录。
- 跨域：后端已放行 `http://localhost:5173`。
- 时间戳 UTC 存储，计划日期按用户本地自然日（PRD 4.4）。
