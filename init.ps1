$ErrorActionPreference = "Stop"

Write-Host "[init] running backend tests"
mvn -q test

Write-Host "[init] building frontend"
Push-Location "frontend"
npm ci
npm run build
Pop-Location

Write-Host "[init] starting backend"
$backendProcess = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -WindowStyle Hidden
Write-Host "[init] starting frontend preview"
$frontendProcess = Start-Process -FilePath "npm" -ArgumentList "run","preview","--","--host","127.0.0.1","--port","4173" -WorkingDirectory (Resolve-Path "frontend") -PassThru -WindowStyle Hidden

try {
    Start-Sleep -Seconds 18
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
    $frontend = Invoke-WebRequest -Uri "http://127.0.0.1:4173" -UseBasicParsing
    $health | ConvertTo-Json -Depth 4
    Write-Host "[init] frontend status: $($frontend.StatusCode)"
    Write-Host "[init] done"
}
finally {
    if ($null -ne $backendProcess -and -not $backendProcess.HasExited) {
        Stop-Process -Id $backendProcess.Id -Force
    }
    if ($null -ne $frontendProcess -and -not $frontendProcess.HasExited) {
        Stop-Process -Id $frontendProcess.Id -Force
    }
}
