/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/AdjustmentStateSync.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/AdjustmentStateSync.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/AdjustmentStateSync.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

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
  * AdjustmentStateSync keeps the state (min,max,value,visibleamount) of multiple
  * Adjustables in synchronization with each other.  When AdjustmentStateSync hears
  * a change through its AdjustmentListener interface, it propagates the change to 
  * all of its slaves.
  * <p>
  * Adjustment parameters may also be set explicitly through the Adjustable interface.
  */

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.util.ArrayList;
import java.util.ListIterator;

public class AdjustmentStateSync
implements AdjustmentListener, Adjustable
{
  private ArrayList mySlaves = new ArrayList();
  private Adjustable myCurrentMaster;

  private int myMinimum, myMaximum;
  private int myValue, myVisibleAmount;
  private int myBlockIncrement;

  public void addAdjustmentListener( AdjustmentListener l ) {}
  public void addSlave( Adjustable vx ) { mySlaves.add(vx); }
  public void adjustmentValueChanged( AdjustmentEvent e )
  {
    Adjustable a = e.getAdjustable();

    myCurrentMaster = a;

    if( a.getValue() != getValue() ) setValue( a.getValue() );
    if( a.getVisibleAmount() != getVisibleAmount() )
      setVisibleAmount( a.getVisibleAmount() );
    if( a.getBlockIncrement() != getBlockIncrement() )
      setBlockIncrement( a.getBlockIncrement() );
    if( a.getMinimum() != getMinimum() ) setMinimum( a.getMinimum() );
    if( a.getMaximum() != getMaximum() ) setMaximum( a.getMaximum() );

    repaint();

    myCurrentMaster = null;
  }
  // These aren't useful here, but are still 
  // required for the Adjustable interface
  public int getBlockIncrement() { return myBlockIncrement; }
  public int getMaximum() { return myMaximum; }
  public int getMinimum() { return myMinimum; }
  public int getOrientation() { return Adjustable.HORIZONTAL; }
  public int getUnitIncrement() { return 1; }
  public int getValue() { return myValue; }
  public int getVisibleAmount() { return myVisibleAmount; }
  /**
   * Copy values from Adjustable a
   */
  public void initialize( Adjustable a )
  {
    adjustmentValueChanged(new AdjustmentEvent(a, 0, 0, 0));
  }
  public void removeAdjustmentListener( AdjustmentListener l ) {}
  public void removeSlave( Adjustable vx ) {
    int index = mySlaves.indexOf(vx);
    mySlaves.remove(index);
  }
  public void repaint()
  {
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      {
	Object o = li.next();
	if( o instanceof Component  &&  o != myCurrentMaster )
	  ((Component)o).repaint();
      }
  }
  public void setBlockIncrement( int b )
  { 
    myBlockIncrement = b;
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      ((Adjustable)(li.next())).setBlockIncrement( b );
  }
  public void setMaximum( int m )
  { 
    myMaximum = m;
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      ((Adjustable)(li.next())).setMaximum( m );
  }
  public void setMinimum( int m )
  { 
    myMinimum = m;
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      ((Adjustable)(li.next())).setMinimum( m );
  }
  public void setUnitIncrement( int i ) {}
  public void setValue( int m )
  { 
    myValue = m;
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      ((Adjustable)(li.next())).setValue( m );
  }
  public void setVisibleAmount( int m )
  { 
    myVisibleAmount = m;
    for( ListIterator li = mySlaves.listIterator(); li.hasNext(); )
      ((Adjustable)(li.next())).setVisibleAmount( m );
  }
}
