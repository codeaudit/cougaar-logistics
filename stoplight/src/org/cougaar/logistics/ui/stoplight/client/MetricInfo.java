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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.lib.uiframework.ui.util.SelectableHashtable;

import org.cougaar.logistics.ui.stoplight.society.ExtractionHelper;
import org.cougaar.logistics.ui.stoplight.society.InventoryMetric;

/**
 * This class provides knowledge about the metrics that should be included
 * in the assessmentMetrics table someday (e.g. metric units)
 */
public class MetricInfo
{
    public static final String ITEM_UNITS = "Items";
    public static final String ITEM_DAY_UNITS = "Items/Day";
    public static final String UNITLESS = "Unitless";

    public static final String GROUPA = "Group A";
    public static final String GROUPB = "Group B";
    public static final String GROUPC = "Group C";

    private static final boolean useAssessmentAgent = true;
    private static final String INV_SAF_METRIC = "Inventory Over Target Level";
    private static final String INV_CRITICAL_METRIC = "Inventory Over Critical Level";
    private static final String RES_DEM_METRIC = "Cumulative Resupply Over Cumulative Demand";
    private static final String DUE_IN_SHORTFALL_METRIC = "Due In Over Requested Due In";
    private static final String DUE_OUT_SHORTFALL_METRIC = "Due Out Over Requested Due Out";
    private static final String DUE_IN_SHORTFALL_QUANTITY_METRIC = "Requested Due In Minus Due In";
    private static final String DUE_OUT_SHORTFALL_QUANTITY_METRIC = "Requested Due Out Minus Due Out";
    private static final String DEMAND_METRIC = "Demand";
    private static final String DUEIN_METRIC = useAssessmentAgent ? "Due In" : "DueIn";
    private static final String DUEOUT_METRIC = useAssessmentAgent ? "Due Out" : "DueOut";
    private static final String INVENTORY_METRIC = "Inventory";
    private static final String CRITICAL_LEVEL_METRIC = "Critical Level";
    private static final String TARGET_LEVEL_METRIC = "Target Level";

    public static final String CUMLATIVE_SUM = "CUMULATIVE SUM";
    public static final String[] UNARY_OPS =
      useAssessmentAgent ? ExtractionHelper.UNARY_OPS :
                           new String[] {CUMLATIVE_SUM};
    public static final String[] BINARY_OPS =
      useAssessmentAgent ? ExtractionHelper.BINARY_OPS :
                           new String[] {"+", "-", "*", "/"};

    /** Hashtable that maps metric to metric units */
    public static Hashtable metricUnits = createMetricUnits();

    /** Hashtable that describes derived metrics */
    public static Hashtable derivedMetrics = createDerivedMetrics();

    public static Hashtable createMetricUnits()
    {
        Hashtable mu = new Hashtable();

        if (useAssessmentAgent)
        {
          for (Iterator i = InventoryMetric.getValidValues().iterator();
               i.hasNext();)
          {
            InventoryMetric metric = (InventoryMetric)i.next();
            mu.put(metric.toString(), InventoryMetric.usesItemUnits(metric) ?
                                      ITEM_UNITS : ITEM_DAY_UNITS);
          }
          mu.put(DUE_IN_SHORTFALL_METRIC, UNITLESS);
          mu.put(DUE_OUT_SHORTFALL_METRIC, UNITLESS);
          mu.put(DUE_IN_SHORTFALL_QUANTITY_METRIC, ITEM_UNITS);
          mu.put(DUE_OUT_SHORTFALL_QUANTITY_METRIC, ITEM_UNITS);
        }
        else
        {
          mu.put(RES_DEM_METRIC, UNITLESS);
        }

        mu.put(DUEIN_METRIC, ITEM_DAY_UNITS);
        mu.put(DUEOUT_METRIC, ITEM_DAY_UNITS);
        mu.put(INVENTORY_METRIC, ITEM_UNITS);
        mu.put(DEMAND_METRIC, ITEM_DAY_UNITS);
        mu.put(TARGET_LEVEL_METRIC, ITEM_UNITS);
        mu.put(CRITICAL_LEVEL_METRIC, ITEM_UNITS);
        mu.put(INV_SAF_METRIC, UNITLESS);
        mu.put(INV_CRITICAL_METRIC, UNITLESS);

        mu.put(GROUPA, ITEM_UNITS);
        mu.put(GROUPB, ITEM_DAY_UNITS);
        mu.put(GROUPC, UNITLESS);

        return mu;
    }

