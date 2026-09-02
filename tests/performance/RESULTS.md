# 已完成性能实验记录

## 正式实验

- 实验编号：`perf-20260902-03`
- 实验日期：2026-09-02
- 正式报告：`docs/12-性能对比实验报告.md`
- 原始结果根目录：`results/performance/perf-20260902-03/`

## 实测参数

| 项目 | 值 |
| --- | --- |
| 单体代码版本 | `e7e32cc` |
| 微服务代码版本 | `dff546e` |
| 并发数 | `10` |
| 预热时间 | `10` 秒 |
| 采样时间 | `20` 秒 |
| 重复次数 | 每接口、每版本各 `3` 次 |
| 接口 | 登录、课程详情、编程判题 |

## 复现正式参数

先在单体版和微服务版中分别导入对应的性能夹具，并确保同一时间只启动一个被测版本。

```powershell
Set-Location D:\teachplatform\Teaching_platform
python -m pip install -r .\tests\performance\requirements.txt

powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\run-performance-suite.ps1 `
  -Target monolith -Runs 3 -Concurrency 10 -WarmupSeconds 10 -DurationSeconds 20 `
  -NoJudgeTaskId -ExperimentId perf-YYYYMMDD-xx

powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\run-performance-suite.ps1 `
  -Target microservices -Runs 3 -Concurrency 10 -WarmupSeconds 10 -DurationSeconds 20 `
  -NoJudgeTaskId -ExperimentId perf-YYYYMMDD-xx
```

`-NoJudgeTaskId` 使判题请求不创建任务提交记录，避免同一学生、同一任务的并发重复写入影响结果；因此该场景仅代表判题计算链路，详细限制见正式报告。
