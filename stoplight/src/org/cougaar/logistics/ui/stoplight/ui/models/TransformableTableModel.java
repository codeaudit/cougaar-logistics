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
package org.cougaar.logistics.ui.stoplight.ui.models;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * The database table model is used to represent the result set of a SQL
 * query as a table model that can be used to populate either a graph or
 * table view of the data.
 *
 * Additional methods are included to manipulate the modeled result set after
 * the query (e.g. transpose, setXY, aggregate).
 */
public class TransformableTableModel implements TableModel
{
    /** string shown in cells that have no data */
    public final static String NO_VALUE = " ";

    /** Vector of table model listeners. */
    private Vector tableModelListeners = new Vector();

    /** Vector of rows modeled.  Each row is a vector of data */
    protected Vector dataRows = new Vector();

    /** Default constructor; model will contain no data until DBQuery is set */
    public TransformableTableModel() {}

    /**
     * Returns the number of rows in the model
     *
     * @return the number of rows in the model
     */
    public synchronized int getRowCount()
    {
        return dataRows.size() - 1;
    }

    /**
     * Returns the number of columns in the model
     *
     * @return the number of columns in the model
     */
    public synchronized int getColumnCount()
    {
        return (dataRows.size() > 0) ?
               ((Vector)dataRows.elementAt(0)).size() : 0;
    }

    /**
     * Returns the name of a column given column index.
     *
     * @return the name of the column
     */
    public String getColumnName(int columnIndex)
    {
        if (dataRows.size() == 0)
          return "";

        String name =
            ((Vector)dataRows.elementAt(0)).elementAt(columnIndex).toString();
        return name;
    }

    /**
     * Set the name of a column given column index.
     *
     * @param columnIndex the index of the column to set
     * @param name        the name of the column
     */
    public void setColumnName(int columnIndex, Object name)
    {
        setValueAt(name, -1, columnIndex);
    }

    /**
     * Gets the index of a column given column name.
     *
     * @param columnName  the name of the column
     * @return the index of the column
     */
    public int getColumnIndex(String columnName)
    {
        int columnIndex = -1;
        Vector columnHeaders = (Vector)dataRows.elementAt(0);
        for (int i=0; i<columnHeaders.size(); i++)
        {
            if (columnName.
                equalsIgnoreCase((String)columnHeaders.elementAt(i)))
            {
                columnIndex = i;
                break;
            }
        }
        return columnIndex;
    }

    /**
     * Gets the class of a column given column index.
     *
     * @return the class of the column
     */
    public Class getColumnClass(int columnIndex)
    {
        return String.class;
    }

    /**
     * Returns true if the view should allow editing of the cell.
     *
     * @param rowIndex    the row index of cell
     * @param columnIndex the column index of cell
     * @return whether the cell is editable
     */
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    /**
     * Returns the contents of the cell.
     *
     * @param rowIndex    the row index of cell
     * @param columnIndex the column index of cell
     * @return the cell contents
     */
    public synchronized Object getValueAt(int rowIndex, int columnIndex)
    {
        try
        {
            return ((Vector)dataRows.elementAt(rowIndex + 1)).
                    elementAt(columnIndex);
        }
        catch (Exception e)
        {
            return new Float(99999); // look into this
        }
    }

