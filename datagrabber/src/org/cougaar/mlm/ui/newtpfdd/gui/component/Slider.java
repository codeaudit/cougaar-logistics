/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/Slider.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.Adjustable;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.ListIterator;


public class Slider extends Component
implements Adjustable
{	
  // drawing scratch variables
  private final static int triNPoints = 3;
  private static int xPoints[] = new int[triNPoints];
  private static int yPoints[] = new int[triNPoints];

  private class Range
  {
	public int start;
	public int width;

	Range( int s, int w ) { start = s; width = w; }

	public int getEnd() { return start+width; }
  }

  // Behaviors
  ////////////////////////////////////////////


  ////////////////////////////////////////////
  // Event Processing

  private int myDragOrigin;
  private int myDragStartValue;
  private boolean myIsDragging = false;

  // Calculation Utilities
  ////////////////////////////////////////////


  ////////////////////////////////////////////
  // Adjustable implementation

  private int myOrientation = Adjustable.HORIZONTAL;
  private int myMinimum = 0;
  private int myMaximum = -1;
  private int myUnitIncrement = 1;
  private int myBlockIncrement = 15;
  private int myVisibleAmount = 15;
  private int myValue = 0;
  private ArrayList myAdjustmentListeners = new ArrayList();

  private static final int myPreferredLength = 400;
  private static final int myPreferredThickness = 15;

  // Adjustable implementation
  ////////////////////////////////////////////

  Slider()
  {
	setBackground( Color.gray.brighter() );
	setForeground( Color.gray );
	enableEvents( AWTEvent.MOUSE_EVENT_MASK 
		  | AWTEvent.MOUSE_MOTION_EVENT_MASK
		  | AWTEvent.KEY_EVENT_MASK );
  }  
  Slider( int orientation )
  {
	this();

	// Check for legal values
	switch( orientation )
	  {
	  case Adjustable.HORIZONTAL:
	  case Adjustable.VERTICAL:
	myOrientation = orientation;
	break;
	  }
  }  
  Slider( Slider s )
  {
	this(); 
	myOrientation = s.getOrientation();
	setMinimum( s.getMinimum() );
	setMaximum( s.getMaximum() );
	setValue( s.getValue() );
	setVisibleAmount( s.getVisibleAmount() );
	setUnitIncrement( s.getUnitIncrement() );
	setBlockIncrement( s.getBlockIncrement() );
	myAdjustmentListeners = (ArrayList)(s.myAdjustmentListeners.clone());
  }  
  public void addAdjustmentListener( AdjustmentListener l )
  {
	myAdjustmentListeners.add(l);
  }  
  private void distributeAdjustmentEvent( AdjustmentEvent e )
  {
	for (ListIterator listeners = myAdjustmentListeners.listIterator();
	 listeners.hasNext(); )
	  {
	((AdjustmentListener)(listeners.next())).adjustmentValueChanged( e );
	  }
  }  
  private void doBlockDecrement()
  {
	if( optimizedMoveThumb( getValue() - getBlockIncrement() ))
	  distributeAdjustmentEvent(
				new AdjustmentEvent( this,
						     AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						     AdjustmentEvent.BLOCK_DECREMENT, 
						     getValue() ));
  }  
  private void doBlockIncrement()
  {
	if( optimizedMoveThumb( getValue() + getBlockIncrement() ))
	  distributeAdjustmentEvent(
				new AdjustmentEvent( this,
						     AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						     AdjustmentEvent.BLOCK_INCREMENT, 
						     getValue() ));
  }  
  private void doTrack( int value )
  {
	if( optimizedMoveThumb( value ) )
	  distributeAdjustmentEvent(
				new AdjustmentEvent( this,
						     AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						     AdjustmentEvent.TRACK, 
						     getValue() ));
  }  
  private void doUnitDecrement()
  {
	if( optimizedMoveThumb( getValue() - getUnitIncrement() ))
	  distributeAdjustmentEvent(
				new AdjustmentEvent( this,
						     AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						     AdjustmentEvent.UNIT_DECREMENT, 
						     getValue() ));
  }  
  private void doUnitIncrement()
  {
	if( optimizedMoveThumb( getValue() + getUnitIncrement() ) )
	  distributeAdjustmentEvent( new AdjustmentEvent( this,
						      AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						      AdjustmentEvent.UNIT_INCREMENT, 
						      getValue() ));
  }  
  // Component extension
  ////////////////////////////////////////////

  ////////////////////////////////////////////
  // Behaviors

  public int getBlockIncrement() { return myBlockIncrement; }  
  public int getMaximum() { return myMaximum; }  
  public int getMinimum() { return myMinimum; }  
  private int getMovementEnd()
  {
	return getSliderEnd() - getTriangleLength();
  }  
  private int getMovementLength()
  {
	return getMovementEnd() - getMovementStart();
  }  
  private int getMovementStart()
  {
	return getSliderStart() + getTriangleLength();
  }  
  public int getOrientation() { return myOrientation; }  
  public Dimension getPreferredSize()
  {
	if( getOrientation() == Adjustable.HORIZONTAL )
	  return new Dimension( myPreferredLength, myPreferredThickness );
	else
	  return new Dimension( myPreferredThickness, myPreferredLength );
  }  
  private int getRange()
  {
	return getMaximum() - getMinimum() + 1;
  }  
  private int getSliderEnd()
  {
	if( getOrientation() == Adjustable.HORIZONTAL )
	  return getSize().width-1;
	else
	  return getSize().height-1;
  }  
  // Event Processing
  ////////////////////////////////////////////


  ////////////////////////////////////////////
  // Calculation Utilities
  //
  // These methods calculate the locations of
  // various points in the slider.  From low
  // end to high end, the significant points are:
  // SliderStart, MovementStart,
  // ThumbStart, ThumbMid, ThumbEnd,
  // MovementEnd, SliderEnd

  private int getSliderStart()
  {
	return 1;
  }  
  private int getThumbEnd()
  {
	return getThumbStart() + getThumbWidth();
  }  
  private int getThumbMid()
  {
	return getThumbStart() + (getThumbWidth()/2);
  }    
  private int getThumbStart()
  {
	if (getRange() <= getVisibleAmount())
	  return getMovementStart();
	return (int)((float)(getValue() - getMinimum()) /
		 (float)getRange() * getMovementLength()) + getMovementStart();
  }  
  private int getThumbWidth()
  {
	if (getRange() <= getVisibleAmount())
	  return getMovementLength();
	return (int)((float)getVisibleAmount() / (float)getRange() *
		 getMovementLength() );
  }  
  private int getTriangleLength()
  {
	if( getOrientation() == Adjustable.HORIZONTAL )
	  return (getSize().height-2) / 2;
	else
	  return (getSize().width-2) / 2;
  }  
  public int getUnitIncrement() { return myUnitIncrement; }  
  public int getValue() { return myValue; }  
  public int getVisibleAmount() { return myVisibleAmount; }  
  public boolean isFocusTraversable() { return true; }  
  /**
	  * Subtracts range b from range a.  The resulting range contains
	  * all values which exist in a but not in b.  <b>NOTE:</b> correct
	  * operation is guaranteed only if b is at least as wide as a.  This
	  * function cannot handle cases where b would split a into two ranges,
	  * and it may make assumptions based on the expectation that b is at
	  * least as wide as a.
	  *
	  * @return Range containing all values in a but not in b.  Returns null
	  *         if no such values exist.
	  */
  private Range minus( Range a, Range b )
  {
	if( b.getEnd() <= a.start  ||  b.start >= a.getEnd() )	// b outside a
	  return a;
	if( b.start <= a.start  &&  b.getEnd() >= a.getEnd() )	// b contains a
	  return null;
	if( b.start > a.start )			// b overlaps high side of a
	  return new Range( a.start, b.start-a.start );
	if( b.getEnd() < a.getEnd() )	// b overlaps low side of a
	  return new Range( b.getEnd(), a.getEnd()-b.getEnd() );
	else
	  return null;
  }  
  /**
	  * relocates and redraws thumb in a new location in an optimized fashion
	  *
	  * @return true if the move was allowed, false if no move actually took place
	  */
  private boolean optimizedMoveThumb( int newValue )
  {
	// Save old location
	Range oldRange = new Range( getThumbStart(), getThumbWidth() );
	int oldValue = getValue();
		
	// Update and paint at new location
	setValue( newValue );
	if( getValue() == oldValue ) return false;
	Graphics g = getGraphics();
	paint( g );

	// Clean up what's left of old location
	Range newRange = new Range( getThumbStart(), getThumbWidth() );
	Range leftovers = minus( oldRange, newRange );
	if( leftovers != null )
	  {
	g.setColor( getBackground() );
	if( getOrientation() == Adjustable.HORIZONTAL )
	  g.fillRect( leftovers.start, 0+1,  leftovers.width, getSize().height-2 );
	else
	  g.fillRect( 0+1, leftovers.start,  getSize().width-2, leftovers.width );
	  }

	return true;
  }  
  public void paint( Graphics g )
  {
	paintThumb( g );
	paintBack( g );
  }  
  public void paintBack( Graphics g )
  {

	// Paint blank space to each side of thumb
	g.setColor( getBackground() );
	if( getOrientation() == Adjustable.HORIZONTAL )
	  {
	g.fillRect( 0, 0,  getThumbStart(), getSize().height );
	g.fillRect( getThumbEnd(), 0,  getSize().width-getThumbEnd(), getSize().height );
	  }
	else //( orientation = Adjustable.VERTICAL)
	  {
	g.fillRect( 0, 0,  getSize().width, getThumbStart() );
	g.fillRect( 0, getThumbEnd(),  getSize().width, getSize().height-getThumbEnd() );
	  }

	// Paint border around component7
	g.setColor( getForeground() );
	g.drawRect( 0,0, getSize().width-1, getSize().height-1 );

	// Paint triangles at each end
	g.setColor( getForeground() );
	if( getOrientation() == Adjustable.HORIZONTAL )
	  {
	final int midHeight = getSize().height / 2;
	// left end
	xPoints[0] = getTriangleLength()+1;	yPoints[0] = 1;
	xPoints[1] = 0+1;					yPoints[1] = midHeight;
	xPoints[2] = getTriangleLength()+1;	yPoints[2] = getSize().height-1;
	g.fillPolygon( xPoints, yPoints, triNPoints );
	// right end
	xPoints[0] = getSize().width-1-getTriangleLength();	yPoints[0] = 1;
	xPoints[1] = getSize().width-1;						yPoints[1] = midHeight;
	xPoints[2] = getSize().width-1-getTriangleLength();	yPoints[2] = getSize().height-1;
	g.fillPolygon( xPoints, yPoints, triNPoints );
	  }
	else //( getOrientation() == Adjustable.VERTICAL )
	  {
	final int midWidth = getSize().width/ 2;
	final int triHeight = (getSize().width-2) / 2;
	// top end
	xPoints[0] = 1;
	yPoints[0] = triHeight+1;
	xPoints[1] = midWidth;
	yPoints[1] = 0+1;
	xPoints[2] = getSize().width-1;
	yPoints[2] = triHeight+1;
	g.fillPolygon( xPoints, yPoints, triNPoints );
	// bottom end
	xPoints[0] = 1;
	yPoints[0] = getSize().height-1-triHeight;
	xPoints[1] = midWidth;
	yPoints[1] = getSize().height-1;
	xPoints[2] = getSize().width-1;
	yPoints[2] = getSize().height-1-triHeight;
	g.fillPolygon( xPoints, yPoints, triNPoints );
	  }
  }  
  public void paintThumb( Graphics g )
  {
	g.setColor( getForeground() );
	if( getOrientation() == Adjustable.HORIZONTAL )
	  {
	g.fill3DRect( getThumbStart(), 0+1,  getThumbWidth()/2, getSize().height-1,  true);
	g.fill3DRect( getThumbMid(),   0+1,  getThumbWidth()/2, getSize().height-1,  true);
	  }
	else //( orientation = Adjustable.VERTICAL)
	  {
	g.fill3DRect( 0+1, getThumbStart(),  getSize().width-1, getThumbWidth()/2,  true);
	g.fill3DRect( 0+1, getThumbMid(),    getSize().width-1, getThumbWidth()/2,  true);
	  }
  }  
  public void processEvent( AWTEvent e )
  {
	int mouseLocation = 0;
	if( e instanceof MouseEvent )
	  {
	if( getOrientation() == Adjustable.HORIZONTAL )
	  mouseLocation = ((MouseEvent)e).getX();
	else
	  mouseLocation = ((MouseEvent)e).getY();
	  }

	switch( e.getID() )
	  {
	  case KeyEvent.KEY_PRESSED:
	switch( ((KeyEvent)e).getKeyCode() )
	  {
	  case KeyEvent.VK_LEFT:
	  case KeyEvent.VK_UP:
	    doUnitDecrement();
	    break;
	  case KeyEvent.VK_RIGHT:
	  case KeyEvent.VK_DOWN:
	    doUnitIncrement();
	    break;
	  case KeyEvent.VK_PAGE_UP:
	    doBlockDecrement();
	    break;
	  case KeyEvent.VK_PAGE_DOWN:
	    doBlockIncrement();
	    break;
	  }
	break;

	//		case MouseEvent.MOUSE_ENTERED:
	//			requestFocus();
	//			break;

	  case MouseEvent.MOUSE_PRESSED:

	requestFocus();

	myDragStartValue = getValue();
	if( getOrientation() == Adjustable.HORIZONTAL )
	  myDragOrigin = ((MouseEvent)e).getX();
	else
	  myDragOrigin = ((MouseEvent)e).getY();
			
	if ( mouseLocation > getMovementEnd() )
	  doUnitIncrement();	// top triangle
	else if( mouseLocation > getThumbEnd() )
	  doBlockIncrement();	// above thumb
	else if( mouseLocation > getThumbMid() )
	  doUnitIncrement();	// thumb top
	else if( mouseLocation > getThumbStart() )
	  doUnitDecrement();	// thumb bottom
	else if( mouseLocation > getMovementStart() )
	  doBlockDecrement();	// below thumb
	else
	  doUnitDecrement();	// bottom triangle
	break;

	  case MouseEvent.MOUSE_DRAGGED:
	myIsDragging = true;
	int mouseDelta = mouseLocation - myDragOrigin;
	doTrack( myDragStartValue
		 + (int)((float)getRange() / getMovementLength() * mouseDelta) );
	  }
  }  
  public void removeAdjustmentListener( AdjustmentListener l )
  {
	int index = myAdjustmentListeners.indexOf(l);
	myAdjustmentListeners.remove(index);
  }  
  public void setBlockIncrement( int b )
  {
	myBlockIncrement = b;
  }  
  public void setMaximum( int max )
  {
	myMaximum = max;
	repaint();
  }  
  public void setMinimum( int min )
  {
	myMinimum = min;
	repaint();
  }  
  public void setUnitIncrement( int u )
  {
	myUnitIncrement = u;
  }  
  public void setValue( int v )
  {
	myValue = v;
	if( myValue > getMaximum() - getVisibleAmount() + 1)
	  myValue = getMaximum() - getVisibleAmount() + 1;
	if( myValue < getMinimum() )
	  myValue = getMinimum();
	repaint();
  }  
  public void setVisibleAmount( int v )
  {
	if (v >= 0)
	  myVisibleAmount = v;
	repaint();
  }  
  ////////////////////////////////////////////
  // Component extension
	
  public void update( Graphics g ) { paint(g); }  
}
