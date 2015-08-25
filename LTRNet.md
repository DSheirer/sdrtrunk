# LTR-Net Decoder #

The LTR-Net decoder will decode both Inbound Status Word (ISW) and Outbound Status Word (OSW) messaging.  ISW messages are transmitted by the mobile radio on the uplink, or input frequency of the repeater.  The network broadcasts OSW messages on the downlink or output frequency of the repeater.

Many of the LTR-Net system information messages are two-part messages.  In the decoded messages table, you will see references to part 1 of 2 or for ESNs, you may only see one half of the ESN (1234xxxx).

## Squelch Control ##

The LTR-Net decoder provides signalling-based squelch control.  If you have a decoding channel selected, then audio will play automatically during a call, and will be squelched at the end of the call, or upon call timeout.

## Channels ##

Each LTR-Net site supports 1-20 Logical Channel Numbers (LCN).  Each site will have a continuously transmitting Home Status Channel (HSC).

Each LCN transmits the same site configuration message set.  The HSC transmits these messages repeatedly, and the individual traffic channels transmit these messages one at a time, every few seconds.

The HSC will support traffic calls if the primary traffic channels become saturated.  In addition, the HSC will reflect all traffic call activity from all traffic channels on the site.  An effective strategy for determining site usage is to monitor the HSC and then look at the call event table to see call activity for the entire site.

## Activity Summary ##
The [Activity Summary](ActivitySummary.md) information window displays all decoded information about the current site (site id, channels, frequencies, talkgroups, etc.).

## Talkgroups ##

LTR-Net supports roaming of users across all sites in the system.  Each talkgroup is defined to be unique at a specific site, or unique across the network.

If you discover that a talkgroup is unique across the network, you can wildcard the talkgroup alias, so that that alias is used on every site in the systme for the talkgroup.  Wildcard the area and channel fields for a global talkgroup ( `*`-`*``*`**-123 )**

## Special Talkgroups ##

Talkgroups 240 - 247 are reserved for special network functions like PSTN telephone calls to/from a radio, or network phone patches.

## Unique ID ##
Each radio is assigned a unique ID by the system.  This supports radio identification to the network, and supports individual radio messaging by the network.  Unique IDs are 16--bit values ranging 1 - 65536.

## ESN ##
Each LTR-Net radio has a globally unique Electronic Serial Number that is set at the vendor factory.

## Decoding ESNs and Unique IDs ##

The mobile radio's Electronic Serial Number (ESN) is broadcast by the radio on the uplink frequency upon registration.  In response, the network broadcasts a radio Unique ID assignment on the downlink frequency to the mobile radio.  The radio then uses that unique ID for all subsequent communications.

You can discover these ESN and Unique ID assignments by decoding both the uplink and downlink frequencies for a specific channel at the same time.  Watch the uplink call event table for ESN messages.  Find the corresponding radio register message from the downlink frequency that matches the timestamp.

If the mobile radio later makes a call, you can discover the Unique ID and the talkgroup that the radio belongs to and the identity of the user.

## LTR-Net Aliasing ##

Setup an alias with all three pieces of information ( ESN, Unique ID,
and Talkgroup ) and you will then know when that radio is registering on the network, and which radio in the talkgroup is broadcasting.

### Aliases to use with LTR-Net ###

  * `*`-`*``*`-240 Special Talkgroup
  * `*`-`*``*`-241 Special Talkgroup
  * `*`-`*``*`-242 Special Talkgroup
  * `*`-`*``*`-243 Special Talkgroup
  * `*`-`*``*`-244 Special Talkgroup
  * `*`-`*``*`-245 Special Talkgroup
  * `*`-`*``*`-246 Special Talkgroup
  * `*`-`*``*`-247 Special Talkgroup
  * `*`-`*``*`-253 Radio Register
  * `*`-`*``*`-254 FCC CWID