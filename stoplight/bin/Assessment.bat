
REM "<copyright>"
REM " Copyright 2001-2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

echo off

echo *******************************************************************
echo * To use, set COUGAAR_INSTALL_PATH.  Set DEVELOPMENT_PATH only if *
echo * you wish to override classes in the cougaar distribution.       *
echo *******************************************************************
rem set COUGAAR_INSTALL_PATH=c:\alpine\aggregationAgent\cougaar
echo COUGAAR_INSTALL_PATH is set to %COUGAAR_INSTALL_PATH%

set DEVELOPMENT_PATH=..\classes
set DEVELOPMENT_PATH=%DEVELOPMENT_PATH%;C:\alpine\aggregationAgent\aggagent\classes
rem set DEVELOPMENT_PATH=%DEVELOPMENT_PATH%;C:\alpine\cougaar\uiframework\classes
set LIB_PATH=%COUGAAR_INSTALL_PATH%\lib
set SYS_PATH=%COUGAAR_INSTALL_PATH%\sys
set DATA_PATH=..\doc\assessment

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar

set DEFAULTORG="1-35-ARBN"
set NAMESERVER="http://localhost:8800"
set AGENTNAME="AGG-Agent"
set PSP="aggregator"
set KEEPALIVEPSP="aggregatorkeepalive"
rem set TIMEOUT=30000

rem LAUNCHER
java -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Dcougaar.aggagent.NAMESERVER=%NAMESERVER% -Dcougaar.aggagent.AGENTNAME=%AGENTNAME% -Dcougaar.aggagent.PSP=%PSP% -Dcougaar.aggagent.KEEPALIVEPSP=%KEEPALIVEPSP% -DDEFAULTORG=%DEFAULTORG% -DDATAPATH=%DATA_PATH% -DTIMEOUT=%TIMEOUT% -classpath %LIBPATHS% org.cougaar.bootstrap.Bootstrapper org.cougaar.logistics.ui.stoplight.client.BJAssessmentLauncher

rem DESKTOP
rem java -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Dcougaar.aggagent.NAMESERVER=%NAMESERVER% -Dcougaar.aggagent.AGENTNAME=%AGENTNAME% -Dcougaar.aggagent.PSP=%PSP% -Dcougaar.aggagent.KEEPALIVEPSP=%KEEPALIVEPSP% -DDEFAULTORG=%DEFAULTORG% -DDATAPATH=%DATA_PATH% -classpath %CP% org.cougaar.bootstrap.Bootstrapper org.cougaar.logistics.ui.stoplight.client.BJAssessmentDesktop
