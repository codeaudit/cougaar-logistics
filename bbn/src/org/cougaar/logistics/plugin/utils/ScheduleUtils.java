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
package org.cougaar.logistics.plugin.utils;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleType;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.PluginDelegate;

import java.util.*;

import org.cougaar.util.log.Logger;
import org.cougaar.util.TimeSpan;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;

import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.plan.NewQuantityScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleType;

/** Provide convenience methods for creating objects. */
public class ScheduleUtils {
  
  protected static final long MSEC_PER_DAY =  86400000;
  protected static final long SECOND_IN_MS =  1000; 
  
  private transient Logger logger;
  
  public ScheduleUtils(UtilsProvider provider) {
    if(provider == null) {
      logger = NullLoggingServiceImpl.getNullLoggingServiceImpl();
    }
    else {
      logger = (Logger)provider.getLoggingService(this);
    }
  }
  
  
  public static Vector convertEnumToVector(Enumeration e) {
    Vector v = new Vector();
    while (e.hasMoreElements()) {
      v.addElement(e.nextElement());
    }
    return v;
  }
  
  
  public static ScheduledContentPG createScheduledContentPG( Asset a, Schedule s) {
    NewScheduledContentPG scp = PropertyGroupFactory.newScheduledContentPG();
    scp.setAsset(a);
    scp.setSchedule(s);
    return scp;
  }

  // truncates time span
  // note amount can be positive or negative.
  public static Schedule adjustSchedule(Schedule sched, long start, long end, int amount, long time_incr) {
    return adjustSchedule(sched, truncTime(start, time_incr), 
			  truncTime(end+1, time_incr)-1, amount);
  }
  
  // note amount can be positive or negative.
  public static Schedule adjustSchedule(Schedule sched, long start, long end, int amount) {
    Schedule simple_sched = buildSimpleQuantitySchedule(amount, start, end);
    return ScheduleUtilities.addSchedules(sched, simple_sched);
  }
  
  public static Schedule buildSimpleQuantitySchedule(double qty, 
						     long start, long end) {
    QuantityScheduleElement nqse = buildQuantityScheduleElement(qty, 
								start, 
								end);
    Vector sched_el = new Vector();
    sched_el.addElement(nqse);
    
    return GLMFactory.newQuantitySchedule(sched_el.elements(), 
					  PlanScheduleType.TOTAL_INVENTORY);
  }
  
  public static Schedule buildSimpleQuantitySchedule(int qty, long start, 
						     long end, long time_incr) {
    return  buildSimpleQuantitySchedule(qty, truncTime(start, time_incr),
					truncTime(end+1, time_incr)-1);
  }
  
  public static long truncTime(long time, long time_incr) {
    return (time/time_incr)*time_incr;
  }
  
  public static QuantityScheduleElement buildQuantityScheduleElement(double qty, long start, long end) {
    NewQuantityScheduleElement e = GLMFactory.newQuantityScheduleElement();
    e.setQuantity(qty);
    e.setStartTime(start);
    e.setEndTime(end);
    return e;
  }

  public static QuantityScheduleElement buildQuantityScheduleElement(double qty, long start, long end, long time_incr) {
    return buildQuantityScheduleElement(qty, truncTime(start, time_incr), 
					truncTime(end+1, time_incr)-1);
  }
  
  
  // note amount can be positive or negative.
  // adjust quantity from start until end of schedule
  // if start is after end of schedule, 
  // append new element from start to start + a day
  public static Schedule adjustSchedule(Schedule sched, long start, int amount) {
    long end = sched.getEndTime();
    if (end > start) end = start+MSEC_PER_DAY;
    return adjustSchedule(sched, start, end, amount);
  }
  
  public static boolean isOffendingSchedule(Schedule sched) {
    long start = sched.getStartTime() -1;
    QuantityScheduleElement qse;
    Enumeration elements = sched.getAllScheduleElements();
    while (elements.hasMoreElements()){
      qse = (QuantityScheduleElement)elements.nextElement();
      if (qse.getStartTime() < start) {
	return true;
      }
      start = qse.getStartTime();
    }
    return false;
  }
  
  public static ScheduleElement getElementWithTime(Schedule s, long time) {
    ScheduleElement se = null;
    if (s != null) {
      Collection c = s.getScheduleElementsWithTime(time);
      if (!c.isEmpty()) {
	se = (ScheduleElement)c.iterator().next();
      }
    }
    return se;
  }
  
