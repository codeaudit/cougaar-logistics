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
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.*;



class ColorCodeUnit {

    class ColorCodeMaxValue {
	Color c;
	Float f;
	ColorCodeMaxValue(Color c, float f) { this.c=c; this.f=new Float(f); }
	Float getValue() { return f; }
	Color getColor() { return c; }
    }
    
    Vector v=new Vector();
    ColorCodeUnit() 
    { v.add(new ColorCodeMaxValue(Color.black, Float.MAX_VALUE)); }
    void add(Color c, float f) {
	add(new ColorCodeMaxValue(c, f));
    }
    void add(ColorCodeMaxValue ccmv) {
	int idx;
	for (idx=0; idx<v.size(); idx++) {
	    if (ccmv.getValue().floatValue() < ((ColorCodeMaxValue)v.elementAt(idx)).getValue().floatValue()) {
		break;
	    }
	}
	v.insertElementAt(ccmv, idx);
	}
    void setColor(Unit unit, String metric) {
	int idx;
	ColorCodeMaxValue ccmv=null;
	Float myvalue=unit.getData(metric);
	if (myvalue==null) { 
	    // myvalue=new Float(Float.MAX_VALUE); 
		System.err.println("Null myvalue for unit/metric: "+unit+" / "+metric); 
	    } else {
		for ( idx=0; idx<v.size(); idx++) {
		    ccmv=((ColorCodeMaxValue)v.elementAt(idx));
		    if (myvalue !=null && 
			myvalue.floatValue() < ccmv.getValue().floatValue()) {
			break;
		    }
		}
	    }
	    Color color=(ccmv!=null) ? ccmv.getColor() : Color.white;
	    unit.setColor(color);
	    System.err.println("unit.setColor for unit/metric: "+unit+" / "+metric+" / "+color); 
	    
	}
    }
