/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

public class CargoType extends DBUIDNode {
    private String cargoName;
    private String cargoType;
    private String cargoNameDBID;
    private String cargoTypeDBID;
  private boolean isLowFi;
    
    public CargoType(UIDGenerator generator, String dbuid) {
	  super (generator, dbuid);
	}

    public String getDisplayName() { return super.getDisplayName() + " (" + getChildCount() + ")"; }

  public int getType () { return UIDGenerator.ASSET_PROTOTYPE; }

    public String getCargoName() { return cargoName; }
    public void setCargoName(String cargoName) { this.cargoName = cargoName; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public String getCargoNameDBID() { return cargoNameDBID; }
    public void setCargoNameDBID(String cargoNameDBID) { this.cargoNameDBID = cargoNameDBID; }

    public String getCargoTypeDBID() { return cargoTypeDBID; }
    public void setCargoTypeDBID(String cargoTypeDBID) { this.cargoTypeDBID = cargoTypeDBID; }

    public boolean isLowFi() { return isLowFi; }
    public void setLowFi(boolean val) { this.isLowFi = val; }
}
