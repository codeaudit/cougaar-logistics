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
import java.awt.image.*;
import java.awt.geom.*;


public class VirtualXGrid extends VirtualXComponent
{
  private long myVirtualXOrigin = 0;
  private long myVirtualXInterval = 150;
  private int myYInterval = 21;
  private long myCurrTimeVX = 0;

  private Paint pastTimeStiples;
  private Stroke timeLineStroke;

  VirtualXGrid()
  {
    setBackground( Color.black );
    setForeground( Color.green );

    // Create stiple pattern
    int px = PatternMaker.getWidth();
    int py = PatternMaker.getHeight();
    BufferedImage bi = new BufferedImage(px, py, BufferedImage.TYPE_INT_RGB);
    Graphics2D big = bi.createGraphics();
    Rectangle2D.Float patternRect = new Rectangle2D.Float(0.0f, 0.0f,
							  (float)px, (float)py);
    Image pattern = PatternMaker.makePattern(PatternMaker.LooseSpecks,
					     getForeground(), getBackground());
    big.drawImage(pattern, null, null);
    pastTimeStiples = new TexturePaint(bi, patternRect);

    // Create style for current time indicator
    timeLineStroke = new BasicStroke(3.0f);
  }

  public long getVirtualXInterval() { return myVirtualXInterval; }

  public long getVirtualXOrigin() { return myVirtualXOrigin; }

  public int getYInterval() { return myYInterval; }

  public void paint( Graphics g )
  {
    g.setColor( getForeground() );

    // Get the Java 2D Graphics context
    Graphics2D g2 = (Graphics2D) g;

    float lineX = (float)ScreenXOfVirtualX(myCurrTimeVX);

    // Past-time Stiples
    if (lineX >= 0.0f) {
      float stopX = lineX;
      if (stopX > (float)(getSize().width - 1))
	stopX = (float)(getSize().width - 1);

      // Draw the stiples
      Paint oldPaint = g2.getPaint();
      g2.setPaint(pastTimeStiples);
      g2.fill(new Rectangle2D.Float(0.0f, 0.0f, stopX, (float)(getSize().height-1)));
      g2.setPaint(oldPaint);
    }

    // Row separators
    for( int y=0; y<getSize().height; y+=getYInterval() )
      g.drawLine(0, y, getSize().width-1, y);
		
    long screenLeft = getVirtualXLocation() - getVirtualXOrigin();
    final long firstVX = screenLeft <= 0
      ? screenLeft / getVirtualXInterval() * getVirtualXInterval()
      : ((screenLeft / getVirtualXInterval()) + 1) * getVirtualXInterval();
    final long lastVX = screenLeft + getVirtualXSize();

    // Vertical time segment lines
    for( long vx=firstVX; vx<lastVX; vx+=getVirtualXInterval() )
      {
	final int x = ScreenXOfVirtualX( vx + getVirtualXOrigin() );
	g.drawLine(x, 0, x, getSize().height-1);
      }

    // Draw current-time line
    if (lineX >= 0.0 && lineX <= (float)(getSize().width - 1)) {
      Stroke oldStroke = g2.getStroke();
      g2.setStroke(timeLineStroke);
      g2.draw(new Line2D.Float(lineX, 0.0f, lineX, (float)(getSize().height-1)));
      g2.setStroke(oldStroke);
    }
  }

  public void setCurrTimeVX( long x ) {
    myCurrTimeVX = x;
    repaint();
  }

  public void setVirtualXInterval( long n ) { myVirtualXInterval = n; }

  public void setVirtualXOrigin( long o ) { myVirtualXOrigin = o; }

  public void setYInterval( int n ) { myYInterval = n; }
}
