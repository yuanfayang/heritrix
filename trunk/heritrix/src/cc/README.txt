______________________________________________________________________________
$Id$
_______________________________________________________________________________

This directory contains the cruisecontrol (cc) config file for the running of
the Internet Archive ArchiveOpenCrawler (heritrix) continuous integration build
as well as supporting ant files and this README.

See http://cruisecontrol.sourceforge.net to learn about CruiseControl.
See http://crawler.archive.org to learn about the Internet Archive heritrix
open crawler project.


HOW-TO SETUP CRUISECONTROL (CC) BUILD

Below, we first describe setting up ssh CVS access using the PKI keys of the
user designated as the user who runs the build.  Once keys are all in place, we
then describe install and configuration of cruisecontrol (cc).

+ Choose user who will be running the build.  This user will need to have 
ssh CVS access to the sourceforge repository.

+ Set up ssh keys so build user can do ssh CVS activities without need of a 
password from the build machine. The automated continuous build requires such
access.  As long as the build user's private key is guarded, running ssh
CVS w/o use of a password should be secure enough.  To generate the build users'
private/public key pair on the machine the builds will be run on, do the 
following in the build user's home directory to generate an ssh2
private/public key pair:

    % $ ssh-keygen -t dsa
    Generating public/private dsa key pair.
    Enter file in which to save the key (/home/stack/.ssh/id_dsa):
    Enter passphrase (empty for no passphrase):
    Enter same passphrase again:
    Your identification has been saved in /home/stack/.ssh/id_dsa.
    Your public key has been saved in /home/stack/.ssh/id_dsa.pub.
    The key fingerprint is:
    9a:37:07:75:80:96:80:ff:63:df:d1:91:73:9d:a3:8d stack@crawl08.archive.org

+ Once above is done, you need to pass sourceforge the public key.
Here is the sourceforge guide on how to post your public key:
http://sourceforge.net/docman/display_doc.php?docid=761&group_id=1
After posting your key and waiting the requisite ten minutes, check that all
is working by doing a ssh to shell.sourceforge.net from the build machine.  If
you don't  have to type in a password to gain access, then your public key has
been taken by sourceforge.  Test that CVS operations don't require a password
by doing something like the following:

    % export CVS_RSH=ssh
    % cvs -z3 -d:ext:developername@cvs.sourceforge.net:/cvsroot/archive-crawler\        co ArchiveOpenCrawler

Above should checkout the crawler w/o requiring a password.  See the end 
of the sourceforge CVS page for more on ssh CVS access:
http://sourceforge.net/cvs/?group_id=73833

+ Install java.  Set JAVA_HOME.  You also have to add the java bin to your 
path:

    % export PATH=$PATH:$JAVA_HOME/bin

+ Install maven.  Set MAVEN_HOME.  You also have to add the maven bin to your
path:

    % export PATH=$PATH:$MAVEN_HOME/bin

+ Install the maven sdocbook plugin by copying it to $MAVEN_HOME/plugins
Its used generating docbook articles.  You'll also need to manually add
the jimi jar to your maven repository. Its needed by sdocbook and its
sun licensed so you'll have to go pull it down yourself.  Maven can't do 
it for you.  I undid the JimiProClasses.zip file and redid is a jar called
jimi-1.0.jar.
    
+ Install cruisecontrol.  This doc. was done w/ 2.1.4.  You have to build it
after downloading (This is the best doc. I found on cruisecontrol setup:
http://c2.com/w4/cc/wiki.cgi?GettingStartedWithCruiseControl).
    
    % cd cruisecontrol-2.1.4/main
    % chmod u+x build.sh
    % ./build.sh

+ After installing java and cruisecontrol, get the cc config file for 
ArchiveOpenCrawler from sourceforge.  Its in the ArchiveOpenCrawler module
under src/cc (This README.txt sits beside it).  Its named config.xml.

+ Make a directory in which cc will do its building.  Make it in a location
that can tolerate a large amount of accumulating data over time.

    % cd /DIR/THAT/CAN/TOLERATE/LARGE/DATA/OVER/TIME
    % mkdir cc
    % export CCBUILDDIR=/DIR/THAT/CAN/TOLERATE/LARGE/DATA/OVER/TIME/cc

+ Once you've created 'cc' ($CCBUILDDIR), go into $CCBUILDDIR and create three
directories: artifacts, checkout and logs (See 
http://c2.com/w4/cc/wiki.cgi?GettingStartedWithCruiseControl for more).

    % cd $CCBUILDDIR
    % mkdir artifacts checkout logs

+ Now checkout the ArchiveOpenCrawler project under heritrix-cc/checkout dir.

    % cd $CCBUILDDIR/checkout
    % export CVS_RSH=ssh
    % cvs -z3 -d:ext:stack-sf@cvs.sourceforge.net:/cvsroot/archive-crawler \
        checkout ArchiveOpenCrawler

+ Now set up the continuous build webserver. See item 6, "Building the web app",
on this page: http://c2.com/w4/cc/wiki.cgi?GettingStartedWithCruiseControl.
Set up the 'override.properties' file making it point into $CCBUILDDIR and then
'chmod u+x build.sh' and then './build.sh war' under the cruisecontrol
reporting/jsp dir.  Make sure the build completes.  I deployed the generated
war under a jetty instance.  I downloaded jetty, copied it into place, played
w/ settings under the jetty etc/ dir (jetty.xml) and then did
'java -Djetty.port=8080 -jar start.jar' under the jetty dir (Note, our crawl??
machines only allow web access on port 8080).

+ Run cruisecontrol.  Here's a sample startup script:
    
    #!/bin/sh
    export JAVA_HOME=$HOME/bin/j2sdk1.4.2_03
    export PATH=$PATH:$JAVA_HOME/bin
    export CVS_RSH=ssh
    export MAVEN_HOME=$HOME/bin/maven-1.0-rc1
    export CCDIR=$HOME/bin/cruisecontrol-2.1.4/main
    export JETTY_HOME=$HOME/bin/Jetty-4.2.15rc0/
    export CCBUILDDIR=$HOME/cc
    mkdir ${CCBUILDDIR}/logs
    nohup $CCDIR/bin/cruisecontrol.sh -port 8081 \
        &> ${CCBUILDDIR}/logs/stdout_err.log &
    cd $JETTY_HOME
    nohup $JAVA_HOME/bin/java -jar start.jar &>
    ${CCBUILDDIR}/logs/jetty.log &

Make sure its all running fine (Check the logs dir under CCBUILDDIR, 
visit the website at HOST:8080, and visit the jmx server at HOST:8081).
