/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/MessageArea.java,v 1.2 2002-08-07 21:09:33 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.SwingUtilities;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;

import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;

import org.cougaar.mlm.ui.newtpfdd.gui.model.ItemPoolModelListener;


public class MessageArea extends JTextArea implements ItemPoolModelListener
{
    JScrollBar scrollbar;

    public MessageArea(String text, int rows, int columns)
    {
	super(text, rows, columns);
    }

    public void setScrollBar(JScrollBar scrollbar)
    {
	this.scrollbar = scrollbar;
    }

    public void fireAddition(Object o)
    {
	fireItemAdded(o);
    }

    public void fireDeletion(Object o)
    {
	fireItemAdded(o);
    }

    public void fireChange(Object o)
    {
	fireItemValueChanged(o);
    }

    public void fireItemAdded(Object o)
    {
	myAppend(o.toString());
    }

    public void fireItemValueChanged(Object o)
    {
	myAppend("MA:fID not implemented.\n");
    }

    public void fireItemDeleted(Object o)
    {
	myAppend("MA:fID: not implemented.\n");
    }

    public void fireItemWithIndexDeleted(Object item, int index)
    {
	myAppend("MA:fIWID: Error: UNIMPLEMENTED Should not ever be called.");
    }

    public void firingComplete()
    {
	invalidate();
	repaint();
    }

    // IMPORTANT: this could get called from any thread.
    public void myAppend(String str)
    {
	final String message = str;
	final MessageArea me = this;

	Runnable appendIt = new Runnable() {
		public void run()
		{
		    me.append(message);
		    firingComplete();
		}
	    };
	SwingQueue.invokeLater(appendIt);
    }
}
