/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.stoplight.society;

import org.cougaar.glm.ldm.*;
import org.cougaar.glm.ldm.*;
import org.cougaar.glm.*;
import org.cougaar.glm.ldm.plan.*;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Capacity;
import org.cougaar.glm.ldm.asset.Person;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.glm.plugins.TimeUtils;
import org.cougaar.glm.plugins.ScheduleUtils;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.FlowRate;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.MassTransferRate;

import java.text.DateFormat;
import java.util.*;

import org.cougaar.core.util.*;
import org.cougaar.core.plugin.util.*;
import org.cougaar.util.*;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleType;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.Verb;

import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.plan.LaborSchedule;
import org.cougaar.glm.ldm.plan.NewQuantityScheduleElement;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.Constants;

/** Composes inventory and capacity schedules from COUGAAR log plan objects.
  Note that the getters return null if the schedule requested is null
  or empty.
  */
// PAS does anyone use TotalSchedule - no
// PSP REQUESTED_DUE_OUT_SHORTFALL not use ie getDueOutShortfallSchedule not used
public class InventoryWrapper {
/*
	static final String[][] capacityInfo = {
      { "AmmunitionTransportation", "Tons" },
      { "AmmunitionHandling", "Tons/Day" },
      { "AmmunitionStorage", "Tons" },
      { "FuelTransportation", "Gallons" },
      { "FuelHandling", "Gallons/Day" },
      { "FuelStorage", "Gallons" },
      { "WaterTransportation", "Gallons" },
      { "WaterHandling", "Gallons/Day" },
      { "WaterStorage", "Gallons" },
      { "ContainerTransportation", "Count" },
      { "NonContainerTransportation", "Tons" },
      { "MaterielHandling", "Tons/Day" },
      { "MaterielStorage", "Tons" },
      { "PassengerTransportation", "Count" },
      { "HETTransportation", "Tons" },
  };
  static final String[] fuelTypes = { "DF2", "DFM", "JP5", "JP8", "MUG" };
*/

  public final static String NO_INVENTORY_SCHEDULE_JUST_CONSUME="NO INVENTORY SCHEDULE(DEMAND)";

  Asset asset;
  TimeSpanSet dueInSchedule = new TimeSpanSet();
  TimeSpanSet unconfirmedDueInSchedule = new TimeSpanSet();
  TimeSpanSet requestedDueInSchedule = new TimeSpanSet();
  TimeSpanSet projectedDueInSchedule = new TimeSpanSet();
  TimeSpanSet projectedRequestedDueInSchedule = new TimeSpanSet();
  TimeSpanSet inactiveDueInSchedule = new TimeSpanSet();
  TimeSpanSet inactiveUnconfirmedDueInSchedule = new TimeSpanSet();
  TimeSpanSet inactiveRequestedDueInSchedule = new TimeSpanSet();
  TimeSpanSet inactiveProjectedDueInSchedule = new TimeSpanSet();
  TimeSpanSet inactiveProjectedRequestedDueInSchedule = new TimeSpanSet();

  Vector dueOutLaborSchedule = null;
  Vector laborSchedule = null;

  Vector onHandDailySchedule = null;
  Vector onHandDetailedSchedule = null;

  Vector dueOutSchedule = null;
  Vector projectedDueOutSchedule = null;
  Vector projectedDueOutLaborSchedule = null;
  Vector requestedDueOutSchedule = null;
  Vector projectedRequestedDueOutSchedule = null;

  Vector inactiveDueOutSchedule = null;
  Vector inactiveProjectedDueOutSchedule = null;
  Vector inactiveRequestedDueOutSchedule = null;
  Vector inactiveProjectedRequestedDueOutSchedule = null;

  Schedule averageDemandSchedule = null;
  Schedule reorderLevelSchedule = null;
  Schedule goalLevelSchedule = null;
  Vector projectedMockDueInSchedule = null;
  Vector projectedRequestedMockDueInSchedule = null;
  Schedule onHandMockSchedule = null;
  Vector projectedRequestedMockDueOutSchedule = null;
  Vector projectedMockDueOutSchedule = null;

//    Vector dueOutShortfallSchedule = null;
//  Vector totalSchedule = null;
  final static long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
  static boolean debug;    

  public InventoryWrapper() {
      // print debug messages if inventory_debug set to true
    debug = false;
    String val = System.getProperty("inventory_debug");
    if (val != null) {
	if (val.equals("true")) {
	    debug = true;
	}
    }
  }

    
  public Asset getAsset() {return this.asset;}

  protected Asset getInsideAsset() {
      if((asset instanceof GLMAsset) &&
	 (((GLMAsset) asset).getScheduledContentPG() != null))
	  return ((GLMAsset)asset).getScheduledContentPG().getAsset();
      else
	  return this.asset;
  }

  /** Set asset for this inventory object.
   * Handle Labor assets differently from non-labor.
   */

  public void setAsset(Asset asset) {
    this.asset = asset;
   
    if(asset instanceof GLMAsset) {
	Schedule s=null;
	if((asset instanceof Inventory) &&
	   (((Inventory) asset).getDetailedScheduledContentPG() != null)) {
	    s = ((Inventory) asset).getDetailedScheduledContentPG().getSchedule();
	    onHandDetailedSchedule = scheduleToNonOverlapVector(s);
	    System.out.println("Got a detailed Inventory schedule!!!");
	    s=null;
	}
	if(((GLMAsset)asset).getScheduledContentPG() != null) {
	    s = ((GLMAsset)asset).getScheduledContentPG().getSchedule();
	}
	if (s != null) {
	    if (s.getScheduleType().equals(PlanScheduleType.TOTAL_CAPACITY)) {
		if (s instanceof LaborSchedule)
		    s = ((LaborSchedule)s).getQuantitySchedule();
		else 
		    System.out.println("InventoryWrapper WARNING: Expected labor schedule");	  
		laborSchedule = scheduleToNonOverlapVector(s);
	    } else
		onHandDailySchedule = scheduleToNonOverlapVector(s);
	}
	if (isLaborAsset(asset)){
	    dueOutLaborSchedule = computeDueOutVector(false); // All non-inventory assets are active
	    projectedDueOutLaborSchedule = computeProjectedDueOutVector(false);
	    //        setDueOutLaborSchedule();
	}
	else {
	    dueOutSchedule = computeDueOutVector(false);
	    inactiveDueOutSchedule = computeDueOutVector(true);
	    projectedDueOutSchedule = computeProjectedDueOutVector(false);
	    inactiveProjectedDueOutSchedule = computeProjectedDueOutVector(true);
            requestedDueOutSchedule = computeRequestedDueOutVector(false);
            inactiveRequestedDueOutSchedule = computeRequestedDueOutVector(true);
            projectedRequestedDueOutSchedule = computeProjectedRequestedDueOutVector(false);
            inactiveProjectedRequestedDueOutSchedule = computeProjectedRequestedDueOutVector(true);
	    
	    if(asset instanceof Inventory) 
		setInventoryLevelsSchedules((Inventory) asset);

//  	    setRequestedDueOutSchedule(); 
//  	    setProjectedRequestedDueOutSchedule();
//          setDueOutShortfallSchedule();
	}
    }
  }

