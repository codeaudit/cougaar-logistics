/*
 * <copyright>
 *  Copyright 2001-2 BBNT Solutions, LLC
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

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Vector;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Workflow;

import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.packer.Geolocs;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.ContentsPG;
import org.cougaar.glm.ldm.asset.NewContentsPG;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.planning.ldm.measure.*;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.asset.PhysicalAsset;

/**
 * Takes PROJECT_SUPPLY tasks 
 * and converts them into TRANSPORT tasks with a RESERVATION prep. <p>
 *
 */
public class AmmoProjectionExpanderPlugin extends AmmoLowFidelityExpanderPlugin {
  public long CHUNK_DAYS = 30;
  public static long MILLIS_PER_DAY = 1000*60*60*24;
  public static long SECS_PER_DAY = 60*60*24;
  public static final String AMMO_CATEGORY_CODE = "MBB";
  public static final String MILVAN_NSN = "NSN/8115001682275";
  public static final double PACKING_LIMIT = 13.9; /* short tons */
  private static Asset MILVAN_PROTOTYPE = null;
  private static final String UNKNOWN = "unknown";

  public void localSetup () {
    super.localSetup ();
    
    try {
      if (getMyParams ().hasParam ("CHUNK_DAYS"))
	CHUNK_DAYS=getMyParams().getLongParam ("CHUNK_DAYS");
    } catch (Exception e) { if (isWarnEnabled()) { warn ("got unexpected exception " + e); } }
  }

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is SUPPLY, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasSupply = task.getVerb().equals (Constants.Verb.PROJECTSUPPLY);
    if (!hasSupply)
      return false;

    if (isDebugEnabled())
      debug (".interestingTask - processing PROJECT_SUPPLY task " + task.getUID ());

