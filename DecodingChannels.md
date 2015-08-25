# Decoding Channels #

The decoding channels window displays a list containing the [channel state](ChannelState.md) for each of the channels that are currently enabled for decoding.

## Listening to Decoded Audio ##

Each channel's audio output can be toggled by selecting that channel.  The currently selected channel is highlighted.  When available, each decoder uses trunking signalling messages to determine when to squelch or unsquelch the currently selected channel.

## Context Menus ##

Right-click on any of the listed channels to see a list of context-sensitive menus applicable to each channel.

  * **Disable** - turns off decoding for this channel
  * **[Activity Summary](ActivitySummary.md)** - displays a current activity summary for the channel
  * **Delete** - disables and deletes the channel.  _Note: this action completely removes the channel from the [playlist](Playlist.md)_