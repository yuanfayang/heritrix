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

+---------------------------------------------------------------------+
| Version |    Date    |                 Description                  |
|---------+------------+----------------------------------------------|
|         |            | Release for second heritrix workshop,        |
|         |            | Copenhagen 06/2004 (1.0.0 first release      |
|         |            | candidate). Added site-first prioritization, |
|         |            | fixed link extraction of multibyte URIs,     |
| 0.10.0  | 2004-06-04 | added metadata to arcs as xml, changed arc   |
|         |            | naming template, new user and developer      |
|         |            | manuals, added basic/digest auth and http    |
|         |            | post/get login facility, and added help to   |
|         |            | UI. Bug fixes.                               |
|---------+------------+----------------------------------------------|
| 0.8.1   | 2004-05-28 | Fixes to build with maven rc2+.              |
|---------+------------+----------------------------------------------|
|         |            | Release (and branch heritrix-0_8 made at the |
|         |            | heritrix-0_7_1 tag) because of               |
|         |            | concurrentmodificationexceptions if tens of  |
|         |            | seeds supplied and to fix domain-scope       |
|         |            | leakage. Also, made continuous build         |
|         |            | publically available, incorporated           |
|         |            | integration selftest into build, made it a   |
|         |            | maven-build only (ant-build no longer        |
| 0.8.0   | 2004-05-18 | supported), added day/night configurations   |
|         |            | (refinements), ameliorated too-many-open     |
|         |            | files, added exploit of http-header          |
|         |            | content-type charset creating character      |
|         |            | streams, and heritrix now crawls ssl sites.  |
|         |            | UI improvements include red start by bad     |
|         |            | configuration, precompilation, and           |
|         |            | delineation of advanced settings. Many bug   |
|         |            | fixes.                                       |
|---------+------------+----------------------------------------------|
|         |            | Release made in advance of radical frontier  |
|         |            | changes. Added bandwidth throttle, operator  |
|         |            | 'diary', settable robots expiration, crawl   |
|         |            | er cookie pre-population, and changing of    |
|         |            | certain options mid-crawl. Many UI           |
| 0.6.0   | 2004-03-25 | improvements including UI display of         |
|         |            | critical exceptions, UI desccription of      |
|         |            | job-order options, and improved reporting.   |
|         |            | Optimizations. Updated httpclient lib to 2.0 |
|         |            | release and jmx libs to 1.2.1. Lots of bug   |
|         |            | fixes.                                       |
|---------+------------+----------------------------------------------|
| 0.4.1   | 2004-02-12 | Memory retention fix.                        |
|---------+------------+----------------------------------------------|
|         |            | Release made for heritrix workshop, San      |
|         |            | Francisco, 02/2004. New MBEAN-based          |
|         |            | configuration, extensive UI revamp, first    |
| 0.4.0   | 2004-02-10 | unit tests and integration selftest          |
|         |            | framework added, pooling of ARCWriters, new  |
|         |            | cmd-line start scripts, httpclient lib       |
|         |            | update (2.0RC3) and bugfixes.                |
|---------+------------+----------------------------------------------|
| 0.2.0   | 2003-01-05 | First 'official' release                     |
|---------+------------+----------------------------------------------|
| 0.1.0   | 2003-12-31 | Mavenized build                              |
+---------------------------------------------------------------------+

Release 0.10.0 - 2004-06-04
                 
