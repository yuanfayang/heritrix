Archive Open Crawler (Heritrix) README.txt

This document is a work in progress.  Hopefully it should be enough to get you
at least up and running, but if you run into major confusions please bring them
to the mailing list so we can what types of issues users are facing and either
address them in the code or here in the documentation.

For more on the Archive Open Crawler (Heritrix), see crawler.archive.org.

Table of Contents

I.  Before You Begin
  a. Requirements
  
II. Getting Started
  a. Fetching from CVS
  b. Building
  c. Building CVS by Tag
  d. Meta-data
  
III.  Configuration Files
  a. Configuration File Manifest
  b. order.xml
  c. environment.txt
  d. build.xml
  e. build.properties
  f.  Sample Configuration Files

IV. Crawling
   a. The Graphical User Interface
   b. The Command Line Interface

V.  License
 
   
I. Before You Begin

a.  Requirements

i. Java Runtime Environment

The Archive Open Crawler is implemented purely in java.  This means that the
only true requirement for running it is that you have a JRE installed.  We
recommend the IBM JRE as it has proven to be much more effecient than other
implementations, but the Sun JRE should work as well.

ii. Ant (recommended)

We also recommend that you download and install ant, a build tool for java
applications.  This is not strictly required, but it will make your life much
easier, and we have written several scripts that should keep you from ever
having to worry about all the ugly realities of java development (think
classpaths).  All this and more can be yours, but, as I mentioned, you need to
install ant.
  
II.  Getting Started

If you're reading this it probably means you've already checked out the crawler
code from CVS. But, for the sake of completeness let's go though it all.

a.  Fetching from CVS

Note:  Because this project is hosted at sourceforge, fetching from CVS is
slightly idosyncratic.  By default if you check out a project anonymously you
will get a nightly snapshot of the project, rather than the latest-greatest
source at the time you start the checkout. 

How you get the source from CVS is up to you.  If you are not familiar with CVS,
or you're just lazy (it's ok we are too) the easiest way to get started is to do
the following:

1.  Install a JRE and ant (see Getting Started)

2.  Browse Sourceforge CVS repository via http:
  
 http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/archive-crawler/ArchiveOpenCrawler/

2. Grab build.xml and build.properties from the repository.

3.  Open build.properties and modify per the instructions provided in the
comments. This should be straightforward.

4.  Grab startbuild.sh or startbuild.bat (depending on your os of choice) and
environment.txt.  Open and modify environment.txt to suit your local enviroment
per the instructions included in the comments.

5.  Run startbuild[sh,bat] checkout-core

Note:  If you are using anonymous CVS you may receive errors during this last
step that indicate a failure to connect to CVS.  This often happens,
particularly during periods of heavy-usage.  Just keep trying, or setup a
sourceforge account and modify build.properties to use this (see example in
build.properties).

And that's it.  You should now be able to see a directory within your checkout
directory (where you ran this script from) that is called 'CVSCheckoutDir' and
contains the Archive Open Crawler source.  

b. Building

If you've followed the above instructions for fetching from CVS you're one
command away from having the crawler built.  If you have the source, but did not
fetch it via the startbuild script you'll need to follow the instructions in
'Fetching From CVS' steps 3-4.

Note:  If you didn't fetch from CVS using the build script you'll need to run
'startbuild[sh,bat] init' to create the appropriate directory structure, then
move the source tree into the newly-created
./CVSCheckoutDir/ArchiveOpenCrawler'.  

Now, all you need to do to start a build is run 'startbuild[sh,bat] package'.
This will compile all the classes needed to run the crawler, and deposit the
class files in the 'build' directory.  It will then create a jar file of
everything needed to run the crawler and deposit this file in a directory called
'RunThis'. 

To see other build possibilities look for <target> elements in build.xml.  There
are serveral options not discussed here that may be very useful.  Specifically,
once you set up authenticated CVS, you may find the 'nightlybuild' target
useful.

c.  Building Releases from CVS by Tag

