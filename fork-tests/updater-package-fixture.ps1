param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (!$env:JAVA_HOME -or !$env:ANDROID_HOME) { throw 'Set JAVA_HOME and ANDROID_HOME first.' }
$android=Join-Path $env:ANDROID_HOME 'platforms\android-36\android.jar'
$out=Join-Path $root 'build\fork-updater-package-fixture'
New-Item -ItemType Directory -Force $out | Out-Null
$temp=Join-Path $out 'tmp'
New-Item -ItemType Directory -Force $temp | Out-Null
$stubs=Get-ChildItem (Join-Path $PSScriptRoot 'updater-stubs') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$base=Join-Path $root 'TMessagesProj\src\main\java\org\telegram\messenger\fork\updater'
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -encoding UTF-8 -cp "$JsonJar;$android" -d $out @stubs (Join-Path $base 'UpdatePolicy.java') (Join-Path $base 'GithubUpdateClient.java') (Join-Path $PSScriptRoot 'UpdaterPackageFixtureTest.java') (Join-Path $PSScriptRoot 'UpdaterDownloadFixtureTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Fixture compilation failed' }
& (Join-Path $env:JAVA_HOME 'bin\java.exe') "-Djava.io.tmpdir=$temp" -cp "$out;$JsonJar;$android" UpdaterPackageFixtureTest
if ($LASTEXITCODE -ne 0) { throw 'Fixture test failed' }
& (Join-Path $env:JAVA_HOME 'bin\java.exe') "-Djava.io.tmpdir=$temp" -cp "$out;$JsonJar;$android" UpdaterDownloadFixtureTest
if ($LASTEXITCODE -ne 0) { throw 'Download fixture test failed' }
