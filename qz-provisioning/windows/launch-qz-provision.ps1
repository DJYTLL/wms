[CmdletBinding()]
param(
    [string]$InstallDir
)

$ErrorActionPreference = 'Stop'

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Select-InstallDirectory {
    param([string]$DefaultPath)

    Add-Type -AssemblyName System.Windows.Forms
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = '请选择 QZ Tray 安装目录'
    if ($DefaultPath) {
        $dialog.SelectedPath = $DefaultPath
    }
    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        return $dialog.SelectedPath
    }
    return $null
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$installScript = Join-Path $scriptDir 'install-qz-provision.ps1'

if (-not $InstallDir) {
    $InstallDir = Select-InstallDirectory -DefaultPath (Join-Path $env:ProgramFiles 'QZ Tray')
}

if (-not $InstallDir) {
    Write-Host '未选择 QZ Tray 安装目录，安装已取消。' -ForegroundColor Yellow
    Read-Host '按回车关闭窗口'
    exit 1
}

if (-not (Test-Administrator)) {
    $command = "& '$installScript' -InstallDir '$InstallDir'; Write-Host 'QZ provisioning 执行完成。' -ForegroundColor Green; Read-Host '按回车关闭窗口'"
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList @(
        '-NoExit',
        '-ExecutionPolicy', 'Bypass',
        '-Command', $command
    )
    exit 0
}

try {
    & $installScript -InstallDir $InstallDir
    Write-Host 'QZ provisioning 执行完成。' -ForegroundColor Green
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Read-Host '按回车关闭窗口'
