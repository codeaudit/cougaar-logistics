/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/ExceptionTools.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

 /*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionTools
{
    public static String toString(String procString, Exception e)
    {
	ByteArrayOutputStream b = new ByteArrayOutputStream();
	PrintStream p = new PrintStream(b);
	e.printStackTrace(p);
	return procString + ": Uncaught exception: " + e + "\n"
	    + procString + ": Dump of " + Thread.currentThread().getName() + " follows\n"
	    + procString + ": " + b.toString() + "\n";
    }

    public static String toString(String procString, Error e)
    {
	ByteArrayOutputStream b = new ByteArrayOutputStream();
	PrintStream p = new PrintStream(b);
	e.printStackTrace(p);
	return procString + ": SERIOUS ERROR, restart likely necessary: " + e + "\n"
	    + procString + ": Dump of " + Thread.currentThread().getName() + " follows\n"
	    + procString + ": " + b.toString() + "\n";
    }

    public static String stackToString(String procString)
    {
	try {
	    throw new Exception();
	}
	catch ( Exception e ) {
	    ByteArrayOutputStream b = new ByteArrayOutputStream();
	    PrintStream p = new PrintStream(b);
	    e.printStackTrace(p);
	    String fullTrace = b.toString();
	    String trace = fullTrace.substring(fullTrace.indexOf("\n", fullTrace.indexOf("\n")) + 1,
					       fullTrace.length());
	    return procString + ": Dump of " + Thread.currentThread().getName() + " follows\n"
		+ procString + ": " + trace + "\n";
	}
    }
}
