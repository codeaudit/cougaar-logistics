/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/view/TreeTableCellRenderer.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
   Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
   Reserved.
  
   This material has been developed pursuant to the BBN/RTI "ALPINE"
   Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
   Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

   @author Sundar Narasimhan, Daniel Bromberg
*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import java.awt.Font;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;


import org.cougaar.mlm.ui.newtpfdd.util.Debug;


public class TreeTableCellRenderer extends JTree implements TableCellRenderer 
{
    protected int visibleRow;
    NewJTreeTable treeTable = null;

    public TreeTableCellRenderer(TreeModel model, NewJTreeTable treeTab, Font myFont)
    { 
	super(model);

	expandRow(0);
	treeTable = treeTab;

	// set default properties
	setForeground(Color.white);
	setBackground(Color.gray);
	//	setRootVisible(false);
	setShowsRootHandles(true);
	
	setCellRenderer(new TPFDDTreeCellRenderer(myFont));

	// Check if this is the right way to do it.
	// I had to do this to make programmatic adds to trees to work.
	// Almost wasted a day before I figured out that nodes* weren't
	// really being handled -- tree structure changes seem to work
	// right.
	/*
	model.addTreeModelListener(new TreeModelListener() {
	    public void treeNodesChanged(TreeModelEvent e) { }
	    public void treeNodesInserted(TreeModelEvent e) { }
	    public void treeNodesRemoved(TreeModelEvent e) { }
	    public void treeStructureChanged(TreeModelEvent e) {
		TreePath p = new TreePath(e.getPath());
		int val = getRowForPath(p);
		setVisibleRowCount(val);
		//		System.out.println ("TreeTableCellRenderer.treeStructureChanged called");
		
	    }
	});
	*/
    }

    public void setBounds(int x, int y, int w, int h)
    {
	if ( treeTable == null )
	    return;
	super.setBounds(x, 0, w, treeTable.getHeight());
    }

    public void setExpanded(TreePath path, boolean state)
    {
	super.setExpandedState(path, state);
    }

    public void paint(Graphics g)
    {
	g.translate(0, -visibleRow * getRowHeight());
	super.paint(g);
    }

    public Component getTableCellRendererComponent(JTable table,
						   Object value,
						   boolean isSelected,
						   boolean hasFocus,
						   int row, int column)
    {
	if ( isSelected )
	    setBackground(table.getSelectionBackground());
	else
	    setBackground(table.getBackground());
       
	visibleRow = row;
	return this;
    }
}
