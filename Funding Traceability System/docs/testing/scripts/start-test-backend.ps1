param(
    [string]$JavaExe = 'D:\java\bin\java.exe',
    [string]$MvnExe = 'D:\apache-maven-3.9.6\bin\mvn.cmd',
    [int]$Port = 8090,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$backend = Join-Path $repo 'backend'
$jar = Join-Path $backend 'target\funding-traceability-1.0.0.jar'
$pom = Join-Path $backend 'pom.xml'

if (-not (Test-Path $JavaExe)) { throw "找不到 java.exe：$JavaExe" }

if ($Rebuild -or -not (Test-Path $jar)) {
    Write-Host '[1/2] 构建后端（跳过单元测试）...'
    & $MvnExe -B -f $pom package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败' }
}

if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "端口 $Port 已被占用，请先执行 stop-test-backend.ps1"
}

New-Item -ItemType Directory -Force -Path (Join-Path $backend 'logs') | Out-Null

Write-Host "[2/2] 启动 test profile（端口 $Port）..."
# 使用相对 jar 路径，避免 Windows 路径空格被 Start-Process 拆分
$process = Start-Process -FilePath $JavaExe `
    -ArgumentList @('-jar', 'target\funding-traceability-1.0.0.jar', '--spring.profiles.active=test') `
    -WorkingDirectory $backend -PassThru

for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$Port/api/auth/login" -Method POST `
            -ContentType 'application/json' `
            -Body '{"username":"sponsor","password":"123456"}' `
            -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) {
            Write-Host "后端已就绪：http://localhost:$Port（PID=$($process.Id)，日志 backend\logs\backend-test.log）"
            return
        }
    } catch {
        # 启动中，继续等待
    }
}

throw '后端启动超时，请查看 backend\logs\backend-test.log 与 backend\logs\backend-test-console.*.log'
