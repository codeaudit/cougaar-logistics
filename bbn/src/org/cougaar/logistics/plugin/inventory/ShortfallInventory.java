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
  private int numDemandSupply=0;
  private int numResupplySupply=0;
  private int numDemandProj=0;
  private int numResupplyProj=0;

  /**
   * Constructor 
   * @param aSupplyType - the supply type this shortfall summary applys to
   */


  public ShortfallInventory (String anInvID) {
      invID = anInvID;
  }
  
  public String getInvID() { return invID; }

  public int getNumDemandSupply() { return numDemandSupply; }
  public int getNumResupplySupply() { return numResupplySupply; }
  public int getNumDemandProj() { return numDemandProj; }
  public int getNumResupplyProj() { return numResupplyProj; }
  public int getNumDemand() { return numDemandSupply + numDemandProj; }
  public int getNumRefill() { return numResupplyProj + numResupplySupply;}
  public int getNumProjection() { return numDemandProj + numResupplyProj; }
  public int getNumActual() { return numDemandSupply + numResupplySupply; }


  public int getNumPermShortfall() { return (getNumDemandSupply() +
					     getNumResupplySupply() +
					     getNumDemandProj() +
					     getNumResupplyProj()); }

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


  public String toString() {
    StringBuffer sb = new StringBuffer(getInvID());
    sb.append("\nNumDemand=" + getNumDemand());
    sb.append(",NumResupplySupply=" + getNumResupplySupply());
    sb.append("\nNumDemandProj=" + getNumDemandProj());
    sb.append(",NumResupplyProj=" + getNumResupplyProj());
    sb.append("\nNumPerm=" + getNumPermShortfall());
    return sb.toString();
  }

  public boolean equals(ShortfallInventory si) {
      return ((this.getInvID().equals(si.getInvID())) &&
	      (this.getNumDemand() == si.getNumDemand()) &&
	      (this.getNumResupplySupply() == si.getNumResupplySupply()) &&
	      (this.getNumDemandProj() == si.getNumDemandProj()) &&
	      (this.getNumResupplyProj() == si.getNumResupplyProj()));
  }
  

  public boolean isPersistable() {
    return true;
  }
}
