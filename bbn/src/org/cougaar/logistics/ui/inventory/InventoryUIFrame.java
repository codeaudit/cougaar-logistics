/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory;


import java.util.Vector;
import java.util.Hashtable;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;


import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.Box;
import javax.swing.JFileChooser;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.IOException;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/** 
 * <pre>
 * 
 * The InventoryUIFrame is the root object that maintains the primary
 * window where the inventory charts are displayed.
 * 
 * @see InventoryConnectionManager
 * @see InventoryDataSource
 * @see InventoryLevelPanel
 * @see InventoryChartPanel
 * @see ResupplyTaskPanel
 * @see DemandChartPanel
 *
 **/


public class InventoryUIFrame extends JFrame 
    implements ActionListener,InventorySelectionListener
{

    InventoryDataSource dataSource;
    private Container contentPane;

    JTextArea editPane;
    JFileChooser fileChooser;

    MultiChartPanel     multiChart;
    InventoryData       inventory;
    
    private Logger logger;

    String defaultCSVPath;


    InventorySelectionPanel selector;
    InventoryXMLParser parser;

    public InventoryUIFrame() {
	super("Inventory GUI");
	addWindowListener(new WindowAdapter() {		
		public void windowClosing(WindowEvent e) { System.exit(0); }
	    });

	parser = new InventoryXMLParser();
	logger = Logging.getLogger(this);
	contentPane = getRootPane().getContentPane();

	

	String baseDir = System.getProperty("org.cougaar.workspace");
	if((baseDir == null) ||
	   (baseDir.trim().equals(""))) {
	    baseDir = System.getProperty("org.cougaar.install.path");
	    baseDir = baseDir + File.separator + "workspace";
	}
	defaultCSVPath = baseDir + File.separator + "INVGUICSV" + File.separator + formatTimeStamp(new Date(),false) + File.separator; 
	File pathDirs = new File(defaultCSVPath);
	try {
	    pathDirs.mkdirs();
	}
	catch(Exception e) {
	    logger.error("Error creating default directory " + defaultCSVPath, e);
	}	

	fileChooser = new JFileChooser(pathDirs);
	// fills frame
	doMyLayout();
	dataSource=null;
	pack();
	setSize(800,600);
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	// set location to middle of the screen
	setLocation(screenSize.width/2 - (int)this.getSize().getWidth()/2,
		    screenSize.height/2 - (int)this.getSize().getHeight()/2);
	setVisible(true);
    }

    protected void doMyLayout() {
	getRootPane().setJMenuBar(makeMenus());

	JTabbedPane tabs = new JTabbedPane();

	contentPane.add(tabs,BorderLayout.CENTER);

	JPanel editPanel = new JPanel();
	editPanel.setLayout(new BorderLayout());

        //Create a text area.
        editPane = new JTextArea();
        editPane.setLineWrap(true);
        editPane.setWrapStyleWord(true);
        JScrollPane areaScrollPane = new JScrollPane(editPane);
        areaScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(new Dimension(700,500));
        areaScrollPane.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder("XML"),
                                BorderFactory.createEmptyBorder(5,5,5,5)),
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
	
	editPanel.add(buttonPanel,BorderLayout.NORTH);
	editPanel.add(areaScrollPane,BorderLayout.CENTER);

	multiChart = new MultiChartPanel();
	multiChart.setPreferredSize(new Dimension(700,250));

	tabs.add("InventoryChart",multiChart);
	tabs.add("XML",editPanel);

	selector = new InventorySelectionPanel(this);
	selector.addInventorySelectionListener(this);
	
	contentPane.add(selector,BorderLayout.SOUTH);
    }

    protected JMenuBar makeMenus() {
	JMenuBar retval = new JMenuBar();
	
	JMenu file = new JMenu("File");
	JMenuItem quit = new JMenuItem(InventoryMenuEvent.MENU_Exit);
	JMenuItem save = new JMenuItem(InventoryMenuEvent.MENU_SaveXML);
	quit.addActionListener(this);
	save.addActionListener(this);
	file.add(quit);
	file.add(save);
	retval.add(file);
    
	JMenu connection = new JMenu("Connection");
	JMenuItem connect = new JMenuItem(InventoryMenuEvent.MENU_Connect);
	connect.addActionListener(this);
	connection.add(connect);
	retval.add(connection);

	return retval;
    }


    public void actionPerformed(ActionEvent e) {
	if(e.getActionCommand().equals(InventoryMenuEvent.MENU_Exit)) {
	    System.exit(0);
	}
	else if(e.getActionCommand().equals(InventoryMenuEvent.MENU_Connect)) {
	    connectToServlet();
	}
	else if(e.getActionCommand().equals(InventoryMenuEvent.MENU_SaveXML)) {
	    if(inventory != null) {
		String fileID = defaultCSVPath + inventory.getOrg() + "-" + (inventory.getItem().replaceAll("/","-")) + "-" + formatTimeStamp(new Date(), false) + ".csv";
		System.out.println("Save to file: " + fileID);
		fileChooser.setSelectedFile(new File(fileID));
	    }
	    int retval = fileChooser.showSaveDialog(this);
	    if(retval == fileChooser.APPROVE_OPTION) {
		File saveFile = fileChooser.getSelectedFile();
		try{
		    FileWriter fw = new FileWriter(saveFile);
		    fw.write(editPane.getText());
		    fw.flush();
		    fw.close();
		}
		catch(IOException ioe) {
		    throw new RuntimeException(ioe);
		}
	    }
		
	}
	else if(e.getActionCommand().equals("Parse")) {
	    logger.info("Parsing");
	    inventory = parser.parseString(dataSource.getCurrentInventoryData());
	    multiChart.setData(inventory);
	}
	else if(e.getActionCommand().equals("Data")) {
	    editPane.setText(dataSource.getCurrentInventoryData());
	}
    }

    protected void connectToServlet() {
	dataSource = InventoryConnectionManager.queryUserForConnection(this);
	if(dataSource == null) {
	    displayErrorString("Was unable to connect to servlet.");
	    return;
	}
	Vector orgs = dataSource.getOrgNames();
	String[] supplyTypes = dataSource.getSupplyTypes();
	selector.initializeComboBoxes(orgs,supplyTypes);		
    }

    private static void displayErrorString(String reply) {
	JOptionPane.showMessageDialog(null, reply, reply, 
				      JOptionPane.ERROR_MESSAGE);
    }    

    public void selectionChanged(InventorySelectionEvent e) {
	if(e.getID() == InventorySelectionEvent.ORG_SELECT) {
	    Vector assetNames = dataSource.getAssetNames(e.getOrg(),
							 e.getSupplyType());
	    selector.setAssetNames(assetNames);
	}
	else if(e.getID() == InventorySelectionEvent.INVENTORY_SELECT) {
	    String invXML = dataSource.getInventoryData(e.getOrg(),
						 e.getAssetName());
	    editPane.setText(invXML);
	    inventory = parser.parseString(invXML);
	    multiChart.setData(inventory);	    
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
	if(includeSeconds) {
	    datestamp += prependSingle(now.get(Calendar.SECOND));
	}
	return datestamp;
    }   

    private static String prependSingle(int digit) {
	if(digit < 10) {
	    return "0" + digit;
	}
	else {
	    return "" + digit;
	}
    }

  public static void main(String[] args)
  {
      InventoryUIFrame frame = new InventoryUIFrame();
  }
}