    public static Hashtable createDerivedMetrics()
    {
        Hashtable dm = new Hashtable();
        Vector f = null;

        if (useAssessmentAgent)
        {
          f = new Vector();
          f.add(InventoryMetric.REQUESTED_DUE_OUT.toString());
          f.add(InventoryMetric.PROJECTED_REQUESTED_DUE_OUT.toString());
          f.add("+");
          dm.put(DEMAND_METRIC, f);

          f = new Vector();
          f.add(DEMAND_METRIC);
          f.add("3");
          f.add(ExtractionHelper.WINDOWED_SUM);
          dm.put(TARGET_LEVEL_METRIC, f);

          f = new Vector();
          f.add(DEMAND_METRIC);
          f.add("1");
          f.add(ExtractionHelper.WINDOWED_SUM);
          dm.put(CRITICAL_LEVEL_METRIC, f);

          f = new Vector();
          f.add(InventoryMetric.DUE_IN.toString());
          f.add(InventoryMetric.PROJECTED_DUE_IN.toString());
          f.add("+");
          f.add(InventoryMetric.REQUESTED_DUE_IN.toString());
          f.add(InventoryMetric.PROJECTED_REQUESTED_DUE_IN.toString());
          f.add("+");
          f.add("/");
          dm.put(DUE_IN_SHORTFALL_METRIC, f);

          f = new Vector();
          f.add(InventoryMetric.DUE_OUT.toString());
          f.add(InventoryMetric.PROJECTED_DUE_OUT.toString());
          f.add("+");
          f.add(DEMAND_METRIC);
          f.add("/");
          dm.put(DUE_OUT_SHORTFALL_METRIC, f);

          f = new Vector();
          f.add(InventoryMetric.REQUESTED_DUE_IN.toString());
          f.add(InventoryMetric.PROJECTED_REQUESTED_DUE_IN.toString());
          f.add("+");
          f.add(InventoryMetric.DUE_IN.toString());
          f.add(InventoryMetric.PROJECTED_DUE_IN.toString());
          f.add("+");
          f.add("-");
          dm.put(DUE_IN_SHORTFALL_QUANTITY_METRIC, f);

          f = new Vector();
          f.add(DEMAND_METRIC);
          f.add(InventoryMetric.DUE_OUT.toString());
          f.add(InventoryMetric.PROJECTED_DUE_OUT.toString());
          f.add("+");
          f.add("-");
          dm.put(DUE_OUT_SHORTFALL_QUANTITY_METRIC, f);

        }

        f = new Vector();
        f.add(INVENTORY_METRIC);
        f.add(TARGET_LEVEL_METRIC);
        f.add("/");
        dm.put(INV_SAF_METRIC, f);

        f = new Vector();
        f.add(INVENTORY_METRIC);
        f.add(CRITICAL_LEVEL_METRIC);
        f.add("/");
        dm.put(INV_CRITICAL_METRIC, f);

        if (!useAssessmentAgent)
        {
          f = new Vector();
          f.add(DUEIN_METRIC);
          f.add(CUMLATIVE_SUM);
          f.add(DEMAND_METRIC);
          f.add(CUMLATIVE_SUM);
          f.add("/");
          dm.put(RES_DEM_METRIC, f);
        }

        return dm;
    }

    public static boolean isDerived(String metric)
    {
        return derivedMetrics.containsKey(metric);
    }

    public static boolean isUnaryOperator(String s)
    {
        boolean isUnary = false;

        for (int i = 0; i < UNARY_OPS.length; i++)
        {
            if (UNARY_OPS[i].equals(s))
            {
                isUnary = true;
                break;
            }
        }

        return isUnary;
    }

    public static boolean isBinaryOperator(String s)
    {
        boolean isBinary = false;

        for (int i = 0; i < BINARY_OPS.length; i++)
        {
            if (BINARY_OPS[i].equals(s))
            {
                isBinary = true;
                break;
            }
        }

        return isBinary;
    }

    public static Vector getExpandedDerivedMetricFormula(String metric)
    {
        Vector metricFormula = (Vector)derivedMetrics.get(metric);
        Vector newFormula = new Vector();
        for (int i = 0; i < metricFormula.size(); i++)
        {
            String item = (String)metricFormula.elementAt(i);
            if (isDerived(item))
            {
                newFormula.addAll(getExpandedDerivedMetricFormula(item));
            }
            else
            {
                newFormula.add(item);
            }
        }
        return newFormula;
    }
}