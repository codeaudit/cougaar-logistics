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

package org.cougaar.mlm.ui.psp.transit;

import java.util.*;

import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.EmptyEnumeration;

import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.glm.ldm.plan.*;

import org.cougaar.mlm.ui.psp.transit.data.legs.Leg;

// Only use of this toolkit module class is commented out below
//import org.cougaar.lib.util.UTILPrepPhrase;

/**
 * The <code>DataComputer</code> contains all the "TOPS-guts" for the
 * <code>PSP_DataGatherer</code>.
 * <pre>
 * There are just four public methods:
 * 1) <tt>
 *   <i>(get a predicate for the PSP)</i>
 *   public static final UnaryPredicate getCarrierAssetsPredicate() {..}</tt>
 * 2) <tt>
 *   <i>(get a predicate for the PSP)</i>
 *   public static final UnaryPredicate getLegTasksPredicate() {..}</tt>
 * 3) <tt>
 *   <i>(compute all the carriers (ships, trucks, etc)</i>
 *   public static final void computeAllCarriers(
 *       Registry toReg,
 *       Collection carrierAssets) {..}</tt>
 *   <i>(where carrerAssets matches the carrierAssets predicate)</i>
 * 4) <tt>
 *   <i>(compute all the legs, assuming that the carriers have already</i>
 *      <i>been computed)</i>
 *   public static final void computeAllLegs(
 *       Registry toReg,
 *       Collection legTasks) {..}</tt>
 *   <i>(where legTasks matches the legTasks predicate)</i>
 *</pre>
 * <p>
 * "DataComputer" isn't such a great name, but at least it's no
 * worse than "PSP_DataGatherer".
 *
 * @see DataRegistry
 */
public abstract class DataComputer {

  public static final boolean DEBUG = 
    Boolean.getBoolean(
        "org.cougaar.mlm.ui.psp.transit.DataComputer.debug");

  private DataComputer() {
    // just static functions!
  }

  //
  // BEGIN PREDICATES
  //

