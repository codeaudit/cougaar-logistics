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
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import java.util.*;
import java.io.Serializable;

/**
 * Data structure to hold Asset information over a period of time.
 * A Chronicle is an array of Frames, where each Frame contains a view
 * of the world for a particular bin of time.  The Frames each hold a
 * hashmap from Positions to instances of Tallier objects that record the
 * binned data.  
 *
 * Assumption -- transit tasks do not overlap.
 *
 * @author Benjamin Lubin; last modified by $Author: mthome $
 *
 * @since 11/14/00
 */
public abstract class Chronicle implements Serializable{

  //Variables:
  ////////////

  public static int CATASTROPHIC=0;
  public static int CRITICAL=1;
  public static int IMPORTANT=2;
  public static int MIDDLING=3;
  public static int MINOR=4;
  public static int TRIVIAL=5;
  public int messageLevel=MIDDLING;

  public static int UNDEF=-1;

  /** The bin size in milliseconds **/
  protected int binSize = 1000 * 60 * 60 * 24;

  /** The date of the start of the first bin, as
   * "milliseconds since January 1, 1970, 00:00:00 GMT"
   **/
  protected long startDate = UNDEF;

  protected ArrayList frames = new ArrayList();

  //Constructors:
  ///////////////

  public Chronicle(){}
  
  /**
   * @param binSize The size of each bin in milliseconds
   **/
  public Chronicle(int binSize){
    this.binSize=binSize;
  }

  //Functions:
  ////////////

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected abstract Tallier getNewTallier();

  /**
   * This function provided so you can provide your own Frame subclass.
   **/
  protected Frame getNewFrame(){
    return new Frame();
  }
  
  /**
   * clean up a transit data list before it is added to the archive.
   * subclasses may wan to do something special to ensure data integrity.
   **/
  protected void cleanupTransitDataList(List lst){
      removeOverlappingTime(lst);
      fillInGaps(lst);
  }

