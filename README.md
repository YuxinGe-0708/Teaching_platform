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

### 微服务统一运行

根目录应用已经固定为无数据库的 Thymeleaf `web-bff`，只负责页面渲染、Session 和 HTTP 编排。所有业务数据由 `user-service`、`learning-service`、`assessment-service` 管理，BFF 不包含 JDBC/MyBatis，也不能直接访问业务表。

仅启动三个微服务和三个独立数据库 Schema：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-e2e-microservices.ps1
```

如果本机已有程序占用 3307，可在 `.env` 中设置
`MICROSERVICES_MYSQL_PORT=3308`（容器内部服务仍连接 3306）；无法访问
Docker Hub 时同时设置 `MICROSERVICES_MYSQL_IMAGE`、`MAVEN_IMAGE` 和
`JAVA_IMAGE` 为可访问的镜像源。

微服务 Dockerfile 使用 Maven 生成的 `target/*.jar` 作为运行时镜像输入；
上面的启动脚本会先完成三个服务的打包，再构建并启动容器。

服务地址分别为 `http://localhost:8082`、`http://localhost:8083`、`http://localhost:8084`。

通过一个网址访问完整页面和微服务 API：

```powershell
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
```

统一入口为 `http://localhost:3000`。网关将公开领域 API 转发到所属微服务；
需要登录 Session 的 `/api/v2/ai` 和 `/api/v2/judge` 先进入无数据库
`web-bff`，由 BFF 校验登录身份并通过 HTTP 委托给 learning-service 或
assessment-service。数据库容器只创建 `user_db`、`learning_service_db`、
`assessment_db`，不会创建或使用旧单体数据库。

验证完整链路：

```powershell
.\scripts\microservices-smoke.ps1
.\scripts\microservices-business-regression.ps1
```

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

### 端到端回归测试

微服务端到端测试放在 `tests/e2e/`，当前先提供 A 同学负责的基础框架：

- 统一 HTTP 客户端
- 服务健康检查
- 测试报告输出
- CI 接入

运行前先确认 Docker Desktop 已启动，并且命令窗口位于仓库根目录：

```powershell
Set-Location D:\teachplatform\Teaching_platform
docker info
```

然后启动微服务环境并执行 E2E：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-e2e-microservices.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-microservices.ps1
```

注意：

- 不要在 `tests/e2e/` 目录下执行 `.\scripts\e2e-microservices.ps1`，`scripts` 目录位于仓库根目录。
- 如果 PowerShell 提示禁止运行脚本，使用上面的 `powershell -ExecutionPolicy Bypass -File ...` 方式。
- 如果提示 `failed to connect to the docker API`，说明 Docker Desktop 还没有启动。
- 报告默认输出到 `ci-artifacts/e2e-report.json`。

### 性能对比实验结果

已完成单体版与微服务版的性能对比实验：3 个主要接口、每个版本各运行 3 次，测试参数为并发 `10`、预热 `10` 秒、正式采样 `20` 秒。

- **结果与差异分析**：`docs/12-性能对比实验报告.md`
- **实验交付与复现参数**：`tests/performance/RESULTS.md`
- **汇总对比数据**：`results/performance/perf-20260902-03/comparison.csv`
- **18 次测试明细**：`results/performance/perf-20260902-03/detailed-summary.csv`
- **请求级原始记录与资源采样**：`results/performance/perf-20260902-03/<版本>/<接口>/run-xx/benchmark.json`、`resources.csv`
- **测试脚本与性能夹具**：`tests/performance/`

本次实测结果显示当前单体版整体更快，不能宣称微服务版本有性能提升。编程判题场景不携带 `taskId`，仅比较判题计算链路；原因和限制见性能报告第 9 节。

## 用 Docker 启动微服务数据库

MySQL 容器自动创建三个服务独占的 Schema、表和演示数据：

```powershell
docker compose -f docker-compose.microservices.yml up -d microservices-mysql
```

- 镜像：`mysql:8.0.40`（官方镜像，无需自写 Dockerfile）
- 首次启动分别执行三个服务目录内的 `schema-*.sql` 和 `seed-*.sql`
- 数据持久化在 Docker 卷 `microservices-mysql-data` 中
- 三个服务共享一个 MySQL 服务器，但使用不同 Schema，不存在跨库外键或跨服务 JOIN
- 参数可在 `.env` 中覆盖，默认见 `.env.example`

默认连接信息：

- 数据库：`user_db`、`learning_service_db`、`assessment_db`
- 用户：`root`
- 密码：`root123456`（用环境变量覆盖）
- 主机端口：`3307`

常用命令：

```powershell
docker compose -f docker-compose.microservices.yml ps
docker compose -f docker-compose.microservices.yml logs -f microservices-mysql
docker compose -f docker-compose.microservices.yml exec microservices-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
```

业务服务分别连接自己拥有的 Schema；BFF 和 gateway 不连接数据库。

## 共享数据库服务器配置

团队共用一台 MySQL 时仍需为三个服务保留独立 Schema 和账号权限，不能让服务读取其他服务的表。

启动项目前设置环境变量：

```powershell
$env:MICROSERVICES_DB_ROOT_PASSWORD="你的MySQL密码"
$env:AI_API_KEY="你的 Qwen Key"
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
```

`serverTimezone=Asia/Shanghai` 是 MySQL/JDBC 的中国标准时区名，北京、上海都使用 UTC+8，所以北京电脑也这样写。

## 运行

推荐使用统一微服务编排：

```powershell
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
```

打开 `http://localhost:3000/login`。首次创建数据卷时会自动导入三个服务的演示数据。

启用 Qwen/百炼 AI、AI 识图点读笔和 Judge0 云端判题：

```powershell
$env:AI_API_KEY="你的阿里云百炼APIKey"
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
```

脚本会自动设置：

- `AI_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- `AI_MODEL=qwen-plus`
- `AI_VISION_MODEL=qwen3-vl-plus`
- `JUDGE0_API_URL=https://ce.judge0.com`

测试账号密码统一为 `123456`：

- 教师：`ms_teacher`、`ms_teacher2`
- 学生：`ms_student`、`ms_student2`、`ms_student3`
- 管理员：`ms_admin`

打开：

- `http://localhost:3000/login`
- `http://localhost:3000/register`
- `http://localhost:3000/home`
- 学生 AI：`http://localhost:3000/student/ai`
- 教师 AI：`http://localhost:3000/teacher/ai`

## 前后端与数据库容器化

Nginx gateway、无数据库 web-bff、三个业务微服务和 MySQL 分别运行在独立容器中。完整启动命令：

```powershell
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up --build -d
docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml ps
```

打开 `http://localhost:3000/login`。启动后可重复执行微服务健康检查和业务回归：

```powershell
.\scripts\microservices-smoke.ps1
.\scripts\microservices-business-regression.ps1
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
