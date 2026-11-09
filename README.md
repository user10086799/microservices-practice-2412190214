# microservices-practice-2412190214

微服务开发与实践-陈鑫涛-2412190214

本仓库用于课程“微服务开发与实践”作业内容的存放与大作业评分唯一标准

微服务实践课程项目：分布式购物系统

一、项目背景

随着电子商务的快速发展，传统单体架构的购物系统在面对高并发、大流量（如双十一大促）时，往往面临扩展困难、代码臃肿、部署效率低等问题。本项目旨在构建一个基于微服务架构的分布式购物系统，通过将业务解耦，提升系统的可扩展性、高可用性和开发效率。

二、目标用户
本系统主要面向以下三类用户角色：

1.普通消费者（买家）：浏览商品、搜索商品、加入购物车、下单支付、查看订单状态、管理收货地址。

2.商家（卖家）：发布商品、管理库存、查看销售订单、处理发货。

3.系统管理员：管理用户账号、管理商品分类、监控系统运行状态、处理异常订单。

三、选择原因（为什么适合在后续课程中逐步演进）

购物系统涵盖了电商的核心链路（浏览 -> 加购 -> 下单 -> 支付 -> 评价），业务逻辑清晰，便于划分微服务边界，用户、商品、订单、库存等实体关系明确，便于进行数据库设计与持久化练习。


三、 核心功能设计

为了实现后续的微服务拆分，我们将系统初步划分为以下几个核心业务模块：

1. 用户服务

   用户注册、登录、鉴权（JWT）。

   个人信息管理、收货地址管理。

2.商品服务
   
   商品分类管理、商品信息（名称、描述、图片、价格）的增删改查。
   
   商品搜索与分页展示。

3.库存服务
   
   库存数量的实时更新与扣减。
   
   库存预警（低库存提醒）。

4.购物车服务
   
   添加商品到购物车、修改数量、删除商品。
   
   购物车商品选中状态管理。

5.订单服务
   
   订单创建、订单状态流转（待支付、已支付、待发货、已发货、已完成、已取消）。
   
   订单超时自动取消。

6.支付服务
   
   对接第三方支付接口（模拟）。
   
   支付回调处理、支付状态同步。

7.评价服务
   
   用户对已购买商品进行评分和文字评价。
   
   商品评价列表展示。


四、核心业务流程（用户下单支付）

浏览商品->查询商品详情->加入购物车->添加商品->点击结算->获取购物车商品->提交订单->订单创建成功，待支付->发起支付->完成支付
->支付成功，等待发货

五、当前项目范围和暂不实现的内容

1.当前阶段范围：
  
   完成核心业务需求分析与数据库设计（ER图）。
   
   实现用户注册登录、商品展示、购物车管理、基础订单创建等基础功能。
   
   使用 MySQL 进行数据持久化。
   
   提供基础的前端页面。

2.暂不实现的内容：
   
   真实的第三方支付对接（仅模拟）。
  
   复杂的推荐算法与大数据分析。


六、第 03 周运行说明

环境要求：Java 25。Maven 使用 `monolith/` 目录中的 Maven Wrapper，不需要单独安装 Maven。Spring Boot 4.0.9。默认端口 8080。

在 `monolith/` 目录执行：

```bash
./mvnw test
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

验证地址：

- 问候接口：http://localhost:8080/api/hello
- 健康检查：http://localhost:8080/actuator/health

本周尚未实现用户、商品、库存、购物车、订单、支付、评价，以及数据库、Service 和 Repository。当前工程只包含应用启动、基础配置、`GET /api/hello` 和启动测试。上文中的业务模块仍是后续规划。

七、第 04 周运行说明

本周在 `monolith/` 中实现买家浏览商品并提交订单。接口前缀为 `/api`，数据使用 MySQL，测试使用 H2。

### 本地 MySQL

1. 安装并启动 MySQL。本机已检测到 MySQL Server 9.5。
2. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS shopping DEFAULT CHARACTER SET utf8mb4;
```

