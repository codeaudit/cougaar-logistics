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
import org.cougaar.logistics.ldm.policy.FeedingPolicy;


public class FeedingPolicyPred implements UnaryPredicate {

  String feedingPolicyName;
  public FeedingPolicyPred(String service) {
   feedingPolicyName = service+"FeedingPolicy";
  }
  
  public boolean execute(Object o) {
    if (o instanceof FeedingPolicy) {
      FeedingPolicy pol = (FeedingPolicy) o;
      if (pol.getName().equals(feedingPolicyName)) {
	return true;
      } 
    } 
    return false;
  } 

  public String getFeedingPolicyName() {
    return feedingPolicyName;
  }

  public boolean equals(Object o) {
    if (o instanceof FeedingPolicyPred) {
      String tmp = ((FeedingPolicyPred)o).getFeedingPolicyName();
      if (tmp.equals(feedingPolicyName)){
	return true;
      }
    }
    return false;
  }

  public int hashCode() {
    return feedingPolicyName.hashCode();
  }

} 
