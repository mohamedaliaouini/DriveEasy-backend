param(
    [switch]$SkipZipkin,
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'use-driveeasy-tools.ps1')

$order = @('service-registry', 'config-server', 'vehicule-service', 'client-service', 'location-service', 'api-gateway')
foreach ($name in $order) {
    $jar = Join-Path $root "$name/target/$name-1.0.0-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "JAR manquant : $jar. Lance d'abord .\tools\build-backend.ps1"
    }
}

function Test-Endpoint([string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Test-PortListening([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync('127.0.0.1', $Port)
        return $task.Wait(1000) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-Endpoint([string]$Url, [string]$Label) {
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        if (Test-Endpoint $Url) {
            Write-Host "$Label est prêt."
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Délai dépassé en attendant $Label ($Url). Vérifie la fenêtre PowerShell du service."
}

if (-not $SkipZipkin) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        & $docker.Source info *> $null
        if ($LASTEXITCODE -eq 0) {
            & (Join-Path $PSScriptRoot 'start-zipkin.ps1')
        } else {
            Write-Warning 'Docker Desktop ne répond pas. Zipkin sera ignoré; tu peux lancer le projet sans les traces.'
        }
    } else {
        Write-Warning 'Docker est absent. Zipkin sera ignoré.'
    }
}

foreach ($name in $order) {
    $healthUrl = switch ($name) {
        'service-registry' { 'http://localhost:8761/' }
        'config-server' { 'http://localhost:8888/actuator/health' }
        'vehicule-service' { 'http://localhost:8081/actuator/health' }
        'client-service' { 'http://localhost:8082/actuator/health' }
        'location-service' { 'http://localhost:8083/actuator/health' }
        'api-gateway' { 'http://localhost:8080/actuator/health' }
    }
    if (-not (Test-Endpoint $healthUrl) -and -not (Test-PortListening ([uri]$healthUrl).Port)) {
        $command = "Set-Location -LiteralPath '$root'; & '$PSScriptRoot\start-service.ps1' -Name '$name'"
        Start-Process -FilePath 'powershell.exe' -WindowStyle Normal -ArgumentList @('-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $command) -WorkingDirectory $root
    } elseif (-not (Test-Endpoint $healthUrl)) {
        Write-Warning "$name a déjà son port ouvert mais son URL de santé n'est pas prête. J'attends le processus existant sans en lancer un deuxième."
    }
    Wait-Endpoint $healthUrl $name
}

if (-not $SkipFrontend) {
    if (-not (Test-Endpoint 'http://localhost:4200/')) {
        Start-Process -FilePath 'powershell.exe' -WindowStyle Normal -ArgumentList @('-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', "Set-Location -LiteralPath '$root'; & '$PSScriptRoot\start-frontend.ps1'") -WorkingDirectory $root
    }
    Wait-Endpoint 'http://localhost:4200/' 'frontend Angular'
}

Write-Host 'Démarrage envoyé. Vérifie Eureka sur http://localhost:8761, Angular sur http://localhost:4200 et Zipkin sur http://localhost:9411 si Docker a démarré.'
