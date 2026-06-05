param(
    [string]$InputPath = "C:\Users\Administrator\AppData\Local\Temp\codex-xls-work\inventory_input_exact.xls",
    [string]$OutputPath = "D:\project\outputs\inventory_dedup_product_name_20260602\inventory_dedup_by_product_name.xlsx"
)

$ErrorActionPreference = "Stop"

$ProductNameCol = 3
$LastInboundTimeCol = 34

function Convert-ToDateTimeOrNull {
    param($Value)

    if ($null -eq $Value -or $Value -eq "") {
        return $null
    }

    if ($Value -is [double] -or $Value -is [int]) {
        try {
            return [datetime]::FromOADate([double]$Value)
        } catch {
            return $null
        }
    }

    try {
        return [datetime]::Parse([string]$Value)
    } catch {
        return $null
    }
}

function Get-Score {
    param($DateValue)

    if ($null -eq $DateValue) {
        return [datetime]::MinValue
    }

    return $DateValue
}

$excel = $null
$inputWorkbook = $null
$outputWorkbook = $null

try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false

    $inputWorkbook = $excel.Workbooks.Open($InputPath, 0, $true)
    $inputSheet = $inputWorkbook.Worksheets.Item(1)
    $inputUsedRange = $inputSheet.UsedRange
    $inputLastRow = $inputUsedRange.Row + $inputUsedRange.Rows.Count - 1

    $outputWorkbook = $excel.Workbooks.Open($OutputPath, 0, $true)
    $outputSheet = $outputWorkbook.Worksheets.Item(1)
    $outputUsedRange = $outputSheet.UsedRange
    $outputLastRow = $outputUsedRange.Row + $outputUsedRange.Rows.Count - 1

    $inputBestByName = @{}
    $inputRowsByName = @{}

    for ($row = 2; $row -le $inputLastRow; $row++) {
        $nameValue = $inputSheet.Cells.Item($row, $ProductNameCol).Value2
        if ($null -eq $nameValue -or [string]::IsNullOrWhiteSpace([string]$nameValue)) {
            continue
        }
        $name = [string]$nameValue
        $score = Get-Score (Convert-ToDateTimeOrNull $inputSheet.Cells.Item($row, $LastInboundTimeCol).Value2)

        if (-not $inputRowsByName.ContainsKey($name)) {
            $inputRowsByName[$name] = 0
        }
        $inputRowsByName[$name]++

        if (-not $inputBestByName.ContainsKey($name) -or $score -gt $inputBestByName[$name].Score) {
            $inputBestByName[$name] = [pscustomobject]@{
                Score = $score
                TimeValue = $inputSheet.Cells.Item($row, $LastInboundTimeCol).Text
            }
        }
    }

    $outputRowsByName = @{}
    $badMatches = New-Object System.Collections.ArrayList

    for ($row = 2; $row -le $outputLastRow; $row++) {
        $nameValue = $outputSheet.Cells.Item($row, $ProductNameCol).Value2
        if ($null -eq $nameValue -or [string]::IsNullOrWhiteSpace([string]$nameValue)) {
            continue
        }
        $name = [string]$nameValue
        if (-not $outputRowsByName.ContainsKey($name)) {
            $outputRowsByName[$name] = 0
        }
        $outputRowsByName[$name]++

        if ($inputRowsByName.ContainsKey($name) -and $inputRowsByName[$name] -gt 1) {
            $actualScore = Get-Score (Convert-ToDateTimeOrNull $outputSheet.Cells.Item($row, $LastInboundTimeCol).Value2)
            if ($actualScore -ne $inputBestByName[$name].Score) {
                [void]$badMatches.Add([pscustomobject]@{
                    ProductName = $name
                    ExpectedTime = $inputBestByName[$name].TimeValue
                    ActualTime = $outputSheet.Cells.Item($row, $LastInboundTimeCol).Text
                })
            }
        }
    }

    $duplicateNamesRemaining = @($outputRowsByName.Keys | Where-Object { $outputRowsByName[$_] -gt 1 })

    [ordered]@{
        input_data_rows = $inputLastRow - 1
        output_data_rows = $outputLastRow - 1
        duplicate_names_remaining = $duplicateNamesRemaining.Count
        checked_duplicate_groups = @($inputRowsByName.Keys | Where-Object { $inputRowsByName[$_] -gt 1 }).Count
        bad_retained_time_matches = $badMatches.Count
    } | ConvertTo-Json -Depth 4
} finally {
    if ($null -ne $outputWorkbook) {
        $outputWorkbook.Close($false)
    }
    if ($null -ne $inputWorkbook) {
        $inputWorkbook.Close($false)
    }
    if ($null -ne $excel) {
        $excel.Quit()
    }
}
