# 第 04 周

在第 03 周的 `monolith/` 上实现买家浏览商品并提交订单，包括模型、Service、REST API 和数据库持久化。包名仍是 `com.zjgsu.cxt`。

## 1. 业务建模

实现思路：先确定一条能够独立走完的流程，只保留商品和订单两个实体。订单通过 `product_id` 指向商品。库存是否足够放在 `OrderService` 里判断，Controller 不写这条规则。

核心流程：查询商品，按数量创建待支付订单，扣减库存并计算金额。

业务规则：下单数量必须大于 0，且不能超过当前库存。不满足时拒绝创建，库存不变。

本周没有单独的运行命令，设计结论写在 [docs/project-proposal.md](../../project-proposal.md)。

## 2. 单体应用实现

实现思路：`model` 放商品和订单，`service` 处理查询与下单，`controller` 只接收请求并返回结果，`repository` 负责数据库访问。

接口：

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`

运行命令：

```powershell
cd monolith
.\mvnw.cmd spring-boot:run
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"buyerName\":\"陈鑫涛\",\"productId\":1,\"quantity\":1}"
```

结果：购买 1 件机械键盘时，接口返回状态 `PENDING_PAYMENT`、金额 `299.00`。购买 11 件时返回 400，消息为「下单数量不能超过商品库存」。这两次响应由 `OrderApiTests` 断言，测试已通过。

对应截图：[screenshots/b6ca19574991b128e6cc62a0342ca528.png](screenshots/b6ca19574991b128e6cc62a0342ca528.png)

## 3. 数据库持久化

实现思路：主配置连接 MySQL 数据库 `shopping`，由 Hibernate 更新 `product` 和 `orders`。字段、主键和外键写在 [docs/database.md](../../database.md)。测试不依赖本机 MySQL，使用 H2，并在 `src/test/resources/data.sql` 装入两件商品。

运行命令：

```sql
CREATE DATABASE IF NOT EXISTS shopping DEFAULT CHARACTER SET utf8mb4;
```

`application.yml` 中的账号是 `root`，示例密码是 `123456`。本机 root 密码不是这个值，启动前需要改成实际密码。测试数据脚本可以按商品名称重复执行。

对应截图：[screenshots/556f59f02a6c5e063c19ff068d6e30cf.png](screenshots/556f59f02a6c5e063c19ff068d6e30cf.png)

## 4. 测试与验证

实现思路：Repository 测试保存并读回一件商品；Service 测试分别覆盖下单成功和超过库存；API 测试覆盖商品查询成功和超库存的 400 响应。

运行命令和结果：

```text
cd monolith
.\mvnw.cmd test
```

```text
OrderApiTests            Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
ProductRepositoryTests   Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
OrderServiceTests        Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
ShoppingApplicationTests Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 11.228 s
Finished at: 2026-11-08T14:27:36+08:00
```

对应截图：[screenshots/d8d1f9d047f3470301a01a644d41f147.png](screenshots/d8d1f9d047f3470301a01a644d41f147.png)

