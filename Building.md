# Downloading and Compiling sdrtrunk #

This page describes several methods for checking out the source code from the SVN server and building the sdrtrunk application on your local computer.

## Eclipse IDE ##

If you use the [Eclipse IDE for Java](http://www.eclipse.org/downloads/), you can download and import the sdrtrunk project directly into Eclipse.

  1. Install an eclipse svn plugin ( Help > Install New Software, choose the Luna site, Collaboration > Subversive SVN )
  1. Project Explorer > Import > SVN > Project from SVN
  1. Create a new repository location: https://sdrtrunk.googlecode.com/svn
> or http://sdrtrunk.googlecode.com/svn/trunk/
  1. Open the ant view ( Window > Show View > Ant )
  1. Drag the ant build script from the Package Explorer (sdrtrunk/build/build.xml) into the ant view panel
  1. Click the play button to execute the default build target
  1. Compiled software package is placed in the sdrtrunk/product/xxx.zip folder

## Manually ##

  1. Install the Java JDK version 1.7 or higher and create a JAVA\_HOME environment variable pointing to the JDK installation folder
  1. Install an SVN client on your computer: [Tortoise SVN](http://tortoisesvn.net/downloads.html) on windows or kdesvn on Linux
  1. Checkout the source code from https://sdrtrunk.googlecode.com/svn
> or http://sdrtrunk.googlecode.com/svn/trunk/
  1. Install the latest version of [Ant](http://ant.apache.org/) and follow the installation instructions in the [manual](http://ant.apache.org/manual/index.html)
  1. Change to the sdrtrunk build directory ( sdrtrunk/build )
  1. Execute the ant command.

Ant will look for a file named build.xml in the current folder and will run the the default build target.  The compiled and zipped output program file will be located in the sdrtrunk/product/xxx.zip folder.