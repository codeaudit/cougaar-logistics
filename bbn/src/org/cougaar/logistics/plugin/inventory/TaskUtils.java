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

package org.cougaar.logistics.plugin.inventory;


import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.util.MoreMath;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.measure.*;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.io.Serializable;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.asset.SupplyClassPG;

/** Provides convenience methods. */
public class TaskUtils extends PluginHelper implements Serializable { // revisit making Serializable later...

    private transient Logger logger;
    private transient InventoryPlugin invPlugin;

    public TaskUtils(InventoryPlugin aPlugin) {
	super();
	invPlugin = aPlugin;
	logger = (Logger)invPlugin.getLoggingService(this);
    }

    public TaskUtils(LoggingService aLogger) {
	super();
	invPlugin = null;
	logger = (Logger)aLogger;
    }

  /** @param task
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
	new Date(getStartTime(task))+
	"  -  " +
	new Date(getEndTime(task));
    } else {
      return task.getUID() + ": "
	+ task.getVerb()+"("+
	getQuantity(task)+" "+
	getTaskItemName(task)+") "+
	new Date(getEndTime(task));
    }
  }

    public String getTaskItemName(Task task){
	Asset prototype = (Asset)task.getDirectObject();
	if (prototype == null) return "null";
	return invPlugin.getAssetUtils().assetDesc(prototype);
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

  // TASK PREFERENCE UTILS

  public static double getQuantity(Task task) {
    return getPreferenceBestValue(task, AspectType.QUANTITY);
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

}






