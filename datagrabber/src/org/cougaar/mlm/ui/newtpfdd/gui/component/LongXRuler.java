/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/LongXRuler.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Sundar Narasimhan, Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class LongXRuler extends VirtualXComponent
    implements VirtualX
{
    public static final int JAVALONG_UNITS = 0;
    public static final int CTHOUSANDS_UNITS = 1;
    public static final int CDAYS_UNITS = 2;
    public static final int PLAINDAYS_UNITS = 3;
      
    private int myLabelUnitsMode = CDAYS_UNITS;

    private long myCDayZeroTime = 0;
    private long myTicInterval = 1000;
    private	long myLabelInterval = 1000;

    private static Dimension myPreferredSize = new Dimension( 400,25 );
    private int myResizeHandleWidth = 5;

    private ArrayList myListeners = new ArrayList();

    int myXDragOrigin;
    long myVXDragOrigin;
    long myVXOriginalSize;
    boolean myAmResizing;	// are we resizing or moving?
    boolean myResizingFromTop;	// are we resizing the top or bottom end?

  private static long MINUTE = 60l*1000l;
  private static long HOUR   = 60l*MINUTE;
  private static long DAY    = 24l*HOUR;
  private static Font biggerFont = new Font("SansSerif", Font.BOLD, 14);

    LongXRuler()
    {
	// TBD -- 
	setFont(new Font("SansSerif", Font.BOLD, 12));
	setBackground( new Color((float)0.25, (float)0.52, (float)0.66));
	setForeground( Color.white );
	enableEvents( AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK );
	setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
    }
    public void addVirtualXAdjustmentListener( VirtualXAdjustmentListener l ) { myListeners.add(l); }
    public long getCDayZeroTime() { return myCDayZeroTime; }
    public long getLabelInterval() { return myLabelInterval; }
    public Dimension getPreferredSize() { return myPreferredSize; }
    public int getResizeHandleWidth() { return myResizeHandleWidth; }
    public long getTicInterval() { return myTicInterval; }
    private void notifyListeners( int changeType )
    {
	for( ListIterator e = myListeners.listIterator(); e.hasNext(); )
	    ((VirtualXAdjustmentListener)e.next()).VirtualXChanged( changeType, this );
    }
    public void paint( Graphics g )
    {
	g.setColor(getBackground());
	g.fillRect(0, 0, getSize().width, getSize().height);

	long start = getVirtualXLocation() - getCDayZeroTime();
	long end = start + getVirtualXSize();
		
	if( end <= start ) return;

	// paint a little beyond the edges, since labels
	// will start to become visible before their tics
	// are actually in view
	long overscan = getVirtualXSize() / 10;
	start = start - overscan;
	end   = end   + overscan;

	// compute start values
	long firstTic;
	if( start >= 0)
	    firstTic = ((start+getTicInterval()-1)
			/ getTicInterval())
		* getTicInterval();
	else // ( start < 0 )
	    firstTic = start
		/ getTicInterval()
		* getTicInterval();

	long firstLabel;
	if( start >=0 )
	    firstLabel = ((start+getLabelInterval()-1)
			  / getLabelInterval())
		* getLabelInterval();
	else // ( start < 0 )
	    firstLabel = start / getLabelInterval() * getLabelInterval();

	// paint tics
	g.setColor( getForeground() );
	for( long i=firstTic; i<=end; i+=getTicInterval() )
	    {
		long x = i + getCDayZeroTime();
		g.drawLine( ScreenXOfVirtualX(x), 0,
			    ScreenXOfVirtualX(x), getSize().height );
	    }

	// paint labels
	g.setFont( getFont() );
	final FontMetrics fm = g.getFontMetrics();
	final int textheight = fm.getAscent();
	for( long j=firstLabel; j<=end; j+=getTicInterval() )
	    {
		long labelx = j + getCDayZeroTime();

		String text;
		{
		    switch( myLabelUnitsMode )
			{
			default:
			case JAVALONG_UNITS:
			    text = Long.toString( labelx );
			    break;
			case CTHOUSANDS_UNITS:
			    text = "C:" + Long.toString( labelx / 1000 );
			    break;
			case PLAINDAYS_UNITS:
			    text = Long.toString((labelx - getCDayZeroTime()) / (1000 * 3600 * 24));
			    break;
			case CDAYS_UNITS:
			    text = "C:" + Long.toString( (labelx-getCDayZeroTime()) / (1000*3600*24) );
			    break;
			}
		}

		if (getTicInterval () < DAY) {
		  long millisToday = labelx % DAY;
		  long hour = millisToday / HOUR;
		  if (millisToday >= HOUR) {
			if (hour < 10)
			  text = "0" + hour + ":00";
			else
			  text = "" + hour + ":00";
			g.setFont( getFont () );
		  }
		  else
			g.setFont( biggerFont );
		}
		
		final int textwidth = fm.stringWidth( text );

		g.drawString(text, ScreenXOfVirtualX(labelx + getTicInterval() / 2) - textwidth / 2,
			     getSize().height / 2 + textheight / 2 );
	    }

	// paint resize handles
	g.setColor(getForeground().darker());
	g.fill3DRect(0,0, getResizeHandleWidth(), getSize().height, true);
	g.fill3DRect(getSize().width-getResizeHandleWidth(),0, getResizeHandleWidth(), getSize().height, true);
    }
    public void processMouseEvent( MouseEvent e )
    {
	switch( e.getID() )
	    {
	    case MouseEvent.MOUSE_PRESSED:
		myXDragOrigin = e.getX();
		myVXDragOrigin = getVirtualXLocation();
		myVXOriginalSize = getVirtualXSize();
		if( myXDragOrigin <= getResizeHandleWidth() )
		    {
			myAmResizing = true;
			myResizingFromTop = false;
		    }
		else if( myXDragOrigin >= getSize().width-getResizeHandleWidth() )
		    {
			myAmResizing = true;
			myResizingFromTop = true;
		    }
		else
		    myAmResizing = false;
		break;
	    }
	super.processMouseEvent(e);
    }
    public void processMouseMotionEvent( MouseEvent e )
    {
	switch( e.getID() )
	    {
	    case MouseEvent.MOUSE_DRAGGED:
		int XDragDelta = myXDragOrigin - e.getX();
		long VXDragDelta = getVirtualXSize() * XDragDelta / getSize().width;
		if( myAmResizing )
		    {
			if( myResizingFromTop ) {
			    setVirtualXSize( myVXOriginalSize + VXDragDelta );
			    notifyListeners( VirtualXAdjustmentListener.SIZE_CHANGED );
			}
			else
			    {
				setVirtualXSize( myVXOriginalSize - VXDragDelta );
				setVirtualXLocation( myVXDragOrigin + VXDragDelta );
				notifyListeners(   VirtualXAdjustmentListener.LOCATION_CHANGED 
						   | VirtualXAdjustmentListener.SIZE_CHANGED );
			    }				
		    }
		else {
		    setVirtualXLocation( myVXDragOrigin + VXDragDelta );
		    notifyListeners( VirtualXAdjustmentListener.LOCATION_CHANGED );
		}
		repaint();
		break;
	    case MouseEvent.MOUSE_MOVED:
		if( e.getX() <= getResizeHandleWidth() )
		    setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
		else if( e.getX() >= getSize().width-getResizeHandleWidth() )
		    setCursor( Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) );
		else
		    setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
	    }
	super.processMouseMotionEvent(e);
    }
    public void removeVirtualXAdjustmentListener( VirtualXAdjustmentListener l ) { myListeners.remove(myListeners.indexOf(l)); }

    public void setCDayZeroTime(long zeroTime)
    {
	myCDayZeroTime = zeroTime;
    }

    public void setLabelUnitsMode(int mode)
    {
	myLabelUnitsMode = mode;
    }

    public void setLabelInterval( long newInterval )
    {
	myLabelInterval = newInterval;
    }
    public void setResizeHandleWidth( int w ) { myResizeHandleWidth = w; }
    public void setTicInterval( long newInterval )
    {
	myTicInterval = newInterval;
    }
    public void setVirtualXLocation( long newLocation )
    {
	super.setVirtualXLocation( newLocation );
	repaint();
    }
    public void setVirtualXSize( long newSize )
    {
	super.setVirtualXSize( newSize );
	repaint();
    }
}
