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
package org.cougaar.mlm.ui.newtpfdd.transit;
import java.util.*;
import java.io.Serializable;

import org.cougaar.mlm.ui.newtpfdd.transit.UnitChronicle.UnitTransitData;
import org.cougaar.mlm.ui.newtpfdd.transit.UnitChronicle.UnitTally;

/**
 * Holds a Chronicle of data about the location of a unit's arrival deviation
 *
 *
 * @since 12/02/00
 */
public class ArrivalDeviationChronicle extends UnitChronicle{

  //Variables:
  ////////////

  protected Map tagToTotal = new HashMap(11);
  
  //Constructors:
  ///////////////

  public ArrivalDeviationChronicle(){
    super();
  }

  public ArrivalDeviationChronicle(int binSize){
    super(binSize);
  }
  
  //Functions:
  ////////////

  /**
   * clean up a transit data list before it is added to the archive.
   * subclasses may wan to do something special to ensure data integrity.
   **/
  protected void cleanupTransitDataList(List lst){
    removeOverlappingTime(lst);
    ensureConsistantCounts(lst);
    ensureConsistantTags(lst);
  }

  public Map getTagToTotalMap(){
    return Collections.unmodifiableMap(tagToTotal);
  }

  protected void updateTotal(String tag, int value){
    Integer i= (Integer)tagToTotal.get(tag);
    if(i==null){
      i=new Integer(value);
      tagToTotal.put(tag,i);
    }else{
      int curVal=i.intValue();
      if(value > curVal)
	tagToTotal.put(tag, new Integer(value));
    }
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

    int index = getIndexBelowEq(e);//where it actually showed up...
    Frame f=(Frame)frames.get(index);

    f.addEndData(td);
  }

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected Tallier getNewTallier(){
    return new ArrivalDeviationTally();
  }

  //Inner Classes:
  ////////////////

  /** Incoming data for each unit transport task.**/
  public static class ArrivalDeviationTransitData extends UnitTransitData{
    protected long requestedEndDate;
    
    public ArrivalDeviationTransitData(Position start,
				       Position end,
				       long startDate,
				       long endDate,
				       long requestedEndDate,
				       String unit,
				       int count){
      super(start,end,startDate,endDate,unit,count);
      this.requestedEndDate = requestedEndDate;
    }

    public long getRequestedEndDate(){return requestedEndDate;}

    /**
     * Used to clone a copy of a TransitData, but set new values for its
     * positions and dates
     **/
    public TransitData cloneNewDatePos(Position startP,
				       Position endP,
				       long startD,
				       long endD){
      return new ArrivalDeviationTransitData(startP,endP,startD,endD,
					     requestedEndDate,
					     tag,count);
    }
  }

  /**
   * Actual data for a time-loc bin.  In this case a hashtable of tags to 
   * TallyInfo objects
   **/
  public class ArrivalDeviationTally implements TagTallier{

    protected Map tagToCount = new HashMap(11);
    
    public ArrivalDeviationTally(){
    }

    public Map getTagToCountMap(){
      Map ret=new HashMap(11);
      Iterator iter=tagToCount.keySet().iterator();
      while(iter.hasNext()){
	Object tag=iter.next();
	TallyInfo ti = (TallyInfo)tagToCount.get(tag);
	ret.put(tag,new Integer(ti.calculateValue()));
      }
      return ret;
    }

    /** Return the count for the given Tag**/
    public int getCount(String tag){
      if(tags.contains(tag)){
	TallyInfo ti = (TallyInfo)tagToCount.get(tag);
	if(ti==null)
	  return 0;
	return ti.calculateValue();
      }
      return 0;
    }

    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean increment(TransitData td){
      ArrivalDeviationTransitData aftd = (ArrivalDeviationTransitData)td;
      updateTotal(aftd.getTag(),internal_increment(td).calculateValue());
      return true;
    }

    /** 
     * Should not be called in this subclass.
     **/
    public boolean decrement(TransitData td){
      logMessage(CRITICAL,"Unexepcted call to decrement");
      return false;
    }

    protected TallyInfo internal_increment(TransitData td){
      ArrivalDeviationTransitData adtd = (ArrivalDeviationTransitData)td;
      return updateTag(adtd.getTag().intern(),
		       Math.max(0,adtd.getEndDate()-
				adtd.getRequestedEndDate()),
		       adtd.getCount());
    }

    protected TallyInfo updateTag(String tag, float delay, int count){
      //First add to overall set:
      tags.add(tag);
      //now update the specific one:
      TallyInfo ti= (TallyInfo)tagToCount.get(tag);
      if(ti==null){
	ti=new TallyInfo();
	tagToCount.put(tag,ti);
      }
      ti.timeDelay+=delay;
      ti.numItems+=count;
      return ti;
    }

    /** This should return a deep copy of this instance **/
    public Tallier deepClone(){
      ArrivalDeviationTally ret = new ArrivalDeviationTally();

      /* We don't want to copy this information -- it will be calculated
	 inependently for each bin.
      Iterator i=tagToCount.keySet().iterator();
      while(i.hasNext()){
	Object tag=i.next();
	TallyInfo ti=(TallyInfo)tagToCount.get(tag);
	ret.tagToCount.put(tag, new TallyInfo(ti.timeDelay, ti.numItems));
      }
      */

      return ret;
    }

    public String toString(){
      Iterator iter=tagToCount.keySet().iterator();
      StringBuffer sb = new StringBuffer();
      sb.append("(");
      while(iter.hasNext()){
	String t = (String)iter.next();
	Integer i = (Integer)tagToCount.get(t);
	sb.append(t);
	sb.append(" = ");
	sb.append(i);
	sb.append(",");
      }
      sb.append(")");
      return sb.toString();
    }
  }

  protected static class TallyInfo{
    public long timeDelay;
    public int numItems;

    public TallyInfo(){
      timeDelay=0;
      numItems=0;
    }
    public TallyInfo(long timeDelay,int numItems){
      this.timeDelay=timeDelay;
      this.numItems=numItems;
    }

    public int calculateValue(){
      return (numItems==0)?0:
	(int)((float)timeDelay / ((float)numItems * 1000 * 60 * 60));
    }

  }
}
