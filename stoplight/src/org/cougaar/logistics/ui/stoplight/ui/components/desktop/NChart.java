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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.awt.*;
import java.util.*;
import java.awt.datatransfer.*;


import javax.swing.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;
//import NChartApplet;


/***********************************************************************************************************************
<b>Description</b>: NChart multiple CChart with test data example.

<br><br><b>Notes</b>:<br>
									Builds a 4 chart frame.

***********************************************************************************************************************/

public class NChart extends javax.swing.JPanel implements PropertyChangeListener
{
	/*********************
  ** Private Variables
  **********************/
  /*********************************************************************************************************************
  <b>Description</b>: double holds the default minmax for x axis.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************************************/
	private double[] xMinMax = {0.0, 200.0};
	
	/*********************************************************************************************************************
  <b>Description</b>: Pointer to NChartUI - used to call back to NChartUI to change the menus when changes to chart occur.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private NChartUI nChartUI = null;
	
	
	
	/*********************************************************************************************************************
  <b>Description</b>: Pointer to NChartApplet - used to call back to NChartApplet to change the menus when changes to chart occur.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private ReadinessChartApplet rChartApplet = null;
	
	/*********************************************************************************************************************
  <b>Description</b>: Pointer to NChartApplet - used to call back to NChartApplet to change the menus when changes to chart occur.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private NChartApplet nChartApplet = null;
	
	/*********************************************************************************************************************
  <b>Description</b>: boolean scroll lock flag.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************************************/
	private boolean xRangeScrollLock = false;
	
	/*********************************************************************************************************************
  <b>Description</b>: double.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************************************/
  private double xScrollSize = 0.0;
  
  /*********************************************************************************************************************
  <b>Description</b>: double.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************************************/
  private double additionalSpace = 0.10;
  
  /*********************************************************************************************************************
  <b>Description</b>: int number of charts to lay out.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private int totalCharts;
	
	/*********************************************************************************************************************
  <b>Description</b>: String holds the x axis label.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	//public CChart mainChart = null;
	
	/*********************************************************************************************************************
  <b>Description</b>: Array of CChart objects.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************************************/
	public CChart[] chartElement;
	
	/*********************************************************************************************************************
  <b>Description</b>: Array of CChart objects.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private CChart[] chartList = null;
	
	/*********************************************************************************************************************
  <b>Description</b>: Vector of DataSet objects on the charts.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	public Vector data = null;
	//private JPanel inventoryChartLegend = new JPanel();
	
	/*********************************************************************************************************************
  <b>Description</b>: JPanel pane which holds the charts.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private JPanel chartPanel = new JPanel(new GridBagLayout());
	
	/*********************************************************************************************************************
  <b>Description</b>: Thumb slider control.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private CMThumbSliderRangeControl xRC = new CMThumbSliderRangeControl(0.0f, 0.0f);
	
	/*********************************************************************************************************************
  <b>Description</b>: Boolean holds twoup flag (charts side by side in 2 rows as opposed to one under the other).

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	private boolean twoUp = true;
	
	/*
***********************
** Public Variables
**********************/

/*********************************************************************************************************************
  <b>Description</b>: String holds the x axis label.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	public String xAxisLabel = null;
/*********************************************************************************************************************
  <b>Description</b>: String holds the y axis label.

  <br><br><b>Notes</b>:<br>
                    - null by default
  *********************************************************************************************************************/
	public String yAxisLabel = null;
	
	// ---------------------------------------------------------------------------------------------------------------------
  // Public Constructors
  // ---------------------------------------------------------------------------------------------------------------------
	/*********************************************************************************************************************
  <b>Description</b>: Constructor called by NChartUI

  <br><b>Notes</b>:<br>
	                  -Sets up the Gridbag Layout

  <br>

	*********************************************************************************************************************/
	
