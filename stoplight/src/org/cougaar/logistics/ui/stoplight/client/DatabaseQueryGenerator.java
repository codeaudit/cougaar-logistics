/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.lib.uiframework.ui.models.DatabaseTableModel;
import org.cougaar.lib.uiframework.ui.models.RangeModel;
import org.cougaar.lib.uiframework.ui.models.VariableModel;
import org.cougaar.lib.uiframework.ui.util.VariableInterfaceManager;

import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

/**
 * Used to feed a DatabaseTableModel with SQL queries based on the selected
 * variable values managed by a VariableInterfaceManager.  This code is domain
 * specific to blackjack applications.  Other versions of this class could
 * be created for other domains and database schemas.
 */
public class DatabaseQueryGenerator extends QueryGenerator
{
    private static final boolean debug = false;

    private VariableInterfaceManager variableManagerKludgeHelper = null;
    private DatabaseTableModel dbTableModel = null;

    /**
     * Creates a new query generator.
     */
    public DatabaseQueryGenerator()
    {
        super(new DatabaseTableModel());
        dbTableModel = (DatabaseTableModel)getTableModel();
    }

    /**
     * Generate a SQL query based on the selected variable values referenced
     * by the given variable interface manager.  Set this query in the
     * query generator's database table model.  Further manipulate the
     * result set as required through the database table model transformation
     * methods.
     *
     * @param vim the variable interface manager from which the variable
     *            value selections will be gathered.
     * @param postQueryRunnable runnable to run after query is complete
     */
    public void generateQuery(VariableInterfaceManager vim,
                              Runnable postQueryRunnable)
    {
        // demo kluge
        variableManagerKludgeHelper = vim;
        System.out.println(vim);

        //
        // build sql query from general query data
        //
        VariableModel xAxis = (VariableModel)vim.
                    getDescriptors(VariableModel.X_AXIS).nextElement();
        VariableModel yAxis = (VariableModel)vim.
                    getDescriptors(VariableModel.Y_AXIS).nextElement();

        VariableModel orgDesc = vim.getDescriptor("Org");
        VariableModel metricDesc = vim.getDescriptor("Metric");
        String metricString = metricDesc.getValue().toString();
        TreeNode selectedOrgNode = (TreeNode)orgDesc.getValue();

        // get aggregation scheme based on metric selected
        aggregationScheme = (AggregationScheme)
            AssessmentDataSource.aggregationSchemes.get(metricString);

        // Create query(s) that aggregate over organizations
        if ((orgDesc.getState() == VariableModel.FIXED) ||
            (selectedOrgNode.isLeaf()))
        {
            String query =
                generateSingleQuery(vim, selectedOrgNode, metricString);
            System.out.println(query);
            dbTableModel.setDBQuery(query, 4, 1);
        }
        else
        {
            for (int i = 0; i < selectedOrgNode.getChildCount(); i++)
            {
                String query =
                    generateSingleQuery(vim, selectedOrgNode.getChildAt(i),
                                        metricString);

                System.out.println("query #" + i + ": " + query);
                if (i == 0)
                {
                    dbTableModel.setDBQuery(query, 4, 1);
                }
                else
                {
                    dbTableModel.appendDBQuery(query, 4, 1);
                }
            }
        }

        // aggregation across a time range must be done at the client (here)
        if (debug) System.out.println("Aggregating across time range");
        VariableModel timeDescriptor = vim.getDescriptor("Time");
        if (timeDescriptor.getState() == VariableModel.FIXED)
        {
            RangeModel timeRange = (RangeModel)timeDescriptor.getValue();
            int timeHeaderColumn = dbTableModel.getColumnIndex("unitsOfTime");
            int[] significantColumns = {dbTableModel.getColumnIndex("org"),
                                        dbTableModel.getColumnIndex("item"),
                                        dbTableModel.getColumnIndex("metric")};
            DatabaseTableModel.Combiner timeCombiner =
              aggregationScheme.getCombiner(aggregationScheme.timeAggregation);
            dbTableModel.aggregateRows(significantColumns,
                                       timeRange.toString(),
                                       timeHeaderColumn,
                                       timeCombiner);
        }

        // transform based on needed X and Y variables
        if (debug) System.out.println("Transforming based on needed X/Y");
        String xDescName = xAxis.getName();
        String yDescName = yAxis.getName();
        String xColumnName = DBInterface.getColumnName(xDescName);
        String yColumnName = DBInterface.getColumnName(yDescName);
        String itemColumnName = DBInterface.getColumnName("Item");
        int[] yColumns =
            (vim.getDescriptor("Item").getState() == VariableModel.FIXED) ?
                new int[]{dbTableModel.getColumnIndex(yColumnName),
                          dbTableModel.getColumnIndex(itemColumnName)} :
                new int[]{dbTableModel.getColumnIndex(yColumnName)};
        dbTableModel.setXY(dbTableModel.getColumnIndex(xColumnName),
                           yColumns,
                           dbTableModel.getColumnIndex("assessmentValue"));

        // convert column and row header ids to names
        if (debug) System.out.println("Converting column headers to names");
        boolean aggregated = !xAxis.getName().equals("Item");
        convertColumnHeaderIDsToNames(xAxis, dbTableModel, yColumns.length,
                                      "ID", aggregated);
        if (debug) System.out.println("Converting row headers to names");
        aggregated = !yAxis.getName().equals("Item");
        convertRowHeaderIDsToNames(yAxis, dbTableModel, 0, "ID", aggregated);
        if (yColumns.length > 1)
        {
            convertRowHeaderIDsToNames(vim.getDescriptor("Item"), dbTableModel,
                                       1, "ID", false);
        }

        // weighted aggregation across items
        VariableModel itemDescriptor = vim.getDescriptor("Item");
        if (aggregationScheme.itemAggregation != AggregationScheme.NONE)
        {
            if (debug) System.out.println("Aggregating across items");
            DefaultMutableTreeNode selectedItemNode =
                (DefaultMutableTreeNode)itemDescriptor.getValue();

            if (itemDescriptor.getState() == VariableModel.FIXED)
            {
                aggregateItems(selectedItemNode, 1);
            }
            else
            {
                if (itemDescriptor.getState() == VariableModel.X_AXIS)
                {
                    dbTableModel.transpose();
                }
                for (int i = 0; i < selectedItemNode.getChildCount(); i++)
                {
                    aggregateItems((DefaultMutableTreeNode)
                        selectedItemNode.getChildAt(i), 0);
                }
                if (itemDescriptor.getState() == VariableModel.X_AXIS)
                {
                    dbTableModel.transpose();
                }
            }
        }
        // remove item column if it is there and not needed
        if ((itemDescriptor.getState() == VariableModel.FIXED) &&
            (aggregationScheme.itemAggregation != AggregationScheme.NONE))
        {
            dbTableModel.removeColumn(1);
        }

        // sort everything in a reasonable way
        sortRowsAndColumns(itemDescriptor);

        // derive unit column if needed
        String metric = vim.getDescriptor("Metric").getValue().toString();
        deriveUnitColumn(yDescName, metric);

        if (debug) System.out.println("Done.  Firing table change event");
        dbTableModel.fireTableChangedEvent(
            new TableModelEvent(dbTableModel, TableModelEvent.HEADER_ROW));

        postQueryRunnable.run();
    }

