# Funcube Dongle Pro Plus Tuner #

![http://sdrtrunk.googlecode.com/svn/wiki/images/FuncubeDongleProPlus.png](http://sdrtrunk.googlecode.com/svn/wiki/images/FuncubeDongleProPlus.png)

## Tuner Configurations ##

SDRTrunk allows you to define multiple named tuner configurations per tuner type.  A **Default** configuration is automatically created the first time you use each specific tuner type and will be recreated again if you accidentally delete or rename the **Default** configuration.

Each time you plug your USB tuner into a new USB port, it will automatically use the **Default** tuner configuration.  If you select a different named configuration, SDRTrunk will remember to use that named configuration each time you use the USB tuner in that specific USB port.  If you create a named configuration for your tuner and subsequently move the tuner to a different USB port, simply select that named configuration from the drop-down list and it will store that port to named configuration setting.

All changes to a named configuration are automatically saved.

## Configuration ##

  * **Frequency** - sets the tuned frequency
  * **Configuration Tab** - controls for changing tuner configuration
  * **Info Tab** - displays information about the tuner

### Configuration Tab ###

  * **Config** - list of named configurations.  Automatically applies the corresponding settings to the tuner
  * **Name** - tuner configuration name.
  * **Correction PPM** - correction value to align the currently tuned frequency with the frequency display values.  Increasing the value causes the frequency display to move to the left and vice-versa.
  * **LNA Gain** - LNA gain setting
  * **Mixer Gain** - Mixer gain setting
  * **New** - creates a new named tuner configuration
  * **Delete** - deletes the currently listed tuner configuration

### Info Tab ###
The info tab displays information about the tuner:

  * **USB Address** - current USB bus and device port, and USB vendor and product identifiers.
  * **Cellular Band** - indicates if the cellular band is blocked or unblocked for this tuner
  * **Firmware** - firmware revision