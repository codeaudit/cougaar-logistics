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
package org.cougaar.logistics.ui.stoplight.society;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;

import org.cougaar.core.util.UID;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.plan.NewQuantityScheduleElement;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.InventoryLevelsPG;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.FlowRate;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.RelationshipImpl;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleElementType;
import org.cougaar.planning.ldm.plan.ScheduleElementWithValue;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.Thunk;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryPG;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryBG;

import org.cougaar.lib.aggagent.query.ResultSetDataAtom;

/**
 * This class will be used in increment format scripts to assist with
 * extracting metric data out of clusters.
 */
public class ExtractionHelper
{
  private final static long cTime = extractCDateFromSociety();
  private final static long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
  public static final String TRIM_TO_SCHEDULE = "TRIM TO SCHEDULE";
  public static final String WINDOWED_SUM = "WINDOWED SUM";
  public static final String AVERAGE = "AVERAGE";
  public static final String[] UNARY_OPS = {};
  public static final String[] BINARY_OPS =
    {"+", "-", "/", WINDOWED_SUM, AVERAGE, TRIM_TO_SCHEDULE};

  private static HashMap metricMethodMap = new HashMap();

  static
  {
    // create mapping from inventory metric to InventoryWrapper method name
    for (Iterator m=InventoryMetric.getValidValues().iterator(); m.hasNext();)
    {
      InventoryMetric metric = (InventoryMetric)m.next();
      String metricString = metric.toString();
      StringBuffer method = new StringBuffer("get");

      for (int i = 0; i < metricString.length(); i++)
      {
        char character = metricString.charAt(i);
        if (character != ' ')
        {
          method.append(character);
        }
      }
      method.append("Schedule");
      metricMethodMap.put(metric, method.toString());
    }
  }

  public static Collection getOrgRelationships(Organization org)
  {
    Collection relations = new LinkedList();
    if (org.isLocal())
    {
      RelationshipSchedule rs = org.getRelationshipSchedule();
      for (Enumeration elements = rs.getAllScheduleElements();
           elements.hasMoreElements();)
      {
        RelationshipImpl r = (RelationshipImpl)elements.nextElement();
        ResultSetDataAtom da = new ResultSetDataAtom();
        da.addIdentifier(GLMAtomTag.START_TIME_TAG,
                         new Integer(convertMSecToCDate(r.getStartTime())));
        da.addIdentifier(GLMAtomTag.END_TIME_TAG,
                         new Integer(convertMSecToCDate(r.getEndTime())));
        da.addIdentifier(GLMAtomTag.RELATIONSHIP_TYPE_TAG,
                         rs.getOtherRole(r).getName());
        da.addValue(GLMAtomTag.OTHER_ORG_TAG, ((Organization)
          rs.getOther(r)).getItemIdentificationPG().getItemIdentification());
        relations.add(da);
      }
    }
    return relations;
  }

  public static ResultSetDataAtom getDemand(Task task)
  {
    // Pull out the start time and put it in a string
    Preference start_pref = task.getPreference (AspectType.START_TIME);
    Integer start_time = new Integer(getBestCTime(start_pref));

    // Pull out the end time and put it in a string
    Preference end_pref = task.getPreference (AspectType.END_TIME);
    Integer end_time = new Integer(getBestCTime(end_pref));

    // Find the organization in the "For" preposition
    PrepositionalPhrase for_phrase = task.getPrepositionalPhrase ("For");
    String org = (String) for_phrase.getIndirectObject();
    if (org.startsWith ("UIC/"))
    {
      org = org.substring (4);
    }

    // check to see if this task belongs to this cluster
    try {
      String ownerUID = task.getPlanElement().getUID().getOwner();
      if (!org.equals(ownerUID))
      {
        // This task does not belong to this cluster.  Ignore.
        return null;
      }
    } catch (Exception e) {
      // Couldn't even check the owner. Ignore.
      return null;
    }

    // Find the item name in direct object's type identification
    String item = getItemUID(task.getDirectObject());

    // Find what the demand rate is and put it in a string
    Rate demand_rate = TaskUtils.getRate(task);
    Double rate = null;
    if ((demand_rate != null) && (demand_rate instanceof CountRate)) {
      CountRate cr = (CountRate) demand_rate;
      rate = new Double(cr.getEachesPerDay());
    }
    else if ((demand_rate != null) && (demand_rate instanceof FlowRate)) {
      FlowRate fr = (FlowRate) demand_rate;
      rate = new Double(fr.getGallonsPerDay());
    }
    else
    {
      Logger logger = Logging.currentLogger();
      if (logger.isWarnEnabled())
        logger.warn("No rate for org "+org+", item " + item);
      return null;
    }

    ResultSetDataAtom dataAtom = new ResultSetDataAtom();
    dataAtom.addIdentifier(GLMAtomTag.ITEM_TAG, item);
    dataAtom.addIdentifier(GLMAtomTag.START_TIME_TAG, start_time);
    dataAtom.addIdentifier(GLMAtomTag.END_TIME_TAG, end_time);
    dataAtom.addValue(GLMAtomTag.VALUE_TAG, rate);

    return dataAtom;
  }

