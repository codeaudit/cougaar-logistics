/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/producer/DateIDKey.java,v 1.2 2002-08-07 19:46:27 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.producer;


import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;


public class DateIDKey implements Comparable
{
    long dateSeconds;
    String ID;

    DateIDKey(long dateSeconds, String ID)
    {
	this.dateSeconds = dateSeconds / (60 * 10 * 1000); // consider 10 minutes apart to be equivalent
	this.ID = ID;
    }

    public int compareTo(Object object)
    {
	if ( !(object instanceof DateIDKey) )
	    OutputHandler.out("DK:cT Error: " + getClass() + " cannot compare to " + object.getClass());
	DateIDKey otherKey = (DateIDKey)object;
	if ( dateSeconds < otherKey.dateSeconds )
	    return -1;
	if ( dateSeconds == otherKey.dateSeconds )
	    return ID.compareTo(otherKey.ID);
	return 1;
    }

    public String toString()
    {
	return ID + ':' + dateSeconds;
    }
}
