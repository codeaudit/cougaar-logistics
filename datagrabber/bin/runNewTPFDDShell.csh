#!/bin/csh -f 

# This script runs the TPFDD Shell

# <copyright>
#  Copyright 2001 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>

# The only decision the user needs to make is whether they want to override the database
# settings in cougaar.rc file or want to run without the cougaar.rc file.  In either case,
# just switch the commented java lines at the bottom of this file.  Then the DB_PROPS and DB_DRIVER
# variables will select the database.

setenv LIBPATHS "${COUGAAR_INSTALL_PATH}/lib/bootstrap.jar"

setenv MYMEMORY "-Xmx200m -Xms64m -Xmaxf0.9 -Xminf0.1"

# PROPERTIES -
#  1) hostPrompt - popup host prompt dialog or not
#  2) defaultHostName - what host to use if no dialog, 
#     or what default value to put in dialog 
#  3) GMT - so times are consistent with COUGAAR society times
#  4) cdayDate - the cday of the demo.  Shows up in gantt charts.
#  5) useReadyAtForOrigin - uses ready at date for start of origin 
#     lozenge in TPFDD display
#  6) warningRunsOK - when true, will show runs that had condition 
#     warning as well as OK

setenv DB_PROPS "-Dorg.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.defaultHostName=mySQLHost"
setenv DB_PROPS "${DB_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.database=dbName"
setenv DB_PROPS "${DB_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbUser=username"
setenv DB_PROPS "${DB_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbPassword=password"

setenv TPFDD_PROPS "-Dorg.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.hostPrompt=false"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.warningRunsOK=true"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.useReadyAtForOrigin=false"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.showRunMenu=true"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.TPFDDStringx=Schedule"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.logPlanStringx=OrganizationalRollupView"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.appTitlex=DeploymentTransportationScheduleViewer"
setenv TPFDD_PROPS "${TPFDD_PROPS} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun.useDerivedTables=true"
setenv TPFDD_PROPS "${TPFDD_PROPS} -DcdayDate=08/15/2005"
setenv TPFDD_PROPS "${TPFDD_PROPS} -DoriginLozengeSize=150"
setenv TPFDD_PROPS "${TPFDD_PROPS} -DdestinationLozengeSize=150"
setenv TPFDD_PROPS "${TPFDD_PROPS} -DRouteSegment.statusFontSize=+2"

setenv DB_DRIVER "-Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverName=org.gjt.mm.mysql.Driver"
setenv DB_DRIVER "${DB_DRIVER} -Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverType=MySQL"

setenv TPFDD_DEBUG ""
#setenv TPFDD_DEBUG "-Dorg.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitPanel.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitTreeModel.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterDialog.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskModel.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewJTreeTable.debug=false"
#setenv TPFDD_DEBUG "${TPFDD_DEBUG} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.NewLogPlanView.debug=false"

setenv TPFDD_TIMING ""
#setenv TPFDD_TIMING "-Dorg.cougaar.mlm.ui.newtpfdd.gui.view.SqlQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.showSqlTime=false"
#setenv TPFDD_TIMING "${TPFDD_TIMING} -Dorg.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.showSqlTime=false"

setenv INSTALL_PATH "-Dorg.cougaar.install.path=${COUGAAR_INSTALL_PATH}"

echo ${DB_PROPS} ${TPFDD_PROPS} ${DB_DRIVER} ${TPFDD_DEBUG} ${TPFDD_TIMING}

#Use these lines if you want to override the cougaar.rc settings or you don't have or want to use a cougaar.rc file.
#java -classpath ${LIBPATHS} -Duser.timezone=GMT  ${MYMEMORY} \
#$ {INSTALL_PATH} {DB_PROPS} ${TPFDD_PROPS} ${DB_DRIVER} ${TPFDD_DEBUG} ${TPFDD_TIMING} \
#org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell

#Use these lines if you want to use the cougaar.rc settings for the database 
java -classpath ${LIBPATHS} -Duser.timezone=GMT ${MYMEMORY} \
${INSTALL_PATH} ${TPFDD_PROPS} ${TPFDD_DEBUG} ${TPFDD_TIMING} \
org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell



