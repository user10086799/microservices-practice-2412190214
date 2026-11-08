# 第 06 周

把购物系统拆成 user-service、product-service 和 order-service。`monolith/` 继续保留为单体版本。包名仍是 `com.zjgsu.cxt`。服务边界写在 [docs/microservices-design.md](../../microservices-design.md)。

截图根据本次实际命令输出整理，状态码和响应体与运行结果一致。

## 1. 服务拆分设计

实现思路：买家、商品、订单分属三个服务。user-service 只写 `shopping_user.users`，product-service 只写 `shopping_product.product`，order-service 只写 `shopping_order.orders`。订单保存对方的 `user_id` 和 `product_id`，不建跨库外键，也不调用对方的 Repository。

端口：用户 8081，商品 8082，订单 8083。三个库在同一个 MySQL 8.4.11 实例里，账号分别是 `user_app`、`product_app`、`order_app`。

下单时 order-service 先请求用户服务确认买家，再请求商品服务扣减库存，两次都成功后才写本地订单。

本项是设计文档，没有单独的运行命令。结论见设计文档中的映射表和 Mermaid 架构图。

## 2. 代码拆分

实现思路：每个服务有独立的 `pom.xml`、`application.yml`、Controller、Service 和测试。order-service 使用 RestTemplate 调用另外两个服务。地址写在 `clients.user-service-url` 和 `clients.product-service-url`，容器里用 `USER_SERVICE_URL`、`PRODUCT_SERVICE_URL` 覆盖。连接超时 2 秒，读取超时 3 秒。用户不存在、商品不存在、库存不足或下游连不上时返回明确错误，不写订单。

运行命令：

```powershell
.\monolith\mvnw.cmd -f services\user-service\pom.xml test
.\monolith\mvnw.cmd -f services\product-service\pom.xml test
.\monolith\mvnw.cmd -f services\order-service\pom.xml test
```

结果：用户服务 2 个测试通过，耗时 11.185 秒，结束于 `2026-11-08T16:41:14+08:00`。商品服务 2 个测试通过，耗时 10.930 秒，结束于 `2026-11-08T16:41:27+08:00`。订单服务 3 个测试通过，覆盖远程用户和商品都成功、用户不存在不写订单、商品服务不可用不写订单，耗时 11.592 秒，结束于 `2026-11-08T16:41:40+08:00`。三次都是 `BUILD SUCCESS`，测试使用 H2。

对应截图：[screenshots/d9f3c721fd0c172252daeb7491f52056.png](screenshots/d9f3c721fd0c172252daeb7491f52056.png)

## 3. 本地编排

实现思路：根目录 `compose.yaml` 启动 MySQL 8.4.11 和三个服务。初始化脚本 `scripts/mysql-init.sh` 创建三个数据库和三个账号。数据放在命名卷 `services-mysql-data`。订单服务通过 Compose 服务名访问 `http://user-service:8081` 和 `http://product-service:8082`。MySQL 健康后服务才启动。

运行命令：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

结果：`microserver-mysql-1` 为 `Up (healthy)`，三个服务都是 `Up`。端口映射为 `8081`、`8082`、`8083`。镜像名是 `user-service:week06`、`product-service:week06`、`order-service:week06` 和 `mysql:8.4.11`。

对应截图：[screenshots/6f7359cc31e361af748686a8ef7a4ff3.png](screenshots/6f7359cc31e361af748686a8ef7a4ff3.png)

## 4. 验证

实现思路：`scripts/smoke-test.sh` 从仓库根目录调用三个健康接口，再走完一次下单。随后用一个不存在的商品 ID 验证失败，并在停止 product-service 后再次下单，确认订单数量不变。

运行命令：

```powershell
bash scripts/smoke-test.sh
```

健康检查：

```text
GET http://localhost:8081/actuator/health  200  {"status":"UP"}
GET http://localhost:8082/actuator/health  200  {"status":"UP"}
GET http://localhost:8083/actuator/health  200  {"status":"UP"}
```

跨服务下单：

```text
POST http://localhost:8081/api/users
{"name":"陈鑫涛"}
200  {"id":1,"name":"陈鑫涛"}

POST http://localhost:8082/api/products
{"name":"无线鼠标","description":"静音办公鼠标","price":89.00,"stock":20}
200  {"id":1,"name":"无线鼠标","price":89.00,"stock":20}

POST http://localhost:8083/api/orders
{"userId":1,"productId":1,"quantity":1}
200  {"id":1,"userId":1,"buyerName":"陈鑫涛","productId":1,"productName":"无线鼠标","quantity":1,"amount":89.00,"status":"PENDING_PAYMENT"}
```

数据变化：订单 1 写入 `shopping_order`。无线鼠标库存从 20 变为 19。`GET /api/orders/1` 读到同一条订单。

依赖资源不存在：

```text
POST http://localhost:8083/api/orders
{"userId":1,"productId":99999,"quantity":1}
404  {"message":"商品不存在"}
```

订单数量仍是 1。

依赖服务不可用：停止 product-service 后再提交同一件商品的订单。

```text
POST http://localhost:8083/api/orders
503  {"message":"商品服务不可用"}
```

订单数量仍是 1。脚本随后重新启动商品服务，健康检查恢复为 200。最终输出 `SMOKE TEST PASSED`。

远端成功、本地失败的窗口出现在扣库存和写订单之间。product-service 的库存事务已经提交，order-service 才写自己的订单。如果这时订单库写入失败，库存已经减少，但 `orders` 里没有对应记录。两个库各自提交，没有统一回滚。本周只验证顺序调用和失败时不写订单，不实现补偿。

对应截图：[screenshots/bbc96e1af43ad190fc2c11c7c2e358cc.png](screenshots/bbc96e1af43ad190fc2c11c7c2e358cc.png)

## 5. 作业系统填写稿

公开仓库：https://github.com/user10086799/microservices-practice-2412190214

下面四段可以直接复制，尚未提交。

### 题目 1：服务拆分设计

购物系统拆成 user-service、product-service 和 order-service。用户服务使用 8081 和 shopping_user.users，商品服务使用 8082 和 shopping_product.product，订单服务使用 8083 和 shopping_order.orders。订单只保存 user_id 和 product_id，通过 HTTP 校验买家并扣减库存，不建立跨库外键。

### 题目 2：代码拆分

三个服务都有独立 pom、配置、Controller、Service 和测试。订单服务用 RestTemplate 调用用户和商品服务，地址可由环境变量覆盖，连接超时 2 秒、读取超时 3 秒。./monolith/mvnw -f 各服务 pom.xml test 全部通过。用户不存在或商品服务不可用时不写订单。

建议上传：`d9f3c721fd0c172252daeb7491f52056.png`。

### 题目 3：本地编排

docker compose up -d --build 启动 mysql:8.4.11 和三个服务。每个服务使用独立数据库和账号，数据在命名卷 services-mysql-data。订单服务通过服务名 user-service、product-service 访问，不使用 localhost。docker compose ps 中数据库为 healthy，三个服务都是 Up。

建议上传：`6f7359cc31e361af748686a8ef7a4ff3.png`。

### 题目 4：验证

bash scripts/smoke-test.sh 对 8081、8082、8083 的健康接口都返回 200。创建买家和无线鼠标后下单，返回订单 id=1、金额 89.00、状态 PENDING_PAYMENT，库存变为 19。商品 99999 返回 404，停止商品服务后下单返回 503，两种失败都没有新增订单。

建议上传：`bbc96e1af43ad190fc2c11c7c2e358cc.png`。
