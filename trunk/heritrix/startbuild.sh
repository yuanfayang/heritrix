#!/bin/sh

#########################################################
##							#
## This will automate the environment setup and running	#
## of the Heritrix ant build.  You will have to modify	#
## this file for your local environment.		#
##							#
######################################################### 

## path to ant's root directory
export ANT_HOME=<PUT ANT HOME HERE>

## path to java home (not java executible)
export JAVA_HOME=<PUT JAVA HOME HERE>

## method to authenticate
## set to 'ssh' if you're using a secure non-anon connection
## otherwise leaving this commented out is ok
#export CVS_RSH="ssh"


## don't worry about stuff below this line (unless you want to)

export PATH=$PATH:${ANT_HOME}/bin
export CVSREAD=1
export CVSROOT=/home/cvs

echo "If you experience errors note that this"
echo "script needs to be run from the directory"
echo "in which it lives."

## run ant with the first target passed (e.g. clean, all, nightly)
## or nightly if no arg is given
if [ $1 ]
then
	ant -f build.xml -logger org.apache.tools.ant.listener.MailLogger $1
else
	echo "defaulting to nightly build"
	ant -f build.xml -logger org.apache.tools.ant.listener.MailLogger nightly
fi
