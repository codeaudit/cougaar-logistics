/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

          Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;


public class LozengePanel extends Container
    implements VirtualX, MessageListener
{
    private ArrayList visibleRows;

    public static final int ALL = 999;
    private static int paintLayerNow = ALL;
    private final int totalLayers = 2;
    private boolean[] layerActive = new boolean[totalLayers];
    private Runnable redrawRunnable;

    public LozengePanel(ArrayList cache)
    {
	setBackground(Color.black);
	visibleRows = cache;
	layerActive[0] = false; // don't show decorators by default
	layerActive[1] = true; // but do show lozenges
	redrawRunnable = new Runnable()
	    {
		public void run()
		{
		    invalidate();
		    repaint();
		}
	    };
    }


    public static int getPaintLayerNow()
    {
	return paintLayerNow;
    }

    public void setLayerActive(int layer, boolean isActive)
    {
	layerActive[layer] = isActive;
	SwingQueue.invokeLater(redrawRunnable);
    }

    //////////////////////////////////////////////////
    // VirtualX
    private long myVirtualXLocation;
    private long myVirtualXSize;
	
    public long getVirtualXLocation() { return myVirtualXLocation; }

    public long getVirtualXSize() { return myVirtualXSize; }

    // VirtualX
    //////////////////////////////////////////////////


    //////////////////////////////////////////////////
    // MessageListener
    private ArrayList myMessageListeners = new ArrayList();

    public void addMessageListener( MessageListener l ) {
	myMessageListeners.add(l);
    }

    public void removeMessageListener( MessageListener l ) {
	myMessageListeners.add(l);
    }

    // MessageListener
    //////////////////////////////////////////////////

	
    //////////////////////////////////////////////////
    // Rendering
    /**
     * Lozenge rows are laid out in a vertical stack with
     * enough space between rows to let gridlines show through.
     * (This means there are 2 free pixel rows at both top and bottom
     * (a grid line and a space), and 3 free pixel rows between lozenge
     * rows (a grid line with a space above and below).
     */
    public void doLayout()
    {
	int numRows = visibleRows.size();

	if( numRows < 1 ) return;

	final int cumLozengeHeight = getSize().height - 2 * 2   // top and bottom margins
	    - 3 * (numRows - 1);  // interrow spaces
	final int indLozengeHeight = cumLozengeHeight / numRows;
	final int lozengeRowInterval = getSize().height / numRows;
	setFont(FontCache.get((lozengeRowInterval + 1) / 2));

	for( int r=0, y=2;  r<numRows;  r++, y+=lozengeRowInterval )
	    {
		LozengeRow lr = (LozengeRow) visibleRows.get(r);
		if( lr != null )
		    {
			lr.setVisible(true);
			lr.setLocation( 0, y );
			lr.setSize( getSize().width, indLozengeHeight );
			lr.doLayout();
		    }
	    }
	repaint();
    }

    public void paint(Graphics g)
    {
	for ( int r = 0; r < visibleRows.size(); r++ ) {
	    LozengeRow lr = (LozengeRow)visibleRows.get(r);
	    if ( lr != null && !(lr.isValid()) )
		lr.doLayout();
	}

	for ( int i = totalLayers - 1; i >= 0; i-- )
	    if ( layerActive[i] ) {
		paintLayerNow = i;
		super.paint(g);
	    }
	paintLayerNow = ALL;
    }

    public void setMessage( String msg )
    {
	for (ListIterator li = myMessageListeners.listIterator(); li.hasNext(); )
	    ((MessageListener)(li.next())).setMessage(msg);
    }

    public void setVirtualXLocation( long newLocation )
    {
	myVirtualXLocation = newLocation;
	for (ListIterator li = visibleRows.listIterator();
	     li != null && li.hasNext(); )
	    {
		VirtualX vx = (VirtualX)li.next();
		if( vx != null ) vx.setVirtualXLocation( newLocation );
	    }
    }

    public void setVirtualXSize( long newSize )
    {
	myVirtualXSize = newSize;
	for (ListIterator li = visibleRows.listIterator();
	     li != null && li.hasNext(); )
	    {
		VirtualX vx = (VirtualX)li.next();
		if( vx != null ) vx.setVirtualXSize( newSize );
	    }
    }

    public void setAllSelected(boolean yesno) {
	// Loop over visible rows and set all lozenges
	for (ListIterator i1 = visibleRows.listIterator(); i1.hasNext(); ) {
	    LozengeRow lr = (LozengeRow) i1.next();
	    if (lr != null)
		for (ListIterator i2 = lr.getLozenges().listIterator(); i2.hasNext(); ) {
		    Lozenge loz = (Lozenge) i2.next();
		    loz.setSelected(yesno);
		}
	}
    }

    public void setAllSelectedExcept(boolean yesno, Object id) {
	// Loop over visible rows and set all lozenges
	for (ListIterator i1 = visibleRows.listIterator(); i1.hasNext(); ) {
	    LozengeRow lr = (LozengeRow) i1.next();
	    if (lr != null)
		for (ListIterator i2 = lr.getLozenges().listIterator(); i2.hasNext(); ) {
		    Lozenge loz = (Lozenge) i2.next();
		    if (!loz.getID().equals(id))
			loz.setSelected(yesno);
		}
	}
    }

    public void setRowSelected(boolean yesno, int rowNum) {
	LozengeRow lr = (LozengeRow) visibleRows.get(rowNum);
	if (lr != null)
	    for (ListIterator i2 = lr.getLozenges().listIterator(); i2.hasNext(); ) {
		Lozenge loz = (Lozenge) i2.next();
		loz.setSelected(yesno);
	    }
    }

    // Rendering	
    //////////////////////////////////////////////////

    //////////////////////////////////
    // Gantt Chart interface functions

    public void scrollUp(LozengeRow[] newRows) {
	int numScrolledOff = newRows.length;
	int i;

	for (i = 0; i < numScrolledOff; i++) {
	    LozengeRow lr = (LozengeRow)visibleRows.get(0);
	    if (lr != null) remove(lr);
	    visibleRows.remove(0);
	}

	for (i = 0; i < numScrolledOff; i++) {
	    if (newRows[i] != null) {
		newRows[i].setVirtualXLocation(getVirtualXLocation());
		newRows[i].setVirtualXSize(getVirtualXSize());
		add(newRows[i], 0);
	    }
	    visibleRows.add(newRows[i]);
	}

	invalidate();
    }

    public void scrollDown(LozengeRow[] newRows) {
	int numScrolledOff = newRows.length;
	int i;

	int stopLoop = visibleRows.size() - 1 - numScrolledOff;
	for (i = visibleRows.size() - 1; i > stopLoop; i--) {
	    LozengeRow lr = (LozengeRow)visibleRows.get(i);
	    if (lr != null) remove(lr);
	    visibleRows.remove(i);
	}

	for (i = numScrolledOff - 1; i >= 0; i--) {
	    if (newRows[i] != null) {
		add(newRows[i], 0);
		newRows[i].setVirtualXLocation(getVirtualXLocation());
		newRows[i].setVirtualXSize(getVirtualXSize());
	    }
	    visibleRows.add(0, newRows[i]);
	}

	invalidate();
    }

    public void insert(int rowNum, LozengeRow lr) {
	LozengeRow pushedOff = (LozengeRow)visibleRows.get(visibleRows.size() - 1);
	if (pushedOff != null)
	    remove(pushedOff);
	visibleRows.remove(visibleRows.size() - 1);

	if (lr != null) {
	    add(lr, 0);
	    lr.setVirtualXLocation(getVirtualXLocation());
	    lr.setVirtualXSize(getVirtualXSize());
	}
	visibleRows.add(rowNum, lr);

	invalidate();
    }

    public void delete(int rowNum, LozengeRow lr, boolean top) {
	LozengeRow deleted = (LozengeRow)visibleRows.get(rowNum);
	if (deleted != null) remove(deleted);
	visibleRows.remove(rowNum);

	if (top) {
	    visibleRows.add(0, lr);
	}
	else {
	    visibleRows.add(lr);
	}

	if (lr != null) {
	    add(lr, 0);
	    lr.setVirtualXLocation(getVirtualXLocation());
	    lr.setVirtualXSize(getVirtualXSize());
	}

	invalidate();
    }

    public void replace(int rowNum, LozengeRow lr)
    {
	LozengeRow replaced = (LozengeRow)visibleRows.get(rowNum);
	if ( replaced != null )
	    remove(replaced);
	
	if (lr != null) {
	    add(lr, 0);
	    lr.setVirtualXLocation(getVirtualXLocation());
	    lr.setVirtualXSize(getVirtualXSize());
	}
	visibleRows.set(rowNum, lr);
	
	invalidate();
    }
}
