-------------------------------------------------------------------------------
$Id$
-------------------------------------------------------------------------------
0.0 Contents

1.0 Introduction
2.0 Webmasters!
3.0 System Runtime Requirements
4.0 Getting Started
5.0 Developer Documentation
6.0 Release History
7.0 License
8.0 Dependencies


1.0 Introduction
Heritrix is the Internet Archive's open-source, extensible, web-scale,
archival-quality web crawler project. Heritrix (sometimes spelled heretrix, or
misspelled or missaid as heratrix/heritix/heretix/heratix) is an archaic word
for heiress (woman who inherits). Since our crawler seeks to collect the digital
artifacts of our culture for the benefit of future researchers and generations,
this name seemed apt. 

2.0 Webmasters!
Heritrix is designed to respect the robots.txt 
<http://www.robotstxt.org/wc/robots.html> exclusion directives and META robots
tags <http://www.robotstxt.org/wc/exclusion.html#meta>. If you notice our
crawler behaving poorly, please send us email at archive-crawler-agent *at*
lists *dot* sourceforge *dot* net. 

3.0 System Runtime Requirements

3.1. Java Runtime Environment
The Heritrix crawler is implemented purely in java. This means that the only
true requirement for running it is that you have a JRE installed. The Heritrix
crawler makes use of Java 1.4 features so your JRE must be at least of a 1.4.0
pedigree. We currently include all of the free/open source third-party
libraries necessary to run Heritrix in the distribution package.  They are
listed along with pointers to their licenses in Section 8. Dependencies below.

3.2. Hardware
We recommend assigning at least 256MB RAM to the Java heap (via the
"-Xmx256MB" VM argument), which is usually suitable for crawls that range over
hundreds of hosts. 

3.3. Linux
The Heritrix crawler has been built and tested primarily on Linux. It has seen
some informal use on Macintosh, Windows 2000 and Windows XP, but is not tested,
packaged, nor supported on platforms other than Linux at this time. 

4.0 Getting Started
See the User Manual at ./docs/articles/user_manual.html or at
<http://crawler.archive.org/articles/user_manual.html>.

5.0 Developer Documentation
See ./docs/articles/developer_manual.html or
<http://crawler.archive.org/articles/developer_manual.html>.


6.0 Release History

Release 0.8.0 - 2004-05-18 14:00

Type Changes                                               By
add  integrate selftest into cruisecontrol build           stack-sf
add  On reedit, red star by bad attribute setting.         kristinn_sig
add  day/night configurations                              kristinn_sig
add  UI should only write changed config                   kristinn_sig
add  record of settings changes should be kept             johnerik
add  Only one build, not two                               stack-sf
add  maven-only build rather than ant & maven              stack-sf
add  ARCWriter should use a pool of open files -- if it    stack-sf
     helps
add  Precomp ile UI pages                                  stack-sf
add  UI should be split into common/uncommon settings      kristinn_sig
add  UI web pages need to be more responsive               kristinn_sig
fix  ConcurrentModificationExceptions                      stack-sf
fix  Too many open files                                   stack-sf
fix  ConcurrentModificationExceptionsd                     stack-sf
fix  empty seeds-report.txt                                kristinn_sig
fix  Doc. assumes bash. Allow tcsh/csh                     stack-sf
fix  script heritrix.sh goes into infinite loop            stack-sf
fix  heritrix.sh launch file path weirdness                stack-sf
fix  ToeThreads hung in ExtractorHTML after Pause          stack-sf
fix  IllegalCharsetNameException: Windows-1256             stack-sf
fix  No doc-files/package.html in javadoc.                 stack-sf
fix  embed-count sensitivity WRT redirects, preconditions  gojomo
fix  Refinement limits are not always saved                kristinn_sig
fix  NPE exception in getMBeanInfo(settings)               johnerik
fix  Untried CrawlURIs should have clear status code       gojomo
fix  Thread underutilization in broad crawls               gojomo
fix  KeyedQueue showing EMPTY status, but the length is 1. gojomo
fix  NPE in XMLSettingsHandler.recursiveFindFiles()        johnerik
fix  Failed DNS does not have intended impact              gojomo
fix  ftp URIs are retried                                  gojomo
fix  Refetching of robots and/or DNS broken                gojomo
fix  NPE switching to 'expert' settings in HEAD            kristinn_sig
fix  rss extractor                                         ia_igor
fix  JS extractor clueless on relative URIs                ia_igor
fix  converting URI's '\' into '/' character               ia_igor
fix  When going back to overrides, directory is gone       kristinn_sig
fix  shutdown.jsp unable to compile                        kristinn_sig
fix  ARCWriterPool timeouts -- legitimate?                 stack-sf
fix  If one URI connect-fails, hold queue, too             gojomo
fix  Fetching simple URLs fails with S_CONNECT_FAILED (-2) gojomo
     error
fix  seeds held back/poor breadth first?                   gojomo
fix  Parsing links found between escaped quotes in         ia_igor
     JavaScript
