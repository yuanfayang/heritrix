#!/bin/sh
##
## This script launches the heritrix crawler.
##
##
## Environment variable prerequisites
##
## JAVA_HOME        Must point at your JDK install.
##
##
## Optional environment variables
## 
## HERITRIX_HOME    Pointer to your heritrix install.  If not present, we 
##                  make an educated guess based of position relative to this
##                  script.
##
## JAVA_OPTS        Java runtime options.
##

# Resolve links - $0 may be a softlink
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

# Read local heritrix properties if any.
if [ -f $HOME/.heritrixrc ]
then 
  . $HOME/.heritrixrc
fi

# Set HERITRIX_HOME.
if [ -z "$HERITRIX_HOME" ]
then
    HERITRIX_HOME=`cd "$PRGDIR/.." ; pwd`
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

if [ -z "$JAVACMD" ] 
then 
   # It may be defined in env - including flags!!
   JAVACMD=$JAVA_HOME/bin/java
fi

# Ignore previous classpath.  Build one that contains heritrix jar and content
# of the lib directory into the variable CP.
for jar in `ls $HERITRIX_HOME/lib/*.jar $HERITRIX_HOME/*.jar`
do
    CP=${CP}:${jar}
done

# Make sure of java opts.
if [ -z "$JAVA_OPTS" ]
then
  JAVA_OPTS=""
fi

# heritrix_dmesg.log contains startup output from the crawler main class. 
# As soon as content appears in this log, this shell script prints the 
# successful (or failed) startup content and moves off waiting on heritrix
# startup. This technique is done so we can show on the console startup 
# messages emitted by java subsequent to the redirect of stdout and stderr.
startMessage="${HERITRIX_HOME}/heritrix_dmesg.log"

# Remove any file that may have been left over from previous starts.
if [ -f $startMessage ]
then
    rm -f $startmessage
fi

# Run heritrix as daemon.  Redirect stdout and stderr to a file.
stdouterrlog=${HERITRIX_HOME}/heritrix_out.log
echo "`date` Starting heritrix" >> $stdouterrlog
main=org.archive.crawler.Heritrix
CLASSPATH=${CP} nohup $JAVA_HOME/bin/java -Dheritrix.home=${HERITRIX_HOME} \
    ${JAVA_OPTS} $main $@ >> ${stdouterrlog} 2>&1 &

# Wait for content in the heritrix_dmesg.log file.
echo -n "`date` Starting heritrix"
while true 
do
    sleep 1
    if [ -s $startMessage ]
    then
        echo
        cat $startMessage
        rm -f $startMessage
        break
    fi
    echo -n '.'
done
