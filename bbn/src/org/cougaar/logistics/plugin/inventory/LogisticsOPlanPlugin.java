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

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.service.DomainService;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.planning.service.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.core.util.UID;
import org.cougaar.util.DynamicUnaryPredicate;
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
  protected MessageAddress clusterId;


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
	clusterId = myOrganization.getClusterPG().getMessageAddress();
      }
    }
    updateOplans();
    updateOrgActivities();
    publishLogOplanObjects();
  }
  
  protected void setupSubscriptions() {
    selfOrganizations = (IncrementalSubscription) getBlackboardService().subscribe(orgsPredicate);

    // If we are rehydrating then this subscription will have things on it
    // So restore our private variables
    if (myOrganization == null && ! selfOrganizations.isEmpty()) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if (myOrganization != null) {
	clusterId = myOrganization.getClusterPG().getMessageAddress();
      }
    }

    oplans = (IncrementalSubscription)getBlackboardService().subscribe(new OplanPredicate());

    // If there are oplans on our subscription, then restore the
    // private hashes / subscriptions
    // Note the assumption that there will be an Oplan object before or
    // at least at same time as there are OrgActivities and therefore
    // LogOplans.
    if (! oplans.isEmpty()) {
      if (logger.isInfoEnabled())
	logger.info(getAgentIdentifier() + ".setupSub: have oplans. Restoring hashes.");
      // Restore org activities subscriptions / hash
      Enumeration enum = oplans.elements();
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
        // publish code was here
      }

      // Restore LogOplan hash
      // If there are none on the blackboard, then wait for the execute
      // method of this Plugin to fire on new OrgActivities,
      // and create one.
      Collection c = getBlackboardService().query(logisticsOPlanPredicate);
      for (Iterator i = c.iterator(); i.hasNext(); ) {
	LogisticsOPlan loplan = (LogisticsOPlan) i.next();
	oplanHash.put(loplan.getOplanUID(), loplan);
      }
      
      // If there already are OrgActivities on the subscriptions we just
      // restored above, but we have not yet created the LogOplans
      // to got with them, then create them here.
      // This may not strictly be necessary, but it doesn't hurt
      publishLogOplanObjects();
    } // end of check for oplans on subscription

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
        // publish code was here
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
  }

  // Each LogisticsOPlan updates its own OrgActivities if needed
  private boolean updateOrgActivities() {
    Iterator enum = oplanHash.values().iterator();
    boolean update = false;
    while (enum.hasNext()) {
      LogisticsOPlan loplan = (LogisticsOPlan) enum.next();
      IncrementalSubscription s = (IncrementalSubscription)
	orgActivitySubscriptionOfOPlanUID.get(loplan.getOplanUID());
      if ((!s.getChangedCollection().isEmpty()) || 
	  (!s.getAddedCollection().isEmpty()) ||
	  (!s.getRemovedCollection().isEmpty())) {
        update = update || loplan.updateOrgActivities(s);
        getBlackboardService().publishChange(loplan);
      }
    }
    return update;
  }

  /** The publishLogOplanObjects() method publishes the LogisticsOPlan objects for
   *  oplans that also have orgactivities.
   *  The updateOPlans() method associates the Oplan with its OrgActivities by placing
   *  them in the orgActivitySubscriptionOfOPlanUID map.  At the end of the execute the
   *  publishLogOplanObjects() looks through the map to find oplans with orgActivities 
   *  that have not been published (do not appear in the oplanHash) and creates and
   *  publishes the LogOplan object.
   **/
  private void publishLogOplanObjects() {
    Oplan oplan;
    UID oplanUID;
    LogisticsOPlan loplan;
    Iterator oplanUIDset = orgActivitySubscriptionOfOPlanUID.keySet().iterator();
    while (oplanUIDset.hasNext()) {
      oplanUID = (UID)oplanUIDset.next();
      oplan = findOplan(oplanUID);
      if (oplan == null) {
        logger.error("Cannot find matching oplan "+oplanUID);
        continue;
      }
      loplan = (LogisticsOPlan) oplanHash.get(oplanUID);
      IncrementalSubscription s = (IncrementalSubscription)
	orgActivitySubscriptionOfOPlanUID.get(oplanUID);
      if (loplan == null) {
	if (logger.isInfoEnabled())
	  logger.info(getAgentIdentifier() + ".publishLogOplan: no loplan for uid " + oplanUID + ". OrgActivity sub.isEmpty? " + s.isEmpty());
        if (!s.isEmpty()) {
          loplan = new LogisticsOPlan(clusterId, oplan);
          loplan.updateOrgActivities(s);
          oplanHash.put(oplanUID, loplan);
          getBlackboardService().publishAdd(loplan);
          if (logger.isDebugEnabled()) {
            logger.debug("Published LogisticsOPlan "+loplan+" for "+clusterId);
          }
        }
      }
    }
  }

  private Oplan findOplan(UID oplanUID) {
    Iterator oplanIt = oplans.iterator();
    Oplan oplan;
    while (oplanIt.hasNext()) {
      oplan = (Oplan)oplanIt.next();
      if (oplan.getUID().equals(oplanUID)) {
        return oplan;
      }
    }
    return null;
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


