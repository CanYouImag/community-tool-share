# 社区闲置工具共享与租赁平台

社区用户之间闲置工具的共享与租赁平台，支持发布工具、预约、确认、交接、归还、评价的完整闭环，以及管理员对工具、用户、订单的审核与管理。

## 当前进度

| 模块 | 状态 |
| --- | --- |
| 登录、注册、个人中心、验证码、头像、实名认证 | 已完成 |
| 钱包、资金流水、管理员调余额 | 已完成 |
| 物品、分类、收藏、审核、图片上传后端 | 已合并，前端待接真实接口 |
| 订单状态机、支付、取物码、归还、验收、纠纷裁决后端 | 已合并，前端待接真实接口 |
| 平台用户管理、实名审核后端 | 已完成（本轮新增） |
| 社区、纠纷、举报、平台统计 | 后端未实现，前端仍 Mock |
| 前后端完整联调、测试报告、用户手册、答辩材料 | 进行中 |

## 项目结构

```text
community-tool-share/
├─ README.md
├─ .github/
├─ docker-compose.yml
├─ backend/
├─ frontend/
├─ db/
│  ├─ init.sql
│  └─ migrations/
└─ scripts/
```

## 快速启动

1. 初始化数据库（仓库根目录）：

```powershell
mysql --default-character-set=utf8mb4 -uroot -p -e "source db/init.sql"
```

2. 启动后端：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

3. 启动前端（另开终端）：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

后端默认 `http://localhost:8080`，Swagger 为 `http://localhost:8080/swagger-ui.html`。

## 演示账号

| 角色 | 账号 | 说明 |
| --- | --- | --- |
| 居民 | `Starling / 123456` | 已实名，钱包余额 500.00 |
| 平台管理员 | `admin / admin123` | 可审核物品、调余额、审核实名、裁决纠纷 |
| 待审核用户 | `pending / 123456` | 用于演示实名审核 |

## 文档入口

- 后端启动、接口、环境变量和当前限制：`backend/README.md`
- 工作分解与进度：`docs/project-management/WBS.md`
- 风险清单：`docs/project-management/风险清单.md`
- 开发职责说明：`docs/project-management/开发职责说明.md`

## 测试

```powershell
cd backend
mvn clean test

cd ../frontend
npm.cmd run build
```
