# HackRF #

![http://sdrtrunk.googlecode.com/svn/wiki/images/HackRF.png](http://sdrtrunk.googlecode.com/svn/wiki/images/HackRF.png)

## Tuner Configurations ##

SDRTrunk allows you to define multiple named tuner configurations per tuner
type.  A **Default** configuration is automatically created the first time you
use each specific tuner type and will be recreated again if you accidentally
delete or rename the **Default** configuration.

Each time you plug your USB tuner into a new USB port, it will automatically
use the **Default** tuner configuration.  If you select a different named
configuration, SDRTrunk will remember to use that named configuration each time
you use the USB tuner in that specific USB port.  If you create a named
configuration for your tuner and subsequently move the tuner to a different
USB port, simply select that named configuration from the drop-down list and it
will store that port to named configuration setting.

All changes to a named configuration are automatically saved.

## Configuration ##

  * **Frequency** - sets the tuned frequency
  * **Config Tab** - controls for changing tuner configuration
  * **Info Tab** - displays information about the tuner

### Configuration Tab ###

  * **Configuration Selection List** - selects a named configuration to use.  Automatically applies the corresponding settings to the tuner
  * **Name** - tuner configuration name.
  * **Correction PPM** - correction value to align the currently tuned frequency with the frequency display values.  Increasing the value causes the frequency display to move to the left and vice-versa.
  * **Sample Rate** - sampling rate of the tuner - corresponds to the visible/tunable bandwidth
  * **Gain - Amp** - turns the amplifier on/off
  * **Gain - LNA** - gain settings for the Low Noise Amplifier
  * **Gain - VGA** - gain settings for the Variable Gain Amplifier

  * **New** - creates a new named tuner configuration
  * **Delete** - deletes the currently listed tuner configuration

### Info Tab ###
The info tab displays information about the tuner:

  * **Serial** - serial number of the HackRF board.
  * **Part** - part number
  * **Firmware** - firmware revision

## Using the HackRF ##

One of the most important settings on the HackRF are the gain settings.  The
gain settings affect each portion of the radio spectrum differently, so you
need to adjust the gain each time you move to a different part of the frequency
spectrum.

Strong nearby signals easily overload the HackRF.  If the spectral display
rises significantly, or if you notice the spectral display jumping up and down,
this is an indication that you have overloaded the tuner.  Another symptom of
overload is spectral mirroring, where the left and right side look identical.

Broadcast FM radio stations overload the tuner and you may or may not be able
to set the gain low enough to visualize the entire FM band.

Note: on startup if you have previously used a high sample rate, you may notice
lagging/stalling in the application.  Change the sample rate to a lower sample
rate until the application stabilizes, and then select the desired sample rate.

### Setting Gain ###

  * Set the frequency, or enable a channel and the tuner will automatically
> adjust the frequency.
  * Turn the amplifier off and set LNA and VGA gain to 0.
  * Slowly adjust the LNA gain higher until you see the spectral display rise
> significantly high or start to jump.  Back off the gain setting until the
> display settles down.
  * Repeat this step for the VGA gain setting.
  * Experiment with the Amp button to turn the amplifier on or off.

If you have a strong nearby signal that is close in frequency to the channel
that you want to decode, attempt to tune the strong signal up or down, so that
it is no longer in the viewable passband of the tuner.

## Updating Firmware ##

The firmware label shown on the Info tab is the GIT revision label that was
tagged against the software release.  SDRTrunk does not currently support
flashing the HackRF with new firmware.  Use the HackRF tools to update the
tuner.