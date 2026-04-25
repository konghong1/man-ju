$ErrorActionPreference = "Stop"

Write-Host "[init] running tests"
mvn -q test

Write-Host "[init] starting app for health check"
$process = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -WindowStyle Hidden

try {
    Start-Sleep -Seconds 14
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
    $health | ConvertTo-Json -Depth 4
    Write-Host "[init] done"
}
finally {
    if ($null -ne $process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
}
