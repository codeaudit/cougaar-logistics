/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
 */

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.policy.BooleanRuleParameter;
import org.cougaar.planning.ldm.policy.IntegerRuleParameter;
import org.cougaar.planning.ldm.policy.DoubleRuleParameter;
import org.cougaar.planning.ldm.policy.LongRuleParameter;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;
import org.cougaar.planning.ldm.policy.StringRuleParameter;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

import java.io.ObjectInputStream;
import java.io.IOException;

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
  public static final String BucketSize = "BucketSize";
  public static final String FillToCapacity = "FillToCapacity";
  public static final String SupplierArrivalTime = "SupplierArrivalTime";

  public SimpleDateFormat dateFormatter;
  private transient Logger logger;

  public InventoryPolicy() {
    dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

    logger = Logging.getLogger(this);

    StringRuleParameter id = new StringRuleParameter(AgentID);
    String exception;
    try {
      id.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error(ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(id);
    StringRuleParameter type = new StringRuleParameter(ResourceType);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("ResourceType-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(type);
        StringRuleParameter arrivalDate = new StringRuleParameter(SupplierArrivalTime);
    try {
      arrivalDate.setValue(null);
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("SupplierArrivalTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(arrivalDate);
    StringRuleParameter specifier = new StringRuleParameter(InventoryManagerSpecifier);
    try {
      type.setValue(new String());
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("InventoryManagerSpecifier-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(specifier);
    IntegerRuleParameter cl = new IntegerRuleParameter(CriticalLevel, 1, 40);
    try {
      cl.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("CriticalLevel-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(cl);
    IntegerRuleParameter st = new IntegerRuleParameter(SupplierAdvanceNoticeTime, 1, 100);
    try {
      st.setValue(new Integer(1));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("SupplierAdvanceNoticeTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(st);
    IntegerRuleParameter of = new IntegerRuleParameter(ReorderPeriod, 1, 40);
    try {
      of.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("ReorderPeriod-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(of);
    IntegerRuleParameter ht = new IntegerRuleParameter(HandlingTime, 0, 40);
    try {
      ht.setValue(new Integer(0));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("HandlingTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(ht);
    IntegerRuleParameter tt = new IntegerRuleParameter(TransportTime, 1, 40);
    try {
      tt.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("TransportTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(tt);
    IntegerRuleParameter ost = new IntegerRuleParameter(OrderShipTime, 1, 40);
    try {
      ost.setValue(new Integer(3));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("OrderShipTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
    Add(ost);
    LongRuleParameter bucket = new LongRuleParameter(BucketSize, TimeUtils.MSEC_PER_HOUR, Long.MAX_VALUE);
    try {
      bucket.setValue(new Long(TimeUtils.MSEC_PER_DAY));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("BucketSize-" + ex.toString());
      }
    }
    Add(bucket);
    BooleanRuleParameter fillToCap = new BooleanRuleParameter(FillToCapacity, false);
    try {
      fillToCap.setValue(new Boolean(false));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("FillToCapacity-" + ex.toString());
      }
    }
    Add(fillToCap);
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting AgentID-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting ResourceType-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
  }

  public long getSupplierArrivalTime() {
    StringRuleParameter param = (StringRuleParameter)
      Lookup(SupplierArrivalTime);
      if(param == null){
       return -1;
      }
      String arrivalDateStr = (String)param.getValue();
      if((arrivalDateStr == null) ||
	 (arrivalDateStr.trim().equals(""))){
        return -1;
      }
      try {
        Date arrivalDate = dateFormatter.parse(arrivalDateStr);
        return arrivalDate.getTime();
      }
      catch(Exception ex) {
        logger.error(ex.toString());
        return -1;
      }
  }

  public void setSupplierArrivalTime(long arrivalTime) {
    String exception;
    StringRuleParameter param = (StringRuleParameter)
      Lookup(SupplierArrivalTime);
    String arrivalTimeStr = dateFormatter.format(new Date(arrivalTime));
    try {
      param.setValue(arrivalTimeStr);
    } catch(RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Setting SupplierArrivalTime-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting InventoryManagerSpecifier-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting CriticalLevel-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting SupplierAdvanceNoticeTime-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting ReorderPeriod-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting HandlingTime-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Setting TransportTime-" + ex.toString());
      }
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
      if (logger.isErrorEnabled()) {
        logger.error("Settting OrderShipTime-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
  }

  public long getBucketSize() {
    LongRuleParameter param = (LongRuleParameter)
      Lookup(BucketSize);
    return ((Long)(param.getValue())).longValue();
  }

  public void setBucketSize(long l) {
    String exception;
    LongRuleParameter param = (LongRuleParameter)
      Lookup(BucketSize);
    try {
      param.setValue(new Long(l));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Setting BucketSize-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
  }

  public boolean getFillToCapacity() {
    BooleanRuleParameter param = (BooleanRuleParameter)
      Lookup(FillToCapacity);
    return ((Boolean)(param.getValue())).booleanValue();
  }

  public void setFillToCapacity(boolean b) {
    String exception;
    BooleanRuleParameter param = (BooleanRuleParameter)
      Lookup(FillToCapacity);
    try {
      param.setValue(new Boolean(b));
    } catch (RuleParameterIllegalValueException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Setting FillToCapacity-" + ex.toString());
      }
      // Print exception, waiting for a static logger
    }
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    logger =  Logging.getLogger(this);
  }

}
