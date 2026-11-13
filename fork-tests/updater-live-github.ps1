param([Parameter(Mandatory=$true)][string]$JsonJar, [string]$ExpectedVersion, [string]$ExpectedSha256)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (!$env:JAVA_HOME -or !$env:ANDROID_HOME) { throw 'Set JAVA_HOME and ANDROID_HOME first.' }
$android=Join-Path $env:ANDROID_HOME 'platforms\android-36\android.jar'
$out=Join-Path $root 'build\fork-updater-live'
New-Item -ItemType Directory -Force $out | Out-Null
$base=Join-Path $root 'TMessagesProj\src\main\java\org\telegram\messenger\fork\updater'
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -encoding UTF-8 -cp "$JsonJar;$android" -d $out (Join-Path $PSScriptRoot 'updater-stubs/android/os/Build.java') (Join-Path $base 'UpdatePolicy.java') (Join-Path $base 'GithubUpdateClient.java') (Join-Path $PSScriptRoot 'UpdaterLiveGithubTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Live updater test compilation failed' }
$expected = @()
if ($ExpectedVersion) { $expected += $ExpectedVersion }
if ($ExpectedSha256) {
    if (!$ExpectedVersion) { throw 'ExpectedVersion is required with ExpectedSha256' }
    $expected += $ExpectedSha256
}
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -cp "$out;$JsonJar;$android" UpdaterLiveGithubTest @expected
if ($LASTEXITCODE -ne 0) { throw 'Live updater test failed' }
