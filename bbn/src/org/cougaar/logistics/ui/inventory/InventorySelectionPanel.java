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

import java.util.Hashtable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.Box;
import javax.swing.border.LineBorder;

import java.awt.event.ItemEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Color;



/** 
 * <pre>
 * 
 * The InventorySelectionPanel is the part of the ui where the user
 * selects the inventory to be displayed.   Drop down lists of orgs,
 * class, and asset name are there so that the user can select which
 * inventory item to peruse.   Upon hitting the org selection or submit button  * the selection is broadcast via a InventorySelectionEvent to any
 * InventorySelectionListeners who have subscibed.  If there is only one 
 * class type than there is no supply type combo box displayed.
 *
 * @see InventoryUIFrame
 * @see InventorySelectionEvent
 * @see InventorySelectionListener
 *
 **/

public class InventorySelectionPanel extends JPanel 
    implements ActionListener,ItemListener {

    public static final String SUBMIT = "Submit";
    public static final String ORGS = "Org";
    public static final String SUPPLY_TYPES = "Class";
    public static final String ITEMS = "Items";

    ArrayList invListeners;

    JComboBox orgsBox;
    JComboBox supplyTypesBox;
    JComboBox assetNamesBox;

    JButton   submitButton;
    
    String currOrg;
    String currSupplyType;
    String currAssetName;
    
    JLabel supplyTypesLabel;

    Component parent;
    boolean supplyTypesActive;

    public InventorySelectionPanel(Component aParent) {
	orgsBox = new JComboBox();
	assetNamesBox = new JComboBox();
	supplyTypesBox = new JComboBox();
	invListeners = new ArrayList();
	parent = aParent;
	supplyTypesActive = true;
	initPanel();
	this.setVisible(true);
    }

    public InventorySelectionPanel(Component aParent,
				   Vector orgs,
				   String[] supplyTypes) {
	orgsBox = new JComboBox();
	assetNamesBox = new JComboBox();
	supplyTypesBox = new JComboBox();
	invListeners = new ArrayList();
	parent = aParent;
	initializeComboBoxes(orgs,supplyTypes);
	initPanel();
	this.setVisible(true);
    }

    private void initPanel() {

	JButton submitButton = new JButton(SUBMIT);

	this.setBorder(new LineBorder(Color.blue));
	assetNamesBox.setPreferredSize(new Dimension(450,25));
	//orgsBox.setPreferredSize(new Dimension(100,25));
	//supplyTypesBox.setPreferredSize(new Dimension(100,25));
        this.setLayout(new FlowLayout());
	
	JPanel orgsPanel = new JPanel();
	orgsPanel.setLayout(new FlowLayout());

	orgsBox.addItemListener(this);
	orgsPanel.add(new JLabel(ORGS));
	orgsPanel.add(orgsBox);

	JPanel supplyPanel = new JPanel();
	supplyPanel.setLayout(new FlowLayout());

	supplyTypesBox.addItemListener(this);
	supplyTypesLabel = new JLabel(SUPPLY_TYPES);
	supplyTypesLabel.setVisible(supplyTypesActive);
	supplyTypesBox.setVisible(supplyTypesActive);
	supplyPanel.add(supplyTypesLabel);
	supplyPanel.add(supplyTypesBox);

	JPanel orgsAndSupply = new JPanel();
	orgsAndSupply.setLayout(new BorderLayout());
	
	orgsAndSupply.add(orgsPanel,BorderLayout.CENTER);
	orgsAndSupply.add(supplyPanel,BorderLayout.SOUTH);

	this.add(orgsAndSupply);

	assetNamesBox.addItemListener(this);
	JPanel assetsPanel = new JPanel();
	assetsPanel.setLayout(new FlowLayout());
	assetsPanel.add(new JLabel(ITEMS));
	assetsPanel.add(assetNamesBox);

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	submitButton.setPreferredSize(new Dimension(100,25));
	submitButton.setMaximumSize(new Dimension(100,25));
	submitButton.addActionListener(this);
	buttonPanel.add(Box.createHorizontalStrut(100));
	buttonPanel.add(submitButton);
	buttonPanel.add(Box.createHorizontalStrut(100));

	JPanel assetsAndButton = new JPanel();
	assetsAndButton.setLayout(new BorderLayout());
	
	assetsAndButton.add(assetsPanel,BorderLayout.NORTH);
	assetsAndButton.add(buttonPanel,BorderLayout.CENTER);

	this.add(assetsAndButton);

	this.setPreferredSize(new Dimension(700,80));
    }

    public void initializeComboBoxes(Vector orgs, String[] supplyTypes) {

	orgsBox.removeItemListener(this);
	supplyTypesBox.removeItemListener(this);

	setOrganizations(orgs);
	setSupplyTypes(supplyTypes);

	orgsBox.addItemListener(this);
	supplyTypesBox.addItemListener(this);

	
	//MWD this should be put back in once servlet plugin is 
	//is the default.
	//fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
    }
    
    public void setSupplyTypes(String[] supplyTypes) {
	if((supplyTypes == null) ||
	   (supplyTypes.length == 0)){
	    currSupplyType = null;
	    supplyTypesActive = false;
	}
	if(supplyTypes.length == 1) {
	    currSupplyType = supplyTypes[0];
	    supplyTypesActive = false;
	}
	else {
	    supplyTypesBox.removeAllItems();
	    for(int i=0; i < supplyTypes.length ; i++) {
		String supplyType = supplyTypes[i];
		supplyTypesBox.addItem(supplyType);
	    }
	    currSupplyType = (String) supplyTypesBox.getItemAt(0);
	    supplyTypesActive = true;
	}		
	supplyTypesLabel.setVisible(supplyTypesActive);
	supplyTypesBox.setVisible(supplyTypesActive);
    }

    public void setOrganizations(Vector orgs) {
	orgsBox.removeAllItems();
	currOrg = null;
	for(int i=0; i < orgs.size() ; i++) {
	    String orgName = (String) orgs.elementAt(i);
	    orgsBox.addItem(orgName);
	}
	if(orgs.size() > 1) {
	    currOrg = (String) orgsBox.getItemAt(0);
	}
    }

    public void setAssetNames(Vector assets) {
	assetNamesBox.removeItemListener(this);
	assetNamesBox.removeAllItems();
	int currAssetIndex=-1;
	for(int i=0; i < assets.size() ; i++) {
	    String assetName = (String) assets.elementAt(i);
	    assetNamesBox.addItem(assetName);
	    if(assetName.equals(currAssetName)) {
		currAssetIndex = i;
	    }
	}
	if(currAssetIndex >= 0) {
	    assetNamesBox.setSelectedIndex(currAssetIndex);
	}
	else if(assets.size() > 1) {
	    currAssetName = (String) assetNamesBox.getItemAt(0);
	}
	else {
	    currAssetName = null;
	}
	assetNamesBox.addItemListener(this);
    }


    public void itemStateChanged(ItemEvent e){
	//System.out.println("Item State Changed event: " + e);
	//System.out.println("Item id: " + e.getID() + " and SELECTED is:" + e.SELECTED);
	if(e.getStateChange() == e.SELECTED) {
	  //System.out.println("Selected Event");
	    JComponent source = (JComponent) e.getSource();
	    if(source == orgsBox) {
		currOrg = (String) orgsBox.getSelectedItem();
		//System.out.println("Selected org " + currOrg);
		fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
	    }
	    else if(source == supplyTypesBox) {
		currSupplyType = (String) supplyTypesBox.getSelectedItem();
		// System.out.println("Selected supply type " + currSupplyType);
		fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
	    }
	    else if(source == assetNamesBox) {
		currAssetName = (String) assetNamesBox.getSelectedItem();
		// System.out.println("Selected asset " + currAssetName);
	    }
	}
	
    }

    public void actionPerformed(ActionEvent e){
	if(e.getActionCommand().equals(SUBMIT)) {
	  //System.out.println("Pressed Submit");
	    fireInventorySelectionEvent(InventorySelectionEvent.INVENTORY_SELECT);
	}
    }

    private static void displayErrorString(String reply) {
	JOptionPane.showMessageDialog(null, reply, reply, 
				      JOptionPane.ERROR_MESSAGE);
    }    

    public void addInventorySelectionListener(InventorySelectionListener l) {
	invListeners.add(l);
    }

    public void removeInventorySelectionListener(InventorySelectionListener l) {
	invListeners.remove(l);
    }

    protected void fireInventorySelectionEvent(int id) {
	InventorySelectionEvent e = new InventorySelectionEvent(id,
								this,
								currOrg,
								currSupplyType,
								currAssetName);
	for(int i=0 ; i < invListeners.size() ; i++) {
	    InventorySelectionListener l = (InventorySelectionListener) invListeners.get(i);
	    l.selectionChanged(e);
	}
    }
	
    public String getSelectedOrg() { return currOrg; }
    public String getSelectedSupplyType() { return currSupplyType; }
    public String getSelectedAssetName() { return currAssetName; }

}


