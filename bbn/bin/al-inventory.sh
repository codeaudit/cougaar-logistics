#!/bin/sh

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


# Modify the argument org.cougaar.ui.userAuthClass to use a
# UserAuthenticator other than the NAI class org.cougaar.core.security.userauth.UserAuthenticatorImpl


if [ -z "$COUGAAR_INSTALL_PATH" ]; then
    if [ "$CIP" ]; then
	COUGAAR_INSTALL_PATH="$CIP";
    else
	s=`echo "$0" | sed -e 's,/bin/.*,,'`
	if [ x$s != x ] ; then
	    COUGAAR_INSTALL_PATH=$s
	else
	    echo "Error: Could not find COUGAAR_INSTALL_PATH!";
	    exit;
	fi
    fi
fi

exec /bin/sh $COUGAAR_INSTALL_PATH/bin/boost org.cougaar.logistics.ui.inventory.InventoryUIFrame $*
