/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.event.ProjectionEvent;

import com.bbn.openmap.LatLonPoint;

import com.bbn.openmap.event.*;
import com.bbn.openmap.layer.location.*;

public class SelectionGraphic extends OMGraphic
{
    private OMPoly bbox;
    private OMGraphicList ogl = new OMGraphicList();

    private float[] bboxpoints = new float[] {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

    private float lat1 = 0.0f;
    private float lon1 = 0.0f;
    private float lat2 = 0.0f;
    private float lon2 = 0.0f;

    public SelectionGraphic(Color color)
    {
    	bbox = new OMPoly(bboxpoints, OMGraphic.DECIMAL_DEGREES, OMGraphic.LINETYPE_STRAIGHT);
    	bbox.setLinePaint (color);
     	ogl.add(bbox);
     	
     	setVisible(false);
    }

    public void setSelection(float lat1, float lon1, float lat2, float lon2)
    {
      bboxpoints[0] = this.lat1 = lat1;
      bboxpoints[1] = this.lon1 = lon1;
      bboxpoints[2] = this.lat1 = lat1;
      bboxpoints[3] = this.lon2 = lon2;
      bboxpoints[4] = this.lat2 = lat2;
      bboxpoints[5] = this.lon2 = lon2;
      bboxpoints[6] = this.lat2 = lat2;
      bboxpoints[7] = this.lon1 = lon1;
      bboxpoints[8] = this.lat1 = lat1;
      bboxpoints[9] = this.lon1 = lon1;

      bbox.setLocation(bboxpoints, OMGraphic.DECIMAL_DEGREES);
    }

    private Rectangle rect = new Rectangle();
    public boolean contains(Projection proj, Point point)
    {
      return(getGraphicBounds(proj, rect).contains(point));
    }

    private Point pt1 = new Point();
    private Point pt2 = new Point();
    private Point pt3 = new Point();
    private Point pt4 = new Point();
    private Point[] corners = new Point[4];
    public Rectangle getGraphicBounds(Projection proj, Rectangle storage)
    {
      corners[0] = proj.forward(lat1, lon1, pt1);
      corners[1] = proj.forward(lat1, lon2, pt2);
      corners[2] = proj.forward(lat2, lon2, pt3);
      corners[3] = proj.forward(lat2, lon1, pt4);

      Point ulC = corners[0];
      for (int i=1; i<4; i++)
      {
        if ((ulC.x > corners[i].x) || (ulC.y > corners[i].y))
        {
          ulC = corners[i];
        }
      }
      
      Point lrC = corners[0];
      for (int i=1; i<4; i++)
      {
        if ((lrC.x < corners[i].x) || (lrC.y < corners[i].y))
        {
          lrC = corners[i];
        }
      }

      storage.x = ulC.x;
      storage.y = ulC.y;
      storage.width = lrC.x - ulC.x;
      storage.height = lrC.y - ulC.y;

      return(storage);
    }

    public void setColor(Color color) { 	bbox.setLinePaint (color); }

    // OMGraphic requirements
    public float distance(int x, int y) { return ogl.distance(x,y); }
    public void render(Graphics g) { ogl.render(g); }
    public boolean generate(Projection  x) { return ogl.generate(x); }
} // end-class
