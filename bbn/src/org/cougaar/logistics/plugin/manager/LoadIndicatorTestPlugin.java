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
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

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
public class LoadIndicatorTestPlugin extends SimplePlugin {
  private IncrementalSubscription myOplanSubscription;
  private IncrementalSubscription myLoadIndicatorSubscription;
  private IncrementalSubscription myGLSSubscription;

  private LoggingService myLoggingService;
  private BlackboardService myBlackboardService;
  private UIDService myUIDService;

  private UnaryPredicate myOplanPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Oplan) {
        return true;
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate myLoadIndicatorPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof LoadIndicator) {
        return true;
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate myGLSPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        Verb verb = task.getVerb();
        if (verb.equals(org.cougaar.glm.ldm.Constants.Verb.GetLogSupport)) {
          return true;
        }
      } 
      
      return false;
    }
  };
  
  protected void setupSubscriptions() {
    myOplanSubscription = (IncrementalSubscription)subscribe(myOplanPred);
    myLoadIndicatorSubscription = (IncrementalSubscription)subscribe(myLoadIndicatorPred);
    myGLSSubscription = (IncrementalSubscription)subscribe(myGLSPred);
    
    myBlackboardService = 
      (BlackboardService) getBindingSite().getServiceBroker().getService(this, BlackboardService.class, null);
    myLoggingService = 
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    myUIDService = 
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);
  }

  public void execute() {
    if (myOplanSubscription.getAddedCollection().size() > 0) {
      LoadIndicator loadIndicator = 
        new LoadIndicator(this.getClass(), 
                          getBindingSite().getAgentIdentifier().toString(),
                          myUIDService.nextUID(),
                          LoadIndicator.NORMAL_LOAD);
      loadIndicator.addTarget(new AttributeBasedAddress("MiniTestConfig",
                                                        "Role", 
                                                        "Manager"));
      myLoggingService.warn("LoadIndicatorTestPlugin: adding LoadIndicator to be sent to " + loadIndicator.getTargets());
      publishAdd(loadIndicator);
    }

    if (myGLSSubscription.getAddedCollection().size() > 0) {
      for (Iterator iterator = myLoadIndicatorSubscription.getCollection().iterator();
           iterator.hasNext();) {
        LoadIndicator loadIndicator = (LoadIndicator) iterator.next();
        loadIndicator.setLoadStatus(LoadIndicator.SEVERE_LOAD);
        myLoggingService.warn("LoadIndicatorTestPlugin: changing load status for LoadIndicator: " + loadIndicator.getTargets());
        publishChange(loadIndicator);
      }
    }
  }
                       
}




