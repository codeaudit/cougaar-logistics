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

package org.cougaar.logistics.ui.inventory.data;

import java.util.Date;
import java.util.ArrayList;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import org.cougaar.logistics.plugin.inventory.TimeUtils;


/**
 * <pre>
 *
 * The InventoryProjTask is the concrete class that corresponds
 * to a projectsupply or projectwithdraw type task schedule element.
 *
 * @see InventoryChildProjTask
 *
 **/

public class InventoryProjTask extends InventoryTaskBase {

  public static final int RATE_INDEX = CSV_START_INDEX + 7;

  protected double rate;

  public InventoryProjTask(String aParentUID,
                           String myUID,
                           String aVerb,
                           String aForOrg,
                           double aRate,
                           long aStartTime,
                           long anEndTime) {
    super(aParentUID, myUID, aVerb, aForOrg, aStartTime, anEndTime);
    rate = aRate;
  }

  public double getDailyRate() {
    return rate;
  }


  public String getHRHeader() {
    return "<parent UID,UID,Verb,For Org,Start Time,End Time,Daily Rate>";
  }

  public String toHRString() {
    return super.toHRString() + "," + getDailyRate();
  }


  public ArrayList explodeToBuckets(long msecUnits, int numUnits) {
    ArrayList bucketlys = new ArrayList();
    long currStartTime = startTime;
    while (currStartTime < endTime) {
      long currEndTime = Math.min(getEndOfPeriod(currStartTime, msecUnits, numUnits), endTime);
      double durationInDays = (double) ((double) ((currEndTime - currStartTime) + 1) / (double) TimeUtils.MSEC_PER_DAY);
      double bucketRate = rate * durationInDays;
      bucketlys.add(new InventoryProjTask(parentUID,
                                          UID,
                                          verb,
                                          forOrg,
                                          bucketRate,
                                          currStartTime,
                                          currEndTime));
      currStartTime = currEndTime + 1;
    }
    return bucketlys;
  }


  public ArrayList explodeToBuckets(long bucketSize) {
    ArrayList dailys = new ArrayList();
    if (bucketSize >= TimeUtils.MSEC_PER_DAY) {
      int numDays = (int) (bucketSize / TimeUtils.MSEC_PER_DAY);
      return explodeToBuckets(TimeUtils.MSEC_PER_DAY, numDays);
    } else {
      int numHours = (int) (bucketSize / TimeUtils.MSEC_PER_HOUR);
      return explodeToBuckets(TimeUtils.MSEC_PER_HOUR, numHours);
    }
  }


  public static InventoryProjTask createFromCSV(String csvString) {
    String[] subStrings = csvString.split(SPLIT_REGEX);

    //double aRate = (new Double(subStrings[RATE_INDEX])).doubleValue();

    long rateBits = Long.parseLong(subStrings[RATE_INDEX],16);
    double aRate = Double.longBitsToDouble(rateBits);

    long aStartTime = -0L;
    String startTimeStr = subStrings[START_TIME_INDEX].trim();
    if (!(startTimeStr.equals(""))) {
      aStartTime = (new Long(startTimeStr)).longValue();
    }
    long anEndTime = (new Long(subStrings[END_TIME_INDEX])).longValue();

    InventoryProjTask newTask = new InventoryProjTask(subStrings[PARENT_UID_INDEX].trim(),
                                                      subStrings[UID_INDEX].trim(),
                                                      subStrings[VERB_INDEX].trim(),
                                                      subStrings[FOR_INDEX].trim(),
                                                      aRate, aStartTime, anEndTime);

    return newTask;

  }

  public String toString() {
    return super.toString() + ",daily_rate=" + getDailyRate();
  }

  public static void main(String[] args) {
    Date now = new Date();
    Logger logger = Logging.getLoggerFactory().createLogger(InventoryLevel.class.getName());
    InventoryProjTask task = InventoryProjTask.createFromCSV(now.getTime() + ",parent UID,UID, PROJECTSUPPLY,3-69-ARBN," + now.getTime() + "," + (now.getTime() + (4 * TimeUtils.MSEC_PER_DAY)) + "," + 23 + "\n");
    logger.shout("InventoryProjTask is " + task);
    InventoryChildProjTask[] expansion = InventoryChildProjTask.expandProjTask(task,
                                                                               2 * TimeUtils.MSEC_PER_DAY);
    logger.shout("ChildTasks are:");
    for (int i = 0; i < expansion.length; i++) {
      logger.shout(expansion[i].toString());
    }

  }
}


