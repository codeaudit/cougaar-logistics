package org.cougaar.logistics.ui.stoplight.society;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.cougaar.lib.aggagent.query.Aggregator;
import org.cougaar.lib.aggagent.query.BatchAggregator;
import org.cougaar.lib.aggagent.query.CompoundKey;
import org.cougaar.lib.aggagent.query.DataAtomMelder;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;

import org.cougaar.logistics.ui.stoplight.client.AggregationScheme;

public abstract class ScheduleMelder implements DataAtomMelder
{
  private DataAtomMelder secondaryMelder = null;
  private Aggregator secondaryAggregator = null;
  private Object aggregationId = null;
  protected int aggregationMethod = 0;
  protected boolean completeAggregation = true;

  public ScheduleMelder(Object aggregationId)
  {
    this.aggregationId = aggregationId;

    List idList = new LinkedList();
    idList.add(GLMAtomTag.START_TIME_TAG);
    secondaryMelder = new SecondaryMelder();
    secondaryAggregator = new BatchAggregator(idList, secondaryMelder);
  }

  public void setAggregationMethod(int aggregationMethod)
  {
    this.aggregationMethod = aggregationMethod;
  }

  public void meld(List idNames, CompoundKey id, List atoms, List output)
  {
    // incoming atoms are correlated across all dimensions except for
    // dimension to aggregate over and time

    boolean timeIncluded = ((ResultSetDataAtom)atoms.iterator().next()).
                            getIdentifier(GLMAtomTag.START_TIME_TAG) != null;

    // expand all atoms such that there is one atom per day (no time ranges)
    Collection expandedAtoms = atoms;
    if (timeIncluded)
    {
      expandedAtoms=DataAtomUtilities.expandAllAtoms(atoms,new TimeExpander());
    }

    // call child class method that takes expanded atoms
    List aggregatedAtoms = new LinkedList();
    aggregatePartiallyCorrelated(timeIncluded, expandedAtoms, aggregatedAtoms);

    // compress all atoms into time ranges
    if (timeIncluded)
    {
      aggregatedAtoms =
        compressTime(aggregatedAtoms, completeAggregation, aggregationId);
    }

    output.addAll(aggregatedAtoms);
  }

  protected abstract void
    aggregatePartiallyCorrelated(boolean timeIncluded,
                                 Collection expandedAtoms, List output);

  protected void aggregate(boolean timeIncluded, Collection atoms, List output)
  {
    if (timeIncluded)
    {
      secondaryAggregator.aggregate(atoms.iterator(), output);
    }
    else
    {
      if (!atoms.isEmpty())
      {
        secondaryMelder.meld(null, null, new LinkedList(atoms), output);
      }
    }
  }

  private static class TimeExpander implements DataAtomUtilities.Expander
  {
    // expand all atoms such that there is one atom per day (no time ranges)
    public Collection expand(ResultSetDataAtom atom)
    {
      Collection newAtoms = new LinkedList();
      if (atom.getIdentifier(GLMAtomTag.END_TIME_TAG) == null)
      {
        newAtoms.add(atom);
      }
      else
      {
        int startTime = getInt(atom.getIdentifier(GLMAtomTag.START_TIME_TAG));
        int endTime = getInt(atom.getIdentifier(GLMAtomTag.END_TIME_TAG));
        if (startTime == endTime) endTime++;
        for (int day = startTime; day < endTime; day++)
        {
          ResultSetDataAtom newAtom = new ResultSetDataAtom(atom);
          newAtom.addIdentifier(GLMAtomTag.START_TIME_TAG, new Integer(day));
          newAtom.removeIdentifier(GLMAtomTag.END_TIME_TAG);
          newAtoms.add(newAtom);
        }
      }
      return newAtoms;
    }
  }

  private static List compressTime(List expandedAtoms,
                                   boolean completeAggregation,
                                   Object aggregationId)
  {
    // compress all atoms into time ranges
    List compressedAtoms = null;
    if (completeAggregation)
    {
      compressedAtoms = compressTime(expandedAtoms);
    }
    else if (!expandedAtoms.isEmpty())
    {
      compressedAtoms = new LinkedList();
      List subList = new LinkedList();
      Object idToMatch = ((ResultSetDataAtom)
        expandedAtoms.iterator().next()).getIdentifier(aggregationId);
      for (Iterator i = expandedAtoms.iterator(); i.hasNext();)
      {
        ResultSetDataAtom da = (ResultSetDataAtom)i.next();
        Object thisId = da.getIdentifier(aggregationId);
        if (thisId.equals(idToMatch))
        {
          subList.add(da);
        }
        else
        {
          compressedAtoms.addAll(compressTime(subList));
          subList = new LinkedList();
          subList.add(da);
          idToMatch = thisId;
        }
      }
      compressedAtoms.addAll(compressTime(subList));
    }

    return compressedAtoms;
  }

