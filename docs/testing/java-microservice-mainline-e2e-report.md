# Java 微服务主线 E2E 测试矩阵报告

本文档由 `ci-artifacts/java-e2e-matrix.json` 整理生成，用于说明 `MicroserviceMainlineE2EScript` 在微服务架构下覆盖的端到端业务主线、测试输入、前置条件、预期输出、实际输出与断言结果。

## 报告说明

- 测试对象：gateway/BFF、user-service、learning-service、assessment-service 以及三套业务数据库。
- 执行方式：JUnit 5 测试类 `src/test/java/org/example/e2e/MicroserviceMainlineE2EScript.java`，测试过程中通过 HTTP 与 JDBC 验证跨服务行为。
- 数据来源：`ci-artifacts/java-e2e-matrix.json`，文件生成时间为 `2026-09-03 14:32:49 +08:00`。
- 文档生成时间：`2026-09-03 16:29:25 +08:00`。
- 安全说明：实际输出中出现的临时 JWT token 已脱敏为 `<JWT_TOKEN_已脱敏>`，其余测试矩阵字段保持原始结构与含义。

## 执行结论

| 指标 | 数值 |
|---|---:|
| 矩阵用例总数 | 5 |
| 通过用例 | 4 |
| 未通过用例 | 1 |

当前矩阵包含未通过用例，不能作为“全部 E2E 主线已通过”的证据。失败项应优先结合实际输出中的 `exception`、断言结果以及相关服务配置定位。

## 总览矩阵

| 测试编号 | 测试名称 | 测试函数（代码语言） | 前置条件（自然语言） | 预期输出（自然语言） | 断言结果 | passed |
|---|---|---|---|---|---|---|
| `E000` | 准备工作：部署所有服务 + 准备测试数据 | Java/JUnit5: GET /api/version, GET /actuator/health, JDBC SELECT | user-service、learning-service、assessment-service 已启动；三库已初始化种子数据。 | 三个服务健康检查和版本接口返回 200；三个数据库可查询且包含基础数据。 | 通过：三微服务、三业务库及演示数据均已就绪。 | `true` |
| `S001` | 主线一_学生：注册 -> 登录 -> 选课 -> 资源进度 -> 讨论 -> 普通作业 -> 考试 -> 编程作业 -> 成绩 -> AI | Java/JUnit5: user-service + learning-service + assessment-service + gateway HTTP/JDBC | 三服务已启动；测试账号可以注册；学习课程、班级、资源和考核任务可写入。 | 学生旅程跨 user、learning、assessment 和 BFF/gateway 完成；选课、进度、成绩和判题结果可回读。 | 通过：学生完成注册、选课、学习、讨论、作业、考试、编程、成绩和 AI 链路。 | `true` |
| `T001` | 主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发布资源/作业 -> 批改复核 -> 成绩统计 -> 归档 | Java/JUnit5: learning-service + assessment-service internal HTTP/JDBC | 教师和学生账号可用；课程、班级、资源及考核服务可写入；学生可通过邀请码加入。 | 课程内容生产、学生加入、作业批改、通知、统计和有学生时归档均成功；越权更新返回 403。 | 通过：教师完成课程、班级、资源、作业、批改、统计和归档链路。 | `true` |
| `A001` | 主线三_管理员：登录 -> 查询用户 -> 修改资料/角色 -> 审计日志 -> 禁用 -> 重置密码 -> 删除 | Java/JUnit5: user-service public/internal HTTP/JDBC | user-service 已启动；存在启用中的管理员账号；测试用户可注册。 | 管理员可治理普通用户；普通用户被禁用后不能登录；重置密码后可登录；测试用户删除且管理员账号保留。 | 通过：管理员完成用户查询、角色/资料修改、审计、禁用、重置密码和删除。 | `true` |
| `F001` | 主线四_跨服务异常与降级：重复选课/关闭课程/Judge0/AI/批改通知/未登录 | Java/JUnit5: gateway + three services HTTP/JDBC | 三服务和 gateway 已启动；Judge0 被配置为不可达且本地降级开启；AI 未配置密钥。 | 重复选课不新增记录；关闭课程拒绝选课；判题和 AI 返回降级结果；通知失败不影响成绩保存；未登录返回 401。 | 失败：失败：异常与降级主线存在异常；容量上限仅记录当前实现的实际行为。 ==> expected: <true> but was: <false> | `false` |

