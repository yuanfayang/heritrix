-------------------------------------------------------------------------------
$Id$
-------------------------------------------------------------------------------
Heritrix is the Internet Archive's open-source, extensible, web-scale,
archival-quality web crawler project. Its home page is
http://crawler.archive.org.

Heritrix (sometimes spelled heretrix , or misspelled or missaid as
heratrix / heritix / heretix / heratix ) is an archaic word for heiress
(woman who inherits). Since our crawler seeks to collect the digital artifacts
of our culture for the benefit of future researchers and generations, this
name seemed apt.

Webmasters!

Heritrix is designed to respect the robots.txt exclusion directives and META
robots tags . If you notice our crawler behaving poorly, please send us email
at archive-crawler-agent *at* lists *dot* sourceforge *dot* net.


0.0 TABLE OF CONTENTS

    1.0 SYSTEM REQUIREMENTS
    2.0 GETTING STARTED
    3.0 DEVELOPER NOTES
    4.0 CHANGES
    5.0 LICENSE


1.0 SYSTEM REQUIREMENTS

The Heritrix crawler is implemented purely in java. This means that the only
true requirement for running it is that you have a JRE installed.

1.1 Java

The Heritrix crawler makes use of Java 1.4 features so your JRE must be at
least of a 1.4.0 pedigree.

We currently include all of the free/open source third-party libraries
necessary to run Heritrix in the distribution package. See DEPENDENCIES for
the complete list (Licenses for all of the listed libraries are listed in the
dependencies section of the raw project.xml at the root of the src download).

1.2 Hardware

We recommend assigning at least 256MB RAM to the Java heap (via the
"-Xmx256MB" VM argument), which is usually suitable for crawls that range over
hundreds of hosts.

1.3 Linux

The Heritrix crawler has been built and tested primarily on Linux. It has seen
some informal use on Windows 2000 and Windows XP, but is not tested, packaged,
or supported on platforms other than Linux at this time.


2.0 GETTING STARTED

This manual is targeted at those who just want to run the crawler. The user has
downloaded a Heritrix binary and they need to know about configuration file
formats and how to source and run a crawl. If you want to build heritrix from
Manual source or if you'd like to make contributions and would like to know
about contribution conventions, etc., see instead the Developer Manual.

2.1 Launching Heritrix

To run Heritrix, first do the following:

    % export HERITRIX_HOME=/PATH/TO/BUILT/HERITRIX

...where $HERITRIX_HOME is the location of your built Heritrix (i.e. under the
Reports dist dir if you built w/ Ant, or under the untarred target/distribution/
heritrix.?.?.?.tar.gz dir if you built w/ Maven, or under the untarred
heritrix.?.?.?.tar.gz if you pulled a packaged binary).

Next run:

    % cd $HERITRIX_HOME
    % chmod u+x $HERITRIX_HOME/bin/heritrix.sh
    % $HERITRIX_HOME/bin/heritrix --help

This should give you usage output like the following:

    Usage: heritrix --help
    Usage: heritrix --nowui ORDER_FILE
    Usage: heritrix [--port=PORT] [--admin=LOGIN:PASSWORD] [--run] [ORDER_FILE]
    Usage: heritrix [--port=PORT] --selftest
    Version: 0.4.0
    Options:
    -a,--admin     Login and password for web user interface administration.
    Default: admin/letmenin.
    -h,--help      Prints this message and exits.
    -n,--nowui     Put heritrix into run mode and begin crawl using ORDER_FILE.
                   Do not put up web user interface.
    -p,--port      Port to run web user interface on.  Default: 8080.
    -r,--run       Put heritrix into run mode. If ORDER_FILE begin crawl.
    -s,--selftest  Run integrated self test.
    Arguments:
    ORDER_FILE     Crawl order to run.

Launch the crawler w/ the UI enabled by doing the following:

    % $HERITRIX_HOME/bin/heritrix.sh

This will start up heritrix printing out a startup message that looks like the
following:

    [b116-dyn-60 619] heritrix-0.4.0 > ./bin/heritrix.sh
    Tue Feb 10 17:03:01 PST 2004 Starting heritrix...
    Tue Feb 10 17:03:05 PST 2004 Heritrix 0.4.0 is running.
    Web UI is at: http://b116-dyn-60.archive.org:8080/admin
    Login and password: admin/letmein

Browse to the location you see printed out on the command line and login using
the supplied login/password. See 'Launching crawl jobs via the web UI', the
next section, for how to create a job to run.

2.2 Launching crawl jobs via the web UI