fix  Does not extract applet URI correctky                 ia_igor
fix  links to likely-embed types should be treated as      ia_igor
     embeds
fix  Frontier.next() forceFetches will cause assertion     gojomo
     error
fix  Flash link extractor causes OutOfMemory exceptions.   ia_igor
fix  Should be possible to resume from                     kristinn_sig
fix  Heritrix ignores charset                              stack-sf
fix  Max # of arcs not being respected.                    stack-sfkristinn_sig
fix  New profile should ensure unique name                 kristinn_sig
fix  When changing scope common scope settings are lost    johnerik
fix  ssl doesn't work                                      stack-sf
fix  Allow that people use tcsh/csh not just bash          stack-sf
fix  https SSLHandshakeException: unknown certificate      stack-sf
fix  Cannot override settings that isn't set in globals    johnerik
fix  'Waiting for pause' even after all threads done       kristinn_sig
fix  filter 'invert', filter names need work               kristinn_sig
fix  max-link-hops (etc.) ignored unless                   stack-sf
fix  order.xml absolute paths                              johnerik
fix  Cannot set Transcl usionFilter attributes             johnerik
fix  Link puts garbage into arc file: http://www.msn.com/  stack-sf
     robots.t

Release 0.6.0 - 2004-03-25

Type   Changes                                                   By
add    861861 Redirects(/refreshes) from seeds should == new     kristinn_sig
       seeds - Completed
add    899223 Special seed-success report shoudl be offered      kristinn_sig
add    891986 Bandwidth throttle function, setting.              johnerik
add    899223 Special seed-success report shoudl be offered      kristinn_sig
add    877275 integrated operator 'diary' needed                 kristinn_sig
add    891983 IP, Robots expirations should be settable          johnerik
add    910152 Recovery of old jobs on WUI (re)s tart             kristinn_sig
add    781171 parsing css                                        ia_igor
add    912986 log views should give an idea of file size (where  kristinn_sig
       possible)
add    912989 Alerts should have 'select all' button...          kristinn_sig
add    856593 [load ] [save ] [turn on/off ] cookies             ia_igor
add    912201 Add levels to alerts                               kristinn_sig
add    896665 Split processor chains.                            johnerik
add    896754 Show total of disregards                           kristinn_sig
add    903095 Show increments of megabytes in ui                 kristinn_sig
add    896794 serious errors (eg outofmemory) should show up in  kristinn_sig
       UI
add    900520 Short description of ComplexTypes in user          kristinn_sig
       interface.
add    899982 Should be possible to alter filters while          kristinn_sig
       crawling.
add    896672 Display progress (doc/sec) with more precision     kristinn_sig
add    896677 Highlight the success or failures of each seed     kristinn_sig
add    896760 Prominent notification when seeds have problems    kristinn_sig
add    896801 java regexps (in log view) need help text          kristinn_sig
add    896778 Log viewing enhancements:                          kristinn_sig
add    896795 frontier, thread report improvements               kristinn_sig
add    876516 default launch should nohup, save stdout/stderr    ia_igor
update Update of httpclient to release 2.0                       stack-sf
update Update of jmx libs to release 1. 2.1                      stack-sf
fix    896763 127.0.0.1 in job report                            kristinn_sig
fix    896767 Frontier retry-delay should include units (eg      kristinn_sig
       -seconds)
fix    898994 Revisiting admin URIs if not logged in should      kristinn_sig
       prompt login
fix    899019 Deadlock in Andy's 2nd Crawl                       johnerik
fix    767225 Better bad-config handling                         parkerthompson
fix    815357 mysterious pause facing network (DNS) problem      gojomo
fix    896747 ExtractorJS's report overstates it's discovered    kristinn_sig
       URIs
fix    896667 Web UI does not display correctly in IE            kristinn_sig
fix    896780 console clarity/safety                             kristinn_sig
fix    896655 Does not respect per settings added after crawl    johnerik
       was started.
fix    856555 'empty' records in compressed arc files            ia_igor
fix    895955 URIRegExpFilter retains memory                     stack-sf

Release 0.4.1 - 2004-02-12

Type Changes                               By
fix  895955 URIRegExpFilter retains memory stack-sf

Release 0.4.0 - 2004-02-10

Type   Changes                                                         By
add    New MBEAN-based configuration system. Reads and writes XML to   stack-sf
       validate against heritrix_settings.xsd.
update UI extensively revamped. Exploits new configuration system.     stack-sf
add    60-odd unit tests added.                                        stack-sf
add    Integration selftest framework.                                 stack-sf
add    Added pooling of ARCWriters.                                    stack-sf
       Start script backgrounds heritrix and redirects stdout/stderr
update to heritrix_out.log. See 876516 Default launch should nohup,    stack-sf
       save stdout/stderrWeb UI accesses are loggged to
       heritrix_out.log also.
