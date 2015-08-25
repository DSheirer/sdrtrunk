## Getting started with SDRTRunk ##

### Setup ###

  * **[Linux](SetupLinux.md)**
  * **[Windows](SetupWindows.md)**

![http://sdrtrunk.googlecode.com/svn/wiki/images/Overview.png](http://sdrtrunk.googlecode.com/svn/wiki/images/Overview.png)

[Application Overview](ApplicationOverview.md)

### Quick Start - SDR Dongle ###

1. Select a tuner to display in the spectral display

2. Tune to a frequency near the channel that you want to decode.  Do not tune
the channel into the center of the display.  Adjust the frequency so that the
channel is either to the left or to the right of the center of the display.

3. Right-click on the frequency in the spectral display, choose **Add Decoder**
select the type of decoder that you want to use.

4. If a tuner is available, the channel will immediately start decoding with
default settings. Select the channel in the **Decoding Channels** window to see
call events and decoded messages.

### Quick Start - Sound Card / Scanner Audio ###

1.  Create a new [channel](Channel.md)

2.  In the channel editor select the correct **Mixer** and **Channel** in the source tab, corresponding to where you have patched your scanner audio into the computer.

3.  Select **Enabled**

4.  Hit **Save**.

5.  Select the channel in the [decoding channels](DecodingChannels.md) window to hear the pass-through audio.