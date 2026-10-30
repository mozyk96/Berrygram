$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot '..\build-with-credentials.ps1'
if (-not (Test-Path $script)) { throw 'Credential build script missing' }
$env:FORK_TELEGRAM_API_ID = '12345'
$env:FORK_TELEGRAM_API_HASH = '0123456789abcdef0123456789abcdef'
& $script -ValidateOnly
if ($LASTEXITCODE -ne 0) { throw 'valid credentials rejected' }
$env:FORK_TELEGRAM_API_HASH = 'invalid'
& $script -ValidateOnly
if ($LASTEXITCODE -eq 0) { throw 'invalid hash accepted' }
Remove-Item Env:FORK_TELEGRAM_API_ID, Env:FORK_TELEGRAM_API_HASH -ErrorAction SilentlyContinue
Write-Output 'Credential validation tests passed'
