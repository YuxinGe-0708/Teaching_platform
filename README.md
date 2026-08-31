# TeachingPlatform

Spring Boot + Thymeleaf + Vue 3 + MyBatis + MySQL 教学平台。

前端统一使用 `src/main/resources/templates/` 下的 Thymeleaf 模板页面；AI 助手页面通过 Vue 3 CDN 做局部交互。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Thymeleaf
- Vue 3 CDN
- MyBatis
- MySQL 8.x


## 微服务架构

系统已拆分为 3 个业务微服务（API 网关仍可按需接入），每个服务有独立数据库边界、构建文件和服务间接口。

| 服务 | 端口 | 数据库 | 职责 |
|---|---|---|---|
| `user-service` | 8082 | `user_db` | 注册、登录、角色权限、用户管理、通知 |
| `learning-service` | 8083 | `learning_service_db` | 课程、班级、选退课、资源、进度、笔记、讨论 |
| `assessment-service` | 8084 | `assessment_db` | 作业、考试、提交、批改、成绩、编程判题 |

### 数据表归属

| 服务 | 独占表 |
|---|---|
| user-service | `user`, `notification`, `operation_log` |
| learning-service | `course`, `course_class`, `course_enrollment`, `resource`, `resource_progress`, `study_note`, `discussion_post`, `discussion_reply` |
| assessment-service | `task`, `submission`, `exam_record` |

> 跨服务数据操作通过 `/internal` 接口完成，禁止跨服务直接联表查询。

### 微服务目录结构

```
services/
├── user-service/          # 用户与身份域
│   ├── pom.xml
│   ├── Dockerfile
│   ├── k8s/user-service/deployment.yaml
│   └── src/main/java/com/teach/user/...
├── learning-service/      # 学习与内容域
│   ├── pom.xml
│   ├── Dockerfile
│   ├── k8s/learning-service/deployment.yaml
│   └── src/main/java/com/teach/learning/...
└── assessment-service/    # 评测与成绩域
```

### 单体与微服务并行

当前单体应用（根目录）仍可完整运行，微服务以独立服务方式提供相同领域能力；两种模式互不覆盖数据库。

如需同时启动三个微服务和三个独立数据库 Schema，使用可选编排文件：

```powershell
docker compose -f docker-compose.microservices.yml up --build -d
```

如果本机已有单体 MySQL 占用 3307，可在 `.env` 中设置
`MICROSERVICES_MYSQL_PORT=3308`（容器内部服务仍连接 3306）；无法访问
Docker Hub 时同时设置 `MICROSERVICES_MYSQL_IMAGE`、`MAVEN_IMAGE` 和
`JAVA_IMAGE` 为可访问的镜像源。

服务地址分别为 `http://localhost:8082`、`http://localhost:8083`、`http://localhost:8084`。

如需通过一个网址同时访问原有页面和微服务 API，使用统一网关编排（启动前先停止
占用 3000/3307 的旧 compose）：

```powershell
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
```

统一入口为 `http://localhost:3000`。网关将 `/api/auth`、`/api/profile`、
`/api/notifications` 转发到 user-service，将课程、选课、资源、讨论和笔记 API
转发到 learning-service，将 `/api/v2/judge` 转发到 assessment-service；其余
页面路由转发到保留的单体后端，以确保现有 Thymeleaf 页面和跳转不发生变化。
若旧单体 MySQL 仍占用 3307，可设置 `LEGACY_MYSQL_PORT=3309`；微服务数据库
端口通过 `MICROSERVICES_MYSQL_PORT` 单独设置（容器间始终使用 3306）。

### 跨服务接口契约

learning-service 依赖 user-service 的接口：

| 接口 | 用途 |
|---|---|
| `GET /internal/users/{id}` | 获取用户姓名、角色 |
| `GET /internal/users/by-ids?ids=1,2,3` | 批量获取用户信息 |
| `POST /internal/notifications` | 发送通知（如新回复提醒） |

调用失败处理：重试 3 次（间隔 1s/2s/4s），全部失败返回降级数据（默认名称、空头像），不阻塞主流程。

### CI/CD 流水线

推送到 `dev_dockerfile` 分支自动触发：

```
ci ──→ publish ──→ deploy
```

- **ci**：编译、单元测试、容器化、集成测试、冒烟测试
- **publish**：构建版本镜像 → 推送华为云 SWR
- **deploy**：Kind 集群部署 → 健康检查 → 登录冒烟

详见 [docs/ci-cd.md](docs/ci-cd.md)。

## 用 Docker 启动数据库

只需要安装 Docker，数据库容器会自动建库、建表并导入测试数据。MySQL 健康后再执行迁移：

```powershell
docker compose up -d --wait mysql
./scripts/db-migrate.ps1
```

- 镜像：`mysql:8.0.40`（官方镜像，无需自写 Dockerfile）
- 首次启动自动执行 `db/init/01_schema.sql` 和 `db/init/02_test_data.sql`
- 数据持久化在 Docker 卷 `mysql-data` 中
- 启动后执行 `scripts/db-migrate.ps1`，按顺序应用 `db/migrations/*.sql`；已执行版本会记录并跳过
- 参数可在 `.env` 中覆盖，默认见 `.env.example`

默认连接信息：

- 数据库：`teaching_platform`
- 用户：`tp_dev`
- 密码：`123456`
- 端口：`3306`

