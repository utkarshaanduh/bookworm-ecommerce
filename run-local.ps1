param(
    [string]$EnvFile = "$PSScriptRoot\..\.env",
    [string]$Mvn = "C:\Users\Utkarsh\Downloads\apache-maven-3.9.16-bin (1)\apache-maven-3.9.16\bin\mvn.cmd"
)
# Reads db_url (postgresql://user:pass@host/db?params) from .env and starts the app.
$line = Get-Content $EnvFile | Where-Object { $_ -match '^\s*db_url\s*=' } | Select-Object -First 1
$url = ($line -split '=', 2)[1].Trim().Trim('"').Trim("'")
if ($url -notmatch '^postgres(ql)?://([^:]+):([^@]+)@([^/]+)/([^?]+)') { throw "db_url not in expected format" }
$env:DB_USER = [uri]::UnescapeDataString($Matches[2])
$env:DB_PASSWORD = [uri]::UnescapeDataString($Matches[3])
$env:DB_URL = "jdbc:postgresql://$($Matches[4])/$($Matches[5])?sslmode=require"
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
Set-Location $PSScriptRoot
& $Mvn -B spring-boot:run