    /** It's a labor asset if the class of the asset is Capacity or
        if the class of the inner asset is Person.
    */

    private boolean isLaborAsset(Asset asset) {
        if (asset instanceof Capacity)
            return true;
        if (getInsideAsset() instanceof Person)
            return true;
        return false;
    }

//      /** total = onHand + dueOut
//    private void setTotalSchedule() {
//      Schedule onHandCSchedule = getCSchedule(onHandDailySchedule);
//      Schedule dueOutCSchedule = getCSchedule(dueOutSchedule);
//      Schedule total = ScheduleUtilities.addSchedules(onHandCSchedule,
//                                                      dueOutCSchedule);
//      if (debug)	{
//  	System.out.println("Available Schedule");
//  	printSchedule(onHandCSchedule);
//  	System.out.println("Allocated COUGAAR Schedule");
//  	printSchedule(dueOutCSchedule);
//  	System.out.println("Total Schedule");
//  	printSchedule(total);
//      }
//      totalSchedule = scheduleToVector(total);
//    }

 //   public Vector getTotalSchedule() {
//      setTotalSchedule(); // only compute this if needed
//      return getSchedule(totalSchedule);
//    }

    // checks if empty schedule, return null
    protected Vector getSchedule(Vector sched) {
	if (sched == null) return null;
	if (sched.size() == 0) return null;
	return sched;
    }

    // checks if empty schedule, return null
    protected TimeSpanSet checkTimeSpanSet(TimeSpanSet set) {
	if (set == null) {
	    System.out.println("Null timespanset");
	    return null;
	}
	if (set.size() == 0) {
	    System.out.println("Empty timespanset");
	    return null;
	}
	return set;
    }
			       

  /** Get human readable form of the TypeIdentificationPG.
    @return String - the nomenclature from the TypeIdentificationPG
   */
  public String getAssetName() {
    TypeIdentificationPG typeIdPG = this.getInsideAsset().getTypeIdentificationPG();
    return typeIdPG.getNomenclature() + ":" + typeIdPG.getTypeIdentification();
  }

  /**
    Returns a schedule as defined in ScheduleType
    */

  public String getScheduleType() {
      if((asset instanceof GLMAsset) &&
	 (((GLMAsset) asset).getScheduledContentPG() != null)) {
	  Schedule s = ((GLMAsset)asset).getScheduledContentPG().getSchedule();
	  if (s != null)
	      return s.getScheduleType();
      }
      else {
	  return NO_INVENTORY_SCHEDULE_JUST_CONSUME;
      }

      return "";
  }

  /**
    Returns the unit type -- i.e. gallons, man hours, tons, etc.
    */
/*
  public String getUnitType() {
    Asset insideAsset=this.getInsideAsset();
    String typeId =
      insideAsset.getTypeIdentificationPG().getTypeIdentification();
    String nomenclature =
      insideAsset.getTypeIdentificationPG().getNomenclature();

    String scheduleType = getScheduleType();

    // capacity
    if (scheduleType.equals(PlanScheduleType.TOTAL_CAPACITY) ||
        scheduleType.equals(PlanScheduleType.AVAILABLE_CAPACITY) ||
        scheduleType.equals(PlanScheduleType.ACTUAL_CAPACITY) ||
        scheduleType.equals(PlanScheduleType.LABOR)) {
      if (typeId.startsWith("MOS"))
        return "Hours/Day";
      else {
        for (int i = 0; i < capacityInfo.length; i++)
          if (capacityInfo[i][0].equals(typeId))
            return capacityInfo[i][1];
      }
      return "";
    }
    
    // inventory
    if (typeId.startsWith("NSN")) {
      for (int i = 0; i < fuelTypes.length; i++)
        if (fuelTypes[i].equals(nomenclature))
          return "Gallons"; // fuel
      return "Items"; // consumables
    } else 
      return "STons"; // ammunition
  }
*/
  /**
     Get the schedule that indicates the on hand inventory for this asset.
     @return Vector - vector of QuantityScheduleElement
  */
  public Vector getOnHandDailySchedule() {
      return getSchedule(onHandDailySchedule);
  }

  /**
     Get the schedule that indicates the on hand inventory for this asset.
     @return Vector - vector of QuantityScheduleElement
  */
  public Vector getOnHandDetailedSchedule() {
      return getSchedule(onHandDetailedSchedule);
  }


  /**
     Get the schedule that indicates the on hand inventory with jaggy ness 
     in the projection phase for this asset.
     @return Vector - vector of QuantityScheduleElement
  */
  public Vector getOnHandMockSchedule() {
      return getSchedule(scheduleToNonOverlapVector(onHandMockSchedule));
  }

  /** Take a COUGAAR schedule and make it into a vector of QuantityScheduleElement
    for serialization.
    */
  static private Vector scheduleToVector(Schedule CSchedule) {
    Vector s = new Vector();
    if(CSchedule != null) {
	Enumeration elements = CSchedule.getAllScheduleElements();
	while (elements.hasMoreElements()) {
	    QuantityScheduleElement scheduleElement = 
		(QuantityScheduleElement)elements.nextElement();
	    s.addElement(new QuantityScheduleElementImpl(scheduleElement.getStartTime(),
                                                         scheduleElement.getEndTime(),
                                                         scheduleElement.getQuantity()));
	}
    }
    return s;
  }

