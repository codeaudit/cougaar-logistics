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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Graphics;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMArrowHead;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.proj.Projection;

import java.text.SimpleDateFormat;

/**
 * A single segment in the route
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 4/19/01
 **/
public class RouteSegment extends OMLine {
  public static final int lineWidth=5;

  protected static final Stroke thinStroke=new BasicStroke(lineWidth);
  protected static final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm");
  protected static final String lightFontColor = "gray";
  protected static final String assetFontColor = "black";
  protected static final String locFontColor  = "black";
  protected static final String dateFontColor  = "black";
  protected static final String descriptiveFontColor  = "black";

  protected static final String statusFontSize=
    System.getProperty ("RouteSegment.statusFontSize","");

  //Constants:
  ////////////
  public static final int ARROW_FORWARD=0;
  public static final int ARROW_BACKWARD=1;
  
  public static final int MODE_UNKNOWN=0;
  public static final int MODE_GROUND=1;
  public static final int MODE_SEA=2;
  public static final int MODE_AIR=3;
  public static final int MODE_AGGREGATE=4;

  public static final int TYPE_UNKNOWN=0;
  public static final int TYPE_TRANSPORTING=1;
  public static final int TYPE_POSITIONING=2;
  public static final int TYPE_RETURNING=3;
  public static final int TYPE_AGGREGATE=4;

  public static final Color brightYellow=new Color(1.0f,1.0f,0.7f);

  //Variables:
  ////////////

  private int mode;
  private int type;

  //  private String startLocID;
  //  private String endLocID;
  private RouteLoc startLoc;
  private RouteLoc endLoc;

  private Color forwardHeadColor;
  private Color backwardHeadColor;
  
  //Constructors:
  ///////////////
  public RouteSegment(int mode, int type,
		      RouteLoc startLoc,
		      RouteLoc endLoc){
    super(startLoc.getLat(), startLoc.getLon(), 
	  endLoc.getLat(), endLoc.getLon(), 
	  OMGraphic.LINETYPE_GREATCIRCLE);
    this.mode=mode;
    this.type=type;
    this.startLoc=startLoc;
    this.endLoc=endLoc;
    setStroke(thinStroke);
    setLinePaint(getColor());
  }

  //Members:
  //////////
  public Object getKey(){return this;}

  public int getMode(){return mode;}
  public int getType(){return type;}

  public float getSLat(){return getLL()[0];}
  public float getSLon(){return getLL()[1];}
  public float getELat(){return getLL()[2];}
  public float getELon(){return getLL()[3];}

  public String getStartLocID(){return startLoc.getDBUID();}
  public String getEndLocID(){return endLoc.getDBUID();}
  public RouteLoc getStartLoc(){return startLoc;}
  public RouteLoc getEndLoc(){return endLoc;}

  public Color getColor(){
    return getColor(mode,type);
  }

  protected Color getColor(int mode, int type){
    Color c = getModeColor(mode);
    return getTypeColor(type,c);
  }
  
  protected Color getTypeColor(int type, Color c){
    switch(type){
    case TYPE_POSITIONING:
    case TYPE_RETURNING:
      if(mode==MODE_GROUND)//Brighter doesn't work for ground, as it is already brightest...
	c=brightYellow;
      else{
	c=c.brighter();
      }
      break;
    case TYPE_TRANSPORTING:
      break;
    case TYPE_AGGREGATE:
      c=c.darker();
    }
    return c;
  }

  protected Color getModeColor(int mode){
    switch(mode){
    case MODE_GROUND:
      return TPFDDColor.TPFDDYellow;
    case MODE_SEA:
      return TPFDDColor.TPFDDGreen;
    case MODE_AIR:
      return TPFDDColor.TPFDDBlue;
    case MODE_AGGREGATE:
      return TPFDDColor.TPFDDPurple;
    default:
      return TPFDDColor.TPFDDGray;
    }
  } 

  protected String closeFont(){
    return "</font>";
  }

  protected String openFont(String color){
    return openFont(color, statusFontSize);
  }

