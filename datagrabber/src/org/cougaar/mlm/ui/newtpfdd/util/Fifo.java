/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Fifo.java,v 1.2 2002-08-07 19:58:02 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.util;

import java.util.NoSuchElementException;
import java.util.LinkedList;


public class Fifo
{
    private LinkedList internal;

    public Fifo()
    {  
	internal = new LinkedList();
    }

    public void enqueue(Object o)
    {
	boolean noGood = false;

	synchronized(internal) {
	    if ( internal.indexOf(o) == -1 )
		internal.addFirst(o);
	    else
		noGood = true;
	}
    }

    public Object dequeue()
    {
	Object o = null;
	try {
	    synchronized(internal) {
		o = internal.removeLast();
	    }
	}
	catch ( NoSuchElementException e ) {e.printStackTrace();
	}
	return o;
    }
}
