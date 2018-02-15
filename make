#!/bin/sh

#rm -r bin/res
JAVAFILES=`find src | grep 'java$'`
javac -classpath "commons-cli-1.4.jar" -extdirs "" -d bin/ $JAVAFILES 2>&1 | egrep --color "^|error"
jar cmf MANIFEST.MF resolution.jar $JAVAFILES -C bin/ .
#cp -t sandbox resolution.jar


