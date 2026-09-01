# 微服务端到端回归测试

本目录提供 A 同学负责的 E2E 基础设施：统一 API 客户端、环境参数、健康检查、报告输出和 CI 入口。

## 运行方式

```powershell
.\scripts\e2e-microservices.ps1
```

只运行测试、不启动环境：

```powershell
python -m pytest tests/e2e `
  --user-url http://localhost:8082 `
  --learning-url http://localhost:8083 `
  --assessment-url http://localhost:8084
```

## 后续补充

B 同学在本目录继续增加业务脚本，例如：

- `test_user_flow.py`：注册、登录、个人资料、通知。
- `test_learning_flow.py`：课程、班级、选退课、资源、笔记、讨论。
- `test_assessment_flow.py`：作业、考试、提交、批改、成绩、判题。

每个测试用 `record_case` 写入证据，最终报告输出到 `ci-artifacts/e2e-report.json`。
