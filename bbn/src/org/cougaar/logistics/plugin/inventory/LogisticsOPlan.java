/**
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

import org.cougaar.glm.plugins.ClusterOPlan;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.util.UID;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.Oplan;
import java.io.Serializable;
import java.util.*;

public class LogisticsOPlan extends ClusterOPlan implements Serializable {

  long arrivalInTheater;

  public LogisticsOPlan(ClusterIdentifier id, Oplan op) {
    super(id, op);
    arrivalInTheater = getStartTime();
  }
  public boolean updateOrgActivities(IncrementalSubscription orgActivitySubscription) {
    boolean update = super.updateOrgActivities(orgActivitySubscription);
    updateArrivalInTheater();
    return update;
  }

  public void updateArrivalInTheater() {
    // Use start time for now, need to find out how to derive arrival in theater
    arrivalInTheater = getStartTime();
  }    
}