  public NChart(int numberOfCharts, String xLabel, String yLabel, NChartUI uiPtr)
  {
  	nChartUI = uiPtr;
  	   
  	yAxisLabel = yLabel;
  	xAxisLabel = xLabel;
  	setLayout(new BorderLayout());
  	totalCharts = numberOfCharts;
  	chartElement = new CChart[numberOfCharts];
  	chartList = new CChart[numberOfCharts];
  	//buildLegends();
  	//System.out.println("%%%% nchart constructor " + numberOfCharts);
  	for(int i = 0; i < numberOfCharts; i++)
  	{
  		JPanel titlePanel = new JPanel();
  		JLabel titleLabel = new JLabel("Title");
  		chartElement[i] = new CChart("chart " + i, titlePanel, yAxisLabel, xAxisLabel, true);
  		//chartElement[i].setShowXRangeScroller(true);
  		chartElement[i].setShowXDividers(true);
  		chartElement[i].setXScrollerRange(new RangeModel(0, 200));
      chartElement[i].setXAxisSigDigitDisplay(1);
      chartElement[i].setToolTipDelay(0);
      chartList[i] = chartElement[i];
  		  		
  		GridBagConstraints constraints = new GridBagConstraints();

	    constraints.gridx = 0;
	    constraints.gridy = i;
	    constraints.gridwidth = 1;
	    constraints.gridheight = 1;
	    constraints.weightx = 2.0;
	    constraints.weighty = 2.0;
	    constraints.fill = GridBagConstraints.BOTH;
	    
	    chartPanel.add(chartElement[i], constraints);
	    
  	  
    }
    //xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
    //xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
    add(chartPanel, BorderLayout.CENTER);
	  //add(xRC, BorderLayout.SOUTH);
	  validate();
	  
  }
  
  
	/*********************************************************************************************************************
  <b>Description</b>: Constructor called by applet

  <br><b>Notes</b>:<br>
	                  -sets up Gridbag Layout

  <br>

	*********************************************************************************************************************/
  
