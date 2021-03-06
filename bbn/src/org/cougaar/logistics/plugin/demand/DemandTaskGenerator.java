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

package org.cougaar.logistics.plugin.demand;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.util.Random;


/**
 * The DemandTaskGenerator generates Supply tasks based on the based in ProjectSupply tasks.
 *
 * @see DemandGeneratorPlugin
 * @see DemandGeneratorModule
 */

public class DemandTaskGenerator extends DemandGeneratorModule
        implements DemandTaskGeneratorIfc {


    protected HashMap projHash;
    protected HashMap supplyHash;

    protected Random poissonGen;

    public DemandTaskGenerator(DemandGeneratorPlugin demandGeneratorPlugin) {
        super(demandGeneratorPlugin);
        projHash = new HashMap();
        supplyHash = new HashMap();
        poissonGen = new Random();
    }

    /**
     * Generate actual demand tasks from the passed in projections
     */

    public List generateDemandTasks(long start, long duration, Collection relevantProjectSupplys, Collection relevantSupplys) {
        ArrayList demandTasks = new ArrayList();
        regenerateProjectionHash(relevantProjectSupplys);
        regenerateSupplyHash(relevantSupplys);
        long end = start + duration;
        Iterator gpTasksIt = projHash.keySet().iterator();
        while (gpTasksIt.hasNext()) {
            Task gpTask = (Task) gpTasksIt.next();
            ArrayList supplyTasks = new ArrayList();
            HashMap assetMap = (HashMap) projHash.get(gpTask);
            Iterator assetsIt = assetMap.keySet().iterator();
            while (assetsIt.hasNext()) {
                Asset consumed = (Asset) assetsIt.next();
                Collection projTasks = (Collection) (assetMap.get(consumed));

                double totalQty = deriveTotalQty(start, start + duration, projTasks);
                if (totalQty <= 0.0) {
                    continue;
                }
                double taskQty = 0.0;
                if (dgPlugin.getPoissonOn()) {
                    taskQty = poissonGen.nextPoisson(totalQty);
                } else {
                    taskQty = totalQty;
                }


                Iterator projTaskIt = projTasks.iterator();

                if ((taskQty > 0) && (projTaskIt.hasNext())) {
                    Task projTask = (Task) projTaskIt.next();

                    double capacity = dgPlugin.getCapacityForTask(projTask);
                    if (capacity > 0.0d) {
                        /** TODO eventually remove MWD
                         if(dgPlugin.getPoissonOn()  && taskQty > capacity) {
                         logger.warn("Random generations at " + dgPlugin.getOrgName() + " produced a qty of " + taskQty +
                         " and totalQty " + totalQty + "also greater than capacity at " + capacity);
                         }
                         **/
                        taskQty = Math.min(taskQty, capacity);
                    }

                    HashMap supplyAssetMap = (HashMap) supplyHash.get(gpTask);
                    Collection issuedSupplyTasks = null;
                    if (supplyAssetMap != null) {
                        issuedSupplyTasks = (Collection) supplyAssetMap.get(consumed);
                    }
                    //If a supply task hasn't already been created for this day,asset
                    //then generate the demand task
                    if ((taskQty > 0) &&
                            ((issuedSupplyTasks == null) ||
                            (issuedSupplyTasks.isEmpty()))) {
                        supplyTasks.add(createNewDemandTask(gpTask,
                                projTask,
                                consumed,
                                start,
                                start + duration,
                                taskQty));
                    }
                }
            }
            if (!supplyTasks.isEmpty()) {
                if (addToAndPublishExpansion(gpTask, supplyTasks)) {
                    demandTasks.addAll(supplyTasks);
                }
            }
        }

        return demandTasks;

    }

    protected void regenerateProjectionHash(Collection tasks) {
        regenerateTaskHash(tasks, projHash);
    }

    protected void regenerateSupplyHash(Collection tasks) {
        regenerateTaskHash(tasks, supplyHash);
    }

    protected void regenerateTaskHash(Collection tasks, HashMap hash) {
        hash.clear();
        Iterator taskIt = tasks.iterator();
        while (taskIt.hasNext()) {
            Task task = (Task) taskIt.next();
            Workflow wf = task.getWorkflow();
            if (wf == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("" + task.getVerb() + " task: " + task.getUID() + " at " + dgPlugin.getOrgName() + " has Null workflow");
                }
                continue;
            }
            Task parTask = wf.getParentTask();
            if (parTask == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("" + task.getVerb() + " task with no parent");
                }
                continue;
            }
            if (!(parTask.getVerb().equals
                    (Constants.Verb.GENERATEPROJECTIONS))) {
                if (logger.isErrorEnabled()) {
                    logger.error("" + task.getVerb() + " task with non generate projections parent");
                }
                continue;
            }
            HashMap assetMap = (HashMap) hash.get(parTask);
            if (assetMap == null) {
                assetMap = new HashMap();
                hash.put(parTask, assetMap);
            }
            Asset asset = task.getDirectObject();
            ArrayList tasksWAsset = (ArrayList) assetMap.get(asset);
            if (tasksWAsset == null) {
                tasksWAsset = new ArrayList();
                assetMap.put(asset, tasksWAsset);
            }
            tasksWAsset.add(task);
        }
    }

    protected double deriveTotalQty(long bucketStart, long bucketEnd, Collection projTasks) {
        Iterator tasksIt = projTasks.iterator();
        double totalQty = 0.0;
        while (tasksIt.hasNext()) {
            Task projTask = (Task) tasksIt.next();
            long taskStart = getTaskUtils().getStartTime(projTask);
            long taskEnd = getTaskUtils().getEndTime(projTask);

            /*
            if (taskStart > bucketEnd || taskEnd < bucketStart) {
              continue;
            }
            */

            long start = Math.max(taskStart, bucketStart);
            long end = Math.min(taskEnd, bucketEnd);
            //duration in seconds
            double duration = ((end - start) / 1000);
            Rate rate = getTaskUtils().getRate(projTask, start, end);
            double qty = (getBaseUnitPerSecond(rate) * duration);
            totalQty += qty;
        }
        return totalQty;
    }

    protected double getBaseUnitPerSecond(Rate rate) {
        if (rate instanceof CostRate) {
            return ((CostRate) rate).getDollarsPerSecond();
        } else if (rate instanceof CountRate) {
            return ((CountRate) rate).getEachesPerSecond();
        } else if (rate instanceof FlowRate) {
            return ((FlowRate) rate).getGallonsPerSecond();
        } else if (rate instanceof MassTransferRate) {
            return ((MassTransferRate) rate).getShortTonsPerSecond();
        } else if (rate instanceof TimeRate) {
            return ((TimeRate) rate).getHoursPerSecond();
        } // if
        return 0.0;
    }

    //Method to check if children in hash still on blackboard if parent gp
    //hash a disposition
    protected void checkForOrphans(Task gpTask) {
        HashMap assetMap = (HashMap) projHash.get(gpTask);
        Iterator assetsIt = assetMap.keySet().iterator();
        while (assetsIt.hasNext()) {
            Asset consumed = (Asset) assetsIt.next();
            Collection projTasks = (Collection) (assetMap.get(consumed));
            Iterator taskIt = projTasks.iterator();
            while (taskIt.hasNext()) {
                Task projTask = (Task) taskIt.next();
                if (dgPlugin.checkIfTaskOnBlackboard(projTask)) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Parent task has Disposition.   Orphaned task = " + getTaskUtils().taskDesc(projTask));
                    }
                }
            }
        }
    }


    protected boolean addToAndPublishExpansion(Task parent, Collection subtasks) {
        Expansion expansion = null;
        try {
            expansion = (Expansion) parent.getPlanElement();
        } catch (ClassCastException ex) {
            if (logger.isErrorEnabled()) {
                logger.error("ClassCastException.  Unexpected disposition on gpTask that was found as parent in ProjectSupplies.  Bug# 13670 GP Task = " + parent, ex);
                checkForOrphans(parent);
                return false;
            }
        }
        NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
        Iterator subtasksIT = subtasks.iterator();
        //TODO: MWD Remove debug statements:
        if ((dgPlugin.getOrgName() != null) &&
                (dgPlugin.getOrgName().trim().equals("1-35-ARBN")) &&
                (logger.isDebugEnabled())) {
            logger.debug("DGPlugin:DemandTaskGenerator:I'm publishing " + subtasks.size() + " " + dgPlugin.getSupplyType() + " Supply tasks");
        }
        while (subtasksIT.hasNext()) {
            Task task = (Task) subtasksIT.next();
            wf.addTask(task);
            ((NewTask) task).setWorkflow(wf);
            dgPlugin.publishAdd(task);
        }
        dgPlugin.publishChange(expansion);

        return true;
    }


    protected NewTask createNewDemandTask(Task parentTask,
                                          Task projTask,
                                          Asset consumed,
                                          long start,
                                          long end,
                                          double qty) {

        Vector prefs = createDemandPreferences(start, end, qty);

        NewTask newTask = getPlanningFactory().newTask();

        newTask.setParentTask(parentTask);
        newTask.setPlan(parentTask.getPlan());

        //TODO: MWD Remove
        //newTask.setPrepositionalPhrases(parentTask.getPrepositionalPhrases());
        //newTask = addPrepositionalPhrase(newTask, prepPhrase);
        //TODO: MWD Remove - also remove createDemandPrepPhrases
        //Vector preps = createDemandPrepPhrases(parentTask, consumed, end);
        //newTask.setPrepositionalPhrases(preps.elements());

        newTask.setPrepositionalPhrases(projTask.getPrepositionalPhrases());

        newTask.setDirectObject(consumed);
        newTask.setVerb(Verb.get(Constants.Verb.SUPPLY));


        newTask.setPreferences(prefs.elements());

        //bug#2974
        newTask.setContext(parentTask.getContext());

        newTask.setCommitmentDate(new Date(end));


        return newTask;
    }


    /**
     *  Create FOR, TO, MAINTAIN, and OFTYPE prepositional phrases
     *  for use by the subclasses.
     *  @param parentTask - The parent GP Task
     *  @param resource
     *  @param time - used to find the OPlan and the geoloc for the TO preposition
     *  @return Vector of PrepostionalPhrases
     *
     **/

    /***
     * TODO: MWD Remove
     *

     protected Vector createDemandPrepPhrases
     (Task parentTask, Asset resource, long time) {

     Asset consumer = parentTask.getDirectObject();

     //  Create prepositions for the new demand task
     Vector pp_vector = new Vector();
     PrepositionalPhrase pp =
     parentTask.getPrepositionalPhrase(Constants.Preposition.OFTYPE);

     if (pp != null) {
     Object obj = pp.getIndirectObject();
     if (obj instanceof String) {
     pp_vector.addElement(newPrepositionalPhrase
     (Constants.Preposition.OFTYPE,  obj));
     }
     }

     pp_vector.addElement(newPrepositionalPhrase
     (Constants.Preposition.FOR, dgPlugin.getOrgName()));

     // when oplan id added to tasks....
     //   pp_vector.addElement (newPrepositionalPhrase (Constants.Preposition.FOROPLAN, oplan));

     GeolocLocation geoloc = getGeolocLocation(time, dgPlugin.getMyOrganization());
     if (geoloc != null) {
     pp_vector.addElement(newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
     } else {
     // Try to use HomeLocation
     try {
     geoloc = (GeolocLocation)
     dgPlugin.getMyOrganization().getMilitaryOrgPG().getHomeLocation();
     pp_vector.addElement
     (newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
     } catch (NullPointerException npe) {
     if(logger.isErrorEnabled()) {
     logger.error("Geoloc not found");
     }
     }
     }

     if (consumer != null) {
     MaintainedItem itemID;
     if (consumer instanceof Asset) {
     TypeIdentificationPG tip = ((Asset) consumer).getTypeIdentificationPG();
     ItemIdentificationPG iip = ((Asset) consumer).getItemIdentificationPG();
     if (iip != null) {
     itemID = MaintainedItem.findOrMakeMaintainedItem
     ("Asset", tip.getTypeIdentification(), iip.getItemIdentification(),
     tip.getNomenclature(),dgPlugin);
     } else {
     itemID = MaintainedItem.findOrMakeMaintainedItem
     ("Asset", tip.getTypeIdentification(), null, tip.getNomenclature(), dgPlugin);
     }
     } else {
     itemID = MaintainedItem.findOrMakeMaintainedItem
     ("Other", consumer.toString(), null, null,dgPlugin);
     }
     pp_vector.addElement(newPrepositionalPhrase
     (Constants.Preposition.MAINTAINING, itemID));
     }

     return pp_vector;
     }


     public PrepositionalPhrase newPrepositionalPhrase(String preposition,
     Object io) {
     NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
     pp.setPreposition(preposition);
     pp.setIndirectObject(io);
     return pp;
     }
     **/

    /**
     * @return Geographic location of this organization at specifed time.
     *         TODO: MWD Remove
     *         *
     *         public GeolocLocation getGeolocLocation(long time, Organization myOrg) {
     *         Enumeration geolocs =
     *         getAssetUtils().getGeolocLocationAtTime(myOrg, time);
     *         if (geolocs.hasMoreElements()) {
     *         return ((GeolocLocation) geolocs.nextElement());
     *         }
     *         return null;
     *         }
     */

    protected Vector createDemandPreferences(long start, long end, double qty) {
        Vector prefs = new Vector();

        //Took out START_TIME preference on 6/3/2003 as they seemed like misleading
        //superfluous information that nobody else counts on.   Nominally a Supply
        //task doesn't have a start time.   Didn't want to include it so that
        //in the future someone may rely upon it.   If this Changes you have
        //to tell PSU

        //Whoops PSU currently rely's on the start time.   Which is a problem
        //For the time being reintroduce it.  Talked To Ray

        prefs.addElement(createTimeScoringFunctionPref(start, AspectType.START_TIME));
        prefs.addElement(createTimeScoringFunctionPref(end, AspectType.END_TIME));

        AspectValue av = AspectValue.newAspectValue(AspectType.QUANTITY, qty);
        ScoringFunction score = ScoringFunction.createStrictlyAtValue(av);
        prefs.addElement(getPlanningFactory().newPreference(AspectType.QUANTITY, score));

        return prefs;
    }


    protected Preference createTimeScoringFunctionPref
            (long bestTime, int aspectType) {

        long bucketStart = dgPlugin.getStartOfPeriod(dgPlugin.getCurrentTimeMillis());
        long bucketEnd = bucketStart + dgPlugin.getPeriod();
        long earliestTime = dgPlugin.getLogOPlanStartTime();
// Get the later of now and org report time
        if (bucketStart > dgPlugin.getLogOPlanStartTime()) {
            earliestTime = bucketStart;
        }
        long theBadPast = bucketStart - 1;
        long latestTime = dgPlugin.getLogOPlanEndTime();
        long firstLateTime = bestTime + dgPlugin.getPeriod();

        double bucketsBetween = ((latestTime - bestTime) / dgPlugin.getPeriod()) - 1;
//Use .0033 as a slope for now
        double lateScore = .0033 * bucketsBetween;
// use some baseline score for earlier or 1 day late
        double alpha = .25;
        Vector points = new Vector();

        AspectScorePoint badest = new AspectScorePoint
                (AspectValue.newAspectValue(aspectType, theBadPast), 1.0);
        AspectScorePoint earliest = new AspectScorePoint
                (AspectValue.newAspectValue(aspectType, earliestTime), alpha);
        AspectScorePoint best = new AspectScorePoint
                (AspectValue.newAspectValue(aspectType, bestTime), 0.0);
        AspectScorePoint first_late = new AspectScorePoint
                (AspectValue.newAspectValue(aspectType, firstLateTime), alpha);
        AspectScorePoint latest = new AspectScorePoint
                (AspectValue.newAspectValue(aspectType, latestTime), alpha + lateScore);

        points.addElement(badest);
        points.addElement(earliest);
        points.addElement(best);
        if (firstLateTime < latestTime) {
            points.addElement(first_late);
        } else if (logger.isInfoEnabled()) {
            // Note that this case can happen when near the end of stage 4 in the UAas its on
            //last day of demand before the new org activity has been issued
            logger.info(dgPlugin.getOrgName() + ".createTimePref skipping firstLate point: latest <= firstLate! latest: " + new Date(latestTime) + ", firstLate: " + new Date(firstLateTime) + ((bestTime == latestTime && aspectType == AspectType.END_TIME) ? ". A Task EndPref where bestTime==OplanEnd." : ". AspectType: " + aspectType));
        }
        points.addElement(latest);

        ScoringFunction score = ScoringFunction.createPiecewiseLinearScoringFunction(points.elements());

        //ScoringFunction score = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(aspectType, bestTime));

        return getPlanningFactory().newPreference(aspectType, score);
    }

}


