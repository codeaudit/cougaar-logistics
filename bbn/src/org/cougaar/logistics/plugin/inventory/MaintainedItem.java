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
  protected transient UtilsProvider utilsPlugin;

  protected String maintainedItemType = null;
  protected String typeIdentification = null;
  protected String itemIdentification = null;
  protected String nomenclature = null;
  private static HashMap cache = new HashMap();

 
  public MaintainedItem() {}

  public MaintainedItem(String type, String typeId, String itemId, String nomen, UtilsProvider aPlugin) {
    maintainedItemType = type;
    typeIdentification = typeId;
    itemIdentification = itemId;
    nomenclature = nomen;
    utilsPlugin = aPlugin;
    if(utilsPlugin == null) {
	logger = NullLoggingServiceImpl.getLoggingService();
    }
    else {
	logger = (Logger)utilsPlugin.getLoggingService(this);
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

  public static MaintainedItem findOrMakeMaintainedItem(String type, String typeId, String itemId, String nomen, UtilsProvider aPlugin) {
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