If the program is launched with a web UI users can access the administrative
interface with any regular browser. The admin section is password protected.

Once logged in, the 'Console' (more on that later) is displayed. Near the top
of the page are several tabs. To create a new job, select the 'Jobs' tab.

The 'Jobs' page offers several ways of creating a new job.

    * Create new crawl job (This will be based on the default profile)
    * Create new crawl job based on a profile
    * Create new crawl job based on an existing job

It is not possible to create jobs from scratch but you will be allowed to edit
any configurable part of the profile or job selected to serve as a template for
the new job. If running Heritrix for the first time there is only the supplied
default profile to chose from.

Having selected a profile/job the user will be asked to supply a name,
description, and seed list for the job (A seed list the list of URLs the
crawler should start its crawl from). Once submitted the name can not be
changed. The description and seed list can however be modified at a later date.

Below the data fields in the new job page, there are five buttons.

    * Modules
    * Filters
    * Settings
    * Overrides
    * Submit job

Each of the first 4 corresponds to a section of the crawl configuration that
can be modified. Modules refers to selecting which pluggable modules (classes)
to use. This includes the 'frontier' and 'processors'. It does not include the
use of pluggable filters which are configurable via the second option. Settings
refers to setting the configurable values on modules (pluggable or otherwise).
Overrides refers to the ability to set alternate values based on which domain
the crawler is working on. Clicking on any of these 4 will cause the job to be
created but kept from being run until the user finish configuring it. The user
will be taken to the relevant page. More on these pages in a bit.

Submit job button will cause the job to be submitted to the pending queue right
away. It can still be edited while in the queue or even after it starts
crawling (although modules and filters can only be set prior to the start of
crawling). If the crawler is set to run and there is no other job currently
crawling, the new job will start crawling at once. Note that some profiles may
not contain entirely default valid settings. In particular, User-Agent and From
attributes in the the http-headers section -- see the Settings on your job --
*MUST* be set to other than the default in the default profile for crawl t o
begin. You should set these to something meaningful that allows administrators
of sites you'll be crawling to contact you. The software requires that
User-Agent value be of the form...

    [name] (+[http-url])[optional-etc]

...where [name ] is the crawler identifier and [http-url ] is an URL giving
more information about your crawling efforts. The From value must be an email
address. (Please do not leave the Archive Open Crawler project's contact
information in these fields, we do not have the time or the resources to field
complaints about crawlers which we do not administer.)

Note, the term running generally means that the crawler will start crawling a
job as soon as one is available and no job is crawling: i.e. it will accept new
jobs to be crawled. While not running jobs will be held in the pending queue
even if there is no current job crawling. The term crawling generally refers to
a job that is actually being executed (crawled). That is pages are being
fetched, links extracted etc. If the crawler is set to not run, there can still
be a job crawling! That is a job that started before the crawler was stopped.
In that scenario once the current job is completed the next job will not be
started.

2.2.1 Modules

This page allows the user to select what URIFrontier implementation to use
(select from combobox) and to configure the chain of processors that are used
when processing a URI. Note that the order of display (top to bottom) is the
order in which processors are run. Options are provided for moving processors
up, down, removing them and adding those not currently in the chain. Those that
are added are placed at the end by default, generally the user should then move
them to their correct location. Detailed configuration of these modules is then
performed by going to the 'Settings' page afterwards.

2.2.2 Filters

Cer tain modules (Scope, all processors, the OrFilter for example) will allow
an arbitrary number of filters to be applied to them. This page presents a
treelike structure of the configuration with the ability to add remove and
reorder filters where they can be placed. For each grouping of filters the
options provided correspond to those that are provided for processors. Note
however that since filters can contain filters the lists can become
complicated. As with modules, detailed configuration of the filters is done via
the 'Settings' page.

2.2.3 Settings

This page provides a treelike representation of the crawl configuration similar
to the one the 'Filters' page does. Here however an input field is provided for
each configurable parameter of each module. Changes made will be saved when the
user navigates to one of the other crawl configuration pages or selects
'Finished'. On all pages choosing 'Finish' will submit the job to the pending
queue. Navigation to other parts of the admin interface will cause the job to
be lost.

2.2.4 Overrides

This page provides an iterative list of domains that contain override settings,
that is values for parameters that override values in the global configuration.
Users can navigate to any domain that has an override, create/edit the
overrides and delete them. When creating/editing them a page similar to the
'Settings' page is provided. The main difference is that each input field is
preceded by a checkbox. If a check is in that box it is an override. If there
is no check the value being displayed is inherited from the current domains'
super domain. To override a setting it is necessary to add a check in front of
it. Removing a check effectively removes the override. Changes made to
non-checked fields will be ignored.

