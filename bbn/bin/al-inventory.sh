#!/bin/sh

# <copyright>
#  Copyright 2001-2003 BBNT Solutions, LLC
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
