/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.TimeSpanSet;
import org.cougaar.util.UnaryPredicate;

//import org.cougaar.glm.ldm.Constants;

import org.cougaar.servicediscovery.SDFactory;

import org.cougaar.servicediscovery.description.LineageEchelonScorer;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.TimeInterval;
import org.cougaar.servicediscovery.plugin.SDClientPlugin;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;

import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.mlm.plugin.organization.GLSConstants;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;

public class ALDynamicSDClientPlugin extends SDClientPlugin implements GLSConstants {
  private HashMap myServiceRequestHistories;

  public static void main(String[] args) {
    ServiceRequestRecord srr = new ServiceRequestRecord("Test", TimeSpans.getSpan(1000, 2000));
    ServiceRequestRecord srr2 = new ServiceRequestRecord("Test", TimeSpans.getSpan(2000, 3000));
    ServiceRequestHistory srh = new ServiceRequestHistory(null, TimeSpans.getSpan(1000, 3000), srr);
    srh.addRequest(srr2);
    System.out.println(srh.containsRequest("Test", TimeSpans.getSpan(1000, 2000)));
    //true
    System.out.println(srh.containsRequest("Test", TimeSpans.getSpan(1000, 3000)));
    //true
    System.out.println(srh.containsRequest("Test", TimeSpans.getSpan(1500, 2500)));
    //true
    System.out.println(srh.containsRequest("Test", TimeSpans.getSpan(500, 2500)));
    //false
    System.out.println(srh.containsRequest("XS", TimeSpans.getSpan(1000, 2000)));
    //false
  }

  protected void setupSubscriptions() {
    myServiceRequestHistories = new HashMap();
    super.setupSubscriptions();
  }

