param(
    [string]$NodeExe = 'node',
    [string]$Folder = ''
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$collection = Join-Path $repo 'docs\testing\postman\Funding-Traceability.postman_collection.json'
$environment = Join-Path $repo 'docs\testing\postman\Funding-Traceability.postman_environment.json'
$newman = Join-Path $repo '.tools\newman\node_modules\newman\bin\newman.js'
$evidence = Join-Path $repo 'docs\testing\evidence'

if (-not (Test-Path $newman)) {
    throw '未找到 newman。请先执行：npm install --prefix .tools/newman newman@6.2.3 newman-reporter-htmlextra'
}
New-Item -ItemType Directory -Force -Path $evidence | Out-Null

$newmanArgs = @(
    $newman, 'run', $collection, '-e', $environment,
    '-r', 'cli,json,htmlextra',
    '--reporter-json-export', (Join-Path $evidence 'postman-result.json'),
    '--reporter-htmlextra-export', (Join-Path $evidence 'postman-report.html'),
    '--reporter-htmlextra-title', '助学帮扶资金溯源系统-接口测试报告'
)
if ($Folder -ne '') { $newmanArgs += @('--folder', $Folder) }

Write-Host '执行 Postman 集合（需先启动 8090 test 后端）...'
& $NodeExe @newmanArgs 2>&1 | Tee-Object -FilePath (Join-Path $evidence 'postman.log')
if ($LASTEXITCODE -ne 0) { throw '接口测试存在失败请求或断言，详见 docs/testing/evidence/postman.log' }

Write-Host '完成：docs/testing/evidence/postman-report.html'
