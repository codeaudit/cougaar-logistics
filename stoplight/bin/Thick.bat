echo off

REM "<copyright>"
REM " Copyright 2001 BBNT Solutions, LLC"
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

REM This application is for testing the aggregation agent capabilities.

echo *******************************************************************
echo * To use, set COUGAAR_INSTALL_PATH.  Set DEVELOPMENT_PATH only if *
echo * you wish to override classes in the cougaar distribution.       *
echo *******************************************************************
rem set COUGAAR_INSTALL_PATH=c:\alpine\aggregationAgent\cougaar
echo COUGAAR_INSTALL_PATH is set to %COUGAAR_INSTALL_PATH%

rem set DEVELOPMENT_PATH=-Dorg.cougaar.class.path=..\classes
set DEVELOPMENT_PATH=
set DEVELOPMENT_PATH=-Dorg.cougaar.class.path=c:\alp\aggagent\classes
set LIB_PATH=%COUGAAR_INSTALL_PATH%\lib
set SYS_PATH=%COUGAAR_INSTALL_PATH%\sys

set CP=%DEVELOPMENT_PATH%
set CP=%CP%;%SYS_PATH%\xerces.jar
set CP=%CP%;%LIB_PATH%\core.jar
set CP=%CP%;%LIB_PATH%\bootstrap.jar
set CP=%CP%;%LIB_PATH%\aggagent.jar
set CP=%CP%;%LIB_PATH%\uiframework.jar
set CP=%CP%;%LIB_PATH%\stoplight.jar
set CP=%CP%;D:\UltraLog\logistics\stoplight\classes

set NAMESERVER=http://localhost:8800
set AGENTNAME=AGG-Agent
set PSP=aggregator
set KEEPALIVEPSP=aggregatorkeepalive

java %DEVELOPMENT_PATH% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Dorg.cougaar.system.path=%COUGAAR_INSTALL_PATH%/sys -classpath %CP% org.cougaar.bootstrap.Bootstrapper org.cougaar.logistics.ui.stoplight.client.aggregator.AggregatorPanel %NAMESERVER%/$%AGENTNAME%/

