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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.awt.dnd.*;
import java.applet.*;

import javax.swing.*;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.MenuUtility;

import org.cougaar.util.OptionPane;


public class ReadinessChartApplet extends JApplet
{
	private static final int FILL_PATTERN = -2;
  private static final int VISIBLE      = -1;
  private static final int BAR          = 0;
  private static final int FILLED_BAR   = 1;
  private static final int STEP         = 2;
  private static final int FILLED_STEP  = 3;
  private static final int LINE         = 4;
  private static final int FILLED_LINE  = 5;
  public Vector dataSetVector = null;
	private int totalCharts = 0;
	public static final double barWidth = 2.0 * 60.0 * 60.0 * 60.0 * 1000;
	private JMenu inventoryChartMenu = null;
	static Object messageString = null;
	NChart chart = null;
	//Container frame = null;
	Container frame = null;
	private JMenu nChartMenu = null;
	public Vector dataSetList = null;
	private Hashtable mainChartVisibilityList = new Hashtable(1);
	private boolean twoUp = true;
	
	public static final String NUMMAINTAINED = "numMaintainedItems";
  public static final String MAINTAINEDCLASSNAME = "maintainedClassName_";
  public static final String NUMDIRECTOBJS = "numAssets_";
  public static final String ASSETNAMEPARAM = "AssetName_";
  public static final String NUMASPECTITEMS = "numReadinessGroups_";
  public static final String READINESSPARAM = "readiness_";
  public static final String ATDATEPARAM = "asOfDate_";
  
  //private Vector stores = new Vector();
	private Hashtable stores = new Hashtable();
	JComboBox assetNameBox = new JComboBox();
	SelectAsset queryListener = new SelectAsset();
	JPanel queryPanel = new JPanel();
	JScrollPane jChartScrollPane = null;
	
	
	public ReadinessChartApplet()
	{
		totalCharts = 1;
		System.out.println("%%%% set to 1 chart outside applet");
	}
	
	
	
  
  public void init()
  {
  	frame = this;
  	setJMenuBar(new JMenuBar());
  	buildChart();
  	JScrollPane jChartScrollPane = new JScrollPane(chart);
    getContentPane().add(jChartScrollPane, BorderLayout.CENTER);
  	
  	pspData();
  	
  	//queryPanel.setBorder(new LineBorder(Color.blue));
    queryPanel.setLayout(new FlowLayout());
    queryPanel.add(new JLabel("Assets"));
  	
  	for(Enumeration enum = stores.keys(); enum.hasMoreElements();)
    {
  	  assetNameBox.addItem((String)enum.nextElement());
  	}
  	assetNameBox.addActionListener(queryListener);
  	queryPanel.add(assetNameBox);
  	getContentPane().add(queryPanel, BorderLayout.NORTH);
  	
  	
  	
    setDataSetMenu();
    setSize(600, 600); 	
    frame.validate();
  }
  
  public void buildChart()
  {
  	System.out.println("%%%% call createMenuAndDialogs");
  	createMenuAndDialogs(getContentPane(), getJMenuBar());
  	double space = 0.0;
  	chart = new NChart(totalCharts, "Time", "Asset", this, space);
  	JScrollPane jChartScrollPane = new JScrollPane(chart);
  	getContentPane().add(jChartScrollPane, BorderLayout.CENTER);
  }
  
  
  public Vector initialData()
  {
  	double[] data1 = new double[] {0.0, 0.0};
  	double[] data2 = new double[] {0.0, 0.0};
  	double[] data3 = new double[] {0.0, 0.0};
  	double[] data4 = new double[] {0.0, 0.0};
  	
  	
  	Vector data1V = new Vector();
  	
	  	try
	  	{
	  		
	  		//  chart 1
	  		DataSet d1a = new PolygonFillableDataSet(data1, data1.length/2, false);
	  		d1a.dataName = "d1a";
	  		data1V.add(d1a);
	  		
	  		
	  		
	  		
	  		
	  	  	  
	    }
	    catch(Exception e)
	    {
	  	
	  }
	  dataSetVector = new Vector(1);
	  dataSetVector.add(data1V);
  	
	  	
	  return dataSetVector;
  }
  
  public Vector testData()
  {
  	double[] data1 = new double[] {0.0, 10.0, 20.0, 5.0, 30.0, 40.0, 50.0, 14.0};
  	double[] data2 = new double[] {0.0, 50.0, 20.0, 7.0, 30.0, 50.0, 40.0, 25.0};
  	double[] data3 = new double[] {0.0, 14.0, 20.0, 78.0, 30.0, 60.0, 40.0, 35.0};
  	double[] data4 = new double[] {0.0, 16.0, 20.0, 11.0, 30.0, 5.0, 40.0, 45.0};
  	
  	
  	Vector data1V = new Vector();
  	
	  	try
	  	{
	  		
	  		//  chart 1
	  		DataSet d1a = new PolygonFillableDataSet(data1, data1.length/2, false);
	  		d1a.dataName = "d1a";
	  		data1V.add(d1a);
	  		DataSet d2a = new PolygonFillableDataSet(data2, data2.length/2, false);
	  		d2a.dataName = "d2a";
	  		data1V.add(d2a);
	  		DataSet d3a = new PolygonFillableDataSet(data3, data3.length/2, false);
	  		d3a.dataName = "d3a";
	  		data1V.add(d3a);
	  		DataSet d4a = new PolygonFillableDataSet(data4, data4.length/2, false);
	  		d4a.dataName = "d4a";
	  		data1V.add(d4a);
	  		
	  		
	  		
	  	  	  
	    }
	    catch(Exception e)
	    {
	  	
	  }
	  dataSetVector = new Vector(1);
	  dataSetVector.add(data1V);
  	
	  	
	  return dataSetVector;
  }
  
  
  
  public void pspData()
  {
  	System.out.println("%%%% pspdata \'" + getParameter(NUMMAINTAINED) + "\'");
  	int numSupplyClasses = Integer.parseInt(getParameter(NUMMAINTAINED));
    int numDirectObjs;
    int numItems = 0;
    double[] dataSet = null;
    // all the data
    for (int chartNum = 1; chartNum <= numSupplyClasses; chartNum ++)
    {
    	String classString = getParameter (new String (MAINTAINEDCLASSNAME + chartNum));
    	if(classString == null)
    	{
    		System.out.println("%%%% null classString for " + MAINTAINEDCLASSNAME + chartNum);
    		continue;
    	}
      String maintName = new String (classString );
      System.out.println("%%%% classString is " + classString);
      numDirectObjs = Integer.parseInt (getParameter (new String (NUMDIRECTOBJS + chartNum)) );
      Vector oneChartVecs[] = new Vector [numDirectObjs];
      String assetName = new String();
      
      // asset data
      for (int dirObjNum = 1; dirObjNum <= numDirectObjs; dirObjNum ++)
      {
      	String assetString = getParameter ( new String (ASSETNAMEPARAM + chartNum + "_" + dirObjNum ) );
      	String numItemsString = getParameter ( new String (NUMASPECTITEMS + chartNum + "_" + dirObjNum ) );
      	if(assetString != null)
          assetName = new String (assetString );
        if(numItemsString != null)
        {
          numItems = Integer.parseInt (numItemsString);
          
        }
        else
        {
        	System.out.println("%%%% null numItems for " + chartNum + "_" + dirObjNum); 
          continue;
        }
        dataSet = new double [numItems*2];
        int itemNum, loadIdx;
        // one set of double array data?
        
        for (itemNum = 1, loadIdx = 0; itemNum <= numItems; itemNum ++, loadIdx +=2)
        {
             //System.out.println ("\t" + ATDATEPARAM + chartNum + "_" + dirObjNum + "_" + itemNum  );
             //System.out.println("%%%% date param " + getParameter (new String (ATDATEPARAM + chartNum + "_" + dirObjNum + "_" + itemNum )));
             //System.out.println ("\t" + READINESSPARAM + chartNum + "_" + dirObjNum + "_" + itemNum );
           // x axis go in even indices, y axis goes in odd indices
           String dateParam = getParameter (new String (ATDATEPARAM + chartNum + "_" + dirObjNum + "_" + itemNum ));
           if(dateParam != null)
           {
	           Double standardDouble = new Double(dateParam);
	           double myDouble = standardDouble.doubleValue();
	           dataSet [loadIdx] = toMillis (myDouble);
	           System.out.println("%%%% data " + myDouble);
          }
          String dataParam = getParameter (new String (READINESSPARAM + chartNum + "_" + dirObjNum + "_" + itemNum ));
          if(dataParam != null)
          {
	           Double standardDouble = new Double(dataParam);
	           double myDouble = standardDouble.doubleValue();
	           dataSet [loadIdx+1] = myDouble;
	           System.out.println("%%%% data " + myDouble);
           }
        }
        try
        {
	        //DataSet d1a = new PolygonFillableDataSet(dataSet, dataSet.length/2, false);
	        DataSet d1a = new StepDataSet(dataSet, dataSet.length/2, false);
		  		d1a.dataName = assetName;
		  		d1a.yAxisLabel = maintName + " - " + assetName;
		  		Vector data1V = new Vector();
		  		data1V.add(d1a);
	        System.out.println("&&&& name " + maintName + " - " + assetName); 
	        stores.put(new String (maintName + " - " + assetName), data1V); // put the vector of dataset into hashtable
	        //stores.add(data1V);
      }
      catch(Exception e)
      {
      	
      }
      }
    }
  }
  
  public static double toMillis (double baseDate)
  {

    GregorianCalendar gregBase = new GregorianCalendar ();
    gregBase.set ( (int) (baseDate / 10000), (int) (baseDate % 10000) / 100 - 1, (int) (baseDate % 100) );
    long baseMillis = gregBase.getTime().getTime();
    return baseMillis;
  }
  
  
  
  // Boiler plate for HotJava
  public String getAppletInfo () {
    return "Pie 1997-06-14 THVV";
  } // getAppletInfo
  
  public String [][] getParameterInfo () {
    String [][] info = {
    };
    return info;
  } // getParameterInfo
  
  private void createMenuAndDialogs(final Container contentPane, JMenuBar menuBar)
  {
  	System.out.println("%%%% nchartmenu begin");
    //JMenu fileMenu = (JMenu) menuBar.add(new JMenu("Chart Layout"));
    JMenu menu = null;
    Action action = null;
      
   //JMenuItem chartUp = (JMenuItem) fileMenu.add(new JMenuItem("Move Chart Up"));
   //chartUp.setMnemonic('U');
   //chartUp.addActionListener(new MoveChartUp());
   
   //JMenuItem chartDown = (JMenuItem) fileMenu.add(new JMenuItem("Move Chart Down"));
   //chartDown.setMnemonic('D');
   //chartDown.addActionListener(new MoveChartDown());
   
   //JMenuItem removeChart = (JMenuItem) fileMenu.add(new JMenuItem("Remove Chart"));
   //.setMnemonic('D');
   //removeChart.addActionListener(new RemoveChart());
   
   //JMenuItem layoutChart = (JMenuItem) fileMenu.add(new JMenuItem("Toggle side-by-side"));
   //layoutChart.setMnemonic('D');
   //layoutChart.addActionListener(new LayoutChart());
   
   //JMenu testMenu = (JMenu) menuBar.add(new JMenu("Test"));
   //JMenuItem load = (JMenuItem) testMenu.add(new JMenuItem("Load Test Data"));
   //load.setMnemonic('L');
   //load.addActionListener(new LoadData());
   
    menu = (JMenu)menuBar.add(new JMenu("Chart Options"));
    menu.setMnemonic('O');


    
   
   
   action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowLeftYAxis(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Left Y-Axis", 'L', action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowRightYAxis(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Right Y-Axis", 'R', action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowXRangeScroller(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "X-Range Scroller", action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowXRangeTickLabels(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "X-Range Tick Labels", action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setXRangeScrollLock(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Lock X-Range Scroller", action, false);
   
   
   
   
   //  graph types
   System.out.println("%%%% nchartmenu ");
   nChartMenu = (JMenu)menuBar.add(new JMenu("Charts"));
    
    
  }
  
  
    class DataSetTypeCheckListener extends AbstractAction
  {
    private PolygonFillableDataSet dataSet = null;
    private Hashtable visibilityList = null;

    public DataSetTypeCheckListener(PolygonFillableDataSet dataSet, Hashtable visibilityList)
    {
      this.dataSet = dataSet;
      this.visibilityList = visibilityList;
    }

    public void actionPerformed(ActionEvent e)
    {
      try
      {
        int newType = Integer.parseInt(e.getActionCommand());
        double[] data = dataSet.getData();

        switch(newType)
        {
          case BAR:
            if (!setPolygonFilled(dataSet, BarDataSet.class, false))
            {
            	dataSet = replace(dataSet, new BarDataSet(data, data.length/2, false, barWidth));
            }
          break;

          case FILLED_BAR:
            if (!setPolygonFilled(dataSet, BarDataSet.class, true))
            {
            	dataSet = replace(dataSet, new BarDataSet(data, data.length/2, true, barWidth));
            }
          break;

          case STEP:
            if (!setPolygonFilled(dataSet, StepDataSet.class, false))
            {
            	dataSet = replace(dataSet, new StepDataSet(data, data.length/2, false));
            	((StepDataSet)dataSet).endPointLead = 60L*60L*24L;
            }
          break;

          case FILLED_STEP:
            if (!setPolygonFilled(dataSet, StepDataSet.class, true))
            {
            	dataSet = replace(dataSet, new StepDataSet(data, data.length/2, true));
              ((StepDataSet)dataSet).endPointLead = 60L*60L*24L;
            }
          break;

          case LINE:
            if (!setPolygonFilled(dataSet, PolygonFillableDataSet.class, false))
            {
            	dataSet = replace(dataSet, new PolygonFillableDataSet(data, data.length/2, false));
            }
          break;

          case FILLED_LINE:
            if (!setPolygonFilled(dataSet, PolygonFillableDataSet.class, true))
            {
            	dataSet = replace(dataSet, new PolygonFillableDataSet(data, data.length/2, true));
            }
          break;

          case VISIBLE:
            if (e.getSource() instanceof JCheckBoxMenuItem)
            {
              chart.setVisible(dataSet, ((JCheckBoxMenuItem)e.getSource()).getState());
              Hashtable groupVisibility = (Hashtable)visibilityList.get(dataSet.dataGroup);
              groupVisibility.put(dataSet.dataName, new Boolean(dataSet.visible));
            }
          break;

          case FILL_PATTERN:
            if (e.getSource() instanceof JCheckBoxMenuItem)
            {
              dataSet.useFillPattern = ((JCheckBoxMenuItem)e.getSource()).getState();
            }
          break;
        }

        resetChartDataSets();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }

      chart.repaint();
    }

    private boolean setPolygonFilled(PolygonFillableDataSet dataSet, Class dataSetClass, boolean filled)
    {
      if (dataSet.getClass().equals(dataSetClass))
      {
        dataSet.polygonFill = filled;
        return(true);
      }

      return(false);
    }


/****************************************************************************************
/*
/*************************************************************************************/
    private void resetChartDataSets()
    {
      chart.detachAllDataSets();
      Vector newData = new Vector();
      DataSet[] dataSets = null;
      DataSet lastData = null;
      for(int i = 0; i < dataSetList.size(); i++)
      {
      	dataSets = (DataSet[])dataSetList.elementAt(i); 
      	Vector newElement = new Vector();
      	for(int j = 0; j < dataSets.length; j++)
      	{
      		newElement.add(dataSets[j]);
      		lastData = dataSets[j];
      		
      	}
      	newData.add(newElement);
      }
      chart.setDataIntoChart(newData, false);
      System.out.println("%%%% setting label to " + lastData.yAxisLabel);
      chart.chartElement[0].setYAxisLabel(lastData.yAxisLabel);
      setDataSetMenu();
    }

    private PolygonFillableDataSet replace(PolygonFillableDataSet oldSet, PolygonFillableDataSet newSet)
    {
      DataSet[] dataSets = null;

      for (int i=0, isize=dataSetList.size(); i<isize; i++)
      {
        dataSets = (DataSet[])dataSetList.elementAt(i);

        for (int j=0; j<dataSets.length; j++)
        {
          if (dataSets[j].equals(oldSet))
          {
            newSet.visible = oldSet.visible;
            newSet.dataGroup = oldSet.dataGroup;
            newSet.dataName = oldSet.dataName;
            newSet.yAxisLabel = oldSet.yAxisLabel;
            newSet.automaticallySetColor = oldSet.automaticallySetColor;
            newSet.colorNumber = oldSet.colorNumber;
            newSet.linecolor = oldSet.linecolor;
            newSet.useFillPattern = oldSet.useFillPattern;
            newSet.xValueOffset = oldSet.xValueOffset;
            newSet.yValueOffset = oldSet.yValueOffset;
            dataSets[j] = newSet;

            return(newSet);
          }
        }
      }

      return(null);
    }
  }
  
  
  
  
  private void addDataSetTypeRadioButtons(PolygonFillableDataSet dataSet, JMenu menu)
  {
    ButtonGroup group = new ButtonGroup();

    DataSetTypeCheckListener listener = new DataSetTypeCheckListener(dataSet, mainChartVisibilityList);

    MenuUtility.addCheckMenuItem(menu, "Use Fill Pattern", "" + FILL_PATTERN, listener, ((dataSet instanceof PolygonFillableDataSet) && ((PolygonFillableDataSet)dataSet).useFillPattern));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Bar Chart", listener, "" + BAR, (dataSet instanceof BarDataSet) && (!dataSet.polygonFill)));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Filled Bar Chart", listener, "" + FILLED_BAR, (dataSet instanceof BarDataSet) && (dataSet.polygonFill)));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Step Chart", listener, "" + STEP, (dataSet instanceof StepDataSet) && (!dataSet.polygonFill)));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Filled Step Chart", listener, "" + FILLED_STEP, (dataSet instanceof StepDataSet) && (dataSet.polygonFill)));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Line Chart", listener, "" + LINE, (dataSet.getClass().equals(PolygonFillableDataSet.class)) && (!dataSet.polygonFill)));

    group.add(MenuUtility.addRadioButtonMenuItem(menu, "Filled Line Chart", listener, "" + FILLED_LINE, (dataSet.getClass().equals(PolygonFillableDataSet.class)) && (dataSet.polygonFill)));
  }
  
  private void addVisibilityCheck(JMenu menu, DataSetTypeCheckListener listener, PolygonFillableDataSet dataSet, Hashtable visibilityList)
  {
    Hashtable groupVisibility = (Hashtable)visibilityList.get(dataSet.dataGroup);
    if (groupVisibility == null)
    {
      groupVisibility = new Hashtable(1);
      visibilityList.put(dataSet.dataGroup, groupVisibility);
    }
    
    Boolean visible = (Boolean)groupVisibility.get(dataSet.dataName);
    if (visible == null)
    {
      visible = new Boolean(dataSet.visible);
      groupVisibility.put(dataSet.dataName, visible);
    }
    
    dataSet.visible = visible.booleanValue();
    
    MenuUtility.addCheckMenuItem(menu, "Visible", "" + VISIBLE, listener, dataSet.visible);
  }
  
  public void setDataSetMenu()
  {
    DataSet[] dataSets = null;
    JMenu[] menu = null;
    JMenu jMenu = null;
    
       
    dataSetList = chart.getDataSets();
    menu = new JMenu[dataSetList.size()];
    System.out.println("%%%% list size is " + dataSetList.size());
    nChartMenu.removeAll();
    for(int i = 0; i < dataSetList.size(); i++)
    {
    	menu[i] = (JMenu)nChartMenu.add(new JMenu("Chart"));
    }
    //for(int cindex = 0; cindex < dataSetList.size(); cindex++)
    //{   
	    dataSets = (DataSet[])dataSetList.elementAt(0);
	    if(dataSets.length != 0)
	    {
	      System.out.println("%%%% set dataname " + dataSets[0].dataName);
	      jMenu = (JMenu)menu[0].add(new JMenu(dataSets[0].dataName));
	      addDataSetTypeRadioButtons((PolygonFillableDataSet)dataSets[0], jMenu);
	    }
	    
    //}
   
  }
  
  
  
  
  public boolean supportsPlaf()
  {
    return(true);
  }
  
  public class SwapCharts extends AbstractAction
   {

     public void actionPerformed(ActionEvent e)
     {
        chart.swapCharts(1, 2);
     }
   }
  
  public class MoveChartUp extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
        String msg = "Enter Chart Number";
        String defaultString = "1";
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Chart Number", 3, null, null, defaultString)) == null)
        {
          return;
        }
        
        String s = (String)messageString;
        s = s.trim();
        if (s.length() != 0)
        {
          int chartNum = Integer.parseInt(s) - 1;
          if(chartNum > 1)
            chart.swapCharts(chartNum, chartNum - 1);
            setDataSetMenu();
        }
        
     }
   }
   
   public class MoveChartDown extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
        String msg = "Enter Chart Number";
        String defaultString = "1";
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Chart Number", 3, null, null, defaultString)) == null)
        {
          return;
        }
        
        String s = (String)messageString;
        s = s.trim();
        if (s.length() != 0)
        {
          int chartNum = Integer.parseInt(s) - 1;
          chart.swapCharts(chartNum, chartNum + 1);
          setDataSetMenu();
        }
        
     }
   }
   
   
   public class LayoutChart extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
     	if(twoUp)
     	  twoUp = false;
     	else
     	  twoUp = true;
     	chart.setTwoUp(twoUp); 
     	chart.redrawCharts();
     }
     
   }
   
   public class RemoveChart extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
        String msg = "Enter Chart Number";
        String defaultString = "1";
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Chart Number", 3, null, null, defaultString)) == null)
        {
          return;
        }
        
        String s = (String)messageString;
        s = s.trim();
        if (s.length() != 0)
        {
          //System.out.println("entered " + s);
          int chartNum = Integer.parseInt(s) - 1;
          chart.removeCharts(chartNum);
          setDataSetMenu();
        }
        chart.redrawCharts();
        
     }
   }
   
   
   
   class SelectAsset implements ActionListener // listens on assetnamebox selecteditemchange
  {
    public void actionPerformed(ActionEvent e)
    {
      System.err.println("SelectAsset: " );
      try
      {
        String assetName = (String)assetNameBox.getSelectedItem();
        if(assetName == null)
          return;
        
        Vector dataVector = new Vector(1);
        Vector dataSetVector = (Vector) stores.get(assetName);
        DataSet dataSet = (DataSet)dataSetVector.elementAt(0); 
        dataVector.add(dataSetVector);
        chart.setDataIntoChart(dataVector, false);
        setDataSetMenu();
        chart.chartElement[0].setYAxisLabel(dataSet.yAxisLabel);
        
        
      }
      catch(Throwable ex)
      {
        ex.printStackTrace();
      }
    }
  }
  
  public class LoadData extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
        Vector graphData = testData();
        chart.setDataIntoChart(graphData, false);
        setDataSetMenu();
        
     }
   }

}