  public static Collection getInventory(Inventory inventory)
  {
    ScheduledContentPG scheduled_content = inventory.getScheduledContentPG();
    if (scheduled_content == null)
      return null;
    Schedule inventory_schedule = scheduled_content.getSchedule();
    inventory_schedule = tryToCombineLikeQuantityElements(inventory_schedule);
    return createInventoryScheduleDataAtoms(inventory, inventory_schedule);
  }

  public static Collection getAverageDemand(Inventory inventory)
  {
    // get average demand schedule
    InventoryLevelsPG invLevPG = inventory.getInventoryLevelsPG();
    Schedule demand_schedule = invLevPG.getAverageDemandSchedule();
    demand_schedule = tryToCombineLikeQuantityElements(demand_schedule);
    return createInventoryScheduleDataAtoms(inventory, demand_schedule);
  }

  public static Collection getReorderSchedule(Inventory inventory)
  {
    // get reorder schedule
    InventoryLevelsPG invLevPG = inventory.getInventoryLevelsPG();
    Schedule reorder_schedule = invLevPG.getReorderLevelSchedule();
    reorder_schedule = tryToCombineLikeQuantityElements(reorder_schedule);
    return createInventoryScheduleDataAtoms(inventory, reorder_schedule);
  }

  public static Collection getGoalSchedule(Inventory inventory)
  {
    // get goal schedule
    InventoryLevelsPG invLevPG = inventory.getInventoryLevelsPG();
    Schedule goal_schedule = invLevPG.getGoalLevelSchedule();
    goal_schedule = tryToCombineLikeQuantityElements(goal_schedule);
    return createInventoryScheduleDataAtoms(inventory, goal_schedule);
  }

  /**
   * Used in unary predicates for collecting plan objects required for creating
   * InventoryWrapper objects.
   *
   * @param assets collection of asset type id strings that are of interest.
   *               if null, all assets are of interest.
   * @param o plan object to check
   * @return true if plan object contains inventory related information for
   *              one of the assets of interest.
   */
  public static boolean checkInventoryRelated(Collection assets, Object o)
  {

    if (!(o instanceof Inventory))
      return false;
      
    Inventory inv = (Inventory)o;
    LogisticsInventoryPG logInvPG=null;
    logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
    if (logInvPG == null)
      return false;
    Asset a1 = logInvPG.getResource();
    if (a1 == null) {
      System.err.println("no asset in Inventory in checkInventoryRelated");
      return false;
    }
    TypeIdentificationPG typeIdPG = a1.getTypeIdentificationPG();
    if (typeIdPG == null) {
      System.err.println(" No typeIdentificationPG for asset");
      return false;
    }
    
    return assets == null ||
           assets.contains(typeIdPG.getTypeIdentification());
  }

  /** Called after submitting a subscription; the container
    contains the assets we need to compute the inventory
    object that we'll return to the client.
    */
  public static Collection getInventoriesFromLogPlan(Collection container) {
    Collection inventories = new LinkedList();

    for (Iterator i = container.iterator(); i.hasNext();)
    {
      Object obj = i.next();
      if (!(obj instanceof Inventory))
        continue;
      InventoryWrapper inventory = new InventoryWrapper((Inventory)obj);
      inventories.add(inventory);
    }

    return inventories;
  }

  // inventories is a collection of InventoryWrapper objects
  public static Collection
    getInventorySchedules(String metricName, Collection inventories)
      throws Exception
  {
    Collection dataAtomCollection = new LinkedList();
    for (Iterator i= inventories.iterator(); i.hasNext();)
    {
      InventoryWrapper inv = (InventoryWrapper)i.next();
      inv.computeSimulatedProjectionSchedules();
      Schedule schedule = getInventorySchedule(metricName, inv);
      dataAtomCollection.addAll(
        createInventoryScheduleDataAtoms((Inventory)inv.getInventory(), schedule));
    }
    return dataAtomCollection;
  }

  public static Collection
    calculateDerivedSchedules(Vector derivedMetricFormula,
                              Collection inventories) throws Exception
  {
    return
      calculateDerivedSchedules(derivedMetricFormula, inventories, null, null,
                                null);
  }

