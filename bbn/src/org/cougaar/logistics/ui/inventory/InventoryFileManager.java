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

package org.cougaar.logistics.ui.inventory;

import java.util.Hashtable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import java.awt.Component;
import javax.swing.JOptionPane;

import java.io.InputStream;
import java.io.ObjectInputStream;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/**
 * <pre>
 *
 * The InventoryFileManager manages all the files opened
 * and behaves like an InventoryDataSource to serve up
 * all the charts that have been opened.
 *
 *
 * @see InventoryDataSource
 * @see InventoryConnectionManager
 * @see InventoryUIFrame
 *
 **/

public class InventoryFileManager implements InventoryDataSource {

  final public static String[] SUPPLY_CLASS_TYPES = {"Ammunition", "BulkPOL", "Subsistence", "ClassVIIIMedical", "PackagedPOL", "Consumable"};
  final public static String[] FILE_CLASS_TYPES = {"File"};

  protected Component parentComponent;
  protected Hashtable orgToItems;
  protected Hashtable currItemToXML;

  protected String invXMLStr;

  protected Logger logger;


  public InventoryFileManager(Component parent) {
    parentComponent = parent;
    logger = Logging.getLogger(this);
    orgToItems = new Hashtable();
    currItemToXML = null;
  }

  public String getCurrentInventoryData() {
    return invXMLStr;
  }

  public static String getFullItemName(InventoryData header) {
    String itemNomenclature = null;
    String nomenclature = header.getNomenclature();
    if ((nomenclature == null) ||
        (nomenclature.trim().equals(""))) {
      itemNomenclature = header.getItem();
    } else {
      if (isClassOfSupply(nomenclature)) {
        itemNomenclature = header.getItem() + ":"
            + nomenclature;
      } else {
        itemNomenclature = nomenclature + ":"
            + header.getItem();
      }
    }
    return itemNomenclature;
  }

  public void addItem(InventoryData header, String xmlStr) {
    String itemNomenclature = getFullItemName(header);
    currItemToXML = (Hashtable) orgToItems.get(header.getOrg());
    if (currItemToXML == null) {
      currItemToXML = new Hashtable();
      orgToItems.put(header.getOrg(), currItemToXML);
    }
    currItemToXML.put(itemNomenclature, xmlStr);
    invXMLStr = xmlStr;
  }


  public static boolean isClassOfSupply(String nomen) {
    for (int i = 0; i < SUPPLY_CLASS_TYPES.length; i++) {
      String aSupplyClass = SUPPLY_CLASS_TYPES[i];
      if (nomen.equals(aSupplyClass)) {
        return true;
      }
    }
    return false;
  }

  public String getInventoryData(String orgName, String assetName) {
    if (logger.isDebugEnabled()) {
      logger.debug("getInventoryData: OrgName: " + orgName +
                   " AssetName: " + assetName);
    }
    currItemToXML = (Hashtable) orgToItems.get(orgName);
    invXMLStr = (String) currItemToXML.get(assetName);
    return invXMLStr;
  }


  public Vector getAssetNames(String orgName, String supplyType) {
    currItemToXML = (Hashtable) orgToItems.get(orgName);
    Enumeration keys = currItemToXML.keys();
    Vector assetNames = new Vector();
    while (keys.hasMoreElements()) {
      assetNames.addElement(keys.nextElement());
    }
    Collections.sort(assetNames);
    return assetNames;
  }

  public String[] getSupplyTypes() {
    return FILE_CLASS_TYPES;
  }

  public String getDefaultOrganizationName() {
    Vector orgNames = getSortedOrgNames();
    if (orgNames == null) return null;
    return (String) orgNames.elementAt(1);
  }

  public Vector getOrgNames(String agentPath,String orgPopMethod) {
    logger.debug("Getting Org List");
    return getSortedOrgNames();
  }

  protected Vector getSortedOrgNames() {
    Enumeration names = orgToItems.keys();
    Vector vNames = new Vector();
    while (names.hasMoreElements())
      vNames.addElement(names.nextElement());
    Collections.sort(vNames);
    return vNames;
  }

  protected void displayErrorString(String reply) {
    JOptionPane.showMessageDialog(parentComponent, reply, reply,
                                  JOptionPane.ERROR_MESSAGE);
  }
}