  public NChart(int numberOfCharts, String xLabel, String yLabel, NChartApplet uiPtr)
  {
  	nChartApplet = uiPtr;
  	   
  	yAxisLabel = yLabel;
  	xAxisLabel = xLabel;
  	setLayout(new BorderLayout());
  	totalCharts = numberOfCharts;
  	chartElement = new CChart[numberOfCharts];
  	chartList = new CChart[numberOfCharts];
  	//buildLegends();
  	//System.out.println("%%%% nchart constructor " + numberOfCharts);
  	for(int i = 0; i < numberOfCharts; i++)
  	{
  		JPanel titlePanel = new JPanel();
  		JLabel titleLabel = new JLabel("Title");
  		chartElement[i] = new CChart("chart " + i, titlePanel, xAxisLabel, yAxisLabel, true);
  		//chartElement[i].setShowXRangeScroller(true);
  		chartElement[i].setShowXDividers(true);
  		chartElement[i].setXScrollerRange(new RangeModel(0, 200));
      chartElement[i].setXAxisSigDigitDisplay(1);
      chartElement[i].setToolTipDelay(0);
      chartList[i] = chartElement[i];
  		  		
  		GridBagConstraints constraints = new GridBagConstraints();

	    constraints.gridx = 0;
	    constraints.gridy = i;
	    constraints.gridwidth = 1;
	    constraints.gridheight = 1;
	    constraints.weightx = 2.0;
	    constraints.weighty = 2.0;
	    constraints.fill = GridBagConstraints.BOTH;
	    
	    chartPanel.add(chartElement[i], constraints);
	    
  	  
    }
    //xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
    //xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
    add(chartPanel, BorderLayout.CENTER);
	 // add(xRC, BorderLayout.SOUTH);
	  validate();
	  
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: Constructor called by applet

  <br><b>Notes</b>:<br>
	                  -sets up Gridbag Layout

  <br>

	*********************************************************************************************************************/
  
  public NChart(int numberOfCharts, String xLabel, String yLabel, ReadinessChartApplet uiPtr, double space)
  {
  	rChartApplet = uiPtr;
  	additionalSpace = space;   
  	yAxisLabel = yLabel;
  	xAxisLabel = xLabel;
  	setLayout(new BorderLayout());
  	totalCharts = numberOfCharts;
  	chartElement = new CChart[numberOfCharts];
  	chartList = new CChart[numberOfCharts];
  	//buildLegends();
  	System.out.println("%%%% readiness constructor " + numberOfCharts);
  	for(int i = 0; i < numberOfCharts; i++)
  	{
  		JPanel titlePanel = new JPanel();
  		JLabel titleLabel = new JLabel("Title");
  		chartElement[i] = new CChart("chart " + i, titlePanel, xAxisLabel, yAxisLabel, true, additionalSpace);
  		//chartElement[i].setShowXRangeScroller(true);
  		chartElement[i].setShowXDividers(true);
  		chartElement[i].setXScrollerRange(new RangeModel(0, 200));
      chartElement[i].setXAxisSigDigitDisplay(2);
      chartElement[i].setYAxisSigDigitDisplay(2);
      chartElement[i].setXAxisExponentDisplayThreshold(0);
      chartElement[i].setYAxisExponentDisplayThreshold(0);
      chartElement[i].setToolTipDelay(0);
      chartList[i] = chartElement[i];
  		  		
  		GridBagConstraints constraints = new GridBagConstraints();

	    constraints.gridx = 0;
	    constraints.gridy = i;
	    constraints.gridwidth = 1;
	    constraints.gridheight = 1;
	    constraints.weightx = 2.0;
	    constraints.weighty = 2.0;
	    constraints.fill = GridBagConstraints.BOTH;
	    
	    chartPanel.add(chartElement[i], constraints);
	    
  	  
    }
    //xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
    //xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
    add(chartPanel, BorderLayout.CENTER);
	 // add(xRC, BorderLayout.SOUTH);
	  validate();
	  
  }
  // ---------------------------------------------------------------------------------------------------------------------
  // Public Member Methods
  // ---------------------------------------------------------------------------------------------------------------------
  /*********************************************************************************************************************
  <b>Description</b>: Set Scrollers

  <br><b>Notes</b>:<br>
	                  -Set X scroll range and X dividers

  <br>
  

  @return void

	*********************************************************************************************************************/
  public void setScrollers()
  {
  	for(int i = 0; i < chartList.length; i++)
  	{
  	  chartList[i].setXScrollerRange(xRC.getRange());
      chartList[i].setShowXDividers(false);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setTwoUp

  <br><b>Notes</b>:<br>
	                  -set twoup flag to boolean

  <br>
  @param t boolean determines chart layout.
  
  @return void

	*********************************************************************************************************************/
  public void setTwoUp(boolean t)
  {
  	twoUp = t;
  }
  /*********************************************************************************************************************
  <b>Description</b>: reDrawCharts

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return void

	*********************************************************************************************************************/
  public void redrawCharts()
  {
  	//System.out.println("%% redraw charts");
  	setDataIntoChart(data, false);
  }
  /*********************************************************************************************************************
  <b>Description</b>: swapCharts

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param chart1Index int first chart t swap.
  @param chart2Index int second chart to swap.
  @return void

	*********************************************************************************************************************/
  public void swapCharts(int chart1Index, int chart2Index)
  {
  	if(chart1Index < data.size()&& chart2Index < data.size())
  	{
	  	Vector newData = (Vector)data.clone();
	  	for(int i = 0; i < data.size(); i++)
	  	{
	  		if(i == chart1Index)
	  		  newData.setElementAt(data.elementAt(i), chart2Index);
	  		else if(i == chart2Index)
	  		  newData.setElementAt(data.elementAt(i), chart1Index);
	  		else
	  		  newData.setElementAt(data.elementAt(i), i);
	  		
	  	}
	  	data = newData;
	  	setDataIntoChart(data, false);
	 }
  }
  /*********************************************************************************************************************
  <b>Description</b>: removeCharts

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param chartIndex int chart to remove.
  @return void

	*********************************************************************************************************************/
  
  public void removeCharts(int chartIndex)
  {
  	if(data.size() == 1)
  	  return;
  	data.removeElementAt(chartIndex);
  	setDataIntoChart(data, false);
  }
  /*********************************************************************************************************************
  <b>Description</b>: setVisible

  <br><b>Notes</b>:<br>
	                  - sets a DataSet visible

  <br>
  @param dataSet DataSet containing the (x,y) data pairs.
  @param visible boolean .
  @return void

	*********************************************************************************************************************/
  
  public void setVisible(DataSet dataSet, boolean visible)
  {
    dataSet.visible = visible;
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].resetYRangeScroller();
      chartList[i].recalculateAutoYRange();
    }
    repaint();
  }
  /*********************************************************************************************************************
  <b>Description</b>: detachAllDataSets

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return void

	*********************************************************************************************************************/
  public void detachAllDataSets()
  {
  	//System.out.println("%%%% detach");
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].detachAllDataSets();
    }

    
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: setDataIntoChart

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param dataVector Vector containing DataSets.
  @param initial boolean if its the first time we don't display.
  @return void

