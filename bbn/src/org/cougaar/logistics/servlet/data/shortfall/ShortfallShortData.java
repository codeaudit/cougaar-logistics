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
  protected final static String NUM_SHORTFALL_INVENTORIES_TAG = "NUM_SHORTFALL_INVENTORIES";
protected final static String NUM_TEMP_SHORTFALL_INVENTORIES_TAG = "NUM_TEMP_SHORTFALL_INVENTORIES";
  protected final static String NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG = "NUM_UNEXPECTED_SHORTFALL_INVENTORIES";
  protected final static String EFFECTED_THREADS_TAG = "EFFECTED_SUPPLY_TYPES";
  protected final static String EFFECTED_THREAD_TAG = "SUPPLY_TYPE";


  public static final String TIME_MILLIS_TAG = 
    "TIME_MILLIS";

  protected String agentName;

  protected long timeMillis;

  protected HashMap summaryMap;

  protected int numShortfallInventories;

  protected int numTempShortfallInventories;

  protected int numUnexpectedShortfallInventories;


  //Constructors:
  ///////////////


  public ShortfallShortData(String agentName, long time, Collection summaries) {
      this.agentName = agentName;
      this.timeMillis = time;
      numShortfallInventories = 0;
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

  public HashMap getShortfallSummaries() {
      return summaryMap;
  }

  public int getNumberOfShortfallInventories() {
    return numShortfallInventories;
  }

  public int getNumberOfTempShortfallInventories() {
      return numTempShortfallInventories;
  }

  public int getNumberOfUnexpectedShortfallInventories() {
    return numUnexpectedShortfallInventories;
  }

  public String getSupplyTypes() {
      Collection threads = summaryMap.keySet();
      Iterator it=threads.iterator();
      String threadsStr = "";
      if(it.hasNext()) {
	  threadsStr = (String) it.next();
      }
      while(it.hasNext()) {
	  String thread = (String) it.next();
	  threadsStr = threadsStr + ",\n " + thread;
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
		boolean ruleMatch=false;
		ShortfallInventory shortInv = (ShortfallInventory)invIT.next();
		Iterator rulesIT = rulesList.iterator();
		while(rulesIT.hasNext()) {
		    ShortfallInventoryRule rule = (ShortfallInventoryRule) rulesIT.next();
		    ShortfallInventory newInv = rule.apply(agentName,shortInv);
		    if(newInv != null) {
		      shortInv=newInv;
		      ruleMatch=true;
		    }
		}
		if(shortInv.getNumTotalShortfall() > 0) {
		    numUnexpectedShortfallInventories++;
		    if((shortInv.getNumTotalShortfall() - shortInv.getNumTempShortfall()) <= 0){
		      numTempShortfallInventories++;
		    }
		}
	    }
	}
    }


  //XMLable members:
  //----------------

 public void supplyTypesToXML(XMLWriter w) throws IOException {
      Collection threads = summaryMap.keySet();
      Iterator it=threads.iterator();
      String threadsStr = "";
      w.optagln(EFFECTED_THREADS_TAG);
      while(it.hasNext()) {
	  String thread = (String) it.next();
	  w.tagln(EFFECTED_THREAD_TAG,thread);
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
    w.tagln(TIME_MILLIS_TAG, getTimeMillis());    
    w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfShortfallInventories());
    w.tagln(NUM_TEMP_SHORTFALL_INVENTORIES_TAG,getNumberOfTempShortfallInventories());
    w.tagln(NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG, getNumberOfUnexpectedShortfallInventories());    
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










