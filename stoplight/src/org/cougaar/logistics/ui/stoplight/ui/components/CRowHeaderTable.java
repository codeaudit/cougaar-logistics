/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.logistics.ui.stoplight.ui.models.DatabaseTableModel;
import org.cougaar.logistics.ui.stoplight.ui.util.SelectableHashtable;

/**
 * This is a JTable with an arbitrary number of column and row headers.  The
 * start of the data is found by the row header table from the given table
 * model.  The data must be of type Float (and the headers must not).
 */
public class CRowHeaderTable extends JTable
{
    private JScrollPane enclosingScrollpane = null;
    private boolean fitRowHeight = false;
    private int minCellCharacterWidth = 7;
    private int minCellWidth = 0;
    private HeaderCellRenderer headerCellRenderer = new HeaderCellRenderer();
    private JTable cornerHeader = createCornerHeader();
    private JTable rowHeader = createRowHeader();
    protected int rowStart = 1;
    protected int columnStart = 1;

    /**
     * Default constructor.  Create new row header table.
     */
    public CRowHeaderTable()
    {
        super();
        init();
    }

    /**
     * Create new row header table using the given table model for data.
     *
     * @param tm the table model to use for data.
     */
    public CRowHeaderTable(TableModel tm)
    {
        super(tm);
        init();
    }

    private void init()
    {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        findDataStart();
        resetMinCellWidth();
        JTableHeader th = getTableHeader();

        // jdk1.2.2 - jdk1.3 special case
        if (usingJdk13orGreater())
        {
            try
            {
                // Without using reflection, the following line is:
                // th.setDefaultRenderer(headerCellRenderer)
                // Will not compile under jdk1.2.2 (thus the use of reflection)
                th.getClass().getMethod("setDefaultRenderer",
                  new Class[] {TableCellRenderer.class}).
                    invoke(th, new Object[] {headerCellRenderer});
            }
            catch (Exception e) {e.printStackTrace();}
        }
        else
        {
            // jdk1.2  (this is much easier in jdk1.3)
            final TableColumnModel tcm = getColumnModel();
            for (int i=0; i <tcm.getColumnCount(); i++)
            {
                tcm.getColumn(i).setHeaderRenderer(headerCellRenderer);
            }
            tcm.addColumnModelListener(
                new TableColumnModelListener() {
                    public void columnAdded(TableColumnModelEvent e)
                    {
                        tcm.getColumn(e.getToIndex()).
                            setHeaderRenderer(headerCellRenderer);
                    }
                    public void columnMarginChanged(ChangeEvent e) {}
                    public void columnMoved(TableColumnModelEvent e) {}
                    public void columnRemoved(TableColumnModelEvent e) {}
                    public void columnSelectionChanged(ListSelectionEvent e) {}
                });
        }

        resizeRowHeadersToFit();
    }

    /**
     * Called whenever look and feel is changed.
     */
    public void updateUI()
    {
        super.updateUI();

        resetMinCellWidth();
        resizeRowHeadersToFit();
    }

    /**
     * Set row height mode.  If false, rows will be automatically set to the
     * correct height for the current look and feel.
     *
     * @param fitRowHeight new value for fitRowHeight.
     */
    public void setFitRowHeight(boolean fitRowHeight)
    {
        this.fitRowHeight = fitRowHeight;
        resizeRowHeadersToFit();
    }

    /**
     * Get row height mode.  If false, rows are automatically set to the
     * correct height for the current look and feel.
     *
     * @return current value for fitRowHeight.
     */
    public boolean getFitRowHeight()
    {
        return fitRowHeight;
    }

    /**
     * Keep row header height and data row height in sync.
     *
     * @param rowHeight new row height
     */
    public void setRowHeight(int rowHeight)
    {
        super.setRowHeight(rowHeight);

        if (rowHeader != null)
        {
            rowHeader.setRowHeight(rowHeight);
        }
    }

    /**
     * Set the number of characters that must be visible in each data cell.
     *
     * @param width the number of characters that must be visible in each data
     *              cell.
     */
    public void setMinCellWidth(int width)
    {
        if (width != minCellCharacterWidth)
        {
            minCellCharacterWidth = width;

            resetMinCellWidth();
            resizeRowHeadersToFit();
        }
    }

    /**
     * Set the number of characters that must be visible in each data cell.
     *
     * @return the number of characters that will be visible in each data cell.
     */
    public int getMinCellWidth()
    {
        return minCellCharacterWidth;
    }

    private void resetMinCellWidth()
    {
        StringBuffer sampleString = new StringBuffer();
        for (int i = 0 ; i < minCellCharacterWidth; i++)
        {
            sampleString.append("#");
        }
        minCellWidth =
            getFontMetrics(UIManager.getFont("Label.font")).
                stringWidth(sampleString.toString());
    }

