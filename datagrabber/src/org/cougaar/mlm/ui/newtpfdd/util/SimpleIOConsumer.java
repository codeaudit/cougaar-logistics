/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/SimpleIOConsumer.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


/**
   Simple consumer that fits in with Producer/Consumer model but
   used to just get stuff to the terminal from which java or appletviewer was 
   started -- in case GUI is not visible or is failing or needs to be duplicated
   for logging purposes.
*/

package org.cougaar.mlm.ui.newtpfdd.util;


public class SimpleIOConsumer implements Consumer
{
    public void fireAddition(Object item)
    {
	System.err.print(item);
    }

    public void fireDeletion(Object item)
    {
	System.err.print("SIOC:fD (?) " + item);
    }

    public void fireChange(Object item)
    {
	System.err.print("SIOC:fC (?) " + item);
    }

    public void firingComplete()
    {
	System.err.flush();
    }
}