It is not possible to override what modules are used in an override. Some of
that functionality can though be achieved via the 'enabled' option that each
processor has. By overriding it and setting it to false you can disable that
processor. It is even possible to have it set to false by default and only
enable it on select domains. Thus any arbitrary chain of processors can be
created for each domain with one major exception. It is not possible to
manipulate the order of the processors. It is possible to similarly disable/
enable filters. It is also possible to add filters. You can not affect the
order of inherited filters, and you can not interject new filters among them.
Override filters will be run after inherited filters.

2.3 Run

Once a job is in the pending queue the user can go back to the Console and
start the crawler. The option to do so is presented just below the general
information on the state of the crawler to the far left. Once started the
console will offer summary information about the progress of the crawl and the
option of terminating it.

2.4 Monitoring the Crawler via the web UI

Once logged in the user will be taken to the Console. It is the central page
for monitoring and affecting a running job. However more detailed reports and
actions are possible from other pages.

Every single page in the admin interface displays the same info header. It
tells you if the crawler is running or crawling a job. If a job is being
crawled it's name is displayed and minimal progress statistics. Information
about the number of pending and completed jobs is also provided.

2.5 Jobs

While a job is running this page allows users to view it's crawl order (the
actual XML configuration file), to view a crawl job report on it (both are also
available after the job is in the completed list) and the option to edit the
job. As noted in the cha pter about launching jobs via the WUI you cannot
modify the pluggable modules but you can change the configurable parameters
that they possess. This page also gives access to a list of pending jobs.

2.6 Logs

A very useful page that allows you to view any of the logs that are created on
a per-job basis. Log's can be viewed by line number, time stamp, regular
expression or 'tail' (show the last lines of the file).

2.7 Reports

This page allows access to the same crawl job report mentioned in the 'Jobs'
page section. This report details number of downloaded documents and various
associated statistics.


2.8 System Properties

Below we document system properties passed on the command-line that can
influence Heritrix behavior.

2.8.1 heritrix.development

Set this property on the command-line when you want to run the crawler from
eclipse. When this property is set, the conf and webapps directories will be
found in their development locations and startup messages will show on the
console.

2.8.2 java.util.logging.config.file

The heritrix conf directory includes a file named heritrix.properties . A
section of this file specifies the default heritrix logging configuration. To
override, point java.util.logging.config.file at a properties file w/ an
alternate logging configuration. Below we reproduce the default for reference:

    # Basic logging setup; to console, all levels
    handlers= java.util.logging.ConsoleHandler
    java.util.logging.ConsoleHandler.level= ALL

    # Default global logging level: only warnings or higher
    .level= WARNING

    # currently necessary (?) for standard logs to work
    crawl.level= INFO
    runtime-errors.level= INFO
    uri-errors.level= INFO
    progress-statistics.level= INFO
    recover.level= INFO

    # HttpClient is too chatty... only want to hear about severe problems
    org.apache.commons.httpclient.level= SEVERE

Here's an example of how you might specify an override:

    % JAVA_OPTS="-Djava.util.logging.config.file=heritrix.properties" \
    ./bin/heritrix.sh --no-wui order.xml


3.0 DEVELOPER NOTES

This doc. is for observers and contributors who'd like to pull and build from
source. In here we'll talk of cvs access, the code layout, core technologies
and patterns and their why, key technical decisions and their why.

3.1 Obtaining Heritrix

There three ways to obtain Heritrix: packaged binary or packaged source
download from the crawler sourceforge home page or via CVS checkout. See the
crawler sourceforge cvs page for how to fetch from CVS (Note, anonymous
access does not give you the current HEAD but a snapshot that can at times be
up to 24 hours behind HEAD). The packaged binary will be named
heritrix-?.?.?.tar.gz (or heritrix-?.?.?.zip ) and the packaged source will
be named heritrix-?.?.?-src.tar.gz (or heritrix-?.?.?-src.zip ) where ?.?.?
is the heritrix release version.

3.2 Building Heritrix

You can build Heritrix from source using Ant or Maven. The Maven build is
more comprehensive and will generate all from either the packaged source or
by Maven   from a CVS checkout. The Ant build is less so in that it doesn't
generate the distribution documentation but it does produce all else needed to
run Heritrix.

If you are building Heritrix w/ Ant, you must have Ant 1.5 installed. Make
sure the Ant optional.jar file sits beside the junit.jar. See JUnit Task for
what you must do setting up Ant to run junit tests (The build requires
junit).

