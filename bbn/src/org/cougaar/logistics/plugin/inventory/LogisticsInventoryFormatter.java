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

import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;

import org.cougaar.glm.ldm.plan.AlpineAspectType;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.util.TimeSpanSet;

/**
 * <pre>
 *
 * The LogisticsInventoryFormatter is a specialized writer class that
 * produces the csv output logged by the LogisticsInventoryLogger.
 * It maintains the contract with LogisticsInventoryBG on how to
 * write its internal data structures into the proper csv data output
 * to a provided output stream writer.   It also provides methods that will
 * wrap that output with useful header and column info, or in xml
 * for a servlet.
 *
 * @see LogisticsInventoryLogger
 * @see LogisticsInventoryBG
 *
 **/

public class LogisticsInventoryFormatter {

  public final static String INVENTORY_DUMP_TAG = "INVENTORY_DUMP";

  public final static String INVENTORY_HEADER_PERSON_READABLE_TAG = "INVENTORY_HEADER_READABLE";
  public final static String INVENTORY_HEADER_GUI_TAG = "INVENTORY_HEADER_GUI";

  public final static String SUPPLY_TASKS_TAG = "SUPPLY_TASKS";
  public final static String WITHDRAW_TASKS_TAG = "WITHDRAW_TASKS";
  public final static String PROJECTSUPPLY_TASKS_TAG = "PROJECTSUPPLY_TASKS";
  public final static String PROJECTWITHDRAW_TASKS_TAG = "PROJECTWITHDRAW_TASKS";
  public final static String COUNTED_PROJECTWITHDRAW_TASKS_TAG = "COUNTED_PROJECTWITHDRAW_TASKS";

  public final static String SUPPLY_TASK_ARS_TAG = "SUPPLY_TASK_ALLOCATION_RESULTS";
  public final static String WITHDRAW_TASK_ARS_TAG = "WITHDRAW_TASK_ALLOCATION_RESULTS";
  public final static String PROJECTSUPPLY_TASK_ARS_TAG = "PROJECTSUPPLY_TASK_ALLOCATION_RESULTS";
  public final static String PROJECTWITHDRAW_TASK_ARS_TAG = "PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS";
  public final static String COUNTED_PROJECTWITHDRAW_TASK_ARS_TAG = "COUNTED_PROJECTWITHDRAW_TASK_ALLOCATION_RESULTS";

  public final static String RESUPPLY_SUPPLY_TASKS_TAG = "RESUPPLY_SUPPLY_TASKS";
  public final static String RESUPPLY_PROJECTSUPPLY_TASKS_TAG = "RESUPPLY_PROJECTSUPPLY_TASKS";
  public final static String RESUPPLY_SUPPLY_TASK_ARS_TAG = "RESUPPLY_SUPPLY_TASK_ALLOCATION_RESULTS";
  public final static String RESUPPLY_PROJECTSUPPLY_TASK_ARS_TAG = "RESUPPLY_PROJECTSUPPLY_TASK_ALLOCATION_RESULTS";
  public final static String INVENTORY_LEVELS_TAG = "INVENTORY_LEVELS";


  public final static String TASKS_TYPE = "TASKS";
  public final static String PROJ_TASKS_TYPE = "PROJTASKS";
  public final static String ARS_TYPE = "ARS";
  public final static String PROJ_ARS_TYPE = "PROJ_ARS";
  public final static String LEVELS_TYPE = "LEVELS";

  public final static String AR_SUCCESS_STR = "SUCCESS";
  public final static String AR_FAILURE_STR = "FAIL";
  public final static String AR_ESTIMATED_STR = "ESTIMATED";
  public final static String AR_REPORTED_STR = "REPORTED";

  public final static String SUBSISTENCE_SUPPLY_TYPE = "Subsistence";
  public final static String BULK_POL_SUPPLY_TYPE = "BulkPOL";
  public final static String PACKAGED_POL_SUPPLY_TYPE = "PackagedPOL";
  public final static String AMMUNITION_SUPPLY_TYPE = "Ammunition";
  public final static String CONSUMABLE_SUPPLY_TYPE = "Consumable";
  public final static String WATER_ITEM_ID = "NSN/8960013687383";

  public final static String SUBSISTENCE_UNIT = "Units of Issue";
  public final static String BULK_POL_UNIT = "Gallons";
  public final static String PACKAGED_POL_UNIT = "Packages";
  public final static String AMMUNITION_UNIT = "Tons";
  public final static String CONSUMABLE_UNIT = "Packages";
  public final static String WATER_UNIT = "Cases";


  protected LoggingService logger;
  protected TaskUtils taskUtils;
  protected long cycleStamp = -0L;
  protected Writer output;

  protected Date startCDay;

  protected LogisticsInventoryPG logInvPG;

  public LogisticsInventoryFormatter(Writer writeOutput, Date startingCDay, InventoryPlugin invPlugin) {
    logger = invPlugin.getLoggingService(this);
    output = writeOutput;
    taskUtils = invPlugin.getTaskUtils();
    startCDay = startingCDay;
  }

  public LogisticsInventoryFormatter(Writer writeOutput, LoggingService aLogger, Date startingCDay) {
    logger = aLogger;
    output = writeOutput;
    taskUtils = new TaskUtils(aLogger);
    startCDay = startingCDay;
  }

