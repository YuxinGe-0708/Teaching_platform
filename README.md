# TeachingPlatform

Spring Boot + Thymeleaf + Vue 3 + MyBatis + MySQL 教学平台。

前端入口统一使用 `src/main/resources/templates/` 下的 Thymeleaf 页面；Vue 3 只嵌入在模板页面中做局部交互，不再使用独立 SPA 或前端工程目录。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Thymeleaf
- Vue 3 CDN
- MyBatis
- MySQL 8.x

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
$env:DB_URL="jdbc:mysql://10.33.134.116:3306/teaching_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
$env:DB_USERNAME="tp_dev"
$env:DB_PASSWORD="123456"
$env:AI_API_KEY="<你的 AI Key>"
mvn spring-boot:run
```

`serverTimezone=Asia/Shanghai` 是 MySQL/JDBC 的中国标准时区名，北京、上海都使用 UTC+8，所以北京电脑也这样写。

## 运行

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

## 数据同步

以下运行时操作都会写入 MySQL：

- 注册、登录、个人信息修改：`user`
- 教师创建、编辑、删除课程：`course`
- 教师创建、删除班级：`course_class`
- 学生选课、退课：`course_enrollment`，并记录 `operation_log`
- 教师发布、删除作业：`task`
- 学生提交作业和编程评测：`submission`
- 教师批改：`submission.score/status/feedback`

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