    private String generateSingleQuery(VariableInterfaceManager vim,
                                       TreeNode orgNode, String metricString)
    {
        String query = null;

        if (MetricInfo.isDerived(metricString))
        {
            int metricInt =
              AssessmentDataSource.getAllMetrics().indexOf(metricString);

            Stack stack = new Stack();
            Vector derivedMetricFormula =
              MetricInfo.getExpandedDerivedMetricFormula(metricString);
            for (int i = 0; i < derivedMetricFormula.size(); i++)
            {
                String item = (String)derivedMetricFormula.elementAt(i);

                if (AssessmentDataSource.getAllMetrics().contains(item))
                {
                    stack.push(
                        generateQueryUsingRootNode(vim, "Org", orgNode, item));
                }
                else if (MetricInfo.isUnaryOperator(item))
                {
                    if (item.equals(MetricInfo.CUMLATIVE_SUM))
                    {
                        String operand = (String)stack.pop();
                        stack.push(generateCumulativeSumQuery(operand));
                    }
                }
                else if (MetricInfo.isBinaryOperator(item))
                {
                    String operand2 = (String)stack.pop();
                    String operand1 = (String)stack.pop();
                    stack.push(
                        generateBinaryOperationQuery(operand1, operand2,
                                                     item, metricInt));
                }
                else
                {
                    System.err.println(
                        "Invalid symbol in derived metric: " + item);
                }
            }

            query = (String)stack.pop();
        }
        else
        {
            query = generateQueryUsingRootNode(vim, "Org", orgNode,
                                               metricString);
        }

        return query;
    }

