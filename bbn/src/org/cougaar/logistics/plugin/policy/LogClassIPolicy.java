/*
 * <copyright>
 *  
 *  Copyright 1997-2004 SRA International
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
 *
 */

package org.cougaar.logistics.plugin.policy;

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
 
public class LogClassIPolicy extends Policy {
  public static final String MEAL_POLICY = "MealPolicy";
  public static final String A_RATION_POLICY = "A-RationPolicy";
  public static final String ENHANCEMENTS_POLICY = "EnhancementsPolicy";
  public static final String BOTTLEDWATER_POLICY = "BottledWaterPolicy";

/**
 * SubsistencePolicy constructor comment.
 */
public LogClassIPolicy() {
}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public RangeRuleParameterEntry[] getARationPolicyRanges() {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(A_RATION_POLICY);
	return (RangeRuleParameterEntry[]) param.getRanges();

}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public RangeRuleParameterEntry[] getEnhancementsPolicyRanges() {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(ENHANCEMENTS_POLICY);
	return (RangeRuleParameterEntry[]) param.getRanges();

}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public RangeRuleParameterEntry[] getMealPolicyRanges() {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(MEAL_POLICY);
	return (RangeRuleParameterEntry[]) param.getRanges();

}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public RangeRuleParameterEntry[] getWaterPolicyRanges() {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(BOTTLEDWATER_POLICY);
	return (RangeRuleParameterEntry[]) param.getRanges();

}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public void setARationPolicyRanges(RangeRuleParameterEntry[] ranges) {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(A_RATION_POLICY);
	param.setRanges(ranges);
}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public void setEnhancementsPolicyRanges(RangeRuleParameterEntry[] ranges) {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(ENHANCEMENTS_POLICY);
	param.setRanges(ranges);
}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public void setMealPolicyRanges(RangeRuleParameterEntry[] ranges) {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(MEAL_POLICY);
	param.setRanges(ranges);
}
/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public void setWaterPolicyRanges(RangeRuleParameterEntry[] ranges) {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(BOTTLEDWATER_POLICY);
	param.setRanges(ranges);
}
  public static final String RULES = "Rules";/**
 * Insert the method's description here.
 * Creation date: (4/16/01 9:01:45 AM)
 */
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
}/**
 * Insert the method's description here.
 * Creation date: (4/16/01 9:01:45 AM)
 */
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
}/**
 * Insert the method's description here.
 * Creation date: (4/11/01 9:45:39 AM)
 */
public KeyRuleParameterEntry[] getEnhancementsKeys(int range) {
  RangeRuleParameterEntry[] enhancements = getEnhancementsPolicyRanges();
  if (enhancements != null) {
	KeyRuleParameter krp = (KeyRuleParameter) enhancements[range].getValue();
	if (krp != null)
	  return krp.getKeys();
  }
  return new KeyRuleParameterEntry[0];
}/**
 * Insert the method's description here.
 * Creation date: (4/11/01 9:45:39 AM)
 */
public KeyRuleParameterEntry[] getMealKeys(int range) {
  RangeRuleParameterEntry[] meals = getMealPolicyRanges();
  if (meals != null) {
	KeyRuleParameter krp = (KeyRuleParameter) meals[range].getValue();
	if (krp != null)
	  return krp.getKeys();
  }
  return new KeyRuleParameterEntry[0];
}/**
 * Insert the method's description here.
 * Creation date: (4/11/01 9:45:39 AM)
 */
public KeyRuleParameterEntry[] getRangeKeys(RangeRuleParameterEntry range) {
  if (range != null) {
	KeyRuleParameter krp = (KeyRuleParameter) range.getValue();
	if (krp != null)
	  return krp.getKeys();
  }
  return new KeyRuleParameterEntry[0];
}/**
 * Insert the method's description here.
 * Creation date: (3/2/01 9:03:51 AM)
 */
public RangeRuleParameterEntry[] getRules() {
	RangeRuleParameter param = (RangeRuleParameter) Lookup(RULES);
	return (RangeRuleParameterEntry[]) param.getRanges();

}/**
 * Insert the method's description here.
 * Creation date: (4/11/01 9:45:39 AM)
 */
public KeyRuleParameterEntry[] getWaterKeys(int range) {
  RangeRuleParameterEntry[] water = getWaterPolicyRanges();
  if (water != null) {
	KeyRuleParameter krp = (KeyRuleParameter) water[range].getValue();
	if (krp != null)
	  return krp.getKeys();
  }
  return new KeyRuleParameterEntry[0];
}/**
 * Insert the method's description here.
 * Creation date: (4/16/01 9:01:45 AM)
 */
void newMethod() {
}}
