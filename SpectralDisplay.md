# Introduction #

The spectral display component shows the frequency activity of the currently selected tuner.

## Cursor ##
A cursor with frequency readout is automatically shown as you move the mouse about the spectral display window.

## Context Menus ##

Right-click anywhere in the spectral display to show a list of context-specific menu options.

### Channel Menu ###

Channels defined in the [Playlist](Playlist.md) are also displayed in the spectral display window.  Each channel exposes a context menu allowing you to start/stop the channel decoder, or view the [ActivitySummary](ActivitySummary.md) for currently decoding channels

### Frequency Menu ###

The current frequency displayed by the cursor displays as a menu that allows you to assign a decoder and automatically start decoding that channel.  A channel configuration is automatically created and enabled.

### Color Menu ###

The color menu allows you to change any of the global color settings used by all spectral displays.

  * **Background** - background color for the spectral display
  * **Channel** - channel overlay color for channels that are defined in the configuration, but are currently not processing (decoding) or selected
  * **Channel Processing** - channel overlay color for any channels that are currently processing (decoding)
  * **Channel Selected** - channel overlay color for the channel that is currently selected in the [Decoding Channels](DecodingChannels.md) window.
  * **Cursor** - color used for the cursor and frequency readout
  * **Gradient Bottom** - bottom color for the gradient used to display the frequency spectrum
  * **Gradient Top** - top color for the gradient used to display the frequency spectrum
  * **Lines** - color used for lines and frequency labels

### Display Menu ###

The display menu allows you to change settings associated with Fast Fourier Transform (FFT) results that are displayed in the spectral display.  Changes made with the display menu are temporary and only apply to the spectral display where they are changed.

  * **FFT Width** - bin size used in FFT calculations, 512 - 32,768.  Using a higher setting provides increased resolution in the display, but also significantly increases CPU usage.
  * **Frame Rate** - determines the number of times per second that the FFT is calculated and the spectral display is updated.
  * **Window Type** - specifies the windowing method to use prior to calculating the FFT.

## Changing Displayed [Tuner](Tuner.md) ##

Right-click on the desired tuner in the [Configuration](Configuration.md) window and select **Show in Main Spectrum**, or choose **New Spectrum Window** to create a new, separate Spectral Display and Waterfall.