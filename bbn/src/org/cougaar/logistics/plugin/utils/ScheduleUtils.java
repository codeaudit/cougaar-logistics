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

import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.plan.NewQuantityScheduleElement;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScheduleType;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.log.Logger;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/** Provide convenience methods for creating objects. */
public class ScheduleUtils {

  protected static final long MSEC_PER_DAY =  86400000;
  protected static final long SECOND_IN_MS =  1000;

  private transient Logger logger;

  public ScheduleUtils(UtilsProvider provider) {
    if(provider == null) {
      logger = NullLoggingServiceImpl.getLoggingService();
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

  public Schedule createConsumerSchedule(Collection col) {
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

  public Schedule convertQuantitySchedule (Schedule qty_sched) {
    ObjectScheduleElement element;
    QuantityScheduleElement qty_el;
    Vector sched_els = new Vector();
    Enumeration qty_els = qty_sched.getAllScheduleElements();
    while (qty_els.hasMoreElements()) {
      qty_el = (QuantityScheduleElement) qty_els.nextElement();
      element = new ObjectScheduleElement(qty_el.getStartDate(), qty_el.getEndDate(),
					  new Double(qty_el.getQuantity()));
      sched_els.addElement(element);
    }
    Schedule result_sched = newObjectSchedule(sched_els.elements());
    return result_sched;
  }

  public Schedule createOrgActivitySchedule(Collection col) {
    OrgActivity orgAct = null;
    long start, end;
    Schedule orgActSchedule = null;
    Vector schedElements = new Vector();
    if (col != null) {
      Iterator list = col.iterator();
      while (list.hasNext()) {
	orgAct = (OrgActivity)list.next();
	start = orgAct.getStartTime();
	end = orgAct.getEndTime();
	schedElements.add(new ObjectScheduleElement(start, end, orgAct));
      }
      orgActSchedule = newObjectSchedule(schedElements.elements());
    } else {
      logger.error("createOrgActivitySchedule passed empty collection");
    }
    return orgActSchedule;
  }

  public static Schedule trimObjectSchedule(Schedule schedule, TimeSpan span) {
    if (span == null) {
      return schedule;
    }
    ObjectScheduleElement startOse=null, endOse=null, ose;
    Vector resultElements = new Vector();
    long start = span.getStartTime();
    long end = span.getEndTime();
//     System.out.println("TIME SPAN "+TimeUtils.dateString(start)+
//                        " to "+TimeUtils.dateString(end)+", SCHEDULE "+schedule);
    // Grab all elements of this schedule that are bounded by timespan
    Collection elements = schedule.getEncapsulatedScheduleElements(start, end);
    // Grab start and end time elements as they may not be bounded by timespan
    Collection col = schedule.getScheduleElementsWithTime(start);
    if (!col.isEmpty()){
      startOse = (ObjectScheduleElement)col.iterator().next();
    }
    col = schedule.getScheduleElementsWithTime(end-1);
    if (!col.isEmpty()) {
      endOse = (ObjectScheduleElement)col.iterator().next();
    }
    Iterator elementsIt = elements.iterator();
    while (elementsIt.hasNext()) {
      ose = (ObjectScheduleElement)elementsIt.next();
      // Check to see if start or end is fully encapsulated in this schedule
      if (ose == startOse) {
        startOse = null;
      } 
      if (ose == endOse) {
        endOse = null;
      }
      // Include all elements that are fully encapsulated by the timespan
      resultElements.add(new ObjectScheduleElement(ose.getStartTime(), ose.getEndTime(),
                                                   ose.getObject()));
    }
    if ((startOse == endOse) && (startOse != null)) {
      resultElements.add(new ObjectScheduleElement(start, end,
                                                   startOse.getObject()));
    } else {
      if (startOse != null) {
        // start does not fully encapsulate this element
        // adjust start of return schedule to reflect time span restriction
        resultElements.add(new ObjectScheduleElement(start, startOse.getEndTime(),
                                                     startOse.getObject()));
      }
      if (endOse != null) {
        // end does not fully encapsulate this element
        // adjust end of return schedule to reflect time span restriction
        resultElements.add(new ObjectScheduleElement(endOse.getStartTime(), end,
                                                     endOse.getObject()));
      }
    }
    return newObjectSchedule(resultElements.elements());
  }
  
  // Only works on contiguous object schedules
  public static Schedule simplifyObjectSchedule(Schedule sched) {
    if (sched == null) {
      return null;
    }
    if (sched.isEmpty()) {
      return sched;
    }
    Vector newElements = new Vector();
    ObjectScheduleElement element = null;
    long start = sched.getStartTime();
    long elementStart=Long.MIN_VALUE, elementEnd=Long.MAX_VALUE;
    Object o = null, test = null;
    while ((element = (ObjectScheduleElement)getElementWithTime(sched, start)) != null) {
      if (o == null) {
        o = element.getObject();
        elementStart = element.getStartTime();
        elementEnd = element.getEndTime();
        start = elementEnd;
      } else {
        test = element.getObject();
        if (test.equals(o)) {
          elementEnd = element.getEndTime();
        } else {
          newElements.add(new ObjectScheduleElement(elementStart, elementEnd, o));
          elementStart = element.getStartTime();
          elementEnd = element.getEndTime();
          o = test;
        }
        start = element.getEndTime();
      }
    }
    if (o != null) {
      newElements.add(new ObjectScheduleElement(elementStart, elementEnd, o));
    }
    return newObjectSchedule(newElements.elements());
  }
}
