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

import org.cougaar.planning.ldm.policy.KeyRuleParameter;
import org.cougaar.planning.ldm.policy.KeyRuleParameterEntry;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RangeRuleParameter;
import org.cougaar.planning.ldm.policy.RangeRuleParameterEntry;

public class FeedingPolicy extends Policy {
  public static final String MEAL_POLICY = "MealPolicy";
  public static final String A_RATION_POLICY = "A-RationPolicy";
  public static final String ENHANCEMENTS_POLICY = "EnhancementsPolicy";
  public static final String BOTTLEDWATER_POLICY = "BottledWaterPolicy";
  private final static RangeRuleParameterEntry[] EMPTY_RANGE_ENTRY = new RangeRuleParameterEntry[0];
  private final static KeyRuleParameterEntry[] EMPTY_KEY_ENTRY = new KeyRuleParameterEntry[0];


  public FeedingPolicy() {
  }

  public RangeRuleParameterEntry[] getARationPolicyRanges() {
    RangeRuleParameterEntry[] entry = EMPTY_RANGE_ENTRY;
    RangeRuleParameter param = (RangeRuleParameter) Lookup(A_RATION_POLICY);
    param.getRanges();
    return entry;
  }

  public RangeRuleParameterEntry[] getEnhancementsPolicyRanges() {
    RangeRuleParameterEntry[] entry = EMPTY_RANGE_ENTRY;
    RangeRuleParameter param = (RangeRuleParameter) Lookup(ENHANCEMENTS_POLICY);
    entry = param.getRanges();
    return entry;
  }

  public RangeRuleParameterEntry[] getMealPolicyRanges() {
    RangeRuleParameterEntry[] entry = EMPTY_RANGE_ENTRY;
    RangeRuleParameter param = (RangeRuleParameter) Lookup(MEAL_POLICY);
    entry = param.getRanges();
    return entry;
  }

