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
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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
    addArrowHead(true);
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
}

