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
package org.cougaar.mlm.ui.newtpfdd.transit;
import java.util.*;
import java.io.Serializable;

import org.cougaar.mlm.ui.newtpfdd.transit.TagChronicle.TagTransitData;
import org.cougaar.mlm.ui.newtpfdd.transit.TagChronicle.TagTally;

/**
 * Holds a Chronicle of data about carrier utilization
 *
 * @author Benjamin Lubin; last modified by $Author: gvidaver $
 *
 * @since 11/15/00
 */
public class CarrierUtilizationChronicle extends TagChronicle{

  //Variables:
  ////////////

  protected Map carIdToPos = null;
  protected Map carIdToType = null;

  private int lastEndIndex;

  public static String SELF_PROP = "Self-Propelled";

  //Constructors:
  ///////////////

  public CarrierUtilizationChronicle(int binSize, Map carIdToPos, 
				     Map carIdToType){
    super(binSize);
    this.carIdToPos=carIdToPos;
    this.carIdToType=carIdToType;
  }
  
  //Functions:
  ////////////

  /**
   * clean up a transit data list before it is added to the archive.
   * subclasses may wan to do something special to ensure data integrity.
   **/
  protected void cleanupTransitDataList(List lst){
    removeOverlappingTime(lst);
    //fillInGaps(lst);
    modifyForCarrierUtil(lst);
  }

  /**
   * Set the locations and the asset types:
   **/
  protected boolean modifyForCarrierUtil(List lst){
    if(lst.size()==0)
      return false;

    for(int i=0;i<lst.size();i++){
      CarrierUtilizationTransitData cutd; 
      cutd = (CarrierUtilizationTransitData)lst.get(i);
      String type = null;
      Position p = null;
      if(carIdToType != null)
	type=(String)carIdToType.get(cutd.getCarrierUID());
      if(carIdToPos != null)
	p = (Position)carIdToPos.get(cutd.getCarrierUID());

      if(type == null && p == null){
	logMessage(MINOR,"Recieved self-propelled carrier: "+
		   cutd.getCarrierUID()); 
	
	p = cutd.getStartPosition();
	type=SELF_PROP;
      }else if(type == null){
	logMessage(IMPORTANT,"Recieved carrier '"+
			   cutd.getCarrierUID() + "' with unknown type.");
	type="Unknown";
      }else if(p==null){
	logMessage(IMPORTANT,"Recieved carrier '"+
			   cutd.getCarrierUID() + "' with unknown home base.");
	p = new FixedPosition("Unknown",0,0);
      }

      cutd.setTag(type);
      cutd.setStartPosition(p);
      cutd.setEndPosition(p);
    }
    return true;
  }

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected Tallier getNewTallier(){
    return new CarrierUtilizationTally();
  }

  /**
   * This function adds new data to appropriate Frames and updates the data
   * structure.
   * <BR>
   * Note: Assumptions:
   * <UL>
   * <LI>Subsequent calls will be sequentially forward in time
   * <LI>TransitData for a single Asset do not overlap in time.
   * </UL>
   **/
  protected void reportTransitDataExistence(TransitData td){
    lastEndIndex=-1;
    reportTransitDataMovement(td);
  }
  
  /**
   * This function adds new data to appropriate Frames and updates the data
   * structure.
   * <BR>
   * Note: Assumptions:
   * <UL>
   * <LI>Subsequent calls will be sequentially forward in time
   * <LI>TransitData for a single Asset do not overlap in time.
   * </UL>
   **/
  protected void reportTransitDataMovement(TransitData td){
    long s=td.getStartDate();
    long e=td.getEndDate();
    
    ensureInterval(s,e);

    int startIndex = getIndexBelowEq(s); //changed from AboveEq, 
                                         //so we integrate over the bin
    int endIndex = getIndexBelowEq(e);

    if(startIndex<=lastEndIndex){
      startIndex=lastEndIndex+1;
    }

    for(int i=startIndex;i<=endIndex;i++){
      Frame f=(Frame)frames.get(i);
      f.addStartData(td);
    }

    if(startIndex<=endIndex)
      lastEndIndex=endIndex;
  }

  //Inner Classes:
  ////////////////

  /** Incoming data for each unit transport task.**/
  public static class CarrierUtilizationTransitData extends TagTransitData{
    protected String carrierUID;
    public CarrierUtilizationTransitData(Position start,
					 Position end,
					 long startDate,
					 long endDate,
					 String carrierUID){
      super(start,end,startDate,endDate,"Unknown",1);
      this.carrierUID=carrierUID.intern();
    }
    public String getCarrierUID(){
      return carrierUID;
    }

    public void setStartPosition(Position p){
      start=p;
    }

    public void setEndPosition(Position p){
      end=p;
    }

    /**
     * Used to clone a copy of a TransitData, but set new values for its
     * positions and dates
     **/
    public TransitData cloneNewDatePos(Position startP,
				       Position endP,
				       long startD,
				       long endD){
      CarrierUtilizationTransitData ret = new 
	CarrierUtilizationTransitData(startP,endP,startD,endD,carrierUID);
      ret.setTag(tag);
      return ret;
    }
  }
  
  /**
   * Actual data for a time-loc bin.  In this case an array of integers, one
   * per unit.
   **/
  public class CarrierUtilizationTally extends TagTally{
 
    /** a list of all the carrier ids seen at this place and time**/   
    protected Set seenCarrierUIDs = new HashSet(11);
    
    public CarrierUtilizationTally(){
    }
    
    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean increment(TransitData td){
      CarrierUtilizationTransitData cutd = (CarrierUtilizationTransitData)td;
      //first check to see if the carrierUID has been used...
      String carID=cutd.getCarrierUID();
      if(seenCarrierUIDs.contains(carID))
	return true;
      //we haven't seen it:
      seenCarrierUIDs.add(carID.intern());
      return updateTag(cutd.getTag().intern(),cutd.getCount());
    }
    
    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean decrement(TransitData td){
      logMessage(CRITICAL,"Decrement should never be called "+
			 "in this subclass!");
      return false;
    }
    
    /** This should return a deep copy of this instance **/
    public Tallier deepClone(){
      CarrierUtilizationTally ret = new CarrierUtilizationTally();
      Iterator i=tagToCount.keySet().iterator();
      while(i.hasNext()){
	Object tag=i.next();
	Integer c=(Integer)tagToCount.get(tag);
	ret.tagToCount.put(tag, new Integer(c.intValue()));
      }
      i=seenCarrierUIDs.iterator();
      while(i.hasNext()){
	ret.seenCarrierUIDs.add(i.next());
      }
      return ret;
    }
  }
}