The Heritrix maven build was developed using 1.0-rc1. See maven.apache.org.

3.3 Building Heritrix with Ant

If you obtained packaged source, here is how you build w/ Ant:

    % tar xfz heritrix-?.?.?-src.tar.gz
    % cd heritrix-?.?.? % $ANT_HOME/bin/ant dist

In the dist subdir will be all you need to run the Heritrix crawler. To learn
more about the ant build, type ant -projecthelp .

3.4 Building Heritrix with Maven

To build a CVS source checkout w/ Maven:

    % cd CVS_CHECKOUT_DIR
    % $MAVEN_HOME/bin/maven dist

In the target/distribution subdir, you will find packaged source and binary
builds. Run $MAVEN_HOME/bin/maven -g for other Maven possibilities.

3.5 Running Heritrix

See the GETTING STARTED section for how to run the built Heritrix.

3.6 Eclipse

At the head of the CVS tree, you'll find Eclipse .project and .classpath
configuration files that should make integrating the CVS checkout into your
Eclipse development environment straight-forward.

3.7 Unit Tests Code

"[A ] popular convention is to place all test classes in a parallel directory
structure. This allows you to use the same Java package names for your tests,
while keeping the source files separate. To be honest, we do not like this
approach because you must look in two different directories to find files."
from Section 4.11.3, Java Extreme Programming Cookbook, By Eric M. Burke,
Brian M. Coyner . We agree w/ the above so we put Unit Test classes beside
the classes they are testing in the source tree giving them the name of the
Class they are testing w/ a Test suffix.

3.8 Coding Conventions

Heritrix baselines on SUN's Code Conventions for the JavaTM Programming
Language . It'd be hard not to they say so little. They at least say maximum
line length of 80 characters . Below are tightenings on the SUN conventions
used in Heritrix.

We also will favor much of what is written in this document, Java Programming
Style Guidelines .

3.8.1 No Tabs

No tabs in source code. Set your editor to indent w/ spaces.

3.8.2 Indent Width

Indents are 4 charcters wide.

3.8.3 File comment

Here is the eclipse template we use for the file header comment:

    /* ${type_name}
    *
    * $$Id$$
    *
    * Created on ${date}
    *
    * Copyright (C) ${year} Internet Archive.
    *
    * This file is part of the Heritrix web crawler (crawler.archive.org).
    *
    * Heritrix is free software; you can redistribute it and/or modify
    * it under the terms of the GNU Lesser Public License as published by
    * the Free Software Foundation; either version 2.1 of the License, or
    * any later version.
    *
    * Heritrix is distributed in the hope that it will be useful,
    * but WITHOUT ANY WARRANTY; without even the implied warranty of
    * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    * GNU Lesser Public License for more details.
    *
    * You should have received a copy of the GNU Lesser Public License
    * along with Heritrix; if not, write to the Free Software
    * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
    */
    ${package_declaration}

3.9 Version and Release Numbering

Heritrix uses a version numbering scheme modelled after the one used for
Linux kernels. Versions are 3 numbers:

    [major ] .[minor/mode ] .[patchlevel ]

The major version number, currently at zero, increments upon significant
architectural changes or the achievement of important milestones in
capabilities. The minor/mode version number increments as progress is made
within a major version, with the added constraint that all external releases
have an even minor/mode version number, and all internal/development versions
have an odd minor/mode version number.

The patchlevel number increments for small sets of changes, providing the
most fine-grain timeline of software evolution. Patchlevels increment
regularly for internal/development(odd minor l evel) work, but only increment
for external releases when an official update to the previous release version
has been tested and packaged.

In the CVS HEAD, version numbers are applied as tags of the form "heritrix-#_
#_#". When a particular development-version is thought appropriate to become
an external/"stable" release, it is considered a "Release Candidate". If
testing confirms it is suitable for release, it is assigned the next even
minor/mode value (and a zero patchlevel), CVS version-labelled, and packaged
for release. Immediately after release, and before additional coding occurs,
the CVS HEAD is assigned the next odd minor/mode value (and a zero
patchlevel) in project/source files.

If patches are required to a released version, before the next release is
ready, they are applied to a CVS branch from the release version tag, tested,
and released as the subsequent patchlevel.

Keep in mind that each version number is an integer, not merely a decimal
digit. To use an extreme example: development version 2.99.99 would be
followed by either the 2.99.100 development version patchlevel or the 2.100.0
release. (And after such a release, the next development version would be
2.101.0.)