    return true;
  }

  int total = 0;

  /**
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up tasks into constituent parts.
   */
  public Vector getSubtasks(Task parentTask) {
    
    Vector childTasks = new Vector ();

    // create the d.o.

    Asset supplyAsset = parentTask.getDirectObject();

    Preference pref = prefHelper.getPrefWithAspectType (parentTask, AlpineAspectType.DEMANDRATE);
    double ratePerSec = prefHelper.getPreferenceBestValue (pref); // base number is in quantity per second!
    double ratePerDay = ratePerSec*SECS_PER_DAY; 

    Date readyAt = prefHelper.getReadyAt   (parentTask);
    Date early   = getEarlyDate (parentTask);
    Date best    = prefHelper.getBestDate  (parentTask);
    Date late    = getLateDate  (parentTask);

    if (early.getTime () < readyAt.getTime ())
      early = readyAt;
    if (best.getTime () < readyAt.getTime ())
      best = readyAt;

    long window = best.getTime () - readyAt.getTime();
    long originalWindowInDays = window/MILLIS_PER_DAY;
    long numSubtasks = window/(CHUNK_DAYS*MILLIS_PER_DAY);
    if (window - (numSubtasks * CHUNK_DAYS*MILLIS_PER_DAY) != 0)
      numSubtasks++;

    if (numSubtasks < 1) {
      error (getName () + ".getSubtasks - task " + parentTask.getUID () + 
	     " will create no subtasks?  Window was " + originalWindowInDays + " days");
    }

    if (isInfoEnabled ()) {
      info (getName () + ".getSubtasks - task " + parentTask.getUID () + 
	     " from " + readyAt + 
	     " to " + best + 
	     " will produce " + numSubtasks + " subtasks.");
    }
    
    String unit = (prepHelper.hasPrepNamed (parentTask, Constants.Preposition.FOR)) ?
      (String) prepHelper.getIndirectObject (parentTask, Constants.Preposition.FOR) : "null";

    // create one subtask for every chunk set of days, with an asset that is the total
    // delivered over the period = days*ratePerDay
    double daysSoFar = 0;
    int totalQuantity = 0;
    int targetQuantity = (int) (((double) (window/1000l))*ratePerSec);
    Date lastBestDate = readyAt;

    if (isInfoEnabled ()) {
      info (getName () + ".getSubtasks - task " + parentTask.getUID () + " target quantity " + targetQuantity + 
	    " windowInSec " + window/1000l + " rate/sec " + ratePerSec);
    }

    for (int i = 0; i < (int) numSubtasks; i++) {
      boolean onLastTask = (window/MILLIS_PER_DAY) < CHUNK_DAYS;
      double daysToChunk = (onLastTask) ? window/MILLIS_PER_DAY : CHUNK_DAYS;

      if (isInfoEnabled () && onLastTask)
	info ("on last task - days " + daysToChunk + " since " + window/MILLIS_PER_DAY + " < " + CHUNK_DAYS);

      daysSoFar += daysToChunk;
      window    -= ((long)daysToChunk)*MILLIS_PER_DAY;
      int quantity = (int) (daysToChunk * ratePerDay);
      if (onLastTask && ((totalQuantity + quantity) != targetQuantity)) {
	if (isInfoEnabled ())
	  info (" task " + parentTask.getUID () + 
		" adjusting quantity from " +quantity + 
		" to " + (targetQuantity - totalQuantity) + 
		" total is " + totalQuantity);
	quantity = targetQuantity - totalQuantity;
      }
      else if (isInfoEnabled ())
	info (".getSubtasks - task " + parentTask.getUID () + " quantity is " + quantity + 
	      " chunk days " + daysToChunk+ " rate " + ratePerDay);
      if (quantity < 1) {
	error (".getSubtasks - task " + parentTask.getUID () + 
	       " gets a quantity of zero, ratePerDay was " + ratePerDay + 
	       " chunk days " +daysToChunk);
      }
      
      double massInKGs = ((GLMAsset)supplyAsset).getPhysicalPG().getMass().getKilograms()*quantity; 
      //      AggregateAsset deliveredAsset = createDeliveredAsset (parentTask, supplyAsset, quantity);
      totalQuantity += quantity; //deliveredAsset.getQuantity ();

      //      if (deliveredAsset.getQuantity () != quantity) {
      //	error (".getSubtasks - task " + parentTask.getUID () + " quantities don't match");
      //      }

      // set item id pg to show it's a reservation, and not a normal task's asset

      ItemIdentificationPG itemIDPG = (ItemIdentificationPG) supplyAsset.getItemIdentificationPG();
      //      NewItemIdentificationPG newItemIDPG = (NewItemIdentificationPG) PropertyGroupFactory.newItemIdentificationPG();
      String itemID = itemIDPG.getItemIdentification();
      String itemNomen = itemIDPG.getNomenclature ();

      if (itemID == null) {
	TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
	itemID = typeID.getTypeIdentification();
      }

      if (itemNomen == null) {
	TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
	itemNomen = typeID.getNomenclature();
      }

      //      newItemIDPG.setItemIdentification (itemID    + "_Reservation_" + total);
      //      newItemIDPG.setNomenclature       (itemNomen + "_Reservation_" + (total++));
      //      deliveredAsset.setItemIdentificationPG (newItemIDPG);
    
      GLMAsset milvan = makeMilvan ();
      addContentsInfo (milvan, itemNomen, itemID, unit, massInKGs);

      Task subTask = makeTask (parentTask, milvan);//deliveredAsset);
      long bestTime = early.getTime() + ((long)daysSoFar)*MILLIS_PER_DAY;
      if (bestTime > best.getTime()) {
	if (isInfoEnabled())
	  info (getName () + 
		".getSubtasks - had to correct bestTime, was " + new Date (bestTime) + 
		" now " + best);
	bestTime = best.getTime();
      }

      if (isDebugEnabled ())
	debug (getName () + ".getSubtasks - making task " + subTask.getUID() + 
	       " with best arrival " + new Date(bestTime));

      prefHelper.replacePreference((NewTask)subTask, 
				   prefHelper.makeEndDatePreference (ldmf,
								     early, 
								     new Date (bestTime),
								     late));
      prefHelper.removePrefWithAspectType (subTask, AlpineAspectType.DEMANDRATE); // we've included it in the d.o.

      prepHelper.removePrepNamed (subTask, Constants.Preposition.MAINTAINING);
      prepHelper.removePrepNamed (subTask, Constants.Preposition.REFILL);
      prepHelper.addPrepToTask (subTask, prepHelper.makePrepositionalPhrase (ldmf, "Start", lastBestDate));

      if (isInfoEnabled())
	info (getName () + " publishing reservation " + subTask.getUID() + 
	      " for " + itemNomen + 
	      " from " + lastBestDate + " to " + new Date(bestTime) + " weight " + massInKGs + " kgs.");

      lastBestDate = new Date (bestTime);
      childTasks.addElement (subTask);
    }

    // post condition
    if (totalQuantity != targetQuantity) {
      if (isWarnEnabled ())
	warn (getName () + " total quantity " + totalQuantity + 
	      " != original total " + targetQuantity +
	      " = window " + originalWindowInDays + 
	      " * ratePerDay " + ratePerDay);
    }

    if (isInfoEnabled())
      info (getName () + " returning " + childTasks.size() + " subtasks for " + parentTask.getUID());

    return childTasks;
  }

  public Date getEarlyDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeStartPoint ()).getAspectValue ().longValue ());
  }
  public Date getLateDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeEndPoint ()).getAspectValue ().longValue ());
  }

  protected Enumeration getValidEndDateRanges (Preference endDatePref) {
    Calendar cal = java.util.Calendar.getInstance();
    cal.set(2200, 0, 0, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date endOfRange = (Date) cal.getTime();

    Enumeration validRanges = 
      endDatePref.getScoringFunction().getValidRanges (new TimeAspectValue (AspectType.END_TIME,
									    0l),
						       new TimeAspectValue (AspectType.END_TIME,
									    endOfRange));
    return validRanges;
  }

  /** create aggregate asset aggregating the direct object's prototype **/
  protected AggregateAsset createDeliveredAsset (Task originalTask, Asset originalAsset, int quantity) {
    Asset prototype = originalAsset.getPrototype ();

    if (prototype == null) {
      prototype = originalAsset;
      GLMAsset glmProto = (GLMAsset)prototype;
      if (!glmProto.hasPhysicalPG()) {
	warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " +
	      prototype + " doesn't have a physical PG - " + glmProto.getPhysicalPG());
	if (!(prototype instanceof PhysicalAsset))
	  error ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + 
		 prototype + " is not a physical asset?.");
	else if (isInfoEnabled()){
	  info ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		" is a physical asset.");
	}
      }
      else { 
	if (glmProto.getPhysicalPG().getFootprintArea() == null) {
	  ((NewPhysicalPG)glmProto.getPhysicalPG()).setFootprintArea (new Area (Area.SQUARE_FEET, 1)); 
	  if (isWarnEnabled()) {
	    warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		  " doesn't have an area slot on its physical pg.");
	  }
	}
	if (glmProto.getPhysicalPG().getVolume() == null) {
	  ((NewPhysicalPG)glmProto.getPhysicalPG()).setVolume (new Volume (Volume.CUBIC_FEET, 1)); 
	  if (isWarnEnabled()) {
	    warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		  " doesn't have a volume slot on its physical pg.");
	  }
	}
      }
      if (!glmProto.hasPackagePG()) {
	warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " + 
	      prototype + " doesn't have a package PG.");
      }
    }

    AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(prototype, quantity);

    return deliveredAsset;
  }

  public void handleTask(Task t) {
    wantConfidence = true;
    super.handleTask(t);
    Preference pref = prefHelper.getPrefWithAspectType (t, AlpineAspectType.DEMANDRATE);
    double ratePerSec = prefHelper.getPreferenceBestValue (pref);
    if (isInfoEnabled ()) 
      info (getName () + ".handleTask - task " + t.getUID() + " had p.e. " + t.getPlanElement().getUID());
    if (t.getPlanElement () instanceof Expansion) {
      addToEstimatedAR (t.getPlanElement (), ratePerSec);
      if (isInfoEnabled()) {
	Workflow tasksWorkflow = t.getWorkflow();
	Workflow peWorkflow = ((Expansion)t.getPlanElement()).getWorkflow();
	info (getName () + ".handleTask " + t.getUID() + " in " + ((tasksWorkflow != null) ? tasksWorkflow.getUID().toString() : "null wf?")+ 
	      " p.e. " +t.getPlanElement ().getUID() + " p.e. wf. " + ((peWorkflow != null) ? peWorkflow.getUID().toString() : " null p.e. wf?"));
      }
    } 
    else if (isWarnEnabled ()) 
      warn (getName () + ".handleTask - task " + t.getUID() + " had no p.e.???");
  }

  protected void addToEstimatedAR (PlanElement exp, double rate) {
    AllocationResult estAR = exp.getEstimatedResult ();
    AspectValue [] aspectValues = estAR.getAspectValueResults();
    AspectValue [] copy = new AspectValue [aspectValues.length+1];
    System.arraycopy (aspectValues, 0, copy, 0, aspectValues.length);
    copy[aspectValues.length] = new AspectValue (AlpineAspectType.DEMANDRATE, rate);

    AllocationResult replacement =  
      ldmf.newAVAllocationResult(UTILAllocate.MEDIUM_CONFIDENCE, true, copy);
    exp.setEstimatedResult (replacement);
  }

  /**
   * Report to superior that the expansion has changed. Usually just a pass
   * through to the UTILPluginAdapter's updateAllocationResult.
   *
   * @param exp Expansion that has changed.
   * @see UTILPluginAdapter#updateAllocationResult
   */
  public void reportChangedExpansion(Expansion cpe) { 
    if (isDebugEnabled ())
      debug (getName () + " : Received changed pe " + 
	    cpe.getUID () + " for task " + 
	    cpe.getTask ().getUID());
    AllocationResult reportedresult = cpe.getReportedResult();
    if (reportedresult != null) {
      // compare entire allocationresults.
      AllocationResult estimatedresult = cpe.getEstimatedResult();
      double confidence = reportedresult.getConfidenceRating ();
      boolean nullEstimated  = (estimatedresult == null);
      // if we are not ignoring low confidence reported values
      boolean highConfidence = (!skipLowConfidence || confidence > HIGH_CONFIDENCE);

      if ( nullEstimated  || 
	   (highConfidence &&
	    (! isEqual(estimatedresult, reportedresult)))) { 
	if (isDebugEnabled ())
          debug (getName () + " : Swapping Alloc Results for task " + 
                              cpe.getTask ().getUID ());
        if (isWarnEnabled() && !reportedresult.isSuccess ())
          warn (getName () + " : " + 
		cpe.getTask ().getUID () + " failed to allocate.");

        cpe.setEstimatedResult(reportedresult);

        double prefValue = 
          cpe.getTask().getPreference(AlpineAspectType.DEMANDRATE).getScoringFunction().getBest().getAspectValue().getValue();

        AspectValue[] aspectValues = cpe.getEstimatedResult().getAspectValueResults();

	AspectValue [] copy = new AspectValue [aspectValues.length+1];
	System.arraycopy (aspectValues, 0, copy, 0, aspectValues.length);
	copy[aspectValues.length] = new AspectValue (AlpineAspectType.DEMANDRATE, prefValue);
	AllocationResult correctedAR = 
	  new AllocationResult(reportedresult.getConfidenceRating(),
			       reportedresult.isSuccess(),
			       copy);

	cpe.setEstimatedResult(correctedAR);          

        if (isInfoEnabled()) 
          info (getName () + " : publish changing task " + cpe.getTask ().getUID ());

	blackboard.publishChange(cpe);
      }
    }
    else if (!cpe.getTask().getSource ().equals (((PluginBindingSite)getBindingSite()).getAgentIdentifier())) {
      error ("ERROR! " + getName () + 
	     " : "     + cpe.getTask ().getUID () + 
	     " has a null reported allocation.");
    }
  }

  /** checks to see if the AllocationResult is equal to this one.
     * @param anAllocationResult
     * @return boolean
     */
  public boolean isEqual(AllocationResult thisAR, AllocationResult that) {
    if (thisAR == that) return true; // quick success
    if (that == null) return false; // quick fail
    if (!(thisAR.isSuccess() == that.isSuccess() &&
          thisAR.isPhased() == that.isPhased() &&
          thisAR.getConfidenceRating() == that.getConfidenceRating())) {
      if (isInfoEnabled())
	info ("AspectValues - success/phased/confidence this AR " + thisAR + " != " + that);
      return false;
    }
       
    //check the real stuff now!
    //check the aspect types
    //check the summary results
    synchronized (thisAR.getAspectValueResults()) {
      if (!nearlyEquals(thisAR.getAspectValueResults(), that.getAspectValueResults())) {
	if (isDebugEnabled())
	  debug ("AspectValues - this AR " + thisAR + " != " + that);
	return false;
      }
      // check the phased results
      if (thisAR.isPhased()) {
        Iterator i1 = that.getPhasedAspectValueResults().iterator();
        Iterator i2 = thisAR.getPhasedAspectValueResults().iterator();
        while (i1.hasNext()) {
          if (!i2.hasNext()) return false;
          if (!nearlyEquals((AspectValue[]) i1.next(), (AspectValue[]) i2.next())) {
	    if (isDebugEnabled())
	      debug ("phased AspectValues - this AR " + thisAR + " != " + that);
	    return false;
	  }
        }
        if (i2.hasNext()) return false;
      }
    }

    // check the aux queries
    
    /*
    String[] taux = that.auxqueries;
    if (auxqueries != taux) {
      if (!Arrays.equals(taux, auxqueries)) return false;
    }
    */

    // must be equals...
    return true;
  }

  public boolean nearlyEquals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    // if (len != avs2.length) return false; // Can't be equal if different length
  outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
      if (type1 == AlpineAspectType.DEMANDRATE)
	continue; // ignore DEMAND RATE!
    inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.nearlyEquals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

  /**
   * An ancillary method that creates an asset that represents a MILVAN 
   * (military container) carrying ammunition
   */
  protected GLMAsset makeMilvan() {
    
    if (MILVAN_PROTOTYPE == null) {
      MILVAN_PROTOTYPE = getLDMService().getLDM().getPrototype(MILVAN_NSN);
      
      if (MILVAN_PROTOTYPE == null) {
        error("AmmoTransport: Error! Unable to get prototype for" +
	      " milvan NSN -" + MILVAN_NSN);
        return null;
      }
    }
    
    String itemID = makeMilvanID();
    Container milvan = 
      //      (Container)assetHelper.createInstance(getLDMService().getLDM(), MILVAN_PROTOTYPE, itemID);
      (Container)getLDMService().getLDM().getFactory().createInstance(MILVAN_PROTOTYPE, itemID);
    
    // AMMO Cargo Code
    NewMovabilityPG movabilityPG = 
      PropertyGroupFactory.newMovabilityPG(milvan.getMovabilityPG());
    movabilityPG.setCargoCategoryCode(AMMO_CATEGORY_CODE);
    milvan.setMovabilityPG(movabilityPG);
    
    // Milvan Contents
    NewContentsPG contentsPG = 
      PropertyGroupFactory.newContentsPG();
    milvan.setContentsPG(contentsPG);
    
    // Unique Item Identification
    NewItemIdentificationPG itemIdentificationPG = 
      (NewItemIdentificationPG)milvan.getItemIdentificationPG();
    //    String itemID = makeMilvanID();
    itemIdentificationPG.setItemIdentification(itemID); // redundant?
    itemIdentificationPG.setNomenclature("Milvan");
    itemIdentificationPG.setAlternateItemIdentification(itemID);
    milvan.setItemIdentificationPG(itemIdentificationPG);

    return milvan;
  }
  
  protected String makeMilvanID() {
    return new String(((PluginBindingSite)getBindingSite()).getAgentIdentifier() +
                      ":Reserved_Milvan" + getCounter());
  }
  
  private static int COUNTER = 0;

  private static synchronized long getCounter() {
    return COUNTER++;
  }

  protected void addContentsInfo(GLMAsset container, String nomen, String typeID, String unit, double massInKGs) {
    List typeIDs = new ArrayList();
    List nomenclatures = new ArrayList();
    List weights = new ArrayList();
    List receivers = new ArrayList();

    /*    for (Iterator iterator = agglist.iterator(); iterator.hasNext();) {
         Task task = (Task) iterator.next();
          TypeIdentificationPG typeIdentificationPG = 
            toAdd.getTypeIdentificationPG();
      String typeID;
      String nomenclature;
      if (typeIdentificationPG != null) {
        typeID = typeIdentificationPG.getTypeIdentification();
        if ((typeID == null) || (typeID.equals(""))) {
          typeID = UNKNOWN;
        } 
        
        nomenclature = typeIdentificationPG.getNomenclature();
        if ((nomenclature == null) || (nomenclature.equals(""))) {
          nomenclature = UNKNOWN;
        }
      } else {
        typeID = UNKNOWN;
        nomenclature = UNKNOWN;
      }
    */
      typeIDs.add(typeID);
      nomenclatures.add(nomen);
      
      Mass mass = Mass.newMass(massInKGs, Mass.KILOGRAMS); 
      weights.add(mass);
      
      /*
      Object receiver = 
        task.getPrepositionalPhrase(Constants.Preposition.FOR);
      String receiverID;
        
      // Add field with recipient
      if ((receiver == null) || !(receiver instanceof Asset)) {
        receiverID = UNKNOWN;
      } else {
        ItemIdentificationPG itemIdentificationPG = 
          ((Asset) receiver).getItemIdentificationPG();
        if ((itemIdentificationPG == null) ||
            (itemIdentificationPG.getItemIdentification() == null) ||
            (itemIdentificationPG.getItemIdentification().equals(""))) {
          receiverID = UNKNOWN;
        } else {
          receiverID = itemIdentificationPG.getItemIdentification();
        }
      }
      receivers.add(receiverID);
      */
      receivers.add(unit);
      //    }
    
    // Contents
    NewContentsPG contentsPG = 
      PropertyGroupFactory.newContentsPG();
    contentsPG.setNomenclatures(nomenclatures);
    contentsPG.setTypeIdentifications(typeIDs);
    contentsPG.setWeights(weights);
    contentsPG.setReceivers(receivers);
    container.setContentsPG(contentsPG);
  }
}
