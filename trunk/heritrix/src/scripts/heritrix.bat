rem This script launches the heritrix crawler on windows.  While Heritrix
rem is unsupported on windows, see 2.1.1.3 in the User Manual
rem [http://crawler.archive.org/articles/user_manual.html], this script was
rem provided by Eric Jensen as a convenience to the windows-afflicted.
rem
rem It is a direct translation of the heritrix linux wrapper script and
rem because windows is not supported on Heritrix, it will likely lag the unix
rem start script.  It is also incomplete; the JMX setup needs finishing.
rem That said, it should be sufficent to get a windows user up and running
rem using Heritrix.
rem
rem To run, JAVA_HOME and JMX_OFF environment variables must be set and the
rem script must be run using 'cmd /v'.  See
rem
rem See https://sourceforge.net/tracker/index.php?func=detail&aid=1514538&group_id=73833&atid=539102
rem for background and, if so inclined, contribute script improvements there.
rem
rem  Optional environment variables
rem 
rem  JAVA_HOME        Point at a JDK install to use.
rem  
rem  HERITRIX_HOME    Pointer to your heritrix install.  If not present, we 
rem                   make an educated guess based of position relative to this
rem                   script.
rem 
rem  HERITRIX_OUT     Pathname to the Heritrix log file written when run in
rem                   daemon mode.
rem                   Default setting is %HERITRIX_HOME%\heritrix_out.log
rem 
rem  JAVA_OPTS        Java runtime options.  Default setting is '-Xmx256m'.
rem 
rem  FOREGROUND       Set to any value -- e.g. 'true' -- if you want to run 
rem                   heritrix in foreground (Used by build system when it runs
rem                   selftest to see if completed successfully or not).
rem 
rem  JMX_OPTS         Default is to startup the JVM JMX administration 
rem                   on port 8849 if the JVM is SUN JVM 1.5.  This allows JMX
rem                   administration of Heritrix.  If the JVM is other than the
rem                   SUN JDK 1.5, the arguments are ignored. If you do not want
rem                   to start the JVM JXM administration server on the SUN JDK
rem                   1.5, set this variable to empty string.
rem 
rem  JMX_PORT         Port you'd like the JVM JMX administration server to run
rem                   on. Default is 8849.
rem 
rem  JMX_OFF          Set to a non-empty string to disable JMX (and JMX setup of
rem                   password file, etc.)
rem 

rem Resolve links - %0 may be a softlink
set PRG=%0

set PRGDIR=%~p0

rem Read local heritrix properties if any.
if exist %HOMEPATH%\.heritrixrc call %HOMEPATH%\.heritrixrc

rem Set HERITRIX_HOME.
if not defined HERITRIX_HOME set HERITRIX_HOME=%PRGDIR%\..

rem Find JAVA_HOME.
if not defined JAVA_HOME echo "Define JAVA_HOME"
rem then
rem   JAVA=`which java`
rem  if [ -z "%JAVA" ] 
rem  then
rem    echo "Cannot find JAVA. Please set JAVA_HOME or your PATH."
rem    exit 1
rem  fi
rem  JAVA_BINDIR=`dirname %JAVA`
rem  JAVA_HOME=%JAVA_BINDIR\..
rem fi

if not defined JAVACMD set JAVACMD="%JAVA_HOME%\bin\java" -Dje.disable.java.adler32=true
rem It may be defined in env - including flags!!
rem See '[ 1482761 ] BDB Adler32 gc-lock OOME risk' for why we include the
rem 'je.disable.java.adler32'.


rem Ignore previous classpath.  Build one that contains heritrix jar and content
rem of the lib directory into the variable CP.
for %%j in ("%HERITRIX_HOME%\lib\*.jar" "%HERITRIX_HOME%\*.jar") do set CP=!CP!;%%j

rem DONT cygwin path translation
rem if expr `uname` : 'CYGWIN*' > /dev/null; then
rem    CP=`cygpath -p -w "%CP"`
rem    HERITRIX_HOME=`cygpath -p -w "%HERITRIX_HOME"`
rem fi

rem Make sure of java opts.
if not defined JAVA_OPTS set JAVA_OPTS= -Xmx256m

if not defined JMX_OFF (
if not defined JMX_PORT set JMX_PORT=8849
if not defined JMX_OPTS set JMX_OPTS=-Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.ssl=false "-Dcom.sun.management.jmxremote.password.file=%HERITRIX_HOME%\jmxremote.password"
)

rem DONT Copy into place a jmxremote password file that uses the heritrix password
rem interpolated (First need to find the current password if one supplied on
rem command-line, else use whats in heritrix.properties as default).
rem Need to make it so its only readable by user else jconsole won't use it.
rem JMX_PASSWORD=`echo "%@" |sed -n -e 's/.*--admin=[^:]*:\([^ ]*\).*/\1/p' -e 's/.*-a *[^:]*:\([^ ]*\).*/\1/p'`
rem if [ -z "%JMX_PASSWORD" ]
rem then
rem  JMX_PASSWORD=`sed -n -e 's/heritrix.cmdline.admin[ ]*=[^:]*:\(.*\)/\1/p' \
rem  %{HERITRIX_HOME}\conf\heritrix.properties`
rem fi
rem JMX_PWORD_FILE="%{HERITRIX_HOME}\jmxremote.password"
rem if [ -f "%{JMX_PWORD_FILE}" ]
rem  then
rem rm -f "%{JMX_PWORD_FILE}"
rem fi
rem sed -e "s/@PASSWORD@/%{JMX_PASSWORD}/" \
rem  "%{HERITRIX_HOME}\conf\jmxremote.password.template" > "%{JMX_PWORD_FILE}"
rem chmod 600 "%{JMX_PWORD_FILE}"

rem Main heritrix class.
if not defined CLASS_MAIN set CLASS_MAIN=org.archive.crawler.Heritrix

rem heritrix_dmesg.log contains startup output from the crawler main class. 
rem As soon as content appears in this log, this shell script prints the 
rem successful (or failed) startup content and moves off waiting on heritrix
rem startup. This technique is done so we can show on the console startup 
rem messages emitted by java subsequent to the redirect of stdout and stderr.
set startMessage=%HERITRIX_HOME%\heritrix_dmesg.log

rem Remove any file that may have been left over from previous starts.
if exist %startMessage% del %startmessage%

rem Run heritrix as daemon.  Redirect stdout and stderr to a file.
rem Print start message with date, java version, java opts, ulimit, and uname.
if not defined HERITRIX_OUT set HERITRIX_OUT=%HERITRIX_HOME%\heritrix_out.log

set stdouterrlog=%HERITRIX_OUT
time /t >> %stdouterrlog%
echo " Starting heritrix" >> %stdouterrlog%
rem uname -a >> %stdouterrlog%
%JAVACMD% %JAVA_OPTS% -version >> %stdouterrlog% 
echo JAVA_OPTS=%JAVA_OPTS% >> %stdouterrlog%
rem ulimit -a >> %stdouterrlog 2>&1

rem DONT If FOREGROUND is set, run heritrix in foreground.
rem if defined FOREGROUND
set CLASSPATH=%CP% 
%JAVACMD% "-Dheritrix.home=%HERITRIX_HOME%" -Djava.protocol.handler.pkgs=org.archive.net "-Dheritrix.out=%HERITRIX_OUT%" %JAVA_OPTS% %JMX_OPTS% %CLASS_MAIN% %1 %2 %3 %4 %5
