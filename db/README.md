# 数据库容器

本目录用于数据库初始化与迁移，数据库容器直接使用 MySQL 官方镜像，无需自写 Dockerfile。

## 文件结构

- `init/01_schema.sql`：建库、建表、索引（首次启动自动执行）。
- `init/02_test_data.sql`：测试数据，账号密码统一为 `123456`（首次启动自动执行）。
- `migrations/`：后续数据迁移脚本约定，见 `migrations/README.md`。

## 启动

在项目根目录执行：

```powershell
docker compose up -d mysql
```

MySQL 官方镜像只在数据目录为空时自动执行 `docker-entrypoint-initdb.d/` 下的脚本；本仓库已把 `db/init` 挂载到该目录。

## 验证

```powershell
docker compose ps
docker compose logs -f mysql
docker compose exec mysql mysql -u tp_dev -p123456 teaching_platform -e "SHOW TABLES;"
```

## 清理

```powershell
docker compose down          # 停止并保留数据
docker compose down -v       # 停止并删除数据卷，下次启动会重新初始化
```