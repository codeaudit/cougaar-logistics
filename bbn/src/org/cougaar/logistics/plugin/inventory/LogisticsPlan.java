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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.Oplan;
import java.util.*;


public class LogisticsPlan implements LogisticsPlanModule {

  protected transient Logger logger;
  private MessageAddress agentID;
  
  public LogisticsPlan(MessageAddress agentIdentifier) {
    agentID = agentIdentifier;
    // Using the static Logger so cannot add the agentID prefix, must do it in the logger messages
    logger = Logging.getLogger(LogisticsPlan.class);
  }

  public LogisticsOPlan updateOrgActivities(IncrementalSubscription oplanSubscription, 
                                            IncrementalSubscription orgActivitySubscription) {
    if (oplanSubscription.isEmpty() || orgActivitySubscription.isEmpty()) {
      return null;
    }
    Oplan oplan=null;
    Collection c = oplanSubscription.getCollection();
    for (Iterator i=c.iterator(); i.hasNext();) {
      oplan = (Oplan)i.next();
      break;
    }
    LogisticsOPlan logOplan = new LogisticsOPlan(agentID, oplan);
    logOplan.updateOrgActivities(orgActivitySubscription);
    return logOplan;
  }
  
}
