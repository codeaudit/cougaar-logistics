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

package org.cougaar.logistics.servlet;


import java.util.*;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.util.log.Logger;
import org.cougaar.util.Random;
import java.io.PrintWriter;


/**
 * The LocalLevel2Mapper expands a Level2Task into it's adjusted
 * (by Level6 tasks) equivilent
 *
 * @see Level2TranslatorPlugin
 * @see Level2TranslatorModule
 **/

public class MapperPrinter {

  protected HashMap level2To6Map;
  protected HashMap endTimeMap;
  protected TreeMap supplyTaskMap;
  protected TreeMap projTaskMap;

  protected transient Logger logger;
  protected transient TaskUtils taskUtils;

  protected MapperServlet servlet;
  protected PrintWriter writer;
  protected String supplyType;


  public MapperPrinter(MapperServlet aServlet) {
    level2To6Map = new HashMap();
    endTimeMap = new HashMap();
    supplyTaskMap = new TreeMap();
    projTaskMap = new TreeMap();
    servlet = aServlet;
    logger = (Logger)servlet.getLoggingService(this);
  }



  public void printDemandStats(Collection level2Tasks,
			       Collection level6Tasks,
			       Collection supplyTasks,
			       String aSupplyType,
			       String org,
			       PrintWriter out) {

      writer=out;
      supplyType = aSupplyType;


      mapSupplyTasks(supplyTasks);
      mapProjTasks(level6Tasks);

      double totalL2Qty = getTotalProjectionQty(level2Tasks);
      double totalL6ProjQty = getTotalProjectionQty(level6Tasks);
      double totalL6ActualQty = getTotalSupplyQty(supplyTasks);
      double countedL6ProjQty = getCountedProjectionQty(level6Tasks);
      double countedL2ProjQty = getCountedProjectionQty(level2Tasks);

      /* MWD Remove
      if(!level2Tasks.isEmpty())
	  writer.println("Level 2 Item key :" + getTaskKey((Task)level2Tasks.iterator().next()));
      */

      writer.println("<head>");
      writer.println("<title> Total Demand for " + org + "</title>");
      writer.println("<body><p><h4> Total Demand for " + org + "</h4><p>");

      writer.println("On " + getTimeUtils().dateString(servlet.currentTimeMillis()) + ". At Agent: " + org + " - " + supplyType + "<br>");
      writer.println("   - Total Actual Demand:" + totalL6ActualQty + "<br>");
      writer.println("   - Total L6 Projected Demand:" + totalL6ProjQty + "<br>");
      writer.println("   - Total Level 2 Demand: " + totalL2Qty + "<br>" );
      writer.println("   - Total Counted L6 Projected Demand: " + countedL6ProjQty + "<br>");
      writer.println("   - Total Counted L2 Projected Demand: " + countedL2ProjQty + "<br>");

      writer.println("</p><p><br><b> Actuals </b><p>");
      printSupplyTasksByCustDodic();
      writer.println("</p><p><br><b> Counted Projected Demand </b><p>");
      printProjTasksByCustDodic();
      writer.println("</body>");
      
  }

  public void printSupplyTasksByCustDodic() {
      Set entries = supplyTaskMap.entrySet();
      Iterator entryIt = entries.iterator();
      while(entryIt.hasNext()) {
	  Map.Entry entry = (Map.Entry) entryIt.next();
	  Collection supplyTasks = (Collection) entry.getValue();
	  double totalDOQty = getTotalSupplyQty(supplyTasks);	  
	  writer.println("Actual: " + entry.getKey() + " -: " + totalDOQty + "<br>");    
      }	  
  }

  public void printProjTasksByCustDodic() {
      Set entries = projTaskMap.entrySet();
      Iterator entryIt = entries.iterator();
      while(entryIt.hasNext()) {
	  Map.Entry entry = (Map.Entry) entryIt.next();
	  Collection projTasks = (Collection) entry.getValue();
	  double totalDOQty = getCountedProjectionQty(projTasks);	  
	  writer.println("Proj: " + entry.getKey() + " -: " + totalDOQty + "<br>");    
      }	  
  }


  protected long getCountedStartTime(Task task) {
    long start = getTaskUtils().getStartTime(task);
    long legalProjectionStart = start;
    Long custLastActual = (Long) endTimeMap.get(getTaskKey(task));
    if (custLastActual != null) {
      legalProjectionStart = custLastActual.longValue() + getTimeUtils().MSEC_PER_DAY;
    }
    return Math.max(start, legalProjectionStart);
  }

