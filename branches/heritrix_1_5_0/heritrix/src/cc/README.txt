______________________________________________________________________________
$Id$
_______________________________________________________________________________

This submodule 'hertrix-cc' contains the cruisecontrol (cc) build directory
and cc config files for the running of the Internet Archive ArchiveOpenCrawler
(heritrix) continuous integration build.

See http://cruisecontrol.sourceforge.net to learn about CruiseControl.
See http://crawler.archive.org to learn about the Internet Archive heritrix
open crawler project.


HOW-TO SETUP CRUISECONTROL (CC) BUILD

Below, we first describe setting up ssh CVS access using the PKI keys of the
user designated running the build.  Once keys are all in place, we then
describe install and configuration of cruisecontrol (cc).

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
After posting your key and waiting the requisite ten minutes (It often seems to
take way longer than ten minutes), check that all is working by doing a ssh to
shell.sourceforge.net from the build machine.  If you don't  have to type in a
password to gain access, then your public key has been taken by sourceforge. 
Test that CVS operations don't require a password by doing something like the
following:

    % export CVS_RSH=ssh
    % cvs -z3 -d:ext:LOGIN_NAME@cvs.sourceforge.net:/cvsroot/archive-crawler\        co ArchiveOpenCrawler

Above should checkout the crawler w/o requiring a password (In the above the 
LOGIN_NAME needs to be replaced by the name of the designated build user, the 
user whos' key has been registered at sourceforge).  See the end of the
sourceforge CVS page for more on ssh CVS access:
http://sourceforge.net/cvs/?group_id=73833

+ Install java.  Set JAVA_HOME.  You also have to add the java bin to your 
path:

    % export PATH=$PATH:$JAVA_HOME/bin

+ Install maven.  Set MAVEN_HOME.  You also have to add the maven bin to your
path:

    % export PATH=$PATH:$MAVEN_HOME/bin 

Later we set an alternate location for plugin and jar repository -- a location
other than default user-particular location.  Watch out for it below when we
set build.properties for our project.

+ In addition to the base maven build, you will need to add the maven sdocbook
plugin which can be found on this page: 
http://sourceforge.net/projects/maven-plugins/.  This plugin is responsible for
generating the user and developer documentation.  The sdocbook plugin has a
dependency on the jimi jar from sun which you will have to manually pull down
and place into your maven respository (Had to unzip the jini zip file and
repackage it as a jar to make maven happy).  Jini can be found here:
http://java.sun.com/products/jimi/

+ Install cruisecontrol.  This doc. was done w/ 2.1.6.  You have to build it
after downloading.
    
    % cd cruisecontrol-2.1.6/main
    % chmod u+x build.sh
    % ./build.sh

+ Note that I had to up the amount of memory cc uses.  To do this, I edited
the cruisecontrol.sh start script at main/bin.  I changed the EXEC line to 
read:

    EXEC="java -Xmx256m -cp $CRUISE_PATH CruiseControl $@"

I added the -Xmx256m after 'java'.

+ After installing java and cruisecontrol, checkout the 'heritrix/src/cc', This
is the checked-in cruisecontrol directory structure  (This README.txt is in the
root of the 'heritrix/src/cc' subodule).  Check it out to a location that can
tolerate large files accumulating over time (The below checks out 
'heritrix/src/cc' to a directory named 'cc'):

    % export CVS_RSH=ssh
    % cvs -z3 -d:ext:developername@cvs.sourceforge.net:/cvsroot/archive-crawler       co -d cc heritrix/src/cc

+ Once you've checked out 'heritrix/src/cc', go into the checked out dir and 
create three directories: artifacts, checkout and logs (See 
http://c2.com/w4/cc/wiki.cgi?GettingStartedWithCruiseControl).

    % cd cc
    % mkdir artifacts checkout logs

+ Now checkout the ArchiveOpenCrawler project under cc/checkout dir.

    % export CCBUILDDIR=/PATH/TO/YOUR/CHECKED/OUT/CC/DIR
    % cd $CCBUILDDIR/checkout
    % export CVS_RSH=ssh
    % cvs -z3 -d:ext:stack-sf@cvs.sourceforge.net:/cvsroot/archive-crawler \
        checkout ArchiveOpenCrawler

+ Once you've checkedout the crawler, you need to add to it a build.properties
file that has overrides necessary to the continuous build environment.  Here is
a sample:

    #Updated by build-ArchiveOpenCrawler.xml
    #Mon Apr 26 16:58:33 PDT 2004
    version.build.suffix=-${version.build.timestamp}
    version.build.timestamp=200404261658
    maven.username=stack-sf
    maven.home.local=/0/cruisecontrol/maven.home.local

This file is updated by the continuous build as it runs.  It has the name of
the user who's key is up at sourceforge and it has location of the maven
local directory into which it will download jars and in which it expects to
find plugins.  When done, there should be a file named 
$CCBUILDDIR/checkout/ArchiveOpenCrawler/build.properties.

+ Now set up the continuous build webserver. See item 6, "Building the web app",
on this page: http://c2.com/w4/cc/wiki.cgi?GettingStartedWithCruiseControl.
Set up the 'override.properties' file making it point into $CCBUILDDIR and then
'chmod u+x build.sh' and then './build.sh war' under the cruisecontrol
reporting/jsp dir.  Make sure the build completes.  I deployed the generated
war under a tomcat instance. The web.xml used in the cruisecontrol.war
is checked in here for comparisons sake.

+ Run cruisecontrol.  See the start.sh in this directory for sample.
It sets up all environment variables and configuration including a custom
jetty 'jetty.xml' file.  Make sure its all running fine. Check the logs dir
under CCBUILDDIR, visit the website at HOST:8080, and visit the jmx server at
HOST:8081.
