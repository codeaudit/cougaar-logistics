/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/model/SimpleItemPoolModelProducer.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

/**
 *  Straightforward implementation of an ItemPoolModelProducer. Meant to be
 * extended or contained by classes that really get items to produce.
 */

package org.cougaar.mlm.ui.newtpfdd.gui.model;


import java.util.Iterator;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;


public class SimpleItemPoolModelProducer extends SimpleProducer
    implements ItemPoolModelProducer
{
    public SimpleItemPoolModelProducer()
    {
	super("Simple Item Pool Model Producer");
    }

    public SimpleItemPoolModelProducer(String name)
    {
	super(name);
    }

    public void addItemNotify(Object item)
    {
	addNotify(item);
    }

    public void deleteItemNotify(Object item)
    {
	deleteNotify(item);
    }

    public void deleteItemWithIndexNotify(Object item, int index)
    {
	if ( consumers.size() == 0 ) {
	    // Debug.out("SIPMP:dIWIN Note: empty consumer list!");
	    return;
	}
	if ( item == null ) {
	    OutputHandler.out("SIPMP:dIWIN Error: null object!");
	    return;
	}
	for ( Iterator e = consumers.iterator(); e.hasNext(); ) {
	    ((ItemPoolModelListener)(e.next())).fireItemWithIndexDeleted(item, index);
	}
    }

    public void changeItemNotify(Object item)
    {
	changeNotify(item);
    }

    public void addItemsNotify(Object[] items)
    {
	addNotify(items);
    }

    public void deleteItemsNotify(Object[] items)
    {
	deleteNotify(items);
    }

    public void changeItemsNotify(Object[] items)
    {
	changeNotify(items);
    }

    public void addConsumer(ItemPoolModelListener consumer)
    {
	super.addConsumer(consumer);
    }

    public void deleteConsumer(ItemPoolModelListener consumer)
    {
	super.deleteConsumer(consumer);
    }
}