+---------------------------------------------------------------------+
| Type |                  Changes                   |       By        |
|------+--------------------------------------------+-----------------|
| add  | job report: show 'active' hosts, show more | kristinn_sig-sf |
|      | size totals                                |                 |
|------+--------------------------------------------+-----------------|
| add  | "Site-first"/'frontline' prioritization    | gojomo          |
|------+--------------------------------------------+-----------------|
| add  | multiple open http connections per host    | gojomo          |
|      | needed                                     |                 |
|------+--------------------------------------------+-----------------|
| add  | Add help to web UI                         | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| add  | When a host last had a completed URI shown | kristinn_sig    |
|      | in crawl report                            |                 |
|------+--------------------------------------------+-----------------|
| add  | Encode multibyte URIs using page charset   | stack-sf        |
|      | before queuing                             |                 |
|------+--------------------------------------------+-----------------|
| add  | One src for site, help, and readme docs.   | stack-sf        |
|------+--------------------------------------------+-----------------|
| add  | identifying ARCs: unique names, header     | stack-sf        |
|      | records                                    |                 |
|------+--------------------------------------------+-----------------|
| add  | Resetting arc file counter for every job.  | stack-sf        |
|------+--------------------------------------------+-----------------|
| add  | ARCs need better headers                   | gojomo          |
|------+--------------------------------------------+-----------------|
| add  | Specify location of jobs dir               | stack-sf        |
|------+--------------------------------------------+-----------------|
| add  | Logging in (HTTP POST, Basic Auth, etc.)   | stack-sf        |
|------+--------------------------------------------+-----------------|
| add  | Update dnsjava from 1.5 to 1.6.2 (Fix NPE) | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | crawl.log entries without annotations end  | gojomo          |
|      | with a space                               |                 |
|------+--------------------------------------------+-----------------|
| fix  | An issue with arc names' date and serial   | stack-sf        |
|      | number alignment                           |                 |
|------+--------------------------------------------+-----------------|
| fix  | Output of warning message leads to         | stack-sf        |
|      | NullPointerExceptions                      |                 |
|------+--------------------------------------------+-----------------|
| fix  | Either UURI or ExtractHTML should strip    | stack-sf        |
|      | whitespace better                          |                 |
|------+--------------------------------------------+-----------------|
| fix  | Maximum documents not enforced             | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| fix  | NPE in path depth filter                   | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | embed/speculative inclusion too loose      | gojomo          |
|------+--------------------------------------------+-----------------|
| fix  | UnsupportedCharsetException handled        | stack-sf        |
|      | awkwardly                                  |                 |
|------+--------------------------------------------+-----------------|
| fix  | UURI accepting/creating unUsable URIs (bad | stack-sf        |
|      | hosts)                                     |                 |
|------+--------------------------------------------+-----------------|
| fix  | CachingDiskLongFPSet UI availability       | gojomo          |
|------+--------------------------------------------+-----------------|
| fix  | Crawls slow till change a setting          | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | zero link-hops should work                 | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| fix  | multiple robots.txt URLs in the "default"  | kristinn_sig    |
|      | frontier                                   |                 |
|------+--------------------------------------------+-----------------|
| fix  | NPE in ExtractorHTML#isHtmlExpectedHere    | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| fix  | Unwanted behavior with seed redirection    | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| fix  | Link extraction failing                    | kristinn_sig    |
|------+--------------------------------------------+-----------------|
| fix  | Memory issues: Frontier.snoozeQueue        | gojomo          |
|------+--------------------------------------------+-----------------|
| fix  | Transitive scope confusion, may not work   | gojomo          |
|      | as expected                                |                 |
|------+--------------------------------------------+-----------------|
| fix  | Wrong stats after deleting URIs from       | kristinn_sig    |
|      | Frontier                                   |                 |
|------+--------------------------------------------+-----------------|
| fix  | NoSuchElementException in admin/reports/   | kristinn_sig    |
|      | frontier.jsp                               |                 |
|------+--------------------------------------------+-----------------|
| fix  | Alert: Authentication scheme(s) not        | stack-sf        |
|      | supported                                  |                 |
|------+--------------------------------------------+-----------------|
| fix  | IP validity: units, TTL vs. setting        | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | ConcurrentModificationException in         | stack-sf        |
|      | DomainScope focus filter                   |                 |
|------+--------------------------------------------+-----------------|
| fix  | ConcurrentModificationException terminate  | stack-sf        |
|      | job                                        |                 |
|------+--------------------------------------------+-----------------|
| fix  | Authentication bug                         | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | terminate running crawl == NPE             | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | java.net.URI parses %20 but getHost null   | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | NPE in java.net.URI.encode                 | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | java.net.URI chokes on                     | stack-sf        |
|      | hosts_with_underscores                     |                 |
|------+--------------------------------------------+-----------------|
| fix  | Doing separate DNS lookup for same host    | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | java.net.URI#getHost fails when leading    | stack-sf        |
|      | digit                                      |                 |
|------+--------------------------------------------+-----------------|
| fix  | Constraining java URI class                | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | Same CrawlServer instance for http &       | stack-sf        |
|      | https.                                     |                 |
|------+--------------------------------------------+-----------------|
| fix  | Broad crawl/ too many open files           | gojomo          |
|------+--------------------------------------------+-----------------|
| fix  | multiple charset headers + long lines      | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | Corrupted blue image in progress bars      | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | NPEs in Andy's Th-Fri Crawl + NPE in RIS   | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | IllegalArgumentEx/                         | stack-sf        |
|      | ReplayCharSequenceFactory (offset vs. size |                 |
|------+--------------------------------------------+-----------------|
| fix  | FTP URIs in seeds interpreted as HTTP      | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | maven rc2 won't make src distribution      | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | Corrupted arc files on termination of job  | stack-sf        |
|------+--------------------------------------------+-----------------|
| fix  | https exception: java.io.IOException: SSL  | stack-sf        |
|      | failure                                    |                 |
|------+--------------------------------------------+-----------------|
| fix  | Excessive ARCWriterPool timeouts:          | stack-sf        |
+---------------------------------------------------------------------+

Release 0.8.1 - 2004-05-28
                 
+---------------------------------------------------------------------+
| Type  |                    Changes                    |     By      |
|-------+-----------------------------------------------+-------------|
| fix   | 080 doesn't build with maven rc2+             | stack-sf    |
+---------------------------------------------------------------------+

Release 0.8.0 - 2004-05-18
                 
+---------------------------------------------------------------------+
| Type |                Changes                |          By          |
|------+---------------------------------------+----------------------|
| add  | integrate selftest into cruisecontrol | stack-sf             |
|      | build                                 |                      |
|------+---------------------------------------+----------------------|
| add  | On reedit, red star by bad attribute  | kristinn_sig         |
|      | setting.                              |                      |
|------+---------------------------------------+----------------------|
| add  | day/night configurations              | kristinn_sig         |
|------+---------------------------------------+----------------------|
| add  | UI should only write changed config   | kristinn_sig         |
|------+---------------------------------------+----------------------|
| add  | record of settings changes should be  | johnerik             |
|      | kept                                  |                      |
|------+---------------------------------------+----------------------|
| add  | Only one build, not two               | stack-sf             |
|------+---------------------------------------+----------------------|
| add  | maven-only build rather than ant &    | stack-sf             |
|      | maven                                 |                      |
|------+---------------------------------------+----------------------|
| add  | ARCWriter should use a pool of open   | stack-sf             |
|      | files -- if it helps                  |                      |
|------+---------------------------------------+----------------------|
| add  | Precompile UI pages                   | stack-sf             |
|------+---------------------------------------+----------------------|
| add  | UI should be split into common/uncom  | kristinn_sig         |
|      | mon settings                          |                      |
|------+---------------------------------------+----------------------|
| add  | UI web pages need to be more          | kristinn_sig         |
|      | responsive                            |                      |
|------+---------------------------------------+----------------------|
| fix  | domain scope leakage                  | ia_igor              |
|------+---------------------------------------+----------------------|
| fix  | ConcurrentModificationExceptions      | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | Too many open files                   | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | ConcurrentModificationExceptions      | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | empty seeds-report.txt                | kristinn_sig         |
|------+---------------------------------------+----------------------|
| fix  | Doc. assumes bash. Allow tcsh/csh     | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | script heritrix.sh goes into infinite | stack-sf             |
|      | loop                                  |                      |
|------+---------------------------------------+----------------------|
| fix  | heritrix.sh launch file path          | stack-sf             |
|      | weirdness                             |                      |
|------+---------------------------------------+----------------------|
| fix  | ToeThreads hung in ExtractorHTML      | stack-sf             |
|      | after Pause                           |                      |
|------+---------------------------------------+----------------------|
| fix  | IllegalCharsetNameException:          | stack-sf             |
|      | Windows-1256                          |                      |
|------+---------------------------------------+----------------------|
| fix  | No doc-files/package.html in javadoc. | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | embed-count sensitivity WRT           | gojomo               |
|      | redirects, preconditions              |                      |
|------+---------------------------------------+----------------------|
| fix  | Refinement limits are not always      | kristinn_sig         |
|      | saved                                 |                      |
|------+---------------------------------------+----------------------|
| fix  | NPE exception in getMBeanInfo         | johnerik             |
|      | (settings)                            |                      |
|------+---------------------------------------+----------------------|
| fix  | Untried CrawlURIs should have clear   | gojomo               |
|      | status code                           |                      |
|------+---------------------------------------+----------------------|
| fix  | Thread underutilization in broad      | gojomo               |
|      | crawls                                |                      |
|------+---------------------------------------+----------------------|
| fix  | KeyedQueue showing EMPTY status, but  | gojomo               |
|      | the length is 1.                      |                      |
|------+---------------------------------------+----------------------|
|      | NPE in                                |                      |
| fix  | XMLSettingsHandler.recursiveFindFiles | johnerik             |
|      | ()                                    |                      |
|------+---------------------------------------+----------------------|
| fix  | Failed DNS does not have intended     | gojomo               |
|      | impact                                |                      |
|------+---------------------------------------+----------------------|
| fix  | ftp URIs are retried                  | gojomo               |
|------+---------------------------------------+----------------------|
| fix  | Refetching of robots and/or DNS       | gojomo               |
|      | broken                                |                      |
|------+---------------------------------------+----------------------|
| fix  | NPE switching to 'expert' settings in | kristinn_sig         |
|      | HEAD                                  |                      |
|------+---------------------------------------+----------------------|
| fix  | rss extractor                         | ia_igor              |
|------+---------------------------------------+----------------------|
| fix  | JS extractor clueless on relative     | ia_igor              |
|      | URIs                                  |                      |
|------+---------------------------------------+----------------------|
| fix  | converting URI's '\' into '/'         | ia_igor              |
|      | character                             |                      |
|------+---------------------------------------+----------------------|
| fix  | When going back to overrides,         | kristinn_sig         |
|      | directory is gone                     |                      |
|------+---------------------------------------+----------------------|
| fix  | shutdown.jsp unable to compile        | kristinn_sig         |
|------+---------------------------------------+----------------------|
| fix  | ARCWriterPool timeouts -- legitimate? | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | If one URI connect-fail s, hold       | gojomo               |
|      | queue, too                            |                      |
|------+---------------------------------------+----------------------|
| fix  | Fetching simple URLs fails with       | gojomo               |
|      | S_CONNECT_FAILED (-2) error           |                      |
|------+---------------------------------------+----------------------|
| fix  | seeds held back/poor breadth first?   | gojomo               |
|------+---------------------------------------+----------------------|
| fix  | Parsing links found between escaped   | ia_igor              |
|      | quotes in JavaScript                  |                      |
|------+---------------------------------------+----------------------|
| fix  | Does not extract applet URI correctky | ia_igor              |
|------+---------------------------------------+----------------------|
| fix  | links to likely-embed types should be | ia_igor              |
|      | treated as embeds                     |                      |
|------+---------------------------------------+----------------------|
| fix  | Frontier.next() forceFetches will     | gojomo               |
|      | cause assertion error                 |                      |
|------+---------------------------------------+----------------------|
| fix  | Flash link extractor causes           | ia_igor              |
|      | OutOfMemory exceptions.               |                      |
|------+---------------------------------------+----------------------|
| fix  | Should be possible to resume from     | kristinn_sig         |
|------+---------------------------------------+----------------------|
| fix  | Heritrix ignores charset              | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | Max # of arcs not being respected.    | stack-sfkristinn_sig |
|------+---------------------------------------+----------------------|
| fix  | New profile should ensure unique name | kristinn_sig         |
|------+---------------------------------------+----------------------|
| fix  | When changing scope common scope      | johnerik             |
|      | settings are lost                     |                      |
|------+---------------------------------------+----------------------|
| fix  | ssl doesn't work                      | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | Allow that people use tcsh/csh not    | stack-sf             |
|      | just bash                             |                      |
|------+---------------------------------------+----------------------|
| fix  | https SSLHandshakeException: unknown  | stack-sf             |
|      | certificate                           |                      |
|------+---------------------------------------+----------------------|
| fix  | Cannot override settings that isn't   | johnerik             |
|      | set in globals                        |                      |
|------+---------------------------------------+----------------------|
| fix  | 'Waiting for pause' even after all    | kristinn_sig         |
|      | threads done                          |                      |
|------+---------------------------------------+----------------------|
| fix  | filter 'invert', filter names need    | kristinn_sig         |
|      | work                                  |                      |
|------+---------------------------------------+----------------------|
| fix  | max-link-hops (etc.) ignored unless   | stack-sf             |
|------+---------------------------------------+----------------------|
| fix  | order.xml absolute paths              | johnerik             |
|------+---------------------------------------+----------------------|
| fix  | Cannot set TransclusionFilter         | johnerik             |
|      | attributes                            |                      |
|------+---------------------------------------+----------------------|
| fix  | Link puts garbage into arc file:      | stack-sf             |
|      | http://www.msn.com/robots.t           |                      |
+---------------------------------------------------------------------+

Release 0.6.0 - 2004-03-25
                 
+---------------------------------------------------------------------+
|  Type  |                  Changes                  |       By       |
|--------+-------------------------------------------+----------------|
| add    | 861861 Redirects(/refreshes) from seeds   | kristinn_sig   |
|        | should == new seeds                       |                |
|--------+-------------------------------------------+----------------|
| add    | 899223 Special seed-success report should | kristinn_sig   |
|        | be offered                                |                |
|--------+-------------------------------------------+----------------|
| add    | 891986 Bandwidth throttle function,       | johnerik       |
|        | setting.                                  |                |
|--------+-------------------------------------------+----------------|
| add    | 877275 integrated operator 'diary' needed | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| add    | 891983 IP, Robots expirations should be   | johnerik       |
|        | settable                                  |                |
|--------+-------------------------------------------+----------------|
| add    | 910152 Recovery of old jobs on WUI (re)   | kristinn_sig   |
|        | start                                     |                |
|--------+-------------------------------------------+----------------|
| add    | 781171 parsing css                        | ia_igor        |
|--------+-------------------------------------------+----------------|
| add    | 912986 log views should give an idea of   | kristinn_sig   |
|        | file size (where possible)                |                |
|--------+-------------------------------------------+----------------|
| add    | 912989 Alerts should have 'select all'    | kristinn_sig   |
|        | button...                                 |                |
|--------+-------------------------------------------+----------------|
| add    | 856593 [load ] [save ] [turn on/off ]     | ia_igor        |
|        | cookies                                   |                |
|--------+-------------------------------------------+----------------|
| add    | 912201 Add levels to alerts               | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| add    | 896665 Split processor chains.            | johnerik       |
|--------+-------------------------------------------+----------------|
| add    | 896754 Show total of disregards           | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| add    | 903095 Show increments of megabytes in ui | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| add    | 896794 serious errors (eg outofmemory)    | kristinn_sig   |
|        | should show up in UI                      |                |
|--------+-------------------------------------------+----------------|
| add    | 900520 Short description of ComplexTypes  | kristinn_sig   |
|        | in user interface.                        |                |
|--------+-------------------------------------------+----------------|
| add    | 899982 Should be possible to alter        | kristinn_sig   |
|        | filters while crawling.                   |                |
|--------+-------------------------------------------+----------------|
| add    | 896672 Display progress (doc/sec) with    | kristinn_sig   |
|        | more precision                            |                |
|--------+-------------------------------------------+----------------|
| add    | 896677 Highlight the success or failures  | kristinn_sig   |
|        | of each seed                              |                |
|--------+-------------------------------------------+----------------|
| add    | 896760 Prominent notification when seeds  | kristinn_sig   |
|        | have problems                             |                |
|--------+-------------------------------------------+----------------|
| add    | 896801 java regexps (in log view) need    | kristinn_sig   |
|        | help text                                 |                |
|--------+-------------------------------------------+----------------|
| add    | 896778 Log viewing enhancements:          | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| add    | 896795 frontier, thread report            | kristinn_sig   |
|        | improvements                              |                |
|--------+-------------------------------------------+----------------|
| add    | 876516 default launch should nohup, save  | ia_igor        |
|        | stdout/stderr                             |                |
|--------+-------------------------------------------+----------------|
| update | Update of httpclient to release 2.0       | stack-sf       |
|--------+-------------------------------------------+----------------|
| update | Update of jmx libs to release 1.2.1       | stack-sf       |
|--------+-------------------------------------------+----------------|
| fix    | 896763 127.0.0.1 in job report            | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| fix    | 896767 Frontier retry-delay should        | kristinn_sig   |
|        | include units (eg -seconds)               |                |
|--------+-------------------------------------------+----------------|
| fix    | 898994 Revisiting admin URIs if not       | kristinn_sig   |
|        | logged in should prompt login             |                |
|--------+-------------------------------------------+----------------|
| fix    | 899019 Deadlock in Andy's 2nd Crawl       | johnerik       |
|--------+-------------------------------------------+----------------|
| fix    | 767225 Better bad-config handling         | parkerthompson |
|--------+-------------------------------------------+----------------|
| fix    | 815357 mysterious pause facing network    | gojomo         |
|        | (DNS) problem                             |                |
|--------+-------------------------------------------+----------------|
| fix    | 896747 ExtractorJS's report overstates    | kristinn_sig   |
|        | it's discovered URIs                      |                |
|--------+-------------------------------------------+----------------|
| fix    | 896667 Web UI does not display correctly  | kristinn_sig   |
|        | in IE                                     |                |
|--------+-------------------------------------------+----------------|
| fix    | 896780 console clarity/safety             | kristinn_sig   |
|--------+-------------------------------------------+----------------|
| fix    | 896655 Does not respect per settings      | johnerik       |
|        | added after crawl was started.            |                |
|--------+-------------------------------------------+----------------|
| fix    | 856555 'empty' records in compressed arc  | ia_igor        |
|        | files                                     |                |
+---------------------------------------------------------------------+

Release 0.4.1 - 2004-02-12
                 
+---------------------------------------------------------------------+
| Type  |                    Changes                     |     By     |
|-------+------------------------------------------------+------------|
| fix   | 895955 URIRegExpFilter retains memory          | stack-sf   |
+---------------------------------------------------------------------+

Release 0.4.0 - 2004-02-10
                 
+---------------------------------------------------------------------+
|  Type  |                     Changes                     |    By    |
|--------+-------------------------------------------------+----------|
|        | New MBEAN-based configuration system. Reads and |          |
| add    | writes XML to validate against                  | stack-sf |
|        | heritrix_settings.xsd.                          |          |
|--------+-------------------------------------------------+----------|
| update | UI extensively revamped. Exploits new           | stack-sf |
|        | configuration system.                           |          |
|--------+-------------------------------------------------+----------|
| add    | 60-odd unit tests added.                        | stack-sf |
|--------+-------------------------------------------------+----------|
| add    | Integration selftest framework.                 | stack-sf |
|--------+-------------------------------------------------+----------|
| add    | Added pooling of ARCWriters.                    | stack-sf |
|--------+-------------------------------------------------+----------|
|        | Start script backgrounds heritrix and redirects |          |
|        | stdout/stderr to heritrix_out.log. See 876516   |          |
| update | Default launch should nohup, save stdout/stderr | stack-sf |
|        | Web UI accesses are loggged to heritrix_out.log |          |
|        | also.                                           |          |
|--------+-------------------------------------------------+----------|
| update | Updated httpclient to version 2.0RC3.           | stack-sf |
|--------+-------------------------------------------------+----------|
| fix    | 763517 IAGZIPOutputStream NPE under IBM JVM     | stack-sf |
|--------+-------------------------------------------------+----------|
| fix    | 809018 Cleaner versioned testing build needed   | stack-sf |
|--------+-------------------------------------------------+----------|
| fix    | 872729 Cmd-line options for setting web ui      | stack-sf |
|        | username/password                               |          |
|--------+-------------------------------------------------+----------|
| fix    | Universal single-pass extractor                 | stack-sf |
+---------------------------------------------------------------------+

Release 0.2.0 - 2003-01-05
                 
+---------------------------------------------------------------------+
|  Type   |                  Changes                  |      By       |
|---------+-------------------------------------------+---------------|
| add     | First 'official' release.                 | stack-sf      |
+---------------------------------------------------------------------+

Release 0.1.0 - 2003-12-31
                 
+---------------------------------------------------------------------+
| Type |                       Changes                       |   By   |
|------+-----------------------------------------------------+--------|
|      | Initial Mavenized development version number (CVS/  |        |
| add  | internal only). Added everything to new project     | gojomo |
|      | layout.                                             |        |
+---------------------------------------------------------------------+

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
