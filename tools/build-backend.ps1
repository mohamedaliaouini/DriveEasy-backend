$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'use-driveeasy-tools.ps1')
$maven = Join-Path $PSScriptRoot 'apache-maven-3.9.16/bin/mvn.cmd'
if (Test-Path -LiteralPath $maven) {
    $mavenArgs = @('-f', (Join-Path $root 'pom.xml'), '-DskipTests', 'package')
    $quoted = ($mavenArgs | ForEach-Object { '"' + $_ + '"' }) -join ' '
    & (Join-Path $env:WINDIR 'System32/cmd.exe') /d /c ('"' + $maven + '" ' + $quoted)
} else {
    & mvn -f (Join-Path $root 'pom.xml') -DskipTests package
}
if ($LASTEXITCODE -ne 0) { throw "La compilation Maven a échoué (code $LASTEXITCODE)." }
