/*
 * <Copyright>
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

package org.cougaar.logistics.ui.inventory;


import java.util.*;


import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.FlowLayout;
import java.awt.Cursor;
import java.awt.Component;
import java.awt.event.*;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;
import org.cougaar.logistics.ui.inventory.data.BlankInventoryData;
import org.cougaar.logistics.ui.inventory.dialog.InventoryPreferenceDialog;

/**
 * <pre>
 *
 * The InventoryUIFrame is the root object that maintains the primary
 * window where the inventory charts are displayed.
 *
 * @see InventoryConnectionManager
 * @see InventoryDataSource
 * @see MultiChartPanel
 * @see InventoryDemandChart
 * @see InventoryRefillChart
 * @see InventoryLevelChart
 *
 **/


public class InventoryUIFrame extends JFrame
    implements ActionListener, ItemListener, InventorySelectionListener {


  static final Cursor waitCursor =
      Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
  static final Cursor defaultCursor =
      Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

  public static final String SHOW_INIT_SHORTFALL = "SHOW_INIT_SHORTFALL";
  public static final String SHOW_INIT_CDAY = "SHOW_INIT_CDAY";

  public static final String INVENTORY_PREF_FILE_NAME = "InventoryPreferences.txt";

  protected InventoryDataSource dataSource;
  protected Container contentPane;

  protected JTextArea editPane;
  protected JFileChooser fileChooser;

  protected JCheckBoxMenuItem showInventoryChart;
  protected JCheckBoxMenuItem showDemandChart;
  protected JCheckBoxMenuItem showRefillChart;

  protected MultiChartPanel multiChart;
  protected InventoryData inventory;

  protected Logger logger;

  protected String cip;
  protected String defaultSaveCSVPath;
  protected String defaultOpenCSVPath;
  protected String defaultPrefsPath;
  protected String helpFileStr;


  protected InventorySelectionPanel selector;
  protected InventoryXMLParser parser;

  protected InventoryPreferenceData prefData;
  protected InventoryPreferenceDialog prefDialog = null;


  protected boolean showInitialShortfall = true;
  protected boolean initialDisplayCDay = true;

  public InventoryUIFrame(HashMap params) {
    super("Inventory GUI");
    initializeUIFrame(params);
  }

  public InventoryUIFrame() {
    super("Inventory GUI");
    initializeUIFrame(new HashMap());
  }

  public InventoryUIFrame(String frameTitle) {
    super(frameTitle);
    initializeUIFrame(new HashMap());
  }

  public InventoryUIFrame(String[] args) {
    super("Inventory GUI");
    initializeUIFrame(readParameters(args));
  }

  public InventoryUIFrame(String[] args, String frameTitle) {
    super(frameTitle);
    //initializeUIFrame(readParameters(args));
  }


  protected void initializeUIFrame(HashMap params) {
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    logger = Logging.getLogger(this);
    parser = new InventoryXMLParser();

    readParameters(params);

    contentPane = getRootPane().getContentPane();

    cip = System.getProperty("org.cougaar.install.path");

    if ((cip == null) ||
        (cip.trim().equals(""))) {
      logger.error("org.cougaar.install.path is not defined in Command line");
    } else {
      helpFileStr = cip + File.separator + "albbn" + File.separator + "doc" + File.separator + "alinvgui" + File.separator + "index.htm";
    }

    String baseDir = System.getProperty("org.cougaar.workspace");
    if ((baseDir == null) ||
        (baseDir.trim().equals(""))) {
      baseDir = cip + File.separator + "workspace";
    }
    defaultOpenCSVPath = baseDir + File.separator + "INVGUICSV";
    defaultPrefsPath = baseDir + File.separator + "INVGUIPREFS";
    defaultSaveCSVPath = defaultOpenCSVPath + File.separator + formatTimeStamp(new Date(), false) + File.separator;
    File pathDirs = new File(defaultOpenCSVPath);

    fileChooser = new JFileChooser(pathDirs);

    String invDataPath = cip + File.separator + "data"
        + File.separator + "ui"
        + File.separator + "inventory" + File.separator;


    InventoryUnitConversionTable conversionTable = new InventoryUnitConversionTable();
    conversionTable.parseAndAddFile(invDataPath + "ammo-conversion.csv");
    InventoryChart.setConversionTable(conversionTable);

    //Setup prefs and pref dialog
    prefData = openPrefData();


// fills frame
    doMyLayout();
    dataSource = null;
    pack();
    setSize(800, 600);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
// set location to middle of the screen
    setLocation(screenSize.width / 2 - (int) this.getSize().getWidth() / 2,
                screenSize.height / 2 - (int) this.getSize().getHeight() / 2);
    setVisible(true);
  }

  protected void doMyLayout() {
    getRootPane().setJMenuBar(makeMenus());

    JTabbedPane tabs = new JTabbedPane();

    contentPane.add(tabs, BorderLayout.CENTER);

    JPanel editPanel = new JPanel();
    editPanel.setLayout(new BorderLayout());

    //Create a text area.
    editPane = new JTextArea();
    editPane.setLineWrap(true);
    editPane.setWrapStyleWord(true);
    JScrollPane areaScrollPane = new JScrollPane(editPane);
    areaScrollPane.setVerticalScrollBarPolicy(
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    areaScrollPane.setPreferredSize(new Dimension(700, 500));
    areaScrollPane.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("XML"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)),
            areaScrollPane.getBorder()));

    JButton parseButton = new JButton("Parse");
    JButton dataButton = new JButton("Data");
    parseButton.addActionListener(this);
    dataButton.addActionListener(this);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout());
    buttonPanel.add(dataButton);
    buttonPanel.add(Box.createHorizontalStrut(60));
    buttonPanel.add(parseButton);

    editPanel.add(buttonPanel, BorderLayout.NORTH);
    editPanel.add(areaScrollPane, BorderLayout.CENTER);

    multiChart = new MultiChartPanel(showInitialShortfall, initialDisplayCDay, prefData);
    multiChart.setPreferredSize(new Dimension(700, 250));

    prefDataChanged(new InventoryPreferenceData(), prefData);

    tabs.add("InventoryChart", multiChart);
    tabs.add("XML", editPanel);

    selector = new InventorySelectionPanel(this);
    selector.addInventorySelectionListener(this);

    contentPane.add(selector, BorderLayout.SOUTH);
  }

  public void setMultiChart(MultiChartPanel mcp) {
    multiChart = mcp;
  }

  public void setParser(InventoryXMLParser myParser) {
    parser = myParser;
  }

  protected JMenuBar makeMenus() {
    JMenuBar retval = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem quit = new JMenuItem(InventoryMenuEvent.MENU_Exit);
    JMenuItem save = new JMenuItem(InventoryMenuEvent.MENU_SaveXML);
    JMenuItem open = new JMenuItem(InventoryMenuEvent.MENU_OpenXML);
    quit.addActionListener(this);
    save.addActionListener(this);
    open.addActionListener(this);
    file.add(open);
    file.add(save);
    file.add(quit);
    retval.add(file);

    JMenu edit = new JMenu("Edit");
    JMenuItem pref = new JMenuItem(InventoryMenuEvent.MENU_Pref);
    pref.addActionListener(this);
    edit.add(pref);
    retval.add(edit);

    JMenu connection = new JMenu("Connection");
    JMenuItem connect = new JMenuItem(InventoryMenuEvent.MENU_Connect);
    connect.addActionListener(this);
    connection.add(connect);
    retval.add(connection);

    JMenu view = new JMenu("View");
    showRefillChart = new JCheckBoxMenuItem(InventoryMenuEvent.MENU_RefillChart, true);
    showDemandChart = new JCheckBoxMenuItem(InventoryMenuEvent.MENU_DemandChart, true);
    showInventoryChart = new JCheckBoxMenuItem(InventoryMenuEvent.MENU_InventoryChart, true);
    showDemandChart.addItemListener(this);
    showRefillChart.addItemListener(this);
    showInventoryChart.addItemListener(this);
    view.add(showInventoryChart);
    view.add(showRefillChart);
    view.add(showDemandChart);
    retval.add(view);

    JMenu helpMenu = new JMenu("Help");
    JMenuItem helpItem = new JMenuItem(InventoryMenuEvent.MENU_Help);
    helpItem.addActionListener(this);
    helpMenu.add(helpItem);
//Help popup is not done yet.
//retval.add(helpMenu);

    return retval;
  }

  public void itemStateChanged(ItemEvent e) {
    if (((JCheckBoxMenuItem) e.getItem()) == showRefillChart) {
      multiChart.showRefillChart(e.getStateChange() == e.SELECTED);
    } else if (((JCheckBoxMenuItem) e.getItem()).getText().equals(InventoryMenuEvent.MENU_DemandChart)) {
      multiChart.showDemandChart(e.getStateChange() == e.SELECTED);
    } else if (((JCheckBoxMenuItem) e.getItem()) == showInventoryChart) {
      multiChart.showInventoryChart(e.getStateChange() == e.SELECTED);
    }

  }

  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(InventoryMenuEvent.MENU_Exit)) {
      System.exit(0);
    } else if (e.getActionCommand().equals(InventoryMenuEvent.MENU_Connect)) {
      connectToServlet();
    } else if (e.getActionCommand().equals(InventoryMenuEvent.MENU_SaveXML)) {
      saveXML();
    } else if (e.getActionCommand().equals(InventoryMenuEvent.MENU_OpenXML)) {
      openXML();
    } else if (e.getActionCommand().equals(InventoryMenuEvent.MENU_Help)) {
      popupHelpPage();
    } else if (e.getActionCommand().equals(InventoryMenuEvent.MENU_Pref)) {
      popupPrefsDialog();
    } else if (e.getActionCommand().equals("Parse")) {
      logger.info("Parsing");
      inventory = parser.parseString(dataSource.getCurrentInventoryData());
      multiChart.setData(inventory);
    } else if (e.getActionCommand().equals("Data")) {
      editPane.setText(dataSource.getCurrentInventoryData());
    }
  }

  protected void popupHelpPage() {

  }

  protected void popupPrefsDialog() {
    if (prefDialog == null) {
      prefDialog = new InventoryPreferenceDialog(this, prefData);
    }
    prefDialog.setVisible(true);
//    System.out.println("About to exit popupPrefsDialog");
  }

  public void prefDataChanged(InventoryPreferenceData origData,
                              InventoryPreferenceData newData) {
    if (origData.startupWCDay != newData.startupWCDay) {
      initialDisplayCDay = newData.startupWCDay;
    }
    if (origData.displayShortfall != newData.displayShortfall) {
      showInitialShortfall = newData.displayShortfall;
    }
    if ((origData.showInventoryChart != newData.showInventoryChart) ||
        (origData.showDemandChart != newData.showDemandChart) ||
        (origData.showRefillChart != newData.showRefillChart)) {
      showDemandChart.setState(newData.showDemandChart);
      showInventoryChart.setState(newData.showInventoryChart);
      showRefillChart.setState(newData.showRefillChart);
    }

    multiChart.prefDataChanged(origData, newData);

    prefData = newData;
  }

  protected void saveXML() {
    File pathDirs = new File(defaultSaveCSVPath);
    try {
      if (!pathDirs.exists()) {
        pathDirs.mkdirs();
      }
    } catch (Exception ex) {
      logger.error("Error creating default directory " + defaultSaveCSVPath, ex);
    }
    if (inventory != null) {
      String fileID = defaultSaveCSVPath + inventory.getOrg() + "-" + (inventory.getItem().replaceAll("/", "-")).replaceAll(":", "-") + "-" + formatTimeStamp(new Date(), false) + ".csv";
      System.out.println("Save to file: " + fileID);
      fileChooser.setSelectedFile(new File(fileID));
    }
    int retval = fileChooser.showSaveDialog(this);
    if (retval == fileChooser.APPROVE_OPTION) {
      File saveFile = fileChooser.getSelectedFile();
      try {
        FileWriter fw = new FileWriter(saveFile);
        fw.write(inventory.getXML());
        fw.flush();
        fw.close();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

  public void savePrefData(InventoryPreferenceData savePrefData) {
    File pathDirs = new File(defaultPrefsPath);
    try {
      if (!pathDirs.exists()) {
        pathDirs.mkdirs();
      }
    } catch (Exception ex) {
      logger.error("Error creating default directory " + defaultOpenCSVPath, ex);
      displayErrorString("Could not write prefs file! Could not create default directory.");
    }


    File saveFile = new File(defaultPrefsPath + File.separator + INVENTORY_PREF_FILE_NAME);
    try {
      FileWriter fw = new FileWriter(saveFile);
      fw.write(savePrefData.toXMLString());
      fw.flush();
      fw.close();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }


  protected InventoryPreferenceData openPrefData() {
    File prefFile = new File(defaultPrefsPath + File.separator + INVENTORY_PREF_FILE_NAME);
    if (!prefFile.exists()) {
      return new InventoryPreferenceData();
    }
    String prefXML = "";
    try {
      BufferedReader br = new BufferedReader(new FileReader(prefFile));

      String nextLine = br.readLine();
      while (nextLine != null) {
        prefXML = prefXML + nextLine + "\n";
        nextLine = br.readLine();
      }
      br.close();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    InventoryPreferenceData savedPrefs = new InventoryPreferenceData();
    try {
      savedPrefs.parseValuesFromXMLString(prefXML);
    } catch (Exception e) {
      logger.error("Error parsing " + prefFile + "!", e);
      return new InventoryPreferenceData();
    }
    return savedPrefs;
  }

  protected void openXML() {
    /**
     if(inventory != null) {
     String fileID = defaultOpenCSVPath;
     System.out.println("Open file at path: " + fileID);
     fileChooser.setSelectedFile(new File(fileID));
     }
     */
    fileChooser.setMultiSelectionEnabled(true);
    int retval = fileChooser.showOpenDialog(this);
    if (retval == fileChooser.APPROVE_OPTION) {
      File[] openFiles = fileChooser.getSelectedFiles();
      timeConsumingTaskStart(this);
      try {
        Thread openXMLThread = new Thread(new OpenXMLRunnable(openFiles));
        openXMLThread.start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  public void doOpenXML(File[] openFiles) {
    InventoryFileManager fm = null;
    String invXML="";

    try {
      SwingUtilities.invokeAndWait(new TaskStartRunnable(this));
    } catch (Exception e) {
      e.printStackTrace();
    }


    if (openFiles.length > 0) {
      if (dataSource instanceof InventoryFileManager) {
        fm = (InventoryFileManager) dataSource;
      } else {
        fm = new InventoryFileManager(this);
        dataSource = fm;
      }
    }
    String lastGoodXML=null;
    for (int i = 0; i < openFiles.length; i++) {
      File openFile = openFiles[i];
      StringWriter writer = new StringWriter(InventoryConnectionManager.INITIAL_XML_SIZE);
      invXML="";
      try {
        BufferedReader br = new BufferedReader(new FileReader(openFile));

        String nextLine = br.readLine();
        while (nextLine != null) {
	    if(!nextLine.trim().equals("")) {
		writer.write(nextLine + "\n");
	    }
	    nextLine = br.readLine();

        }
        br.close();
	writer.flush();
	invXML = writer.toString();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }

      if(invXML.trim().equals("")) {
	  displayErrorString("Empty File!","File " + openFile.getName() + " has no contents!");
	  if(lastGoodXML != null) {
	      invXML=lastGoodXML;
	      inventory = parser.parseString(invXML);
	  }
	  else {
	      inventory=null;	
	  }
      }
      else {
	  inventory = parser.parseString(invXML);
	  fm.addItem(inventory, invXML);
          lastGoodXML=invXML;
      }
    }
    try {
      SwingUtilities.invokeAndWait(new FlushXMLToScreenRunnable(invXML, openFiles, fm));
    } catch (Exception e) {
      e.printStackTrace();
    }
    fileChooser.setMultiSelectionEnabled(false);
  }

  public void flushXMLToScreen(String invXML,
                               File[] openFiles,
                               InventoryFileManager fm) {
    if ((openFiles.length > 0) &&
        (inventory != null)) {
      editPane.setText(invXML);
      Vector orgs = dataSource.getOrgNames(".",selector.getSelectedOrgPopMethod());
      String[] fileType = dataSource.getSupplyTypes();
      selector.initializeComboBoxes(orgs, fileType);
      Vector assetNames = dataSource.getAssetNames(inventory.getOrg(), fileType[0]);
      selector.setAssetNames(assetNames);
      selector.setSelectedOrgAsset(inventory.getOrg(), fm.getFullItemName(inventory));
      multiChart.setData(inventory);
    }
    timeConsumingTaskEnd(this);
  }

  protected void readParameters(HashMap map) {
    String showShortfall = (String) map.get(SHOW_INIT_SHORTFALL);
    if ((showShortfall != null) &&
        (!showShortfall.trim().equals(""))) {
      showInitialShortfall = (!(showShortfall.trim().toLowerCase().equals("false")));
    }
    String displayCDay = (String) map.get(SHOW_INIT_CDAY);
    if ((displayCDay != null) &&
        (!displayCDay.trim().equals(""))) {
      initialDisplayCDay = (displayCDay.trim().toLowerCase().equals("true"));
    }
  }

  public static HashMap readParameters(String[] args) {
    HashMap params = new HashMap();
    for (int i = 0; i < args.length; i++) {
      String[] keyAndValue = args[i].split("[=]");
      String key = keyAndValue[0];
      String value = keyAndValue[1];
      params.put(key, value);
    }
    return params;
  }

  public static void timeConsumingTaskStart(Component c) {
    JFrame frame = null;
    if (c != null)
      frame = (JFrame) SwingUtilities.getRoot(c);
    if (frame == null)
      return;
    timeConsumingTaskStart(frame);
  }

  public static void timeConsumingTaskStart(JFrame frame) {
    Component glass = frame.getGlassPane();
    glass.setCursor(waitCursor);
    glass.addMouseListener(new DoNothingMouseListener());
    glass.setVisible(true);
  }

  public static void timeConsumingTaskEnd(Component c) {
    JFrame frame = null;
    if (c != null)
      frame = (JFrame) SwingUtilities.getRoot(c);
    if (frame == null)
      return;
    timeConsumingTaskEnd(frame);
  }

  public static void timeConsumingTaskEnd(JFrame frame) {
    Component glass = frame.getGlassPane();
    MouseListener[] mls =
        (MouseListener[]) glass.getListeners(MouseListener.class);
    for (int i = 0; i < mls.length; i++)
      if (mls[i] instanceof DoNothingMouseListener)
        glass.removeMouseListener(mls[i]);
    glass.setCursor(defaultCursor);
    glass.setVisible(false);
  }

  protected void connectToServlet() {
    dataSource = InventoryConnectionManager.queryUserForConnection(this, dataSource);
    if (dataSource == null) {
      displayErrorString("Was unable to connect to servlet.");
      return;
    }
    Vector orgs = dataSource.getOrgNames(".",selector.getSelectedOrgPopMethod());
    if (orgs == null) {
      displayErrorString("Error. Was unable to retrieve orgs.");
    }
    else {
      String[] supplyTypes = dataSource.getSupplyTypes();
      selector.initializeComboBoxes(orgs, supplyTypes);
    }
  }

  protected static void displayErrorString(String reply) {
      displayErrorString(reply,reply);
  }

  protected static void displayErrorString(String title, String reply) {
    JOptionPane.showMessageDialog(null, reply, title,
                                  JOptionPane.ERROR_MESSAGE);
  }

  protected static void displayWarnString(String title, String reply) {
    JOptionPane.showMessageDialog(null, reply, title,
                                  JOptionPane.WARNING_MESSAGE);
  }

  public void selectionChanged(InventorySelectionEvent e) {
    Vector assetNames = null;
    Vector newOrgs;
    if (e.getID() == InventorySelectionEvent.ORG_SELECT) {
      if (e.getOrg() == null){
        newOrgs = dataSource.getOrgNames(".",e.getOrgPopMethod());
      }
      else {
        newOrgs = dataSource.getOrgNames(e.getOrg(),e.getOrgPopMethod());
      }
      if(newOrgs != null) {
	selector.reinitializeOrgBox(newOrgs);
      }
      else {
        System.out.println("InventoryUIFrame: newOrgs is null.");
      }
      if((e.getOrg() != null) &&
         !(e.getOrg().startsWith("."))) {
	assetNames = dataSource.getAssetNames(e.getOrg(),
					      e.getSupplyType());
      }
      if(assetNames == null) {
	assetNames = new Vector();
      }
      selector.setAssetNames(assetNames);

    } else if (e.getID() == InventorySelectionEvent.ORG_POP_SELECT) {

      if (dataSource == null) {
        connectToServlet ();
      }

	newOrgs = dataSource.getOrgNames(e.getOrg(),e.getOrgPopMethod());
	if(newOrgs != null) {
	  selector.reinitializeOrgBox(newOrgs);
	}
	/*** MWD remove - badly place functionality
	if(!(selector.getSelectedOrg().equals(e.getOrg()))) {
	  if(!(e.getOrg().startsWith("."))) {
	    assetNames = dataSource.getAssetNames(e.getSelectedOrg(),
						  e.getSupplyType());
	  }
	  if(assetNames == null) {
	    assetNames = new Vector();
	  }
	  selector.setAssetNames(assetNames);
	}
	***/
    } else if (e.getID() == InventorySelectionEvent.INVENTORY_SELECT) {
      String invXML = null;
      if((e.getOrg().trim().equals("")) ||
	 (e.getOrg().trim().startsWith("."))) {
	  displayErrorString("Submit","Not a legal org to submit for inventory asset");

      }

      else if((e.getAssetName() == null) ||
	 (e.getAssetName().trim().equals(""))) {
	  displayErrorString("Submit","No Asset Picked!");
	  
      }
      else {

        invXML = dataSource.getInventoryData(e.getOrg(),
                                             e.getAssetName());
      }

      if((invXML != null) && (!(invXML.trim().equals("")))) {
	  editPane.setText(invXML);
	  inventory = parser.parseString(invXML);
	  multiChart.setData(inventory);
      }
      else {
	  inventory = new BlankInventoryData(e.getOrg(),e.getAssetName());
	  editPane.setText("Null XML");
	  multiChart.setData(inventory);
      }
    }
  }

  public static String formatTimeStamp(Date dateToFormat,
                                       boolean includeSeconds) {

    String datestamp;
    TimeZone gmt = TimeZone.getDefault();
    GregorianCalendar now = new GregorianCalendar(gmt);
    now.setTime(dateToFormat);
    datestamp = "" + now.get(Calendar.YEAR);
    datestamp += prependSingle(now.get(Calendar.MONTH) + 1);
    datestamp += prependSingle(now.get(Calendar.DAY_OF_MONTH));
    datestamp += prependSingle(now.get(Calendar.HOUR_OF_DAY));
    datestamp += prependSingle(now.get(Calendar.MINUTE));
    if (includeSeconds) {
      datestamp += prependSingle(now.get(Calendar.SECOND));
    }
    return datestamp;
  }

  protected static String prependSingle(int digit) {
    if (digit < 10) {
      return "0" + digit;
    } else {
      return "" + digit;
    }
  }

  protected class HelpDialog extends JDialog {

    protected String helpFileURLStr;

    public HelpDialog(JFrame owner,
                      String aHelpFileURLStr) {
      super(owner, "Inventory GUI Help", false);
      helpFileURLStr = aHelpFileURLStr;
      setupAndLayout();
    }

    protected void setupAndLayout() {

    }

  }

  protected class OpenXMLRunnable implements Runnable {

    protected File[] openFiles;

    public OpenXMLRunnable(File[] filesToOpen) {
      openFiles = filesToOpen;
    }

    public void run() {
      doOpenXML(openFiles);
    }
  }

  protected class TaskStartRunnable implements Runnable {

    InventoryUIFrame frame;

    public TaskStartRunnable(InventoryUIFrame aFrame) {
      frame = aFrame;
    }

    public void run() {
      timeConsumingTaskStart(frame);
    }
  }

  protected class FlushXMLToScreenRunnable implements Runnable {

    protected File[] openFiles;
    protected String invXML;
    protected InventoryFileManager fm;

    public FlushXMLToScreenRunnable(String xmlStr,
                                    File[] filesToOpen,
                                    InventoryFileManager fileMgr) {
      invXML = xmlStr;
      openFiles = filesToOpen;
      fm = fileMgr;
    }

    public void run() {
      flushXMLToScreen(invXML, openFiles, fm);
    }
  }

  static class DoNothingMouseListener extends MouseInputAdapter {
    public void mouseClicked(MouseEvent e) {
      e.consume();
    }

    public void mouseDragged(MouseEvent e) {
      e.consume();
    }

    public void mouseEntered(MouseEvent e) {
      e.consume();
    }

    public void mouseExited(MouseEvent e) {
      e.consume();
    }

    public void mouseMoved(MouseEvent e) {
      e.consume();
    }

    public void mousePressed(MouseEvent e) {
      e.consume();
    }

    public void mouseReleased(MouseEvent e) {
      e.consume();
    }
  }

  public static void main(String[] args) {
    InventoryUIFrame frame = new InventoryUIFrame(args);
  }
}


