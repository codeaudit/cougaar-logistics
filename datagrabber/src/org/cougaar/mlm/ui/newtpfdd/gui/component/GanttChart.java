/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;

import org.cougaar.mlm.ui.newtpfdd.gui.model.RowModelListener;
import org.cougaar.mlm.ui.newtpfdd.gui.model.ProxyRowModelListener;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;
import org.cougaar.mlm.ui.grabber.logger.Logger;


public class GanttChart extends Container 
  implements VirtualX, MouseListener, PropertyChangeListener, Adjustable, RowModelListener
{
  ////////////////////////////////////
  // ** Lozenge Decorator Constants **
  // Set these values when you subclass GanttChart

  // Relative size of the decorators
  public static float DEC_RELATIVE_SIZE = 0.2f;

  // Shapes assigned to each type
  public static Shape[] decShapes = null;

  // Relative heights of the different decorators
  public static float[] decYRelHeights = null;

  // T=attaches to left side, F=attaches to right side of lozenge
  public static boolean[] decAttachLeft = null;

  // ** Lozenge Decorator Constants **
  ////////////////////////////////////

  ////////////////////////////////////
  // ** Lozenge Status Constants **
  // Set these values when you subclass GanttChart
  // Values correspond to pattern constants in PatternMaker.java.

  public static final int NONE = 0;        // No pattern
    
    // ** Lozenge Status Constants **
    ////
    ////////////////////////////////

    //	private Component myIconPanel;
  private int minLabelWidth = -1; //200;

  private ArrayList myLabelRows = new ArrayList();
  protected LabelPanel myLabelPanel = new LabelPanel(myLabelRows);

  private ArrayList myLozengeRows = new ArrayList();
  protected LozengePanel myLozengePanel = new LozengePanel(myLozengeRows);
  
  private StatusLine myStatusLine = new StatusLine();

  private LongXRuler myVirtualXScroller = new LongXRuler();
  private Slider myVerticalScroller = new Slider(Adjustable.VERTICAL);

  private VirtualXGrid myLozengeGrid = new VirtualXGrid();

  private VirtualXSync myVirtualXSync = new VirtualXSync();
  private AdjustmentStateSync myVScrollSync = new AdjustmentStateSync();

  private boolean myHaveRightSideLabels = false;

  private int myNumVisibleRows;

  private Runnable fixScroller = new Runnable() {
      public void run() {
	myVerticalScroller.invalidate();
	myVerticalScroller.repaint();
      }
    };

  //////////////////////////////////////////////////
  // Adjustable

  protected int myOrientation = Adjustable.VERTICAL;
  protected int myMinimum = 0;
  protected int myMaximum = -1;
  protected int myUnitIncrement = 1;
  protected int myVisibleAmount = 0;
  protected int myBlockIncrement = 0;
  protected int myValue = 0;
  protected ArrayList myAdjustmentListeners = new ArrayList();
  protected int oldWidth;

  private ProxyRowModelListener rowProxy;

  public GanttChart()
  {
    setLayout(null);
    setBackground( Color.black );
    setForeground( Color.white );

    add( myLabelPanel );
    add( myLozengePanel );
    add( myStatusLine );
    add( myVirtualXScroller );
    add( myVerticalScroller );
    add( myLozengeGrid );	     // myLozengeGrid needs to be added *after*
    // myLozengePanel to appear behind it

    myVirtualXScroller.addVirtualXAdjustmentListener( myVirtualXSync );
    myVirtualXSync.addSlave( myLozengePanel );
    myVirtualXSync.addSlave( myLozengeGrid );
    myVirtualXSync.addSlave( myVirtualXScroller );
    myVirtualXSync.setVirtualXLocation( getVirtualXLocation() );
    //myVirtualXSync.setVirtualXSize( getVirtualXSize() );

    myVerticalScroller.addAdjustmentListener( myVScrollSync );

    // myLabelPanel and myLozengePanel are updated by GanttChart
    myVScrollSync.addSlave( myVerticalScroller );
    myVScrollSync.addSlave( this );

    myLozengePanel.addMessageListener( myStatusLine );
    myLabelPanel.addMessageListener( myStatusLine );

    setVirtualXLocation(-501);
    setVirtualXSize(1001);
    setTicInterval(500);

    // Initialize everybody
    myVScrollSync.initialize(this);

    rowProxy = new ProxyRowModelListener(this);

    repaint();
  }      

  public void addAdjustmentListener(AdjustmentListener l)
  {
    myAdjustmentListeners.add(l);
  }  
  // VirtualX
  ///////////////////////////////////////////////////////////

  public void doLayout()
  {
    if ( getVisibleAmount() == 0 || getMaximum() == -1)
      return;
    myVerticalScroller.setBlockIncrement(getVisibleAmount());
    int labelWidth = myLabelPanel.getNiceWidth(getSize().width, minLabelWidth);
    // Debug.out("GC:dL lW: " + labelWidth + " gS().w: " + getSize().width + " mLW: " + minLabelWidth);
    int vScrollWidth = myVerticalScroller.getPreferredSize().width;
    int lozengeWidth = getSize().width - labelWidth - vScrollWidth;
    if( getHaveRightSideLabels() )
      lozengeWidth -= (labelWidth + vScrollWidth);

    int hScrollHeight = myVirtualXScroller.getPreferredSize().height;
    int statlineHeight = myStatusLine.getPreferredSize().height;
    int lozengeHeight = getSize().height - hScrollHeight - statlineHeight;

    myLabelPanel.setLocation( vScrollWidth, hScrollHeight );
    myLabelPanel.setSize(labelWidth, lozengeHeight);

    /* FIXME and add right handling to above functions
       if( getHaveRightSideLabels() )
       {
       if( myRightSideVerticalScroller == null )
       {
       myRightSideVerticalScroller = new Slider( myVerticalScroller );
       add( myRightSideVerticalScroller );
       myRightSideVerticalScroller.addAdjustmentListener( myVScrollSync );
       myVScrollSync.addSlave( myRightSideVerticalScroller );
       }
       myRightSideVerticalScroller.setLocation( getSize().width-labelWidth-vScrollWidth,
       hScrollHeight );
       myRightSideVerticalScroller.setSize( vScrollWidth, lozengeHeight );

       if( myRightSideLabelPanel == null )
       {
       myRightSideLabelPanel = new LabelPanel();
       add( myRightSideLabelPanel );
       myRightSideLabelPanel.addMessageListener( myStatusLine );
       }
       myRightSideLabelPanel.setLocation( getSize().width-labelWidth, hScrollHeight );
       myRightSideLabelPanel.setSize( labelWidth, lozengeHeight );
       }
    */

    myVerticalScroller.setLocation( 0, hScrollHeight );
    myVerticalScroller.setSize( vScrollWidth, lozengeHeight );

    myVirtualXScroller.setLocation( labelWidth + vScrollWidth, 0 );
    myVirtualXScroller.setSize( lozengeWidth, hScrollHeight );

    myLozengePanel.setLocation( labelWidth + vScrollWidth,  hScrollHeight );
    myLozengePanel.setSize( lozengeWidth, lozengeHeight );
    myLozengeGrid.setLocation( labelWidth + vScrollWidth,  hScrollHeight );
    myLozengeGrid.setSize( lozengeWidth, lozengeHeight );
    myLozengeGrid.setYInterval( lozengeHeight / getVisibleAmount() );

    myStatusLine.setLocation( 0, hScrollHeight + lozengeHeight );
    myStatusLine.setSize( getSize().width, statlineHeight );

    /* 
     * This was an experiment to try to get it draw readable days as you dragged the window 
     * to be smaller.  We would need to redesign the way the components do drawing to get this
     * to not try to draw the same data in a smaller space.
     */
    /*
      if (oldWidth != 0) {
      double widthDecreaseRatio = (double)oldWidth/(double)getSize().width;
      setVirtualXSize((long)(getVirtualXSize ()*widthDecreaseRatio));
      } 
      oldWidth = getSize().width;
    */

    if (false){
      Font currentFont = getFont ();
      if (currentFont != null) {
	Font smallerFont = new Font (currentFont.getName(), currentFont.getStyle (), currentFont.getSize ()-1);
	myStatusLine.setFont (smallerFont);
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "GanttChart.doLayout - setting smaller font " + smallerFont);
      }
      else
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "GanttChart.doLayout - no font set");
    }
	
  }  
  // PropertyChangeListener
  //////////////////////////////////////////////////

  public void dump()
  {
    try {
      Object[] rows = myLozengeRows.toArray();
      for (int i = 0; i < rows.length; i++) {
	LozengeRow row = (LozengeRow) rows[i];
	row.dump();
      }
    } catch (Exception e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "GanttChart.dump() " + e);
    }
  }  

  public int getBlockIncrement()
  {
    return myBlockIncrement;
  }

  public long getCDayZeroTime()
  {
    return myVirtualXScroller.getCDayZeroTime();
  }

  public void setCDayZeroTime(long o)
  {
    myVirtualXScroller.setCDayZeroTime(o);
    myLozengeGrid.setVirtualXOrigin(o);
  }  

  // Functions for backward compatibility
  public int getFirstVisibleRow()
  {
    return getValue();
  }

  public boolean getHaveRightSideLabels() { return myHaveRightSideLabels; }  

  public long getLabelPanelWidth() { return myLabelPanel.getWidth(); }

  public int getLozengePanelWidth() { return myLozengePanel.getWidth(); }  

  public LozengePanel getLozengePanel()
  {
    return myLozengePanel;
  }

  public int getMaximum() { return myMaximum; }  

  public int getMinimum() { return myMinimum; }  

  public int getOrientation() { return Adjustable.VERTICAL; }  

  public int getRow(String label)
  {
    int i;
    for (i=0; i<myNumVisibleRows; i++) {
      if (myLabelPanel.getRowText(i).equals(label))
	return i;
    }
    return -1;
  }

  public int getRowHeight()
  {
    return ( getSize().height - myVirtualXScroller.getPreferredSize().height -
	     myStatusLine.getPreferredSize().height ) /
      getVisibleAmount(); 
  }  

  public String getRowLabel(int r)
  {
    return myLabelPanel.getRowText(r);
  }

  public void setLabelUnitsMode(int mode)
  {
    myVirtualXScroller.setLabelUnitsMode(mode);
  }

  public long getTicInterval() { return myVirtualXScroller.getTicInterval(); }  

  public long getTicLabelInterval() { return myVirtualXScroller.getLabelInterval(); }  

  public int getUnitIncrement() { return 1; }  

  public int getValue()
  {
    return myValue;
  }

  ///////////////////////////////////////////////////////////
  // VirtualX

  public long getVirtualXLocation() { return myVirtualXSync.getVirtualXLocation(); }  

  public long getVirtualXSize() { return myVirtualXSync.getVirtualXSize(); }  

  public int getVisibleRowLocation( int vr )
  {
    return myVirtualXScroller.getPreferredSize().height + getRowHeight()*vr;
  }  
  // Adjustable
  //////////////////////////////////////////////////

  // The rowXXX functions are used by the data structure, or other containing
  // class, to tell the GanttChart that rows have changed, and it needs to
  // update itself.

    // AscentConsumer portion of RowModelListener interface
  public void fireAddition(Object item)
  {
    rowProxy.fireAddition(item);
  }

  public void fireDeletion(Object item)
  {
    rowProxy.fireDeletion(item);
  }

  public void fireChange(Object item)
  {
    rowProxy.fireChange(item);
  }

  // Remainder of RowModelListener interface
  public void fireRowAdded(int rowNum)
  {
    final Object item = readItem(rowNum);
    if ( rowNum >= 0 && rowNum <= getMaximum() + 1 ) { // expected; n rows numbered 0 to n - 1
      setMaximum(getMaximum() + 1);
    } else if ( rowNum > getMaximum() + 1 ) {
      setMaximum(rowNum);
      OutputHandler.out("GC:fRA Warning: adding row " + rowNum
			+ " beyond known max " + getMaximum());
    }
    else if ( rowNum < 0 ) {
      OutputHandler.out("GC:fRA Error: Serious oddity: got negative row " + rowNum);
      return;
    }
    if (rowNum >= getValue() && rowNum < getValue() + getVisibleAmount()) { // if visible
      final int screenIndex = rowNum - getValue();
	    
      Runnable addRowRunnable = new Runnable() {
	  public void run() {
	    LozengeRow lr = makeLozengeRow(item);
	    LozengeRow rl = makeLabelRow(item);
	    myLozengePanel.insert(screenIndex, lr);
	    myLabelPanel.insert(screenIndex, rl);
	  }
	};
      SwingQueue.invokeLater(addRowRunnable);
    }
    SwingQueue.invokeLater(fixScroller);
  }
  
  public void fireRowChanged(int rowNum)
  {
    final Object item = readItem(rowNum);
    //    Debug.out("GC:fRC item " + item + " " + rowNum);
    if ( rowNum > getMaximum() ) {
      //	    OutputHandler.out("GC:fRC Error: change to nonexistent row " + rowNum + " obj " + item);
      return;
    }
    if (rowNum >= getValue() && rowNum < getValue() + getVisibleAmount()) {
      final int screenIndex = rowNum - getValue();

      Runnable changeRowRunnable = new Runnable() {
	  public void run() {
	    LozengeRow lr = makeLozengeRow(item);
	    LozengeRow rl = makeLabelRow(item);
	    myLozengePanel.replace(screenIndex, lr);
	    myLabelPanel.replace(screenIndex, rl);
	  }
	};
      SwingQueue.invokeLater(changeRowRunnable);
    }
  }

  public void fireRowDeleted(int rowNum)
  {
    //    Debug.out("GC:fRD " + rowNum);
    if (rowNum < 0 || rowNum > getMaximum()) {
      OutputHandler.out("GC:fRD Error: delete " + rowNum + " out of range " + getMaximum());
      return;
    }
    setMaximum(getMaximum() - 1);
    if (rowNum < getValue()) {
      setValueNoScroll(getValue() - 1);
    }
    else if (rowNum < getValue() + getVisibleAmount()) {
      final int rowNum_f = rowNum;
      final int value = getValue();
      Runnable deleteRowRunnable = new Runnable() {
	  public void run() {
	    int screenIndex = rowNum_f - value;
	    boolean top = false;
	    Object scrollIn = null;
	    if (getMaximum() >= getVisibleAmount()) {
	      if (value <= getMaximum() - getVisibleAmount()) {
		scrollIn = readItem(value + getVisibleAmount());
	      } else {
		setValueNoScroll(value - 1);
		scrollIn = readItem(value);
		top = true;
	      }
	    }
	    LozengeRow lr = null;
	    LozengeRow rl = null;
	    if (scrollIn != null) {
	      lr = makeLozengeRow(scrollIn);
	      rl = makeLabelRow(scrollIn);
	    }
	    myLozengePanel.delete(screenIndex, lr, top);
	    myLabelPanel.delete(screenIndex, rl, top);
	  }
	};
      SwingQueue.invokeLater(deleteRowRunnable);
    }
  }
    
  public void firingComplete()
  {
    Runnable firingCompleteRunnable = new Runnable()
      {
	public void run()
	{
	  invalidate();
	  repaint();
	}
      };
    SwingQueue.invokeLater(firingCompleteRunnable);
  }

  public void rowSelected(int row, Object ID) {}  
    
  // The "make" functions are used by the GanttChart to convert a data item
  // into a LozengeRow and a Label to be displayed.  These will typically be
  // overridden.
  protected LozengeRow makeLabelRow(Object o)
  {
    return (LozengeRow)o;
  }

  protected LozengeRow makeLozengeRow(Object o)
  {
    return (LozengeRow)o;
  }

  //////////////////////////////////////////////////
  // MouseListener
  public void mouseClicked(MouseEvent me)
  {
    Component c = me.getComponent();

    if (c != null) {
      if (c instanceof LozengeRow) {
	myLozengePanel.setAllSelected(false);
	reportAllSelected(false);
      }
      else if (c instanceof Lozenge) {
	Lozenge loz = (Lozenge) c;
	if (loz.getMouseInside()) {
	  if (!me.isShiftDown())
	    myLozengePanel.setAllSelectedExcept(false, loz.getID());
	  reportItemSelected(loz.getID(), me.isShiftDown());
	}
	else {
	  myLozengePanel.setAllSelected(false);
	  reportAllSelected(false);
	}
      }
    }
  }  

  public void mouseEntered(MouseEvent me) {}  

  public void mouseExited(MouseEvent me) {}  

  public void mousePressed(MouseEvent me) {}  

  public void mouseReleased(MouseEvent me) {}  

  public void doPopup (LozengeRow row) {}

  public void paint(Graphics g)
  {
    if (getVisibleAmount() == 0 || getMaximum() == -1) return;

    // GanttChart uses doLayout in a nonstandard way which requires
    // layout to be done more often.  Changes in the data cause changes
    // in the layout of the lozenges, which are treated as internal components.
    validate();

    g.setColor(getBackground());
    g.fillRect(0, 0,  getSize().width, myVirtualXScroller.getSize().height);

    for ( int i = 0; i < getVisibleAmount(); i++ ) {
      //g.setColor( getRowBackground( getFirstVisibleRow() + i ));
      g.setColor(Color.black);
      g.fillRect(0, getVisibleRowLocation(i), getSize().width, getRowHeight());
    }

    super.paint(g);
  }

  // MouseListener
  //////////////////////////////////////////////////

  //////////////////////////////////////////////////
  // PropertyChangeListener
  public void propertyChange(PropertyChangeEvent pce)
  {
    Object o = pce.getSource();

    if (o != null && o instanceof Lozenge) {
      Lozenge loz = (Lozenge) o;
      reportItemChanged(loz.getID());
    }
  }  

  /**
     * Used by the Gantt Chart to read an item from the data structure.
     * This function will usually be overridden.
     */
  protected Object readItem(int rowNum)
  {
    return null;
  }  

  public void removeAdjustmentListener(AdjustmentListener l)
  {
    int index = myAdjustmentListeners.indexOf(l);
    myAdjustmentListeners.remove(index);
  }  

  // The "report" functions are used by the GanttChart to report changes
  // to a lozenge that a user made through the interface.
  public void reportAllSelected(boolean yesno) {}  

  public void reportItemChanged(Object ID) {}  

  public void reportItemSelected(Object ID, boolean group) {}  

  public void setBlockIncrement(int b)
  {
    myBlockIncrement = b;

    // Update slider
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));
  }  

  public void setFirstVisibleRow(int i)
  {
    setValue(i);
  }

  public void setHaveRightSideLabels(boolean have)
  {
    if ( have != myHaveRightSideLabels ) {
      myHaveRightSideLabels = have;
      invalidate();
    }
  }  

  public void setLabelPanelWidth(long newSize)
  {
    myLabelPanel.setVirtualXSize(newSize);
  }

  public void setMaximum(int max)
  {
    myMaximum = max;

    // Update slider
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));
  }  

  public void setMinimum(int min)
  {
    myMinimum = min;

    // Update slider
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));
  }  

  public void setMinLabelWidth(int w) 
  {
    minLabelWidth = w;
  }

  public void setRowHeight( int h ) {}  
  /* FIXME -- used by LabelPanel
	 public void setLabelRow(int r, LozengeRow lr) {
	 // FIXME 	myLabelPanel.setLozengeRow(r, lr);
	 myLabelPanel.setRowText( r, "testText" );
	 }

	 public void setLabelPanelWidth(long newSize) {
	 myLabelPanel.setVirtualXSize(newSize);
	 }
	 */

  public void setTicInterval(long n) {
    myVirtualXScroller.setTicInterval(n); 
    myLozengeGrid.setVirtualXInterval(n);
    myVirtualXScroller.setLabelInterval(n);
  }  

  public void setTicLabelInterval(long n) 
  {
    myVirtualXScroller.setTicInterval(n); 
    myLozengeGrid.setVirtualXInterval(n);
    myVirtualXScroller.setLabelInterval(n);
  }  

  public void setUnitIncrement(int u)
  {
    myUnitIncrement = u;
  }
    
  public void setValue(int v)
  {
    // Debug.out("GC:sV enter " + v);
    int newValue = v;
    if (newValue > getMaximum() - getVisibleAmount() + 1)
      newValue = getMaximum() - getVisibleAmount() + 1;
    if (newValue < getMinimum())
      newValue = getMinimum();

	// Determine what has to be redrawn
    if (newValue == getValue())
      return;
    if (newValue < getValue()) {
      int i, j, arraySize;
      arraySize = Math.min(getValue() - newValue, getVisibleAmount());
      LozengeRow newRows[] = new LozengeRow[arraySize];
      LozengeRow newLabels[] = new LozengeRow[arraySize];
      for (i = newValue, j = 0; j < arraySize; i++, j++) {
	Object item = readItem(i);
	newRows[j] = makeLozengeRow(item);
	newLabels[j] = makeLabelRow(item);
      }
      myLozengePanel.scrollDown(newRows);
      myLabelPanel.scrollDown(newLabels);
    } else {
      int i, j, arraySize, start;
      arraySize = getVisibleAmount();
      start = newValue;
      if (newValue < getValue() + getVisibleAmount()) {
	start = getValue() + getVisibleAmount();
	arraySize = newValue - getValue();
      }
      LozengeRow newRows[] = new LozengeRow[arraySize];
      LozengeRow newLabels[] = new LozengeRow[arraySize];
      for (i = start, j = 0; j < arraySize; i++, j++) {
	Object item = readItem(i);
	newRows[j] = makeLozengeRow(item);
	newLabels[j] = makeLabelRow(item);
      }
      myLozengePanel.scrollUp(newRows);
      myLabelPanel.scrollUp(newLabels);
    }
    myValue = newValue;

	// Update slider
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));
  }

  protected void setValueNoScroll(int v)
  {
    int newValue = v;
    if (newValue > getMaximum() - getVisibleAmount() + 1)
      newValue = getMaximum() - getVisibleAmount() + 1;
    if (newValue < getMinimum())
      newValue = getMinimum();

    myValue = newValue;

    // Update slider
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));
  }  

  public int getNumRows()
  {
    return (getMaximum() + 1 - getMinimum());
  }
    
  public void setNumRows(int n)
  {
    setMaximum(getMinimum() + n - 1);
  }

  // Set current value and force re-load of all rows in Label- and LozengePanels.
  // Useful if the data set has suddenly changed significantly.
  public void setValueReloadAll(int v)
  {
    int newValue = v;
    if (newValue > getMaximum() - getVisibleAmount() + 1)
      newValue = getMaximum() - getVisibleAmount() + 1;
    if (newValue < getMinimum())
      newValue = getMinimum();


    int i, j, arraySize;
    arraySize = Math.min(getMaximum() - getMinimum() + 1, getVisibleAmount());
    for ( i = newValue, j = 0; j < arraySize; i++, j++) {
      Object item = readItem(i);
      myLozengePanel.replace(j, makeLozengeRow(item));
      myLabelPanel.replace(j, makeLabelRow(item));
    }
    for ( j = arraySize; j < getVisibleAmount(); j++ ) {
      myLozengePanel.replace(j, null);
      myLabelPanel.replace(j, null);
    }

    myValue = newValue;

    // Update slider, calls setVisibleAmount() on Adjustment listeners
    myVScrollSync.adjustmentValueChanged(new AdjustmentEvent(this, 0, 0, 0));

    myLozengePanel.invalidate();
    myLabelPanel.invalidate();
    repaint();
  }
    
  public void setVirtualXLocation(long newLocation)
  {
    myVirtualXSync.setVirtualXLocation(newLocation);
  }

  private static long SECOND = 1000l;
  private static long MINUTE = 60l*SECOND;
  private static long HOUR   = 60l*MINUTE;
  private static long DAY    = 24l*HOUR;
  private static long WEEK   = 7l*DAY;
  private static long MONTH  = 4l*WEEK;
  private static long YEAR   = 365l*DAY;
  
  public void setVirtualXSize(long newSize) {
    myVirtualXSync.setVirtualXSize(newSize);
    if (newSize > 48*MONTH) {
      setTicInterval (YEAR);
    } else if (newSize > 12*MONTH) {
      setTicInterval (MONTH);
    } else if (newSize > MONTH) {
      setTicInterval (WEEK);
    } else if (newSize > WEEK) {
      setTicInterval (3*DAY);
    } else if (newSize > 3*DAY) {
      setTicInterval (DAY);
    } else if (newSize > DAY) {
      setTicInterval (12*HOUR);
    } else if (newSize > 12*HOUR) {
      setTicInterval (6*HOUR);
    } else if (newSize > 6*HOUR) {
      setTicInterval (3*HOUR);
    } else if (newSize > 3*HOUR) {
      setTicInterval (HOUR);
    } else if (newSize > HOUR) {
      setTicInterval (15*MINUTE);
    } else if (newSize > 15*MINUTE) {
      setTicInterval (5*MINUTE);
    } else if (newSize > 5*MINUTE) {
      setTicInterval (2*MINUTE);
    } else if (newSize > 2*MINUTE) {
      setTicInterval (30*SECOND);
    } else if (newSize > 30*SECOND) {
      setTicInterval (15*SECOND);
    } else if (newSize > 15*SECOND) {
      setTicInterval (5*SECOND);
    } else if (newSize > 5*SECOND) {
      setTicInterval (2*SECOND);
    } else {
      setTicInterval (SECOND);
    }
  }

  public int getVisibleAmount()
  {
    return myVisibleAmount;
  }  

  public void setVisibleAmount(int v)
  {
    if ( v < 0 ) {
      OutputHandler.out("GC:sVA Error: Illegal value " + v + " received.");
      return;
    }
	
    if ( v == getVisibleAmount() ) {
      // Debug.out("GC:sVA Filtering secondary call: " + v);
      return;
    }

    if ( v < getVisibleAmount() ) {
      for ( int i = v; i < getVisibleAmount(); i++ ) {
	myLozengePanel.replace(i, null);
	myLabelPanel.replace(i, null);
      }
      for ( int i = v; i < getVisibleAmount(); i++ ) {
	myLozengeRows.remove(v);
	myLabelRows.remove(v);
      }
    }
    else {
      for ( int i = getVisibleAmount(); i < v; i++ ) {
	myLozengeRows.add(null);
	myLabelRows.add(null);
      }
    }

    myVisibleAmount = v;
    setValueReloadAll(getValue()); // results in a call to setVisibleAmount()
  }
}
