/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.cougaar.planning.ldm.LDMServesPlugin;

import org.cougaar.logistics.ldm.Constants;

import org.cougaar.glm.ldm.asset.ContainPG;
import org.cougaar.glm.ldm.asset.ForUnitPG;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.asset.NewForUnitPG;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.PhysicalPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILAssetListener;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;

import org.cougaar.planning.ldm.measure.*;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewTask;

/**
 * Breaks up incoming tasks into aggregates that are no bigger than the largest carrier asset.
 * Carrier could be truck, ship, or plane.
 */
public class TransportExpanderPlugin extends UTILExpanderPluginAdapter implements UTILAssetListener {

  protected static final double CAP_FUDGE = 0.1;

  public void localSetup() {     
    super.localSetup();
    glmPrepHelper = new GLMPrepPhrase (logger);
    ldmProtoCache = getLDMService().getLDM();
  }

  /**
   * <pre>
   * The idea is to add subscriptions (via the filterCallback), and when 
   * they change, to have the callback react to the change, and tell 
   * the listener (many times the plugin) what to do.
   *
   * Override and call super to add new filters, or override 
   * createXXXCallback to change callback behaviour.
   * </pre>
   */
  public void setupFilters () {
    super.setupFilters ();
              
    addFilter (myAssetCallback = new UTILAssetCallback(this, logger));
  }

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is TRANSPORT, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasStratTrans = false;
      
    if (prepHelper.hasPrepNamed (task, "OFTYPE")) {
      Asset oftype = (Asset) prepHelper.getIndirectObject (task, "OFTYPE");
      String typeid = oftype.getTypeIdentificationPG().getTypeIdentification ();
      hasStratTrans = typeid.equals ("StrategicTransportation");
    }

    boolean hasTransport = task.getVerb().equals (Constants.Verb.TRANSPORT);
    if (!hasTransport)
      return false;

    if (!prepHelper.hasPrepNamed (task, Constants.Preposition.FROM)) {
      if (isDebugEnabled())
	debug (".interestingTask - ignoring TRANSPORT task " + task.getUID () + " that doesn't have a FROM prep.");
      return false;
    }
    if (!prepHelper.hasPrepNamed (task, Constants.Preposition.TO)) {
      if (isDebugEnabled())
	debug (".interestingTask - ignoring TRANSPORT task " + task.getUID () + " that doesn't have a TO prep.");
      return false;
    }

    boolean val = (!(task instanceof MPTask)) &&
      !prepHelper.hasPrepNamed (task, "VISHNU");

    if (isDebugEnabled() && val && (task.getDirectObject () instanceof AssetGroup))
      debug (".interested in task with asset group? " + task.getUID ());

    if (isDebugEnabled() && !val) {
      debug (".interestingTask - IGNORING      " + task.getUID ());
    }
    else if (isDebugEnabled()) {
      debug (".interestingTask - interested in " + task.getUID ());
    }

    if ((task.getDirectObject () instanceof AggregateAsset) &&
	(((AggregateAsset) task.getDirectObject ()).getQuantity () == 0)) {
      info (".interestingTask - NOTE - ignoring task " + task.getUID () +
	    " which has a zero quantity for aggregate asset " + task.getDirectObject());
      val = false;
    }
		
