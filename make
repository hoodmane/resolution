#!/bin/sh

#rm -r bin/res
JAVAFILES=`find src | grep 'java$'`
javac -classpath "lib/*" -d bin/ $JAVAFILES 2>&1 | egrep --color "^|error"
cp src/*.jar bin
jar cmf MANIFEST.MF resolution.jar $JAVAFILES -C bin/ .