  /**adds all the data for a given asset.
   * @param lst a list of ALL TransitData objects for SINGLE asset.
   * <B>Note: lst will be modified by this function!</B>
   */
  public void reportTransitData(List lst){
    if(lst.size()==0)
      return;
    try{
      Collections.sort(lst);
      cleanupTransitDataList(lst);

      reportTransitDataExistence((TransitData)lst.get(0));
      
      for(int i=1;i<lst.size();i++){
	  reportTransitDataMovement((TransitData)lst.get(i));
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Removes overlapping/redundant TransitData blocks from the sorted list
   **/
  protected void removeOverlappingTime(List lst){

    //Naive algorothm -- simply walk the list and delete any blocks that
    //start before the previous one ended... should end up with longest
    //time blocks.

    if(lst.size()==0)
      return;

    long lastEndDate = ((TransitData)lst.get(0)).getEndDate();

    Iterator i=lst.iterator();
    i.next();
    while(i.hasNext()){
      TransitData td=(TransitData)i.next();
      if(td.getStartDate() < lastEndDate) {
	i.remove();
      } else {
	lastEndDate=td.getEndDate();
    }
    }
  }

  /**
   * Clones TransitData elements for missing legs
   **/
  protected void fillInGaps(List lst){
    if(lst.size()==0)
      return;

    ListIterator i=lst.listIterator();
    TransitData lastData=(TransitData)i.next();

    while(i.hasNext()){
      TransitData curData=(TransitData)i.next();
      if(! curData.getStartPosition().equals(lastData.getEndPosition())){
	//end != start, so add a new one
	if(curData.getStartDate() <= lastData.getEndDate()){
	  logMessage(CRITICAL, 
		     "Not enough time to insert missing leg between ["+
		     lastData.getEndPosition()+" -> " +
		     curData.getStartPosition() + "]");
	}else{
	  TransitData insert=
	    lastData.cloneNewDatePos(lastData.getEndPosition(),
				     curData.getStartPosition(),
				     lastData.getEndDate(),
				     curData.getStartDate());
	  logMessage(IMPORTANT,
		     "Inserting missing leg between ["+
		     lastData.getEndPosition()+" -> " +
		     curData.getStartPosition() + "]");
	  //Insert it before the current guy:
	  i.previous();
	  i.add(insert);
	  i.next();
	}
      }
      lastData=curData;
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
    long s=td.getStartDate();
    long e=td.getEndDate();
    
    ensureInterval(s,e);

    /*Debug:
    if(getIndexBelow(s)+1 != getIndexAboveEq(s))
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ERR S: "+getIndexBelow(s)+","+getIndexAboveEq(s));
    if(getIndexBelowEq(e)+1 != getIndexAbove(e))
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ERR E: "+getIndexBelowEq(e)+","+getIndexAbove(e));
    */

    int startIndex = getIndexAboveEq(s);
    int endIndex = getIndexBelowEq(e);

    for(int i=0;i<startIndex;i++){
      Frame f=(Frame)frames.get(i);
      f.addStartData(td);
    }
    for(int i=startIndex;i<=endIndex;i++){
      Frame f=(Frame)frames.get(i);
      f.addTransitData(td);
    }
    for(int i=endIndex+1;i<frames.size();i++){
      Frame f=(Frame)frames.get(i);
      f.addEndData(td);
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
  protected void reportTransitDataMovement(TransitData td){
    long s=td.getStartDate();
    long e=td.getEndDate();
    
    ensureInterval(s,e);

    int startIndex = getIndexAboveEq(s);
    int endIndex = getIndexBelowEq(e);

    for(int i=startIndex;i<=endIndex;i++){
      Frame f=(Frame)frames.get(i);
      if(!f.updateTransitData(td)){
	logMessage(MIDDLING, "Error updatingTransitData["+i+"]");
      }
    }
    for(int i=endIndex+1;i<frames.size();i++){
      Frame f=(Frame)frames.get(i);
      if(!f.updateStartEndData(td)){
	logMessage(MIDDLING, "Error updatingStartEndData["+i+"]");
      }
    }
  }
  
  /** 
   * Ensure that there are bins for the time spanning s and e.<br>
   * This means that there will be at least one bin before s, and at least
   * one bin after e
   **/
  public void ensureInterval(long s, long e){
    ensureIntervalExclusive(s-binSize, e+binSize);
  }

  /** Ensure that there are bins for the time spanning s and e.<br>
   * This means that bins are added for the time between s and e --
   * but that there will be portions of the time block extending past
   * the last bins on either side (assuming they aren't already present
   * before the call).
   * <B>Note: startDate must have already been set and there must
   * be at least one frame</B>**/ 
  public void ensureIntervalExclusive(long s, long e){
    //First case: this is the first guy, and we haven't set a start date
    //so set the startDate to midnight before the startTime.
    if(startDate == UNDEF){ //frames.size()==0 would also be ok for condition.
      Calendar c = new 
	GregorianCalendar(TimeZone.getTimeZone("GMT"),Locale.US);
      c.setTime(new Date(s));
      c.set(Calendar.HOUR_OF_DAY,0);
      c.set(Calendar.MINUTE,0);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);

      startDate=c.getTime().getTime();
      frames.add(getNewFrame());
    }

    Frame f;
    int growbackward=getIndexAboveEq(s); //negative value
    int growforward=getIndexBelowEq(e)-(frames.size()-1) +1;

    //Grow back in time:
    f=(Frame)frames.get(0);
    for(int i=growbackward;i<0;i++){
      frames.add(0,f.deepClone());
      startDate-=binSize;
    }

    //Grow forward in time:
    f=(Frame)frames.get(frames.size()-1);
    for(int i=0;i<growforward;i++){
      frames.add(f.deepClone());
    }
  }

  /** Get the index number above (or on) the given date -- 
   * note that this does
   * not ensure that this is a valid index.  Dates before the first
   * bin will return negative indices.<br>
   * <B>Note: startDate must have already been set</B>
   **/
  public int getIndexAboveEq(long date){
    return (int)Math.ceil((float)(date-startDate)/(float)binSize);
  }

  /** Get the index number above the given date -- 
   * note that this does
   * not ensure that this is a valid index.  Dates before the first
   * bin will return negative indices.<br>
   * <B>Note: startDate must have already been set</B>
   **/
  public int getIndexAbove(long date){
    float index=(float)(date-startDate)/(float)binSize;
    int c = (int)Math.ceil(index);
    int f = (int)Math.floor(index);
    return (c==f)?c+1:c;
  }

  /** Get the index number below (or on)the given date -- 
   * note that this does
   * not ensure that this is a valid index.  Dates before the first
   * bin will return negative indices.<br>
   * <B>Note: startDate must have already been set</B>
   **/
  public int getIndexBelowEq(long date){
    return (int)Math.floor((float)(date-startDate)/(float)binSize);
  }

  /** Get the index number below the given date -- 
   * note that this does
   * not ensure that this is a valid index.  Dates before the first
   * bin will return negative indices.<br>
   * <B>Note: startDate must have already been set</B>
   **/
  public int getIndexBelow(long date){
    float index = (float)(date-startDate)/(float)binSize;
    int c = (int)Math.ceil(index);
    int f = (int)Math.floor(index);
    return (c==f)?f-1:f;
  }

  public int getNumberOfFrames(){
    return frames.size();
  }

  public boolean isEmpty(){
    return frames.size()==0;
  }

  public long getFrameDate(int index){
    if(startDate==UNDEF)
      return UNDEF;
    return startDate + (long)index*(long)binSize;
  }

  public Frame getFrame(int index){
    return (Frame)frames.get(index);
  }

  public String toString(){
    StringBuffer sb=new StringBuffer(4096);
    sb.append("["+getClassName()+":\n");
    for(int i=0;i<frames.size();i++){
      sb.append("Frame ["+i+"]: ");
      Frame f=(Frame)frames.get(i);
      sb.append(f + "\n");
    }
    sb.append("]");
    return sb.toString();
  }

  //Inner Classes:
  ////////////////

/** 
 * Represents one bin of time in a Chronicle
 **/
  public class Frame implements Serializable{
    public Tallier total;
    protected Map posToTallier = new HashMap(11);
    
    public Frame(){
      total = getNewTallier();
    }
    
    protected Frame(Tallier total, Map posToTallier){
      this.total = total;
      this.posToTallier = posToTallier;
    }
    
    /**
     * get a mapping of positions to Talliers
     **/
    public Map getPosToTallierMap(){
      return posToTallier;
    }

    /** increment the Tallier at the starting position; increment total **/
    public void addStartData(TransitData td){
      Tallier t = (Tallier)posToTallier.get(td.getStartPosition());
      if(t==null){
	t=getNewTallier();
	posToTallier.put(td.getStartPosition(), t);
      }
      t.increment(td);
      total.increment(td);
    }
    
    /** increment the Tallier at the TransitPosition; increment total **/
    public void addTransitData(TransitData td){
      TransitPosition tp = new TransitPosition(td.getStartPosition(),
					       td.getEndPosition());
      Tallier t = (Tallier)posToTallier.get(tp);
      if(t==null){
	t=getNewTallier();
	posToTallier.put(tp, t);
      }
      t.increment(td);
      total.increment(td);
    }
    
    /** increment the Tallier at the ending position; increment total **/
    public void addEndData(TransitData td){
      Tallier t = (Tallier)posToTallier.get(td.getEndPosition());
      if(t==null){
	t=getNewTallier();
	posToTallier.put(td.getEndPosition(), t);
      }
      t.increment(td);
      total.increment(td);
    }
    
    /** increment the Tallier at the TransitPosition and decrement it
     * at the start position
     **/
    public boolean updateTransitData(TransitData td){
      //increment TP
      TransitPosition tp = new TransitPosition(td.getStartPosition(),
					       td.getEndPosition());
      Tallier t = (Tallier)posToTallier.get(tp);
      if(t==null){
	t=getNewTallier();
	posToTallier.put(tp, t);
      }
      t.increment(td);
      //decrement start:
      t = (Tallier)posToTallier.get(td.getStartPosition());
      if(t==null){
	logMessage(MIDDLING,"Unexpected adding Tallier to '"+
			   tp+"' in order to decrement");
	t=getNewTallier();
	posToTallier.put(td.getStartPosition(), t);
      }
      if(!t.decrement(td)){
	logMessage(MIDDLING,"Problem decrementing for transit: "+td);
	return false;
      }
      return true;
    }
    
    /** increment the Tallier at the end and decrement it
     * at the start position
     **/
    public boolean updateStartEndData(TransitData td){
      //increment end
      Tallier t = (Tallier)posToTallier.get(td.getEndPosition());
      if(t==null){
	t=getNewTallier();
	posToTallier.put(td.getEndPosition(), t);
      }
      t.increment(td);
      //decrement start:
      t = (Tallier)posToTallier.get(td.getStartPosition());
      if(t==null){
	logMessage(MIDDLING,"Unexpected adding Tallier to '"+
			   t+"' in order to decrement");
	t=getNewTallier();
	posToTallier.put(td.getStartPosition(), t);
      }
      if(!t.decrement(td)){
	logMessage(MIDDLING,": problem decrementing for start/end: "+td);
	return false;
      }
      return true;
    }
    
    /** Deep clone ALL the talliers, but don't bother cloning positions **/
    public Frame deepClone(){
      Tallier t;
      Map m = new HashMap();
      Iterator i=posToTallier.keySet().iterator();
      while(i.hasNext()){
	Position p=(Position)i.next();
	t=(Tallier)posToTallier.get(p);
	m.put(p,t.deepClone());
      }
      t=total.deepClone();
      return new Frame(t,m);
    }

    public String toString(){
      Iterator i=posToTallier.keySet().iterator();
      StringBuffer sb = new StringBuffer();
      sb.append("(");
      while(i.hasNext()){
	Position p = (Position)i.next();
	Tallier t = (Tallier)posToTallier.get(p);
	sb.append(p);
	sb.append("-->");
	sb.append(t);
	sb.append(",\n");
      }
      sb.append(")");
      return sb.toString();
    }
  }

  /** 
   * utility function to get just the name of the class of an object 
   * (no package) 
   **/
  protected static String getClassName(Object obj) {
    String classname = obj.getClass().getName ();
    int index = classname.lastIndexOf (".");
    classname = classname.substring (index+1, classname.length ());
    return classname;
  }

  protected String getClassName(){
    return getClassName(this);
  }

  protected void logMessage(int level, String message){
    if(level <= messageLevel)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, getClassName()+ ": " + message);
  }

  protected static void logStaticMessage(int level, String message){
    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, message);
  }
}