  private QuantityScheduleElement getScheduleElementFromTask(Task task) {
    if (task == null) {
      System.out.println("InventoryWrapper WARNING: Allocation result is null");
      return null;
    }

    double quantity;
    
    if(TaskUtils.isProjection(task)) {
	Rate r = TaskUtils.getRate(task);
	if(r == null) 
	    quantity = -1;
	else
	    quantity = getDailyQty(r);

    }
    else {
	quantity  = getPreferenceValue(task, AspectType.QUANTITY);
    }
    if (quantity < 0) {
      System.out.println("InventoryWrapper WARNING: EXPECTED QUANTITY IN ALLOCATION RESULT WHEN DERIVING SCHEDULE ELEMENT FROM TASK");
      return null; // ignore if no quantity
    }

    long end_time = getPreferredTime(task, AspectType.END_TIME);
    if (end_time < 0) {
      System.out.println("InventoryWrapper WARNING: NO END_TIME IN ALLOCATION RESULT WHEN DERIVING SCHEDULE ELEMENT FROM TASK");
      return null;
    }

    return new QuantityScheduleElementImpl(end_time - 1, end_time, quantity);
  }

  /**
   * create a Vector of schedule elements for the given task. If pe is
   * not null, use the allocation result from it else use the tasks
   * preferences. An AllocationResultHelper does a lot of the work.
   **/
  private Vector getScheduleFromProjectionTask(Task projectTask,
                                               boolean isInactive,
                                               Allocation pe)
  {
    if (projectTask == null) {
      System.out.println("InventoryWrapper WARNING: Project Task is null");
      return null;
    }
    if(!TaskUtils.isProjection(projectTask)) {
	System.out.println("InventoryWrapper WARNING: Expected Project Task.");
	return null;
    }
    Vector schedule = new Vector();
    AllocationResultHelper helper = new AllocationResultHelper(projectTask, pe);
    for (int i = 0, n = helper.getPhaseCount(); i < n; i++) {
      AllocationResultHelper.Phase phase = helper.getPhase(i);
      long startTime = phase.getStartTime();
      long endTime = phase.getEndTime();
      AspectRate arate = (AspectRate) phase.getAspectValue(AlpineAspectType.DEMANDRATE);
      double quantity = getDailyQty(arate.getRateValue());
      while (startTime < endTime) {
        NewQuantityScheduleElement el =
          new QuantityScheduleElementImpl(startTime, startTime + 1, quantity);
        if (isInactive(projectTask, el) == isInactive) {
          schedule.add(el);
          if (debug) {
            System.out.println("InventoryWrapper::getScheduleFromProjectionTask: add " + el + " inactive is " + isInactive + " for task: " + TaskUtils.taskDesc(projectTask));
          }
        }
        startTime += TimeUtils.MSEC_PER_DAY;
      }
    }
    return schedule;
  }
    
    public static double getDailyQty(Rate aRate) {
	if (aRate instanceof FlowRate) {
	    return ((FlowRate) aRate).getGallonsPerDay();
	}
	if (aRate instanceof CountRate) {
	    return ((CountRate) aRate).getEachesPerDay();
	}
	if (aRate instanceof MassTransferRate) {
	    return ((MassTransferRate) aRate).getShortTonsPerDay();
	}
        System.out.println("InventoryWrapper::getDailyQty - unknown type of rate - " + aRate.getClass().getName());
        return -1;
    }


    public static double getPreferenceValue(Task task, int aspect_type) {
	Preference task_pref = task.getPreference(aspect_type);
	if (task_pref == null) {
	    return -1;
	} else if (task_pref.getScoringFunction() == null) {
	    return -1;
	} else if (task_pref.getScoringFunction().getBest() == null) {
	    return -1;
	} else {
	    return task_pref.getScoringFunction().getBest().getValue();
	}
    }

  private QuantityScheduleElement getScheduleFromAllocation(AllocationResult allocationResult)
  {
    long endTime = -1;
    double quantity = 0;

    if (allocationResult == null) {
      System.out.println("InventoryWrapper WARNING: Allocation result is null");
      return null;
    }

    if (allocationResult.isDefined(AspectType.QUANTITY))
      quantity = allocationResult.getValue(AspectType.QUANTITY);
    else {
      System.out.println("InventoryWrapper WARNING: EXPECTED QUANTITY IN ALLOCATION RESULT FOR DUE OUT");
      return null; // ignore if no quantity
    }

    if (allocationResult.isDefined(AspectType.END_TIME)) {
      endTime = (long)allocationResult.getValue(AspectType.END_TIME);
    } else {
      return null; // ignore if no end time
    }

    //    if (debug)  System.out.println("Creating schedule: " + quantity +
    //		       " start time: " + startTime +
    //		       " end time: " + endTime);

    return new QuantityScheduleElementImpl(endTime-1, endTime, quantity);
  }


    protected void setInventoryLevelsSchedules(Inventory inventory) {
	InventoryLevelsPG invLevPG = inventory.getInventoryLevelsPG();

        averageDemandSchedule = invLevPG.getAverageDemandSchedule();
	goalLevelSchedule = invLevPG.getGoalLevelSchedule();
	reorderLevelSchedule = invLevPG.getReorderLevelSchedule();
    }

    protected int getProjectedDueOutPeriodDays() { return 1; }

    protected long getProjectedDueOutPeriodMSecs() {
	return TimeUtils.MSEC_PER_DAY * getProjectedDueOutPeriodDays();
    }

    protected static double deriveMockDueInQty(Schedule s,long startTime, long endTime,
					   double invQty, double reorderLevel, double goalLevel) {
	if(invQty <  reorderLevel) {
	    if (debug)
	        System.out.println("InventoryWrapper::deriveMockDueInQty reordering:  InvQty is " + invQty + " Reorder is " + reorderLevel + " diff is " + (invQty - reorderLevel));
	    return goalLevel - invQty;
	}
	else return 0.0;
    }

