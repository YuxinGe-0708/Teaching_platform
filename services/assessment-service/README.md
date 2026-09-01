# assessment-service

评测与成绩域，独占 `assessment_db` 中的 `task`、`submission`、`exam_record` 三张表。
课程和选课关系由 learning-service 提供，用户、通知和操作日志由 user-service 提供；本服务不访问其它服务数据库。

```powershell
mvn -B test
mvn -B package
```

端口默认为 8084。服务间校验通过 `LEARNING_SERVICE_URL` 调用课程/选课接口，批改后通过 `USER_SERVICE_URL` 发送通知和记录日志；调用使用有限超时和最多三次重试，失败时不会写入对方数据库。
