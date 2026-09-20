# 社区闲置工具共享与租赁平台

社区用户之间闲置工具的共享与租赁平台，支持发布工具、预约、确认、交接、归还、评价的完整闭环，以及管理员对工具、用户、订单的审核与管理。

## 项目结构

```text
community-tool-share/
├─ README.md
├─ .gitignore
├─ .github/
│  ├─ workflows/ci.yml
│  ├─ ISSUE_TEMPLATE/
│  └─ PULL_REQUEST_TEMPLATE.md
├─ docker-compose.yml
├─ backend/
├─ frontend/
├─ db/
│  └─ init.sql
├─ docs/                     # 课程/项目文档（仅本地，不入库）
└─ scripts/
```

## 角色分工（GitHub 协作）

| 角色 | 负责人 | 主要交付物 | 代码/文档目录 |
|---|---|---|---|
| 项目经理 | 陈麒元 | 初始/详细软件项目计划 | docs/01-inception |
| 系统分析员 | 宋嘉幸 | 软件需求规范、用例 | docs/requirements |
| 设计员 | 宋嘉幸 | 设计文档、后端、数据库 | backend、docs/design |
| 测试经理 | 常子衡 | 测试计划/设计/报告 | tests、docs/test |
| 配置经理 | 钟姝伶 | 配置库、CI、Release | .github、docker-compose.yml |
| 界面设计员 | 林星 | 原型、前端、用户手册 | frontend、docs/ui |

## 开发流程

- 分支策略与 PR 规范详见 `docs/03-construction/配置管理计划.md`（本地）。
- 需求拆分请建立 `REQ-xx` Issue；缺陷请建立带 `bug` 标签的 Issue。
- `main` 分支受保护，需经 PR 审核后合并。

## 状态

> 项目初始化阶段：目录骨架已建立，代码与文档待填充。