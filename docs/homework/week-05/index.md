# 第 05 周

把第 04 周的 `monolith/` 和 MySQL 放进容器。镜像在容器里用 Maven 打出可运行 JAR，Compose 一条命令同时启动应用和 MySQL 8.4.11。包名仍是 `com.zjgsu.cxt`，业务接口没有改。

截图根据本次实际命令输出整理，命令、状态码和响应体与运行结果一致。

## 1. Dockerfile 与镜像构建

实现思路：`monolith/Dockerfile` 分两段。构建阶段使用 `maven:3.9.11-eclipse-temurin-25`，在镜像里执行 `mvn -B -DskipTests package`，不使用本机 Maven 或本机 `target/`。运行阶段使用 `eclipse-temurin:25-jre`，只复制 `distributed-shopping-0.0.1-SNAPSHOT.jar`。构建阶段访问 Maven Central 时连接中断，因此在镜像内写入阿里云公共仓库镜像后再打包。

`monolith/.dockerignore` 排除 `target/`、`.idea/`、`*.iml`、`.git/`、`.env` 和 `*.md`。

在仓库根目录执行：

```powershell
docker build -t distributed-shopping:week05 ./monolith
```

结果：Maven 构建 `BUILD SUCCESS`，耗时 21.917 秒，结束时间 `2026-11-08T08:18:38Z`。镜像名 `distributed-shopping:week05`。随后 `docker compose up -d --build` 复用该构建层，当前镜像 ID 为 `3dc6b4d0b39b`，大小 588MB。基础镜像为 `maven:3.9.11-eclipse-temurin-25`、`eclipse-temurin:25-jre` 和 `mysql:8.4.11`。

对应截图：[screenshots/7f63a5f43a915dfde1cc2c8459195299.png](screenshots/7f63a5f43a915dfde1cc2c8459195299.png)

## 2. Compose 编排与启动

实现思路：根目录 `compose.yaml` 不写 `version`。`mysql` 使用 `mysql:8.4.11`，数据放在命名卷 `mysql-data`。`app` 的构建上下文是 `./monolith`，与数据库同在网络 `shopping`，连接地址使用服务名 `mysql`。账号、密码、库名和宿主机端口来自环境变量，模板是 `.env.example`，真实 `.env` 已写入 `.gitignore`。MySQL 配有 `mysqladmin ping` 健康检查，应用 `depends_on` 条件为 `service_healthy`。

在仓库根目录执行：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

结果：`microserver-mysql-1` 状态为 `Up (healthy)`，`microserver-app-1` 状态为 `Up`，端口 `0.0.0.0:8080->8080/tcp`。应用日志中的 JDBC 地址是 `jdbc:mysql://mysql:3306/shopping`，数据库版本 `8.4.11`，约 4.1 秒后启动完成。

对应截图：[screenshots/6f10dee1ebf457d92450d78da9974593.png](screenshots/6f10dee1ebf457d92450d78da9974593.png)

## 3. 个人业务 API 验证

实现思路：沿用第 04 周的浏览商品和下单接口。商品表由 Hibernate 创建，启动后是空的，先在 MySQL 容器中按 [docs/database.md](../../database.md) 插入机械键盘和无线鼠标。然后通过映射端口 `8080` 调用健康检查、商品查询、创建订单和订单查询。

请求与结果：

```text
GET  http://localhost:8080/actuator/health
HTTP/1.1 200
{"groups":["liveness","readiness"],"status":"UP"}

GET  http://localhost:8080/api/products
HTTP/1.1 200
机械键盘 stock=9，无线鼠标 stock=19

POST http://localhost:8080/api/orders
{"buyerName":"陈鑫涛","productId":2,"quantity":1}
HTTP/1.1 200
{"id":2,"buyerName":"陈鑫涛","productId":2,"productName":"无线鼠标","quantity":1,"amount":89.00,"status":"PENDING_PAYMENT"}

GET  http://localhost:8080/api/orders/2
HTTP/1.1 200
同一条订单，金额 89.00，状态 PENDING_PAYMENT
```

