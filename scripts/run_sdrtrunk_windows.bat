@echo off

java -version

SETLOCAL ENABLEDELAYEDEXPANSION

for /f "usebackq tokens=3" %%a in (`java -version 2^>^&1 ^| find "version"`) do ( 
	rem set b to the unquoted version string parsed from the java version command
	set b=%%~a

	rem set c to the first character of the unquoted version string
	set c=!b:~0,1!

	if !c! equ 9 (
		echo Using Java 9 options		
		java -cp "SDRTrunk.jar;*;libs/*;config/*;images/*" --add-modules java.xml.bind ua.in.smartjava.gui.SDRTrunk
	) else (
		echo Using Java 8 options		
		java -XX:+UseG1GC -cp "SDRTrunk.jar;*;libs/*;config/*;images/*" ua.in.smartjava.gui.SDRTrunk
	)
)

ENDLOCAL

@echo on
