# 社区闲置工具共享与租赁平台（邻租）

社区用户之间闲置工具的共享与租赁平台，支持发布工具、预约、确认、交接、归还、评价的完整闭环，以及管理员对工具、用户、订单的审核与管理。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.3.5 · MyBatis-Plus · MySQL 8 · JWT |
| 前端 | Vue 3 · Vite 5 · Pinia · Axios |
| 部署 | Docker · Docker Compose · Nginx |

## 目录结构

```text
community-tool-share/
├─ README.md                       # 本文档
├─ .gitignore
├─ .github/workflows/ci.yml        # CI：后端 Maven 打包 + 前端 Vite 构建
├─ docker-compose.yml              # 一键启动 MySQL / 后端 / 前端
├─ backend/                        # Spring Boot 后端（含 Dockerfile）
├─ frontend/                       # Vue 3 前端（含 Dockerfile / nginx.conf）
├─ db/
│  ├─ user-wallet.sql              # 用户与资金域建表（陈麒元维护）
│  ├─ community-dispute-platform.sql  # 社区/纠纷/平台域建表（钟姝伶维护）
│  ├─ order.sql                     # 订单域最小载体（待窦源泽替换为完整订单域）
│  └─ init.sql                      # 建表入口占位（待表设计完成后补充）
├─ docs/                           # 需求 / 设计 / 原型 / 测试 / 配置管理文档
└─ scripts/start.sh                # 一键启动脚本（Linux/macOS）
```

## 一键启动（推荐）

只需安装 **Docker Desktop**（含 Docker Compose），无需本地装 JDK / Node / MySQL。

```bash
docker compose up -d --build
```

首次启动会自动构建后端镜像（阿里云 Maven 镜像加速依赖）、前端镜像（npmmirror 加速），并初始化数据库（自动执行 `db/` 下建表脚本 + 种子数据）。

启动完成后访问：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080 |
| MySQL | localhost:3307（容器内 3306） |

## 测试账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 平台管理员（PLATFORM_ADMIN），兼任青禾社区（id=1）管理员 |
| Starling | 123456 | 认证居民，物主（钱包余额 500）；青禾社区成员，名下有种子物品「电钻」与一笔已完成订单 |
| shuling | 123456 | 认证居民，租客（钱包余额 200、冻结押金 50）；青禾社区成员，对应上述订单的租客 |

> 演示数据由 `backend` 的 DemoDataInitializer（用户/钱包）与 `db/community-dispute-platform.sql`（社区/物品/订单/举报种子）在首次启动时写入，用户 ID 固定为 Starling=1、admin=2、shuling=3。

## 端口说明

- `80`：前端（Nginx 托管构建产物），并反向代理 `/api` 到后端。
- `8080`：后端 Spring Boot。
- `3307`：MySQL 容器映射到宿主机的端口；用 3307 是为了避开本机已安装 MySQL 默认占用的 3306。

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

后端（需 JDK 17 + Maven）：

```bash
cd backend
mvn spring-boot:run
```

前端（需 Node 20）：

```bash
cd frontend
npm install
npm run dev        # 默认走本地 Mock 数据
```

> Docker 生产构建已用 `VITE_USE_MOCK=false` 关闭 Mock，前端直接对接真实后端。

## CI

`.github/workflows/ci.yml`：push / PR 到 `main` 时，后端用 JDK 17 打 Maven 包（跳过测试），前端用 Node 20 执行 `npm run build`（关闭 Mock）。
