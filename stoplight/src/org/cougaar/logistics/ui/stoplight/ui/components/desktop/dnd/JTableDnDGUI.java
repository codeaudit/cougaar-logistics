/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.util.Vector;
import java.util.Date;


import org.cougaar.logistics.ui.stoplight.ui.components.CChart;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CDesktopFrame;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CNMTableModel;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.ClusterNetworkMetrics;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.TableModel;
import org.cougaar.logistics.ui.stoplight.ui.components.drilldown.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;

public class JTableDnDGUI extends org.cougaar.logistics.ui.stoplight.ui.components.desktop.ComponentFactory implements org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopPropertiesUI, DropTarget, DragSource
{
  private JTextArea textArea = new JTextArea();
  private Color background = textArea.getBackground();
  private JPanel panel = new JPanel();
  private CDesktopFrame frame = null;
  private TableModel tm = new TableModel();
  //private JTable table = new JTable(tm);
  private JTable table = new JTable(new CNMTableModel());
  private JScrollPane tableScrollPane = new JScrollPane(table);
  // Drag & Drop supporting class
  private DragAndDropSupport dndSupport = new DragAndDropSupport();
  CNMTableModel cmModel = new CNMTableModel();
  



  // ------------------- DragSource Interface ----------------------------  
  
  public Object getData(Component componentAt, Point location)
	{
		int row = table.rowAtPoint(location);
		int col = table.columnAtPoint(location);
		Object tblObject = (Object) table.getValueAt(row, 0);
		
		if(tblObject instanceof String)
		{
		
		
			CNMTableModel cModel = (CNMTableModel)table.getModel();
			
			if(col == 3)
			{
				//  xmit
				//System.out.println("%%%% getdata row is " + row);
				return cModel.transmitPackets.elementAt(row);
			}
			else if(col == 4)
			{
				// receive
				return cModel.receivePackets.elementAt(row);
			}
			else if(col == 5)
			{
				// lost
				return cModel.lostPackets.elementAt(row);
			}
			else
			{
				return (new Double("40"));
			}
			
		}
		else
		  return (new Double("40"));
	}

  public Vector getTargetComponents()
  {
    Vector components = new Vector(1);
    components.add(frame);
    return(components);
  }

  public boolean dropToSubComponents()
  {
    return(true);
  }

  public boolean readyForDrop(Component componentAt, Point location, DataFlavor flavor)
  {
    return(true);
  }

  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean droppable)
  {
		if(show)
		{
		  if (droppable)
		  {
			  textArea.setBackground(Color.green);
		  }
		  else
		  {
			  textArea.setBackground(Color.red);
			}
		}
		else
		{
			textArea.setBackground(background);
		}
  }

  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object data)
  {
  	//System.out.println("%%%% dataset " + flavor.getRepresentationClass());
  	try
  	{
	  	if(flavor.getRepresentationClass() == Class.forName("org.cougaar.logistics.ui.stoplight.ui.components.graph.DataSet"))
	  	{
		  	table.setModel(tm);
		  	DataSet myDS = (DataSet) data;
		  	String name = myDS.dataName;
		  	textArea.setText(name);
		  	tm.setData(myDS);
		  	tm.tblDataSet = myDS;
		  	frame.setTitle(name);
	    }
	    else if(flavor.getRepresentationClass() == Class.forName("org.cougaar.logistics.ui.stoplight.ui.components.desktop.ClusterNetworkMetrics"))
	    {
	    	//CNMTableModel cmModel = new CNMTableModel((ClusterNetworkMetrics)data);
	    	cmModel.addData((ClusterNetworkMetrics)data);
	    	table.setModel(cmModel);
	    }
    }
    catch(Exception e)
    {
    	e.printStackTrace();
    }
  
  	frame.show();
  	
  }
  
  public Vector getSourceComponents()
  {
    Vector components = new Vector(1);
    components.add(table);
    
    return(components);
  }
  
  public boolean dragFromSubComponents()
  {
    return(true);
  }
  
  public void dragDropEnd(boolean success)
  {
	}

  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    Vector flavors = new Vector(1);
    flavors.add(ObjectTransferable.getDataFlavor(DataSet.class));
    flavors.add(ObjectTransferable.getDataFlavor(ClusterNetworkMetrics.class));
    return(flavors);
  }



