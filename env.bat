@echo off
echo Setting environment for Java 11...

set "JAVA_HOME=C:\Program Files\Java\jdk-11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo JAVA_HOME set to: %JAVA_HOME%
java -version