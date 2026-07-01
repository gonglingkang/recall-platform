# Recall 前端（骨架）

> 本目录为 **Vue3 + Vite + TypeScript 骨架**，仅完成项目脚手架、请求封装、路由与状态管理的最小可运行结构。
> **实际页面功能由其他模型/开发者实现**，本 README 给出对接约定与未完成清单。

## 技术栈

- Vue 3 + `<script setup>`
- Vite 5
- TypeScript 5
- Vue Router 4（路由表已按 PRD 第 10 章建好）
- Pinia 2（已建 `auth` store）
- axios（已封装 `src/api/request.ts`）

## 目录结构

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json / tsconfig.node.json
├── .env.development            # VITE_API_BASE_URL=http://localhost:20020
├── package.json
└── src/
    ├── main.ts                 # 入口：注册 Pinia + Router
    ├── App.vue
    ├── env.d.ts
    ├── api/
    │   ├── request.ts          # axios 实例：自动带 JWT，401 跳登录
    │   └── types.ts            # ApiResult / ResultCode
    ├── stores/
    │   └── auth.ts             # 登录态（token/user 存 localStorage）
    ├── router/
    │   └── index.ts            # 路由表 + 登录守卫(redirect 回跳)
    ├── components/
    │   └── PlaceholderView.vue # 占位组件
    ├── assets/main.css
    └── views/                  # 各路由占位页（含 TODO 提示）
```

## 启动

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # 类型检查 + 构建
```

## 与后端对接约定

- 接口前缀：`/api`，RESTful 风格（见后端 PRD 第 11 章）。
- 统一响应结构：`{ code, message, data }`，`code === 200` 为成功。
- 鉴权：请求头 `Authorization: Bearer <token>`，token 由 `POST /api/auth/login` 返回。
- 401：`request.ts` 已统一拦截——清登录态并跳 `/login?redirect=...`。
- 跨域：后端已允许 `http://localhost:5173`。

## 已就绪 / 待实现

| 模块 | 状态 |
|------|------|
| 项目脚手架 + 依赖 | ✅ 已就绪 |
| axios 请求封装（JWT/401） | ✅ 已就绪 |
| 路由表 + 登录守卫 | ✅ 已就绪 |
| Pinia auth store | ✅ 已就绪 |
| 各业务页面 UI 与逻辑 | ⬜ 待实现（每个 view 文件内有 TODO 提示对应 PRD 章节） |
| 布局组件（左侧导航+右侧内容） | ⬜ 待实现 |
| 接口模块化封装（按业务拆 api/*.ts） | ⬜ 待实现 |

> 实现各页面时，请参考 `docs/PRD-待办系统产品需求文档-v1.1.md` 第 6 章（功能详细需求）与第 10 章（路由）。
