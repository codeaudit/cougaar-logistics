/**
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

import org.cougaar.glm.plugins.ClusterOPlan;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import java.io.Serializable;
import java.util.*;

public class LogisticsOPlan extends ClusterOPlan implements UniqueObject {

  long arrivalInTheater;
  Schedule defensiveSchedule;
  UID theUID;

  public LogisticsOPlan(MessageAddress id, Oplan op) {
    super(id, op);
    setUID(UID.toUID(id.toString()+":"+op.getOplanId()+"/1"));
    arrivalInTheater = getStartTime();
    defensiveSchedule = ScheduleUtils.newObjectSchedule((new Vector()).elements());
  }
  public boolean updateOrgActivities(IncrementalSubscription orgActivitySubscription) {
    boolean update = super.updateOrgActivities(orgActivitySubscription);
    updateArrivalInTheater(orgActivitySubscription);
    updateDefensiveSchedule(orgActivitySubscription);
//     System.out.println("DEFENSIVE SCHEDULE : "+defensiveSchedule);
    return update;
  }

  public void updateArrivalInTheater(IncrementalSubscription orgActivitySubscription) {
    // Arrival in theater is the end date of the Deployment orgActivity
    long arrival = getStartTime();
    OrgActivity activity;
    Enumeration activities = orgActivitySubscription.elements();

    // search for Deployment orgActivity
    while (activities.hasMoreElements()) {
      activity = (OrgActivity)activities.nextElement();
      if (activity.getActivityType().equals(OrgActivity.DEPLOYMENT)) {
	if (activity.getEndTime() > arrival) {
	  arrival = activity.getEndTime();
	}
      }
    }
    arrivalInTheater = arrival;
  }    

  public long getArrivalTime() {
    return arrivalInTheater;
  }

  public void updateDefensiveSchedule(IncrementalSubscription orgActivitySubscription) {
    OrgActivity activity;
    Enumeration activities = orgActivitySubscription.elements();
    Vector defensiveList = new Vector();
    ObjectScheduleElement element = null;
    
    // search for Deployment orgActivity
    while (activities.hasMoreElements()) {
      activity = (OrgActivity)activities.nextElement();
      if (activity.getActivityType().equals("Defensive")) {
        element = new ObjectScheduleElement(activity.getStartTime(), activity.getEndTime(),
                                            new Boolean(true));
      } else {
        element = new ObjectScheduleElement(activity.getStartTime(), activity.getEndTime(),
                                            new Boolean(false));
      }
      defensiveList.add(element);
    }
    Schedule tmpSched = ScheduleUtils.newObjectSchedule(defensiveList.elements());
    defensiveSchedule = ScheduleUtils.simplifyObjectSchedule(tmpSched);
  }    

  public Schedule getDefensiveSchedule() {
    return defensiveSchedule;
  }

  public UID getUID() {
    return theUID;
  }

  public void setUID(UID theUID) {
    this.theUID = theUID;
  }

  public String toString() {
    return new String(super.toString()+" Start: "+TimeUtils.dateString(getStartTime())+
		      " End: "+TimeUtils.dateString(getEndTime())+" Arrival: "+
		      TimeUtils.dateString(arrivalInTheater));
  }
}

