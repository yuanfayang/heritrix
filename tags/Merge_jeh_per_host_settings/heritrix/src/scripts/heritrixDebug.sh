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

# Check HERITRIX_HOME has been set.
if [ -z "$HERITRIX_HOME" ]
then
    # TODO: Add some detective work locating heritrix.
    echo "Before running this script, you must set the HERITRIX_HOME" 
    echo "environment variable so it points at your heritrix install." 
    exit 1
fi

# We need a LOGGING_DIR to write stdout and stdin to.
if [ -z "${HERITRIX_LOGGING_DIR}" ]
then
    # TODO: Do this hardcoding better.  Read from order file.
    HERITRIX_LOGGING_DIR=${HERITRIX_HOME}/disk
fi

XTRA_OPTS="-enableassertions -verbose:gc -Xloggc:${HERITRIX_LOGGING_DIR}/gc.log"
JAVA_OPTS="${JAVA_OPTS} ${XTRA_OPTS}" $HERITRIX_HOME/bin/heritrix.sh $@
