# Introduction #

Channel Maps are used by some decoders to specify the channel number to frequency mapping used by a system.  Most decoders do not require a channel map.  Channel Maps allow the trunking controller to tell a mobile radio to go to a channel number for a radio call, without having to specify the frequency.  An accurate channel map is essential for decoders that support traffic channel following, so that SDRTrunk can tune to the correct traffic channel frequency to decode the traffic channel call.

![http://sdrtrunk.googlecode.com/svn/wiki/images/ChannelMap.png](http://sdrtrunk.googlecode.com/svn/wiki/images/ChannelMap.png)

## Create a Channel Map ##

Right-click on the Channel Maps folder and selected **New Channel Map**

## Delete a Channel Map ##

Right-click on the Channel Map and select **Delete**

## Channel Ranges ##

A channel map contains one or more channel ranges.  A range defines a range of channel numbers, the base frequency, and the channel size.  A simple math formula produces the channel frequency by multiplying the channel number by the channel size, and adding that value to the base frequency.  A channel map can contain multiple channel ranges to support systems where the channel frequencies are not allocated in a contiguous manner.

### Add a Channel Range ###

Click 'New Range'.

### Delete a Channel Range ###

Highlight the range and click 'Delete'

## Configuring a Channel Map ##

  * **Name** - Update the name for your channel map.  Once you have finished updating the channel map, you can then select this name in the channel's decoder configuration tab.

  * **Range** - update the default channel range in the table with beginning and ending channel numbers, base frequency, and channel size.  Use the **New Range** button to add additional channel ranges to the channel map.  Use the **Delete** button to delete any channel ranges that aren't needed.

  * **Save** - saves the current configuration.

  * **Reset** - resets the channel map to the last saved state