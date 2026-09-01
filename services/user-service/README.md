# user-service（用户与身份域）

> 在线教学平台 microservice 拆分 —— user-service。
> 管辖表：`user`、`notification`、`operation_log`（仅此三张，独立数据库 `user_db`）。

## 职责
- 注册、登录（BCrypt + 无状态 JWT）
- 个人资料
- 角色与用户管理（admin）
- 站内通知
- 操作日志（统一归口）

不包含：课程/选课/资源/作业/成绩/讨论等（分属 learning/assessment）。

## 关键约束
- **只读自己的三张表**，无任何跨服务 JOIN / 跨库外键。
- 其它服务经 `/internal/**` 取数；禁止直读本服务库。
- 认证：登录签发 JWT（claims: sub=userId, username, role），网关校验并把 `X-User-Id` / `X-User-Role` / `X-User-Name` 透传下来；本服务拦截器也认 `Authorization: Bearer <JWT>`。

## 公共接口（面向前端 /api）
| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 公开 | 登录，返回 token + 用户信息 |
| POST | /api/auth/register | 公开 | 注册 student/teacher |
| GET | /api/profile | 登录 | 当前用户资料 |
| PUT | /api/profile | 登录 | 改 name/email |
| GET | /api/notifications | 登录 | 通知列表 |
| GET | /api/notifications/unread-count | 登录 | 未读数 |
| POST | /api/notifications/read | 登录 | 标记已读 |
| POST | /api/notifications/read-all | 登录 | 全部已读 |
| GET | /api/users | admin | 用户列表 |
| PUT | /api/users/{id}/status | admin | 启用/禁用 |
| POST | /api/users/{id}/reset-password | admin | 重置密码 |
| DELETE | /api/users/{id} | admin | 删除用户 |
| GET | /api/logs | admin | 操作日志 |
| GET | /api/version | 公开 | 版本号 |
| GET | /actuator/health / /actuator/health/liveness / /actuator/health/readiness | 公开 | 健康/就绪探针 |

## 服务间接口（/internal，供 learning/assessment）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /internal/users/{id} | 按 ID 取用户 |
| GET | /internal/users/by-ids?ids=.. | 批量取用户 |
| GET | /internal/users?role=.. | 按角色/状态过滤 |
| POST | /internal/notifications | 创建通知 |
| POST | /internal/notifications/batch | 批量创建通知 |
| POST | /internal/notifications/{id}/read | 标记已读 |
| POST | /internal/notifications/read-all | 全部已读 |
| POST | /internal/operation-logs | 记录操作日志 |

## 本地运行
```bash
# 1. 建库（若已用 docker 建 user_db 可跳过）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS user_db ..."

# 2. 建表（先执行一次，或手动 run schema-user.sql）
# 3. 配置环境变量并启动
DB_URL='jdbc:mysql://localhost:3306/user_db?...' DB_USERNAME=root DB_PASSWORD=... \
  mvn -B spring-boot:run
```

默认端口 `8082`（`SERVER_PORT` 可覆盖）。默认管理员账号由单体 `DatabaseInitializer` 生成；本服务骨架未内置初始化器，可自行按 `db/schema-user.sql` 建表并插入一条 admin。

## 构建/测试/部署
```bash
mvn -B test
mvn -B package
docker build -t teaching-platform-user-service:$TAG .
kubectl apply -f k8s/user-service/deployment.yaml
```
