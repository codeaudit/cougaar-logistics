/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.logistics.ui.stoplight.ui.components.graph;

import java.awt.*;
import java.text.*;

/***********************************************************************************************************************
<b>Description</b>: Polygon fillable chart data set.  Extension of the DataSet to render a line chart based on a given
                    data set and provide functionality to fill in below the line with the color of the data set.

***********************************************************************************************************************/
public class PolygonFillableDataSet extends DataSet
{
  /*********************************************************************************************************************
  <b>Description</b>: Flag to indicate if the data set should use polygon filling to produce a solid graph rendering
                      instead of a line (wire frame) rendering of the graph.

  <br><br><b>Notes</b>:<br>
                    - polygonFill is false by default
  *********************************************************************************************************************/
  public boolean polygonFill = false;

  /*********************************************************************************************************************
  <b>Description</b>: Flag to indicate if the data set should use a fill pattern to produce a stripped graph rendering
                      instead of a line (wire frame) rendering of the graph.

  <br><br><b>Notes</b>:<br>
                    - useFillPattern is false by default
  *********************************************************************************************************************/
  public boolean useFillPattern = false;

  /*********************************************************************************************************************
  <b>Description</b>: Color of fill pattern stripe.

  <br><br><b>Notes</b>:<br>
                    - fillPatternColor is Color.white by default
  *********************************************************************************************************************/
  public Color fillPatternColor = Color.white;

  /*********************************************************************************************************************
  <b>Description</b>: Distance of fill pattern stripes from each other in pixels.

  <br><br><b>Notes</b>:<br>
                    - distanceBetweenStripes is 8 pixels by default
  *********************************************************************************************************************/
  public int distanceBetweenStripes = 8;

  /*********************************************************************************************************************
  <b>Description</b>: Thickness of fill pattern stripes in pixels.

  <br><br><b>Notes</b>:<br>
                    - stripeThickness is 4 pixels by default
  *********************************************************************************************************************/
  public int stripeThickness = 4;

  /*********************************************************************************************************************
  <b>Description</b>: Adjusts plot locations based on drawn line thickness.

  <br><br><b>Notes</b>:<br>
                    - adjustLineWidthInsets is false by default
  *********************************************************************************************************************/
  public boolean adjustLineWidthInsets = false;

  protected int[] xPoints = new int[0];
  protected int[] yPoints = new int[0];

  public int patternArrayIncrement = 512;
  protected int[] xPatternPoints = new int[patternArrayIncrement];
  protected int[] yPatternPoints = new int[patternArrayIncrement];
  protected int patternPointsCount = 0;

  /*********************************************************************************************************************
  <b>Description</b>: Default constructor.
  *********************************************************************************************************************/
  public PolygonFillableDataSet()
  {
    super();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Constructor that builds a data set of values based on the specified array and count of data
                      points and sets the polygonFill flag of this data set.

  <br>
  @param d Array of (x,y) points where d[n] is the x value and d[n+1] is the y value
  @param n Number of data point pairs in the array
  @param fill Indicates if this data set should use polygon fill for rendering
  *********************************************************************************************************************/
  public PolygonFillableDataSet(double d[], int n, boolean fill) throws Exception
  {
    super(d, n);
    polygonFill = fill;
  }

  /*********************************************************************************************************************
  <b>Description</b>: Constructor that sets the polygonFill flag of this data set.

  <br>
  @param fill Indicates if this data set should use polygon fill for rendering
  *********************************************************************************************************************/
  public PolygonFillableDataSet(boolean fill)
  {
    super();
    polygonFill = fill;
  }

  public void draw_data(Graphics g, Rectangle bounds)
  {
    if (!visible) return;

    if (xaxis != null)
    {
      xmax = xaxis.maximum;
      xmin = xaxis.minimum;
    }

    if (yaxis != null)
    {
      ymax = yaxis.maximum;
      ymin = yaxis.minimum;
    }

    xrange = xmax - xmin;
    yrange = ymax - ymin;

    if (clipping) g.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);

    Color c = g.getColor();

    if (linecolor != null)
    {
      g.setColor(linecolor);
    }

    if((linestyle != DataSet.NOLINE) && (yrange > 0.0))
    {
      draw_lines(g, bounds);
    }

    g.setColor(c);
  }