    protected static double extractFirstQtyInDay(Schedule s, long time) {
	Collection col = s.getOverlappingScheduleElements(time,time + TimeUtils.MSEC_PER_DAY);
	// Collection col = s.getOverlappingScheduleElements(time,s.getEndTime());
	if(col.size() >= 1) {
	    return ((QuantityScheduleElement) (col.toArray())[0]).getQuantity();
	}
	else
	{
	    throw new RuntimeException("Should be at least one Schedule element in schedule for " + new Date(time) + " but there are " + col.size());
	}
    }

    protected static double extractSingleQtyOnTime(Schedule s, long time) {
	Collection col = s.getScheduleElementsWithTime(time);
	if(col.size() == 1) {
	    return ((QuantityScheduleElement) (col.toArray())[0]).getQuantity();
	}
	else
	{
	    throw new RuntimeException("Should be one Schedule element in schedule for " + new Date(time) + " but there are " + col.size());
	}
    }
      
    protected static double getQtySumOnSchedOverTime(Schedule s, long startTime,long endTime)    {
	Collection col = s.getEncapsulatedScheduleElements(startTime,endTime);
	return ScheduleUtilities.sumElements(col);
    }
	
    /***
     *
               	Iterator it = col.iterator();
	while(it.hasNext()) {
	    QuantityScheduleElement el = (QuantityScheduleElement) it.next();
	    sum+=el.getQuantity();
	}	
	    return sum;
    }

    **/


    protected void initializeMockSchedules() {
	onHandMockSchedule = getCSchedule(onHandDailySchedule);
	projectedMockDueInSchedule = getProjectedDueInSchedule();
	projectedRequestedMockDueInSchedule = getProjectedRequestedDueInSchedule();
	projectedMockDueOutSchedule = getProjectedDueOutSchedule();
	projectedRequestedMockDueOutSchedule = getProjectedRequestedDueOutSchedule();
    }

    protected boolean validateDataForSimulatedProjections() {

	boolean theReturn = true;

	if((onHandDailySchedule == null) ||
	   (reorderLevelSchedule == null || goalLevelSchedule == null) ||
	   (projectedDueInSchedule == null) || 
	   (projectedRequestedDueInSchedule == null) ||
	   (projectedDueOutSchedule == null) || 
	   (projectedRequestedDueOutSchedule == null)) {
	    System.out.println("WARNING: Not all the info is there to computeSimulatedProjectionSchedules");

	    if(reorderLevelSchedule == null) {
		System.out.println("WARNING:reorderLevelSchedule");
		reorderLevelSchedule = new ScheduleImpl();
		theReturn = false;
	    }
	    if(goalLevelSchedule == null) {
		System.out.println("WARNING:goalLevelSchedule");
		goalLevelSchedule = new ScheduleImpl();
		theReturn = false;
	    }
	    if(projectedDueInSchedule == null) {
		System.out.println("WARNING:projectedDueInSchedule");
	    }
	    if(projectedRequestedDueInSchedule == null) {
		System.out.println("WARNING:projectedRequestedDueInSchedule");
	    }
	    if(projectedDueOutSchedule == null) {
		System.out.println("WARNING:projectedDueOutSchedule");
	    }
	    if(projectedRequestedDueOutSchedule == null) {
		System.out.println("WARNING:projectedRequestedDueOutSchedule");
	    }
	    if(onHandDailySchedule == null) {
		System.out.println("WARNING:onHandDailySchedule");
		theReturn = false;
	    }
	}
	return theReturn;
    }

    public void computeSimulatedProjectionSchedules() {

	
	if(!validateDataForSimulatedProjections()) {
	    initializeMockSchedules();
	    return;
	}

	Schedule onHandAlpSched = getCSchedule(onHandDailySchedule);
	Schedule projReqDueOutAlpSched = getCSchedule(projectedRequestedDueOutSchedule);
	Schedule projReqDueInAlpSched = getCSchedule(projectedRequestedDueInSchedule);
	Schedule projDueInAlpSched = getCSchedule(projectedDueInSchedule);
	Schedule projDueOutAlpSched = getCSchedule(projectedDueOutSchedule);

	long switchOverDay = Schedule.MAX_VALUE;
	if(projReqDueInAlpSched.getAllScheduleElements().hasMoreElements()) {
	    switchOverDay = projReqDueInAlpSched.getStartTime();
	}
	long endDayTime  = Schedule.MAX_VALUE;
	if(onHandAlpSched.getAllScheduleElements().hasMoreElements()) {
	    endDayTime = onHandAlpSched.getEndTime();
	}
	long startTime = switchOverDay;
	double invQty=0.0;


	projectedRequestedMockDueInSchedule = new Vector();
	projectedMockDueInSchedule = new Vector();

	projectedRequestedMockDueOutSchedule =
	    scheduleToNonOverlapVector(
	    new ScheduleImpl(projReqDueOutAlpSched.getOverlappingScheduleElements(
						    projReqDueOutAlpSched.MIN_VALUE,
						    switchOverDay)));

	projectedMockDueOutSchedule = 
	    scheduleToNonOverlapVector(
	    new ScheduleImpl(projDueOutAlpSched.getOverlappingScheduleElements(
						   projDueOutAlpSched.MIN_VALUE,
						   switchOverDay)));

	onHandMockSchedule = 
	    new ScheduleImpl(onHandAlpSched.getOverlappingScheduleElements(
						     onHandAlpSched.MIN_VALUE,
						     switchOverDay));

	

	if(startTime != Schedule.MAX_VALUE)
	    invQty = extractFirstQtyInDay(onHandAlpSched,startTime);

	//	System.out.println("InventoryWrapper:computeSimulatedProj:  startTime/switchOverDay is: " + new Date(startTime) + " and end day time is : " + new Date(endDayTime));

	while(startTime <  endDayTime) {
	    long endTime = startTime + getProjectedDueOutPeriodMSecs() - 1;
	    if(endTime > endDayTime) 
		endTime = endDayTime;
	    
	    double reqDueOutQty = getQtySumOnSchedOverTime(projReqDueOutAlpSched,
							   startTime,endTime);

	    double dueOutQty = getQtySumOnSchedOverTime(projDueOutAlpSched,
							startTime,endTime);

	    //take out due outs
	    invQty = invQty - dueOutQty;

	    double reorderLevel = extractFirstQtyInDay(reorderLevelSchedule,endTime);
	    double goalLevel = extractFirstQtyInDay(goalLevelSchedule,endTime);

	    double reqDueInQty = deriveMockDueInQty(projReqDueInAlpSched,startTime,endTime,
						    invQty, reorderLevel, goalLevel);

	    double dueInQty = deriveMockDueInQty(projDueInAlpSched,startTime,endTime,
						 invQty, reorderLevel, goalLevel);

	    //put in due ins
	    invQty+=dueInQty;


	    //	    System.out.println("InventoryWrapper:computeSimulatedProj:  within: " + new Date(startTime) + " and : " + new Date(endTime) + " Due outs are: " + dueOutQty + " due ins are " + dueInQty + " and inventory is: " + invQty + ". reorderLevel shows: " + reorderLevel + " and goalLevel is: " + goalLevel + ".");

	    onHandMockSchedule.add(ScheduleUtils.buildQuantityScheduleElement(invQty, startTime, endTime));

	    projectedRequestedMockDueOutSchedule.add(new QuantityScheduleElementImpl(endTime-1, endTime, reqDueOutQty));

	    projectedMockDueOutSchedule.add(new QuantityScheduleElementImpl(endTime-1, endTime, dueOutQty));

	    projectedRequestedMockDueInSchedule.add(new QuantityScheduleElementImpl(endTime-1, endTime, reqDueInQty));

	    projectedMockDueInSchedule.add(new QuantityScheduleElementImpl(endTime-1, endTime, dueInQty));

	    startTime = (endTime+1);
	}
	
    }

