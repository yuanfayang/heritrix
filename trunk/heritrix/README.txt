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

Heritrix Release Notes

Table of Contents

6.1. Release 1.2.0 - TBD
    6.1.1. Known Limitations
    6.1.2. Changes

6.2. Release 1.0.4 - 2004-09-22
    6.2.1. Changes

6.3. Release 1.0.2 - 2004-09-14
    6.3.1. Changes

6.4. Release 1.0.0 - 2004-08-06
    6.4.1. Known Limitations
    6.4.2. Changes

6.5. Release 0.10.0 - 2004-06-046
    6.5.1. Changes

6.6. Release 0.10.0 - 2004-06-04
    6.6.1. Changes

6.7. Release 0.8.0 - 2004-05-18
    6.7.1. Synopsis
    6.7.2. Changes

6.8. Release 0.6.0 - 2004-03-25
    6.8.1. Changes

6.9. Release 0.4.1 - 2004-06-04
    6.9.1. Changes

6.10. Release 0.4.0 - 2004-02-10
    6.10.1. Changes

6.11. Release 0.2.0 - 2004-01-05
6.12. Release 0.1.0 - 2003-12-31


6.1. Release 1.2.0 - TBD

Abstract

TBD

6.1.1. Known Limitations

6.1.1.1. IBM JVM

The IBM JVM generally is more performant than SUN JVMs. It also emits more
detailed heap dumps. For these reasons we like the IBM JVM. That said, new
Heritrix 1.2.0 features may not work on the IBM JVM.

6.1.1.1.1. HTTPS

Heritrix 1.2.0 uses the new HttpClient 3.0x library which allows the setting of
socket read timeouts. Connections to https sites fail if using the IBM JVM.

The IBM JVM 141 (cxia321411-20030930) NPEs setting the NoTcpDelay.

java.lang.NullPointerException
   at com.ibm.jsse.bf.setTcpNoDelay(Unknown Source)
   at org.apache.commons.httpclient.HttpConnection.open(HttpConnection.java:683)
   at org.apache.commons.httpclient.MultiThreadedHttpConnectionManager$HttpConnectionAdapter.open(MultiThreadedHttpConnectionManager.java:1328)

Using the IBM JVM 142, its saying SSL connection not open when we go to use
inputstreams:

java.net.SocketException: Socket is not connected
   at java.net.Socket.getInputStream(Socket.java:726)     at com.ibm.jsse.bs.getInputStream(Unknown Source)
   at org.apache.commons.httpclient.HttpConnection.open(HttpConnection.java:715)
   at org.apache.commons.httpclient.MultiThreadedHttpConnectionManager$HttpConnectionAdapter.open(MultiThreadedHttpConnectionManager.java:1328)

Newer versions of the httpclient library may address this (Current version is
alpha2).

6.1.2. Changes

Table 1. Changes