## 明细记录

### E000 准备工作：部署所有服务 + 准备测试数据

| 元素 | 内容 |
|---|---|
| 测试名称 | 准备工作：部署所有服务 + 准备测试数据 |
| 测试编号 | `E000` |
| 测试函数（代码语言） | Java/JUnit5: GET /api/version, GET /actuator/health, JDBC SELECT |
| 前置条件（自然语言） | user-service、learning-service、assessment-service 已启动；三库已初始化种子数据。 |
| 预期输出（自然语言） | 三个服务健康检查和版本接口返回 200；三个数据库可查询且包含基础数据。 |
| 断言结果 | 通过：三微服务、三业务库及演示数据均已就绪。 |
| passed | `true` |

**输入数据（代码语言）**

```json
{
  "baseUrl": "http://localhost:3000",
  "userServiceUrl": "http://localhost:8082",
  "learningServiceUrl": "http://localhost:8083",
  "assessmentServiceUrl": "http://localhost:8084",
  "database": "127.0.0.1:3308",
  "databaseUser": "root"
}
```

**实际输出（代码语言）**

```json
{
  "config": {
    "baseUrl": "http://localhost:3000",
    "userServiceUrl": "http://localhost:8082",
    "learningServiceUrl": "http://localhost:8083",
    "assessmentServiceUrl": "http://localhost:8084",
    "database": "127.0.0.1:3308",
    "databaseUser": "root"
  },
  "userService": {
    "version": "{httpStatus=200, apiCode=200, message=success, data={name=user-service, version=1.0.0}}",
    "health": "{httpStatus=200, apiCode=200, message=null, data=null}"
  },
  "learningService": {
    "version": "{httpStatus=200, apiCode=200, message=success, data={service=learning-service, version=1.0.0}}",
    "health": "{httpStatus=200, apiCode=200, message=null, data=null}"
  },
  "assessmentService": "{httpStatus=200, apiCode=200, message=null, data=null}",
  "database": {
    "adminCount": "1",
    "courseCount": "3",
    "taskCount": "4"
  }
}
```

### S001 主线一_学生：注册 -> 登录 -> 选课 -> 资源进度 -> 讨论 -> 普通作业 -> 考试 -> 编程作业 -> 成绩 -> AI

| 元素 | 内容 |
|---|---|
| 测试名称 | 主线一_学生：注册 -> 登录 -> 选课 -> 资源进度 -> 讨论 -> 普通作业 -> 考试 -> 编程作业 -> 成绩 -> AI |
| 测试编号 | `S001` |
| 测试函数（代码语言） | Java/JUnit5: user-service + learning-service + assessment-service + gateway HTTP/JDBC |
| 前置条件（自然语言） | 三服务已启动；测试账号可以注册；学习课程、班级、资源和考核任务可写入。 |
| 预期输出（自然语言） | 学生旅程跨 user、learning、assessment 和 BFF/gateway 完成；选课、进度、成绩和判题结果可回读。 |
| 断言结果 | 通过：学生完成注册、选课、学习、讨论、作业、考试、编程、成绩和 AI 链路。 |
| passed | `true` |

**输入数据（代码语言）**

```json
{
  "teacher": "e2e_st_3143241799",
  "student": "e2e_ss_3143241799",
  "courseCode": "E2E-S-241799"
}
```

**实际输出（代码语言）**

