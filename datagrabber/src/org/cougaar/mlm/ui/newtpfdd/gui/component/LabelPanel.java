/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;


import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;
import org.cougaar.mlm.ui.grabber.logger.Logger;


public class LabelPanel extends Container
    implements VirtualX, MessageListener
{
    private ArrayList visibleRows;

    //////////////////////////////////////////////////
    // Adjustable
    //private int myNumVisibleRows = 0;
    //private int myFirstVisibleRow = 0;

    // Adjustable
    //////////////////////////////////////////////////
	

    //////////////////////////////////////////////////
    // VirtualX
    private long myVirtualXLocation;
    private long myVirtualXSize;
	
    // VirtualX
    //////////////////////////////////////////////////

	
    //////////////////////////////////////////////////
    // MessageListener
    private ArrayList myMessageListeners = new ArrayList();
	
    // MessageListener
    //////////////////////////////////////////////////


    /////////////////////////////////////////////
    // Rendering
    private static final int myHMargin = 4; // 2;
    private static final int myVMargin = 4;
    private FontMetrics fontMetrics = null;
    public int maxLength = 0;
    private Color myDefaultBackground = new Color((float) 0.6, (float) 0.6, (float) 0.6);

    public LabelPanel(ArrayList cache) {
	setBackground(Color.black);
	visibleRows = cache;
    }
    public void addMessageListener(MessageListener l) {
	myMessageListeners.add(l);
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

	repaint();
    }

    /**
     * Lozenge rows are laid out in a vertical stack with
     * enough space between rows to let gridlines show through.
     * (This means there are 2 free pixel rows at both top and bottom
     * (a grid line and a space), and 3 free pixel rows between lozenge
     * rows (a grid line with a space above and below).
     */
    public void doLayout()
    {
	// Debug.out("LP:dL gs(): " + getSize());
	int numRows = visibleRows.size();

	if (numRows < 1)
	    return;
	final int cumLozengeHeight = getSize().height - 2 * 2 // top and bottom margins
	    -3 * (numRows - 1); // interrow spaces
	final int indLozengeHeight = cumLozengeHeight / numRows;
	final int lozengeRowInterval = getSize().height / numRows;
	for (int r = 0, y = 2; r < numRows; r++, y += lozengeRowInterval) {
	    LozengeRow lr = (LozengeRow) visibleRows.get(r);
	    if (lr != null) {
		lr.setVisible(true);
		lr.setLocation(0, y);
		lr.setSize(getSize().width, indLozengeHeight);
		lr.doLayout();
	    }
	}
	repaint();
    }
    public int getBlockIncrement() { return 1; }  
    public Color getDefaultBackground() { return myDefaultBackground; }  
    public int getMinimum() { return 0; }  

    private void setFontMetrics()
    {
	int rowHeight;
	Font myFont;
	if ( visibleRows.size() == 0 )
	    myFont = FontCache.get(12);
	else {
	    rowHeight = getSize().height / visibleRows.size();
	    myFont = FontCache.get(rowHeight - myVMargin * 2);
	    // Debug.out("LP:sFM rowHeight: " + rowHeight);
	}
	setFont(myFont);
	fontMetrics = getFontMetrics(myFont);
    }

    public int getNiceWidth(int overallWidth, int minLabelWidth)
    {
	if ( getSize().height == 0 )
	    return 0;
	setFontMetrics();
	if ( minLabelWidth < 0 ) {
	    int maxWidth = -1;
	    String maxString = "{NOTHING}";
	    for ( int i = 0; i < visibleRows.size(); i++ ) {
		LozengeRow lozRow = (LozengeRow)visibleRows.get(i);
		if ( lozRow == null ) {
		    // OutputHandler.out("LP:gNW Errning: lozRow is null: " + i);
		    continue;
		}
		ArrayList lozengeList = lozRow.getLozenges();
		if ( lozengeList == null ) {
		    // OutputHandler.out("LP:gNW Errning: lozenge list is null: " + i);
		    continue;
		}
		Lozenge loz = (Lozenge)(lozRow.getLozenges().get(0));
		LozengeLabel label = loz.getLozengeLabel(0);
		String labelText = label.getText();
		int width = fontMetrics.stringWidth(labelText);
		if ( width > maxWidth ) {
		    maxString = labelText;
		    maxWidth = width;
		}
	    }
	    return maxWidth + myHMargin * 2 + 16;
	}
	else
	    return (int)Math.min(overallWidth / 4, minLabelWidth);
    }
    
    public int getOrientation() { return Adjustable.VERTICAL; }  
    public Color getRowBackground(int r) {
	if (r >= getComponentCount())
	    return getDefaultBackground();
	Component comp = getComponent(r);
	Color c = comp.getBackground();
	return (c != null) ? c : getDefaultBackground();
    }  
    public String getRowText(int row) {
	String ss = "LabelPanel.getRowText is obsolete -- don't use it!";
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, ss);
	return ss;
	/***********
		    try {
		    LozengeRow lr = getLozengeRow(row);
		    Lozenge l = (Lozenge) lr.getComponent(0);
		    if (l != null) {
		    LozengeLabel ll = l.getLozengeLabel(0);
		    if (ll != null) {
		    String s = ll.getText();
		    if (s != null)
		    return s;
		    }
		    }
		    return "";
		    } catch (ArrayIndexOutOfBoundsException e) {
		    return "";
		    }
	************/
    }
    public int getUnitIncrement() { return 1; }  
    public long getVirtualXLocation() { return myVirtualXLocation; }  
    public long getVirtualXSize() { return myVirtualXSize; }  
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

	// lr width & height are 0 here!  Shouldn't setting VirturalXSize also set physical size?
	repaint();
    }

    public int maxLabelLength() {
	return maxLength;
    }  

    public void paint(Graphics g)
    {
	// Debug.out("LP:paint gF(): " + getFont());
	for (int r = 0; r < visibleRows.size(); r++) {
	    LozengeRow lr = (LozengeRow) visibleRows.get(r);
	    if (lr != null && !(lr.isValid()))
		lr.doLayout();
	}
	super.paint(g);
    }
    public void removeMessageListener(MessageListener l) {
	myMessageListeners.remove(l);
    }
    public void replace(int rowNum, LozengeRow lr) {
	LozengeRow replaced = (LozengeRow)visibleRows.get(rowNum);
	if (replaced != null) remove(replaced);

	if (lr != null) {
	    add(lr, 0);
	    lr.setVirtualXLocation(getVirtualXLocation());
	    lr.setVirtualXSize(getVirtualXSize());
	}
	visibleRows.set(rowNum, lr);

	repaint();
    }

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

	repaint();
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

	repaint();
    }

    public void setDefaultBackground( Color c ) { myDefaultBackground = c; }  
    // Rendering	
    //////////////////////////////////////////////////

    //////////////////////////////////
    // Gantt Chart interface functions

    public void setMessage(String msg) {
	for (ListIterator li = myMessageListeners.listIterator(); li.hasNext();)
	    ((MessageListener) (li.next())).setMessage(msg);
    }
    public void setMinimum( int i ) { }  
    public void setRowBackground( int r, Color c )
    {
	if (r >= getComponentCount())
	    return;
	Component comp = getComponent(r);
	comp.setBackground( c );
    }  
    // This is a dangerous method, heldover only for backward compatibility...FIXME
    public void setRowText(int row, String s) {
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "LabelPanel.setRowText is obsolete -- don't use it!");
	/***************
			if (s.length() > maxLength) {
			maxLength = s.length();
			//setVirtualXSize(maxLength*ReqIDSLabelRow.DAY/6); // FIXME!
			}
			LozengeLabel ll = new LozengeLabel(s); // (float) 0.0, LozengeLabel.LEFT);
			Lozenge l = new Lozenge();
			l.setForeground(Color.yellow);
			l.setLeftTipType(Lozenge.SQUARE_TIP);
			l.setRightTipType(Lozenge.SQUARE_TIP);
			l.setLozengeVirtualXLocation(0);
			l.setLozengeVirtualXSize(2 * ReqIDSLabelRow.DAY);
			l.setLozengeDescription("Label created by setRowText");
			l.addLozengeLabel(ll);
			LozengeRow lr = new LozengeRow();
			lr.add(l);
			setLozengeRow(row, lr);
	*****************/
    }
    public void setUnitIncrement( int n ) {}  
    public void setVirtualXLocation(long newLocation) {
	myVirtualXLocation = newLocation;
	for (ListIterator li = visibleRows.listIterator(); li != null && li.hasNext();) {
	    VirtualX vx = (VirtualX) li.next();
	    if (vx != null)
		vx.setVirtualXLocation(newLocation);
	}
    }
    public void setVirtualXSize(long newSize) {
	myVirtualXSize = newSize;
	for (ListIterator li = visibleRows.listIterator(); li != null && li.hasNext();) {
	    VirtualX vx = (VirtualX) li.next();
	    if (vx != null)
		vx.setVirtualXSize(newSize);
	}
    }
}
