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

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.service.DomainService;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.core.util.UID;

import java.io.Serializable;
import java.util.*;


/** The LogisticsOPlanPlugin collects the OPlans and OrgActivities
 *  in the cluster and creates an oplan object with the information
 *  pertinent to the InventoryPlugin.
 **/

public class LogisticsOPlanPlugin extends ComponentPlugin {
  
  public IncrementalSubscription         oplans;
  /** Map keyed by OPlan UID to an org activity subscription **/
  private Map orgActivitySubscriptionOfOPlanUID = new HashMap();
  /** Hash of oplans **/
  private HashMap oplanHash = new HashMap();
  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;
  private LoggingService logger;
  private Organization myOrganization;
  protected ClusterIdentifier clusterId;


  // oplan
  static class OplanPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Oplan);
    }
  } 

  static class OplanOrgActivitiesPredicate implements UnaryPredicate {
    UID oplanUID_;
    public OplanOrgActivitiesPredicate(UID uid) {
      oplanUID_ = uid;
    }
    
    public boolean execute(Object o) {
      if (o instanceof OrgActivity) {
	if (oplanUID_.equals(((OrgActivity)o).getOplanUID())) {
	  return true;
	}
      }
      return false;
    }
  }

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
	return ((Organization)o).isSelf();
      }
      return false;
    }
  };  	

  private static UnaryPredicate logisticsOPlanPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof LogisticsOPlan;
    }
  };

  public void load() {
    super.load();
    logger = getLoggingService(this);
  }

  public synchronized void execute() {
    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if (myOrganization == null) {
	return;
      } else {
	clusterId = myOrganization.getClusterPG().getClusterIdentifier();
      }
    }
    updateOplans();
    updateOrgActivities();
  }
  
  protected void setupSubscriptions() {
    oplans = (IncrementalSubscription)getBlackboardService().subscribe(new OplanPredicate());
    selfOrganizations = (IncrementalSubscription) getBlackboardService().subscribe(orgsPredicate);

    Collection anyOplans = getBlackboardService().query(new OplanPredicate());

    if((anyOplans != null) &&
       (!(anyOplans.isEmpty())) &&
       getBlackboardService().didRehydrate()) {
      getLogisticsOPlans();
      doUpdateOplans();
    }
  }

  private void getLogisticsOPlans() {


    Collection c = getBlackboardService().query(logisticsOPlanPredicate);
    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if (myOrganization != null) {
	clusterId = myOrganization.getClusterPG().getClusterIdentifier();
      }
    }
    int waited = 0;
    while (c.isEmpty()) {
      try {
        if (logger.isErrorEnabled()) {
          logger.error("starting to sleep for oplan objects at "+waited+" seconds at "+clusterId);
        }
	Thread.currentThread().sleep(3000);
	waited=waited+3;
      } catch (Exception ex) {
        if (logger.isErrorEnabled()) {
          logger.error("Exception sleeping for OPlan "+ex);
        }
	ex.printStackTrace();
      }
      c = getBlackboardService().query(logisticsOPlanPredicate);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Got the OPlan at "+clusterId);
    }
    for (Iterator i = c.iterator(); i.hasNext(); ) {
      LogisticsOPlan loplan = (LogisticsOPlan) i.next();
      oplanHash.put(loplan.getOplanUID(), loplan);
      
      UID oplanUID = loplan.getOplanUID();
      IncrementalSubscription oplanActivities = (IncrementalSubscription)
	orgActivitySubscriptionOfOPlanUID.get(oplanUID);
      if (oplanActivities == null) {
	oplanActivities = (IncrementalSubscription)
	  getBlackboardService().subscribe(new OplanOrgActivitiesPredicate(oplanUID));
	orgActivitySubscriptionOfOPlanUID.put(oplanUID, oplanActivities);
      }
    }
  }

  private boolean updateOplans() {
    boolean oplanChange = false;
    if (logger.isDebugEnabled()) {
      logger.debug("starting updateOplans");
    }
    if (oplans.hasChanged()) {
      doUpdateOplans();
      oplanChange = true;
    }
    return oplanChange;
  }

  // Process Oplan subscription
  private void doUpdateOplans() {
    if (logger.isDebugEnabled()) {
      logger.debug("Updating the Oplans!");
    }
    Enumeration enum;
    // Create new LogisticsOPlan objects for each added Oplan
    if (oplans.getAddedList().hasMoreElements()) {
      enum = oplans.getAddedList();
      while (enum.hasMoreElements()) {
	Oplan oplan = (Oplan)enum.nextElement();
	UID oplanUID = oplan.getUID();
	IncrementalSubscription oplanActivities = (IncrementalSubscription)
	  orgActivitySubscriptionOfOPlanUID.get(oplanUID);
	if (oplanActivities == null) {
	  oplanActivities = (IncrementalSubscription)
	    getBlackboardService().subscribe(new OplanOrgActivitiesPredicate(oplanUID));
	  orgActivitySubscriptionOfOPlanUID.put(oplanUID, oplanActivities);
	}
	LogisticsOPlan loplan = (LogisticsOPlan) oplanHash.get(oplanUID);
	if (loplan == null) {
	  loplan = new LogisticsOPlan(clusterId, oplan);
	  oplanHash.put(oplanUID, loplan);
	  getBlackboardService().publishAdd(loplan);
          if (logger.isDebugEnabled()) {
            logger.debug("Published LogisticsOPlan "+loplan+" for "+clusterId);
          }
	}
      }
    }
    // Remove LogisticsOPlan objects that are no longer relevant
    if (oplans.getRemovedList().hasMoreElements()) {
      enum = oplans.getRemovedList();
      while (enum.hasMoreElements()) {
	Oplan oplan = (Oplan)enum.nextElement();
	UID oplanUID = oplan.getUID();
	LogisticsOPlan loplan = (LogisticsOPlan) oplanHash.get(oplanUID);
	// Remove LogisticsOPlan from array
	oplanHash.remove(oplanUID);
	// Cancel subscription
	IncrementalSubscription s = (IncrementalSubscription)
	  orgActivitySubscriptionOfOPlanUID.remove(oplanUID);
	if (s != null) getBlackboardService().unsubscribe(s);
	getBlackboardService().publishRemove(loplan);
	break;
      }
    }
    if (oplanHash.isEmpty() && logger.isErrorEnabled()) {
      logger.error(" updateOplans no OPLAN");
    }
  }

  // Each LogisticsOPlan updates its own OrgActivities if needed
  private boolean updateOrgActivities() {
    Iterator enum = oplanHash.values().iterator();
    boolean update = false;
    while (enum.hasNext()) {
      LogisticsOPlan loplan = (LogisticsOPlan) enum.next();
      IncrementalSubscription s = (IncrementalSubscription)
	orgActivitySubscriptionOfOPlanUID.get(loplan.getOplanUID());
      update = update || loplan.updateOrgActivities(s);
    }
    return update;
  }
 
  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService) 
      getServiceBroker().getService(requestor,
				    LoggingService.class,
				    null);
  }

  private Organization getMyOrganization(Enumeration orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasMoreElements()) {
      myOrg = (Organization) orgs.nextElement();
    }
    return myOrg;
  }

}


