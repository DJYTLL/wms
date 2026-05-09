param(
  [string]$Host = "localhost",
  [int]$Port = 5432,
  [string]$Database = "wms_backend",
  [string]$User = "postgres",
  [string]$Output = ""
)

if ([string]::IsNullOrWhiteSpace($Output)) {
  $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
  $Output = "backup_$timestamp.dump"
}

if (-not $env:PGPASSWORD) {
  Write-Host "PGPASSWORD not set. Please set env var before running (e.g. `$env:PGPASSWORD='your_password')"
  exit 1
}

pg_dump -h $Host -p $Port -U $User -F c -f $Output $Database