    private String
        generateBinaryOperationQuery(String operand1, String operand2,
                                     String op, int metricInt)
    {
        String query = null;

        if (DBInterface.DBTYPE.equalsIgnoreCase("oracle"))
        {
            // if numerator data does not exist, assume 0
            query = "select t2.org, t2.item, t2.unitsOfTime, " + metricInt +
                " as METRIC, (NVL(t1.assessmentValue, 0)" + op +
                "t2.assessmentValue)" +
                " as \"ASSESSMENTVALUE\" from (" + operand1 + ") t1, (" +
                operand2 + ") t2 WHERE (t1.ORG (+) = t2.ORG and" +
                " t1.UnitsOfTime (+) = t2.UnitsOfTime and " +
                "t1.item (+) = t2.item and t2.assessmentValue <> 0)";
        }
        else
        {
            // if numerator data does not exist, NULL will be returned
            // (could not get Access to assume 0 as with Oracle)
            query = "select t2.org, t2.item, t2.unitsOfTime, " + metricInt +
                " as METRIC, (t1.assessmentValue" + op +
                "t2.assessmentValue) as \"ASSESSMENTVALUE\" from (" +
                operand1 + ") t1 RIGHT OUTER JOIN (" + operand2 +
                ") t2 ON (t1.ORG=t2.ORG and" +
                " t1.UnitsOfTime=t2.UnitsOfTime and t1.item=t2.item and" +
                " t2.assessmentValue<>0)";
        }

        return query;
    }

    private String generateCumulativeSumQuery(String baseQuery)
    {
        String query = null;

        //Base query must be modified to get values for all time up to max day.
        int startIndex = baseQuery.indexOf("unitsOfTime >=");
        int endIndex = baseQuery.indexOf("AND ", startIndex) + 4;
        String minTimeConstraint = baseQuery.substring(startIndex, endIndex);
        baseQuery = baseQuery.substring(0, startIndex) +
                    baseQuery.substring(endIndex);

        query = "select t1.org, t1.item, t1.unitsOfTime, t1.metric, " +
                "sum(t2.assessmentValue) as " +
                "\"ASSESSMENTVALUE\" from (" + baseQuery + ") t1, (" +
                baseQuery + ") t2 where (t1." + minTimeConstraint +
                "t1.ORG=t2.ORG and t1.UnitsOfTime>=t2.UnitsOfTime and " +
                "t1.item=t2.item and t1.metric=t2.metric) " +
                "group by t1.unitsOfTime, t1.item, t1.org, t1.metric";

        return query;
    }

    private String
        generateQueryUsingRootNode(VariableInterfaceManager vim,
                                   String varName, TreeNode tn, String metric)
    {
        StringBuffer query = new StringBuffer();

        // determine whether single or multiple metric
        if (metric.startsWith("Group"))
        {
            DefaultMutableTreeNode metricNode =
                (DefaultMutableTreeNode)vim.getDescriptor("Metric").getValue();
            Enumeration metrics = metricNode.children();
            while (metrics.hasMoreElements())
            {
                String childMetric = ((DefaultMutableTreeNode)
                    metrics.nextElement()).getUserObject().toString();
                query.append(
                    generateSingleQuery(vim, tn, childMetric));
                if (metrics.hasMoreElements())
                {
                    query.append(" UNION ALL ");
                }
            }
        }
        else
        {
            String org_id = ((Hashtable)
                             ((DefaultMutableTreeNode)
                                tn).getUserObject()).get("ID").toString();

            int metric_id =
              AssessmentDataSource.getAllMetrics().indexOf(metric);
            String metric_catalog_table = DBInterface.getTableName("Metric");
            String metric_table =
                DBInterface.lookupValue(metric_catalog_table,
                                        "name", "table_name", metric);

            query.append("SELECT ");
            query.append(org_id);
            query.append(" AS \"ORG\", item, unitsOfTime, ");
            query.append(metric_id);
            query.append(" as \"METRIC\", ");
            query.append(aggregationScheme.getSQLString(varName));
            query.append("(");
            query.append(metric_table);
            query.append(".assessmentValue) AS \"ASSESSMENTVALUE\" FROM ");
            query.append(metric_table);
            query.append(" WHERE (");

            // filter data needed based on org, item, metric, time
            if (debug) System.out.println("Generating Org where clause");
            query.append(
              generateWhereClause("Org",
                      TreeUtilities.getSubordinateList(tn, true).elements()));
            if (debug) System.out.println("Generating Item where clause");
            query.append(" AND " +
                         generateWhereClause(vim.getDescriptor("Item")));
            if (debug) System.out.println("Generating Time where clause");
            query.append(" AND " +
                         generateWhereClause(vim.getDescriptor("Time")));
            query.append(")");

            query.append(" GROUP BY item, unitsOfTime");
        }

        return query.toString();
    }

    /**
     * Generate a SQL where clause to constrain a result set based on the
     * contents of the given variable descriptor.
     *
     * @param v variable descriptor to used to formulate the where clause
     * @return the generated where clause
     */
    private String generateWhereClause(VariableModel v)
    {
        String whereClause = null;
        String varName = v.getName();

        if (varName.equals("Time")) // time is a special case
        {
            RangeModel timeRange = (RangeModel)v.getValue();
            StringBuffer timeWhereClause = new StringBuffer();
            timeWhereClause.append("(unitsOfTime >= ");
            timeWhereClause.append(timeRange.getMin());
            timeWhereClause.append(" AND unitsOfTime <= ");
            timeWhereClause.append(timeRange.getMax());
            timeWhereClause.append(")");
            whereClause = timeWhereClause.toString();
        }
        else if (v.getValue() instanceof DefaultMutableTreeNode)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)v.getValue();

