#!/bin/sh

# You will need to generate a PGP Signature. See https://stackoverflow.com/questions/28846802/how-to-manually-publish-jar-to-maven-central

echo "========================== START =========================="
echo "Checking brew installed..."
brewInstalled=$(type brew 2>/dev/null)
if [ "$brewInstalled" = "" ]; then
    echo "Installing brew..."

    /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
else
    echo "Brew installed.";
fi

echo "Checking GPG installed..."
gpgInstalled=$(type gpg 2>/dev/null)
if [ "gpgInstalled" = "" ]; then
    echo "Installing GPG..."

    brew install gpg
else
    echo "GPG installed.";
fi

echo ""
read -p  "Enter version number: " input
echo ""

# sign and bundle annotations
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
echo ""

# sign and bundle maven plugin
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
echo ""

# sign and bundle parent
directory=~/.m2/repository/com/michaelhradek/aurkitu-parent/"$input"/
ls -l $directory
cd $directory
rm _remote.repositories

gpg -ab *"$input".pom

jar -cvf aurkitu-parent-"$input"-bundle.jar .
cp *-bundle.jar $directory/../../

# Any files with the words 'lastUpdated' in them are invalid and it means that you should delete the repo directory and then rebuild the project.
echo ""

echo "========================== FINISH =========================="
echo "JAR files located at: ~/.m2/repository/com/michaelhradek"
