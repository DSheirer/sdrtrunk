# Alias #

An alias is a user readable name that can be associated with one or more identifiers produced by the decoders.

![http://sdrtrunk.googlecode.com/svn/wiki/images/Alias.png](http://sdrtrunk.googlecode.com/svn/wiki/images/Alias.png)

## Creating an Alias ##

Right-click on the Alias Group folder and select **New Alias**.

## Deleting an Alias ##

Right-click on the Alias and select **Delete**

## Configuring an Alias ##

  * **Name** - display name for the alias
  * **Map Color** - for decoders that produce (GPS) locations where the entity can be plotted on the map, designates the color of the label and history trail used for that entity
  * **Map Icon** - picture to display with the alias
  * **[Icon Manager](IconManager.md)** - tool for managing and importing new pictures to use with Aliases.
  * **Save** - saves any changes to the alias
  * **Reset** - resets the alias with the last saved values

## Alias Identifiers ##

Each message and event produced by the decoders contains user and system identifiers that can be aliased.  Each alias can be associated with one or more alias identifiers.

### Alias Identifier Wildcards ###

Each of the alias identifiers (except LTR-Net unique ID and Status) allow you to specify a single character wildcard (asterisk) or a full java regular expression to use when matching identifiers.

Examples:

```
0-**-123                 LTR talkgroup 123 on any LCN
ABCD12**                 Any electronic serial numbers beginning with ABCD12
(?!FA-40|FA-41|FA-42).*  Any lojack site ID except for FA-40, FA-41, or FA-42 (use '|' to separate each site identifier)
```

### Create an Alias Identifier ###

Right-click on the Alias folder and select the type of alias identifer you want to associate with the Alias.

### Delete an Alias Identifier ###

Right-click on the Alias identifier and select **Delete**.

## Alias Identifier Types ##

  * **ESN** - electronic serial number.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasESNID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasESNID.png)

  * **Fleetsync** - fleetsync identifier.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasFleetsyncID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasFleetsyncID.png)

  * **MDC-1200** - MDC-1200 identifier.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMDC1200ID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMDC1200ID.png)

  * **MIN** - passport radio Mobile ID Number (MIN).

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMIDID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMIDID.png)

  * **MPT-1327** - MPT-1327 user or group identifier.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMPT1327ID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasMPT1327ID.png)

  * **Site** - site number.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasSiteID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasSiteID.png)

  * **Talkgroup** - used with LTR and Passport talkgroups.  **TGID** identifies the formatted talkgroup.  **Audio** identifies how the audio from a call for this talkgroup should be handled : _Normal, Muted, or Inverted_.

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasTalkgroupID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasTalkgroupID.png)

  * **Unique ID** - LTR-Net mobile radio unique ID (UID).

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasUniqueID.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasUniqueID.png)