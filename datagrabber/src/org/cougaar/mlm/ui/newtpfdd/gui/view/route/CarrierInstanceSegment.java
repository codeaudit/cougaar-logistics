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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMArrowHead;
import com.bbn.openmap.util.Debug;

import java.text.SimpleDateFormat;

/**
 * A single segment displayed in the CarrerInstance route
 * NOTE: In this file, the superclasses notion of start and end
 * are misleading, as a single CarrierInstanceSegment can represent multiple
 * movements, possibly in opposite directions...
 *
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/19/01
 **/
public class CarrierInstanceSegment extends RouteSegment {

  //Constants:
  ////////////

  //Variables:
  ////////////

  /**Contains the info for all contained legs**/
  protected List forwardLegInfoList;
  protected List backwardLegInfoList;

  private String carrierType;
  private String carrierName;

  //Constructors:
  ///////////////
  public CarrierInstanceSegment(int mode, int type, String legId, 
				RouteLoc startLoc, Date start,
				RouteLoc endLoc, Date end){
    super(mode,type,
	  startLoc,
	  endLoc);
    forwardLegInfoList=new ArrayList();
    backwardLegInfoList=new ArrayList();
    addLegInfo(legId, type, startLoc.getDBUID(), start, endLoc.getDBUID(), end);
  }
  
  //Members:
  //////////

  public String getCarrierType(){
    return carrierType;
  }
  public void setCarrierType(String ct){
    carrierType=ct;
  }
  public String getCarrierName(){
    return carrierName;
  }
  public void setCarrierName(String cn){
    carrierName=cn;
  }

  public Object getKey(){
    return new CarrierInstanceSegmentKey(getStartLocID(),getEndLocID());
  }

  public boolean multipleLegs(){
    return (forwardLegInfoList.size()+backwardLegInfoList.size())>1;
  }
  
  public int getType(){
    int f=getForwardType();
    int b=getBackwardType();
    if(f==TYPE_UNKNOWN){
      if(b==TYPE_UNKNOWN){
	return TYPE_UNKNOWN;
      }else{
	return b;
      }
    }else if(b==TYPE_UNKNOWN){
      return f;
    }else if(f==b){
      return f;
    }else{
      return TYPE_AGGREGATE;
    }
  }

  public int getForwardType(){
    if(forwardLegInfoList.size()<1)
      return TYPE_UNKNOWN;
    int ret=((LegInfo)(forwardLegInfoList.get(0))).getType();
    for(int i=1;i<forwardLegInfoList.size();i++){
      int t=((LegInfo)(forwardLegInfoList.get(i))).getType();
      if(t!=ret)
	return TYPE_AGGREGATE;
    }
    return ret;
  }
  
  public int getBackwardType(){
    if(backwardLegInfoList.size()<1)
      return TYPE_UNKNOWN;
    int ret=((LegInfo)(backwardLegInfoList.get(0))).getType();
    for(int i=1;i<backwardLegInfoList.size();i++){
      int t=((LegInfo)(backwardLegInfoList.get(i))).getType();
      if(t!=ret)
	return TYPE_AGGREGATE;
    }
    return ret;
  }
  
