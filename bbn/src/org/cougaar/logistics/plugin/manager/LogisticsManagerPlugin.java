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

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityRoster;

import org.cougaar.util.UnaryPredicate;


/**
 * Test implementation to verify that ABAMessages are reaching the
 * Manager agent. Logs all added/modified/removed LoadIndicators
 */
public class LogisticsManagerPlugin extends SimplePlugin {
  private IncrementalSubscription myLoadIndicators;
  private LoggingService myLoggingService;

  private UnaryPredicate myLoadIndicatorsPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LoadIndicator) {
        return true;
      } else {
        return false;
      }
    }
  };

  protected void setupSubscriptions() {
    myLoggingService = 
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("LogisticsManagerPlugin: setting up subscriptions.");
    }
    
    myLoadIndicators = (IncrementalSubscription) subscribe(myLoadIndicatorsPred);
  }

  public void execute() {
    for (Iterator iterator = myLoadIndicators.getAddedCollection().iterator(); 
         iterator.hasNext();) {
      myLoggingService.warn(getClusterIdentifier() + ":New LoadIndicator: " + 
                            ((LoadIndicator) iterator.next()).toString());
    }
    
    for (Iterator iterator = myLoadIndicators.getChangedCollection().iterator(); 
         iterator.hasNext();) {
      myLoggingService.warn(getClusterIdentifier() + ":Modified LoadIndicator: " + 
                            ((LoadIndicator) iterator.next()).toString());
    }

    for (Iterator iterator = myLoadIndicators.getRemovedCollection().iterator(); 
         iterator.hasNext();) {
      myLoggingService.warn(getClusterIdentifier() + ":Removed LoadIndicator: " + 
                            ((LoadIndicator) iterator.next()).toString());

    }

    
  }
                       
}






