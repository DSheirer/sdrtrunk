# Alias Action #

You can assign one or more alias actions to an alias that will be executed when the alias becomes active in a decoded message.  Each message type can produce one or more aliases against any of the identifiers in the message that can be associated with an alias.

## Interval ##

Interval defines when and how often the action will occur.

**Once** - the action will only occur once when the alias is first active for the duration that sdrtrunk is running.

**Once, Reset After Delay** - the action will occur when the alias is first active, but will suppress any further actions for a specified number of seconds.  Once the delay period has elapsed, this action will again alert in the future if the alias is active.

**Until Dismissed** - the action will occur when the alias is first noted active and will continue to occur every specified number of seconds.  A dialog window with an OK button will pop up and the action will repeat until the user clicks the OK button.  The action will reset 15 seconds after the OK button is pressed and will alert again in the future if the alias is active.

## Period ##

Specifies the time period in seconds for the two interval options that require a time period setting.

# Available Alias Actions #

![http://sdrtrunk.googlecode.com/svn/wiki/images/AliasActions.png](http://sdrtrunk.googlecode.com/svn/wiki/images/AliasActions.png)

## Audio Clip ##

Plays the audio clip specified by the file name.  The audio clip action provides a file selection button and a test button to allow you to play the selected audio file to ensure that the file is a supported audio type.

## Beep ##

Beeps the computer

## Script ##

The script action allows you to select a script, batch file, or program to execute when the action occurs.  Select the program from the File button and use the Test button to test the script to ensure that the script operates correctly.

![http://sdrtrunk.googlecode.com/svn/wiki/images/ScriptAction.png](http://sdrtrunk.googlecode.com/svn/wiki/images/ScriptAction.png)

# Errors #

If any errors are encountered playing an audio clip or executing a script, a popup window will appear providing details on the error.

# How To Assign An Alias Action #

  * Right-click on an alias
  * Select the Add Action menu and choose from the list of available  actions
  * Ensure the newly added alias action is highlighted in the tree
  * Configure the interval and period settings
  * Optionally select a file and test the action against the file
  * Click the Save button at the bottom of the configuration window