CVS allows us to keep track of which files, in what state, went into each
build/version that is released.  If you wish to build a specific version or build,
for example to test the behavior of an old build, you may do so by specifying
a build target of 'build-by-tag' and passing the build/versions' cvs tag as
an additional argument.

To determine a particular build's tag refer to the file build.metadata within the
crawler's root directory.  All official releases should contain a tag that is in the
form vX_Y for major/minor version releases, where X is the major version 
number and Y is the minor version number, and bX for regularly occuring
builds, where X is an integer (e.g. b1025).

d. Meta-data

As mentioned in 'Building Releases from CVS by Tag' ant builds of Heritrix
include a file called 'build.metadata' that contains some useful information 
pertaining to the who/what/when/wheres of the build process.

If you experience execution problems and the build environment differs 
significantly from your own you may need to rebuild.  If you experience
a problem that is solved in this way (they should be few and far between)
PLEASE REPORT IT.

Additionally, some of this information should be included whenever 
submitting a bug report, where pertinent.

III. Configuration

By default the crawler should be set up to run on a single machine
out-of-the-box with very little configuration.  

a.  Configuration File Manifest

Following is a list of requisite configuration files and a brief description of
why they exist:

  order.xml - 	This is the "master config file".  It specifies which modules
	will be used to  process URIs, in which order URIs will be processed, 
	how and where files will ge written to disk, how "polite" the crawler 
	should be, etc.  

  seeds.txt - This file specifies a one-per-line list of URIs with which to 
	seed the crawl.  

  environment.txt – This file specifies several environment variables that the 
	build scripts will need to work on your local system.

  build.xml – This file is used by and in the build process.  

  build.properties – Defines variables used by ant when building targets 
	defined in build.xml.

b.  order.xml

<develop a detailed description of order.xml elements/attributes after the schema has settled down>

c. environment.txt

This file is meant as a one-stop configuration location.  Because many scripts
will need to know about things like the location of java, the location of ant,
etc, this file was created so these only need be defined once.  

d. build.xml

This is a standard ant configuration file and will be used if you use ant to
build Heritrix.  It specifies a number of targets.  You will probably be
interested in the following targets:

  all – build it all, package it up, get it ready to run (fetches latest from cvs).
  localbuild – build from local source.
  run-unit-testcases – run unit tests against the source.
  clean – clean up after the build.
  checkout-core – get the lastest source from cvs.
  compile-core – create class files.
  
e. build.properties 

build.properties is ant's solution to seperating content and business logic.
While build.xml is the business logic that makes the builds happen,
build.properties specifies the content to plug into these directives.  This is
where output directories are defined, cvs usernames/passwords/authentication
methods are defined, etc.

f.  Sample Configuration Files

Sample configuration files can be found in cvs.  Build configuration files
(build.xml, environment.txt, build.properties) can be found in the cvs root
directory.  Various examples of order.xml and seeds.txt files can be found under
the configurations directory within cvs.

IV. Crawling

Starting a crawl should be relatively painless operation.  Currently there are
two ways to start a crawl, graphically, or using a shell script.  For most
crawls both methods should be easy to use, though if you want to make
configuration changes without having to hand-edit files the graphical interface
may be the better choice.

Note:  Before you beging crawling you must change the default user agent in
order.xml. You should set this to something meaningful that allows
administrators of sites you'll be crawling to contact you.  Please do not leave
the Archive Open Crawler project's contact information in this field, we do not
have the time or the resources to field complaints about crawlers which we are
not in control of.

a.  The Graphical User Interface

<insert some stuff here when we get a GUI that we want people to use>

b.  Command Line Interface

If you have an aversion to GUIs there's also a shell script called
startcrawl.[sh,bat] that may be used to start a crawl.  


V. License

Heritrix is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation; either version 2.1 of the License, or
any later version.
 
Heritrix is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser Public License for more details.

You should have received a copy of the GNU Lesser Public License
along with Heritrix (See LICENSE.txt); if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
