/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.security.AccessControlException;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;

import java.util.Date;

import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JSeparator;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JApplet;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;

import org.cougaar.mlm.ui.newtpfdd.producer.ClusterCache;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.QueryHandler;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;
import javax.swing.Icon;
import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import org.cougaar.mlm.ui.newtpfdd.gui.view.route.RouteViewRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.route.RouteView;

import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetDetailView;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.CarrierDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.CarrierDetailView;

import org.cougaar.mlm.ui.newtpfdd.gui.view.statistics.StatisticsPane;
import org.cougaar.mlm.ui.newtpfdd.gui.view.statistics.GraphPane;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

public class NewTPFDDShell extends JApplet implements ActionListener,
						      TPFDDConstants, DatabaseState, PopupDialogSupport
{
  private JMenu ivjactionMenu = null;
  private JMenu ivjclusterMenu = null;
  private JMenuItem ivjtaskItem = null;
  private JMenuItem ivjnewViewItem = null;
  private JMenuItem ivjretryAggregationInitItem = null;
  private JMenuItem ivjreloadAggregationItem = null;
  private JMenuBar ivjTPFDDShellMenuBar = null;
  private JCheckBoxMenuItem ivjmessageItem = null;
  private JCheckBoxMenuItem ivjganttItem = null;
  private JSeparator ivjpanelMenuSeparator = null;
  private JCheckBoxMenuItem ivjlogPlanItem = null;
  private JCheckBoxMenuItem ivjassetUsageItem = null;
  private JMenu ivjpanelMenu = null;
  private JRadioButtonMenuItem ivjstackItem = null;
  private JRadioButtonMenuItem ivjtileItem = null;
  private JPanel ivjcontainerPanel = null;
  private JPanel ivjmessageTab = null;
  private JPanel ivjganttTab = null;
  private JPanel ivjlogPlanTab = null;
  private JPanel ivjassetUsageTab = null;
  private JTabbedPane ivjtabbingPane = null;
  private JPanel ivjtilingPane = null;

  private static boolean debug = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.debug"));

  public static String TPFDDString = 
    System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.TPFDDString", "TPFDD"); 
  private static String logPlanString = 
    System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.logPlanString", "Log Plan View");
  private static String assetUsageString = 
    System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.assetUsageString", "Asset Usage View");
  private static String appTitle = 
    System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.appTitle", "TPFDD Viewer");
  private static boolean showRunMenu = 
    "true".equals(System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell.showRunMenu", 
				      "true"));
  
  private static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
  private static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

  private JLabel machineText = null;

  // user code members
  // gui
  private Vector clusterMenuItems;
  private JMenuItem selectedClusterItem = null;
  private NewLogPlanView logPlanView = null;
  private AssetUsageView assetUsageView = null;
  private JScrollPane debugScroller = null;
  private JScrollPane statusScroller = null;

  // data store
  private ClusterCache clusterCache;
  private DatabaseConfig dbConfig = null;
  Map itemToRun = new HashMap ();
  DatabaseRun run;
  JMenuItem lastItemSelected = null;
  Icon check = new Icon () {
      public int getIconWidth  () { return 8; }
      public int getIconHeight () { return 8; }
      public void paintIcon (Component c, Graphics g, int x, int y) {
	g.setColor(TPFDDColor.TPFDDGreen);
	g.fillRect(x, y, 8, 8);
      }
	  
    };

  // applet settable parameters
  private String startDateString;
  private int fontSize;

  /**
   * Gets the applet information.
   * @return String
   */
  public String getAppletInfo()
  {
    return "ALP Transportation Movement Visualization Tool";
  }

  private NewLogPlanView getlogPlanView()
  {
    if ( logPlanView == null ) {
      try {
	Debug.out("TS:gP new LPV");
	logPlanView = new NewLogPlanView(this, fontSize);
      } catch (RuntimeException ivjExc) {
	handleException(ivjExc);
      }
    }
    return logPlanView;
  }

  private AssetUsageView getassetUsageView()
  {
    if ( assetUsageView == null ) {
      try {
	Debug.out("TS:gP new AUV");
	assetUsageView = new AssetUsageView(this, fontSize);
      } catch (RuntimeException ivjExc) {
	handleException(ivjExc);
      }
    }
    return assetUsageView;
  }

  private ClusterCache getclusterCache()
  {
    if ( clusterCache == null ) {
      try {
	Debug.out("TS:gCC new CC");
	clusterCache = new ClusterCache(true);
      } catch (RuntimeException ivjExc) {
	handleException(ivjExc);
      }
    }
    return clusterCache;
  }
	    
  /*
    private ClientPlanElementProvider getprovider()
    {
    if ( provider == null ) {
    try {
    Debug.out("TS:gP new PEP");
    provider = new ClientPlanElementProvider(getclusterCache(), false);
    } catch (Exception ivjExc) {
    handleException(ivjExc);
    }
    }
    return provider;
    }
  */
  private JScrollPane getDebugScroller()
  {
    if ( debugScroller == null ) {
      try {
	MessageArea debugMessages =
	  new MessageArea("Debugging log -- for programmers and testers\n", 10, 80);
	debugMessages.setEditable(false);
	debugScroller = new JScrollPane(debugMessages);
	debugMessages.setScrollBar(debugScroller.getVerticalScrollBar());
	debugScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	//		Debug.getHandler().addConsumer(debugMessages);
      } catch (Exception ivjExc) {
	handleException(ivjExc);
      }
    }
    return debugScroller;
  }

  /*
    private OutputHandler getmessageHandler()
    {
    if ( messageHandler == null ) {
    try {
    messageHandler =
    new OutputHandler(new SimpleProducer("Output Handler"), true);
    }
    catch (Exception ivjExc) {
    handleException(ivjExc);
    }
    }
    return messageHandler;
    }
  */
		    
  private JScrollPane getStatusScroller()
  {
    if ( statusScroller == null ) {
      try {
	MessageArea statusMessages =
	  new MessageArea("Status log -- for administrators and interested users\n", 10, 80);
	statusMessages.setEditable(false);
	statusScroller = new JScrollPane(statusMessages);
	statusMessages.setScrollBar(statusScroller.getVerticalScrollBar());
	statusScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	//		getmessageHandler().addConsumer(statusMessages);
      } catch (Exception ivjExc) {
	handleException(ivjExc);
      }
    }
    return statusScroller;
  }
    
  /**
   * Return the containerPanel property value.
   * @return JPanel
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JPanel getcontainerPanel()
  {
    if (ivjcontainerPanel == null) {
      try {
	ivjcontainerPanel = new JPanel();
	ivjcontainerPanel.setName("containerPanel");
	ivjcontainerPanel.setLayout(new BorderLayout());
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjcontainerPanel;
  }

  /**
   * Return the messageTab property value.
   * @return JPanel
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JPanel getmessageTab()
  {
    if (ivjmessageTab == null) {
      try {
	ivjmessageTab = new JPanel();
	ivjmessageTab.setName("messageTab");
	ivjmessageTab.setLayout(new GridLayout(2, 1));
	// user code begin {1}
	ivjmessageTab.add(getStatusScroller());
	ivjmessageTab.add(getDebugScroller());
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjmessageTab;
  }

  /**
   * Return the messageItem property value.
   * @return JCheckBoxMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JCheckBoxMenuItem getmessageItem() {
    if (ivjmessageItem == null) {
      try {
	ivjmessageItem = new JCheckBoxMenuItem();
	ivjmessageItem.setName("messageItem");
	ivjmessageItem.setToolTipText("Log of diagnostic messages");
	ivjmessageItem.setText("Messages");
	ivjmessageItem.setSelected(true);
	// user code begin {1}
	ivjmessageItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjmessageItem;
  }

  /**
   * Return the ganttItem property value.
   * @return JCheckBoxMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JCheckBoxMenuItem getganttItem() {
    if (ivjganttItem == null) {
      try {
	ivjganttItem = new JCheckBoxMenuItem();
	ivjganttItem.setName("ganttItem");
	ivjganttItem.setToolTipText("Time-phased display of various cluster-object movements,");
	ivjganttItem.setText("Gantt Chart View");
	ivjganttItem.setSelected(true);
	// user code begin {1}
	ivjganttItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjganttItem;
  }

  /**
   * Return the clusterMenu property value.
   * @return JMenu
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JMenu getclusterMenu()
  {
    if (ivjclusterMenu == null) {
      try {
	ivjclusterMenu = new JMenu();
	ivjclusterMenu.setName("clusterMenu");
	ivjclusterMenu.setText("Select Cluster");
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjclusterMenu;
  }

  private JMenuItem getretryAggregationInitItem()
  {
    if (ivjretryAggregationInitItem == null) {
      try {
	ivjretryAggregationInitItem = new JMenuItem();
	ivjretryAggregationInitItem.setName("retryAggregationInitItem");
	ivjretryAggregationInitItem.setToolTipText("Reset or retry connection to database");
	ivjretryAggregationInitItem.setText("Reset Database Connection");
	ivjretryAggregationInitItem.addActionListener(this);
      } catch (Exception ivjExc) {
	handleException(ivjExc);
      }
    }
    return ivjretryAggregationInitItem;
  }
	    
  private JMenuItem getreloadAggregationItem()
  {
    if (ivjreloadAggregationItem == null) {
      try {
	ivjreloadAggregationItem = new JMenuItem();
	ivjreloadAggregationItem.setName("reloadAggregationItem");
	ivjreloadAggregationItem.setToolTipText("Force Database to make new snapshot of cluster data");
	ivjreloadAggregationItem.setText("Reload Database");
	ivjreloadAggregationItem.addActionListener(this);
      } catch (Exception ivjExc) {
	handleException(ivjExc);
      }
    }
    return ivjreloadAggregationItem;
  }

  /**
   * Return the ganttTab property value.
   * @return JPanel
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JPanel getganttTab() {
    if (ivjganttTab == null) {
      try {
	ivjganttTab = new JPanel();
	ivjganttTab.setName("ganttTab");
	ivjganttTab.setLayout(new BorderLayout());
	// user code begin {1}

	// not doing gantt chart view just yet
	//		ganttChartView = new GanttChartView(dbConfig, startDateString);
	//		ivjganttTab.add(ganttChartView, BorderLayout.CENTER);
	Debug.out("TS:gP new GCV");
	////		getprovider().addRowConsumer(ganttChartView.getWidget());
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjganttTab;
  }

  /**
   * Return the panelMenuSeparator property value.
   * @return JSeparator
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JSeparator getpanelMenuSeparator() {
    if (ivjpanelMenuSeparator == null) {
      try {
	ivjpanelMenuSeparator = new JSeparator();
	ivjpanelMenuSeparator.setName("panelMenuSeparator");
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjpanelMenuSeparator;
  }
    
  /**
   * Return the logPlanItem property value.
   * @return JCheckBoxMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JCheckBoxMenuItem getlogPlanItem()
  {
    if (ivjlogPlanItem == null) {
      try {
	ivjlogPlanItem = new JCheckBoxMenuItem();
	ivjlogPlanItem.setName("logPlanItem");
	ivjlogPlanItem.setToolTipText("Hierarchical view of all known tasks.");
	ivjlogPlanItem.setText("LogPlan View");
	ivjlogPlanItem.setSelected(true);
	// user code begin {1}
	ivjlogPlanItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjlogPlanItem;
  }
   
  private JCheckBoxMenuItem getassetUsageItem()
  {
    if (ivjassetUsageItem == null) {
      try {
	ivjassetUsageItem = new JCheckBoxMenuItem();
	ivjassetUsageItem.setName("assetUsageItem");
	ivjassetUsageItem.setToolTipText("Hierarchical view of all known tasks.");
	ivjassetUsageItem.setText("AssetUsage View");
	ivjassetUsageItem.setSelected(true);
	// user code begin {1}
	ivjassetUsageItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjassetUsageItem;
  }
 
  /**
   * Return the logPlanTab property value.
   * @return JPanel
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JPanel getlogPlanTab()
  {
    if (ivjlogPlanTab == null) {
      try {
	ivjlogPlanTab = new JPanel();
	ivjlogPlanTab.setName("logPlanTab");
	ivjlogPlanTab.setLayout(new BorderLayout());
	// user code begin {1}
	logPlanView = getlogPlanView();
	//		getprovider().addItemPoolConsumer(logPlanView);
	ivjlogPlanTab.add(logPlanView, BorderLayout.CENTER);
	ivjlogPlanTab.add(new LegendPanel(), BorderLayout.SOUTH);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjlogPlanTab;
  }

  private JPanel getassetUsageTab()
  {
    if (ivjassetUsageTab == null) {
      try {
	ivjassetUsageTab = new JPanel();
	ivjassetUsageTab.setName("assetUsageTab");
	ivjassetUsageTab.setLayout(new BorderLayout());
	// user code begin {1}
	assetUsageView = getassetUsageView();
	//		getprovider().addItemPoolConsumer(assetUsageView);
	ivjassetUsageTab.add(assetUsageView, BorderLayout.CENTER);
	ivjassetUsageTab.add(new LegendPanel(), BorderLayout.SOUTH);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjassetUsageTab;
  }

  /**
   * Return the actionMenu property value.
   * @return JMenu
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JMenu getactionMenu()
  {
    if (ivjactionMenu == null) {
      try {
	ivjactionMenu = new JMenu();
	ivjactionMenu.setName("actionMenu");
	ivjactionMenu.setText("Actions");
	ivjactionMenu.add(getnewViewItem());
	ivjactionMenu.add(makeMenuItem("Exit"));
	//		ivjactionMenu.add(getretryAggregationInitItem());
	//		ivjactionMenu.add(getreloadAggregationItem());
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjactionMenu;
  }
    
  /**
   * Return the panelMenu property value.
   * @return JMenu
   */

  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JMenu getpanelMenu()
  {
    if (ivjpanelMenu == null) {
      try {
	ivjpanelMenu = new JMenu();
	ivjpanelMenu.setName("panelMenu");
	ivjpanelMenu.setToolTipText("Commands for manipulating panel arrangment.");
	ivjpanelMenu.setText("Panel");
	//		ivjpanelMenu.add(getmessageItem());
	ivjpanelMenu.add(getlogPlanItem());
	ivjpanelMenu.add(getassetUsageItem());
	ivjpanelMenu.add(getganttItem());
	ivjpanelMenu.add(getpanelMenuSeparator());
	ivjpanelMenu.add(gettileItem());
	ivjpanelMenu.add(getstackItem());
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjpanelMenu;
  }

  private JMenuItem gettaskItem()
  {
    if (ivjtaskItem == null) {
      try {
	ivjtaskItem = new JMenuItem();
	ivjtaskItem.setName("taskItem");
	ivjtaskItem.setToolTipText("Request all tasks (raw LogPlan form)");
	ivjtaskItem.setText("Tasks");
	ivjtaskItem.addActionListener(this);
      } catch (Exception ivjExc) {
	handleException(ivjExc);
      }
    }
    return ivjtaskItem;
  }

  /**
   * Return the newViewItem property value.
   * @return JMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JMenuItem getnewViewItem()
  {
    if (ivjnewViewItem == null) {
      try {
	ivjnewViewItem = new JMenuItem();
	ivjnewViewItem.setName("newViewItem");
	//		ivjnewViewItem.setToolTipText("Filter Dialog");
	ivjnewViewItem.setText(TPFDDString + " Filter Dialog...");
	// user code begin {1}
	ivjnewViewItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjnewViewItem;
  }

  /**
   * Return the stackItem property value.
   * @return JRadioButtonMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JRadioButtonMenuItem getstackItem()
  {
    if (ivjstackItem == null) {
      try {
	ivjstackItem = new JRadioButtonMenuItem();
	ivjstackItem.setName("stackItem");
	ivjstackItem.setToolTipText("Tabbed view; frontmost takes up entire display");
	ivjstackItem.setText("Stack");
	ivjstackItem.setSelected(true);
	// user code begin {1}
	ivjstackItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjstackItem;
  }
    
  /**
   * Return the tabbingPane property value.
   * @return JTabbedPane
   */

  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JTabbedPane gettabbingPane()
  {
    if (ivjtabbingPane == null) {
      try {
	ivjtabbingPane = new JTabbedPane();
	ivjtabbingPane.setName("tabbingPane");
	// user code begin {1}
	if ( getlogPlanItem().isSelected() )
	  ivjtabbingPane.addTab(logPlanString, getlogPlanTab());
	if ( getassetUsageItem().isSelected() )
	  ivjtabbingPane.addTab(assetUsageString, getassetUsageTab());
	ivjtabbingPane.addTab(StatisticsPane.NAME,
			      new StatisticsPane(this));

	GraphPane graphPane = new GraphPane (this);
	String name = graphPane.getName ();
	ivjtabbingPane.addTab(name, graphPane);

	//		if ( getganttItem().isSelected() )
	//		    ivjtabbingPane.addTab("Gantt Chart", getganttTab());
	//		if ( getmessageItem().isSelected() )
	//		    ivjtabbingPane.addTab("Messages", getmessageTab());

	//		for ( int i = 0; i < newViews.size(); i++ )
	//		    ivjtabbingPane.addTab("View " + (i + 1), (JPanel)(newViews.get(i)));
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjtabbingPane;
  }

  private JPanel gettilingPane()
  {
    if ( ivjtilingPane == null ) {
      try {
	ivjtilingPane = new JPanel();
	ivjtilingPane.setName("tilingPane");
	boolean m = getmessageItem().isSelected();
	boolean l = getlogPlanItem().isSelected();
	boolean a = getassetUsageItem().isSelected();
	boolean g = getganttItem().isSelected();
	//		int numTabs = (m ? 1 : 0) + (l ? 1 : 0) + (g ? 1 : 0) + newViews.size();
	int numTabs = (m ? 1 : 0) + (l ? 1 : 0) + (g ? 1 : 0);// + newViews.size();
	int largestFactor = 1;
	for ( int i = numTabs / 2; i > 1; i-- )
	  if ( numTabs == (numTabs / i) * i ) {
	    largestFactor = i;
	    break;
	  }
	ivjtilingPane.setLayout(new GridLayout(numTabs / largestFactor, largestFactor));
	if ( m ) ivjtilingPane.add(getmessageTab());
	if ( l ) ivjtilingPane.add(getlogPlanTab());
	if ( a ) ivjtilingPane.add(getassetUsageTab());
	if ( g ) ivjtilingPane.add(getganttTab());
	//		for ( int i = 0; i < newViews.size(); i++ )
	//		    ivjtilingPane.add("View " + (i + 1), (JPanel)(newViews.get(i)));
      } catch ( Exception ivjExc ) {
	handleException(ivjExc);
      }
    }
    return ivjtilingPane;
  }

    
  /**
   * Return the tileItem property value.
   * @return JRadioButtonMenuItem
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JRadioButtonMenuItem gettileItem()
  {
    if (ivjtileItem == null) {
      try {
	ivjtileItem = new JRadioButtonMenuItem();
	ivjtileItem.setName("tileItem");
	ivjtileItem.setToolTipText("Arrange windows to fully cover main panel with no overlap.");
	ivjtileItem.setText("Tile");
	ivjtileItem.setSelected(false);
	// user code begin {1}
	ivjtileItem.addActionListener(this);
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjtileItem;
  }
    
  /**
   * Return the TPFDDShellMenuBar property value.
   * @return JMenuBar
   */
  /* WARNING: THIS METHOD WILL BE REGENERATED. */
  private JMenuBar getTPFDDShellMenuBar()
  {
    if (ivjTPFDDShellMenuBar == null) {
      try {
	ivjTPFDDShellMenuBar = new JMenuBar();
	ivjTPFDDShellMenuBar.setName("TPFDDShellMenuBar");
	ivjTPFDDShellMenuBar.add(getactionMenu());
	//		ivjTPFDDShellMenuBar.add(getclusterMenu());
	//		ivjTPFDDShellMenuBar.add(getpanelMenu());
	if (showRunMenu)
	  ivjTPFDDShellMenuBar.add(createRunMenu());
	ivjTPFDDShellMenuBar.add(createSourceText());
                
	// user code begin {1}
	// user code end
      } catch (Exception ivjExc) {
	// user code begin {2}
	// user code end
	handleException(ivjExc);
      }
    }
    return ivjTPFDDShellMenuBar;
  }
    

  private JPanel createSourceText() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel label = new JLabel("Database:  ");
    machineText = new JLabel("               ");
    
    // Make everything show up prettily
    //panel.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel miniPanel = new JPanel();
    miniPanel.add(label);
    miniPanel.add(machineText);
    panel.add(miniPanel);

    return panel;
  }

  protected JMenuItem makeRunItem (DatabaseRun run) {
    JMenuItem item = new JMenuItem();
    String label = run.toString ();
    item.setName(label);
    item.setToolTipText(label);
    item.setText(label);
    item.addActionListener(this);
    itemToRun.put (item, run);
	
    return item;
  }

  protected JMenu makeDatabaseItem (String label) {
    JMenu item = new JMenu();
    item.setName(label);
    item.setToolTipText(label);
    item.setText(label);
    //	item.addActionListener(this);

    return item;
  }

  protected JMenuItem makeMenuItem (String label) {
    return makeMenuItem (label, this);
  }

  protected JMenuItem makeMenuItem (String label, ActionListener listener) {
    JMenuItem item = new JMenuItem();

    item.setName(label);
    item.setToolTipText(label);
    item.setText(label);
    item.setActionCommand(label);
    item.addActionListener(listener);
	
    return item;
  }

  protected JMenu createRunMenu () {
    QueryHandler queryHandler = new QueryHandler ();
    DatabaseConfig dbConfig = new DatabaseConfig (getclusterCache().getHost());
    DatabaseRun defaultRun = queryHandler.getRecentRun (dbConfig);

    List databases = queryHandler.getDatabases (dbConfig);
    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "NewTPFDDShell.createRunMenu - found " + databases.size() + " databases : " + databases);

    JMenu runMenu = new JMenu();
    runMenu.setName("runMenu");
    runMenu.setToolTipText("Databases and runs that can be displayed.");
    runMenu.setText("Run");
  
    for (int i = 0; i < databases.size (); i++) {
      String db = (String) databases.get (i);
	  
      List runs = queryHandler.getRuns (dbConfig, db);
      JMenu menu = null;
	  
      if (!runs.isEmpty ()) {
	menu = makeDatabaseItem (db);
	runMenu.add (menu);
      }

      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "NewTPFDDShell.createRunMenu - Database #" + i + " : " + db + " has " + runs.size() + " runs : ");
	  
      for (int j = 0; j<runs.size(); j++) {
	DatabaseRun run = (DatabaseRun) runs.get(j);
	JMenuItem menuItem = makeRunItem (run);
	menu.add (menuItem);
		
	if (run.equals(defaultRun)) {
	  menuItem.setIcon (check);
	  lastItemSelected = menuItem;
	}

	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "\t" + runs.get(j));
      }
    }
	
    return runMenu;
  }
  
  private void handleException(Exception e)
  {
    OutputHandler.out(ExceptionTools.toString("TS:hE", e));
  }

  public void performInitialQuery () {
    setCursor (waitCursor);
    if (run == null) {
      machineText.setText (getclusterCache().getHost() + " - no datagrabber runs in database.");
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Note : no runs found in default database. " +
			  "Either no datagrabber runs have been done or all have been deleted.");
    } else {
      dbConfig.setDatabase (run.getDatabase());
      dbConfig.setConnection (dbConfig.createDBConnection (dbConfig.getHost (), 
							   dbConfig.getDatabase ()));
      logPlanView.doInitialQuery ();
      assetUsageView.doInitialQuery();
      machineText.setText (getclusterCache().getHost() + ", " + run.getDatabase () + " - " + run);
    }
    setCursor (defaultCursor);
  }

  protected void setHost () {
    String ccHost = getDocumentBase().getHost();
    if ( ccHost == null || ccHost.length() == 0 )
      ccHost = "localhost";
    getclusterCache().clientGuiSetHost(ccHost);
    dbConfig = new DatabaseConfig (getclusterCache().getHost());
  }
	  
  private void resetView()
  {
    Debug.out("TS:resetView enter");
    getcontainerPanel().removeAll();
    if ( getstackItem().isSelected() ) { // tabbing view
      gettabbingPane().removeAll(); // free children of constraints before killing
      ivjtabbingPane = null;
      getcontainerPanel().add(gettabbingPane(), BorderLayout.CENTER);
    }
    else { // tiling view
      gettilingPane().removeAll();
      ivjtilingPane = null;
      getcontainerPanel().add(gettilingPane(), BorderLayout.CENTER);
    }
    getcontainerPanel().validate(); // this is necessary, not sure why; invalidate doesn't work
    Debug.out("TS:resetView leave");
  }

  public void init()
  {
    //	Debug.setHandler(new Debug(new SimpleProducer("Debug Handler"), false));
    setHost ();

    if (!goodDatabaseConnection ()) {
      System.err.println ("There is a problem with the database connection.  Please fix and retry.");
      return;
    }
	  
    String fontSizeString = getParameter("fontSize");
    if ( fontSizeString == null )
      fontSizeString = "12";
    fontSize = Integer.parseInt(fontSizeString);
    if ( fontSize < 8 )
      fontSize = 8;
	
    startDateString = getParameter("startDateString");
    if ( startDateString == null )
      startDateString = System.getProperty("cdayDate", "07/04/00");
	
    //	String debugParam = getParameter("debug");
    //if ( debugParam != null )
    //  Debug.set(debugParam.equalsIgnoreCase("true"));
    Debug.set(true);
    // init text areas (inside scrollers) so they'll catch all messages from the start
    getDebugScroller();
    getStatusScroller();
	
    boolean stupidLinuxAppletviewerBug = false;
    String doStupidLinuxAppletviewerBug = null;
    try {
      doStupidLinuxAppletviewerBug =
	System.getProperty("stupidLinuxAppletviewerBug");
    }
    catch ( AccessControlException e ) {e.printStackTrace();
    }
    if ( doStupidLinuxAppletviewerBug != null &&
	 doStupidLinuxAppletviewerBug.equalsIgnoreCase("true") )
      stupidLinuxAppletviewerBug = true;

    try {
      setName(appTitle);
      setSize(1000, 700);
      if ( stupidLinuxAppletviewerBug )
	setVisible(true);
      setJMenuBar(getTPFDDShellMenuBar());
      setContentPane(getcontainerPanel());
      // user code begin {1}
      resetView();
      // user code end
    } catch (Exception ivjExc) {
      // user code begin {2}
      // user code end
      handleException(ivjExc);
    }

    machineText.setText(getclusterCache().getHost());
  }
    
  public void start() {
    if (!goodDatabaseConnection ())
      return;
	  
    QueryHandler queryHandler = new QueryHandler ();
    DatabaseConfig dbConfig = new DatabaseConfig (getclusterCache().getHost());
    // use database from property

    DatabaseRun run = queryHandler.getRecentRun (dbConfig);
    setRun (run);
    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "NewTPFDDShell.start - initial run is " + run);
	  
    performInitialQuery ();
  }

  public boolean goodDatabaseConnection () {
    DatabaseConfig dbConfig = new DatabaseConfig (getclusterCache().getHost());
    return (dbConfig.getConnection () != null);
  }

  public void setRun (DatabaseRun run) { 
    this.run = run;
  }
  public DatabaseRun getRun () { 
    return run;
  }
  public DatabaseConfig getDBConfig () { 
    return dbConfig;
  }
  
  /*
   * Event Handling routines
   */
  public void actionPerformed(ActionEvent event)
  {
    String command = event.getActionCommand();
    Object source = event.getSource();
    DatabaseRun run = null;

    if (command.equals ("Exit")) {
      System.exit (0);
    } else { 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Command " + command);
    }
	
	
    Debug.out("TS:aP command " + command);
    if ( source instanceof JMenuItem ) {
      JMenuItem item = (JMenuItem)source;
      // Action Menu
      if ( source == getnewViewItem() ) {
	logPlanView.showFilterDialog ();
      }
      else if ( (run = (DatabaseRun) itemToRun.get (source)) != null) {
	setRun (run);
	performInitialQuery();
	if(lastItemSelected != null)
	  lastItemSelected.setIcon (null);
	lastItemSelected = (JMenuItem)source;
	lastItemSelected.setIcon (check);
      }
      else if ( source == getreloadAggregationItem() ) {
	// ignore for now
	/*
	  QueryData query = new QueryData();
	  query.setOtherCommand("RELOAD yourself");
	  provider.request("Aggregation", PSPClientConfig.UIDataPSP_id, query);
	*/
      }
      // Panel Menu
      else if ( source == getmessageItem() || source == getlogPlanItem() || source == getganttItem() ) {
	resetView();
      }
      else if ( source == gettileItem() ) {
	if ( !gettileItem().isSelected() ) // was selected; make it stay selected
	  gettileItem().setSelected(true);
	else {
	  getstackItem().setSelected(false);
	  resetView();
	}
      }
      else if ( source == getstackItem() ) {
	if ( !getstackItem().isSelected() ) // was selected; make it stay selected
	  getstackItem().setSelected(true);
	else {
	  gettileItem().setSelected(false);
	  resetView();
	}
      }
      else if ( clusterMenuItems.contains(item) ) {
	selectedClusterItem.setSelected(false);
	selectedClusterItem = item;
	selectedClusterItem.setSelected(true);

	OutputHandler.out("TS:aP Changed selected cluster to: " + item.getText());
      }
      else
	OutputHandler.out("TS:aP Error: unknown JMenuItem source: " + source);
    }
    else
      OutputHandler.out("TS:aP Error: unknown bean source: " + source);
  }


  public void showTPFDDView (FilterClauses filterClauses, boolean useFilterQuery, boolean assetBased) {
    TaskModel model = new TaskModel(this);
    if (assetBased) model = new AssetModel(this);
    GanttChartView ganttChartView = new GanttChartView(model, startDateString);
    JPanel ganttChartPanel = new JPanel();
    ganttChartPanel.setLayout(new BorderLayout());
    ganttChartPanel.add(ganttChartView, BorderLayout.CENTER);
    JFrame ganttChartFrame = new JFrame();
    ganttChartFrame.getContentPane().add(ganttChartPanel, BorderLayout.CENTER);
    ganttChartFrame.setTitle(TPFDDString + " Display " + filterClauses);//units + "/" + carriers + "/" + cargo);
    ganttChartFrame.setSize(640, 480);
    ganttChartFrame.setVisible(true);
    ganttChartFrame.setJMenuBar (ganttChartView.getMenuBar ());
    if (useFilterQuery) {
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "NewTPFDDShell.showTPFDDView - showing TPFDD Filter Lines");
      ganttChartView.showTPFDDFilterLines (filterClauses);
    }
    else {
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "NewTPFDDShell.showTPFDDView - showing TPFDD Lines");
      filterClauses.setSortByName (true);
      ganttChartView.showTPFDDLines (filterClauses);
    }
  }

  public void showRouteView(RouteViewRequest rvr){
    new RouteView(getDBConfig(),this,getRun().getRunID(), rvr);
  }

  public void showAssetDetailView(AssetDetailRequest adr){
    new AssetDetailView(getDBConfig(),getRun().getRunID(), adr);
  }

  public void showCarrierDetailView(CarrierDetailRequest cdr){
    new CarrierDetailView(getDBConfig(),getRun().getRunID(), cdr);
  }

  /**
   * main entrypoint - starts the part when it is run as an application
   * @param args String[]
   */
  public static void main(String[] args)
  {
    try {
      JFrame frame = new JFrame();
      NewTPFDDShell aTPFDDShell;
      Class iiCls = Class.forName("org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell");
      ClassLoader iiClsLoader = iiCls.getClassLoader();
      aTPFDDShell = (NewTPFDDShell)java.beans.Beans.instantiate(iiClsLoader,
								"org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell");
      frame.getContentPane().add(aTPFDDShell, BorderLayout.CENTER);
      frame.setSize(aTPFDDShell.getSize());
      frame.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    System.exit(0);
	  };
	});
      frame.setVisible(true);
      aTPFDDShell.start();
      frame.setTitle (appTitle);
    }
    catch (Exception exception) {
      System.err.println("Exception occurred in main() of TPFDDShell");
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "", exception);
    }
  }
}

