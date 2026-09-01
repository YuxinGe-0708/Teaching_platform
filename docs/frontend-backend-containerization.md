# 微服务容器化说明

当前系统只有一条受支持的运行链路：Nginx gateway 作为统一入口，无数据库 web-bff 负责 Thymeleaf 页面、Session 和请求编排，业务数据与能力分别由 user-service、learning-service、assessment-service 提供。

```text
browser -> gateway -> web-bff -> user-service       -> user_db
                            -> learning-service   -> learning_service_db
                            -> assessment-service -> assessment_db
```

公共 REST 路由可以由 gateway 转发给领域服务；需要页面 Session 的 AI 与判题请求先进入 BFF，由 BFF 注入登录用户身份后再调用领域服务。BFF 的 Maven 依赖中没有 JDBC、MySQL 或 MyBatis，不能直接访问数据库。

## 启动

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml ps
```

打开 `http://localhost:3000/login`。默认账号见 README，密码均为 `123456`。

容器职责：

- `gateway`：Nginx 统一入口，宿主机默认端口 3000。
- `web-bff`：页面渲染、Session、跨服务页面数据编排，无数据库。
- 三个业务微服务：各自构建、运行、测试，只访问自己的 Schema。
- `microservices-mysql`：同一 MySQL 实例承载三个逻辑隔离的 Schema。

学习资源和作业附件由对应领域服务保存到挂载的 `uploads/`。AI 配置只注入 learning-service，Judge0 配置只注入 assessment-service。

## 验证

```powershell
mvn -B test package
mvn -B test package -f services/user-service/pom.xml
mvn -B test package -f services/learning-service/pom.xml
mvn -B test package -f services/assessment-service/pom.xml
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml config --quiet
./scripts/microservices-smoke.ps1
./scripts/microservices-business-regression.ps1
```

回归测试从 gateway 入口建立真实 BFF Session，同时检查三个服务 API，覆盖注册、登录、课程、班级、选退课、资源、进度、笔记、讨论、AI、作业、批改、考试、成绩和编程判题。判题测试不从浏览器伪造 `studentId`，而是验证 BFF 使用当前登录学生身份并在 assessment-service 中保存成绩。

## 停止

```powershell
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml down
```

仅在明确需要清空三个 Schema 并重新导入测试数据时使用 `down -v`。

旧单体 Compose、单体数据库脚本和直接连接单体库的 E2E 脚本已经移除，CI/CD 不再引用这些入口。
