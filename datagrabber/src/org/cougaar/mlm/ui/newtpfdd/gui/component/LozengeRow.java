/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/LozengeRow.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

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
import java.util.ArrayList;
import java.util.ListIterator;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;


public class LozengeRow extends Container
    implements VirtualX, MessageListener
{
    private long myVirtualLeft;
    private long myVirtualWidth;
    protected boolean isSelected = false;
    private GanttChart gc;
    private String label = "N/A";

    private ArrayList lozengeList = new ArrayList();

    // Virtual X to Screen X conversion factor
    private float virtToScreen;

    public LozengeRow () {
	setBackground( Color.black );
	setForeground( Color.green );
	setLayout(null);
    }

    public LozengeRow (GanttChart gc) {
	this();
	this.gc = gc;
	addMouseListener(gc);
    }

    public boolean getSelected() {
	return isSelected;
    }

    public void setSelected(boolean yesno) {
	isSelected = yesno;
    }

    public String getLabelText() { return label; }
    public void setLabelText(String l) { label = l; }

    public ArrayList getLozenges() {
	return lozengeList;
    }

    /**   the only approved way to add a lozenge is
     * via addLozenge.
     */
    public void add(Lozenge l) {
	addLozenge(l);
    }          
    public void add(Object foo) {
	try {
	    throw new Exception("Lozenge.add(Object): You can add only Lozenge and its derivatives to LozengeRow");
	} catch (Exception e) {
	    System.out.println(e);
	}
    }
    public void addLozenge(Lozenge l) {
	add(l, 0);   	// add to beginning so children will draw on top
	lozengeList.add(l);
    }

    public Lozenge findLozenge(Object ID) {
	for( ListIterator li = lozengeList.listIterator(); li.hasNext(); ) {
	    Lozenge loz = (Lozenge)li.next();
	    if (loz.getID().equals(ID))
		return loz;
	}
	return null;
    }

    public void dump()
    {
	try {
	    Object[] lozenges = lozengeList.toArray();
	    for ( int i = 0; i < lozenges.length; i++ ) {
		Lozenge l = (Lozenge) lozenges[i];
		Debug.out("LR:dump dumping Lozenge: " + l);
		l.dump();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    ///////////////////////////////////////
    // Overrides

    public void doLayout()
    {
	virtToScreen = (float)getSize().width / (float)getVirtualXSize();

	for( ListIterator li = lozengeList.listIterator(); li.hasNext(); ) {
	    Lozenge loz = (Lozenge)li.next();
	    layoutLozenge(loz);
	}
    }

    // Overrides
    ///////////////////////////////////////


    ///////////////////////////////////////
    // VirtualX

    public long getVirtualXLocation() { return myVirtualLeft; }

    public long getVirtualXSize() { return myVirtualWidth; }

    public void setVirtualXLocation( long newLocation ) {
	if( myVirtualLeft != newLocation )
	    {
		myVirtualLeft = newLocation;
		invalidate();
	    }
    }

    public void setVirtualXSize( long newSize ) {
	if( myVirtualWidth != newSize )
	    {
		myVirtualWidth = newSize;
		invalidate();
	    }
    }

    // VirtualX
    ///////////////////////////////////////

    public void layoutLozenge(Lozenge loz) {
	// Determine size of decorators
	int decScreenSize = (int)((loz.getRelativeHeight() * getSize().height) *
				  gc.DEC_RELATIVE_SIZE);

	long obtainedVirtLeft = loz.getLozengeVirtualXLocation();
	long obtainedVirtSize = loz.getLozengeVirtualXSize();

	if(loz.useFixedWidth()){
	  //If we are doing fixed width, muck with the lozScreen locations...
	  switch(loz.getFixedWidthJustification()){
	  case Lozenge.FWJ_LEFT:
	    obtainedVirtSize=(long)((float)loz.getFixedWidth()/virtToScreen);
	    break;
	  case Lozenge.FWJ_RIGHT:
	    long newSize=(long)((float)loz.getFixedWidth()/virtToScreen);
	    long diff=newSize-obtainedVirtSize;
	    obtainedVirtLeft-=diff;
	    obtainedVirtSize+=diff;
	    break;
	  default:
	    System.err.println("Unknown Fixed Width Justification");
	  }
	}

	// Find lozenge component boundaries (virtual)
	long lozVirtLeft = obtainedVirtLeft;
	long lozVirtRight = lozVirtLeft + obtainedVirtSize;
	int leftDecorator = -1, rightDecorator = -1;
	int numDex = loz.numDecorators();
	for (int i = 0; i < numDex; i++) {
	    long decVX = loz.getDecoratorVirtualXLocation(i);
	    if (decVX < lozVirtLeft) {
		lozVirtLeft = decVX;
		leftDecorator = i;
	    }
	    if (decVX > lozVirtRight) {
		lozVirtRight = decVX;
		rightDecorator = i;
	    }
	}

	// Expand Lozenge to allow for size of decorators
	if (leftDecorator >= 0) {
	    lozVirtLeft -= (long) ( (float) (decScreenSize / 2) / virtToScreen );
	}
	if (rightDecorator >= 0) {
	    lozVirtRight += (long) ( (float) (decScreenSize / 2) / virtToScreen );
	}

	// Set screen X coord's of Lozenge
	long lozVirtSize = lozVirtRight - lozVirtLeft;

	int lozScreenX = (int)( (float)(lozVirtLeft - getVirtualXLocation()) *
				virtToScreen ) + getLocation().x;
	int lozScreenSize = (int)( (float)lozVirtSize * virtToScreen );

	lozScreenSize = Math.max( 1, lozScreenSize );

	Point lozPoint = loz.getLocation();
	Dimension lozDim = loz.getSize();
	lozPoint.x = lozScreenX;
	lozDim.width = lozScreenSize;
	lozDim.height = getSize().height;
	loz.setLocation(lozPoint);
	loz.setSize(lozDim);

	// Set Lozenge X coord's of LozengeBar
	int lozBarScreenLeft = (int)((float)(obtainedVirtLeft -
					     lozVirtLeft) * virtToScreen);
	int lozBarScreenSize = (int)((float)obtainedVirtSize * virtToScreen);
	loz.setLozengeScreenXLocation(lozBarScreenLeft);
	loz.setLozengeScreenXSize(lozBarScreenSize);

       
	// Set Lozenge X coord's of Decorators
	for (int i = 0; i < numDex; i++) {
	    int decScreenX = (int)((float)(loz.getDecoratorVirtualXLocation(i) -
					   lozVirtLeft) * virtToScreen) -
		(decScreenSize / 2);
	    loz.setDecoratorScreenXLocation(i, decScreenX);
	    loz.setDecoratorScreenXSize(i, decScreenSize);
	}
	
	// Call Lozenge's own layout function to finish up.
	loz.doLayout();
    }

    public void setMessage( String msg ) {
	if( getParent() instanceof MessageListener )
	    ((MessageListener)getParent()).setMessage( msg );
    }

    public void update( Graphics g ) {
	paint(g);
    }
}
