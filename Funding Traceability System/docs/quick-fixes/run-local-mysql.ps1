param(
    [string]$MySqlExe = 'mysql',
    [string]$MvnExe = 'mvn',
    [string]$JavaExe = 'java',
    [string]$NodeExe = 'node',
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 3306,
    [int]$Port = 18090,
    [switch]$KeepTestDatabases
)
$ErrorActionPreference = 'Stop'
if ($DbHost -notin @('127.0.0.1', 'localhost')) { throw '此脚本仅用于本机MySQL；不连接远程或生产服务。' }
foreach ($tool in @($MySqlExe, $MvnExe, $JavaExe, $NodeExe)) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "找不到程序：$tool，请设置相应Exe参数或PATH。" }
}
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "端口$Port已占用，请换一个Port；脚本不会停止已有服务。" }
$project = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backend = Join-Path $project 'backend'
$schemaPath = Join-Path $backend 'src\main\resources\sql\init_test_db.sql'
$smokePath = Join-Path $PSScriptRoot 'database-smoke.mjs'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$suffix = [guid]::NewGuid().ToString('N').Substring(0,6)
$testDb = "funding_quickfix_${stamp}_${suffix}"
$privacyDb = "${testDb}_privacy"
if ($testDb -notmatch '^funding_quickfix_[a-z0-9_]+$') { throw '测试库名异常' }
$credential = Get-Credential -UserName 'root' -Message '输入本机MySQL账号（需创建/删除独立测试库权限）；密码只在本机使用。'
if ($null -eq $credential) { throw '未提供本机凭据。' }
$names = @('MYSQL_PWD','DB_USERNAME','DB_PASSWORD','TEST_DB_URL','TEST_PRIVACY_DB_URL','QUICKFIX_BASE_URL','QUICKFIX_DB','QUICKFIX_DB_HOST','QUICKFIX_DB_PORT','QUICKFIX_MYSQL_EXE','QUICKFIX_DOCKER_CONTAINER','QUICKFIX_RESULTS')
$originalEnv = @{}
foreach ($name in $names) { $originalEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$oldEncoding = $OutputEncoding
$backendProcess = $null
$created = $false
$output = Join-Path $project "docs\testing\evidence\quickfix-$stamp-$suffix"
function Invoke-TestSql([string]$Sql) {
    $mysqlArgs = @("--host=$DbHost", "--port=$DbPort", '--protocol=TCP', "--user=$($credential.UserName)", '--default-character-set=utf8mb4', '--batch', '--skip-column-names')
    $response = $Sql | & $MySqlExe @mysqlArgs
    if ($LASTEXITCODE -ne 0) { throw '本机独立测试库SQL执行失败。' }
    return $response
}
try {
    $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    $env:MYSQL_PWD = $credential.GetNetworkCredential().Password
    $env:DB_USERNAME = $credential.UserName
    $env:DB_PASSWORD = $env:MYSQL_PWD
    $count = Invoke-TestSql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('$testDb','$privacyDb');"
    if ([int]($count | Select-Object -Last 1) -ne 0) { throw '测试库名已存在，停止执行，不覆盖。' }
    $schema = Get-Content -Raw -Encoding UTF8 $schemaPath
    $schema = $schema.Replace('funding_privacy_test', $privacyDb).Replace('funding_test', $testDb)
    # 包含DROP的初始化脚本已全部替换为本次生成的唯一库名，不触及演示库或既有测试库。
    $created = $true
    Invoke-TestSql $schema | Out-Null
    Write-Host "已创建独立测试库：$testDb / $privacyDb"
    New-Item -ItemType Directory -Force $output | Out-Null
    $testBuild = Join-Path $output 'build'
    New-Item -ItemType Directory -Force $testBuild | Out-Null
    Copy-Item -LiteralPath (Join-Path $backend 'pom.xml') -Destination $testBuild
    Copy-Item -LiteralPath (Join-Path $backend 'src') -Destination $testBuild -Recurse
    # 在独立目录构建，避免重写已运行服务正在使用的target/JAR。
    & $MvnExe -B -f (Join-Path $testBuild 'pom.xml') package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败。' }
    $env:TEST_DB_URL = "jdbc:mysql://${DbHost}:${DbPort}/${testDb}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
    $env:TEST_PRIVACY_DB_URL = "jdbc:mysql://${DbHost}:${DbPort}/${privacyDb}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
    $env:QUICKFIX_BASE_URL = "http://127.0.0.1:$Port"
    $env:QUICKFIX_DB = $testDb
    $env:QUICKFIX_DB_HOST = $DbHost
    $env:QUICKFIX_DB_PORT = "$DbPort"
    $env:QUICKFIX_MYSQL_EXE = $MySqlExe
    Remove-Item Env:QUICKFIX_DOCKER_CONTAINER -ErrorAction SilentlyContinue
    $env:QUICKFIX_RESULTS = Join-Path $output 'database-results.json'
    $backendProcess = Start-Process -FilePath $JavaExe -ArgumentList @('-Duser.timezone=Asia/Shanghai', '-jar', 'target\funding-traceability-1.0.0.jar', '--spring.profiles.active=test', "--server.port=$Port", '--server.address=127.0.0.1', '--funding.mybatis.stdout-sql-log=false') -WorkingDirectory $testBuild -PassThru -RedirectStandardOutput (Join-Path $output 'backend.log') -RedirectStandardError (Join-Path $output 'backend-error.log')
    $ready = $false
    for ($i=0; $i -lt 60; $i++) {
        if ($backendProcess.HasExited) { throw "后端退出，请在本机查看$output中的日志。" }
        try {
            $response = Invoke-RestMethod -Uri "$env:QUICKFIX_BASE_URL/api/auth/login" -Method Post -ContentType 'application/json' -Body '{"username":"sponsor","password":"123456"}' -TimeoutSec 2
            if ($response.code -eq 200) { $ready=$true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw '测试后端启动超时。' }
    & $NodeExe $smokePath
    if ($LASTEXITCODE -ne 0) { throw '数据库回归失败，查看本机输出；不要发送数据库密码。' }
    $env:QUICKFIX_RESULTS = Join-Path $output 'round2-results.json'
    & $NodeExe (Join-Path $PSScriptRoot 'round2-database-smoke.mjs')
    if ($LASTEXITCODE -ne 0) { throw '第二轮功能数据库回归失败，请查看本机输出。' }
    Write-Host "两轮专项测试通过，结果保存在：$output"
} finally {
    if ($null -ne $backendProcess -and -not $backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue }
    if ($created -and -not $KeepTestDatabases) {
        try { Invoke-TestSql "DROP DATABASE IF EXISTS $testDb; DROP DATABASE IF EXISTS $privacyDb;" | Out-Null }
        catch { Write-Warning "自动清理失败，请仅手工清理本次独立库：$testDb / $privacyDb" }
    }
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $originalEnv[$name], 'Process') }
    $OutputEncoding = $oldEncoding
}
