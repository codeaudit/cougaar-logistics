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

package org.cougaar.logistics.plugin.inventory;


import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.MoreMath;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.PlanningFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.io.Serializable;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.asset.SupplyClassPG;

/** Provides convenience methods. */
public class TaskUtils extends PluginHelper implements Serializable { // revisit making Serializable later...

    private transient Logger logger;
    private transient UtilsProvider utilProvider;
    private transient AssetUtils assetUtils;

    public TaskUtils(UtilsProvider provider) {
	super();
	utilProvider = provider;
	logger = (Logger)utilProvider.getLoggingService(this);
	assetUtils = utilProvider.getAssetUtils();
    }

    public TaskUtils(LoggingService aLogger) {
	super();
	utilProvider = null;
	logger = (Logger)aLogger;
	assetUtils = new AssetUtils(aLogger);
    }

  /** @param t the task
   *  @param type type identification string
   *  @return true if the task's OFTYPE preposition's indirect object is 
   *  an asset with nomeclature equal to 'type'.*/
  public static boolean isTaskOfType(Task t, String type) {
    PrepositionalPhrase pp =t.getPrepositionalPhrase(Constants.Preposition.OFTYPE) ;
    if (pp != null) {
      Object obj = pp.getIndirectObject();
      if (obj instanceof Asset) {
	Asset a = (Asset)obj;
	return a.getTypeIdentificationPG().getTypeIdentification().equals(type);
      } 
    }
    return false;
  }

 /** @param t the task
   *  @param type type identification string
   *  @return true if the task's OFTYPE preposition's indirect object is 
   *  an string with nomeclature equal to 'type'.*/
  public static boolean isTaskOfTypeString(Task t, String type) {
    PrepositionalPhrase pp =t.getPrepositionalPhrase(Constants.Preposition.OFTYPE) ;
    if (pp != null) {
      Object obj = pp.getIndirectObject();
      if (obj instanceof String) {
	return ((String)obj).equals(type);
      } 
    }
    return false;
  }

  public boolean isDirectObjectOfType(Task t, String type) {
    boolean result = false;
    Asset asset = t.getDirectObject();
    // Check for aggregate assets and grab the prototype
    if (asset instanceof AggregateAsset) {
      asset = ((AggregateAsset)asset).getAsset();
    }
    try {
      SupplyClassPG pg = (SupplyClassPG)asset.searchForPropertyGroup(SupplyClassPG.class);
      if (pg != null) {
	result = type.equals(pg.getSupplyType());
	if((result == true) && (type.equals("PackagedPOL")) && 
	  asset.getTypeIdentificationPG().getTypeIdentification().endsWith("Aggregate")) {
	  logger.debug("\n direct object type... type for plugin is: " +
		       type + "]" + " type for DO is: [" + pg.getSupplyType() + "]");
	}
	  
      }
      else {
	logger.debug("No SupplyClassPG found on asset "+ this.taskDesc(t));
      }
    } catch (Exception e) {
      logger.error("Tasks DO is null "+ this.taskDesc(t)+"\n"+e);
    }
    return result;
  }

  // utility functions
  public String taskDesc(Task task) {
    if (isProjection(task)) {
      return task.getUID() + ": "
	+ task.getVerb()+"("+
	  getDailyQuantity(task)+" "+
	  getTaskItemName(task)+") "+
	  getTimeUtils().
	  dateString(new Date(getStartTime(task)))+
	  "  -  " +
	  getTimeUtils().
	  dateString(new Date(getEndTime(task)));
    } else {
      return task.getUID() + ": "
	+ task.getVerb()+"("+
	  getQuantity(task)+" "+
	  getTaskItemName(task)+") "+
	  getTimeUtils().
	  dateString(new Date(getEndTime(task)));
    }
  }


    public String getTaskItemName(Task task){
	Asset prototype = (Asset)task.getDirectObject();
	if (prototype == null) return "null";
	return assetUtils.assetDesc(prototype);
    }

