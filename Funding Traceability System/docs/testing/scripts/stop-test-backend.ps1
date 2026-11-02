param(
    [int]$Port = 8090
)

$ErrorActionPreference = 'Stop'

$conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $conn) {
    Write-Host "端口 $Port 未监听，无需停止"
    return
}

Write-Host "停止 PID=$($conn.OwningProcess)（端口 $Port）..."
Stop-Process -Id $conn.OwningProcess -Force

for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
        Write-Host "端口 $Port 已停止"
        return
    }
}

throw "端口 $Port 仍未释放，请手工检查进程"
