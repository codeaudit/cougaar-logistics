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
import java.io.*;

/***********************************************************************************************************************
<b>Description</b>: Stackable bar chart data set.  Extension of the DataSet to render a set of bar charts stacked (or
                    summed on each bar location (X point value) within each bar data set.

***********************************************************************************************************************/
public class StackableBarDataSet extends DataSet
{
  private Vector dataSetList = new Vector(0);

  private ColorProducer colorProducer = null;

  private Axis yaxisRight = null;

  /*********************************************************************************************************************
  <b>Description</b>: Width of every bar in the set of bar data (in user defined scale) to draw.

  <br><br><b>Notes</b>:<br>
                    - A bar 1/2 of this width is drawn on either side of the point specifed to keep the center of the
                      total bar aligned with the X value of the specified point
  *********************************************************************************************************************/
  public double barWidth = 1.0;

  /*********************************************************************************************************************
  <b>Description</b>: Constructor for StackableBarDataSet.

  <br>
  @param graph Graph this data set (and all contained data sets) is to be attached to
  @param xaxis X-axis this data set (and all contained data sets) is to be attached to
  @param yaxisLeft Y-axis this data set (and all contained data sets) is to be attached to
  @param yaxisRight Y-axis this data set (and all contained data sets) is to be attached to
  @param width Width of the bars to be rendered

  *********************************************************************************************************************/
  public StackableBarDataSet(Graph2D graph, Axis xaxis, Axis yaxisLeft, Axis yaxisRight, double width, ColorProducer producer)
  {
    graph.attachDataSet(this);
    xaxis.attachDataSet(this);
    yaxisLeft.attachDataSet(this);
    yaxisRight.attachDataSet(this);

    this.yaxisRight = yaxisRight;

    barWidth = width;
    colorProducer = producer;
  }

