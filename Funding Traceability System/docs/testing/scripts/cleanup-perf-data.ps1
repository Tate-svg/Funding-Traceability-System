param(
    [string]$MysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $MysqlExe)) { throw "找不到 mysql.exe：$MysqlExe" }

$env:MYSQL_PWD = $DbPassword
try {
    Write-Host '清理压测产生的申请单（保留种子基线）...'
    & $MysqlExe -u $DbUser -e "delete from funding_test.privacy_access_apply where reason like '性能压测申请%'; select count(*) applies_left from funding_test.privacy_access_apply;"
    if ($LASTEXITCODE -ne 0) { throw '清理压测数据失败' }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