  protected String openFont(String color, String fontSize){
    String size="";
    if(fontSize!=null&&fontSize!="")
      size=" size="+fontSize;
    return "<font color="+color+size+">";
  }
  
  protected String getModeFontColor(int mode){
    String retval;

    switch(mode){
    case RouteSegment.MODE_GROUND:
      retval = "yellow";
	  break;
    case RouteSegment.MODE_SEA:
      retval = "green";
	  break;
    case RouteSegment.MODE_AIR:
      retval = "blue";
	  break;
    case RouteSegment.MODE_AGGREGATE:
      retval = "purple";
	  break;
    default:
      retval = "gray";
    }
    return retval;
  } 

  public String getInfo () {
    return toString();
  }

  public String toString () 
  {
    return "RouteSegment - " + getLL();
  }

  /**
   * Prepare the line for rendering.
   * @param proj Projection
   * @return true if generate was successful */
  public boolean generate(Projection proj) {
    boolean ret=super.generate(proj);
    if(forwardHeadColor!=null){
      internalSetArrowHeadColor(ARROW_FORWARD,forwardHeadColor);
    }
    if(backwardHeadColor!=null){
      internalSetArrowHeadColor(ARROW_BACKWARD,backwardHeadColor);
    }
    return ret;
  }

  private void internalSetArrowHeadColor(int direction, Color c){
    if(arrowHead==null)
      return;
    if(arrowHead.length > direction)
      ((MyArrowHead)(arrowHead[direction])).setColor(c);
  }
  
  public void setArrowHeadColor(int direction, Color c){
    switch(direction){
    case ARROW_FORWARD:
      forwardHeadColor=c;
      break;
    case ARROW_BACKWARD:
      backwardHeadColor=c;
      break;
    }
  }

  //Cope with arrow heads being too small for wide lines in default impl:
  private final static int STRAIGHT_LINE = 0;
  private final static int CURVED_LINE = 1;