            //get all required leaf nodes
            //would need to be modified to use aggregated values in database
            if (/*(v.getState() == v.FIXED) || */(node.isLeaf()))
            {
                whereClause = generateWhereClause(varName, node);
            }
            else
            {
                //Enumeration children = node.children(); for Agg. values in db
                Enumeration children =
                    TreeUtilities.getSubordinateList(node, true).elements();
                whereClause = generateWhereClause(varName, children);
            }
        }
        else
        {
            whereClause =generateWhereClause(varName, v.getValue().toString());
        }
        return whereClause;
    }

    private String generateWhereClause(String varName, Enumeration neededValues)
    {
        StringBuffer whereClause =  new StringBuffer();
        String columnName = DBInterface.getColumnName(varName);

        whereClause.append("(");

        Vector neededIDsVector = new Vector();
        while (neededValues.hasMoreElements())
        {
            DefaultMutableTreeNode n =
                (DefaultMutableTreeNode)neededValues.nextElement();
            int id = Integer.parseInt(
                ((Hashtable)n.getUserObject()).get("ID").toString());
            neededIDsVector.add(new Integer(id));
        }

        Collections.sort(neededIDsVector);
        for (int i = 0; i < neededIDsVector.size(); i++)
        {
            int id = ((Integer)neededIDsVector.elementAt(i)).intValue();
            neededIDsVector.setElementAt(new RangeModel(id, id), i);
        }

        // demo kludge
        String itemString =
            ((Hashtable)((DefaultMutableTreeNode)variableManagerKludgeHelper.
                getDescriptor("Item").getValue()).getUserObject()).get("UID").
                toString();
        if (varName.equals("Item") && itemString.equals("ALL_ITEMS"))
        {
            whereClause.append("0=0");
        }
        else
        {
            // find ranges of ids to compare against
            int runCount = 1;
            for (int run = 0; run < runCount; run++)
            {
                for (int i1 = 0; i1 < neededIDsVector.size(); i1++)
                {
                    for (int i2 = i1; i2 < neededIDsVector.size(); i2++)
                    {
                        RangeModel r1 = (RangeModel)neededIDsVector.elementAt(i1);
                        RangeModel r2 = (RangeModel)neededIDsVector.elementAt(i2);

                        if (r1.getMin() - 1 == r2.getMax())
                        {
                            r1.setMin(r2.getMin());
                            neededIDsVector.remove(i2);
                            i2--;
                        }
                        else if (r1.getMax() + 1 == r2.getMin())
                        {
                            r1.setMax(r2.getMax());
                            neededIDsVector.remove(i2);
                            i2--;
                        }
                    }
                }
            }
            for (int i = 0; i < neededIDsVector.size(); i++)
            {
                RangeModel r = (RangeModel)neededIDsVector.elementAt(i);

                if (r.getMin() == r.getMax())
                {
                    whereClause.append(columnName + " = " + r.getMin());
                }
                else
                {
                    whereClause.append("(");
                    whereClause.append(columnName + " >= " + r.getMin());
                    whereClause.append(" AND ");
                    whereClause.append(columnName + " <= " + r.getMax());
                    whereClause.append(")");
                }

                if (i < neededIDsVector.size() - 1)
                {
                    whereClause.append(" OR ");
                }
            }
        }
        whereClause.append(")");

        return whereClause.toString();
    }

    private static String generateWhereClause(String varName,
                                              String neededValue)
    {
        StringBuffer whereClause = new StringBuffer();
        String columnName = DBInterface.getColumnName(varName);

        whereClause.append(columnName);
        whereClause.append(" = ");
        if (varName.equalsIgnoreCase("time"))
        {
            whereClause.append(neededValue);
        }
        else
        {
            whereClause.append(
                DBInterface.lookupValue(DBInterface.getTableName(varName),
                                       "name", "id", neededValue));
        }

        return whereClause.toString();
    }

    private static String generateWhereClause(String varName,
                                              DefaultMutableTreeNode neededValue)
    {
        StringBuffer whereClause = new StringBuffer();
        String columnName = DBInterface.getColumnName(varName);

        whereClause.append(columnName);
        whereClause.append(" = ");
        whereClause.append(((Hashtable)neededValue.getUserObject()).get("ID"));
        return whereClause.toString();
    }

    /**
     * for debug
     */
    private static void printHashtable(Hashtable ht)
    {
        System.out.println("----------");
        Enumeration htKeys = ht.keys();
        while (htKeys.hasMoreElements())
        {
            Object key = htKeys.nextElement();
            System.out.println(key + ": " + ht.get(key));
        }
        System.out.println("----------");
    }
}