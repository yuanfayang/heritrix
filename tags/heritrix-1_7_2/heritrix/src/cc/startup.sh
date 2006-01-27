#!/bin/sh
export BUILD_HOME=/0/cruisecontrol
export HOME=$BUILD_HOME
export JAVA_HOME=$BUILD_HOME/bin/jdk1.5.0_01/
export PATH=$PATH:$JAVA_HOME/bin
export CVS_RSH=ssh
export MAVEN_HOME=$BUILD_HOME/bin/maven
export CCDIR=$BUILD_HOME/bin/cruisecontrol/main
export TOMCAT_HOME=$BUILD_HOME/bin/jakarta-tomcat-4.1.29/

# Make sure logs dir exists.
if test ! -d ${BUILD_HOME}/logs
then
    mkdir ${BUILD_HOME}/logs
fi

# CC output goes into cruisecontrol.log.  No need of catching it for second
# time into a stdout/stderr file.
nohup $CCDIR/bin/cruisecontrol.sh -port 8081 -debug -rmiport=1099 \
    -configfile $HOME/config.xml &> $HOME/cc.out.log &
# Start tomcat.
export JAVA_HOME
$TOMCAT_HOME/bin/startup.sh
