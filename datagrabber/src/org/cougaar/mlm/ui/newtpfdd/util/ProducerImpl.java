/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/ProducerImpl.java,v 1.2 2002-08-08 19:23:15 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

/**
 * Straightforward implementation of AscentProducer interface.
 */


package org.cougaar.mlm.ui.newtpfdd.util;


import java.util.Vector;
import java.util.Iterator;



public class ProducerImpl implements Producer
{
    private boolean suspended;
    protected Vector consumers;
    protected String name;

    public ProducerImpl(String name)
    {
      this.name = name;
      suspended = false;
      consumers = new Vector();
    }

    public boolean isSuspended()
    {
	return suspended;
    }

    public void setSuspended(boolean suspended)
    {
	this.suspended = suspended;
	// Debug.out("SAP:sS set suspended state of " + this + " to " + this.suspended);
    }

    public String getName()
    {
	return name;
    }

    public void addNotify(Object element)
    {
	Object elements[] = { element };
	addNotify(elements);
    }

    public void deleteNotify(Object element)
    {
	Object elements[] = { element };
	deleteNotify(elements);
    }

    public void changeNotify(Object element)
    {
	Object elements[] = { element };
	changeNotify(elements);
    }

    public void addNotify(Object[] elements)
    {
	if ( suspended || consumers.size() == 0 )
	    return;
	for ( int i = 0; i < elements.length; i++ ) {
            if ( elements[i] == null ) {
		OutputHandler.out("SAP:aN Error: null object at index " + i + "!");
		continue;
	    }
	    for ( Iterator e = consumers.iterator(); e.hasNext(); )
		((Consumer)(e.next())).fireAddition(elements[i]);
	}
    }

    public void deleteNotify(Object[] elements)
    {
	if ( suspended || consumers.size() == 0 )
	    return;
	for ( int i = 0; i < elements.length; i++ ) {
            if ( elements[i] == null ) {
		OutputHandler.out("SAP:dN Error: null object at index " + i + "!");
		continue;
	    }
	    for ( Iterator e = consumers.iterator(); e.hasNext(); )
		((Consumer)(e.next())).fireDeletion(elements[i]);
	}
    }

    public void changeNotify(Object[] elements)
    {
	if ( suspended || consumers.size() == 0 )
	    return;
	for ( int i = 0; i < elements.length; i++ ) {
            if ( elements[i] == null ) {
		OutputHandler.out("SAP:cN Error: null object at index " + i + "!");
		continue;
	    }
	    for ( Iterator e = consumers.iterator(); e.hasNext(); )
		((Consumer)(e.next())).fireChange(elements[i]);
	}
    }

    public void addConsumer(Consumer consumer)
    {
	// Debug.out("SAP:aC " + consumer);
	if ( consumers.contains(consumer) ) {
	    Debug.out("SAP:aC Note: attempt to doubly add consumer " + consumer);
	} else {
	    consumers.add(0, consumer);
    }
    }

    public void deleteConsumer(Consumer consumer)
    {
	Debug.out("SAP:dC " + consumer);
	if ( consumers.contains(consumer) ) {
	    consumers.remove(consumer);
	} else {
	    OutputHandler.out("SAP:dC Error: attempt to remove non-existent consumer " + consumer);
    }
    }

    public String toString()
    {
        return super.toString() + " " + name;
    }

    public void firingComplete()
    {
	for ( Iterator i = consumers.iterator(); i.hasNext(); )
	    ((Consumer)(i.next())).firingComplete();
    }
}