  public static Collection
    calculateDerivedSchedules(Vector derivedMetricFormula,
                              Collection inventories, Integer startCTime,
                              Integer endCTime, String timeAggregation)
                              throws Exception
  {
    boolean filterTime = ((startCTime != null) && (endCTime != null));

    // Convert metric strings in derived metric formula to InventoryMetric
    // objects
    Vector convertedDerivedMetricFormula = new Vector();
    for (int i = 0; i < derivedMetricFormula.size(); i++)
    {
      String itemString = (String)derivedMetricFormula.elementAt(i);
      InventoryMetric metric = InventoryMetric.fromString(itemString);
      if (metric == null)
      {
        convertedDerivedMetricFormula.addElement(itemString);
      }
      else
      {
        convertedDerivedMetricFormula.addElement(metric);
      }
    }

    Collection dataAtomCollection = new LinkedList();
    for (Iterator i= inventories.iterator(); i.hasNext();)
    {
      InventoryWrapper inv = (InventoryWrapper)i.next();
      inv.computeSimulatedProjectionSchedules();

      Stack stack = new Stack();
      for (int index = 0; index < convertedDerivedMetricFormula.size();index++)
      {
        Object item = convertedDerivedMetricFormula.elementAt(index);

        if (item instanceof InventoryMetric)
        {
          stack.push(getInventorySchedule((InventoryMetric)item, inv));
        }
        else if (isUnaryOperator((String)item))
        {
          //if (item.equals(TRIM_TO_MOCK_INVENTORY))
          //{
          //  Object operand = (String)stack.pop();
          //  stack.push(trimToMockInventory(operand));
          //}
        }
        else if (isBinaryOperator((String)item))
        {
          Object operand2 = stack.pop();
          Object operand1 = stack.pop();
          stack.push(applyBinaryOperation(operand1, operand2, (String)item));
        }
        else
        {
          stack.push(item);
        }
      }

      Schedule finalSchedule = (Schedule)stack.pop();

      // simplify
      finalSchedule = tryToCombineLikeQuantityElements(finalSchedule);

      Collection dataAtoms =
        createInventoryScheduleDataAtoms((Inventory)inv.getInventory(),
                                         finalSchedule);

      if (filterTime)
      {
        // filter based on time
        dataAtoms = filterTimePeriod(dataAtoms, startCTime.intValue(),
                                     endCTime.intValue());

        if (timeAggregation != null)
        {
          dataAtoms =
            aggregateScheduleOverTime(dataAtoms, startCTime.intValue(),
                                      endCTime.intValue(), timeAggregation);
        }
      }

      dataAtomCollection.addAll(dataAtoms);
    }
    return dataAtomCollection;
  }

  private static Collection
    aggregateScheduleOverTime(Collection elements, int aggStartTime,
                              int aggEndTime, String aggregationMethod)
  {
    if (elements.isEmpty())
      return elements;

    boolean min = aggregationMethod.equals("MIN");
    boolean max = aggregationMethod.equals("MAX");
    Object aggItem = ((ResultSetDataAtom)elements.iterator().next()).
                      getIdentifier(GLMAtomTag.ITEM_TAG);
    double aggValue = min ? Double.MAX_VALUE : 0;
    int totalDays = 0;
    for (Iterator i = elements.iterator(); i.hasNext();)
    {
      ResultSetDataAtom element = (ResultSetDataAtom)i.next();
      int startTime=
        ((Number)element.getIdentifier(GLMAtomTag.START_TIME_TAG)).intValue();
      int endTime =
        ((Number)element.getIdentifier(GLMAtomTag.END_TIME_TAG)).intValue();
      double value =
        ((Number)element.getValue(GLMAtomTag.VALUE_TAG)).doubleValue();

      if (min)
      {
        aggValue = Math.min(aggValue, value);
      }
      else if (max)
      {
        aggValue = Math.max(aggValue, value);
      }
      else
      {
        int days = endTime - startTime;
        if (days == 0) days = 1;
        totalDays += days;
        aggValue += days * value;
      }
    }
    Collection aggregated = new LinkedList();
    ResultSetDataAtom aggDataAtom = new ResultSetDataAtom();
    if (aggregationMethod.equals("AVG"))
    {
      //use commented out code to calculate total days if you want to count
      //non-existent data as zeros.
      //int totalDays = aggEndTime - aggStartTime + 1;
      aggValue = aggValue / totalDays;
    }
    aggDataAtom.addIdentifier(GLMAtomTag.ITEM_TAG, aggItem);
    aggDataAtom.addValue(GLMAtomTag.VALUE_TAG, new Double(aggValue));
    aggregated.add(aggDataAtom);

    return aggregated;
  }