3.10 Making a Heritrix Release

Before going about a release, its assumed that the current HEAD version has
been run through the integration self test, all unit tests pass, that the (as
yet non-existent) test suite has been exercised, and that general usage shows
HEAD to be release worthy.

    1. Send a mail to the list to freeze commits until the all-clear is given.
    2. Up the project.xml 'currentVersion' element and the build.xml 'version'
    property. Ensure they are the same version number. (See Version and
    Release Numbering on this page for guidance on what version number to
    use)
    3. Generate the site. Review all documentation making sure it remains
    applicable. Fix at least the embarrassing. Make issues to have the rest
    addressed.
    4. Make the maven distributions: % $MAVEN_HOME/bin/maven dist
    5. Build the maven produced src distribution version using both maven and
    ant.
    6. Run both the binary and src-built product through the integration self
    test suite: % $HERITRIX_HOME/bin/heritrix.sh --selftest
    7. Update xdocs/changes.xml .
    8. (TODO: Changelog based off CVS history).
    9. Update the README.txt. Do html2txt on maven generated xdocs.
    10. Tag the CVS repository: % cvs -q tag heritrix-?_?_?
    11. Update the project.xml 'currentVersion' and build.xml 'version' property
    to both be a version number beyond that of the release currently being
    made (If we're releasing 0.2.0, then increment to 0.3.0).
    12. Login and upload the maven 'dist' product to sourceforge into the
        admin->File releases section.
    13. Update news on website, send announcement to mailinglist -- and give an
    all-clear that commits may resume -- and update our release state on
    freshmeat site (Here is the URL I used creating our freshmeat project:
    http://freshmeat.net/add-project/all-done/43820/46804/ -- 46804 is our
    project ID).

3.11 Integration self test

Run the integration self test on the command line by doing the following:

    % $HERITRIX_HOME/bin/heritrix.sh --selftest

This will set the crawler going against itself, in particular, the garden
webapp. When done, it runs an analsys of the produced arc files and logs and
dumps a ruling into heritrix_out.log . See the org.archive.crawler.garden
package for more on how the selftest works.

3.12 cruisecontrol

See src/cc for a config.xml that will run the heritrix maven build under
cruisecontrol . See the README.txt in the same directory for how to set up
continuous building using cc.


4.0 CHANGES

Release 0.4.0 - 2004-02-10

+------------------------------------------------------------+
|  Type  |                  Changes                  |  By   |
|--------+-------------------------------------------+-------|
|        | New MBEAN-based configuration system.     |       |
| add    | Reads and writes XML to validate against  | stack |
|        | heritrix_settings.xsd. UI revamped to use |       |
|        | new configuration system.                 |       |
|--------+-------------------------------------------+-------|
| add    | 60-odd unit tests added.                  | stack |
|--------+-------------------------------------------+-------|
| add    | Integration selftest framework.           | stack |
|--------+-------------------------------------------+-------|
|        | Start script backgrounds heritrix and     |       |
| update | redirects stdout/stderr to                | stack |
|        | heritrix_out.log. See Default launch      |       |
|        | should nohup, save stdout/stderr          |       |
|--------+-------------------------------------------+-------|
| update | Updated httpclient to version 2.0RC3.     | stack |
|--------+-------------------------------------------+-------|
| fix    | IAGZIPOutputStream NPE under IBM JVM      | stack |
|--------+-------------------------------------------+-------|
| fix    | Cleaner versioned testing build needed    | stack |
|--------+-------------------------------------------+-------|
| fix    | IAGZIPOutputStream NPE under IBM JVM      | stack |
|--------+-------------------------------------------+-------|
| fix    | Cleaner versioned testing build needed    | stack |
|--------+-------------------------------------------+-------|
| fix    | Cmd-line options for setting web ui       | stack |
|        | username/password                         |       |
|--------+-------------------------------------------+-------|
| fix    | Universal single- pass extractor          | stack |
+------------------------------------------------------------+

Release 0.2.0 - 2003-01-05

+------------------------------------------------------------+
|  Type  |                Changes                 |    By    |
|--------+----------------------------------------+----------|
| add    | First 'official' release.              | stack    |
+------------------------------------------------------------+

Release 0.1.0 - 2003-12-31

+------------------------------------------------------------+
| Type |                  Changes                   |   By   |
|------+--------------------------------------------+--------|
|      | Initial Mavenized development version      |        |
| add  | number (CVS/internal only). Added          | gojomo |
|      | everything to new project layout.          |        |
+------------------------------------------------------------+


5.0 LICENSE

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
