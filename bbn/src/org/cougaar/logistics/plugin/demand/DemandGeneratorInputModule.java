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

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.plan.GeolocLocationImpl;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.PrepositionalPhraseImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.UnaryPredicate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The DemandGeneratorInputModule generates demand from a data file input
 * Expected Input File Format is comma seperated value with the following
 * fields in the following order:
 *    timestamp(long), org(String), maintained asset id (String),
 *    item id (String), quantity (double)
 *
 *    note: Any String field that contains a comma will be expected in quotes (")
 *
 * @see DemandGeneratorPlugin
 * @see DemandGeneratorModule
 * @see DemandGeneratorOutputModule
 **/
public class DemandGeneratorInputModule extends DemandTaskGenerator {

  /**
   * This predicate will gather GenerateProjection tasks matching a specific
   * item/supply type
   */
  private static class GenerateProjectionsPredicate implements UnaryPredicate {
    String supplyType;
    MaintainedItem maintainedItem;

    /**
     * Predicate Constructor
     * @param type supply type of item
     * @param item item for which generate projections task is desired
     */
    public GenerateProjectionsPredicate(String type, MaintainedItem item) {
      supplyType = type;
      maintainedItem = item;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.GENERATEPROJECTIONS)) {
          // Got the right verb...
          PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.OFTYPE);
          if (pp != null && pp.getIndirectObject().equals(supplyType)) {
            // Got the right supply type...
            Asset asset = task.getDirectObject();
            if (asset instanceof AggregateAsset) {
              asset = ((AggregateAsset)asset).getAsset();
            }
            else {
              // If this is NOT an AggregateAsset we'll want to ensure the
              // item id's match (assuming both are non-null)
              if (maintainedItem.getItemIdentification() != null &&
                  asset.getItemIdentificationPG() != null &&
                  asset.getItemIdentificationPG().getItemIdentification() != null &&
                  !asset.getItemIdentificationPG().getItemIdentification().equals(
                  maintainedItem.getItemIdentification())) {
                return false;
              }
            }
            // Finally, check type ids
            String typeId = asset.getTypeIdentificationPG().getTypeIdentification();
            return (typeId.equals(maintainedItem.getTypeIdentification()));
          }
        }
      }
      return false;
    }

  }

  private BufferedReader reader;
  private int maxLineLength = 256;

  /**
   * Constructor defers to parent class
   * @param demandGeneratorPlugin DemandGeneratorPlugin for which this module is
   * created
   */
  public DemandGeneratorInputModule(DemandGeneratorPlugin demandGeneratorPlugin) {
    super(demandGeneratorPlugin);
  }

  /**
   * Initialize the file reader
   */
  private void initReader() {
    if (reader != null) {
      // Already initialized
      return;
    }

    String dirPath = System.getProperty("org.cougaar.workspace", ".") + File.separator +      DemandGeneratorOutputModule.OUTPUT_SUBDIR;

    // Open file for input of demand data
    File file = new File(dirPath,
                         "ExecutionDemand." + dgPlugin.getOrgName() + "." +
                         dgPlugin.getSupplyType());
    try {
      reader = new BufferedReader(new FileReader(file));
    }
    catch (IOException ioe) {
      if(logger.isWarnEnabled()) {
        logger.warn("Unable to open demand input file: " + file.toString());
      }
    }
  }

  /**
   * Generate demand tasks from file demand over the given time period
   * @param start start time of period over which to collect data and create demand
   * @param duration duration of period over which to collect data and create demand
   * @param relevantProjectSupplys NOT USED
   */
  public List generateDemandTasks(long start, long duration, Collection relevantProjectSupplys) {
    ArrayList demandTasks = new ArrayList();

    // Make sure the file reader is initialized
    initReader();
    if (reader == null) {
      return demandTasks;
    }

    HashMap subtaskMap = new HashMap();
    String line = null;

    try {
      // Mark current position in file
      reader.mark(maxLineLength);

      // Reader lines until we exit our defined period (ignore entries earlier)
      for (line = reader.readLine(); line != null; line=reader.readLine()) {
        line = line.trim();

        // skip empty lines
        if (line.length() == 0)
          continue;

        StringTokenizer tok = new StringTokenizer(line, ",");

        long timeStamp = (new Long(tok.nextToken())).longValue();
        if (timeStamp < start) {
          // Ignore entries prior to start
          continue;
        }

        if (timeStamp >= start + duration) {
          // Done.  Reset file marker to beginning of this line
          reader.reset();
          break;
        }

        // Extract The Org Name
        String org = tok.nextToken();

        // Extract the Item Id of the item being maintained
        String maintainedItemId = tok.nextToken();
        if (maintainedItemId.equalsIgnoreCase("null")) {
          maintainedItemId = null;
        }

        // Extract the Type Id of the item being maintained
        String maintainedTypeId = tok.nextToken();

        // Extract the Nomenclature of the item being maintained
        String maintainedNomen = tok.nextToken();
        if (maintainedNomen.equalsIgnoreCase("null")) {
          maintainedNomen = null;
        }

        // Extract the Id of the consumed item
        String consumedItem = tok.nextToken();

        // Extract the qty demanded
        double qty = Double.parseDouble(tok.nextToken());

        // Extract the location (lat, lon, name) for delivery
        double lat = Double.parseDouble(tok.nextToken());
        double lon = Double.parseDouble(tok.nextToken());
        String geoLocName = tok.nextToken();

        // Create an instance of the consumed item to determine supply type
        Asset consumed = getPlanningFactory().createInstance(consumedItem);
        SupplyClassPG supplyPG = (SupplyClassPG)consumed.searchForPropertyGroup(
            org.cougaar.glm.ldm.asset.SupplyClassPG.class);
        if (supplyPG == null) {
	  if(logger.isErrorEnabled()) {
            logger.error("Unable to retrieve SupplyClassPG for " + consumedItem);
	  }
          continue;
        }
        String type = supplyPG.getSupplyType();
        if (!type.equals(dgPlugin.getSupplyType())) {
          // This plugin/module does not handle this particular supply type
          continue;
        }

        // Create an instance of the Maintained Item and find the associated
        // GenerateProjections task
        MaintainedItem item = new MaintainedItem(type, maintainedTypeId,
            maintainedItemId, maintainedNomen, this.dgPlugin);
        Task gpTask = getGenerateProjectionsTask(item, type);
        if (gpTask == null) {
	  if(logger.isErrorEnabled()) {
            logger.error("Unable to find GenerateProjections task for item type id = '" +
                       item.getTypeIdentification() + "' and type = '" + type + "'");
	  }
          continue;
        }

        // Create the geoloc for delivery
        GeolocLocationImpl geoLoc = new GeolocLocationImpl(Latitude.newLatitude(lat),
            Longitude.newLongitude(lon), geoLocName);

        // Create the new demand task for the entry and add to list of new subtasks
        Task demandTask = createNewDemandTask(gpTask, consumed, timeStamp,
            timeStamp+duration, qty, geoLoc, org, type, item);
        demandTasks.add(demandTask);

        ArrayList subtasks = (ArrayList)subtaskMap.get(gpTask);
        if (subtasks == null) {
          subtasks = new ArrayList();
          subtaskMap.put(gpTask, subtasks);
        }
        subtasks.add(demandTask);

        // Mark the beginning of the next line before reading
        reader.mark(maxLineLength);
      }
    }
    catch (IOException ioe) {
      if(logger.isErrorEnabled()) {
        logger.error("Unable to parse demand input file: '" + line + "'");
      }
      ioe.printStackTrace();
      return demandTasks;
    }

    // Go through each of the new demand tasks and publish as subtasks
    // of the appropriate GenerateProjections task
    Iterator gpTasks = subtaskMap.keySet().iterator();
    while (gpTasks.hasNext()) {
      Task gpTask = (Task)gpTasks.next();
      Collection subtasks = (Collection)subtaskMap.get(gpTask);
      addToAndPublishExpansion(gpTask, subtasks);
    }

    // Return list of created demand tasks
    return demandTasks;
  }

  /**
   * Retreive the GenerateProjections task for a given (maintained) item
   * and supply type
   * @param item item being maintained
   * @param type supply type
   * @return The GenerateProjections task for the given item/supply type
   */
  protected Task getGenerateProjectionsTask(MaintainedItem item, String type) {
    Task gpTask = null;
    // Query blackboard...
    Collection gpTasks = dgPlugin.getBlackboardService().query(
        new GenerateProjectionsPredicate(type, item));
    if (!gpTasks.isEmpty()) {
      // There should only ever be one!
      gpTask = (Task)gpTasks.iterator().next();
    }
    return gpTask;
  }

  /**
   * Create new demand task based on demand file entries
   * @param parentTask Parent of task to be created
   * @param consumed The asset consumed
   * @param start The start preference
   * @param end The end preference
   * @param qty The qty preference
   * @param loc The GeolocLocation for delivery
   * @param org The requesting org
   * @param type The supply type
   * @param item The maintained item
   * @return The newly created SUPPLY task
   */
  protected NewTask createNewDemandTask(Task parentTask, Asset consumed,
                                        long start, long end, double qty,
    GeolocLocationImpl loc, String org, String type, MaintainedItem item) {

    Vector prefs = createDemandPreferences(start, end, qty);

    NewTask newTask = getPlanningFactory().newTask();

    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());

    // Build Prepositional Phrases and add to task
    Vector pps = new Vector();
    PrepositionalPhraseImpl pp = new PrepositionalPhraseImpl();
    pp.setPreposition(Constants.Preposition.FOR);
    pp.setIndirectObject(org);
    pps.add(pp);
    pp = new PrepositionalPhraseImpl();
    pp.setPreposition(Constants.Preposition.TO);
    pp.setIndirectObject(loc);
    pps.add(pp);
    pp = new PrepositionalPhraseImpl();
    pp.setPreposition(Constants.Preposition.MAINTAINING);
    pp.setIndirectObject(item);
    pps.add(pp);
    pp = new PrepositionalPhraseImpl();
    pp.setPreposition(Constants.Preposition.OFTYPE);
    pp.setIndirectObject(type);
    pps.add(pp);

    newTask.setPrepositionalPhrases(pps.elements());
    newTask.setDirectObject(consumed);
    newTask.setVerb(Verb.get(Constants.Verb.SUPPLY));
    newTask.setPreferences(prefs.elements());
    newTask.setContext(parentTask.getContext());
    newTask.setCommitmentDate(new Date(end));

    return newTask;
  }
}
