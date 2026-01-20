@echo off
set JAVA_HOME=C:\Users\devin\liberica-jdk-25-full\jdk-25.0.1-full
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\devin\Desktop\SDRTrunk\SDRTrunk Source\sdrtrunk"
call gradlew.bat run
