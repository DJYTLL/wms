param(
    [Parameter(Mandatory = $true)]
    [string]$CommitMessageFile
)

$ErrorActionPreference = "Stop"

function Warn {
    param([string]$Text)
    Write-Host "[finish-task reminder] $Text" -ForegroundColor Yellow
}

if (-not (Test-Path -LiteralPath $CommitMessageFile)) {
    exit 0
}

$message = (Get-Content -LiteralPath $CommitMessageFile -Raw).Trim()
$stagedFiles = @(
    git diff --cached --name-only --diff-filter=ACMR |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
)

if ($message -notmatch "[\u4e00-\u9fff]") {
    Warn "提交信息建议使用中文动宾短句，正式提交统一走 tools/finish-task.ps1。"
}

if ($message -match "^(feat|fix|chore|docs|style|refactor|test|perf)(\(.+\))?:") {
    Warn "提交信息建议改为纯中文动宾短句，不再使用英文前缀。"
}

$blockedMessages = @(
    "修改一下",
    "更新代码",
    "保存当前改动",
    "临时提交",
    "随手提交"
)
if ($blockedMessages -contains $message) {
    Warn "当前提交信息过于模糊，建议改为可追踪的业务描述。"
}

if ($stagedFiles -notcontains "README.md") {
    Warn "本次提交未包含根 README.md，请确认已补充业务变更记录。"
}

if ($stagedFiles.Count -gt 15) {
    Warn "本次暂存文件较多，请检查是否混入其他业务，建议优先使用 git add -p。"
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
    Warn "检测到可能的生成文件已暂存，请确认这些文件确实需要提交。"
}

exit 0
