# 性能对比实验

本目录用于对比单体版与微服务版在相同机器、相同数据、相同脚本下的性能表现。

## 建议工具

- 压测工具：Python `requests` + `ThreadPoolExecutor`
- 指标采集：`docker stats`、`kubectl top pod`、`curl`
- 结果记录：JSON/CSV

## 对比对象

- 单体版：`http://localhost:8081`
- 微服务版：`http://localhost:3000`

## 目标接口

1. 登录：`POST /login`
2. 课程详情：`GET /student/course/detail/{courseId}`
3. 编程判题：`POST /api/v2/judge/submit`

## 目录约定

- `scripts/`：启动与压测脚本
- `data/`：压测参数与输入数据
- `results/`：原始结果
- `reports/`：汇总报告

## 实验条件

- 单体版入口默认是 `http://localhost:8081`
- 微服务版统一入口默认是 `http://localhost:3000`
- 两版不能同时占用同一个数据库或端口；每次只启动一个被测版本
- 默认并发数为 `10`，预热 `15` 秒，正式采样 `60` 秒
- 每个接口每个版本默认运行 `3` 次
- 两版均使用性能夹具账号 `perf_student`、课程 ID `900001`、任务 ID `900001`
- 夹具脚本：`tests/performance/scripts/seed-performance-fixture.ps1`
- 判题请求固定使用相同的 Python 代码，并使用本地判题降级，避免云端 Judge0 波动

导入单体夹具：

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\seed-performance-fixture.ps1 -Target monolith
```

导入微服务夹具：

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\seed-performance-fixture.ps1 -Target microservices
```

## 运行性能实验

在仓库根目录执行。先确认 Docker Desktop 和被测版本已启动：

```powershell
Set-Location D:\teachplatform\Teaching_platform
python -m pip install -r tests/performance/requirements.txt
```

运行单体版三次：

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\run-performance-suite.ps1 -Target monolith
```

运行微服务版三次：

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\performance\scripts\run-performance-suite.ps1 -Target microservices
```

结果目录：

- 每次请求原始数据：`results/performance/<实验时间>/<版本>/<接口>/run-xx/benchmark.json`
- CPU/内存原始采样：同目录下的 `resources.csv`
- 单版本汇总：`detailed-summary.csv`
- 对比汇总：`comparison.csv`

只有 `comparison.csv` 和原始 JSON/CSV 都生成后，才可以在报告中写实际性能结论。
