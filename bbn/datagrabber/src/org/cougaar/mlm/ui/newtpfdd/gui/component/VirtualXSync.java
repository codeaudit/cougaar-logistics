/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/Attic/VirtualXSync.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

/**
  * VirtualXSync keeps multiple VirtualX objects synchronized to each other.
  * When VirtualXSync hears a change through its VirtualXAdjustmentListener interface,
  * it sets all of its slaves to have the same VirtualX values as the originator of change.
  * <p>
  * VirtualX parameters may also be set explicitly through the VirtualX interface.
  * VirtualXSync propagates the settings to all of its slaves.
  */

import java.awt.Component;
import java.util.ListIterator;
import java.util.ArrayList;

public class VirtualXSync
implements VirtualX, VirtualXAdjustmentListener
{
	private ArrayList mySlaves = new ArrayList();

	private long myVirtualXLocation;
	private long myVirtualXSize;
	
	public void addSlave( VirtualX vx ) { mySlaves.add(vx); }
	public long getVirtualXLocation() { return myVirtualXLocation; }
	public long getVirtualXSize() { return myVirtualXSize; }
	public void removeSlave( VirtualX vx ) { mySlaves.remove(mySlaves.indexOf(vx)); }
	public void setVirtualXLocation( long newLocation )
	{
		myVirtualXLocation = newLocation;

		for( ListIterator e = mySlaves.listIterator();  e != null && e.hasNext(); )
		{
			Object o = e.next();
			((VirtualX)o).setVirtualXLocation( newLocation );
			if( o instanceof Component ) ((Component)o).repaint();
		}
	}
	public void setVirtualXSize( long newSize )
	{
		myVirtualXSize = newSize;

		for( ListIterator e = mySlaves.listIterator();  e != null && e.hasNext(); )
		{
			Object o = e.next();
			((VirtualX)o).setVirtualXSize( newSize );
			if( o instanceof Component ) ((Component)o).repaint();
		}
	}
	public void VirtualXChanged(int type, VirtualX vx )
	{
		if( (type & VirtualXAdjustmentListener.LOCATION_CHANGED) != 0 )
			setVirtualXLocation( vx.getVirtualXLocation() );
		if( (type & VirtualXAdjustmentListener.SIZE_CHANGED) != 0 )
			setVirtualXSize( vx.getVirtualXSize() );
	}
}
