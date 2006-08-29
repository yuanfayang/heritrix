:: This script launches the heritrix crawler on windows.  While Heritrix
:: is unsupported on windows, see 2.1.1.3 in the User Manual
:: [http://crawler.archive.org/articles/user_manual.html], this script was
:: provided by Eric Jensen, with additions by Max Schöfmann, as a convenience
:: to the windows-afflicted.
::
:: It is a direct translation of the heritrix linux wrapper script -- and
:: because windows is not supported on Heritrix, it will likely lag the unix
:: start script.  It is also incomplete; the JMX setup needs finishing.
:: That said, it should be sufficent to get a windows user up and running
:: using Heritrix.
::
:: See also:
:: https://sourceforge.net/tracker/index.php?func=detail&aid=1514538&group_id=73833&atid=539102
::
:: Versions:
::
:: 2006-07-17  Original Version by Eric Jensen
::
:: 2006-08-04  Disclaimer added by Michael Stack
::
:: 2006-08-28  A few fixes by Max Schöfmann:
::             - command extensions and veriable expansion are automatically
::               enabled
::             - JMX configuration fixed (not the fancy "sed" stuff however)
::             - Try to set permissions of JMX password file if Heritrix
::               failes to start and JMX is enabled
::             - a few more small improvements
::             - comments changed from rem to :: and file renamed to .cmd 
::               (to make clear it won't work on Win 9x...)
::
::
::  Optional environment variables
:: 
::  JAVA_HOME        Point at a JDK install to use.
::  
::  HERITRIX_HOME    Pointer to your heritrix install.  If not present, we 
::                   make an educated guess based of position relative to this
::                   script.
:: 
::  HERITRIX_OUT     Pathname to the Heritrix log file written when run in
::                   daemon mode.
::                   Default setting is %HERITRIX_HOME%\heritrix_out.log
:: 
::  JAVA_OPTS        Java runtime options.  Default setting is '-Xmx256m'.
:: 
::  FOREGROUND       Set to any value -- e.g. 'true' -- if you want to run 
::                   heritrix in foreground (Used by build system when it runs
::                   selftest to see if completed successfully or not).
:: 
::  JMX_OPTS         Default is to startup the JVM JMX administration 
::                   on port 8849 if the JVM is SUN JVM 1.5.  This allows JMX
::                   administration of Heritrix.  If the JVM is other than the
::                   SUN JDK 1.5, the arguments are ignored. If you do not want
::                   to start the JVM JXM administration server on the SUN JDK
::                   1.5, set this variable to empty string.
:: 
::  JMX_PORT         Port you'd like the JVM JMX administration server to run
::                   on. Default is 8849.
:: 
::  JMX_OFF          Set to a non-empty string to disable JMX (and JMX setup of
::                   password file, etc.)
:: 
@echo off

:: Enabling command extensions and delayed variable expansion
if "%1"=="RUN" goto run
cmd /E:ON /V:ON /c %0 RUN %1 %2 %3 %4 %5 %6 %7 %8 %9
goto:eof

:run

:: Resolve links - %0 may be a softlink
set PRG=%0

set PRGDIR=%~p0

:: Read local heritrix properties if any.
if exist %HOMEPATH%\.heritrixrc call %HOMEPATH%\.heritrixrc

:: Set HERITRIX_HOME.
if not defined HERITRIX_HOME set HERITRIX_HOME=%PRGDIR%\..

:: Find JAVA_HOME.
if not defined JAVA_HOME goto no_java_home
:: then
::   JAVA=`which java`
::  if [ -z "%JAVA" ] 
::  then
::    echo "Cannot find JAVA. Please set JAVA_HOME or your PATH."
::    exit 1
::  fi
::  JAVA_BINDIR=`dirname %JAVA`
::  JAVA_HOME=%JAVA_BINDIR\..
:: fi

if not defined JAVACMD set JAVACMD="%JAVA_HOME%\bin\java" -Dje.disable.java.adler32=true
:: It may be defined in env - including flags!!
:: See '[ 1482761 ] BDB Adler32 gc-lock OOME risk' for why we include the
:: 'je.disable.java.adler32'.


:: Ignore previous classpath.  Build one that contains heritrix jar and content
:: of the lib directory into the variable CP.
for %%j in ("%HERITRIX_HOME%\lib\*.jar" "%HERITRIX_HOME%\*.jar") do set CP=!CP!;%%j

:: DONT cygwin path translation
:: if expr `uname` : 'CYGWIN*' > /dev/null; then
::    CP=`cygpath -p -w "%CP"`
::    HERITRIX_HOME=`cygpath -p -w "%HERITRIX_HOME"`
:: fi

:: Make sure of java opts.
if not defined JAVA_OPTS set JAVA_OPTS= -Xmx256m

:: Setting environment vars in nested IFs is error prone, thus using GOTOs
if not defined JMX_OFF goto configure_jmx
goto jmx_configured

:configure_jmx
if not defined JMX_PORT set JMX_PORT=8849
if not defined JMX_OPTS set JMX_OPTS=-Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.ssl=false "-Dcom.sun.management.jmxremote.password.file=%HERITRIX_HOME%\jmxremote.password"

:: DONT Copy into place a jmxremote password file that uses the heritrix password
:: interpolated (First need to find the current password if one supplied on
:: command-line, else use whats in heritrix.properties as default).
:: Need to make it so its only readable by user else jconsole won't use it.
:: JMX_PASSWORD=`echo "%@" |sed -n -e 's/.*--admin=[^:]*:\([^ ]*\).*/\1/p' -e 's/.*-a *[^:]*:\([^ ]*\).*/\1/p'`
:: if [ -z "%JMX_PASSWORD" ]
:: then
::  JMX_PASSWORD=`sed -n -e 's/heritrix.cmdline.admin[ ]*=[^:]*:\(.*\)/\1/p' \
::  %{HERITRIX_HOME}\conf\heritrix.properties`
:: fi
:: JMX_PWORD_FILE="%{HERITRIX_HOME}\jmxremote.password"
:: if [ -f "%{JMX_PWORD_FILE}" ]
::  then
:: rm -f "%{JMX_PWORD_FILE}"
:: fi
:: sed -e "s/@PASSWORD@/%{JMX_PASSWORD}/" \
::  "%{HERITRIX_HOME}\conf\jmxremote.password.template" > "%{JMX_PWORD_FILE}"
:: chmod 600 "%{JMX_PWORD_FILE}"

:jmx_configured

:: Main heritrix class.
if not defined CLASS_MAIN set CLASS_MAIN=org.archive.crawler.Heritrix

:: heritrix_dmesg.log contains startup output from the crawler main class. 
:: As soon as content appears in this log, this shell script prints the 
:: successful (or failed) startup content and moves off waiting on heritrix
:: startup. This technique is done so we can show on the console startup 
:: messages emitted by java subsequent to the redirect of stdout and stderr.
set startMessage=%HERITRIX_HOME%\heritrix_dmesg.log

:: Remove any file that may have been left over from previous starts.
if exist %startMessage% del %startmessage%

:: Run heritrix as daemon.  Redirect stdout and stderr to a file.
:: Print start message with date, java version, java opts, ulimit, and uname.
if not defined HERITRIX_OUT set HERITRIX_OUT=%HERITRIX_HOME%\heritrix_out.log

set stdouterrlog=%HERITRIX_OUT%
echo %DATE% %TIME% Starting heritrix >>%stdouterrlog%
:: uname -a >> %stdouterrlog%
%JAVACMD% %JAVA_OPTS% -version >>%stdouterrlog%  2>&1
echo JAVA_OPTS=%JAVA_OPTS% >>%stdouterrlog%
:: ulimit -a >> %stdouterrlog 2>&1

:: DONT If FOREGROUND is set, run heritrix in foreground.
:: if defined FOREGROUND
set CLASSPATH=%CP% 

:start_heritrix
%JAVACMD% "-Dheritrix.home=%HERITRIX_HOME%" -Djava.protocol.handler.pkgs=org.archive.net "-Dheritrix.out=%HERITRIX_OUT%" %JAVA_OPTS% %JMX_OPTS% %CLASS_MAIN% %2 %3 %4 %5 %6 %7 %8 %9
if not defined JMX_OFF. (if errorlevel 1 goto fix_jmx_permissions)
goto:eof

:fix_jmx_permissions
echo.
echo Heritrix failed to start properly.
echo This may be caused by a permissions problem with the JMX password file. 
set /P FIXIT=Do you want to try to fix it (Y/N)?
if /I "%FIXIT:~0,1%"=="n" goto:eof
cacls %HERITRIX_HOME%\jmxremote.password /P %USERNAME%:R
if errorlevel 1 goto fix_jmx_permission_failed
set /P RESTART=Restart Heritrix (Y/N)?
if /I "%RESTART:~0,1%"=="y" goto start_heritrix
goto:eof

:fix_jmx_permission_failed
echo Fixing permissions failed
goto:eof

:no_java_home
echo JAVA_HOME not defined
goto:eof