+-----------------------------------------------------------------------------+
|  ID   |Type|                Summary                 |Open Date |     By     |
|-------+----+----------------------------------------+----------+------------|
|903845 |Add |IP-based politeness                     |2004-10-28|gojomo      |
|-------+----+----------------------------------------+----------+------------|
|1054849|Add |Recover from crawl initialized with a   |2004-10-26|stack-sf    |
|       |    |recovery log                            |          |            |
|-------+----+----------------------------------------+----------+------------|
|1054851|Add |Import gzipped or non-gzipped recovery  |2004-10-26|stack-sf    |
|       |    |log                                     |          |            |
|-------+----+----------------------------------------+----------+------------|
|1050378|Add |Add bdb alreadyseen option to           |2004-10-19|stack-sf    |
|       |    |hostsqueuesfrontier                     |          |            |
|-------+----+----------------------------------------+----------+------------|
|973881 |Add |Force generation of report files        |2004-06-16|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1010883|Add |Scripts to generate end-of-job reports  |2004-08-17|danavery    |
|-------+----+----------------------------------------+----------+------------|
|988277 |Add |[Need feedback] "Done with ARC file"    |2004-07-09|stack-sf    |
|       |    |event                                   |          |            |
|-------+----+----------------------------------------+----------+------------|
|1044977|Add |Logging of scope-rejected URIs          |2004-10-11|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|902970 |Add |HTTPClient should use supplied IP /     |2004-02-23|stack-sf    |
|       |    |avoid DNS lookup                        |          |            |
|-------+----+----------------------------------------+----------+------------|
|903093 |Add |Setting of Integer.MAX_VALUE is ugly    |2004-02-23|nobody      |
|       |    |                                        |          |stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|900004 |Add |canonicalization of URIs for            |2004-02-18|stack-sf    |
|       |    |alreadyIncluded testing                 |          |            |
|-------+----+----------------------------------------+----------+------------|
|941072 |Add |Allow operator-configured mid-HTTP-fetch|2004-04-23|stack-sf    |
|       |    |filters                                 |          |            |
|-------+----+----------------------------------------+----------+------------|
|1037891|Add |Cmdline defaults in properties file     |2004-09-30|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1037304|Add |Upgrade httpclient to 3.0.x             |2004-09-29|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|994141 |Add |Update build to use maven 1.0           |2004-07-19|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1002336|Add |Figure what profiler to use             |2004-08-02|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1000840|Fix |DiskIncludedFrontier performance is     |2004-07-30|gojomo      |
|       |    |awful                                   |          |            |
|-------+----+----------------------------------------+----------+------------|
|1043251|Fix |better/longer dns retries on lookup     |2004-10-08|gojomo      |
|       |    |failure                                 |          |            |
|-------+----+----------------------------------------+----------+------------|
|1051072|Fix |ExtractorHTML takes forever on          |2004-10-20|gojomo      |
|       |    |worst-case HTML                         |          |            |
|-------+----+----------------------------------------+----------+------------|
|1051916|Fix |ExtractorJS takes forever on worst-case |2004-10-21|gojomo      |
|       |    |JS                                      |          |            |
|-------+----+----------------------------------------+----------+------------|
|1050238|Fix |jdk required (doc implies jre)          |2004-10-19|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1038135|Fix |prerequisite hysteresis/robots ahead of |2004-09-30|gojomo      |
|       |    |dns                                     |          |            |
|-------+----+----------------------------------------+----------+------------|
|1015728|Fix |Crawl upper time/size bounds ignored    |2004-08-24|gojomo      |
|-------+----+----------------------------------------+----------+------------|
|1002356|Fix |timing issue on crawl-start & run-time  |2004-08-02|gojomo      |
|       |    |stat                                    |          |            |
|-------+----+----------------------------------------+----------+------------|
|1002332|Fix |inactiveQueuesMemoryLoadTarget mechanism|2004-08-02|gojomo      |
|       |    |behaves poorly                          |          |            |
|-------+----+----------------------------------------+----------+------------|
|1045016|Fix |DNS URIs don't get override settings    |2004-10-11|gojomo      |
|-------+----+----------------------------------------+----------+------------|
|998184 |Fix |Gzipped recover log corrupt at end; last|2004-07-26|gojomo      |
|       |    |< 32K unrecoverable                     |          |            |
|-------+----+----------------------------------------+----------+------------|
|998272 |Fix |No crawl if host-queues-memory-capacity |2004-07-26|nobody      |
|       |    |= 0                                     |          |stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1002335|Fix |frontier report unusable in big crawls; |2004-08-02|gojomo      |
|       |    |frontier info needed                    |          |            |
|-------+----+----------------------------------------+----------+------------|
|984390 |Fix |Build fails: "rws" mode and Mac OS X    |2004-07-02|stack-sf    |
|       |    |interact badly                          |          |            |
|-------+----+----------------------------------------+----------+------------|
|1000929|Fix |fatal runtimeexceptions in frontier give|2004-07-30|gojomo      |
|       |    |no info in web UI                       |          |            |
|-------+----+----------------------------------------+----------+------------|
|964625 |Fix |seed parser *too* lenient               |2004-06-01|johnerik    |
|-------+----+----------------------------------------+----------+------------|
|980051 |Fix |Auth unsupported logged to console      |2004-06-25|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1002146|Fix |bad queue keys: shouldn't be URIs;      |2004-08-02|stack-sf    |
|       |    |should be handled better                |          |            |
|-------+----+----------------------------------------+----------+------------|
|1046696|Fix |UURIFactory.validateEscaping() ->       |2004-10-13|stack-sf    |
|       |    |IllegalArgumentException                |          |            |
|-------+----+----------------------------------------+----------+------------|
|1045736|Fix |ARCReader crashes if zero-length gzip   |2004-10-12|stack-sf    |
|       |    |record                                  |          |            |
|-------+----+----------------------------------------+----------+------------|
|1002144|Fix |[UURI] Catch bad-encoding earlier       |2004-08-02|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1036680|Fix |PathDepthFilter innerAccepts SEVERE log:|2004-09-28|stack-sf    |
|       |    |"Failed getPath..."                     |          |            |
|-------+----+----------------------------------------+----------+------------|
|1045847|Fix |Unnecessary toString() in               |2004-10-12|gojomo      |
|       |    |ExtractorHTML.processScriptCode()       |          |            |
|-------+----+----------------------------------------+----------+------------|
|1044527|Fix |Domain names in 'overrides' are not in  |2004-10-11|kristinn_sig|
|       |    |alphabetical order                      |          |            |
|-------+----+----------------------------------------+----------+------------|
|1012639|Fix |If CC timesout selftest, no build failed|2004-08-19|stack-sf    |
|       |    |message                                 |          |            |
|-------+----+----------------------------------------+----------+------------|
|1012642|Fix |selftest hanging because no crawl stop  |2004-08-19|stack-sf    |
|       |    |event                                   |          |            |
|-------+----+----------------------------------------+----------+------------|
|1036720|Fix |NPE in ArcWriterProcessor.writeDns()    |2004-09-28|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|931565 |Fix |CrawlStateUpdater - NullPointerException|2004-04-08|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|973294 |Fix |NoSuchElementException in URI queues    |2004-06-15|gojomo      |
|       |    |halts crawling                          |          |            |
|-------+----+----------------------------------------+----------+------------|
|1033657|Fix |[UURI] >2047 AFTER escaping (Stops      |2004-09-23|stack-sf    |
|       |    |crawl)                                  |          |            |
|-------+----+----------------------------------------+----------+------------|
|1010966|Fix |crawl.log has URIs with spaces in them  |2004-08-17|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|963970 |Fix |unfetchable URI schemes should never be |2004-05-31|gojomo      |
|       |    |queued                                  |          |            |
|-------+----+----------------------------------------+----------+------------|
|1031607|Fix |KeyedQueue server<->key mismatch noted: |2004-09-20|stack-sf    |
|       |    |pfbuser<->mprsrv.agr                    |          |            |
|-------+----+----------------------------------------+----------+------------|
|1031525|Fix |NPE reading override                    |2004-09-20|stack-sf    |
|-------+----+----------------------------------------+----------+------------|
|1031168|Fix |Wrong handling of date in               |2004-09-20|johnerik    |
|       |    |ARCRecordMetaData                       |          |            |
+-----------------------------------------------------------------------------+