  protected static String buildTaskPrefixString(Task aTask) {
    String taskStr = aTask.getParentTaskUID() + ",";
    taskStr = taskStr + aTask.getUID() + ",";
    taskStr = taskStr + aTask.getVerb() + ",";
    PrepositionalPhrase pp_for = aTask.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp_for != null) {
      Object org;
      org = pp_for.getIndirectObject();
      taskStr = taskStr + org + ",";
    } else {
      taskStr = taskStr + ",";
    }
    return taskStr;
  }

  protected void logTasks(ArrayList tasks,
                          long aCycleStamp,
                          boolean expandTimestamp,
                          boolean isCountedTask) {
    cycleStamp = aCycleStamp;
    for (int i = 0; i < tasks.size(); i++) {
      Task aTask = (Task) tasks.get(i);
      logTask(aTask, expandTimestamp, isCountedTask);
    }
  }

  protected void logTask(Task aTask, boolean expandTimestamp, boolean isCountedTask) {
    if (aTask == null) {
      return;
    }
    String taskStr = buildTaskPrefixString(aTask);
    long startTime = -1;
    if (TaskUtils.getPreferenceBest(aTask, AspectType.START_TIME) == null) {
      taskStr += ",";
    } else {
      startTime = TaskUtils.getStartTime(aTask);
      if (isCountedTask &&
          (TaskUtils.isProjection(aTask))) {
        startTime = logInvPG.getEffectiveProjectionStart(aTask, startTime);
      }
      taskStr = taskStr + getDateString(startTime, expandTimestamp) + ",";
    }
    taskStr = taskStr + getDateString(TaskUtils.getEndTime(aTask), expandTimestamp) + ",";
    try {
      //This is qty for supply, daily rate for projection
      double dQuantity = taskUtils.getDailyQuantity(aTask);
      if (!expandTimestamp) {
        long dbits = Double.doubleToLongBits(dQuantity);
        taskStr = taskStr + Long.toHexString(dbits);
      } else {
        String testStr = taskStr + dQuantity;
        taskStr = testStr;
      }
    } catch (ClassCastException e) {
      logger.error("Formatter:Problem task and the print out is :\n" + taskUtils.taskDesc(aTask));
      throw e;
    } catch (RuntimeException re) {
      if (re.getMessage().indexOf("Assertion botch:") > -1) {
        taskStr = taskStr + "Assertion botch!";

        String itemId = taskUtils.getTaskItemName(aTask);
        TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
        String nomenclature = null;
        String orgId = logInvPG.getOrg().getItemIdentificationPG().getItemIdentification();
        if (typeIdPG == null) {
          logger.warn("No typeIdentificationPG for asset");
        } else {
          nomenclature = typeIdPG.getNomenclature();
        }
        if (nomenclature == null) {
          nomenclature = itemId;
        }
        String errorString = "Assertion Botch: start time: " + getDateString(startTime, true);
        errorString += (" end time: " + getDateString(TaskUtils.getEndTime(aTask), true));
        errorString += (" UID: " + aTask.getUID());
        errorString += (" NSN: " + itemId + ":" + nomenclature);
        errorString += (" at Org " + orgId + " ");
        double dQuantity = taskUtils.getDailyQuantity(aTask);
        long dbits = Double.doubleToLongBits(dQuantity);
        long rbits = Double.doubleToRawLongBits(dQuantity);
        errorString += ("quantity_d: " + Long.toHexString(dbits));
        errorString += ("quantity_r: " + Long.toHexString(rbits));
        logger.error(errorString, re);
      } else {
        throw re;  // re-throw for now
      }
    }

    writeln(taskStr);
  }

  protected void logAllocationResults(ArrayList tasks, long aCycleStamp, boolean expandTimestamp, boolean isCountedAR) {
    cycleStamp = aCycleStamp;
    for (int i = 0; i < tasks.size(); i++) {
      Task aTask = (Task) tasks.get(i);
      logAllocationResult(aTask, expandTimestamp, isCountedAR);
    }
  }

  protected void logAllocationResult(Task aTask, boolean expandTimestamp, boolean isCountedAR) {
    if (aTask == null) {
      return;
    }
    PlanElement pe = aTask.getPlanElement();
    if (pe == null) {
      return;
    }
    String resultType = AR_REPORTED_STR;
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      resultType = AR_ESTIMATED_STR;
      ar = pe.getEstimatedResult();
    }
    if (ar != null) {
      String taskStr = buildTaskPrefixString(aTask);
      taskStr = taskStr + resultType + ",";
      if (ar.isSuccess()) {
        taskStr = taskStr + AR_SUCCESS_STR + ",";
      } else {
        taskStr = taskStr + AR_FAILURE_STR + ",";
      }
      if (!ar.isPhased()) {
        long endTime = (long) TaskUtils.getEndTime(ar);
        String endTimeString = getDateString(endTime, expandTimestamp);
        long startTime = 0;
        String outputStr = null;

        if (TaskUtils.isProjection(aTask)) {
          startTime = (long) TaskUtils.getStartTime(ar);
          if (isCountedAR) {
            startTime = logInvPG.getEffectiveProjectionStart(aTask, startTime);
            if (startTime >= endTime) {
              return;
            }
          }
          outputStr = taskStr + getDateString(startTime, expandTimestamp) + ",";
          outputStr = outputStr + endTimeString + ",";
        } else {
          //If supply or withdraw task ignore the start date
          outputStr = taskStr + "," + endTimeString + ",";
        }

        // extra debugging if endTime is 0, or getDateString end time is "0"
        if (((endTime <= 0) || endTimeString.equals("0")) && (logger.isWarnEnabled())) {
          logger.warn("For Org " + getOrgName() + "-" + getSupplyType() +
                      " item: " + taskUtils.getTaskItemName(aTask) +
                      " - " + getInvNomenclatureName() + " the endTime (" +
                      endTime + ") for the " +
                      "non-phased allocation result prints out to " + endTimeString);
        }

        try {
          double dQuantity = taskUtils.getQuantity(aTask, ar);
          if (!expandTimestamp) {
            long dbits = Double.doubleToLongBits(dQuantity);
            outputStr = outputStr + Long.toHexString(dbits);
          } else {
            String testStr = outputStr + dQuantity;
            outputStr = testStr;
          }
        } catch (RuntimeException re) {
          if (re.getMessage().indexOf("Assertion botch:") > -1) {
            outputStr = outputStr + "Assertion botch!";
            String itemId = taskUtils.getTaskItemName(aTask);
            TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
            String nomenclature = null;
            String orgId = logInvPG.getOrg().getItemIdentificationPG().getItemIdentification();
            if (typeIdPG == null) {
              logger.warn("No typeIdentificationPG for asset");
            } else {
              nomenclature = typeIdPG.getNomenclature();
            }
            if (nomenclature == null) {
              nomenclature = itemId;
            }
            String errorString = "Assertion Botch on non-phased AllocationResult: start time: " + getDateString(startTime, true);
            errorString += (" end time: " + getDateString(endTime, true));
            errorString += (" UID: " + aTask.getUID());
            errorString += (" NSN: " + itemId + ":" + nomenclature);
            errorString += (" at Org " + orgId + " ");
            double dQuantity = taskUtils.getQuantity(aTask, ar);
            long dbits = Double.doubleToLongBits(dQuantity);
            long rbits = Double.doubleToRawLongBits(dQuantity);
            errorString += (" quantity_d: " + Long.toHexString(dbits));
            errorString += (" quantity_r: " + Long.toHexString(rbits));
            logger.error(errorString, re);
          } else {
            throw re;
          }
        }
        writeln(outputStr);
      } else {
        int[] ats = ar.getAspectTypes();
        int qtyInd = -1;
        if (taskUtils.isProjection(aTask)) {
          qtyInd = getIndexForType(ats, AlpineAspectType.DEMANDRATE);
        } else {
          qtyInd = getIndexForType(ats, AspectType.QUANTITY);
        }
        int startInd = getIndexForType(ats, AspectType.START_TIME);
        int endInd = getIndexForType(ats, AspectType.END_TIME);
        Enumeration phasedResults = ar.getPhasedResults();
        while (phasedResults.hasMoreElements()) {
          double[] results = (double[]) phasedResults.nextElement();
          String outputStr = taskStr;
          long startTime = 0;
          long endTime;
          if (startInd == -1) {
            outputStr += ",";
          } else {
            startTime = (long) results[startInd];
            if (isCountedAR &&
                (TaskUtils.isProjection(aTask))) {
              startTime = logInvPG.getEffectiveProjectionStart(aTask, startTime);
            }
            outputStr += getDateString(startTime, expandTimestamp) + ",";
          }
          if (endInd == -1) {
            /** MWD The following line of code which replaces the
             *  allocation result end time with the task end time
             *  is only due to a current error in the
             *  UniversalAllocator.   The UA is only setting
             *  the start time in the allocation result. There
             *  is a bug in and when the UA is fixed this line
             *  of code should be removed. If there is no end time
             *  in the allocation result, none should be appended.
             *  GUI needs the end
             *  times for the rates.
             */
            endTime = TaskUtils.getEndTime(aTask);
            outputStr += getDateString(endTime, expandTimestamp);
            outputStr += ",";
          } else {
            endTime = (long) results[endInd];
            outputStr += getDateString(endTime, expandTimestamp) + ",";
          }
          if ((qtyInd >= results.length) ||
              (qtyInd < 0)) {
            logger.error("qtyInd is " + qtyInd + " - No Qty in this phase of allocation results: " + outputStr);
          } else {
            double dailyRate = taskUtils.convertResultsToDailyRate(aTask, results[qtyInd]);
            try {
              if (!expandTimestamp) {
                long dbits = Double.doubleToLongBits(dailyRate);
                outputStr = outputStr + Long.toHexString(dbits);
              } else {
                String testStr = outputStr + dailyRate;
                outputStr = testStr;
              }
            } catch (RuntimeException re) {
              if (re.getMessage().indexOf("Assertion botch:") > -1) {
                outputStr = outputStr + "Assertion botch!";
                String itemId = taskUtils.getTaskItemName(aTask);
                TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
                String nomenclature = null;
                String orgId = logInvPG.getOrg().getItemIdentificationPG().getItemIdentification();
                if (typeIdPG == null) {
                  logger.warn("No typeIdentificationPG for asset");
                } else {
                  nomenclature = typeIdPG.getNomenclature();
                }
                if (nomenclature == null) {
                  nomenclature = itemId;
                }
                String errorString = "Assertion Botch on phased AllocationResult: start time: " + getDateString(startTime, true);
                errorString += (" end time: " + getDateString(endTime, true));
                errorString += (" UID: " + aTask.getUID());
                errorString += (" NSN: " + itemId + ":" + nomenclature);
                errorString += (" at Org " + orgId + " ");
                long dbits = Double.doubleToLongBits(dailyRate);
                long rbits = Double.doubleToRawLongBits(dailyRate);
                errorString += (" quantity_d: " + Long.toHexString(dbits));
                errorString += (" quantity_r: " + Long.toHexString(rbits));
                logger.error(errorString, re);
              } else {
                throw re;
              }
            }
            if (startTime <= endTime) {
              writeln(outputStr);
            } else if (!isCountedAR) {
              logger.warn("logAllocationResult: not going to log an allocation result where endTime is less than start time.  CSV line is is:" + outputStr);
            }
          }
        }
      }
    }
  }


  protected void logLevels(Schedule reorderLevels,
                           Schedule inventoryLevels,
                           Schedule targetLevels,
                           TimeSpanSet orgActivities,
                           long aCycleStamp,
                           boolean expandTimestamp) {
    cycleStamp = aCycleStamp;
    Enumeration e = reorderLevels.getAllScheduleElements();
    while (e.hasMoreElements()) {
      QuantityScheduleElement qse = (QuantityScheduleElement) e.nextElement();
      Collection invLevelsInRange =
          inventoryLevels.getEncapsulatedScheduleElements(qse.getStartTime(), qse.getEndTime());
      Collection targetLevelsInRange =
          targetLevels.getEncapsulatedScheduleElements(qse.getStartTime(), qse.getEndTime());
      Collection orgActsInRange =
          orgActivities.intersectingSet(qse.getStartTime(), qse.getEndTime());
      logLevels(qse, invLevelsInRange, targetLevelsInRange, orgActsInRange, expandTimestamp);
    }
  }

  protected void logLevels(QuantityScheduleElement reorderLevel,
                           Collection invLevelsInRange,
                           Collection targetLevelsInRange,
                           Collection orgActsInRange,
                           boolean expandTimestamp) {

    String outputStr = getDateString(reorderLevel.getStartTime(), expandTimestamp) + ",";
    outputStr += getDateString(reorderLevel.getEndTime(), expandTimestamp) + ",";
    outputStr += reorderLevel.getQuantity() + ",";
    if (invLevelsInRange.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("logLevel:no inventory level in range " + outputStr);
      }
      outputStr += ",";
    } else {
      Iterator it = invLevelsInRange.iterator();
      boolean moreThanOne = false;
      boolean alreadyLogged = false;
      while (it.hasNext()) {
        QuantityScheduleElement invLevel = (QuantityScheduleElement) it.next();
        outputStr = outputStr + invLevel.getQuantity() + ",";
        if (moreThanOne && !alreadyLogged) {
          if (logger.isWarnEnabled()) {
            logger.warn("logLevel:More than one inventory level in range " + outputStr);
          }
          alreadyLogged = true;
        }
        moreThanOne = true;
      }
      if (!targetLevelsInRange.isEmpty()) {
        moreThanOne = false;
        alreadyLogged = false;
        it = targetLevelsInRange.iterator();
        while (it.hasNext()) {
          QuantityScheduleElement targetLevel = (QuantityScheduleElement) it.next();
          outputStr += targetLevel.getQuantity();
          if (moreThanOne && !alreadyLogged) {
            if (logger.isWarnEnabled()) {
              logger.warn("logLevel:More than one target level in range " + outputStr);
            }
            alreadyLogged = true;
          }
          moreThanOne = true;
        }
      }

      if (!orgActsInRange.isEmpty()) {
        moreThanOne = false;
        alreadyLogged = false;
        it = orgActsInRange.iterator();
        while (it.hasNext()) {
          OrgActivity orgAct = (OrgActivity) it.next();
          outputStr += "," + orgAct.getActivityType() + "," + orgAct.getOpTempo();
          if (moreThanOne && !alreadyLogged) {
            if (logger.isWarnEnabled()) {
              logger.warn("logLevel:More than one org Activity in range " + outputStr);
            }
            alreadyLogged = true;
          }
          moreThanOne = true;
        }
      }
      writeln(outputStr);
    }
  }

  protected static int getIndexForType(int[] types, int type) {
    for (int ii = 0; ii < types.length; ii++) {
      if (types[ii] == type) {
        return ii;
      }
    }
    return -1;
  }

  protected static String getDateString(double datetime, boolean expandTimestamp) {
    return getDateString(Math.round(datetime), expandTimestamp);
  }

  protected static String getDateString(long datetime, boolean expandTimestamp) {
    if (expandTimestamp) {
      return TimeUtils.dateString(datetime);
    } else {
      return Long.toString(datetime);
    }
  }

  protected void excelLogLevels(Schedule reorderLevels,
                                Schedule inventoryLevels,
                                Schedule targetLevels,
                                long aCycleStamp) {
    writeNoCycleLn("CYCLE,START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL, TARGET LEVEL");
    logLevels(reorderLevels, inventoryLevels, targetLevels, new TimeSpanSet(), aCycleStamp, true);
  }


  protected void excelLogProjections(ArrayList tasks, boolean isCountedTask, long aCycleStamp) {
    writeNoCycleLn("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,DAILY RATE");
    logTasks(tasks, aCycleStamp, true, isCountedTask);
  }


  protected void excelLogNonProjections(ArrayList tasks, long aCycleStamp) {
    writeNoCycleLn("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,QTY");
    logTasks(tasks, aCycleStamp, true, false);
  }


  protected void excelLogARs(ArrayList tasks, boolean isCountedAR, long aCycleStamp) {
    writeNoCycleLn("CYCLE,PARENT UID,TASK UID,TASK VERB,TASK FOR(ORG),AR TYPE,AR SUCCESS,AR START TIME,AR END TIME,AR QTY");
    logAllocationResults(tasks, aCycleStamp, true, isCountedAR);
  }


  protected void xmlLogLevels(Schedule reorderLevels,
                              Schedule inventoryLevels,
                              Schedule targetLevels,
                              TimeSpanSet orgActivities,
                              boolean humanReadable,
                              long aCycleStamp) {
    if (humanReadable) {
      writeNoCycleLn("<START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL,TARGET LEVEL,ACTIVITY TYPE, OPTEMPO>");
    }
    logLevels(reorderLevels, inventoryLevels, targetLevels, orgActivities, aCycleStamp, humanReadable);
  }


  protected void xmlLogProjections(ArrayList tasks, boolean isCountedTask, boolean humanReadable, long aCycleStamp) {
    if (humanReadable) {
      writeNoCycleLn("<PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,DAILY RATE>");
    }
    logTasks(tasks, aCycleStamp, humanReadable, isCountedTask);
  }


  protected void xmlLogNonProjections(ArrayList tasks,
                                      boolean humanReadable,
                                      long aCycleStamp) {
    if (humanReadable) {
      writeNoCycleLn("<PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,QTY>");
    }
    logTasks(tasks, aCycleStamp, humanReadable, false);
  }


  protected void xmlLogProjectionARs(ArrayList tasks,
                                     boolean isCountedAR,
                                     boolean humanReadable,
                                     long aCycleStamp) {
    if (humanReadable) {
      writeNoCycleLn("<PARENT UID,TASK UID,TASK VERB,TASK FOR(ORG),AR TYPE,AR SUCCESS,AR START TIME,AR END TIME,AR DAILY RATE>");
    }
    logAllocationResults(tasks, aCycleStamp, humanReadable, isCountedAR);
  }


  protected void xmlLogNonProjectionARs(ArrayList tasks,
                                        boolean humanReadable,
                                        long aCycleStamp) {
    if (humanReadable) {
      writeNoCycleLn("<PARENT UID,TASK UID,TASK VERB,TASK FOR(ORG),AR TYPE,AR SUCCESS,AR START TIME,AR END TIME,AR QTY>");
    }
    logAllocationResults(tasks, aCycleStamp, humanReadable, false);
  }

  protected ArrayList buildParentTaskArrayList(ArrayList tasks) {
    ArrayList parentList = new ArrayList(tasks.size());
    for (int i = 0; i < tasks.size(); i++) {
      Task parentTask = null;
      if (((Task)tasks.get(i)).getWorkflow() != null)
	parentTask = ((Task) tasks.get(i)).getWorkflow().getParentTask();
      if (parentTask != null) {
        parentList.add(parentTask);
      } else {
        logger.error("Problem deriving parent task from task " + tasks.get(i));
      }
    }
    return parentList;
  }

  /***
   ** This method flattens the 2 dimensional array list into one
   ** dimension and weeds out PROJECTWITHDRAWs.
   **
   **/
  protected ArrayList extractProjFromCounted(ArrayList countedDemandList) {
    ArrayList countedProjDemand = new ArrayList();
    for (int i = 0; i < countedDemandList.size(); i++) {
      ArrayList bucketODemand = (ArrayList) countedDemandList.get(i);
      for (int j = 0; j < bucketODemand.size(); j++) {
        Task aTask = (Task) bucketODemand.get(j);
        if (aTask.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
          if (!(countedProjDemand.contains(aTask))) {
            countedProjDemand.add(aTask);
          }
        }
      }
    }
    return countedProjDemand;
  }


  public void logToExcelOutput(LogisticsInventoryPG inv,
                               long aCycleStamp) {

    logInvPG = inv;
    if (logInvPG != null) {
      logToExcelOutput(logInvPG.getWithdrawList(),
                       logInvPG.getProjWithdrawList(),
                       logInvPG.getActualDemandTasksList(),
                       logInvPG.getSupplyList(),
                       logInvPG.getProjSupplyList(),
                       logInvPG.getBufferedCritLevels(),
                       logInvPG.getBufferedInvLevels(),
                       logInvPG.getBufferedTargetLevels(),
                       aCycleStamp);
    }
  }

  protected void logToExcelOutput(ArrayList withdrawList,
                                  ArrayList projWithdrawList,
                                  ArrayList countedDemandList,
                                  ArrayList resupplyList,
                                  ArrayList projResupplyList,
                                  Schedule reorderLevels,
                                  Schedule inventoryLevels,
                                  Schedule targetLevels,
                                  long aCycleStamp) {
    ArrayList countedProjWithdraw = extractProjFromCounted(countedDemandList);
    logDemandToExcelOutput(withdrawList, projWithdrawList, countedProjWithdraw, aCycleStamp);
    logResupplyToExcelOutput(resupplyList, projResupplyList, aCycleStamp);
    logLevelsToExcelOutput(reorderLevels, inventoryLevels, targetLevels, aCycleStamp);

    try {
      output.flush();
    } catch (IOException e) {
      logger.error("Exception when trying to flush excel stream." + e.toString());
    }
  }

  protected void logDemandToExcelOutput(ArrayList withdrawList,
                                        ArrayList projWithdrawList,
                                        ArrayList countedProjWithdrawList,
                                        long aCycleStamp) {
    cycleStamp = aCycleStamp;

    ArrayList supplyList = buildParentTaskArrayList(withdrawList);
    ArrayList projSupplyList = buildParentTaskArrayList(projWithdrawList);

    writeNoCycleLn("SUPPLY TASKS:START");
    logger.debug("SUPPLY TASKS:START");
    excelLogNonProjections(supplyList, aCycleStamp);
    writeNoCycleLn("SUPPLY TASKS:END");
    writeNoCycleLn("WITHDRAW TASKS:START");
    logger.debug("WITHDRAW TASKS:START");
    excelLogNonProjections(withdrawList, aCycleStamp);
    writeNoCycleLn("WITHDRAW TASKS:END");
    writeNoCycleLn("PROJECTSUPPLY TASKS:START");
    logger.debug("PROJECTSUPPLY TASKS:START");
    excelLogProjections(projSupplyList, false, aCycleStamp);
    writeNoCycleLn("PROJECTSUPPLY TASKS:END");
    writeNoCycleLn("PROJECTWITHDRAW TASKS:START");
    logger.debug("PROJECTWITHDRAW TASKS:START");
    excelLogProjections(projWithdrawList, false, aCycleStamp);
    writeNoCycleLn("PROJECTWITHDRAW TASKS:END");
    writeNoCycleLn("COUNTED PROJECTWITHDRAW TASKS:START");
    logger.debug("COUNTED PROJECTWITHDRAW TASKS:START");
    excelLogProjections(countedProjWithdrawList, true, aCycleStamp);
    writeNoCycleLn("COUNTED PROJECTWITHDRAW TASKS:END");

    writeNoCycleLn("SUPPLY TASK ALLOCATION RESULTS :START");
    logger.debug("SUPPLY TASK ALLOCATION RESULTS :START");
    excelLogARs(supplyList, false, aCycleStamp);
    writeNoCycleLn("SUPPLY TASK ALLOCATION RESULTS :END");
    writeNoCycleLn("WITHDRAW TASK ALLOCATION RESULTS :START");
    logger.debug("WITHDRAW TASK ALLOCATION RESULTS :START");
    excelLogARs(withdrawList, false, aCycleStamp);
    writeNoCycleLn("WITHDRAW TASK ALLOCATION RESULTS :END");
    writeNoCycleLn("PROJECTSUPPLY TASK ALLOCATION RESULTS :START");
    logger.debug("PROJECTSUPPLY TASK ALLOCATION RESULTS :START");
    excelLogARs(projSupplyList, false, aCycleStamp);
    writeNoCycleLn("PROJECTSUPPLY TASK ALLOCATION RESULTS :END");
    writeNoCycleLn("PROJECTWITHDRAW TASK ALLOCATION RESULTS :START");
    logger.debug("PROJECTWITHDRAW TASK ALLOCATION RESULTS :START");
    excelLogARs(projWithdrawList, false, aCycleStamp);
    writeNoCycleLn("PROJECTWITHDRAW TASK ALLOCATION RESULTS :END");
    writeNoCycleLn("COUNTED PROJECTWITHDRAW TASK ALLOCATION RESULTS :START");
    logger.debug("COUNTED PROJECTWITHDRAW TASK ALLOCATION RESULTS :START");
    excelLogARs(countedProjWithdrawList, true, aCycleStamp);
    writeNoCycleLn("COUNTED PROJECTWITHDRAW TASK ALLOCATION RESULTS :END");

  }

  protected void logResupplyToExcelOutput(ArrayList resupplyList,
                                          ArrayList projResupplyList,
                                          long aCycleStamp) {
    cycleStamp = aCycleStamp;

    writeNoCycleLn("RESUPPLY SUPPLY TASKS: START");
    excelLogNonProjections(resupplyList, aCycleStamp);
    writeNoCycleLn("RESUPPLY SUPPLY TASKS: END");
    writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASKS: START");
    excelLogProjections(projResupplyList, false, aCycleStamp);
    writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASKS: END");

    writeNoCycleLn("RESUPPLY SUPPLY TASK ALLOCATION RESULTS: START");
    excelLogARs(resupplyList, false, aCycleStamp);
    writeNoCycleLn("RESUPPLY SUPPLY TASK ALLOCATION RESULTS: END");
    writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS: START");
    excelLogARs(projResupplyList, false, aCycleStamp);
    writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS: END");

  }

  protected void logLevelsToExcelOutput(Schedule reorderLevels,
                                        Schedule inventoryLevels,
                                        Schedule targetLevels,
                                        long aCycleStamp) {

    writeNoCycleLn("INVENTORY LEVELS: START");
    excelLogLevels(reorderLevels, inventoryLevels, targetLevels, aCycleStamp);
    writeNoCycleLn("INVENTORY LEVELS: END");
  }

  protected void logHeaderStartToXMLOutput(Inventory invAsset,
                                           Organization anOrg,
                                           boolean level2Inv,
                                           boolean humanReadable) {

    String orgId = anOrg.getItemIdentificationPG().getItemIdentification();

    String itemId = invAsset.getItemIdentificationPG().getItemIdentification();
    int idx = itemId.indexOf(':');
    itemId = itemId.substring(idx + 1);


    String header;

    if (humanReadable) {
      header = "<" + INVENTORY_HEADER_PERSON_READABLE_TAG + " org=" + orgId + " item=" + itemId;
    } else {
      header = "<" + INVENTORY_HEADER_GUI_TAG + " org=" + orgId + " item=" + itemId;
    }


    header = header + " unit=" + getUnitForAsset(invAsset);

    TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
    if (typeIdPG == null) {
      logger.warn("No typeIdentificationPG for asset");
    }

    String nomenclature = typeIdPG.getNomenclature();

    if (nomenclature == null) {
      nomenclature = itemId;

    }
    if (level2Inv) {
      //SupplyClassPG supplyPG = (SupplyClassPG) logInvPG.getResource().searchForPropertyGroup(SupplyClassPG.class);
      //if (supplyPG != null) {
      //nomenclature = supplyPG.getSupplyType();
      //}
      nomenclature = "";
    }

    header = header + " nomenclature=" + nomenclature;

    if (humanReadable) {
      header = header + " cDay=" + (new Date(startCDay.getTime())) + ">";
    } else {
      header = header + " cDay=" + startCDay.getTime() + ">";
    }

    writeNoCycleLn(header);
  }

  protected void logHeaderEndToXMLOutput(boolean humanReadable) {
    if (humanReadable) {
      writeNoCycleLn("</" + INVENTORY_HEADER_PERSON_READABLE_TAG + ">");
    } else {
      writeNoCycleLn("</" + INVENTORY_HEADER_GUI_TAG + ">");
    }
  }

  protected void logToXMLOutput(Inventory invAsset,
                                TimeSpanSet orgActivities,
                                Organization anOrg,
                                ArrayList withdrawList,
                                ArrayList projWithdrawList,
                                ArrayList countedDemandList,
                                ArrayList resupplyList,
                                ArrayList projResupplyList,
                                Schedule reorderLevels,
                                Schedule inventoryLevels,
                                Schedule targetLevels,
                                boolean level2Inv,
                                boolean humanReadable,
                                long aCycleStamp) {
    cycleStamp = aCycleStamp;

    logHeaderStartToXMLOutput(invAsset, anOrg, level2Inv, humanReadable);

    ArrayList countedProjWithdrawList = extractProjFromCounted(countedDemandList);
    logDemandToXMLOutput(withdrawList, projWithdrawList, countedProjWithdrawList, humanReadable, aCycleStamp);
    logResupplyToXMLOutput(resupplyList, projResupplyList, humanReadable, aCycleStamp);
    logLevelsToXMLOutput(reorderLevels, inventoryLevels, targetLevels, orgActivities, humanReadable, aCycleStamp);
    logHeaderEndToXMLOutput(humanReadable);
  }


  protected void logToXMLOutput(Inventory invAsset,
                                TimeSpanSet orgActivities,
                                Organization anOrg,
                                ArrayList withdrawList,
                                ArrayList projWithdrawList,
                                ArrayList countedDemandList,
                                ArrayList resupplyList,
                                ArrayList projResupplyList,
                                Schedule reorderLevels,
                                Schedule inventoryLevels,
                                Schedule targetLevels,
                                boolean level2Inv,
                                long aCycleStamp) {
    cycleStamp = aCycleStamp;
    writeNoCycleLn("<" + INVENTORY_DUMP_TAG + ">");
    logToXMLOutput(invAsset, orgActivities, anOrg, withdrawList,
                   projWithdrawList, countedDemandList,
                   resupplyList, projResupplyList,
                   reorderLevels, inventoryLevels, targetLevels,
                   level2Inv, true, aCycleStamp);
    logToXMLOutput(invAsset, orgActivities, anOrg, withdrawList,
                   projWithdrawList, countedDemandList,
                   resupplyList, projResupplyList,
                   reorderLevels, inventoryLevels, targetLevels,
                   level2Inv, false, aCycleStamp);
    writeNoCycleLn("</" + INVENTORY_DUMP_TAG + ">");
    try {
      output.flush();
    } catch (IOException e) {
      logger.error("Exception when trying to flush xml stream." + e.toString());
    }
  }

  public void logToXMLOutput(Inventory inv,
                             TimeSpanSet orgActivities,
                             long aCycleStamp) {

    logInvPG = null;
    logInvPG = (LogisticsInventoryPG) inv.searchForPropertyGroup(LogisticsInventoryPG.class);
    if (logInvPG != null) {
      logToXMLOutput(inv,
                     orgActivities,
                     logInvPG.getOrg(),
                     logInvPG.getWithdrawList(),
                     logInvPG.getProjWithdrawList(),
                     logInvPG.getActualDemandTasksList(),
                     logInvPG.getSupplyList(),
                     logInvPG.getProjSupplyList(),
                     logInvPG.getBufferedCritLevels(),
                     logInvPG.getBufferedInvLevels(),
                     logInvPG.getBufferedTargetLevels(),
                     logInvPG.getIsLevel2(),
                     aCycleStamp);
    }
  }

  protected void logDemandToXMLOutput(ArrayList withdrawList,
                                      ArrayList projWithdrawList,
                                      ArrayList countedProjWithdrawList,
                                      boolean humanReadable,
                                      long aCycleStamp) {

    ArrayList supplyList = buildParentTaskArrayList(withdrawList);
    ArrayList projSupplyList = buildParentTaskArrayList(projWithdrawList);

    writeNoCycleLn("<" + SUPPLY_TASKS_TAG + " type=TASKS>");
    xmlLogNonProjections(supplyList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + SUPPLY_TASKS_TAG + ">");
    writeNoCycleLn("<" + WITHDRAW_TASKS_TAG + " type=TASKS>");
    xmlLogNonProjections(withdrawList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + WITHDRAW_TASKS_TAG + ">");
    writeNoCycleLn("<" + PROJECTSUPPLY_TASKS_TAG + " type=PROJTASKS>");
    xmlLogProjections(projSupplyList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + PROJECTSUPPLY_TASKS_TAG + ">");
    writeNoCycleLn("<" + PROJECTWITHDRAW_TASKS_TAG + " type=PROJTASKS>");
    xmlLogProjections(projWithdrawList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + PROJECTWITHDRAW_TASKS_TAG + ">");
    writeNoCycleLn("<" + COUNTED_PROJECTWITHDRAW_TASKS_TAG + " type=PROJTASKS>");
    xmlLogProjections(countedProjWithdrawList, true, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + COUNTED_PROJECTWITHDRAW_TASKS_TAG + ">");

    writeNoCycleLn("<" + SUPPLY_TASK_ARS_TAG + " type=ARS>");
    xmlLogNonProjectionARs(supplyList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + SUPPLY_TASK_ARS_TAG + ">");
    writeNoCycleLn("<" + WITHDRAW_TASK_ARS_TAG + " type=ARS>");
    xmlLogNonProjectionARs(withdrawList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + WITHDRAW_TASK_ARS_TAG + ">");
    writeNoCycleLn("<" + PROJECTSUPPLY_TASK_ARS_TAG + " type=PROJ_ARS>");
    xmlLogProjectionARs(projSupplyList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + PROJECTSUPPLY_TASK_ARS_TAG + ">");
    writeNoCycleLn("<" + PROJECTWITHDRAW_TASK_ARS_TAG + " type=PROJ_ARS>");
    xmlLogProjectionARs(projWithdrawList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + PROJECTWITHDRAW_TASK_ARS_TAG + ">");
    writeNoCycleLn("<" + COUNTED_PROJECTWITHDRAW_TASK_ARS_TAG + " type=PROJ_ARS>");
    xmlLogProjectionARs(countedProjWithdrawList, true, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + COUNTED_PROJECTWITHDRAW_TASK_ARS_TAG + ">");

  }

  protected void logResupplyToXMLOutput(ArrayList resupplyList,
                                        ArrayList projResupplyList,
                                        boolean humanReadable,
                                        long aCycleStamp) {
    cycleStamp = aCycleStamp;

    writeNoCycleLn("<" + RESUPPLY_SUPPLY_TASKS_TAG + " type=TASKS>");
    xmlLogNonProjections(resupplyList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + RESUPPLY_SUPPLY_TASKS_TAG + ">");
    writeNoCycleLn("<" + RESUPPLY_PROJECTSUPPLY_TASKS_TAG + " type=PROJTASKS>");
    xmlLogProjections(projResupplyList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + RESUPPLY_PROJECTSUPPLY_TASKS_TAG + ">");

    writeNoCycleLn("<" + RESUPPLY_SUPPLY_TASK_ARS_TAG + " type=ARS>");
    xmlLogNonProjectionARs(resupplyList, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + RESUPPLY_SUPPLY_TASK_ARS_TAG + ">");
    writeNoCycleLn("<" + RESUPPLY_PROJECTSUPPLY_TASK_ARS_TAG + " type=PROJ_ARS>");
    xmlLogProjectionARs(projResupplyList, false, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + RESUPPLY_PROJECTSUPPLY_TASK_ARS_TAG + ">");

  }

  protected void logLevelsToXMLOutput(Schedule reorderLevels,
                                      Schedule inventoryLevels,
                                      Schedule targetLevels,
                                      TimeSpanSet orgActivities,
                                      boolean humanReadable,
                                      long aCycleStamp) {
    writeNoCycleLn("<" + INVENTORY_LEVELS_TAG + " type=LEVELS>");
    xmlLogLevels(reorderLevels, inventoryLevels, targetLevels, orgActivities, humanReadable, aCycleStamp);
    writeNoCycleLn("</" + INVENTORY_LEVELS_TAG + ">");
  }

  public void writeln(String csvString) {
    writeNoCycleLn(cycleStamp + "," + csvString);
  }

  public void writeNoCycleLn(String aString) {
    if (output != null) {
      try {
        output.write(aString + "\n");
      } catch (IOException e) {
        logger.error("Exception trying to write to Writer: " + output, e);
      }
    }
  }

  public static String getUnitForAsset(Inventory invAsset) {


    LogisticsInventoryPG logInvPG = (LogisticsInventoryPG) invAsset.searchForPropertyGroup(LogisticsInventoryPG.class);

    SupplyClassPG pg = (SupplyClassPG) logInvPG.getResource().searchForPropertyGroup(SupplyClassPG.class);
    String supplyType = pg.getSupplyType();

    if (supplyType.equals(SUBSISTENCE_SUPPLY_TYPE)) {
      String itemId = invAsset.getItemIdentificationPG().getItemIdentification();
      int idx = itemId.indexOf(':');
      itemId = itemId.substring(idx + 1);
      if (itemId.equals(WATER_ITEM_ID)) {
        return WATER_UNIT;
      } else {
        return SUBSISTENCE_UNIT;
      }
    } else if (supplyType.equals(BULK_POL_SUPPLY_TYPE)) {
      return BULK_POL_UNIT;
    } else if (supplyType.equals(PACKAGED_POL_SUPPLY_TYPE)) {
      return PACKAGED_POL_UNIT;
    } else if (supplyType.equals(AMMUNITION_SUPPLY_TYPE)) {
      return AMMUNITION_UNIT;
    } else if (supplyType.equals(CONSUMABLE_SUPPLY_TYPE)) {
      return CONSUMABLE_UNIT;
    }

    return "UNKNOWN UNIT";
  }

  public String getSupplyType() {
    SupplyClassPG supplyPG = (SupplyClassPG) logInvPG.getResource().searchForPropertyGroup(SupplyClassPG.class);
    if (supplyPG != null) {
      return supplyPG.getSupplyType();
    }
    return null;
  }

  public String getOrgName() {
    return (logInvPG.getOrg().getItemIdentificationPG().getItemIdentification());
  }

  public String getInvNomenclatureName() {
    TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
    String nomenclature = null;
    if (typeIdPG == null) {
      logger.error("No typeIdentificationPG for asset");
    } else {
      nomenclature = typeIdPG.getNomenclature();
    }
    return nomenclature;
  }
}





