/*
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
public class SupplyLoadIndicatorTestPlugin extends SimplePlugin {
  private IncrementalSubscription myOplanSubscription;
  private IncrementalSubscription myLoadIndicatorSubscription;
  private IncrementalSubscription myGLSSubscription;
  private IncrementalSubscription myInterAgentOperatingModePolicySubscription;

  private BlackboardService myBlackboardService;
  private LoggingService myLoggingService;
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
        if (verb.equals(org.cougaar.logistics.ldm.Constants.Verb.GetLogSupport)) {
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
    myOplanSubscription = (IncrementalSubscription)subscribe(myOplanPred);
    myLoadIndicatorSubscription = (IncrementalSubscription)subscribe(myLoadIndicatorPred);
    myGLSSubscription = (IncrementalSubscription)subscribe(myGLSPred);
    myInterAgentOperatingModePolicySubscription = (IncrementalSubscription)subscribe(myInterAgentOperatingModePolicyPred);
    
    myBlackboardService = 
      (BlackboardService) getBindingSite().getServiceBroker().getService(this, BlackboardService.class, null);

    myUIDService = 
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null); 

    myLoggingService = 
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null); 
    
  }

  public void execute() {
    if (myOplanSubscription.getAddedCollection().size() > 0) {
      CommunityService communityService =
        (CommunityService) getBindingSite().getServiceBroker().getService(this, CommunityService.class, null);

      if (communityService == null) {
        myLoggingService.error("CommunityService not available.");
        return;
      }

      Collection alCommunities = communityService.listParentCommunities(getAgentIdentifier().toString(),
                                                                        "(CommunityType=AdaptiveLogistics)",
                                                                        null);

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

    if (myGLSSubscription.getAddedCollection().size() > 0) {
      for (Iterator iterator = myLoadIndicatorSubscription.getCollection().iterator();
           iterator.hasNext();) {
        LoadIndicator loadIndicator = (LoadIndicator) iterator.next();
        loadIndicator.setLoadStatus(LoadIndicator.SEVERE_LOAD);
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier().toString() + 
                                 ": changing load status to SEVERE_LOAD for LoadIndicator to  " + 
                                 loadIndicator.getTargets());
        }
        publishChange(loadIndicator);
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

