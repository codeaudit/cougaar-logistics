/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JPanel;

import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;

import javax.swing.event.TreeSelectionListener;

import java.awt.Color;
import java.awt.BorderLayout;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDTreeCellRenderer;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;

import java.awt.Component;
import java.awt.Font;

public class UnitPanel extends JPanel
{
    private JScrollPane scroll;
    private JTree tree;
    private TreeCellRenderer renderer;
    private TreeSelectionListener parent;

  protected TreeModel unitTreeModel;

  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.UnitPanel.debug", 
									   "false"));

  Font myFont;
  
    private JScrollPane getscroll()
    {
	if ( scroll == null ) {
	    try {
		scroll = new JScrollPane(gettree());
		scroll.setBackground(Color.gray);
	    }
	    catch ( Exception e ) {
		handleException(e);
	    }
	}
	return scroll;
    }

    private JTree gettree()
    {
	if ( tree == null ) {
	    try {
		tree = new JTree(unitTreeModel);
		tree.setSelectionRow(0);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree.setForeground(Color.white);
		tree.setBackground(Color.gray);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(getRenderer());

		tree.expandRow (0);
	    }
	    catch ( Exception e ) {
		handleException(e);
	    }
	}
	return tree;
    }

  protected void expandAll (JTree tree) {
	boolean allExpanded = false;
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel - doing expand all - ");
	
	while (!allExpanded) {
	  int rowsDisplayedBefore = tree.getRowCount ();
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel - rows before " + rowsDisplayedBefore);
	  for (int i = 0; i < tree.getRowCount (); i++) {
		try {
		  tree.expandRow(i);
		} catch (Exception e) {
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel.expandAll - got exception " + e);
		}
	  }
	  
	  int rowsDisplayedAfter = tree.getRowCount ();
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel - rows after " + rowsDisplayedAfter);
	  if (rowsDisplayedBefore == rowsDisplayedAfter)
		allExpanded = true;
	}
  }
  
    private TreeCellRenderer getRenderer()
    {
	DefaultTreeCellRenderer internal;

	if ( renderer == null ) {
	    try {
		  internal = new DefaultTreeCellRenderer() {
			  public Component getTreeCellRendererComponent(JTree tree, Object value,
															boolean selected, boolean expanded,
															boolean leaf, int row,
															boolean hasFocus)
			  {
				Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded,
																	leaf, row, hasFocus);
				if ( value instanceof Node )
				  setText(((Node)value).getDisplayName());

				return comp;
			  }
			};
		  //		  internal = new TPFDDTreeCellRenderer(myFont);
		  
		internal.setLeafIcon(null);
		internal.setOpenIcon(null);
		internal.setClosedIcon(null);
		internal.setBackground(Color.gray);
		internal.setBackgroundSelectionColor(TPFDDColor.TPFDDBurgundy);
		internal.setBackgroundNonSelectionColor(Color.gray);
		renderer = internal;
	    }
	    catch ( Exception e ) {
		handleException(e);
	    }
	}
	return renderer;
    }

    public UnitPanel(DatabaseState dbState, TreeSelectionListener parent, Font myFont)
    {
	super();
	if (debug)	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel ctor -------------- ");
	this.parent = parent;
	this.unitTreeModel = new UnitTreeModel (dbState);
	if (debug)	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel ctor - doing initial query ");
	((UnitTreeModel)unitTreeModel).doInitialQuery ();
	if (debug) {
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel ctor - unitTreeModel tree : ");
	  ((UnitTreeModel)unitTreeModel).getTree().show();
	}

	setName("unitPanel");
	setLayout(new BorderLayout());
	add(getscroll(), BorderLayout.CENTER);

	gettree().addTreeSelectionListener(parent);

	refresh();
	}

  protected void refresh () {
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		  expandAll (tree);
		  gettree().setVisibleRowCount (gettree().getRowCount ());
		  if (debug)
			TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel.refreshing " + gettree().getRowCount() + " rows");
		  gettree().invalidate();
		  gettree().repaint();
		  getscroll().invalidate();
		  getscroll().repaint();
		}
	  });
  }
	    
    private void handleException(Exception e)
    {
	OutputHandler.out(ExceptionTools.toString("UD:hE", e));
    }

  /** turn this back on later */

  public void selectAllChildrenOfSelected() {
	// if no rows are selected, return -- how could this happen?
	int[] rows = gettree().getSelectionRows();
	if ( rows != null && rows.length == 0 )
	  return;

	// don't receive events while we're messing with the selections
	gettree().removeTreeSelectionListener(parent);

	gettree().addSelectionPaths (getPaths ());
	
	parent.valueChanged(null);

	// OK, we can receive events again
	gettree().addTreeSelectionListener(parent);
  }

  public TreePath [] getPaths () {
	List allPaths = new ArrayList();
	
	int[] rows = gettree().getSelectionRows();
	if ( rows != null && rows.length > 0 ) {
	  for ( int i = 0; i < rows.length; i++ ) {
		// path to selected node
		TreePath path = gettree().getPathForRow(rows[i]);
		List paths = new ArrayList ();
		// get all paths to children of selected node
		getPathsToDescendents (path, paths);
		// add descendant paths to total
		allPaths.addAll (paths);
	  }
	}
	TreePath [] result = new TreePath [allPaths.size()];
	return (TreePath []) allPaths.toArray (result);
  }
  
  protected void getPathsToDescendents (TreePath pathToSelectedUnit, List descendentPaths) {
	Node unitNode = (Node)pathToSelectedUnit.getLastPathComponent();

 	for (int i = 0; i < unitTreeModel.getChildCount (unitNode); i++) {
	  Node childOfUnit = (Node) unitTreeModel.getChild (unitNode, i);
	  TreePath pathToChild = pathToSelectedUnit.pathByAddingChild (childOfUnit);
	  descendentPaths.add (pathToChild);
	  // recurse on each child path
	  getPathsToDescendents (pathToChild, descendentPaths);
	}
  }
  
  public void getSelectedUnitNames(FilterClauses clauses) {
	//	String[] selectedUnits = null;
	int[] rows = gettree().getSelectionRows();
	if ( rows != null && rows.length > 0 ) {
	  //	    selectedUnits = new String[rows.length];
	  for ( int i = 0; i < rows.length; i++ ) {
		TreePath path = gettree().getPathForRow(rows[i]);
		//		  selectedUnits[i] = (Node)(path.getLastPathComponent());
		Node unitNode = (Node)path.getLastPathComponent();
		clauses.addUnitDBUID (((UnitTreeModel) unitTreeModel).getDBUID (unitNode));
	  }
	}
  }

  public List getSelectedUnitNodes() {
	List list = new ArrayList ();
	int[] rows = gettree().getSelectionRows();
	if ( rows != null && rows.length > 0 ) {
	  for ( int i = 0; i < rows.length; i++ ) {
		TreePath path = gettree().getPathForRow(rows[i]);
		Node unitNode = (Node)path.getLastPathComponent();
		list.add (unitNode);
	  }
	}
	return list;
  }

  public boolean anyUnitsSelected () {
	int [] rows = gettree().getSelectionRows();
	
	return (rows != null && rows.length > 0);
  }
  
  /*
    public void setUnitNode(String unitDBUID)
    {
	  //	  Debug.out("UP:sUN name " + name + " path " + getPathToNode(nodeDBUID));
	  //	  gettree().setSelectionPath(getPathToNode(unitDBUID));
    }
  */

  public TreePath getPathToNode(/*Node unitNode)*/String nodeDBUID)
  {
	int nodeType = UIDGenerator.ORGANIZATION;
	Object child = /*unitNode;*/((UnitTreeModel)unitTreeModel).getTree().getNodeDBUID (nodeType, nodeDBUID);
	if (child == null)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitPanel.getPathToNode - null node for " + nodeDBUID + "?");
	
	Vector chain = new Vector();
	Object chainWalk = child;
	while ( chainWalk != null ) {
	  chain.add(0, chainWalk);
	  chainWalk = ((UnitTreeModel)unitTreeModel).getParent((Node) chainWalk);
	}
	return new TreePath(chain.toArray());
  }
}
