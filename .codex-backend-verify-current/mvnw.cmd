@echo off
setlocal

set "JAVA_HOME=F:\Java\jdk"
set "MAVEN_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12"
set "M2_HOME=%MAVEN_HOME%"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

call "%MAVEN_HOME%\bin\mvn.cmd" %*

