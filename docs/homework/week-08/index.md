# 第 08 周

在第 07 周的 Nacos 注册之上，订单服务改为用 OpenFeign 按服务名调用用户服务和商品服务。商品服务启动两个实例，用来验证负载均衡。三个业务库分别放到 `mysql-user`、`mysql-product`、`mysql-order`，商品库停止后用户和已有订单仍可查询。

调用链路图在 [docs/service-communication.md](../../service-communication.md)。截图来自本机 Windows PowerShell 窗口。

本次本机 `8082`、`8083` 被其他程序占用，`.env` 把宿主机端口设为用户 `8081`、商品实例 A `18082`、商品实例 B `18083`、订单 `18084`。容器内端口仍是 `8081`、`8082`、`8083`。

## 1. 选择一个跨服务业务流程

实现思路：创建订单必须同时知道买家和商品。调用方是 order-service，接口是 `POST /api/orders`。它先调用 user-service 的 `GET /api/users/{id}`，再调用 product-service 的 `POST /api/products/{id}/stock-deductions`。两次都成功后才把订单写入 `mysql-order` 上的 `shopping_order`。用户库、商品库、订单库各在一个 MySQL 实例上。

运行命令：

```powershell
docker compose up -d --build
bash scripts/e2e-test.sh
```

结果：2026-11-22 22:55 的运行创建了买家 `id=4`、商品 `id=4`，下单返回 200。订单 `id=2`，`buyerName` 为 `week8-buyer-225527`，`productName` 为 `week8-goods-225527`，金额 `12.50`，状态 `PENDING_PAYMENT`。随后停止 `mysql-product`，`GET /api/users/4` 和 `GET /api/orders` 仍返回 200，新的下单返回 503。

对应截图：[screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png](screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png)

三个数据库实例和两个商品实例的容器状态：[screenshots/160d8320150d7cfe1eef09739fb429d0.png](screenshots/160d8320150d7cfe1eef09739fb429d0.png)

## 2. 使用 OpenFeign

实现思路：`UserClient` 使用 `@FeignClient(name = "user-service")`，`ProductClient` 使用 `@FeignClient(name = "product-service")`。订单服务依赖 `spring-cloud-starter-openfeign` 和 `spring-cloud-starter-loadbalancer`，版本由 `spring-cloud-dependencies:2025.1.0` 管理。`RemoteCatalogClient` 记录「OpenFeign 调用服务名」以及服务名和路径。404 返回清晰的业务错误；其他 4xx 沿用对方的状态码和 `message`；超时、连接失败和 5xx 返回 503，正文是「用户服务不可用」或「商品服务不可用」，并且不写订单。连接超时 2000 毫秒，读取超时 3000 毫秒。

运行命令：

```powershell
bash scripts/e2e-test.sh
docker compose logs --no-color --tail 80 order-service
```

结果：下单返回 200。订单服务日志中的调用是服务名，例如：

```text
OpenFeign 调用服务名 user-service GET /api/users/4
OpenFeign 调用服务名 product-service POST /api/products/4/stock-deductions
```

停止 `mysql-product` 后，同一买家再次下单返回 `503 {"message":"商品服务不可用"}`，订单数保持 2。

对应截图：[screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png](screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png)

服务层测试里同样打印了按服务名调用的日志：[screenshots/31da31a84353c797d6a1f4dc3c0ac29a.png](screenshots/31da31a84353c797d6a1f4dc3c0ac29a.png)

## 3. 负载均衡验证

实现思路：`compose.yaml` 里 `product-service-a` 和 `product-service-b` 使用同一镜像 `product-service:week08`，`spring.application.name` 都是 `product-service`。`INSTANCE_ID` 分别是 `product-a` 和 `product-b`。宿主机端口是 `18082` 和 `18083`，容器内都是 `8082`，两个实例都连接 `mysql-product`。每个实例在 `/api/` 请求上写响应头 `X-Instance-Id`，并打印 `instance=` 日志。订单服务通过 `GET /api/orders/upstream/products/{id}` 用 OpenFeign 访问 `GET /api/products/{id}`，把实际命中的实例标识返回给调用方。负载均衡缓存关闭。

运行命令：

```powershell
bash scripts/e2e-test.sh
```

结果：连续 10 次探测交替命中两个实例：

```text
PROBE 1 -> 200 instance=product-b
PROBE 2 -> 200 instance=product-a
PROBE 3 -> 200 instance=product-b
PROBE 4 -> 200 instance=product-a
PROBE 5 -> 200 instance=product-b
PROBE 6 -> 200 instance=product-a
PROBE 7 -> 200 instance=product-b
PROBE 8 -> 200 instance=product-a
PROBE 9 -> 200 instance=product-b
PROBE 10 -> 200 instance=product-a
PASS load-balance saw product-a and product-b
```

商品服务日志也分别出现 `instance=product-a GET /api/products/4` 和 `instance=product-b GET /api/products/4`。

对应截图：[screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png](screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png)

## 4. 测试

实现思路：`OrderPlacementServiceTests` 用 `@MockitoBean` 替换 `UserClient` 和 `ProductClient`，覆盖按服务名调用、用户不存在时不写订单、商品服务超时时不写订单，以及从响应头读取实例标识。端到端脚本是仓库根目录的 `scripts/e2e-test.sh`。脚本在商品库停止后确认用户和订单仍可查询，新下单返回 503，并且订单数不变。

运行命令：

```powershell
.\monolith\mvnw.cmd -f services\order-service\pom.xml test
bash scripts/e2e-test.sh
```

结果：测试在 `2026-11-22T22:57:26+08:00` 结束，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，总时间 `11.129 s`。端到端脚本输出 `E2E TEST PASSED`，退出码 0。

停止 `mysql-product` 后的下单被当成失败：HTTP 503，订单数仍是 2。扣库存是写操作，调用方收到失败时不能判断库存是否已经提交。这次运行结束后，商品 4 的库存是 18，成功订单只有 1 笔，创建时库存是 20。商品服务日志里，失败的那一次请求已经进入 `POST /api/products/4/stock-deductions`。库存写入的结果是未知的，这次实际已经提交。订单服务没有把这次调用记成下单成功，也没有做补偿。

对应截图：

- 服务层测试：[screenshots/31da31a84353c797d6a1f4dc3c0ac29a.png](screenshots/31da31a84353c797d6a1f4dc3c0ac29a.png)
- 端到端与依赖不可用：[screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png](screenshots/d42de9dd38aa92d42782ff4e69ee2dad.png)
