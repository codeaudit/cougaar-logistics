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

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.Query;

import java.util.Set;

public class UnitTreeModel extends TaskModel {
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.UnitTreeModel.debug", 
									   "false"));

  // what about super call to set root???
  public UnitTreeModel (DatabaseState dbState)
  {
	super (dbState);
  }

    //
    // The TreeModel interface
    //
    public int getChildCount(Object parent)
    {
	  if (parent instanceof Org) {
		Org org = (Org) parent;
		int total = 0;
		for (int i = 0; i < org.getChildCount (); i++) {
		  DBUIDNode child = (DBUIDNode) myTree.getChild(org, i);
		  if (child.getType () == UIDGenerator.ORGANIZATION) { 
			total++;
		  } else if (debug) {
			System.out.println ("UnitTreeModel.getChildCount - found equipment node " + child);
		  }
		}
		if (debug)
		  System.out.println ("UnitTreeModel.getChildCount - " + parent + 
							  " has " + total + " children");
		
		return total;
	  }
	  
	  return myTree.getChildCount((Node)parent);
    }

    public Object getChild(Object node, int index) {
	  Org org = (Org) node;
	  int total = 0;
	  for (int i = 0; i < org.getChildCount (); i++) {
		DBUIDNode child = (DBUIDNode) myTree.getChild(org, i);
		if (child.getType () == UIDGenerator.ORGANIZATION) {
		  if (total == index)
			return child;
		  total++;
		}
	  }
	  return null;
    }

    // only show down to orgs -- not carrier/cargo stuff below
    public boolean isLeaf(Object node) { 
	  if ( node == root )
	    return false;
	  boolean leaf = !((Node)node).hasChildren ();

	  if (leaf) {
		return leaf;
	  } else if (node instanceof Org) {
		// if my child is not an org, I'm a leaf
		//		return !(myTree.getChild(org, 0) instanceof Org);
		boolean retval = (getChildCount (node) == 0);
		return retval;
	  }
	
	  return leaf;
    }

  public void doInitialQuery () {
    Query hierarchyQuery = queryHandler.createLightHierarchyQuery (dbState);
    
    // should be in a separate thread...?
    
    Set forest = queryHandler.performQuery (dbState.getDBConfig(), hierarchyQuery);
    
    // expect only one tree
    Tree tree = (Tree) forest.iterator().next();
    
    myTree = tree;
    
    setRoot (myTree.getRoot ());
    
    notifyListenersTreeChanged ();
    
    if (debug)
      System.out.println ("TaskModel.doInitialQuery - Run is " + dbState.getRun());	
  }
}


