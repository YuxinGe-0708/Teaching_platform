# 前后端容器化说明

当前分支已经有数据库容器配置。新增的应用容器采用渐进式方案：不重写 Thymeleaf 页面、不修改 Controller 和业务 URL，前端容器使用 Nginx 作为统一入口，把所有原有请求转发给 Spring Boot 后端。

## 文件

```text
docker/backend/Dockerfile       Maven 编译 + JRE 运行后端
docker/frontend/Dockerfile      Nginx 前端镜像
docker/frontend/nginx.conf      原路径反向代理、大文件和长请求配置
docker-compose.app.yml          backend/frontend 编排
.dockerignore                   排除源码构建无关文件和本地密钥
scripts/container-smoke.ps1     容器启动后的路由和登录冒烟测试
```

原来的 `docker-compose.yml` 仍然只定义 MySQL，所以以下命令继续有效：

```powershell
docker compose up -d mysql
```

## 完整启动

先准备配置文件：

```powershell
Copy-Item .env.example .env
notepad .env
```

使用 Qwen 时，在不会提交到 Git 的 `.env` 中填写 `AI_API_KEY`；示例文件已默认使用百炼兼容接口、`qwen-plus` 和 `qwen3-vl-plus`。不要把真实 Key 写进 Dockerfile、Compose 或提交记录。

如果宿主机已有 MySQL 占用 `3306`，把 `.env` 中的 `MYSQL_PORT` 改为 `3307`。这只改变宿主机访问容器数据库的端口，backend 仍在 Docker 网络内使用 `mysql:3306`，无需修改 Java 数据库配置。

完整启动使用两个 Compose 文件：

```powershell
docker compose -f docker-compose.yml -f docker-compose.app.yml up --build -d
docker compose -f docker-compose.yml -f docker-compose.app.yml ps
```

访问：

```text
http://localhost:3000/login
```

默认测试账号来自现有 `db/init/02_test_data.sql`：`admin / 123456`。后端容器使用 `mysql:3306`，不是 `localhost:3306`；浏览器只访问 frontend，Session Cookie 和原有 URL 保持不变。

## 版本镜像

不要只使用 `latest`。发布或验收时设置版本标签：

```powershell
$env:IMAGE_TAG="v1.0.0"
docker compose -f docker-compose.yml -f docker-compose.app.yml build
docker image ls teaching-platform-backend teaching-platform-frontend
```

镜像名类似：

```text
teaching-platform-backend:v1.0.0
teaching-platform-frontend:v1.0.0
```

如果团队网络无法访问 Docker Hub，可在 `.env` 中把 `MYSQL_IMAGE`、`MAVEN_IMAGE`、`JAVA_IMAGE`、`NGINX_IMAGE` 改为团队可访问的镜像仓库地址；Dockerfile 无需修改。

## 回归检查

```powershell
mvn -B test
.\scripts\container-smoke.ps1
.\scripts\container-business-regression.ps1
.\scripts\container-ai-smoke.ps1
```

冒烟测试覆盖 frontend 健康检查、原 `/login`、`/register`、`/help` 路径、backend 直接路由以及管理员登录 Session。业务回归脚本覆盖管理员、教师、学生三种角色，选退课、课程/班级/任务 CRUD、资源上传下载和视频流、笔记、作业提交与批改、考试暂存与交卷、编程判题、讨论、通知、成绩导出、权限隔离及动态页面渲染。脚本只创建带 `TPREG_` 前缀的临时数据，并在 `finally` 中清理。AI 冒烟脚本在 `.env` 已配置 Qwen Key 时真实调用文本和视觉接口，但不会读取或输出 Key。

后端运行镜像使用 JDK，并包含 Python 3 和 GCC，因此 Judge0 不可用而触发本地回退时，Java、Python、C 三种判题仍可执行。容器内的软件包使用阿里云 Ubuntu 镜像源，降低国内网络首次构建失败的概率。

数据库使用 Compose 卷，上传文件保存在仓库 `uploads/` 目录；停止容器不会删除数据：

```powershell
docker compose -f docker-compose.yml -f docker-compose.app.yml down
```

只有明确需要重建空数据库时才使用 `down -v`。

## 为什么不会改变原功能

- Thymeleaf 模板仍由原 Spring Boot 应用渲染。
- 所有原路径、表单 action、REST API、Session 和权限代码不变。
- Nginx 只做转发，不解析或改写业务请求。
- 仓库 `uploads/` 绑定到 `/app/uploads`，已有文件和新上传文件在宿主机与容器模式下都可用，容器重建不会丢失。
- AI/Judge0 配置从 `.env` 注入后端，不写入镜像层。
- MySQL 初始化和迁移脚本仍由现有数据库容器负责。