    return val;
  }

  public void processTasks (java.util.List tasks) {
      super.processTasks (getPrunedTaskList(tasks));
  }

    protected List getPrunedTaskList (List tasks) {
	java.util.List prunedTasks = new java.util.ArrayList(tasks.size());

	Collection removed = myInputTaskCallback.getSubscription().getRemovedCollection();

	for (Iterator iter = tasks.iterator(); iter.hasNext();){
	    Task task = (Task) iter.next();
	    if (removed.contains(task)) {
		if (isInfoEnabled()) {
		    info ("ignoring task on removed list " + task.getUID());
		}
	    }
	    else
		prunedTasks.add (task);
	}
	return prunedTasks;
    }

  /**
   * Breaks up task aggregates into truck sized chunks if bigger
   * than biggest truck.  This is defined by a parameter : maxTruckContainWeight
   *
   * @param task the task to expand
   * @return a vector with the exapnded subtask
   */
  public Vector getSubtasks(Task task){
    Vector subtasks = new Vector();
    ForUnitPG unitPG = getForUnitPG (task.getDirectObject());

    if (!didSetCapacity)
      resetCapacities ();

    try{
      if (prepHelper.hasPrepNamed (task, GLMTransConst.LOW_FIDELITY) &&
	  (!isSelfPropelled  (task, task.getDirectObject()) || 
	   (getAgentIdentifier().toString().indexOf("Packer") != -1))) {
	expandLowFi (task, subtasks);
      }
      else if (task.getDirectObject () instanceof AggregateAsset) {
	expandAggregates (task, subtasks);
	if (subtasks.isEmpty ())
	  error (".getSubtasks -- ERROR - after expanding aggregate asset of task " +
		 task + " got no subtasks.");
      } else if (task.getDirectObject () instanceof AssetGroup) {
	if (isDebugEnabled())
	  debug(".getSubtasks - got asset group for task " + task.getUID());
	Collection assets = assetHelper.expandAssetGroup ((AssetGroup)task.getDirectObject ());
	for (Iterator iter = assets.iterator (); iter.hasNext ();) {
	  Asset subasset = (Asset) iter.next();
	  //		  if (subasset instanceof AggregateAsset)
	  //			expandAggregates (task, subtasks);
	  //		  else
	  subtasks.add (makeTask (task, subasset, unitPG));
	}
	if (subtasks.isEmpty ())
	  error (".getSubtasks -- ERROR - after expanding AssetGroup of task " +
		 task + " got no subtasks.");
      } else
	subtasks.add(makeTask (task, task.getDirectObject (), unitPG));
	  
    }catch(Exception e){
      logger.error(e.getMessage(), e);
    }
    if (isDebugEnabled()) {
      String uids = "";
      for (Iterator iter = subtasks.iterator (); iter.hasNext (); )
	uids = uids + ((Task) iter.next ()).getUID () + " ";
	  
      debug (" created subtasks " + uids);
    }
	
    if (subtasks.isEmpty())
      error ("getSubtasks - tried to create subtasks for " +
	     task.getUID () + " but couldn't.");

    return subtasks;
  }

  protected ForUnitPG getForUnitPG (Asset asset) {
    return (ForUnitPG) asset.resolvePG(org.cougaar.glm.ldm.asset.ForUnitPG.class, 
				       Asset.UNSPECIFIED_TIME);
  }

  /**
   * gets the contribution each individual item makes to consuming
   * the capacity of the transport vehicle
   * Sub classes may want to override this
   * @param GLMAsset -- the asset of the direct object of the task
   * @return double of the contribution in TONS
   */
  public double getItemContribution( GLMAsset itemAsset)
  {
    double itemWeight = 0.01;
    try {
      itemWeight = itemAsset.getPackagePG().getPackMass ().getTons();
      if (itemWeight < 0.00001) {
	try {
	  itemWeight = itemAsset.getPhysicalPG().getMass ().getTons();
	} catch (Exception ee) {
	  if (!itemAsset.hasPersonPG ())
	    warn (".getItemContribution - WARNING - unable to determine " + 
		  itemAsset.getUID () + "'s weight.");
	}
      }
    } catch (Exception e) {
      try {
	itemWeight = itemAsset.getPhysicalPG().getMass ().getTons();
      } catch (Exception ee) {
	if (!itemAsset.hasPersonPG ())
	  warn (".getItemContribution - WARNING - unable to determine " + 
		itemAsset.getUID () + "'s weight.");
      }
    }
	
    return itemWeight;
  }

  /**
   * gets the quantity of items in this task
   * Sub classes may want to override this
   * @param AggregateAsset asset that has quantity
   * @return quantity in int
   */
  public int getAssetQuantity( AggregateAsset aggAsset) 
  {
    return (int) aggAsset.getQuantity ();
  }

  protected void expandLowFi (Task task, Vector subtasks) {
    if (task.getDirectObject () instanceof AggregateAsset) {
      expandAggregates (task, subtasks); // it's a person, or an aggregate of very small items
    }
    else {
      expandLowFiAsset (task, subtasks);
    }
  }

  /** 
   * <pre>
   * expands aggregate assets into truck-sized chunks, putting the 
   * subtasks into <code>subtasks</code> 
   *
   * Rounds the item wt to the same precision as vishnu uses so no disaggreements
   * about how much a truck can hold.
   * </pre>
   **/
  protected void expandAggregates (Task task, Vector subtasks) {
    AggregateAsset directObject = (AggregateAsset) task.getDirectObject();
    GLMAsset itemProto = (GLMAsset) directObject.getAsset();
    ForUnitPG unitPG = getForUnitPG(directObject);

    boolean isPerson = itemProto.hasPersonPG ();

    double itemContrib = getItemContribution(itemProto);

    if (isPerson)
      itemContrib = 1.0;
    double maxCap = (isPerson) ? maxPassengerCapacity : maxContainCapacity - CAP_FUDGE;
	
    // round item weight to same precision as Vishnu
    BigDecimal bigD = new BigDecimal (itemContrib);
    itemContrib = bigD.setScale (2,BigDecimal.ROUND_UP).doubleValue();

    int remainingQuantity = getAssetQuantity(directObject);
    double totalAggregateContrib = ((double)remainingQuantity)*itemContrib;
	  
    boolean mustExpand = (totalAggregateContrib > maxCap);
	  
    if (isDebugEnabled()) {
      debug (".expandAggregates - item contribution " + 
	    itemContrib + 
	    " quantity " + remainingQuantity + 
	    " total " + totalAggregateContrib +
	    " maxCapacity " + maxCap +
	    ((mustExpand) ? " must expand " : ""));
    }

    int numItemsOnTruck = (int) (maxCap/itemContrib);

    // sanity checking 
    if (numItemsOnTruck == 0) {
      warn (".expandAggregates - WARNING - max items on truck " 
	    + numItemsOnTruck +
	    " is ZERO.  Base asset of aggregate : " + itemProto + 
	    " is " + itemContrib + " tons, which is larger than" + 
	    " carrier asset's max capacity " + maxCap);
      numItemsOnTruck = 1;
    }

    if (remainingQuantity == 0)
      error (".expandAggregates - ERROR - aggregate asset quantity is zero for direct object " +
	     directObject + " of task " + task.getUID());

    double maxOnTruck = ((double)numItemsOnTruck)*itemContrib;

    if (isDebugEnabled())
      debug (".expandAggregates - max items on truck " 
	    + numItemsOnTruck +
	    " max num " + maxOnTruck);

	
    // keep making subtasks
    while (totalAggregateContrib > maxCap) {  
      Task newtask;
      Asset truckSizedSet = ldmf.createAggregate (itemProto, 
						  numItemsOnTruck);
      setItemPG (truckSizedSet, itemProto);
      subtasks.add (newtask = makeTask (task, truckSizedSet, unitPG));
      totalAggregateContrib -= maxOnTruck;
      remainingQuantity     -= numItemsOnTruck;

      if (isDebugEnabled())
	debug(".expandAggregates - cargo > truckload, " +
	     "Expanding with " + 
	     numItemsOnTruck + 
	     " boxes on truck, task id " + 
	     newtask.getUID() + 
	     " remaining quantity " + 
	     remainingQuantity + 
	     ", contribution " + totalAggregateContrib);
    }

    // if there's any left over, make one last task
    if (remainingQuantity > 0) {
      Asset truckSizedSet = 
	(mustExpand) ? 
	ldmf.createAggregate (itemProto, remainingQuantity) : 
	task.getDirectObject();
      setItemPG (truckSizedSet, itemProto);

      if (isDebugEnabled() && mustExpand)
	debug(".expandAggregates - cargo was > truckload, " + 
	     remainingQuantity + 
	     " items on truck, truckSizedSet had " +
	     ((AggregateAsset)truckSizedSet).getQuantity () 
	     + " items");
      subtasks.add (makeTask (task, truckSizedSet, unitPG));
    }
  }

  /** 
   * <pre>
   * expands aggregate assets into truck-sized chunks, putting the 
   * subtasks into <code>subtasks</code> 
   *
   * Rounds the item wt to the same precision as vishnu uses so no disaggreements
   * about how much a truck can hold.
   * </pre>
   **/
  protected void expandLowFiAsset (Task task, Vector subtasks) {
    if (isDebugEnabled())
      debug ("expanding low fi asset for task " + task.getUID());
    //    test();
    GLMAsset itemProto = (GLMAsset) task.getDirectObject();
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " original dimensions :" + 
	     " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " v " + itemProto.getPhysicalPG().getVolume ().getCubicFeet() + " ft^3");
    }

    ForUnitPG unitPG = getForUnitPG(itemProto);

    boolean isPerson = itemProto.hasPersonPG ();
    double totalContrib = getItemContribution(itemProto);
    boolean originalWasTiny = (totalContrib <= 0.00001);

    if (originalWasTiny) {
      warn (".expandLowFiAsset - item contribution " + totalContrib + 
	    " for task " + task.getUID() + 
	    "'s direct object " + itemProto + 
	    " is < 0.00001, was " + totalContrib);
    }

    if (isPerson)
      totalContrib = 1.0;
    double maxCap = (isPerson) ? maxPassengerCapacity : maxContainCapacity - CAP_FUDGE;
	
    // round item weight to same precision as Vishnu
    BigDecimal bigD = new BigDecimal (totalContrib);
    totalContrib = bigD.setScale (2,BigDecimal.ROUND_UP).doubleValue();

    boolean mustExpand = (totalContrib > maxCap);
    double itemContrib = (mustExpand) ? maxCap : totalContrib;
      
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset - item contribution " + itemContrib + 
	    " maxCapacity " + maxCap +
	    ((mustExpand) ? " must expand " : ""));
    }

    int numItemsOnTruck = 1;

    double maxOnTruck = maxCap;

    if (isDebugEnabled())
      debug (".expandLowFiAsset - max items on truck " 
	    + numItemsOnTruck +
	    " max num " + maxOnTruck);

    PhysicalPG originalPhysicalPG = itemProto.getPhysicalPG ();

    // keep making subtasks
    while (totalContrib > maxCap) {  
      Task newtask;

      if (isDebugEnabled()) {
	debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " before adjust dimensions :" + 
	       " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, ");
      }

      String itemID = getUniqueID (itemProto);

      Asset truckSizedAsset = 
	getTruckSizedAsset (itemProto, originalPhysicalPG, itemID, maxOnTruck);

      if (isDebugEnabled()) {
	debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " after adjust dimensions :" + 
	       " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, ");
      }

      subtasks.add (newtask = makeTask (task, truckSizedAsset, unitPG));
      totalContrib -= itemContrib;

      if (isDebugEnabled())
	debug(".expandLowFiAsset - cargo > truckload, " +
	     "Expanding with " + 
	     numItemsOnTruck + 
	     " items on truck, task id " + 
	     newtask.getUID() + 
	      ", contribution " + maxOnTruck);
    }

    // if there's any left over, make one last task
    if (totalContrib > 0.00001 || originalWasTiny) {
      Asset truckSizedAsset;
      if (mustExpand) {
	String itemID = getUniqueID (itemProto);

	truckSizedAsset = getTruckSizedAsset (itemProto, originalPhysicalPG, itemID, totalContrib);
      }
      else
	truckSizedAsset = task.getDirectObject();

      if (isDebugEnabled() && mustExpand)
	debug(".expandLowFiAsset - cargo was > truckload, " + 
	      " truckSizedAsset is " + totalContrib + " tons");
      subtasks.add (makeTask (task, truckSizedAsset, unitPG));
    }
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " after dimensions :" + 
	     " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " v " + itemProto.getPhysicalPG().getVolume ().getCubicFeet() + " ft^3");
    }
  }

  protected boolean isSelfPropelled (Task t, Asset directObject) {
    GLMAsset baseAsset = 
      (directObject instanceof AggregateAsset) ? 
      (GLMAsset) ((AggregateAsset)directObject).getAsset() : 
      (GLMAsset) directObject;
	
    MovabilityPG move_prop = baseAsset.getMovabilityPG();

    if (move_prop != null) {
      String cargocatcode = move_prop.getCargoCategoryCode();
      if (cargocatcode.charAt(0) == 'R') {
	if (isDebugEnabled())
	  debug (getName() + ".isSelfPropelled - found self-propelled vehicle on task " + t.getUID());
	return true;
      }
    }

    return false;
  }

  protected Asset getTruckSizedAsset (Asset itemProto, PhysicalPG originalPhysicalPG, String itemID, double contrib) {
    Asset truckSizedAsset = 
      assetHelper.createInstance (ldmProtoCache, GLMTransConst.LOW_FIDELITY_PROTOTYPE, itemID);
    
    LowFidelityAssetPG currentLowFiAssetPG = (LowFidelityAssetPG)
      itemProto.resolvePG (LowFidelityAssetPG.class);
    truckSizedAsset.addOtherPropertyGroup(currentLowFiAssetPG);// now subobject has pointer back to parent

    NewMovabilityPG movabilityPG = (NewMovabilityPG)ldmf.createPropertyGroup(MovabilityPG.class);
    ((GLMAsset)truckSizedAsset).setMovabilityPG(movabilityPG);
    movabilityPG.setCargoCategoryCode(currentLowFiAssetPG.getCCCDim().getCargoCatCode());

    adjustDimensions ((GLMAsset)truckSizedAsset, originalPhysicalPG, contrib);

    return truckSizedAsset;
  }

  void test () {
    GLMAsset original = (GLMAsset) ldmf.createInstance (GLMTransConst.LOW_FIDELITY_PROTOTYPE, "low_original");

    debug (".test original " + original.getUID () + " - " + original + 
	   " mass before " + original.getPhysicalPG().getMass().getKilograms());

    GLMAsset child = (GLMAsset) ldmf.createInstance (original, "child");

    debug (".test child " + child.getUID () + " - " + child + 
	   " mass before " + child.getPhysicalPG().getMass().getKilograms());

    if (original.getPhysicalPG () == child.getPhysicalPG ()) {
      debug (".test pgs are the same(EXPECTED)");
    }
    else {
      debug (".test pgs are different");
    }

    debug (".test original pg " + original.getPhysicalPG ().getClass());
    NewPhysicalPG newPhysicalPG = PropertyGroupFactory.newPhysicalPG ();
    debug (".test " + newPhysicalPG.getClass());
    newPhysicalPG.setMass  (new Mass (666,   Mass.KILOGRAMS ));
    child.setPhysicalPG (newPhysicalPG);

    if (original.getPhysicalPG () == child.getPhysicalPG ()) {
      debug (".test pgs are the same (WEIRD) !");
    }
    else {
      debug (".test pgs are different (EXPECTED)");
    }

    debug (".test child " + child.getUID () + " - " + child + 
	   " mass after " + child.getPhysicalPG().getMass().getKilograms());

    debug (".test original " + original.getUID () + " - " + original + 
	   " mass after " + original.getPhysicalPG().getMass().getKilograms());
  }

  protected void setItemPG (Asset newAsset, Asset boxProto) {
    ItemIdentificationPG itemPG = PropertyGroupFactory.newItemIdentificationPG ();
    newAsset.setItemIdentificationPG (itemPG);
    String name = getUniqueID(boxProto);
    if (isDebugEnabled())
      debug(".setItemPG - item name " + name);

    ((NewItemIdentificationPG) itemPG).setItemIdentification (name);
    ((NewItemIdentificationPG) itemPG).setNomenclature   (name);
  }

  int uniqueID = 0;

  protected String getUniqueID (Asset boxProto) {
    return boxProto.getTypeIdentificationPG().getTypeIdentification() + "_" + 
      getAgentIdentifier() + "_" + uniqueID++;
  }
  
  protected void adjustDimensions (GLMAsset asset, PhysicalPG originalPhysicalPG, double tons) {
    if (isDebugEnabled())
      debug (".adjustDimensions called on "+ asset.getUID ());

    double aggregateTons  = originalPhysicalPG.getMass().getTons();

    double ratio = tons/aggregateTons;

    double area   = originalPhysicalPG.getFootprintArea().getSquareMeters();
    double volume = originalPhysicalPG.getVolume().getCubicMeters();
    double mass   = originalPhysicalPG.getMass().getKilograms();

    NewPhysicalPG newPhysicalPG = PropertyGroupFactory.newPhysicalPG ();
    newPhysicalPG.setMass  (new Mass     (mass*ratio,   Mass.KILOGRAMS ));

    newPhysicalPG.setFootprintArea(new Area (area*ratio,   Area.SQUARE_METERS));
    newPhysicalPG.setVolume(new Volume   (volume*ratio,   Volume.CUBIC_METERS));

    if (isDebugEnabled()) {
      debug (".adjustDimensions chunk "+ asset.getUID () + " original dimensions :" + 
	     " m " + asset.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " v " + asset.getPhysicalPG().getVolume ().getCubicFeet() + " ft^3");
    }

    asset.setPhysicalPG (newPhysicalPG);

    if (isDebugEnabled()) {
      debug (".adjustDimensions chunk "+ asset.getUID () + " dimensions :" + 
	     " m " + asset.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " v " + asset.getPhysicalPG().getVolume ().getCubicFeet() + " ft^3");
    }
  }

  /** 
   * Makes subtask of parent task, with given direct object.
   *
   * removes OFTYPE prep, since it's not needed by scheduler 
   **/
  Task makeTask (Task parentTask, Asset directObject, ForUnitPG unitPG) {
    Task newtask = expandHelper.makeSubTask (ldmf,
					     parentTask,
					     directObject,
					     getAgentIdentifier());
    glmPrepHelper.removePrepNamed(newtask, Constants.Preposition.OFTYPE);

    if (unitPG == null) {
      // Next four lines create a Property Group for unit and attach it to all assets attached to task
      unitPG = (ForUnitPG)ldmf.createPropertyGroup(ForUnitPG.class);
      if (!glmPrepHelper.hasPrepNamed (newtask,Constants.Preposition.FOR)) {
	String owner = directObject.getUID().getOwner();

	if (isWarnEnabled()) {
	  warn (".makeTask - WARNING : got task " + parentTask.getUID() + 
		" which has no FOR unit prep, using owner - " + owner + ".");
	}
	
	((NewForUnitPG)unitPG).setUnit(owner);
      } else {
	((NewForUnitPG)unitPG).setUnit((String)glmPrepHelper.getIndirectObject(newtask,Constants.Preposition.FOR));
	glmPrepHelper.removePrepNamed(newtask, Constants.Preposition.FOR);
      }
    }
	
    attachPG(directObject, unitPG);

    return newtask;
  }

  /** 
   * Since FOR preps are lost a custom property is added to determine unit
   */
  public void attachPG(Asset asset, PropertyGroup thisPG) {
    if (asset instanceof AssetGroup) {
      Vector assetList = ((AssetGroup)asset).getAssets();
      for (int i = 0; i < assetList.size(); i++) {
	attachPG((Asset)assetList.elementAt(i), thisPG);
      }
    } else if (asset instanceof AggregateAsset) {
      // Put in both because unsure of behavior
      asset.addOtherPropertyGroup(thisPG);
      // Don't want to do this, since every aggregate of X
      //XX asset will then have this pg
      //	    attachUnitPG(((AggregateAsset)asset).getAsset(),unitPG);
    } else {
      asset.addOtherPropertyGroup(thisPG);
    }
  }


  /**
   * <pre>
   * Implemented for UTILAssetListener
   *
   * OVERRIDE to see which assets you
   * think are interesting
   * </pre>
   * @param a asset to check for notification
   * @return boolean true if asset is interesting
   */
  public boolean interestingAsset(Asset a) {
    if (!(a instanceof GLMAsset)) {
      if (isDebugEnabled())
	debug (".interestingAsset - ignoring asset " 
	       + a + " because it's not an GLMAsset.");
      return false;
    }
    
    boolean val = ((GLMAsset)a).hasContainPG ();
    if (isDebugEnabled() && !val)
      debug (".interestingAsset - ignoring GLMAsset " 
	    + a + " because it's missing a Contain PG.");
    if (isDebugEnabled() && val)
      debug (".interestingAsset - interested in asset " 
	     + a);
    return val;
 

  }

  public void handleChangedAssets(Enumeration newAssets) {}
  
  public void resetCapacities () {
    Collection carriers = myAssetCallback.getSubscription().getCollection();
    handleNewAssets(Collections.enumeration (carriers));
    if (isWarnEnabled()) 
      warn (getName() + ".getSubtasks - recalculated maxContainContrib after rehydration.");

    if (maxContainCapacity == Double.MAX_VALUE) {
      error (getName() + ".getSubtasks - maxContainCapacity has not been set, it's " + 
	     maxContainCapacity);
    }
  }

  /**
   * <pre>
   * Place to handle new assets.
   *
   * Looks for least common denominator
   *
   * </pre>
   * @param newAssets new assets found in the container
   */
  public void handleNewAssets(Enumeration newAssets) {
    if (isDebugEnabled())
      debug (".handleNewAssets - called.");

    // Look through the assets, and find the least common denominator
    for (; newAssets.hasMoreElements (); ){
      Object asset = newAssets.nextElement ();
      if (asset instanceof GLMAsset) {
	GLMAsset alpasset = (GLMAsset) asset;
	if (alpasset.hasContainPG()) {
	  double [] maxContain = getAssetMaxContain(alpasset);
	  calculateCommonMaxContain(maxContain);
	  didSetCapacity = true;
	}
	else {
	  if (isDebugEnabled())
	    debug (".handleNewAssets - ignoring GLMAsset without contain pg - " + 
		   alpasset.getUID());
	}
      } 
      else {
	if (isDebugEnabled())
	  debug (".handleNewAssets - ignoring non GLMAsset - " + asset);
      }
    }
  }

  // 
  // Sub classes may need to derive this separately
  //
  public double [] getAssetMaxContain (GLMAsset glmasset) {
    double max [] = new double [2];
    max[0] = 1.0d;
    max[1] = 1.0d;
    if (glmasset.hasContainPG()) {
      try {
	max[0] = glmasset.getContainPG().getMaximumWeight().getTons();
      } catch (Exception e) {
	if (isDebugEnabled())
	  debug (".getAssetMaxContain : no contain pg for asset " + glmasset);
      }
	  
      max[1] = glmasset.getContainPG().getMaximumPassengers();
      if (isDebugEnabled())
	debug (".getAssetMaxContain : max weight - " + max[0] +
	      " max pax " + max[1]);
    }
    return max;
  }

  /** calculates the max contain -- smallest of all seen */
  protected void calculateCommonMaxContain (double [] maxcontain) {
    if (maxcontain[0] < maxContainCapacity)
      maxContainCapacity   = maxcontain[0];

    if (maxcontain[1] < maxPassengerCapacity)
      maxPassengerCapacity = maxcontain[1];
  }

  protected double maxContainCapacity   = Double.MAX_VALUE;
  protected double maxPassengerCapacity = Double.MAX_VALUE;
  protected boolean didSetCapacity = false;

  protected GLMPrepPhrase glmPrepHelper;
  protected LDMServesPlugin ldmProtoCache;
  protected UTILAssetCallback myAssetCallback;
}
