# Call Events #

The call events window displays a history of the radio calls decoded for a channel.  Select a channel in the [decoding channels](DecodingChannels.md) window to display the call event history for that channel.

Note: call events continue to be displayed for the last selected channel, allowing you to select and then deselect a channel in order to turn off the audio for that channel and continue to see all call events.

![http://sdrtrunk.googlecode.com/svn/wiki/images/CallEvents.png](http://sdrtrunk.googlecode.com/svn/wiki/images/CallEvents.png)

## Call Event Table Details ##

  * **Time** - time that the event occurred
  * **Event** - call event description
  * **From** - identifier of the radio that initiated the call
  * **From Alias** - alias label of the from entity
  * **To** - identifier of the radio or talkgroup receiving the call
  * **To Alias** - alias label of the to entity
  * **Channel** - channel number for the event
  * **Frequency** - frequency of the channel number
  * **Details** - minimal additional details for the call event

## Call Events ##
  * **Acknowledge** - acknowledgement from a radio
  * **ANI** - automatic number identification for a radio
  * **Call Detect** - a call detected on another channel
  * **Call End** - call end event
  * **Call Start** - call start
  * **Call - No Tuner** - call start event when no tuner is available
  * **Call Timeout** - call end event was not received and call expire timer forced a timeout on the call event
  * **Emergency** - emergency notification from a radio
  * **ESN** - radio registering on the network with an ESN, or responding to an ESN interrogation
  * **GPS** - gps update from a radio
  * **Unique ID** - LTR-Net unique identifier for a radio
  * **Page** - the system is paging a radio
  * **Register** - radio registering on the network
  * **SDM** - short data message
  * **Status** - status message