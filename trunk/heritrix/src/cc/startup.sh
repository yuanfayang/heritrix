#!/bin/sh
#
export BUILD_HOME=/0/cruisecontrol
export HOME=$BUILD_HOME
export JAVA_HOME=$BUILD_HOME/bin/j2sdk1.4.2_03
export PATH=$PATH:$JAVA_HOME/bin
export CVS_RSH=ssh
export MAVEN_HOME=$BUILD_HOME/bin/maven
export CCDIR=$BUILD_HOME/bin/cruisecontrol/main
export JETTY_HOME=$BUILD_HOME/bin/jetty
if test ! -d ${BUILD_HOME}/logs
then
    mkdir ${BUILD_HOME}/logs
fi
# CC output goes into cruisecontrol.log.  No need of catching it for second
# time into a stdout/stderr file.
nohup $CCDIR/bin/cruisecontrol.sh -port 8081 \
    &> $BUILD_HOME/cruisecontrol_out.txt &
cd $JETTY_HOME
nohup $JAVA_HOME/bin/java -jar start.jar $BUILD_HOME/cc_jetty.xml \
    &> ${BUILD_HOME}/logs/jetty.log &
