#!/bin/sh
##
## This script launches the heritrix crawler w/ debugging enabled.
##
## Environment variable prerequisites
## 
## HERITRIX_HOME    Pointer to your heritrix install.
##
## JAVA_OPTS        (Optional) Java runtime options.  Default ''.
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
                                                                                
# Get standard environment variables
PRGDIR=`dirname "$PRG"`
HERITRIX_HOME=`cd "$PRGDIR/.." ; pwd`

XTRA_OPTS="-enableassertions -verbose:gc -Xloggc:${HERITRIX_LOGGING_DIR}/gc.log"
JAVA_OPTS="${JAVA_OPTS} ${XTRA_OPTS}" ${HERITRIX_HOME}/bin/heritrix.sh $@
