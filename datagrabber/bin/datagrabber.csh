#!/bin/csh -f
# This script starts the datagrabber.  
#
# The datagrabber only needs tops.jar, mm-mysql-2.jar, and the xerces jar

# <copyright>
#  
#  Copyright 2001-2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
# </copyright>


setenv CONFIG_FILE ServletGrabberConfig.xml

setenv LIBPATHS "${COUGAAR_INSTALL_PATH}/lib/bootstrap.jar"

# Modify the argument org.cougaar.ui.userAuthClass to use a
# UserAuthenticator other than the NAI class org.cougaar.core.security.userauth.UserAuthenticatorImpl

# PROPERTIES -

echo java -server -Duser.timezone=GMT -Dorg.cougaar.install.path=${COUGAAR_INSTALL_PATH} -classpath ${LIBPATHS} org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.grabber.DataGrabber ${CONFIG_FILE}

java -server -Duser.timezone=GMT -Dorg.cougaar.install.path=${COUGAAR_INSTALL_PATH} -classpath ${LIBPATHS} org.cougaar.bootstrap.Bootstrapper org.cougaar.mlm.ui.grabber.DataGrabber ${CONFIG_FILE}

