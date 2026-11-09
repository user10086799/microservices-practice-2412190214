# 第 07 周

在第 06 周的 user-service、product-service、order-service 上接入 Nacos。服务名不变，分组是 `CXT_SHOPPING`，命名空间使用默认 public。订单服务每次下单都用 `DiscoveryClient.getInstances` 取当前实例，不再保存固定的服务地址。

截图根据本次实际命令和接口结果整理，没有包含 Token 或密码。

## 1. 部署 Nacos 并注册服务

实现思路：`compose.yaml` 增加 `nacos/nacos-server:v3.2.4`。控制台映射 `127.0.0.1:18080 -> 8080`，服务端 HTTP 是 `8848`，SDK 通信是 `9848`。Token、Server identity 和鉴权写在环境变量里，数据放在命名卷 `nacos-data`。先单独启动 Nacos，调用初始化接口设置管理员密码，再启动三个业务服务。依赖版本由 `spring-cloud-alibaba-dependencies:2025.1.0.0` 管理，没有单独指定 Nacos Client 版本，也没有引入 Config Starter。

运行命令：

```powershell
docker compose up -d nacos
curl.exe -sS -o NUL -w "init:%{http_code}" -X POST http://127.0.0.1:8848/nacos/v3/auth/user/admin --data-urlencode "password=<.env 中的 NACOS_PASSWORD>"
docker compose up -d --build
```

结果：初始化接口返回 `200`。控制台 `http://127.0.0.1:18080/` 返回 `302`。三个服务都注册成功，分组都是 `CXT_SHOPPING`：

```text
user-service     172.18.0.4:8081  healthy=true
product-service  172.18.0.5:8082  healthy=true
order-service    172.18.0.6:8083  healthy=true
```

本机 `8082` 被其他程序占用，商品服务的宿主机端口改为 `18082`。Nacos 记录的是容器内端口 `8082`。

对应截图：[screenshots/5bba2f39e1c66a69a221b2965a83b096.png](screenshots/5bba2f39e1c66a69a221b2965a83b096.png)

## 2. 配置隔离并完成服务发现调用

实现思路：三个服务用同一组环境变量读取 `NACOS_SERVER_ADDR=nacos:8848`、账号和分组 `CXT_SHOPPING`。下单流程是：发现 `user-service`，请求 `GET /api/users/{id}`；再发现 `product-service`，请求 `POST /api/products/{id}/stock-deductions`；两次都成功后才写本地订单。实例列表为空时返回 503。商品不存在仍返回 404，不写订单。`USER_SERVICE_URL` 和 `PRODUCT_SERVICE_URL` 已删除。连接超时 2 秒，读取超时 3 秒。

运行命令和结果：

```text
product-service 当前实例 172.18.0.5:8082 healthy=true

POST http://127.0.0.1:8081/api/users
{"name":"陈鑫涛"}
200 {"id":2,"name":"陈鑫涛"}

POST http://127.0.0.1:18082/api/products
{"name":"无线鼠标","description":"静音办公鼠标","price":89.00,"stock":20}
200 {"id":2,"name":"无线鼠标","price":89.00,"stock":20}

POST http://127.0.0.1:8083/api/orders
{"userId":2,"productId":2,"quantity":1}
200 {"id":2,"userId":2,"buyerName":"陈鑫涛","productId":2,"productName":"无线鼠标","quantity":1,"amount":89.00,"status":"PENDING_PAYMENT"}
```

订单服务没有访问宿主机 `18082`，而是访问 Nacos 返回的 `172.18.0.5:8082`。无线鼠标库存从 20 变为 19。

空实例测试在 `OrderPlacementServiceTests` 中模拟 `DiscoveryClient` 返回空列表，断言返回「用户服务不可用」且订单数不变。该测试不连接真实 Nacos。

对应截图：[screenshots/598d4444a2b2152a343baeb450a17061.png](screenshots/598d4444a2b2152a343baeb450a17061.png)

## 3. 验证健康状态、下线与恢复

实现思路：三个服务都保留 `/actuator/health`。全部可用时先完成上一节的下单。然后用一个不存在的商品检查 404，再停止 product-service，用另一件已存在的商品发起新请求。确认失败没有新增订单、库存也没变。恢复后再买这件商品。

运行命令和结果：

```text
GET http://127.0.0.1:8081/actuator/health  200  UP
GET http://127.0.0.1:18082/actuator/health 200  UP
GET http://127.0.0.1:8083/actuator/health  200  UP

POST http://127.0.0.1:8083/api/orders
{"userId":2,"productId":99999,"quantity":1}
404 {"message":"商品不存在"}

docker compose stop product-service
product-service 实例列表变为 []
POST http://127.0.0.1:8083/api/orders
{"userId":2,"productId":3,"quantity":1}
503 {"message":"商品服务不可用"}
```

