/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.Query;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

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
			TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitTreeModel.getChildCount - found equipment node " + child);
		  }
		}
		if (debug)
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitTreeModel.getChildCount - " + parent +
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
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TaskModel.doInitialQuery - Run is " + dbState.getRun());
  }
}