  /*********************************************************************************************************************
  <b>Description</b>: Adds a bar data set to the top of the stack.

  <br><b>Notes</b>:<br>
                    - While the data set is not required to have a bar location (X point value) for every bar location
                      in the other bar data sets, it should have unique bar locations (X point values) within its own
                      data set (this is not enforced)
                    - Once the data set is added, it should not be added directly to the graph to be rendered

  <br>
  @param dataSet Bar data set to add
  *********************************************************************************************************************/
  public void addDataSet(BarDataSet dataSet)
  {
    if ((dataSet.automaticallySetColor) && (colorProducer != null))
    {
      dataSet.linecolor = colorProducer.getColor(dataSet.linecolor, dataSet.colorNumber);
    }

    g2d.attachDataSet(dataSet);
    xaxis.attachDataSet(dataSet);
    yaxis.attachDataSet(dataSet);
    yaxisRight.attachDataSet(dataSet);
    dataSetList.add(dataSet);
    dataSet.stacked = true;

//     Update the range on Axis that this data is attached to
   range(stride);
   if(xaxis != null) xaxis.resetRange();
   if(yaxis != null) yaxis.resetRange();
   if(yaxisRight != null) yaxisRight.resetRange();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Removes a bar data set from the stack.

  <br><b>Notes</b>:<br>
                    - The removed data set will be restored such that it may be directly attached to the graph and
                      rendered independently of the stack chart (it's orignal width is stored)

  <br>
  @param dataSet Bar data set to remove
  *********************************************************************************************************************/
  public void removeDataSet(BarDataSet dataSet)
  {
    g2d.detachDataSet(dataSet);
    xaxis.detachDataSet(dataSet);
    yaxis.detachDataSet(dataSet);
    yaxisRight.detachDataSet(dataSet);
    dataSetList.remove(dataSet);
    dataSet.stacked = false;

//     Update the range on Axis that this data is attached to
   range(stride);
   if(xaxis != null) xaxis.resetRange();
   if(yaxis != null) yaxis.resetRange();
   if(yaxisRight != null) yaxisRight.resetRange();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Removes a bar data set from the stack.

  <br><b>Notes</b>:<br>
                    - The removed data set will be restored such that it may be directly attached to the graph and
                      rendered independently of the stack chart (it's orignal width is stored)

  <br>
  @param position Position in the stack to remove the bar data set from
  *********************************************************************************************************************/
  public void removeDataSet(int position)
  {
    removeDataSet((BarDataSet)dataSetList.remove(position));
  }

  /*********************************************************************************************************************
  <b>Description</b>: Reattaches the stack data set to the specified graph/xaxis/yaxis once it is removed from its
                      original graph/xaxis/yaxis.

  <br><b>Notes</b>:<br>
                    - This method must be used to attach a stack data set to a graph/xaxis/yaxis

  <br>
  @param graph Graph this data set (and all contained data sets) is to be attached to
  @param xaxis X-axis this data set (and all contained data sets) is to be attached to
  @param yaxis Y-axis this data set (and all contained data sets) is to be attached to
  *********************************************************************************************************************/
  public void reAttachBarBataSets(Graph2D graph, Axis xaxis, Axis yaxis, Axis yaxisRight, ColorProducer producer)
  {
    graph.attachDataSet(this);
    xaxis.attachDataSet(this);
    yaxis.attachDataSet(this);
    yaxisRight.attachDataSet(this);

    this.yaxisRight = yaxisRight;

    colorProducer = producer;

    // Attach the internal data sets to the graph and axes
    BarDataSet dataSet = null;
    for (int i=0, isize=dataSetList.size(); i<isize; i++)
    {
      dataSet = (BarDataSet)dataSetList.elementAt(i);
      g2d.attachDataSet(dataSet);
      xaxis.attachDataSet(dataSet);
      yaxis.attachDataSet(dataSet);
      yaxisRight.attachDataSet(dataSet);
    }
  }

  /*********************************************************************************************************************
  <b>Description</b>: Loads the specified data file.  The format is a comma separated file, the first line must be the
                      space between values (i.e. 5 (or 5.0) means the data read in will have bar location (x-values) of
                      5.0, 10.0, 15.0, 20.0, etc.).  Each line after contains height values (y-values) separated by
                      commas where non-data entries must have empty space commas (i.e. 1.2,3.4, ,5.4,5.6, etc.).


  <br><b>Notes</b>:<br>
                    - The easiest way to generate this file is to use MS Excel and save the data in .csv format

  <br>
  @param fileName Name of the file
  *********************************************************************************************************************/
  public void loadFile(String fileName) throws Exception
  {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line = null;
    double stepSize = 1.0;

    line = reader.readLine();
    stepSize = Double.parseDouble(line.substring(0, line.indexOf(",")));

    int pointCount = 0;
    String value = null;
    double[] data = null;
    String[] valueList = null;
    while ((line = reader.readLine()) != null)
    {
      valueList = tokenize(line, ",");
      data = new double[valueList.length*2];
      pointCount = 0;
      for (int i=0; i<valueList.length; i++)
      {
        if (valueList[i].trim().length() == 0)
        {
          continue;
        }

        pointCount++;

        data[i*2+0] = pointCount*stepSize;
        data[i*2+1] = Double.parseDouble(valueList[i]);
      }

      addDataSet(new BarDataSet(data, pointCount));
    }

    reader.close();

//     Update the range on Axis that this data is attached to
   range(stride);
   if(xaxis != null) xaxis.resetRange();
   if(yaxis != null) yaxis.resetRange();
   if(yaxisRight != null) yaxisRight.resetRange();
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
    DataSet dataSet = null;
    double yMax = Double.NaN;
    double y = 0.0;

    // Draw Step/Line filled
      for (int i=0, isize=dataSetList.size(); i<isize; i++)
    {
      dataSet = (DataSet)dataSetList.elementAt(i);
      if (dataSet.visible == false)
      {
        continue;
      }

      y = dataSet.getYmaxInRange(xMin, xMax);
      yMax = (((yMax < y) && (!Double.isNaN(y))) || Double.isNaN(yMax)) ? y : yMax;
    }

    return(yMax);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets the data set line/fill color to a new value supplied by the color producer.
	*********************************************************************************************************************/
  public void resetDataSetColors()
  {
    if (colorProducer != null)
    {
      DataSet dataSet = null;
      for (int i=0, isize=dataSetList.size(); i<isize; i++)
      {
        dataSet = (DataSet)dataSetList.elementAt(i);
        if (dataSet.automaticallySetColor)
        {
          dataSet.linecolor = colorProducer.getColor(dataSet.linecolor, dataSet.colorNumber);
        }
      }
    }
  }

  private String[] tokenize(String string, String delim)
  {
    Vector tokenList = new Vector(0);
    int index = 0;
    int lastIndex = 0;

    while ((index = string.indexOf(delim, lastIndex)) != -1)
    {
      tokenList.add(string.substring(lastIndex, index));
      lastIndex = index + delim.length();
    }

    tokenList.add(string.substring(lastIndex));

    return((String[])tokenList.toArray(new String[tokenList.size()]));
  }

  public void draw_data(Graphics g, Rectangle bounds)
  {
    if (!visible) return;

    try
    {
      Hashtable heightTable = new Hashtable(1);
      for (int i=0, isize=dataSetList.size(); i<isize; i++)
      {
        ((BarDataSet)dataSetList.elementAt(i)).setHeightTable(heightTable);
      }

      for (int i=dataSetList.size(); i>0; i--)
      {
        ((BarDataSet)dataSetList.elementAt(i-1)).drawData(g, bounds, barWidth);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  protected void range(int stride)
  {
    dxmin = 0.0;
    dxmax = 0.0;
    dymin = 0.0;
    dymax = 0.0;

    if ((dataSetList == null) || (dataSetList.size() == 0))
    {
      return;
    }

    DataSet dataSet = (DataSet)dataSetList.elementAt(0);
    dxmin = dataSet.getXmin();
    dxmax = dataSet.getXmax();
    dymin = dataSet.getYmin();
    dymax = dataSet.getYmax();

    for (int i=0, isize=dataSetList.size(); i<isize; i++)
    {
      dataSet = (DataSet)dataSetList.elementAt(i);

      if(dxmax < dataSet.getXmax())
      {
        dxmax = dataSet.getXmax();
      }
      else if(dxmin > dataSet.getXmin())
      {
        dxmin = dataSet.getXmin();
      }
    }

    Hashtable heightTable = new Hashtable(1);
    for (int i=0, isize=dataSetList.size(); i<isize; i++)
    {
      ((BarDataSet)dataSetList.elementAt(i)).setHeightTable(heightTable);
    }

    Double d = null;
    for (Enumeration e=heightTable.keys(); e.hasMoreElements();)
    {
      d = (Double)heightTable.get(e.nextElement());

      if(dymax < d.doubleValue())
      {
        dymax = d.doubleValue();
      }
      else if(dymin > d.doubleValue())
      {
        dymin = d.doubleValue();
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
    if(yaxisRight == null)
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
    DataSet ds;
    double a[] = new double[3];
    double distsq = -1.0;
    double data[] = {0.0, 0.0};

    for (int i=0, isize=dataSetList.size(); i<isize; i++)
    {
      ds = (DataSet)dataSetList.elementAt(i);

      if (!ds.visible) continue;

      a = ds.getClosestPoint(x, y, useOffset);

      if(distsq < 0.0 || distsq > a[2])
      {
        data[0] = a[0];
        data[1] = a[1];
        distsq  = a[2];
      }
    }

    return data;
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

    if (((barWidth/2) < Math.abs(point[0] - x)) || (0 > point[2]))
    {
      return(null);
    }

    return(point);
  }
}