  private static boolean isUnaryOperator(String s)
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

  private static boolean isBinaryOperator(String s)
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

  private static Schedule
    applyBinaryOperation(Object operand1, Object operand2, String operation)
  {
    Schedule schedule = null;

    if (operand1 instanceof String)
    {
        schedule =
          applyScalar(operation, (Schedule)operand2, (String)operand1, true);
    }
    else if (operand2 instanceof String)
    {
        schedule =
          applyScalar(operation, (Schedule)operand1, (String)operand2, false);
    }
    else if (operation.equals("+"))
    {
      schedule =
        ScheduleUtilities.addSchedules((Schedule)operand1, (Schedule)operand2);

      // workaround to bug #606
      schedule =
        setScheduleElementType(schedule,
                               ((Schedule)operand1).getScheduleElementType());
    }
    else if (operation.equals("-"))
    {
      schedule = ScheduleUtilities.subtractSchedules((Schedule)operand1,
                                                     (Schedule)operand2);

      // workaround to bug #606
      schedule =
        setScheduleElementType(schedule,
                               ((Schedule)operand1).getScheduleElementType());
    }
    else if (operation.equals("/"))
    {
      schedule =
        divideSchedules((Schedule)operand1, (Schedule)operand2, false);
    }
    else if (operation.equals(AVERAGE))
    {
      schedule = averageSchedules((Schedule)operand1, (Schedule)operand2);
    }
    else if (operation.equals(TRIM_TO_SCHEDULE))
    {
      schedule = trimToSchedule((Schedule)operand1, (Schedule)operand2);
    }
    else
    {
      Logger logger = Logging.currentLogger();
      if (logger.isErrorEnabled())
        logger.error(operation + " is not yet supported");
    }
    return schedule;
  }

  private static Schedule
    applyScalar(String operation, Schedule schedule, String scalar,
                boolean scalarFirst)
  {
    Schedule resultSchedule = null;

    if (operation.equals(WINDOWED_SUM))
    {
      int windowSize = Integer.parseInt(scalar);
      resultSchedule =
        applyWindowedSumToDiscreteSchedule(schedule, windowSize, true);
    }
    else if (operation.equals("+") ||
             operation.equals("-") ||
             operation.equals("/"))
    {
      ScalarOp so = null;
      if (operation.equals("+"))
      {
        so = new ScalarOp() {
            public double applyOp(double operand1, double operand2)
            {
              return operand1 + operand2;
            }
          };
      }
      else if (operation.equals("-"))
      {
        so = new ScalarOp() {
            public double applyOp(double operand1, double operand2)
            {
              return operand1 - operand2;
            }
          };
      }
      else if (operation.equals("/"))
      {
        so = new ScalarOp() {
            public double applyOp(double operand1, double operand2)
            {
              return operand1 / operand2;
            }
          };
      }

      double scalarValue = Double.parseDouble(scalar);
      ScheduleImpl newSchedule = new ScheduleImpl();
      newSchedule.setScheduleType(schedule.getScheduleType());
      newSchedule.setScheduleElementType(schedule.getScheduleElementType());
      for (Enumeration elements = schedule.getAllScheduleElements();
           elements.hasMoreElements(); )
      {
        ScheduleElementWithValue element =
          (ScheduleElementWithValue)elements.nextElement();
        NewQuantityScheduleElement qse=GLMFactory.newQuantityScheduleElement();
        qse.setStartTime(element.getStartTime());
        qse.setEndTime(element.getEndTime());
        double value = (scalarFirst) ?
          so.applyOp(scalarValue, element.getValue()) :
          so.applyOp(element.getValue(), scalarValue);
        qse.setQuantity(Double.isInfinite(value) ? Double.MAX_VALUE : value);
        newSchedule.addScheduleElement(qse);
      }
      resultSchedule = newSchedule;
    }
    else
    {
      Logger logger = Logging.currentLogger();
      if (logger.isErrorEnabled())
        logger.error(operation + " is not yet supported for scalars");
    }

    return resultSchedule;
  }

  private static interface ScalarOp
  {
      double applyOp(double operand1, double operand2);
  }

  private static double getValueAtTime(Schedule schedule, long time)
  {
    Collection elements =
      schedule.getScheduleElementsWithTime(time);
    if (elements.isEmpty())
      return 0;;
    ScheduleElementWithValue element =
      (ScheduleElementWithValue)elements.iterator().next();
    return element.getValue();
  }

