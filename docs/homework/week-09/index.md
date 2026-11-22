# 第 09 周

在第 08 周的三个业务服务前面增加 `gateway-service`。对外端口是 `8080`。登录和 JWT 放在 user-service。订单查询需要登录，用户列表需要管理员。截图来自本机 Windows PowerShell 窗口。`scripts/auth-test.sh` 的输出不打印令牌。登录响应截图保留了当次接口返回的 JSON，其中没有签名密钥。

认证流程和路由表在 [docs/security.md](../../security.md)。

## 1. API Gateway

实现思路：新建 `services/gateway-service`，使用 Spring Cloud Gateway。四条路由都写服务名：`/api/auth/**` 和 `/api/users/**` 转到 `lb://user-service`，`/api/products/**` 转到 `lb://product-service`，`/api/orders/**` 转到 `lb://order-service`。网关自己提供 `GET /api/health`，Actuator 提供 `GET /actuator/health`。

运行命令：

```powershell
docker compose up -d --build gateway-service
bash scripts/auth-test.sh
```

结果：`GET http://127.0.0.1:8080/api/health` 返回 200，`GET /actuator/health` 返回 200。`GET /api/products` 经网关转到 product-service，返回 200。网关日志写出实际使用的服务名，例如 `网关按服务名转发 product-service GET /api/products`、`user-service POST /api/auth/login`、`order-service GET /api/orders`。

对应截图：

- 命令文件名和关键结果：[screenshots/5455e587a93acbb989ce89101d6e6a32.png](screenshots/5455e587a93acbb989ce89101d6e6a32.png)
- 路由和健康检查：[screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png](screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png)
- 服务名转发日志：[screenshots/cc884c91439ed163f0482b73f4ccd59e.png](screenshots/cc884c91439ed163f0482b73f4ccd59e.png)
- 网关访问、登录响应和鉴权失败：[screenshots/855a77999658a6d66db55cbb18970789.png](screenshots/855a77999658a6d66db55cbb18970789.png)

## 2. 认证服务

实现思路：`POST /api/auth/login` 校验用户名和口令，成功后签发 JWT。角色有 `BUYER` 和 `ADMIN`。测试账号是 `buyer` / `buyer-week09` 和 `admin` / `admin-week09`。启动时写入数据库的是 BCrypt 哈希，`password_hash` 不以明文保存口令。签名密钥使用环境变量 `JWT_SECRET`，仓库中的示例是占位符。

运行命令：

```powershell
bash scripts/auth-test.sh
.\monolith\mvnw.cmd -f services\user-service\pom.xml test
```

结果：买家登录返回 200，`role=BUYER`。管理员登录返回 200，`role=ADMIN`。脚本只打印 `token=present`，不打印令牌。用户服务测试在 `2026-11-22T23:34:06+08:00` 结束，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，总时间 `10.742 s`。其中检查了哈希以 `$2` 开头、库里的哈希不包含口令明文，以及错误口令返回「用户名或密码错误」。

对应截图：

- 登录结果：[screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png](screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png)
- 登录响应 JSON：[screenshots/855a77999658a6d66db55cbb18970789.png](screenshots/855a77999658a6d66db55cbb18970789.png)
- 服务层测试：[screenshots/f699546b02393a713942d0c5218101b3.png](screenshots/f699546b02393a713942d0c5218101b3.png)

## 3. 网关鉴权

实现思路：未登录访问 `/api/orders/**` 返回 401，`message` 是「未登录」。携带有效令牌返回 200。`GET /api/users` 需要 `ADMIN`，买家访问返回 403，`message` 是「权限不足」。网关先删除客户端传入的 `X-User-Id`、`X-User-Name`、`X-User-Role`，再按令牌写回。user-service 的用户列表只认 JWT，不认这三个头，所以直接访问 `8081` 并伪造管理员头仍然是 401。

运行命令：

```powershell
bash scripts/auth-test.sh
```

结果：

```text
PASS unauthenticated GET /api/orders -> 401
PASS buyer token GET /api/orders -> 200
PASS buyer token GET /api/users -> 403
PASS forged X-User-Role ignored -> 403
PASS direct user-service forged header -> 401
PASS admin token GET /api/users -> 200
```

