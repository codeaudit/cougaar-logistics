/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.io.*;
import java.awt.*;

import java.util.Vector;
import java.util.Hashtable;

import java.util.Date;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.JMenu;
import javax.swing.BorderFactory;
import javax.swing.border.*;




import javax.swing.JLabel;
import javax.swing.Icon;


import javax.swing.table.TableColumnModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.cougaar.logistics.ui.stoplight.ui.components.CChartLegend;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.models.*;




import org.cougaar.mlm.ui.data.UISimpleNamedSchedule;
import org.cougaar.mlm.ui.data.UISimpleInventory;





public class InventoryQuery implements Query, PropertyChangeListener
{
  String assetName;
  String clusterName;
  Hashtable clusters;
  Hashtable assetInventories;
  private boolean fileBased = false;
  private boolean buildFile = false;
  UISimpleInventory inventory;
  InventoryTableModel model;
  JTable table;
  TableColumn[] tblColumn;
  String[] persistentColumnNames;

  /** Called to create an inventory chart from a query using asset and cluster.
   */
  public InventoryQuery(String assetName, String clusterName, Hashtable clusters, Hashtable assetInventories)
  {
    //System.out.println("constructor 1");
    this.assetName = assetName;
    this.clusterName = clusterName;
    this.clusters = clusters;
    this.assetInventories = assetInventories;
    if(assetInventories != null)
    {
      clusters.put(clusterName, assetInventories);
      buildFile = true;
    }
  }

  /** Called to create an inventory chart from data retrieved from a file.
   */

  public InventoryQuery(UISimpleInventory inventory, String clusterName, Hashtable clusters)
  {
    //System.out.println("constructor 2");
    this.inventory = inventory;
    this.assetName = inventory.getAssetName();
    this.clusterName = clusterName;
    fileBased = true;
    buildFile = true;
  }

  public String getQueryToSend()
  {
    return assetName;
  }





  public JPanel reinitializeAndUpdateChart(String title)
  {
    return null;
  }

 public void resetChart()
 {
 }

  public void setToCDays(boolean useCDays)
  {
  }

  public JTable createTable(String title)
  {
    return null;
  }

  public String getPSP_id()
  {
    return "inventory";
  }
  
  /*
  public String getPSP_id()
  {
    return "GLMINVENTORY.PSP";
  }
  */
  public void readReply(InputStream is)
  {
    inventory = null;
    if(!fileBased)
    {
      try {
        ObjectInputStream p = new ObjectInputStream(is);
        inventory = (UISimpleInventory)p.readObject();
        // System.out.println("inventory = " + inventory);
        if(buildFile)
        {
          //System.out.println(assetName);
          save();
        }
      } catch (Exception e) {
        System.err.println("Object read exception: " + e);
        //System.out.println("Object read exception: " + e);
        return;
      }
    }

  }

    public void save(File file)
    {
    try {
      FileOutputStream f = new FileOutputStream(file);
      ObjectOutputStream o = new ObjectOutputStream(f);
      o.writeObject(inventory);
      o.flush();
      f.close();
    } catch (Exception exc) {
      System.err.println("Cannot open file");
    }
    }

    public void save(String file)
    {
    try {
      FileOutputStream f = new FileOutputStream("tInv");
      ObjectOutputStream o = new ObjectOutputStream(f);
      o.writeObject(inventory);
      o.flush();
      f.close();
    } catch (Exception exc) {
      System.err.println("Cannot open file");
    }
    }

  public void save()
  {
    // build the hashtable of inventory objects for assetnames
    assetInventories.put(assetName, inventory);
    //System.out.println("add asset " + assetName  + "  to cluster " + clusterName);

  }

  public JPanel createChart(String title,  JSplitPane split)
  {
    return null;
  }

  public boolean setChartData(String title, BlackJackInventoryChart chart, CChartLegend legend)
  {
    chart.detachAllDataSets();
    legend.removeAllDataSets();

//      chart.setTitle(model.getAssetName());
    // The slider control uses an int for range which cannot hold a millis since 1970 date so we scaled the data time
    chart.setTimeScale(InventoryTableModel.timeScale);
    // Set the chart Y-axis to display the same number of digits past the decimal as the table displays
    chart.setYAxisSigDigitDisplay(2);

    if (inventory != null)
    {
//      System.out.println("try for chart");

      try
      {
        String units = InventoryChartUI.itemUnits.getUnit(model.getNSN());
        System.out.println("Units: " + units);

        chart.setYAxisLabel(units);
      }
      catch (Exception e)
      {
        chart.setYAxisLabel("Quantity");
        System.out.println("No Units Found ");
      }

      System.out.println(model.getNSN());

      // Graphs should be attached in a order that prevents one chart from being hidden by another
      Hashtable dataSets = model.getDataSets();
      DataSet dataSet = null;
      for (Enumeration e=dataSets.keys(); e.hasMoreElements();)
      {
        dataSet = (DataSet)dataSets.get(e.nextElement());

        // Attach the data set to the chart (after adding data???)
        attachDataSet(chart, dataSet);

        // Add the data set to the legend (after attaching to chart so the color is set
        legend.addDataSet(dataSet);
      }

      return(true);
    }

    return(false);
  }

