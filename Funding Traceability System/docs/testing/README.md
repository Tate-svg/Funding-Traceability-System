# 助学帮扶资金溯源系统 测试交付物

> 测试工具：JUnit 5 + Mockito、Postman（newman）、Apache JMeter
> 测试范围：功能测试、权限测试、基础安全测试、性能测试
> 环境：Spring Boot `test` profile，端口 **8090**，独立测试库 **`funding_test` / `funding_privacy_test`**
> 演示库 `funding_db` / `funding_privacy` **全程未连接**

---

## 一、交付物索引（对应任务要求）

| 任务要求 | 交付物 | 位置 |
| --- | --- | --- |
| ① JUnit 单元测试源码（Mock，不污染业务库） | 6 个测试类 / 53 个方法 | `backend/src/test/java/com/funding/**` |
| ② Postman 导出集合（功能 / 权限 / 安全，含断言与批量执行） | Collection v2.1（80 请求 / 201 断言）+ Environment | `docs/testing/postman/*.json` |
| ③ JMeter 压测脚本（自动登录取 Token，500 并发） | JMX（setUp + 2 个事务 + 断言） | `docs/testing/jmeter/Funding-Traceability-Performance.jmx` |
| ④ 测试用例文档 | 功能 28 + 权限 28 + 安全 18 + 性能 3 + 单元 53 | `docs/testing/测试用例文档.md` |
| ④ 测试报告（含实测指标） | 汇总、性能明细、瓶颈分析、结论 | `docs/testing/测试报告.md` |
| ④ 测试截图 | JUnit / Postman / JMeter 三张 | `docs/testing/screenshots/*.png` |
| ④ 后端运行日志 | test profile 运行日志 | `docs/testing/evidence/backend-test.log` |
| ④ 环境配置说明 | 依赖、建库、启动、执行、FAQ、隔离要求 | `docs/testing/环境配置说明.md` |
| 附录：原始证据 | 日志 / JTL / JSON / HTML 报告 | `docs/testing/evidence/` |

---

## 二、实测结果速览（2026-11-02）

| 测试 | 结果 | 关键指标 |
| --- | --- | --- |
| JUnit | **53 / 53 通过** | Failures 0、Errors 0、Skipped 0，耗时 1.05s |
| Postman | **80 请求 / 201 断言通过** | 失败 0，平均响应 15ms，耗时 5.7s |
| JMeter | **5002 请求，0 错误（0.00%）** | 500 线程 / 5 循环，峰值在线 494，总 TPS **452.9/s** |
| 性能达标 | 溯源查询达标；申请提交未达 1s 目标 | 溯源平均 221.9ms；申请提交平均 1333.7ms（瓶颈分析见报告） |

---

## 三、目录结构

```
docs/testing/
├── README.md                              # 本文件
├── 测试用例文档.md                          # F/P/S/PF + JUnit 用例 + 缺陷风险
├── 测试报告.md                              # 实测结果、性能指标、结论
├── 环境配置说明.md                          # 依赖、建库、启动、执行、FAQ
├── run-all.ps1                            # 一键执行（重置库→JUnit→后端→Postman→JMeter→清理）
├── scripts/
│   ├── reset-test-db.ps1                  # 重建独立测试库与种子数据
│   ├── start-test-backend.ps1             # 构建并以 test profile 启动 8090
│   ├── stop-test-backend.ps1              # 按端口停止 8090
│   ├── run-junit.ps1                      # 执行单元测试 + 生成 Surefire 报告
│   ├── run-postman.ps1                    # newman 批量执行 + HTML 报告
│   ├── run-jmeter.ps1                     # 500 并发压测 + Dashboard
│   └── cleanup-perf-data.ps1              # 清理压测产生的申请单
├── postman/
│   ├── Funding-Traceability.postman_collection.json
│   └── Funding-Traceability.postman_environment.json
├── jmeter/
│   └── Funding-Traceability-Performance.jmx
├── evidence/
│   ├── backend-test.log                   # 后端运行日志
│   ├── junit-test-run.log                 # JUnit 控制台日志
│   ├── postman.log / postman-result.json / postman-report.html
│   ├── jmeter-result.jtl / jmeter.log / jmeter-console.log
│   ├── jmeter-report/                     # JMeter HTML Dashboard
│   └── junit-report/                      # Surefire HTML 报告
└── screenshots/
    ├── junit-report.png
    ├── postman-report.png
    └── jmeter-dashboard.png
```

---

## 四、快速开始

> 前置：JDK 21（`D:\java`）、Maven 3.9.6、MySQL 8.0、Node.js；JMeter 5.6.3 解压到 `.tools\apache-jmeter-5.6.3`。

默认参数集中在脚本顶部，可按本机路径调整。

```powershell
cd "d:\Funding Traceability System\Funding Traceability System\docs\testing"

# 一键全流程（会自动重置测试库、启动 8090、跑完再停止）
powershell -ExecutionPolicy Bypass -File .\run-all.ps1
```

分步执行：

```powershell
# 1. 重置独立测试库（7 用户 / 8 流水 / 0 申请单）
powershell -ExecutionPolicy Bypass -File .\scripts\reset-test-db.ps1

# 2. JUnit（不连库）
powershell -ExecutionPolicy Bypass -File .\scripts\run-junit.ps1

# 3. 启动 test 后端（8090）
powershell -ExecutionPolicy Bypass -File .\scripts\start-test-backend.ps1

# 4. Postman / newman
powershell -ExecutionPolicy Bypass -File .\scripts\run-postman.ps1

# 5. JMeter 500 并发
powershell -ExecutionPolicy Bypass -File .\scripts\run-jmeter.ps1

# 6. 清理压测数据并停止后端
powershell -ExecutionPolicy Bypass -File .\scripts\cleanup-perf-data.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\stop-test-backend.ps1
```

---

## 五、注意事项

1. **只用测试库**：`application-test.yml` 已指向 `funding_test` / `funding_privacy_test`；请勿把 `baseUrl` 或 JMeter `PORT` 指向演示服务。
2. **执行顺序**：Postman 与 JMeter 需在后端启动后执行；两者前都建议重置测试库，保证断言与结果可重复。
3. **JMeter 报告目录**：`-o` 指向的目录必须不存在或为空；脚本已自动清理。
4. **JMeter 需要 java 在 PATH**：脚本会临时把 `D:\java\bin` 加到 PATH。
5. **工具不入库**：`.tools/`（JMeter、newman）与 `*.log` 已被 `.gitignore` 忽略，仅 `docs/testing/evidence/*.log` 作为证据保留。
6. **性能数据环境**：JMeter、后端、MySQL 同机运行，指标偏保守；生产部署后需按测试报告第 6.5 节复测。
