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
package org.cougaar.logistics.plugin.manager;

import java.util.*;

import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.community.CommunityService;

import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.oplan.Oplan;

/**
 * Test implementation -  generates a LoadIndicator when the Oplan is received, 
 * modifies LoadIndicator when GLS received.
 * 
 */
public class TransportLoadIndicatorTestPlugin extends SimplePlugin {
  private IncrementalSubscription myLoadIndicatorSubscription;
  private IncrementalSubscription myTaskSubscription;
  private IncrementalSubscription myInterAgentOperatingModePolicySubscription;

  private BlackboardService myBlackboardService;
  private LoggingService myLoggingService;
  private UIDService myUIDService;

  private UnaryPredicate myLoadIndicatorPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LoadIndicator) {
        return true;
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate myTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        Verb verb = task.getVerb();
        if (verb.equals(org.cougaar.glm.ldm.Constants.Verb.Transport)) {
          return true;
        }
      } 
      
      return false;
    }
  };

  private UnaryPredicate myInterAgentOperatingModePolicyPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof InterAgentOperatingModePolicy) {
        return true;
      } else {
        return false;
      }
    }
  };
  
  protected void setupSubscriptions() {
    myLoadIndicatorSubscription = (IncrementalSubscription)subscribe(myLoadIndicatorPred);
    myTaskSubscription = (IncrementalSubscription)subscribe(myTaskPred);
    myInterAgentOperatingModePolicySubscription = (IncrementalSubscription)subscribe(myInterAgentOperatingModePolicyPred);
    
    myBlackboardService = 
      (BlackboardService) getBindingSite().getServiceBroker().getService(this, BlackboardService.class, null);

    myUIDService = 
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null); 

    myLoggingService = 
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null); 
    
  }

  public void execute() {
    if ((myTaskSubscription.getAddedCollection().size() > 0) &&
        (myLoadIndicatorSubscription.size() == 0)) {
      CommunityService communityService = 
        (CommunityService) getBindingSite().getServiceBroker().getService(this, CommunityService.class, null);

      if (communityService == null) {
        myLoggingService.error("CommunityService not available.");
        return;
      }
      
      Collection alCommunities = communityService.listParentCommunities(getAgentIdentifier().toString(), "(CommunityType=AdaptiveLogistics)");

      if (alCommunities.size() == 0) {
        myLoggingService.warn(getAgentIdentifier().toString() + 
                                     " does not belong to an AdaptiveLogistics community.");
      }

      for (Iterator iterator = alCommunities.iterator();
           iterator.hasNext();) {
        String community = (String) iterator.next();
        LoadIndicator loadIndicator = 
          new LoadIndicator(this.getClass(), 
                            getAgentIdentifier().toString(),
                            myUIDService.nextUID(),
                            LoadIndicator.MODERATE_LOAD);
        loadIndicator.addTarget(AttributeBasedAddress.getAttributeBasedAddress(community,
                                                          "Role", 
                                                          "AdaptiveLogisticsManager"));
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier().toString() + 
                                 ": adding LoadIndicator to be sent to " + 
                                 loadIndicator.getTargets());
        }
        publishAdd(loadIndicator);
      }
    }

    if (myInterAgentOperatingModePolicySubscription.getChangedCollection().size() > 0) {
      // Increase load to SEVERE
      for (Iterator iterator = myLoadIndicatorSubscription.getCollection().iterator();
           iterator.hasNext();) {
        LoadIndicator loadIndicator = (LoadIndicator) iterator.next();
        if (!loadIndicator.getLoadStatus().equals(LoadIndicator.SEVERE_LOAD)) {
          loadIndicator.setLoadStatus(LoadIndicator.SEVERE_LOAD);
          if (myLoggingService.isDebugEnabled()) {
            myLoggingService.debug(getAgentIdentifier().toString() + 
                                   ": changing load status to SEVERE_LOAD for LoadIndicator to  " + 
                                   loadIndicator.getTargets());
          }
          publishChange(loadIndicator);
        }
      }
    }

    if (myLoggingService.isDebugEnabled()) {
      for (Iterator iterator = myInterAgentOperatingModePolicySubscription.getAddedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug(getAgentIdentifier().toString() + 
                               ": new InterAgentOperatingModePolicy: " + 
                               ((InterAgentOperatingModePolicy) iterator.next()).toString());
        
      } 


      for (Iterator iterator = myInterAgentOperatingModePolicySubscription.getChangedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug(getAgentIdentifier().toString() + ": modified InterAgentOperatingModePolicy: " + 
                                 ((InterAgentOperatingModePolicy) iterator.next()).toString());
      }
    
      for (Iterator iterator = myInterAgentOperatingModePolicySubscription.getRemovedCollection().iterator(); 
           iterator.hasNext();) {
        myLoggingService.debug(getAgentIdentifier().toString() + 
                               ": removed InterAgentOperatingModePolicy: " + 
                               ((InterAgentOperatingModePolicy) iterator.next()).toString());
        
      } 
    }
  }
}