  private static Schedule
    trimToSchedule(Schedule modelSchedule, Schedule scheduleToTrim)
  {
    // find start of non-zero in model
    long modStart = 0;
    for (Enumeration modElements = modelSchedule.getAllScheduleElements();
         modElements.hasMoreElements();)
    {
      ScheduleElementWithValue modElement =
        (ScheduleElementWithValue)modElements.nextElement();
      if (modElement.getValue() > 0)
      {
        modStart = modElement.getStartTime();
        break;
      }
    }

    ScheduleImpl cropped_schedule = new ScheduleImpl();
    cropped_schedule.setScheduleType(scheduleToTrim.getScheduleType());
    cropped_schedule.
      setScheduleElementType(scheduleToTrim.getScheduleElementType());
    cropped_schedule.addAll(scheduleToTrim.getEncapsulatedScheduleElements(
      modStart, modelSchedule.getEndTime()));
    return cropped_schedule;
  }

  /** not currently being used */
  private static Schedule
    applyWindowedSum(Schedule sourceSchedule, int windowSize)
  {
    // not worrying about efficiency yet
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleType(sourceSchedule.getScheduleType());
    schedule.setScheduleElementType(sourceSchedule.getScheduleElementType());
    if (sourceSchedule.isEmpty())
      return schedule;

    for (long time=sourceSchedule.getStartTime()-(windowSize * MILLIS_IN_DAY);
         time < sourceSchedule.getEndTime();
         time += MILLIS_IN_DAY)
    {
      double windowedSum = 0;
      for (int day = 0; day < windowSize; day++)
      {
        windowedSum +=
          getValueAtTime(sourceSchedule, time + (day * MILLIS_IN_DAY));
      }

      NewQuantityScheduleElement qse = GLMFactory.newQuantityScheduleElement();
      qse.setStartTime(time);
      qse.setEndTime(time + MILLIS_IN_DAY);
      qse.setQuantity(windowedSum);
      schedule.addScheduleElement(qse);
    }

    return schedule;
  }

  private static Schedule
    applyWindowedSumToDiscreteSchedule(Schedule discreteSourceSchedule,
                                       int windowSize, boolean returnDiscrete)
  {
    // not worrying about efficiency yet
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleType(discreteSourceSchedule.getScheduleType());
    schedule.setScheduleElementType(discreteSourceSchedule.getScheduleElementType());
    if (discreteSourceSchedule.isEmpty())
      return schedule;

    // dump schedule into vector for easy indexing
    Vector sourceVector = new Vector();
    Enumeration sourceElements =
      discreteSourceSchedule.getAllScheduleElements();
    while (sourceElements.hasMoreElements())
    {
      sourceVector.add(sourceElements.nextElement());
    }

    for (int day = 1 - windowSize; day < sourceVector.size(); day++)
    {
      double windowedSum = 0;
      int startWindowDay = (day < 0) ? 0 : day;
      int endWindowDay = Math.min(sourceVector.size(), day + windowSize);
      for (int windowIndex = startWindowDay;
           windowIndex < endWindowDay; windowIndex++)
      {
        windowedSum += ((ScheduleElementWithValue)
          sourceVector.elementAt(windowIndex)).getValue();
      }

      NewQuantityScheduleElement qse = GLMFactory.newQuantityScheduleElement();
      long startTime = 0;
      long endTime = 0;
      if (day < 0)
      {
        ScheduleElementWithValue sourceElement =
          (ScheduleElementWithValue)sourceVector.elementAt(0);
        long timeOffset = (day * MILLIS_IN_DAY);
        startTime = sourceElement.getStartTime() + timeOffset;
        endTime = sourceElement.getEndTime() + timeOffset;
      }
      else
      {
        ScheduleElementWithValue sourceElement =
          (ScheduleElementWithValue)sourceVector.elementAt(day);
        startTime = sourceElement.getStartTime();
        endTime = sourceElement.getEndTime();
      }
      qse.setStartTime(startTime);
      if (returnDiscrete)
        qse.setEndTime(endTime);
      else
        qse.setEndTime(startTime + MILLIS_IN_DAY);
      qse.setQuantity(windowedSum);
      schedule.addScheduleElement(qse);
    }

    return schedule;
  }

