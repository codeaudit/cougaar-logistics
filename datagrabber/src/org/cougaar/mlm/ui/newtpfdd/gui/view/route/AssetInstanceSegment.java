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

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMArrowHead;
import com.bbn.openmap.util.Debug;

import java.text.SimpleDateFormat;

/**
 * A single segment displayed in the AssetInstance route
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/19/01
 **/
public class AssetInstanceSegment extends RouteSegment {

  //Constants:
  ////////////

  //Variables:
  ////////////

  private Date end;
  private Date start;

  private String cargoType;
  private String cargoName;
  private String carrierType;
  private String carrierName;
  
  //Constructors:
  ///////////////
  public AssetInstanceSegment(int mode, int type,
			      RouteLoc startLoc,
			      RouteLoc endLoc){
    super(mode,type,
	  startLoc,
	  endLoc);
  }
  
  //Members:
  //////////

  public void setStart (Date start){
    this.start = start;
  }
  
  public void setEnd (Date end){
    this.end = end;
  }

  public Date getStart(){
    return start;
  }

  public Date getEnd(){
    return end;
  }
  
  public void setCargoType (String cargo) {
    this.cargoType = cargo;
  }
  
  public void setCargoName (String cargo) {
    this.cargoName = cargo;
  }
  
  public void setCarrierType (String carrier) {
    this.carrierType = carrier;
  }
  
  public void setCarrierName (String carrier) {
    this.carrierName = carrier;
  }
  
  public String getInfo () {
    return "<html>"+
      openFont(assetFontColor) + cargoType + " " + cargoName + closeFont()+
      openFont(lightFontColor)+" moved by "+ closeFont() + 
      openFont(getModeFontColor(getMode())) + 
      carrierType + " "+ carrierName + closeFont() +
      openFont(lightFontColor)+ " from " + closeFont() + 
      openFont(dateFontColor) + formatter.format(getStart()) + closeFont()+
      openFont(lightFontColor)+ " to " + closeFont() +
      openFont(dateFontColor) + formatter.format(getEnd()) + closeFont()+
      "</html>";
  }
}