此时订单数仍是 2。机械键盘库存仍是 10。

```text
docker compose start product-service
实例恢复为 172.18.0.5:8082 healthy=true
POST 同一请求
200 {"id":3,"userId":2,"buyerName":"陈鑫涛","productId":3,"productName":"机械键盘","quantity":1,"amount":299.00,"status":"PENDING_PAYMENT"}
```

机械键盘库存变为 9，订单数变为 3。

SDK 能连上 Nacos，只说明注册中心可访问。实例列表里的 `healthy=true` 表示 Nacos 还认为这个实例在线。`/actuator/health` 返回 `UP` 表示应用自己的健康端点可用。停掉商品服务后，Actuator 已经连不上，Nacos 里的实例也会被摘掉；不能反过来认为 Actuator 返回 `DOWN` 就会自动摘除实例。服务发现也没有消除扣库存成功但订单写入失败的窗口，本周不实现补偿。

对应截图：[screenshots/dd37cab67559608f00a76bb34d96a165.png](screenshots/dd37cab67559608f00a76bb34d96a165.png)

## 4. 验证脚本

实现思路：`scripts/smoke-test.sh` 从仓库根目录读取端口，检查 `http://127.0.0.1:18080/`，再检查三个健康接口。任一检查不是预期状态就返回 1。脚本不打印密码或 Token。测试配置关闭 Nacos 自动注册，继续使用 H2；订单测试用模拟的 DiscoveryClient 和 RestTemplate。

运行命令：

```powershell
Set-Location services\user-service
..\..\monolith\mvnw.cmd test
bash scripts/smoke-test.sh
```

结果：

```text
user-service    Tests run: 2, Failures: 0  BUILD SUCCESS  2026-11-08T17:03:30+08:00
product-service Tests run: 2, Failures: 0  BUILD SUCCESS  Total time: 11.513 s  2026-11-08T17:03:44+08:00
order-service   Tests run: 4, Failures: 0  BUILD SUCCESS  Total time: 11.386 s  2026-11-08T17:04:26+08:00

bash scripts/smoke-test.sh
PASS GET http://127.0.0.1:18080/ -> 302
PASS user-service    200 UP
PASS product-service 200 UP
PASS order-service   200 UP
SMOKE TEST PASSED
```

脚本退出码是 0。检查项不符合预期时会输出 `SMOKE TEST FAILED` 并返回 1。

对应截图：[screenshots/902828a645d1d2b859f21a871aa8ab95.png](screenshots/902828a645d1d2b859f21a871aa8ab95.png)

## 5. 作业系统填写稿

公开仓库：https://github.com/user10086799/microservices-practice-2412190214

下面四段可以直接复制，尚未提交。

### 题目 1：部署 Nacos 并注册服务

先执行 docker compose up -d nacos，初始化管理员密码返回 200。控制台是 http://127.0.0.1:18080 ，服务端 8848，SDK 9848。再启动三个业务服务。Nacos 分组 CXT_SHOPPING 中能看到 user-service 172.18.0.4:8081、product-service 172.18.0.5:8082、order-service 172.18.0.6:8083。

建议上传：`5bba2f39e1c66a69a221b2965a83b096.png`。

### 题目 2：配置隔离并完成服务发现调用

下单由 order-service 完成。它每次用 DiscoveryClient 查询分组 CXT_SHOPPING 中的 user-service 和 product-service，本次商品实例是 172.18.0.5:8082。购买 1 件无线鼠标返回订单 id=2、金额 89.00、状态 PENDING_PAYMENT，库存从 20 变为 19。实例列表为空时返回 503，不写订单。

建议上传：`598d4444a2b2152a343baeb450a17061.png`。

### 题目 3：验证健康状态、下线与恢复

三个 Actuator 健康接口都返回 UP。停止 product-service 后实例列表为空，新的下单请求返回 503，订单数和机械键盘库存都没有变化。恢复后同一请求返回 200，订单 id=3，金额 299.00，库存变为 9。Actuator UP 表示应用健康，Nacos healthy 表示实例仍被注册中心保留，二者不会自动同步。

建议上传：`dd37cab67559608f00a76bb34d96a165.png`。

### 题目 4：验证脚本

在仓库根目录执行 bash scripts/smoke-test.sh。脚本检查 http://127.0.0.1:18080/ 返回 302，以及 8081、18082、8083 的健康接口都返回 200。输出 SMOKE TEST PASSED，退出码为 0。任一检查失败时脚本返回 1。三个服务的 mvn test 也全部通过。

建议上传：`902828a645d1d2b859f21a871aa8ab95.png`。