```json
{
  "registration": {
    "teacher": {
      "response": "{httpStatus=200, apiCode=200, message=注册成功, data={id=1021, username=e2e_st_3143241799, role=teacher, name=E2E 教师}}",
      "userId": 1021,
      "user": {
        "id": 1021,
        "username": "e2e_st_3143241799",
        "role": "teacher",
        "name": "E2E 教师"
      }
    },
    "student": {
      "response": "{httpStatus=200, apiCode=200, message=注册成功, data={id=1022, username=e2e_ss_3143241799, role=student, name=E2E 学生}}",
      "userId": 1022,
      "user": {
        "id": 1022,
        "username": "e2e_ss_3143241799",
        "role": "student",
        "name": "E2E 学生"
      }
    }
  },
  "login": {
    "response": "{httpStatus=200, apiCode=200, message=success, data={token=<JWT_TOKEN_已脱敏>, user={id=1022, username=e2e_ss_3143241799, role=student, name=E2E 学生, avatarUrl=, status=1, createdAt=2026-09-02T22:32:41.000+00:00}}}",
    "tokenPresent": true,
    "userId": 1022,
    "user": {
      "id": 1022,
      "username": "e2e_ss_3143241799",
      "role": "student",
      "name": "E2E 学生",
      "avatarUrl": "",
      "status": 1,
      "createdAt": "2026-09-03T06:32:41+08:00"
    }
  },
  "courseAndEnrollment": {
    "before": "{httpStatus=200, apiCode=200, message=success, data=[]}",
    "course": {
      "response": "{httpStatus=200, apiCode=200, message=success, data={id=1012, name=学生全流程课程 20260903143241799, code=E2E-S-241799, description=学生全流程课程 20260903143241799 描述, credits=3, subjectCategory=微服务 E2E, hours=32, teacherId=1021, inviteCode=43DAB705, allowJoin=true, status=active}}",
      "id": 1012,
      "body": {
        "id": 1012,
        "name": "学生全流程课程 20260903143241799",
        "code": "E2E-S-241799",
        "description": "学生全流程课程 20260903143241799 描述",
        "credits": 3,
        "subjectCategory": "微服务 E2E",
        "hours": 32,
        "teacherId": 1021,
        "inviteCode": "43DAB705",
        "allowJoin": true,
        "status": "active"
      }
    },
    "class": {
      "response": "{httpStatus=200, apiCode=200, message=success, data={id=1012, courseId=1012, name=学生全流程班级, inviteCode=BB68D785, maxCount=30, currentCount=0}}",
      "id": 1012,
      "body": {
        "id": 1012,
        "courseId": 1012,
        "name": "学生全流程班级",
        "inviteCode": "BB68D785",
        "maxCount": 30,
        "currentCount": 0
      }
    },
    "enroll": "{httpStatus=200, apiCode=200, message=success, data={id=12, studentId=1022, courseId=1012, classId=1012}}",
    "myCourses": "{httpStatus=200, apiCode=200, message=success, data=[{id=1012, name=学生全流程课程 20260903143241799, code=E2E-S-241799, description=学生全流程课程 20260903143241799 描述, credits=3, subjectCategory=微服务 E2E, hours=32, teacherId=1021, inviteCode=43DAB705, allowJoin=true, status=active, createdAt=2026-09-02T22:32:42.000+00:00}]}",
    "check": "{httpStatus=200, apiCode=200, message=success, data=true}",
    "classDb": {
      "current_count": "1",
      "max_count": "30"
    }
  },
  "resourceProgress": {
    "save": "{httpStatus=200, apiCode=200, message=success, data=保存成功}",
    "read": "{httpStatus=200, apiCode=200, message=success, data={duration=600.0, progress=50.0, lastPosition=300.0}}",
    "db": {
      "progress": "50.00",
      "last_position": "300.00",
      "duration": "600.00"
    }
  },
  "discussionAndNote": {
    "post": "{httpStatus=200, apiCode=200, message=success, data={id=1006, courseId=1012, userId=1022, title=E2E 学生提问, content=如何理解服务边界？, anonymous=false, postType=discussion, targetRole=all}}",
    "reply": "{httpStatus=200, apiCode=200, message=success, data={id=5, postId=1006, userId=1021, content=请结合 BFF 和领域服务理解。, anonymous=false, assistantReply=false}}",
    "note": {
      "response": "{httpStatus=200, apiCode=200, message=success, data={id=1004, studentId=1022, courseId=1012, resourceId=1009, title=E2E 学习笔记, content=服务边界和选课一致性}}",
      "id": 1004,
      "body": {
        "id": 1004,
        "studentId": 1022,
        "courseId": 1012,
        "resourceId": 1009,
        "title": "E2E 学习笔记",
        "content": "服务边界和选课一致性"
      }
    }
  },
  "assessment": {
    "homework": "{httpStatus=200, apiCode=200, message=success, data={id=1014, taskId=1015, studentId=1022, content=BFF 负责会话适配和请求编排。, filePath=uploads/demo/e2e-answer.txt, score=88.0, status=graded, feedback=E2E 批改通过, submittedAt=2026-09-02T22:32:42.000+00:00, comment=E2E 批改通过, submitTime=2026-09-02T22:32:42.000+00:00, submissionId=1014, submitStatus=已批改, fileUrl=uploads/demo/e2e-answer.txt, judgeStatus=-, language=-}}",
    "homeworkDb": {
      "score": "88.0",
      "status": "graded",
      "file_path": "uploads/demo/e2e-answer.txt"
    },
    "examBegin": "{httpStatus=200, apiCode=200, message=success, data={id=1005, taskId=1016, studentId=1022, startTime=2026-09-03T06:32:42.233+00:00, content=, status=IN_PROGRESS, inProgress=true, submitted=false, notStarted=false}}",
    "examProgress": "{httpStatus=200, apiCode=200, message=success, data={id=1005, taskId=1016, studentId=1022, startTime=2026-09-03T06:32:42.000+00:00, content=暂存答案, status=IN_PROGRESS, createdAt=2026-09-02T22:32:42.000+00:00, updatedAt=2026-09-02T22:32:42.000+00:00, inProgress=true, submitted=false, notStarted=false}}",
    "examSubmit": "{httpStatus=200, apiCode=200, message=success, data={id=1005, taskId=1016, studentId=1022, startTime=2026-09-03T06:32:42.000+00:00, submitTime=2026-09-03T06:32:42.261+00:00, content=正确, score=100.0, status=SUBMITTED, createdAt=2026-09-02T22:32:42.000+00:00, updatedAt=2026-09-02T22:32:42.000+00:00, inProgress=false, submitted=true, notStarted=false}}",
    "examDb": {
      "status": "SUBMITTED",
      "score": "100.0",
      "content": "正确"
    },
    "scores": "{httpStatus=200, apiCode=200, message=success, data=[{id=1016, taskId=1017, studentId=1022, content=a,b=map(int,input().split())\nprint(a+b), score=100.0, status=graded, judgeResult=AC, feedback=答案通过全部评测。, submittedAt=2026-09-02T22:32:45.000+00:00, comment=答案通过全部评测。, submitTime=2026-09-02T22:32:45.000+00:00, submissionId=1016, submitStatus=已批改, judgeStatus=AC, language=-}, {id=1014, taskId=1015, studentId=1022, content=BFF 负责会话适配和请求编排。, filePath=uploads/demo/e2e-answer.txt, score=88.0, status=graded, feedback=E2E 批改通过, submittedAt=2026-09-02T22:32:42.000+00:00, comment=E2E 批改通过, submitTime=2026-09-02T22:32:42.000+00:00, submissionId=1014, submitStatus=已批改, fileUrl=uploads/demo/e2e-answer.txt, judgeStatus=-, language=-}, {id=1015, taskId=1016, studentId=1022, content=正确, filePath=, score=100.0, status=graded, judgeResult=AC, feedback=系统自动判分：答案正确。, submittedAt=2026-09-02T22:32:42.000+00:00, comment=系统自动判分：答案正确。, submitTime=2026-09-02T22:32:42.000+00:00, submissionId=1015, submitStatus=已批改, judgeStatus=AC, language=-}]}"
  },
  "gateway": {
    "pageLogin": "{httpStatus=302, apiCode=302, body=}",
    "judge": "{httpStatus=200, apiCode=200, message=评测完成, data={status=AC, score=100.0, passedCases=2, totalCases=2, timeUsedMs=30.0, memoryUsedKb=3316.0, diagnosis=答案通过全部评测。, usedLocalJudge=false}}",
    "ai": "{httpStatus=200, apiCode=200, message=success, data={reply=AI 助手尚未配置 API Key。}}"
  },
  "judgeDb": {
    "status": "graded",
    "judge_result": "AC",
    "score": "100.0"
  },
  "notifications": "{httpStatus=200, apiCode=200, message=success, data=[{id=12, userId=1022, title=讨论收到新回复, content=请结合 BFF 和领域服务理解。, type=course, isRead=false, createdAt=2026-09-02T22:32:42.000+00:00}, {id=13, userId=1022, title=成绩已发布, content=E2E 批改通过, type=grade, isRead=false, createdAt=2026-09-02T22:32:42.000+00:00}]}"
}
```

