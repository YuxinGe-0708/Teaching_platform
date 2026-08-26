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

## 用 Docker 启动数据库

只需要安装 Docker，数据库容器会自动建库、建表并导入测试数据：

```powershell
docker compose up -d mysql
```

- 镜像：`mysql:8.0.40`（官方镜像，无需自写 Dockerfile）
- 首次启动自动执行 `db/init/01_schema.sql` 和 `db/init/02_test_data.sql`
- 数据持久化在 Docker 卷 `mysql-data` 中
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
