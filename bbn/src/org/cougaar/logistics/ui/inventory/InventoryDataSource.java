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

import java.util.Vector;
import java.util.Hashtable;


/** 
 * <pre>
 * 
 * The InventoryDataSource interface provides the methods for the 
 * data model of the Logistics Inventory UI.    There will be two
 * methods of data model.   Initially a connection to a servlet and
 * additionally data files.
 * 
 * @see InventoryConnectionManager
 * @see InventoryFileManager
 *
 **/
public interface InventoryDataSource
{
  Vector getOrgNames(String agentPath,String orgsPopMethod);
  String[] getSupplyTypes();
  Vector getAssetNames(String orgName, String supplyType);
  String getInventoryData(String orgName, String assetName);
  String getCurrentInventoryData();
    //String getDefaultAssetName();
    //String getDefaultOrganizationName();
}


