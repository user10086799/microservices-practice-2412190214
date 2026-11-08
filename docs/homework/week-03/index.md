# 第 03 周

沿用第 02 周选题「分布式购物系统」。本周只让 Spring Boot 工程启动，并完成基础配置、一个问候接口和启动测试。

## 1. 本周计划

- 在 `docs/project-proposal.md` 记录项目名称、目标用户、优先业务场景和两个核心模型。
- 在 `monolith/` 创建 Java 25、Spring Boot 4.0.9、Maven 工程，包名为 `com.zjgsu.cxt`。
- 提供 `GET /api/hello`，并确认 `/actuator/health` 为 UP。
- 用 `@SpringBootTest` 的 `contextLoads` 验证应用上下文可以加载。
- 在根目录 `README.md` 写明环境、命令、访问地址和尚未实现的业务能力。

不实现业务模型、完整 REST API、Service、Repository 或数据库。

## 2. 启动命令与接口响应

配置文件 `monolith/src/main/resources/application.yml` 中的端口是 **8080**。

```powershell
cd monolith
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8088"
```

课程要求的命令是：

```bash
cd monolith
./mvnw spring-boot:run
```

Windows PowerShell 对应 `.\mvnw.cmd spring-boot:run`。已用 Git Bash 确认 `./mvnw -version` 可以执行，Maven 3.9.16，Java 25.0.4。

临时端口上的启动日志：

```text
Tomcat initialized with port 8088 (http)
Tomcat started on port 8088 (http) with context path '/'
Started ShoppingApplication in 1.345 seconds
```

`GET /api/hello`：

```json
{"message":"hello","project":"分布式购物系统"}
```

`GET /actuator/health`：

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

对应截图：

- [screenshots/c61f1bba1a947a64e4be852c14d442f6.png](screenshots/c61f1bba1a947a64e4be852c14d442f6.png)
- [screenshots/0ac3b57bf15a6d6c5d70ec2503d2f49d.png](screenshots/0ac3b57bf15a6d6c5d70ec2503d2f49d.png)
- [screenshots/86fc431fa4b9c950b6cb9b50e506700b.png](screenshots/86fc431fa4b9c950b6cb9b50e506700b.png)
- [screenshots/8d9b7c9b71c0aeafb1f3578976414bac.png](screenshots/8d9b7c9b71c0aeafb1f3578976414bac.png)

## 3. 测试结果

在 `monolith/` 执行：

```powershell
.\mvnw.cmd test
```

`com.zjgsu.cxt.ShoppingApplicationTests` 的 `contextLoads` 通过。

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:34 min
Finished at: 2026-11-08T13:21:12+08:00
```

对应截图：

- [screenshots/0dc5b924d241de53e5d3578bb1677018.png](screenshots/0dc5b924d241de53e5d3578bb1677018.png)
- [screenshots/d229786ba5835ce9dec7dd91a4f3dd4b.png](screenshots/d229786ba5835ce9dec7dd91a4f3dd4b.png)

## 4. 本周完成内容

- 选题仍为分布式购物系统。优先场景是买家浏览商品并提交订单，两个核心模型是商品和订单，只写在提案里。
- `monolith/` 可以启动，包含 `pom.xml`、Maven Wrapper、启动类、`application.yml`、`GET /api/hello` 和 Actuator。
- `./mvnw test` 对应的 Windows 命令已通过，应用上下文可以加载。
- 根目录 `README.md` 已写明 Java 25、Maven Wrapper、启动和测试命令、两个访问地址，以及尚未实现的业务能力。