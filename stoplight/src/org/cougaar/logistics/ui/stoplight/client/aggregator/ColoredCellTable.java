/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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