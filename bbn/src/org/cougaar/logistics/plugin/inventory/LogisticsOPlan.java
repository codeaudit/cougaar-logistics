/**
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
import org.cougaar.util.TimeSpan;

import java.io.Serializable;
import java.util.*;

public class LogisticsOPlan extends ClusterOPlan implements UniqueObject {

  long arrivalInTheater;
  Schedule defensiveSchedule;
  UID theUID;

  public LogisticsOPlan(MessageAddress id, Oplan op) {
    super(id, op);
    setUID(UID.toUID(id.toString()+":"+op.getOplanId()+"/1"));
    arrivalInTheater = Long.MIN_VALUE;
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
    long arrival = Long.MIN_VALUE;
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

  public long getStartTime() {
    TimeSpan oplanSpan = getOplanSpan();
    return oplanSpan.getStartTime();
  }

  public long getEndTime() {
    TimeSpan oplanSpan = getOplanSpan();
    return oplanSpan.getEndTime();
  }

  public String toString() {
    return new String(super.toString()+" Start: "+TimeUtils.dateString(getStartTime())+
		      " End: "+TimeUtils.dateString(getEndTime())+" Arrival: "+
		      TimeUtils.dateString(arrivalInTheater));
  }
}

