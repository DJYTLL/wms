param(
    [string]$InputPath = "C:\Users\Administrator\AppData\Local\Temp\codex-xls-work\inventory_input_exact.xls",
    [string]$OutputDir = "D:\project\outputs\inventory_dedup_product_name_20260602"
)

$ErrorActionPreference = "Stop"

$ProductNameCol = 3
$LastInboundTimeCol = 34
$XlFileFormatXlsx = 51

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

function Get-TimestampScore {
    param($DateValue)

    if ($null -eq $DateValue) {
        return [datetime]::MinValue
    }

    return $DateValue
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$outputPath = Join-Path $OutputDir "inventory_dedup_by_product_name.xlsx"
$summaryPath = Join-Path $OutputDir "summary.json"

$excel = $null
$workbook = $null

try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false

    $workbook = $excel.Workbooks.Open($InputPath, 0, $false)
    $sheet = $workbook.Worksheets.Item(1)
    $usedRange = $sheet.UsedRange
    $lastRow = $usedRange.Row + $usedRange.Rows.Count - 1

    $bestByName = @{}
    $allRowsByName = @{}

    for ($row = 2; $row -le $lastRow; $row++) {
        $nameValue = $sheet.Cells.Item($row, $ProductNameCol).Value2
        if ($null -eq $nameValue -or [string]::IsNullOrWhiteSpace([string]$nameValue)) {
            continue
        }

        $name = [string]$nameValue
        $dateValue = Convert-ToDateTimeOrNull $sheet.Cells.Item($row, $LastInboundTimeCol).Value2
        $score = Get-TimestampScore $dateValue

        if (-not $allRowsByName.ContainsKey($name)) {
            $allRowsByName[$name] = New-Object System.Collections.ArrayList
        }
        [void]$allRowsByName[$name].Add($row)

        if (
            -not $bestByName.ContainsKey($name) -or
            $score -gt $bestByName[$name].Score
        ) {
            $bestByName[$name] = [pscustomobject]@{
                Row = $row
                Score = $score
            }
        }
    }

    $rowsToDelete = New-Object System.Collections.ArrayList
    $duplicateGroupCount = 0

    foreach ($name in $allRowsByName.Keys) {
        $rows = $allRowsByName[$name]
        if ($rows.Count -le 1) {
            continue
        }

        $duplicateGroupCount++
        $keepRow = $bestByName[$name].Row
        foreach ($row in $rows) {
            if ($row -ne $keepRow) {
                [void]$rowsToDelete.Add($row)
            }
        }
    }

    $rowsToDelete = @($rowsToDelete | Sort-Object -Descending)
    foreach ($row in $rowsToDelete) {
        [void]$sheet.Rows.Item($row).Delete()
    }

    if (Test-Path -LiteralPath $outputPath) {
        Remove-Item -LiteralPath $outputPath -Force
    }

    $workbook.SaveAs($outputPath, $XlFileFormatXlsx)

    $summary = [ordered]@{
        input_data_rows = $lastRow - 1
        output_data_rows = ($lastRow - 1) - $rowsToDelete.Count
        removed_rows = $rowsToDelete.Count
        duplicate_product_name_groups = $duplicateGroupCount
        product_name_column_index = $ProductNameCol
        last_inbound_time_column_index = $LastInboundTimeCol
        output_path = $outputPath
    }

    $summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
    $summary | ConvertTo-Json -Depth 4
} finally {
    if ($null -ne $workbook) {
        $workbook.Close($false)
    }
    if ($null -ne $excel) {
        $excel.Quit()
    }
}