  /**
    Add schedule elements from an allocation to the due-ins. Get the
    end time and quantity from the allocation reported results. Adds
    the elements to the inactive or active schedules depending on the
    time of the elements. IGNORE ALLOCATION RESULTS IF isSuccess IS
    FALSE. */

  public void addDueInSchedule(Allocation allocation) {
    Task task = allocation.getTask();
    AllocationResult ar = allocation.getReportedResult();
    if (TaskUtils.isProjection(task)) {
      if (ar == null) allocation = null; // Ignore allocation if no reported result
      for (int i = 0; i < 2; i++) {
        boolean isInactive = (i == 0);
        Vector schedule = getScheduleFromProjectionTask(task, isInactive, allocation);
        if (schedule != null) {
//        if (debug) {
//          System.out.println("Adding projected due in schedule from allocation: " + 
//                             allocation.getUID());
//          for (int j = 0; j < schedule.size(); j++) 
//            printQuantityScheduleElement((QuantityScheduleElement) schedule.elementAt(j));
//        }
          if (isInactive) {
            inactiveProjectedDueInSchedule.addAll(schedule);
          } else {
            projectedDueInSchedule.addAll(schedule);
          }
        }
      }
    } else if (ar == null) {
      // Use request if no allocation result
      QuantityScheduleElement se = 
        getScheduleElementFromTask(task); // Use end time for due-ins
      if (se != null) {
//      if (debug) {
//  	  System.out.println("Adding unconfirmed due in schedule from allocation: " + 
//			     allocation.getUID());
//  	  printQuantityScheduleElement(se);
//  	}
        if (isInactive(task, se)) {
          inactiveUnconfirmedDueInSchedule.add(se);
        } else {
          unconfirmedDueInSchedule.add(se);
        }
      }
    } else if (ar.isSuccess()) {
      QuantityScheduleElement se = 
        getScheduleFromAllocation(allocation.getReportedResult());
      if (se != null) {
//      if (debug) {
//        System.out.println("Adding due in schedule from allocation: " + 
//                           allocation.getUID());
//        printQuantityScheduleElement(se);
//      }
        if (isInactive(allocation, se)) {
          inactiveDueInSchedule.add(se);
        } else {
          dueInSchedule.add(se);
        }
      }
    } else {
      // Ignore if not successful
    }
  }

  /** Add schedule elements from the preferences in the task in the due-in schedule.
   */

  public void addRequestedDueInSchedule(Allocation allocation) {
    Task task = allocation.getTask();
    if (task == null) {
      System.out.println("InventoryWrapper WARNING: no task in due-in allocation");
      return;
    }

    if (TaskUtils.isProjection(task)) {
      for (int i = 0; i < 2; i++) {
        boolean isInactive = (i == 0);
	Vector projectSchedule = getScheduleFromProjectionTask(task, isInactive, null);
	if (projectSchedule != null) {
          if (isInactive) {
	    inactiveProjectedRequestedDueInSchedule.addAll(projectSchedule);
            TimeSpan ts = (TimeSpan) inactiveProjectedRequestedDueInSchedule.last();
            if (debug)
              System.out.println("End of inactive requested duein schedule"
                                 + (ts == null ? "none" : TimeUtils.dateString(ts.getEndTime())));
          } else {
	    projectedRequestedDueInSchedule.addAll(projectSchedule);
            TimeSpan ts = (TimeSpan) projectedRequestedDueInSchedule.last();
            if (debug)
              System.out.println("End of active requested duein schedule"
                                 + (ts == null ? "none" : TimeUtils.dateString(ts.getEndTime())));
          }
        }
      }
    } else {
      long endTime = getPreferredTime(task, AspectType.END_TIME);
      double quantity = getPreferenceValue(task, AspectType.QUANTITY);

      // must have end time and quantity
      if ((quantity != -1) && (endTime != -1)) { 
        NewQuantityScheduleElement se = 
          new QuantityScheduleElementImpl(endTime-1, endTime, quantity);
        if (isInactive(task, se))
          inactiveRequestedDueInSchedule.add(se);
        else
          requestedDueInSchedule.add(se);
      }
    }
  }

  private Vector convertTimeSpanSet(TimeSpanSet schedule, String debugLabel) {
     if (checkTimeSpanSet(schedule) == null) return null;
     
     if (debug) {
	 // 	// print original schedule for debugging
	 System.out.println("ORIGINAL " + debugLabel + " SCHEDULE");
	 Vector s = new Vector(schedule);
	 printSchedule(s);
     }
     
     // make an alp schedule so we can use the alp utilities
     // to make it non-overlapping
     Schedule tmpSchedule = makeNonOverlapping(schedule);
     Vector results = scheduleToVector(tmpSchedule);
     
     if (debug)  {
	 System.out.println("FINAL " + debugLabel + " SCHEDULE");
	 printSchedule(results);
     }	
     return results;
  }