  private static Schedule divideSchedules(Schedule numeratorSchedule,
                                          Schedule denominatorSchedule,
                                          boolean returnDiscrete)
  {
    // not worrying about efficiency yet
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleType(numeratorSchedule.getScheduleType());
    schedule.setScheduleElementType(numeratorSchedule.getScheduleElementType());
    if (numeratorSchedule.isEmpty() || denominatorSchedule.isEmpty())
      return schedule;

    // ensure that denominator is discrete
    denominatorSchedule = makeDiscrete(denominatorSchedule);

    // iterate through denominator (denominator is discrete)
    for (Enumeration elements = denominatorSchedule.getAllScheduleElements();
         elements.hasMoreElements(); )
    {
      ScheduleElementWithValue element =
        (ScheduleElementWithValue)elements.nextElement();
      long startTime = element.getStartTime();
      double numeratorValue = getValueAtTime(numeratorSchedule, startTime);
      NewQuantityScheduleElement qse = GLMFactory.newQuantityScheduleElement();
      qse.setStartTime(startTime);
      if (returnDiscrete)
        qse.setEndTime(element.getEndTime());
      else
        qse.setEndTime(startTime + MILLIS_IN_DAY);
      double ratio = numeratorValue / element.getValue();
      qse.setQuantity(Double.isInfinite(ratio) ? Double.MAX_VALUE : ratio);
      schedule.addScheduleElement(qse);
    }

    return schedule;
  }

  private static Schedule makeDiscrete(Schedule s)
  {
    ScheduleElementWithValue firstElement = (ScheduleElementWithValue)
      s.getAllScheduleElements().nextElement();

    // already discrete?
    if ((firstElement.getStartTime() + 1) == firstElement.getEndTime())
      return s;

    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleType(s.getScheduleType());
    schedule.setScheduleElementType(s.getScheduleElementType());
    for (Enumeration e = s.getAllScheduleElements(); e.hasMoreElements();)
    {
      ScheduleElementWithValue se = (ScheduleElementWithValue)e.nextElement();

      for (long startTime = se.getStartTime(); startTime < se.getEndTime();
           startTime += MILLIS_IN_DAY)
      {
        NewQuantityScheduleElement qse=GLMFactory.newQuantityScheduleElement();
        qse.setStartTime(startTime);
        qse.setEndTime(startTime + 1);
        qse.setQuantity(se.getValue());
        schedule.addScheduleElement(qse);
      }
    }

    return schedule;
  }

  private static Collection filterOutPositives(Schedule schedule)
  {
    // create collection of schedule elements where value is below 0
    Collection negValueCollection = schedule.filter(new UnaryPredicate() {
        public boolean execute(Object o)
        {
          ScheduleElementWithValue se = (ScheduleElementWithValue)o;
          return se.getValue() < -0.0001; // don't be too picky
        }
      });
    return negValueCollection;
  }

  private static Collection
    createInventoryScheduleDataAtoms(Inventory inventory,
                                     Collection scheduleElements)
  {
    // get the organization (not used yet)
    String org = inventory.getUID().getOwner();

    // get item UID
    ScheduledContentPG scheduled_content = inventory.getScheduledContentPG();
    if (scheduled_content == null)
      return null;
    String item = getItemUID(scheduled_content.getAsset());

    Collection scheduleAtoms =createScheduleElementDataAtoms(scheduleElements);
    DataAtomUtilities.addIdentifier(scheduleAtoms, GLMAtomTag.ITEM_TAG, item);

    return scheduleAtoms;
  }

  private static Collection
    createInventoryScheduleDataAtoms(Inventory inventory, Schedule schedule)
  {
    if (schedule == null)
    {
      Logger logger = Logging.currentLogger();
      if (logger.isWarnEnabled())
        logger.warn("requested schedule not found");
      return null;
    }

    return createInventoryScheduleDataAtoms(inventory,
                                            getCollectionCopy(schedule));
  }

  private static Schedule tryToCombineLikeQuantityElements(Schedule schedule)
  {
    // there is a bug in ScheduleUtilities.combineLikeQuantityElements
    //schedule = ScheduleUtilities.combineLikeQuantityElements(schedule);
    if (ScheduleElementWithValue.class.
        isAssignableFrom(schedule.getScheduleElementType()))
    {
      schedule = combineLikeQuantityElements(schedule);
    }
    else
    {
      Logger logger = Logging.currentLogger();
      if (logger.isWarnEnabled())
        logger.warn("could not combine like quantity elements, type is: " + schedule.getScheduleElementType().getName());
    }

    return schedule;
  }

  private static Collection
    createScheduleElementDataAtoms(Collection scheduleElements)
  {
    LinkedList resultSetAtoms = new LinkedList();

    // create a data atom describing each schedule element
    for (Iterator elements = scheduleElements.iterator();elements.hasNext();) {
      ScheduleElement element =(ScheduleElement)elements.next();

      // Pull out the start time and put it in a string
      Integer start_time =
        new Integer(convertMSecToCDate(element.getStartTime()));

      // Pull out the end time and put it in a string
      Integer end_time =
        new Integer(convertMSecToCDate(element.getEndTime()));

      // get the rate
      Double rate = null;
      if (element instanceof ScheduleElementWithValue) {
        ScheduleElementWithValue q_element = (ScheduleElementWithValue)element;
        rate = new Double(q_element.getValue());
      }
      else {
        Logger logger = Logging.currentLogger();
        if (logger.isWarnEnabled())
          logger.warn("Not sure what kind of ScheduleElementImpl this is: " + element.getClass().getName());
        continue;
      }

      ResultSetDataAtom dataAtom = new ResultSetDataAtom();
      dataAtom.addIdentifier(GLMAtomTag.START_TIME_TAG, start_time);
      dataAtom.addIdentifier(GLMAtomTag.END_TIME_TAG, end_time);
      dataAtom.addValue(GLMAtomTag.VALUE_TAG, rate);
      resultSetAtoms.add(dataAtom);
    } /* end of while */

    return resultSetAtoms;
  }

