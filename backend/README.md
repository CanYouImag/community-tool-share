# 邻租后端（用户与资金域）

## 技术栈

- Java 17+（本机 JDK 21 可直接编译，Maven 按 Java 17 目标编译）
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.7
- MySQL 8
- JWT（jjwt 0.12.6）
- SpringDoc OpenAPI（Swagger UI）

## 启动步骤

1. 建库建表（在仓库根目录执行）：

```powershell
mysql -uroot -p -e "source db/user-wallet.sql"
```

如果你用的是 cmd，也可以：

```bat
mysql -uroot -p < db\user-wallet.sql
```

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

## 演示账号

首次启动会自动插入：

```text
用户名：Starling
密码：123456
```

演示账号已实名认证，钱包余额 500.00。配置项在 `application.yml` 的 `app.demo` 下，可关闭。

## 已实现接口

| 方法 | 路径                          | 说明                       |
| ---- | ----------------------------- | -------------------------- |
| POST | `/api/v1/auth/register`       | 注册                       |
| POST | `/api/v1/auth/login`          | 登录                       |
| GET  | `/api/v1/auth/me`             | 当前用户                   |
| POST | `/api/v1/auth/logout`         | 退出登录（前端清除 token） |
| GET  | `/api/v1/users/me`            | 个人资料                   |
| PUT  | `/api/v1/users/me`            | 修改资料                   |
| PUT  | `/api/v1/users/me/password`   | 修改密码                   |
| POST | `/api/v1/users/me/real-name`  | 提交实名认证               |
| PUT  | `/api/v1/users/me/avatar`     | 修改头像                   |
| GET  | `/api/v1/wallet`              | 钱包                       |
| GET  | `/api/v1/wallet/transactions` | 资金流水                   |
| POST | `/api/v1/wallet/freeze`       | 冻结余额                   |
| POST | `/api/v1/wallet/unfreeze`     | 解冻余额                   |

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

上述环境变量只对各自当前终端生效。使用 8081 时，Swagger 地址也改为 `http://localhost:8081/swagger-ui.html`。部署时可用 Nginx 把 `/api` 转发到后端。后端已配置 `localhost` / `127.0.0.1` 的 CORS。

前端默认仍开启物品、社区等模块的 Mock；登录和个人资料请求访问真实后端。能登录不代表订单创建、支付等流程已完成联调。

## 数据库脚本

`db/user-wallet.sql` 由陈麒元维护，包含：

- `t_user`
- `t_user_real_name`
- `t_wallet`
- `t_wallet_transaction`

最终由窦源泽合并进 `db/init.sql`，不要直接覆盖其他域的建表脚本。