  public Vector getReorderLevelSchedule() {
      return getSchedule(scheduleToNonOverlapVector(reorderLevelSchedule));
  }

  public Vector getAverageDemandSchedule() {
      return getSchedule(scheduleToNonOverlapVector(averageDemandSchedule));
  }

  public Vector getGoalLevelSchedule() {
      return getSchedule(scheduleToNonOverlapVector(goalLevelSchedule));
  }

  /**
     Get the schedule that indicates the due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getDueInSchedule() {
    return convertTimeSpanSet(dueInSchedule, "DUE IN");
  }

  /**
     Get the schedule that indicates the due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getUnconfirmedDueInSchedule() {
    return convertTimeSpanSet(unconfirmedDueInSchedule, "UNCONFIRMED DUE IN");
  }

  /**
     Get the schedule that indicates the projected due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getProjectedDueInSchedule() {
    return convertTimeSpanSet(projectedDueInSchedule, "PROJECTED DUE IN");
  }

    /** Get the projected due in schedule for simulated OnHandMockSchedule
     */
    public Vector getProjectedMockDueInSchedule() {
	return getSchedule(projectedMockDueInSchedule); 
    }

  /**
     Get the schedule that indicates the inactive due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getInactiveDueInSchedule() {
    return convertTimeSpanSet(inactiveDueInSchedule, "INACTIVE DUE IN");
  }

  /**
     Get the schedule that indicates the due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getInactiveUnconfirmedDueInSchedule() {
    return convertTimeSpanSet(inactiveUnconfirmedDueInSchedule, "INACTIVE UNCONFIRMED DUE IN");
  }

  /**
     Get the schedule that indicates the projected due-in inventory for this asset.
     @return Vector - the schedule for this asset in this cluster
  */
  public Vector getInactiveProjectedDueInSchedule() {
      return convertTimeSpanSet(inactiveProjectedDueInSchedule, "INACTIVE PROJECTED DUE IN");
  }

    public static Schedule makeNonOverlapping(TimeSpanSet inSchedule) {
	// make an alp schedule so we can use the alp utilities
	// to make it non-overlapping
	Schedule tmpCSchedule = getCSchedule(inSchedule);
	if (isOverlappingSchedule(tmpCSchedule)) {
	    if (debug)  System.out.println("InventoryWrapper::makeNonOverlapping:IS OVERLAPPING");
	    tmpCSchedule =
		ScheduleUtilities.computeNonOverlappingSchedule(tmpCSchedule);
	} else if (debug) {
	    System.out.println("InventoryWrapper::makeNonOverlapping:is NOT Overlapping");
	}
	return tmpCSchedule;
    }

    /** Get the requested due in schedule.
     */
    public Vector getRequestedDueInSchedule() {
	if (checkTimeSpanSet(requestedDueInSchedule) == null) return null;
	return new Vector(requestedDueInSchedule); 
    }

    /** Get the projected requested due in schedule for simulated OnHandMockSchedule
     */
    public Vector getProjectedRequestedMockDueInSchedule() {
	return getSchedule(projectedRequestedMockDueInSchedule); 
    }


    /** Get the projected requested due in schedule.
     */
    public Vector getProjectedRequestedDueInSchedule() {
	if (checkTimeSpanSet(projectedRequestedDueInSchedule) == null) return null;
	return new Vector(projectedRequestedDueInSchedule); 
    }


    /** Get the requested due in schedule.
     */
    public Vector getInactiveRequestedDueInSchedule() {
	if (checkTimeSpanSet(inactiveRequestedDueInSchedule) == null) return null;
	return new Vector(inactiveRequestedDueInSchedule); 
    }

    /** Get the projected requested due in schedule.
     */
    public Vector getInactiveProjectedRequestedDueInSchedule() {
	if (checkTimeSpanSet(inactiveProjectedRequestedDueInSchedule) == null) return null;
	return new Vector(inactiveProjectedRequestedDueInSchedule); 
    }


    /** Get the schedule that indicates the due-out (allocated)
	schedule for a labor asset.  This is similar to the due-out
	inventory schedule, but uses both the start and end times.
    */

  public Vector getDueOutLaborSchedule() {
      return getSchedule(dueOutLaborSchedule);
  }

  /**
     Get the schedule that indicates the due-out inventory for this asset.
     The schedule is from the allocations reported results from
     the allocations in the role schedules attached to the assets.
     @return Vector - the schedule for this asset in this cluster
  */
  
  public Vector getDueOutSchedule() {
      return getSchedule(dueOutSchedule);
  }

  public Vector getInactiveDueOutSchedule() {
      return getSchedule(inactiveDueOutSchedule);
  }

  public Vector getProjectedDueOutSchedule() {
      return getSchedule(projectedDueOutSchedule);
  }

  public Vector getProjectedMockDueOutSchedule() {
      return getSchedule(projectedMockDueOutSchedule);
  }

  public Vector getInactiveProjectedDueOutSchedule() {
      return getSchedule(inactiveProjectedDueOutSchedule);
  }

    /*  Get allocations to this.asset from the RoleSchedule.
     *  Create schedule where each element is based on an allocation result. */
  private Vector computeDueOutVector(boolean inactive) {
       return computeDueOutVectorWVerb(Constants.Verb.Withdraw, inactive);
  }


    /*  Get allocations to this.asset from the RoleSchedule.
     *  Create schedule where each element is based on an allocation result. */
  private Vector computeProjectedDueOutVector(boolean inactive) {
      return computeDueOutVectorWVerb(Constants.Verb.ProjectWithdraw, inactive);
  }
    

