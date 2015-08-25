# Channel #

A channel defines all of the configuration details needed for to setup a radio channel for decoding. When a [Channel](Channel.md) is actively decoding, the system icon is colored Green.  If there is no source available, the channel icon turns red.

![http://sdrtrunk.googlecode.com/svn/wiki/images/Channel.png](http://sdrtrunk.googlecode.com/svn/wiki/images/Channel.png)

## Creating a New Channel ##

Right-click on a [Site](Site.md) folder and select 'New Channel'.

## Deleting a Channel ##

Right-click on the [Channel](Channel.md) and select 'Delete'

## Starting/Stopping Channel Decoding ##

Use the Enabled check box in the channel configuration to turn decoding on or off.  Once you save the configuration, the channel will automatically start decoding, if enabled.

**Quick Start** - right-click on the channel in the configuration window, or right-click on the channel in the spectral display and selected 'Enable' or 'Disable' to turn the decoder on or off with one click.

## Channel Configuration ##

  * **Enabled** - turns the channel on/off for decoding.
  * **Name** - name for the channel
  * **Aliases** - assigns an AliasList to the channel so that any identifiers produced by the decoder can have an [Alias](Alias.md) value.
  * **Source Tab** - designates the source for raw sampled data to feed the decoder
  * **Decoder Tab** - designates the primary decoder to use for this channel
  * **Aux Tab** - identifies additional auxiliary decoders to use in addition to the primary decoder
  * **Event Log Tab** - logging options for decoded events
  * **Record Tab** - recording options

### Source Tab ###

The source tab allows you to specify the source for raw sampled data to feed the primary and auxiliary decoders.

  * **No Source** - this is the default source when a new channel is created.  You cannot decode a channel with this source.
  * **Mixer/Sound Card** - allows you to use an external radio scanner with the audio output or discriminator tapped audio from the scanner patched to a sound card input on the computer.  Select the Mixer (Sound Card) and Channel for the sound card where you have the scanner audio patched.
  * **Tuner** - allows you to specify a radio frequency as the source for raw samples.  You must have a [Tuner](Tuner.md) attached to your computer and it must appear in the list of [Tuners](Tuners.md) in the SDRTrunk [Configuration](Configuration.md) window.  If no tuners are attached, or if none of the attached tuners can tune the requested frequency, the channel will indicated 'No Tuner' when you enable it.

### Decoder Tab ###

The decoder tab provides a list of available primary decoders to select for the channel.  Only one primary decoder can be selected per channel.  If you want to run multiple primary decoders, create additional channels, each with a different primary decoder.

  * **[LTR-Standard](LTR.md)** - FM demodulator and Logic Trunked Radio (LTR) trunked radio signaling decoder
  * **[LTR-Net](LTRNet.md)** - FM demodulator and LTR-Net trunked radio signaling decoder
  * **[MPT-1327](MPT1327.md)** - FM demodulator and MPT-1327 trunked radio signaling decoder.  Provides automatic traffic channel following.
  * **[NBFM](NBFM.md)** - narrow-band FM demodulator.
  * **[Passport](Passport.md)** - FM demodulator and Passport trunked radio signaling decoder.

## Aux Tab ##
The aux tab allows you to select from multiple auxiliary decoders to use in addition to the selected primary decoder.  Auxiliary decoders are used to decode pass-band audio signals like gps and digital radio identification bursts that are used on some trunked and conventional radio systems.

  * **[Fleetsync II](Fleetsync2Decoder.md)** - decodes ANI, Acknowledge, Status, Paging and GPS bursts
  * **[MDC-1200](MDC1200Decoder.md) - decodes MDC-1200 ANI bursts.**

### Event Log Tab ###
The event log tab allows you to select from multiple textual logging options.

  * **Binary Messages** - logs raw binary messages created by the decoder.  Each message contains a timestamp, protocol, and a string of binary ones and zeros.
  * **Decoded Messages** - logs decoded textual messages created by the decoder.
  * **Call Events** - logs all of the call events that can be seen in the [Calls](CallEvents.md) window.

### Record Tab ###
The record tab provides options for recording of the demodulated/decoded audio and/or the raw I/Q samples from a tuner or complex mixer source.

  * **Audio** - records all demodulated audio, uninterrupted and without squelch control in a wave file.
  * **Baseband I/Q** - records all raw baseband sample data in a two-channel, 16-bits per sample wave file.