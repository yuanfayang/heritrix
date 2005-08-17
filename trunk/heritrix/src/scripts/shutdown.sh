#!/usr/bin/env sh
##
## This script shutsdown the local heritrix crawler. Must be run with jdk 1.5.0
## and the heritrix instance must be running jdk 1.5.0 with jmx enabled (Its on
## by default).
##
## Optional environment variables
##
## JAVA_HOME        Point at a JDK install to use.
## 
## HERITRIX_HOME    Pointer to your heritrix install.  If not present, we 
##                  make an educated guess based of position relative to this
##                  script.
## JMX_PORT         Port you'd like the JVM JMX administration server to run
##                  on. Default is 8849.
##
#set -e


usage () {
cat <<EOM
Usage: $0
Usage: $0 -h|--help
Options:
 -h   Prints out this message.
This script shuts down the local Heritrix instance by making a RMI connection to
the Heritrix JMX_PORT and invoking the 'stop' operation. This script will only
succeed if JAVA_HOME points to jdk 1.5.0 and the Heritrix instance is running
in a 1.5.0 JVM (with the default JMX listener enabled).
EOM
exit 0
}

while getopts "h" opt
do
    case $opt in
        *)
            usage
            ;;
    esac
done

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

# Set HERITRIX_HOME.
if [ -z "$HERITRIX_HOME" ]
then
    HERITRIX_HOME=`cd "$PRGDIR/.." ; pwd`
fi

# Find JAVA_HOME.
if [ -z "$JAVA_HOME" ]
then
  JAVA=`which java 2> /dev/null`
  if [ -z "$JAVA" ] 
  then
    echo "Cannot find JAVA. Please set your JAVA_HOME (or PATH)."
    exit 1
  fi
  JAVA_BINDIR=`dirname $JAVA`
  JAVA_HOME=$JAVA_BINDIR/..
fi

if [ -z "${JMX_PORT}" ]
then
    JMX_PORT=8849
fi

JMX_PWORD_FILE="${HERITRIX_HOME}/jmxremote.password"
if [ ! -e "${JMX_PWORD_FILE}" ]
then
   echo "Failed to find ${JMX_PWORD_FILE}"
   exit 1
fi
USERPWORD=`cat ${JMX_PWORD_FILE} | \
    sed -n -e 's/\(controlRole\) *\([^ ]*\)/\1:\2/p'`

# Check java version

version=`${JAVA_HOME}/bin/java -version 2>&1 |sed -n -e '/java version/p'`
case "$version" in
    *1.5.*) 
        :;;
    *) 
        echo "Requires version 1.5.0 of jdk.  Found $version"
        exit 1
    ;;
esac

${JAVA_HOME}/bin/java -jar ${HERITRIX_HOME}/bin/cmdline-jmxclient*.jar \
    ${USERPWORD} localhost:${JMX_PORT} \
    org.archive.crawler:name=Heritrix,type=Service shutdown
