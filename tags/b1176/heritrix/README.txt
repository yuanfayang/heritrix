----------------------------------------------------------------------
$Id$
----------------------------------------------------------------------
Heritrix is the Internet Archive's open-source, extensible, web-scale, 
archival-quality web crawler.  See <http://crawler.archive.org> for
project details. 

Webmasters!  Heritrix is designed to respect the robots.txt exclusion
directives and META robots tags. If you notice our crawler behaving 
poorly, please send us email at archive-crawler-agent *at* lists *dot* 
sourceforge *dot* net.


Table of Contents

I.   Before You Begin
  a. Requirements
  
II.  Getting Started
  a. Building
  b. Starting Heritrix
  
III. Configuration Files

IV.   Crawling
  a. ARC Files
  b. The Graphical User Interface
  c. The Command Line Interface

V.   License

VI.  Miscellaneous

   
I. Before You Begin

a.  Requirements

i. Java Runtime Environment

The Heritrix crawler is implemented purely in Java.  This means that the
only true requirement for running it is that you have a Java Runtime
Environment (JRE) installed.  The Heritrix crawler makes use of 1.4 
features so your JRE must be at least of a 1.4.0 pedigree. The Sun Java 
runtimes for many platforms are available at <http://java.com>.

ii. Linux

The Heritrix crawler has been primarily built and tested on Linux.  It
may perform acceptably elsewhere due to Java portability.
iii. Building

You can build Heritrix from source using Ant or Maven.  The Maven build 
is more comprehensive and will generate all from either the packaged 
source ofrom a CVS checkout.  The Ant build is less complete in that it 
doesn't generate the distribution documentation but it does produce all 
else needed to run Heritrix.

If you are building Heritrix w/ Ant, you must have Ant installed.  You 
can get Ant here: <http://ant.apache.org/>.  Our build used 1.5.x Ant.  
If you want to run the Heritrix unit tests from Ant, you will have to 
make sure the Ant optional.jar file sits beside the junit.jar.  See 
<http://ant.apache.org/manual/OptionalTasks/junit.html> for what you 
must do setting up Ant to run junit tests.

The Heritrix maven build was developed using 1.0-rc1 Maven.  You can get 
Maven from here: <http://maven.apache.org>.


II.  Getting Started

There are three ways to obtain Heritrix: 
  (1) packaged binary download from:
      <http://sourceforge.net/projects/archive-crawler>
  (2) packaged source download from:
      <http://sourceforge.net/projects/archive-crawler>
  (3) checkout from CVS
      cvs.sourceforge.net:/cvsroot/archive-crawler

The packaged binary is named heritrix-?.?.?.tar.gz or heritrix-?.?.?.zip 
and the packaged source is named heritrix-?.?.?-src.tar.gz or 
heritrix-?.?.?-src.zip where '?.?.?' is the current heritrix release 
version.

For how to get Heritrix from CVS, see 
<http://sourceforge.net/cvs/?group_id=73833>.  Be aware that anonymous 
access does not give you the current HEAD but a snapshot that can at 
times be up to 24 hours behind current development.

a. Building

i. If you obtained packaged source, here is how you build w/ Ant:

    % tar xfz heritrix-?.?.?-src.tar.gz
    % cd heritrix-?.?.?
    % $ANT_HOME/bin/ant dist

In the 'dist' subdir will be all you need to run the Heritrix crawler. 
To learn more about the ant build, type 'ant -projecthelp'.

To build a CVS source checkout w/ Maven:

    $ cd CVS_CHECKOUT_DIR
    $ $MAVEN_HOME/bin/maven dist

In the 'target/distribution' subdir, you will find packaged source and 
binary builds. Run '$MAVEN_HOME/bin/maven -g' for other Maven 
possibilities.

b. Starting Heritrix

To run Heritrix, first do the following:

    % export $HERITRIX_HOME=/PATH/TO/BUILT/HERITRIX

...where $HERITRIX_HOME is the location of your built Heritrix (i.e.  
under the 'dist' dir if you built w/ Ant, or under the untarred  binary
target/distribution/heritrix.?.?.?.tar.gz dir if you built w/ Maven, or 
under the untarred heritrix.?.?.?.tar.gz if you pulled a packaged 
binary).

Next run:

    % cd $HERITRIX_HOME
    % chmod u+x $HERITRIX_HOME/bin/heritrix.sh
    % $HERITRIX_HOME/bin/heritrix --help

