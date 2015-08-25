# Passport #

The passport decoder will decode the Outbound Status Word (OSW) messages and signalling that are transmitted by the network to the mobile radio.

## Talkgroups ##

User mobile radios are normally assigned the talkgroup identifiers starting from 1, upward.  Group talkgroups are normally assigned from the highest talkgroup numbers working their way downward.  You can create an alias for each talkgroup.

## Mobile ID Number (MIN) ##

Each passport mobile radio on the network is assigned a MIN.  You can create an alias for each MIN.

## Squelch Control ##

The Passport decoder provides signalling-based squelch control.  If you have a decoding channel selected, then audio will play automatically during a call, and will be squelched at the end of the call, or upon call timeout.

## Aliases ##

If you are in the United States, and your Passport network occasionally broadcasts its FCC station callsign, then add the following alias to each alias list you are using for Passport:

  * 65535 FCC CWID

## Passport Decode Example ##

![http://sdrtrunk.googlecode.com/svn/wiki/images/passport_decoding.png](http://sdrtrunk.googlecode.com/svn/wiki/images/passport_decoding.png)