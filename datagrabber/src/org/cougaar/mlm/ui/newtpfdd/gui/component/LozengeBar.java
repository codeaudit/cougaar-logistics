/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/LozengeBar.java,v 1.2 2002-08-09 16:46:10 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Sundar Narasimhan, Harry Tsai, Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;


public class LozengeBar extends LayeredComponent implements VirtualX
{
    private int myLeftTipType = Lozenge.POINTED_TIP;
    private int myRightTipType = Lozenge.POINTED_TIP;

    private Polygon myPolygon = null;
    private Polygon myFullFillPolygon = null;
    private Polygon myInsetOutlinePolygon = null;
    private double myHeightScaling = 1.0;
    private double myHeightOffset = 0.0;

    private LozengeLabel[] myLabels = new LozengeLabel[5];
    private String myDescription = null;

    private Lozenge loz;

    private long myVX, myVSize;
    private int myX, mySize;
    private int rowHeight;

    private int top, bottom, firstQHeight, midHeight, thirdQHeight;
    private int left, right, midWidth;
    private int leftIndent, rightIndent;

  boolean debug = false;
  
    public LozengeBar(Lozenge loz, int layer)
    {
	super(layer);
	this.loz = loz;
    }

    public void dump()
    {
	Debug.out("LB:dump l: " + left + " r: " + right + " labels: ");
	for ( int i = 0; i < 5; i++ )
	    if ( myLabels[i] != null )
		Debug.out("LB:dump label " + i + ": " + myLabels[i]);
    } 

    public int getBottom() { return bottom; }
    public int getTop() { return top; }

    public String getDescription() { return myDescription; }
    public void setDescription(String desc) { myDescription = desc; }

    public void addLabel(LozengeLabel l) {
	if (l != null)
	    myLabels[l.getPosition()] = l;
    }
    public LozengeLabel getLabel(int position) {
	return myLabels[position];
    }
    public void removeLabel(int position) {
	myLabels[position] = null;
    }

    public int getLeftTipType() { return myLeftTipType; }
    public void setLeftTipType( int type ) { myLeftTipType = type; }

    public int getRightTipType() { return myRightTipType; }
    public void setRightTipType( int type ) { myRightTipType = type; }

    public double getRelativeHeight() { return myHeightScaling; }
    public void setRelativeHeight( double newHeight ) {
	myHeightScaling = newHeight;
    }

    public double getRelativeHeightOffset()
    {
	return myHeightOffset;
    }

    public void setRelativeHeightOffset(double heightOffset)
    {
	myHeightOffset = heightOffset;
    }

    public Polygon getDetectionPolygon() { return myPolygon; }

    public long getVirtualXLocation() { return myVX; }
    public void setVirtualXLocation( long x ) { myVX = x; }

    public long getVirtualXSize() { return myVSize; }
    public void setVirtualXSize( long s ) { myVSize = s; }

    public int getScreenXLocation() { return myX; }
    public void setScreenXLocation( int x ) { myX = x; }

    public int getScreenXSize() { return mySize; }
    public void setScreenXSize( int s ) { mySize = s; }

  public void paint(Graphics g)
  {
	if ( getLayer() != LozengePanel.getPaintLayerNow()
	     && LozengePanel.getPaintLayerNow() != LozengePanel.ALL )
	  return;
	
	Graphics2D g2 = (Graphics2D) g;

	g2.setPaint( loz.getFillPaint() );
	g2.fill( myFullFillPolygon );

	if( rowHeight >= 7 ) {
	  g2.setPaint( loz.getOutlinePaint() );
	  g2.draw( myPolygon );
	}

	g2.setFont(loz.getFont());
	FontMetrics fm = g2.getFontMetrics();
	int xMargin = 1;
	int yMargin = 1;
	int textheight = fm.getAscent();
	int yPosition = rowHeight/2 - textheight/2;
	int labeltop = yPosition - yMargin;
	int labelheight = textheight + 2*yMargin;

	// These variables keep track of how much free space we
	// still have on each side of the lozenge.
	int leftStart = xMargin + 5;
	int leftEnd = mySize/2 - xMargin;
	int rightStart = mySize/2 + xMargin;
	int rightEnd = mySize - xMargin - 5;

	String text;
	for (int i = 0; i < myLabels.length; i++) {
	  LozengeLabel label = myLabels[i];

	  if (label == null)
		continue;

	  int gap = 0;
	  switch( label.getPosition() ) {
	  case LozengeLabel.CENTER:
		gap = (rightEnd - leftStart);
		break;
	  case LozengeLabel.LEFT:
	  case LozengeLabel.LEFT_MIDDLE:
		gap = (leftEnd - leftStart);
		break;
	  case LozengeLabel.RIGHT:
	  case LozengeLabel.RIGHT_MIDDLE:
		gap = (rightEnd - rightStart);
		break;
	  default:
		continue;
	  }

	  gap -= 2*xMargin;
	  if ((text = label.bestFit(gap, fm)) == null)
		continue;

	  int textwidth = fm.stringWidth( text );
	  int labelwidth = textwidth + 2*xMargin;

	  // Compute labelleft, and update free-space variables
	  int labelleft = 0;
	  switch( label.getPosition() ) {
	  case LozengeLabel.CENTER:
		labelleft = leftEnd - labelwidth/2;
		leftEnd = labelleft - 1;
		rightStart = labelleft + labelwidth + 1;
		break;
	  case LozengeLabel.LEFT:
		labelleft = leftStart;
		leftStart = labelleft + labelwidth + 1;
		break;
	  case LozengeLabel.LEFT_MIDDLE:
		labelleft = leftStart + (leftEnd - leftStart)/2 - labelwidth/2;
		break;
	  case LozengeLabel.RIGHT:
		labelleft = rightEnd - labelwidth;
		rightEnd = labelleft - 1;
		break;
	  case LozengeLabel.RIGHT_MIDDLE:
		labelleft = rightStart + (rightEnd - rightStart)/2 - labelwidth/2;
		break;
	  }

	  g2.setPaint( loz.getTextBackground() );
	  g2.fillRect( labelleft, labeltop, labelwidth, labelheight );
	  g2.setPaint( loz.getTextForeground() );
	  g2.drawString( text, labelleft+xMargin, labeltop+yMargin+textheight );
	  if (debug)
		System.out.println ("LozengeBar.paint - Drawing " + text + " at x " + labelleft+xMargin + " y " + 
							labeltop+yMargin+textheight);
	}
  }

