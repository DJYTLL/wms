[CmdletBinding()]
param(
    [string]$InstallDir = "$env:ProgramFiles\\QZ Tray",
    [switch]$SkipRestart
)

$ErrorActionPreference = 'Stop'

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw '请以管理员身份运行此脚本。'
    }
}

function Stop-QzProcesses {
    Get-Process | Where-Object {
        $_.Path -and $_.Path.StartsWith($InstallDir, [System.StringComparison]::OrdinalIgnoreCase)
    } | Stop-Process -Force -ErrorAction SilentlyContinue
}

Assert-Administrator

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$provisionDir = Join-Path $InstallDir 'provision'
$qzConsole = Join-Path $InstallDir 'qz-tray-console.exe'
$qzGui = Join-Path $InstallDir 'qz-tray.exe'

if (-not (Test-Path $qzConsole)) {
    throw "未找到 QZ Tray 控制台程序：$qzConsole"
}

New-Item -ItemType Directory -Force -Path $provisionDir | Out-Null

Copy-Item (Join-Path $scriptDir 'provision.json') $provisionDir -Force
Copy-Item (Join-Path $scriptDir 'digital-certificate.txt') $provisionDir -Force
Copy-Item (Join-Path $scriptDir 'self-signed.crt') $provisionDir -Force

Stop-QzProcesses

Write-Host '触发 QZ certgen 阶段...'
& $qzConsole certgen

Write-Host '写入当前用户 allowed.dat ...'
& $qzConsole --allow (Join-Path $provisionDir 'digital-certificate.txt')

if (-not $SkipRestart) {
    Write-Host '重启 QZ Tray ...'
    Start-Process -FilePath $qzGui
}

Write-Host 'QZ Tray provisioning 安装完成。'
