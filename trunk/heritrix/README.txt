Archive Open Crawler README.txt

This document is a work in progress.  Hopefully it should be enough to get you at least up and running, but if you 
run into major confusions please bring them to the mailing list so we can what types of issues users are facing
and either address them in the code or here in the documentation.

Table of Contents

I.  Before You Begin
  a. Requirements
  
II. Getting Started
  a. Fetching from CVS
  b. Building
  
III.  Configuration
  a. Configuration File Manifest
  b. order.xml
  c. Sample Configuration Files

IV. Crawling
   a. The Graphical User Interface
   b. The Command Line Interface
 
   
I. Before You Begin

a.  Requirements

i. Java Runtime Environment

The Archive Open Crawler is implemented purely in java.  This means that the only true requirement for running it
is that you have a JRE installed.  We recommend the IBM JRE as it has proven to be much more effecient than 
other implementations, but the Sun JRE should work as well.

ii. Ant (recommended)

We also recommend that you download and install ant, a build tool for java applications.  This is not strictly 
required, but it will make your life much easier, and we have written several scripts that should keep you from
ever having to worry about all the ugly realities of java development (think classpaths).  All this and more can 
be yours, but, as I mentioned, you need to install ant.
  
II.  Getting Started

If you're reading this it probably means you've already checked out the crawler code from CVS. But, for the sake
of completeness let's go though it all.

a.  Fetching from CVS

Note:  Because this project is hosted at sourceforge, fetching from CVS is slightly idosyncratic.  By default if you check out
a project anonymously you will get a nightly snapshot of the project, rather than the latest-greatest source at the time
you start the checkout. 

How you get the source from CVS is up to you.  If you are not familiar with CVS, or you're just lazy (it's ok we are too)
the easiest way to get started is to do the following:

1.  Install a JRE and ant (see Getting Started)

2.  Browse Sourceforge CVS repository via http:
	http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/archive-crawler/ArchiveOpenCrawler/

2. Grab build.xml and build.properties from the repository.

3.  Open build.properties and modify per the instructions provided in the comments.  This should be straightforward.

4.  Grab startbuild.sh or startbuild.bat (depending on your os of choice).  You will also need to modify the java and 
ant paths in this file.

5.  Run startbuild[sh,bat] checkout-core

Note:  If you are using anonymous CVS you may receive errors during this last step that indicate a failure to
connect to CVS.  This often happens, particularly during periods of heavy-usage.  Just keep trying, or setup
a sourceforge account and modify build.properties to use this (see example in build.properties).

And that's it.  You should now be able to see a directory within your checkout directory (where you ran this script 
from) that is called 'CVSCheckoutDir' and contains the Archive Open Crawler source.  

b. Building

If you've followed the above instructions for fetching from CVS you're one command away from having the crawler
built.  If you have the source, but did not fetch it via the startbuild script you'll need to follow the instructions in
'Fetching From CVS' steps 3-4.

Note:  If you didn't fetch from CVS using the build script you'll need to run startbuild[sh,bat] init to create the appropriate
directory structure, then move the source tree into the newly-created ./CVSCheckoutDir/ArchiveOpenCrawler'.  

Now, all you need to do to start a build is run 'startbuild[sh,bat] package'.  This will compile all the classes needed to
run the crawler, and deposit the class files in the 'build' directory.  It will then create a jar file of everything needed 
to run the crawler and deposit this file in a directory called 'RunThis'. 

To see other build possibilities look for <target> elements in build.xml.  There are serveral options not discussed 
here that may be very useful.  Specifically, once you set up authenticated CVS, you may find the 'nightlybuild' 
target useful.


III. Configuration

By default the crawler should be set up to run on a single machine out-of-the-box with very little configuration.  

a.  Configuration File Manifest

Following is a list of requisite configuration files and a brief description of why they exist:

order.xml - This is the "master config file".  It specifies which modules will be used to process URIs, in which
order URIs will be processed, how and where files will ge written to disk, how "polite" the crawler should be,
etc.  

seeds.txt - This file specifies a one-per-line list of URIs with which to seed the crawl.  

b.  order.xml

<develop a detailed description of order.xml elements/attributes after the schema has settled down>

c.  Sample Configuration Files

<create some generic config files and put them somewhere useful>


IV. Crawling

Starting a crawl should be relatively painless operation.  Currently there are two ways to start a crawl, graphically,
or using a shell script.  For most crawls both methods should be easy to use, though if you want to make configuration
changes without having to hand-edit files the graphical interface may be the better choice.

a.  The Graphical User Interface

<insert some stuff here when we get a GUI that we want people to use>

b.  Command Line Interface

If you have an aversion to GUIs there's also a shell script called startcrawl.[sh,bat] that may be used to start
a crawl.  

<make this script slightly more generic, then include instructions for use>






