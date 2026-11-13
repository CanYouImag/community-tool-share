# 社区闲置工具共享与租赁平台（邻租）

社区用户之间闲置工具的共享与租赁平台，支持发布工具、预约、确认、交接、归还、评价的完整闭环，以及管理员对工具、用户、订单、社区、纠纷和举报的审核与管理。

## 当前进度

| 模块 | 状态 |
| --- | --- |
| 登录、注册、个人中心、验证码、头像、实名认证 | 已完成 |
| 钱包、资金流水、管理员调余额 | 已完成 |
| 物品、分类、收藏、审核、图片上传后端 | 已合并，前端待接真实接口 |
| 订单状态机、支付、取物码、归还、验收、纠纷裁决后端 | 已合并，前端待接真实接口 |
| 平台用户管理、实名审核后端 | 已完成 |
| 社区、纠纷、举报、平台管理后端 | 已完成（本次合并） |
| 前后端完整联调、测试报告、用户手册、答辩材料 | 进行中 |

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.3.5 · MyBatis-Plus · MySQL 8 · JWT |
| 前端 | Vue 3 · Vite 5 · Pinia · Axios |
| 部署 | Docker · Docker Compose · Nginx |

## 目录结构

```text
community-tool-share/
├─ README.md                          # 本文档
├─ .github/workflows/ci.yml           # CI：后端 Maven 打包 + 前端 Vite 构建
├─ docker-compose.yml                 # 一键启动 MySQL / 后端 / 前端
├─ backend/                           # Spring Boot 后端（含 Dockerfile）
├─ frontend/                          # Vue 3 前端（含 Dockerfile / nginx.conf）
├─ db/
│  ├─ init.sql                        # 用户 / 钱包 / 物品 / 订单统一建表入口
│  ├─ community-dispute-platform.sql  # 社区 / 纠纷 / 举报 / 平台建表与治理域种子
│  ├─ user-wallet.sql                 # 用户与资金域分域脚本
│  ├─ item.sql                        # 物品域分域脚本
│  ├─ order.sql                       # 订单域分域脚本
│  └─ migrations/                     # 增量迁移脚本
├─ docs/                              # 需求 / 设计 / 原型 / 测试 / 配置管理文档
└─ scripts/start.sh                   # 一键启动脚本（Linux/macOS）
```

## 一键启动（推荐）

只需安装 **Docker Desktop**（含 Docker Compose），无需本地安装 JDK / Node / MySQL。

```bash
docker compose up -d --build
```

首次启动会自动构建后端镜像（阿里云 Maven 镜像加速依赖）、前端镜像（npmmirror 加速），并按顺序初始化数据库：先执行 `db/init.sql` 创建用户、钱包、物品、订单表，再执行 `db/community-dispute-platform.sql` 创建社区、纠纷、举报、平台表并写入演示种子。

启动完成后访问：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| MySQL | localhost:3307（容器内 3306） |

## 测试账号

| 角色 | 账号 | 说明 |
| --- | --- | --- |
| 平台管理员 | `admin / admin123` | 可审核物品与实名、调余额、管理社区与用户、裁决纠纷 |
| 居民（物主） | `Starling / 123456` | 已实名，钱包余额 500.00；青禾社区成员，名下有种子物品「电钻」 |
| 居民（租客） | `shuling / 123456` | 已实名，钱包余额 200.00、冻结押金 100.00；青禾社区成员 |
| 待审核用户 | `pending / 123456` | 用于演示管理端实名审核通过 / 驳回 |

> 演示数据由 `backend` 的 `DemoDataInitializer`（用户 / 钱包 / 实名）与 `db/community-dispute-platform.sql`（社区 / 物品 / 订单 / 举报种子）在首次启动时写入。首次初始化时用户 ID 为 Starling=1、admin=2、shuling=3；已有数据库不会重复写入。

## 端口说明

- `80`：前端（Nginx 托管构建产物），并反向代理 `/api` 到后端。
- `8080`：后端 Spring Boot。
- `3307`：MySQL 容器映射到宿主机的端口；使用 3307 是为了避开本机 MySQL 默认占用的 3306。

## 常用命令

```bash
# 查看容器状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 停止
docker compose down

# 停止并清空数据库（重新初始化）
docker compose down -v && docker compose up -d
```

## 本地开发（可选，不依赖 Docker）

后端需要 JDK 17 + Maven，前端需要 Node 20 + npm。

1. 初始化数据库（在仓库根目录执行；已有旧库建议先重建，避免唯一键冲突）：

```powershell
mysql --default-character-set=utf8mb4 -uroot -p -e "source db/init.sql"
mysql --default-character-set=utf8mb4 -uroot -p -e "source db/community-dispute-platform.sql"
```

2. 启动后端：

```powershell
cd backend
mvn spring-boot:run
```

3. 启动前端（另开终端）：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

> Docker 生产构建已用 `VITE_USE_MOCK=false` 关闭 Mock，前端直接对接真实后端；本地开发也可通过环境变量切换。

## 文档入口

- 后端启动、接口、环境变量和当前限制：`backend/README.md`
- 工作分解与进度：`docs/01-inception/project-management/WBS.md`
- 风险清单：`docs/01-inception/project-management/风险清单.md`
- 开发职责说明：`docs/01-inception/project-management/01-开发职责说明.md`

## 测试

```powershell
cd backend
mvn clean test

cd ../frontend
npm.cmd run build
```

## CI

`.github/workflows/ci.yml`：push / PR 到 `main` 时，后端用 JDK 17 打 Maven 包（跳过测试），前端用 Node 20 执行 `npm run build`（关闭 Mock）。
