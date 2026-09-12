$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$runtimePath = Join-Path $taskRoot '.runtime'
New-Item -ItemType Directory -Force $runtimePath | Out-Null

function Get-Listener([int]$port) {
    @([System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object Port -eq $port).Count -gt 0
}

$knownProcesses = @{}
$processFile = Join-Path $runtimePath 'processes.json'
if (Test-Path $processFile) { $knownProcesses = Get-Content $processFile -Raw | ConvertFrom-Json }
$backendListener = Get-Listener 8082
$frontendListener = Get-Listener 5174
foreach ($entry in @(@{ Listening = $backendListener; KnownId = $knownProcesses.Backend; Name = 'java'; Port = 8082 }, @{ Listening = $frontendListener; KnownId = $knownProcesses.Frontend; Name = 'node'; Port = 5174 })) {
    if ($entry.Listening) {
        $known = if ($entry.KnownId) { Get-Process -Id $entry.KnownId -ErrorAction SilentlyContinue } else { $null }
        if (-not $known -or $known.ProcessName -ne $entry.Name) { throw "Port $($entry.Port) is already occupied. No process has been stopped." }
    }
}
$backendId = $knownProcesses.Backend
$frontendId = $knownProcesses.Frontend

if (-not $backendListener) {
    Push-Location (Join-Path $taskRoot 'backend')
    try {
        & mvn package -q
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
    } finally { Pop-Location }
    $backendProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList '-jar', 'backend/target/restaurant-management-1.6.0.jar', '--spring.jpa.show-sql=false' -WorkingDirectory $taskRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimePath 'backend.log') -RedirectStandardError (Join-Path $runtimePath 'backend-error.log') -PassThru
    $backendId = $backendProcess.Id
}

if (-not $frontendListener) {
    $frontendPath = Join-Path $taskRoot 'vue3-frontend'
    if (-not (Test-Path (Join-Path $frontendPath 'node_modules/vite/bin/vite.js'))) {
        Push-Location $frontendPath
        try {
            & npm ci
            if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
        } finally { Pop-Location }
    }
    $frontendProcess = Start-Process -FilePath (Get-Command node).Source -ArgumentList 'node_modules/vite/bin/vite.js', '--host', '127.0.0.1' -WorkingDirectory $frontendPath -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimePath 'frontend.log') -RedirectStandardError (Join-Path $runtimePath 'frontend-error.log') -PassThru
    $frontendId = $frontendProcess.Id
}

@{ Backend = $backendId; Frontend = $frontendId } | ConvertTo-Json | Set-Content $processFile
$ready = $false
for ($attempt = 0; $attempt -lt 45; $attempt++) {
    if ((Get-Listener 8082) -and (Get-Listener 5174)) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Startup timed out. Check the logs in .runtime.' }
Write-Host 'Restaurant v1.6 is running: http://127.0.0.1:5174'
Write-Host 'Backend port: 8082. This project does not use port 8081.'
