$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'use-driveeasy-tools.ps1')
$node = Join-Path $PSScriptRoot 'node-v26.13.0-win-x64/node.exe'
$npmCli = Join-Path $PSScriptRoot 'node-v26.13.0-win-x64/node_modules/npm/bin/npm-cli.js'
$frontend = Join-Path $root 'frontend'
if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules'))) {
    throw "Dépendances Angular absentes. Dans frontend, lance npm install puis réessaie."
}
Set-Location $frontend
if ((Test-Path -LiteralPath $node) -and (Test-Path -LiteralPath $npmCli)) { & $node $npmCli run start }
else { & npm run start }
