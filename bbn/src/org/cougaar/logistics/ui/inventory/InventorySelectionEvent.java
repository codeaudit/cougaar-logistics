/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory;

import javax.swing.event.ChangeEvent;


/** 
 * <pre>
 * 
 * The InventorySelectionEvent has the select events made in the 
 * InventorySelectionPanel to aid the 
 * mechanism to broadcast the given action event to other parts
 * of the inventory UI.
 * 
 * 
 * @see InventorySelectionListener
 *
 **/

public class InventorySelectionEvent extends ChangeEvent 
{
    
    public static final int ORG_SELECT=1;
    public static final int INVENTORY_SELECT=ORG_SELECT+1;

    public int    id;
    public String org;
    public String supplyType;
    public String assetName;


    public InventorySelectionEvent(int anID,
				  Object source, 
				  String anOrg,
				  String aSupplyType,
				  String anAssetName) {
	super(source);
	id = anID;
	org = anOrg;
	supplyType = aSupplyType;
	assetName = anAssetName;
    }

    public int getID() { return id; }
    public String getOrg() { return org; }
    public String getSupplyType() { return supplyType; }
    public String getAssetName() { return assetName; }

}