    /**
     * Sets the contents of the cell.
     * (Does not trigger a table model change event)
     *
     * @param aValue      new value for the cell
     * @param rowIndex    the row index of cell
     * @param columnIndex the column index of cell
     */
    public synchronized void
        setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        ((Vector)dataRows.elementAt(rowIndex + 1)).
            setElementAt(aValue, columnIndex);
    }

    /**
     * Adds a new table model listener to the table.
     *
     * @param tml the table model listener to add
     */
    public void addTableModelListener(TableModelListener tml)
    {
        tableModelListeners.add(tml);
    }

    /**
     * Removes a table model listener from the table.
     *
     * @param tml the table model listener to remove
     */
    public void removeTableModelListener(TableModelListener tml)
    {
        tableModelListeners.remove(tml);
    }

    /**
     * Fire a new table change event on table.  This will cause all views to
     * resync with model.
     *
     * @param e the table model event to associate with the table change event
     */
    public void fireTableChangedEvent(TableModelEvent e)
    {
        for (int i = 0; i < tableModelListeners.size(); i++)
        {
            TableModelListener tml =
                (TableModelListener)tableModelListeners.elementAt(i);
            tml.tableChanged(e);
        }
    }

    /**
     * This method takes three column indices and transforms the table such
     * that the values in the first column are used as column headers and the
     * values in the second column are used as row headers.  The values in the
     * third column are used to populate the body of the new table (indexed by
     * the other two columns).  The new table will have a single row of column
     * headers and a single column of row headers.
     *
     * @param xColumn the column index of which the values should be used as
     *                column headers after transformation
     * @param yColumn the column index of which the values should be used as
     *                row headers after transformation
     * @param valueColumn the column index of which the values should be used
     *                    as data cells after transformation
     */
    public synchronized void setXY(int xColumn, int[] yColumn, int valueColumn)
    {
        if (dataRows.size() > 0)
        {
            // determine new column headers and mapped data points
            Vector columnHeaders = new Vector();
            for (int i = 0; i < yColumn.length; i++)
            {
                columnHeaders.addElement(" "); // column header for row headers
            }
            Vector[] rowHeaders = new Vector[yColumn.length];
            for (int i = 0; i < rowHeaders.length; i++)
            {
                rowHeaders[i] = new Vector();
            }
            Vector dataPoints = new Vector();
            for (int rowIndex = 1; rowIndex < dataRows.size(); rowIndex++)
            {
                int transformedColumnIndex = 0;
                int transformedRowIndex = 0;
                Vector origRowVector = (Vector)dataRows.elementAt(rowIndex);

                Object newColumnHeader = origRowVector.elementAt(xColumn);
                transformedColumnIndex = columnHeaders.indexOf(newColumnHeader);
                if (transformedColumnIndex == -1)
                {
                    columnHeaders.addElement(newColumnHeader);
                    transformedColumnIndex = columnHeaders.size() - 1;
                }

                // Find row (if it exists)
                Object[] newRowHeader = new Object[rowHeaders.length];
                for (int i = 0; i < newRowHeader.length; i++)
                {
                    newRowHeader[i] = origRowVector.elementAt(yColumn[i]);
                }
                for (int row = 0; row < rowHeaders[0].size(); row++)
                {
                    boolean found = true;
                    for (int i = 0; i < newRowHeader.length; i++)
                    {
                        if (!newRowHeader[i].equals(rowHeaders[i].elementAt(row)))
                        {
                            found = false;
                        }
                    }
                    if (found)
                    {
                        transformedRowIndex = row + 1;
                        break;
                    }
                }

                // row not found, new row needs to be created
                if (transformedRowIndex == 0)
                {
                    for (int i = 0; i < rowHeaders.length; i++)
                    {
                        rowHeaders[i].addElement(newRowHeader[i]);
                    }
                    transformedRowIndex = rowHeaders[0].size();
                }

                dataPoints.addElement(
                    new DataPoint(transformedRowIndex, transformedColumnIndex,
                                  origRowVector.elementAt(valueColumn)));
            }

            // use data point vector to create new row vectors
            int maxRowSize = 0;
            Vector newRows = new Vector();
            newRows.addElement(columnHeaders);
            for (int i = 0; i < dataPoints.size(); i++)
            {
                DataPoint dp = (DataPoint)dataPoints.elementAt(i);

                while(dp.rowIndex > newRows.size())
                {
                    Vector newRow = new Vector();
                    newRows.addElement(newRow);
                }

                if (dp.rowIndex == newRows.size())
                {
                    Vector newRow = new Vector();
                    for (int x = 0; x < rowHeaders.length; x++)
                    {
                        newRow.addElement(rowHeaders[x].elementAt(dp.rowIndex-1));
                    }
                    newRows.addElement(newRow);
                }
                Vector targetRow = (Vector)newRows.elementAt(dp.rowIndex);

                while(dp.columnIndex > targetRow.size())
                {
                    targetRow.add(NO_VALUE);
                }

                if (dp.columnIndex >= targetRow.size())
                {
                    targetRow.add(dp.value);
                }
                else
                {
                    targetRow.setElementAt(dp.value, dp.columnIndex);
                }

                maxRowSize = Math.max(maxRowSize, targetRow.size());
            }

            // Make sure that all rows are the same size by padding with N/As
            for (int i = 0; i < newRows.size(); i++)
            {
                Vector row = (Vector)newRows.elementAt(i);
                while (row.size() < maxRowSize)
                {
                    row.add(NO_VALUE);
                }
            }

            dataRows = newRows;

            fireTableChangedEvent(
                new TableModelEvent(this, TableModelEvent.HEADER_ROW));
        }
    }

    /**
     * Sort rows based on given column.
     *
     * @param sortColumn column on which to sort
     */
    public void sortRows(final int sortColumn)
    {
        Comparator c = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Vector v1 = (Vector)o1;
                    Vector v2 = (Vector)o2;
                    String s1 = v1.elementAt(sortColumn).toString();
                    String s2 = v2.elementAt(sortColumn).toString();

                    try {
                        Float f1 = new Float(s1);
                        Float f2 = new Float(s2);
                        return f1.compareTo(f2);
                    }
                    catch(Exception e){}
                    return s1.compareTo(s2);
                }
            };

        sortRows(c);
    }

    /**
     * Sort rows using given comparator.  The two objects passed to the
     * comparator will be two Vectors each representing a row.
     *
     * @param c comparator to use to compare rows.
     */
    public void sortRows(Comparator c)
    {
        Collections.sort(dataRows, c);
    }

    /**
     * This class is used by setXY transformation for intermediate data
     * handling
     */
    private class DataPoint
    {
        public int rowIndex;
        public int columnIndex;
        public Object value;
        public DataPoint(int rowIndex, int columnIndex, Object value)
        {
            this.rowIndex = rowIndex;
            this.columnIndex= columnIndex;
            this.value = value;
        }
        public String toString()
        {
            return "row: " + rowIndex + ", column: " + columnIndex +
                   ", value: " + value;
        }
    }

    /**
     * Insert a new column into model. (filled with nulls)
     *
     * @param columnIndex where to insert new column
     */
    public synchronized void insertColumn(int columnIndex)
    {
        for (int row = 0; row < dataRows.size(); row++)
        {
            Vector dataRow = (Vector)dataRows.elementAt(row);
            dataRow.insertElementAt(NO_VALUE, columnIndex);
        }
        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * Remove a column from model.
     *
     * @param columnIndex index of column to remove
     */
    public synchronized void removeColumn(int columnIndex)
    {
        for (int row = 0; row < dataRows.size(); row++)
        {
            Vector dataRow = (Vector)dataRows.elementAt(row);
            dataRow.remove(columnIndex);
        }
        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * Insert a new row into model. (filled with nulls)
     *
     * @param rowIndex where to insert new row
     */
    public synchronized void insertRow(int rowIndex)
    {
        Vector newRow = new Vector();
        for (int column=0; column < getColumnCount(); column++)
        {
            newRow.add(NO_VALUE);
        }
        dataRows.add(newRow);
        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * transpose the contents of this model.
     * Rows become columns and columns become rows.
     */
    public synchronized void transpose()
    {
        if (dataRows.size() > 0)
        {
            Vector newRows = new Vector();

            for (int rowIndex = 0;
                 rowIndex < ((Vector)dataRows.elementAt(0)).size(); rowIndex++)
            {
                newRows.add(new Vector());
            }

            for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++)
            {
                Vector rowVector = (Vector)dataRows.elementAt(rowIndex);
                for (int columnIndex = 0; columnIndex < rowVector.size();
                     columnIndex++)
                {
                    Object data = rowVector.elementAt(columnIndex);
                    ((Vector)newRows.elementAt(columnIndex)).add(data);
                }
            }

            dataRows = newRows;
        }

        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * Returns the number of rows that have given id as a row header
     *
     * @param rowID the header to search for
     * @param headerColumn the index of the column to use as header column
     */
    public int getRowCount(String rowID, int headerColumn)
    {
        int count = 0;

        for (int i = 0; i < dataRows.size(); i++)
        {
            Vector row = (Vector)dataRows.elementAt(i);
            if (row.elementAt(headerColumn).toString().equals(rowID))
            {
                count++;
            }
        }

        return count;
    }

    /**
     * Apply expander to all rows in table.
     *
     * @param expander object that tells how to expand rows
     */
    public void expandAllRows(Expander expander)
    {
        Vector newDataRows = new Vector();

        for (int i = 0; i < dataRows.size(); i++)
        {
            Vector row = (Vector)dataRows.elementAt(i);
            Vector newRows = expander.expand(row);
            newDataRows.addAll(newRows);
        }

        dataRows = newDataRows;
    }

    /**
     * aggregates all rows in which all values in the significant columns
     * are equal between rows.  The agg header is the new header value placed
     * in the aggregatedHeaderColumn of the aggregated rows.
     *
     * @param significantColumns indices of columns that should be the same in
     *                           rows to aggregate
     * @param aggHeader          the new header to set in the new aggragated
     *                           row
     * @param aggregatedHeaderColumn the column index in which the new
     *                               aggregated header should be set
     */
    public void aggregateRows(int[] significantColumns, String aggHeader,
                              int aggregatedHeaderColumn, Combiner c)
    {
        if (getRowCount() > 1)
        {
            Vector modelRow;
            while (!(modelRow = (Vector)dataRows.elementAt(1)).
                   elementAt(aggregatedHeaderColumn).equals(aggHeader))
            {
                dataRows.removeElementAt(1);
                Vector aggregateRow =
                    extractAndAggregateRows(modelRow, significantColumns,
                                            aggHeader, aggregatedHeaderColumn,
                                            c);
                dataRows.addElement(aggregateRow);
            }
        }
    }

    /**
     * aggregates all rows in which all values in the significant columns
     * are equal to model row.  The agg header is the new header value placed
     * in the aggregatedHeaderColumn of the aggregated rows.
     *
     * @param modelRow           index of the row to use as model for
     *                           comparison
     * @param significantColumns indices of columns that should be the same in
     *                           rows to aggregate
     * @param aggHeader          the new header to set in the new aggragated
     *                           row
     * @param aggregatedHeaderColumn the column index in which the new
     *                               aggregated header should be set
     * @return the new aggregated row
     */
     private Vector
        extractAndAggregateRows(Vector modelRow, int[] significantColumns,
                                String aggHeader, int aggregatedHeaderColumn,
                                Combiner c)
    {
        Vector aggregateRow = c.prepare(modelRow, aggregatedHeaderColumn);

        for (int rowIndex = 1; rowIndex < dataRows.size(); rowIndex++)
        {
            Vector row = (Vector)dataRows.elementAt(rowIndex);
            if (row.elementAt(aggregatedHeaderColumn).equals(aggHeader)) break;
            boolean matches = true;
            for (int i = 0; i < significantColumns.length; i++)
            {
                int columnIndex = significantColumns[i];
                Object valueToMatch = modelRow.elementAt(columnIndex);
                Object value = row.elementAt(columnIndex);
                if (!value.equals(valueToMatch))
                {
                    matches = false;
                    break;
                }
            }
            if (matches)
            {
                // prepare row using combiner
                row = c.prepare(row, aggregatedHeaderColumn);

                dataRows.removeElementAt(rowIndex--);
                aggregateRow = combine(aggregateRow, row, c);
            }
         }

         // set header to higher level node
         aggregateRow.setElementAt(aggHeader, aggregatedHeaderColumn);

         // finalize combined row using combiner
         aggregateRow = c.finalize(aggregateRow, aggregatedHeaderColumn);

         return aggregateRow;
    }

    /**
     * Aggregates the given list of rows into a single row with new header
     *
     * @param rowList      list of row header strings to combine
     * @param aggHeader    the new header to set in the new aggragated
     *                     row
     * @param headerColumn the column index in which the new
     *                     aggregated header should be set
     * @param combiner     the object used to combine two values into one.
     */
    public void aggregateRows(Vector rowList, Object aggHeader,
                              int headerColumn, Combiner combiner)
    {
        if (headerColumn == 1)
        {
            Vector primaryHeaderVector = new Vector();
            for (int row = 0; row < getRowCount(); row++)
            {
                Object primaryHeader = getValueAt(row, 0);
                if (!primaryHeaderVector.contains(primaryHeader))
                {
                    primaryHeaderVector.add(primaryHeader);
                }
            }
            for (int i = 0; i < primaryHeaderVector.size(); i++)
            {
                Object primaryHeader = primaryHeaderVector.elementAt(i);
                aggregateRows(primaryHeader, rowList.elements(), aggHeader,
                              headerColumn, combiner);
            }
        }
        else
        {
            aggregateRows(null, rowList.elements(), aggHeader, headerColumn,
                          combiner);
        }
    }

    private void aggregateRows(Object primaryHeader, Enumeration rowList,
                              Object aggHeader, int headerColumn,
                              Combiner combiner)
    {
        Vector aggregateRow = null;
        while (rowList.hasMoreElements())
        {
            String rowID = rowList.nextElement().toString();
            RowHeaderDescriptor[] rhd = (primaryHeader == null) ?
                new RowHeaderDescriptor[]
                    {new RowHeaderDescriptor(headerColumn, rowID)} :
                new RowHeaderDescriptor[]
                    {new RowHeaderDescriptor(0, primaryHeader),
                     new RowHeaderDescriptor(headerColumn, rowID)};
            Vector row = findRow(rhd);
            if (row != null)
            {
                // prepare row using combiner
                row = combiner.prepare(row, headerColumn);

                dataRows.remove(row);
                if (aggregateRow == null)
                {
                    aggregateRow = row;
                }
                else
                {
                    aggregateRow = combine(aggregateRow, row, combiner);
                }
            }
        }

        if (aggregateRow != null)
        {
            // set header to higher level node
            aggregateRow.set(headerColumn, aggHeader);

            // finalize combined row using combiner
            aggregateRow = combiner.finalize(aggregateRow, headerColumn);

            // add aggregated row to table
            dataRows.add(aggregateRow);

            fireTableChangedEvent(
                new TableModelEvent(this, TableModelEvent.HEADER_ROW));
        }
    }

    /**
     * Returns list of rows the have the given row header
     *
     * @rowID        the row header to search for
     * @headerColumn the index of the column to use as row header column
     */
    private Vector findRow(RowHeaderDescriptor[] hd)
    {
        for (int i = 0; i < dataRows.size(); i++)
        {
            Vector row = (Vector)dataRows.elementAt(i);

            boolean found = true;
            for (int x = 0; x < hd.length; x++)
            {
                if (!row.elementAt(hd[x].headerColumn).toString().
                    equals(hd[x].value.toString()))
                {
                    found = false;
                    break;
                }
            }

            if (found)
            {
                return row;
            }
        }
        return null;
    }

    /**
     * combines two rows into one row
     *
     * @param row1 first row to be combined
     * @param row2 second row to be combined
     * @param combiner the object used to combine two values into one.
     * @return the combined row
     */
    private Vector combine(Vector row1, Vector row2, Combiner combiner)
    {
        if (row1 == null) return row2;
        if (row2 == null) return row1;

        Vector combinedRow = new Vector();
        for (int i = 0; i < row1.size(); i++)
        {
            combinedRow.add(
                combiner.combine(row1.elementAt(i), row2.elementAt(i)));
        }
        return combinedRow;
    }

    /**
     * Used to model how to combine two values into one
     */
    public static interface Combiner
    {
        /**
         * method is called on each row to be aggregated before aggregation.
         *
         * @param row row to be aggregated
         * @param headerColumn index of column that holds descriptions
         *                      of the items being combined
         * @return prepared row
         */
        public Vector prepare(Vector row, int headerColumn);

        /**
         * combines values into one value
         *
         * @param obj1 first object to be combined
         * @param obj2 second object to be combined
         * @return the combined object
         */
        public Object combine(Object obj1, Object obj2);

        /**
         * Operation to perform on aggregated value after all rows have been
         * combined.  (needed for averaging)
         *
         * @param row the aggregated row
         * @param headerColumn index of column that holds descriptions
         *                      of the items being combined
         * @return finalized aggregated row
         */
        public Vector finalize(Vector row, int headerColumn);
    }

    /**
     * Used to model how to expand one row into many rows
     */
    public static interface Expander
    {
        /**
         * expands one row into many rows
         *
         * @param row the row to be expanded
         * @return a collection of rows to replace this row
         */
        public Vector expand(Vector row);
    }

    private class RowHeaderDescriptor
    {
        public int headerColumn;
        public Object value;

        RowHeaderDescriptor(int headerColumn, Object value)
        {
            this.headerColumn = headerColumn;
            this.value = value;
        }

        public String toString()
        {
            return headerColumn + ", " + value;
        }
    }

    /**
     * For debug.  Prints out contents of model to standard out
     */
    public void printDataMatrix()
    {
        System.out.println(dataRows.size() + " rows, " +
                           ((Vector)dataRows.elementAt(0)).size() +" columns");
        for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++)
        {
            System.out.println();
            Vector rowVector = (Vector)dataRows.elementAt(rowIndex);
            for (int columnIndex = 0; columnIndex < rowVector.size();
                 columnIndex++)
            {
                System.out.print(rowVector.elementAt(columnIndex) + " ");
            }
        }
    }
}