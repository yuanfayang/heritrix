#!/bin/sh

#########################################################
##							#
## This will automate the environment setup and running	#
## of the Heritrix ant build.  You will have to modify	#
## this file for your local environment.		#
##							#
######################################################### 

# make sure they specified a build file
if [ ! $1 ]; then
	echo ""
	echo "  usage: $0 <build file> [target]"
	echo ""
	exit
fi

# make sure CRAWL_HOME is set
if [ ! $CRAWL_HOME ]; then
        echo ""
        echo "  Before running this script you must set"
        echo "  the environemnt variable CRAWL_HOME"
        echo ""
        exit
fi
                                                                                
## the following block reads common (shared) environment
## variables from the configuration file
                                                                                
# set field seperator to split on newlines
IFS='
'
for line in `cat $CRAWL_HOME/environment.txt`
do
        notcomment=${line##\#*}
        if [ $notcomment ]; then
                export $line
        fi
done


## make sure we got what we needed from the 
## env var file
if [ ! $JAVA_HOME ] || [ ! ANT_HOME ]; then
	echo ""
	echo "  You have failed to set (manually or via the 'environment.txt'"
	echo "  file either JAVA_HOME, ANT_HOME, or both.  Please make sure "
	echo "  these are set before running this script.  See the docs for "
	echo "  more information."
	echo ""
	exit
fi

export PATH=$PATH:${ANT_HOME}/bin
export CVSREAD=1
export CVSROOT=/cvsroot/archive-crawler

## build it, for they've already come
if [ ! $2 ] 
then
	ant -f $1 -logger org.apache.tools.ant.listener.MailLogger $2 
elif [ $2 = "build-by-tag" ]
then
	if [ $3 ]; then
		ant -f $1 -logger org.apache.tools.ant.listener.MailLogger $2 -DcvsTag=$3
	else
		echo "build-by-tag usage: $0 <build file> build-by-tag <build-label>"
	fi
else
	ant -f $1 -logger org.apache.tools.ant.listener.MailLogger $2
fi