3. 打开 `monolith/src/main/resources/application.yml`，确认地址是 `jdbc:mysql://localhost:3306/shopping`，并把 `username`、`password` 改成你的 MySQL 账号。示例密码是 `123456`。
4. 在 `monolith/` 启动应用。Hibernate 会创建或更新 `product`、`orders` 两张表。
5. 如果商品表是空的，执行 [docs/database.md](docs/database.md) 里的插入语句。同一条语句可以重复执行。

### 启动、测试和调用

```bash
cd monolith
./mvnw test
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
cd monolith
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

`./mvnw test` 使用内存 H2 和 `src/test/resources/data.sql`，不连接 MySQL，可以重复通过。

默认端口仍是 8080。至少可以这样调用：

```bash
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"buyerName\":\"陈鑫涛\",\"productId\":1,\"quantity\":1}"
```

第一条返回商品列表。第二条在库存足够时创建待支付订单，返回订单金额和状态 `PENDING_PAYMENT`。数量超过库存时返回 400，消息为「下单数量不能超过商品库存」。

八、第 05 周容器运行说明

第 06 周起，根目录 `compose.yaml` 改为同时启动 user-service、product-service、order-service 和 MySQL。本节保留第 05 周单体容器的构建和调用记录。

本周用 Docker 构建 `monolith/`，并用 Compose 同时启动应用和 MySQL 8.4.11。应用通过服务名 `mysql` 连接数据库，数据保存在命名卷 `mysql-data`。

### 配置模板和启动

在仓库根目录执行：

```bash
cp .env.example .env
docker build -t distributed-shopping:week05 ./monolith
docker compose up -d --build
docker compose ps
docker compose down
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
docker build -t distributed-shopping:week05 ./monolith
docker compose up -d --build
docker compose ps
docker compose down
```

复制后编辑 `.env`，填写 `MYSQL_ROOT_PASSWORD`。`.env` 不提交。停止时不要加 `-v`，否则会删掉数据库卷。

### 健康检查和业务接口

应用映射到本机 8080，除非 `.env` 里修改了 `APP_PORT`。

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"buyerName\":\"陈鑫涛\",\"productId\":1,\"quantity\":1}"
curl http://localhost:8080/api/orders/1
```

健康接口返回 `UP`。商品表为空时，先按 [docs/database.md](docs/database.md) 插入商品，再创建订单。创建成功后，`GET /api/orders/1` 返回同一条订单。`docker compose down` 不要加 `-v`，命名卷会保留订单和库存；再次 `docker compose up -d` 后查询同一 ID，关键字段不变。

九、第 06 周微服务

第 07 周起，订单服务改为通过 Nacos 按服务名发现实例。本节保留第 06 周三个服务和独立数据库的记录。当前启动步骤在下一节。

服务边界和架构图见 [docs/microservices-design.md](docs/microservices-design.md)。`monolith/` 仍然是单体版本。三个服务在 `services/` 下，各自有 `pom.xml`、`src/` 和 Dockerfile。

### 测试

在仓库根目录执行。测试使用 H2，不连接 MySQL。

```powershell
.\monolith\mvnw.cmd -f services\user-service\pom.xml test
.\monolith\mvnw.cmd -f services\product-service\pom.xml test
.\monolith\mvnw.cmd -f services\order-service\pom.xml test
```

```bash
./monolith/mvnw -f services/user-service/pom.xml test
./monolith/mvnw -f services/product-service/pom.xml test
./monolith/mvnw -f services/order-service/pom.xml test
```

### 配置模板、启动和验证

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
bash scripts/smoke-test.sh
docker compose down
```

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
bash scripts/smoke-test.sh
docker compose down
```

编辑 `.env` 里的 `MYSQL_ROOT_PASSWORD`、`USER_DB_PASSWORD`、`PRODUCT_DB_PASSWORD` 和 `ORDER_DB_PASSWORD`。`.env` 不提交。停止时不要加 `-v`，否则会删除 `services-mysql-data`。

