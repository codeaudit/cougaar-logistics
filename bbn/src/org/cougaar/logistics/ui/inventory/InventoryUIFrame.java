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

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;


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


    InventorySelectionPanel selector;
  InventoryXMLParser parser;

    public InventoryUIFrame() {
	super("Inventory GUI");
	addWindowListener(new WindowAdapter() {		
		public void windowClosing(WindowEvent e) { System.exit(0); }
	    });

	parser = new InventoryXMLParser();
	contentPane = getRootPane().getContentPane();
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

        //Create a text area.
        editPane = new JTextArea();
        editPane.setLineWrap(true);
        editPane.setWrapStyleWord(true);
        JScrollPane areaScrollPane = new JScrollPane(editPane);
        areaScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(new Dimension(600,400));
        areaScrollPane.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder("XML"),
                                BorderFactory.createEmptyBorder(5,5,5,5)),
                areaScrollPane.getBorder()));

	JButton parseButton = new JButton("Parse");
	parseButton.addActionListener(this);
	contentPane.add(parseButton,BorderLayout.NORTH);

	contentPane.add(areaScrollPane,BorderLayout.CENTER);

	selector = new InventorySelectionPanel(this);
	selector.addInventorySelectionListener(this);
	
	contentPane.add(selector,BorderLayout.SOUTH);
    }

    protected JMenuBar makeMenus() {
	JMenuBar retval = new JMenuBar();
	
	JMenu file = new JMenu("File");
	JMenuItem quit = new JMenuItem(InventoryMenuEvent.MENU_Exit);
	quit.addActionListener(this);
	file.add(quit);
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
	    System.out.println("Connecting....");
	    connectToServlet();
	}
	else if(e.getActionCommand().equals("Parse")) {
	    parser.parseString(editPane.getText());
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
	}
    }


   
  public static void main(String[] args)
  {
      InventoryUIFrame frame = new InventoryUIFrame();
  }
}


