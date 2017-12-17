# sdrtrunk
A cross-platform java application for decoding, monitoring, recording and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).

* [Getting Started](https://github.com/DSheirer/sdrtrunk/wiki/GettingStarted)
* [User's Manual Version 0.3.0](https://github.com/DSheirer/sdrtrunk/wiki/UserManual_Version0.3.0)
* [Download](https://github.com/DSheirer/sdrtrunk/releases)
* [Support](https://groups.google.com/forum/#!forum/sdrtrunk)

![sdrtrunk Application Overview - Version 0.3.0](https://github.com/DSheirer/sdrtrunk/wiki/images/ApplicationOverview_V0.3.0.png)
**Figure 1:** sdrtrunk **Version 0.3.0** Application Screenshot

# Instructions:
## Build the project
Project uses gradle as build system. You can build it with locally installed gradle or if you do not have/want install gradle you can use preconfigured linux/windows wrapper.
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
gradle.bat clean buildSdr
```
This would add gradle, download all project dependencies and build JAR

## Development
All dependencies/versions are controlled from build.gradle.
To change the new release version tag of artifact - change property:
```
version = '0.3.2'
```

## Run SDRTrunk
SDRTrunk is packed into single uber-jar file. There is need to have folders with dependecies and
adding them with classpath. Just run with java.
### Use Java8
```
java -jar build/libs/sdr-trunk-all-0.3.2.jar 
```

## TODO ITEMS

- [x] Move project to gradle build
- [x] Changed folders structure, based on java project
- [x] Dependencies are moved to gradle and versions are updated
- [x] Create uberJAR with all dependencies
- [ ] Some dependencies left in imports folder. Find them and include into gradle
- [ ] Move images into resources of project
- [ ] Add tests to project. This would guarantee that after refactoring part of functionality is not broken
- [ ] Create modular OOP system (radio-module, gui, web-ui, etc). This would allow to use RADIO modules to call from CLI, GUI or Web UI. And use SDRTrunk as dependency for other projects.