    /**
     * Called when table model changes.  Updates view of model.
     *
     * @param e table model event describing change.
     */
    public void tableChanged(TableModelEvent e)
    {
        findDataStart();
        resizeRowHeadersToFit();

        super.tableChanged(e);

        if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
        {
            // Remove repeated header columns
            TableColumnModel cm = getColumnModel();
            for (int i = 0; i < columnStart + 1; i++)
            {
                if (cm.getColumnCount() > 0)
                {
                    cm.removeColumn(cm.getColumn(0));
                }
            }
            if ((rowHeader != null) && (cornerHeader != null))
            {
                ((AbstractTableModel)rowHeader.getModel()).
                    fireTableStructureChanged();
                ((AbstractTableModel)cornerHeader.getModel()).
                    fireTableStructureChanged();
            }
        }
    }

    /**
     * Get the table used for rendering row headers for main table
     *
     * @return the table used for rendering row headers for main table
     */
    public JTable getRowHeader()
    {
        return rowHeader;
    }

    /**
     * If this JTable is the viewportView of an enclosing JScrollPane
     * (the usual situation), configure this ScrollPane by, amongst other
     * things, installing the table's rowHeader as the rowHeaderView of
     * the scroll pane.
     */
    protected void configureEnclosingScrollPane()
    {
        super.configureEnclosingScrollPane();

        Container c = getParent();
        if (c instanceof JViewport)
        {
            c = c.getParent();
            if (c instanceof JScrollPane)
            {
                JScrollPane sp = (JScrollPane)c;
                enclosingScrollpane = sp;
                sp.setRowHeaderView(rowHeader);
                sp.setCorner(JScrollPane.UPPER_LEFT_CORNER, cornerHeader);

                sp.addComponentListener(new ComponentAdapter() {
                        public void componentResized(ComponentEvent e)
                        {
                            resizeRowHeadersToFit();
                        }
                    });
            }
        }
    }

    /**
     * Future work:  modify this method to return a JTable that includes all
     * row header columns (instead of just the first one).
     */
    private JTable createRowHeader()
    {
        TableModel tm = new AbstractTableModel() {
                public int getColumnCount()
                {
                    return columnStart + 1;
                }
                public int getRowCount()
                {
                    return getModel().getRowCount();
                }
                public Object getValueAt(int row, int column)
                {
                    return getModel().getValueAt(row, column);
                }
            };
        JTable newRowHeader = new JTable(tm);
        newRowHeader.setDefaultRenderer(Object.class, headerCellRenderer);
        return newRowHeader;
    }

    private JTable createCornerHeader()
    {
        TableModel tm = new AbstractTableModel() {
                public int getColumnCount()
                {
                    return columnStart + 1;
                }
                public int getRowCount()
                {
                    return 1;
                }
                public Object getValueAt(int row, int column)
                {
                    return getModel().getColumnName(column);
                }
            };
        JTable newCornerHeader = new JTable(tm);
        newCornerHeader.setDefaultRenderer(Object.class, headerCellRenderer);
        return newCornerHeader;
    }

    /**
     * Returns the cell renderer to use for the given table cell.
     *
     * @param row cell row index
     * @param column cell column index
     */
    public TableCellRenderer getCellRenderer(int row, int column)
    {
        if (row < rowStart)
        {
            return headerCellRenderer;
        }
        else
        {
            return super.getCellRenderer(row, column);
        }
    }

    private void findDataStart()
    {
        TableModel tm = getModel();

        // find start of data (i.e. end of headers)
        search : {
            for (rowStart = 0; rowStart < tm.getRowCount(); rowStart++)
            {
                for (columnStart = 0; columnStart < tm.getColumnCount();
                     columnStart++)
                {
                    Object value = tm.getValueAt(rowStart, columnStart);
                    if ((value instanceof Float) ||
                        (value.toString().equals(DatabaseTableModel.NO_VALUE)) ||
                        (value.toString().equals("Infinity")))
                    {
                        break search;
                    }
                }
            }
        }

        // Protection against apparent race condition bug that results in this
        // being set too high.  (demo kludge)
        if (rowStart > 0) rowStart = 1;

        // adjust for first column which is now rendered in seperate row header
        // component.
        if (columnStart > 0) columnStart--;
    }

