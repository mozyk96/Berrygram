[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$buildDir = Join-Path $root 'build\fork-smoke'
New-Item -ItemType Directory -Force $buildDir | Out-Null
if ($env:JAVA_HOME) {
    $javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $javac = (Get-Command javac -ErrorAction Stop).Source
    $java = (Get-Command java -ErrorAction Stop).Source
}
$base = Join-Path $root 'TMessagesProj\src\main\java\org\telegram\messenger\fork'
$sources = @(
    'auth\AuthDiagnostics.java', 'auth\BackendPolicy.java', 'auth\SessionCredentials.java',
    'text\CustomTextValidator.java', 'text\CustomTextPackValidator.java', 'text\LocalAliasPolicy.java',
    'text\CustomTextResolution.java', 'text\CustomTextContext.java', 'text\CustomTextScopePolicy.java',
    'text\CustomTextPackKeys.java', 'camera\RoundCameraPolicy.java', 'camera\RoundVideoQualityPolicy.java',
    'voice\VoiceQualityPolicy.java', 'panels\PanelPolicy.java', 'updater\UpdatePolicy.java'
) | ForEach-Object { Join-Path $base $_ }
$tests = @('AuthDiagnosticsTest', 'BackendPolicyTest', 'SessionCredentialsTest', 'CustomTextValidatorTest',
    'CustomTextPackValidatorTest', 'CustomTextFormatTest', 'CustomTextSafetyTest', 'LocalAliasPolicyTest',
    'CustomTextResolutionTest', 'CustomTextContextTest', 'CustomTextScopePolicyTest', 'CustomTextOriginalTest',
    'CustomTextPackKeysTest', 'RoundCameraPolicyTest', 'RoundVideoQualityPolicyTest', 'VoiceQualityPolicyTest', 'PanelPolicyTest', 'UpdatePolicyTest')
$testFiles = $tests | ForEach-Object { Join-Path $PSScriptRoot "$_.java" }
& $javac -encoding UTF-8 -d $buildDir @sources @testFiles
if ($LASTEXITCODE -ne 0) { throw 'Smoke-test compilation failed' }
foreach ($test in $tests) {
    & $java -cp $buildDir $test
    if ($LASTEXITCODE -ne 0) { throw "Smoke test failed: $test" }
}
$managerDir = Join-Path $buildDir 'manager'
New-Item -ItemType Directory -Force $managerDir | Out-Null
$stubs = Get-ChildItem (Join-Path $PSScriptRoot 'stubs') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$textSources = Get-ChildItem (Join-Path $base 'text') -Filter '*.java' | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d $managerDir @stubs @textSources (Join-Path $PSScriptRoot 'CustomTextManagerTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Custom Text manager test compilation failed' }
& $java -cp $managerDir CustomTextManagerTest
if ($LASTEXITCODE -ne 0) { throw 'Custom Text manager test failed' }
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir (Join-Path $base 'camera\RoundCameraPolicy.java') (Join-Path $base 'camera\RoundCameraSettings.java') (Join-Path $PSScriptRoot 'RoundCameraSettingsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Round camera settings test compilation failed' }
& $java -cp $managerDir RoundCameraSettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Round camera settings test failed' }
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir (Join-Path $base 'voice\VoiceQualityPolicy.java') (Join-Path $base 'voice\VoiceQualitySettings.java') (Join-Path $base 'panels\ForkPanelSettings.java') (Join-Path $PSScriptRoot 'VoiceQualitySettingsTest.java') (Join-Path $PSScriptRoot 'PanelSettingsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Voice and panel settings test compilation failed' }
& $java -cp $managerDir VoiceQualitySettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Voice settings test failed' }
& $java -cp $managerDir PanelSettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Panel settings test failed' }
$python = if ($env:FORK_PYTHON) { $env:FORK_PYTHON } else { (Get-Command python -ErrorAction Stop).Source }
& $python (Join-Path $PSScriptRoot 'test_berrygram_resources.py')
if ($LASTEXITCODE -ne 0) { throw 'BerryGram resources test failed' }
& $python (Join-Path $PSScriptRoot 'test_auth_privacy.py')
if ($LASTEXITCODE -ne 0) { throw 'Auth privacy test failed' }
& $python (Join-Path $PSScriptRoot 'test_release_scope.py')
if ($LASTEXITCODE -ne 0) { throw 'Release scope test failed' }
Write-Output 'Fork smoke tests passed (no Android device or account used)'
