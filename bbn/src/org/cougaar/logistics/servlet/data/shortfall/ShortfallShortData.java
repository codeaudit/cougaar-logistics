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
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.xml.sax.Attributes;

import org.cougaar.logistics.plugin.inventory.ShortfallSummary;
import org.cougaar.logistics.plugin.inventory.ShortfallInventory;


/**
 * Abstract representation of the data leaving the Completion PSP.
 *
 * @see FullShortfallData
 * @see ShortfallShortData
 **/
public class ShortfallShortData implements XMLable, Serializable{

  //Variables:
  ////////////

  public static final String NAME_TAG = "Shortfall";
  protected static ArrayList rulesList=new ArrayList();


  protected final static String AGENT_NAME_TAG = "AGENT";
  protected final static String GEO_LOC_TAG = "GEO_LOC";
  protected final static String USER_MODE_TAG = "USER_MODE";
  protected final static String NUM_SHORTFALL_INVENTORIES_TAG = "NUM_SHORTFALL_INVENTORIES";
  protected final static String NUM_TEMP_SHORTFALL_INVENTORIES_TAG = "NUM_TEMP_SHORTFALL_INVENTORIES";
  protected final static String NUM_SHORTFALL_PERIOD_INVENTORIES_TAG = "NUM_SHORTFALL_PERIOD_INVENTORIES";
  protected final static String NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG = "NUM_UNEXPECTED_SHORTFALL_INVENTORIES";
  protected final static String EFFECTED_THREADS_TAG = "EFFECTED_SUPPLY_TYPES";
  protected final static String EFFECTED_THREAD_TAG = "SUPPLY_TYPE";


  public static final String TIME_MILLIS_TAG = 
    "TIME_MILLIS";

  protected String agentName;

  protected String geoLocString;

  protected long timeMillis;

  protected HashMap summaryMap;

  protected int numShortfallInventories;

  protected int numShortfallPeriodInventories;

  protected int numTempShortfallInventories;

  protected int numUnexpectedShortfallInventories;

  protected long c0Time;

  protected boolean userMode=false;


  //Constructors:
  ///////////////


  public ShortfallShortData(String agentName,
			    String geoLocString,
			    long time, 
			    long c0Time,
			    Collection summaries, 
			    boolean userMode) {
      this.agentName = agentName;
      this.geoLocString = geoLocString;
      this.timeMillis = time;
      this.c0Time = c0Time;
      this.userMode = userMode;
      numShortfallInventories = 0;
      numShortfallPeriodInventories = 0;
      summaryMap = new HashMap(4);
      Iterator it = summaries.iterator();
      while(it.hasNext()) {
	  ShortfallSummary summary = (ShortfallSummary) it.next();
	  summaryMap.put(summary.getSupplyType(),summary);
	  numShortfallInventories+=summary.getShortfallInventories().size();
      }
      
      computeNumShortfallWithRules();
  }

  //Setters:
  //////////