  private static Collection
    createScheduleElementDataAtoms(Schedule schedule)
  {
    return createScheduleElementDataAtoms(getCollectionCopy(schedule));
  }

  private static Collection getCollectionCopy(Schedule schedule)
  {
    UnaryPredicate getAll = new UnaryPredicate() {
        public boolean execute(Object o)
        {
          return true;
        }
      };
    return schedule.filter(getAll);
  }

  private static Schedule
    averageSchedules(Schedule scheduleA, Schedule scheduleB)
  {
    Schedule averaged_schedule =
      ScheduleUtilities.addSchedules(scheduleA, scheduleB);

    // workaround for bug #606
    averaged_schedule =
      setScheduleElementType(averaged_schedule,
                             scheduleA.getScheduleElementType());

    averaged_schedule = combineLikeQuantityElements(averaged_schedule);
    final ScheduleImpl si = new ScheduleImpl();
    si.setScheduleType(averaged_schedule.getScheduleType());
    si.setScheduleElementType(averaged_schedule.getScheduleElementType());
    Thunk halveThunk = new Thunk() {
        public void apply(Object sElement)
        {
          ScheduleElementWithValue e = (ScheduleElementWithValue)sElement;
          ScheduleElement newE =
            e.newElement(e.getStartTime(), e.getEndTime(), e.getValue() / 2);
          si.addScheduleElement(newE);
        }
      };
    averaged_schedule.applyThunkToScheduleElements(halveThunk);
    averaged_schedule = si;
    return averaged_schedule;
  }

  private static String getItemUID(Asset a)
  {
    if (a == null)
    {
      Logger logger = Logging.currentLogger();
      if (logger.isWarnEnabled())
        logger.warn("asset is null");
      return null;
    }

    TypeIdentificationPG type_id_pg = a.getTypeIdentificationPG();
    if (type_id_pg == null) {
      Logger logger = Logging.currentLogger();
      if (logger.isWarnEnabled())
        logger.warn ("no typeIdentificationPG for asset");
      return null;
    }

    return type_id_pg.getTypeIdentification();
  }

  private static int getBestCTime(Preference pref)
  {
    long pref_long =
      (long)pref.getScoringFunction().getBest().getAspectValue().getValue();
    return convertMSecToCDate(pref_long);
  }

  // Needed for Schedule Utilities bug workaround (bug report #606)
  private static Schedule
    setScheduleElementType(Schedule schedule, Class elementType)
  {
    ScheduleImpl fixed_schedule = new ScheduleImpl();
    fixed_schedule.setScheduleType(schedule.getScheduleType());
    fixed_schedule.setScheduleElementType(elementType);
    fixed_schedule.addAll(schedule);
    return fixed_schedule;
  }

  private static long extractCDateFromSociety () {
    long c_time_msec = 0L;
    String cdate_property =
      System.getProperty("org.cougaar.core.agent.startTime");
    String timezone_property = System.getProperty("user.timezone");
    if ((cdate_property == null) || (timezone_property == null))
      return c_time_msec;

    try {
      DateFormat f = (new SimpleDateFormat("MM/dd/yyy H:mm:ss"));
      f.setTimeZone(TimeZone.getTimeZone(timezone_property));
      c_time_msec = f.parse(cdate_property).getTime();
    } catch (ParseException e) {
      Logger logger = Logging.currentLogger();
      if (logger.isErrorEnabled())
        logger.warn("Error: could not parse CDate -> " + cdate_property);
      return 0l;
    }

    // This was needed to ensure that the milliseconds were set to 0
    c_time_msec = c_time_msec / 1000;
    c_time_msec *= 1000;

    return c_time_msec;
  } /* end of extractCDateFromSociety */

  private static int convertMSecToCDate (long current_time_msec) {
    return (int) ((current_time_msec - cTime) / MILLIS_IN_DAY);
  }

  private static long convertCDateToMSec (int current_time_c) {
    return (cTime + (current_time_c * MILLIS_IN_DAY));
  }

