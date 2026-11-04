[CmdletBinding()]
param([string]$SourceFile)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$build = Join-Path $root 'build/fork-smoke/chat-header'
New-Item -ItemType Directory -Force $build | Out-Null
if (!$SourceFile) { $SourceFile = Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAvatarContainer.java' }
# The retired wrapper must not intercept native measurement, layout or state transitions.
foreach ($file in @($SourceFile,
    (Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java'),
    (Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java'))) {
    if ((Get-Content $file -Raw) -match '(?i)floatingChatHeader|floatingHeaderTextWidth|floatingHeaderMenuWidth') {
        throw "Experimental chat header remains in $file"
    }
}
$appearance = Get-Content (Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/ForkAppearanceSettingsActivity.java') -Raw
if ($appearance -match 'ForkNewChatHeader|ForkChatHeaderSettings') { throw 'Retired chat header setting remains visible' }
$settings = Join-Path $root 'TMessagesProj/src/main/java/org/telegram/messenger/fork/panels/ForkChatHeaderSettings.java'
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { (Get-Command javac -ErrorAction Stop).Source }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
& $javac -encoding UTF-8 -d $build $settings (Join-Path $PSScriptRoot 'FloatingChatHeaderGeometryTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Native chat header compatibility compilation failed' }
& $java -cp $build FloatingChatHeaderGeometryTest
if ($LASTEXITCODE -ne 0) { throw 'Native chat header compatibility test failed' }
