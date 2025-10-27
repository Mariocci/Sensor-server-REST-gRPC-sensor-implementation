@echo off
start cmd /k "java -jar build/libs/sensor-1.0-SNAPSHOT-all.jar"
timeout /t 2 /nobreak >nul
start cmd /k "java -jar build/libs/sensor-1.0-SNAPSHOT-all.jar"
timeout /t 2 /nobreak >nul
start cmd /k "java -jar build/libs/sensor-1.0-SNAPSHOT-all.jar"
timeout /t 10 /nobreak >nul
start cmd /k "java -jar build/libs/sensor-1.0-SNAPSHOT-all.jar"
