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
import javax.swing.*;
import javax.swing.table.*;

/**
  * used to create table with color coded rows
  */
public class ColoredCellTable extends JTable
{
  public ColoredCellTable(TableModel tm)
  {
    super(tm);

    setDefaultRenderer(Object.class, new TextFieldRenderer());
    setDefaultEditor(Object.class, new TextFieldEditor());
    setDefaultRenderer(Boolean.class, new CheckBoxRenderer());
    setDefaultRenderer(JButton.class, new ColoredTableCellButton("Cancel"));
  }

  /**
   * Override this method to define how each cell should be colored
   */
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

  private class TextFieldRenderer extends DefaultTableCellRenderer
  {
    public Component
      getTableCellRendererComponent(JTable table, Object text,
                                    boolean isSelected, boolean hasFocus,
                                    int row, int column) {
      colorRenderer(this, isSelected, table, row, column);
      Component comp =
        super.getTableCellRendererComponent(table, text, isSelected,
                                            hasFocus, row, column);
      return comp;
    }
  }

  private class TextFieldEditor extends DefaultCellEditor
  {
    public TextFieldEditor()
    {
      super(new JTextField());
    }

    public Component
      getTableCellRendererComponent(JTable table, Object text,
                                    boolean isSelected, boolean hasFocus,
                                    int row, int column) {
      Component comp =
        super.getTableCellEditorComponent(table, text, isSelected,
                                          row, column);
      colorRenderer(comp, isSelected, table, row, column);
      return comp;
    }
  }

  private class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
  {
    public CheckBoxRenderer()
    {
      super(" "); // This is needed due to a Swing print bug
    }

    public Component
      getTableCellRendererComponent(JTable table, Object value,
                                    boolean isSelected, boolean hasFocus,
                                    int row, int column) {
      colorRenderer(this, isSelected, table, row, column);
      setSelected((value != null && ((Boolean)value).booleanValue()));
      setHorizontalAlignment(JCheckBox.CENTER);
      return this;
    }
  }

  private class ColoredTableCellButton extends TableCellButton
  {
    public ColoredTableCellButton(String label)
    {
      super(label);
    }

    protected void colorRenderer(Component comp, boolean isSelected,
                                 JTable table, int row, int column)
    {
      ColoredCellTable.this.colorRenderer(comp, isSelected, table, row, column);
    }
  }
}