	*********************************************************************************************************************/
  public void setDataIntoChart(Vector dataVector, boolean initial)
  {
  	chartPanel.removeAll();
  	data = dataVector;
  	
  	for(int i = 0; i < totalCharts; i++)
  	{
  		remove(chartList[i]);
  	}
  	int numberOfCharts = dataVector.size();
  	setLayout(new BorderLayout());
  	totalCharts = numberOfCharts;
  	chartElement = new CChart[numberOfCharts];
  	chartList = new CChart[numberOfCharts];
  	//buildLegends();
  	int xPos = 0;
  	int yPos = 0;
  	System.out.println("%%%% set data " + numberOfCharts);
  	for(int i = 0; i < numberOfCharts; i++)
  	{
  		System.out.println("%%%% setdata into chart");
  		JPanel titlePanel = new JPanel();
  		JLabel titleLabel = new JLabel("Title");
  		chartElement[i] = new CChart("chart " + i, titlePanel, xAxisLabel, yAxisLabel , true);
  		new DragSourceDropTarget(chartElement[i]);
  		if(totalCharts > 1)
  		{
  		  chartElement[i].setShowXRangeScroller(true);
  		  chartElement[i].setXScrollerRange(new RangeModel(0, 200));
  		}
  		chartElement[i].setShowXDividers(true);
  		chartElement[i].setXAxisSigDigitDisplay(2);
      chartElement[i].setYAxisSigDigitDisplay(2);
      chartElement[i].setXAxisExponentDisplayThreshold(0);
      chartElement[i].setYAxisExponentDisplayThreshold(0);
      chartElement[i].setToolTipDelay(0);
      chartList[i] = chartElement[i];
  		  		
  		GridBagConstraints constraints = new GridBagConstraints();

	    constraints.gridx = xPos;
	    constraints.gridy = yPos;
	    constraints.gridwidth = 1;
	    constraints.gridheight = 1;
	    constraints.weightx = 2.0;
	    constraints.weighty = 2.0;
	    constraints.fill = GridBagConstraints.BOTH;
	    
	    chartPanel.add(chartElement[i], constraints);
	    if(twoUp)
	    {
		    if(xPos == 1)
	  	  {
	  	    xPos = 0;
	  	    yPos++;
	  	  }
	  	  else
	  	  {
	  	    xPos = 1;
	  	  }
  	  }
  	  else
  	  {
  	  	yPos++;
  	  }
  	  
    }
    
	  validate();
	  if(!initial)
	  {
	  	displayDataSets(dataVector);
	  	RangeModel range = chartElement[0].getTotalXRange();
	  	if(numberOfCharts > 1)
	  	{
		  	xRC.setSliderRange(range.getMin(), range.getMax());     
	      xRC.setRange(range);
	      xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
	      xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
	      setScrollers();
	      setXRangeScrollLock(false);
	      add(xRC, BorderLayout.SOUTH);
      }
	  }
	  
	  
	  setLayout(new BorderLayout());
	  RangeModel range = chartElement[0].getTotalXRange();
	  if(numberOfCharts > 1)
	  {
	    xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
	    xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
	    add(xRC, BorderLayout.SOUTH);
	    setScrollers();
    }       
    setXRangeScrollLock(false);
    add(chartPanel, BorderLayout.CENTER);
	  setShowRightYAxis(false);	
	  
	  	  
  }
  /*********************************************************************************************************************
  <b>Description</b>: displayDataSets

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param myData Vector containing DataSets.
  @return void

	*********************************************************************************************************************/
  public void displayDataSets(Vector myData)
  {
  	System.out.println("%%%% display datasets");
  	for(int i = 0; i < myData.size(); i++)
  	{
  		Vector dataForChart = (Vector)myData.elementAt(i);
  		for(int j = 0; j < dataForChart.size(); j++)
  		{
  			DataSet d = (DataSet)dataForChart.elementAt(j);
  			attachDataSet(d, i);
  			
  		}
  		
  	}
  	resetTR();
  	resetR();
  	
  }
  /*********************************************************************************************************************
  <b>Description</b>: attachDataSet

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param dataSet DataSet to attach.
  @param chartNumber int of chart number to use.
  @return void

	*********************************************************************************************************************/
  public void attachDataSet(DataSet dataSet, int chartNumber)
  {
  	//System.out.println("%%%% attach # " + chartNumber);
  	chartList[chartNumber].attachDataSet(dataSet);
     
  }
  /*********************************************************************************************************************
  <b>Description</b>: getDataSets

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return Vector of DataSets

	*********************************************************************************************************************/
  public Vector getDataSets()
  {
    Vector list = new Vector(0);
    for (int i=0; i<chartList.length; i++)
    {
    	list.add(chartList[i].getDataSets());
    }

    return(list);
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: setAutoYRange

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setAutoYRange(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setAutoYRange(value);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setShowTitle

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param show boolean .
  @return void

	*********************************************************************************************************************/
  public void setShowTitle(boolean show)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowTitle(true);
    }
  }

