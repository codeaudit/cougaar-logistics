/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Attic/Callback.java,v 1.2 2002-08-07 19:46:27 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.util;


import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;


public class Callback
{
    private Object target;
    private Method method;

    public Callback(Object target, Method method)
    {
	this.target = target;
	this.method = method;
    }

    public Object getTarget()
    {
	return target;
    }

    public Method getMethod()
    {
	return method;
    }

    public void execute(Object arg)
    {
	try {
	    Object[] args = { arg };
	    getMethod().invoke(getTarget(), args);
	}
	catch ( Exception e ) {
	    OutputHandler.out(ExceptionTools.toString("Cb:execute", e));
	}
    }
}
