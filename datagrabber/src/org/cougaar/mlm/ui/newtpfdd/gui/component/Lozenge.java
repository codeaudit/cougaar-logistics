/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/Lozenge.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai, Stephen Lines, Jason Leatherman, Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.ListIterator;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;


public class Lozenge extends Component
    implements MouseListener, MouseMotionListener
{
  static boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.component.Lozenge.debug", 
									   "false"));
    // Tip Types
    public static final int POINTED_TIP = 0;
    public static final int SQUARE_TIP = 1;
    public static final int BROKEN_TIP = 2;

    private Color myFillColor;
    private Color myOutlineColor = Color.white;
    private Color mySelectedFillColor = Color.orange;
    private Color mySelectedOutlineColor = Color.orange;
    private Color myHighlightFillColor = Color.white;
    private Color myHighlightOutlineColor = Color.white;
    private Color myTextForeground = Color.white;
    private Color myTextBackground = Color.black;

    // Stiple pattern + myFillColor
    private Paint myFillPaint = myOutlineColor;

    private boolean myMouseInside = false;
    private boolean mySelected = false;

    private int myStatus = GanttChart.NONE;
    private Object id;

    private LozengeBar lozBar = new LozengeBar(this, 1);
    private ArrayList decoratorList = new ArrayList();

  /**Use this size as a fixed width (instead of VirtualX) if >=0**/
  private int fixedWidth=-1;
  /**Where to justify the fixed with w.r.t the Virtual X.
   * If FWJ_LEFT, it will anchor the left side to the VirtualX location.
   * IF FWJ_RIGHT, it will anchor the right side to the VirtualX+VirtualXSize
   **/
  private byte fixedWidthJustify=FWJ_LEFT;
  public static final byte FWJ_LEFT=1;
  public static final byte FWJ_RIGHT=2;

    public Lozenge()
    {
	// setFillColor( Color.green );
	addMouseListener(this);
	addMouseMotionListener(this);

    }
    
    public Lozenge(GanttChart gc, Object o)
    {
	this();
	addMouseListener(gc);
	addPropertyChangeListener(gc);
	id = o;
    }  

  /**Set to use a fixed width instead of a virtual size (that re-adjusts with zooming)**/
  public void setFixedWidth(int size, byte justification){
    fixedWidth=size;
    fixedWidthJustify=justification;
  }

  public boolean useFixedWidth(){return fixedWidth>=0;}
  public int getFixedWidth(){return fixedWidth;}
  public byte getFixedWidthJustification(){return fixedWidthJustify;}

    public void addDecorator(int type, long value, boolean warning)
    {
	LozengeDecorator ld = new LozengeDecorator(this, type, value, 0, warning);
	decoratorList.add(ld);
    }
    
    public void addDecorator(int type, long value)
    {
	LozengeDecorator ld = new LozengeDecorator(this, type, value, 0);
	decoratorList.add(ld);
    }
    
    public void addLozengeLabel(LozengeLabel l)
    {
	if (l != null)
	    lozBar.addLabel(l);
    }
    
    public void doLayout()
    {
	// Call layout's of subcomponents
	lozBar.doLayout(getSize().height);
	
	for (ListIterator li = decoratorList.listIterator(); li.hasNext(); ) {
	    LozengeDecorator d = (LozengeDecorator) li.next();
	    d.doLayout(lozBar);
	}
    }

    public void dump()
    {
	Debug.out("Lozenge:dump " + (id != null ? id : "[null]"));
	lozBar.dump();
    }

    /** Decorator Accessors **/

    public String getDecoratorDescription( int i ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	return ld.getDescription();
    }  
    public long getDecoratorVirtualXLocation( int i ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	return ld.getVirtualXLocation();
    }  
    public Color getFillColor() { return myFillColor; }  
    public Paint getFillPaint() {
	/*	if (myMouseInside)
		return getHighlightFillColor();
		else if (mySelected)
		return getSelectedFillColor();
		else
		return myFillPaint; */
	if (mySelected)
	    return getSelectedFillColor();
	if (myMouseInside)
	    return getHighlightFillColor();
	return myFillPaint;
    }  
    public Color getHighlightFillColor() {
	if (myHighlightFillColor == null)
	    return myFillColor;
	else
	    return myHighlightFillColor;
    }  
    public Color getHighlightOutlineColor() {
	if (myHighlightOutlineColor == null)
	    return myOutlineColor;
	else
	    return myHighlightOutlineColor;
    }  
    public Object getID() { return id; }  
    public int getLeftTipType() { return lozBar.getLeftTipType(); }  
    /** Lozenge Accessors **/

    public String getLozengeDescription() { return lozBar.getDescription(); }  
    public LozengeLabel getLozengeLabel(int n) { return lozBar.getLabel(n); }  
    public Polygon getLozengePolygon() { return lozBar.getDetectionPolygon(); }  
    public long getLozengeVirtualXLocation() { return lozBar.getVirtualXLocation(); }  
    public long getLozengeVirtualXSize() { return lozBar.getVirtualXSize(); }  
    public boolean getMouseInside() { return myMouseInside; }  
    public Color getOutlineColor() { return myOutlineColor; }  
    public Paint getOutlinePaint() {
	if (myMouseInside)
	    return getHighlightOutlineColor();
	else if (mySelected)
	    return getSelectedOutlineColor();
	else
	    return myOutlineColor;
    }  
    public double getRelativeHeight() { return lozBar.getRelativeHeight(); }  
    public int getRightTipType() { return lozBar.getRightTipType(); }  
    public boolean getSelected() { return mySelected; }  
    public Color getSelectedFillColor() {
	if (mySelectedFillColor == null)
	    return myFillColor;
	else
	    return mySelectedFillColor;
    }  
    public Color getSelectedOutlineColor() {
	if (mySelectedOutlineColor == null)
	    return myOutlineColor;
	else
	    return mySelectedOutlineColor;
    }  
    public int getStatus() { return myStatus; }  
    public Color getTextBackground() {
	return myTextBackground;
    }  
    public Color getTextForeground() {
	return myTextForeground;
    }  
    public void editProperties(MouseEvent me) {
    }

    //////////////////////////////////////////////////
    // MouseListener

    // Single click -> selection.
    // Double click -> create editor.
    public void mouseClicked(MouseEvent me)
    {
	if ( !myMouseInside )
	    return;
	int mod = me.getModifiers();
	Component c = getParent();
	LozengeRow lozRow = (LozengeRow)c;
	if ( c instanceof LozengeRow ) {
	    if ((mod & InputEvent.BUTTON3_MASK) != 0) {
		// Right mouse button: single click -> menu; double click -> default menu action
		editProperties(me);
	    } else {
		// Left mouse button: treat single and double-click the same 
		lozRow.setSelected(!lozRow.getSelected());
		toggleSelected();
	    }
	} 
    }

    public void mouseDragged(MouseEvent me) {}

    public void mouseEntered(MouseEvent me)
    {
	  if (debug) {
		System.out.println("mouseEntered: "+getLozengeDescription());
		System.out.println("            : "+
						   lozBar.getDetectionPolygon().xpoints[0]+" "+
						   lozBar.getDetectionPolygon().xpoints[1]+" "+
						   lozBar.getDetectionPolygon().xpoints[2]+" "+
						   lozBar.getDetectionPolygon().xpoints[3]);
		System.out.println("            : "+
						   lozBar.getDetectionPolygon().ypoints[0]+" "+
						   lozBar.getDetectionPolygon().ypoints[1]+" "+
						   lozBar.getDetectionPolygon().ypoints[2]+" "+
						   lozBar.getDetectionPolygon().ypoints[3]);
		System.out.println("            : "+me.getX()+" / "+me.getY());
	  }
	  
	if (lozBar.getDetectionPolygon().contains(me.getX(), me.getY()))
	    setMouseInside(true);
    }
    
    public void mouseExited(MouseEvent me) {
	setMouseInside(false);
    }  
    // MouseListener
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // MouseMotionListener

    public void mouseMoved(MouseEvent me) {
	if (lozBar.getDetectionPolygon().contains(me.getX(), me.getY())) {
	    setMouseInside(true);
	}
	else {
	    setMouseInside(false);
	}
    }  
    public void mousePressed(MouseEvent me) {}  
    public void mouseReleased(MouseEvent me) {}  
    // MouseMotionListener
    //////////////////////////////////////////////////

    public int numDecorators() { return decoratorList.size(); }  
    /** Rendering **/

    public void paint(Graphics g)
    {
	// Set clipping rectangle explicitly, since we might 
	// be positioned "off the side" of our parent.
	Rectangle clipBounds = getBounds();
	clipBounds.x = -getLocation().x;			// use parent's left
	if ( clipBounds.x < 0 )
	    clipBounds.x = 0;

	clipBounds.width = getParent().getSize().width - getLocation().x;
	g.setClip(clipBounds);

	lozBar.paint(g);

	// Call all decorator paint()'s.
	for (ListIterator li = decoratorList.listIterator(); li.hasNext(); ) {
	    LozengeDecorator d = (LozengeDecorator) li.next();
	    d.paint(g);
	}
    }

    public void removeLozengeLabel( int position ) { lozBar.removeLabel( position ); }  
    public void removeLozengeLabel(LozengeLabel l) {
	for (int i = 0; i < 5; i++) {
	    if (lozBar.getLabel(i) == l) {
		lozBar.removeLabel(i);
		break;
	    }
	}
    }  
    private void sendMessage( String msg ) {
	  if (debug) {
		System.out.println("sendMessage: "+msg);
	  }
	  
	if( getParent() instanceof MessageListener )
	    ((MessageListener)getParent()).setMessage( msg );
    }  
    public void setDecoratorDescription( int i, String desc ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	ld.setDescription(desc);
    }  
    // These should be used by LozengeRow, only.
    public void setDecoratorScreenXLocation( int i, int x ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	ld.setScreenXLocation(x);
    }  
    public void setDecoratorScreenXSize( int i, int s ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	ld.setScreenXSize(s);
    }  
    public void setDecoratorVirtualXLocation( int i, long newLocation ) {
	LozengeDecorator ld = (LozengeDecorator) decoratorList.get(i);
	ld.setVirtualXLocation(newLocation);
    }  
    public void setFillColor( Color c ) {
	myFillColor = c;
	super.setForeground(c);

	// Force reset of paint pattern
	setStatus(getStatus());
    }  
    public void setForeground( Color c ) { setFillColor(c); }  
    public void setHighlightFillColor( Color c ) { myHighlightFillColor = c; }  
    public void setHighlightOutlineColor( Color c ) { myHighlightOutlineColor = c; }  
    public void setID( Object o ) { id = o; }  
    public void setLeftTipType( int type ) { lozBar.setLeftTipType(type); }  
    public void setLozengeDescription( String desc ) { lozBar.setDescription(desc); }  
    // These set's should be used by LozengeRow, only.
    public void setLozengeScreenXLocation( int newLocation ) {
	lozBar.setScreenXLocation(newLocation);
    }  
    public void setLozengeScreenXSize( int newSize ) { 
	lozBar.setScreenXSize(newSize);
    }  
    public void setLozengeVirtualXLocation( long newLocation ) {
	lozBar.setVirtualXLocation(newLocation);
    }  
    public void setLozengeVirtualXSize( long newSize ) { 
	lozBar.setVirtualXSize(newSize);
    }  
    public void setMouseInside(boolean yesno) {
	if (myMouseInside != yesno) {
	    myMouseInside = yesno;
	    repaint();
	    if (myMouseInside) {
		if( lozBar.getDescription() != null )
		    sendMessage( lozBar.getDescription() );
	    }
	    else {
		sendMessage( null );
	    }
	}
    }  
    public void setOutlineColor( Color c ) { myOutlineColor = c; }  
    public void setRelativeHeight( double newHeight ) { lozBar.setRelativeHeight(newHeight); }  
    public void setRelativeHeightOffset( double newOffset ) { lozBar.setRelativeHeightOffset(newOffset); }  
    public void setRightTipType( int type ) { lozBar.setRightTipType(type); }  
    public void setSelected(boolean yesno) {
	if (mySelected != yesno) {
	    mySelected = yesno;
	    repaint();
	}
    }  
    public void setSelectedFillColor( Color c ) { mySelectedFillColor = c; }  
    public void setSelectedOutlineColor( Color c ) { mySelectedOutlineColor = c; }  
    public void setStatus( int s ) {
	myStatus = s;
	if (s == GanttChart.NONE)
	    myFillPaint = myFillColor;
	else {
	    // Set up status stiple pattern
	    int px = PatternMaker.getWidth();
	    int py = PatternMaker.getHeight();
	    BufferedImage bi = new BufferedImage(px, py, BufferedImage.TYPE_INT_RGB);
	    Graphics2D big = bi.createGraphics();
	    Rectangle2D.Float patternRect = new Rectangle2D.Float(0.0f, 0.0f,
								  (float)px, (float)py);
	    Image pattern = PatternMaker.makePattern(s, myFillColor, Color.black);
	    big.drawImage(pattern, null, null);
	    myFillPaint = new TexturePaint(bi, patternRect);
	}
	repaint();
    }  
    public void setTextBackground( Color c ) { myTextBackground = c; }  
    public void setTextForeground( Color c ) { myTextForeground = c; }  
    public boolean toggleSelected() {
	mySelected = !mySelected;
	repaint();
	return mySelected;
    }  
}
