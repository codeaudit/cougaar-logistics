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
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import com.bbn.openmap.LatLonPoint;

import com.bbn.openmap.Layer;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.ProjectionEvent;

import com.bbn.openmap.event.*;
import com.bbn.openmap.layer.location.*;

public class VecIcon extends OMGraphic {
    public static long maxNumber = -1;

    public long locationNumber = -1;

    public AssetBarGraphic assetBarGraphic = null;

    float basepixyf=.2f;
    float basepixxf=.1f;
    float pixyf=basepixyf;
    float pixxf=basepixxf;
    float lw=3;
    float lw2=5;
    int scale=1;
    String defaultLabel="Label";
    OMText label;
    OMPoly bbox;
    OMGraphicList ogl=new OMGraphicList();
    static String msg="Generic Default";
    float lat1, lon1;
    float lat2, lon2; 
    Color bgc, fgc;

    public VecIcon() { }
    public VecIcon(float lat, float lon, Color bc) {
 	this(lat, lon, bc, Color.black, 1);
    }

    public VecIcon(float lat, float lon, Color bc, Color c) {
 	this(lat, lon, bc, c, 1);
    }

    public VecIcon(float lat, float lon, Color bc, Color c, int sc) {
	init(lat, lon, bc, c, sc);
    }
    public void init(float lat, float lon, Color bc, Color c, int sc) {
	bgc=bc; fgc=c;
	initLocation(lat, lon);
	initScale(sc);
	initLabel();
	initSymbol();
	initBoundingBox();
    }

    String msgAddon="";
    public String getMessage() { return msg; }
    public String getFullMessage() { return msg+msgAddon; }
    public void setMessage(String str) { msg=str; }
    public void addToMessage(String str) { msgAddon+=" "+str; }
    public String getMessageAddon() { return msgAddon;  }
    public void setMessageAddon(String str) { msgAddon=" "+str; }
    public void setLabel(String lab) { label.setData(lab); }
    public String getLabel() { return label.getData(); }


    protected void initScale(int scale) { 
	this.scale=scale; pixyf=scale*basepixyf; pixxf=scale*basepixxf; 
    }
    public void initLabel() {
	initLabel(defaultLabel);
    }
    public void initLabel(String str) {
	label=new OMText(lat1+(pixxf/2), lon1, 3, 1, str,
				OMText.JUSTIFY_RIGHT);
  label.setShowBounds(true);
  label.setBoundsFillColor(Color.white);
	ogl.add(label);
    }

    public void changeLocation (float lat, float lon)
    {
      initLocation (lat, lon);
      if (label != null)
      {
        label.setLat(lat);
        label.setLon(lon);
      }
    }

    public void initLocation(float lat, float lon) {
	float lat1a, lon1a;
	float lat2a, lon2a;
	lat1a=lat;
	lat2a=lat+pixxf;
	lon1a=lon;
	lon2a=lon+pixyf;
	lat1=Math.min(lat1a, lat2a);
	lon1=Math.min(lon1a, lon2a);
	lat2=Math.max(lat1a, lat2a);
	lon2=Math.max(lon1a, lon2a);
    }

    protected void initBoundingBox() {
	float[] bboxpoints= new float [] {
				      lat1, lon1, 
				      lat1, lon2,
				      lat2, lon2, 
				      lat2, lon1,
				      lat1, lon1
	};
	bbox = new OMPoly(bboxpoints, OMGraphic.DECIMAL_DEGREES, 
			  OMGraphic.LINETYPE_STRAIGHT);
	bbox.setFillColor(bgc);
	bbox.setLineColor(fgc);
//	bbox.setLineWidth(lw);
 	ogl.add(bbox);
   }

    // default;  override for other symbols
    protected void initSymbol() {
	float[] symbolPoints=new float [] {lat1, lon1, lat1, lon2};
	initSymbol(symbolPoints);
   }

    protected void initSymbol(float [] polypoints) {
 	OMPoly poly = new OMPoly(  polypoints,
				 OMGraphic.DECIMAL_DEGREES,
				 OMGraphic.LINETYPE_STRAIGHT
				 );
	poly.setLineColor(fgc);
//	poly.setLineWidth(lw2);
	ogl.add(poly);
   }

    int getScale() { return scale; }
    void setColor(Color bc) { 	bbox.setFillColor(bc); }

    public float getLatLocation () { return lat1; }
    public float getLonLocation () { return lon1; }

    // OMGraphic requirements
    public float distance(int x, int y) { return ogl.distance(x,y); }
    public void render(Graphics g)
    {
      ogl.render(g);
      if (assetBarGraphic != null)
      {
        assetBarGraphic.render(g, label.getPolyBounds().getBounds());
      }
    }

    public boolean generate(Projection  x)
    {
//      return(ogl.generate(x) && assetBarGraphic.generate(x));
      return(ogl.generate(x));
    }
} // end-class
