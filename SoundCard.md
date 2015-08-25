# Sound Card / Mixer Sources #

You can input the headset audio, and/or discriminator audio output from your scanner to use as a source for each of the decoders.  Normal headset audio works great with MPT-1327, Fleetsync and MDC-1200.  Discriminator audio is required for LTR, LTR-Net and Passport since the signalling would be filtered out by the normal headset audio filters.

Use the **NBFM** decoder first when you are trying to setup a scanner to sound card setup.  Configure a channel using the NBFM decoder and select the sound card and left or right channel as the input.  Enable the channel and once running, select the channel in the [decoding channels](DecodingChannels.md) window to hear the audio output.  Adjust the scanner audio volume to mid-level, but not so high as to induce clipping on the signal.

Once you have good sound running through the system, change the decoder from NBFM to the one you want to use and save the configuration.