/*
  public void install(CDesktopFrame f)
  {
  	frame = f; 	
	  panel.setLayout(new BorderLayout());
		tableScrollPane = new JScrollPane(table);
    panel.add(tableScrollPane, BorderLayout.CENTER);
    f.getContentPane().add(panel);

    background = panel.getBackground();
    // Add the drop target
    dndSupport.addDropTarget(this);
    dndSupport.addDragSource(this);
  }
*/


  private DataSet getDataSetAt(Point location)
  {
		int row = table.rowAtPoint(location);
		int col = table.columnAtPoint(location);
    //System.out.println("%%%% tablemodel is " + table.getModel() + " col is " + col);
		CNMTableModel tModel = (CNMTableModel)table.getModel();
		switch(col)
		{
			case 3:
			  return (DataSet) tModel.transmitPackets.elementAt(row);
			case 4:
			  return (DataSet) tModel.receivePackets.elementAt(row);
			case 5:
			  return (DataSet) tModel.lostPackets.elementAt(row);
		}
		
		return null;
		//TableModel tModel = (TableModel)table.getModel();
		
		  //if(col == 3 || col == 4 || col == 5)
				//return(tModel.tblDataSet);
			//else
			  //return null;
    
    
    
    

	  
  }


  public void install(CDesktopFrame f)
  {
  	frame = f; 	
		tableScrollPane = new JScrollPane(table);

    DrillDownStack stack = new DrillDownStack(tableScrollPane);
    stack.addDrillDown(new TableDrillDown(table), tableScrollPane.getViewport(), null);

    f.getContentPane().add(stack);

    background = panel.getBackground();
    // Add the drop target
    dndSupport.addDropTarget(this);
    dndSupport.addDragSource(this);
    f.setSize(600, 400);	
  }

  private class TableDrillDown implements DrillDown
  {
    private JTable table = null;
    
    public TableDrillDown(JTable table)
    {
      this.table = table;
    }
  
    public DrillDown getNextDrillDown(MouseEvent e)
    {
      SubTableDrillDown subTable = null;
      
      DataSet data = getDataSetAt(e.getPoint());
      //System.out.println("%%%% drilldown dataset is " + data);
      if (data != null)
      {
        subTable = new SubTableDrillDown();
        subTable.setData(data);
      }
      
      return(subTable);
    }
  
    public void setData(Object data)
    {
      // Should never be called
      throw(new RuntimeException("TableDrillDown.setData called!!!"));
    }
  
    public Component activate(DrillDownStack drillDownStack)
    {
      // Should never be called
      throw(new RuntimeException("TableDrillDown.activate called!!!"));
    }
  }

  private class SubTableDrillDown implements DrillDown
  {
    private DataSet dataSet = null;
    
    public DrillDown getNextDrillDown(MouseEvent e)
    {
      ChartDrillDown chart = new ChartDrillDown();
      chart.setData(dataSet);
  
      return(chart);
    }
  
    public void setData(Object data)
    {
      dataSet = (DataSet)data;
    }
  
    public Component activate(DrillDownStack drillDownStack)
    {
    	JTable ttable = new JTable();
    	JScrollPane jScrollPane = new JScrollPane(ttable);
    	
    	//CNMTableModel dModel = new CNMTableModel(dataSet);
      TableModel dModel = new TableModel(dataSet);
      
      drillDownStack.addDrillDown(this, ttable, null);
      
      ttable.setModel(dModel);

      return(jScrollPane);
    }
  }
  
  private class ChartDrillDown implements DrillDown
  {
    private DataSet dataSet = null;
    
    public DrillDown getNextDrillDown(MouseEvent e)
    {
      // Should never be called
      throw(new RuntimeException("ChartDrillDown.getNextDrillDown called!!!"));
    }
  
    public void setData(Object data)
    {
      dataSet = (DataSet)data;
    }
  
    public Component activate(DrillDownStack drillDownStack)
    {
      CChart chart = new CChart("A Chart!!!", new JPanel(), "Time", "xAxisLabel", false);
      chart.attachDataSet(dataSet);
      chart.setShowTitle(true);
	    chart.setTitle(dataSet.title);
      chart.setYAxisLabel(dataSet.yAxisLabel);
      //chart.setXAxisLabel(dataSet.yAxisLabel);
      chart.resetTotalRange();
      chart.resetRange();
      return(chart);
    }
  }







	public String getToolDisplayName()
	{
	  return("JTable Target Test UI");
	}

	public CougaarDesktopUI create()
	{
	  return((CougaarDesktopUI)this);
	}

  public boolean supportsPlaf()
  {
    return(true);
  }

  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public boolean isPersistable()
  {
    return(false);
  }

  public Serializable getPersistedData()
  {
    return(null);
  }

  public void setPersistedData(Serializable data)
  {
  }

  public String getTitle()
  {
    return("JTable Target Test UI");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(200, 200));
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
