/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.servlet.data.shortfall;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Collection;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.xml.sax.Attributes;
import org.cougaar.logistics.servlet.ShortfallAlertServlet;
import org.cougaar.logistics.plugin.inventory.ShortfallSummary;
import org.cougaar.logistics.plugin.inventory.ShortfallInventory;
import org.cougaar.logistics.plugin.inventory.ShortfallPeriod;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;


/**
 * Abstract representation of the data leaving the Completion PSP.
 *
 * @see FullShortfallData
 * @see ShortfallShortData
 **/
public class FullShortfallData extends ShortfallShortData implements XMLable, Serializable{

  //Variables:
  ////////////

  public final static String THREAD_ATTR="ClassOfSupply";
  public final static String INVENTORIES_TAG="INVENTORIES";
  public final static String INVENTORIES_TYPE_TAG="INVENTORIES_TYPE";
  public final static String INVENTORY_TAG="INVENTORY";
  public final static String SHORTFALL_PERIOD_TAG = "SHORTFALL_PERIOD";

  public final static String INVENTORY_ID_ATTR="ID";
  public final static String INVENTORY_NUM_DEMAND_ATTR="NumDemand";
  public final static String INVENTORY_NUM_REFILL_ATTR="NumRefill";
  public final static String INVENTORY_NUM_PROJECTION_ATTR="NumProjection";  
  public final static String INVENTORY_NUM_ACTUAL_ATTR="NumActual";
  public final static String INVENTORY_IS_UNEXPECTED_ATTR = "Unexpected";

  public final static String INVENTORY_PERIOD_TAG = "SHORTFALL_PERIOD";
  public final static String PERIOD_START_TIME_ATTR = "StartTime";
  public final static String PERIOD_END_TIME_ATTR = "EndTime";
  public final static String PERIOD_DURATION_ATTR = "Duration";
  public final static String UNIT_OF_ISSUE_ATTR = "UnitOfIssue";
  public final static String PERIOD_TOTAL_DEMAND_ATTR = "TotalDemand";
  public final static String PERIOD_TOTAL_FILLED_ATTR = "TotalFilled";
  public final static String PERIOD_SHORTFALL_QTY_ATTR = "ShortfallQty";
  public final static String PERIOD_PERCENT_SHORTFALL_ATTR = "PercentShortfall";

  //Constructors:
  ///////////////


  public FullShortfallData(String agentName,
			   String geoLocString,
			   long time, 
			   long c0Time,
			   Collection summaries,
			   boolean userMode) {
      super(agentName,geoLocString,time,c0Time,summaries,userMode);
  }


  //Getters:
  //////////



  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(getNameTag());
    w.tagln(AGENT_NAME_TAG, getAgentName());
    w.tagln(GEO_LOC_TAG, getGeoLocString());
    w.tagln(USER_MODE_TAG, Boolean.toString(userMode));
    w.tagln(TIME_MILLIS_TAG, getTimeMillis());    
    if(userMode) {
      w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfShortfallPeriodInventories());
    }
    else{
      w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfShortfallInventories());
      w.tagln(NUM_SHORTFALL_PERIOD_INVENTORIES_TAG,getNumberOfShortfallPeriodInventories());
      w.tagln(NUM_TEMP_SHORTFALL_INVENTORIES_TAG,getNumberOfTempShortfallInventories());
      w.tagln(NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG, getNumberOfUnexpectedShortfallInventories());    
    }    
    supplyTypesToXML(w);

    w.optagln(INVENTORIES_TAG);
    Iterator summaries = summaryMap.values().iterator();
    while(summaries.hasNext()) {
	ShortfallSummary summary = (ShortfallSummary) summaries.next();
	w.optagln(INVENTORIES_TYPE_TAG,THREAD_ATTR, summary.getSupplyType());
	Iterator inventoryItems = summary.getShortfallInventories().iterator();
	while(inventoryItems.hasNext()) {
	    ShortfallInventory inv = (ShortfallInventory) inventoryItems.next();
	    if((!userMode) ||
	       ((userMode) && !(inv.getShortfallPeriods().isEmpty()))) {
		if(userMode) {
		w.optagln(INVENTORY_TAG,
			  INVENTORY_ID_ATTR,inv.getInvID()
			  );
		}
		else {
		w.optagln(INVENTORY_TAG,
			  INVENTORY_ID_ATTR,inv.getInvID(),
			  INVENTORY_IS_UNEXPECTED_ATTR,Boolean.toString(inv.getUnexpected())
			  );

		}
		Iterator periods = inv.getShortfallPeriods().iterator();
		while(periods.hasNext()) {
		  ShortfallPeriod period = (ShortfallPeriod) periods.next();
		  String unitOfIssue = inv.getUnitOfIssue();
		  long bucketSize = summary.getMsecPerBucket();
		  boolean roundToInt = !(unitOfIssue.equals(LogisticsInventoryFormatter.AMMUNITION_UNIT));
		  w.sitagln(INVENTORY_PERIOD_TAG,
			    PERIOD_START_TIME_ATTR,ShortfallAlertServlet.createTimeString(period.getStartTime(),bucketSize,c0Time),
			    PERIOD_END_TIME_ATTR,ShortfallAlertServlet.createTimeString(period.getEndTime(),bucketSize,c0Time),
			    PERIOD_DURATION_ATTR,Integer.toString(period.getNumBuckets(bucketSize)),
			    UNIT_OF_ISSUE_ATTR,unitOfIssue,
			    PERIOD_TOTAL_DEMAND_ATTR,Double.toString(period.getRoundedTotalDemand(roundToInt)),
			    PERIOD_TOTAL_FILLED_ATTR,Double.toString(period.getRoundedTotalFilled(roundToInt)),
			    PERIOD_SHORTFALL_QTY_ATTR,Double.toString(period.getShortfallQty(roundToInt)),
			    PERIOD_PERCENT_SHORTFALL_ATTR,Double.toString(period.getPercentShortfall()));
			    
		}
		w.cltagln(INVENTORY_TAG);
	    }
	}
	w.cltagln(INVENTORIES_TYPE_TAG);
    }
    w.cltagln(INVENTORIES_TAG);
    w.cltagln(getNameTag());
  }


  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234092540398212345L;
}










