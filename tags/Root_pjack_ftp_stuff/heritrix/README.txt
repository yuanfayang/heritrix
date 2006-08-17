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

1.0 Introduction
Heritrix is the Internet Archive's open-source, extensible, web-scale,
archival-quality web crawler project. Heritrix (sometimes spelled heretrix, or
misspelled or missaid as heratrix/heritix/heretix/heratix) is an archaic word
for heiress (woman who inherits). Since our crawler seeks to collect and
preserve the digital artifacts of our culture for the benefit of future
researchers and generations, this name seemed apt. 

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
Default heap size is 256MB RAM.  This should be suitable for crawls that range
over hundreds of hosts. 

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

See the Heritrix Release Notes in the local directory
docs/articles/releasenotes.html if this is a binary release or
at http://crawler.archive.org/articles/releasenotes.html.


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
distribution, see below in section '8.0 Dependencies'.