    /*  Get allocations to this.asset from the RoleSchedule.
     *  Create schedule where each element is based on an allocation result. */
  private Vector computeDueOutVectorWVerb(Verb compareVerb, boolean inactive){
    RoleSchedule roleSchedule = asset.getRoleSchedule();
    if(debug) {System.out.println("InventoryWrapper-Projected Due Outs:");}
    if (roleSchedule == null) {
      System.out.println("InventoryWrapper WARNING: no role schedule in asset");
      return null;
    }
    Enumeration e = roleSchedule.getRoleScheduleElements();
    if (e == null) {
      System.out.println("InventoryWrapper WARNING: no role schedule in role schedule");
      return null;
    }

    Vector due_outs = new Vector();
    while (e.hasMoreElements()) {
      Allocation allocation = (Allocation) e.nextElement();
      Task task = allocation.getTask();
      Verb dueOutTaskVerb= task.getVerb();

      if ((dueOutTaskVerb.equals(compareVerb))) {
	  if (TaskUtils.isProjection(task)) {
            Vector projectionScheduleExpansion = 
              getScheduleFromProjectionTask(task, inactive, allocation);
            due_outs.addAll(projectionScheduleExpansion);
	  } else {
              AllocationResult er = allocation.getEstimatedResult();
	      QuantityScheduleElement el = getScheduleFromAllocation(er);
	      if ((el != null) &&
		  (isInactive(allocation, el) == inactive)) {
		  due_outs.addElement(el); 
	      }
	  }
      }
    }
    return scheduleToNonOverlapVector(due_outs);
  }
    
  private boolean isInactive(Task task, QuantityScheduleElement el) {
    return isInactive(task, el.getStartTime());
  }

  private boolean isInactive(Allocation alloc, QuantityScheduleElement el) {
    return isInactive(alloc.getTask(), el.getStartTime());
  }

  private boolean isInactive(Allocation alloc, long time) {
    return isInactive(alloc.getTask(), time);
  }

  private boolean isInactive(Task task, long time) {
/*
    if (asset instanceof Inventory) {
      Inventory inv = (Inventory) asset;
      InventoryPG invpg = inv.getInventoryPG();
      /* Keeping the units straight is tricky. There are two time
         origins: time zero is the origin of the Java time standard
         (January 1970). Inventory start is an arbitrary day boundary
         time before interesting things happen to the inventory.
         day0 is the day (since time 0) of the start time of the inventory.
         today is measured since the start time of the inventory.
         (day0 + today) is today since time 0
         day is the day (since time 0) of the task.
         imputedDay should be the days between today and the day of the task.
         imputedDay = day - (day0 + today)
      */
/*
      int today = invpg.getToday(); // Days since starttime
      int day = invpg.convertTimeToDay(time);
      int imputedDay = day - today;
      double weight = invpg.getProjectionWeight().getProjectionWeight(task, imputedDay);

      if(debug) {
	  Date startDate = new Date(invpg.convertDayToTime(0));
	  System.out.println("InventoryWrapper::isInactive: Weight is " + weight);
	  System.out.println("Start time is " + startDate + " today is (days from them) " + today + " and time of element is " + day + " hence imputed is: " + imputedDay);

      }

      return weight < 0.5;
    }
*/    
    return false;
  }

    static private Vector scheduleToNonOverlapVector(Vector schedule) {
	Vector   nonOverlapVector;
	// make an alp schedule so we can use the alp utilities
	// to make it non-overlapping
	if (schedule == null || schedule.size() == 0)
	    return null;
	Schedule tmpCSchedule = getCSchedule(schedule);
	return scheduleToNonOverlapVector(tmpCSchedule);
    }

    static private Vector scheduleToNonOverlapVector(Schedule schedule) {
	Vector   nonOverlapVector;
	if (debug)  {
	    System.out.println("Original schedule");
	    printSchedule(schedule);
	}
	if (isOverlappingSchedule(schedule)) {
	    Schedule nonoverlapping =
		ScheduleUtilities.computeNonOverlappingSchedule(schedule);
	    nonOverlapVector = scheduleToVector(nonoverlapping);
	    if (debug)  {
		System.out.println("Is Overlapping::Computing non-overlapping schedule");
		printSchedule(nonOverlapVector);
	    }
	} else
	    nonOverlapVector = scheduleToVector(schedule);
	
	return nonOverlapVector;
    }

  private long getPreferredTime(Task task, int aspectType) {
    Preference preference = task.getPreference(aspectType);
    ScoringFunction scoringFunction = preference.getScoringFunction();
    AspectScorePoint pt = scoringFunction.getBest();
    AspectValue aspectValue = pt.getAspectValue();
    if (aspectValue instanceof TimeAspectValue)
      return ((TimeAspectValue)aspectValue).longValue();
    else 
      return aspectValue.longValue();
  }

  private Vector computeRequestedDueOutVector(boolean isInactive) {
      return computeRequestedDueOutVectorWVerb(Constants.Verb.WITHDRAW, isInactive);
  }

  private Vector computeProjectedRequestedDueOutVector(boolean isInactive) {
      return computeRequestedDueOutVectorWVerb(Constants.Verb.PROJECTWITHDRAW, isInactive);
  }

  private Vector computeRequestedDueOutVectorWVerb(String compareVerb, boolean isInactive) {
    RoleSchedule roleSchedule = asset.getRoleSchedule();
    Vector scheduleElements = new Vector();
    if (roleSchedule == null) {
      System.out.println("InventoryWrapper WARNING: no role schedule in asset");
      return null;
    }
    Enumeration e = roleSchedule.getRoleScheduleElements();
    if (e == null) {
      System.out.println("InventoryWrapper WARNING: no role schedule in role schedule");
      return null;
    }
    while (e.hasMoreElements()) {
      Allocation allocation = (Allocation) e.nextElement();
      Task task = allocation.getTask();
      if (task == null) { 
        System.out.println("InventoryWrapper WARNING: no allocation task in allocation");
        continue;
      }

      //// If not the right kind of task, then don't compute and add to schedule
      if (!(task.getVerb().equals(compareVerb))) { 
	  //System.out.println("InventoryWrapper::computeRequestedDueOutScheduleWVerb Unexpected verb: " + task.getVerb());
	  continue;
      }
      if (TaskUtils.isProjection(task)) {
	  Vector projectionScheduleExpansion = 
	      getScheduleFromProjectionTask(task, isInactive, null);
	  scheduleElements.addAll(projectionScheduleExpansion);
      } else { 
	  long endTime = getPreferredTime(task, AspectType.END_TIME);
	  double quantity = task.getPreferredValue(AspectType.QUANTITY);
	  if (debug) System.out.println("Adding " + compareVerb
                                        + " requested due out task: "
                                        + task.getUID()
                                        + " " + quantity
                                        + " at "+new Date(endTime));
	  // must have end time and quantity
	  if ((quantity != -1) && (endTime != -1)) { 
            if (isInactive(task, endTime) == isInactive) {
		  NewQuantityScheduleElement element;
		  element = new QuantityScheduleElementImpl(endTime - 1, endTime, quantity);
		  scheduleElements.addElement(element);
	      }
	  }
      }
    }
    return scheduleToNonOverlapVector(scheduleElements);
  }