### T001 主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发布资源/作业 -> 批改复核 -> 成绩统计 -> 归档

| 元素 | 内容 |
|---|---|
| 测试名称 | 主线二_教师：注册 -> 登录 -> 创建课程/班级 -> 发布资源/作业 -> 批改复核 -> 成绩统计 -> 归档 |
| 测试编号 | `T001` |
| 测试函数（代码语言） | Java/JUnit5: learning-service + assessment-service internal HTTP/JDBC |
| 前置条件（自然语言） | 教师和学生账号可用；课程、班级、资源及考核服务可写入；学生可通过邀请码加入。 |
| 预期输出（自然语言） | 课程内容生产、学生加入、作业批改、通知、统计和有学生时归档均成功；越权更新返回 403。 |
| 断言结果 | 通过：教师完成课程、班级、资源、作业、批改、统计和归档链路。 |
| passed | `true` |

**输入数据（代码语言）**

```json
{
  "teacher": "e2e_tt_3143245657",
  "student": "e2e_ts_3143245657",
  "courseCode": "E2E-T-245657"
}
```

**实际输出（代码语言）**

```json
{
  "registration": {
    "teacher": {
      "response": "{httpStatus=200, apiCode=200, message=注册成功, data={id=1023, username=e2e_tt_3143245657, role=teacher, name=E2E 教师主线}}",
      "userId": 1023,
      "user": {
        "id": 1023,
        "username": "e2e_tt_3143245657",
        "role": "teacher",
        "name": "E2E 教师主线"
      }
    },
    "student": {
      "response": "{httpStatus=200, apiCode=200, message=注册成功, data={id=1024, username=e2e_ts_3143245657, role=student, name=E2E 加入学生}}",
      "userId": 1024,
      "user": {
        "id": 1024,
        "username": "e2e_ts_3143245657",
        "role": "student",
        "name": "E2E 加入学生"
      }
    }
  },
  "course": {
    "response": "{httpStatus=200, apiCode=200, message=success, data={id=1013, name=教师课程 20260903143245657, code=E2E-T-245657, description=教师课程 20260903143245657 描述, credits=3, subjectCategory=微服务 E2E, hours=32, teacherId=1023, inviteCode=3F92A4C1, allowJoin=true, status=active}}",
    "id": 1013,
    "body": {
      "id": 1013,
      "name": "教师课程 20260903143245657",
      "code": "E2E-T-245657",
      "description": "教师课程 20260903143245657 描述",
      "credits": 3,
      "subjectCategory": "微服务 E2E",
      "hours": 32,
      "teacherId": 1023,
      "inviteCode": "3F92A4C1",
      "allowJoin": true,
      "status": "active"
    }
  },
  "class": {
    "response": "{httpStatus=200, apiCode=200, message=success, data={id=1013, courseId=1013, name=教师班级, inviteCode=BDFB18C0, maxCount=20, currentCount=0}}",
    "id": 1013,
    "body": {
      "id": 1013,
      "courseId": 1013,
      "name": "教师班级",
      "inviteCode": "BDFB18C0",
      "maxCount": 20,
      "currentCount": 0
    }
  },
  "resource": {
    "response": "{httpStatus=200, apiCode=200, message=success, data={id=1010, courseId=1013, title=教师发布资源, filePath=uploads/demo/teacher.pdf, type=pdf, chapter=第2章, fileSize=2048}}",
    "id": 1010,
    "body": {
      "id": 1010,
      "courseId": 1013,
      "title": "教师发布资源",
      "filePath": "uploads/demo/teacher.pdf",
      "type": "pdf",
      "chapter": "第2章",
      "fileSize": 2048
    }
  },
  "joinByInvite": "{httpStatus=200, apiCode=200, message=success, data={id=13, studentId=1024, courseId=1013, classId=1013}}",
  "assessment": {
    "task": 1018,
    "submission": "{httpStatus=200, apiCode=200, message=success, data={id=1017, taskId=1018, studentId=1024, content=学生提交内容, status=submitted, submissionId=1017, submitStatus=已提交, judgeStatus=-, language=-}}",
    "grade": "{httpStatus=200, apiCode=200, message=success, data={id=1017, taskId=1018, studentId=1024, content=学生提交内容, score=91.0, status=graded, feedback=教师复核通过, submittedAt=2026-09-02T22:32:45.000+00:00, comment=教师复核通过, submitTime=2026-09-02T22:32:45.000+00:00, submissionId=1017, submitStatus=已批改, judgeStatus=-, language=-}}",
    "tasks": "{httpStatus=200, apiCode=200, message=success, data=[{id=1018, title=教师普通作业, description=教师发布的作业, courseId=1013, type=homework, maxScore=100, timeLimitMs=15000, memoryLimitMb=128, status=published, createdAt=2026-09-02T22:32:45.000+00:00, updatedAt=2026-09-02T22:32:45.000+00:00}]}",
    "stats": "{httpStatus=200, apiCode=200, message=success, data={courseId=1013, submissions=[{id=1017, taskId=1018, studentId=1024, content=学生提交内容, score=91.0, status=graded, feedback=教师复核通过, submittedAt=2026-09-02T22:32:45.000+00:00, comment=教师复核通过, submitTime=2026-09-02T22:32:45.000+00:00, submissionId=1017, submitStatus=已批改, judgeStatus=-, language=-}], averageScore=91.0, submittedCount=1}}"
  },
  "authorization": {
    "wrongUpdate": "{httpStatus=200, apiCode=403, message=无权限或课程不存在, data=null}",
    "archive": "{httpStatus=200, apiCode=200, message=success, data=归档成功}"
  },
  "database": {
    "course": {
      "status": "archived"
    },
    "class": {
      "current_count": "1",
      "max_count": "20"
    },
    "submission": {
      "score": "91.0",
      "status": "graded"
    },
    "gradeNotificationCount": "1"
  }
}
```

