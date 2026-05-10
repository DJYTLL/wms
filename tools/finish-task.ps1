param(
    [Parameter(Mandatory = $true)]
    [string]$Message,
    [switch]$SkipCheck
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Text)
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Fail {
    param([string]$Text)
    Write-Error $Text
    exit 1
}

function Get-RepoRoot {
    $root = git rev-parse --show-toplevel 2>$null
    if (-not $root) {
        Fail "当前目录不在 Git 仓库中，无法执行业务收口。"
    }
    return $root.Trim()
}

function Test-ChineseCommitMessage {
    param([string]$CommitMessage)

    $trimmed = $CommitMessage.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) {
        Fail "提交信息不能为空。"
    }
    if ($trimmed -match "[\r\n]") {
        Fail "提交信息必须是单行中文业务描述。"
    }
    if ($trimmed.Length -lt 4 -or $trimmed.Length -gt 30) {
        Fail "提交信息长度建议控制在 4 到 30 个字符之间。"
    }
    if ($trimmed -notmatch "[\u4e00-\u9fff]") {
        Fail "提交信息必须包含中文。"
    }
    if ($trimmed -match "^(feat|fix|chore|docs|style|refactor|test|perf)(\(.+\))?:") {
        Fail "提交信息应使用纯中文动宾短句，不要使用英文前缀。"
    }

    $blockedMessages = @(
        "修改一下",
        "更新代码",
        "保存当前改动",
        "临时提交",
        "随手提交"
    )
    if ($blockedMessages -contains $trimmed) {
        Fail "提交信息过于模糊，请改为明确的中文业务描述。"
    }
}

function Get-StagedFiles {
    $lines = git diff --cached --name-only --diff-filter=ACMR
    return @($lines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Get-StagedAddedFiles {
    $lines = git diff --cached --name-only --diff-filter=A
    return @($lines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Invoke-CheckedCommand {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Step $Label
    & $Command
    if ($LASTEXITCODE -ne 0) {
        Fail "$Label 失败，请先修复后再提交。"
    }
}

function Get-LocalBinCommand {
    param(
        [string]$WorkingDirectory,
        [string]$CommandName
    )

    $candidates = @(
        (Join-Path $WorkingDirectory "node_modules\.bin\$CommandName.cmd"),
        (Join-Path $WorkingDirectory "node_modules\.bin\$CommandName.ps1"),
        (Join-Path $WorkingDirectory "node_modules\.bin\$CommandName")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    Fail "未找到前端校验命令 $CommandName，请先安装依赖后再执行收口脚本。"
}

$repoRoot = Get-RepoRoot
Set-Location $repoRoot

Write-Step "检查提交信息"
Test-ChineseCommitMessage -CommitMessage $Message

Write-Step "读取已暂存文件"
$stagedFiles = Get-StagedFiles
if ($stagedFiles.Count -eq 0) {
    Fail "当前没有已暂存文件，请先使用 git add -p 或显式文件路径暂存本次业务改动。"
}

Write-Host "本次提交仅包含以下已暂存文件：" -ForegroundColor Yellow
$stagedFiles | ForEach-Object { Write-Host " - $_" }

$hasReadme = $stagedFiles -contains "README.md"
if (-not $hasReadme) {
    Fail "本次提交未包含根 README.md，请先补充业务变更记录并暂存后再提交。"
}

$codeFiles = @($stagedFiles | Where-Object { $_ -ne "README.md" })
if ($codeFiles.Count -eq 0) {
    Write-Warning "当前仅暂存了 README.md，请确认本次业务确实属于纯文档变更。"
}

if ($stagedFiles.Count -gt 15) {
    Write-Warning "本次暂存文件较多，请确认没有混入其他业务。建议优先使用 git add -p 分批提交。"
}

$generatedPatterns = @(
    "^auto-parts-wms-vue/dist/",
    "^wms-backend/target/",
    "^auto-parts-wms-vue/auto-imports\.d\.ts$",
    "^auto-parts-wms-vue/components\.d\.ts$"
)
$generatedFiles = @(
    $stagedFiles | Where-Object {
        $path = $_
        $generatedPatterns | Where-Object { $path -match $_ }
    }
)
if ($generatedFiles.Count -gt 0) {
    Write-Warning "检测到可能的生成文件已暂存，请确认这些文件确实需要纳入本次业务提交："
    $generatedFiles | ForEach-Object { Write-Host " - $_" -ForegroundColor DarkYellow }
}

$touchesFrontend = @($stagedFiles | Where-Object { $_ -like "auto-parts-wms-vue/*" }).Count -gt 0
$touchesBackend = @($stagedFiles | Where-Object { $_ -like "wms-backend/*" }).Count -gt 0
$stagedAddedFiles = Get-StagedAddedFiles
$frontendLintTargets = @(
    $stagedAddedFiles | Where-Object {
        $_ -like "auto-parts-wms-vue/*" -and $_ -match "\.(vue|ts|tsx|js|jsx|mjs|cjs)$"
    } | ForEach-Object {
        $_.Substring("auto-parts-wms-vue/".Length)
    }
)

if (-not $SkipCheck) {
    if ($touchesFrontend) {
        Push-Location (Join-Path $repoRoot "auto-parts-wms-vue")
        try {
            $eslintCommand = Get-LocalBinCommand -WorkingDirectory (Get-Location) -CommandName "eslint"
            $vueTscCommand = Get-LocalBinCommand -WorkingDirectory (Get-Location) -CommandName "vue-tsc"
            if ($frontendLintTargets.Count -gt 0) {
                Invoke-CheckedCommand -Label "执行新增前端文件 ESLint 检查" -Command { & $eslintCommand @frontendLintTargets --cache --max-warnings=0 }
            }
            else {
                Write-Warning "当前没有新增前端文件；已跳过整文件 ESLint，避免被历史存量问题阻塞。"
            }
            Invoke-CheckedCommand -Label "执行前端类型检查" -Command { & $vueTscCommand --build }
        }
        finally {
            Pop-Location
        }
    }

    if ($touchesBackend) {
        Push-Location (Join-Path $repoRoot "wms-backend")
        try {
            Invoke-CheckedCommand -Label "执行后端编译检查" -Command { mvn -q -DskipTests compile }
        }
        finally {
            Pop-Location
        }
    }
}
else {
    Write-Warning "已显式跳过校验，请确认这是有意识的例外操作。"
}

Write-Step "提交已暂存内容"
git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    Fail "Git 提交失败。"
}

Write-Host ""
Write-Host "业务收口完成。" -ForegroundColor Green