用户服务映射 `8081`，商品服务映射 `8082`，订单服务映射 `8083`。容器内订单服务通过 `http://user-service:8081` 和 `http://product-service:8082` 调用，不使用 localhost。

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl -X POST http://localhost:8081/api/users -H "Content-Type: application/json" -d "{\"name\":\"陈鑫涛\"}"
curl -X POST http://localhost:8082/api/products -H "Content-Type: application/json" -d "{\"name\":\"无线鼠标\",\"description\":\"静音办公鼠标\",\"price\":89.00,\"stock\":20}"
curl -X POST http://localhost:8083/api/orders -H "Content-Type: application/json" -d "{\"userId\":1,\"productId\":1,\"quantity\":1}"
```

`scripts/smoke-test.sh` 会检查 Nacos 控制台和三个服务的健康接口。任一检查失败时脚本返回非零。跨服务下单、服务下线和恢复的记录在第 07 周说明里。

十、第 07 周 Nacos

先单独启动 Nacos，初始化管理员密码，再启动数据库和三个业务服务。控制台只绑定本机，地址是 http://127.0.0.1:18080 。服务端 HTTP 端口是 8848，SDK 使用 9848。分组是 `CXT_SHOPPING`，命名空间使用默认 public。真实 Token、Server identity 和管理员密码只写在 `.env`。

```bash
cp .env.example .env
set -a
source .env
set +a
docker compose up -d nacos
curl -sS -o /dev/null -w "init:%{http_code}\n" -X POST http://127.0.0.1:8848/nacos/v3/auth/user/admin -d "password=${NACOS_PASSWORD}"
docker compose up -d --build
docker compose ps
bash scripts/smoke-test.sh
```

```powershell
Copy-Item .env.example .env
docker compose up -d nacos
$nacosPassword = (Get-Content .env | Where-Object { $_ -like 'NACOS_PASSWORD=*' } | Select-Object -First 1).Substring('NACOS_PASSWORD='.Length)
curl.exe -sS -o NUL -w "init:%{http_code}`n" -X POST http://127.0.0.1:8848/nacos/v3/auth/user/admin --data-urlencode "password=$nacosPassword"
docker compose up -d --build
docker compose ps
bash scripts/smoke-test.sh
```

初始化接口在管理员已经存在时会拒绝再次设置，可以继续执行后面的启动命令。停止时不要加 `-v`，否则会删除 `nacos-data` 和 `services-mysql-data`。

查看注册列表时登录控制台，服务名是 `user-service`、`product-service`、`order-service`，分组是 `CXT_SHOPPING`。

### 测试

进入各服务目录运行测试。测试使用 H2，并关闭 Nacos 自动注册，不连接真实 Nacos 或 MySQL。

```powershell
Set-Location services\user-service
..\..\monolith\mvnw.cmd test
Set-Location ..\..
Set-Location services\product-service
..\..\monolith\mvnw.cmd test
Set-Location ..\..
Set-Location services\order-service
..\..\monolith\mvnw.cmd test
Set-Location ..\..
```

```bash
(cd services/user-service && ../../monolith/mvnw test)
(cd services/product-service && ../../monolith/mvnw test)
(cd services/order-service && ../../monolith/mvnw test)
```

业务调用示例：

本次验证中本机 `8082` 已被其他程序占用，`.env` 的 `PRODUCT_PORT` 使用 `18082`。容器内商品服务仍监听 `8082`，Nacos 实例端口也是 `8082`。

```bash
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:18082/actuator/health
curl http://127.0.0.1:8083/actuator/health
curl -X POST http://127.0.0.1:8081/api/users -H "Content-Type: application/json" -d "{\"name\":\"陈鑫涛\"}"
curl -X POST http://127.0.0.1:18082/api/products -H "Content-Type: application/json" -d "{\"name\":\"无线鼠标\",\"description\":\"静音办公鼠标\",\"price\":89.00,\"stock\":20}"
curl -X POST http://127.0.0.1:8083/api/orders -H "Content-Type: application/json" -d "{\"userId\":1,\"productId\":1,\"quantity\":1}"
```

订单服务每次向 Nacos 查询 `user-service` 和 `product-service` 的实例，再用实例地址访问上面的路径。

---
*本仓库为《微服务架构与实践》课程持续演进的唯一项目仓库。*