  public static boolean isMyRefillTask(Task task, String myOrgName) {
    PrepositionalPhrase pp =task.getPrepositionalPhrase(Constants.Preposition.REFILL);
    if (pp == null) {
      return false;
    }
    pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String orgName = (String)io;
      if ( orgName.equals(myOrgName)) {
	return true;
      }
    }
    return false;
  }

 public static boolean isMyInventoryProjection(Task task, String myOrgName) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String orgName = (String)io;
      if ( orgName.equals(myOrgName)) {
	pp = task.getPrepositionalPhrase(Constants.Preposition.MAINTAINING);
	if (pp != null) {
	  try {
	    if (((MaintainedItem)pp.getIndirectObject()).getMaintainedItemType().equals("Inventory")) {
	      return true;
	    } 
	  } catch (ClassCastException exc) {
	    return false;
	  }
	}
      }
    }
    return false;
  }


  public boolean isReadyForTransport(Task task){
      PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.READYFORTRANSPORT);
      return (pp != null);
  }

  public boolean isMyDemandForecastProjection(Task task,String orgName) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
        String taskOrg = (String) io;
        if(taskOrg.equals(orgName)) {
          pp = task.getPrepositionalPhrase(Constants.Preposition.REFILL);
          return (pp == null);
        }
    }
    return false;
  }



  /** return the preference of the given aspect type.  Returns null if
   *  the task does not have the given aspect. */
  public static double getPreference(Task t, int aspect_type) {
    Preference p = t.getPreference(aspect_type);
    if (p == null) return Double.NaN;

    AspectScorePoint asp = p.getScoringFunction().getBest();
    return asp.getValue();
  }


  public static boolean isProjection(Task t) {
    return t.getPreference(AlpineAspectType.DEMANDRATE) != null;
  }

  public static boolean isSupply(Task t) {
    return !isProjection(t);
  }

  public boolean isLevel2(Task t) {
    return assetUtils.isLevel2Asset(t.getDirectObject());
  }

  // TASK PREFERENCE UTILS

  public static double getQuantity(Task task) {
    return getPreferenceBestValue(task, AspectType.QUANTITY);
  }

  public static Preference createDemandRatePreference(PlanningFactory rf, Rate rate) {
    ScoringFunction sf = ScoringFunction
      .createStrictlyAtValue(AspectValue.newAspectValue(AlpineAspectType.DEMANDRATE,
                                                        rate));
    return rf.newPreference(AlpineAspectType.DEMANDRATE, sf);
  }

    /**
   * Compare the preferences of two tasks return true if the tasks
   * have preferences for the same aspect types and if all
   * corresponding AspectValues are nearly equal.
   * This needs to be fixed to be more efficient.
   **/
  public static boolean comparePreferences(Task a, Task b) {
    return comparePreferencesInner(a, b) && comparePreferencesInner(b, a);
  }

  private static boolean comparePreferencesInner(Task a, Task b) {
    Enumeration ae = a.getPreferences();
    while (ae.hasMoreElements()) {
      Preference p = (Preference) ae.nextElement();
      int at = p.getAspectType();
      double av = p.getScoringFunction().getBest().getValue();
      double bv = getPreferenceBestValue(b, at);
      if (!MoreMath.nearlyEquals(av, bv, 0.0001)) return false;
    }
    return true;
  }

  /** @param task
   *  @return Value of the FOR Preposition if available, else null
   */
  public static Object getCustomer(Task task) {
    PrepositionalPhrase pp_for = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    Object org;
    if (pp_for != null) {
      org = pp_for.getIndirectObject();
      return org;
    }
    return null;
  }

  public Rate getRate(Task task) {
    AspectValue best = getPreferenceBest(task, AlpineAspectType.DEMANDRATE);
    if (best == null)
      logger.error("TaskUtils.getRate(), Task is not Projection :"+taskDesc(task));
    return ((AspectRate) best).getRateValue();
  }

  public double getDailyQuantity(Task task) {
    if (isProjection(task)) {
      return getDailyQuantity(getRate(task));
    } else {
      return getQuantity(task);
    }
  }


  public static double getDailyQuantity(Rate r) {
    if (r instanceof FlowRate) {
      return ((FlowRate) r).getGallonsPerDay();
    } else if (r instanceof CountRate) {
      return ((CountRate) r).getEachesPerDay();
    } else if (r instanceof MassTransferRate) {
      return ((MassTransferRate) r).getShortTonsPerDay();
    } else {
      return Double.NaN;
    }
  }

  /** 
   * Given a Scalar, return a double value representing
   * Gallons for Volume,
   * Eaches for Count and
   * Short Tons for Mass.
   **/
  public double getDouble(Scalar measure) {
    double result = Double.NaN;
    if (measure instanceof Volume) {
      result = ((Volume)measure).getGallons();
    } else if (measure instanceof Count) {
      result = ((Count)measure).getEaches();
    } else if (measure instanceof Mass) {
      result = ((Mass)measure).getShortTons();
    } else {
      logger.error("InventoryBG.getDouble(), Inventory cannot determine type of measure");
    }
    return result;
  }	

  public static double getQuantity(AllocationResult ar) {
    return getARAspectValue(ar, AspectType.QUANTITY);
  }

    // Hand in the demandRate from a phase of particular allocation result
    // and its parent task.  This function basically handles the
    // contained demand rate result and returns the corresponding
    // daily rate.    If its fuel (FlowRate) that's already gallons
    // per day, otherwise its eaches per millisecond and should be
    // multiplied correspondingly.
  public double convertResultsToDailyRate(Task task, double demandRate) {
      if(isProjection(task)) {
	  Rate r = getRate(task);
	  if(!(r instanceof FlowRate)) {
	      return demandRate * TimeUtils.SEC_PER_DAY;
	  }
      }
      return demandRate;
  }

  public double getQuantity(Task task, AllocationResult ar) {
      if(isProjection(task)) {
// 	  logger.warn("TaskUtils::getting qty from projection!");
	  return convertResultsToDailyRate(task,
					   getARAspectValue(ar, AlpineAspectType.DEMANDRATE));
      }
      else {
	  return getQuantity(ar);
      }
  }

  public double getQuantity(Task task, AllocationResult ar, long time_spanned) {
    if (isProjection(task)) {
      Rate rate = getARAspectRate(ar);
      Duration d = Duration.newMilliseconds((double)time_spanned);
      Scalar scalar = (Scalar)rate.computeNumerator(d);
      return getDouble(scalar);
    } else {
      return getQuantity(ar);
    }
  }

  public static Rate getARAspectRate(AllocationResult ar) {
    if (ar == null) return null;
    AspectValue[] avs = ar.getAspectValueResults();
    for (int ii = 0; ii < avs.length; ii++) {
      if (avs[ii].getAspectType() == AlpineAspectType.DEMANDRATE) {
	return ((AspectRate)avs[ii]).getRateValue();
      }
    }
    return null;
  }

    public TimeUtils getTimeUtils() {return utilProvider.getTimeUtils();}

  public static Collection getUnallocatedTasks(Collection tasks, Verb verb) {
    Iterator taskIt = tasks.iterator();
    ArrayList list = new ArrayList();
    Task task;
    while (taskIt.hasNext()) {
      task = (Task)taskIt.next();
      if ((task.getPlanElement() == null) && (task.getVerb().equals(verb))){
        list.add(task);
      }
    }
    return list;
  }
}






