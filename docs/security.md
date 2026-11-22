# 认证与网关

统一入口是 `gateway-service`，容器端口 `8080`。业务服务仍注册在 Nacos 分组 `CXT_SHOPPING`。网关用服务名转发，不写容器 IP。

## 测试账号

这两个账号由 user-service 在启动时写入。数据库列 `password_hash` 只保存 BCrypt 哈希，代码里同样只保存哈希。下面的口令是课程测试账号。

| 用户名 | 密码 | 角色 | 能做的事 |
| --- | --- | --- | --- |
| buyer | buyer-week09 | BUYER | 登录后查看订单 |
| admin | admin-week09 | ADMIN | 登录后查看订单，并列出全部用户 |

购物流程里 `POST /api/users` 仍只保存买家姓名，不设置登录口令。这类用户不能登录。

## 认证流程

1. 客户端调用 `POST /api/auth/login`，请求体是 `{"username","password"}`。
2. user-service 按用户名查出账号，用 BCrypt 核对口令。失败时返回 401，`message` 是「用户名或密码错误」。
3. 成功时返回 JSON：`token`、`tokenType`（Bearer）、`username`、`role`、`userId`。令牌用 HMAC-SHA256 签名，有效期 2 小时。签名密钥来自环境变量 `JWT_SECRET`，仓库里的值是占位符。
4. 之后的请求在 `Authorization: Bearer <token>` 里带上令牌。

文档不粘贴真实令牌。

## 角色

- `BUYER`：可以访问需要登录的订单接口。
- `ADMIN`：包含买家的访问范围，另外可以 `GET /api/users` 列出全部用户。

## 网关路由

| 路由 id | 路径 | 目标 | 访问规则 |
| --- | --- | --- | --- |
| auth-service | `/api/auth/**` | `lb://user-service` | 公开 |
| user-service | `/api/users/**` | `lb://user-service` | `GET /api/users` 需要 ADMIN；`POST /api/users` 公开；其余 `/api/users/` 需要登录 |
| product-service | `/api/products/**` | `lb://product-service` | 公开 |
| order-service | `/api/orders/**` | `lb://order-service` | 需要登录 |

网关自己提供 `GET /api/health` 和 `GET /actuator/health`。

未登录访问受保护接口返回 401，`message` 是「未登录」。角色不够返回 403，`message` 是「权限不足」。

## 身份请求头

网关在转发前去掉客户端传来的 `X-User-Id`、`X-User-Name`、`X-User-Role`。只有令牌验签通过后，才按令牌里的用户编号、用户名和角色重新写入这三个头。买家令牌即使自带 `X-User-Role: ADMIN`，列出用户仍然返回 403。

user-service 的用户列表只检查 `Authorization` 里的 JWT，不读取上述三个头。因此直接访问用户服务的宿主机端口，并伪造 `X-User-Role: ADMIN`，会得到 401。跳过网关不能变成管理员。

订单、商品服务的宿主机端口仍保留，方便前面几周从本机调试。对外应只使用网关端口。订单接口的登录校验放在网关；用户列表在网关和 user-service 都会校验角色。
