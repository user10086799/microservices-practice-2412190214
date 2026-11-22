# 微服务拆分设计

单体购物系统保留在 `monolith/`。第 06 周按买家、商品、订单三条边界拆到 `services/`，每个服务只读写自己的库。

## 单体到微服务

| 单体内容 | 微服务 | 本服务拥有的数据 |
| --- | --- | --- |
| 订单里的买家姓名 | user-service | `users` |
| `Product`、库存和价格 | product-service | `product` |
| `Order`、金额和待支付状态 | order-service | `orders` |

订单不再使用 JPA 关联去读商品表。`orders` 只保存 `user_id` 和 `product_id`，姓名、商品名、单价由当次 HTTP 响应写入订单自己的列。库和库之间没有外键，服务之间也不调用对方的 Repository。

## 服务、端口和数据库

第 06 周和第 07 周把三个库放在同一个 MySQL 实例里。第 08 周改成三个 MySQL 8.4.11 实例，一个服务对应一个实例。商品库停掉之后，用户库和订单库仍可查询。

| 服务 | 容器端口 | 数据库实例 | 数据库 | 账号 | 数据表 | 职责 |
| --- | --- | --- | --- | --- | --- | --- |
| user-service | 8081 | mysql-user | shopping_user | user_app | users | 注册买家，按 ID 查询买家 |
| product-service | 8082 | mysql-product | shopping_product | product_app | product | 维护商品、价格和库存，提供扣减库存接口 |
| order-service | 8083 | mysql-order | shopping_order | order_app | orders | 校验买家和商品后创建待支付订单 |

商品服务有两个容器 `product-service-a` 和 `product-service-b`，共用 `mysql-product`，服务名都是 `product-service`。

## 调用关系

下单由 order-service 串起来。订单服务用 OpenFeign 按服务名调用，实例由 Nacos 分组 `CXT_SHOPPING` 和 Spring Cloud LoadBalancer 选择：

1. 调用方 order-service，被调用方 user-service，`GET /api/users/{id}`。用户不存在时返回 404，不写订单，也不扣库存。用户服务不可用时返回 503。
2. 调用方 order-service，被调用方 product-service，`POST /api/products/{id}/stock-deductions`。商品不存在返回 404，数量超过库存返回 400。商品服务不可用时返回 503。这些情况下 order-service 不写本地订单。
3. 两次调用都成功后，order-service 才把订单写入 `shopping_order`。金额等于商品单价乘以数量，状态为 `PENDING_PAYMENT`。

负载均衡探测走 `GET /api/orders/upstream/products/{id}`。订单服务再调用 product-service 的 `GET /api/products/{id}`，并读取响应头 `X-Instance-Id`。

user-service 和 product-service 不互相调用。三个服务使用默认命名空间，分组都是 `CXT_SHOPPING`。注册中心地址用 `NACOS_SERVER_ADDR` 配置，容器里是 `nacos:8848`。OpenFeign 的连接超时是 2 秒，读取超时是 3 秒。

库存扣减在 product-service 提交成功之后，订单才开始写自己的库。扣库存的响应如果超时或中断，调用方只能知道这次下单没有记成成功，不能据此判断库存是否已经提交。本周把这个窗口说明清楚，不实现补偿。

```mermaid
flowchart LR
  client[客户端]
  userSvc[user-service :8081]
  productA[product-service-a :8082]
  productB[product-service-b :8082]
  orderSvc[order-service :8083]
  userDb[(mysql-user shopping_user)]
  productDb[(mysql-product shopping_product)]
  orderDb[(mysql-order shopping_order)]

  client --> userSvc
  client --> productA
  client --> productB
  client --> orderSvc
  orderSvc -->|OpenFeign user-service GET /api/users/id| userSvc
  orderSvc -->|OpenFeign product-service POST /api/products/id/stock-deductions| productA
  orderSvc -->|OpenFeign product-service POST /api/products/id/stock-deductions| productB
  userSvc --> userDb
  productA --> productDb
  productB --> productDb
  orderSvc --> orderDb
```
