[CmdletBinding()]
param([string]$SourceFile)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$build = Join-Path $root 'build/fork-smoke/chat-header'
New-Item -ItemType Directory -Force $build | Out-Null
if (!$SourceFile) { $SourceFile = Join-Path $root 'TMessagesProj/src/main/java/org/telegram/ui/Components/ChatAvatarContainer.java' }
$source = Get-Content $SourceFile -Raw
# Compile the actual Android-independent production method, not a copied formula.
$method = [regex]::Match($source, '(?s)    static int floatingHeaderTextWidth\(int containerWidth, int menuWidth, int avatarSlotWidth\) \{[^}]+\}')
if (!$method.Success) { throw 'Production header width policy was not found' }
$setter = [regex]::Match($source, '(?s)    public void setFloatingHeaderMenuWidth\(int width\) \{.*?\r?\n    \}')
if (!$setter.Success) { throw 'Production menu-width setter was not found' }
$wrapper = Join-Path $build 'FloatingChatHeaderGeometry.java'
Set-Content $wrapper ("final class FloatingChatHeaderGeometry {`n" + $method.Value + "`nint floatingHeaderMenuWidth, layoutCalls; void forceLayout() { layoutCalls++; }`n" + $setter.Value + "`n}")
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { (Get-Command javac -ErrorAction Stop).Source }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
& $javac -encoding UTF-8 -d $build $wrapper (Join-Path $PSScriptRoot 'FloatingChatHeaderGeometryTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Chat header geometry compilation failed' }
& $java -cp $build FloatingChatHeaderGeometryTest
if ($LASTEXITCODE -ne 0) { throw 'Chat header geometry test failed' }
