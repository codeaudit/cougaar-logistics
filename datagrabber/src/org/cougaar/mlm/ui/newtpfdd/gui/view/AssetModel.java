/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import java.lang.reflect.InvocationTargetException;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import java.awt.Cursor;
import java.awt.Container;
import javax.swing.SwingUtilities;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;

import org.cougaar.mlm.ui.newtpfdd.producer.ClusterCache;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.ByCarrier;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CategoryNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.AssetPrototypeNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.Query;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.QueryHandler;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;

import org.cougaar.mlm.ui.newtpfdd.gui.model.RowModelListener;

public class AssetModel extends TaskModel implements TreeTableModel {
    protected Tree myTree;
    protected DatabaseState dbState;
    protected QueryHandler queryHandler = new QueryHandler();
    
  boolean debug = "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.AssetModel.debug", "false"));
    /** The active worker. */
    private transient TPFDDSwingWorker worker;

    static protected String[] AMcolumnNames = {"Name", "During"}; 
    static protected Class[]  AMcolumnTypes = { TreeTableModel.class, Node.class };
    

    public AssetModel (DatabaseState dbState) {
	super (dbState);


	this.dbState = dbState;
	myTree = new Tree ();

	// Sets dummy node as temporary root
	Org tempRoot = (Org) getRoot ();
	myTree.setRoot(tempRoot);
    }