### A001 主线三_管理员：登录 -> 查询用户 -> 修改资料/角色 -> 审计日志 -> 禁用 -> 重置密码 -> 删除

| 元素 | 内容 |
|---|---|
| 测试名称 | 主线三_管理员：登录 -> 查询用户 -> 修改资料/角色 -> 审计日志 -> 禁用 -> 重置密码 -> 删除 |
| 测试编号 | `A001` |
| 测试函数（代码语言） | Java/JUnit5: user-service public/internal HTTP/JDBC |
| 前置条件（自然语言） | user-service 已启动；存在启用中的管理员账号；测试用户可注册。 |
| 预期输出（自然语言） | 管理员可治理普通用户；普通用户被禁用后不能登录；重置密码后可登录；测试用户删除且管理员账号保留。 |
| 断言结果 | 通过：管理员完成用户查询、角色/资料修改、审计、禁用、重置密码和删除。 |
| passed | `true` |

**输入数据（代码语言）**

```json
{
  "admin": "seeded admin",
  "temporaryUser": "e2e_as_3143245926",
  "resetPassword": "654321"
}
```

**实际输出（代码语言）**

```json
{
  "adminLogin": "{httpStatus=200, apiCode=200, message=success, data={token=<JWT_TOKEN_已脱敏>, user={id=1001, username=ms_admin, role=admin, name=微服务管理员, email=ms_admin@example.com, avatarUrl=, status=1, createdAt=2026-09-02T19:45:31.000+00:00}}}",
  "targetRegistration": {
    "response": "{httpStatus=200, apiCode=200, message=注册成功, data={id=1025, username=e2e_as_3143245926, role=student, name=E2E 待治理用户}}",
    "userId": 1025,
    "user": {
      "id": 1025,
      "username": "e2e_as_3143245926",
      "role": "student",
      "name": "E2E 待治理用户"
    }
  },
  "list": "{httpStatus=200, apiCode=200, message=success, data=[{id=1025, username=e2e_as_3143245926, role=student, name=E2E 待治理用户, avatarUrl=, status=1, createdAt=2026-09-02T22:32:46.000+00:00}, {id=1004, username=ms_student, role=student, name=微服务学生, email=ms_student@example.com, avatarUrl=, status=1, createdAt=2026-09-02T19:45:31.000+00:00}, {id=1005, username=ms_student2, role=student, name=测试学生二, email=ms_student2@example.com, avatarUrl=, status=1, createdAt=2026-09-02T19:45:31.000+00:00}, {id=1006, username=ms_student3, role=student, name=测试学生三, email=ms_student3@example.com, avatarUrl=, status=1, createdAt=2026-09-02T19:45:31.000+00:00}]}",
  "roleUpdate": {
    "response": "{httpStatus=200, apiCode=200, message=success, data=1}",
    "loginUser": {
      "response": "{httpStatus=200, apiCode=200, message=success, data={token=<JWT_TOKEN_已脱敏>, user={id=1025, username=e2e_as_3143245926, role=teacher, name=E2E 教师角色, email=e2e@example.com, avatarUrl=, status=1, createdAt=2026-09-02T22:32:46.000+00:00}}}",
      "tokenPresent": true,
      "userId": 1025,
      "user": {
        "id": 1025,
        "username": "e2e_as_3143245926",
        "role": "teacher",
        "name": "E2E 教师角色",
        "email": "e2e@example.com",
        "avatarUrl": "",
        "status": 1,
        "createdAt": "2026-09-03T06:32:46+08:00"
      }
    }
  },
  "audit": {
    "seed": "{httpStatus=200, apiCode=200, message=ok, data=null}",
    "logs": "{httpStatus=200, apiCode=200, message=success, data=[{id=4, userId=1025, username=e2e_as_3143245926, action=E2E 管理审计, detail=角色已修改, createdAt=2026-09-02T22:32:46.000+00:00}, {id=1, userId=1001, username=ms_admin, action=初始化演示数据, detail=user-service 演示账号、通知和日志已导入, createdAt=2026-09-02T19:45:31.000+00:00}]}"
  },
  "passwordAndStatus": {
    "disable": "{httpStatus=200, apiCode=200, message=ok, data=null}",
    "disabledLogin": "{httpStatus=200, apiCode=401, message=用户名或密码错误，或账号已禁用, data=null}",
    "reset": "{httpStatus=200, apiCode=200, message=ok, data=null}",
    "enable": "{httpStatus=200, apiCode=200, message=ok, data=null}",
    "newLogin": "{httpStatus=200, apiCode=200, message=success, data={token=<JWT_TOKEN_已脱敏>, user={id=1025, username=e2e_as_3143245926, role=teacher, name=E2E 教师角色, email=e2e@example.com, avatarUrl=, status=1, createdAt=2026-09-02T22:32:46.000+00:00}}}"
  },
  "delete": "{httpStatus=200, apiCode=200, message=ok, data=null}",
  "database": {
    "remainingTarget": "0",
    "adminStillExists": "1"
  }
}
```

