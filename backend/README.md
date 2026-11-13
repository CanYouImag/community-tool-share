# 邻租后端（用户、钱包、物品、订单、社区、纠纷、平台）

## 技术栈

- Java 17+（本机 JDK 21 可直接编译，Maven 按 Java 17 目标编译）
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.7
- MySQL 8
- JWT（jjwt 0.12.6）
- SpringDoc OpenAPI（Swagger UI）

## 模块概览

| 模块 | 代码位置 | 当前状态 |
| --- | --- | --- |
| 认证、用户、实名 | `backend/.../user` | 已完成主体；本轮补齐管理端用户/实名审核接口 |
| 钱包、资金流水 | `backend/.../wallet` | 已完成主体；管理员调余额可用 |
| 物品、分类、收藏、审核 | `backend/.../item` | 后端已合并；前端待接真实接口 |
| 订单、状态机、取物码、结算 | `backend/.../order` | 后端已合并；前端待接真实接口 |
| 社区、成员、公告 | `backend/.../community` | 后端已完成；前端已接真实接口 |
| 纠纷、仲裁、举报 | `backend/.../dispute` | 后端已完成；前端已接真实接口 |
| 平台社区、用户、配置、统计、日志 | `backend/.../platform` | 后端已完成 |

## 启动步骤

1. 建库建表（在仓库根目录执行）：

```powershell
mysql --default-character-set=utf8mb4 -uroot -p -e "source db/init.sql"
mysql --default-character-set=utf8mb4 -uroot -p -e "source db/community-dispute-platform.sql"
```

`db/init.sql` 已合并 user、wallet、item、order 四个域的建表脚本，`db/community-dispute-platform.sql` 负责社区、纠纷、举报、平台表及治理域种子。注意：

- 必须带 `--default-character-set=utf8mb4`，否则中文分类名会乱码。
- 分类和治理域初始化数据不是幂等的；重复执行可能因唯一键冲突失败。需要重置时先删库再导入。
- 只做用户/钱包域调试时，也可以使用增量脚本 `db/user-wallet.sql`。
- 使用 Docker Compose 时会按顺序自动执行这两个脚本，无需手工导入。

2. 配置数据库连接。默认读取：

```text
DB_URL=jdbc:mysql://localhost:3306/linzu?...
DB_USERNAME=root
DB_PASSWORD=123456
JWT_SECRET=linzu-demo-secret-key-change-me-32bytes
```

如果本机密码不是 `123456`，启动前设置环境变量：

```powershell
$env:DB_PASSWORD="你的密码"
```

3. 进入 `backend` 目录启动（与上面的数据库环境变量设置使用同一个终端）：

```powershell
cd backend
mvn spring-boot:run
```

4. 打开 Swagger：

```text
http://localhost:8080/swagger-ui.html
```

> 已有旧数据库如果还是 `id_card_no VARCHAR(32)`，先执行 `db/migrations/2026-11-12-user-real-name-encrypt.sql`。
> 新库直接用 `db/init.sql` 即可。

## 可选环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `JWT_SECRET` | 演示密钥 | 生产环境必须覆盖，至少 32 字节 |
| `ID_CARD_KEY` | 演示密钥 | 身份证 AES-GCM 加密密钥，生产环境必须覆盖 |
| `UPLOAD_DIR` | `./uploads` | 本地头像/物品图片存储目录 |
| `CAPTCHA_ENABLED` | `true` | 是否启用图形验证码；本地调试可设为 `false` |
| `AUTO_VERIFY_REAL_NAME` | `false` | 是否跳过实名审核；设为 `true` 时提交即通过 |
| `ORDER_COMMISSION_RATE` | `0` | 平台佣金比例（0~1）；需求默认 10%，演示前建议设为 `0.1` |
| `SERVER_PORT` | `8080` | 后端端口 |
| `VITE_API_TARGET` | `http://localhost:8080` | 前端代理目标（前端目录使用） |

## 演示账号

首次启动会自动插入：

| 角色 | 账号 | 说明 |
| --- | --- | --- |
| 居民（物主） | `Starling / 123456` | 已实名，钱包余额 500.00，青禾社区成员 |
| 居民（租客） | `shuling / 123456` | 已实名，钱包余额 200.00、冻结押金 100.00，青禾社区成员 |
| 平台管理员 | `admin / admin123` | `PLATFORM_ADMIN`，可审核物品、调余额、审核实名、管理社区与用户、裁决纠纷 |
| 待审核用户 | `pending / 123456` | 已提交实名但状态为 `PENDING`，用于演示管理端审核通过/驳回 |

