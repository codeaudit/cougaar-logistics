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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.tree.*;

import org.w3c.dom.*;

import org.cougaar.lib.uiframework.ui.models.DatabaseTableModel;

public class AggregationScheme
{
    private final static String[] aggregationLabels =
        {"NONE", "SUM", "MIN", "MAX", "AVG", "Weighted AVG","Furthest from 1"};

    public final static int NONE = 0;
    public final static int SUM = 1;
    public final static int MIN = 2;
    public final static int MAX = 3;
    public final static int AVG = 4;
    public final static int WAVG = 5;
    public final static int FONE = 6;

    public int orgAggregation = NONE;
    public int timeAggregation = NONE;
    public int itemAggregation = NONE;

    public AggregationScheme(int orgAggregation, int timeAggregation,
                             int itemAggregation)
    {
        this.orgAggregation = orgAggregation;
        this.timeAggregation = timeAggregation;
        this.itemAggregation = itemAggregation;
    }

    public AggregationScheme(Element root)
    {
      orgAggregation = Integer.parseInt(root.getAttribute("org"));
      timeAggregation = Integer.parseInt(root.getAttribute("time"));
      itemAggregation = Integer.parseInt(root.getAttribute("item"));
    }

    public int getAggregationMethod(String aggregationString)
    {
        int aggMethod = 0;
        for (int i = 0; i < aggregationLabels.length; i++)
        {
            if (aggregationString.equals(aggregationLabels[i]))
            {
                aggMethod = i;
                break;
            }
        }
        return aggMethod;
    }

    public String getSQLString(String variable)
    {
        if (variable.equalsIgnoreCase("Org"))
        {
            return getSQLString(orgAggregation);
        }
        if (variable.equalsIgnoreCase("Time"))
        {
            return getSQLString(timeAggregation);
        }
        if (variable.equalsIgnoreCase("Item"))
        {
            return getSQLString(itemAggregation);
        }

        return null;
    }

    public String getSQLString(int aggMethod)
    {
        switch (aggMethod)
        {
            case SUM:  return "SUM";
            case MIN:  return "MIN";
            case MAX:  return "MAX";
            case AVG:  return "AVG";
       }

       return null;
    }

    public static String getLabelString(int aggMethod)
    {
        return aggregationLabels[aggMethod];
    }

    public DatabaseTableModel.Combiner getCombiner(int aggMethod)
    {
        switch (aggMethod)
        {
            case SUM: return new AdditiveTableCombiner();
            case MIN: return new MinTableCombiner();
            case MAX: return new MaxTableCombiner();
            case AVG: return new AverageTableCombiner();
            case WAVG: return new WeightedAverageTableCombiner(true);
            case FONE: return new FurthestFromOneTableCombiner();
        }

        return null;
    }

    private abstract class FloatTableCombiner
      implements DatabaseTableModel.Combiner
    {
        public Vector prepare(Vector row, int headerColumn)
        {
            return row;
        }

        public Object combine(Object obj1, Object obj2)
        {
            if ((obj1 == null) ||
                (obj1.toString().equals(DatabaseTableModel.NO_VALUE)))
                return obj2;
            Object combinedObject = null;
            if ((obj1 instanceof Float) && (obj2 instanceof Float))
            {
                float f1 = ((Float)obj1).floatValue();
                float f2 = ((Float)obj2).floatValue();
                combinedObject = new Float(combine(f1, f2));
            }
            else
            {
                combinedObject = obj1;
            }

            return combinedObject;
        }

        protected abstract float combine(float f1, float f2);

        public Vector finalize(Vector row, int headerColumn)
        {
            return row;
        }
    }

    private class FurthestFromOneTableCombiner extends FloatTableCombiner
    {
        protected float combine(float f1, float f2)
        {
            float f1Badness = Math.abs(f1 - 1);
            float f2Badness = Math.abs(f2 - 1);
            return (f1Badness > f2Badness) ? f1 : f2;
        }
    };

    private class MinTableCombiner extends FloatTableCombiner
    {
        protected float combine(float f1, float f2)
        {
            return Math.min(f1, f2);
        }
    }

    private class MaxTableCombiner extends FloatTableCombiner
    {
        protected float combine(float f1, float f2)
        {
            return Math.max(f1, f2);
        }
    }

    private class AdditiveTableCombiner extends FloatTableCombiner
    {
        protected float combine(float f1, float f2)
        {
            return f1 + f2;
        }
    };

    private class AverageTableCombiner extends AdditiveTableCombiner
    {
        private int numRowsCombined = 0;

        public Vector prepare(Vector row, int headerColumn)
        {
            numRowsCombined++;
            return row;
        }

        public Vector finalize(Vector row, int headerColumn)
        {
            for (int i = 0; i < row.size(); i++)
            {
                Object obj = row.elementAt(i);
                if (obj instanceof Float)
                {
                    row.setElementAt(
                       new Float((((Number)obj).floatValue())/numRowsCombined),
                       i);
                }
            }
            numRowsCombined = 0;
            return row;
        }
    }

    private class WeightedAverageTableCombiner extends AdditiveTableCombiner
    {
        private float totalWeight = 0;
        private boolean assumeNullEqZero = false;

        public WeightedAverageTableCombiner(boolean assumeNullEqZero)
        {
            this.assumeNullEqZero = assumeNullEqZero;
        }

        public Vector prepare(Vector row, int headerColumn)
        {
            DefaultMutableTreeNode headerNode =
                (DefaultMutableTreeNode)row.elementAt(headerColumn);
            String weightString =
              ((Hashtable)headerNode.getUserObject()).get("WEIGHT").toString();
            float weight = Float.parseFloat(weightString);
            totalWeight += weight;
            for (int i = 0; i < row.size(); i++)
            {
                Object obj = row.elementAt(i);
                if (obj instanceof Float)
                {
                    row.setElementAt(
                       new Float((((Number)obj).floatValue())*weight), i);
                }
            }

            return row;
        }

        public Vector finalize(Vector row, int headerColumn)
        {
            if (assumeNullEqZero)
            {
                totalWeight = 0;

                // find total weight under branch
                DefaultMutableTreeNode headerNode =
                    (DefaultMutableTreeNode)row.elementAt(headerColumn);
                Enumeration branchChildren = headerNode.children();
                while (branchChildren.hasMoreElements())
                {
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode)branchChildren.nextElement();
                    String weightString = ((Hashtable)
                        node.getUserObject()).get("WEIGHT").toString();
                    float weight = Float.parseFloat(weightString);
                    totalWeight += weight;
                }
            }

            for (int i = 0; i < row.size(); i++)
            {
                Object obj = row.elementAt(i);
                if (obj instanceof Float)
                {
                    row.setElementAt(
                       new Float((((Number)obj).floatValue())/totalWeight),
                       i);
                }
            }

            totalWeight = 0;
            return row;
        }
    }

    public String toXML()
    {
        StringBuffer xml = new StringBuffer();
        xml.append("<aggregation org=\"");
        xml.append(orgAggregation);
        xml.append("\" time=\"");
        xml.append(timeAggregation);
        xml.append("\" item=\"");
        xml.append(itemAggregation);
        xml.append("\"/>");
        return xml.toString();
    }
}