@echo off

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

REM This is a bat script

REM This script starts the TPFDDShell. 

REM The only decision the user needs to make is whether they want to override the database
REM settings in cougaar.rc file or want to run without the cougaar.rc file.  In either case,
REM just switch the commented java lines at the bottom of this file.  Then the DB_PROPS and DB_DRIVER
REM variables will select the database.

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar


set MYMEMORY=-Xmx200m -Xms64m -Xmaxf0.9 -Xminf0.1

REM PROPERTIES -
REM  1) hostPrompt - popup host prompt dialog or not
REM  2) defaultHostName - what host to use if no dialog, 
REM     or what default value to put in dialog 
REM  3) GMT - so times are consistent with COUGAAR society times
REM  4) cdayDate - the cday of the demo.  Shows up in gantt charts.
REM  5) useReadyAtForOrigin - uses ready at date for start of origin 
REM     lozenge in TPFDD display
REM  6) warningRunsOK - when true, will show runs that had condition 
REM     warning as well as OK

set DB_PROPS=-Dorg.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.defaultHostName=localhost
set DB_PROPS=%DB_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.database=grabber
set DB_PROPS=%DB_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbUser=root
set DB_PROPS=%DB_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbPassword=""

set TPFDD_PROPS=-Dorg.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.hostPrompt=false
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.warningRunsOK=true
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.useReadyAtForOrigin=false
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.showRunMenu=true
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.TPFDDStringx=Schedule
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.logPlanStringx=OrganizationalRollupView
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.appTitlex=DeploymentTransportationScheduleViewer
set TPFDD_PROPS=%TPFDD_PROPS% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun.useDerivedTables=true
set TPFDD_PROPS=%TPFDD_PROPS% -DcdayDate=08/15/2005
set TPFDD_PROPS=%TPFDD_PROPS% -DoriginLozengeSize=150
set TPFDD_PROPS=%TPFDD_PROPS% -DdestinationLozengeSize=150
set TPFDD_PROPS=%TPFDD_PROPS% -DRouteSegment.statusFontSize=+2

set DB_DRIVER=-Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverName=org.gjt.mm.mysql.Driver
set DB_DRIVER=%DB_DRIVER% -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverType=MySQL

set INSTALL_PATH=-Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%

set TPFDD_DEBUG=
REM set TPFDD_DEBUG=-Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.producer.DatabaseConfig.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitPanel.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitTreeModel.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterDialog.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskModel.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewJTreeTable.debug=false
REM set TPFDD_DEBUG=%TPFDD_DEBUG% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewLogPlanView.debug=false

set TPFDD_TIMING=
REM set TPFDD_TIMING=-Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.showSqlTime=false
REM set TPFDD_TIMING=%TPFDD_TIMING% -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.showSqlTime=false

REM echo %DB_PROPS% %TPFDD_PROPS% %DB_DRIVER% %TPFDD_DEBUG% %TPFDD_TIMING%

echo "java -classpath %LIBPATHS% -Duser.timezone=GMT %MYMEMORY% %TPFDD_PROPS% %DB_DRIVER% %TPFDD_DEBUG% %TPFDD_TIMING% org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell"

REM Use this line if you want to override the cougaar.rc settings or you don't have or want to use a cougaar.rc file.
REM java -classpath %LIBPATHS% -Duser.timezone=GMT %MYMEMORY% %TPFDD_PROPS% %INSTALL_PATH% %DB_DRIVER% %DB_PROPS% %TPFDD_DEBUG% %TPFDD_TIMING% org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell

REM Use this line if you want to use the cougaar.rc settings for the database 
java -classpath %LIBPATHS% -Duser.timezone=GMT %MYMEMORY% %TPFDD_PROPS% %INSTALL_PATH% %TPFDD_DEBUG% %TPFDD_TIMING% org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell


