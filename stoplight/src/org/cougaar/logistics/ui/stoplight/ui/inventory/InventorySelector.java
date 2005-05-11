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

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.border.*;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.table.TableColumn;

import java.net.URL;

import org.cougaar.util.OptionPane;



import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.models.*;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

import org.cougaar.mlm.ui.data.UISimpleInventory;
import org.cougaar.mlm.ui.planviewer.ConnectionHelper;
import org.cougaar.mlm.ui.planviewer.XMLClientConfiguration;

public class InventorySelector implements CougaarUI, InventoryDataProvider
{
  private static final int FILL_PATTERN = -2;
  private static final int VISIBLE      = -1;
  private static final int BAR          = 0;
  private static final int FILLED_BAR   = 1;
  private static final int STEP         = 2;
  private static final int FILLED_STEP  = 3;
  private static final int LINE         = 4;
  private static final int FILLED_LINE  = 5;

//  Container container;
  //  south - control panel
  JPanel queryPanel = new JPanel();
  //  center pane - split pane
//  CSplitPane outerSplit = new CSplitPane(JSplitPane.VERTICAL_SPLIT);
//  CSplitPane innerSplit = new CSplitPane(JSplitPane.VERTICAL_SPLIT);
  JComboBox clusterNameBox = new JComboBox();
  JComboBox assetNameBox = new JComboBox();

  boolean doDisplayTable = true;
  boolean listFilled = false;
  String currentAsset = null;
  boolean buildFile = true;
  String clusterHost = "localhost";
  String clusterPort = "8800";
  String SET_ASSET = "Set Asset";
  String SET_CLUSTER = "Set Cluster";
  String hostAndPort = null; // defaults to http://localhost:8800/
  String buttonFileText = "Read Clusters From File";
  String buttonPortText = "Read Clusters From Port";
  Hashtable clusterURLs;
  Hashtable clusterContainer = new Hashtable(1);
  Hashtable clusterData = null;
  
  String clusterName; // set from code base if called as applet
  String fileName = null;
  String cacheFileName = "inventoryCache.inv";
  boolean useCache = false;
  int startParam = 0;
  int endParam = 0;
  static Object messageString = null;
  Container frame = null;
  Vector assetNames;
  DoQuery queryListener = new DoQuery();
  //static final String PSP_id = "GLMINVENTORY.PSP";
  static final String PSP_id = "inventory"; //"GLMINVENTORY.PSP";


//  private BlackJackInventoryChart chart = new BlackJackInventoryChart("", null, "Quantity", true);
  public BlackJackInventoryChart chart = new BlackJackInventoryChart("", null, "Quantity", true);
  private CChartLegend legend = new CChartLegend();
  private JTable table = new JTable(new InventoryTableModel());
  private JScrollPane tableScrollPane = new JScrollPane(table);
  private FileOutputStream ostream = null;
  private ObjectOutputStream objectOutputStream = null;

  private boolean fileBased = false;

  JCheckBoxMenuItem yRangeScrollerMI = null;
  JCheckBoxMenuItem yRangeTickLabelsMI = null;
  JCheckBoxMenuItem yRangeScrollLockMI = null;

  private JMenu inventoryChartMenu = null;
  private JMenu supplierChartMenu = null;
  private JMenu consumerChartMenu = null;
  private Vector dataSetList = null;

  private JLabel dataTipLabel = new JLabel(" ", SwingConstants.LEFT);

  private JDialog cDateDialog = null;
  private JTextField monthField = new JTextField();
  private JTextField dayField = new JTextField();
  private JTextField yearField = new JTextField();

  private javax.swing.Timer refreshTimer = new javax.swing.Timer(30000, new RefreshAction());
  boolean autoDataRefresh = false;
  private JDialog refreshDelayDialog = null;
  private JTextField delayTimeField = new JTextField("30000");

  private String cluster = null;
  private String asset = null;

  private Hashtable mainChartVisibilityList = new Hashtable(1);
  private Hashtable minorChart1VisibilityList = new Hashtable(1);
  private Hashtable minorChart2VisibilityList = new Hashtable(1);

  private FileWriter logFile;

  public InventorySelector()
  {
  }

  public InventorySelector(URL codeBase, Container container)
  {

  }

  public InventorySelector(String host, String port, String file,
                           String incluster, String inasset, long sTime, long eTime)
  {
//    cacheFileName = System.getProperty("cacheFileName", cacheFileName);
    cacheFileName = System.currentTimeMillis() + ".inv";
    
    
    hostAndPort = "http://" + host + ":" + port + "/";
    clusterHost = host;
    clusterPort = port;
    cluster = incluster;
    asset = inasset;
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    startParam = (int)sTime;
    endParam = (int)eTime;
    
    fileName = file;
    
    logFile = getDefaultLogFile();

    if(file != null )
    {
      getFileData(file);
      fileBased = true;
    }

  }

  public static FileWriter getDefaultLogFile() {
      String logFileName = System.getProperty("org.cougaar.log.displaytimes");
      FileWriter logFile = null;
      
      if((logFileName != null) &&
	 (!(logFileName.trim().equals("")))) {
	  try {
	    logFile = new FileWriter(logFileName,true);
	    logFile.write("   Timestamp,    Display time (milliseconds)\n");
	  }
	  catch(IOException except) {
	      System.err.println("Couldn't open file " + logFileName + " to log display times");
	      System.err.println("Error was: " + except);
	    logFile = null;
	  }
      }

      return logFile;
  }
      



  public void install(JFrame installFrame)
  {
    frame = installFrame;

System.out.println("java.version: " + System.getProperty("java.version"));

    if (installFrame.getJMenuBar() == null)
    {
      installFrame.setJMenuBar(new JMenuBar());
    }

    buildControlPanel(installFrame.getContentPane(), installFrame.getJMenuBar());

    if (cluster != null)
    {
      loadInitialData (cluster, asset);
    }

    installFrame.addWindowListener(new WindowAdapter()
      {
        public void windowClosing(WindowEvent e)
        {
          if(buildFile)
            saveObject();
          if (frame instanceof JFrame)
          {
	      if(logFile != null) {
		  try {
		      logFile.flush();
		      logFile.close();
		  }
		  catch(IOException ioe) {
		      System.err.println("Error closing log file: " + ioe);
		  }
	      }
	      System.exit(0);
          }
          else if (frame instanceof JInternalFrame)
          {
          }
        }
      });

    installFrame.show();

    installFrame.validate();
  }
  public void install(JInternalFrame installFrame)
  {
    frame = installFrame;

    if (installFrame.getJMenuBar() == null)
    {
      installFrame.setJMenuBar(new JMenuBar());
    }

    buildControlPanel(installFrame.getContentPane(), installFrame.getJMenuBar());

    if (cluster != null)
    {
      loadInitialData (cluster, asset);
    }

    installFrame.validate();
  }

  public boolean supportsPlaf()
  {
    return true;
  }

  public CMThumbSliderDateAndTimeRangeControl getRangeControl()
  {
    return(chart.xRC);
  }

  public void populate(String newCluster, String newPort)
  {
    loadInitialData(newCluster, newPort);
  }

  public void getFileData(String filename)
  {
    ObjectInputStream in = null;
   try {
   in = new ObjectInputStream (new
                              FileInputStream(filename));
   //currentAsset = null;
   }
   catch (Exception e)
   { //file not found or something wrong with opening it
    System.err.println("error opening  file"  +
                        filename );

    if (frame instanceof JFrame)
    {
      System.exit(1);
    }
    else if (frame instanceof JInternalFrame)
    {
    }
   }

   try
   {
     clusterData = (Hashtable) in.readObject();
   }
   catch (Exception e)
   {
    System.err.println("unable to read data object from " +
                        filename + e);
   }
    try {
       in.close();
    }
    catch (Exception g)
    {
      System.err.println("unable to close profile.dat  : " + g);
    }
  }
  
  
  
  public void buildControlPanel(Container contentPane, JMenuBar menuBar)
  {
    createMenuAndDialogs(contentPane, menuBar);
    JPanel container = new JPanel(new BorderLayout());
    queryPanel.setBorder(new LineBorder(Color.blue));
    assetNameBox.setPreferredSize(new Dimension(500,25));
    queryPanel.setLayout(new FlowLayout());
    queryPanel.add(new JLabel("Org"));


    contentPane.add(container, BorderLayout.CENTER);
    contentPane.add(queryPanel, BorderLayout.SOUTH);
    tableScrollPane.setMinimumSize(new Dimension(10,10));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
//    innerSplit.setOneTouchExpandable(true);
//    innerSplit.setTopComponent(chart);
    if(!InventoryChartUI.tableView)
    {
      container.add(chart, BorderLayout.CENTER);
      container.add(dataTipLabel, BorderLayout.SOUTH);
      chart.setDataTipLabel(dataTipLabel);
    }
    else
     container.add(tableScrollPane, BorderLayout.CENTER);
//    innerSplit.setBottomComponent(tableScrollPane);

//    outerSplit.setOneTouchExpandable(true);
//    outerSplit.setTopComponent(innerSplit);
//    outerSplit.setBottomComponent(legend);


    legend.addPropertyChangeListener(chart);

    legend.setMinimumSize(new Dimension(0,0));
//    tableScrollPane.setMinimumSize(new Dimension(0,8));
    chart.setMinimumSize(new Dimension(0,0));

    GregorianCalendar cal = new GregorianCalendar();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.HOUR, 0);
    monthField.setText("" + (cal.get(Calendar.MONTH) + 1));
    dayField.setText("" + cal.get(Calendar.DAY_OF_MONTH));
    yearField.setText("" + cal.get(Calendar.YEAR));
    chart.setCDate(cal.getTime().getTime());

    chart.setMinimumSize(new Dimension(0,0));
    chart.setShowRightYAxis(false);
    chart.setShowYRangeScroller(false);

//    container.add(outerSplit, BorderLayout.CENTER);

    if(!fileBased)
      addClusterList();
    else
      addClustersFromHash();

    clusterNameBox.addActionListener(new FillAssetList());
    JButton refreshButton = new JButton("Refresh");
    refreshButton.setPreferredSize(new Dimension(100,25));
    refreshButton.addActionListener(new RefreshAction());
    assetNameBox.addActionListener(queryListener);
    queryPanel.add(clusterNameBox);
    queryPanel.add(new JLabel("Items"));
    queryPanel.add(assetNameBox);
    queryPanel.add(refreshButton);


    queryPanel.setPreferredSize(new Dimension(20,50));
//    container.add(queryPanel, BorderLayout.SOUTH);

    contentPane.setSize(800,600);
  }

     public void updateInventoryBox()
     {
     	 
       assetNameBox.removeActionListener(queryListener);
       if(!fileBased)
       {
         if (assetNameBox.getItemCount() != 0)
           assetNameBox.removeAllItems();

         if (clusterNameBox != null)
            clusterName = (String)clusterNameBox.getSelectedItem();
         
         assetNames = getAssets(clusterName);
         if (assetNames != null)
            for (int i = 0; i < assetNames.size(); i++)
              assetNameBox.addItem(assetNames.elementAt(i));
         else
            System.err.println("assets are null");
       }
       else  //  all data coming from file object
       {
         if (assetNameBox.getItemCount() != 0)
           assetNameBox.removeAllItems();
         if (clusterNameBox == null)
         {
           return;
         }
         clusterName = (String) (clusterNameBox.getSelectedItem());
         if (clusterName != null)
         {
//           System.out.println("clusterName " + clusterName);
           Object assets = (Object) clusterData.get(clusterName);
           if(assets != null)
           {
             if(!assets.getClass().getName().equals("java.util.Hashtable"))
               return;
             Enumeration e = ((Hashtable)assets).keys();
             while(e.hasMoreElements())
             {
                String asset = (String) e.nextElement();
                assetNameBox.addItem(asset);
             }
           }
           else
           {
            JOptionPane.showMessageDialog(chart, "No Data in File", "alert", JOptionPane.ERROR_MESSAGE);
           }
        }

       }
      assetNameBox.addActionListener(queryListener);
      return;
    }

    private void loadInitialData (String clusterName, String assetName)
    {
      clusterNameBox.setSelectedItem(clusterName);

      if (assetName != null)
      {
        assetNameBox.setSelectedItem(assetName);
      }
      else
      {
        assetNameBox.setSelectedIndex(0);
      }

    }


  /* Send request to the cluster to get the list of assets
     with scheduled content property groups.
     */

  private Vector getAssets(String clusterName)
  {
    //System.out.println("Submitting: ASSET to: " + clusterName +
    //                   " for: " + PSP_id);
    String clusterURL = (String)clusterURLs.get(clusterName);
    
    if (clusterURL == null)
    {
      System.err.println("Error: No cluster URL found for " + clusterName);
      return(null);
    }
    
    InputStream is = null;
    try
    {
      ConnectionHelper connection =
	new ConnectionHelper(clusterURL, "inventory");
      
        //new ConnectionHelper(clusterURL,
	//                   XMLClientConfiguration.PSP_package, PSP_id);

      
      connection.sendData("ASSET");
      is = connection.getInputStream();
    } catch (Exception e)
    {
      System.err.println("error getting assets");
      e.printStackTrace();
      return null;
    }
    Vector assetNames = null;
    try
    {
      ObjectInputStream p = new ObjectInputStream(is);
      assetNames = (Vector)p.readObject();
    } catch (Exception e)
    {
      
      System.err.println("error getting asset names from stream");
      e.printStackTrace();
    }
    Collections.sort(assetNames);
    //System.out.println("assetsNames " + assetNames);
    return assetNames;
  }

  private void addClustersFromHash()
  {
    //Enumeration e = clusterData.keys();
    
    Vector keys = new Vector();
  	
  	for(Enumeration e = clusterData.keys(); e.hasMoreElements();)
  	{
  		keys.add(e.nextElement());
  	}
  	
    Collections.sort(keys);    
    clusterNameBox.removeAllItems();
//    System.out.println("clusterfromhash");
    
    for(int i = 0; i < keys.size(); i++)
    {
    	String clusterName = (String)keys.elementAt(i);
      clusterNameBox.addItem(clusterName);
      
    }
  }

  private void addClusterList()
  {
    //System.out.println("Querying for cluster list");
    if(clusterHost == null || clusterPort == null)
      return;
    try {
    	//System.out.println("try connection helper");
      ConnectionHelper connection = new ConnectionHelper(hostAndPort);
      //System.out.println("add cluster list host and port " + hostAndPort + " connection " + connection);
      clusterURLs = connection.getClusterIdsAndURLs();
      if (clusterURLs == null) {
        System.err.println("No clusters");
        
      }
    } catch (Exception e) {
      System.err.println(e);
      
    }
    Enumeration names = clusterURLs.keys();
    clusterNameBox.removeAllItems();
    Vector vNames = new Vector();
    while (names.hasMoreElements())
      vNames.addElement(names.nextElement());
    Collections.sort(vNames);
    for (int i = 0; i < vNames.size(); i++)
    {
        clusterNameBox.addItem(vNames.elementAt(i));
        clusterContainer.put(vNames.elementAt(i), new Hashtable(1));

    }
    if (!vNames.isEmpty())
      clusterName = (String)vNames.elementAt(0);
   
  }

  private void createMenuAndDialogs(final Container contentPane, JMenuBar menuBar)
  {
    JMenu fileMenu = (JMenu) menuBar.add(new JMenu("Connections"));
    JMenu menu = null;
    Action action = null;

    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          useCache = ((JCheckBoxMenuItem)e.getSource()).getState();
        }
      };

    MenuUtility.addCheckMenuItem(fileMenu, "Use Cache", 'U', action, false);
    //createMenuItem(frame.fileMenu, "Open", 'O', "", new GetFile());  // open file selection
    //createMenuItem(frame.fileMenu, "Connect", 'T', "", new GetConnectionData());
    // ***** create File menu

   fileMenu.setMnemonic('Z');
   //frame.createMenuItem(fileMenu, "Open", 'O', "", new GetFile());  // open file selection
   //frame.createMenuItem(fileMenu, "Connect", 'T', "", new GetConnectionData());
   JMenuItem openItem = (JMenuItem) fileMenu.add(new JMenuItem("Open"));
   openItem.setMnemonic('O');
   openItem.addActionListener(new GetFile());
   
   JMenuItem saveItem = (JMenuItem) fileMenu.add(new JMenuItem("Save and Exit"));
   saveItem.setMnemonic('S');
   saveItem.addActionListener(new SetFile());

   JMenuItem connectItem = (JMenuItem) fileMenu.add(new JMenuItem("Connect"));
   connectItem.setMnemonic('T');
   connectItem.addActionListener(new GetConnectionData());

   //JMenuItem cacheItem = (JMenuItem) fileMenu.add(new JMenuItem("Use Cache"));
   //cacheItem.setMnemonic('U');



    menu = (JMenu)menuBar.add(new JMenu("Charts"));
    inventoryChartMenu = (JMenu)menu.add(new JMenu("Inventory Chart"));
    supplierChartMenu = (JMenu)menu.add(new JMenu("Supplier Chart"));
    consumerChartMenu = (JMenu)menu.add(new JMenu("Consumer Chart"));
    menu.setMnemonic('C');




    menu = (JMenu)menuBar.add(new JMenu("Chart Options"));
    menu.setMnemonic('O');


    ButtonGroup group = new ButtonGroup();
    ChartViewsCheckListener listener = new ChartViewsCheckListener();
    JMenu chartViews = (JMenu)menu.add(new JMenu("Chart Views"));

    group.add(MenuUtility.addRadioButtonMenuItem(chartViews, "All Charts", listener, "" + BlackJackInventoryChart.SHOW_ALL_CHARTS, true));
    group.add(MenuUtility.addRadioButtonMenuItem(chartViews, "Inventory Chart", listener, "" + BlackJackInventoryChart.SHOW_INVENTORY_CHART, false));
    group.add(MenuUtility.addRadioButtonMenuItem(chartViews, "Supplier Chart", listener, "" + BlackJackInventoryChart.SHOW_SUPPLIER_CHART, false));
    group.add(MenuUtility.addRadioButtonMenuItem(chartViews, "Consumer Chart", listener, "" + BlackJackInventoryChart.SHOW_CONSUMER_CHART, false));





    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setScrollMainChart(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Scroll Inventory Chart (3 Chart View)", action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowTitle(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Show Chart Legends", action, true);


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


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          boolean state = ((JCheckBoxMenuItem)e.getSource()).getState();

          yRangeScrollerMI.setEnabled(!state);
          yRangeTickLabelsMI.setEnabled(!state);
          yRangeScrollLockMI.setEnabled(!state);

          if (state == false)
          {
            chart.setShowYRangeScroller(yRangeScrollerMI.getState());
          }
          else
          {
            chart.setShowYRangeScroller(false);
          }

          chart.setAutoYRange(state);
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Auto Y-Range", action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowYRangeScroller(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    yRangeScrollerMI = MenuUtility.addCheckMenuItem(menu, "Y-Range Scroller", action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowYRangeTickLabels(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    yRangeTickLabelsMI = MenuUtility.addCheckMenuItem(menu, "Y-Range Tick Labels", action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setYRangeScrollLock(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    yRangeScrollLockMI = MenuUtility.addCheckMenuItem(menu, "Lock Y-Range Scroller", action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowGrid(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Display Grid", action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setGridOnTop(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Grid On Top", action, false);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setShowDataTips(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Show Data Tips", action, true);


    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          chart.setUseCDate(((JCheckBoxMenuItem)e.getSource()).getState());
        }
      };
    MenuUtility.addCheckMenuItem(menu, "Use C-Date", 'U', action, false);


    cDateDialog = new JDialog((JFrame)null, "Enter C-Date", true);
    cDateDialog.setResizable(false);
    cDateDialog.getContentPane().setLayout(new BorderLayout());
    cDateDialog.getContentPane().add(getCDateDialogPanel(), BorderLayout.CENTER);
    cDateDialog.pack();
    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          cDateDialog.setLocationRelativeTo(contentPane);
          cDateDialog.show();
        }
      };
    MenuUtility.addMenuItem(menu, "Set C-Date ...", 'S', action);

    refreshDelayDialog = new JDialog((JFrame)null, "Enter Refresh Time", true);
    refreshDelayDialog.setResizable(false);
    refreshDelayDialog.getContentPane().setLayout(new BorderLayout());
    refreshDelayDialog.getContentPane().add(getRefreshDelayDialogPanel(), BorderLayout.CENTER);
    refreshDelayDialog.pack();
    action = new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          refreshDelayDialog.setLocationRelativeTo(contentPane);
          refreshDelayDialog.show();
        }
      };
    MenuUtility.addMenuItem(menu, "Set Data Refresh ...", action);
  }

  


  private JPanel getCDateDialogPanel()
  {
    JPanel panel = new JPanel(new GridLayout(1, 4));
    JButton button = new JButton("Set C-Date");

    button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            int month = Integer.parseInt(monthField.getText());
            int day = Integer.parseInt(dayField.getText());
            int year = Integer.parseInt(yearField.getText());

            GregorianCalendar cal = new GregorianCalendar();
            cal.set(year, month-1, day, 0, 0, 0);

            chart.setCDate(cal.getTime().getTime());
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }

          cDateDialog.hide();
        }
      });

    panel.add(monthField);
    panel.add(dayField);
    panel.add(yearField);
    panel.add(button);

    return(panel);
  }

  private JPanel getRefreshDelayDialogPanel()
  {
    JPanel panel = new JPanel(new GridLayout(3, 1));
    JButton button = new JButton("Set Refresh Delay");

    button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            refreshTimer.setDelay(Integer.parseInt(delayTimeField.getText()));
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }

          refreshDelayDialog.hide();
        }
      });

    JCheckBox checkBox = new JCheckBox("Auto Refresh");
    checkBox.setSelected(autoDataRefresh);
    checkBox.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          autoDataRefresh = ((JCheckBox)e.getSource()).isSelected();
          
          if ((autoDataRefresh) && (assetNameBox.getSelectedItem() != null))
          {
            refreshTimer.restart();
          }
          else
          {
            refreshTimer.stop();
          }
        }
      });

    panel.add(delayTimeField);
    panel.add(checkBox);
    panel.add(button);

    return(panel);
  }









  public void saveObject()
  {
    try
    {
      if(clusterContainer.size() > 0)
      {
        ostream = new FileOutputStream(cacheFileName);
        objectOutputStream = new ObjectOutputStream(ostream);
        objectOutputStream.writeObject(clusterContainer);
        objectOutputStream.flush();
        System.err.println("save object " + clusterContainer.getClass().getName());
        System.err.println("\n\nSaved File Name:  " + cacheFileName + "\n\n");
      }
      else
        System.err.println("don't save object");
    }
    catch(Exception e)
    {
       e.printStackTrace();
    }
  }



  private void setDataSetMenu()
  {
    DataSet[] dataSets = null;
    JMenu menu = null;

    dataSetList = chart.getDataSets();

    inventoryChartMenu.removeAll();
    dataSets = (DataSet[])dataSetList.elementAt(0);
    for (int i=0; i<dataSets.length; i++)
    {
      menu = (JMenu)inventoryChartMenu.add(new JMenu(dataSets[i].dataName));
      addDataSetTypeRadioButtons((PolygonFillableDataSet)dataSets[i], menu);
    }

    supplierChartMenu.removeAll();
    dataSets = (DataSet[])dataSetList.elementAt(1);
    for (int i=0; i<dataSets.length; i++)
    {
      menu = (JMenu)supplierChartMenu.add(new JMenu(dataSets[i].dataName));
      DataSetTypeCheckListener listener = new DataSetTypeCheckListener((PolygonFillableDataSet)dataSets[i], minorChart1VisibilityList);
//      MenuUtility.addCheckMenuItem(menu, "Visible", "" + VISIBLE, listener, dataSets[i].visible);
      addVisibilityCheck(menu, listener, (PolygonFillableDataSet)dataSets[i], minorChart1VisibilityList);
      MenuUtility.addCheckMenuItem(menu, "Use Fill Pattern", "" + FILL_PATTERN, listener, ((dataSets[i] instanceof PolygonFillableDataSet) && ((PolygonFillableDataSet)dataSets[i]).useFillPattern));
    }

    consumerChartMenu.removeAll();
    dataSets = (DataSet[])dataSetList.elementAt(2);
    for (int i=0; i<dataSets.length; i++)
    {
      menu = (JMenu)consumerChartMenu.add(new JMenu(dataSets[i].dataName));
      DataSetTypeCheckListener listener = new DataSetTypeCheckListener((PolygonFillableDataSet)dataSets[i], minorChart2VisibilityList);
//      MenuUtility.addCheckMenuItem(menu, "Visible", "" + VISIBLE, listener, dataSets[i].visible);
      addVisibilityCheck(menu, listener, (PolygonFillableDataSet)dataSets[i], minorChart2VisibilityList);
      MenuUtility.addCheckMenuItem(menu, "Use Fill Pattern", "" + FILL_PATTERN, listener, ((dataSets[i] instanceof PolygonFillableDataSet) && ((PolygonFillableDataSet)dataSets[i]).useFillPattern));
    }
  }

  private void addDataSetTypeRadioButtons(PolygonFillableDataSet dataSet, JMenu menu)
  {
    ButtonGroup group = new ButtonGroup();

    DataSetTypeCheckListener listener = new DataSetTypeCheckListener(dataSet, mainChartVisibilityList);

//    MenuUtility.addCheckMenuItem(menu, "Visible", "" + VISIBLE, listener, dataSet.visible);
    addVisibilityCheck(menu, listener, (PolygonFillableDataSet)dataSet, mainChartVisibilityList);
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

  class ChartViewsCheckListener extends AbstractAction
  {
    public ChartViewsCheckListener()
    {
    }

    public void actionPerformed(ActionEvent e)
    {
      int view = Integer.parseInt(e.getActionCommand());

      switch(view)
      {
        case BlackJackInventoryChart.SHOW_ALL_CHARTS:
          chart.showAllCharts();
        break;

        case BlackJackInventoryChart.SHOW_INVENTORY_CHART:
          chart.showFullInventoryChart();
        break;

        case BlackJackInventoryChart.SHOW_SUPPLIER_CHART:
          chart.showFullSupplierChart();
        break;

        case BlackJackInventoryChart.SHOW_CONSUMER_CHART:
          chart.showFullConsumerChart();
        break;
      }
    }
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
              dataSet = replace(dataSet, new BarDataSet(data, data.length/2, false, InventoryTableModel.barWidth));
            }
          break;

          case FILLED_BAR:
            if (!setPolygonFilled(dataSet, BarDataSet.class, true))
            {
              dataSet = replace(dataSet, new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth));
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

    private void resetChartDataSets()
    {
      chart.detachAllDataSets();
      legend.removeAllDataSets();

      DataSet[] dataSets = null;

      dataSets = (DataSet[])dataSetList.elementAt(0);
      for (int i=0; i<dataSets.length; i++)
      {
        chart.attachDataSet(dataSets[i], 0);
        legend.addDataSet(dataSets[i]);
      }

      dataSets = (DataSet[])dataSetList.elementAt(1);
      for (int i=0; i<dataSets.length; i++)
      {
        chart.attachDataSet(dataSets[i], 1);
      }

      dataSets = (DataSet[])dataSetList.elementAt(2);
      for (int i=0; i<dataSets.length; i++)
      {
        chart.attachDataSet(dataSets[i], 2);
      }
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

  public String getSelectedCluster()
  {
    return(getSelectedItem(clusterNameBox));
  }

  public DataSet[] getAssetDataSets()
  {
    return(chart.mainChart.getDataSets());
  }

  public void setSelectedCluster(String selection)
  {
    setSelectedItem(clusterNameBox, selection);
  }

  public String getSelectedAsset()
  {
    return(getSelectedItem(assetNameBox));
  }

  public void setSelectedAsset(String selection)
  {
    setSelectedItem(assetNameBox, selection);
  }

  public String getFileName()
  {
    return(fileName);
  }

  public String getClusterHost()
  {
    return(clusterHost);
  }

  public String getClusterPort()
  {
    return(clusterPort);
  }

  protected String getSelectedItem(JComboBox comboBox)
  {
    String selected = null;
    if ((comboBox != null) && (comboBox.getSelectedItem() != null))
    {
      selected = (String)comboBox.getSelectedItem();
    }
    
    return(selected);
  }

  protected void setSelectedItem(JComboBox comboBox, String selection)
  {
    if (comboBox != null)
    {
      ComboBoxModel model = comboBox.getModel();
      for (int i=0; i<model.getSize(); i++)
      {
        if (selection.equals(model.getElementAt(i)))
        {
          comboBox.setSelectedItem((String)selection);
          break;
        }
      }
    }
  }

  class RefreshAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      queryListener.performQueryUpdateChart(true,false);
    }
  }

  private void updateChartView()
  {
    // Must set the total range of the chart slider
    chart.resetTotalRange();
    chart.resetRange();

    // Set the slider to be in a two month range from the start of the data
    long msInTwoMonths = 1000L*60L*60L*24L*30L*2L;
    chart.setInitialRange(msInTwoMonths);
    //System.out.println("st " + start);
    if(startParam != 0)
    {
      //System.out.println("range " + startParam + " " + endParam);
      chart.setXScrollerRange(new RangeModel(startParam, endParam));
    }

    setDataSetMenu();
    frame.validate();

    if (autoDataRefresh)
    {
      refreshTimer.restart();
    }
  }

  // -------- InventoryDataProvider interface

  public Hashtable getInventoryData(String clusterName, String assetName)
  {
    System.out.println("getInventoryData: " + clusterName + "   " + assetName);
    
    clusterName = clusterName == null ? getSelectedCluster() : clusterName;
    assetName = assetName == null ? getSelectedAsset() : assetName;

    InventoryQuery query = null;
    Hashtable sets = null;
    if ((clusterName != null) && (assetName != null))
    {
      try
      {
        if(!fileBased && !useCache)
        {
          Hashtable clusterHash = (Hashtable) clusterContainer.get(clusterName);
          query = new InventoryQuery(assetName, clusterName, clusterContainer, clusterHash);
          new QueryHelper(query, hostAndPort + "$" + clusterName + "/", null, null, null, false);
        }
        else if(fileBased)
        {
        	//System.out.println("clusterName " + clusterName);
          Hashtable assetList = (Hashtable) clusterData.get(clusterName);
          UISimpleInventory inventory = (UISimpleInventory) assetList.get(assetName);
          query = new InventoryQuery(inventory, clusterName, clusterContainer);
          new QueryHelper(query, null, null, null);
        }
        else if(useCache)
        {
          if(clusterContainer.size() > 0)
          {
            Hashtable assetList = (Hashtable) clusterContainer.get(clusterName);
            if(assetList.containsKey(assetName))
            {
              System.out.println("from cache");
              UISimpleInventory inventory = (UISimpleInventory) assetList.get(assetName);
              query = new InventoryQuery(inventory, clusterName, clusterContainer);
              new QueryHelper(query, null, null, null);
            }
            else
            {
              System.out.println("from port");
              Hashtable clusterHash = (Hashtable) clusterContainer.get(clusterName);
              query = new InventoryQuery(assetName, clusterName, clusterContainer, clusterHash);
              new QueryHelper(query, hostAndPort + "$" + clusterName + "/", null, null, null, false);
            }
          }
        }
      }
      catch (Throwable t)
      {
        t.printStackTrace();
      }

      if (query != null)
      {
        sets = (Hashtable)query.getDataSets().clone();
      }
    }

    return(sets);
  }

  public String getDefaultAssetName()
  {
    return(getSelectedAsset());
  }

  public String getDefaultOrganizationName()
  {
    return(getSelectedCluster());
  }

  // ---------------------------------------------------------


  class DoQuery implements ActionListener // listens on assetnamebox selecteditemchange
  {
    public void actionPerformed(ActionEvent e)
    {
      System.err.println("doquery filebased: " + fileBased + " usecache " + useCache);
      try
      {
        refreshTimer.stop();

        InventoryQuery query = performQueryUpdateChart(true);
      }
      catch(Throwable ex)
      {
        ex.printStackTrace();
      }
    }


    public InventoryQuery performQueryUpdateChart(boolean setCurrentAsset) {
	  return performQueryUpdateChart(setCurrentAsset, true); 
    }

    public InventoryQuery performQueryUpdateChart(boolean setCurrentAsset,
				   boolean updateChart) {
	long startTime = System.currentTimeMillis();
	InventoryQuery myQuery = performQuery(setCurrentAsset);
	if(updateChart) {
	    updateChartView();
	}
	if(logFile != null) {
	    long endTime = System.currentTimeMillis();
	    long diffTime = endTime - startTime;
	    Date timestamp = new Date(startTime);
	    try {
		logFile.write(timestamp + ", " + diffTime + "\n");
		logFile.flush();
	    }
	    catch (IOException e) {
		System.err.println(e);
	    }
	}

	return myQuery;
    }

    private InventoryQuery performQuery(boolean setCurrentAsset)
    {
      InventoryQuery query = null;

      try
      {
        String assetName = (String)assetNameBox.getSelectedItem();
        String clusterName = (String) clusterNameBox.getSelectedItem();
        if(setCurrentAsset)
          currentAsset = assetName;
        //System.out.println("performQuery currentAsset = " + currentAsset + " setcuurentasset = " + setCurrentAsset);
        if(!fileBased && !useCache)
        {
          Hashtable clusterHash = (Hashtable) clusterContainer.get(clusterName);
          query = new InventoryQuery(assetName, clusterName, clusterContainer, clusterHash);
          new QueryHelper(query, hostAndPort + "$" + clusterName + "/", chart, table, legend, doDisplayTable);
        }
        else if(fileBased)
        {
        	//System.out.println("clusterName " + clusterName);
          Hashtable assetList = (Hashtable) clusterData.get(clusterName);
          UISimpleInventory inventory = (UISimpleInventory) assetList.get(assetName);
          query = new InventoryQuery(inventory, clusterName, clusterContainer);
          new QueryHelper(query, chart, table, legend);
        }
        else if(useCache)
        {
          if(clusterContainer.size() > 0)
          {
            Hashtable assetList = (Hashtable) clusterContainer.get(clusterName);
            if(assetList.containsKey(assetName))
            {
              System.out.println("from cache");
              UISimpleInventory inventory = (UISimpleInventory) assetList.get(assetName);
              query = new InventoryQuery(inventory, clusterName, clusterContainer);
              new QueryHelper(query, chart, table, legend);
            }
            else
            {
              System.out.println("from port");
              Hashtable clusterHash = (Hashtable) clusterContainer.get(clusterName);
              query = new InventoryQuery(assetName, clusterName, clusterContainer, clusterHash);
              new QueryHelper(query, hostAndPort + "$" + clusterName + "/", chart, table, legend, doDisplayTable);
            }
          }
        }
        if (frame instanceof JFrame)
        {
          ((JFrame)frame).setTitle(query.model.getAssetName());
        }
        else if (frame instanceof JInternalFrame)
        {
          ((JInternalFrame)frame).setTitle(query.model.getAssetName());
        }
        
      }
      catch(Throwable ex)
      {
        ex.printStackTrace();
      }

      return(query);
    }
  }

  class InvFilter extends FileFilter
  {
    String suffix = ".inv";
    public boolean accept(File name)
    {
      String f = name.getName();
      return f.indexOf(suffix) != -1;
    }
    public String getDescription()
    {
      return "filefilter";
    }
  }

  class FillAssetList implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {

      //System.out.println("fillassetlist ");
      listFilled = false;
      boolean foundMatch = false;
      clusterName = (String)clusterNameBox.getSelectedItem();
      if(currentAsset != null && clusterName != null)
      {
      	//  find the asset which matches int he selected cluster or if none matches
      	//  bring up the first asset (if one exists)
      	//  then perform doquery with this cluster and asset
      	
      	//clusterName = (String)clusterNameBox.getSelectedItem();
      	//System.out.println("clustername at fill " + clusterName);
      	Vector myAssetNames = new Vector();
      	updateInventoryBox();
      	if(fileBased)
      	{
      		Hashtable assetList = (Hashtable) clusterData.get(clusterName);
          
          
          for(Enumeration en = assetList.keys(); en.hasMoreElements();)
          {
          	myAssetNames.add((String)en.nextElement());
          }
        }
      	else
      	  myAssetNames = assetNames;
      	
      	
      	//System.out.println("assetnames size is " + myAssetNames.size());
      	if(myAssetNames != null && myAssetNames.size() > 0)
      	{
	      	for(int i = 0; i < myAssetNames.size(); i++)
	      	{
	      		String matchAsset = (String) myAssetNames.elementAt(i);
	      		//System.out.println("next asset " + matchAsset + " " + currentAsset + " " + i);
	      		if(matchAsset.equals(currentAsset))
	      		{
	      			//  do performQuery with current asset
	      			//  set currentAsset as selected item
	      			assetNameBox.setSelectedItem(currentAsset);

				queryListener.performQueryUpdateChart(false);

	      			foundMatch = true;
	      			break;
	      		}
	      		
	      	}
	      	if(!foundMatch)
		  		{
		  			//  do performQuery with 1st asset
		  			//  but don't reset currentAsset
		  			//  set 1st item as selected item
		  			//System.out.println("setting to first item");
		  			assetNameBox.removeActionListener(queryListener);
		  			assetNameBox.setSelectedItem(myAssetNames.elementAt(0));
		  			assetNameBox.addActionListener(queryListener);
		  			queryListener.performQueryUpdateChart(false);
		   		}
	      }
	      else
	      {
	      	//  nothing to display
	      	//System.out.println("removing all");
	      	if (frame instanceof JFrame)
          {
            ((JFrame)frame).setTitle("");
          }
          else if (frame instanceof JInternalFrame)
          {
            ((JInternalFrame)frame).setTitle("");
          }
	      	chart.detachAllDataSets();
          legend.removeAllDataSets();
          chart.repaint();
	      }
      }
      else if(clusterNameBox.getSelectedItem() != null )
        updateInventoryBox();
    }
  }

  public class SetFile extends AbstractAction
   {

     public void actionPerformed(ActionEvent e)
     {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir") );
        fc.setFileFilter(new InvFilter());
        if(fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
          //System.err.println("file is " + fc.getSelectedFile());
          cacheFileName = fc.getSelectedFile().getName();
          if(buildFile)
            saveObject();
          if (frame instanceof JFrame)
          {
            System.exit(0);
          }
          else if (frame instanceof JInternalFrame)
          {
          }
        }
     }
   }

  public class GetFile extends AbstractAction
   {

     public void actionPerformed(ActionEvent e)
     {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir") );
        fc.setFileFilter(new InvFilter());
        if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
        {
          System.err.println("file is " + fc.getSelectedFile());
          fileName = fc.getSelectedFile().getName();
          getFileData(fileName);
          fileBased = true;
          addClustersFromHash();
        }
     }
   }

  public void loadInventoryFile(String fileName)
  {
    getFileData(fileName);
    fileBased = true;
    addClustersFromHash();
  }

   public class GetConnectionData extends AbstractAction
   {
     public void actionPerformed(ActionEvent e)
     {
        String msg = "Enter cluster Log Plan Server location as host:port";

        String host = "localhost";
        String port = "8800";

        String defaultString = host + ":" + port;
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Cluster Location", 3, null, null, defaultString)) == null)
        {
          return;
        }
        
        String s = (String)messageString;
        s = s.trim();
        if (s.length() != 0)
        {
          int i = s.indexOf(":");
          if (i != -1)
          {
            host = s.substring(0, i);
            port = s.substring(i+1);
          }
        }
        clusterHost = host;
        clusterPort = port;
        hostAndPort = "http://" + clusterHost + ":" + clusterPort + "/";
        fileName = null;
        fileBased = false;
        addClusterList();
     }
   }
}
