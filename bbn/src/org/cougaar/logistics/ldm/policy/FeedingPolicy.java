/*
 * <copyright>
 *  Copyright 1997-2003 SRA International
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
 *
 */

package org.cougaar.logistics.ldm.policy;

import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.BooleanRuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;
import org.cougaar.planning.ldm.policy.RangeRuleParameter;
import org.cougaar.planning.ldm.policy.StringRuleParameter;
import org.cougaar.planning.ldm.policy.KeyRuleParameter;
import org.cougaar.planning.ldm.policy.KeyRuleParameterEntry;
import org.cougaar.planning.ldm.policy.RangeRuleParameterEntry;
import java.util.Arrays;
 
public class FeedingPolicy extends Policy {
  public static final String MEAL_POLICY = "MealPolicy";
  public static final String A_RATION_POLICY = "A-RationPolicy";
  public static final String ENHANCEMENTS_POLICY = "EnhancementsPolicy";
  public static final String BOTTLEDWATER_POLICY = "BottledWaterPolicy";

  public FeedingPolicy() {
  }

  public RangeRuleParameterEntry[] getARationPolicyRanges() {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(A_RATION_POLICY);
    return (RangeRuleParameterEntry[]) param.getRanges(); 
  }

  public RangeRuleParameterEntry[] getEnhancementsPolicyRanges() {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(ENHANCEMENTS_POLICY);
    return (RangeRuleParameterEntry[]) param.getRanges();
  }

  public RangeRuleParameterEntry[] getMealPolicyRanges() {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(MEAL_POLICY);
    return (RangeRuleParameterEntry[]) param.getRanges(); 
  }

  public RangeRuleParameterEntry[] getWaterPolicyRanges() {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(BOTTLEDWATER_POLICY);
    return (RangeRuleParameterEntry[]) param.getRanges();
  }

  public void setARationPolicyRanges(RangeRuleParameterEntry[] ranges) {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(A_RATION_POLICY);
    param.setRanges(ranges);
  }

  public void setEnhancementsPolicyRanges(RangeRuleParameterEntry[] ranges) {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(ENHANCEMENTS_POLICY);
    param.setRanges(ranges);
  }

  public void setMealPolicyRanges(RangeRuleParameterEntry[] ranges) {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(MEAL_POLICY);
    param.setRanges(ranges);
  }

  public void setWaterPolicyRanges(RangeRuleParameterEntry[] ranges) {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(BOTTLEDWATER_POLICY);
    param.setRanges(ranges);
  }
  public static final String RULES = "Rules";

  public KeyRuleParameterEntry[] getActionKeys(RangeRuleParameter theRule) {
    if (theRule != null) {
      RangeRuleParameterEntry[] rule = (RangeRuleParameterEntry[]) theRule.getRanges(); 
	if (rule != null) {
	  KeyRuleParameter theAction = (KeyRuleParameter) rule[1].getValue();
	  if (theAction != null)
	    return theAction.getKeys();
	}
    }
    return null;
  }

  public KeyRuleParameterEntry[] getConditionKeys(RangeRuleParameter theRule) {
    if (theRule != null) {
      RangeRuleParameterEntry [] rule = (RangeRuleParameterEntry []) theRule.getRanges(); 
      if (rule != null) {
	  KeyRuleParameter theCondition = (KeyRuleParameter) rule[0].getValue();
	  if (theCondition != null)
	    return theCondition.getKeys();
      }
    }
    return null;
  }

  public KeyRuleParameterEntry[] getEnhancementsKeys(int range) {
    RangeRuleParameterEntry[] enhancements = getEnhancementsPolicyRanges();
    if (enhancements != null) {
      KeyRuleParameter krp = (KeyRuleParameter) enhancements[range].getValue();
      if (krp != null)
	return krp.getKeys();
    }
    return new KeyRuleParameterEntry[0];
  }

  public KeyRuleParameterEntry[] getMealKeys(int range) {
    RangeRuleParameterEntry[] meals = getMealPolicyRanges();
    if (meals != null) {
      KeyRuleParameter krp = (KeyRuleParameter) meals[range].getValue();
      if (krp != null)
	return krp.getKeys();
    }
    return new KeyRuleParameterEntry[0];
  }

  public KeyRuleParameterEntry[] getRangeKeys(RangeRuleParameterEntry range) {
    if (range != null) {
      KeyRuleParameter krp = (KeyRuleParameter) range.getValue();
      if (krp != null)
	return krp.getKeys();
    }
    return new KeyRuleParameterEntry[0];
  }

  public RangeRuleParameterEntry[] getRules() {
    RangeRuleParameter param = (RangeRuleParameter) Lookup(RULES);
    return (RangeRuleParameterEntry[]) param.getRanges();
    
  }
  
  public KeyRuleParameterEntry[] getWaterKeys(int range) {
    RangeRuleParameterEntry[] water = getWaterPolicyRanges();
    if (water != null) {
      KeyRuleParameter krp = (KeyRuleParameter) water[range].getValue();
      if (krp != null)
	return krp.getKeys();
    }
    return new KeyRuleParameterEntry[0];
  }
}

