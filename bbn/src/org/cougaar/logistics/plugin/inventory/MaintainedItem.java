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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Trimmed down, representation of an asset
 **/
public class MaintainedItem implements Serializable {
    
  protected transient Logger logger;
  protected transient InventoryPlugin invPlugin;

  protected String maintainedItemType = null;
  protected String typeIdentification = null;
  protected String itemIdentification = null;
  protected String nomenclature = null;
  private static HashMap cache = new HashMap();

 
  public MaintainedItem() {}

  public MaintainedItem(String type, String typeId, String itemId, String nomen, InventoryPlugin aPlugin) {
    maintainedItemType = type;
    typeIdentification = typeId;
    itemIdentification = itemId;
    nomenclature = nomen;
    invPlugin = invPlugin;
    if(aPlugin == null) {
	logger = NullLoggingServiceImpl.getNullLoggingServiceImpl();
    }
    else {
	logger = (Logger)aPlugin.getLoggingService(this);
    }
  }

  public String getMaintainedItemType() {
    return maintainedItemType;
  }

  public String getTypeIdentification() {
    return typeIdentification;
  }

  public String getItemIdentification() {
    return itemIdentification;
  }

  public String getNomenclature() {
    return nomenclature;
  }

  public static MaintainedItem findOrMakeMaintainedItem(String type, String typeId, String itemId, String nomen, InventoryPlugin aPlugin) {
    if (type == null || typeId == null) {
	if(aPlugin != null) {
	    Logger aLogger = (Logger)aPlugin.getLoggingService("MaintainedItem");
	    aLogger.error("Type and/or TypeIdentification cannot be null");
	}
      return null;
    }
    String key = type+typeId;
    if (itemId != null) key = key+itemId;
    MaintainedItem item = (MaintainedItem)cache.get(key);
    if (item == null) {
      item = new MaintainedItem(type, typeId, itemId, nomen, aPlugin);
      cache.put(key, item);
    }
    return item;
  }

  public String toString() {
    return this.getClass().getName()+": <"+maintainedItemType+">, <"+
	typeIdentification+">, <"+itemIdentification+">, <"+nomenclature+">";
  }

  private transient int _hc = 0;
  public int hashCode() {
    if (_hc != 0) return _hc;
    int hc = 1;
    if (maintainedItemType != null) hc+=maintainedItemType.hashCode();
    if (typeIdentification != null) hc+=typeIdentification.hashCode();
    if (itemIdentification != null) hc+=itemIdentification.hashCode();
    _hc = hc;
    return hc;
  }

  /** Equals for MaintainedItems is equivalent to the test for equal
   *  assets.  Assumes
   **/
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(this.getClass() == o.getClass())) return false;
    MaintainedItem oi = (MaintainedItem)o;
    String oType = oi.getMaintainedItemType();
    if (oType == null || maintainedItemType == null || !(maintainedItemType.equals(oType))) return false;
    String oTypeID = oi.getTypeIdentification();
    if (oTypeID == null || typeIdentification == null || !(typeIdentification.equals(oTypeID))) return false;
    String oItemID = oi.getItemIdentification();
    if (oItemID == itemIdentification) return true;
    if (itemIdentification != null) {
      return itemIdentification.equals(oItemID);
    } else {
      return false;
    }
  }
}