  protected Schedule createConsumerSchedule(Collection col) {
    ScheduledContentPG scp;
    int qty;
    Asset consumer, asset;
    Schedule consumerSched = null;
    Iterator list = col.iterator();
    while (list.hasNext()) {
      asset = (Asset)list.next();
      if (asset instanceof AggregateAsset) {
	AggregateAsset aa = (AggregateAsset)asset;
	qty = (int)aa.getQuantity();
	consumer = aa.getAsset();
      } else {
	qty = 1;
	consumer = asset.getPrototype();
      }
      if (consumer == null) {
	logger.error("Missing prototype on asset: "+asset);
	continue;
      }
      Schedule roleSched = asset.getRoleSchedule().getAvailableSchedule();
      if (roleSched == null) {
	logger.error("Missing RoleSchedule on asset: "+asset);
	continue;
      }
      long start = roleSched.getStartTime();
      long end = roleSched.getEndTime();
      if (start >= end-1) {
	logger.error("Bad schedule time(s): start "+TimeUtils.dateString(start)+
		     ", end "+TimeUtils.dateString(end)+" for asset: "+asset);
	continue;
      }
      // ScheduleUtils schedule methods have a granularity - so 1000 = 1 sec
      // so everything is scheduled on an even second
      if (consumerSched == null) {
	consumerSched = buildSimpleQuantitySchedule (qty, start, end, SECOND_IN_MS);
      } else {
	consumerSched = adjustSchedule (consumerSched, start, end, qty, SECOND_IN_MS);
      }
    }
    return consumerSched;
  }

  public static Schedule newObjectSchedule(Enumeration elements) {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(PlanScheduleElementType.OBJECT);
    s.setScheduleType(ScheduleType.OTHER);
    s.setScheduleElements(elements);
    return s;
  }


  /**
   * Here is what this method does (Ray Tomlinson). Merges a number
   * of parameter schedules into a combined schedule. In the
   * combined schedule, the "parameter" of each element is a Vector
   * of the parameters from each of the input schedules. For
   * elements not covered by the input schedule, the corresponding
   * Vector element is null. The output schedule elements correspond
   * to the intersections of the elements or inter-element gaps of
   * the input schedules. No schedule element is generated for time
   * spans where none of the input schedules has an element.
   * Conversely, all elements of the merged schedule have at least
   * one non-null parameter.
   *
   * There is an assumption that the elements of the input schedules
   * and the output schedule are non-overlapping.
   **/
  public Schedule getMergedSchedule(Vector parameterSchedules) {
    // sets scheds to Enumerations of schedule elements
    // sets intervals to the initial schedule element within each schedule
    Schedule mergedSchedule;
    Vector scheds = parameterSchedules;
    int num_params = scheds.size();
    ObjectScheduleElement[] intervals = new ObjectScheduleElement[num_params];
    Enumeration[] enums = new Enumeration[num_params];
    logger.debug("getMergedSchedule "+num_params + " num params");
    ObjectScheduleElement ose;
    long start = TimeSpan.MAX_VALUE;
    for (int ii = 0; ii < num_params; ii++) {
      enums[ii] = ((Schedule) scheds.get(ii)).getAllScheduleElements();
      if (enums[ii].hasMoreElements()) {
	ose = (ObjectScheduleElement) enums[ii].nextElement();
	intervals[ii] = ose;
	if (ose.getStartTime() < start) {
	  start = ose.getStartTime();
	}
      } else {
	intervals[ii] = null; // Empty schedule
      }
    }

    Vector result_sched = new Vector();
    long end = TimeSpan.MIN_VALUE;
    while (end != TimeSpan.MAX_VALUE) {
      Vector params = new Vector(num_params);
      params.setSize(num_params);
      boolean haveParams = false;
      end = TimeSpan.MAX_VALUE;
      for (int ii = 0; ii < num_params; ii++) {
	params.set(ii, null);// Presume no element for schedule(ii)
	// check if interval good
	ose = intervals[ii];
	if (ose != null) {
	  if (ose.getEndTime() <= start) {
	    // This has already been covered; Step to next
	    if (!enums[ii].hasMoreElements()) {
	      // ran off end of schedule(ii)
	      intervals[ii] = null;
	      continue;
	    }
	    ose = (ObjectScheduleElement) enums[ii].nextElement();
	    intervals[ii] = ose;
	  }
	  if (ose.getStartTime() > start) {
	    // ose is _not_ part of this result
	    // element, it's later (there is a gap)
	    if (ose.getStartTime() < end) {
	      // This result element ends not later
	      // than the start of this pending element
	      end = ose.getStartTime();
	    }
	    continue;
	  }
	  // search for earliest end time 
	  if (ose.getEndTime() < end) {
	    end = ose.getEndTime();
	  }
	  // add current param to list
	  params.set(ii, ose.getObject());
	  haveParams = true;
	}
      }
      if (haveParams) {
	result_sched.add(new ObjectScheduleElement(start, end, params));
      }
      start = end;
    }
    mergedSchedule = newObjectSchedule(result_sched.elements());
    logger.debug("getMergedSchedule created mergedSchedule "+result_sched.size());
    return mergedSchedule;
  }

}
