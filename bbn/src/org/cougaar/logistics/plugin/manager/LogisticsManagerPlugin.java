/*
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
package org.cougaar.logistics.plugin.manager;

import java.util.*;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityRequest;
import org.cougaar.core.util.UID;

import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.util.UnaryPredicate;


/**
 * Test implementation to verify that ABAMessages are reaching the
 * Manager agent. Logs all added/modified/removed LoadIndicators
 */
public class LogisticsManagerPlugin extends SimplePlugin {
  private IncrementalSubscription myLoadIndicators;
  private IncrementalSubscription myFallingBehindPolicies;

  private UIDService myUIDService;
  private LoggingService myLoggingService;

  private double myFallingBehindValue = UNINITIALIZED;

  Collection myCommunitiesToManage;

  private static double UNINITIALIZED = -1.0;

  // From FallingBehind values in the playbook
  private static double NORMAL = .5;
  private static double MODERATE = 1.0;
  private static double SEVERE = 2.0;
  private static double MAX_FALLING_BEHIND_VALUE = SEVERE;
  private static double DEFAULT_FALLING_BEHIND_VALUE = NORMAL;
  

  private static UnaryPredicate myLoadIndicatorsPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LoadIndicator) {
        return true;
      } else {
        return false;
      }
    }
  };

  private static UnaryPredicate myFallingBehindPoliciesPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof FallingBehindPolicy) {
        return true;
      } else {
        return false;
      }
    }
  };

  protected void setupSubscriptions() {
    myUIDService = 
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);
    
    myLoggingService = 
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    myLoadIndicators = (IncrementalSubscription) subscribe(myLoadIndicatorsPred);
    myFallingBehindPolicies = (IncrementalSubscription) subscribe(myFallingBehindPoliciesPred);

    // Find name of community to monitor
    myCommunitiesToManage = getCommunityService().search("(CommunityManager=" +
      getAgentIdentifier().toString() + ")");
    if (myCommunitiesToManage.isEmpty()) {
      myLoggingService.warn(getAgentIdentifier() + " is not a CommunityManager." +
                            " Plugin will not be receiving LoadIndicators.");
    }

    if (didRehydrate()) {
      myFallingBehindValue = communityFallingBehindValue();
    }


    if (myFallingBehindValue == UNINITIALIZED) {
      myFallingBehindValue = DEFAULT_FALLING_BEHIND_VALUE;
    }

    if (myFallingBehindPolicies.size() == 0) {
      createFallingBehindPolicies();
    }
    
  }
  
  public void execute() {

    if (myLoggingService.isDebugEnabled()) {
      for (Iterator iterator = myLoadIndicators.getAddedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug("New LoadIndicator: " + 
                               ((LoadIndicator) iterator.next()).toString());
      }
      
      for (Iterator iterator = myLoadIndicators.getChangedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug("Modified LoadIndicator: " + 
                               ((LoadIndicator) iterator.next()).toString());
      }
      
      for (Iterator iterator = myLoadIndicators.getRemovedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug("Removed LoadIndicator: " + 
                               ((LoadIndicator) iterator.next()).toString());
      }
    }

    if (myLoadIndicators.hasChanged()) {
      double currentFallingBehindValue = communityFallingBehindValue();
      
      if (myFallingBehindValue != currentFallingBehindValue) {
        myFallingBehindValue = currentFallingBehindValue;
        adjustFallingBehindPolicy();
        
      }
    }
  }

  protected double getFallingBehindValue() {
    return myFallingBehindValue;
  }

  private double communityFallingBehindValue() {
    double FallingBehindValue = -1;

    for (Iterator iterator = myLoadIndicators.getCollection().iterator(); 
         iterator.hasNext();) {
      double nextFallingBehindValue = 
        loadStatusToFallingBehindValue(((LoadIndicator) iterator.next()));

      FallingBehindValue = Math.max(FallingBehindValue, nextFallingBehindValue);

      if (FallingBehindValue == MAX_FALLING_BEHIND_VALUE) {
        // Exit loop because we can't get any worse
        break;
      }
    }
    
    return FallingBehindValue;
  }
  
  private double loadStatusToFallingBehindValue(LoadIndicator loadIndicator) {
    String loadStatus = loadIndicator.getLoadStatus();
    
    if (loadStatus.equals(LoadIndicator.NORMAL_LOAD)) {
      return NORMAL;
    } else if (loadStatus.equals(LoadIndicator.MODERATE_LOAD)) {
      return MODERATE;
    } else if (loadStatus.equals(LoadIndicator.SEVERE_LOAD)) {
      return SEVERE;
    } else {
      myLoggingService.warn("Unrecognized load status: " + loadStatus);
      return UNINITIALIZED;
    }
  }


  private void adjustFallingBehindPolicy() {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("Modifying FallingBehindPolicy: new falling behind =  " + getFallingBehindValue()); 
    }

    FallingBehindPolicy fallingBehindPolicy;

    for (Iterator iterator = myFallingBehindPolicies.iterator();
           iterator.hasNext();) {
        fallingBehindPolicy = (FallingBehindPolicy) iterator.next();
        fallingBehindPolicy.setFallingBehindValue(getFallingBehindValue());
        publishChange(fallingBehindPolicy);
    }

  }

  /**
   * Gets reference to CommunityService.
   */
  private CommunityService getCommunityService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      return (CommunityService)sb.getService(this, CommunityService.class,
        new ServiceRevokedListener() {
          public void serviceRevoked(ServiceRevokedEvent re) {}
      });
    } else {
      myLoggingService.error("CommunityService not available");
      return null;
    }
  }
 
  private void createFallingBehindPolicies() {
    // Publish default falling behind policy
    for (Iterator iterator = myCommunitiesToManage.iterator(); 
         iterator.hasNext();) {
      FallingBehindPolicy fallingBehindPolicy = 
        new FallingBehindPolicy(getFallingBehindValue());
      fallingBehindPolicy.setUID(myUIDService.nextUID());
      String community = (String) iterator.next();
      fallingBehindPolicy.setTarget(new AttributeBasedAddress(community,
                                                              "Role",
                                                              "Member"));
      publishAdd(fallingBehindPolicy);
    }
  }
    
}








