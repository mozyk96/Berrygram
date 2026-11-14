[CmdletBinding()]
param(
    [string]$ConfigFile = (Join-Path $env:LOCALAPPDATA 'Berrygram\ReleaseSigning\credentials.dpapi.json'),
    [string]$LogFile = (Join-Path $PSScriptRoot '..\build\berrygram-release-build.log')
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Unprotect-LocalSecret([string]$ciphertext) {
    $secret = ConvertTo-SecureString $ciphertext
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

if (-not (Test-Path -LiteralPath $ConfigFile)) { throw 'Protected release configuration is missing' }
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $jdk = Get-ChildItem (Join-Path $root '.tooling\jdk') -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
    if (-not $jdk) { throw 'JDK 17 was not found' }
    $env:JAVA_HOME = $jdk.FullName
}
if (-not $env:ANDROID_HOME -or -not (Test-Path (Join-Path $env:ANDROID_HOME 'build-tools\36.0.0'))) {
    $env:ANDROID_HOME = Join-Path $root '.tooling\sdk'
}
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$tools = Join-Path $env:ANDROID_HOME 'build-tools\36.0.0'
if (-not (Test-Path (Join-Path $tools 'apksigner.bat'))) { throw 'Android build tools 36.0.0 were not found' }
$config = Get-Content -Raw -LiteralPath $ConfigFile | ConvertFrom-Json
$public = Get-Content -Raw -LiteralPath (Join-Path $root 'Tools\berrygram-signing.json') | ConvertFrom-Json
$props = @{}
Get-Content (Join-Path $root 'gradle.properties') | ForEach-Object {
    if ($_ -match '^(APP_VERSION_NAME|APP_VERSION_CODE|APP_PACKAGE)=(.*)$') { $props[$matches[1]] = $matches[2].Trim() }
}
if ($props.APP_PACKAGE -ne $public.applicationId -or $props.APP_VERSION_NAME -notmatch '^\d+\.\d+\.\d+$') { throw 'Release identity is invalid' }
$version = $props.APP_VERSION_NAME
$code = [int]$props.APP_VERSION_CODE * 10 + 9
$output = Join-Path $root "build\berrygram-release\$version"
$name = "BerryGram-$version-$($public.assetQualifier)-arm64-v8a.apk"
if (Test-Path (Join-Path $output $name)) { throw 'Release output already exists; preserve it and choose a clean output location' }
if (-not (Test-Path -LiteralPath $config.keystore)) { throw 'Protected release keystore is missing' }

Push-Location $root
try {
    $source = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine source commit' }
    git diff --quiet HEAD -- TMessagesProj/src TMessagesProj_App/build.gradle gradle.properties build.gradle settings.gradle
    if ($LASTEXITCODE -ne 0) { throw 'Commit production source changes before the release build' }
    $env:FORK_TELEGRAM_API_ID = Unprotect-LocalSecret $config.apiId
    $env:FORK_TELEGRAM_API_HASH = Unprotect-LocalSecret $config.apiHash
    $env:BERRYGRAM_RELEASE_KEYSTORE = $config.keystore
    $env:BERRYGRAM_RELEASE_KEY_ALIAS = $config.alias
    $env:BERRYGRAM_RELEASE_STORE_PASSWORD = Unprotect-LocalSecret $config.storePassword
    $env:BERRYGRAM_RELEASE_KEY_PASSWORD = $env:BERRYGRAM_RELEASE_STORE_PASSWORD
    if ($env:FORK_TELEGRAM_API_ID -notmatch '^[1-9][0-9]*$' -or $env:FORK_TELEGRAM_API_HASH -cnotmatch '^[0-9a-f]{32}$') { throw 'Protected Telegram API configuration is invalid' }
    $listing = & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') '-J-Duser.language=en' '-J-Duser.country=US' -list -v -keystore $config.keystore -storetype PKCS12 -storepass:env BERRYGRAM_RELEASE_STORE_PASSWORD -alias $config.alias 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot unlock the release keystore' }
    $fingerprint = @($listing | Where-Object { $_ -match 'SHA256:\s*([0-9A-F:]+)' } | Select-Object -First 1)
    if (-not $fingerprint -or (($fingerprint[0] -replace '^.*SHA256:\s*','' -replace ':','').Trim().ToLowerInvariant() -ne $public.certificateSha256)) { throw 'Release keystore certificate does not match the configured identity' }
    New-Item -ItemType Directory -Force (Split-Path -Parent $LogFile) | Out-Null
    $ErrorActionPreference = 'Continue'
    & '.\gradlew.bat' ':TMessagesProj_App:assembleAfatRelease' '-PforkAbi=arm64-v8a' '--console=plain' '--max-workers=2' *>&1 | ForEach-Object {
        $_.ToString().Replace($env:FORK_TELEGRAM_API_HASH, '[REDACTED]').Replace($env:BERRYGRAM_RELEASE_STORE_PASSWORD, '[REDACTED]')
    } | Tee-Object -FilePath $LogFile
    $buildExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($buildExit -ne 0) { throw "Release build failed ($buildExit); see the redacted log" }
    if ((git rev-parse HEAD).Trim() -ne $source) { throw 'Source commit changed during the build' }
    git diff --quiet HEAD -- TMessagesProj/src TMessagesProj_App/build.gradle gradle.properties build.gradle settings.gradle
    if ($LASTEXITCODE -ne 0) { throw 'Production source changed during the build' }
    $apk = Join-Path $root 'TMessagesProj_App\build\outputs\apk\afat\release\app.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw 'Release APK was not produced' }
    & python (Join-Path $PSScriptRoot 'verify_release_artifact.py') $apk --aapt (Join-Path $tools 'aapt.exe') --apksigner (Join-Path $tools 'apksigner.bat') --zipalign (Join-Path $tools 'zipalign.exe') --package $public.applicationId --version $version --version-code $code --signer-sha256 $public.certificateSha256
    if ($LASTEXITCODE -ne 0) { throw 'Release APK verification failed' }
    New-Item -ItemType Directory -Force $output | Out-Null
    Copy-Item -LiteralPath $apk -Destination (Join-Path $output $name)
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $output $name)).Hash.ToLowerInvariant()
    "$hash  $name" | Set-Content -LiteralPath (Join-Path $output 'SHA256SUMS') -Encoding ascii
    $source | Set-Content -LiteralPath (Join-Path $output 'source-commit.txt') -Encoding ascii
    Write-Host "Verified release APK: $(Join-Path $output $name)"
    Write-Host "SHA-256: $hash"
} finally {
    Pop-Location
    Remove-Item Env:FORK_TELEGRAM_API_ID, Env:FORK_TELEGRAM_API_HASH, Env:BERRYGRAM_RELEASE_KEYSTORE, Env:BERRYGRAM_RELEASE_KEY_ALIAS, Env:BERRYGRAM_RELEASE_STORE_PASSWORD, Env:BERRYGRAM_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
}