配置项在 `application.yml` 的 `app.demo` 下，可关闭。实名认证测试可用身份证号 `220101200101100115`（仅用于校验格式和校验位，不对应真实身份）。

## 登录验证码

登录前先调用 `GET /api/v1/auth/captcha`，返回：

```json
{
  "captchaId": "32 位 id",
  "image": "data:image/svg+xml;base64,...",
  "expireSeconds": 120
}
```

登录请求体需要带 `captchaId` 和 `captchaCode`，验证码一次性使用、2 分钟过期。
如果本地不想每次输入，可设置 `CAPTCHA_ENABLED=false` 后重启后端。

## 已实现接口

### 用户与钱包

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/auth/captcha` | 获取图形验证码 |
| POST | `/api/v1/auth/register` | 注册 |
| POST | `/api/v1/auth/login` | 登录（需验证码） |
| GET | `/api/v1/auth/me` | 当前用户 |
| POST | `/api/v1/auth/logout` | 退出登录（前端清除 token） |
| GET | `/api/v1/users/me` | 个人资料 |
| PUT | `/api/v1/users/me` | 修改资料 |
| PUT | `/api/v1/users/me/password` | 修改密码 |
| POST | `/api/v1/users/me/real-name` | 提交实名认证 |
| POST | `/api/v1/users/me/avatar` | 上传头像（multipart） |
| PUT | `/api/v1/users/me/avatar` | 修改头像（兼容 URL 方式） |
| GET | `/api/v1/wallet` | 钱包 |
| GET | `/api/v1/wallet/transactions` | 资金流水 |
| GET | `/api/v1/wallet/flows` | 资金流水（兼容旧前端 `{ list, total }`） |
| GET | `/api/v1/wallet/deposits` | 押金明细（兼容旧前端） |
| POST | `/api/v1/wallet/freeze` | 冻结余额 |
| POST | `/api/v1/wallet/unfreeze` | 解冻余额 |
| POST | `/api/v1/wallet/admin/adjust` | 管理员调整余额（`platform:balance`） |

### 平台用户管理（本轮新增）

权限码已与 `PermissionCodes` 对齐：用户管理使用 `platform:ban`，实名审核使用 `platform:cert`。`PLATFORM_ADMIN` 会自动放行。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/admin/users` | 分页查询用户；参数 `keyword/role/status/page/size` |
| POST | `/api/v1/admin/users/{id}/ban` | 封禁用户；不能封禁自己或平台管理员，请求体可选 `{ "reason": "原因" }` 并写入操作日志 |
| POST | `/api/v1/admin/users/{id}/unban` | 解封用户，请求体可选 `{ "reason": "原因" }` 并写入操作日志 |
| GET | `/api/v1/admin/real-names` | 分页查询实名申请；审核队列传 `status=PENDING`，支持 `keyword` |
| POST | `/api/v1/admin/real-names/{id}/approve` | 通过实名认证，普通用户角色升级为 `CERTIFIED` |
| POST | `/api/v1/admin/real-names/{id}/reject` | 驳回实名认证，请求体 `{ "reason": "驳回原因" }` |

管理端返回字段：

- 用户列表 `records[]`：`id`、`username`、`phone`、`nickname`、`avatarUrl`、`role`、`status`、`creditScore`、`realNameStatus`、`realName`、`idCardMasked`、`rejectReason`、`createdAt`。
- 实名审核列表 `records[]`：`id`（申请 ID）、`userId`、`username`、`nickname`、`realName`、`idCardMasked`、`status`、`rejectReason`、`createdAt`、`updatedAt`。
- 两个列表都使用统一分页结构 `{ records, total, page, size }`。

实名审核流程：

1. 普通用户注册后调用 `POST /api/v1/users/me/real-name`，默认进入 `PENDING`。
2. 管理员调用 `GET /api/v1/admin/real-names?status=PENDING` 获取审核队列。
3. 调用 `approve` 或 `reject`。通过后用户 `realNameStatus=VERIFIED`、角色变为 `CERTIFIED`；驳回后保留原因，用户可重新提交。
4. 演示账号 `pending / 123456` 已预置一条待审核记录。

### 社区、纠纷、举报与平台治理

