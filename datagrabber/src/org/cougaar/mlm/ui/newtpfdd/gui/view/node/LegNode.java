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
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

public class LegNode extends DBUIDNode {
    private int tripTag = TRIP_TAG;
    private int legType = DGPSPConstants.LEG_TYPE_TRANSPORTING;

    public static final int TRIP_TAG = 26;
    public static final int NON_TRIP_TAG = 27;

  public static String SEA_LEG = "Sea leg";
  public static String AIR_LEG = "Air leg";
  public static String GROUND_LEG = "Ground leg";
  public static String UNKNOWN_LEG = "Leg";

  public String carrierType = "carrier";
  public String carrierName = "?";
  
    public LegNode(UIDGenerator generator, String dbuid) {
	  super (generator, dbuid);
    }

  public String getDisplayName () {
	switch (getMode()) {
	case MODE_SEA:
	  return SEA_LEG + " - " + getLegDescrip ();
	case MODE_AIR:
	  return AIR_LEG + " - " + getLegDescrip ();
	  //	case MODE_GROUND:
	default:
	  return GROUND_LEG + " - " + getLegDescrip ();
	}
  }

  public int getLegType () { return legType; }
  public String getLegDescrip () {
	return DGPSPConstants.LEG_TYPES[legType];
  }
  public String getCarrierType () 
  {
	return carrierType;
  }

  public void setCarrierType (String c) 
  {
	carrierType=c;
  }

  public String getCarrierName () 
  {
	return carrierName;
  }

  public void setCarrierName (String c) 
  {
	carrierName=c;
  }
  
  public void setLegType (int type) { legType = type; }
  
  public int getType () { return UIDGenerator.LEG; }

  public boolean isTransport() { return true; }

    public int getTripTag() { return tripTag; }
    public void setTripTag(int tripTag) { this.tripTag = tripTag; }

}
