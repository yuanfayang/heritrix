#!/bin/sh
export JAVA_HOME=$HOME/bin/j2sdk1.4.2_03
export PATH=$PATH:$JAVA_HOME/bin
export CVS_RSH=ssh
export MAVEN_HOME=$HOME/bin/maven-1.0-rc1
export CCDIR=$HOME/bin/cruisecontrol-2.1.4/main
export JETTY_HOME=$HOME/bin/Jetty-4.2.15rc0/
export CCBUILDDIR=$HOME/cc
if test ! -d ${CCBUILDDIR}/logs
then
    mkdir ${CCBUILDDIR}/logs
fi
# CC output goes into cruisecontrol.log.  No need of catching it for second
# time into a stdout/stderr file.
nohup $CCDIR/bin/cruisecontrol.sh -port 8081 &> /dev/null &
cd $JETTY_HOME
nohup $JAVA_HOME/bin/java -jar start.jar &> ${CCBUILDDIR}/logs/jetty.log &