| 域 | 主要路径 | 说明 |
| --- | --- | --- |
| 社区 | `/api/v1/communities` | 社区列表/详情、加入/退出、成员、加入申请审核、公告、我的社区管理员身份 |
| 成员关系 | `/api/v1/memberships/mine` | 当前用户加入的社区 |
| 纠纷 | `/api/v1/disputes` | 发起纠纷、列表/详情、响应、留言、调解、上报仲裁、仲裁裁决、裁决书 |
| 举报 | `/api/v1/reports` | 提交举报、我的举报、举报列表、社区/平台处理 |
| 平台治理 | `/api/v1/admin` | 社区 CRUD、任命/罢免社区管理员、平台配置、平台/社区统计、操作日志 |

### 物品与订单

| 域 | 主要路径 | 说明 |
| --- | --- | --- |
| 物品 | `/api/v1/items`、`/api/v1/items/{id}` | 搜索、详情、发布、编辑、删除、上下架、收藏、图片上传 |
| 物品管理 | `/api/v1/admin/items` | 审核、置顶、违规下架、分类维护 |
| 订单 | `/api/v1/orders` | 创建、列表、详情、支付、接单/拒单、取物、归还、验收、取消、留言 |
| 订单管理 | `/api/v1/admin/orders/{id}/resolve` | 平台纠纷裁决 |

完整请求参数和响应字段以 Swagger 为准。物品与订单前端仍有多处 Mock/字段契约不一致，联调时先按后端字段调整。

## 统一返回与鉴权

统一返回体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

鉴权请求头：

```text
Authorization: Bearer <token>
```

## 与前端联调

前端 `request.js` 的 `baseURL` 是 `/api`，`vite.config.js` 已配置开发代理，默认转发到 `http://localhost:8080`。

如果 8080 被其他程序占用，可以在后端终端设置端口后重新启动：

```powershell
$env:SERVER_PORT = "8081"
mvn spring-boot:run
```

在另一个终端进入 `frontend` 目录，设置相同的目标端口，再启动前端：

```powershell
$env:VITE_API_TARGET = "http://localhost:8081"
npm.cmd run dev
```

上述环境变量只对各自当前终端生效。使用 8081 时，Swagger 地址也改为 `http://localhost:8081/swagger-ui.html`。部署时可用 Nginx 把 `/api` 和 `/uploads` 都转发到后端。后端已配置 `localhost` / `127.0.0.1` 的 CORS。

前端默认仍开启物品、社区等模块的 Mock；登录、个人资料、钱包、物品详情下单和订单主流程已接真实后端。支付、接单、取物、归还、验收可在订单详情页操作。

## 头像与实名信息

- 头像上传保存到 `UPLOAD_DIR`（默认 `backend/uploads`），通过 `/uploads/**` 访问；前端 Vite 已代理 `/uploads` 到后端。
- 支持 JPG、PNG、GIF、WebP，最大 2MB，并校验文件头，防止只改扩展名。
- 身份证号提交前校验 18 位校验位，入库使用 AES-GCM 加密（`enc:v1:` 前缀），页面和管理端只返回脱敏值。
- 历史明文身份证数据仍可读取，下次提交认证时会自动改为密文。
- 管理端审核接口不会返回身份证密文，只返回前 6 位和后 4 位。

## 测试

```powershell
cd backend
mvn clean test
```

当前共 43 个后端测试，覆盖身份证校验/加密/脱敏、验证码、头像存储、钱包对账、订单租期/佣金/逾期/自动验收/取消守卫、订单评价、个人资料实名驳回原因、管理端用户封禁/解封与实名审核。

## 数据库脚本

- `db/init.sql`：核心建库脚本，已合并 user、wallet、item、order 四个域。
- `db/community-dispute-platform.sql`：社区、纠纷、举报、平台建表与治理域种子。
- `db/user-wallet.sql`：用户/钱包域增量脚本，由陈麒元维护。
- `db/migrations/`：旧库增量迁移脚本。

最终由窦源泽统一维护 `db/init.sql`；各域新增 DDL 时不要直接覆盖其他域的建表语句。

## 当前限制

- 站内通知、举报处置联动、登录失败锁定、XSS 过滤等治理与安全项仍需继续补。
- 物品、订单、社区、纠纷域还没有完整单元测试，前端契约仍需联调确认。
- 旧库升级需执行 `db/migrations/2026-11-13-order-features.sql`（评价、凭证、逾期字段）。
- 旧库升级需先补 `t_item.community_id`、`t_order.community_id` 等字段，再执行治理域脚本；新库直接按两个脚本初始化即可。
