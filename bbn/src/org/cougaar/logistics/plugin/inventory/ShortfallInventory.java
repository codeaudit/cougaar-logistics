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
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;

/** 
 * ShortfallSummary is an object that summarizizes which inventories of 
 * a particular inventory have shortfall.
 */

public class ShortfallInventory implements java.io.Serializable, Publishable {



  private String invID;
  private String unitOfIssue;
  private int numDemandSupply=0;
  private int numResupplySupply=0;
  private int numDemandProj=0;
  private int numResupplyProj=0;
  private int numTempResupplySupply=0;
  private int numTempDemandSupply=0;
  private int numTempResupplyProj=0;
  private int numTempDemandProj=0;



  private boolean unexpected = true;

  private ArrayList shortfallPeriods;


  /**
   * Constructor 
   * @param anInvID String identifier of the inventory (item/NSN)
   * @param uoi  The Unit of Issue for this Shortfall Inventory
   */


  public ShortfallInventory (String anInvID,
			     String uoi) {
      invID = anInvID;
      unitOfIssue = uoi;
      shortfallPeriods = new ArrayList();
  }
  
  public String getInvID() { return invID; }
  public String getUnitOfIssue() { return unitOfIssue; }

  public int getNumDemandSupply() { return numDemandSupply; }
  public int getNumResupplySupply() { return numResupplySupply; }
  public int getNumDemandProj() { return numDemandProj; }
  public int getNumResupplyProj() { return numResupplyProj; }
  public int getNumDemand() { return numDemandSupply + numDemandProj; }
  public int getNumRefill() { return numResupplyProj + numResupplySupply;}
  public int getNumProjection() { return numDemandProj + numResupplyProj; }
  public int getNumActual() { return numDemandSupply + numResupplySupply; }
  public int getNumTempResupplySupply() { return numTempResupplySupply; }
  public int getNumTempDemandSupply() { return numTempDemandSupply; }
  public int getNumTempResupplyProj() { return numTempResupplyProj; }
  public int getNumTempDemandProj() { return numTempDemandProj; }

  public int getNumPermShortfall() { return (getNumTotalShortfall() -
					     getNumTempShortfall()); }
  public int getNumTempShortfall() {return (getNumTempResupplySupply() +
					    getNumTempDemandSupply() +
					    getNumTempResupplyProj() +
					    getNumTempDemandProj()); }
  public int getNumTotalShortfall() { return ((getNumDemandSupply() +
					       getNumResupplySupply() +
					       getNumDemandProj() +
					       getNumResupplyProj()));}
  public boolean getUnexpected() { return unexpected; }

  public void setUnexpected(boolean isUnexpected) { unexpected = isUnexpected; }

  public void setNumDemandSupply(int numDemandSupply) {
      this.numDemandSupply = numDemandSupply;
  }
  public void setNumResupplySupply(int numResupplySupply) {
      this.numResupplySupply = numResupplySupply;
  }
  public void setNumDemandProj(int numDemandProj) {
      this.numDemandProj = numDemandProj;
  }
  public void setNumResupplyProj(int numResupplyProj) {
      this.numResupplyProj = numResupplyProj;
  }
  public void setNumTempDemandSupply(int numDemandTemp) {
      this.numTempDemandSupply = numDemandTemp;
  }
  public void setNumTempResupplySupply(int numResupplyTemp) {
      this.numTempResupplySupply = numResupplyTemp;
  }
  public void setNumTempDemandProj(int numDemandTemp) {
      this.numTempDemandProj = numDemandTemp;
  }
  public void setNumTempResupplyProj(int numResupplyTemp) {
      this.numTempResupplyProj = numResupplyTemp;
  }

  public void addShortfallPeriod(ShortfallPeriod aPeriod) {
      this.shortfallPeriods.add(aPeriod);
  }

  public ArrayList getShortfallPeriods() { return shortfallPeriods; }

  public int getMaxPercentShortfall() {
      int maxShortfall=0;
      Iterator periodsIt = shortfallPeriods.iterator();
      while(periodsIt.hasNext()) {
	  ShortfallPeriod period = (ShortfallPeriod) periodsIt.next();
	  maxShortfall = Math.max(maxShortfall,
				  ((int)period.getPercentShortfall()));
      }
      return maxShortfall;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(getInvID());
    sb.append("\nUnitOfIssue=" + getUnitOfIssue());
    sb.append("\nNumDemand=" + getNumDemand());
    sb.append(",NumResupplySupply=" + getNumResupplySupply());
    sb.append("\nNumDemandProj=" + getNumDemandProj());
    sb.append(",NumResupplyProj=" + getNumResupplyProj());
    sb.append(",NumTempDemandSupply=" + getNumTempDemandSupply());
    sb.append(",NumTempResupplySupply=" + getNumTempResupplySupply());
    sb.append(",NumTempDemandProj=" + getNumTempDemandProj());
    sb.append(",NumTempResupplyProj=" + getNumTempResupplyProj());
    sb.append("\nNumPerm=" + getNumPermShortfall());
    return sb.toString();
  }

  public boolean equals(ShortfallInventory si) {
      return ((this.getInvID().equals(si.getInvID())) &&
	      (this.getNumDemand() == si.getNumDemand()) &&
	      (this.getNumResupplySupply() == si.getNumResupplySupply()) &&
	      (this.getNumDemandProj() == si.getNumDemandProj()) &&
	      (this.getNumResupplyProj() == si.getNumResupplyProj()) &&
	      (this.getNumTempDemandSupply() == si.getNumTempDemandSupply()) &&
	      (this.getNumTempResupplySupply() == si.getNumTempResupplySupply()) &&	     
	      (this.getNumTempDemandProj() == si.getNumTempDemandProj()) &&
	      (this.getNumTempResupplyProj() == si.getNumTempResupplyProj()) &&
	      (hasEqualShortfallPeriods(si.getShortfallPeriods())));
  }

  public boolean hasEqualShortfallPeriods(ArrayList otherShortfallPeriods) {
    Iterator myPeriods = getShortfallPeriods().iterator();
    outer_loop: while(myPeriods.hasNext()) {
	ShortfallPeriod myPeriod = (ShortfallPeriod) myPeriods.next();
	Iterator otherPeriods = otherShortfallPeriods.iterator();
	boolean found=false;
	while(otherPeriods.hasNext()) {
	  ShortfallPeriod otherPeriod = (ShortfallPeriod) otherPeriods.next();
	  if(myPeriod.equals(otherPeriod)) {
	      continue outer_loop;
	  }
	}
	return false;
     }
     return true;
    }
  public boolean isPersistable() {
    return true;
  }
}
