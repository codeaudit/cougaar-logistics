/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.utils;

import org.cougaar.glm.ldm.asset.ClassVIIMajorEndItem;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.asset.Asset;

/** 
 * Consumer Predicate 
 **/
public class ConsumerPredicate implements UnaryPredicate {
  String itemName;
  public ConsumerPredicate(String itemName) {
    this.itemName = itemName;
  }
  
  public boolean execute(Object o) {
    if (o instanceof ClassVIIMajorEndItem) {
      if (itemName.equals(getTypeID((Asset)o))) {
	return true;
      }
    }
    if (o instanceof AggregateAsset) {
      if (((AggregateAsset)o).getAsset() instanceof ClassVIIMajorEndItem) {
	if (itemName.equals(getTypeID(((AggregateAsset)o).getAsset()))) {
	  return true;
	}
      }
    }
    return false;
  }
  
  public String getItemName() {
    return itemName;
  }
  
  private String getTypeID(Asset asset) {
    String item = asset.getTypeIdentificationPG().getTypeIdentification();
    return item;
  }
  
  public boolean equals(Object o) {
    if (o instanceof ConsumerPredicate) {
      String tmp = ((ConsumerPredicate)o).getItemName();
      if (tmp.equals(itemName)) {
	return true;
      }
    }
    return false;
  }

  public int hashCode() {
    return itemName.hashCode();
  }

}

