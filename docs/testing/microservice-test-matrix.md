# 三微服务集成/API 测试矩阵

业务名称与编号以《软件详细设计说明书》3 章为准。`public-api-coverage.csv` 是 55 个公开 HTTP 映射的逐接口清单，CI 中的覆盖闸门保证新增公开接口不能漏测。

| 用例 | 主成功流程 | 备选流程 | 异常流程 | 自动化层 |
|---|---|---|---|---|
| UC001 访客注册 | 注册后写入 user_db，再登录取得 JWT | 不同学生账号注册 | 重复用户名、非法角色/请求字段 | AuthControllerApiTest；容器业务回归 |
| UC002 用户登录 | 凭据校验、JWT/BFF session、资料查询与修改 | 资料字段部分更新 | 错误密码、未登录、用户不存在 | Auth/PublicControllerApiTest；容器业务回归 |
| UC003 学生选课与退课 | learning_db 写入、删除、再次选课 | 已选课程返回既有记录 | 课程不可加入、未选课退课 | Enrollment/PublicApiCoverageTest；容器业务回归 |
| UC004 教师课程和班级管理 | 课程/班级 CRUD | 按教师/学生查询课程 | 无权限、课程或班级不存在 | PublicApiCoverageTest |
| UC005 学习资源与进度管理 | 资源 CRUD、下载计数、进度新增/更新 | 无进度返回 0 | 资源不存在 | PublicApiCoverageTest；容器业务回归 |
| UC006 学生作业提交 | 选课校验、提交落库、修改/补交 | 带附件与纯文本 | 任务不存在、未选课、提交不存在 | CrossServiceControllerApiTest；容器业务回归 |
| UC007 学生参加考试并自动提交 | 开始、暂存、提交并生成 submission | 自动提交 | 考试不存在、未选课、无权暂存/提交 | CrossServiceControllerApiTest；容器业务回归 |
| UC008 编程作业自动评测 | 评测、成绩落库 | 本地 Judge 降级 | 空代码、任务不存在、语言不允许 | JudgeControllerApiTest；容器业务回归 |
| UC009 AI学习笔记和辅助 | AI 五接口、笔记 CRUD | 默认会话/课程名、AI 降级 | 空消息、笔记不存在 | PublicApiCoverageTest；容器业务回归 |
| UC010 课程讨论 | 发帖、回复、查询并通知 | 匿名/定向参数默认值 | 帖子或回复不存在 | PublicApiCoverageTest；容器业务回归 |
| UC011 管理员管理用户 | 查询、状态、重置密码、删除、日志 | 角色筛选 | 非管理员、非法状态/密码、用户不存在 | PublicControllerApiTest |
| UC012 成绩统计与查询 | 学生/课程成绩读取 | 空成绩集合 | 非法资源由接口返回业务错误 | CrossServiceControllerApiTest；容器业务回归 |
| UC013 教师批改、复核成绩并通知学生 | 批改落库，调用 user-service 发通知和日志 | 无评语 | 提交不存在 | CrossServiceControllerApiTest；容器业务回归 |

容器业务回归使用真实 MySQL，并从网关/BFF 发起请求；它验证 user、learning、assessment 三库访问及 BFF/assessment 对其它服务的 HTTP 调用。MockMvc 层验证全部公开映射和业务分支，二者职责互补。