    private void resizeRowHeadersToFit()
    {
        // This can take a long time (esp. when using jdk1.2)
        // I am having trouble getting a wait cursor to appear during this
        // operation

        // Resize row header columns to fit contents
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    if ((rowHeader == null)||(getParent() == null)) return;

                    // Set data cell widths
                    // (only use horizontal scroll bar if needed)
                    int columnCount = getColumnModel().getColumnCount();
                    int dataWidth = 0;
                    if (columnCount > 0)
                    {
                        int vpWidth = getParent().getSize().width;
                        if ((vpWidth / columnCount) < minCellWidth)
                        {
                            setAutoResizeMode(AUTO_RESIZE_OFF);
                            dataWidth = minCellWidth;
                        }
                        else
                        {
                            setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
                        }
                    }

                    for (int i = 0; i < columnCount; i++)
                    {
                        TableColumn c = getColumnModel().getColumn(i);
                        c.setPreferredWidth(dataWidth);
                        c.setMinWidth(dataWidth);
                    }

                    fixRowHeight();

                    // Primary Row Headers are sized to show contents
                    int headCount = rowHeader.getModel().getColumnCount();
                    int totalWidth = 0;
                    for (int column = 0; column < headCount; column++)
                    {
                        int columnWidth =
                            sizeRowHeaderColumnBasedonContents(column);
                        totalWidth += columnWidth;
                    }
                    if (!usingJdk13orGreater())
                    {
                        totalWidth += headCount; // insets
                    }
                    rowHeader.setPreferredScrollableViewportSize(
                       new Dimension(totalWidth,
                       rowHeader.getPreferredScrollableViewportSize().height));
                    cornerHeader.setPreferredScrollableViewportSize(
                       new Dimension(totalWidth,
                       cornerHeader.getPreferredScrollableViewportSize().height));

                    repaintAndValidate();
                }
            });
    }

    private void fixRowHeight()
    {
        // Presentation mode fix
        TableModel tm = getModel();
        if ((tm != null) && (tm.getColumnCount() > 0))
        {
            int rowHeight = getColumnHeader(CRowHeaderTable.this, 0).
                getTableCellRendererComponent(CRowHeaderTable.this,
                "#", false, false, 0, 0).getPreferredSize().height;
            if ((rowHeader != null) && (cornerHeader != null)
                && (rowHeight > 0))
            {
                setRowHeight(rowHeight);
                cornerHeader.setRowHeight(rowHeight);
            }
        }

        int forcedRowHeight = 0;
        if (fitRowHeight)
        {
            // adjust row height such that all rows fit in viewport
            forcedRowHeight = (enclosingScrollpane.
                getViewportBorderBounds().height)/getRowCount();
            if (!usingJdk13orGreater())
            {
                forcedRowHeight--; // adjust for intercell spacing
            }
            if (forcedRowHeight < getRowHeight())
            {
                setRowHeight((forcedRowHeight <= 0) ? 1 : forcedRowHeight);
            }
        }

        // If cells are being compressed to a very small height/width,
        // turn off borders around cells.
        int bWidth = 1;
        int bHeight = 1;
        if (getColumnModel().getColumnCount() > 0)
        {
            int actualDataColumnWidth =
                ((TableColumn)getColumnModel().getColumn(0)).getWidth();
            if (actualDataColumnWidth < 3)
            {
                bWidth = 0;
            }
            if (getRowHeight() < 3)
            {
                bHeight = 0;
                if (!usingJdk13orGreater())
                {
                    // if jdk < 1.3 row height will change when intercell
                    // spacing is set to 0. (so we set it back to the correct
                    // height)
                    if (forcedRowHeight > 0)
                        setRowHeight(getRowHeight() + 1);
                }
            }
        }
        setIntercellSpacing(new Dimension(bWidth, bHeight));
        if (!usingJdk13orGreater())
        {
            rowHeader.setIntercellSpacing(
                new Dimension(rowHeader.getIntercellSpacing().width, bHeight));
        }
    }

    private void repaintAndValidate()
    {
        // Ensure that table repaints correctly
        sizeColumnsToFit(-1);
        getTableHeader().resizeAndRepaint();
        rowHeader.sizeColumnsToFit(-1);
        rowHeader.revalidate();
        rowHeader.repaint();
        cornerHeader.sizeColumnsToFit(-1);
        cornerHeader.revalidate();
        cornerHeader.repaint();
        revalidate();
        repaint();
    }

    /**
     * Needed for compatibility with jdk1.2.2
     */
    private static boolean usingJdk13orGreater()
    {
        float versionNumber =
            Float.parseFloat(System.getProperty("java.class.version"));
        return (versionNumber >= 47.0);
    }

    /* not currently being used (but might be useful later)
    private static int sizeColumnBasedonContents(JTable table,int columnIndex)
    {
        TableModel tm = table.getModel();
        TableColumn column = table.getColumnModel().getColumn(columnIndex);

        TableCellRenderer cr = getColumnHeader(table, columnIndex);

        Component comp =
            cr.getTableCellRendererComponent(null, column.getHeaderValue(),
                                             false, false, 0, 0);
        int headerWidth = comp.getPreferredSize().width;

        int maxCellWidth = 0;
        for (int row = 0; row < tm.getRowCount(); row++)
        {
            comp = table.getDefaultRenderer(tm.getColumnClass(columnIndex)).
                    getTableCellRendererComponent(
                        table, tm.getValueAt(row, columnIndex),
                        false, false, row, columnIndex);
            int cellWidth = comp.getPreferredSize().width+table.getRowMargin();
            maxCellWidth = Math.max(maxCellWidth, cellWidth);
        }

        int targetWidth = Math.max(headerWidth, maxCellWidth);
        column.setMinWidth(targetWidth);
        column.setMaxWidth(targetWidth + 25);

        return targetWidth;
    }
    */

    private int sizeRowHeaderColumnBasedonContents(int columnIndex)
    {
        TableModel tm = cornerHeader.getModel();
        TableCellRenderer cr = cornerHeader.getCellRenderer(0, columnIndex);

        Component comp =
            cr.getTableCellRendererComponent(null, tm.getValueAt(0, columnIndex),
                                             false, false, 0, 0);
        int headerWidth = comp.getPreferredSize().width;

        JTable table = rowHeader;
        tm = table.getModel();
        int maxCellWidth = 0;
        for (int row = 0; row < tm.getRowCount(); row++)
        {
            comp = table.getDefaultRenderer(tm.getColumnClass(columnIndex)).
                    getTableCellRendererComponent(
                        table, tm.getValueAt(row, columnIndex),
                        false, false, row, columnIndex);
            int cellWidth = comp.getPreferredSize().width+table.getRowMargin();
            maxCellWidth = Math.max(maxCellWidth, cellWidth);
        }

        int targetWidth = Math.max(headerWidth, maxCellWidth);

        // reduce the width if the height is so small that it isn't readable
        int fontHeight = getFontMetrics(getFont()).getHeight();
        if (getRowHeight() < fontHeight)
        {
            targetWidth = 10;
        }

        targetWidth += 5; // A little breathing room
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(targetWidth);
        column.setPreferredWidth(targetWidth);
        column.setMaxWidth(targetWidth);
        column = cornerHeader.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(targetWidth);
        column.setPreferredWidth(targetWidth);
        column.setMaxWidth(targetWidth);
        return targetWidth;
    }

    private TableCellRenderer getColumnHeader(JTable table,int columnIndex)
    {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);

        TableCellRenderer cr = null;
        if (usingJdk13orGreater())
        {
            JTableHeader th = table.getTableHeader();

            try
            {
                // Without using reflection, the following line is:
                // ct = th.getDefaultRenderer()
                // Will not compile under jdk1.2.2 (thus the use of reflection)
                cr = (TableCellRenderer)th.getClass().
                        getMethod("getDefaultRenderer", null).invoke(th, null);
            }
            catch (Exception e) {e.printStackTrace();}
        }
        else
        {
            cr = column.getHeaderRenderer(); // jdk1.2
        }

        return cr;
    }

    private class HeaderCellRenderer extends DefaultTableCellRenderer
    {
        public Component
            getTableCellRendererComponent(JTable table, Object value,
                                          boolean isSelected, boolean hasFocus,
                                          int row, int column)
        {
            prepareComponent();
            String dispValue = (value == null) ? "" : value.toString();
            setText(dispValue);
            HeaderCellRenderer.this.setToolTipText(dispValue);
            if (row < rowStart)
            {
                 setHorizontalAlignment(JLabel.CENTER);
            }
            else
            {
                 setHorizontalAlignment(JLabel.LEFT);
            }

            // This special case statement is not generic and should be moved
            // to a domain specific class when time becomes available to
            // rework design.
            if (column <= columnStart)
            {
                if (value instanceof DefaultMutableTreeNode)
                {
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode)value;
                    if (!tn.isLeaf())
                    {
                        setText("(+) " + getText());
                    }

                    Object userObject = tn.getUserObject();
                    if (userObject instanceof SelectableHashtable)
                    {
                        SelectableHashtable sht =
                            (SelectableHashtable)userObject;
                        String selectedProperty = sht.getSelectedProperty();
                        String tooltipProperty =
                            selectedProperty.equals("UID") ? "ITEM_ID" : "UID";
                        Object tooltip = sht.get(tooltipProperty);
                        if (tooltip != null)
                        {
                            HeaderCellRenderer.this.setToolTipText(tooltip.toString());
                        }
                    }
                }
            }

            return this;
        }

        private void prepareComponent()
        {
            if (CRowHeaderTable.this != null)
            {
                JTableHeader header = getTableHeader();
                if (header != null)
                {
                    HeaderCellRenderer.this.setForeground(header.getForeground());
                    HeaderCellRenderer.this.setBackground(header.getBackground());
                          HeaderCellRenderer.this.setFont(header.getFont());
                }
            }
            HeaderCellRenderer.this.setOpaque(true);
            HeaderCellRenderer.this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        }
    }
}