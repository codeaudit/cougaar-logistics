
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

import java.io.Serializable;

import java.awt.dnd.*;

import javax.swing.*;


import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.*;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;


/***********************************************************************************************************************
<b>Description</b>: NChartUIComponent GUI component for NChart example.

<br><br><b>Notes</b>:<br>
									Provides the component factory to NChart.

***********************************************************************************************************************/

public class NChartUIComponent extends ComponentFactory implements CougaarDesktopPropertiesUI
{
  private NChartUI selector = null;
  private Vector persistedData = null;
  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public void install(CDesktopFrame f)
  {
    try
    {
    	if(persistedData != null)
    	  selector = new NChartUI(persistedData, f);
    	else
        selector = new NChartUI(6, f);
      //selector.install(f);
    }
    catch(RuntimeException e)
    {
    	
    }
  }
  
  public String getToolDisplayName()
	{
	  return("NChart UI");
	}
  
  public CougaarDesktopUI create()
	{
	  return(this);
	}
  
  public boolean supportsPlaf()
  {
    return(true);
  }
  
  // #DnD -------------------------------------------------------------------

  public boolean isPersistable()
  {
    return(true);
  }
  
  public Serializable getPersistedData()
  {
  	
  	//  convert data to be persisted to serializable object
    
    Vector persisted = new Vector();
    DataSet[] dataSets = null;
		
		Vector currentChartList = (Vector) selector.chart.getDataSets();
		for(int i = 0; i < currentChartList.size(); i++)
		{
			dataSets = (DataSet[])currentChartList.elementAt(i);
		  Vector persistedElement = new Vector(dataSets.length);
		  for(int j = 0; j < dataSets.length; j++)
		  {
		  	DataSet d = dataSets[j];
		  	ChartPersistentData cpd = new ChartPersistentData(d.getData(), d.dataName);
		    if(dataSets[j] instanceof BarDataSet)
		    {
		    	if(((PolygonFillableDataSet)dataSets[j]).polygonFill)
		    	  cpd.setType("FilledBar");
		    	else
		        cpd.setType("Bar");
		    }
		    else if(dataSets[j] instanceof StepDataSet)
		    {
		    	if(((PolygonFillableDataSet)dataSets[j]).polygonFill)
		    	  cpd.setType("FilledStep");
		    	else
		      	cpd.setType("Step");
		    }
		    else if(dataSets[j] instanceof PolygonFillableDataSet)
		    {
		    	if(((PolygonFillableDataSet)dataSets[j]).polygonFill)
		    	  cpd.setType("FilledLine");
		    	else
		    	  cpd.setType("Line");
		    }
		    
		    persistedElement.add(cpd);
		  }
		  persisted.add(persistedElement);
		}
   
    return persisted;
  }

  public void setPersistedData(Serializable data)
   
  {
  	if(data != null)
  	{
	  	Vector incomingData = (Vector) data;
	  	persistedData = new Vector(incomingData.size());
	  	
	  	for(int i = 0; i < incomingData.size(); i++)
	  	{
	       Vector chartData = (Vector) incomingData.elementAt(i);
	       Vector subVector = new Vector(chartData.size());
	       for(int j = 0; j < chartData.size(); j++)
	       {
	         ChartPersistentData cpd = (ChartPersistentData)chartData.elementAt(j);
	         double[] data1 = cpd.getData();
	         String name = cpd.getName();
	         DataSet d1a = null;
	         try
	         {
	         	if(cpd.getType().equals("Bar"))
	         	  d1a = new BarDataSet(data1, data1.length/2, false, selector.barWidth);
	         	else if(cpd.getType().equals("Step"))
	         	  d1a = new StepDataSet(data1, data1.length/2, false);
	         	else if(cpd.getType().equals("FilledBar"))
	         	  d1a = new BarDataSet(data1, data1.length/2, true, selector.barWidth);
	          else if(cpd.getType().equals("FilledStep"))
	         	  d1a = new StepDataSet(data1, data1.length/2, true);
	         	else
		          d1a = new PolygonFillableDataSet(data1, data1.length/2, false);
		        d1a.dataName = name;
		        subVector.add(d1a);
	         }
	         catch(Exception e)
	         {
	         	  e.printStackTrace();
	         }
	       
	         
	       }
	       persistedData.add(subVector);
	    }
	    
    }
  }
  
  public String getTitle()
  {
    return("NChartUI");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(900, 600));
  }

  public boolean isResizable()
  {
    return(true);
  }
  
  public String getProperties()
  {
    String msg = "Author - Frank Cooley";
  	msg += '\n';
  	msg += "Company - Clark Software Engineering LTD.";
  	msg += '\n';
  	msg += "User dir - " + System.getProperty("user.dir");
    return msg;
  }

}
