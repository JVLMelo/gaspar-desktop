@echo off
setlocal
set MAVEN_VERSION=3.9.11
set BASE_DIR=%~dp0
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set ARCHIVE=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  if not exist "%ARCHIVE%" powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ARCHIVE%'"
  powershell -NoProfile -Command "Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%BASE_DIR%.mvn' -Force"
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
