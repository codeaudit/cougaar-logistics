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
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.Component;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.tree.*;

import org.cougaar.lib.uiframework.ui.models.TransformableTableModel;
import org.cougaar.lib.uiframework.ui.models.RangeModel;
import org.cougaar.lib.uiframework.ui.models.VariableModel;
import org.cougaar.lib.uiframework.ui.util.VariableInterfaceManager;

import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

/**
 * Used to feed a TransformableTableModel with queries based on the selected
 * variable values managed by a VariableInterfaceManager.
 */
public abstract class QueryGenerator
{
    private static final boolean debug = false;
    protected static final String NO_DATA = "No Data Available";

    /** the table model to set queries on. */
    protected TransformableTableModel tableModel;

    protected AggregationScheme aggregationScheme = null;

    /**
     * Creates a new query generator.
     *
     * @param tableModel the database table model to set queries on
     */
    public QueryGenerator(TransformableTableModel tableModel)
    {
        this.tableModel = tableModel;
    }

    /** provide public access to finalizer for all query generators */
    public void finalize() {}

    public TableModel getTableModel()
    {
      return tableModel;
    }

    /**
     * Generate a query based on the selected variable values referenced
     * by the given variable interface manager.  Set this query in the
     * query generator's table model.  Further manipulate the
     * result set as required through the table model transformation
     * methods.
     *
     * @param vim the variable interface manager from which the variable
     *            value selections will be gathered.
     * @param postQueryRunnable runnable to run after query is complete
     */
    public abstract void generateQuery(VariableInterfaceManager vim,
                                       Runnable postQueryRunnable);

    protected void sortRowsAndColumns(VariableModel itemDescriptor)
    {
        // sort columns and rows (sort items by NSN)
        Comparator itemCompare = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Vector v1 = (Vector)o1;
                    Vector v2 = (Vector)o2;
                    String s1 = convert(v1.elementAt(0), "UID");
                    String s2 = convert(v2.elementAt(0), "UID");

                    // comp. for bad NSNs for Class I
                    if (s1.startsWith("NSN/89") && s2.startsWith("NSN/89"))
                    {
                        s1 = convert(v1.elementAt(0), "ITEM_ID");
                        s2 = convert(v2.elementAt(0), "ITEM_ID");
                    }

                    return s1.compareTo(s2);
                }