   static private boolean isOverlappingSchedule(Schedule aSchedule) {
       if(aSchedule != null) {
	   Enumeration enum = aSchedule.getAllScheduleElements();
	   if(!enum.hasMoreElements()) return false; 
	   long last_time = aSchedule.getStartTime()-1;
	   while (enum.hasMoreElements()) {
	       ScheduleElement element = (ScheduleElement)enum.nextElement();
	       if (element.getStartTime() <= last_time) return true;
	       last_time = element.getEndTime();
	   }
       }
       return false;
   }

//   private boolean isOverlappingSchedule(Schedule aSchedule) {
//     long earliestTime = aSchedule.getStartTime();
//     long latestTime = aSchedule.getEndTime();
//     Calendar calendar = new GregorianCalendar();
//     calendar.setTime(new Date(earliestTime));
//     Vector scheduleElements = new Vector();
//     long time = earliestTime;
//     while (time <= latestTime) {
//       CountElementsAtTime counter = new CountElementsAtTime(time);
//       aSchedule.applyThunkToScheduleElements(counter);
//       if (counter.count > 1)
//         return true;
//       calendar.add(calendar.DAY_OF_YEAR, 1);
//       time = calendar.getTime().getTime();
//     }
//     return false;
//   }

  public Vector getRequestedDueOutSchedule() {
    return getSchedule(requestedDueOutSchedule);
  }

  public Vector getInactiveRequestedDueOutSchedule() {
    return getSchedule(inactiveRequestedDueOutSchedule);
  }

  public Vector getProjectedRequestedDueOutSchedule() {
    return getSchedule(projectedRequestedDueOutSchedule);
  }

  public Vector getProjectedRequestedMockDueOutSchedule() {
    return getSchedule(projectedRequestedMockDueOutSchedule);
  }

  public Vector getInactiveProjectedRequestedDueOutSchedule() {
    return getSchedule(inactiveProjectedRequestedDueOutSchedule);
  }


  /** Take a schedule of QuantityScheduleElements and
    make it into an COUGAAR schedule
    */

  private static Schedule getCSchedule(Collection mySchedule) {
    if(mySchedule == null) return new ScheduleImpl();
    Vector scheduleElements = new Vector();
    for (Iterator it = mySchedule.iterator(); it.hasNext();) {
      QuantityScheduleElement s = (QuantityScheduleElement)it.next();
      NewQuantityScheduleElement qse = GLMFactory.newQuantityScheduleElement();
      qse.setStartTime(s.getStartTime());
      qse.setEndTime(s.getEndTime());
      qse.setQuantity(s.getQuantity());
      scheduleElements.addElement(qse);
    }
    return GLMFactory.newQuantitySchedule(scheduleElements.elements(),
                                       PlanScheduleType.TOTAL_INVENTORY);
  }

 //   /** Shortfall = requestedDueOut - dueOut (shows as a positive shortage)
//     */

//    private void setDueOutShortfallSchedule() {
//      if (RequestedDueOutSchedule == null || dueOutSchedule == null)
//        return;
//      if (dueOutSchedule.size() == 0) {
//        if (debug) 
//  	  System.out.println("Due out schedule is empty; using requested due out as due out shortfall");
//        dueOutShortfallSchedule = dueOutSchedule;
//        return;
//      }

//      Schedule dueOut = getCSchedule(dueOutSchedule);
//      Enumeration scheduleElements =
//        RequestedDueOutSchedule.getAllScheduleElements();
//      if (!scheduleElements.hasMoreElements()) {
//        if (debug) System.out.println("Requested due out schedule is empty; ignoring due out shortfall");
//        return;
//      }
//      Schedule results = ScheduleUtilities.subtractSchedules(dueOut,RequestedDueOutSchedule);
//      dueOutShortfallSchedule = scheduleToVector(results);
//    }

//    public Vector getDueOutShortfallSchedule() {
//        return getSchedule(dueOutShortfallSchedule);
//    }

  public Vector getLaborSchedule() {
      return getSchedule(laborSchedule);
  }

  // for debugging
  static private void printSchedule(Schedule s) {
      if (s == null) return;
    Enumeration e = s.getAllScheduleElements();
    while (e.hasMoreElements()) {
      QuantityScheduleElement se = (QuantityScheduleElement)e.nextElement();
      System.out.println("Start date: " + shortDate(se.getStartTime()) +
			 " end date: " + shortDate(se.getEndTime()) +
			 " quantity: " + se.getQuantity());
    }
  }

  static private void printSchedule(Vector s) {
      if (s == null || s.isEmpty()) {
	  System.out.println("printSchedule() Empty Schedule");
	  return;
      }
      Enumeration e = s.elements();
      while (e.hasMoreElements()) {
	  printQuantityScheduleElement((QuantityScheduleElement)e.nextElement());
      }
  }

    static private void printQuantityScheduleElement(QuantityScheduleElement qse) {
      System.out.println("Start date: " + shortDate(qse.getStartTime()) +
			 " end date: " + shortDate(qse.getEndTime()) +
			 " quantity: " + qse.getQuantity());
    }

    static private String shortDate(long time) {
	String sdate = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(time));
	// map '9/8/00 12:00 AM' to ' 9/8/00 12:00 AM'
	while(sdate.length()<17){
	    sdate = " "+sdate;
	}
	return sdate;
    }
}

class CountElementsAtTime implements org.cougaar.util.Thunk {
  private long time;
  public int count = 0;
  public CountElementsAtTime(long t) {
    time = t;
  }
  public void apply(Object o) {
    ScheduleElement se = (ScheduleElement) o;
    if (time >= se.getStartTime()  && time < se.getEndTime()) {
      count++;
    }
  }
}
