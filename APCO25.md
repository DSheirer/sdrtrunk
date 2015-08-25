# APCO-25 Decoder #

The APCO-25 decoder will decoder P-25 phase I signals that are using either
C4FM modulation or LSM Simulcast (CQPSK) modulation.  It will process trunked,
conventional and packet data messages.

## Audio Vocoder ##

APCO-25 Phase-1 audio calls use the multi-band excitation model vocoder to
compress the digitized audio.  SDRTrunk supports the JMBE library for decoding
audio, but DOES NOT include the jmbe library in the normal application download.
You must download and compile the library and place the jmbe-x.x.x.jar file in
the same directory from where you launch the sdrtrunk application.  On startup,
sdrtrunk will attempt to discover the library and can then decode imbe audio.

[JMBE library](https://github.com/DSheirer/jmbe)

[How To Setup JMBE](JMBE.md)

## Traffic Channel Following ##
The decoder currently does not automatically follow trunked traffic channels.
This feature is planned for a future release

![http://sdrtrunk.googlecode.com/svn/wiki/images/P25_Decoding.png](http://sdrtrunk.googlecode.com/svn/wiki/images/P25_Decoding.png)

## Talkgroups and Addresses ##

All talkgroups and radio addresses are listed in hexadecimal (base-16) notation
and use the digits 0 - 9 and the letters A - F.  Talkgroups are four characters
in length and radio addresses are six characters in length.

Packet data IP Version 4 addresses will normally show in the from column of the
events window and the Logical Link ID (radio address) will show in the from
column with the radio's IP Version 4 address listed in the details column. If
the data contains UDP over IP protocol, the UDP source and destination ports
will be appended to the IP address.

## Aliases ##

You can create an Alias List and attach it to your P25 decoder in the channel
configuration window.

When aliasing talkgroups and radio addresses, ensure you include the full 4 or 6
character talkgroup or address with leading zeros.

## Modulation ##

Choose either C4FM or LSM Simulcast (CQPSK) modulation in the channel configuration
decoder tab.  When you select the P25 Decoder from the drop-down list, the
modulation selector list will appear.

![http://sdrtrunk.googlecode.com/svn/wiki/images/P25_Decoding_2.png](http://sdrtrunk.googlecode.com/svn/wiki/images/P25_Decoding_2.png)