@rem Gradle wrapper batch script for Windows
@setlocal
@if not defined JAVA_HOME (
    @set "JAVA_HOME=C:\Users\Keoo2\Documents\Codex\2026-10-28\files-pasted-by-the-user-senior\work\jdk17\jdk-17.0.20.1+1"
)
@if not defined ANDROID_HOME (
    @set "ANDROID_HOME=C:\Users\Keoo2\Documents\Codex\2026-10-28\files-pasted-by-the-user-senior\work\android-sdk"
)
@"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -cp "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
@endlocal