  /**
   * Create the ArrowHead objects for the lines, based on the
   * settings.
   * This function is called while OMLine is being generated. 
   * User's don't need to call this function
   */
  protected void createArrowHeads() {

    //NOTE: xpoints[0] refers to the original copy of the xpoints,
    //as opposed to the [1] copy, which gets used when the line
    //needs to wrap around the screen and show up on the other
    //side.  Might have to think about the [1] points, and adding
    //a arrowhead there if it shows up in the future.
    
    int pointIndex = xpoints[0].length - 1;
    if (Debug.debugging("arrowheads")) {
      Debug.output("createArrowHeads(): Number of points = " + pointIndex);
	}
    int drawingLinetype = STRAIGHT_LINE;	
    if (pointIndex > 1) drawingLinetype = CURVED_LINE;

	// Used as the index for points in the xy point array to use
	// as anchors for the arrowheads
    int[] end = new int[2];
    int[] start = new int[2];
    end[0] = pointIndex;
    start[0] = 0;
    end[1] = 0;
    start[1] = pointIndex;
    int numArrows = 1; // default

    // one for the start and end of each arrowhead (there could be two)
    Point sPoint1 = new Point();
    Point ePoint1 = new Point();
    Point sPoint2 = new Point(); 
    Point ePoint2 = new Point();

    int arrowDirection = arrowDirectionType;

    if (arc != null && arc.getReversed() == true) {
      if (arrowDirection == OMArrowHead.ARROWHEAD_DIRECTION_FORWARD) {
	arrowDirection = OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD;
      } else if (arrowDirection == OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD) {
	arrowDirection = OMArrowHead.ARROWHEAD_DIRECTION_FORWARD;
      }
    }

    switch(drawingLinetype) {
    case STRAIGHT_LINE:
      Debug.message("arrowheads","createArrowHeads(): Inside x-y space");
      int newEndX = xpoints[0][end[0]];
      int newEndY = ypoints[0][end[0]];
      int dx, dy;
      float dd;
      switch(arrowDirection) {
      case OMArrowHead.ARROWHEAD_DIRECTION_BOTH:
	// Doing the backward arrow here...

	Debug.message("arrowheads","createArrowHeads(): direction backward and");
	int newEndX2 = xpoints[0][end[1]];
	int newEndY2 = ypoints[0][end[1]];
	if (arrowLocation != 100) {
	  dx = xpoints[0][end[1]] - xpoints[0][start[1]];
	  dy = ypoints[0][end[1]] - ypoints[0][start[1]];
	  int offset = 0;
	  // Straight up or down
	  if (dx == 0) {
	    // doesn't matter, start and end the same
	    newEndX2 = xpoints[0][start[1]]; 
	    // calculate the percentage from start of line
	    offset = (int)((float)dy*(arrowLocation/100.0));
	    // set the end at the begining...
	    newEndY2 = ypoints[0][start[1]];
	    // and adjust...
	    if (dy < 0) {
            newEndY2 -= offset;
	    } else {
            newEndY2 += offset;
        }
	  } else {
	    dd = Math.abs((float)dy/(float)dx);
	    // If the line moves more x than y
	    if (Math.abs(dx) > Math.abs(dy)) {
	      // set the x
	      newEndX2 = xpoints[0][start[1]] + 
		(int)((float)dx*(arrowLocation/100.0));
	      // find the y for that x and set that
	      newEndY2 = ypoints[0][start[1]];
	      offset = (int)((float)Math.abs(xpoints[0][start[1]] - newEndX2)*dd);
	      if (dy < 0) {
              newEndY -= offset;
	      } else {
              newEndY += offset;
          }
	    } else  {
	      // switch everything...set y end
	      newEndY2 = ypoints[0][start[1]] + 
		(int)((float)dy*(arrowLocation/100.0));
	      // initialize the x to beginning
	      newEndX2 = xpoints[0][start[1]];
	      // calculate the difference x has to move based on y end
	      offset = (int)((float)Math.abs(ypoints[0][start[1]] - newEndY2)/dd);
	      // set the end
	      if (dx < 0) {
		newEndX2 -= offset;
	      } else {
		newEndX2 += offset;
	      }
	    }
	  }
	}

	if (start[1] < 0 ) {
	  start[1] = 0;
	}
	sPoint2.x = xpoints[0][start[1]];
	sPoint2.y = ypoints[0][start[1]];
	ePoint2.x = newEndX2;
	ePoint2.y = newEndY2;
	numArrows = 2;
      case OMArrowHead.ARROWHEAD_DIRECTION_FORWARD:
	Debug.message("arrowheads","createArrowHeads(): direction forward.");
	break;
      case OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD:
	Debug.message("arrowheads","createArrowHeads(): direction backward.");
	start[0] = pointIndex;
	end[0] = 0;
	break;
      }
      // And doing the forward arrow here...
      if (arrowLocation != 100) {
	dx = xpoints[0][end[0]] - xpoints[0][start[0]];
	dy = ypoints[0][end[0]] - ypoints[0][start[0]];
	int offset = 0;
	// Straight up or down
	if (dx == 0) {
	  // doesn't matter, start and end the same
	  newEndX = xpoints[0][start[0]]; 
	  // calculate the percentage from start of line
	  offset = (int)((float)dy*(arrowLocation/100.0));
	  // set the end at the begining...
	  newEndY = ypoints[0][start[0]];
	  // and adjust...
	  if (dy < 0) {
          newEndY -= offset;
	  } else {
          newEndY += offset;
      }
	} else {
	  dd = Math.abs((float)dy/(float)dx);
	  // If the line moves more x than y
	  if (Math.abs(dx) > Math.abs(dy)) {
	    // set the x
	    newEndX = xpoints[0][start[0]] + 
	      (int)((float)dx*(arrowLocation/100.0f));
	    // find the y for that x and set that
	    newEndY = ypoints[0][start[0]];
	    offset = (int)((float)Math.abs(xpoints[0][start[0]] - newEndX)*dd);
	    if (dy < 0) {
            newEndY -= offset;
	    } else {
            newEndY += offset;
        }
	  } else {
	    // switch everything...set y end
	    newEndY = ypoints[0][start[0]] + 
	      (int)((float)dy*(arrowLocation/100.0));
	    // initialize the x to beginning
	    newEndX = xpoints[0][start[0]];
	    // calculate the difference x has to move based on y end
	    offset = (int)((float)Math.abs(ypoints[0][start[0]] - newEndY)/dd);
	    // set the end
	    if (dx < 0) {
	      newEndX -= offset;
	    } else {
	      newEndX += offset;
	    }
	  }
	}
      }

      if (start[0] < 0) start[0] = 0;

      sPoint1.x = xpoints[0][start[0]];
      sPoint1.y = ypoints[0][start[0]];
      ePoint1.x = newEndX;
      ePoint1.y = newEndY;
      break;
    case CURVED_LINE:
      Debug.message("arrowheads","createArrowHeads(): Curved line arrowhead");
      switch(arrowDirection) {
      case OMArrowHead.ARROWHEAD_DIRECTION_BOTH:
	Debug.message("arrowheads","createArrowHeads(): direction backward and");
	start[1] = pointIndex - 
	  (int)((float)pointIndex*(float)(arrowLocation/100.0)) - 1;
	if (start[1] < 1) start[1] = 1;
	end[1] = start[1] - 1;

	sPoint2.x = xpoints[0][start[1]];
	sPoint2.y = ypoints[0][start[1]];
	ePoint2.x = xpoints[0][end[1]];
	ePoint2.y = ypoints[0][end[1]];

	numArrows = 2;
      case OMArrowHead.ARROWHEAD_DIRECTION_FORWARD:
	Debug.message("arrowheads", "createArrowHeads(): direction forward.");
	pointIndex = (int)((float)pointIndex*(float)(arrowLocation/100.0));
	end[0] = pointIndex;
	start[0] = pointIndex - 1;
	break;
      case OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD:
	Debug.message("arrowheads", "createArrowHeads(): direction backward.");
	start[0] =  pointIndex - 
	  (int)((float)pointIndex*(float)(arrowLocation/100.0)) - 1;
	if (start[0] < 1) start[0] = 1;
	end[0] = start[0] - 1;
	break;
      }
      if (start[0] < 0) start[0] = 0;
      if (end[0] < 0) end[0] = 0;

      sPoint1.x = xpoints[0][start[0]];
      sPoint1.y = ypoints[0][start[0]];
      ePoint1.x = xpoints[0][end[0]]; 
      ePoint1.y = ypoints[0][end[0]];
      break;

    }

    arrowHead = new OMArrowHead[numArrows];
    arrowHead[0] = new MyArrowHead(sPoint1, ePoint1,lineWidth);
    if (numArrows > 1) {
      arrowHead[1] = new MyArrowHead(sPoint2, ePoint2,lineWidth);
    }
  }

