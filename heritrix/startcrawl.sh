#!/bin/sh

##########################################################
##                                                      ##
## This will automate the environment setup and running ##
## of Heritrix following a successful build.  You 	##
## should only need to modify JAVA_HOME to make it work ##
##							##
##########################################################
                                                                                                                                 
                                                                                                                                 
## path to java home (not java executible)
#export JAVA_HOME=<PUT JAVA HOME HERE>
export JAVA_HOME=/usr/local/j2sdk1.4.0_02

## move into the run dir
cd RunThis

## move arc files generated by old runs (if applicable)
mkdir oldarcs
mv arcs/* oldarcs


echo "If you experience errors note that this"
echo "script needs to be run from the directory"
echo "in which it lives."

## run this sucker
$JAVA_HOME/bin/java -cp "lib/crawlerclasses.jar:lib/commons-httpclient.jar:lib/dnsjava.jar:lib/stataclasses.jar:lib/junit.jar:lib/commons-logging.jar" org.archive.crawler.Heritrix test-config/order.xml