  public double getCountedProjectionQty(Collection projTasks) {
      Iterator projTaskIt = projTasks.iterator();
      double totalQty=0.0;
      while(projTaskIt.hasNext()) {
	  Task projTask = (Task)projTaskIt.next();
	  long countedStartTime = getCountedStartTime(projTask);
	  long end = getTaskUtils().getEndTime(projTask);
	  if(end > countedStartTime) {
	      totalQty+=deriveTotalQty(countedStartTime, end, projTask);
	  }
      }
      return totalQty;
  }


  public double getTotalProjectionQty(Collection projTasks) {
      Iterator projTaskIt = projTasks.iterator();
      double totalQty=0.0;
      while(projTaskIt.hasNext()) {
	  Task projTask = (Task)projTaskIt.next();
	  long start = getTaskUtils().getStartTime(projTask);
	  long end = getTaskUtils().getEndTime(projTask);
	  totalQty += deriveTotalQty(start, end, projTask);
      }
      return totalQty;
  }

  public double getTotalSupplyQty(Collection supplyTasks) {
      Iterator supplyTaskIt = supplyTasks.iterator();
      double totalQty=0.0;
      while(supplyTaskIt.hasNext()) {
	  Task supplyTask = (Task)supplyTaskIt.next();
	  long end = getTaskUtils().getEndTime(supplyTask);
	  //logger.warn("MapperSupplyTask: " + getTaskUtils().taskDesc(supplyTask));
	  totalQty += getTaskUtils().getQuantity(supplyTask);
      }
      return totalQty;
  }

  protected TaskUtils getTaskUtils() { return servlet.getTaskUtils(); }
  protected TimeUtils getTimeUtils() { return servlet.getTimeUtils(); }

