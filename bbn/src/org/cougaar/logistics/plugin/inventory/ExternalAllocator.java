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

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.logistics.ldm.Constants;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.plugin.util.AllocationResultHelper;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;


public class ExternalAllocator extends InventoryModule implements AllocatorModule {

  Role providerRole;
  /** list of nsn's with no resupply point */
  private Vector noResupply;

  public ExternalAllocator(InventoryPlugin imPlugin, Role provider) {
    super(imPlugin);
    providerRole = provider;
    noResupply = new Vector();
  }

  public void forwardSupplyTasks(Collection supplyTasks) {
    //assuming inventory plugin calls this with knowledge that 
    //all passed tasks are to be forwarded
    Task supplyTask;
    Iterator tasksIter = supplyTasks.iterator();
    if (providerRole == null) {
      logger.error("Unknown Role.  Cannot allocate tasks.");
      return;
    }
    while (tasksIter.hasNext()) {
      supplyTask = (Task)tasksIter.next();
      allocateTask(supplyTask);
    }
  }

  // allocate our new refill tasks
  public void allocateRefillTasks(Collection newRefills) {
    Task refill;
    Iterator refillIT = newRefills.iterator();
    while(refillIT.hasNext()) {
      refill = (Task) refillIT.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc" + inventoryPlugin.getSupplyType()+"]" +
                     "trying to allocate refill task: " + refill.getUID());
      }
      allocateTask(refill);
    }
  }

  private boolean allocateTask(Task task) {
    if (task.getPlanElement() != null) {
      logger.error("Found Task with PlanElement. Ignoring task.");
      return true;
    }
    Organization provider = findBestSource(task);
    if (provider != null) {
      if(verifyBeforeAllocation(task,provider)){
        AllocationResult estAR =  createPredictedAllocationResult(task);
        Allocation alloc;
        //either make an allocation or reset the estimated AR on the
        // already existing pe - mostly likely to happen if a task's prefs
        // changed.
        if (task.getPlanElement() == null) {
          alloc = buildAllocation(task, provider, providerRole);
          if (estAR != null){
            alloc.setEstimatedResult(estAR);
            if(inventoryPlugin.publishAdd(alloc)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc[" + inventoryPlugin.getSupplyType()+"]" +
                             "publish Adding allocation: " + alloc.getUID()+
                             " parent task is: " + task.getUID() + " publishAdd returned: true");
              }
              return true;
            }
            else {
              logger.error("Unable to publish the allocation " + alloc);
            }
          }
        } else {
          alloc = (Allocation)task.getPlanElement();
          if (estAR != null){
            alloc.setEstimatedResult(estAR);
            if(inventoryPlugin.publishChange(alloc)) {
              return true;
            }
            else {
              logger.error("Unable to publish Change the allocation " + alloc);
            }
          }
        }
      }
    }
    return false;
  }

  /** build Allocation with an estimated alloc result */
  private Allocation buildAllocation(Task t, Asset a, Role r)
  {
    return inventoryPlugin.getPlanningFactory().createAllocation(t.getPlan(), t, a, null, r);
  }

  private AllocationResult createPredictedAllocationResult(Task task) {
    //MWD in the future this will have to generate the expected result
    //from the predictor.
    return new AllocationResultHelper(task, null).getAllocationResult(0.25, true);
  }

  /** Figure out which organization supplying item is best for us. */
  private Organization findBestSource(Task task) {
    Enumeration support_orgs;
    if (getTaskUtils().isProjection(task)) {
      /* For a projection, should be time-phased as support
      changes over time. We ignore that, for now */
      support_orgs = getAssetUtils().getSupportingOrgs(inventoryPlugin.getMyOrganization(),
                                                       providerRole,
                                                       getTaskUtils().getStartTime(task),
                                                       TaskUtils.getEndTime(task));
    } else {
      support_orgs = getAssetUtils().getSupportingOrgs(inventoryPlugin.getMyOrganization(),
                                                       providerRole,
                                                       getTaskUtils().getEndTime(task));
    }
    if (support_orgs.hasMoreElements()) {
      // For now, returning the first supporting org during the time span
      return (Organization)support_orgs.nextElement();
    }
    else {
      String itemId = task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
      if (getTaskUtils().isProjection(task)) {
        logger.warn("No "+providerRole+" for task " + task.getUID() + ", during ["+
                    getTimeUtils().dateString(getTaskUtils().getStartTime(task))+
                    "-" +
                    getTimeUtils().dateString(TaskUtils.getEndTime(task)) +"]"+". Will retry.");
      } else {
        logger.warn("No "+providerRole+" for task " + task.getUID() + ", during "+
                    getTimeUtils().dateString(getTaskUtils().getEndTime(task))+". Will retry.");
      }
      // prevents same error message from being printed many times.
// 	    if (!noResupply.contains(itemId)) {
// 		logger.debug("findBestSource() <"+ providerRole +
// 			   "> allocateRefillTask no best source for "+itemId);
// 		noResupply.addElement(itemId);
// 	    }
    }
    return null;
  }

  private boolean verifyBeforeAllocation(Task task, Organization org) {
    // Do not allocate tasks after they have taken place-AF does this make sense?
    if (!(task.beforeCommitment(new Date(inventoryPlugin.currentTimeMillis())))) {
      logger.warn("verifyBeforeAllocation: return ... after commitment"+task.getCommitmentDate()+" task:"+task+" to Asset "+org);
      // too late to change
      return false;
    }
    return true;
  }

  public HashSet updateAllocationResult(Collection sub) {
    //    System.out.println("ex alloc update ARs being called in" + inventoryPlugin.getClusterId());
    HashSet backwardFlowInventories = new HashSet();
    // Set up the affected inventories for the AllocationAssessor
    Task refill;
    Asset asset;
    Allocation alloc;
    Inventory inventory;
    LogisticsInventoryPG logInvPG = null;
    Iterator refill_list = sub.iterator(); // ###
    while (refill_list.hasNext()) {
      alloc = (Allocation) refill_list.next();
      refill = alloc.getTask();
      asset = (Asset)refill.getDirectObject();
      inventory = inventoryPlugin.findOrMakeInventory(asset);

      if (PluginHelper.updatePlanElement(alloc)) {
        boolean didPubChg = inventoryPlugin.publishChange(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc[" + inventoryPlugin.getSupplyType()+"]" +
                       "publist Changing allocation due to AR change: " + alloc.getUID()+ 
                       " parent task is: " + alloc.getTask().getUID() + " publishChange returned: " + didPubChg);
        }
        if (inventory != null) { // else { this is a pass-thru }
          //System.out.println("ex all publish changing and added to AA's inventory list " + logInvPG.getResource());
          backwardFlowInventories.add(inventory);

          logInvPG = (LogisticsInventoryPG)
              inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
          if (refill.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
            logInvPG.updateRefillProjection(refill);
          } else {
            logInvPG.updateRefillRequisition(refill);
          }
        }
      }
    }
    return backwardFlowInventories;
  }
}
