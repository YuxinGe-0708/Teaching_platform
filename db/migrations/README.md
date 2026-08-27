# 数据迁移约定

初始表结构统一放在 `db/init/01_schema.sql`。之后的数据库结构变更按以下规则放在本目录，并在部署/发布流程中按文件名顺序执行：

- 命名：`NNN_description.sql`，例如 `001_add_course_cover.sql`。
- 原则：每个脚本只描述一次变更；生产环境按顺序执行后不得回退重写已执行脚本。
- 幂等：尽量写成可重复执行的形式（如使用存储过程判断列/索引是否存在，或先检查 `information_schema`）。
- 测试：迁移脚本应在 CI 的集成测试阶段，用干净 MySQL 容器从 `init/01_schema.sql` 初始化后再顺序执行验证。

模板示例：

```sql
-- 001_add_example_column.sql
ALTER TABLE `course` ADD COLUMN `example_column` VARCHAR(100) DEFAULT '' AFTER `description`;
```