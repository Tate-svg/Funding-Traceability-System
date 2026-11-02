param(
    [int]$Threads = 500,
    [int]$Loops = 5,
    [int]$RampUp = 2,
    [string]$JavaHome = 'D:\java',
    [string]$JmeterHome = ''
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
if ($JmeterHome -eq '') { $JmeterHome = Join-Path $repo '.tools\apache-jmeter-5.6.3' }
$jmx = Join-Path $repo 'docs\testing\jmeter\Funding-Traceability-Performance.jmx'
$evidence = Join-Path $repo 'docs\testing\evidence'
$jtl = Join-Path $evidence 'jmeter-result.jtl'
$jlog = Join-Path $evidence 'jmeter.log'
$report = Join-Path $evidence 'jmeter-report'

$jmeter = Join-Path $JmeterHome 'bin\jmeter.bat'
if (-not (Test-Path $jmeter)) {
    throw "未找到 $jmeter。请按《环境配置说明》第 6.1 节下载 JMeter 5.6.3 并解压到 .tools\apache-jmeter-5.6.3"
}
$javaBin = Join-Path $JavaHome 'bin'
if (-not (Test-Path (Join-Path $javaBin 'java.exe'))) { throw "找不到 Java：$javaBin" }

# jmeter.bat 依赖 PATH 中的 java
$env:PATH = "$javaBin;" + $env:PATH
New-Item -ItemType Directory -Force -Path $evidence | Out-Null

Write-Host "[1/2] 清理旧结果（-o 报告目录必须为空）..."
Remove-Item $report -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $jtl, $jlog -Force -ErrorAction SilentlyContinue

Write-Host "[2/2] 执行压测：$Threads 线程 / $Loops 循环 / ramp-up ${RampUp}s ..."
& $jmeter -n -t $jmx `
    -JTHREADS=$Threads -JLOOPS=$Loops -JRAMP_UP=$RampUp `
    -l $jtl -j $jlog -e -o $report 2>&1 |
    Tee-Object -FilePath (Join-Path $evidence 'jmeter-console.log')

if ($LASTEXITCODE -ne 0) { throw "JMeter 执行失败，详见 $jlog" }

Write-Host '完成：docs/testing/evidence/jmeter-report/index.html'
Write-Host '关键字段：Statistics 中的 Error %、Average、90%/95%/99%、Transactions/s'
