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

import org.cougaar.util.UnaryPredicate;
import org.cougaar.glm.ldm.asset.MilitaryPerson;
import org.cougaar.glm.ldm.asset.Person;
import org.cougaar.planning.ldm.asset.AggregateAsset;

/** 
 *  Military personnel, consumers for Subsistence 
 **/
public class MilitaryPersonPred implements UnaryPredicate {
  private static final String predString = "MilitaryPersonPred";
    
  public boolean execute (Object o) {
    if (o instanceof MilitaryPerson) {
      return true;
    }
    if (o instanceof AggregateAsset) {
      if (((AggregateAsset)o).getAsset() instanceof MilitaryPerson) {
	return true;
      }
    }
    return false;
  }

  public boolean equals(Object o) {
    if (o instanceof MilitaryPersonPred) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return predString.hashCode();
  }
}
