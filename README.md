# sdrtrunk
A cross-platform java application for decoding, monitoring, recording and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).

* [Getting Started](https://github.com/DSheirer/sdrtrunk/wiki/GettingStarted_V0.3.0)
* [User's Manual Version 0.3.0](https://github.com/DSheirer/sdrtrunk/wiki/UserManual_V0.3.0)
* [Download](https://github.com/DSheirer/sdrtrunk/releases)
* [Support](https://groups.google.com/forum/#!forum/sdrtrunk)

![sdrtrunk Application Overview - Version 0.3.0](https://github.com/DSheirer/sdrtrunk/wiki/v0.3/images/ApplicationOverview_V0.3.0.png)
**Figure 1:** sdrtrunk **Version 0.3.0** Application Screenshot

# End User Instructions:

If you simply want to download and run the program, please follow these instructions.

## Install Java 8 (or newer)

## Download the latest sdrtrunk release
 
All release versions of sdrtrunk are available from the [releases](https://github.com/DSheirer/sdrtrunk/releases) tab.

**(final)** These versions have been tested and are the current release version.

**(alpha)** These versions are under development feature previews and likely to contain bugs and unexpected behavior.

**(beta)** These versions are currently being tested for bugs and functionality.

## Run the application

Either double-click on the downloaded file (if supported on your operating system) or open a terminal/command window
and change to the directory where you downloaded the release file and type:

```
java -jar downloaded-jar-filename 
```

Note: replace _downloaded-jar-filename_ with the actual name of the sdrtrunk release version that you downloaded 

## Optional - P25 Audio
If you're using sdrtrunk with a P25 trunked radio system, the [JMBE](https://github.com/DSheirer/sdrtrunk/wiki/JMBE) wiki page contains instructions
for downloading the JMBE audio library source code and compiling the JMBE library.  Copy the resulting JMBE audio library 
jar file to the same folder containing the sdrtrunk application to use the library with sdrtrunk.

# Developer Instructions:

If you're interested in modifying and/or compiling the source code, please follow these instructions to use gradle to compile the code. 

## Build the project
sdrtrunk uses gradle as the build system. You can build it with locally installed gradle or if you do not have/want 
to install gradle you can use preconfigured linux/windows wrapper.

### Build with locally installed gradle 4.3.1
```
gradle clean buildSdr
```
### Build with preconfigured wrapper for linux/windows
Linux
```
./gradlew clean buildSdr
```
Windows
```
gradlew.bat clean buildSdr
```
This would add gradle, download all project dependencies and build JAR

## Development
All dependencies/versions are controlled from build.gradle.
To change the new release version tag of artifact - change property:
```
version = '0.3.2'
```

## Run SDRTrunk
SDRTrunk is packed into single uber-jar file. There is need to have folders with dependencies and
adding them with classpath. Just run with java.
### Use Java8
```
java -jar build/libs/sdr-trunk-all-0.3.2.jar 
```
