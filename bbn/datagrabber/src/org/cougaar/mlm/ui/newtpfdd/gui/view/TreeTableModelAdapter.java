/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/Attic/TreeTableModelAdapter.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
 * %W% %E%
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

import javax.swing.table.AbstractTableModel;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;

/**
 * This is a wrapper class takes a TreeTableModel and implements 
 * the table model interface. The implementation is trivial, with 
 * all of the event dispatching support provided by the superclass: 
 * the AbstractTableModel. 
 *
 * @version %I% %G%
 *
 * @author Philip Milne
 * @author Scott Violet
 */


public class TreeTableModelAdapter extends AbstractTableModel
{
    JTree tree;
    TreeTableModel treeTableModel;

    public TreeTableModelAdapter(TreeTableModel treeTableModel, JTree tree) { 
        this.tree = tree;
        this.treeTableModel = treeTableModel;

	tree.addTreeExpansionListener(new TreeExpansionListener() {
	    // Don't use fireTableRowsInserted() here; 
	    // the selection model would get  updated twice. 
	    public void treeExpanded(TreeExpansionEvent event) {  
	      fireTableDataChanged(); 
	    }
            public void treeCollapsed(TreeExpansionEvent event) {  
	      fireTableDataChanged(); 
	    }
	});

	treeTableModel.addTreeModelListener (new TreeModelListener () {
	    public void treeNodesChanged(TreeModelEvent event) {
	      fireTableDataChanged(); 
		}
	    public void treeNodesInserted(TreeModelEvent event) {
	      fireTableDataChanged(); 
		}
	    public void treeNodesRemoved(TreeModelEvent event) {
	      fireTableDataChanged(); 
		}
	    public void treeStructureChanged(TreeModelEvent event) {
	      fireTableDataChanged(); 
		}
	  });
	
    }

    // to get the underlying tree table model
    public TreeTableModel getTreeTableModel() {
	return treeTableModel;
    }

    public void setTreeTableModel(TreeTableModel m) {
      treeTableModel = m;
    }

  // Wrappers, implementing TableModel interface. 

    public int getColumnCount() {
	return treeTableModel.getColumnCount();
    }

    public String getColumnName(int column) {
	return treeTableModel.getColumnName(column);
    }

    public Class getColumnClass(int column) {
	return treeTableModel.getColumnClass(column);
    }

    public int getRowCount() {
	return tree.getRowCount();
    }

    protected Object nodeForRow(int row) {
	TreePath treePath = tree.getPathForRow(row);
	Node node = (Node) treePath.getLastPathComponent();         
	//	System.out.println ("nodeForRow - row " + row + " -> node " + node);
	
	return node;
    }

    public Object getValueAt(int row, int column) {
	return treeTableModel.getValueAt(nodeForRow(row), column);
    }

    public boolean isCellEditable(int row, int column) {
         return treeTableModel.isCellEditable(nodeForRow(row), column); 
    }

    public void setValueAt(Object value, int row, int column) {
	  treeTableModel.setValueAt(value, nodeForRow(row), column);
	  fireTableRowsUpdated(row,row);
    }
}
