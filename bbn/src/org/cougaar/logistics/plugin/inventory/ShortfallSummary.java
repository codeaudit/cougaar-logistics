/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.OwnedUniqueObject;
import org.cougaar.core.util.SimpleUniqueObject;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

/** 
 * ShortfallSummary is an object that summarizizes which inventories of 
 * a particular inventory have shortfall.
 */

public class ShortfallSummary implements UniqueObject, java.io.Serializable, Publishable {

  private final static String HOURS = "Hours";
  private final static String DAYS = "Days";

  private UID uid = null;
  private String supplyType = "";
  private long bucketSize;
  private HashMap shortfallInventories;

  /**
   * Constructor 
   * @param aSupplyType - the supply type this shortfall summary applys to
   */

  
  public ShortfallSummary (String aSupplyType, UID aUID, long mSecs) {
    supplyType = aSupplyType;
    bucketSize = mSecs;
    shortfallInventories = new HashMap();
    this.uid = aUID;
  }
  

  // UniqueObject interface
  public UID getUID() {
    return uid;
  }
 
  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID: " + uid);
    this.uid = uid;
  }

  public long getMsecPerBucket() { return bucketSize; }

  public String getSupplyType() { return supplyType; }

  protected boolean addShortfallInventory(ShortfallInventory inv) {
      ShortfallInventory orig = (ShortfallInventory) shortfallInventories.get(inv.getInvID());
      if((orig == null) ||
	(!(orig.equals(inv)))) {
	  shortfallInventories.put(inv.getInvID(),inv);
	  return true;
      }
      return false;
  }

  protected boolean removeShortfallInventory(ShortfallInventory inv) {
      return removeShortfallInventory(inv.getInvID());
  }

  protected boolean removeShortfallInventory(String invID) {
      ShortfallInventory orig = (ShortfallInventory) shortfallInventories.get(invID);
      if(orig!=null) {
	  shortfallInventories.remove(invID);
	  return true;
      }
      return false;
  }

  public void setShortfallInventories(Collection invs) {
      shortfallInventories.clear();
      Iterator it = invs.iterator();
      while(it.hasNext()) {
	  ShortfallInventory si = (ShortfallInventory) it.next();
	  shortfallInventories.put(si.getInvID(),si);
      }
  }

  public boolean addShortfallInventories(Collection invs) {
      Iterator it = invs.iterator();
      boolean addedSomething = false;
      while(it.hasNext()) {
	  ShortfallInventory si = (ShortfallInventory) it.next();
	  if(addShortfallInventory(si)) {
	      addedSomething = true;
	  }
	  
      }
      return addedSomething;
  }

  public boolean removeShortfallInventories(Collection invIDs) {
      Iterator it = invIDs.iterator();
      boolean removedSomething=false;
      while(it.hasNext()) {
	  String invID = (String) it.next();
	  if(removeShortfallInventory(invID)) {
	      removedSomething = true;
	  }
      }
      return removedSomething;
  }

  public boolean hasPercentShortfallAbove(int thresholdPercent) {
    Iterator invIt = getShortfallInventories().iterator();
    while(invIt.hasNext()) {
      ShortfallInventory shortInv = (ShortfallInventory) invIt.next();
      if((shortInv.getUnexpected() &&
	 (shortInv.getMaxPercentShortfall() > thresholdPercent))) {
	  return true;
      }
    }
    return false;
  }

  public int getNumShortfallPeriodInvs() {
    int ctr=0;
    Iterator invIt = getShortfallInventories().iterator();
    while(invIt.hasNext()) {
      ShortfallInventory shortInv = (ShortfallInventory) invIt.next();
      if((shortInv.getUnexpected()) &&  (shortInv.getShortfallPeriods().size() > 0)) {
	  ctr++;
      }
    }
    return ctr;
  }

  public Collection getShortfallInventories() {
      return shortfallInventories.values();
  }

  public String getUnit() {
    return getUnit(getMsecPerBucket());
  }

  public static String getUnit(long msecPerBucket) {
      if(msecPerBucket == TimeUtils.MSEC_PER_DAY) {
	  return DAYS;
      }
      else if (msecPerBucket == TimeUtils.MSEC_PER_HOUR) {
	  return HOURS;
      }
      else { 
	  return "UNKNOWN";
      }
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer(getSupplyType());
    sb.append(" ");
    sb.append(" Inventories: ");
    Iterator it = shortfallInventories.values().iterator();
    while (it.hasNext()) {
      ShortfallInventory si = (ShortfallInventory) it.next();
      sb.append(si.toString() + "\n");
    }
    return sb.toString();
  }


  

  public boolean isPersistable() {
    return true;
  }
}
