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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.table.TableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;

public class NewLogPlanView extends JPanel
{
    private NewJTreeTable treeTable;
  private TreeTableCellRenderer tree; // an instance of JTree
    private TaskModel model;

   public NewLogPlanView(NewTPFDDShell shell, int fontSize)
    {
	model = new TaskModel(shell);
	Font font = new Font("SansSerif", Font.PLAIN, fontSize);
	treeTable = new NewJTreeTable(shell, model, font);
	treeTable.setShowGrid(true);
	treeTable.setForeground(Color.white);
	treeTable.setBackground(Color.gray);
	treeTable.setSelectionForeground(Color.white);
	treeTable.setSelectionBackground(TPFDDColor.TPFDDBurgundy);

	treeTable.getTableHeader().setForeground(Color.white);
	treeTable.getTableHeader().setBackground(TPFDDColor.TPFDDBlue);
	ToolTipManager.sharedInstance().registerComponent(treeTable);

	TableCellRenderer renderer = treeTable.getDefaultRenderer(TreeTableModel.class);
	tree = (TreeTableCellRenderer)renderer;
	tree.setForeground(Color.white);
	tree.setBackground(Color.gray);
	//	tree.setRootVisible(false);
	tree.setShowsRootHandles(true);
	setBackground(Color.gray);
	setLayout(new BorderLayout());
	JScrollPane pane = new JScrollPane(treeTable);
	pane.setBackground(Color.gray);
	add(pane, BorderLayout.CENTER);
	setVisible(true);
	setupExpansionListener ();
    }

  public void doInitialQuery () {
	treeTable.resetFilterDialog();
	model.doInitialQuery ();
  }

  public void showFilterDialog () {
	treeTable.showFilterDialog ();
  }

  public void resetFilterDialog () {
	treeTable.resetFilterDialog ();
  }
  
  protected void setupExpansionListener () {
	tree.addTreeExpansionListener (new TreeExpansionListener () {
		public void treeCollapsed (TreeExpansionEvent evt) {
		  jTreeTreeCollapsed (evt);
		}
		public void treeExpanded (TreeExpansionEvent evt) {
		  jTreeTreeExpanded (evt);
		}
	  }
									);
	tree.addTreeSelectionListener (new TreeSelectionListener () {
		public void valueChanged (TreeSelectionEvent evt) {
		  jTreeValueChanged (evt);
		}
	  }
										);
  }
  
  /**
     * Called when a node is expanded. Stops the active worker, if any,
     * and starts a new worker to create children for the expanded node. 
     */
  private void jTreeTreeExpanded (TreeExpansionEvent evt) {
	model.stopWorker();
	Object node = (Object) evt.getPath().getLastPathComponent();
	model.expandNode(node, this);
  }

  /**
     * Called when a node is collapsed. Stops the active worker, if any,
     * and removes all the children. 
     */
  private void jTreeTreeCollapsed (TreeExpansionEvent evt) {
	model.stopWorker();
	Object node = (Object) evt.getPath().getLastPathComponent();
  }

  /** Updates the status line when a node is selected. */
  private void jTreeValueChanged (TreeSelectionEvent evt) {
	Object node = evt.getPath().getLastPathComponent();
  }

}


