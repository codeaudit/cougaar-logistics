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
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.ProjectionEvent;

import com.bbn.openmap.event.*;
import com.bbn.openmap.layer.location.*;

public class NonMilSalesVecIcon extends VecIcon
{

    float basepixyf=.1f;
    float basepixxf=.05f;
    float pixyf=basepixyf;
    float pixxf=basepixxf;
    float lw=3;
    float lw2=5;
    int scale=1;
    OMText label;
       OMPoly bbox;

    OMGraphicList ogl=new OMGraphicList();
    String msg="Generic Non Military Sales (CA) Default";
//     public String getMessage() { return msg; }
//     public void setMessage(String str) { msg=str; }
//     public void addToMessage(String str) { msg+=": "+str; }

    public void setLabel(String lab) { label.setData(lab); }

    public String getLabel() { return label.getData(); }

    public NonMilSalesVecIcon(float lat, float lon, Color bc)
    {
      this(lat, lon, bc, Color.black, 1);
    }

    public NonMilSalesVecIcon(float lat, float lon, Color bc, Color c)
    {
      this(lat, lon, bc, c, 1);
    }

    private void setScale(int scale) {
    this.scale=scale; pixyf=scale*basepixyf; pixxf=scale*basepixxf;
    }

    int getScale() { return scale; }

    void setColor(Color bc) { 	bbox.setFillColor(bc); }

    // Constructor
    NonMilSalesVecIcon(float lat, float lon, Color bc, Color c, int sc)
    {

      setScale(sc);

      bbox = new OMPoly(
                  new float [] {
                                      lat, lon,
                                      lat, lon+pixyf,
                                      lat+pixxf, lon+pixyf,
                                      lat+pixxf, lon,
                                      lat, lon
                                  },
                                  OMGraphic.DECIMAL_DEGREES,
                                  OMGraphic.LINETYPE_STRAIGHT
                                  );
        bbox.setFillColor(bc);
        bbox.setLineColor(c);
//        bbox.setLineWidth(lw);

        OMFixedText cewiText = new OMFixedText ( lat+(pixxf/2),
                                                 lon+(pixyf/3),
                                                 0.05f,
                                                 0,
                                                 0,
                                                 "CA",
                                                 new Font ("SansSerif", Font.BOLD, 1),
                                                 OMText.JUSTIFY_LEFT);

        ogl.add(cewiText);

        OMLine supplyLine = new OMLine (lat + (pixxf/3), lon, lat + (pixxf/3), lon + pixyf, OMLine.LINETYPE_STRAIGHT );
        supplyLine.setLineColor(c);
        ogl.add(supplyLine);

        ogl.add(bbox);

    }


    // OMGraphic requirements
    public float distance(int x, int y) { return ogl.distance(x,y); }
    public void render(Graphics g) { ogl.render(g); }
    public boolean generate(Projection  x) { return ogl.generate(x); }
} // end-class  public NonMilSalesVecIcon()