  private static final UnaryPredicate carrierAssetsPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof PhysicalAsset) ||
           ((o instanceof GLMAsset) &&
            (((GLMAsset)o).hasPersonPG())));
      }
    };

  /**
   * Get the <code>UnaryPredicate</code> for <tt>computeAllCarriers</tt>.
   *
   * Filter for carrier assets owned by this cluster.
   *
   * Currently asset transfer is not supported, so all
   * PhysicalAssets/People in this cluster are owned by
   * this cluster.
   */
  public static final UnaryPredicate getCarrierAssetsPredicate() {
    return carrierAssetsPredicate;
  }

  private static final UnaryPredicate convoysPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Object pe = ((Task)o).getPlanElement();
          if (pe instanceof Allocation) {
            Asset asset = ((Allocation)pe).getAsset();
            return 
              ((asset instanceof AssetGroup) &&
               ((asset instanceof Convoy) ||
                (asset instanceof Train)));
          }
        }
        return false;
      }
    };

  /**
   * Get the <code>UnaryPredicate</code> for <tt>computeAllConvoys</tt>.
   *
   * Filter for all Tasks allocated to convoys.
   *
   * Currently asset transfer is not supported, so all the carriers
   * in the convoys should be within this cluster.
   */
  public static final UnaryPredicate getConvoysPredicate() {
    return convoysPredicate;
  }

  private static final class LegTasksPredicate implements UnaryPredicate {
    boolean includeTransitLegs;
    public LegTasksPredicate (boolean includeTransitLegs) { this.includeTransitLegs = includeTransitLegs; }
    public boolean execute(Object o) {
      if (o instanceof Task) {
	Task task = (Task)o;
	Verb v = task.getVerb();
	if ((Constants.Verb.Transport).equals(v) ||
	    (includeTransitLegs && (Constants.Verb.Transit).equals(v)) ||
	    (Constants.Verb.TransportationMission).equals(v) ||
	    (Constants.Verb.Supply).equals(v)) {
	  Object pe = task.getPlanElement();
	  if (pe instanceof Allocation) {
	    if (task.getPrepositionalPhrase("IMPLIED") == null) {
	      Allocation alloc = (Allocation)pe;
	      Asset asset = alloc.getAsset();
	      return 
		((asset instanceof PhysicalAsset) ||
		 ((asset instanceof GLMAsset) &&
		  (((GLMAsset)asset).hasPersonPG())));
	    }
	  }
	}
      }
      return false;
    }
  }

  /**
   * Get the <code>UnaryPredicate</code> for <tt>computeAllLegs</tt>.
   * <pre>
   * Leg tasks have these properties:
   *   1) The verb must be "Transport", "TransportationMission",
   *       or "Supply".
   *   2) There must be no "IMPLIED" preposition.
   *   3) The Task is allocated to either a physical assets or 
   *      a person.
   * </pre>
   */
  public static final UnaryPredicate getLegTasksPredicate(boolean includeTransitLegs) {
    return new LegTasksPredicate (includeTransitLegs);
  }

  //
  // END PREDICATES
  //

  //
  // BEGIN COMPUTE
  //

  /**
   * Compute all our carriers, not including self-propelled cargo.
   *
   * Must do this <i>before</i> <tt>computeAllConvoys</tt> and
   * <tt>computeAllLegs</tt>.
   */
  public static final void computeAllCarriers(
      Registry toReg,
      Collection carrierAssets) {
    if (DEBUG) {
      System.out.println("DataComputer.computeAllCarriers");
    }

    // pre-register all our carrier assets.  this will allow
    // the legs to differentiate between cluster-owned and
    // self-propelled carriers.
    Iterator carrierAssetIter = carrierAssets.iterator();
    for (int n = carrierAssets.size(); n > 0; n--) {
      Asset asset = (Asset)carrierAssetIter.next();
      // register as non-self-propelled
      DataRegistry.registerCarrierInstance(
          toReg, asset, false);
    }
  }

  /**
   * Compute all our convoys.
   *
   * Must do this <i>after</i> <tt>computeAllCarriers</i> and <i>before</i> 
   * <tt>computeAllLegs</tt>.
   */
  public static final void computeAllConvoys(
      Registry toReg,
      Collection convoyTasks) {
    if (DEBUG) {
      System.out.println("DataComputer.computeAllConvoys");
    }

    // register all our convoy assets (Convoys and Trains).
    Iterator convoyTasksIter = convoyTasks.iterator();
    for (int n = convoyTasks.size(); n > 0; n--) {
      Task task = (Task)convoyTasksIter.next();
      Allocation alloc = (Allocation)task.getPlanElement();
      Asset convoyAsset = alloc.getAsset();
      if (DataRegistry.containsConvoy(toReg, convoyAsset)) {
        // already registered this convoy
      } else {
        // get the start and end times
        long startTime;
        long endTime;
        Enumeration itinElems = getItineraryElements(task);
        if (itinElems.hasMoreElements()) {
          // use the earliest and latest times of the schedule
          ItineraryElement ie = (ItineraryElement)itinElems.nextElement();
          startTime = ie.getStartTime();
          endTime = ie.getEndTime();
          while (itinElems.hasMoreElements()) {
            ie = (ItineraryElement)itinElems.nextElement();
            long t = ie.getStartTime();
            if (t < startTime) {
              startTime = t;
            }
            t = ie.getEndTime();
            if (t > endTime) {
              endTime = t;
            }
          }
        } else {
          // error? use the estimated start and end times
          startTime = 0;
          endTime = 0;
          AllocationResult est = alloc.getEstimatedResult(); 
          if (est != null) {
            if (est.isDefined(AspectType.START_TIME)) {
              startTime = (long)est.getValue(AspectType.START_TIME);
              if (est.isDefined(AspectType.END_TIME)) {
                endTime = (long)est.getValue(AspectType.END_TIME);
              }
            }
          }
        }
        // register the convoy
        DataRegistry.registerConvoy(
            toReg, convoyAsset, startTime, endTime);
      }
    }
  }

  /**
   * Compute all our legs, locations, and cargo information.
   *
   * Must do this <i>after</i> <tt>computeAllCarriers</tt> and 
   * <tt>computeAllConvoys</tt>.
   */
  public static final void computeAllLegs(
      Registry toReg,
      Collection legTasks) {

    if (DEBUG) {
      System.out.println("DataComputer.computeAllLegs");
    }

    // a temporary "helper" list
    List tmpLegs = new ArrayList();

    // Get all the leg tasks -- i.e. tasks in this cluster
    // that have a plan element that is an allocation AND
    // that the asset allocated to is a physical asset
    Iterator legTaskIter = legTasks.iterator();
    for (int n = legTasks.size(); n > 0; n--) {
      Task task = (Task)legTaskIter.next();

      if (DEBUG) {
        System.out.println("DataComputer.computeAllLegs - " + 
            "Task w/ physical asset: "+ 
            task.getUID().toString());
      }

      // get the "skeleton" legs for this task
      //   later we will fill in the ids, carrier, etc
      tmpLegs.clear();
      if (!(computeLegs(toReg, tmpLegs, task))) {
        // invalid?
        continue;
      }
      int nTmpLegs = tmpLegs.size();
      if (nTmpLegs > 0) {
        // get the other task-based information

        Allocation alloc = (Allocation)task.getPlanElement();
        // get the "cargo" == whatever's being moved
        Asset cargoAsset = task.getDirectObject();

        // get the "carrier" == the conveyer == the mover
        Asset carrierAsset = alloc.getAsset();

        // register the carrier.  if not already registered then
        //   register the instance as self-propelled.
        UID carrierId = 
          DataRegistry.registerCarrierInstance(
              toReg, carrierAsset, true);

        // leg is just "detail" if allocated to a Deck,
        //   as opposed to an allocation to the Ship/etc.
        boolean isDetail = (carrierAsset instanceof Deck);

        // register the cargo
        List cargoIds = new ArrayList();
	boolean validCargoID = 
	  DataRegistry.registerCargoInstance(toReg, 
					     cargoIds, 
					     cargoAsset,
					     task);

        if (!validCargoID) {
          // invalid cargo?
	  System.err.println ("DataComputer got null cargoID for " + cargoAsset + 
			      "? This should never happen.");
          continue;
        }

        int nCargoIds = cargoIds.size();

        // configure and register our legs
        long i = 0;

        do {
          Leg li = (Leg)tmpLegs.get((int)i);

          // configure leg

	  // i will probably never not be zero!
          li.UID = new UID(task.getUID().getOwner(), 
			   task.getUID().getId()+ (i << 56)); // store the leg ids in upper most 8 bits
	  
          li.conveyanceUID = carrierId;
          li.isDetail = isDetail;
          // inefficient:
          for (int j = 0; j < nCargoIds; j++) {
            li.addCarriedAsset((UID)cargoIds.get(j));
          }

          // register the leg
          DataRegistry.registerLeg(toReg, li);
        } while (++i < nTmpLegs);
      }
    }

    // success!
    toReg.endComputeTime = System.currentTimeMillis();
  }

  /**
   * Add the legs for this task to the given <code>List</code>.
   *
   * The legs are "skeletons" in that they will lack Ids, 
   * carrier, cargo, and other information that will be provided
   * by the caller.
   */
  private static boolean computeLegs(
      Registry toReg,
      List toSkelLegs,
      Task task) {
    // attempt to use the optional itinerary schedule
    Enumeration itinElemEnum = getItineraryElements(task);
    if (itinElemEnum.hasMoreElements()) {
      // use the itinerary for our leg193G
      if (computeLegsFromItinerary(
            toReg,
            toSkelLegs,
            task, 
            itinElemEnum)) {
        return true;
      }
      // ignore itinerary schedule
    }

    // treat the task as a single "Transport" leg
    return computeLegFromTask(toReg, toSkelLegs, task);
  }

  private static boolean computeLegFromTask(
      Registry toReg,
      List toSkelLegs,
      Task task) {
    // get the start location
    GeolocLocation startGeoloc = getStartLocation(task);
    if (startGeoloc == null) {
      // no start location?
      return false;
    }
    String startLoc = 
      DataRegistry.registerLocation(toReg, startGeoloc);

    // get the end location
    GeolocLocation endGeoloc = getEndLocation(task);
    if (endGeoloc == null) {
      // no end location?
      return false;
    }
    String endLoc = 
      DataRegistry.registerLocation(toReg, endGeoloc);

    // create a "Transport" Leg
    Leg leg = new Leg();
    leg.legType = getLegType(task.getVerb());
    leg.startLoc = startLoc;
    leg.endLoc = endLoc;

    if (DEBUG) {
      System.out.println(
          "DataComputer.computeLegFromTask - for leg " + leg + 
          ", start geoloc " + startLoc + " end geoloc " + endLoc + " leg type " + leg.legType);
    }

    // get the time information
    if (!(setEstimatedTimes(leg, task))) {
      // no start/end times?
      return false;
    }

    if (!(setPreferredStartTime(leg, task))) {
      // okay, use startTime
      leg.readyAtTime = leg.startTime;
    }

    if (!(setPreferredEndTimes(leg, task))) {
      // okay, use endTime
      long endTime = leg.endTime;
      leg.earliestEndTime = endTime;
      leg.bestEndTime = endTime;
      leg.latestEndTime = endTime;
    }

    if (leg.legType == Leg.LEG_TYPE_TRANSPORTING) {
      // get the route
      TransportationRoute transRoute = 
        getTransportationRoute(task);
      if (transRoute != null) {
        leg.routeUID = DataRegistry.registerRoute(toReg, transRoute);
      }
    }

    // add leg to list
    toSkelLegs.add(leg);

    // success!
    return true;
  }

  private static boolean computeLegsFromItinerary(
      Registry toReg,
      List toSkelLegs,
      Task task, 
      Enumeration itinElemEnum) {
    // use the itinerary for our legs
    int origSkelLegsSize = toSkelLegs.size();
    try {
      while (true) {
        ItineraryElement ie = 
          (ItineraryElement)itinElemEnum.nextElement();
        // add legs based on the itinerary element
        if (!(computeLegsFromItineraryElement(
                toReg,
                toSkelLegs,
                task, 
                ie))) {
          // invalid itinerary element?
          break;
        }
        if (!(itinElemEnum.hasMoreElements())) {
          // successfully used all of schedule
          //
          // patch ready/early/best/latest times
          Leg firstLeg = (Leg)toSkelLegs.get(origSkelLegsSize);
          setPreferredStartTime(firstLeg, task);
          Leg lastLeg = (Leg)toSkelLegs.get(toSkelLegs.size()-1);
          setPreferredEndTimes(lastLeg, task);

          // success!!!
          return true;
        }
      }
    } catch (Exception e) {e.printStackTrace();
      // invalid itinerary element?
    }
    // remove any partial results
    for (int i = toSkelLegs.size(); 
        (i > origSkelLegsSize);
        ) {
      toSkelLegs.remove(--i);
    }
    return false;
  }

  private static boolean computeLegsFromItineraryElement(
      Registry toReg,
      List toSkelLegs,
      Task task, 
      ItineraryElement ie) {
    // create "skeleton" leg
    Leg leg = new Leg();
    int legType = getLegType(ie.getRole());
    leg.legType = legType;
    if (legType == Leg.LEG_TYPE_TRANSPORTING) {
      // get the route
      TransportationRoute transRoute = 
        getTransportationRoute(task);
      if (transRoute != null) {
        leg.routeUID = DataRegistry.registerRoute(toReg, transRoute);
      }
    }
    leg.startLoc =
      DataRegistry.registerLocation(
          toReg, 
          (GeolocLocation)ie.getStartLocation());
    leg.startTime =
      ie.getStartTime();
    leg.endLoc = 
      DataRegistry.registerLocation(
          toReg,
          (GeolocLocation)ie.getEndLocation());
    long endTime = ie.getEndTime();
    leg.endTime = endTime;

    // use start/end values for preferred time ranges
    leg.readyAtTime = leg.startTime;
    leg.earliestEndTime = endTime;
    leg.bestEndTime = endTime;
    leg.latestEndTime = endTime;

    // add leg to list
    toSkelLegs.add(leg);

    return true;
  }

  //
  // END COMPUTE
  //

  //
  // BEGIN UTILITIES
  //

  /**
   * Get the "Leg.LEG_TYPE_*" for the given <code>Verb</code>.
   */
  private static final int getLegType(Verb v) {
    return
      ((Constants.Verb.Transport).equals(v)) ?
      Leg.LEG_TYPE_TRANSPORTING :
      ((Constants.Verb.Load).equals(v)) ?
      Leg.LEG_TYPE_LOADING :
      ((Constants.Verb.Unload).equals(v)) ?
      Leg.LEG_TYPE_UNLOADING :
      ((Constants.Verb.Transit).equals(v)) ?
      // covers POSITIONING and RETURNING
      Leg.LEG_TYPE_POSITIONING :
      ((Constants.Verb.Fuel).equals(v)) ?
      Leg.LEG_TYPE_REFUELING :
      // unknown
      Leg.LEG_TYPE_UNKNOWN;
  }

  /**
   * Get the start location for a Task.
   *
   * @return the GeolocLocation
   */
  private static final GeolocLocation getStartLocation(Task task) {
    try {
      PrepositionalPhrase prepFrom =
        task.getPrepositionalPhrase(Constants.Preposition.FROM);
      if (prepFrom != null) {
        return
          (GeolocLocation)prepFrom.getIndirectObject();
      }
    } catch (Exception e) {e.printStackTrace();
      // error?
    }
    return null;
  }

  /**
   * Get the end location for a Task.
   *
   * @return the GeolocLocation
   */
  private static final GeolocLocation getEndLocation(Task task) {
    try {
      PrepositionalPhrase prepFrom =
        task.getPrepositionalPhrase(Constants.Preposition.TO);
      if (prepFrom != null) {
        return
          (GeolocLocation)prepFrom.getIndirectObject();
      }
    } catch (Exception e) {e.printStackTrace();
      // error?
    }
    return null;
  }

  /**
   * Set the "startTime" and "endTime" for the Leg.
   *
   * @return true if successful
   */
  private static final boolean setEstimatedTimes(
      Leg toLeg, Task task) {
    try {
      // get the estimated result
      AllocationResult est = 
        task.getPlanElement().getEstimatedResult();
      if (est != null) {
        // start time
        if (est.isDefined(AspectType.START_TIME)) {
          toLeg.startTime =
            (long)est.getValue(AspectType.START_TIME);
          // end time
          if (est.isDefined(AspectType.END_TIME)) {
            toLeg.endTime =
              (long)est.getValue(AspectType.END_TIME);
            return true;
          }
        }
      }
    } catch (Exception e) {e.printStackTrace();
      // error?
    }
    return false;
  }

  /**
   * Set the "readyAtTime" for the Leg.
   *
   * @return true if successful
   */
  private static final boolean setPreferredStartTime(
      Leg toLeg, Task task) {
    try {
      // preferred ready time
      Preference startPref =
        task.getPreference(AspectType.START_TIME);
      if (startPref != null) {
        ScoringFunction sf = startPref.getScoringFunction();
        AspectScorePoint bestP = sf.getBest();
        toLeg.readyAtTime = (long)bestP.getValue();
        if (toLeg.readyAtTime == toLeg.startTime) {
//           Date readyAt = (Date)UTILPrepPhrase.getIndirectObject(task,TOPSConst.READYAT);
//           toLeg.readyAtTime = readyAt.getTime();
        }
        return true;
      } 
    } catch (Exception e) {e.printStackTrace();
      // error?
    }
    return false;
  }

  /**
   * Set the "earliestEndTime"/"bestEndTime"/"latestEndTime" for 
   * the Leg.
   *
   * @return true if successful
   */
  private static final boolean setPreferredEndTimes(
      Leg toLeg, Task task) {
    // preferred end times
    try {
      Preference endPref =
        task.getPreference(AspectType.END_TIME);
      if (endPref != null) {
        ScoringFunction sf = endPref.getScoringFunction();
        AspectScorePoint bestP = sf.getBest();
        toLeg.bestEndTime = (long)bestP.getValue();
        Enumeration rangeEn = sf.getValidRanges(null, null);
        AspectScoreRange range =
          (AspectScoreRange)rangeEn.nextElement();
        AspectScorePoint startP = range.getRangeStartPoint();
        AspectScorePoint endP = range.getRangeEndPoint();
        toLeg.earliestEndTime = (long)startP.getValue();
        toLeg.latestEndTime = (long)endP.getValue();
        return true;
      }
    } catch (Exception e) {e.printStackTrace();
      // error?
    }
    return false;
  }

  private static Enumeration getItineraryElements(Task task) {
    PrepositionalPhrase prepItinerary =
      task.getPrepositionalPhrase(
          Constants.Preposition.ITINERARYOF);
    if (prepItinerary != null) {
      Object itinIndObj =
        prepItinerary.getIndirectObject();
      if (itinIndObj instanceof Schedule) {
        Schedule itinSched = (Schedule)itinIndObj;
        return itinSched.getAllScheduleElements();
      }
    }
    return EmptyEnumeration.elements();
  }

  /**
   * Get the transportation route for the given <code>Task</code>
   * and "Transport" itinerary.
   *
   * Currently there is only one route per task, so we don't use 
   * the itinerary (for now).
   */
  private static TransportationRoute getTransportationRoute(Task task) {
    try {
      PrepositionalPhrase prepVia = 
        task.getPrepositionalPhrase(Constants.Preposition.VIA);
      if (prepVia != null) {
        Object viaIndObj = prepVia.getIndirectObject();
        if (viaIndObj instanceof TransportationRoute) {
          return (TransportationRoute)viaIndObj;
        }
      }
    } catch (Exception e) {e.printStackTrace();
    }
    return null;
  }

  //
  // END UTILITIES
  //

}
