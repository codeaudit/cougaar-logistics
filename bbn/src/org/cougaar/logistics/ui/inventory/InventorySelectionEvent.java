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
    public static final int ORG_POP_SELECT = INVENTORY_SELECT+1;

    //org pop methods
    public static final String ORGS_ALL = "All";
    public static final String ORGS_NAV = "Hierarchy";
    public static final String ORGS_HIST = "History";

    public int    id;
    public String org;
    public String supplyType;
    public String assetName;
    public String orgPopMethod;


    public InventorySelectionEvent(int anID,
				  Object source, 
				  String anOrg,
				  String aSupplyType,
				  String anAssetName,
				  String anOrgPopMethod) {
	super(source);
	id = anID;
	org = anOrg;
	supplyType = aSupplyType;
	assetName = anAssetName;
	orgPopMethod = anOrgPopMethod;
    }

    public int getID() { return id; }
    public String getOrg() { return org; }
    public String getSupplyType() { return supplyType; }
    public String getAssetName() { return assetName; }
    public String getOrgPopMethod() { return orgPopMethod; }

}


