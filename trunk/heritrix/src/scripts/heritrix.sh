#!/bin/sh
##
## This script launches the heritrix crawler.
##
## Environment variable prerequisites
## 
## HERITRIX_HOME    Pointer to your heritrix install.
##
## JAVA_HOME        Must point at your JDK install.
##
## JAVA_OPTS        (Optional) Java runtime options.  Default ''.
##
## TODO: Replace by just starting up java.

# Read local heritrix properties if any.
if [ -f $HOME/.heritrixrc ]
then 
  . $HOME/.heritrixrc
fi

# Check HERITRIX_HOME has been set.
if [ -z "$HERITRIX_HOME" ]
then
    # TODO: Add some detective work locating heritrix.
    echo "Before running this script, you must set the HERITRIX_HOME" 
    echo "environment variable so it points at your heritrix install." 
    exit 1
fi

# Find JAVA_HOME.
if [ -z "$JAVA_HOME" ]
then
  JAVA=`which java`
  if [ -z "$JAVA" ] 
  then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BINDIR=`dirname $JAVA`
  JAVA_HOME=$JAVA_BINDIR/..
fi

if [ "$JAVACMD" = "" ] 
then 
   # it may be defined in env - including flags!!
   JAVACMD=$JAVA_HOME/bin/java
fi

# Ignore previous classpath.  Heritrix jar has a launch location dependent 
# Class-Path define in its MANIFEST-MF that points to all jars its dependent on.
oldCP=$CLASSPATH
unset CLASSPATH

CLASSPATH=`ls ${HERITRIX_HOME}/heritrix*.jar`
for jar in `ls $HERITRIX_HOME/lib`
do
    CLASSPATH=${CLASSPATH}:${HERITRIX_HOME}/lib/${jar}
done

# Make sure of java opts.
if [ "$JAVA_OPTS" = "" ]
then
  JAVA_OPTS=""
fi


# Start log contains useful information on started crawler.
# This file is created by Heritrix main class on start up.
startlog="start.log"

# Clean up start log just in case that something whent wrong during previous 
# run.
if [ -f $startlog ]
then
    rm -f $startlog
fi

# Run heritrix as daemon
MAIN=org.archive.crawler.Heritrix
run="$JAVA_HOME/bin/java ${JAVA_OPTS} -classpath ${CLASSPATH} $MAIN $@"
nohup $run >> heritrix-run.log 2>> heritrix-run.err < /dev/null &

# Wait for creation of start log 
echo "Starting Heritrix... please wait!"
while [ ! -f $startlog ]
do
    sleep 1;
done

# Cat and clean up start log.
if [ -f $startlog ]
then
	cat $startlog
	rm -f $startlog
fi

# Restore any old CLASSPATH.
if [ "$oldCP" != "" ]; then
    CLASSPATH=${oldCP}
    export CLASSPATH
else
    unset CLASSPATH
fi