  /**
   * overrides the superclass to deal with service request histories
   */
  protected void handleChangedServiceContractRelays(Collection changedRelays) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
                             " handleChangedServiceContractRelays " +changedRelays.size());
    }

    for (Iterator iterator = changedRelays.iterator();
	   iterator.hasNext();) {
      ServiceContractRelay relay = (ServiceContractRelay)iterator.next();

      // Only interested if we are the client
      if (!(relay.getClient().equals(getSelfOrg()))) {
	continue;
      }

      ServiceContract contract = relay.getServiceContract();
      Role role = contract.getServiceRole();


      //if the role is now completely covered, discard the history
      if (checkProviderCompletelyCovered(role)) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 ": handleChangedServiceContractRelays() " +
				 "completely covered for " + role);
        }
        myServiceRequestHistories.remove(role);
      } else {

	//if your service contract got revoked, or your requested
	//service time interval was not satistied, do a new service query
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 " revoked " + 
				 contract.isRevoked() +
				 " !satisfied " + 
				 !timeIntervalRequestedCompletelySatisfied(relay));
	}
	
	// Contract timespan does not == requested timespan
	if ((contract.isRevoked() ||
	     !timeIntervalRequestedCompletelySatisfied(relay))) {
	  handleModifiedServiceContract(relay);
	} else {
	  // We know from checkProviderCovered that service is not covered
	  // Ping execute so that we query for missing services
	  setNeedToFindProviders(true);
	  wake();
	}
      }
    }
  }

  private ServiceRequestHistory augmentServiceRequestHistory(ServiceContractRelay relay, 
							     TimeSpan requestedTimeInterval) {
    
    if (requestedTimeInterval == null) {
      myLoggingService.debug(getAgentIdentifier() +
			     " service request to " + 
			     relay.getProviderName() + 
			     " for " +
			     relay.getServiceRequest().getServiceRole() +
			     " does not have valid start/end time preferences.");
    }
    
    
    ServiceRequestRecord srr = 
      new ServiceRequestRecord(relay.getProviderName(),
			       requestedTimeInterval);
    
    //if this request is not in the history (because it previously
    //completely satisfied the request) put it in
    Role role = relay.getServiceContract().getServiceRole();
    ServiceRequestHistory srh = 
      (ServiceRequestHistory) myServiceRequestHistories.get(role);
    if (srh == null) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " handleChangedServiceContractRelays, put into history " +
			       relay.getProviderName() + " " + role);
      }
      
      srh = new ServiceRequestHistory(role, requestedTimeInterval, srr);
      myServiceRequestHistories.put(role, srh);
      
      if (myLoggingService.isDebugEnabled()) {
	for (Iterator it = myServiceRequestHistories.keySet().iterator();
	     it.hasNext();) {
	  Role key = (Role) it.next();
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug("key " + key);
	  }
	}
      }
    } else {
      //Add new service request history
      srh.addRequest(srr);
    }

    return srh;
  }

  /**
   * returns true if the relay's request time interval is a subset of the
   * relay's contract time interval
   */
  private boolean timeIntervalRequestedCompletelySatisfied(ServiceContractRelay relay) {
    if (relay.getServiceContract() == null) {
      return false;
    }

    TimeSpan requested = 
      SDFactory.getTimeSpanFromPreferences(relay.getServiceRequest().getServicePreferences());
    TimeSpan provided = 
      SDFactory.getTimeSpanFromPreferences(relay.getServiceContract().getServicePreferences());

    if ((requested == null) ||
	(provided == null)) {
      return false;
    } else {
    return ((requested.getStartTime() >= provided.getStartTime()) && 
	    (requested.getEndTime() <= provided.getEndTime()));
    }
  }

  /**
   * overrides superclass to deal with service request histories
   */
  protected void requestServiceContract(ServiceDescription serviceDescription,
                                        TimeSpan interval) {
    super.requestServiceContract(serviceDescription, interval);
    String providerName = serviceDescription.getProviderName();
    Role role = getRole(serviceDescription);

    //add to your service request history
    ServiceRequestRecord srr = 
      new ServiceRequestRecord(providerName,
			       interval);
    ServiceRequestHistory history;

    if (myServiceRequestHistories.containsKey(role)) {
      history = (ServiceRequestHistory) myServiceRequestHistories.get(role);
      history.addRequest(srr);
    } else {
      history = new ServiceRequestHistory(role, interval, srr);
    }

    myServiceRequestHistories.put(role, history);
  }


  /**
   * overrides the superclass to deal with service request histories
   */
  protected void handleRequestWithNoRemainingProviderOption(Role role, 
							    TimeSpan currentInterval) {
    //this means you have a time interval where you have exhausted all possible
    //providers. Log a warning.
    if (myLoggingService.isWarnEnabled()) {
      myLoggingService.warn(getAgentIdentifier() +
                            " failed to contract with " + role +
                            " for time period from " + 
			    new java.util.Date(currentInterval.getStartTime()) +
                            " to " + 
			    new java.util.Date(currentInterval.getEndTime()));
    }
    //Flush the request history.
    myServiceRequestHistories.remove(role);
    //set the automatic retry
    activateAutomaticFindProviderRetry();
  }

  /**
   * overrides the superclass
   * reorder ties by provider name using the scored service description comparator
   */
  protected Collection reorderAnyTiedServiceDescriptions(ArrayList scoredServiceDescriptions) {
    ArrayList sortedSSD = new ArrayList(scoredServiceDescriptions);
    Collections.sort(sortedSSD, 
		     new ScoredServiceDescriptionComparator());
    return sortedSSD;
  }

  /**
   * Return true if according to your service request history, you have already
   * asked this provider for this complete time interval for this role.
   * Overrides the superclass to deal with service request history.
   */
  protected boolean alreadyAskedForContractWithProvider(Role role, 
							String providerName,
							TimeSpan timeInterval) {

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
                             " alreadyAskedForContractWithProvider check " + 
			     providerName + " " + role);
    }
    //what if part of the current time interval was requested but another part(s)
    //wasn't? (should return false, because if there is any uncovered part, you
    //want to make the request)

    //search for all myServiceRequestHistories with matching role
    //there should be one or none
    ServiceRequestHistory srh = 
      (ServiceRequestHistory) myServiceRequestHistories.get(role);
    if (srh != null) {
      boolean ret = srh.containsRequest(providerName, timeInterval);
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() +
                               " alreadyAskedForContractWithProvider " + ret);
      }
      return ret;
    } else if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " alreadyAskedForContractWithProvider srh is null");
      Iterator it = myServiceRequestHistories.keySet().iterator();
      while(it.hasNext()) {
	Role key = (Role) it.next();
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug("key " + key);
	}
      }
    }

    return false;
  }

  /**
   * requires that start <= end
   * Return a collection of all time intervals between desiredStart and
   * desiredEnd that are not covered by existing service contracts or
   * outstanding requests for this role.
   * Overrides superclass to deal with provider relationships with time
   * spans not the default.
   */
  protected Collection getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
      long desiredStart, long desiredEnd, Role role) {

    Collection stillNeeded = getCurrentlyUncoveredIntervals(desiredStart,
							    desiredEnd, 
							    role);

    //go through the existing service contracts and remove the
    //time periods that have been requested but not yet responded to
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if (!relay.getClient().equals(getSelfOrg())) {
        continue;
      }

      if (relay.getServiceContract() == null  &&
          relay.getServiceRequest().getServiceRole().equals(role)) {

	TimeSpan requestedTimeInterval = 
	  SDFactory.getTimeSpanFromPreferences(relay.getServiceRequest().getServicePreferences());	
	
	stillNeeded = TimeInterval.removeInterval(requestedTimeInterval,
						  stillNeeded);
      }
    }
    
    return stillNeeded;
  }

  //requires that start <= end
  //returns a collection of intervals between neededStartTime and 
  // neededEndTime which is not covered for the specified role
  private Collection getCurrentlyUncoveredIntervals(long desiredStart, 
						    long desiredEnd, 
						    Role role) {

    ArrayList desiredCoverageIntervals = new ArrayList();
    desiredCoverageIntervals.add(TimeSpans.getSpan(desiredStart, desiredEnd));
    Collection stillNeeded = desiredCoverageIntervals;

    //go through the existing service contracts and remove the time
    //periods which are covered already
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if (!relay.getClient().equals(getSelfOrg())) {
        continue;
      }

      ServiceContract contract = relay.getServiceContract();

      if ((contract != null)  &&
          (!contract.isRevoked()) &&
          (contract.getServiceRole().equals(role))) {

	TimeSpan providedTimeInterval = 
	  SDFactory.getTimeSpanFromPreferences(relay.getServiceContract().getServicePreferences());
      
	if (providedTimeInterval == null) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " getCurrentlyUncoveredIntervals: " +
				   " contract with " + 
				   relay.getProviderName() +
				   " for " + role + 
				   " does not have a valid time period.");
	  }
	} else {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() +
				   " getCurrentlyUncoveredIntervals: " +
				   " contract with " + 
				   relay.getProviderName() +
				   " for " + role + 
				   " runs " + 
				   providedTimeInterval.getStartTime() +
				   " to " + providedTimeInterval.getEndTime());
	  }
	  // If contract specifies a valid time range, remove it.
	  stillNeeded = 
	    TimeInterval.removeInterval(providedTimeInterval,
					stillNeeded);				 
	}
      }
    }

    return stillNeeded;
  }

  /**
   * If there is a current request history for this role, check the
   * time interval of it. Otherwise, check the default time interval.
   * Return true if this role is completely covered by service contracts or
   * outstanding requests
   * Override superclass to deal with potential provider relationships
   * that have start/end time not equal to default start/end.
   */
  protected boolean checkProviderCompletelyCoveredOrRequested(Role role) {
    TimeSpan opconTimeSpan = getOPCONTimeSpan();
    Collection c = getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
        opconTimeSpan.getStartTime(),
	opconTimeSpan.getEndTime(),
	role);

    //check the personal history time interval (if it exists)
    //instead of the default interval
    ServiceRequestHistory srh = 
      (ServiceRequestHistory) myServiceRequestHistories.get(role);
    if (srh != null) {
      c = 
	getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(srh.requestedTimeInterval.getStartTime(),
								 srh.requestedTimeInterval.getEndTime(), 
								 role);
    }

    for (Iterator uncovered = c.iterator();
	 uncovered.hasNext();) {
      TimeSpan current = (TimeSpan) uncovered.next();
      if (current.getStartTime() < current.getEndTime()) {
        if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 " checkProviderCompletelyCoveredOrRequested has millisec difference of: "
				 + (current.getEndTime() - current.getStartTime()) +
				 " start " + current.getStartTime() + 
				 " end " + current.getEndTime());

        }
        return false;
      }
    }
    return true;
  }

  /**
   * If there is a current request history for this role, check the
   * time interval of it. Otherwise, check the default time interval.
   * Return true if the interval is covered by non-revoked service contracts.
   * Override superclass to deal with potential provider relationships
   * that have start/end time not equal to default start/end.
   */
  protected boolean checkProviderCompletelyCovered(Role role) {
    TimeSpan opconTimeSpan = getOPCONTimeSpan();
    Collection c = 
      getCurrentlyUncoveredIntervals(opconTimeSpan.getStartTime(),
				     opconTimeSpan.getEndTime(),
				     role);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " checkProviderCompletelyCovered: " + 
			     " uncovered intervals for " + role +
			     " = " + c);
    }

    //check the personal history time interval (if it exists)
    //instead of the default interval
    ServiceRequestHistory srh = 
      (ServiceRequestHistory) myServiceRequestHistories.get(role);
    if (srh != null) {
      c = 
	getCurrentlyUncoveredIntervals(srh.requestedTimeInterval.getStartTime(),
				       srh.requestedTimeInterval.getEndTime(),
				       role);

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " checkProviderCompletelyCovered: " + 
			       " uncovered intervals from service request history " + 
			       srh  + " = " + c);
      }
    }

    for (Iterator uncovered = c.iterator(); uncovered.hasNext();) {
      TimeSpan current = (TimeSpan) uncovered.next();

      if (current.getStartTime() < current.getEndTime()) {
        if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " checkProviderCompletelyCovered has millisec difference of: "
                                   + (current.getEndTime() - current.getStartTime()) +
                                   " start " + current.getStartTime() + " end " +
                                   current.getEndTime() + " for role " + role);
        }
        return false;
      }
    }
    return true;
  }

  protected void handleModifiedServiceContract(ServiceContractRelay relay) {
    ServiceContract contract = relay.getServiceContract();
    Role role = contract.getServiceRole();

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " queryServices because a service contract was revoked or did not satisfy the request for " +
			     role);
    }
    
    TimeSpan requestedTimeInterval = 
      SDFactory.getTimeSpanFromPreferences(relay.getServiceRequest().getServicePreferences());
    
    ServiceRequestHistory srh = 
      augmentServiceRequestHistory(relay, requestedTimeInterval);
    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " do another queryServices for " +
			     role);
    }
    
    TimeSpan providedTimeInterval = 
      SDFactory.getTimeSpanFromPreferences(contract.getServicePreferences());
    
    Collection uncoveredTimeIntervals = 
      TimeInterval.removeInterval(providedTimeInterval, requestedTimeInterval);
    
    String minimumEchelon = getMinimumEchelon(role);
    
    for (Iterator uncoveredIterator = uncoveredTimeIntervals.iterator();
	 uncoveredIterator.hasNext();) {
      
      TimeSpan uncoveredInterval = 
	(TimeSpan) uncoveredIterator.next();
      
      Collection opconIntervals = 
	getOPCONSchedule().intersectingSet(uncoveredInterval);
      if (opconIntervals.isEmpty()) {
	myLoggingService.error(getAgentIdentifier() + 
			       " handleChangedServiceContractRelays: " +
			       " no OPCON lineage on blackboard for " + 
			       new Date(uncoveredInterval.getStartTime()) + 
			       " to " +
			       new Date(uncoveredInterval.getEndTime()) +
			       ". Unable to generate MMRoleQuery for " + role);
	continue;
      } else if (!continuousCoverage(uncoveredInterval, 
				     new TimeSpanSet(opconIntervals))) {
	myLoggingService.error(getAgentIdentifier() + 
			       " handleChangedServiceContractRelays: " +
			       " gap in OPCON coverage at some point in " + 
			       new Date(uncoveredInterval.getStartTime()) + 
			       " to " +
			       new Date(uncoveredInterval.getEndTime()) +
			       ".");
      }
      
      for (Iterator opconIterator = opconIntervals.iterator();
	   opconIterator.hasNext();) {
	TimeSpan opconInterval = (TimeSpan) opconIterator.next();
	
	Lineage commandLineage = getCommandLineage(opconInterval);
	
	if (commandLineage != null) {
	  ArrayList arrayList = new ArrayList(1);
	  arrayList.add(TimeSpans.getSpan(opconInterval.getStartTime(),
					  opconInterval.getEndTime()));
	  
	  LineageEchelonScorer serviceScorer = 
	    new ALLineageEchelonScorer(commandLineage,
				       getMinimumEchelon(role),
				       role,
				       getAgentIdentifier().toString(),
				       srh, 
				       arrayList);
	  
	  queryServices(role, serviceScorer, opconInterval);
	} else {
	  // Should be impossible
	  myLoggingService.error(getAgentIdentifier() + 
				 " handleChangedServiceContractRelays: " +
				 " no OPCON lineage on blackboard for " + 
				 new Date(opconInterval.getStartTime()) + 
				 " to " +
				 new Date(opconInterval.getEndTime()) +
				 " Unable to generate MMRoleQuery for " + role);
	}
      }
      //publishRemove(relay);
    }
  }

  private void activateAutomaticFindProviderRetry() {
    //todo: set a daily alarm
  }

  private static class ServiceRequestHistory {
    
    Role requestedRole;
    TimeSpan requestedTimeInterval;
    ArrayList serviceRequestRecords;
    
    
    ServiceRequestHistory(Role requestedRole, 
			  TimeSpan requestedTimeInterval,
			  ServiceRequestRecord firstRequestRecord) {
      this.requestedRole = requestedRole;
      this.requestedTimeInterval = requestedTimeInterval;
      serviceRequestRecords = new ArrayList();
      serviceRequestRecords.add(firstRequestRecord);
    }
    
    void addRequest(ServiceRequestRecord srr) {
      serviceRequestRecords.add(srr);
    }
    
    //start with timeInterval
    //for every ServiceRequestRecord with matching provider, subtract that time
    //interval
    //at the end, see if there is anything remaining
    //if so, return false
    boolean containsRequest(String providerName, TimeSpan requestedTimeInterval) {
      TimeSpan unrequested = 
	TimeSpans.getSpan(requestedTimeInterval.getStartTime(),
			 requestedTimeInterval.getEndTime());
      ArrayList unRequestedTimeIntervals = new ArrayList();
      unRequestedTimeIntervals.add(unrequested);
      
      for (Iterator requests = serviceRequestRecords.iterator();
	   requests.hasNext();) {
	ServiceRequestRecord current = (ServiceRequestRecord) requests.next();
	ArrayList newUnrequestedTimeIntervals = new ArrayList();
	
	if (current.providerName.equals(providerName)) {
	  for (Iterator oldUnrequested = unRequestedTimeIntervals.iterator();
	       oldUnrequested.hasNext();) {
	    TimeSpan oldCurrent = (TimeSpan) oldUnrequested.next();
	    newUnrequestedTimeIntervals.addAll(TimeInterval.removeInterval(current.requestedTimeInterval, 
									   oldCurrent));
	  }
	  unRequestedTimeIntervals = newUnrequestedTimeIntervals;
	}
      }
      
      if (unRequestedTimeIntervals.isEmpty()) {
	return true;
      } else {
	for (Iterator unrequestedIterator = unRequestedTimeIntervals.iterator();
	     unrequestedIterator.hasNext();) {
	  TimeSpan current = (TimeSpan) unrequestedIterator.next();
	  //may need to add an epsilon ball around each time
	  if (current.getStartTime() != current.getEndTime()) {
	    return false;
	  }
	}
	return true;
      }
    }
  }

  protected static class ServiceRequestRecord {
    String providerName;
    TimeSpan requestedTimeInterval;
    
    ServiceRequestRecord(String providerName, 
			 TimeSpan requestedTimeInterval) {
      this.providerName = providerName;
      this.requestedTimeInterval = requestedTimeInterval;
    }
  }

  protected static class ScoredServiceDescriptionComparator implements java.util.Comparator {

    public int compare(Object first, Object second) {
      if (!(first instanceof ScoredServiceDescription) &&
	  !(second instanceof ScoredServiceDescription)) {
	throw new ClassCastException("Both objects must be ScoredServiceDescriptions");
      } else  {
	ScoredServiceDescription firstSSD = (ScoredServiceDescription) first;
	ScoredServiceDescription secondSSD = (ScoredServiceDescription) second;
	if (firstSSD.getScore() > secondSSD.getScore()) {
	  return 1;
	} else if (firstSSD.getScore() < secondSSD.getScore()) {
	  return -1;
	} else {
	  return firstSSD.getProviderName().compareTo(secondSSD.getProviderName());
	}
      }
    }
  }
    
  protected static class ALLineageEchelonScorer extends LineageEchelonScorer {
    private static Logger logger = 
      Logging.getLogger(ALLineageEchelonScorer.class);

    private transient ServiceRequestHistory serviceRequestHistory;
    private transient Collection uncoveredTimeIntervals;
    private transient String clientName;

    public ALLineageEchelonScorer() {
      super();
    }

    public ALLineageEchelonScorer(Lineage lineage,
				  String minimumEchelon,
				  Role role,
				  String cn,
				  ServiceRequestHistory srh,
				  Collection uti) {
      super(lineage, minimumEchelon, role);
      clientName = cn;
      uncoveredTimeIntervals = uti;
      serviceRequestHistory = srh;
    }

    public int scoreServiceInfo(ServiceInfo serviceInfo) {

      if (serviceRequestHistory == null) {
	// Nothing to evaluate against
	return super.scoreServiceInfo(serviceInfo);
      }
      
      String providerName = serviceInfo.getProviderName();
      
      for (Iterator iterator = uncoveredTimeIntervals.iterator();
	   iterator.hasNext();) {
	TimeSpan timeInterval = (TimeSpan) iterator.next();
	
	if (!serviceRequestHistory.containsRequest(providerName, 
						   timeInterval)) {
	  if (logger.isDebugEnabled()) {
	    logger.debug(clientName + " ALLineageEchelonScorer passed " + 
			 providerName +
			 " for " + 
			 new Date(timeInterval.getStartTime()) +
			 " to " + 
			 new Date(timeInterval.getEndTime()));
	  }
	  return super.scoreServiceInfo(serviceInfo);
	} 
      } 
	
      if (logger.isDebugEnabled()) {
	logger.debug(clientName + 
		     "ALLineageEchelonScorer returned -1 for " + 
		     providerName);
      }
      // All intervals previously asked for
      return -1;
    }
  }
}