  public void attachDataSet(BlackJackInventoryChart chart, DataSet dataSet)
  {
    // Only batched data sets go on the secondary charts
    if (!dataSet.dataName.startsWith(InventoryScheduleNames.BATCHED_HEADER))
    {
      chart.attachDataSet(dataSet, 0);
    }
    else
    {
      dataSet.dataName = dataSet.dataName.substring(InventoryScheduleNames.BATCHED_HEADER.length());

      if (dataSet.dataGroup.equals(InventoryScheduleNames.SUPPLIER))
      {
        chart.attachDataSet(dataSet, 1);
      }
      else if (dataSet.dataGroup.equals(InventoryScheduleNames.CONSUMER))
      {
        chart.attachDataSet(dataSet, 2);
      }
    }
  }

  public void buildTableModel()
  {
    model = new InventoryTableModel(inventory);
  }

  public Hashtable getDataSets()
  {
    return(model.getDataSets());
  }

  private static PropertyChangeListener lastListener = null;

  public boolean setTableData(String title, JTable table, CChartLegend legend)
  {
    if (inventory != null)
    {
      this.table = table;

      legend.removePropertyChangeListener(lastListener);  // remove previous listener
      legend.addPropertyChangeListener(this);
      lastListener = this;

      //model = new InventoryTableModel(inventory);
      table.setModel(model);
      //table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.sizeColumnsToFit(0);
      TableColumnModel columnModel = table.getColumnModel();
      persistentColumnNames = new String[model.getColumnCount()];
      tblColumn = new TableColumn[model.getColumnCount()];
      for(int i = 0; i < model.getColumnCount(); i++)
      {
        persistentColumnNames[i] = model.getColumnName(i);
        //System.out.println("persistent name " + model.getColumnName(i));
      }
      Enumeration tableColumns = columnModel.getColumns();
      int i = 0;
      while(tableColumns.hasMoreElements())
      {
        tblColumn[i] = (TableColumn) tableColumns.nextElement();
        DataSet dataSet = (DataSet)model.dataSets.get(model.getColumnName(i));
       /* if(dataSet != null)
        {
          LabelIcon icon = new LabelIcon(dataSet);
           TableCellRenderer headerRenderer = tblColumn[i].getHeaderRenderer();
           ((DefaultTableCellRenderer)headerRenderer).setIcon(icon);
          //System.out.println("persistent name " + model.getColumnName(i));
        }*/

        i++;
      }
      
      for (i=0; i<tblColumn.length; i++)
      {
        DataSet dataSet = (DataSet)model.dataSets.get(model.getColumnName(i));
        if ((dataSet != null) && (!dataSet.visible))
        {
          columnModel.removeColumn(tblColumn[i]);
          //System.out.println("persistent name " + model.getColumnName(i));
        }
        tblColumn[i].setPreferredWidth(200);
      }

      table.sizeColumnsToFit(0);

      return(true);
    }

    return(false);
  }

  
  
    public void propertyChange(PropertyChangeEvent e)
    {
      boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
      AdjustCol(e.getPropertyName(), newValue);
    }


  public void AdjustCol( String actionColumn, boolean add)
  {
    TableColumnModel columnModel = table.getColumnModel();
    int currentColumnCount = columnModel.getColumnCount();
    for(int i = 0; i < persistentColumnNames.length; i++)
    {
      if(persistentColumnNames[i].equals(actionColumn))
      {
        if(!add)
        {
          columnModel.removeColumn(tblColumn[i]);
        }
        else
        {
          columnModel.addColumn(tblColumn[i]);
          currentColumnCount = columnModel.getColumnCount();

          if(currentColumnCount > i)
            columnModel.moveColumn(currentColumnCount - 1, i);

        }
      }

    }
    table.sizeColumnsToFit(0);
  }

  class LabelIcon implements Icon
  {
    private DataSet dataSet = null;
    private int width = 10;
    private int height = 10;

    public LabelIcon(DataSet set)
    {
      dataSet = set;
    }
    public int getIconHeight()
    {
      return(height);
    }

    public int  getIconWidth()
    {
      return(width);
    }

    public void paintIcon(Component comp, Graphics g, int x, int y)
    {
      boolean filled = ((dataSet instanceof PolygonFillableDataSet) && ((PolygonFillableDataSet)dataSet).polygonFill);

      Color c = g.getColor();
      g.setColor(comp.getForeground());
      g.drawRect(x, y, width, height);

      g.setColor(dataSet.linecolor);

      if (filled)
      {
        g.fillRect(x+1, y+1, width-1, height-1);
      }
      // Line graph
      else if (dataSet.getClass().equals(PolygonFillableDataSet.class))
      {
        g.fillRect(x+1, y+1, width-1, height-1);
      }
      // Bar graph
      else if (dataSet.getClass().equals(BarDataSet.class))
      {
        g.fillRect(x+1, y+1, width-1, height-1);
        g.setColor(comp.getBackground());
        g.fillRect(x+3, y+3, width-5, height-5);
      }
      // Step graph
      else if (dataSet.getClass().equals(StepDataSet.class))
      {
        g.fillRect(x+1, y+1, width-1, height-1);
      }

      g.setColor(c);
    }
  }
}
