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

REM The datagrabber only needs datagrabber.jar, mm-mysql-2.jar, core.jar, and the xerces jar

set CONFIG_FILE=ServletGrabberConfig.xml

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar

REM Modify the argument org.cougaar.ui.userAuthClass to use a
REM UserAuthenticator other than the NAI class org.cougaar.core.security.userauth.UserAuthenticatorImpl

REM PROPERTIES -

echo "java -server -Duser.timezone=GMT -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -classpath %LIBPATHS% org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%

java -server -Duser.timezone=GMT -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -classpath %LIBPATHS% org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.grabber.DataGrabber %CONFIG_FILE%


