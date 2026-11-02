param(
    [string]$MysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456'
)

$ErrorActionPreference = 'Stop'

# 仓库根目录：docs/testing/scripts -> 上三级
$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$sql = Join-Path $repo 'backend\src\main\resources\sql\init_test_db.sql'

if (-not (Test-Path $sql)) { throw "找不到初始化脚本：$sql" }
if (-not (Test-Path $MysqlExe)) { throw "找不到 mysql.exe：$MysqlExe" }

Write-Host '[1/2] 重建独立测试库 funding_test / funding_privacy_test ...'
$env:MYSQL_PWD = $DbPassword
try {
    Get-Content $sql -Raw | & $MysqlExe -u $DbUser
    if ($LASTEXITCODE -ne 0) { throw "初始化测试库失败（exit=$LASTEXITCODE）" }

    Write-Host '[2/2] 校验基线数据 ...'
    & $MysqlExe -u $DbUser -e "select (select count(*) from funding_test.sys_user) users,(select count(*) from funding_test.fund) funds,(select count(*) from funding_test.privacy_access_apply) applies,(select count(*) from funding_privacy_test.privacy_profile) profiles;"
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
