/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Debug.java,v 1.3 2002-08-07 21:17:33 tom Exp $ */

 /*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.security.AccessControlException;


public class Debug extends OutputHandler
{
    private static boolean debug;
    private static Debug debugHandler = null;
    
    static
    {
	String doDebug = "false";
	try {
	    doDebug = System.getProperty("debug");
	}
	catch ( AccessControlException e ) {e.printStackTrace();
	}
	if ( doDebug != null && doDebug.equalsIgnoreCase("true") ) {
	    System.out.println("Debugging is on.");
	    debug = true;
	}
    }

    public Debug(Producer producer, boolean registerAsGlobal)
    {
	super(producer, registerAsGlobal);
    }

    public static void set(boolean state)
    {
	debug = state;
    }

    public static Debug getHandler()
    {
	return debugHandler;
    }

    public static void setHandler(Debug handler)
    {
	debugHandler = handler;
    }

    public static void out(String message)
    {
	if ( !debug || debugHandler == null )
	    return;
	debugHandler.putMessage("[D] " + message, true, true);
    }
}
