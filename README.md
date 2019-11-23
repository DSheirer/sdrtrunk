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

## Download the latest sdrtrunk release for your operating system
 
All release versions of sdrtrunk are available from the [releases](https://github.com/DSheirer/sdrtrunk/releases) tab.

**(alpha)** These versions are under development feature previews and likely to contain bugs and unexpected behavior.
**(beta)** These versions are currently being tested for bugs and functionality prior to final release.
**(final)** These versions have been tested and are the current release version.

## Unzip the release

Use 7-zip or any zip utility to unzip the release file

## Start the application

Once unzipped, open a command prompt to where you unzipped the release.  Change to the **/bin** directory and use the launch script to start the application:
**Windows** sdr-trunk.bat
**Linux/OSX** ./sdr-trunk

## Optional - P25 Audio
If you're using sdrtrunk with a P25 trunked radio system, the [JMBE](https://github.com/DSheirer/sdrtrunk/wiki/JMBE) wiki page contains instructions for downloading the JMBE audio library source code and compiling the JMBE library.  Once you have compiled the library, launch the sdrtrunk application.  From the menu bar, choose **View >> Preferences**.  In the **JMBE Audio Codec** section, update the path to where your compiled JMBE library is located.  Any channels that are started after you set the path will be able to produce P25 audio.

# Developer Instructions:

If you're interested in modifying and/or compiling the source code, please follow these instructions to use gradle to compile the code. 

## Build the project
sdrtrunk uses the gradle build system. This requires OpenJDK 11 or higher installed on your local compuber.  Use the gradle wrapper to build the source code:

### Linux
```
./gradlew clean build
```
### Windows
```
gradlew.bat clean build
```

The **/build/distributions** folder will contain the zip file of the compiled program.  Unzip it and launch the program from the scripts in the **/bin** directory.

## Development
All dependencies/versions are controlled from build.gradle.
To change the new release version tag of artifact - change property:
```
version = '0.4.0'
```
