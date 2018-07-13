#!/bin/sh

read -p  "Enter version number: " input

directory=~/.m2/repository/com/michaelhradek/aurkitu-annotations/"$input"/
ls -l $directory
cd $directory
rm _remote.repositories
 
gpg -ab *"$input"-javadoc.jar
gpg -ab *"$input"-sources.jar
gpg -ab *"$input".jar
gpg -ab *"$input".pom 
 
jar -cvf aurkitu-annotations-"$input"-bundle.jar .
cp *-bundle.jar $directory/../../

directory=~/.m2/repository/com/michaelhradek/aurkitu-maven-plugin/"$input"/
ls -l $directory
cd $directory
rm _remote.repositories

gpg -ab *"$input"-javadoc.jar
gpg -ab *"$input"-sources.jar
gpg -ab *"$input".jar
gpg -ab *"$input".pom 

jar -cvf aurkitu-maven-plugin-"$input"-bundle.jar .
cp *-bundle.jar $directory/../../