  protected void draw_lines(Graphics g, Rectangle w)
  {
    draw_lines(g, w, data, length, 0.0);
  }

  protected void draw_lines(Graphics g, Rectangle w, double[] pointData, int dataLength, double height)
  {
    int i;
    int j;
    boolean inside0 = false;
    boolean inside1 = false;
    double x,y;
    int x0 = 0 , y0 = 0;
    int x1 = 0 , y1 = 0;
    //     Calculate the clipping rectangle
    Rectangle clip = g.getClipBounds();
    int xcmin = clip.x;
    int xcmax = clip.x + clip.width;
    int ycmin = clip.y;
    int ycmax = clip.y + clip.height;

    //    Is there any data to draw? Sometimes the draw command will
    //    will be called before any data has been placed in the class.
    if ( pointData == null || dataLength < stride )
    {
      return;
    }

    int pointCount = 0;
    patternPointsCount = 0;
    
    if (xPoints.length < pointData.length*2)
    {
      xPoints = new int[pointData.length*2];
      yPoints = new int[pointData.length*2];
    }

    //    Is the first point inside the drawing region ?
    if ( (inside0 = inside(pointData[0] + xValueOffset, pointData[1] + yValueOffset)) )
    {
      x0 = (int)(w.x + ((pointData[0] + xValueOffset - xmin)/xrange)*w.width);
      y0 = (int)(w.y + (1.0 - (pointData[1] + yValueOffset + height - ymin)/yrange)*w.height);

/*      if (polygonFill)
      {
        // (y0 < ycmin) can be true but the total filled polygon can still be inside
        if (x0 < xcmin || x0 > xcmax || y0 > ycmax)
        {
          inside0 = false;
        }
      }
      else
      {
        if (x0 < xcmin || x0 > xcmax || y0 < ycmin || y0 > ycmax)
        {
          inside0 = false;
        }
      }*/
      if (polygonFill)
      {
        if (y0 > ycmax)
        {
          inside0 = false;
        }
      }
    }


    for (i=stride; i<dataLength; i+=stride)
    {
      //        Is this point inside the drawing region?
      inside1 = inside( pointData[i] + xValueOffset, pointData[i+1] + yValueOffset);


// #CSE# 02/15/2001 Fix line through rectangle whose points are ouside rectangle
      if (!inside0 && !inside1 && insideRect(pointData[i-stride] + xValueOffset, pointData[i-stride+1] + yValueOffset, pointData[i] + xValueOffset, pointData[i+1] + yValueOffset))
      {
        inside0 = true;

        x0 = (int)(w.x + ((pointData[i-stride] + xValueOffset - xmin)/xrange)*w.width);
        y0 = (int)(w.y + (1.0 - (pointData[i-stride+1]  + yValueOffset - ymin)/yrange)*w.height);
      }


      //        If one point is inside the drawing region calculate the second point
      if ( inside1 || inside0 )
      {
        x1 = (int)(w.x + ((pointData[i] + xValueOffset - xmin)/xrange)*w.width);
        y1 = (int)(w.y + (1.0 - (pointData[i+1] + yValueOffset + height - ymin)/yrange)*w.height);
/*
        if (polygonFill)
        {
          // (y1 < ycmin) can be true but the total filled polygon can still be inside
          if (x1 < xcmin || x1 > xcmax || y1 > ycmax)
          {
            inside1 = false;
          }
        }
        else
        {
          if ( x1 < xcmin || x1 > xcmax || y1 < ycmin || y1 > ycmax)
          {
            inside1 = false;
          }
        }*/

        if (polygonFill)
        {
          if (y1 > ycmax)
          {
            inside1 = false;
          }
        }
      }

      //        If the second point is inside calculate the first point if it
      //        was outside
      if ( !inside0 && inside1 )
      {
        x0 = (int)(w.x + ((pointData[i-stride] + xValueOffset - xmin)/xrange)*w.width);
        y0 = (int)(w.y + (1.0 - (pointData[i-stride+1] + yValueOffset + height - ymin)/yrange)*w.height);
      }

      //        If either point is inside draw the segment
      if ( inside0 || inside1 )
      {
        if (polygonFill)
        {
          // Since we're poloygon filling vertical point pairs are "lost in the fill" and not needed so we can
          // reduce the total number of points by half if we don't include these extra pixel point pairs
          if (x0 != x1)
          {
            xPoints[pointCount] = x0;
            yPoints[pointCount] = ycmax;
            pointCount++;

            xPoints[pointCount] = x0;
            yPoints[pointCount] = y0;
            pointCount++;

            xPoints[pointCount] = x1;
            yPoints[pointCount] = y1;
            pointCount++;

            xPoints[pointCount] = x1;
            yPoints[pointCount] = ycmax;
            pointCount++;

          }
        }
        else
        {
          // A quick hack EBM
          if (adjustLineWidthInsets)
          {
            if (y0>y1)
            {
              xPoints[pointCount] = x0+(int)((lineThickness)/2);
              yPoints[pointCount] = y0;
              pointCount++;

              xPoints[pointCount] = x1+(int)((lineThickness)/2);
              yPoints[pointCount] = y1;
              pointCount++;
            }
            else if (y0<y1)
            {
              xPoints[pointCount] = x0-(int)((lineThickness+1)/2);
              yPoints[pointCount] = y0;
              pointCount++;

              xPoints[pointCount] = x1-(int)((lineThickness+1)/2);
              yPoints[pointCount] = y1;
              pointCount++;
            }
            else if (x0<x1)
            {
              xPoints[pointCount] = x0+(int)((lineThickness)/2);
              yPoints[pointCount] = y0;
              pointCount++;

              xPoints[pointCount] = x1-(int)((lineThickness+1)/2);
              yPoints[pointCount] = y1;
              pointCount++;
            }
            else if (x0>x1)
            {
              xPoints[pointCount] = x0-(int)((lineThickness+1)/2);
              yPoints[pointCount] = y0;
              pointCount++;

              xPoints[pointCount] = x1+(int)((lineThickness)/2);
              yPoints[pointCount] = y1;
              pointCount++;
            }
          }
          else
          {
            xPoints[pointCount] = x0;
            yPoints[pointCount] = y0;
            pointCount++;

            xPoints[pointCount] = x1;
            yPoints[pointCount] = y1;
            pointCount++;
          }
        }
      }

      /*
      **        The reason for the convolution above is to avoid calculating
      **        the points over and over. Now just copy the second point to the
      **        first and grab the next point
      */
      inside0 = inside1;
      x0 = x1;
      y0 = y1;
    }
//System.println();
    // No points to draw
    if (pointCount == 0)
    {
//      System.out.println("No points to draw: " + dataName);
      return;
    }

    if (polygonFill)
    {
      g.fillPolygon(xPoints, yPoints, pointCount);

      // If we're using a fill pattern, apply the pattern
      if (useFillPattern)
      {
        Color c = g.getColor();
        g.setColor(fillPatternColor);

        for (int ii=0; ii<pointCount; ii+=4)
        {
          drawPattern(g, xPoints[ii+0], yPoints[ii+0], xPoints[ii+1], yPoints[ii+1], xPoints[ii+2], yPoints[ii+2], xPoints[ii+3], yPoints[ii+3], ycmax, ycmin);
        }

        g.setColor(c);
      }
    }
    else
    {
      drawLine(xPoints, yPoints, pointCount, g);
    }
  }

// EBM: Break glass in case of emergency
/*
  // This methods will mutate the values of the int arrays passed in
  protected void drawPattern(int[] x, int[] y, int ycmax, int ycmin, Graphics g)
  {
    int y0pixel = y[2] + 10;
    int y1pixel = y[2];
    
    // Move this to outside loop, or pass in as values
    stripeThickness = (int)((yaxis.amax.y - yaxis.amin.y)*0.18);
    distanceBetweenStripes = (int)((yaxis.amax.y - yaxis.amin.y)*0.36);

    if (distanceBetweenStripes == 0)
    {
      return;
    }

    do
    {
      y[0] = y0pixel + stripeThickness;
      y[1] = y0pixel;
      y[2] = y1pixel;
      y[3] = y1pixel + stripeThickness;
      if (y0pixel >= ycmin)
      {
        g.fillPolygon(x, y, 4);
      }
      y0pixel += distanceBetweenStripes;
      y1pixel += distanceBetweenStripes;
    }
    while (y1pixel < ycmax);
  }*/

