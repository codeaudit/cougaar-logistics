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
  public static final String InventoryManagerSpecifier = "InventoryManagerSpecifier";
  public static final String CriticalLevel = "CriticalLevel";
  public static final String SupplierAdvanceNoticeTime = "SupplierAdvanceNoticeTime";
  public static final String ReorderPeriod = "ReorderPeriod";
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
    StringRuleParameter specifier = new StringRuleParameter(InventoryManagerSpecifier);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(specifier);
    IntegerRuleParameter cl = new IntegerRuleParameter(CriticalLevel, 1, 40);
    try {
      cl.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(cl);
    IntegerRuleParameter st = new IntegerRuleParameter(SupplierAdvanceNoticeTime, 1, 40);
    try {
      st.setValue(new Integer(1));
    } catch (RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
    Add(st);
    IntegerRuleParameter of = new IntegerRuleParameter(ReorderPeriod, 1, 40);
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

  public String getInventoryManagerSpecifier() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(InventoryManagerSpecifier);
    return (String)param.getValue();
  }
  
  public void setInventoryManagerSpecifier(String name) {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(InventoryManagerSpecifier);
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

  public int getSupplierAdvanceNoticeTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SupplierAdvanceNoticeTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setSupplierAdvanceNoticeTime(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SupplierAdvanceNoticeTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      System.out.println(ex);
    }
  }

  public int getReorderPeriod() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(ReorderPeriod);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setReorderPeriod(int i) {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(ReorderPeriod);
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
