@echo OFF

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


REM This script starts the datagrabber.  

REM The datagrabber only needs datagrabber.jar, mm-mysql-2.jar, and the xerces jar

REM set CONFIG_FILE=ServletGrabberConfig.xml
set CONFIG_FILE=ServletConfig.xml

REM set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\datagrabber.jar
set LIBPATHS=d:/datagrabber/src
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\xerces.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\mm-mysql-2.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar

REM PROPERTIES -

echo "java -Duser.timezone=GMT -classpath %LIBPATHS% org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%"

java -Duser.timezone=GMT -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -classpath %LIBPATHS% org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%


