/* **********************************************************************
 *
 *  Clark Software Engineering, Ltd.
 *  5100 Springfield St. Ste 308
 *  Dayton, OH 45431-1263
 *  (937) 256-7848
 *
 *  Copyright (C) 2001
 *  This software is subject to copyright protection under the laws of
 *  the United States and other countries.
 *
 */

package org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon;

import java.awt.*;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.util.Debug;

public class OMWideLine extends OMLine
{
  private float lineWidth = 1.0f;

  public OMWideLine()
  {
  }

  public OMWideLine(float f, float f1, float f2, float f3, int i)
  {
      super(f, f1, f2, f3, i);
  }

  public OMWideLine(float f, float f1, float f2, float f3, int i, int j)
  {
      super(f, f1, f2, f3, i, j);
  }

  public OMWideLine(int i, int j, int k, int l)
  {
      super(i, j, k, l);
  }

  public OMWideLine(float f, float f1, int i, int j, int k, int l)
  {
      super(f, f1, i, j, k, l);
  }

  public void setWidth(float width)
  {
    lineWidth = width;
  }

  public void render(Graphics g)
  {
    Stroke s = null;
    if (g instanceof Graphics2D)
    {
      s = ((Graphics2D)g).getStroke();
      ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
    }


    if(getNeedToRegenerate())
        return;
    g.setColor(getDisplayColor());
    int ai[][] = xpoints;
    int ai1[][] = ypoints;
    for(int i = 0; i < ai.length; i++)
        g.drawPolyline(ai[i], ai1[i], ai[i].length);

    if(doArrowHead)
    {
//        for(int j = 0; j < arrowhead.length; j++)
//            arrowhead[j].draw(g);
        throw (new UnsupportedOperationException("Arrowhead not implemented"));
    }
    if(Debug.debugging("arc") && arc != null)
    {
        OMGraphicList omgraphiclist = arc.getArcGraphics();
        Debug.output("OMLine rendering " + omgraphiclist.size() + " arcGraphics.");
        omgraphiclist.render(g);
    }


    if (g instanceof Graphics2D)
    {
      ((Graphics2D)g).setStroke(s);
    }
  }

  public int[][] getXpoints()
  {
    return xpoints;
  }

  public int[][] getYpoints()
  {
    return ypoints;
  }
  
}


