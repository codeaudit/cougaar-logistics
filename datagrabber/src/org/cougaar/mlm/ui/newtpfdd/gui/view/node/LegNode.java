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
