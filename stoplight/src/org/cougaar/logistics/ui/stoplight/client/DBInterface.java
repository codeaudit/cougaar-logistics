/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.logistics.ui.stoplight.client;

import java.util.Vector;

import org.cougaar.lib.uiframework.transducer.configs.SqlTableMap;
import org.cougaar.lib.uiframework.transducer.elements.Structure;

import org.cougaar.lib.uiframework.ui.util.DBDatasource;

/**
 * This class is used to extract data from a database that uses the blackjack
 * assessment schema.  The blackjack assessment schema defines four tables:
 * assessmentOgrs, assessmentItems, assessmentMetrics, assessmentValues.
 * The orgs, items, and metrics tables index into the values table.
 * The orgs and items table represent a hierarchy of data items.
 */
public class DBInterface extends DBDatasource
{
    private static final String itemUnitsTable = "assessmentItemUnits";
    //private static final String itemUnitsTable = "catalog_master";
    private static final String unitsColumn = "unit_issue";

    private static final String itemWeightsTable = "itemWeights";
    private static final String idColumn = "id";
    private static final String parentIdColumn = "parent_id";
    private static final String uidColumn = "item_id";
    private static final String commonNameColumn = "name";
    private static final String weightColumn = "weight";

    /** used to get min or max time for all metrics */
    public static int getTimeExt(boolean max)
    {
        Vector metricTables =
            executeVectorReturnQuery("SELECT table_name FROM " +
                                     getTableName("Metric"));
        int time = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        String function = max ? "MAX" : "MIN";
        for (int i = 0; i < metricTables.size(); i++)
        {
            int queryResult = getIntFromQuery("SELECT " + function + "(" +
                               getColumnName("Time") + ") FROM " +
                               metricTables.elementAt(i).toString());
            time = max?Math.max(queryResult, time):Math.min(queryResult, time);
        }

        return time;
    }

    /**
     * Gets tree representation of data in specified table.  Table must follow
     * a given schema.
     */
    public static Structure createTree(String table)
    {
        SqlTableMap config = new SqlTableMap();
        config.setDbTable(table);
        config.setIdKey("id");
        config.setParentKey("parent");
        config.addContentKey("UID", "name");
        config.addContentKey("ID", "id");
        //config.addContentKey("annotation", "note");
        config.setPrimaryKeys(new String[] {"keynum"});

        return restoreFromDb(config);
    }

    /**
     * Gets tree representation of data in specified table.  Table must follow
     * a given schema.
     */
    public static Structure createItemTree()
    {
        String itemTable = getTableName("item");
        SqlTableMap config = new SqlTableMap();
        config.setPrimaryTableName(itemTable);
        if (!DBTYPE.equalsIgnoreCase("access"))
        {
            config.setJoinConditions(/*"'NSN/'||" +*/
              itemUnitsTable + ".nsn(+)=" + itemTable + ".item_id");
            config.setDbTable(itemTable + ", " + itemUnitsTable);
        }
        else
        {
            config.setDbTable(itemTable +
                " LEFT JOIN " + itemUnitsTable + " ON " + itemUnitsTable +
                ".nsn =" + itemTable + ".item_id");
        }
        config.setIdKey(idColumn);
        config.setParentKey(parentIdColumn);
        config.addContentKey("UID", uidColumn);
        config.addContentKey("ID", idColumn);
        config.addContentKey("ITEM_ID", commonNameColumn);
        config.addContentKey("WEIGHT", weightColumn);
        config.addContentKey("UNITS", unitsColumn);
        config.setPrimaryKeys(new String[] {"keynum"});
        Structure trees = restoreFromDb(config);
        System.out.println("Item Tree Read from Database");
        return trees;
    }

    private static int getIntFromQuery(String query)
    {
        try
        {
            String intString = (String)
                DBInterface.executeVectorReturnQuery(query).firstElement();
            return Integer.parseInt(intString);
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    /**
     * get column name that corresponds to variable descriptor name
     *
     * @param variableDescriptorName name of variable descriptor
     * @return name of column that corresponds to variable desciptor name
     */
    public static String getColumnName(String variableDescriptorName)
    {
        String columnName;
        if (variableDescriptorName.equalsIgnoreCase("time"))
        {
            columnName = "unitsOfTime";
        }
        else
        {
            columnName = variableDescriptorName;
        }
        return columnName;
    }

    /**
     * get table name that corresponds to variable descriptor name
     *
     * @param variableDescriptorName name of variable descriptor
     * @return name of table that corresponds to variable desciptor name
     */
    public static String getTableName(String variableDescriptorName)
    {
        String tableName = null;

        if (!variableDescriptorName.equalsIgnoreCase("time"))
        {
            if (variableDescriptorName.equalsIgnoreCase("Item"))
            {
                tableName = itemWeightsTable;
            }
            else
            {
                tableName = "assessment" + variableDescriptorName + "s";
            }
        }
        return tableName;
    }
}