网关日志里对应出现「网关拒绝未登录 GET /api/orders」和「网关拒绝权限不足 GET /api/users」。

对应截图：

- 鉴权结果：[screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png](screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png)
- 401 与 403 响应体：[screenshots/855a77999658a6d66db55cbb18970789.png](screenshots/855a77999658a6d66db55cbb18970789.png)
- 拒绝日志：[screenshots/cc884c91439ed163f0482b73f4ccd59e.png](screenshots/cc884c91439ed163f0482b73f4ccd59e.png)

## 4. 测试脚本

实现思路：`scripts/auth-test.sh` 从仓库根目录运行。它依次检查未登录失败、登录成功、携带令牌访问订单成功、买家访问管理接口失败。另外检查伪造身份头和直连用户服务。失败时打印 `AUTH TEST FAILED` 并返回 1。

运行命令：

```powershell
bash scripts/auth-test.sh
.\monolith\mvnw.cmd -f services\user-service\pom.xml test
.\monolith\mvnw.cmd -f services\gateway-service\pom.xml test
```

结果：脚本输出 `AUTH TEST PASSED`，退出码 0。用户服务测试 5 个全部通过，结束时间 `2026-11-22T23:34:06+08:00`。网关路由策略测试 2 个全部通过，`BUILD SUCCESS`，结束时间 `2026-11-22T23:29:21+08:00`。

对应截图：

- 带文件名的脚本运行：[screenshots/5455e587a93acbb989ce89101d6e6a32.png](screenshots/5455e587a93acbb989ce89101d6e6a32.png)
- 脚本：[screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png](screenshots/6d71b0e8dd6071918e21d7aa88d2a2f6.png)
- 用户服务测试：[screenshots/f699546b02393a713942d0c5218101b3.png](screenshots/f699546b02393a713942d0c5218101b3.png)

## 5. 作业系统填写稿

公开仓库：https://github.com/user10086799/microservices-practice-2412190214

下面四段可以直接复制。仓库和作业系统都尚未提交。

### 题目 1：API Gateway

gateway-service 使用 8080 作为统一入口。/api/auth 和 /api/users 转到服务名 user-service，/api/products 转到 product-service，/api/orders 转到 order-service。GET /api/health 和 /actuator/health 都返回 200。经网关访问商品列表返回 200。日志是「网关按服务名转发 product-service GET /api/products」。终端里能看到命令 bash scripts/auth-test.sh 和 AUTH TEST PASSED。

建议上传：`5455e587a93acbb989ce89101d6e6a32.png`、`855a77999658a6d66db55cbb18970789.png`、`cc884c91439ed163f0482b73f4ccd59e.png`。

### 题目 2：认证服务

POST /api/auth/login 为 buyer 和 admin 签发 JWT。buyer 的角色是 BUYER，admin 的角色是 ADMIN。口令 buyer-week09、admin-week09 只作为测试账号公布，数据库保存 BCrypt 哈希。登录响应 HTTP 200，JSON 中 role 为 BUYER、tokenType 为 Bearer。用户服务测试 5 个通过。

建议上传：`855a77999658a6d66db55cbb18970789.png`、`f699546b02393a713942d0c5218101b3.png`。

### 题目 3：网关鉴权

未登录访问 GET /api/orders 返回 HTTP 401，正文是「未登录」。买家登录后访问 GET /api/users 返回 HTTP 403，正文是「权限不足」。同一买家令牌再带上伪造的 X-User-Role: ADMIN，仍然返回 403。直接请求用户服务并伪造管理员头返回 401。管理员令牌访问用户列表返回 200。

建议上传：`855a77999658a6d66db55cbb18970789.png`、`5455e587a93acbb989ce89101d6e6a32.png`。

### 题目 4：测试脚本

在仓库根目录执行 bash scripts/auth-test.sh。终端同时显示该文件名和 AUTH TEST PASSED。脚本覆盖未登录、登录、携带令牌访问和低权限访问管理接口，退出码为 0。用户服务 mvn test 在 2026-11-22T23:34:06 结束，5 个测试 0 失败。网关策略测试 2 个通过。

建议上传：`5455e587a93acbb989ce89101d6e6a32.png`、`f699546b02393a713942d0c5218101b3.png`。
