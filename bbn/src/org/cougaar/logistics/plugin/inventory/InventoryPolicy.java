/*--------------------------------------------------------------------------
 *                         RESTRICTED RIGHTS LEGEND
 *
 *   Use, duplication, or disclosure by the Government is subject to
 *   restrictions as set forth in the Rights in Technical Data and Computer
 *   Software Clause at DFARS 52.227-7013.
 *
 *                             BBNT Solutions LLC,
 *                             10 Moulton Street
 *                            Cambridge, MA 02138
 *                              (617) 873-3000
 *
 *   Copyright 2000 by
 *             BBNT Solutions LLC,
 *             all rights reserved.
 *
 * --------------------------------------------------------------------------*/
package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.policy.BooleanRuleParameter;
import org.cougaar.planning.ldm.policy.IntegerRuleParameter;
import org.cougaar.planning.ldm.policy.DoubleRuleParameter;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;
import org.cougaar.planning.ldm.policy.StringRuleParameter;

public class InventoryPolicy extends Policy {
  public static final String ResourceType = "ResourceType";
  public static final String CriticalLevel = "CriticalLevel";
  public static final String SlackTime = "SlackTime";
  public static final String OrderFrequency = "OrderFrequency";
  public static final String HandlingTime = "HandlingTime";
  public static final String TransportTime = "TransportTime";

  public InventoryPolicy() {
    StringRuleParameter type = new StringRuleParameter(ResourceType);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(type);
    IntegerRuleParameter cl = new IntegerRuleParameter(CriticalLevel, 1, 40);
    try {
      cl.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(cl);
    IntegerRuleParameter st = new IntegerRuleParameter(SlackTime, 1, 40);
    try {
      st.setValue(new Integer(1));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(st);
    IntegerRuleParameter of = new IntegerRuleParameter(OrderFrequency, 1, 40);
    try {
      of.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(of);
    IntegerRuleParameter ht = new IntegerRuleParameter(HandlingTime, 0, 40);
    try {
      ht.setValue(new Integer(0));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(ht);
    IntegerRuleParameter tt = new IntegerRuleParameter(TransportTime, 1, 40);
    try {
      tt.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(tt);
  }

  public String getResourceType() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(ResourceType);
    return (String)param.getValue();
  }
  
  public void setResourceType(String name) {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(ResourceType);
    try {
      param.setValue(name);
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getCriticalLevel() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(CriticalLevel);
    return ((Integer)(param.getValue())).intValue();
  }
  
  public void setCriticalLevel(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(CriticalLevel);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getSlackTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SlackTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setSlackTime(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SlackTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getOrderFrequency() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(OrderFrequency);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setOrderFrequency(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(OrderFrequency);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getHandlingTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(HandlingTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setHandlingTime(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(HandlingTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getTransportTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(TransportTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setTransportTime(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(TransportTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }
}