  public void addLegInfo(String legID, int type, 
			 String sLocID, Date start,
			 String eLocID, Date end){
    LegInfo li = new LegInfo(legID, type, sLocID, start, eLocID, end);
    if(getStartLocID().equals(sLocID)){
      forwardLegInfoList.add(li);
      myAddArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_FORWARD);
    }else if(getStartLocID().equals(eLocID)){
      backwardLegInfoList.add(li);
      myAddArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD);
    }else{
      System.err.println("CarrierInstanceSegment -- Mismatched Loc ID");
    }
    setLinePaint(getColor(getMode(),getType()));
  }

  protected void myAddArrowHead(int direction){
    if(arrowDirectionType==OMArrowHead.ARROWHEAD_DIRECTION_BOTH)
      return;
    if(arrowDirectionType==direction&&doArrowHead)
      return;
    if(direction==OMArrowHead.ARROWHEAD_DIRECTION_FORWARD){
      if(arrowDirectionType==OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD) {
	addArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_BOTH);
      } else {
	addArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_FORWARD);
    }
    }else if(direction==OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD){
      if(arrowDirectionType==OMArrowHead.ARROWHEAD_DIRECTION_FORWARD) {
	addArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_BOTH);
      } else {
	addArrowHead(OMArrowHead.ARROWHEAD_DIRECTION_BACKWARD);
    }
    }
  }

  public String getInfo () {
    StringBuffer ret=new StringBuffer();
    ret.append("<html>");
    String prefix="";
    int type=getType();
    boolean multipleLegs=multipleLegs();
    switch(type){
    case TYPE_POSITIONING:
      prefix="Position ";
      break;
    case TYPE_RETURNING:
      prefix="Return ";
      break;
    case TYPE_TRANSPORTING:
      prefix="Transport ";
      break;
    case TYPE_AGGREGATE:
      prefix="Aggregate ";
      break;
    }

    ret.append(openFont(descriptiveFontColor));
    if(multipleLegs){
      ret.append(" Multiple ");
      if(type!=TYPE_AGGREGATE)
	ret.append(prefix);
    }else{
      ret.append(prefix);
    }
    ret.append(closeFont());
    ret.append(openFont(lightFontColor));
    if(multipleLegs) {
      ret.append("legs for ");
    } else {
      ret.append("leg for ");
    }
    ret.append(closeFont());
    ret.append(openFont(getModeFontColor(getMode())) +
	       carrierType + " "+ carrierName + closeFont());
    if(multipleLegs){
      ret.append("");
    }else{
      LegInfo li;
      if(forwardLegInfoList.size()>0) {
	li=(LegInfo)forwardLegInfoList.get(0);
      }else {
	li=(LegInfo)backwardLegInfoList.get(0);
    }
      ret.append(openFont(lightFontColor)+" from " +closeFont()+ 
		 openFont(dateFontColor) + 
		 formatter.format(li.getStart()) + closeFont()+
		 openFont(lightFontColor)+" to " + closeFont()+ 
		 openFont(dateFontColor)+ 
		 formatter.format(li.getEnd()) + closeFont());
    }
    ret.append(openFont(lightFontColor));
    ret.append(": Right click for details");
    ret.append(closeFont());
    ret.append("</html>");
    return ret.toString();
  }

  public Map getLegDescriptionMap(){
    Map ret=new HashMap(37);
    for(int i=0;i<forwardLegInfoList.size();i++){
      LegInfo li=(LegInfo)forwardLegInfoList.get(i);
      ret.put(li.getLegID(),li.getDescription(getStartLoc().getPrettyName(),
					      getEndLoc().getPrettyName()));
    }
    for(int i=0;i<backwardLegInfoList.size();i++){
      LegInfo li=(LegInfo)backwardLegInfoList.get(i);
      ret.put(li.getLegID(),li.getDescription(getEndLoc().getPrettyName(),
					      getStartLoc().getPrettyName()));
    }
    return ret;
  }

  public List getLegOrder(){
    Comparator myComp=new Comparator(){
	public int compare(Object o1,
			   Object o2){
	  LegInfo li1=(LegInfo)o1;
	  LegInfo li2=(LegInfo)o2;
	  return li1.getStart().compareTo(li2.getStart());
	}
      };
    ArrayList tmp=new ArrayList(forwardLegInfoList.size()+
				backwardLegInfoList.size());
    tmp.addAll(forwardLegInfoList);
    tmp.addAll(backwardLegInfoList);
    Collections.sort(tmp,myComp);
    ArrayList ret=new ArrayList(tmp.size());
    for(int i=0;i<tmp.size();i++)
      ret.add(((LegInfo)tmp.get(i)).getLegID());
    return ret;
  }

  //Inner Classes:
  ////////////////
  public static class CarrierInstanceSegmentKey{
    private String sLocID;
    private String eLocID;
    public CarrierInstanceSegmentKey(String sLocID, String eLocID){
      this.sLocID=sLocID;
      this.eLocID=eLocID;
    }
    public boolean equals(Object o){
      if(super.equals(o))
	return true;
      if(o instanceof CarrierInstanceSegmentKey){
	CarrierInstanceSegmentKey other=(CarrierInstanceSegmentKey)o;
	return 
	  (sLocID.equals(other.sLocID)&&
	   eLocID.equals(other.eLocID)) ||
	  (sLocID.equals(other.eLocID)&&
	   eLocID.equals(other.sLocID));
      }
      return false;
    }
    public int hashCode(){
      return sLocID.hashCode() + eLocID.hashCode();
    }
  }

  public static class LegInfo{
    private String legID;
    private int type;
    private Date start;
    private Date end;
    LegInfo(String legID, int type, 
	    String sLocID, Date start, 
	    String eLocID, Date end){
      this.legID=legID;
      this.type=type;
      this.start=start;
      this.end=end;
    }
    public String getLegID(){
      return legID;
    }
    public int getType(){
      return type;
    }
    public Date getStart(){
      return start;
    }
    public Date getEnd(){
      return end;
    }

    public String getDescription(String f, String t){
      StringBuffer ret=new StringBuffer();
      ret.append("<html>");
      ret.append(openFont(descriptiveFontColor));
      switch(type){
      case RouteSegment.TYPE_TRANSPORTING:
	ret.append("Transporting ");
	break;
      case RouteSegment.TYPE_POSITIONING:
	ret.append("Positioning ");
	break;
      case RouteSegment.TYPE_RETURNING:
	ret.append("Returning ");
	break;
      default:
	ret.append("");
	break;
      }
      ret.append(closeFont());
      ret.append(openFont(lightFontColor));
      ret.append(" from ");
      ret.append(closeFont());
      ret.append(openFont(locFontColor));
      ret.append(f);
      ret.append(closeFont());
      ret.append(openFont(lightFontColor));
      ret.append(" to ");
      ret.append(closeFont());
      ret.append(openFont(locFontColor));
      ret.append(t);
      ret.append(closeFont());
      ret.append(openFont(lightFontColor));
      ret.append(". Leg starts ");
      ret.append(closeFont());
      ret.append(openFont(dateFontColor));
      ret.append(formatter.format(start));
      ret.append(closeFont());
      ret.append(openFont(lightFontColor));
      ret.append(" and ends ");
      ret.append(closeFont());
      ret.append(openFont(dateFontColor));
      ret.append(formatter.format(end));
      ret.append(closeFont());
      ret.append("</html>");
      return ret.toString();
    }

    private String openFont(String color){
      return "<font color="+color+">";
    }
    private String closeFont(){
      return "</font>";
    }
  }
}