update Updated httpclient to version 2.0RC3.                           stack-sf
fix    763517 IAGZIPOutputStream NPE under IBM JVM                     stack-sf
fix    809018 Cleaner versioned testing build needed                   stack-sf
fix    872729 Cmd-line options for setting web ui username/password    stack-sf
fix    863317 Universal single-pass extractor                          stack-sf

Release 0.2.0 - 2003-01-05

Type Changes                   By
add  First 'official' release. stack-sf

Release 0.1.0 - 2003-12-31

Type Changes                                                             By
add  Initial Mavenized development version number (CVS/internal only).   gojomo
     Added everything to new project layout.


7.0 License

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

8.0 Dependencies

8.1. commons-httpclient
version: 2.0-final
url: http://jakarta.apache.org/commons/httpclient/
description: This package is used for fetching URIs via http.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.2. commons-logging
version: 1.0.3
url: http://jakarta.apache.org/commons/logging.html
description: Provides logging adapters.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.3. dnsjava
version: 1.6.2
url: http://www.dnsjava.org/
description: DNS Lookups.
license: BSD

8.4. stataclasses
version: 0.1.0
url: http://cvs.sourceforge.net/viewcvs.py/*checkout*/archive-crawler/ArchiveOpenCrawler/lib/stataclasses-0.1.0.jar
description: Utils supplied by Rayme Stata. Gave it arbitrary 0.1.0 version
number.
license: ?

8.5. jetty
version: 4.2.17
url: http://jetty.mortbay.com/jetty/
description: The Jetty servlet container.
license: Jetty license, http://jetty.mortbay.org/jetty/LICENSE.html

8.6. servlet
version: 2.3
url: http://jakarta.apache.org/tomcat/
description: Taken from tomcat.
license: http://jakarta.apache.org/site/legal.html

8.7. jasper-runtime
version: 4.1.30
url: http://jakarta.apache.org/tomcat/
description: Taken from tomcat.
license: http://jakarta.apache.org/site/legal.html

8.8. jasper-compiler
version: 4.1.30
url: http://jakarta.apache.org/tomcat/
description: Taken from tomcat.
license: http://jakarta.apache.org/site/legal.html

8.9. jmxri
version: 
url: http://java.sun.com/products/JavaManagement/index.jsp
description: JMX Reference Implementation.
license: SUN Binary Code License http://java.com/en/download/license.jsp

8.10. jmxtools
version: 
url: http://java.sun.com/products/JavaManagement/index.jsp
description: JMX tools.
license: SUN Binary Code License http://java.com/en/download/license.jsp

8.11. poi
version: 2.0-RC1-20031102
url: http://jakarta.apache.org/poi/
description: For parsing PDFs.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.12. poi-scratchpad
version: 2.0-RC1-20031102
url: http://jakarta.apache.org/poi/
description: For parsing PDFs. Has the
org.apache.poi.hdf.extractor.WordDocument.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.13. javaswf
version: 
url: http://www.anotherbigidea.com/javaswf
description: JavaSWF2 is a set of Java packages that enable the parsing,
manipulation and generation of the Macromedia Flash(TM) file format known as
SWF ("swiff"). Added jar was made by unzipping javaswf-CVS-SNAPSHOT-1.zip
download, compiling the java classes therein, and then making a jar of the
product.
license: The JavaSWF BSD License,
http://anotherbigidea.com/javaswf/JavaSWF2-BSD.LICENSE.html

8.14. itext
version: 1.0
url: http://www.lowagie.com/iText/
description: A library for parsing PDF files.
license: MPL and LGPL

8.15. ant
version: 1.5.4
url: http://ant.apache.org
description: Build tool
license: Apache 1.1. http://ant.apache.org/license.html

8.16. junit
version: 3.8.1
url: http://www.junit.org/
description: A framework for implimenting the unit testing methology.
license: IBM's Common Public License Version 0.5.

8.17. commons-pool
version: 1.1
url: http://jakarta.apache.org/site/binindex.cgi#commons-pool
description: For object pooling.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.18. commons-collections
version: 2.1
url: http://jakarta.apache.org/site/binindex.cgi#commons-collections
description: Needed by commons-pool.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.19. commons-cli
version: 1.0
url: http://jakarta.apache.org/site/binindex.cgi
description: Needed doing Heritrix command-line processing.
license: Apache 1.1 http://www.apache.org/LICENSE.txt

8.20. concurrent
version: 1.3.2
url: http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html
description: Concurrency utilities.
license: Public Domain

8.21. commons-net
version: 1.1.0
url: http://jakarta.apache.org/commons/net/
description: This is an Internet protocol suite Java library originally
developed by ORO, Inc. This version supports Finger, Whois, TFTP, Telnet, POP3,
FTP, NNTP, SMTP, and some miscellaneous protocols like Time and Echo as well as
BSD R command support. Heritrix uses its FTP implementation.
license: Apache 1.1 http://www.apache.org/LICENSE.txt
