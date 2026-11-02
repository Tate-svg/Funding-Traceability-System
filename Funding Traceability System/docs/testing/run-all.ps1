param(
    [switch]$SkipJUnit,
    [switch]$SkipPostman,
    [switch]$SkipJmeter,
    [switch]$KeepBackend
)

$ErrorActionPreference = 'Stop'

$scripts = Join-Path $PSScriptRoot 'scripts'
$backendStarted = $false

try {
    Write-Host '===== 1/5 重置独立测试库 ====='
    & (Join-Path $scripts 'reset-test-db.ps1')

    if (-not $SkipJUnit) {
        Write-Host '===== 2/5 JUnit 单元测试 ====='
        & (Join-Path $scripts 'run-junit.ps1')
    }

    Write-Host '===== 3/5 启动 test 后端（8090）====='
    & (Join-Path $scripts 'start-test-backend.ps1')
    $backendStarted = $true

    if (-not $SkipPostman) {
        Write-Host '===== 4/5 Postman 接口测试 ====='
        & (Join-Path $scripts 'run-postman.ps1')
    }

    if (-not $SkipJmeter) {
        Write-Host '===== 5/5 JMeter 500 并发压测 ====='
        & (Join-Path $scripts 'run-jmeter.ps1')
        & (Join-Path $scripts 'cleanup-perf-data.ps1')
    }
} finally {
    if ($backendStarted -and -not $KeepBackend) {
        & (Join-Path $scripts 'stop-test-backend.ps1')
    }
    if (-not $KeepBackend) {
        Write-Host '恢复测试库种子基线 ...'
        & (Join-Path $scripts 'reset-test-db.ps1')
    }
}
