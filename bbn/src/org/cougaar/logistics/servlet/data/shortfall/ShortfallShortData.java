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
import java.util.Iterator;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.xml.sax.Attributes;

import org.cougaar.logistics.plugin.inventory.ShortfallSummary;


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

  protected final static String UID_TAG = "UID";
  protected final static String NUM_SHORTFALL_INVENTORIES_TAG = "Num_Shortfall_Inventories";
  protected final static String THREADS_TAG = "THREADS";


  public static final String TIME_MILLIS_ATTR = 
    "TimeMillis";


  protected long timeMillis;

  protected HashMap summaryMap;

  protected int numPermShortfallInventories;


  //Constructors:
  ///////////////


  public ShortfallShortData(Collection summaries) {
      numPermShortfallInventories = 0;
      summaryMap = new HashMap(4);
      Iterator it = summaries.iterator();
      while(it.hasNext()) {
	  ShortfallSummary summary = (ShortfallSummary) it.next();
	  summaryMap.put(summary.getSupplyType(),summary);
	  numPermShortfallInventories+=summary.getShortfallInventories().size();
      }
  }

  //Setters:
  //////////

  public void setTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
  }



  public void setNumberOfPermShortfallInventories(int numInventories) {
    this.numPermShortfallInventories = numInventories;
  }

  //Getters:
  //////////

  public long getTimeMillis() {
    return timeMillis;
  }

  public String getNameTag() {
    return NAME_TAG;
  }

  public HashMap getShortfallSummaries() {
      return summaryMap;
  }

  public int getNumberOfPermShortfallInventories() {
    return numPermShortfallInventories;
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

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(getNameTag());
    w.tagln(NUM_SHORTFALL_INVENTORIES_TAG, getNumberOfPermShortfallInventories());    
    w.tagln(THREADS_TAG, getSupplyTypes());
    w.cltagln(getNameTag());
  }


  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234092540398212345L;
}










