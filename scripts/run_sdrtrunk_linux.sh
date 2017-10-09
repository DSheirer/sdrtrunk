#!/bin/bash

java -version

# Test for java version 9
JAVA_9=$(java -version 2>&1 | grep -i version | sed 's/.* "\([9]\).*/\1/')

if [ -z ${JAVA_9+x} ]; then
  echo Using Java 9 program options
  # Java 9 uses the G1 garbage collector by default and requires that we specify the
  # java.xml.bind package since it's been removed from the java se core library
  java -cp "*:libs/*" --add-modules java.xml.bind gui.SDRTrunk;
else
  echo Using Java 8 program options
  java -XX:+UseG1GC -cp "*:libs/*" gui.SDRTrunk;
fi