6.2. Release 1.0.4 - 2004-09-22

Abstract

Bug fix.

6.2.1. Changes

Table 2. Changes

+--------------------------------------------------------------+
|  ID   |Type|               Summary                |Open Date |
|-------+----+--------------------------------------+----------|
|1010966|Fix |crawl.log has URIs with spaces in them|2004-08-17|
+--------------------------------------------------------------+

6.3. Release 1.0.2 - 2004-09-14

Abstract

Bug fixes.

6.3.1. Changes

Table 3. Changes

+------------------------------------------------------------+
|  ID   |Type|                    Summary                    |
|-------+----+-----------------------------------------------|
|1020770|Fix |old crawls stick around, consuming memory      |
|-------+----+-----------------------------------------------|
|1002319|Fix |Terminating paused crawl leaves zombie threads |
|-------+----+-----------------------------------------------|
|935146 |Fix |Excessive ARCWriterPool timeouts               |
|-------+----+-----------------------------------------------|
|1010859|Fix |Per host overrides not taking effect.          |
|-------+----+-----------------------------------------------|
|1014732|Fix |document size limit not working                |
|-------+----+-----------------------------------------------|
|1012520|Fix |UURI.length() > 2k                             |
|-------+----+-----------------------------------------------|
|1010966|Fix |crawl.log has URIs with spaces in them stack-sf|
+------------------------------------------------------------+

6.4. Release 1.0.0 - 2004-08-06

Abstract

Added new prefix ('SURT') scope and filter, compression of recovery log, mass
adding of URIs to running crawler, crawling via a http proxy, adding of headers
to request, improved out-of-the-box defaults, hash of content to crawl log and
to arcreader output, and many bug fixes.

6.4.1. Known Limitations

6.4.1.1. Crawl Size Upper Bounds

Heritrix 1.0.0 uses disk-based queues to hold any number of pending URIs
bounded only by available disk space, but still relies on in-memory structures
to efficiently track all discovered hosts and previously-scheduled URIs. Crawls
whose total scheduled URIs or discovered hosts exhaust all available memory
will trigger out-of-memory errors, which freeze a crawl at the point of the
error.

With the default settings, and an assignment of a 256MB Java heap to the
Heritrix process, crawling which discovers up to 10 000 hosts, and schedules
over 6 000 000 URIs, should be possible. Discovery of higher numbers of URIs/
hosts will likely trigger out-of-memory problems unless a larger java heap was
assigned at startup.

Broad crawls -- those using the BroadScope or ranging over domains with many
subdomains -- can easily and quickly exceed these parameters. Thus broad crawls
in Heritrix 1.0.0 are not recommended, except for experimental purposes.

Narrower crawls, restricted to specific hosts or domains a limited number of
subdomains, can run for a week or more, collecting millions of resources.
Larger heaps can allow crawls to run into the tens of millions of collected
URIS, and tens of thousands of discovered hosts.

An experimental alternate Frontier, the DiskIncludedFrontier, is also available
via the 'Modules' crawl configuration tab. It uses a capped amount of memory
plus disk storage to remember any number of scheduled URIs, but its performance
is poor and it has not received the same testing as our default Frontier. The
memory cost of additional discovered hosts continues to rise without limit when
using a DiskIncludedFrontier.

Future versions of Heritrix will include other frontier implementations
allowing larger and unbounded crawls with minimal performance penalties.

6.4.1.2. [ 958055 ] Seed ConcurrentModificationException

Its possible to get ConcurrentModificationsException editing options on a
running crawl.

6.4.1.2.1. Workaround

Pause the crawl when making changes to crawl options.

6.4.1.3. [ 984390 ] Build fails: "rws" mode and Mac OS X interact badly

On macintoshes and linux kernel version 2.6, heritrix fails to build (unit
tests fail).

6.4.1.3.1. Workaround

See issue, [ 984390 ] Build fails: "rws" mode and Mac OS X interact badly, for
source code workaround edit.

6.4.1.4. [ 955975 ] Build fails: JVM and kernel 2.6+ (Was 2 tests fail...)

Heritrix fails to build on linux kernel 2.6.

6.4.1.4.1. Workaround

Build fails unless you use a JDK in advance of pedigree 1.5 beta 2 (It works
with jdk1.5.0-rc). See [ 955975 ] Build fails: JVM and kernel 2.6+ (Was 2 tests
fail...) and above.

6.4.2. Changes

Table 4. Changes

