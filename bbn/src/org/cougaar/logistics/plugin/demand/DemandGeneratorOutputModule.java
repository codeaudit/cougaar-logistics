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

package org.cougaar.logistics.plugin.demand;

import org.cougaar.util.log.Logger;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.plan.GeolocLocation;

import java.util.List;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * The DemandGeneratorOutputModule outputs to a file demand data
 * based on ProjectSupply tasks.
 *
 * @see DemandGeneratorPlugin
 * @see DemandGeneratorModule
 * @see DemandGeneratorInputModule
 **/
public class DemandGeneratorOutputModule {

  public final static String OUTPUT_SUBDIR = "DemandGeneratorOutput";

  protected transient Logger logger;
  protected transient DemandGeneratorPlugin dgPlugin;
  private boolean aborted = false;

  // PrintWriter for file output
  private PrintWriter writer;

  /**
   * DemandGeneratorOutputModule Constructor
   * @param demandGeneratorPlugin Demand GeneratorPlugin for which this module
   * is created
   */
  public DemandGeneratorOutputModule(DemandGeneratorPlugin demandGeneratorPlugin) {
    this.dgPlugin = demandGeneratorPlugin;
    logger = dgPlugin.getLoggingService(this);
  }

  /**
   * Perform the output to file task
   * @param supplyTasks List of supply tasks from which demand is to be extracted
   * and written to file
   */
  public void writeDemandOutputToFile(List supplyTasks) {
    if (aborted) {
      // Do not try and repeatedly open this file
      return;
    }

    String dirPath = System.getProperty("org.cougaar.workspace", ".") + File.separator +      OUTPUT_SUBDIR;

    if (writer == null) {

      File dirFile = new File(dirPath);
      // Open file for output of demand data
      File file = new File(dirPath,
                           "ExecutionDemand." + dgPlugin.getOrgName() + "." +
                           dgPlugin.getSupplyType());
      try {
	dirFile.mkdirs();
        writer = new PrintWriter(new FileWriter(file,
            dgPlugin.getBlackboardService().didRehydrate()));
      }
      catch (IOException ioe) {
	if(logger.isErrorEnabled()) {
          logger.error("Unable to open output file: " + file.toString());
	}
        aborted = true;
        return;
      }
    }

    // Track all demand in this ordered map so to ensure that the demand
    // is written to file time-ordered
    TreeMap demandMap = new TreeMap();

    // Loop through each of the supply tasks provided, extract relevant demand
    // data and output as single entry in output file
    Iterator tasks = supplyTasks.iterator();
    while (tasks.hasNext()) {
      Task task = (Task)tasks.next();

      // Timestamp on demand data will coorespond to the start time pref
      long timeStamp = (long)task.getPreference(AspectType.START_TIME).
                       getScoringFunction().getBest().getValue();

      // Organization being supported
      String org = (String)task.getPrepositionalPhrase(
          Constants.Preposition.FOR).getIndirectObject();

      // Item (or truck) being maintained
      MaintainedItem item = (MaintainedItem)task.getPrepositionalPhrase(
          Constants.Preposition.MAINTAINING).getIndirectObject();
      String maintainedItemId = item.getItemIdentification();
      String maintainedTypeId = item.getTypeIdentification();
      String maintainedNomen = item.getNomenclature();

      // Required Item Id
      Asset consumed = task.getDirectObject();
      String consumedId = consumed.getTypeIdentificationPG().getTypeIdentification();
      SupplyClassPG supplyPG = (SupplyClassPG)consumed.searchForPropertyGroup(
            org.cougaar.glm.ldm.asset.SupplyClassPG.class);
        if (supplyPG == null) {
	  if(logger.isErrorEnabled()) {
            logger.error("Unable to retrieve SupplyClassPG for " + consumedId);
	  }
          continue;
        }
        String type = supplyPG.getSupplyType();
        if (!type.equals(dgPlugin.getSupplyType())) {
          continue;
        }

      // Required Item Qty
      double qty = task.getPreference(AspectType.QUANTITY).getScoringFunction().getBest().getValue();

      // Geoloc
      GeolocLocation geoloc = (GeolocLocation)task.getPrepositionalPhrase(
          Constants.Preposition.TO).getIndirectObject();

      // Construct simple csv formatted line of data for this demand & write to file
      String output = timeStamp + "," + org + "," + maintainedItemId + "," +
                      maintainedTypeId + "," + maintainedNomen + "," +
                      consumedId + "," + qty + "," +
                      geoloc.getLatitude().getDegrees() + "," +
                      geoloc.getLongitude().getDegrees() + "," +
                      geoloc.getName();

      Long key = new Long(timeStamp);
      ArrayList list = (ArrayList)demandMap.get(key);
      if (list == null) {
        list = new ArrayList();
        demandMap.put(key, list);
      }
      list.add(output);
    }

    // Loop through all demand and write to file
    Iterator lists = demandMap.values().iterator();
    while (lists.hasNext()) {
      ArrayList list = (ArrayList)lists.next();
      Iterator outputs = list.iterator();
      while (outputs.hasNext()) {
        String output = (String)outputs.next();
        writer.println(output);
      }
    }
    writer.flush();
  }
}