### F001 主线四_跨服务异常与降级：重复选课/关闭课程/Judge0/AI/批改通知/未登录

| 元素 | 内容 |
|---|---|
| 测试名称 | 主线四_跨服务异常与降级：重复选课/关闭课程/Judge0/AI/批改通知/未登录 |
| 测试编号 | `F001` |
| 测试函数（代码语言） | Java/JUnit5: gateway + three services HTTP/JDBC |
| 前置条件（自然语言） | 三服务和 gateway 已启动；Judge0 被配置为不可达且本地降级开启；AI 未配置密钥。 |
| 预期输出（自然语言） | 重复选课不新增记录；关闭课程拒绝选课；判题和 AI 返回降级结果；通知失败不影响成绩保存；未登录返回 401。 |
| 断言结果 | 失败：失败：异常与降级主线存在异常；容量上限仅记录当前实现的实际行为。 ==> expected: <true> but was: <false> |
| passed | `false` |

**输入数据（代码语言）**

```json
{
  "judge0": "127.0.0.1:9",
  "aiApiKey": "empty",
  "classMaxCount": 1,
  "capacityNote": "当前实现未强制 max_count 上限"
}
```

**实际输出（代码语言）**

```json
{
  "enrollmentIdempotency": {
    "first": "{httpStatus=200, apiCode=200, message=success, data={id=14, studentId=1027, courseId=1014, classId=1014}}",
    "duplicate": "{httpStatus=200, apiCode=200, message=success, data={id=14, studentId=1027, courseId=1014, classId=1014, enrolledAt=2026-09-02T22:32:46.000+00:00}}",
    "enrollmentCount": "1",
    "classCount": "1"
  },
  "judgeFallback": {
    "login": "{httpStatus=302, apiCode=302, body=}",
    "judge": "{httpStatus=200, apiCode=200, message=评测完成, data={status=AC, score=100.0, passedCases=1, totalCases=1, timeUsedMs=27.0, memoryUsedKb=3220.0, diagnosis=答案通过全部评测。, usedLocalJudge=false}}"
  },
  "aiFallback": "{httpStatus=200, apiCode=200, message=success, data={reply=AI 助手尚未配置 API Key。}}",
  "notificationFailure": {
    "gradeSeed": "{httpStatus=200, apiCode=200, message=success, data={id=1018, taskId=1019, studentId=1027, content=容错提交, score=100.0, status=graded, judgeResult=AC, feedback=答案通过全部评测。, submittedAt=2026-09-02T22:32:48.000+00:00, comment=答案通过全部评测。, submitTime=2026-09-02T22:32:48.000+00:00, submissionId=1018, submitStatus=已批改, judgeStatus=AC, language=-}}",
    "grade": "{httpStatus=200, apiCode=200, message=success, data={id=1018, taskId=1019, studentId=1027, content=容错提交, score=77.0, status=graded, feedback=通知服务异常时仍应保留成绩, submittedAt=2026-09-02T22:32:49.000+00:00, comment=通知服务异常时仍应保留成绩, submitTime=2026-09-02T22:32:49.000+00:00, submissionId=1018, submitStatus=已批改, judgeStatus=-, language=-}}",
    "savedGrade": {
      "score": "77.0",
      "status": "graded"
    }
  },
  "authorizationAndClosedCourse": {
    "unauthorized": "{httpStatus=401, apiCode=401, body=}",
    "closedCourse": "{httpStatus=200, apiCode=200, message=success, data={id=1015, name=关闭课程 20260903143246289, code=E2E-FC-246289, credits=0, subjectCategory=, hours=0, teacherId=1026, inviteCode=A4179DD1, allowJoin=false, status=closed}}",
    "closedEnroll": "{httpStatus=200, apiCode=400, message=选课失败，课程不存在或不允许加入, data=null}",
    "closedCount": "0"
  },
  "exception": "org.opentest4j.AssertionFailedError: 失败：异常与降级主线存在异常；容量上限仅记录当前实现的实际行为。 ==> expected: <true> but was: <false>"
}
```

## 后续处理建议

- 若 CI 中出现 `Tests run: 1, Skipped: 1`，说明测试类环境探测未通过，不能视为 Java E2E 主线通过。
- 本矩阵中 `F001` 当前失败点来自实际输出中的 `usedLocalJudge=false`，而断言期望 Judge0 本地降级被使用；需要让测试环境的 Judge0 配置确实不可达，或调整该主线对远端 Judge0 可用场景的判定。
- 每次重新执行 `MicroserviceMainlineE2EScript` 后，应以新生成的 `ci-artifacts/java-e2e-matrix.json` 更新本文档，确保测试证据与当前运行结果一致。

