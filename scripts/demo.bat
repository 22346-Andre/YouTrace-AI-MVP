@echo off
cd /d "%~dp0\.."
if not exist build mkdir build
javac -encoding UTF-8 -d build backend\src\main\java\com\youtrace\core\IncidentEngine.java demo\LocalServer.java
if errorlevel 1 exit /b 1
java -cp build LocalServer 8080
