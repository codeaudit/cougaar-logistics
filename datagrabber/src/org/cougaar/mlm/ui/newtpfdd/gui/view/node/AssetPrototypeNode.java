/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
      return getUsedAssets() == 0;
    }

}
