# JMBE Library #

The Java Multi-Band Excitation (JMBE) library provides decoder support for converting IMBE encoded audio frames to normal audio so that you can listen to the audio calls over your computer speakers.

Website: [JMBE](https://github.com/DSheirer/jmbe)

## Patent Warning ##

The JMBE library contains source code that may be patent protected.  Users
should check local laws before downloading, compiling, using, or DISTRIBUTING
compiled versions of the JMBE library.

## Copyright Notice ##

Copyrights for the terms MBE and IMBE are the property of their respective
owners.

## Adding JMBE to SDRTrunk for decoding MBE audio ##

# Download the **jmbe\_builder.zip** (windows) or the **jmbe\_builder.tar.gz** (linux) file to your computer and uncompress the file.  These files are available in the [Download](https://drive.google.com/folderview?id=0B7BHsssXUq8eOHBTNndCczZTd0k&usp=sharing) folder

# Run the **make\_jmbe\_library\_windows.bat** (windows) or the **make\_jmbe\_library\_linux.sh** (linux) file.  This will download the source code and create the **jmbe-x.x.x.jar** file.  The file that gets created will have version numbers in place of the x's in the filename.

# Copy the **jmbe-x.x.x.jar** file to the same folder where you run the sdrtrunk scripts to start the sdrtrunk program.

# Start SDRTrunk normally and the program will automatically discover and use the jmbe library.

## Troubleshooting ##

If you download, compile and copy the JMBE library jar file to the same directory as the SDRTrunk application, you should not have any issues.

SDRTrunk will generate a log entry to let you know if it discovered the jmbe library correctly for each P25 channel that you have decoding:

> 09:38:54.166 INFO  d.p.audio.P25AudioOutput - JMBE audio conversion library successfully loaded - P25 audio will be available

Or, when the library cannot be found, each channel will log:

> 09:38:54.166 INFO  d.p.audio.P25AudioOutput - JMBE audio conversion  library NOT FOUND