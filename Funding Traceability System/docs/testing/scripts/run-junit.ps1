param(
    [string]$MvnExe = 'D:\apache-maven-3.9.6\bin\mvn.cmd'
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backend = Join-Path $repo 'backend'
$pom = Join-Path $backend 'pom.xml'
$log = Join-Path $backend 'target\junit-test-run.log'
$evidence = Join-Path $repo 'docs\testing\evidence'

New-Item -ItemType Directory -Force -Path (Split-Path $log -Parent) | Out-Null
New-Item -ItemType Directory -Force -Path $evidence | Out-Null

Write-Host '[1/2] 执行 JUnit 单元测试（Mock，不连数据库）...'
& $MvnExe -B -f $pom test 2>&1 | Tee-Object -FilePath $log
if ($LASTEXITCODE -ne 0) { throw "单元测试失败，详见 $log" }

Write-Host '[2/2] 生成 Surefire HTML 报告 ...'
& $MvnExe -B -f $pom surefire-report:report-only -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Warning 'Surefire 报告生成失败，可跳过（不影响测试结果）' }

Copy-Item $log (Join-Path $evidence 'junit-test-run.log') -Force
$site = Join-Path $backend 'target\reports'
if (Test-Path $site) {
    $dest = Join-Path $evidence 'junit-report'
    Remove-Item $dest -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item $site $dest -Recurse -Force
}

Write-Host "完成：日志 $log；报告 backend\target\reports\surefire.html；证据已复制到 docs/testing/evidence/"
