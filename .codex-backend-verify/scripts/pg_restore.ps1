param(
  [string]$Host = "localhost",
  [int]$Port = 5432,
  [string]$Database = "wms_backend",
  [string]$User = "postgres",
  [string]$Input = ""
)

if ([string]::IsNullOrWhiteSpace($Input)) {
  Write-Host "Please provide -Input <backup_file>"
  exit 1
}

if (-not $env:PGPASSWORD) {
  Write-Host "PGPASSWORD not set. Please set env var before running (e.g. `$env:PGPASSWORD='your_password')"
  exit 1
}

pg_restore -h $Host -p $Port -U $User -d $Database -c $Input
