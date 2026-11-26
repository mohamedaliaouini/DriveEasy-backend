param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('service-registry', 'config-server', 'vehicule-service', 'client-service', 'location-service', 'api-gateway')]
    [string]$Name
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'use-driveeasy-tools.ps1')
Set-Location $root
$jar = Join-Path $root "$Name/target/$Name-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jar)) {
    throw "JAR introuvable : $jar. Lance d'abord .\tools\build-backend.ps1"
}
$localJava = Join-Path $PSScriptRoot 'jdk-21.0.13+9/bin/java.exe'
if (Test-Path -LiteralPath $localJava) { & $localJava -jar $jar } else { & java -jar $jar }
