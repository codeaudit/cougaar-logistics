/* **********************************************************************
 * 
 *  BBNT Solutions LLC, A part of GTE
 *  10 Moulton St.
 *  Cambridge, MA 02138
 *  (617) 873-2000
 * 
 *  Copyright (C) 1998, 2000
 *  This software is subject to copyright protection under the laws of 
 *  the United States and other countries.
 * 
 * **********************************************************************
 * 
 * 
 * 
 * 
 * 
 * 
 * **********************************************************************
 */

package org.cougaar.logistics.ui.stoplight.ui.map.layer;
import java.awt.*;

// import java.awt.event.*;
// import javax.swing.*;
// import java.util.*;

public class ArmoredVecIcon extends VecIcon {
    {  // static initialization 
	msg="Generic Armored Default";
    }

    public ArmoredVecIcon() { }
    public ArmoredVecIcon(float lat, float lon, Color bc) {
 	this(lat, lon, bc, Color.black, 1);
    }

    public ArmoredVecIcon(float lat, float lon, Color bc, Color c) {
 	this(lat, lon, bc, c, 1);
    }

    public ArmoredVecIcon(float lat, float lon, Color bc, Color c, int sc) {
	super(lat, lon, bc, c, sc);
    }

    public void initSymbol() {
	float lat1a, lon1a;
	float lat2a, lon2a;
	float ilat1, ilon1;
	float ilat2, ilon2;

	lat1a=lat1+pixxf*.33f;
	lat2a=lat1+pixxf*.66f;
	lon1a=lon1+pixyf*.33f;
	lon2a=lon1+pixyf*.66f;
	ilat1=Math.min(lat1a, lat2a);
	ilon1=Math.min(lon1a, lon2a);
	ilat2=Math.max(lat1a, lat2a);
	ilon2=Math.max(lon1a, lon2a);

	float[] symbolPoints;
	symbolPoints=new float [] {ilat1, ilon1, ilat1, ilon2, 
				   ilat2, ilon2, ilat2, ilon1,
				   ilat1, ilon1 };
	initSymbol(symbolPoints);
    }
} // end-class
