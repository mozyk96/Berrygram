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
    'text\CustomTextPackKeys.java', 'text\ForkLocalePolicy.java', 'camera\RoundCameraPolicy.java', 'camera\RoundVideoQualityPolicy.java',
    'camera\CameraProfilePolicy.java',
    'voice\VoiceQualityPolicy.java', 'voice\VoiceProfilePolicy.java', 'storage\ForkArchivedRestorePolicy.java', 'panels\PanelPolicy.java', 'panels\GlassPolicy.java', 'panels\GraphicsColorPolicy.java', 'panels\MenuSurfacePolicy.java', 'diagnostics\NotificationDiagnosticsPolicy.java', 'updater\UpdatePolicy.java', 'updater\UpdateInstallIntent.java', 'stealth\StealthPolicy.java', 'performance\DeviceAnalyzerPolicy.java'
) | ForEach-Object { Join-Path $base $_ }
$tests = @('AuthDiagnosticsTest', 'BackendPolicyTest', 'SessionCredentialsTest', 'CustomTextValidatorTest',
    'CustomTextPackValidatorTest', 'CustomTextFormatTest', 'CustomTextSafetyTest', 'LocalAliasPolicyTest',
    'CustomTextResolutionTest', 'CustomTextContextTest', 'CustomTextScopePolicyTest', 'CustomTextOriginalTest',
    'CustomTextPackKeysTest', 'ForkLocalePolicyTest', 'RoundCameraPolicyTest', 'RoundVideoQualityPolicyTest', 'CameraProfilePolicyTest', 'VoiceQualityPolicyTest', 'VoiceProfilePolicyTest', 'PanelPolicyTest', 'GlassPolicyTest', 'GraphicsColorPolicyTest', 'MenuSurfacePolicyTest', 'NotificationDiagnosticsPolicyTest', 'UpdatePolicyTest', 'UpdateInstallIntentTest', 'ForkTabIndicatorInterpolatorTest', 'ForkUnifiedSettingsMappingTest', 'StealthPolicyTest', 'DeviceAnalyzerPolicyTest', 'ForkArchivedRestorePolicyTest')
