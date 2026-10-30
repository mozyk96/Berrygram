[CmdletBinding()]
param([switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'

function Test-Credentials([string]$ApiId, [string]$ApiHash) {
    $number = 0
    return [int]::TryParse($ApiId, [ref]$number) -and $number -gt 0 -and $ApiHash -cmatch '^[0-9a-fA-F]{32}$'
}

if ($ValidateOnly) {
    if (Test-Credentials $env:FORK_TELEGRAM_API_ID $env:FORK_TELEGRAM_API_HASH) { exit 0 }
    exit 1
}

function Read-PrivateValue([string]$Prompt) {
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr); $secure.Dispose() }
}

$workspace = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$jdk = Join-Path $workspace 'work\jdk17\jdk-17.0.20.1+1'
$sdk = Join-Path $workspace 'work\android-sdk'
$gradleHome = Join-Path $workspace 'work\gradle-home'
$androidHome = Join-Path $workspace 'work\android-user'
$java = Join-Path $jdk 'bin\java.exe'
if (-not (Test-Path $java) -or -not (Test-Path $sdk)) { throw 'Local JDK or Android SDK missing; restore the project toolchain first.' }
Write-Host 'Enter the values locally. They will not be printed or saved in Git.'
Write-Host 'The resulting APK and Gradle build intermediates contain these credentials; treat them as extractable.'
$apiId = Read-PrivateValue 'Telegram API ID'
$apiHash = Read-PrivateValue 'Telegram API hash'
if (-not (Test-Credentials $apiId $apiHash)) { throw 'Invalid API ID or hash. The ID must be a positive integer and the hash must contain 32 hexadecimal characters.' }
try {
    $env:FORK_TELEGRAM_API_ID = $apiId
    $env:FORK_TELEGRAM_API_HASH = $apiHash
    $env:JAVA_HOME = $jdk
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_USER_HOME = $androidHome
    $env:GRADLE_USER_HOME = $gradleHome
    Push-Location $PSScriptRoot
    try {
        & $java -classpath 'gradle/wrapper/gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain ':TMessagesProj_App:assembleAfatDebug' '-Pandroid.injected.build.abi=arm64-v8a' '-PforkAbi=arm64-v8a' '--no-daemon' '--max-workers=2' '-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed; see the error above.' }
        $built = Join-Path $PSScriptRoot 'TMessagesProj_App\build\intermediates\apk\afat\debug\app.apk'
        if (-not (Test-Path $built)) { throw 'Gradle reported success but the APK was not found.' }
        $destination = Join-Path (Split-Path $PSScriptRoot -Parent) 'ForkClient-arm64-with-api-debug.apk'
        Copy-Item -LiteralPath $built -Destination $destination -Force
        Write-Host "APK: $destination"
    } finally { Pop-Location }
} finally {
    Remove-Item Env:FORK_TELEGRAM_API_ID, Env:FORK_TELEGRAM_API_HASH -ErrorAction SilentlyContinue
    $apiId = $null; $apiHash = $null
}