常用命令：

```powershell
docker compose ps
docker compose logs -f mysql
docker compose exec mysql mysql -u tp_dev -p123456 teaching_platform -e "SHOW TABLES;"
docker compose down          # 停止但保留数据
docker compose down -v       # 停止并清空数据，重新初始化
```

数据库就绪后，再按下面的“共享数据库配置”或“运行”部分启动后端，后端连接 `localhost:3306` 即可。

## 共享数据库配置

团队共用一台 MySQL 时，只需要在主机电脑创建数据库和用户一次，然后所有人连接同一个地址。

PowerShell 不能使用 `< docs/schema.sql` 这种输入重定向。请用下面任意一种方式导入表结构：

```powershell
cmd /c "mysql -u root -p < docs\schema.sql"
```

或：

```powershell
Get-Content docs\schema.sql | mysql -u root -p
```

启动项目前设置环境变量：

```powershell
$env:DB_URL="jdbc:mysql://<你的MySQL主机IP>:3306/teaching_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
$env:DB_USERNAME="tp_dev"
$env:DB_PASSWORD="123456"
$env:AI_API_KEY="<你的 AI Key>"
mvn spring-boot:run
```

`serverTimezone=Asia/Shanghai` 是 MySQL/JDBC 的中国标准时区名，北京、上海都使用 UTC+8，所以北京电脑也这样写。

## 运行

推荐直接使用一键启动脚本：

```powershell
.\run-dev.ps1 -SeedTestData
```

脚本会提示输入 MySQL 密码，自动确保 `teaching_platform` 数据库存在，设置运行所需环境变量，并启动项目。`-SeedTestData` 会导入测试账号、课程、班级、资源、作业、成绩、讨论等数据；重复运行不会反复插入同名测试数据。

常用参数：

```powershell
.\run-dev.ps1 -DbUsername root -DbPassword "你的MySQL密码" -Port 8080 -SeedTestData
.\run-dev.ps1 -DbUsername root -DbPassword "你的MySQL密码" -SkipDbCreate
```

启用 Qwen/百炼 AI、AI 识图点读笔和 Judge0 云端判题：

```powershell
$env:QWEN_API_KEY="你的阿里云百炼APIKey"
.\run-dev.ps1 -DbUsername root -DbPassword "你的MySQL密码" -SeedTestData -UseQwen
```

脚本会自动设置：

- `AI_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- `AI_MODEL=qwen-plus`
- `AI_VISION_MODEL=qwen3-vl-plus`
- `JUDGE0_API_URL=https://ce.judge0.com`

测试账号密码统一为 `123456`：

- 教师：`teacher_demo`、`teacher_algo`
- 学生：`student_001`、`student_002`、`student_003`、`student_004`、`student_005`、`student_006`
- 管理员：`admin`

也可以手动运行：

```powershell
mvn clean compile
mvn spring-boot:run
```

打开：

- `http://localhost:8080/login`
- `http://localhost:8080/register`
- `http://localhost:8080/home`
- 学生 AI：`http://localhost:8080/student/ai`
- 教师 AI：`http://localhost:8080/teacher/ai`

## 前后端容器化

在保留原 Thymeleaf 页面和业务接口的前提下，前后端可以分别运行在独立容器中：Nginx frontend 作为统一入口，Spring Boot backend 处理原有页面、接口和文件，MySQL 使用现有数据库容器。原来的数据库命令不变：

```powershell
docker compose up -d mysql
./scripts/db-migrate.ps1
```

完整启动应用容器：

```powershell
Copy-Item .env.example .env
notepad .env
docker compose -f docker-compose.yml -f docker-compose.app.yml up --build -d
docker compose -f docker-compose.yml -f docker-compose.app.yml ps
```

打开 `http://localhost:3000/login`。详细结构、版本镜像和回归检查见 [docs/frontend-backend-containerization.md](docs/frontend-backend-containerization.md)。

启动后可重复执行完整回归检查。业务脚本会创建唯一命名的临时数据，验证完成后自动清理：

```powershell
.\scripts\container-smoke.ps1
.\scripts\container-business-regression.ps1
.\scripts\container-ai-smoke.ps1
```

## 主要数据表

- `user` — 用户（注册、登录、个人信息）
- `course` — 课程（创建、编辑、删除）
- `course_class` — 班级
- `course_enrollment` — 选课记录，写入选课时记录 `operation_log`
- `task` — 任务（作业/考试/编程实训）
- `submission` — 提交与批改

## AI 配置

默认调用 DeepSeek 兼容接口：

```yaml
app:
  ai:
    api-url: ${AI_API_URL:https://api.deepseek.com/v1/chat/completions}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:deepseek-chat}
```

如需换其他兼容 OpenAI Chat Completions 格式的服务，设置 `AI_API_URL`、`AI_MODEL`、`AI_API_KEY` 即可。

如果页面提示 `Unauthorized - 401`，通常不是前端问题，而是下面三项之一：

- `AI_API_KEY` 无效、过期、复制时带了多余空格，或不是当前服务商的 Key。
- `AI_API_URL` 和 Key 不属于同一个服务商。
- `AI_MODEL` 不是这个服务商账号可用的模型。

环境变量里可以直接写裸 Key，例如 `sk-...`；如果已经写成 `Bearer sk-...`，后端也会兼容。
