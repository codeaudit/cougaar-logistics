/*
 * <copyright>
 *  Copyright 1999-2003 Honeywell Inc.
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

package org.cougaar.logistics.plugin.packer;

// utils

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.NewContentsPG;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Priority;
import org.cougaar.planning.ldm.plan.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class AmmoTransport extends AggregationClosure {
  public static final String AMMO_CATEGORY_CODE = "MBB";
  public static final String MILVAN_NSN = "NSN/8115001682275";
  public static final double PACKING_LIMIT = 13.9; /* short tons */

  private static Asset MILVAN_PROTOTYPE = null;
  private static long COUNTER = 0;

  private GeolocLocation mySource;
  private GeolocLocation myDestination;

  public static Collection getTransportGroups(Collection tasks) {
    HashMap destMap = new HashMap();

    for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
      Task task = (Task) iterator.next();
      GeolocLocation destination = getDestination(task);
      Collection destTasks = (Collection) destMap.get(destination);
      if (destTasks == null) {
        destTasks = new ArrayList();
        destMap.put(destination, destTasks);
      }
      destTasks.add(task);
    }

    return destMap.values();
  }

  public AmmoTransport() {
  }

  //  public AmmoTransport(ArrayList tasks) {
  //    setDestinations(tasks);
  //  }

  public void setDestinations(ArrayList tasks) {
    for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
      Task t = (Task) iterator.next();
      GeolocLocation taskSource = getSource(t);
      GeolocLocation taskDestination = getDestination(t);

      if ((taskSource == null) || (taskDestination == null)) {

        _gp.getLoggingService().error("AmmoTransport(): task without a source/destination");
      } else if ((mySource == null) || (myDestination == null)) {
        mySource = taskSource;
        myDestination = taskDestination;
      } else if (!(mySource.getGeolocCode().equals(taskSource.getGeolocCode()))) {
        _gp.getLoggingService().error("AmmoTransport(): " + mySource + " not equal to " +
                                      taskSource);
      } else if (!(myDestination.getGeolocCode().equals(taskDestination.getGeolocCode()))) {
        _gp.getLoggingService().error("AmmoTransport(): " + myDestination +
                                      " not equal to " + taskDestination);
      }
    }
  }

  /**
   * returns max quanitity in short tons
   *
   * BOZO - unit picked because it agreed with the incoming supply requests
   */
  public double getQuantity() {
    return PACKING_LIMIT;
  }

  /**
   * returns appropriate transport source location
   *
   * Currently hardcoded because incoming supply tasks don't have that
   * info
   */
  public static GeolocLocation getSource(Task task) {
    return Geolocs.blueGrass();
  }

  /**
   * returns appropriate transport destination location
   *
   */
  public static GeolocLocation getDestination(Task task) {
    PrepositionalPhrase phrase =
        task.getPrepositionalPhrase(Constants.Preposition.TO);
    GeolocLocation destination = null;
    if (phrase != null) {
      destination = (GeolocLocation) phrase.getIndirectObject();
    }
    return destination;
  }

  public boolean validTask(Task t) {
    return (mySource.getGeolocCode().equals(getSource(t).getGeolocCode()) &&
        myDestination.getGeolocCode().equals(getDestination(t).getGeolocCode()));
  }

  /**
   * Creates a Transport task, per the interface published by TOPS.
   */
  public NewMPTask newTask() {
    if (_gp == null) {
      _gp.getLoggingService().error("AmmoTransport:  Error!  AmmoTransport not properly initialized: setGenericPlugin not called.");
      return null;
    }

    if ((mySource == null) ||
        (myDestination == null)) {
      _gp.getLoggingService().error("AmmoTransport:  Error!  AmmoTransport not properly initialized: some parameter(s) are null.");
      return null;
    }

    Asset milvan = makeMilvan();
    if (milvan == null) {
      return null;
    }

    NewMPTask task = _gp.getGPFactory().newMPTask();
    task.setVerb(Constants.Verb.Transport);

    task.setPriority(Priority.UNDEFINED);

    task.setDirectObject(milvan);

    Vector preps = new Vector(2);

    NewPrepositionalPhrase fromPrepositionalPhrase =
        _gp.getGPFactory().newPrepositionalPhrase();
    fromPrepositionalPhrase.setPreposition(Constants.Preposition.FROM);
    fromPrepositionalPhrase.setIndirectObject(mySource);
    preps.addElement(fromPrepositionalPhrase);

    NewPrepositionalPhrase toPrepositionalPhrase =
        _gp.getGPFactory().newPrepositionalPhrase();
    toPrepositionalPhrase = _gp.getGPFactory().newPrepositionalPhrase();
    toPrepositionalPhrase.setPreposition(Constants.Preposition.TO);
    toPrepositionalPhrase.setIndirectObject(myDestination);
    preps.addElement(toPrepositionalPhrase);

    task.setPrepositionalPhrases(preps.elements());

    return task;
  }

  /**
   * An ancillary method that creates an asset that represents a MILVAN
   * (military container) carrying ammunition
   */
  protected Asset makeMilvan() {

    if (MILVAN_PROTOTYPE == null) {
      MILVAN_PROTOTYPE = _gp.getGPFactory().getPrototype(MILVAN_NSN);

      if (MILVAN_PROTOTYPE == null) {
        _gp.getLoggingService().error("AmmoTransport: Error! Unable to get prototype for" +
                                      " milvan NSN -" + MILVAN_NSN);
        return null;
      }
    }

    Container milvan =
        (Container) _gp.getGPFactory().createInstance(MILVAN_PROTOTYPE);

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
        (NewItemIdentificationPG) milvan.getItemIdentificationPG();
    String itemID = makeMilvanID();
    itemIdentificationPG.setItemIdentification(itemID);
    itemIdentificationPG.setNomenclature("Milvan");
    itemIdentificationPG.setAlternateItemIdentification(itemID);
    milvan.setItemIdentificationPG(itemIdentificationPG);

    return milvan;
  }

  protected String makeMilvanID() {
    return new String("Milvan" + getCounter());
  }

  private static synchronized long getCounter() {
    return COUNTER++;
  }

}