  /**
   * This is based on org.cougaar.planning.ldm.plan.ScheduleUtilities.
   * combineLikeQuantityElements()
   * It is different in that it doesn't needlessly throw an Exception when
   * a schedule with ScheduleElementType.RATE elements is passed in.
   *
   * A bug report has been submitted (#605).
   *
   * Returns a schedule that creates ScheduleElementWithValue that span multiple
   * days if there are Like ScheduleElementWithValues during that time period.
   * This is more efficient than having a scheduleelement for each day.
   * This method expects a Schedule containing ONLY ScheduleElementWithValues
   * @param aSchedule
   * @return Schedule
   * @see org.cougaar.planning.ldm.plan.Schedule
   * @see org.cougaar.planning.ldm.plan.ScheduleElementWithValue
   * @throws IllegalArgumentException
   **/
  private static Schedule combineLikeQuantityElements(Schedule aSchedule) {
    if (! ScheduleElementWithValue.class.isAssignableFrom(aSchedule.getScheduleElementType())) {
      throw new IllegalArgumentException("ScheduleUtilities.combineLikeQuantityElements expects " +
                "a Schedule with ScheduleElementWithValues!");
    }
    double currentQuantity = 0;
    double quantity = 0;
    long startTime = 0;
    long endTime = 0;
    ArrayList minimalScheduleElements = new ArrayList();

    ArrayList scheduleElements = new ArrayList(aSchedule);

    if (scheduleElements.size() > 0) {
      ScheduleElementWithValue s =
        (ScheduleElementWithValue) scheduleElements.get(0);
      startTime = s.getStartTime();
      endTime = s.getEndTime();
      currentQuantity = s.getValue();

      for (int i = 1; i < scheduleElements.size(); i++) {
        s = (ScheduleElementWithValue) scheduleElements.get(i);
        if (s.getStartTime() > (endTime+1000) ||
            s.getValue() != currentQuantity) {
          minimalScheduleElements.add(s.newElement(startTime, endTime, currentQuantity));
          currentQuantity = s.getValue();
          startTime = s.getStartTime();
        }
        endTime = s.getEndTime();
      }
      //get the last range element
      if (startTime != 0 && (endTime-startTime)>1000L) {
        minimalScheduleElements.add(s.newElement(startTime, endTime, currentQuantity));
      }
    }

    // create a new schedule to return
    ScheduleImpl newsched = new ScheduleImpl();
    newsched.setScheduleType(aSchedule.getScheduleType());
    newsched.setScheduleElementType(aSchedule.getScheduleElementType());
    newsched.setScheduleElements(minimalScheduleElements);

    return newsched;
  }

  private static Schedule
    getInventorySchedule(String metricName, InventoryWrapper inventory)
      throws Exception
  {
    return
      getInventorySchedule(InventoryMetric.fromString(metricName), inventory);
  }

  private static Schedule
    getInventorySchedule(InventoryMetric metric, InventoryWrapper inventory)
      throws Exception
  {
    String methodName = (String)metricMethodMap.get(metric);
    Method invAccessMethod = InventoryWrapper.class.getMethod(methodName, null);
    return (Schedule)invAccessMethod.invoke(inventory, null);
  }

  /**
   * filter out elements that don't satisfy unary predicate
   */
  public static Collection filterCollection(Collection elements,
                                            UnaryPredicate up)
  {
    for (Iterator i = elements.iterator(); i.hasNext();)
    {
      if (!up.execute(i.next()))
        i.remove();
    }

    return elements;
  }

  public static Collection
    filterTimePeriod(Collection dataAtoms, int startTime, int endTime)
  {
    endTime++;
    Collection newCollection = new LinkedList();
    for (Iterator i = dataAtoms.iterator(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      int atomStartTime =
        ((Number)da.getIdentifier(GLMAtomTag.START_TIME_TAG)).intValue();
      int atomEndTime =
        ((Number)da.getIdentifier(GLMAtomTag.END_TIME_TAG)).intValue();
      if (atomStartTime < startTime)
      {
        if (atomEndTime > startTime)
        {
          da.addIdentifier(GLMAtomTag.START_TIME_TAG, new Integer(startTime));
          if (atomEndTime > endTime)
          {
            da.addIdentifier(GLMAtomTag.END_TIME_TAG, new Integer(endTime));
          }
          newCollection.add(da);
        }
      }
      else
      {
        if (atomStartTime < endTime)
        {
          if (atomEndTime > endTime)
          {
            da.addIdentifier(GLMAtomTag.END_TIME_TAG, new Integer(endTime));
          }
          newCollection.add(da);
        }
      }
    }

    return newCollection;
  }
}

