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

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.Constants;

import org.cougaar.core.plugin.util.AllocationResultHelper;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

public class ExternalAllocator extends InventoryModule {

    Organization myOrg;
    Role         providerRole;
    /** list of nsn's with no resupply point */
    private Vector noResupply;


    public ExternalAllocator(InventoryPlugin imPlugin, Role provider) {
	super(imPlugin);
	providerRole = provider;
	noResupply = new Vector();
    }

    public void forwardSupplyTasks(Collection supplyTasks, Organization org) {
	//assuming inventory plugin calls this with knowledge that 
	//all passed tasks are to be forwarded
        myOrg = org;
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
	    allocateTask(refill);
	}
    }

    private boolean allocateTask(Task task) {
	Organization provider = findBestSource(task);
	if (provider != null) {
	  if(verifyBeforeAllocation(task,provider)){
	    AllocationResult estAR =  createPredictedAllocationResult(task,provider);
	    Allocation alloc = buildAllocation(task, provider, providerRole);
	    if (estAR != null){
	      alloc.setEstimatedResult(estAR);
	      if(inventoryPlugin.publishAdd(alloc)) {
		return true;
	      }
	      else {
		logger.error("Unable to publish the allocation " + alloc);
	      }
		   
	    }
	  }
	}
	return false;
    }

    /** build Allocation with an estimated alloc result */
    private Allocation buildAllocation(Task t, Asset a, Role r)
    {
	return inventoryPlugin.getRootFactory().createAllocation(t.getPlan(), t, a, null, r);
    }

    private AllocationResult createPredictedAllocationResult(Task task, Organization provider) {
	//MWD in the future this will have to generate the expected result
	//from the predictor.
	return new AllocationResultHelper(task, null).getAllocationResult(0.25);
    }

    /** Figure out which organization supplying item is best for us. */
    private Organization findBestSource(Task task) {
	Enumeration support_orgs;
        if (getTaskUtils().isProjection(task)) {
            /* For a projection, should be time-phased as support
               changes over time. We ignore that, for now */
            support_orgs = getAssetUtils().getSupportingOrgs(myOrg, 
							     providerRole, 
							     getTaskUtils().getStartTime(task), 
							     TaskUtils.getEndTime(task));
        } else {
            support_orgs = getAssetUtils().getSupportingOrgs(myOrg, providerRole, 
                                                        getTaskUtils().getEndTime(task));
        }
	if (support_orgs.hasMoreElements()) {
	    // For now, returning the first supporting org during the time span
	    return (Organization)support_orgs.nextElement();
	}
	else {
	    String itemId = task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
	    if (getTaskUtils().isProjection(task)) {
		logger.error("No "+providerRole+" for task " + task.getUID() + ", during ["+getTimeUtils().dateString(getTaskUtils().getStartTime(task))+
			     "-" +
			   getTimeUtils().dateString(TaskUtils.getEndTime(task)) +"]");
	    } else {
		logger.error("No "+providerRole+" for task " + task.getUID() + ", during "+getTimeUtils().dateString(getTaskUtils().getEndTime(task)));
	    }
	    // prevents same error message from being printed many times.
	    if (!noResupply.contains(itemId)) {
		logger.debug("findBestSource() <"+ providerRole +
			   "> allocateRefillTask no best source for "+itemId);
		noResupply.addElement(itemId);
	    }
	}
	return null;
    }



    private boolean verifyBeforeAllocation(Task task, Organization org) {
      // Do not allocate tasks after they have taken place-AF does this make sense?
      if (!(task.beforeCommitment(new Date(inventoryPlugin.currentTimeMillis())))) {
      logger.warn("publishAllocation: return ... after commitment"+task.getCommitmentDate()+" task:"+task+" to Asset "+org);
      // too late to change
      return false;
      }
      PlanElement pe = task.getPlanElement();
      if (pe == null) {
	return true;
      }
      else {
	logger.error("Should not publishAdd.  Task" + task + 
		     " unexpectedly already has a plan element [" +
		     pe + "]");
      }
      return false;
    }


  public void updateAllocationResult(IncrementalSubscription sub) {
    // Set up the affected inventories for the AllocationAssessor
    Task refill;
    Asset asset;
    Allocation alloc;
    Inventory inventory;
    LogisticsInventoryPG logInvPG;
    Iterator refill_list = sub.getAddedCollection().iterator();
    while (refill_list.hasNext()) {
      alloc = (Allocation) refill_list.next();
      //      refill = (Task)refill_list.next();
      refill = alloc.getTask();
      asset = (Asset)refill.getDirectObject();
      inventory = inventoryPlugin.findOrMakeInventory(asset);
      logInvPG = (LogisticsInventoryPG)
	inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      if (refill.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
	logInvPG.updateRefillProjection(refill);
      } else {
	logInvPG.updateRefillRequisition(refill);
      }
      inventoryPlugin.touchInventory(inventory);
    }
    PluginHelper.updateAllocationResult(sub);
    rebuildPGCustomerHash();
  }

  private void rebuildPGCustomerHash() {
    Collection changedInventories = inventoryPlugin.getTouchedInventories();
    Iterator invIter = changedInventories.iterator();
    Inventory inventory;
    LogisticsInventoryPG thePG;
    while (invIter.hasNext()) {
      inventory = (Inventory)invIter.next();
      thePG = (LogisticsInventoryPG)
	inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      thePG.rebuildCustomerHash();
    }
  }
}
