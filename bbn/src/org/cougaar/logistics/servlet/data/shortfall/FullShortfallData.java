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
import org.cougaar.logistics.plugin.inventory.ShortfallSummary;
import org.cougaar.logistics.plugin.inventory.ShortfallInventory;


/**
 * Abstract representation of the data leaving the Completion PSP.
 *
 * @see FullShortfallData
 * @see ShortfallShortData
 **/
public class FullShortfallData extends ShortfallShortData implements XMLable, Serializable{

  //Variables:
  ////////////

  public final static String THREAD_TAG="CLASS_OF_SUPPLY";
  public final static String INVENTORIES_TAG="INVENTORIES";
  public final static String INVENTORY_TAG="INVENTORY";

  //Constructors:
  ///////////////


  public FullShortfallData(String agentName,long time, Collection summaries) {
      super(agentName,time,summaries);
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
    w.tagln(TIME_MILLIS_TAG, getTimeMillis());    
    w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfShortfallInventories());
    w.tagln(NUM_TEMP_SHORTFALL_INVENTORIES_TAG,getNumberOfTempShortfallInventories());
    w.tagln(NUM_UNEXPECTED_SHORTFALL_INVENTORIES_TAG, getNumberOfUnexpectedShortfallInventories());    
    supplyTypesToXML(w);
    Iterator summaries = summaryMap.values().iterator();
    while(summaries.hasNext()) {
	ShortfallSummary summary = (ShortfallSummary) summaries.next();
	w.optagln(INVENTORIES_TAG,THREAD_TAG, summary.getSupplyType());
	Iterator inventoryItems = summary.getShortfallInventories().iterator();
	while(inventoryItems.hasNext()) {
	    ShortfallInventory inv = (ShortfallInventory) inventoryItems.next();
	    w.tagln(INVENTORY_TAG,inv.getInvID());
	}
	w.cltagln(INVENTORIES_TAG);
    }
    w.cltagln(getNameTag());
  }


  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234092540398212345L;
}










