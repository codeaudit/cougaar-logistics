
REM "<copyright>"
REM " "
REM " Copyright 2001-2004 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects"
REM " Agency (DARPA)."
REM ""
REM " You can redistribute this software and/or modify it under the"
REM " terms of the Cougaar Open Source License as published on the"
REM " Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"
REM " "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"
REM " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"
REM " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"
REM " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
REM " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
REM " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"
REM " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"
REM " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"
REM " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"
REM " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
REM " "
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