+------------------------------------------------------------------------+
|  ID  |Type|                          Summary                           |
|------+----+------------------------------------------------------------|
|939679|Add |Mass-add URIs to running crawl and force reconsideration    |
|------+----+------------------------------------------------------------|
|986977|Add |SurtPrefix scope (and filter)                               |
|------+----+------------------------------------------------------------|
|989816|Add |Specification of default CharSequence charset               |
|------+----+------------------------------------------------------------|
|983001|Add |crawl.log entries all on one line                           |
|------+----+------------------------------------------------------------|
|869584|Add |Hash content-bodies, show in logs (and future ARCs)         |
|------+----+------------------------------------------------------------|
|964581|Add |option to preference (quick-get) embeds                     |
|------+----+------------------------------------------------------------|
|964493|Add |Compress recover.log                                        |
|------+----+------------------------------------------------------------|
|988106|Add |[UURI] 'http:///...' converted to 'http://...'              |
|------+----+------------------------------------------------------------|
|926143|Add |enable use through HTTP proxy                               |
|------+----+------------------------------------------------------------|
|945922|Add |Allow adding (subtracting?) http headers                    |
|------+----+------------------------------------------------------------|
|983109|Add |Improved out-of-the-box defaults                            |
|------+----+------------------------------------------------------------|
|982909|Add |ARCWriter makes FAT gzip header                             |
|------+----+------------------------------------------------------------|
|925734|Add |exponential backoff URI/host retries                        |
|------+----+------------------------------------------------------------|
|-     |Fix |Total data "written" isn't necessarily written (wording)    |
|------+----+------------------------------------------------------------|
|-     |Fix |embeds within scope problem                                 |
|------+----+------------------------------------------------------------|
|-     |Fix |NPE clearing alerts                                         |
|------+----+------------------------------------------------------------|
|-     |Fix |arcmetadata repeated once for every domain config           |
|------+----+------------------------------------------------------------|
|-     |Fix |CCE deserializing diskqueue [Was: IllegalArgumentExcepti...]|
|------+----+------------------------------------------------------------|
|-     |Fix |no docs for recovery-journal feature                        |
|------+----+------------------------------------------------------------|
|-     |Fix |Pause/Terminate ignored on 2.6 kernel 1.5 JVM               |
|------+----+------------------------------------------------------------|
|-     |Fix |Investigate "Relative URI but no base"                      |
|------+----+------------------------------------------------------------|
|-     |Fix |User-Agent should be able to mimic Mozilla (as does Google) |
|------+----+------------------------------------------------------------|
|-     |Fix |referral URL should be stored in recover.log                |
|------+----+------------------------------------------------------------|
|-     |Fix |ToeThreads hung in FetchDNS after Pause                     |
|------+----+------------------------------------------------------------|
|-     |Fix |robots.txt lookup for different ports on same host          |
|------+----+------------------------------------------------------------|
|-     |Fix |Empty log percentages displayed as NaN%                     |
|------+----+------------------------------------------------------------|
|-     |Fix |UURI doubly-encodes %XX sequences                           |
|------+----+------------------------------------------------------------|
|-     |Fix |Single settings change causes two versions to be created    |
|------+----+------------------------------------------------------------|
|-     |Fix |New IA debian image is 2.6 (Was: Build fails: JVM and ...)  |
|------+----+------------------------------------------------------------|
|-     |Fix |NPE in PathDepthFilter                                      |
|------+----+------------------------------------------------------------|
|-     |Fix |[investigate & rule out] Thread report deadlock risks       |
|------+----+------------------------------------------------------------|
|-     |Fix |jetty susceptible to DoS attack                             |
|------+----+------------------------------------------------------------|
|-     |Fix |'ignore' robots does not ignore meta nofollow               |
|------+----+------------------------------------------------------------|
|-     |Fix |URI Syntax Errors stop page parsing.                        |
|------+----+------------------------------------------------------------|
|-     |Fix |NPE in ExtractorHTML/TextUtils.getMatcher()                 |
|------+----+------------------------------------------------------------|
|-     |Fix |ARCReader: Failed to find GZIP MAGIC                        |
|------+----+------------------------------------------------------------|
|-     |Fix |javascript embedded URLs                                    |
|------+----+------------------------------------------------------------|
|-     |Fix |NoClassDefFoundError when starting a job                    |
|------+----+------------------------------------------------------------|
|-     |Fix |Max number of deferrals hard-coded to 10.                   |
|------+----+------------------------------------------------------------|
|-     |Fix |Frontier report thread safety problems?                     |
|------+----+------------------------------------------------------------|
|-     |Fix |ARCReader hanging                                           |
|------+----+------------------------------------------------------------|
|-     |Fix |log-browsing by regexp outofmemoryerror                     |
|------+----+------------------------------------------------------------|
|-     |Fix |Deferred URLs due the DNS problem -- Heritrix(-50)-Deferred |
|------+----+------------------------------------------------------------|
|-     |Fix |Assertion failures shouldn't be more fatal than Runtime Exc.|
|------+----+------------------------------------------------------------|
|-     |Fix |min-interval is superfluous; remove                         |
|------+----+------------------------------------------------------------|
|-     |Fix |crawl doesn't end when using valence > 1                    |
|------+----+------------------------------------------------------------|
|-     |Fix |Giant (in # of files) state directory problematic           |
|------+----+------------------------------------------------------------|
|-     |Fix |robots-expiration units, default wrong                      |
|------+----+------------------------------------------------------------|
|-     |Fix |NoSuchElementException in URI queues halts crawling         |
|------+----+------------------------------------------------------------|
|-     |Fix |#anchor links not trimmed, and thus recrawled               |
|------+----+------------------------------------------------------------|
|-     |Fix |arc's filedesc file name includes .gz                       |
|------+----+------------------------------------------------------------|
|-     |Fix |[denmark-workshop] Cookie mangling                          |
|------+----+------------------------------------------------------------|
|-     |Fix |HttpException: Unable to parse header                       |
|------+----+------------------------------------------------------------|
|-     |Fix |bogus ARC-header when no Content-type                       |
|------+----+------------------------------------------------------------|
|-     |Fix |paths when crawling without UI                              |
|------+----+------------------------------------------------------------|
|-     |Fix |domain scope leakage                                        |
+------------------------------------------------------------------------+

6.5. Release 0.10.0 - 2004-06-046

Abstract

Release for second heritrix workshop, Copenhagen 06/2004 (1.0.0 first release
candidate). Added site-first prioritization, fixed link extraction of multibyte
URIs, added metadata to arcs as xml, changed arc naming template, new user and
developer manuals, added basic/digest auth and http post/get login facility,
and added help to UI. Bug fixes.

6.5.1. Changes

Table 5. Changes

+------------------------------------------------------------------------+
|  ID  |Type|                          Summary                           |
|------+----+------------------------------------------------------------|
|896769|Add |job report: show 'active' hosts, show more size totals      |
|------+----+------------------------------------------------------------|
|896772|Add |"Site-first"/'frontline' prioritization                     |
|------+----+------------------------------------------------------------|
|956614|Add |multiple open http connections per host needed              |
|------+----+------------------------------------------------------------|
|896674|Add |Add help to web UI                                          |
|------+----+------------------------------------------------------------|
|964931|Add |When a host last had a completed URI shown in crawl report  |
|------+----+------------------------------------------------------------|
|958335|Add |Encode multibyte URIs using page charset before queuing     |
|------+----+------------------------------------------------------------|
|909246|Add |One src for site, help, and readme docs.                    |
|------+----+------------------------------------------------------------|
|936684|Add |identifying ARCs: unique names, header records              |
|------+----+------------------------------------------------------------|
|930667|Add |Resetting arc file counter for every job.                   |
|------+----+------------------------------------------------------------|
|863318|Add |ARCs need better headers                                    |
|------+----+------------------------------------------------------------|
|908507|Add |Specify location of jobs dir                                |
|------+----+------------------------------------------------------------|
|914301|Add |Logging in (HTTP POST, Basic Auth, etc.)                    |
|------+----+------------------------------------------------------------|
|944066|Add |Update dnsjava from 1.5 to 1.6.2 (Fix NPE)                  |
|------+----+------------------------------------------------------------|
|966168|Fix |crawl.log entries without annotations end with a space      |
|------+----+------------------------------------------------------------|
|966172|Fix |An issue with arc names' date and serial number alignment   |
|------+----+------------------------------------------------------------|
|957963|Fix |Output of warning message leads to NullPointerExceptions    |
|------+----+------------------------------------------------------------|
|963965|Fix |Either UURI or ExtractHTML should strip whitespace better   |
|------+----+------------------------------------------------------------|
|965267|Fix |Maximum documents not enforced                              |
|------+----+------------------------------------------------------------|
|965308|Fix |NPE in path depth filter                                    |
|------+----+------------------------------------------------------------|
|934549|Fix |embed/speculative inclusion too loose                       |
|------+----+------------------------------------------------------------|
|962899|Fix |UnsupportedCharsetException handled awkwardly               |
|------+----+------------------------------------------------------------|
|962892|Fix |UURI accepting/creating unUsable URIs (bad hosts)           |
|------+----+------------------------------------------------------------|
|860733|Fix |CachingDiskLongFPSet UI availability                        |
|------+----+------------------------------------------------------------|
|954130|Fix |Crawls slow till change a setting                           |
|------+----+------------------------------------------------------------|
|961867|Fix |zero link-hops should work                                  |
|------+----+------------------------------------------------------------|
|942627|Fix |multiple robots.txt URLs in the "default" frontier          |
|------+----+------------------------------------------------------------|
|957941|Fix |NPE in ExtractorHTML#isHtmlExpectedHere                     |
|------+----+------------------------------------------------------------|
|953718|Fix |Unwanted behavior with seed redirection                     |
|------+----+------------------------------------------------------------|
|952636|Fix |Link extraction failing                                     |
|------+----+------------------------------------------------------------|
|863315|Fix |Memory issues: Frontier.snoozeQueue                         |
|------+----+------------------------------------------------------------|
|903838|Fix |Transitive scope confusion, may not work as expected        |
|------+----+------------------------------------------------------------|
|955345|Fix |Wrong stats after deleting URIs from Frontier               |
|------+----+------------------------------------------------------------|
|952276|Fix |NoSuchElementException in admin/reports/frontier.jsp        |
|------+----+------------------------------------------------------------|
|952665|Fix |Alert: Authentication scheme(s) not supported               |
|------+----+------------------------------------------------------------|
|936702|Fix |IP validity: units, TTL vs. setting                         |
|------+----+------------------------------------------------------------|
|951582|Fix |ConcurrentModificationException in DomainScope focus filter |
|------+----+------------------------------------------------------------|
|949489|Fix |ConcurrentModificationException terminate job               |
|------+----+------------------------------------------------------------|
|949551|Fix |Authentication bug                                          |
|------+----+------------------------------------------------------------|
|948898|Fix |terminate running crawl == NPE                              |
|------+----+------------------------------------------------------------|
|927940|Fix |java.net.URI parses %20 but getHost null                    |
|------+----+------------------------------------------------------------|
|874220|Fix |NPE in java.net.URI.encode                                  |
|------+----+------------------------------------------------------------|
|808270|Fix |java.net.URI chokes on hosts_with_underscores               |
|------+----+------------------------------------------------------------|
|788277|Fix |Doing separate DNS lookup for same host                     |
|------+----+------------------------------------------------------------|
|910120|Fix |java.net.URI#getHost fails when leading digit               |
|------+----+------------------------------------------------------------|
|949548|Fix |Constraining java URI class                                 |
|------+----+------------------------------------------------------------|
|943373|Fix |Same CrawlServer instance for http & https.                 |
|------+----+------------------------------------------------------------|
|887999|Fix |Broad crawl/ too many open files                            |
|------+----+------------------------------------------------------------|
|926912|Fix |multiple charset headers + long lines                       |
|------+----+------------------------------------------------------------|
|926338|Fix |Corrupted blue image in progress bars                       |
|------+----+------------------------------------------------------------|
|896757|Fix |NPEs in Andy's Th-Fri Crawl + NPE in RIS                    |
|------+----+------------------------------------------------------------|
|922080|Fix |IllegalArgumentEx/ReplayCharSequenceFactory (offset vs. size|
|------+----+------------------------------------------------------------|
|935271|Fix |FTP URIs in seeds interpreted as HTTP                       |
|------+----+------------------------------------------------------------|
|945923|Fix |maven rc2 won't make src distribution                       |
|------+----+------------------------------------------------------------|
|947754|Fix |Corrupted arc files on termination of job                   |
|------+----+------------------------------------------------------------|
|931269|Fix |https exception: java.io.IOException: SSL failure           |
|------+----+------------------------------------------------------------|
|935146|Fix |Excessive ARCWriterPool timeouts:                           |
+------------------------------------------------------------------------+

6.6. Release 0.10.0 - 2004-06-04

Abstract

Fixes to build with maven rc2+.

6.6.1. Changes

Table 6. Changes

+---------------------------------------------+
|  ID  |Type|             Summary             |
|------+----+---------------------------------|
|962361|Fix |080 doesn't build with maven rc2+|
+---------------------------------------------+

6.7. Release 0.8.0 - 2004-05-18

Abstract

Release (and branch heritrix-0_8 made at the heritrix-0_7_1 tag) because of
concurrentmodificationexceptions if tens of seeds supplied and to fix
domain-scope leakage. Also, made continuous build publically available,
incorporated integration selftest into build, made it a maven-build only
(ant-build no longer supported), added day/night configurations (refinements),
ameliorated too-many-open files, added exploit of http-header content-type
charset creating character streams, and heritrix now crawls ssl sites. UI
improvements include red start by bad configuration, precompilation, and
delineation of advanced settings. Many bug fixes.

6.7.1. Synopsis

6.7.2. Changes

Table 7. Changes

+------------------------------------------------------------------------+
|  ID  |Type|                          Summary                           |
|------+----+------------------------------------------------------------|
|939032|Add |integrate selftest into cruisecontrol build                 |
|------+----+------------------------------------------------------------|
|903078|Add |On reedit, red star by bad attribute setting.               |
|------+----+------------------------------------------------------------|
|935215|Add |day/night configurations                                    |
|------+----+------------------------------------------------------------|
|928745|Add |UI should only write changed config                         |
|------+----+------------------------------------------------------------|
|908723|Add |record of settings changes should be kept                   |
|------+----+------------------------------------------------------------|
|909249|Add |Only one build, not two                                     |
|------+----+------------------------------------------------------------|
|925614|Add |maven-only build rather than ant & maven                    |
|------+----+------------------------------------------------------------|
|877295|Add |ARCWriter should use a pool of open files -- if it helps    |
|------+----+------------------------------------------------------------|
|899226|Add |Precompile UI pages                                         |
|------+----+------------------------------------------------------------|
|896798|Add |UI should be split into common/uncommon settings            |
|------+----+------------------------------------------------------------|
|895341|Add |UI web pages need to be more responsive                     |
|------+----+------------------------------------------------------------|
|955527|Fix |domain scope leakage                                        |
|------+----+------------------------------------------------------------|
|943770|Fix |ConcurrentModificationExceptions                            |
|------+----+------------------------------------------------------------|
|943768|Fix |Too many open files                                         |
|------+----+------------------------------------------------------------|
|943781|Fix |ConcurrentModificationExceptions                            |
|------+----+------------------------------------------------------------|
|943453|Fix |empty seeds-report.txt                                      |
|------+----+------------------------------------------------------------|
|903092|Fix |Doc. assumes bash. Allow tcsh/csh                           |
|------+----+------------------------------------------------------------|
|908419|Fix |script heritrix.sh goes into infinite loop                  |
|------+----+------------------------------------------------------------|
|922104|Fix |heritrix.sh launch file path weirdness                      |
|------+----+------------------------------------------------------------|
|935122|Fix |ToeThreads hung in ExtractorHTML after Pause                |
|------+----+------------------------------------------------------------|
|938591|Fix |IllegalCharsetNameException: Windows-1256                   |
|------+----+------------------------------------------------------------|
|934642|Fix |No doc-files/package.html in javadoc.                       |
|------+----+------------------------------------------------------------|
|815544|Fix |embed-count sensitivity WRT redirects, preconditions        |
|------+----+------------------------------------------------------------|
|936610|Fix |Refinement limits are not always saved                      |
|------+----+------------------------------------------------------------|
|935340|Fix |NPE exception in getMBeanInfo(settings)                     |
|------+----+------------------------------------------------------------|
|904767|Fix |Untried CrawlURIs should have clear status code             |
|------+----+------------------------------------------------------------|
|914287|Fix |Thread underutilization in broad crawls                     |
|------+----+------------------------------------------------------------|
|930736|Fix |KeyedQueue showing EMPTY status, but the length is 1.       |
|------+----+------------------------------------------------------------|
|934585|Fix |NPE in XMLSettingsHandler.recursiveFindFiles()              |
|------+----+------------------------------------------------------------|
|935352|Fix |Failed DNS does not have intended impact                    |
|------+----+------------------------------------------------------------|
|896764|Fix |ftp URIs are retried                                        |
|------+----+------------------------------------------------------------|
|848661|Fix |Refetching of robots and/or DNS broken                      |
|------+----+------------------------------------------------------------|
|935221|Fix |NPE switching to 'expert' settings in HEAD                  |
|------+----+------------------------------------------------------------|
|896779|Fix |rss extractor                                               |
|------+----+------------------------------------------------------------|
|896775|Fix |JS extractor clueless on relative URIs                      |
|------+----+------------------------------------------------------------|
|913214|Fix |converting URI's '\' into '/' character                     |
|------+----+------------------------------------------------------------|
|928665|Fix |When going back to overrides, directory is gone             |
|------+----+------------------------------------------------------------|
|923342|Fix |shutdown.jsp unable to compile                              |
|------+----+------------------------------------------------------------|
|913876|Fix |ARCWriterPool timeouts -- legitimate?                       |
|------+----+------------------------------------------------------------|
|896766|Fix |If one URI connect-fails, hold queue, too                   |
|------+----+------------------------------------------------------------|
|908719|Fix |Fetching simple URLs fails with S_CONNECT_FAILED (-2) error |
|------+----+------------------------------------------------------------|
|809567|Fix |seeds held back/poor breadth first?                         |
|------+----+------------------------------------------------------------|
|831480|Fix |Parsing links found between escaped quotes in JavaScript    |
|------+----+------------------------------------------------------------|
|895303|Fix |Does not extract applet URI correctky                       |
|------+----+------------------------------------------------------------|
|791481|Fix |links to likely-embed types should be treated as embeds     |
|------+----+------------------------------------------------------------|
|900826|Fix |Frontier.next() forceFetches will cause assertion error     |
|------+----+------------------------------------------------------------|
|877873|Fix |Flash link extractor causes OutOfMemory exceptions.         |
|------+----+------------------------------------------------------------|
|899976|Fix |Should be possible to resume from                           |
|------+----+------------------------------------------------------------|
|896878|Fix |Heritrix ignores charset                                    |
|------+----+------------------------------------------------------------|
|910210|Fix |Max # of arcs not being respected.                          |
|------+----+------------------------------------------------------------|
|904723|Fix |New profile should ensure unique name                       |
|------+----+------------------------------------------------------------|
|902940|Fix |When changing scope common scope settings are lost          |
|------+----+------------------------------------------------------------|
|903910|Fix |ssl doesn't work                                            |
|------+----+------------------------------------------------------------|
|903084|Fix |Allow that people use tcsh/csh not just bash                |
|------+----+------------------------------------------------------------|
|896788|Fix |https SSLHandshakeException: unknown certificate            |
|------+----+------------------------------------------------------------|
|901397|Fix |Cannot override settings that isn't set in globals          |
|------+----+------------------------------------------------------------|
|892253|Fix |'Waiting for pause' even after all threads done             |
|------+----+------------------------------------------------------------|
|896800|Fix |filter 'invert', filter names need work                     |
|------+----+------------------------------------------------------------|
|896835|Fix |max-link-hops (etc.) ignored unless                         |
|------+----+------------------------------------------------------------|
|872069|Fix |order.xml absolute paths                                    |
|------+----+------------------------------------------------------------|
|892105|Fix |Cannot set TransclusionFilter attributes                    |
|------+----+------------------------------------------------------------|
|874057|Fix |Link puts garbage into arc file: http://www.msn.com/robots.t|
+------------------------------------------------------------------------+

6.8. Release 0.6.0 - 2004-03-25

Abstract

Release made in advance of radical frontier changes. Added bandwidth throttle,
operator 'diary', settable robots expiration, crawler cookie pre-population,
and changing of certain options mid-crawl. Many UI improvements including UI
display of critical exceptions, UI desccription of job-order options, and
improved reporting. Optimizations. Updated httpclient lib to 2.0 release and
jmx libs to 1.2.1. Lots of bug fixes.

6.8.1. Changes

Table 8. Changes

+-----------------------------------------------------------------------------+
|  ID  |Type|                             Summary                             |
|------+----+-----------------------------------------------------------------|
|861861|Add |861861 Redirects(/refreshes) from seeds should == new seeds      |
|------+----+-----------------------------------------------------------------|
|899223|Add |899223 Special seed-success report should be offered             |
|------+----+-----------------------------------------------------------------|
|891986|Add |891986 Bandwidth throttle function, setting.                     |
|------+----+-----------------------------------------------------------------|
|877275|Add |877275 integrated operator 'diary' needed                        |
|------+----+-----------------------------------------------------------------|
|891983|Add |891983 IP, Robots expirations should be settable                 |
|------+----+-----------------------------------------------------------------|
|910152|Add |910152 Recovery of old jobs on WUI (re)start                     |
|------+----+-----------------------------------------------------------------|
|781171|Add |781171 parsing css                                               |
|------+----+-----------------------------------------------------------------|
|912986|Add |912986 log views should give an idea of file size (where         |
|      |    |possible)                                                        |
|------+----+-----------------------------------------------------------------|
|912989|Add |912989 Alerts should have 'select all' button...                 |
|------+----+-----------------------------------------------------------------|
|856593|Add |856593 [load][save][turn on/off] cookies                         |
|------+----+-----------------------------------------------------------------|
|912201|Add |912201 Add levels to alerts                                      |
|------+----+-----------------------------------------------------------------|
|896665|Add |896665 Split processor chains.                                   |
|------+----+-----------------------------------------------------------------|
|896754|Add |896754 Show total of disregards                                  |
|------+----+-----------------------------------------------------------------|
|903095|Add |903095 Show increments of megabytes in ui                        |
|------+----+-----------------------------------------------------------------|
|896794|Add |896794 serious errors (eg outofmemory) should show up in UI      |
|------+----+-----------------------------------------------------------------|
|900520|Add |900520 Short description of ComplexTypes in user interface.      |
|------+----+-----------------------------------------------------------------|
|899982|Add |899982 Should be possible to alter filters while crawling.       |
|------+----+-----------------------------------------------------------------|
|896672|Add |896672 Display progress (doc/sec) with more precision            |
|------+----+-----------------------------------------------------------------|
|896677|Add |896677 Highlight the success or failures of each seed            |
|------+----+-----------------------------------------------------------------|
|896760|Add |896760 Prominent notification when seeds have problems           |
|------+----+-----------------------------------------------------------------|
|896801|Add |896801 java regexps (in log view) need help text                 |
|------+----+-----------------------------------------------------------------|
|896778|Add |896778 Log viewing enhancements:                                 |
|------+----+-----------------------------------------------------------------|
|896795|Add |896795 frontier, thread report improvements                      |
|------+----+-----------------------------------------------------------------|
|876516|Add |876516 default launch should nohup, save stdout/stderr           |
|------+----+-----------------------------------------------------------------|
|896763|Fix |127.0.0.1 in job report                                          |
|------+----+-----------------------------------------------------------------|
|896767|Fix |Frontier retry-delay should include units (eg -seconds)          |
|------+----+-----------------------------------------------------------------|
|898994|Fix |Revisiting admin URIs if not logged in should prompt login       |
|------+----+-----------------------------------------------------------------|
|899019|Fix |Deadlock in Andy's 2nd Crawl                                     |
|------+----+-----------------------------------------------------------------|
|767225|Fix |Better bad-config handling                                       |
|------+----+-----------------------------------------------------------------|
|815357|Fix |mysterious pause facing network (DNS) problem                    |
|------+----+-----------------------------------------------------------------|
|896747|Fix |ExtractorJS's report overstates it's discovered URIs             |
|------+----+-----------------------------------------------------------------|
|896667|Fix |Web UI does not display correctly in IE                          |
|------+----+-----------------------------------------------------------------|
|896780|Fix |console clarity/safety                                           |
|------+----+-----------------------------------------------------------------|
|896655|Fix |Does not respect per settings added after crawl was started.     |
|------+----+-----------------------------------------------------------------|
|856555|Fix |'empty' records in compressed arc files                          |
+-----------------------------------------------------------------------------+

6.9. Release 0.4.1 - 2004-06-04

Abstract

Memory retention fix.

6.9.1. Changes

Table 9. Changes

+---------------------------------------------+
|ID|Type|               Summary               |
|--+----+-------------------------------------|
|- |Fix |895955 URIRegExpFilter retains memory|
+---------------------------------------------+

6.10. Release 0.4.0 - 2004-02-10

Abstract

Release made for heritrix workshop, San Francisco, 02/2004. New MBEAN-based
configuration, extensive UI revamp, first unit tests and integration selftest
framework added, pooling of ARCWriters, new cmd-line start scripts, httpclient
lib update (2.0RC3) and bugfixes.

6.10.1. Changes

Table 10. Feature

+-----------------------------------------------------------------------------+
|ID|Type|                               Summary                               |
|--+----+---------------------------------------------------------------------|
|- |Add |New MBEAN-based configuration system. Reads and writes XML to        |
|  |    |validate against heritrix_settings.xsd.                              |
|--+----+---------------------------------------------------------------------|
|- |Add |UI extensively revamped. Exploits new configuration system.          |
|--+----+---------------------------------------------------------------------|
|- |Add |60-odd unit tests added.                                             |
|--+----+---------------------------------------------------------------------|
|- |Add |Integration selftest framework.                                      |
|--+----+---------------------------------------------------------------------|
|- |Add |Added pooling of ARCWriters.                                         |
|--+----+---------------------------------------------------------------------|
|- |Add |Start script backgrounds heritrix and redirects stdout/stderr to     |
|--+----+---------------------------------------------------------------------|
|- |Add |876516 Default launch should nohup, save stdout/stderr               |
|--+----+---------------------------------------------------------------------|
|- |Add |Web UI accesses are loggged to heritrix_out.log also.                |
|--+----+---------------------------------------------------------------------|
|- |Add |Updated httpclient to version 2.0RC3.                                |
|--+----+---------------------------------------------------------------------|
|- |Add |763517 IAGZIPOutputStream NPE under IBM JVM                          |
|--+----+---------------------------------------------------------------------|
|- |Add |809018 Cleaner versioned testing build needed                        |
|--+----+---------------------------------------------------------------------|
|- |Add |872729 Cmd-line options for setting web ui username/password         |
|--+----+---------------------------------------------------------------------|
|- |Add |863317 Universal single-pass extractor                               |
+-----------------------------------------------------------------------------+

6.11. Release 0.2.0 - 2004-01-05

Abstract

First 'official' release.

6.12. Release 0.1.0 - 2003-12-31

Abstract

Initial Mavenized development version number (CVS/internal only). Added
everything to new project layout.


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