  public RangeRuleParameterEntry[] getWaterPolicyRanges() {
    RangeRuleParameterEntry[] entry = EMPTY_RANGE_ENTRY;
    RangeRuleParameter param = (RangeRuleParameter) Lookup(BOTTLEDWATER_POLICY);
    entry = param.getRanges();
    return entry;
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
      RangeRuleParameterEntry[] rule = theRule.getRanges();
      if (rule != null) {
        KeyRuleParameter theAction = (KeyRuleParameter) rule[1].getValue();
        if (theAction != null)
          return theAction.getKeys();
      }
    }
    return EMPTY_KEY_ENTRY;
  }

  public KeyRuleParameterEntry[] getConditionKeys(RangeRuleParameter theRule) {
    if (theRule != null) {
      RangeRuleParameterEntry [] rule = theRule.getRanges();
      if (rule != null) {
        KeyRuleParameter theCondition = (KeyRuleParameter) rule[0].getValue();
        if (theCondition != null)
          return theCondition.getKeys();
      }
    }
    return EMPTY_KEY_ENTRY;
  }

  public KeyRuleParameterEntry[] getEnhancementsKeys(int range) {
    RangeRuleParameterEntry[] enhancements = getEnhancementsPolicyRanges();
    if (enhancements != null) {
      KeyRuleParameter krp = (KeyRuleParameter) enhancements[range].getValue();
      if (krp != null)
        return krp.getKeys();
    }
    return EMPTY_KEY_ENTRY;
  }

  public KeyRuleParameterEntry[] getMealKeys(int range) {
    RangeRuleParameterEntry[] meals = getMealPolicyRanges();
    if (meals != null) {
      KeyRuleParameter krp = (KeyRuleParameter) meals[range].getValue();
      if (krp != null)
        return krp.getKeys();
    }
    return EMPTY_KEY_ENTRY;
  }

  public KeyRuleParameterEntry[] getRangeKeys(RangeRuleParameterEntry range) {
    if (range != null) {
      KeyRuleParameter krp = (KeyRuleParameter) range.getValue();
      if (krp != null)
        return krp.getKeys();
    }
    return EMPTY_KEY_ENTRY;
  }

  public RangeRuleParameterEntry[] getRules() {
    RangeRuleParameterEntry[] entry = EMPTY_RANGE_ENTRY;
    RangeRuleParameter param = (RangeRuleParameter) Lookup(RULES);
    entry = param.getRanges();
    return entry;
  }

  public KeyRuleParameterEntry[] getWaterKeys(int range) {
    RangeRuleParameterEntry[] water = getWaterPolicyRanges();
    if (water != null) {
      KeyRuleParameter krp = (KeyRuleParameter) water[range].getValue();
      if (krp != null)
        return krp.getKeys();
    }
    return EMPTY_KEY_ENTRY;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof FeedingPolicy) {
      FeedingPolicy fp2 = (FeedingPolicy) o;
      return isSamePolicy(this.getMealPolicyRanges(), fp2.getMealPolicyRanges(), this, fp2) &&
          isSamePolicy(this.getEnhancementsPolicyRanges(), fp2.getEnhancementsPolicyRanges(), this, fp2) &&
          isSamePolicy(this.getWaterPolicyRanges(), fp2.getWaterPolicyRanges(), this, fp2) &&
          isSamePolicy(this.getARationPolicyRanges(), fp2.getARationPolicyRanges(), this, fp2) &&
          isSamePolicy(this.getRules(), fp2.getRules(), this, fp2);
    }
    return false;
  }

  public boolean isSamePolicy(RangeRuleParameterEntry[] range1, RangeRuleParameterEntry[] range2,
                              FeedingPolicy fp1, FeedingPolicy fp2) {

    if (range1.length != range2.length) {
      return false;
    }

    for (int i = 0; i < range1.length; i++) {
      if (range1[i].getRangeMin() != range2[i].getRangeMin()) {
        return false;
      }
      if (range1[i].getRangeMax() != range2[i].getRangeMax()) {
        return false;
      }

      KeyRuleParameterEntry[] keys1 = fp1.getRangeKeys(range1[i]);
      KeyRuleParameterEntry[] keys2 = fp2.getRangeKeys(range2[i]);
      for (int j = 0; j < keys1.length; j++) {
        if (!keys1[j].getValue().equals(keys2[j].getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  public static void main(String[] args) {
    RangeRuleParameterEntry[] newRangeRule = new RangeRuleParameterEntry [1];
    FeedingPolicy fp = new FeedingPolicy();
    int end = 25;
    int start = 0;

    KeyRuleParameter keyRule = new KeyRuleParameter(MEAL_POLICY);
    KeyRuleParameterEntry[] keyEntries = new KeyRuleParameterEntry[3];
    keyEntries[0] = new KeyRuleParameterEntry("Breakfast", "MRE");
    keyEntries[1] = new KeyRuleParameterEntry("Lunch", "UGR");
    keyEntries[2] = new KeyRuleParameterEntry("Dinner", "MRE");
    keyRule.setKeys(keyEntries);
    newRangeRule[0] =  new RangeRuleParameterEntry(keyRule, start, end);

    RangeRuleParameter rp = new RangeRuleParameter(MEAL_POLICY, newRangeRule);
    fp.Add(rp);

    RangeRuleParameterEntry rrpe [] = fp.getMealPolicyRanges();
    KeyRuleParameterEntry[] keys = null;
    System.out.println("what is the size " + rrpe.length);

    for (int i = 0; i < rrpe.length; i++) {
      System.out.println("C+" + rrpe[i].getRangeMin() + " To C+" + rrpe[i].getRangeMax());
      KeyRuleParameter param = (KeyRuleParameter) rrpe[i].getValue();
      keys = param.getKeys();

      for (int j = 0; j < keys.length; j++)  {
        System.out.println( keys[j].getValue());
      }
    }
    /********************************/
    newRangeRule = new RangeRuleParameterEntry [1];
    FeedingPolicy fp2 = new FeedingPolicy();
    end = 25;
    start = 0;

    keyRule = new KeyRuleParameter(MEAL_POLICY);
    keyEntries = new KeyRuleParameterEntry[3];
    keyEntries[0] = new KeyRuleParameterEntry("Breakfast", "MRE");
    keyEntries[1] = new KeyRuleParameterEntry("Lunch", "UGR");
    keyEntries[2] = new KeyRuleParameterEntry("Dinner", "MRE");
    keyRule.setKeys(keyEntries);
    newRangeRule[0] =  new RangeRuleParameterEntry(keyRule, start, end);

    rp = new RangeRuleParameter(MEAL_POLICY, newRangeRule);
    fp2.Add(rp);


    System.out.println("---------------------------- are they equal " +  fp.equals(fp2));
    System.out.println("---------------------------- are they equal " +  (fp == fp2));
  }
}