$testFiles = $tests | ForEach-Object { Join-Path $PSScriptRoot "$_.java" }
$sources += (Join-Path $base 'performance\PerformancePolicy.java')
$sources += (Join-Path $root 'TMessagesProj\src\main\java\org\telegram\ui\fork\navigation\ForkTabIndicatorInterpolator.java')
$sources += (Join-Path $root 'TMessagesProj\src\main\java\org\telegram\ui\fork\settings\ForkUnifiedSettingsMapping.java')
$tests += 'PerformancePolicyTest'
$testFiles = $tests | ForEach-Object { Join-Path $PSScriptRoot "$_.java" }
& $javac -encoding UTF-8 -d $buildDir @sources @testFiles
if ($LASTEXITCODE -ne 0) { throw 'Smoke-test compilation failed' }
foreach ($test in $tests) {
    & $java -cp $buildDir $test
    if ($LASTEXITCODE -ne 0) { throw "Smoke test failed: $test" }
}
$bitmapDir = Join-Path $buildDir 'bitmap-geometry'
New-Item -ItemType Directory -Force $bitmapDir | Out-Null
$bitmapStubs = Get-ChildItem (Join-Path $PSScriptRoot 'bitmap-stubs') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$bitmapSource = Join-Path $root 'TMessagesProj\src\main\java\org\telegram\ui\Components\blur3\source'
& $javac -encoding UTF-8 -d $bitmapDir @bitmapStubs (Join-Path $bitmapSource 'BlurredBackgroundSource.java') (Join-Path $bitmapSource 'BlurredBackgroundSourceBitmap.java') (Join-Path $bitmapSource 'LiquidGlassBitmapSource.java') (Join-Path $PSScriptRoot 'BitmapSourceGeometryTest.java') (Join-Path $PSScriptRoot 'LiquidGlassSourceLifecycleTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Bitmap geometry test compilation failed' }
& $java -cp $bitmapDir BitmapSourceGeometryTest
if ($LASTEXITCODE -ne 0) { throw 'Bitmap geometry test failed' }
& $java -cp $bitmapDir LiquidGlassSourceLifecycleTest
if ($LASTEXITCODE -ne 0) { throw 'Liquid Glass source lifecycle test failed' }
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
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir (Join-Path $base 'voice\VoiceQualityPolicy.java') (Join-Path $base 'voice\VoiceProfilePolicy.java') (Join-Path $base 'voice\VoiceProfileSettings.java') (Join-Path $base 'voice\VoiceQualitySettings.java') (Join-Path $base 'panels\PanelPolicy.java') (Join-Path $base 'panels\GraphicsColorPolicy.java') (Join-Path $base 'panels\ForkPanelSettings.java') (Join-Path $PSScriptRoot 'VoiceQualitySettingsTest.java') (Join-Path $PSScriptRoot 'PanelSettingsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Voice and panel settings test compilation failed' }
& $java -cp $managerDir VoiceQualitySettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Voice settings test failed' }
& $java -cp $managerDir PanelSettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Panel settings test failed' }
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir (Join-Path $base 'panels\GlassPolicy.java') (Join-Path $base 'panels\ForkGlassSettings.java') (Join-Path $PSScriptRoot 'GlassSettingsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Glass settings test compilation failed' }
& $java -cp $managerDir GlassSettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Glass settings test failed' }
$python = if ($env:FORK_PYTHON) { $env:FORK_PYTHON } else { (Get-Command python -ErrorAction Stop).Source }
& $python (Join-Path $PSScriptRoot 'test_beta8_updater_lifecycle.py')
if ($LASTEXITCODE -ne 0) { throw 'Updater runnable lifecycle test failed' }
& $python (Join-Path $PSScriptRoot 'test_berrygram_resources.py')
& $python (Join-Path $PSScriptRoot 'test_fork_string_escapes.py')
if ($LASTEXITCODE -ne 0) { throw 'Fork string escape test failed' }
if ($LASTEXITCODE -ne 0) { throw 'BerryGram resources test failed' }
& $python (Join-Path $PSScriptRoot 'test_auth_privacy.py')
if ($LASTEXITCODE -ne 0) { throw 'Auth privacy test failed' }
& $python (Join-Path $PSScriptRoot 'test_release_scope.py')
if ($LASTEXITCODE -ne 0) { throw 'Release scope test failed' }
$surfaceDir = Join-Path $buildDir 'surface-animation'
New-Item -ItemType Directory -Force $surfaceDir | Out-Null
$surfaceStubs = Get-ChildItem (Join-Path $PSScriptRoot 'surface-animation-stubs') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d $surfaceDir @surfaceStubs (Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/ForkSurfaceAnimation.java') (Join-Path $PSScriptRoot 'SurfaceAnimationLifecycleTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Surface animation test compilation failed' }
& $java -cp $surfaceDir SurfaceAnimationLifecycleTest
if ($LASTEXITCODE -ne 0) { throw 'Surface animation lifecycle test failed' }

& $javac -encoding UTF-8 -d $buildDir (Join-Path $base 'panels/HeaderGeometry.java') (Join-Path $PSScriptRoot 'HeaderGeometryTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Header geometry test compile failed' }
& $java -cp $buildDir HeaderGeometryTest
if ($LASTEXITCODE -ne 0) { throw 'Header geometry test failed' }
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir (Join-Path $base 'panels/ForkChatHeaderSettings.java') (Join-Path $base 'panels/ForkAnimationSettings.java') (Join-Path $PSScriptRoot 'GraphicsPreferencesPersistenceTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Graphics preferences compile failed' }
& $java -cp $managerDir GraphicsPreferencesPersistenceTest
if ($LASTEXITCODE -ne 0) { throw 'Graphics preferences test failed' }
node (Join-Path $PSScriptRoot 'test_popup_animation_lifecycle.js')
if ($LASTEXITCODE -ne 0) { throw 'Popup lifecycle test failed' }
& (Join-Path $PSScriptRoot 'chat-header-geometry.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Chat header geometry test failed' }
node (Join-Path $PSScriptRoot 'test_graphics_surface_state.js')
if ($LASTEXITCODE -ne 0) { throw 'test_graphics_surface_state.js failed' }
node (Join-Path $PSScriptRoot 'test_native_main_header.js')
if ($LASTEXITCODE -ne 0) { throw 'Native main header test failed' }

$actionSources = Get-ChildItem (Join-Path $base 'actions') -Filter '*.java' | ForEach-Object { $_.FullName }
$actionTests = @('ActionMenuPolicyTest', 'ActionMenuSettingsTest', 'ForkReactionSettingsTest', 'ReactionFilterPolicyTest')
$actionTestFiles = $actionTests | ForEach-Object { Join-Path $PSScriptRoot "$_.java" }
& $javac -encoding UTF-8 -cp $managerDir -d $managerDir @actionSources @actionTestFiles
if ($LASTEXITCODE -ne 0) { throw 'Action system test compilation failed' }
foreach ($test in $actionTests) {
    & $java -cp $managerDir $test
    if ($LASTEXITCODE -ne 0) { throw "Action test failed: $test" }
}
$performanceDir = Join-Path $buildDir 'performance'
New-Item -ItemType Directory -Force $performanceDir | Out-Null
$performanceStubs = @(Get-ChildItem (Join-Path $PSScriptRoot 'performance-stubs') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
$performanceSources = @(
    (Join-Path $base 'performance\DeviceAnalyzerPolicy.java'),
    (Join-Path $base 'performance\PerformancePolicy.java'),
    (Join-Path $base 'performance\PerformanceSettings.java'),
    (Join-Path $PSScriptRoot 'PerformanceSettingsTest.java')
)
& $javac -encoding UTF-8 -d $performanceDir ($stubs + $performanceStubs + $performanceSources)
if ($LASTEXITCODE -ne 0) { throw 'Performance settings test compilation failed' }
& $java -cp $performanceDir PerformanceSettingsTest
if ($LASTEXITCODE -ne 0) { throw 'Performance settings test failed' }

node (Join-Path $PSScriptRoot 'test_gesture_dispatch.js')
if ($LASTEXITCODE -ne 0) { throw 'Gesture dispatch test failed' }
node (Join-Path $PSScriptRoot 'test_gesture_native_path.js')
if ($LASTEXITCODE -ne 0) { throw 'Gesture native path test failed' }
node (Join-Path $PSScriptRoot 'test_reaction_integration.js')
if ($LASTEXITCODE -ne 0) { throw 'Reaction integration test failed' }
node (Join-Path $PSScriptRoot 'test_main_tabs_indicator.js')
if ($LASTEXITCODE -ne 0) { throw 'Main tabs indicator test failed' }
node (Join-Path $PSScriptRoot 'test_message_menu_integration.js')
if ($LASTEXITCODE -ne 0) { throw 'Message menu integration test failed' }
Write-Output 'Fork smoke tests passed (no Android device or account used)'
