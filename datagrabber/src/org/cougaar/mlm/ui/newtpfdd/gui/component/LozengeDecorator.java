/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/LozengeDecorator.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Jason Leatherman, Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;


import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;


class LozengeDecorator extends LayeredComponent implements VirtualX
{
    private Lozenge loz;

    private String myDescription = null;
    private boolean warning;
    private int decoratorType;

    private long myVx;                  // Virtual
    private float myX, myY, mySize;     // Screen (float since Java 2D is used)
    private float cx1, cx2, cy;         // Screen connector endpoints

    public LozengeDecorator(Lozenge loz, int type, long val, int layer)
    {
	this(loz, type, val, layer, false);
    }

    public LozengeDecorator(Lozenge loz, int type, long val, int layer, boolean warning) {
	super(layer);
	this.loz = loz;
	decoratorType = type;
	setVirtualXLocation( val );
	this.warning = warning;
	setScreenXSize(10);
    }

    public String getDescription() { return myDescription; }
    public void setDescription( String desc ) { myDescription = desc; }

    public long getVirtualXLocation() { return myVx; }
    public void setVirtualXLocation( long newLocation ) {
	myVx = newLocation;
    }

    // A decorator's virtual size is undefined.
    public long getVirtualXSize() { return 0; }
    public void setVirtualXSize( long newSize ) {}

    public int getScreenXLocation() { return (int) myX; }
    public void setScreenXLocation( int x ) { myX = (float) x; }

    public int getScreenXSize() { return (int) mySize; }
    public void setScreenXSize( int s ) { mySize = (float) s; }

    public void paint(Graphics g)
    {
	if ( getLayer() != LozengePanel.getPaintLayerNow() )
	    return;
	Graphics2D g2 = (Graphics2D) g;
	if (g2 == null) return;

	// If the decorator is lying right on the lozenge's line,
	// there's no need for a connector.
	if (Math.abs(cx1 - cx2) > (mySize / 2.0f)) {
	    // Debug.out("LD:paint connector: " + cx1 + ", " + cy + " to " + cx2 + ", " + cy);
	    Line2D.Float connector = new Line2D.Float(cx1, cy, cx2, cy);
	    g2.setColor(loz.getOutlineColor());
	    g2.draw(connector);
	}

	Shape s = GanttChart.decShapes[decoratorType];

	if ( warning )
	    g2.setColor(Color.red);
	else
	    g2.setColor(Color.cyan);
	g2.translate(cx1, cy);
	g2.fill(s);
	g2.setColor(loz.getOutlineColor());
	g2.draw(s);
	g2.translate(-cx1, -cy);
    }

    public void doLayout(LozengeBar lb) {
	float yStart = (float)lb.getTop();
	float ySpan = (float)lb.getBottom() - yStart;
	myY = yStart + (ySpan * GanttChart.decYRelHeights[decoratorType]) -
	    (mySize / 2.0f);

	// Connector endpoints
	cy = yStart + (ySpan * GanttChart.decYRelHeights[decoratorType]);
	cx1 = (float)myX;
	cx2 = lb.computeX(cy, GanttChart.decAttachLeft[decoratorType]);
    }
}
