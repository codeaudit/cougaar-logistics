/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
import org.cougaar.util.UnaryPredicate;

//import org.cougaar.glm.ldm.Constants;

import org.cougaar.servicediscovery.SDFactory;

import org.cougaar.servicediscovery.description.LineageEchelonScorer;
import org.cougaar.servicediscovery.description.LineageListWrapper;
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
  private HashMap serviceRequestHistories;

  public static void main(String[] args) {
    ServiceRequestRecord srr = new ServiceRequestRecord("Test", new TimeInterval(1000, 2000));
    ServiceRequestRecord srr2 = new ServiceRequestRecord("Test", new TimeInterval(2000, 3000));
    ServiceRequestHistory srh = new ServiceRequestHistory(null, new TimeInterval(1000, 3000), srr);
    srh.addRequest(srr2);
    System.out.println(srh.containsRequest("Test", new TimeInterval(1000, 2000)));
    //true
    System.out.println(srh.containsRequest("Test", new TimeInterval(1000, 3000)));
    //true
    System.out.println(srh.containsRequest("Test", new TimeInterval(1500, 2500)));
    //true
    System.out.println(srh.containsRequest("Test", new TimeInterval(500, 2500)));
    //false
    System.out.println(srh.containsRequest("XS", new TimeInterval(1000, 2000)));
    //false
  }

  protected void setupSubscriptions() {
    serviceRequestHistories = new HashMap();
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

      //if the role is now completely covered, discard the history
      if(checkProviderCompletelyCovered(relay.getServiceRequest().getServiceRole())) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " handleChangedServiceContractRelays, completely covered for "
                                 + relay.getServiceRequest().getServiceRole());
        }
        serviceRequestHistories.remove(relay.getServiceRequest().getServiceRole().toString());
	continue;
      }

      //only take action if you are the client agent
      // (not if you are the provider)

      //if your service contract got revoked, or your requested
      //service time interval was not satistied, do a new service query
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() +
                               " revoked " + relay.getServiceContract().isRevoked() +
                               " !satisfied " + !timeIntervalRequestedCompletelySatisfied(relay) +
                               " client " + relay.getClient().equals(getSelfOrg()));
      }

      if ((relay.getClient().equals(getSelfOrg())) &&
	  ((relay.getServiceContract().isRevoked() ||
           !timeIntervalRequestedCompletelySatisfied(relay)))) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " queryServices because a service contract was revoked or did not satisfy the request for " +
                                 relay.getServiceContract().getServiceRole().toString());
        }

	TimeInterval requestedTimeInterval = 
	  getPreferenceTimeInterval(relay.getServiceRequest().getServicePreferences());

	if (requestedTimeInterval == null) {
	  myLoggingService.debug(getAgentIdentifier() +
				 " service request to " + 
				 relay.getProviderName() + 
				 " for " +
				 relay.getServiceRequest().getServiceRole() +
				 " does not have valid start/end time preferences.");
	}

	TimeInterval providedTimeInterval = 
	  getPreferenceTimeInterval(relay.getServiceContract().getServicePreferences());

       
	ServiceRequestRecord srr = 
	  new ServiceRequestRecord(relay.getProviderName(),
				   requestedTimeInterval);

        //if this request is not in the history (because it previously
        //completely satisfied the request) put it in
        ServiceRequestHistory srh = (ServiceRequestHistory)serviceRequestHistories.get(
            relay.getServiceContract().getServiceRole().toString());
        if(srh == null) {
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " handleChangedServiceContractRelays, put into history " +
                                   relay.getProviderName() + " "
                                   + relay.getServiceRequest().getServiceRole());
          }

          srh = new ServiceRequestHistory(relay.getServiceContract().getServiceRole(),
              requestedTimeInterval, srr);
          serviceRequestHistories.put(relay.getServiceContract().getServiceRole().toString(),
                                      srh);

	  if (myLoggingService.isDebugEnabled()) {
	    Iterator it = serviceRequestHistories.keySet().iterator();
	    while(it.hasNext()) {
	      String key = (String)it.next();
	      if (myLoggingService.isDebugEnabled()) {
		myLoggingService.debug("key " + key);
	      }
	    }
          }
        } else {
	  //Add new service request history
	  srh.addRequest(srr);
	}

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " do another queryServices for " +
                                 relay.getServiceContract().getServiceRole().toString());
        }

	Collection uncoveredTimeIntervals = 
	  requestedTimeInterval.subtractInterval(providedTimeInterval);

	Role role = relay.getServiceContract().getServiceRole();
	String minimumEchelon = getMinimumEchelon(role);
	
	LineageListWrapper commandListWrapper = getCommandLineageList();
	
	if (commandListWrapper != null) {
	  
	  LineageEchelonScorer serviceScorer = 
	    new ALLineageEchelonScorer(commandListWrapper,
				       getMinimumEchelon(role),
				       role,
				       getAgentIdentifier().toString(),
				       srh, 
				       uncoveredTimeIntervals);

	  queryServices(role, serviceScorer);
	} else {
	  myLoggingService.error(getAgentIdentifier() + 
				 " handleChangedServiceContractRelays: " +
				 " no COMMAND LineageList on blackboard." + 
				 " Unable to generate MMRoleQuery for " + role);
	}
        //publishRemove(relay);
      } else if ((relay.getClient().equals(getSelfOrg())) &&
		 (timeIntervalRequestedCompletelySatisfied(relay))) {
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " handleChangedServiceContractRelays timeIntervalRequestedCompletelySatisfied " +
                                   relay.getServiceContract().getServiceRole().toString());
          }

      } else if (!timeIntervalRequestedCompletelySatisfied(relay) &&
             relay.getClient().equals(getSelfOrg())) {
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " handleChangedServiceContractRelays not timeIntervalRequestedCompletelySatisfied " +
                                   relay.getServiceContract().getServiceRole().toString());
          }

        }
    }
  }

  /**
   * returns true if the relay's request time interval is a subset of the
   * relay's contract time interval
   */
  private boolean timeIntervalRequestedCompletelySatisfied(ServiceContractRelay relay) {
    if(relay.getServiceContract() == null) {
      return false;
    }

    TimeInterval requested = 
      getPreferenceTimeInterval(relay.getServiceRequest().getServicePreferences());
    TimeInterval provided = 
      getPreferenceTimeInterval(relay.getServiceContract().getServicePreferences());

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
                                        TimeInterval interval) {
    super.requestServiceContract(serviceDescription, interval);
    String providerName = serviceDescription.getProviderName();
    Role role = getRole(serviceDescription);

    //add to your service request history
    ServiceRequestRecord srr = new ServiceRequestRecord(providerName,
        interval);
    if(serviceRequestHistories.containsKey(role.toString())) {
      ServiceRequestHistory history = (ServiceRequestHistory) serviceRequestHistories.get(role.toString());
      history.addRequest(srr);
      serviceRequestHistories.put(role.toString(), history);
    }
    else {
      ServiceRequestHistory history = new ServiceRequestHistory(role, interval, srr);
      serviceRequestHistories.put(role.toString(), history);
    }
  }


  /**
   * overrides the superclass to deal with service request histories
   */
  protected void handleRequestWithNoRemainingProviderOption(Role role, TimeInterval currentInterval) {
    //this means you have a time interval where you have exhausted all possible
    //providers. Log a warning.
    if (myLoggingService.isWarnEnabled()) {
      myLoggingService.warn(getAgentIdentifier() +
                            " failed to contract with " + role.toString() +
                            " for time period from " + new java.util.Date(currentInterval.getStartTime()) +
                            " to " + new java.util.Date(currentInterval.getEndTime()));
    }
    //Flush the request history.
    serviceRequestHistories.remove(role.toString());
    //set the automatic retry
    activateAutomaticFindProviderRetry();
  }

  /**
   * overrides the superclass
   * reorder ties by provider name using the scored service description comparator
   */
  protected void reorderAnyTiedServiceDescriptions(ArrayList scoredServiceDescriptions) {
    Collections.sort(scoredServiceDescriptions, new ScoredServiceDescriptionComparator());
  }

  /**
   * Return true if according to your service request history, you have already
   * asked this provider for this complete time interval for this role.
   * Overrides the superclass to deal with service request history.
   */
  protected boolean alreadyAskedForContractWithProvider(Role role, String providerName,
      TimeInterval timeInterval) {

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
                             " alreadyAskedForContractWithProvider check " + providerName +
                             " " + role);
    }
    //what if part of the current time interval was requested but another part(s)
    //wasn't? (should return false, because if there is any uncovered part, you
    //want to make the request)

    //search for all ServiceRequestHistories with matching role
    //there should be one or none
    ServiceRequestHistory srh = (ServiceRequestHistory) serviceRequestHistories.get(role.toString());
    if(srh != null) {
      boolean ret = srh.containsRequest(providerName, timeInterval);
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() +
                               " alreadyAskedForContractWithProvider " + ret);
      }
      return ret;
    }
    else if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " alreadyAskedForContractWithProvider srh is null");
      Iterator it = serviceRequestHistories.keySet().iterator();
      while(it.hasNext()) {
	String key = (String)it.next();
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
        desiredEnd, role);

    //go through the existing service contracts and remove the
    //time periods that have been requested but not yet responded to
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if(!relay.getClient().equals(getSelfOrg())) {
        continue;
      }
      if (relay.getServiceContract() == null  &&
          relay.getServiceRequest().getServiceRole().toString().equals(role.toString())) {

	TimeInterval requestedTimeInterval = 
	  getPreferenceTimeInterval(relay.getServiceRequest().getServicePreferences());	
	
	stillNeeded = TimeInterval.removeInterval(requestedTimeInterval,
						  stillNeeded);
      }
    }

    
    
    return stillNeeded;
  }

  //requires that start <= end
  //returns a collection of intervals between neededStartTime and neededEndTime which is not
  //covered for the specified role
  private Collection getCurrentlyUncoveredIntervals(long desiredStart, long desiredEnd, Role role) {

    ArrayList desiredCoverageIntervals = new ArrayList();
    desiredCoverageIntervals.add(new TimeInterval(desiredStart, desiredEnd));
    Collection stillNeeded = desiredCoverageIntervals;

    //go through the existing service contracts and remove the time
    //periods which are covered already
    for (Iterator relayIterator = myServiceContractRelaySubscription.iterator();
         relayIterator.hasNext();) {
      ServiceContractRelay relay =
        (ServiceContractRelay) relayIterator.next();
      if(!relay.getClient().equals(getSelfOrg())) {
        continue;
      }

      ServiceContract contract = relay.getServiceContract();

      if ((contract != null)  &&
          (!contract.isRevoked()) &&
          (contract.getServiceRole().equals(role))) {

	TimeInterval providedTimeInterval = 
	  getPreferenceTimeInterval(relay.getServiceContract().getServicePreferences());

      
	if (providedTimeInterval == null) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(getAgentIdentifier() + 
				   " getCurrentlyUncoveredIntervals: " +
				   " contract with " + relay.getProviderName() +
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
    Collection c = getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(
        DEFAULT_START_TIME, DEFAULT_END_TIME, role);
    //check the personal history time interval (if it exists)
    //instead of the default interval
    ServiceRequestHistory srh = (ServiceRequestHistory)serviceRequestHistories.get(role.toString());
    if(srh != null) {
      c = getCurrentlyUncoveredIntervalsWithoutOutstandingRequests(srh.requestedTimeInterval.getStartTime(),
          srh.requestedTimeInterval.getEndTime(), role);
    }
    Iterator uncovered = c.iterator();
    while(uncovered.hasNext()) {
      TimeInterval current = (TimeInterval) uncovered.next();
      if(current.getStartTime() < current.getEndTime()) {
        if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " checkProviderCompletelyCoveredOrRequested has millisec difference of: "
                                   + (current.getEndTime() - current.getStartTime()) +
                                   " start " + current.getStartTime() + " end " +
                                   current.getEndTime());

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
    Collection c = getCurrentlyUncoveredIntervals(DEFAULT_START_TIME, DEFAULT_END_TIME,
        role);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     " checkProviderCompletelyCovered: " + 
			     " uncovered intervals for " + role +
			     " = " + c);
    }

    //check the personal history time interval (if it exists)
    //instead of the default interval
    ServiceRequestHistory srh = (ServiceRequestHistory)serviceRequestHistories.get(role.toString());
    if(srh != null) {
      c = getCurrentlyUncoveredIntervals(srh.requestedTimeInterval.getStartTime(),
          srh.requestedTimeInterval.getEndTime(), role);

      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(getAgentIdentifier() +
			       " checkProviderCompletelyCovered: " + 
			       " uncovered intervals from service request history " + 
			       srh  + " = " + c);
      }
    }

    for (Iterator uncovered = c.iterator(); uncovered.hasNext();) {
      TimeInterval current = (TimeInterval) uncovered.next();

      if(current.getStartTime() < current.getEndTime()) {
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

  private void activateAutomaticFindProviderRetry() {
    //todo: set a daily alarm
  }

  private static class ServiceRequestHistory {
    
    Role requestedRole;
    TimeInterval requestedTimeInterval;
    ArrayList serviceRequestRecords;
    
    
    ServiceRequestHistory(Role requestedRole, 
			  TimeInterval requestedTimeInterval,
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
    boolean containsRequest(String providerName, TimeInterval requestedTimeInterval) {
      
      Iterator requests = serviceRequestRecords.iterator();
      ArrayList unRequestedTimeIntervals = new ArrayList();
      unRequestedTimeIntervals.add(new TimeInterval(requestedTimeInterval.getStartTime(),
						    requestedTimeInterval.getEndTime()));
      while(requests.hasNext()) {
	ServiceRequestRecord current = (ServiceRequestRecord) requests.next();
	ArrayList newUnrequestedTimeIntervals = new ArrayList();
	if(current.providerName.equals(providerName)) {
	  Iterator oldUnrequested = unRequestedTimeIntervals.iterator();
	  while(oldUnrequested.hasNext()) {
	    TimeInterval oldCurrent = (TimeInterval)oldUnrequested.next();
	    newUnrequestedTimeIntervals.addAll(oldCurrent.subtractInterval(current.requestedTimeInterval));
	  }
	  unRequestedTimeIntervals = newUnrequestedTimeIntervals;
	}
      }
      
      if(unRequestedTimeIntervals.isEmpty()) {
	return true;
      }
      else {
	Iterator unrequested = unRequestedTimeIntervals.iterator();
	while(unrequested.hasNext()) {
	  TimeInterval current = (TimeInterval)unrequested.next();
	  //may need to add an epsilon ball around each time
	  if(current.getStartTime() != current.getEndTime()) {
	    return false;
	  }
	}
	return true;
      }
      
    }
    
    
  }

  protected static class ServiceRequestRecord {
    String providerName;
    TimeInterval requestedTimeInterval;
    
    ServiceRequestRecord(String providerName, 
			 TimeInterval requestedTimeInterval) {
      this.providerName = providerName;
      this.requestedTimeInterval = requestedTimeInterval;
    }
  }

  protected static class ScoredServiceDescriptionComparator implements java.util.Comparator {

    public int compare(Object first, Object second) {
      if( !(first instanceof ScoredServiceDescription) &&
	  !(second instanceof ScoredServiceDescription)) {
	throw new ClassCastException("Both objects must be ScoredServiceDescriptions");
      }
      else  {
	ScoredServiceDescription firstSSD = (ScoredServiceDescription) first;
	ScoredServiceDescription secondSSD = (ScoredServiceDescription) second;
	if(firstSSD.getScore() > secondSSD.getScore()) {
	  return 1;
	}
	else if(firstSSD.getScore() < secondSSD.getScore()) {
	  return -1;
	}
	else {
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

    public ALLineageEchelonScorer(LineageListWrapper lineageListWrapper,
				  String minimumEchelon,
				  Role role,
				  String cn,
				  ServiceRequestHistory srh,
				  Collection uti) {
      super(lineageListWrapper, minimumEchelon, role);
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
	TimeInterval timeInterval = (TimeInterval) iterator.next();
	
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
  
  /** returns null if start/end time preference not specified **/
  private static TimeInterval getPreferenceTimeInterval(Collection servicePreferences) {
    long start = 
      (long)SDFactory.getPreference(servicePreferences,
				    Preference.START_TIME);
    long end = 
      (long)SDFactory.getPreference(servicePreferences,
				    Preference.END_TIME);

    if ((start == -1) || (end == -1)) {
      return null;
    } else {
      return new TimeInterval(start, end);
    }
  }

}

