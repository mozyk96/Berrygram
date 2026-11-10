[CmdletBinding()]
param(
    [string]$EnvFile = "$env:TEMP\apienv.txt",
    [string]$LogFile = "$env:TEMP\berrygram-142-release.log"
)
$ErrorActionPreference = 'Stop'

# The release credential is taken from a temporary file that was prepared from
# the build tree of the previous release of the same fork. The values are never
# printed and never leave this process.
if (-not (Test-Path $EnvFile)) { throw "The release credential file is missing: $EnvFile" }
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}

$signing = Get-Content (Join-Path $PSScriptRoot '..\.fork-local\release.json') -Raw | ConvertFrom-Json
$env:BERRYGRAM_RELEASE_KEYSTORE = $signing.store
$env:BERRYGRAM_RELEASE_KEY_ALIAS = $signing.alias
$env:BERRYGRAM_RELEASE_STORE_PASSWORD = $signing.password
$env:BERRYGRAM_RELEASE_KEY_PASSWORD = $signing.password

$env:GRADLE_USER_HOME = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }

Push-Location (Join-Path $PSScriptRoot '..')
try {
    $ErrorActionPreference = 'Continue'
    & '.\gradlew.bat' ':TMessagesProj_App:assembleAfatRelease' '-PforkAbi=arm64-v8a' '--console=plain' "--max-workers=2" '-Dorg.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1g' *>&1 |
        Tee-Object -FilePath $LogFile
    $gradleExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($gradleExitCode -ne 0) { throw "The release build failed ($gradleExitCode), see $LogFile" }
} finally {
    Pop-Location
    Remove-Item Env:FORK_TELEGRAM_API_ID, Env:FORK_TELEGRAM_API_HASH -ErrorAction SilentlyContinue
}
Write-Host "Release build finished, log: $LogFile"
