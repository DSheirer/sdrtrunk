1. Install Java JRE version 7 or newer from http://www.oracle.com/java

2. Verify java installation:

```
terminal: java -version

java version "1.7.0_55"
Java(TM) SE Runtime Environment (build 1.7.0_55)
Java HotSpot (TM) 64-bit Server VM
```

3. Install udev rules file for any tuners you'll be using into the /etc/udev/rules.d/ directory.  The **funcube-dongle.rules** and **rtl-sdr.rules** files are included.

4. Run the SDRTrunk start script:

```
terminal: ./run_SDRTrunk
```