  private static List compressTime(List expandedAtoms)
  {
    List compressedAtoms = new LinkedList();

    // sort by time
    Collections.sort(expandedAtoms, new TimeComparator());

    int lastStartTime = 0;
    Object valueToMatch = null;
    ResultSetDataAtom compressedDataAtom = null;
    for (Iterator i = expandedAtoms.iterator(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      int thisStartTime =
        ((Integer)da.getIdentifier(GLMAtomTag.START_TIME_TAG)).intValue();
      Object thisValue = da.getValue(GLMAtomTag.VALUE_TAG);

      if (compressedDataAtom == null)
      {
        compressedDataAtom = da;
        valueToMatch = thisValue;
      }
      else if ((thisStartTime != lastStartTime + 1) ||
               !thisValue.equals(valueToMatch))
      {
        compressedDataAtom.addIdentifier(GLMAtomTag.END_TIME_TAG,
                                         new Integer(lastStartTime + 1));
        compressedAtoms.add(compressedDataAtom);
        compressedDataAtom = da;
        valueToMatch = thisValue;
      }

      lastStartTime = thisStartTime;
    }

    // last one
    if (compressedDataAtom != null)
    {
      compressedDataAtom.addIdentifier(GLMAtomTag.END_TIME_TAG,
                                       new Integer(lastStartTime + 1));
      compressedAtoms.add(compressedDataAtom);
    }

    return compressedAtoms;
  }

  private static class TimeComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      ResultSetDataAtom da1 = (ResultSetDataAtom)o1;
      ResultSetDataAtom da2 = (ResultSetDataAtom)o2;
      Integer daStart1 = (Integer)da1.getIdentifier(GLMAtomTag.START_TIME_TAG);
      Integer daStart2 = (Integer)da2.getIdentifier(GLMAtomTag.START_TIME_TAG);
      return daStart1.compareTo(daStart2);
    }
  }

  private static int getInt(Object intObject)
  {
    if (intObject instanceof Integer)
      return ((Integer)intObject).intValue();
    else
      return Integer.parseInt((String)intObject);
  }

  private class SecondaryMelder implements DataAtomMelder
  {
    public void meld(List idNames, CompoundKey id, List atoms, List output)
    {
      // incoming atoms are correlated across all dimensions except for org

      double aggValue = aggregationMethod == AggregationScheme.MIN ?
                        Double.MAX_VALUE : 0;
      double count = 0;
      ResultSetDataAtom da = null;
      for (Iterator i = atoms.iterator(); i.hasNext();)
      {
        da = (ResultSetDataAtom)i.next();

        // I'm not sure why the rate would ever be a String here
        // (but sometimes it is)
        Object valueObj = da.getValue(GLMAtomTag.VALUE_TAG);
        double value = 0;
        if (valueObj instanceof Double)
        {
          value = ((Double)valueObj).doubleValue();
        }
        else
        {
          value = Double.parseDouble((String)valueObj);
        }

        if (aggregationMethod == AggregationScheme.MIN)
        {
          aggValue = Math.min(aggValue, value);
        }
        else if (aggregationMethod == AggregationScheme.MAX)
        {
          aggValue = Math.max(aggValue, value);
        }
        else if (aggregationMethod == AggregationScheme.WAVG)
        {
          double weight = ((Double)da.getIdentifier("weight")).doubleValue();
          count += weight;
          aggValue += value * weight;
        }
        else
        {
          count++;
          aggValue += value;
        }
      }

      if ((aggregationMethod == AggregationScheme.AVG) ||
          (aggregationMethod == AggregationScheme.WAVG))
      {
        aggValue /= count;
      }

      ResultSetDataAtom aggregatedAtom = new ResultSetDataAtom(da);
      aggregatedAtom.addValue(GLMAtomTag.VALUE_TAG, new Double(aggValue));
      output.add(aggregatedAtom);
    }
  }

  protected static Vector convertToStringVector(Collection sourceCollection)
  {
    Vector stringVector = new Vector();
    for (Iterator i = sourceCollection.iterator(); i.hasNext();)
      stringVector.addElement(i.next().toString());
    return stringVector;
  }
}