  // This method will mutate the values of the int arrays passed in
/*  protected void drawPattern(int[] x, int[] y, int ycmax, int ycmin, Graphics g)
  {
    int y0pixel = y[1];
    int y1pixel = y[2];
    
    int yStep = (distanceBetweenStripes + stripeThickness);
    do
    {
      y0pixel += yStep;
      y[0] = y0pixel;
      y[1] = y0pixel-stripeThickness;
      y[2] = y1pixel;
      y[3] = y1pixel+stripeThickness;
      if (y0pixel >= ycmin)
      {
        g.fillPolygon(x, y, 4);
      }
      y1pixel += yStep;
    }
    while (y0pixel < ycmax);
  }*/

  // That's not natural ... but Oak Express is
  protected void drawPattern(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, int ycmax, int ycmin)
  {
    int y0pixel = y1;
    int y1pixel = y2;

    int yStep = (distanceBetweenStripes + stripeThickness);
    patternPointsCount = 0;
    do
    {
      y0pixel += yStep;
      if (y0pixel >= ycmin)
      {
        if (xPatternPoints.length < (patternPointsCount + 6))
        {
          int[] temp = xPatternPoints;
          xPatternPoints = new int[temp.length + patternArrayIncrement];
          System.arraycopy(temp, 0, xPatternPoints, 0, patternPointsCount);

          temp = yPatternPoints;
          yPatternPoints = new int[temp.length + patternArrayIncrement];
          System.arraycopy(temp, 0, yPatternPoints, 0, patternPointsCount);
        }

        xPatternPoints[patternPointsCount] = x0;
        yPatternPoints[patternPointsCount] = y0pixel;
        patternPointsCount++;

        xPatternPoints[patternPointsCount] = x1;
        yPatternPoints[patternPointsCount] = y0pixel-stripeThickness;
        patternPointsCount++;

        xPatternPoints[patternPointsCount] = x2;
        yPatternPoints[patternPointsCount] = y1pixel;
        patternPointsCount++;

        xPatternPoints[patternPointsCount] = x3;
        yPatternPoints[patternPointsCount] = y1pixel+stripeThickness;
        patternPointsCount++;

        xPatternPoints[patternPointsCount] = x0;
        yPatternPoints[patternPointsCount] = y0pixel;
        patternPointsCount++;
      }

      y1pixel += yStep;
    }
    while (y0pixel < ycmax);
    
    g.fillPolygon(xPatternPoints, yPatternPoints, patternPointsCount);
  }

  protected void drawLine(int[] x, int[] y, int pointCount, Graphics g)
  {
    if (g instanceof Graphics2D)
    {
      Graphics2D graphics2D = (Graphics2D)g;
      Stroke stroke = graphics2D.getStroke();
      BasicStroke newStroke = new BasicStroke(lineThickness);
      graphics2D.setStroke(newStroke);

      g.drawPolyline(x, y, pointCount);

      graphics2D.setStroke(stroke);
    }
    else
    {
      g.drawPolyline(x, y, pointCount);
    }
  }

  protected boolean inside(double x, double y)
  {
    if (polygonFill)
    {
      // (y <= ymax) can be false but the total filled polygon can still be inside
      if ((x >= xmin) && (x <= xmax) && (y >= ymin))
      {
        return true;
      }
    }
    else
    {
      if ((x >= xmin) && (x <= xmax) && (y >= ymin) && (y <= ymax))
      {
        return true;
      }
    }

    return false;
  }
}
