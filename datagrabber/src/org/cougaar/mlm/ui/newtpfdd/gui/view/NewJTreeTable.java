/*
   Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
   Reserved.
  
   This material has been developed pursuant to the BBN/RTI "ALPINE"
   Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
   Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

   @author Sundar Narasimhan, Daniel Bromberg
*/

/*
 * Loosely based on Sun code which came with this notice.
 *
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
    

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CarrierType;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoType;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.ConvoyNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CategoryNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LocationNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.AssetPrototypeNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CarrierInstance;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.MissionNode;

import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;
import org.cougaar.mlm.ui.newtpfdd.gui.view.route.AssetInstanceRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.route.CarrierInstanceRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.LegAssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetAssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.CarrierDetailRequest;

/**
 * This example shows how to create a simple JTreeTable component, 
 * by using a JTree as a renderer (and editor) for the cells in a 
 * particular column in the JTable.  
 *
 * @version %I% %G%
 *
 * @author Philip Milne
 * @author Scott Violet
 */

public class NewJTreeTable extends JTable implements ActionListener, SwingUpdate
{
  private NewTPFDDShell shell;
  private DatabaseState dbState;
  
  protected TreeTableCellRenderer tree;
  protected TreeTableCellRenderer filtered;
  protected ScheduleCellRenderer  scheduleCellRenderer;
  protected Runnable fireChangedRunnable;
  
  JCheckBoxMenuItem  miNonTransport;
  
  TaskModel taskModel;
  TreeTableModelAdapter ttModelAdapter;
  
  private AwareMenuItem TPFDDItem;
  private AwareMenuItem carrierTPFDDItem;
  private AwareMenuItem filterItem;
  private AwareMenuItem assetRouteItem;
  private AwareMenuItem carrierRouteItem;
  
