[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Apk,
    [Parameter(Mandatory)][int]$VersionCode,
    [Parameter(Mandatory)][ValidateSet('stage1','stage2')][string]$Stage,
    [string]$Serial = '5bf68684'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$adb = Join-Path $root '.tooling\sdk\platform-tools\adb.exe'
$tools = Join-Path $root '.tooling\sdk\build-tools\36.0.0'
$env:JAVA_HOME = Join-Path $root '.tooling\jdk\jdk-17.0.21+9'
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
$package = 'org.berrygram.client'
$public = Get-Content -Raw (Join-Path $root 'Tools\berrygram-signing.json') | ConvertFrom-Json

function AdbChecked([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return ($result -join "`n").Trim()
}
function VerifySigner([string]$Path) {
    $signature = & (Join-Path $tools 'apksigner.bat') verify --print-certs $Path 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
    $matches = @($signature | Select-String '^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$')
    if ($matches.Count -ne 1 -or $matches[0].Matches[0].Groups[1].Value.ToLowerInvariant() -ne $public.certificateSha256) {
        throw 'Signing identity mismatch; stop without uninstalling'
    }
}

if ((AdbChecked @('get-state')) -ne 'device') { throw 'The selected device is not authorized' }
$model = AdbChecked @('shell','getprop','ro.product.model')
$abi = AdbChecked @('shell','getprop','ro.product.cpu.abilist')
$sdk = [int](AdbChecked @('shell','getprop','ro.build.version.sdk'))
if ($abi -notmatch '(^|,)arm64-v8a(,|$)' -or $sdk -lt 29) { throw 'Device does not support this candidate' }
VerifySigner $apkPath
$badging = & (Join-Path $tools 'aapt.exe') dump badging $apkPath
if ($LASTEXITCODE -ne 0 -or ($badging -join "`n") -notmatch "name='org.berrygram.client' versionCode='$VersionCode' versionName='2.0.0'") {
    throw 'Candidate identity/version does not match'
}
$output = Join-Path $root "build\private-200-$Stage-deploy"
if (Test-Path $output) { throw 'Deployment evidence already exists; preserve it before retrying' }
New-Item -ItemType Directory -Path $output | Out-Null
$before = AdbChecked @('shell','dumpsys','package',$package)
$before | Set-Content (Join-Path $output 'package-before.txt')
$installedPath = if ($before -match 'versionCode=\d+') { AdbChecked @('shell','pm','path',$package) } else { '' }
$firstInstallBefore = [regex]::Match($before, 'firstInstallTime=([^\r\n]+)').Groups[1].Value.Trim()
if ($installedPath -match '(?m)^package:(.+)$') {
    $oldApk = Join-Path $output 'installed-predecessor.apk'
    AdbChecked @('pull',$matches[1].Trim(),$oldApk) | Out-Null
    VerifySigner $oldApk
    $oldCode = [regex]::Match($before, 'versionCode=(\d+)').Groups[1].Value
    if (-not $oldCode -or [int]$oldCode -ge $VersionCode) { throw 'Candidate must have a strictly increased versionCode' }
} elseif ($Stage -eq 'stage2') { throw 'Stage 1 must already be installed before Stage 2' }
$install = AdbChecked @('install','-r',$apkPath)
if ($install -notmatch '(?m)^Success$') { throw "Installation did not report Success: $install" }
$after = AdbChecked @('shell','dumpsys','package',$package)
$after | Set-Content (Join-Path $output 'package-after.txt')
if ($after -notmatch "versionCode=$VersionCode\b") { throw 'Installed versionCode does not match' }
$firstInstallAfter = [regex]::Match($after, 'firstInstallTime=([^\r\n]+)').Groups[1].Value.Trim()
if ($firstInstallBefore -and $firstInstallBefore -ne $firstInstallAfter) { throw 'First-install timestamp changed unexpectedly' }
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apkPath).Hash.ToLowerInvariant()
$remote = "/sdcard/Download/Berrygram-2.0.0-$Stage-$VersionCode-arm64-v8a.apk"
AdbChecked @('push',$apkPath,$remote) | Out-Null
$remoteHash = AdbChecked @('shell','sha256sum',$remote)
if ($remoteHash -notmatch "^$hash\s") { throw 'Downloads transfer checksum mismatch' }
$launch = AdbChecked @('shell','am','start','-W','-n',"$package/org.telegram.ui.LaunchActivity")
$launch | Set-Content (Join-Path $output 'launch.txt')
if ($launch -notmatch 'Status: ok') { throw 'Launcher did not return Status: ok' }
Start-Sleep -Seconds 3
$appPid = AdbChecked @('shell','pidof',$package)
if ($appPid -notmatch '^\d+( \d+)*$') { throw 'Application process did not remain running' }
$crashes = AdbChecked @('logcat','-d',"--pid=$($appPid.Split(' ')[0])",'-s','AndroidRuntime:E')
$crashes | Set-Content (Join-Path $output 'runtime-errors.txt')
if ($crashes -match 'FATAL EXCEPTION') { throw 'Immediate application crash detected' }
$report = [ordered]@{ stage=$Stage; serial=$Serial; model=$model; sdk=$sdk; versionCode=$VersionCode;
    apk=$apkPath; sha256=$hash; signerSha256=$public.certificateSha256; downloads=$remote;
    firstInstallTimeBefore=$firstInstallBefore; firstInstallTimeAfter=$firstInstallAfter;
    installation='Success'; launch='Status: ok'; process=$appPid;
    dataEvidence='No uninstall or clear-data; timestamp continuity checked for an update. Authenticated data not inspected.' }
$report | ConvertTo-Json | Set-Content (Join-Path $output 'verification.json')
$report | ConvertTo-Json
