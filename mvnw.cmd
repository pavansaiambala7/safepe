@echo off
set "TOOLS_DIR=%~dp0.tools"
set "JAVA_HOME=%TOOLS_DIR%\jdk17"
set "PATH=%JAVA_HOME%\bin;%TOOLS_DIR%\maven\bin;%PATH%"

"%TOOLS_DIR%\maven\bin\mvn.cmd" %*