  protected FilterDialog filterDialog;
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.NewJTreeTable.debug", 
									   "false"));
  //    Node selectedNode = null;

    public void resetColumns()
    {
	// set column widths
	TableColumn column = null;
	if (taskModel instanceof AssetModel) {
	  column = getColumnModel().getColumn(0);	
	  column.setPreferredWidth(550);
	  column = getColumnModel().getColumn(1);	
	  column.setPreferredWidth(460);
	} else {
	  column = getColumnModel().getColumn(0);	
	  column.setPreferredWidth(500);
	  column = getColumnModel().getColumn(1);	
	  column.setPreferredWidth(50);
	  column = getColumnModel().getColumn(2);	
	  column.setPreferredWidth(50);
	  column = getColumnModel().getColumn(3);
	  column.setPreferredWidth(410);
	}
    }

  public NewJTreeTable(NewTPFDDShell shell, TreeTableModel treeTableModel, Font myFont)
    {
	super();
	setFont(myFont);
	setRowHeight(getFontMetrics(myFont).getHeight());
	taskModel = (TaskModel) treeTableModel;

	// Create the tree. It will be used as a renderer and editor. 
	tree = new TreeTableCellRenderer(treeTableModel, this, myFont); 

	// Install a tableModel representing the visible rows in the tree. 
	super.setModel(ttModelAdapter = new TreeTableModelAdapter(treeTableModel, tree)); 

	// Force the JTable and JTree to share their row selection models. 
	tree.setSelectionModel(new DefaultTreeSelectionModel() { 
		// Extend the implementation of the constructor, as if: 
		/* public this() */ {
	    setSelectionModel(listSelectionModel); 
		} 
	    }); 
	// Make the tree and table row heights the same.
	tree.setRowHeight(getRowHeight());

	// Install the tree editor renderer and editor. 
	setDefaultRenderer(TreeTableModel.class, tree); 
	setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());  
	
	// Install the schedule cell renderer
	scheduleCellRenderer = new ScheduleCellRenderer(taskModel instanceof AssetModel ? true : false);
	setDefaultRenderer(Node.class, scheduleCellRenderer); 

	setShowGrid(false);
	setIntercellSpacing(new Dimension(0, 0)); 	        

	// set column widths
	resetColumns();

	this.shell = shell;
	this.dbState = shell;
	
	//	resetFilterDialog ();
	
	MouseAdapter rowAdapter = new MouseAdapter()
	  {
		private MouseEvent saveEvent = null;
		private int TPFDDflags = MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK;
		
		public void mousePressed(MouseEvent e)
		{
		  // Debug.out("JTT:MTT:MA:mP PRESS " + e.getPoint());
		  saveEvent = e;
		}

		public void mouseReleased(MouseEvent e)
		{
		  // Debug.out("JTT:MTT:MA:mR RELEASE " + e.getPoint());
		  if ( saveEvent != null
			   && e.getX() >= 0 && e.getX() <= e.getComponent().getWidth()
			   && e.getY() >= 0 && e.getY() <= e.getComponent().getHeight()
			   && Math.abs(e.getX() - saveEvent.getX()) < 25 // cells are much wider than tall
			   && Math.abs(e.getY() - saveEvent.getY()) < 10 )
			clicked(e);
		}

		private void clicked(MouseEvent e)
		{
		  saveEvent = null;
		  int row = rowAtPoint(e.getPoint());
		  int column = columnAtPoint(e.getPoint());
		  Object o = getValueAt(row, taskModel.getColumnCount()-1);
		  if ( !(o instanceof Node) ) {
			OutputHandler.out("JTT:JTT:MA Warning: table was rearranged. Don't know where to get task.");
			return;
		  }
		  Node node = (Node)o;
		  if ( (e.getModifiers() & TPFDDflags) != 0 && column >= 0 && column < 3) {
		    String unitName = null;
		    String unitDBID=null;
		    
		    if (taskModel instanceof AssetModel) {
		    }
		    else{
		      Node unitNode = taskModel.getOrgParent(node.getUID());
		      unitName=unitNode.getDisplayName();
		      unitDBID=taskModel.getDBUID(unitNode);
		    }

		    //getTPFDDItem().setName("" + taskModel.getDBUID(unitNode));
		    //if (debug) System.out.println ("NewJTreeTable - UnitNode is " + unitNode);

		    getPopup(node, 
			     unitName,
			     unitDBID).show(e.getComponent(), e.getX(), e.getY());
		  }
		}
	  };
	addMouseListener(rowAdapter);

	fireChangedRunnable = new Runnable()
	  {
		public void run() {
		  ((AbstractTableModel)getModel()).fireTableDataChanged();
		}
	  };
    }

  private JMenuItem getTPFDDItem(Node selectedNode, 
				 String unitName, 
				 String unitDBUID){
    if( TPFDDItem == null ){
      TPFDDItem = new AwareMenuItem();
      TPFDDItem.addActionListener(this);
    }
    TPFDDItem.setNode(selectedNode);
    TPFDDItem.setUnitName(unitName);
    TPFDDItem.setUnitDBUID(unitDBUID);
    TPFDDItem.setActionCommand ("showTPFDD");
    TPFDDItem.setName("tpfddItem");

    String desc=null;
    if(selectedNode instanceof DBUIDNode){
      DBUIDNode curNode=(DBUIDNode)selectedNode;
      int type=curNode.getType();
      if(type==UIDGenerator.CARRIER_PROTOTYPE||
	 type==UIDGenerator.CARRIER_INSTANCE||
	 type==UIDGenerator.ASSET_PROTOTYPE||
	 type==UIDGenerator.ASSET_INSTANCE||
	 type==UIDGenerator.CONVOY||
	 type==UIDGenerator.LOCATION)
	desc=curNode.getDisplayName();
    }
    String text="New TPFDD";
    if(unitName != null)
      text+=" for "+unitName;
    if(unitName !=null && desc != null) {
      text+=": ";
    } else{
      text+=" ";
    }
    if(desc !=null)
      text+=desc;
    TPFDDItem.setText(text);
    return TPFDDItem;
  }
  private JMenuItem getCarrierTPFDDItem(Node selectedNode){
    if(carrierTPFDDItem == null ){
      carrierTPFDDItem = new AwareMenuItem();
      carrierTPFDDItem.addActionListener(this);
    }
    carrierTPFDDItem.setNode(selectedNode);
    carrierTPFDDItem.setName("carriertpfddItem");
    carrierTPFDDItem.setActionCommand ("showCarrierTPFDD");
    
    String text="New Carrier Use";
    if(selectedNode instanceof DBUIDNode){
      DBUIDNode curNode=(DBUIDNode)selectedNode;
      text+=": "+curNode.getDisplayName();
    }
    carrierTPFDDItem.setText(text);
    return carrierTPFDDItem;
  }

  private JMenuItem getfilterItem(Node selectedNode, String unitName, String unitDBUID){
    if(filterItem == null){
      filterItem = new AwareMenuItem();
      filterItem.addActionListener(this);
    }
    filterItem.setNode(selectedNode);
    filterItem.setUnitName(unitName);
    filterItem.setUnitDBUID(unitDBUID);
    filterItem.setText("Select filter...");
    filterItem.setName("filterItem");
    filterItem.setActionCommand ("showFilterDialog");
    return filterItem;
  }

  private JMenuItem getAssetRouteItem(Node selectedNode, 
				      String unitName, 
				      String unitDBUID){
    if(assetRouteItem==null){
      assetRouteItem=new AwareMenuItem();
      assetRouteItem.addActionListener(this);
    }
    assetRouteItem.setNode(selectedNode);
    assetRouteItem.setUnitName(unitName);
    assetRouteItem.setUnitDBUID(unitDBUID);
    String text="Display Route";
    if(selectedNode instanceof DBUIDNode){
      DBUIDNode curNode=(DBUIDNode)selectedNode;
      text+=": "+curNode.getDisplayName();
    }
    assetRouteItem.setText(text);
    assetRouteItem.setActionCommand("displayAssetRoute");
    return assetRouteItem;
  }

  private JMenuItem getCarrierRouteItem(Node selectedNode, 
					String unitName, 
					String unitDBUID){
    if(carrierRouteItem==null){
      carrierRouteItem=new AwareMenuItem();
      carrierRouteItem.addActionListener(this);
    }
    carrierRouteItem.setNode(selectedNode);
    carrierRouteItem.setUnitName(unitName);
    carrierRouteItem.setUnitDBUID(unitDBUID);
    String text="Display Route";
    if(selectedNode instanceof DBUIDNode){
      DBUIDNode curNode=(DBUIDNode)selectedNode;
      text+=": "+curNode.getDisplayName();
    }
    carrierRouteItem.setText(text);
    carrierRouteItem.setActionCommand("displayCarrierRoute"); 
    return carrierRouteItem;
  }

  private JMenuItem getAssetDetailsItem(Node selectedNode,
					String unitName,
					String unitDBUID){
    AwareMenuItem assetDetailsItem=new AwareMenuItem();
    assetDetailsItem.addActionListener(this);
    assetDetailsItem.setNode(selectedNode);
    assetDetailsItem.setUnitName(unitName);
    assetDetailsItem.setUnitDBUID(unitDBUID);
    String text="Display Details";
    if(selectedNode instanceof DBUIDNode){
      DBUIDNode curNode=(DBUIDNode)selectedNode;
      text+=" for: "+curNode.getDisplayName();
    }
    assetDetailsItem.setText(text);
    assetDetailsItem.setActionCommand("displayDetails");
    return assetDetailsItem;
  }

  private JPopupMenu getPopup(Node selectedNode, String unitName, String unitDBUID)
  {
    JPopupMenu carrierPopup = new JPopupMenu();
    //This is needed to prevent refresh problem because this thing uses legacy
    //awt stuff...
    carrierPopup.setLightWeightPopupEnabled(false);
    try{
      if(selectedNode != null){
	if(taskModel instanceof AssetModel) {
	  fillAssetPopup(carrierPopup, selectedNode, unitName,unitDBUID);
	} else {
	  fillUnitPopup(carrierPopup, selectedNode, unitName, unitDBUID);
    }
      }
    }catch ( Exception e ) {
      handleException(e);
    }
    return carrierPopup;
  }
  
  private void fillAssetPopup(JPopupMenu menu, Node selectedNode,
			      String unitName, String unitDBUID){
    int type=-1;
    if(selectedNode instanceof DBUIDNode)
      type=((DBUIDNode)selectedNode).getType();

    if(type!=UIDGenerator.LEG&&
       !taskModel.getTree().getRoot().equals(selectedNode)){
      menu.add(getCarrierTPFDDItem(selectedNode));
    }

    if(type==UIDGenerator.CARRIER_INSTANCE||
       //This is a total hack: should be CARRIER_INSTANCE but AssetModel
       //uses the wrong type...
       type==UIDGenerator.ASSET_PROTOTYPE){
      menu.add(getTPFDDItem(selectedNode,unitName,unitDBUID));
    }

    if(type==UIDGenerator.CARRIER_INSTANCE){
      menu.add(getCarrierRouteItem(selectedNode,unitName, unitDBUID));	  
    }

    if((type==UIDGenerator.LEG && !(selectedNode instanceof MissionNode)) ||
       (type==UIDGenerator.CARRIER_INSTANCE)){
      menu.add(getAssetDetailsItem(selectedNode,unitName,unitDBUID));
    }
  }

  private void fillUnitPopup(JPopupMenu menu, Node selectedNode, 
			     String unitName, String unitDBUID){
    int type=-1;
    if(selectedNode instanceof DBUIDNode)
      type=((DBUIDNode)selectedNode).getType();

    if(type!=UIDGenerator.LEG){
      menu.add(getTPFDDItem(selectedNode,unitName, unitDBUID));
    }

    if(type==UIDGenerator.CARRIER_INSTANCE||
       type==UIDGenerator.CARRIER_PROTOTYPE){
      menu.add(getCarrierTPFDDItem(selectedNode));
    }

    if(type==UIDGenerator.ASSET_INSTANCE){
      menu.add(getAssetRouteItem(selectedNode, unitName, unitDBUID));	  
    }
    if(type==UIDGenerator.CARRIER_INSTANCE){
      menu.add(getCarrierRouteItem(selectedNode,unitName, unitDBUID));	  
    }

    if(type==UIDGenerator.ASSET_INSTANCE||
       type==UIDGenerator.CARRIER_INSTANCE){
      menu.add(getAssetDetailsItem(selectedNode,unitName,unitDBUID));
    }

    menu.addSeparator();
    menu.add(getfilterItem(selectedNode,unitName, unitDBUID));
  }
  

    private void handleException(Exception exception)
    {
	OutputHandler.out(ExceptionTools.toString("JTT:hE", exception));
    }
  
  public void actionPerformed(ActionEvent ae) {
    Object source = ae.getSource();
    String unitName;
    String unitDBUID=null;
    Node selectedNode=null;
    DBUIDNode curNode=null;

    if(source instanceof AwareMenuItem){
      AwareMenuItem ami=(AwareMenuItem)source;
      unitName=ami.getUnitName();
      unitDBUID=ami.getUnitDBUID();
      selectedNode=ami.getNode();
    }else{
      return;
    }
    if(selectedNode==null){
      System.err.println("NewJTreeTable: Selected Node is null.");
      return;
    }

    if(selectedNode instanceof DBUIDNode){
      curNode=(DBUIDNode)selectedNode;
    }

    if (debug)
      System.out.println ("NewJTreeTable.actionPerformed - source is " + source + 
						  "\n\tcommand " + ae.getActionCommand());

    if (ae.getActionCommand ().equals ("showFilterDialog")) {
      makeFilterDialogVisible();
    }else if(ae.getActionCommand().equals("displayAssetRoute")){
      if(curNode==null ||
	 curNode.getType()!=UIDGenerator.ASSET_INSTANCE)
	return;
      String dbuid=taskModel.getDBUID(curNode);
      shell.showRouteView(new 
	AssetInstanceRequest(curNode.getDisplayName(),
			     dbuid));
    }else if(ae.getActionCommand().equals("displayCarrierRoute")){
      if(curNode==null ||
	 curNode.getType()!=UIDGenerator.CARRIER_INSTANCE)
	return;
      String dbuid=taskModel.getDBUID(curNode);
      CarrierInstanceRequest cir;
      if(unitDBUID==null){
	cir = new CarrierInstanceRequest(curNode.getDisplayName(),
					 dbuid);
      }else{
	cir = new CarrierInstanceRequest(curNode.getDisplayName(),
					 dbuid, unitDBUID);
      }
      shell.showRouteView(cir);
    }else if(ae.getActionCommand().equals("showTPFDD")){
      FilterClauses filterClauses = new FilterClauses ();
      if(unitDBUID!=null)
	filterClauses.addUnitDBUID (unitDBUID);
      boolean useFilter=false;
      if(curNode!=null){
	int type=curNode.getType();
	switch(type){
	case UIDGenerator.CARRIER_PROTOTYPE:
	  filterClauses.addCarrierType(taskModel.getDBUID(curNode));
	  useFilter=true;
	  break;
	case UIDGenerator.CARRIER_INSTANCE:
	  filterClauses.addCarrierInstance(taskModel.getDBUID(curNode));
	  useFilter=true;
	  break;
	case UIDGenerator.ASSET_PROTOTYPE:
    if(taskModel instanceof AssetModel) {
	    //This is an awful hack because the AssetModel uses the wrong type.
	    filterClauses.addCarrierType(taskModel.getDBUID(curNode));
    } else {
	    filterClauses.addCargoType(taskModel.getDBUID(curNode));
    }
	  useFilter=true;
	  break;
	case UIDGenerator.ASSET_INSTANCE:
	  filterClauses.addCargoInstance(taskModel.getDBUID(curNode));
	  useFilter=true;
	  break;
	}
      }
      shell.showTPFDDView(filterClauses, useFilter, false);
    }else if(ae.getActionCommand().equals("showCarrierTPFDD")){
      if(curNode!=null){
	FilterClauses filterClauses = new FilterClauses ();
	if(completeAssetFilterClause(filterClauses, curNode)){
	  shell.showTPFDDView (filterClauses, false, true);
	}
      }
    }else if(ae.getActionCommand().equals("displayDetails")){
      if(curNode!=null){
	String dbuid=taskModel.getDBUID(curNode);
	if(curNode.getType()==UIDGenerator.LEG){
	  AssetDetailRequest adr = new LegAssetDetailRequest(dbuid);
	  shell.showAssetDetailView(adr);
	}else if(curNode.getType()==UIDGenerator.ASSET_INSTANCE){
	  AssetDetailRequest adr = new AssetAssetDetailRequest(dbuid);
	  shell.showAssetDetailView(adr);
	}else if(curNode.getType()==UIDGenerator.CARRIER_INSTANCE){
	  CarrierDetailRequest cdr=new CarrierDetailRequest(dbuid);
	  shell.showCarrierDetailView(cdr);
	}
      }
    }else{
      System.out.println ("NewJTreeTable.actionPerformed - unknown command");
    }
  }

  protected void makeFilterDialogVisible (/*String nodeDBUID*/) {
    //	filterDialog.setUnitNode (nodeDBUID);
	filterDialog.setVisible (true);
  }

  protected void showFilterDialog () {
	if (filterDialog != null) {// if you do this *right* after it starts up
	  filterDialog.setVisible (true);
	}
  }

  public void resetFilterDialog () {
	if (filterDialog != null)
	  filterDialog.setVisible(false);
	if (debug)
	  System.out.println ("NewJTreeTable.resetFilterDialog - run is " + dbState.getRun ());
	
	filterDialog = new FilterDialog (shell, getFont());
  }
  
    public Runnable getFireChangedRunnable()
    {
	return fireChangedRunnable;
    }

    /* Workaround for BasicTableUI anomaly. Make sure the UI never tries to 
     * paint the editor. The UI currently uses different techniques to 
     * paint the renderers and editors and overriding setBounds() below 
     * is not the right thing to do for an editor. Returning -1 for the 
     * editing row in this case, ensures the editor is never painted. 
     */
    public int getEditingRow()
    {
	return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;  
    }

    // 
    // The editor used to interact with tree nodes, a JTree.  
    //
    public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor
    {
	public Component getTableCellEditorComponent(JTable table, Object value,
						     boolean isSelected, int r, int c)
	{
	    return tree;
	}
    }

    private boolean completeAssetFilterClause(FilterClauses filter, 
					      DBUIDNode node) {
      if(taskModel instanceof AssetModel){
	if (node.getType() == UIDGenerator.CATEGORY) {
	    switch (node.getMode()) {
	    case Node.MODE_AIR:
		filter.addAssetType(CategoryNode.AIR);
		return true;
	    case Node.MODE_SEA:
		filter.addAssetType(CategoryNode.SEA);
		return true;
	    case Node.MODE_GROUND:
		filter.addAssetType(CategoryNode.GROUND);
		return true;
	    case Node.MODE_SELF:
		filter.addAssetType(CategoryNode.SELF);
		return true;
	    case Node.MODE_UNKNOWN:
	    default:
		return false;
	    }
	} else if (node.getType() == UIDGenerator.ASSET_PROTOTYPE) {
	    filter.addCarrierType(((AssetPrototypeNode)node).getAssetPrototypeName());
	    return true;
	} else if (node.getType() == UIDGenerator.LOCATION) {
	    filter.addLocation(((LocationNode)node).getLocationName());
	    return true;
	} else if (node.getType() == UIDGenerator.CARRIER_INSTANCE) {
	    filter.addCarrierInstance(((CarrierInstance)node).getCarrierName());
	    return true;
	} else if (node.getType() == UIDGenerator.CONVOY) {
	  filter.addConvoy(((ConvoyNode)node).getConvoyID());
	  return true;
	}
      }else{
	//This is a hack, as the Asset view apperently uses ASSET_PROTOTYPE
	//For CarrierPrototypes, as seen earlier in this function.
	if(node.getType() == UIDGenerator.CARRIER_INSTANCE){
	  filter.addCarrierInstance(taskModel.getDBUID(node));
	  return true;
	}else if(node.getType()==UIDGenerator.CARRIER_PROTOTYPE){
	  filter.addCarrierType(taskModel.getDBUID(node));
	  return true;
	}
      }
      return false;
    }

  public NewTPFDDShell getShell() {
    return shell;
  }

  //Inner Classes:
  
  public static class AwareMenuItem extends JMenuItem{
    private Node node;
    private String unitDBUID;
    private String unitName;

    public AwareMenuItem(){
    }

    public void setNode(Node node){
      this.node=node;
    }
    public Node getNode(){
      return node;
    }
    public void setUnitName(String unitName){
      this.unitName=unitName;
    }
    /**Note: this may be null**/
    public String getUnitName(){
      return unitName;
    }
    public void setUnitDBUID(String unitDBUID){
      this.unitDBUID=unitDBUID;
    }
    /**Note: this may be null**/
    public String getUnitDBUID(){
      return unitDBUID;
    }
  }
}