无线鼠标原库存 20，下单 1 件后库存为 19。此前还创建过订单 1：购买 1 件机械键盘，金额 `299.00`，状态 `PENDING_PAYMENT`，键盘库存由 10 变为 9。

对应截图：[screenshots/83bf798631b0e7c4e86beaf332d7e23b.png](screenshots/83bf798631b0e7c4e86beaf332d7e23b.png)

## 4. 数据持久化验证

实现思路：订单 2 由业务接口创建。执行 `docker compose down`，不使用 `-v`。命名卷 `microserver_mysql-data` 仍然存在。再执行 `docker compose up -d`，等应用启动后查询同一 ID。

重建前：

```text
GET http://localhost:8080/api/orders/2
HTTP/1.1 200
{"id":2,"buyerName":"陈鑫涛","productId":2,"productName":"无线鼠标","quantity":1,"amount":89.00,"status":"PENDING_PAYMENT"}
```

`docker compose down` 删除了应用容器、数据库容器和网络，卷 `microserver_mysql-data` 仍在。`docker compose up -d` 后 MySQL 变为 healthy，应用重新启动。

重建后：

```text
GET http://localhost:8080/api/orders/2
HTTP/1.1 200
{"id":2,"buyerName":"陈鑫涛","productId":2,"productName":"无线鼠标","quantity":1,"amount":89.00,"status":"PENDING_PAYMENT"}

GET http://localhost:8080/api/products
HTTP/1.1 200
机械键盘库存 9，无线鼠标库存 19
```

订单 2 的 ID、买家、商品、数量、金额和状态与重建前相同。订单 1 也同样还在。命名卷把 MySQL 数据目录留在容器外面，所以只重建容器不会丢掉已经创建的订单和扣减后的库存。

对应截图：[screenshots/8dfcc00e330e1b57d18d1a02d575ea50.png](screenshots/8dfcc00e330e1b57d18d1a02d575ea50.png)

## 5. 作业系统填写稿

公开仓库：https://github.com/user10086799/microservices-practice-2412190214

下面四段可以直接复制，尚未提交。

### 题目 1：Dockerfile 与镜像构建

在仓库根目录执行 docker build -t distributed-shopping:week05 ./monolith。Dockerfile 先用 Maven 镜像打包 JAR，再用 JRE 镜像运行，构建不依赖本机 Maven。BUILD SUCCESS，镜像 distributed-shopping:week05 已生成，当前大小 588MB。

建议上传：`7f63a5f43a915dfde1cc2c8459195299.png`。

### 题目 2：Compose 编排与启动

compose.yaml 启动 app 和 mysql:8.4.11。应用通过服务名 mysql 连接数据库，密码和库名来自 .env，真实 .env 不提交。MySQL 健康后应用才启动。docker compose up -d --build 后两个服务都在运行，应用映射到本机 8080。

建议上传：`6f10dee1ebf457d92450d78da9974593.png`。

### 题目 3：个人业务 API 验证

健康检查 GET /actuator/health 返回 200 和 UP。GET /api/products 返回机械键盘和无线鼠标。POST /api/orders 购买 1 件无线鼠标，返回订单 id=2、金额 89.00、状态 PENDING_PAYMENT。GET /api/orders/2 读到同一条订单。

建议上传：`83bf798631b0e7c4e86beaf332d7e23b.png`。

### 题目 4：数据持久化验证

先用创建订单接口写入订单 2，再执行 docker compose down，不使用 -v。命名卷 microserver_mysql-data 仍在。docker compose up -d 后再次 GET /api/orders/2，ID、买家、商品、数量、金额 89.00 和状态 PENDING_PAYMENT 都没有变化。

建议上传：`8dfcc00e330e1b57d18d1a02d575ea50.png`。
