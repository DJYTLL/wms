$env:JAVA_HOME = 'F:\Java\jdk'
$env:MAVEN_HOME = 'C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.12'
$env:M2_HOME = $env:MAVEN_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;$env:Path"

Write-Output "JAVA_HOME=$env:JAVA_HOME"
Write-Output "MAVEN_HOME=$env:MAVEN_HOME"
Write-Output "M2_HOME=$env:M2_HOME"