This should output something like the following:

    Heritrix: Version unknown. Build unknown
    USAGE: java Heritrix [--no-wui | --port:xxxx] ORDER.XML
            [--start | --wait | --set-as-default] [-?]
        --no-wui    Start crawler without Web User Interface
        --port:xxxx The port that the web UI will run on, 8080 is default
        ORDER.XML   The "crawl order file" to launch. Optional if --no-wui not
                    specified, in which case the next parameter controls it's
                    behavior.  
    Only if --no-wui is NOT selected and a "crawl order file" file IS specified:
       --start      Start crawling as specified by the given crawl order file.
       --wait       Load the job specified by the given crawl order file but
                    do not start crawling. Default behavior.
       --set-as-default Set the specified crawl order as default crawl order
       -?           Display this message

The "crawl order file" or "order.xml" is the "master config file".  It
specifies which modules will be used to  process URIs, in which order 
URIs will be processed, how and where files will be written to disk, how 
"polite" the crawler should be, crawl limits, etc.  The configuration 
system is currently undergoing revision and the format of order.xml will 
be changed.  The best thing to do meantime is to copy an existing 
"order.xml" file.  See under 'docs/example-settings/broad-crawl' for an 
up-to-date sample configuration that does a broad crawl (If there is no 
docs/example-settings in your built distribution, see 
http://crawler.archive.org/docs/example-settings/broad-crawl/order.xml).

Before you begin crawling you *MUST* at least change the default 
"User-Agent" and "From" header fields in the order.xml (or via the
administrative interface).  You should set these to something meaningful 
that allows administrators of sites you'll be crawling to contact you.
The software requires that User-Agent value be of the form...

	  [name] (+[http-url])[optional-etc]

...where [name] is the crawler identifier and [http-url] is an URL 
giving more information about your crawling efforts. If desired,
additional info may be placed after the close-parenthesis.

Also, the From value must be an email address.
   
(Please do not leave the Archive Open Crawler project's contact 
information in these fields, we do not have the time or the resources to 
field complaints about crawlers which we do not administer.)

Once you have an order.xml file edited to your liking you can run the 
crawler either via the UI or without.  Here is how you'd run it w/o 
going via the UI:

    $ $HERITRIX_HOME/bin/heritrix.sh --no-wui order.xml

You should see output showing the crawler running.  Tail the logs, see 
your order.xml for where you told the crawler to dump them, to monitor 
crawler progress.

To start the crawler w/ the UI enabled run the following:

    $ $HERITRIX_HOME/bin/heritrix.sh

You should see output like the following:

    14:11:10.415 EVENT  Starting Jetty/4.2.15rc0
    14:11:10.603 EVENT  Checking Resource aliases
    14:11:10.832 EVENT  Started WebApplicationContext[/admin,Admin]
    14:11:11.094 EVENT  Started SocketListener on 0.0.0.0:8080
    14:11:11.095 EVENT  Started org.mortbay.jetty.Server@1f6f0bf
    Heritrix is running
            Web UI on port 8080

Browse to the Web UI to start a crawl and to load and configure crawl 
jobs. Eventually this will be the preferred mechanism for configuring 
and running crawls, but it is currently under regular revision.

III. Configuration

TODO: The configuration system is being revised at the moment.  Meantime 
study extant configurations at docs/example-settings.

IV. Crawling

a. ARC Files.

As Heritrix runs, by default, all it pulls it saves off as Internet 
Archive ARC files. Internet Archive ARC files are described here: 
<http://www.archive.org/web/researcher/ArcFileFormat.php>.  Heritrix 
uses version 1 ARC files.

b. The Graphical User Interface
        TODO

c. The Command Line Interface
        TODO

V. License

Heritrix is free software; you can redistribute it and/or modify it 
under the terms of the GNU Lesser Public License as published by the 
Free Software Foundation; either version 2.1 of the License, or any 
later version.
 
Heritrix is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser Public License for more details.

You should have received a copy of the GNU Lesser Public License
along with Heritrix (See LICENSE.txt); if not, write to the Free 
Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
02111-1307  USA

For the licenses for libraries used by Heritrix and included in its 
distribution, see the dependencies section of project.xml for pointers 
to their licenses (TODO: Generate a license page).


VI. Miscellaneous
    TODO
