# 数据库设计

本周使用 MySQL 数据库 `shopping`。应用通过 Spring Data JPA 维护表结构，`spring.jpa.hibernate.ddl-auto=update`。

## 表关系

一个商品对应多笔订单，一笔订单只关联一个商品。`orders.product_id` 引用 `product.id`。

```text
product 1 ----< orders
```

## product

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| id | BIGINT | 主键，自增 |
| name | VARCHAR(100) | 非空 |
| description | VARCHAR(500) | 可空 |
| price | DECIMAL(10,2) | 非空 |
| stock | INT | 非空 |

## orders

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| id | BIGINT | 主键，自增 |
| buyer_name | VARCHAR(50) | 非空 |
| product_id | BIGINT | 非空，外键，引用 product.id |
| quantity | INT | 非空 |
| amount | DECIMAL(10,2) | 非空 |
| status | VARCHAR(32) | 非空，本周取值为 PENDING_PAYMENT |

## 可重复执行的示例数据

测试使用 H2，数据在 `monolith/src/test/resources/data.sql`，每次 `./mvnw test` 都会重新建表并装入。

本地 MySQL 建表由应用启动完成。表为空时可以执行下面的语句；按商品名称判断，重复执行不会插入第二份相同商品。

```sql
CREATE DATABASE IF NOT EXISTS shopping DEFAULT CHARACTER SET utf8mb4;

INSERT INTO product (name, description, price, stock)
SELECT '机械键盘', '87 键热插拔键盘', 299.00, 10
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '机械键盘');

INSERT INTO product (name, description, price, stock)
SELECT '无线鼠标', '静音办公鼠标', 89.00, 20
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '无线鼠标');
```