  //Inner Classes:
  public static class MyArrowHead extends OMArrowHead{
    private Color headColor=null;

    /**
     * Create an arrowhead.
     * @param from Point
     * @param to Point
     */
    public MyArrowHead(Point from, Point to, int lineWidth){
      super(from,to);
      int dx = to.x - from.x;
      int dy = to.y - from.y;
      int dd = Dist(dx, dy);
      if (dd < 6) dd = 6;
      int l = 3 * (lineWidth + 1) / 2;
      int l2 = 9 * (lineWidth + 1) / 2;
      xpts = new int[3];
      ypts = new int[3];
      xpts[0] = (int) (to.x + (dy * ( l) - dx * l2) / dd);
      ypts[0] = (int) (to.y + (dx * (-l) - dy * l2) / dd);
      xpts[1] = (int) (to.x);
      ypts[1] = (int) (to.y);
      xpts[2] = (int) (to.x + (dy * (-l) - dx * l2) / dd);
      ypts[2] = (int) (to.y + (dx * ( l) - dy * l2) / dd);
    }
    public void setColor(Color hc){
      headColor=hc;
    }
    /**
     * Draw the arrowheads.
     * @param gr Graphics
     */
    public void draw(Graphics gr){
      Color temp=gr.getColor();
      if(headColor!=null)
	gr.setColor(headColor);
      if (xpts != null)
	gr.fillPolygon(xpts, ypts, xpts.length);
      gr.setColor(temp);
    }
  }
}