    /**
     * LozengeBar's myX and mySize should be set before this
     * function is called.
     */
    public void doLayout(int rowHeight)
    {
	// loz.setFont(FontCache.get(rowHeight * 3 / 4));

	this.rowHeight = rowHeight;
	final int margin = (int)(((1.0-getRelativeHeight())
				  * rowHeight)
				 / 2.0);
	top = margin + (int)(getRelativeHeightOffset() * rowHeight);
	bottom = rowHeight - 1 - margin + (int)(getRelativeHeightOffset() * rowHeight);
	midHeight = (bottom+top) / 2;
	firstQHeight = (midHeight+top) / 2;
	thirdQHeight = (bottom+midHeight) / 2;

	left = myX;
	right = myX + mySize;
	midWidth = (right+left) / 2;

	myPolygon = new Polygon();
	myFullFillPolygon = new Polygon();
	myInsetOutlinePolygon = new Polygon();

	// left tip
	int indentedLeftX;
	switch( myLeftTipType )
	    {
	    default:
	    case Lozenge.POINTED_TIP:
		leftIndent = Math.min(midHeight - margin, midWidth - left);
		indentedLeftX = left + leftIndent;
		myPolygon.addPoint( indentedLeftX, top );
		myPolygon.addPoint( left, midHeight );
		myPolygon.addPoint( indentedLeftX, bottom );
		myFullFillPolygon.addPoint( indentedLeftX, top );
		myFullFillPolygon.addPoint( left, midHeight );
		myFullFillPolygon.addPoint( indentedLeftX, bottom+1 );
		myInsetOutlinePolygon.addPoint( indentedLeftX+1, top+1 );
		myInsetOutlinePolygon.addPoint( left+2, midHeight );
		myInsetOutlinePolygon.addPoint( indentedLeftX+1, bottom-1 );
		break;
	    case Lozenge.SQUARE_TIP:
		myPolygon.addPoint( left, top );
		myPolygon.addPoint( left, bottom );
		myFullFillPolygon.addPoint( left, top );
		myFullFillPolygon.addPoint( left, bottom );
		myInsetOutlinePolygon.addPoint( left+1, top+1 );
		myInsetOutlinePolygon.addPoint( left+1, bottom-1 );
		break;
	    case Lozenge.BROKEN_TIP:
		leftIndent = Math.min(midHeight - margin, midWidth - left) / 2;
		indentedLeftX = left + leftIndent;
		myPolygon.addPoint( indentedLeftX, top );
		myPolygon.addPoint( left, firstQHeight );
		myPolygon.addPoint( indentedLeftX, midHeight );
		myPolygon.addPoint( left, thirdQHeight );
		myPolygon.addPoint( indentedLeftX, bottom );
		myFullFillPolygon.addPoint( indentedLeftX, top );
		myFullFillPolygon.addPoint( left, firstQHeight );
		myFullFillPolygon.addPoint( indentedLeftX, midHeight );
		myFullFillPolygon.addPoint( left, thirdQHeight );
		myFullFillPolygon.addPoint( indentedLeftX, bottom+1 );
		myInsetOutlinePolygon.addPoint( indentedLeftX+1, top+1 );
		myInsetOutlinePolygon.addPoint( left+2, firstQHeight );
		myInsetOutlinePolygon.addPoint( indentedLeftX+1, midHeight );
		myInsetOutlinePolygon.addPoint( left+2, thirdQHeight );
		myInsetOutlinePolygon.addPoint( indentedLeftX+1, bottom-1 );
		break;
	    }

	// right tip
	int indentedRightX;
	switch( myRightTipType )
	    {
	    default:
	    case Lozenge.POINTED_TIP:
		rightIndent = Math.min(midHeight - margin, midWidth - left);
		indentedRightX = right - rightIndent + 1;
		myPolygon.addPoint( indentedRightX, bottom );
		myPolygon.addPoint( right, midHeight );
		myPolygon.addPoint( indentedRightX, top );
		myFullFillPolygon.addPoint( indentedRightX+1, bottom+1);
		myFullFillPolygon.addPoint( right+1, midHeight );
		myFullFillPolygon.addPoint( indentedRightX+1, top );
		myInsetOutlinePolygon.addPoint( indentedRightX-1, bottom-1 );
		myInsetOutlinePolygon.addPoint( right-2, midHeight );
		myInsetOutlinePolygon.addPoint( indentedRightX-1, top+1 );
		break;
	    case Lozenge.SQUARE_TIP:
		myPolygon.addPoint( right, bottom );
		myPolygon.addPoint( right, top );
		myFullFillPolygon.addPoint( right+1, bottom+1);
		myFullFillPolygon.addPoint( right+1, top );
		myInsetOutlinePolygon.addPoint( right-1, bottom-1 );
		myInsetOutlinePolygon.addPoint( right-1, top+1 );
		break;
	    case Lozenge.BROKEN_TIP:
		rightIndent = Math.min(midHeight - margin, midWidth - left) / 2;
		indentedRightX = right - rightIndent + 1;
		myPolygon.addPoint( right, bottom );
		myPolygon.addPoint( indentedRightX, thirdQHeight );
		myPolygon.addPoint( right, midHeight );
		myPolygon.addPoint( indentedRightX, firstQHeight );
		myPolygon.addPoint( right, top );
		myFullFillPolygon.addPoint( right+1, bottom+1 );
		myFullFillPolygon.addPoint( indentedRightX+1, thirdQHeight );
		myFullFillPolygon.addPoint( right+1, midHeight );
		myFullFillPolygon.addPoint( indentedRightX+1, firstQHeight );
		myFullFillPolygon.addPoint( right+1, top );
		myInsetOutlinePolygon.addPoint( right-2, bottom-1 );
		myInsetOutlinePolygon.addPoint( indentedRightX-1, thirdQHeight );
		myInsetOutlinePolygon.addPoint( right-2, midHeight );
		myInsetOutlinePolygon.addPoint( indentedRightX-1, firstQHeight );
		myInsetOutlinePolygon.addPoint( right-2, top+1 );
		break;
	    }
    }

