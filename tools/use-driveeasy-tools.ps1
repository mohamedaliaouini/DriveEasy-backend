# Charge les outils contenus dans ce dossier pour le terminal PowerShell actuel.
$driveEasyRoot = Split-Path -Parent $PSScriptRoot
$bundledJdk = Join-Path $PSScriptRoot 'jdk-21.0.13+9'
$bundledMaven = Join-Path $PSScriptRoot 'apache-maven-3.9.16/bin'
$bundledNode = Join-Path $PSScriptRoot 'node-v26.13.0-win-x64'
if (Test-Path (Join-Path $bundledJdk 'bin/java.exe')) {
    $env:JAVA_HOME = $bundledJdk
    $env:PATH = "$($bundledJdk)/bin;$env:PATH"
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 21 est absent. Installe JDK 21 et réessaie.'
}
if (Test-Path (Join-Path $bundledMaven 'mvn.cmd')) { $env:PATH = "$bundledMaven;$env:PATH" }
elseif (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven est absent. Installe Maven 3.9+ et réessaie.' }
if (Test-Path (Join-Path $bundledNode 'node.exe')) { $env:PATH = "$bundledNode;$env:PATH" }
elseif (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Node.js 22.12+ est absent. Installe Node.js et réessaie.' }
$env:DRIVEEASY_CURRENT_YEAR = (Get-Date).Year.ToString()
$env:DRIVEEASY_CONFIG_GIT_URI = ([System.Uri]::new($driveEasyRoot)).AbsoluteUri
$env:NPM_CONFIG_CACHE = Join-Path $driveEasyRoot '.npm'
$env:MAVEN_OPTS = "-Dmaven.repo.local=$(Join-Path $driveEasyRoot '.m2/repository')"
Write-Host "DriveEasy utilise Java $(& java -version 2>&1 | Select-Object -First 1), Maven et Node.js $(& node --version) dans ce terminal."
Write-Host 'La configuration centralisée est chargée via Config Server depuis le dépôt Git local (config-repo/).'

