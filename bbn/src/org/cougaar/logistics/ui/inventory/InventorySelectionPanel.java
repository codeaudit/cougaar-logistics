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
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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
  
  protected final static Insets BLANK_INSETS = new Insets(0, 0, 0, 0);
  
  public static final String SUBMIT = "Submit";
  public static final String ORGS = "Org";
  public static final String SUPPLY_TYPES = "Class";
  public static final String ITEMS = "Items";
  
  public static final String ORGS_ALL = InventorySelectionEvent.ORGS_ALL;
  public static final String ORGS_NAV = InventorySelectionEvent.ORGS_NAV;
  public static final String ORGS_HIST = InventorySelectionEvent.ORGS_HIST;
  
  public static final String SUBMIT_TOOL_TIP = "Retrieves graph data for given organization/item";
  public static final String ORG_TOOL_TIP = "Select Organization";
  public static final String POP_TOOL_TIP = "Select Org Combo box population method";
  public static final String SUPPLY_TOOL_TIP = "Select Class of Supply to filter items";
  public static final String ASSET_TOOL_TIP = "Select Graph data inventory item";

  public static final String[] ORG_POP_OPTIONS = {ORGS_NAV, ORGS_HIST, ORGS_ALL};

  protected ArrayList invListeners;

  JComboBox orgsBox;
  JComboBox supplyTypesBox;
  JComboBox assetNamesBox;
  
  JComboBox orgsPopulationBox;
  
  JButton   submitButton;
    
  protected String currOrg;
  protected String currSupplyType;
  protected String currAssetName;
    
  JLabel supplyTypesLabel;
  
  Component parent;
  boolean supplyTypesActive;
  boolean supplyTypesVisible;

    public InventorySelectionPanel(Component aParent) {
	orgsBox = new JComboBox();
	assetNamesBox = new JComboBox();
	supplyTypesBox = new JComboBox();
	orgsPopulationBox = new JComboBox(ORG_POP_OPTIONS);
	invListeners = new ArrayList();
	parent = aParent;
	supplyTypesActive = true;
	supplyTypesVisible = true;
	initPanel();
	this.setVisible(true);
    }

    public InventorySelectionPanel(Component aParent,
				   Vector orgs,
				   String[] supplyTypes) {
	orgsBox = new JComboBox();
	assetNamesBox = new JComboBox();
	supplyTypesBox = new JComboBox();
	orgsPopulationBox = new JComboBox(ORG_POP_OPTIONS);
	invListeners = new ArrayList();
	parent = aParent;
	initializeComboBoxes(orgs,supplyTypes);
	initPanel();
	this.setVisible(true);
    }

    private void initPanel() {

	JButton submitButton = new JButton(SUBMIT);

	submitButton.setToolTipText(SUBMIT_TOOL_TIP);
	orgsBox.setToolTipText(ORG_TOOL_TIP);
	orgsPopulationBox.setToolTipText(POP_TOOL_TIP);
	supplyTypesBox.setToolTipText(SUPPLY_TOOL_TIP);
	assetNamesBox.setToolTipText(ASSET_TOOL_TIP);

	GridBagConstraints constraints;

	this.setBorder(new LineBorder(Color.blue));
	assetNamesBox.setMinimumSize(new Dimension(450,25));
	orgsBox.setMinimumSize(new Dimension(100,25));
	//supplyTypesBox.setPreferredSize(new Dimension(100,25));
        this.setLayout(new GridBagLayout());
	
	JPanel orgsPanel = new JPanel();
	orgsPanel.setLayout(new FlowLayout());

	orgsBox.addActionListener(this);
	constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, 0,
					     1, 1, 0.2, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.NONE,
					     BLANK_INSETS, 0, 0);
	//orgsPanel.add(new JLabel(ORGS),constraints);
	constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, 0,
					     6, 1, 0.8, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.HORIZONTAL,
					     BLANK_INSETS, 0, 0);
	//orgsPanel.add(orgsBox,constraints);
	orgsPanel.add(new JLabel(ORGS));
	orgsPanel.add(orgsBox,constraints);


	JPanel supplyPanel = new JPanel();
	supplyPanel.setLayout(new FlowLayout());

	supplyTypesBox.addItemListener(this);
	supplyTypesLabel = new JLabel(SUPPLY_TYPES);
	supplyTypesLabel.setVisible(supplyTypesVisible);
	supplyTypesBox.setVisible(supplyTypesVisible);
	supplyPanel.add(supplyTypesLabel);
	supplyPanel.add(supplyTypesBox);

	JPanel orgsAndPop = new JPanel();
	orgsAndPop.setLayout(new GridBagLayout());
	orgsPopulationBox.addItemListener(this);
	orgsPopulationBox.setSelectedItem(ORGS_NAV);
	constraints = new GridBagConstraints(0,GridBagConstraints.RELATIVE,
					     1, 1, 1.0, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.NONE,
					     BLANK_INSETS, 0, 0);
					     
	orgsAndPop.add(orgsPanel,constraints);
	orgsAndPop.add(orgsPopulationBox,constraints);

	assetNamesBox.addItemListener(this);
	JPanel assetsPanel = new JPanel();
	assetsPanel.setLayout(new FlowLayout());
	assetsPanel.add(new JLabel(ITEMS));
	assetsPanel.add(assetNamesBox);
	//assetsPanel.setBorder(new LineBorder(Color.black));

	submitButton.setPreferredSize(new Dimension(100,25));
	//submitButton.setMaximumSize(new Dimension(100,25));
	submitButton.addActionListener(this);

	
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	//buttonPanel.setBorder(new LineBorder(Color.black));
	//buttonPanel.add(Box.createHorizontalStrut(60));
	buttonPanel.add(submitButton);
	//buttonPanel.add(Box.createHorizontalStrut(100));
	

	/***

	JPanel buttonAndSupplyPanel = new JPanel();
	buttonAndSupplyPanel.setLayout(new FlowLayout());
	buttonAndSupplyPanel.setBorder(new LineBorder(Color.black));
	buttonAndSupplyPanel.add(supplyPanel);
	buttonAndSupplyPanel.add(buttonPanel);
	****/


	JPanel assetsAndButton = new JPanel();
	assetsAndButton.setLayout(new GridBagLayout());

	constraints = new GridBagConstraints(0,0,
					     8, 1, 1.0, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.NONE,
					     BLANK_INSETS, 0, 0);


       	assetsAndButton.add(assetsPanel,constraints);
	//assetsAndButton.add(buttonAndSupplyPanel);

	constraints = new GridBagConstraints(0,1,
					     1, 1, 0.12, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.NONE,
					     BLANK_INSETS, 0, 0);

	assetsAndButton.add(supplyPanel,constraints);

	constraints = new GridBagConstraints(GridBagConstraints.RELATIVE,1,
					     7, 1, 0.88, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.HORIZONTAL,
					     BLANK_INSETS, 0, 0);


	assetsAndButton.add(buttonPanel,constraints);



	constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, 0,
					     2, 1, 0.4, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.BOTH,
					     BLANK_INSETS, 0, 0);

	this.add(orgsAndPop,constraints);


	constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, 0,
					     3, 1, 0.6, 1.0,
					     GridBagConstraints.WEST,
					     GridBagConstraints.NONE,
					     BLANK_INSETS, 0, 0);

	
	this.add(assetsAndButton,constraints);

	this.setMaximumSize(new Dimension(780,80));
    }

    public void initializeComboBoxes(Vector orgs, String[] supplyTypes) {

	orgsBox.removeActionListener(this);
	supplyTypesBox.removeItemListener(this);

	setOrganizations(orgs);
	setSupplyTypes(supplyTypes);
	setAssetNames(new Vector());

	orgsBox.addActionListener(this);
	supplyTypesBox.addItemListener(this);

	
	//MWD this should be put back in once servlet plugin is 
	//is the default.
	//fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
    }

    public void reinitializeOrgBox(Vector orgs) {

	orgsBox.removeActionListener(this);

	String origOrg = currOrg;
	setOrganizations(orgs);
	orgsBox.setSelectedItem(origOrg);
	currOrg = (String) orgsBox.getSelectedItem();
	if(!(currOrg.equals(origOrg))) {
          //System.out.println("InventorySelectionPanel>>reinitializeOrgBox - So |" + currOrg + "| is different from |" + origOrg);
	    fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
	}
	orgsBox.addActionListener(this);

    }
    
    public void setSupplyTypes(String[] supplyTypes) {
	if((supplyTypes == null) ||
	   (supplyTypes.length == 0)){
	    currSupplyType = null;
	    supplyTypesActive = false;
	    supplyTypesVisible = false;
	}
	//	if(supplyTypes.length == 1) {
	//    currSupplyType = supplyTypes[0];
	//    supplyTypesActive = false;
	//    supplyTypesVisible = true;
	//}
	else {
	    supplyTypesBox.removeAllItems();
	    for(int i=0; i < supplyTypes.length ; i++) {
		String supplyType = supplyTypes[i];
		supplyTypesBox.addItem(supplyType);
	    }
	    currSupplyType = (String) supplyTypesBox.getItemAt(0);
	    if(supplyTypes.length == 1) {
		supplyTypesActive = false;
	    }
	    else {
		supplyTypesActive = true;
	    }
	    supplyTypesVisible = true;
	}		
	supplyTypesLabel.setVisible(supplyTypesVisible);
	supplyTypesBox.setVisible(supplyTypesVisible);
	supplyTypesBox.setEnabled(supplyTypesActive);
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

    public void setSelectedOrgAsset(String org, String assetName) {
	assetNamesBox.removeItemListener(this);
	orgsBox.removeActionListener(this);
	orgsBox.setSelectedItem(org);
	assetNamesBox.setSelectedItem(assetName);
	currOrg = org;
	currAssetName = assetName;
	assetNamesBox.addItemListener(this);
	orgsBox.addActionListener(this);
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
	else if(assets.size() >= 1) {
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
	    else if(source == orgsPopulationBox) {
		String currOrgPop = (String) getSelectedOrgPopMethod();

		/*** MWD take out until the right comboBoxEditor is in to do this
		if(currOrgPop.equals(ORGS_NAV)) {
		    orgsBox.setEditable(false);
		}
		else {
		    orgsBox.setEditable(true);
		}
		***/

		fireInventorySelectionEvent(InventorySelectionEvent.ORG_POP_SELECT);
	    }
	}
	
    }

    public void actionPerformed(ActionEvent e){
	if(e.getActionCommand().equals(SUBMIT)) {
	  //System.out.println("Pressed Submit");
	    fireInventorySelectionEvent(InventorySelectionEvent.INVENTORY_SELECT);
	}
	else if(e.getSource() == orgsBox) {
          //System.out.println("InventorySelectionPanel: Orgs box action is: " + e);
	    currOrg = (String) orgsBox.getSelectedItem();
          //This is fine if there is already an org selected from previous navigation but
          // if you haven't then you'll get an NPE
            if (currOrg == null) {
              currOrg = ".";
            }
	    fireInventorySelectionEvent(InventorySelectionEvent.ORG_SELECT);
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
								currAssetName,
								getSelectedOrgPopMethod());
	for(int i=0 ; i < invListeners.size() ; i++) {
	    InventorySelectionListener l = (InventorySelectionListener) invListeners.get(i);
	    l.selectionChanged(e);
	}
    }
	
    public String getSelectedOrg() { return currOrg; }
    public String getSelectedSupplyType() { return currSupplyType; }
    public String getSelectedAssetName() { return currAssetName; }
    public String getSelectedOrgPopMethod() { return (String) orgsPopulationBox.getSelectedItem(); }
    

}