    /**
     * For use by decorators: compute the x value where a line
     * on a given y would intersect the lozenge.  Assumes
     * layout() has been called.
     * @param y         The y value of the line.
     * @param leftFlag  Intersecting left side?  Else right side.
     */
    public float computeX(float y, boolean leftFlag) {
	float x;
	float q0 = (float)top;
	float q1 = (float)firstQHeight;
	float q2 = (float)midHeight;
	float q3 = (float)thirdQHeight;
	float q4 = (float)bottom;

	if (leftFlag) {
	    switch( myLeftTipType )
		{
		default:
		case Lozenge.POINTED_TIP:
		    x = (float)left + ((float)leftIndent * (Math.abs(q2 - y) / (q2 - q0)));
		    break;
		case Lozenge.SQUARE_TIP:
		    x = (float)left;
		    break;
		case Lozenge.BROKEN_TIP:
        if (y < q1) {
			x = (float)left + ((float)leftIndent * ((q1 - y) / (q1 - q0)));
        } else if (y < q2) {
			x = (float)left + ((float)leftIndent * (1.0f - ((q2 - y) / (q2 - q1))));
        } else if (y < q3) {
			x = (float)left + ((float)leftIndent * ((q3 - y) / (q3 - q2)));
        } else{
			x = (float)left + ((float)leftIndent * (1.0f - ((q4 - y) / (q4 - q3))));
        }
		    break;
		}
	}
	else {
	    switch( myRightTipType )
		{
		default:
		case Lozenge.POINTED_TIP:
		    x = (float)right - ((float)rightIndent * (Math.abs(q2 - y) / (q2 - q0)));
		    break;
		case Lozenge.SQUARE_TIP:
		    x = (float)right;
		    break;
		case Lozenge.BROKEN_TIP:
        if (y < q1) {
			x = (float)right - ((float)rightIndent * (1.0f - ((q1 - y) / (q1 - q0))));
        } else if (y < q2) {
			x = (float)right - ((float)rightIndent * ((q2 - y) / (q2 - q1)));
        } else if (y < q3){
			x = (float)right - ((float)rightIndent * (1.0f - ((q3 - y) / (q3 - q2))));
        } else {
			x = (float)right - ((float)rightIndent * ((q4 - y) / (q4 - q3)));
        }
		    break;
		}
	}

	return x;
    }
}
