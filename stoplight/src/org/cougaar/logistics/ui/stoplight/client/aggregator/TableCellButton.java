/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.client.aggregator;

import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * Used to render a clickable button in a table cell.  Create seperate
 * instances of this component for the TableCellRenderer and the
 * TableCellEditor.
 */
public class TableCellButton extends JPanel
  implements TableCellRenderer, TableCellEditor
{
  private JButton               button                = null;
  private ButtonPressedListener actionListener        = null;
  private Vector                listeners             = new Vector();
  private Vector                buttonEditorListeners = new Vector();

  public void updateUI()
  {
    super.updateUI();

    if (button != null)
      button.updateUI();
  }

  public TableCellButton(String buttonLabel)
  {
    super();
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(Box.createGlue());
    button = new JButton(buttonLabel);
    button.setHorizontalAlignment(JButton.CENTER);
    button.setVerticalAlignment(JButton.CENTER);
    button.setMargin(new Insets(0, 0, 0, 0));
    add(button);
    add(Box.createGlue());

    button.addActionListener(actionListener = new ButtonPressedListener());
  }

  public Component
    getTableCellRendererComponent(JTable table, Object value,
                                  boolean isSelected, boolean hasFocus,
                                  int row, int column) {
    colorRenderer(this, isSelected, table, row, column);
    return this;
  }

  public static interface ButtonEditorListener
  {
    public void buttonPressed(JTable table, int row);
  }

  public void addButtonEditorListener(ButtonEditorListener bel)
  {
    buttonEditorListeners.addElement(bel);
  }

  public void removeButtonEditorListener(ButtonEditorListener bel)
  {
    buttonEditorListeners.removeElement(bel);
  }

  public Component
    getTableCellEditorComponent(final JTable table, Object value,
                                boolean isSelected,
                                final int row, int column) {
    actionListener.setRow(row);
    actionListener.setTable(table);
    colorRenderer(this, isSelected, table, row, column);
    return this;
  }

  public void addCellEditorListener(CellEditorListener l)
  {
    listeners.addElement(l);
  }

  public void removeCellEditorListener(CellEditorListener l)
  {
    listeners.remove(l);
  }

  public boolean stopCellEditing()
  {
    for (int i = 0; i < listeners.size(); i++)
    {
      CellEditorListener cel = (CellEditorListener)listeners.elementAt(i);
      cel.editingStopped(new ChangeEvent(this));
    }
    return true;
  }

  protected void colorRenderer(Component comp, boolean isSelected,
                               JTable table, int row, int column)
  {
    if (isSelected)
    {
      comp.setForeground(table.getSelectionForeground());
      comp.setBackground(table.getSelectionBackground());
    }
    else
    {
      comp.setForeground(table.getForeground());
      comp.setBackground(table.getBackground());
    }
  }

  public void cancelCellEditing(){}
  public Object getCellEditorValue(){ return new Boolean(false);}
  public boolean isCellEditable(EventObject anEvent){ return true; }
  public boolean shouldSelectCell(EventObject anEvent) { return false; }

  private class ButtonPressedListener implements ActionListener
  {
    private int row = 0;
    private JTable table = null;

    public void setRow(int row)
    {
      this.row = row;
    }

    public void setTable(JTable table)
    {
      this.table = table;
    }

    public void actionPerformed(ActionEvent event)
    {
      for (int i = 0; i < buttonEditorListeners.size(); i++)
      {
        ((ButtonEditorListener)buttonEditorListeners.
          elementAt(i)).buttonPressed(table, row);
      }
      stopCellEditing();
    }
  }
}