  public void setTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
  }

  public void setAgentName(String agentName) {
    this.agentName = agentName;
  }

  public void setGeoLocString(String geoLocString) {
    this.geoLocString = geoLocString;
  }

  public static void setRulesList(ArrayList theRulesList) {
    rulesList = theRulesList;
  }

  public void setNumberOfShortfallInventories(int numInventories) {
    this.numShortfallInventories = numInventories;
  }

  public void setNumberOfUnexpectedShortfallInventories(int numInventories) {
    this.numUnexpectedShortfallInventories = numInventories;
  }

  //Getters:
  //////////

  public long getTimeMillis() {
    return timeMillis;
  }

  public String getNameTag() {
    return NAME_TAG;
  }

  public String getAgentName() {
    return agentName;
  }

  public String getGeoLocString() {
    return geoLocString;
  }

  public HashMap getShortfallSummaries() {
      return summaryMap;
  }

  public int getNumberOfShortfallInventories() {
    return numShortfallInventories;
  }

  public int getNumberOfShortfallPeriodInventories() {
    return numShortfallPeriodInventories;
  }

  public int getNumberOfTempShortfallInventories() {
      return numTempShortfallInventories;
  }

  public int getNumberOfUnexpectedShortfallInventories() {
    return numUnexpectedShortfallInventories;
  }

  public String getSupplyTypes() {
    Iterator summaries = summaryMap.values().iterator();
    String threadsStr = "";
    while(summaries.hasNext()) {
      ShortfallSummary summary = (ShortfallSummary) summaries.next();
      Iterator invIT = summary.getShortfallInventories().iterator();
    invLoop: while(invIT.hasNext()) {
        ShortfallInventory shortInv = (ShortfallInventory)invIT.next();
	if(((userMode) && 
	    (!shortInv.getShortfallPeriods().isEmpty()) && 
	    (shortInv.getUnexpected())) || (!userMode)) {
	  if(threadsStr.equals("")) {
	    threadsStr = summary.getSupplyType();
	  }
	  else {
	    threadsStr = threadsStr + ",\n" + summary.getSupplyType();
	  }
	  break invLoop;
	}
    }
    }
    return threadsStr;
  }


 

    protected void computeNumShortfallWithRules() {
      Collection summaries = getShortfallSummaries().values();
      Iterator summaryIT = summaries.iterator();
      numUnexpectedShortfallInventories=0;
      numTempShortfallInventories=0;
      while(summaryIT.hasNext()) {
	ShortfallSummary summary = (ShortfallSummary) summaryIT.next();
	Iterator invIT = summary.getShortfallInventories().iterator();
	while(invIT.hasNext()) {
	  ShortfallInventory shortInv = (ShortfallInventory)invIT.next();
	  ShortfallInventory origInv = shortInv;

	    
	  Iterator rulesIT = rulesList.iterator();
	  while(rulesIT.hasNext()) {
	    ShortfallInventoryRule rule = (ShortfallInventoryRule) rulesIT.next();
	    ShortfallInventory newInv = rule.apply(agentName,shortInv);
	    if(newInv != null) {
	      shortInv=newInv;
	    }
	  }
	  if(shortInv.getNumTotalShortfall() > 0) {
	    numUnexpectedShortfallInventories++;
	    origInv.setUnexpected(true);
	    if((shortInv.getNumTotalShortfall() - shortInv.getNumTempShortfall()) <= 0){
	      numTempShortfallInventories++;
	    }
	    if(!shortInv.getShortfallPeriods().isEmpty()) {
		numShortfallPeriodInventories++;
	    }
	  }
	  else {
	    origInv.setUnexpected(false);
	  }
	}
      }
    }

  public boolean hasPercentShortfallAbove(int thresholdPercent) {
    Iterator summariesIt = summaryMap.values().iterator();
    while(summariesIt.hasNext()) {
      ShortfallSummary summary = (ShortfallSummary) summariesIt.next();
      if(summary.hasPercentShortfallAbove(thresholdPercent)) {
	  return true;
      }
    }
    return false;
  }

  //XMLable members:
  //----------------

 public void supplyTypesToXML(XMLWriter w) throws IOException {
    Iterator summaries = summaryMap.values().iterator();
    w.optagln(EFFECTED_THREADS_TAG);
    while(summaries.hasNext()) {
      ShortfallSummary summary = (ShortfallSummary) summaries.next();
      Iterator invIT = summary.getShortfallInventories().iterator();
    invLoop: while(invIT.hasNext()) {
      ShortfallInventory shortInv = (ShortfallInventory)invIT.next();
      if(((userMode) && 
	  (!shortInv.getShortfallPeriods().isEmpty()) && 
	  (shortInv.getUnexpected())) || (!userMode)) {
	  w.tagln(EFFECTED_THREADS_TAG,summary.getSupplyType());
	  break invLoop;
      }
    }
    }
    w.cltagln(EFFECTED_THREADS_TAG);
   }



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
    else {
      w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfShortfallInventories());
      w.tagln(NUM_SHORTFALL_PERIOD_INVENTORIES_TAG,getNumberOfShortfallPeriodInventories());
      w.tagln(NUM_TEMP_SHORTFALL_INVENTORIES_TAG,getNumberOfTempShortfallInventories());
      w.tagln(NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG, getNumberOfUnexpectedShortfallInventories());    
    }
    supplyTypesToXML(w);
    w.cltagln(getNameTag());
  }



  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234092540398212345L;
}










