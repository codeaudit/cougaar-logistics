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

  private Component parentComponent;
  Hashtable orgToItems;
  Hashtable currItemToXML;

  private String invXMLStr;

  private Logger logger;


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

  private Vector getSortedOrgNames() {
    Enumeration names = orgToItems.keys();
    Vector vNames = new Vector();
    while (names.hasMoreElements())
      vNames.addElement(names.nextElement());
    Collections.sort(vNames);
    return vNames;
  }

  private void displayErrorString(String reply) {
    JOptionPane.showMessageDialog(parentComponent, reply, reply,
                                  JOptionPane.ERROR_MESSAGE);
  }
}


