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
import java.applet.*;
import java.util.*;
import java.lang.*;

/***********************************************************************************************************************
<b>Description</b>: Bar chart data set.  Extension of the PolygonFillableDataSet to render a bar chart based on
                    a given data set.

***********************************************************************************************************************/
public class BarDataSet extends PolygonFillableDataSet
{
  protected double[] barData = new double[0];

  protected Hashtable heightTable = new Hashtable(1);

  protected boolean stacked = false;

  /*********************************************************************************************************************
  <b>Description</b>: Width of the bar (in user defined scale) to draw.

  <br><br><b>Notes</b>:<br>
                    - A bar 1/2 of this width is drawn on either side of the point specifed to keep the center of the
                      total bar aligned with the X value of the specified point
  *********************************************************************************************************************/
  public double barWidth = 1.0;

  /*********************************************************************************************************************
  <b>Description</b>: Constructor for a new empty bar data set.
  *********************************************************************************************************************/
  public BarDataSet()
  {
    // Set the fillable property to true (fills in the bar with the data set's color)
    super(true);
    adjustLineWidthInsets = true;

    calculateTemp();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Constructor for a new bar data set with the specifed bar data and color.

  <br><b>Notes</b>:<br>
                    - Data is appended as a single dimension array where index i would be the bar location (X value)
                      and index i+1 would be the height (Y value) (e.g. d[0] = bar x location, d[1] = bar height)
                    - The number of bar data points to get from the array is also passed in so more than one bar
                      can be added at a time

  <br>
  @param d Bar data array
  @param count Number of bar data points to read from the data array
  *********************************************************************************************************************/
  public BarDataSet(double d[], int count) throws Exception
  {
    // Set the fillable property to true (fills in the bar with the data set's color when the bar is rendered)
    super(true);
    adjustLineWidthInsets = true;
    // Add the bar data to the data set
    appendBarData(d, count);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Constructor that builds a data set of values based on the specified array and count of data
                      points, sets the polygonFill flag of this data set and sets the width of the data bars.

  <br>
  @param d Array of (x,y) points where d[n] is the x value and d[n+1] is the y value
  @param count Number of data point pairs in the array
  @param fill Indicates if this data set should use polygon fill for rendering
  @param width Indicates the width, in units, of the data bars
  *********************************************************************************************************************/
  public BarDataSet(double d[], int count, boolean fill, double width) throws Exception
  {
    // Set the fillable property to true (fills in the bar with the data set's color when the bar is rendered)
    super(fill);
    adjustLineWidthInsets = true;
    barWidth = width;

    // Add the bar data to the data set
    appendBarData(d, count);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns number of data points contained by the data set.

  @return Number of data points
	*********************************************************************************************************************/
  public int dataPoints()
  {
    return(barData.length/2);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the data point at a specified index.

  <br>
  @param index Index to retrieve data point from
  @return Double array of the data point at the index where double[0] = X value and double[1] = Y value, or null if
            the index does not exist
	*********************************************************************************************************************/
  public double[] getPoint(int index)
  {
    if ((index < 0) || (index >= (barData.length/2)))
    {
      return(null);
    }

    return(new double[] {data[index*2+0], data[index*2+1]});
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the maximum Y value within the specified X range.

  <br>
  @param xMin Minimum X value
  @param xMax Maximum X value
  @return Maximum Y value in the specified X range
	*********************************************************************************************************************/
  public double getYmaxInRange(double xMin, double xMax)
  {
    // Go through each data point in the range, comparing them to find the largest Y value
    double yMax = Double.NaN;
    for (int i=0; i<barData.length; i+=stride)
    {
      if ((xMin <= barData[i]) && (barData[i] <= xMax))
      {
        yMax = ((yMax < barData[i+1]) || (Double.isNaN(yMax))) ? barData[i+1] : yMax;
      }
    }

    return(yMax);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the data points of the data set.

  <br>
  @return Data points of the data set where double[n] = X value and double[n+1] = Y value
	*********************************************************************************************************************/
  public double[] getData()
  {
    return(barData);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Adds bar data points to the data set.

  <br><b>Notes</b>:<br>
                    - Data is appended as a single dimension array where index i would be the bar location (X value)
                      and index i+1 would be the height (Y value) (e.g. d[0] = bar x location, d[1] = bar height)
                    - The number of bar data points to get from the array is also passed in so more than one bar
                      can be added at a time

  <br>
  @param d Bar data array
  @param count Number of bar data points to read from the data array
  *********************************************************************************************************************/
  public void appendBarData(double d[], int count) throws Exception
  {
    // Resize the data array and add the new data to it
    int lastIndex = resizeBarData(barData.length + count*2);
    System.arraycopy(d, 0, barData, lastIndex, count*2);

//     Update the range on Axis that this data is attached to
    range(stride);
    if(xaxis != null) xaxis.resetRange();
    if(yaxis != null) yaxis.resetRange();

    calculateTemp();
  }

  protected int resizeBarData(int newSize)
  {
    // Create a new array with the new size and copy the old array's contents to it
    int previousSize = barData.length;
    double[] temp = new double[newSize];
    System.arraycopy(barData, 0, temp, 0, (newSize <= barData.length) ? newSize : barData.length);
    barData = temp;

    return(previousSize);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Changes bar height values in the data set.

  <br><b>Notes</b>:<br>
                    - Data is passed as a single dimension array where index i would be the bar location (X value)
                      and index i+1 would be the height (Y value) (e.g. d[0] = bar x location, d[1] = bar height)
                    - The number of bar data points to get from the array is calculated based on d.length/2

  <br>
  @param d Bar data array
  @see #changeBarData(double[], int)
  *********************************************************************************************************************/
  public void changeBarData(double d[])
  {
    changeBarData(d, d.length/2);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Changes bar height values in the data set.  This method will search through the current set of
                      bar data for X point values that match with each X point specifed in the change array (d[]) and,
                      if the X point value is found, will replace the corresponding Y point value with the changed
                      value.

  <br><b>Notes</b>:<br>
                    - Bar x location (X point) values should be unique across this data set, however, this restriction
                      is not enforced
                    - Data is passed as a single dimension array where index i would be the bar location (X value)
                      and index i+1 would be the height (Y value) (e.g. d[0] = bar x location, d[1] = bar height)
                    - The number of bar data points to get from the array is also passed in so more than one bar
                      can be added at a time

  <br>
  @param d Bar data array
  @param count Number of bar data points to read from the data array
  *********************************************************************************************************************/
  public void changeBarData(double d[], int count)
  {
    // For every data point, change the point's Y value in the data array
    for (int i=0; i<count; i++)
    {
      change(d[2*i+0], d[2*i+1]);
    }

//     Update the range on Axis that this data is attached to
    range(stride);
    if(xaxis != null) xaxis.resetRange();
    if(yaxis != null) yaxis.resetRange();

    calculateTemp();
  }

  protected void change(double x, double newY)
  {
    // Find the X value in the data array and change its corresponding Y value
    for (int i=0; i<barData.length; i+=2)
    {
      if (barData[i] == x)
      {
        barData[i+1] = newY;
        break;
      }
    }
  }

  protected void setHeightTable(Hashtable ht)
  {
    // Clone the height table and use it as this data set's current height table
    heightTable = (Hashtable)ht.clone();
    Double key = null;
    Double obj = null;
    double value = 0.0;

    if (!visible) return;

    // Add this data set's height (Y) values to the original height table
    for (int i=0; i<barData.length; i+=2)
    {
      // If the table has a value at the specified X value, get it, otherwise, use 0.0
      value = 0.0;
      key = new Double(barData[i]);
      if ((obj = (Double)ht.get(key)) != null)
      {
        value = obj.doubleValue();
      }

      // Add/change the new height (previous height plus the height value of the current point) to the height table
      ht.put(key, new Double(value + barData[i+1]));
    }
  }

  // Do this because this data set can be attached to the graph twice (directly and indirectly) when being stacked
  public void draw_data(Graphics g, Rectangle bounds)
  {
    if (!visible) return;

    // If this data set has the stacked flag set, it is part of a StackableBarDataSet and should not be drawn when this
    // method is called (this method is called because the data set must be attached to the graph, even if it is part of
    // a StackableBarDataSet, and, as such, causes this method to be called by the graph when it should not be called)
    if (!stacked)
    {
//      drawData(g, bounds, barWidth);
      super.draw_data(g, bounds);
    }
  }

  protected double[] temp = null;

  protected void calculateTemp()
  {
    temp = new double[barData.length*4];
    double width = barWidth/2.0;

    for (int i=0; i<barData.length; i+=2)
    {
      temp[i*4 + 0] = barData[i+0] - width;
      temp[i*4 + 1] = 0;
      temp[i*4 + 2] = barData[i+0] - width;
      temp[i*4 + 3] = barData[i+1];
      temp[i*4 + 4] = barData[i+0] + width;
      temp[i*4 + 5] = barData[i+1];
      temp[i*4 + 6] = barData[i+0] + width;
      temp[i*4 + 7] = 0;
    }
  }

  protected void draw_lines(Graphics g, Rectangle w)
  {
    if (barData.length == 0)
    {
      return;
    }

    draw_lines(g, w, temp, temp.length, 0.0);
  }

  // For stacking bars
  public void drawData(Graphics g, Rectangle bounds, double width)
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

    if( linestyle != DataSet.NOLINE )
    {
      draw_lines(g, bounds, width);
    }

    g.setColor(c);
  }

  protected void draw_lines(Graphics g, Rectangle w, double width)
  {
    if (barData.length == 0)
    {
      return;
    }

    double[] temp = new double[barData.length*4];
    width = width/2.0;

    double height = 0.0;
    Double obj = null;

    for (int i=0; i<barData.length; i+=2)
    {
      height = 0.0;
      if ((stacked) && ((obj = (Double)heightTable.get(new Double(barData[i]))) != null))
      {
        height = obj.doubleValue();
      }

      temp[i*4 + 0] = barData[i+0] - width;
      temp[i*4 + 1] = 0;
      temp[i*4 + 2] = barData[i+0] - width;
      temp[i*4 + 3] = barData[i+1];
      temp[i*4 + 4] = barData[i+0] + width;
      temp[i*4 + 5] = barData[i+1];
      temp[i*4 + 6] = barData[i+0] + width;
      temp[i*4 + 7] = 0;
    }

    draw_lines(g, w, temp, temp.length, height);
  }

  protected void range(int stride)
  {
    dxmin = 0.0;
    dxmax = 0.0;
    dymin = 0.0;
    dymax = 0.0;

    if ((barData == null) || (barData.length < 2))
    {
      return;
    }

    dxmin = dxmax = barData[0];
    dymin = dymax = barData[1];

    for (int i=0; i<barData.length; i+=2)
    {
      if(dxmax < barData[i])
      {
        dxmax = barData[i];
      }
      else if(dxmin > barData[i])
      {
        dxmin = barData[i];
      }

      if(dymax < barData[i+1])
      {
        dymax = barData[i+1];
      }
      else if(dymin > barData[i+1])
      {
        dymin = barData[i+1];
      }
    }

    if(xaxis == null)
    {
      xmin = dxmin;
      xmax = dxmax;
    }
    if(yaxis == null)
    {
      ymin = dymin;
      ymax = dymax;
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the closest point to the specified coordinates.

  <br>
  @param x X coordinate
  @param y Y Coordinate
  @return Closest point to the specified coordinates as a double array of double[0] = X value, double[1] = Y value,
            double[2] = distance where distance will be -1 if no point was found
	*********************************************************************************************************************/
  public double[] getClosestPoint(double x, double y)
  {
    return(getClosestPoint(x, y, false));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the closest point to the specified coordinates specifying whether or not to use the data
                      set's offset value (if it has one).

  <br>
  @param x X coordinate
  @param y Y Coordinate
  @param useOffset Use data set offset
  @return Closest point to the specified coordinates as a double array of double[0] = X value, double[1] = Y value,
            double[2] = distance where distance will be -1 if no point was found
	*********************************************************************************************************************/
  public double[] getClosestPoint(double x, double y, boolean useOffset)
  {
    double xOffset = 0.0;
    double yOffset = 0.0;
    if (useOffset)
    {
      xOffset = xValueOffset;
      yOffset = yValueOffset;
    }

    double point[] = {0.0, 0.0, -1.0};
    int i;
    double xdiff, ydiff;

    if (barData.length == 0) return(point);

    double lastXDiff = -1;
    for(i=0; i<barData.length-1; i+=stride)
    {
      xdiff = Math.abs(barData[i  ] + xOffset - x);
      ydiff = barData[i+1] + yOffset - y;

      if (((xdiff < lastXDiff) || (lastXDiff == -1)) && (barData[i+1] > y))
      {
        point[0] = barData[i  ];
        point[1] = barData[i+1];
        point[2] = xdiff*xdiff + ydiff*ydiff;
      }

      lastXDiff = xdiff;
    }

    return(point);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the closest point to the specified coordinates within the specified maximum squared
                      distance specifying whether or not to use the data set's offset value (if it has one).

  <br>
  @param x X coordinate
  @param y Y Coordinate
  @param maxDist2 Maximum squared distance of the closest point
  @param useOffset Use data set offset
  @return Closest point to the specified coordinates, or null if there is no such point
	*********************************************************************************************************************/
  public double[] getClosestPoint(double x, double y, double maxDist2, boolean useOffset)
  {
    double point[] = getClosestPoint(x, y, useOffset);

    double xOffset = 0.0;
    double yOffset = 0.0;
    if (useOffset)
    {
      xOffset = xValueOffset;
      yOffset = yValueOffset;
    }

    if (((barWidth/2) < Math.abs(point[0] + xOffset - x)) || (0 > point[2]))
    {
      return(null);
    }

    return(point);
  }
}
