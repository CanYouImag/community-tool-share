# 配置库索引（「邻租」社区闲置工具共享与租赁平台）

| 文档信息 | 内容 |
| --- | --- |
| 文档名称 | 「邻租」配置库索引 |
| 文档版本 | V1.0 |
| 作者 | 钟姝伶 |
| 所属基线 | —（随各基线更新） |
| 最后更新 | 2026-11-13 |

> 配置经理：钟姝伶
> 本文档是「邻租」配置库的目录索引，说明各配置项的存放位置、版本与所属基线。
> 配置库以文件目录形式提交，目录结构遵照《03-配置管理计划》第 3.1 节「配置标识」。

## 一、目录结构

```text
community-tool-share/
├─ README.md                       # 项目说明、启动方式、测试账号
├─ backend/                        # 后端源码（Java 17 · Spring Boot 3.3.5 · MyBatis-Plus）
├─ frontend/                       # 前端源码（Vue 3 · Vite 5 · Pinia · Axios）
├─ db/                             # 数据库脚本（init.sql / user-wallet.sql / item.sql / order.sql / community-dispute-platform.sql / migrations/）
├─ docker-compose.yml              # 一键启动 MySQL / 后端 / 前端
├─ .github/workflows/ci.yml        # CI：后端打包 + 前端构建
├─ scripts/start.sh                # 辅助启动脚本
└─ docs/                           # 过程文档（按 RUP 四阶段组织）
   ├─ 01-inception/
   │  └─ project-management/       # 先启阶段：初始/详细软件开发计划、开发职责说明
   ├─ 02-elaboration/
   │  ├─ requirements/             # 精化阶段：软件需求规范（需求分析）、用例图
   │  ├─ design/                   # 精化阶段：设计文档（待设计员补充）
   │  └─ ui/                       # 精化阶段：界面原型、截图
   ├─ 03-construction/
   │  ├─ configuration-management/ # 构建阶段：配置管理计划、基线记录与变更记录
   │  └─ test/                     # 构建阶段：测试计划/测试设计（待测试经理补充）
   └─ 04-transition/               # 产品化阶段：测试报告、用户手册（待补充）
```

## 二、配置项清单（版本 / 基线）

| 配置项 | 位置 | 版本 | 所属基线 | 责任人 |
| --- | --- | --- | --- | --- |
| 初始软件开发计划 | `docs/01-inception/project-management/` | V1.0 | 计划基线-v1.0 | 陈麒元 |
| 详细软件开发计划 | `docs/01-inception/project-management/` | V1.0 | 计划基线-v1.0 | 陈麒元 |
| 开发职责说明 | `docs/01-inception/project-management/` | V1.0 | 计划基线-v1.0 | 陈麒元 |
| 软件需求规范（需求分析） | `docs/02-elaboration/requirements/` | V1.0 | 需求基线-v1.0 | 宋嘉幸 |
| 界面原型 | `docs/02-elaboration/ui/` | V1.0 | 原型基线-v1.0 | 林星 |
| 设计文档 | `docs/02-elaboration/design/` | 待补充 | 设计基线-v1.0（待建立） | 窦源泽 |
| 后端源码 | `backend/` | — | 代码基线-v1.0（待建立） | 全体开发 |
| 前端源码 | `frontend/` | — | 代码基线-v1.0（待建立） | 全体开发 |
| 数据库脚本 | `db/` | — | 代码基线-v1.0（待建立） | 窦源泽 |
| 配置管理计划 | `docs/03-construction/configuration-management/` | V1.0 | 计划基线-v1.0（配套） | 钟姝伶 |
| 基线记录与变更记录 | `docs/03-construction/configuration-management/` | V1.0 | —（随各基线更新） | 钟姝伶 |
| 软件测试计划 / 测试设计 | `docs/03-construction/test/` | 待补充 | 测试基线-v1.0（待建立） | 常子衡 |
| 软件测试报告 | `docs/04-transition/` | 待补充 | 发布基线-v1.0（待建立） | 常子衡 |
| 用户手册 | `docs/04-transition/` | 待补充 | 发布基线-v1.0（待建立） | 林星 |

## 三、基线状态

| 基线 | Tag | 提交 | 状态 |
| --- | --- | --- | --- |
| 计划基线-v1.0 | `计划基线-v1.0` | 79c5806 | 已建立 |
| 需求基线-v1.0 | `需求基线-v1.0` | 1a07a5c | 已建立 |
| 原型基线-v1.0 | `原型基线-v1.0` | b1c2c9e | 已建立 |
| 设计基线-v1.0 | 待建立 | — | 待设计员窦源泽交付设计文档 |
| 代码基线-v1.0 | 待建立 | — | 待冒烟验证通过后打 tag |
| 测试基线-v1.0 | 待建立 | — | 待测试经理常子衡交付测试计划/设计 |
| 发布基线-v1.0 | 待建立 | — | 待测试报告 + 用户手册 + 全量工件 |

> 详细变更历史见 `docs/03-construction/configuration-management/03-基线记录与变更记录.md`。
