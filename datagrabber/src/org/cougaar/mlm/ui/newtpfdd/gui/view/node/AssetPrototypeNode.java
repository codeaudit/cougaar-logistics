/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.node;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;

public class AssetPrototypeNode extends DBUIDNode {
    private String assetName;
    private String assetType;
  private int totalAssets;
  private int usedAssets;

    private boolean isSelfPropelled = false;
  
    public AssetPrototypeNode (UIDGenerator generator, String dbuid) {
	super (generator, dbuid);
    }
    
    public String getDisplayName() { return super.getDisplayName() + " (" +usedAssets + "/" + totalAssets+ ")"; }
    
    public int getType () { return UIDGenerator.ASSET_PROTOTYPE; }
    
    public String getAssetPrototypeName() { return assetName; }
    public void setAssetPrototypeName(String assetName) { this.assetName = assetName; }

    public String getAssetPrototypeType() { return assetType; }
    public void setAssetPrototypeType(String assetType) { this.assetType = assetType; }

    public boolean isSelfPropelled() { return isSelfPropelled; }
    public void setSelfPropelled(boolean selfProp) { this.isSelfPropelled = selfProp; }

  public void setTotalAssets(int totalAssets) { this.totalAssets = totalAssets; }
  public int getTotalAssets() { return totalAssets; }

  public void setUsedAssets(int usedAssets) { this.usedAssets = usedAssets; }
  public int getUsedAssets() { return usedAssets; }

    public boolean isLeaf() {
      if (getUsedAssets() == 0) return true;
      else return false;
    }

}
