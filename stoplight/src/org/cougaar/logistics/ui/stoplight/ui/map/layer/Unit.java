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

import java.io.*;
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
//import com.bbn.openmap.layer.location.*;
// import assessment.*;
import org.cougaar.logistics.ui.stoplight.transducer.XmlInterpreter;
import org.cougaar.logistics.ui.stoplight.transducer.elements.Structure;


public class Unit {
	String label;
	OMGraphic graphic;
	Hashtable data;

	public Unit (String str, OMGraphic omg, Hashtable ht)
  {
 	    label=str;
	    graphic=omg;
	    data=ht;
	}

	Float getData(String metric)
  {
	    Float ret=null;
	    Object fl=data.get(metric);
	    if (fl!=null && fl instanceof Float) ret=(Float)fl;
	    return ret;
	}

	void setColor(Color c)
  {
	    if (graphic instanceof VecIcon) {
		((VecIcon)graphic).setColor(c); 
	    }

	}
	public OMGraphic getGraphic() { return graphic; }
	public String getLabel() { return label; }
    }

