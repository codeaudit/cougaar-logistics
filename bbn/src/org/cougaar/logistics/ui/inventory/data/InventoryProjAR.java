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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import java.util.Date;
import java.util.ArrayList;

import org.cougaar.logistics.plugin.inventory.TimeUtils;

/**
 * <pre>
 *
 * The InventoryProjAR is the concrete class that corresponds
 * to a projectsupply or projectwithdraw type Allocation Result.
 *
 *@see InventoryChildProjAR
 *
 **/

public class InventoryProjAR extends InventoryAR {


  public InventoryProjAR(String aParentUID,
                         String myUID,
                         String aVerb,
                         String aForOrg,
                         int aResultType,
                         boolean isSuccess,
                         double aRate,
                         long aStartTime,
                         long anEndTime) {
    super(aParentUID, myUID, aVerb, aForOrg,
          aResultType, isSuccess, aRate, aStartTime, anEndTime);
  }

  public ArrayList explodeToBuckets(long msecUnits, int numUnits) {
    ArrayList bucketlys = new ArrayList();
    long currStartTime = startTime;
    while (success &&
        (currStartTime < endTime)) {
      long currEndTime = Math.min(getEndOfPeriod(currStartTime, msecUnits, numUnits), endTime);
      double durationInDays = (double) ((double) ((currEndTime - currStartTime) + 1) / (double) TimeUtils.MSEC_PER_DAY);
      double bucketRate = getDailyRate() * durationInDays;
      bucketlys.add(new InventoryProjAR(parentUID,
                                        UID,
                                        verb,
                                        forOrg,
                                        resultType,
                                        success,
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


  public double getDailyRate() {
    /*** MWD remove - projection ars are daily rates
     long duration = getEndTime() - getStartTime();
     if(duration < TimeUtils.MSEC_PER_DAY)
     return getQty();
     else
     return getQty()/(duration/TimeUtils.MSEC_PER_DAY);
     **
     ***/

    return getQty();
  }

  public String toString() {
    return super.toString() + ",dailyRate=" + getDailyRate();
  }

  public String getHRHeader() {
    return "<parent UID,UID,Verb,For Org,Result Type,Success?,Start Time,End Time,Daily Rate>";
  }

  public static InventoryProjAR createProjFromCSV(String csvString) {
    String[] subStrings = csvString.split(SPLIT_REGEX);

    //double aQty = (new Double(subStrings[AR_QTY_INDEX])).doubleValue();

    long qtyBits = Long.parseLong(subStrings[AR_QTY_INDEX],16);
    double aQty = Double.longBitsToDouble(qtyBits);


    long aStartTime = -0L;
    String startTimeStr = subStrings[AR_START_TIME_INDEX].trim();
    if (!(startTimeStr.equals(""))) {
      aStartTime = (new Long(startTimeStr)).longValue();
    }
    long anEndTime = (new Long(subStrings[AR_END_TIME_INDEX])).longValue();

    int aResultType = AR_ESTIMATED;
    if (subStrings[AR_TYPE_INDEX].trim().equals(AR_REPORTED_STR)) {
      aResultType = AR_REPORTED;
    }

    boolean isSuccess = (subStrings[AR_SUCCESS_INDEX].trim().equals(AR_SUCCESS_STR));

    InventoryProjAR newAR = new InventoryProjAR(subStrings[PARENT_UID_INDEX].trim(),
                                                subStrings[UID_INDEX].trim(),
                                                subStrings[VERB_INDEX].trim(),
                                                subStrings[FOR_INDEX].trim(),
                                                aResultType, isSuccess,
                                                aQty, aStartTime, anEndTime);

    return newAR;

  }

  public static void main(String[] args) {
    Date now = new Date();
    InventoryProjAR ar = InventoryProjAR.createProjFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,ESTIMATED,SUCCESS," + now.getTime() + "," + (now.getTime() + (TimeUtils.MSEC_PER_DAY * 3)) + "," + 69 + "\n");
    Logger logger = Logging.getLoggerFactory().createLogger(InventoryLevel.class.getName());
    logger.shout("InventoryProjAR is " + ar);
    logger.shout("Children are");
    InventoryChildProjAR[] children = InventoryChildProjAR.expandProjAR(ar,
                                                                        (TimeUtils.MSEC_PER_DAY * 2));
    for (int i = 0; i < children.length; i++) {
      logger.shout(children[i].toString());
    }
  }
}