  /*********************************************************************************************************************
  <b>Description</b>: setShowXRangeScroller

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/

  public void setShowXRangeScroller(boolean value)
  {
    if (value)
    {
      add(xRC, BorderLayout.SOUTH);
      validate();
    }
    else
    {
      remove(xRC);
      validate();
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setShowXRangeTickLabels

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setShowXRangeTickLabels(boolean value)
  {
    xRC.setDrawTickLabels(value);
  }
  /*********************************************************************************************************************
  <b>Description</b>: setShowLeftYAxis

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setShowLeftYAxis(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowLeftYAxis(value);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setShowRightYAxis

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setShowRightYAxis(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowRightYAxis(value);
    }
  }
  
  
  /*********************************************************************************************************************
  <b>Description</b>: setShoeYRangeScroller

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/

  public void setShowYRangeScroller(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowYRangeScroller(value);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setShowYRangeTickLabels

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setShowYRangeTickLabels(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowYRangeTickLabels(value);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: setYRangeScrollLock

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean .
  @return void

	*********************************************************************************************************************/
  public void setYRangeScrollLock(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setYRangeScrollLock(value);
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: propertyChange

  <br><b>Notes</b>:<br>
	                  - repaint the charts after property change

  <br>
  @param e PropertyChangeEvent .
  @return void

	*********************************************************************************************************************/
  public void propertyChange(PropertyChangeEvent e)
  {
  	//System.out.println("%%%% repaint charts");
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].repaint();
    }
  }
  /*********************************************************************************************************************
  <b>Description</b>: rangeChangeListener

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return void

	*********************************************************************************************************************/
  private class RangeChangeListener implements PropertyChangeListener
  {
    private CMThumbSliderRangeControl rC = null;
    private double[] minMax = null;
    private CChart[] chartList = null;

    public RangeChangeListener(CMThumbSliderRangeControl rC, double[] minMax, CChart[] chartList)
    {
      RangeChangeListener.this.rC = rC;
      RangeChangeListener.this.minMax = minMax;
      RangeChangeListener.this.chartList = chartList;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
      rangeChanged(rC, minMax, chartList, xScrollSize, xRangeScrollLock, RangeChangeListener.this);
    }
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: rangeChanged

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param rC CMThumbSliderRangeControl.
  @param minMax double[].
  @param chartList CChart[].
  @param scrollSize double.
  @param scrollLock boolean.
  @param listener PropertyChangeListener.
  @return void

	*********************************************************************************************************************/

  private void rangeChanged(CMThumbSliderRangeControl rC, double[] minMax, CChart[] chartList, double scrollSize, boolean scrollLock, PropertyChangeListener listener)
  {
    double currentMin = rC.getRange().getMin();
    double currentMax = rC.getRange().getMax();

    if ((currentMin == currentMax) || ((minMax[0] == currentMin) && (minMax[1] == currentMax)))
    {
      return;
    }

    if (scrollLock)
    {
      rC.removePropertyChangeListener("range", listener);
      if (minMax[0] != currentMin)
      {
        rC.setRange(new RangeModel((int)currentMin, (int)(currentMin + scrollSize)));
      }
      else if (minMax[1] != currentMax)
      {
        rC.setRange(new RangeModel((int)(currentMax - scrollSize), (int)currentMax));
      }
      else
      {
        rC.setRange(new RangeModel((int)minMax[0], (int)minMax[1]));
      }
      rC.addPropertyChangeListener("range", listener);
    }

    minMax[0] = rC.getRange().getMin();
    minMax[1] = rC.getRange().getMax();
    
    
    for(int i = 0; i < chartList.length; i++)
    {
    	chartList[i].setXScrollerRange(new RangeModel((int)minMax[0], (int)minMax[1]));
    }
    
  }
  /*********************************************************************************************************************
  <b>Description</b>: resetR

  <br><b>Notes</b>:<br>
	                  - reset range for each chart

  <br>

  @return void

	*********************************************************************************************************************/
  public void resetR()
  {
  	for(int i = 0; i < totalCharts; i++)
  	{
  	  chartList[i].resetRange();
  	}
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: resetR

  <br><b>Notes</b>:<br>
	                  - reset total range for each chart

  <br>

  @return void

	*********************************************************************************************************************/
  public void resetTR()
  {
  	for(int i = 0; i < totalCharts; i++)
  	{
  	  chartList[i].resetTotalRange();
  	}
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: setXRangeScrollLock

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param value boolean.
  @return void

	*********************************************************************************************************************/
  
  public void setXRangeScrollLock(boolean value)
  {
    xRangeScrollLock = value;

    if (xRangeScrollLock)
    {
      xScrollSize = xRC.getRange().getMax() - xRC.getRange().getMin();
    }
  }
 
  /*********************************************************************************************************************
  <b>Description</b>: DragSourceDropTarget class implements DragSource and DropTarget

  <br><b>Notes</b>:<br>
	                  -

  <br>

  

	*********************************************************************************************************************/
  
  private class DragSourceDropTarget implements DragSource, DropTarget
  {
  		// Drag & Drop supporting class
    private DragAndDropSupport dndSupport = new DragAndDropSupport();
    private Color background = null;
  	CChart chart = null;
  	// ---------------------------------------------------------------------------------------------------------------------
  // Public Constructors
  // ---------------------------------------------------------------------------------------------------------------------
  /*********************************************************************************************************************
  <b>Description</b>: Constructor 

  <br><b>Notes</b>:<br>
	                  -Sets up drag and drop component

  <br>

	*********************************************************************************************************************/
  	public DragSourceDropTarget(CChart cChart)
  	{
  		chart = cChart;
  		background = chart.getBackground();
  		// Add the drag source
      dndSupport.addDragSource(this);
     // Add the drop target
      dndSupport.addDropTarget(this);
  	}
  	
  



  // ------------------- DragSource Interface ----------------------------  
  // ---------------------------------------------------------------------------------------------------------------------
  // Public Member Methods
  // ---------------------------------------------------------------------------------------------------------------------
  
  /*********************************************************************************************************************
  <b>Description</b>: getSourceComponents

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return Vector of draggable components

	*********************************************************************************************************************/

	  public Vector getSourceComponents()
	  {
	    Vector components = new Vector(1);
	    components.add(chart);
	    
	    return(components);
	  }
	/*********************************************************************************************************************
  <b>Description</b>: dragFromSubComponents

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return boolean

	*********************************************************************************************************************/
	  public boolean dragFromSubComponents()
	  {
	    return(true);
	  }
	/*********************************************************************************************************************
  <b>Description</b>: getData

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param componentAt Component to drag from.
  @param location Point of drag occurence.
  @return Object - the drag object

	*********************************************************************************************************************/
	  public Object getData(Component componentAt, Point location)
	  {
	  	
	  	DataSet ds = chart.getClosestDataSet(location.x, location.y);
	  	if(ds == null)
	  	  return null;
	  	ds.xAxisLabel = xAxisLabel;
	  	ds.yAxisLabel = yAxisLabel;
	    return(ds);
	  }
	 /*********************************************************************************************************************
  <b>Description</b>: dragDropEnd

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param success boolean
  @return void

	*********************************************************************************************************************/
	  public void dragDropEnd(boolean success)
	  {
  	}
	
		// ------------------- DragSource Interface ----------------------------  
	/*********************************************************************************************************************
  <b>Description</b>: getTargetComponents

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return Vector of components

	*********************************************************************************************************************/
	  public Vector getTargetComponents()
	  {
	    Vector components = new Vector(1);
	    components.add(chart);
	    
	    return(components);
	  }
	/*********************************************************************************************************************
  <b>Description</b>: dropToSubComponents

  <br><b>Notes</b>:<br>
	                  -

  <br>

  @return boolean

	*********************************************************************************************************************/
	  public boolean dropToSubComponents()
	  {
	    return(true);
	  }
	/*********************************************************************************************************************
  <b>Description</b>: readyForDrop

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param componentAt Component
  @param location Point
  @param flavor DataFlavor
  @return boolean

	*********************************************************************************************************************/
	  public boolean readyForDrop(Component componentAt, Point location, DataFlavor flavor)
	  {
	    return(true);
	  }
	/*********************************************************************************************************************
  <b>Description</b>: showAsDroppable

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param componentAt Component
  @param location Point
  @param flavor DataFlavor
  @param show boolean
  @param droppable boolean
  @return void

	*********************************************************************************************************************/
	  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean droppable)
	  {
			if(show)
			{
			  if (droppable)
			  {
				  chart.setBackground(Color.green);
			  }
			  else
			  {
				  chart.setBackground(Color.red);
				}
			}
			else
			{
				chart.setBackground(background);
			}
	  }
	/*********************************************************************************************************************
  <b>Description</b>: dropData

  <br><b>Notes</b>:<br>
	                  - act on dropped object (data)

  <br>
  @param componentAt Component
  @param location Point
  @param flavor DataFlavor
  @param data Object
  @return void

	*********************************************************************************************************************/
	  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object data)
	  {
	  	//System.out.println("%%%% drop dataset");
	  	try
	  	{
	  		double[] values  = ((DataSet)data).getData();
	  		
	  		String newName = ((DataSet)data).dataName;
	  		DataSet newDataSet = new PolygonFillableDataSet(values, values.length/2, false);
	  		newDataSet.dataName = newName;
	  		
	  		
	  		
      	int lowX = (int)values[0];
      	int highX = (int)values[values.length -2];
      	      	
      	xMinMax[0] = lowX;
      	xMinMax[1] = highX;
      	
	  	  chart.attachDataSet(newDataSet);
	  	  RangeModel range = chart.getTotalXRange();
      	xRC.setSliderRange(range.getMin(), range.getMax());     
        xRC.setRange(range);
        chart.setXScrollerRange(new RangeModel(lowX, highX));
        setScrollers();
        
        
        if(nChartUI != null)
  	     nChartUI.setDataSetMenu();
  	    else if(nChartApplet != null)
  	     nChartApplet.setDataSetMenu();
  	    else if(rChartApplet != null)
  	     rChartApplet.setDataSetMenu();
  	     
  	   
  	   
  	    //  comment this out for applet
	  	  //nChartUI.setDataSetMenu();
	  	  	  	  
	  	  chart.setShowTitle(true);
	  	  chart.setTitle(((DataSet)data).title);
	  	  
	  	  chart.setYAxisLabel(((DataSet)data).yAxisLabel);
	  	  chart.setXMinorTicMarks(2);
	  	  chart.resetTotalRange();
	  	  chart.resetRange();
        
        
        
        /*
        chart = new CChart("A Chart!!!", new JPanel(), "Time", "xAxisLabel", false);
      chart.attachDataSet((DataSet)data);
      chart.setShowTitle(true);
	    chart.setTitle(((DataSet)data).title);
      chart.setYAxisLabel(((DataSet)data).yAxisLabel);
      //chart.setXAxisLabel(dataSet.yAxisLabel);
      chart.resetTotalRange();
      chart.resetRange();*/
      	
      	
      	
	  	}
    	  catch(Exception e)
  	  {
	  	e.printStackTrace();
	    }
	  	
	  }
	/*********************************************************************************************************************
  <b>Description</b>: getSupportedDataFlavors

  <br><b>Notes</b>:<br>
	                  -

  <br>
  @param componentAt Component
  @param location  Point
  @return Vector of supported dataflavors

	*********************************************************************************************************************/
	  public Vector getSupportedDataFlavors(Component componentAt, Point location)
	  {
	    Vector flavors = new Vector(1);
	    flavors.add(ObjectTransferable.getDataFlavor(DataSet.class));
	    
	    return(flavors);
	  }
  	
  }
  
  
  
}
