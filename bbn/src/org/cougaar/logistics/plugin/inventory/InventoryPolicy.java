/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.planning.ldm.policy.BooleanRuleParameter;
import org.cougaar.planning.ldm.policy.IntegerRuleParameter;
import org.cougaar.planning.ldm.policy.DoubleRuleParameter;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;
import org.cougaar.planning.ldm.policy.StringRuleParameter;

public class InventoryPolicy extends Policy {
  public static final String AgentID = "AgentID";
  public static final String ResourceType = "ResourceType";
  public static final String InventoryManagerSpecifier = "InventoryManagerSpecifier";
  public static final String CriticalLevel = "CriticalLevel";
  public static final String SupplierAdvanceNoticeTime = "SupplierAdvanceNoticeTime";
  public static final String ReorderPeriod = "ReorderPeriod";
  public static final String HandlingTime = "HandlingTime";
  public static final String TransportTime = "TransportTime";
  public static final String OrderShipTime = "OrderShipTime";

  public InventoryPolicy() {
    StringRuleParameter id = new StringRuleParameter(AgentID);
    String exception;
    try {
      id.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(id);
    StringRuleParameter type = new StringRuleParameter(ResourceType);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(type);
    StringRuleParameter specifier = new StringRuleParameter(InventoryManagerSpecifier);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(specifier);
    IntegerRuleParameter cl = new IntegerRuleParameter(CriticalLevel, 1, 40);
    try {
      cl.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(cl);
    IntegerRuleParameter st = new IntegerRuleParameter(SupplierAdvanceNoticeTime, 1, 100);
    try {
      st.setValue(new Integer(1));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(st);
    IntegerRuleParameter of = new IntegerRuleParameter(ReorderPeriod, 1, 40);
    try {
      of.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(of);
    IntegerRuleParameter ht = new IntegerRuleParameter(HandlingTime, 0, 40);
    try {
      ht.setValue(new Integer(0));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(ht);
    IntegerRuleParameter tt = new IntegerRuleParameter(TransportTime, 1, 40);
    try {
      tt.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(tt);
    IntegerRuleParameter ost = new IntegerRuleParameter(OrderShipTime, 1, 40);
    try {
      ost.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
    Add(ost);
  }

  public String getAgentID() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(AgentID);
    return (String)param.getValue();
  }
  
  public void setAgentID(String name) {
    String exception;
    StringRuleParameter param = (StringRuleParameter)
      Lookup(AgentID);
    try {
      param.setValue(name);
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public String getResourceType() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(ResourceType);
    return (String)param.getValue();
  }
  
  public void setResourceType(String name) {
    String exception;
    StringRuleParameter param = (StringRuleParameter)
      Lookup(ResourceType);
    try {
      param.setValue(name);
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public String getInventoryManagerSpecifier() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(InventoryManagerSpecifier);
    return (String)param.getValue();
  }
  
  public void setInventoryManagerSpecifier(String name) {
    String exception;
    StringRuleParameter param = (StringRuleParameter)
      Lookup(InventoryManagerSpecifier);
    try {
      param.setValue(name);
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getCriticalLevel() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(CriticalLevel);
    return ((Integer)(param.getValue())).intValue();
  }
  
  public void setCriticalLevel(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(CriticalLevel);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getSupplierAdvanceNoticeTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SupplierAdvanceNoticeTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setSupplierAdvanceNoticeTime(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(SupplierAdvanceNoticeTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getReorderPeriod() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(ReorderPeriod);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setReorderPeriod(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(ReorderPeriod);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getHandlingTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(HandlingTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setHandlingTime(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(HandlingTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getTransportTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(TransportTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setTransportTime(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(TransportTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

  public int getOrderShipTime() {
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(OrderShipTime);
    return ((Integer)(param.getValue())).intValue();
  }

  public void setOrderShipTime(int i) {
    String exception;
    IntegerRuleParameter param = (IntegerRuleParameter)
      Lookup(OrderShipTime);
    try {
      param.setValue(new Integer(i));
    } catch(RuleParameterIllegalValueException ex) {
      exception = ex.toString();
      // Print exception, waiting for a static logger
    }
  }

}
