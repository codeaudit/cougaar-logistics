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
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
//test
import org.cougaar.planning.ldm.plan.Relationship;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.TimeInterval;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.util.UDDIConstants;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.plugin.SDClientPlugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
      if ((relay.getServiceContract().isRevoked() ||
           !timeIntervalRequestedCompletelySatisfied(relay) ) &&
           (relay.getClient().equals(getSelfOrg()))) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " queryServices because a service contract was revoked or did not satisfy the request for " +
                                 relay.getServiceContract().getServiceRole().toString());
        }

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
          long start = (long)SDFactory.getPreference(relay.getServiceRequest().getServicePreferences(),
              Preference.START_TIME);
          long end = (long)SDFactory.getPreference(relay.getServiceRequest().getServicePreferences(),
              Preference.END_TIME);
          TimeInterval timeInterval = new TimeInterval(start, end);
          ServiceRequestRecord srr = new ServiceRequestRecord(relay.getProviderName(),
              timeInterval);
          srh = new ServiceRequestHistory(relay.getServiceContract().getServiceRole(),
              timeInterval, srr);
          serviceRequestHistories.put(relay.getServiceContract().getServiceRole().toString(),
                                      srh);
          Iterator it = serviceRequestHistories.keySet().iterator();
          while(it.hasNext()) {
            if (myLoggingService.isDebugEnabled()) {
              myLoggingService.debug("key " +(String)it.next());
            }
          }
        }

        //change the relay. If revoked, change request to zero.
        //If contract doesnt cover request, shrink request
        if(relay.getServiceContract().isRevoked()) {
          //todo, doesn't matter much now because we won't look at revoked ones
        }
        else { //change request to match contract
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " change request to match contract for " +
                                   relay.getProviderName() + " " +
                                   relay.getServiceContract().getServiceRole().toString());
          }
          ServiceRequest request =
              mySDFactory.newServiceRequest(getSelfOrg(),
              relay.getServiceContract().getServiceRole(),
              relay.getServiceContract().getServicePreferences());
          relay.setServiceRequest(request);
          publishChange(relay);
        }

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() +
                                 " do another queryServices for " +
                                 relay.getServiceContract().getServiceRole().toString());
        }
        queryServices(relay.getServiceContract().getServiceRole());
        //publishRemove(relay);
      }
      else if (timeIntervalRequestedCompletelySatisfied(relay) &&
             relay.getClient().equals(getSelfOrg())) {
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " handleChangedServiceContractRelays timeIntervalRequestedCompletelySatisfied " +
                                   relay.getServiceContract().getServiceRole().toString());
          }

        }
      else if (!timeIntervalRequestedCompletelySatisfied(relay) &&
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

    double requestedStart = SDFactory.getPreference(relay.getServiceRequest().getServicePreferences(),
                            Preference.START_TIME);
    double requestedEnd = SDFactory.getPreference(relay.getServiceRequest().getServicePreferences(),
                            Preference.END_TIME);
    double contractedStart = SDFactory.getPreference(relay.getServiceContract().getServicePreferences(),
                            Preference.START_TIME);
    double contractedEnd = SDFactory.getPreference(relay.getServiceContract().getServicePreferences(),
                            Preference.END_TIME);

    boolean ret = requestedStart >= contractedStart && requestedEnd <= contractedEnd;
    return ret;

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
    else {
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug(getAgentIdentifier() +
                               " alreadyAskedForContractWithProvider srh is null");
      }
      Iterator it = serviceRequestHistories.keySet().iterator();
      while(it.hasNext()) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug("key " +(String)it.next());
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

        double endTime = SDFactory.getPreference(
            relay.getServiceRequest().getServicePreferences(),
            Preference.END_TIME);
        double startTime = SDFactory.getPreference(
            relay.getServiceRequest().getServicePreferences(),
            Preference.START_TIME);

        stillNeeded = TimeInterval.removeInterval(new TimeInterval((long)startTime, (long)endTime), stillNeeded);
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
      if (relay.getServiceContract() != null  &&
          !relay.getServiceContract().isRevoked() &&
          relay.getServiceContract().getServiceRole().toString().equals(role.toString())) {

        double endTime = SDFactory.getPreference(
            relay.getServiceContract().getServicePreferences(),
            Preference.END_TIME);
        double startTime = SDFactory.getPreference(
            relay.getServiceContract().getServicePreferences(),
            Preference.START_TIME);

        stillNeeded = TimeInterval.removeInterval(new TimeInterval((long)startTime, (long)endTime), stillNeeded);
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
    Iterator unCovered = c.iterator();
    while(unCovered.hasNext()) {
      TimeInterval current = (TimeInterval) unCovered.next();
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
    //check the personal history time interval (if it exists)
    //instead of the default interval
    ServiceRequestHistory srh = (ServiceRequestHistory)serviceRequestHistories.get(role.toString());
    if(srh != null) {
      c = getCurrentlyUncoveredIntervals(srh.requestedTimeInterval.getStartTime(),
          srh.requestedTimeInterval.getEndTime(), role);
    }
    Iterator unCovered = c.iterator();
    while(unCovered.hasNext()) {
      TimeInterval current = (TimeInterval) unCovered.next();

      if(current.getStartTime() < current.getEndTime()) {
        if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier() +
                                   " checkProviderCompletelyCovered has millisec difference of: "
                                   + (current.getEndTime() - current.getStartTime()) +
                                   " start " + current.getStartTime() + " end " +
                                   current.getEndTime());
        }
        return false;
      }
    }
    return true;
  }

  private void activateAutomaticFindProviderRetry() {
    //todo: set a daily alarm
  }

}

