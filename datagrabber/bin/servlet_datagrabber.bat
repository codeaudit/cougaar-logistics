@echo OFF

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


REM This script starts the datagrabber.  

REM The datagrabber only needs datagrabber.jar, mm-mysql-2.jar, and the xerces jars

REM set CONFIG_FILE=ServletGrabberConfig.xml
set CONFIG_FILE=ServletConfig.xml

REM set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\datagrabber.jar
set LIBPATHS=d:/datagrabber/src
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\xercesImpl.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\xml-apis.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\sys\mm-mysql-2.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\util.jar

REM PROPERTIES -

echo "java -Duser.timezone=GMT -classpath %LIBPATHS% org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%"

java -Duser.timezone=GMT -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -classpath %LIBPATHS% org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%