  protected void mapCustomerEndTimes(Collection supplyTasks) {
    endTimeMap.clear();
    Iterator supplyTaskIt = supplyTasks.iterator();
    while (supplyTaskIt.hasNext()) {
      Task supplyTask = (Task) supplyTaskIt.next();
      long endTime = getTaskUtils().getEndTime(supplyTask);
      Object org = getTaskUtils().getCustomer(supplyTask);
      if (org != null) {
        Long lastActualSeen = (Long) endTimeMap.get(org);
        if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
          endTimeMap.put(org, new Long(endTime));
        }
      }
    }
  }

  protected String getTaskKey(Task task) {
      Object org = getTaskUtils().getCustomer(task);
      String taskItem = getTaskUtils().getTaskItemName(task);
      String key=taskItem + org.toString();
      return key;
  }

  protected String getLevel2TaskKey(Task task) {
      Object org = getTaskUtils().getCustomer(task);
      String assetName = "Level2" + supplyType;
      String taskItem = assetName + "(" + assetName + " asset)";
      String key=taskItem + org.toString();
      return key;
  }

  protected void mapSupplyTasks(Collection supplyTasks) {
    endTimeMap.clear();
    supplyTaskMap.clear();

    Iterator supplyTaskIt = supplyTasks.iterator();
    while (supplyTaskIt.hasNext()) {
      Task supplyTask = (Task) supplyTaskIt.next();
      long endTime = getTaskUtils().getEndTime(supplyTask);
      String key=getTaskKey(supplyTask);
      if (key != null) {
        Long lastActualSeen = (Long) endTimeMap.get(key);
        if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
          endTimeMap.put(key, new Long(endTime));
        }
	HashSet tasks = (HashSet) supplyTaskMap.get(key);
	if(tasks == null) {
	    tasks = new HashSet();
	    supplyTaskMap.put(key,tasks);
	}
	tasks.add(supplyTask);
      }
      String l2Key = getLevel2TaskKey(supplyTask);
      Long lastL2ActualSeen = (Long) endTimeMap.get(l2Key);
      if ((lastL2ActualSeen == null) || (endTime > lastL2ActualSeen.longValue())) {
          endTimeMap.put(l2Key, new Long(endTime));
      }      
    }
  }

  protected void mapProjTasks(Collection projTasks) {
    projTaskMap.clear();
    Iterator projTaskIt = projTasks.iterator();
    while (projTaskIt.hasNext()) {
      Task projTask = (Task) projTaskIt.next();
      String key=getTaskKey(projTask);
      if (key != null) {
	HashSet tasks = (HashSet) projTaskMap.get(key);
	if(tasks == null) {
	    tasks = new HashSet();
	    projTaskMap.put(key,tasks);
	}
	tasks.add(projTask);
      }      
    }
  }

  protected void mapLevel2ToLevel6(Collection level2s, Collection level6s) {
    //alreadyMapped is only here to print out the error below.   This turns out
    //to be a nominal case where level 6s overlap more than one level2.  Commented
    //out the debug code for right now.  Can uncomment if any need to re-examine.
    //HashSet alreadyMapped = new HashSet();
    level2To6Map.clear();
    Iterator level2It = level2s.iterator();
    while (level2It.hasNext()) {
      Task level2Task = (Task) level2It.next();
      long l2StartTime = getTaskUtils().getStartTime(level2Task);
      long l2EndTime = getTaskUtils().getEndTime(level2Task);
      Object l2Cust = getTaskUtils().getCustomer(level2Task);
      Iterator level6It = level6s.iterator();
      ArrayList mappedL6s = new ArrayList();
      while (level6It.hasNext()) {
        Task level6Task = (Task) level6It.next();
        long l6StartTime = getTaskUtils().getStartTime(level6Task);
        long l6EndTime = getTaskUtils().getEndTime(level6Task);
        Object l6Cust = getTaskUtils().getCustomer(level2Task);
        if ((l6StartTime < l2EndTime) &&
            (l6EndTime > l2StartTime)) {
          if (l2Cust.equals(l6Cust)) {
            mappedL6s.add(level6Task);
            /**
            if ((alreadyMapped.contains(level6Task)) &&
                logger.isWarnEnabled()) {
              //Apparently lots overlap commented out alreadyMapped and all debug related code.
              logger.warn("The following task has already been mapped: " + level6Task.getUID() + " startTime: " +
                          new Date(l6StartTime) + " endTime: " +
                          new Date(l6EndTime) + ".  And the new overlapping L2 Task startTime " + new Date(l2StartTime) +
                          " and endTime is:" + new Date(l2EndTime));
            } else {
              alreadyMapped.add(level6Task);
            }
            **/
          } else {
            logger.error("Unexpected Customer of level2Task " + l2Cust + " differs from level6 cust:" + l6Cust);
          }
        }
      }
      level2To6Map.put(level2Task, mappedL6s);
    }
  }

  protected double deriveTotalQty(long bucketStart, long bucketEnd, Collection projTasks) {
    Iterator tasksIt = projTasks.iterator();
    double totalQty = 0.0;
    while (tasksIt.hasNext()) {
      Task projTask = (Task) tasksIt.next();
      double qty = deriveTotalQty(bucketStart, bucketEnd, projTask);
      totalQty += qty;
    }
    return totalQty;
  }


  protected double deriveTotalQty(long bucketStart, long bucketEnd, Task projTask) {

    long taskStart = getTaskUtils().getStartTime(projTask);
    long taskEnd = getTaskUtils().getEndTime(projTask);
    long start = Math.max(taskStart, bucketStart);
    long end = Math.min(taskEnd, bucketEnd);
    double qty = 0.0;
    //duration in seconds
    if(start < end) {
	double duration = ((end - start) / 1000);
	Rate rate = getTaskUtils().getRate(projTask);
	qty = (getBaseUnitPerSecond(rate) * duration);
    }
    return qty;
  }

  protected static double getBaseUnitPerSecond(Rate rate) {
    if (rate instanceof CostRate) {
      return ((CostRate) rate).getDollarsPerSecond();
    } else if (rate instanceof CountRate) {
      return ((CountRate) rate).getEachesPerSecond();
    } else if (rate instanceof FlowRate) {
      return ((FlowRate) rate).getGallonsPerSecond();
    } else if (rate instanceof MassTransferRate) {
      return ((MassTransferRate) rate).getShortTonsPerSecond();
    } else if (rate instanceof TimeRate) {
      return ((TimeRate) rate).getHoursPerSecond();
    } // if
    return 0.0;
  }

  protected Rate newRateFromUnitPerSecond(Rate rate, double unitsPerSecond) {
    if (rate instanceof CostRate) {
      return (CostRate.newDollarsPerSecond(unitsPerSecond));
    } else if (rate instanceof CountRate) {
      return (CountRate.newEachesPerSecond(unitsPerSecond));
    } else if (rate instanceof FlowRate) {
      return (FlowRate.newGallonsPerSecond(unitsPerSecond));
    } else if (rate instanceof MassTransferRate) {
      return (MassTransferRate.newShortTonsPerSecond(unitsPerSecond));
    } else if (rate instanceof TimeRate) {
      return (TimeRate.newHoursPerSecond(unitsPerSecond));
    } // if

    if (logger.isErrorEnabled()) {
      logger.error("Unknown rate type");
    }
    return (CountRate.newEachesPerSecond(unitsPerSecond));
  }

}