/*
class TimeInterval {
  private long startTime;
  private long endTime;
  TimeInterval(long start, long end) {
    startTime = start;
    endTime = end;
  }
  long getEndTime() {
    return endTime;
  }
  long getStartTime() {
    return startTime;
  }

  public static Collection removeInterval(TimeInterval interval, Collection desiredCoverageIntervals) {
    ArrayList ret = new ArrayList();
    Iterator it = desiredCoverageIntervals.iterator();
    while(it.hasNext()) {
      TimeInterval current = (TimeInterval) it.next();
      ret.addAll(current.substractInterval(interval));
    }
    return ret;
  }


  Collection substractInterval(TimeInterval interval) {
    ArrayList ret = new ArrayList();

    //intervals are not overlapping
    //return this
    //this      ******
    //interval           *****
    if(this.getStartTime() < interval.getStartTime() &&
       this.getEndTime() < interval.getEndTime() &&
       this.getEndTime() <= interval.getStartTime()) {
      ret.add(this);
      return ret;
    }
    //this             ******
    //interval *****
    else if(interval.getStartTime() < this.getStartTime() &&
            interval.getEndTime() < this.getEndTime() &&
            interval.getEndTime() <= this.getStartTime()) {
      ret.add(this);
      return ret;
    }

    //this is completely contained
    //return empty
    //this       ****
    //interval  *******
    else if(interval.getStartTime() <= this.getStartTime() &&
       interval.getEndTime() >= this.getEndTime()) {
      return ret;
    }

    //interval is completely contained
    //return 0 or 1 or 2 time intervals
    //this     *********
    //interval    ****
    else if(this.getStartTime() <= interval.getStartTime() &&
            this.getEndTime() >= interval.getEndTime()) {

      if(this.getStartTime() < interval.getStartTime()) {
        ret.add(new TimeInterval(this.getStartTime(), interval.getStartTime()));
      }
      if(this.getEndTime() > interval.getEndTime()) {
        ret.add(new TimeInterval(interval.getEndTime(), this.getEndTime()));
      }
      return ret;
    }

    //overlap w/o containing
    //return 1 time interval
    //this   ******
    //inteval    *****
    else if(this.getStartTime() <= interval.getStartTime() &&
            this.getEndTime() <= interval.getEndTime() &&
            interval.getStartTime() < this.getEndTime()) {
      ret.add(new TimeInterval(this.getStartTime(), interval.getStartTime()));
      return ret;

    }
    //this       ******
    //inteval  *****
    else if(interval.getStartTime() <= this.getStartTime() &&
            interval.getEndTime() <= this.getEndTime() &&
            this.getStartTime() < interval.getEndTime()) {
      ret.add(new TimeInterval(interval.getEndTime(), this.getEndTime()));
      return ret;
    }

    else {
      //this should never happen
      return ret;
    }
  }

}*/

class ServiceRequestHistory{

  Role requestedRole;
  TimeInterval requestedTimeInterval;
  ArrayList serviceRequestRecords;

  ServiceRequestHistory(Role requestedRole, TimeInterval requestedTimeInterval,
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
          newUnrequestedTimeIntervals.addAll(oldCurrent.substractInterval(current.requestedTimeInterval));
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

class ServiceRequestRecord{
  String providerName;
  TimeInterval requestedTimeInterval;

  ServiceRequestRecord(String providerName, TimeInterval requestedTimeInterval) {
    this.providerName = providerName;
    this.requestedTimeInterval = requestedTimeInterval;
  }
}

class ScoredServiceDescriptionComparator implements java.util.Comparator {

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
