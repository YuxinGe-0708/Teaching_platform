# 环境与工具配置

## 必备环境

- Windows PowerShell 5+
- Docker Desktop
- Java 8 / Maven 3.8+
- Python 3.10+
- Python 3.10+、`requests`、`psutil`

## 推荐安装

### Python 压测器

- 使用固定线程数执行同一批 HTTP 请求
- 输出吞吐量、平均耗时、P95、P99、错误率和状态码

### Docker

- 用于查看被测容器的 CPU 和内存
- 采集脚本同时记录主机 CPU 和内存

## 运行前准备

- 启动 Docker Desktop
- 启动单体版或微服务版服务
- 确认数据库数据已导入
- 确认测试账号可登录

## 结果要求

- 每个版本至少跑 3 次
- 保留原始结果文件
- 记录测试时间、机器配置、并发数、数据集版本、镜像版本
