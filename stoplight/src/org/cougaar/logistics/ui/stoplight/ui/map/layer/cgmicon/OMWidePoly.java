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

public class OMWidePoly extends OMPoly
{
  private float lineWidth = 1.0f;

  public OMWidePoly()
  {
      super();
  }

  public OMWidePoly(float af[], int i, int j)
  {
      super(af, i, j);
  }

  public OMWidePoly(float af[], int i, int j, int k)
  {
      super(af, i, j, k);
  }

  public OMWidePoly(int ai[])
  {
      super(ai);
  }

  public OMWidePoly(int ai[], int ai1[])
  {
      super(ai, ai1);
  }

  public OMWidePoly(float f, float f1, int ai[], int i)
  {
      super(f, f1, ai, i);
  }

  public OMWidePoly(float f, float f1, int ai[], int ai1[], int i)
  {
      super(f, f1, ai, ai1, i);
  }

  public void setWidth(float width)
  {
    lineWidth = width;
  }

  public void render(Graphics g)
  {
    if(getNeedToRegenerate())
        return;

    Stroke s = null;
    if (g instanceof Graphics2D)
    {
      s = ((Graphics2D)g).getStroke();
      ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
    }

    int ai[][] = xpoints;
    int ai1[][] = ypoints;
    int i = ai.length;
    for(int j = 0; j < i; j++)
    {
        int ai2[] = ai[j];
        int ai3[] = ai1[j];
        if(isPolygon)
        {
//            if(setPaint(g))
            g.fillPolygon(ai2, ai3, ai2.length);
            g.setColor(getDisplayColor());
            g.drawPolyline(ai2, ai3, ai2.length);
        }
        else
        {
            g.setColor(getDisplayColor());
            g.drawPolyline(ai2, ai3, ai2.length);
        }
    }


    if (g instanceof Graphics2D)
    {
      ((Graphics2D)g).setStroke(s);
    }
  }
}