                private String convert(Object o, String key)
                {
                    try
                    {
                        DefaultMutableTreeNode tn = (DefaultMutableTreeNode)o;
                        String s = ((Hashtable)
                            tn.getUserObject()).get(key).toString();
                        return s;
                    } catch (Exception e) {}

                    return o.toString();
                }
            };

        if (debug) System.out.println("Sorting columns");
        tableModel.transpose();
        if (itemDescriptor.getState() == VariableModel.X_AXIS)
        {
            tableModel.sortRows(itemCompare);
        }
        else
        {
            tableModel.sortRows(0);
        }
        tableModel.transpose();

        if (debug) System.out.println("Sorting rows");
        if (itemDescriptor.getState() == VariableModel.Y_AXIS)
        {
            tableModel.sortRows(itemCompare);
        }
        else
        {
            tableModel.sortRows(0);
        }
    }

    protected void aggregateItems(DefaultMutableTreeNode node, int itemColumn)
    {
        if (!node.isLeaf())
        {
            Vector childVector = new Vector();
            for (int i = 0; i < node.getChildCount(); i++)
            {
                DefaultMutableTreeNode tn =
                    (DefaultMutableTreeNode)node.getChildAt(i);
                childVector.add(tn);
                aggregateItems(tn, itemColumn);
            }
            tableModel.aggregateRows(childVector, node, itemColumn,
             aggregationScheme.getCombiner(aggregationScheme.itemAggregation));
        }
    }

    protected static void convertColumnHeaderIDsToNames(VariableModel vm,
                                                    TransformableTableModel tm,
                                                    int columnStart,
                                                    String oldIdName,
                                                    boolean aggregated)
    {
        String varName = vm.getName();

        if (!varName.equalsIgnoreCase("time"))
        {
            Vector oldColumnHeaders = new Vector();
            for (int column=columnStart; column<tm.getColumnCount(); column++)
            {
                oldColumnHeaders.add(tm.getColumnName(column));
            }

            Enumeration newColumnHeaders =
              convertHeaderIDsToNames(vm, oldColumnHeaders.elements(),
                                      oldIdName, aggregated);

            int columnCount = columnStart;
            while (newColumnHeaders.hasMoreElements())
            {
                Object name = newColumnHeaders.nextElement();
                if (tm.getColumnCount() <= columnCount)
                {
                    if (tm.getRowCount() == 0)
                    {
                        tm.insertRow(0);
                    }
                    tm.insertColumn(columnCount);
                    tm.setColumnName(columnCount, NO_DATA);
                }
                tm.setColumnName(columnCount++, name);
            }
        }
    }

    protected static void convertRowHeaderIDsToNames(VariableModel vm,
                                                  TransformableTableModel tm,
                                                  int rowHeaderColumn,
                                                  String oldIdName,
                                                  boolean aggregated)
    {
        String varName = vm.getName();

        if (!varName.equalsIgnoreCase("Time"))
        {
            Vector oldRowHeaders = new Vector();
            for (int row = 0; row < tm.getRowCount(); row++)
            {
                oldRowHeaders.add(tm.getValueAt(row, rowHeaderColumn));
            }

            Enumeration newRowHeaders =
              convertHeaderIDsToNames(vm, oldRowHeaders.elements(), oldIdName,
                                      aggregated);

            int rowCount = 0;
            while (newRowHeaders.hasMoreElements())
            {
                if (tm.getRowCount() <= rowCount)
                {
                    tm.insertRow(rowCount);
                    if (tm.getColumnCount() == 1)
                    {
                        tm.insertColumn(1);
                        tm.setColumnName(1, NO_DATA);
                    }
                }
                Object header = newRowHeaders.nextElement();
                tm.setValueAt(header, rowCount++, rowHeaderColumn);
            }
        }
    }

    private static Enumeration
        convertHeaderIDsToNames(VariableModel vm, Enumeration oldHeaders,
                                String oldIdName, boolean aggregated)
    {
        Enumeration newHeaders = null;
        if (vm.getValue() instanceof DefaultMutableTreeNode)
        {
            Vector newHeaderObjects = new Vector();
            DefaultMutableTreeNode tn = (DefaultMutableTreeNode)vm.getValue();
            if (tn.isLeaf())
            {
                while (oldHeaders.hasMoreElements())
                {
                    String oldHeader = oldHeaders.nextElement().toString();
                    newHeaderObjects.add(tn);
                }
            }
            else
            {
                Vector allLeaves = null;
                if (!aggregated)
                {
                  allLeaves = TreeUtilities.getSubordinateList(tn, true);
                }

                while (oldHeaders.hasMoreElements())
                {
                    String oldHeader = oldHeaders.nextElement().toString();

                    boolean found = false;
                    if (!aggregated)
                    {
                        for (int i = 0; i < allLeaves.size(); i++)
                        {
                            DefaultMutableTreeNode child =
                                (DefaultMutableTreeNode)allLeaves.elementAt(i);
                            Hashtable childHT = (Hashtable)child.getUserObject();
                            if (oldHeader.equals(childHT.get(oldIdName)))
                            {
                                newHeaderObjects.add(child);
                                found = true;
                                break;
                            }
                        }
                    }
                    else
                    {
                        for (int ci = 0; ci < tn.getChildCount(); ci++)
                        {
                            DefaultMutableTreeNode child =
                                (DefaultMutableTreeNode)tn.getChildAt(ci);
                            Hashtable childHT = (Hashtable)child.getUserObject();
                            if (oldHeader.equals(childHT.get(oldIdName)))
                            {
                                newHeaderObjects.add(child);
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found)
                    {
                      System.out.println("WARNING: " + oldHeader +
                                         " not found in item weights tree");
                      newHeaderObjects.add(oldHeader);
                    }
                }

                /*
                // fill with N/As if data does not exist
                if (newHeaderObjects.size() != tn.getChildCount())
                {
                    for (int ci = 0; ci < tn.getChildCount(); ci++)
                    {
                        DefaultMutableTreeNode child =
                            (DefaultMutableTreeNode)tn.getChildAt(ci);
                        if (!newHeaderObjects.contains(child))
                        {
                            newHeaderObjects.add(child);
                        }
                    }
                }
                */
            }
            newHeaders = newHeaderObjects.elements();
        }
        else
        {
            newHeaders = DBInterface.
                    lookupValues(DBInterface.getTableName(vm.getName()), "id",
                    "name", oldHeaders).elements();
        }

        return newHeaders;
    }

    protected void deriveUnitColumn(String yDescName, String metric)
    {
        // derive unit column if needed
        if (yDescName.equals("Item") &&
          !MetricInfo.metricUnits.get(metric).equals(MetricInfo.UNITLESS))
        {
            if (debug) System.out.println("Adding unit of issue column");
            tableModel.insertColumn(1);
            tableModel.setColumnName(1, "UI");
            boolean uifound = false;
            for (int row = 0; row < tableModel.getRowCount(); row++)
            {
                if (!(tableModel.getValueAt(row, 0) instanceof
                  DefaultMutableTreeNode)) continue;

                DefaultMutableTreeNode tn =
                    (DefaultMutableTreeNode)tableModel.getValueAt(row, 0);
                Hashtable ht = (Hashtable)tn.getUserObject();
                Object units = ht.get("UNITS");
                if (units != null)
                {
                    uifound = true;
                    tableModel.setValueAt(units, row, 1);
                }
                else
                {
                    tableModel.setValueAt("", row, 1);
                }
            }
            if (!uifound)
            {
                tableModel.removeColumn(1);
            }
        }
    }

    /**
     * Aggregate the rows of the table model based on the child list of the
     * given tree node.
     *
     * @param node         node under which to aggregate
     * @param headerColumn index of column that contains row headers to match
     *                     with tree elements.
     * @param combiner     the object used to combine two values into one.
     */
    /*
    private void aggregateTreeRows(DefaultMutableTreeNode node,
                                   int headerColumn,
                                   DatabaseTableModel.Combiner combiner)
    {
        for (int i = 0; i < node.getChildCount(); i++)
        {
            DefaultMutableTreeNode tn =
                (DefaultMutableTreeNode)node.getChildAt(i);
            dbTableModel.aggregateRows(getLeafList(tn).elements(),
                                       tn.getUserObject().toString(),
                                       headerColumn, combiner);
        }
    }
    */

    /**
     * Creates a delimited list string based on given parameters.
     *
     * @param items objects that will be converted to strings as the
     *              elements of the new delimited list
     * @param prefix prefix to include before each element in the list
     * @param postfix postfix to include after each element in the list
     * @param delimiter delimiter to include between the elements of the list
     * @return the generated delimited list string
     */
    protected static String
      createDelimitedList(Enumeration items, String prefix,
                          String postfix, String delimiter)
    {
        StringBuffer list = new StringBuffer();
        while (items.hasMoreElements())
        {
            Object item = items.nextElement();
            list.append(prefix + item.toString() + postfix);
            if (items.hasMoreElements()) list.append(delimiter);
        }
        return list.toString();
    }
}