//     public Node getOrgParent (long nodeUID) {
// 	Node parent = myTree.getNode (nodeUID);
// 	while (!(parent instanceof Org) && parent != myTree.getRoot())
// 	    parent = myTree.getNode (parent.getParentUID());
// 	return parent;
//     }
  
    public String getDBUID (Node node) {
	return myTree.getDBUID(node.getUID());
    }
  
    public long getMinTaskStart () {
	return myTree.getMinTaskStart();
    }

    public long getMaxTaskEnd () {
	return myTree.getMaxTaskEnd();
    }

    public void doInitialQuery () {
	Query assetCategoryQuery = queryHandler.createAssetCategoryQuery(dbState);

	Set forest = queryHandler.performQuery(dbState.getDBConfig(), assetCategoryQuery);
	myTree = (Tree) forest.iterator().next();

	setRoot (myTree.getRoot());
	
	notifyListenersTreeChanged ();

	if (debug)
	    System.out.println ("AssetModel.doInitialQuery - Run is " + dbState.getRun());
    }

    protected void notifyListenersTreeChanged () {
	Runnable fireTreeChangedRunnable = new Runnable() {
	    public void run() {
		Object [] path = new Object [] { getRoot () };
		fireTreeStructureChanged(this, path, null, null);
	    }
	};
	SwingUtilities.invokeLater(fireTreeChangedRunnable);
    }
    
    /**
     * Given an expanded node, starts a SwingWorker
     * to create children for the expanded node and insert them into
     * the tree. 
     */
  public void expandNode (final Object parent) {
	if (debug)
	  System.out.println ("AssetModel.expandNode - expanding node " + parent);
	
	final Node parentNode = (Node) parent;

	if (!(parentNode instanceof AssetPrototypeNode)) return;
	if (!parentNode.hasChildren()) {
	  worker = new AssetInstanceQueryWorker(parentNode, myTree);
	  worker.startWorker ();
	} else if (debug)
	  System.out.println ("AssetModel.expandNode - parent node has children already, skipping.");
  }
    
    public void showTPFDDLines(FilterClauses filterClauses, RowModelListener ganttChart, Container dialog, WorkListener listener) {
	  worker = new AssetTPFDDQueryWorker (filterClauses, myTree, ganttChart, dialog, listener);
	  worker.startWorker ();
    }
    
    public void showFilterTPFDDLines(FilterClauses filterClauses, RowModelListener ganttChart, Container dialog, WorkListener listener) {
	  worker = new FilterQueryWorker (filterClauses, myTree, ganttChart, dialog, listener);
	  worker.startWorker ();
    }
    
    private static Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
    private static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    
    class AssetInstanceQueryWorker extends TPFDDSwingWorker {
	Set forest = new HashSet();
	Node parentNode;
	Tree tree;
	
	public AssetInstanceQueryWorker (Node parentNode, Tree tree) {
	    this.parentNode = parentNode;
	    this.tree = tree;
	    if (debug)
		System.out.println ("AssetModel.AssetInstanceQueryWorker - ctor.");
	}
	
	public void construct(){
	    /* Create children for the expanded node. */
	    if (debug) {
		System.out.println("AssetModel.AssetInstanceQueryWorker - this is " + this);
		System.out.println("AssetModel.doAssetInstanceQuery - parentNode " + parentNode);
		System.out.println("AssetModel.doAssetInstanceQuery - forest " + forest);
	    }
		doAssetInstanceQuery(parentNode, forest);
	}
	
	public void finished() {
	    /* Set the worker to null,
	       but only if we are the active worker. */
	    if (worker == this) {
		worker = null;
	    }
	    handleResults ();
	}
	
	protected void handleResults () {
	  // add children to node
	  Tree returnedTree = (Tree) forest.iterator().next();
	  for (int i = 0; i < returnedTree.getChildCount (returnedTree.getRoot()); i++) {
		Node child = returnedTree.getChild (returnedTree.getRoot(), i);
		myTree.graftOnChild (parentNode, child, returnedTree);
	  }
	  
	    Node unitParent = myTree.getNode (parentNode.getParentUID());
	    tree.doRollup (unitParent);
	    myTree.doRollup(myTree.getRoot());
	    unitParent.setWasQueried (true);
	    parentNode.setWasQueried (true);
	    if (debug) {
		System.out.println ("AssetModel.SwingWorker - parent <" + parentNode +
				    "> now has " + parentNode.getChildCount () +
				    " children");
		System.out.println ("TaskMode.SwingWorker - tree below parent " + parentNode + " : ");
		// tree.showNode (parentNode, "");
	    }
	    nodeStructureChanged(parentNode);
	    if (debug)
		System.out.println("Expanded "+parentNode);
	}
    };
    
  class CarrierQueryWorker extends UnitQueryWorker {
	public CarrierQueryWorker (Node parentNode, Tree tree, Container panel) {
	  super (parentNode, tree, panel);
	}

	public void construct(){
	  /* Create children for the expanded node. */
	  if (debug) System.out.println("AssetModel.startWorker - doing CarrierQuery.");
	  panel.setCursor (waitCursor);
	  doCarrierQuery (parentNode, forest);
	}
  }

  class AssetTPFDDQueryWorker extends UnitQueryWorker {
	//	String unitDBUID;
	FilterClauses filterClauses;
	RowModelListener ganttChart;
	
	public AssetTPFDDQueryWorker (FilterClauses filterClauses, Tree tree, RowModelListener ganttChart, Container dialog, WorkListener listener) {
	  super (null, tree, dialog, listener);
	  //	  this.unitDBUID = unitDBUID;
	  this.filterClauses = filterClauses;
	  this.ganttChart = ganttChart;
	}

	public void construct(){
	  /* Create children for the expanded node. */
	  if (debug) System.out.println("AssetModel.TPFDDQueryWorker - doing TPFDDQuery.");
	  panel.setCursor (waitCursor);
	  doAssetTPFDDQuery (filterClauses, forest);
	}

	protected void handleResults () {
	  // my tree is the result
	  myTree = (Tree) forest.iterator ().next();
	  setRoot (myTree.getRoot ());
	  myTree.doRollup (myTree.getRoot ());

	  for (int i = 0; i < myTree.getChildCount (myTree.getRoot()); i++)
		ganttChart.fireRowAdded (i); // evil
	  ganttChart.firingComplete ();

	  panel.setCursor (defaultCursor);
	}
  }
	
  class FilterQueryWorker extends TPFDDQueryWorker {
	public FilterQueryWorker (FilterClauses filterClauses, Tree tree, RowModelListener ganttChart, Container dialog, WorkListener listener) {
	  super (filterClauses, tree, ganttChart, dialog);
	}
	public void construct(){
	  // Create children for the expanded node.
	  if (debug) System.out.println("AssetModel.FilterQueryWorker - doing FilterQuery.");
	  panel.setCursor (waitCursor);
	  doFilterQuery (filterClauses, forest);
	}
  }
  
  /** Stops the active worker, if any. */

  public void stopWorker() {
	if (debug)
	  System.out.println("AssetModel.stopWorker - Stopping worker...");
	if (worker != null) {
	  worker.interrupt();
	  // worker set to null in finished
	}
  }

  protected void doUnitQuery (Node parentNode, Set forest) {
	if (debug)
	  System.out.println("AssetModel.doUnitQuery - parentNode " + parentNode);
	if (debug)
	  System.out.println("AssetModel.doUnitQuery - forest " + forest);
	String DBUID = myTree.getDBUID (parentNode.getUID());
	
	if (debug)
	  System.out.println("AssetModel.doUnitQuery - unit is " + DBUID);
	
	FilterClauses filterClauses = new FilterClauses ();
	filterClauses.addUnitDBUID (DBUID);
	Query unitQuery = queryHandler.createUnitQuery (dbState, filterClauses);

	forest.addAll (queryHandler.performQuery (dbState.getDBConfig(), unitQuery));
  }

  protected void doCarrierQuery (Node parentNode, Set forest) {
	String DBUID = myTree.getDBUID (myTree.getNode(parentNode.getUID()).getParentUID());
	
	if (debug)
	  System.out.println("AssetModel.doCarrierQuery - unit is " + DBUID);
	FilterClauses filterClauses = new FilterClauses ();
	filterClauses.addUnitDBUID (DBUID);
	Query carrierQuery = queryHandler.createCarrierQuery (dbState, filterClauses);

	forest.addAll (queryHandler.performQuery (dbState.getDBConfig(), carrierQuery));
  }

  protected void doAssetTPFDDQuery (FilterClauses filterClauses, Set forest) {
	if (debug)
	  System.out.println("AssetModel.doTPFDDQuery - TPFDD query is " + filterClauses + " this " + this + ", dbState " + dbState.getRun());

	Query tpfddQuery = queryHandler.createAssetTPFDDQuery (dbState, filterClauses);

	forest.addAll (queryHandler.performQuery (dbState.getDBConfig(), tpfddQuery));
  }

  protected void doFilterQuery (FilterClauses filterClauses, Set forest) {
	if (debug)
	  System.out.println("AssetModel.doFilterQuery - filter query is " + filterClauses + " dbState " + dbState.getRun());

	Query tpfddQuery = queryHandler.createFilterQuery (dbState, filterClauses);

	forest.addAll (queryHandler.performQuery (dbState.getDBConfig(), tpfddQuery));
  }

  protected void doAssetInstanceQuery (Node parentNode, Set forest) {
	if (debug)
	  System.out.println("AssetModel.doAssetInstanceQuery - parentNode " + parentNode);
	if (debug)
	  System.out.println("AssetModel.doAssetInstanceQuery - forest " + forest);
	String DBUID = myTree.getDBUID (parentNode.getUID());
	
	if (debug)
	  System.out.println("AssetModel.doUnitQuery - unit is " + DBUID);
	
	FilterClauses filterClauses = new FilterClauses ();
	filterClauses.addCarrierType (((AssetPrototypeNode)parentNode).getAssetPrototypeName());
	Query assetInstanceQuery = queryHandler.createAssetInstanceQuery (dbState, filterClauses);

	forest.addAll (queryHandler.performQuery (dbState.getDBConfig(), assetInstanceQuery));
  }

  protected void nodeStructureChanged (Node parent) {
	final Object path[] = getPathToNode(parent);
	final int indices[] = new int[parent.getChildCount()];
	final Object children[] = new Object[indices.length];
	for (int i = 0; i < parent.getChildCount (); i++) {
	  indices[i] = i;//taskModel.getIndexOfChild(parent, parent.getChildID(i));
	  children[i] = myTree.getNode (parent.getChildUID (i));
	}
	Runnable fireTreeChangedRunnable = new Runnable() {
	    public void run() {
	      fireTreeStructureChanged(this, path, indices, children);
	    }
	  };
	SwingUtilities.invokeLater(fireTreeChangedRunnable);
  }
	
    /**
     * Builds the parents of node up to and including the root node,
     * where the original node is the last element in the returned array.
     * The length of the returned array gives the node's depth in the
     * tree.
     * 
     * @param aNode  the TreeNode to get the path for
     * @param depth  an int giving the number of steps already taken towards
     *        the root (on recursive calls), used to size the returned array
     * @return an array of TreeNodes giving the path from the root to the
     *         specified node 
     */
    // public methods to manipulate roots
    public Object[] getPathToNode(Object node)
    {
	  return getPathToNode(node, 0);
    }

    private Object[] getPathToNode(Object aNode, int depth)
    {
        Object[] retNodes;
	// This method recurses, traversing towards the root in order
	// size the array. On the way back, it fills in the nodes,
	// starting from the root and working back to the original node.


		//	  System.out.println ("getPathToNode (2) - " + aNode + " depth " + depth);
        /* Check for null, in case someone passed in a null node, or
           they passed in an element that isn't rooted at root. */
        if ( aNode == null ) {
            if ( depth == 0 )
                return null;
            else
                retNodes = new Object[depth];
        }
        else {
            depth++;
            if ( aNode == root )
                retNodes = new Object[depth];
            else {
			  Node parentNode = myTree.getNode(((Node)aNode).getParentUID());
			  retNodes = getPathToNode(parentNode, depth);
			}
			
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    // For ScheduleCellRenderer
    public Tree getTree()
    {
	  return myTree;
    }

    //
    // The TreeModel interface
    //
    public int getChildCount(Object parent)
    {
	  return myTree.getChildCount((Node)parent);
    }

    public Object getChild(Object node, int index)
    {
	  return myTree.getChild((Node)node, index);
    }

  public Object getParent(Object child) 
  {
	return myTree.getNode(((Node)child).getParentUID());
  }
  
    // This is officially part of the model but not used in JTree's default
    // mode. However, it's useful for our own code for calculating the update
    // event messages.
    public int getIndexOfChild(Object parent, Object child)
    {
	return myTree.getIndexOfChild((Node)parent, (Node)child);
    }

    // The superclass's implementation would work, but this is more efficient. 
    public boolean isLeaf(Object node)
    { 
	if ( node == root )
	    return false;
	
	return ((Node)node).isLeaf ();
    }

    //
    //  The TreeTableNode interface. 
    //
    public int getColumnCount()
    {
	return AMcolumnNames.length;
    }

    public String getColumnName(int column)
    {
	if ( column < 0 || column > 3 ) {
	    OutputHandler.out("Error: getColumnName() received invalid column: " + column);
	    return null;
	}
	return AMcolumnNames[column];
    }

    public Class getColumnClass(int column)
    {
	if ( column < 0 || column > 3 ) {
	    OutputHandler.out("Error: getColumnClass() received invalid column: " + column);
	    return null;
	}
	return AMcolumnTypes[column];
    }

    public Object getValueAt(Object item, int column)
    {
	if ( !(item instanceof Node) ) {
	    OutputHandler.out("Error: getValueAt() received item of class " + item.getClass()
			       + ", expecting Node");
	    return null;
	}
	Node node = (Node)item;



	if ( column == 1 )
	    return item;

	if ( node == root ) {
	    if ( column == 0 )
		return "ALP Tasks";
	    if ( column > 0 && column <= 4 )
		return "";
	}

	switch ( column ) {
	    case 0:
		return node.getDisplayName();
		//	    case 1:
		  //		if (node instanceof TypeNode)
		  //		    return ((TypeNode)node).getCargoName();
		//		  node.getDisplayName ();
// 	    case 1:
// 		  //		if ( node instanceof ItineraryNode )
// 		  //		    return node.getChild(0).getFromName();
// 		  return node.getFromName();
// 	    case 2:
// 		  //		if ( node instanceof ItineraryNode )
// 		  //		    return node.getChild(node.getChildCount() - 1).getToName();
// 		  return node.getToName();
	    case 1:
		return node;
	}
   
	OutputHandler.out("Error: getValueAt() received invalid column: " + column);
	return null;
    }
}


