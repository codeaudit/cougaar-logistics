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

/**
 * Holds a Chronicle of data about the location of some String indexed property
 *
 * @author Benjamin Lubin; last modified by $Author: mthome $
 *
 * @since 11/15/00
 */
public class TagChronicle extends Chronicle{

  //Variables:
  ////////////

  protected Set tags = new HashSet(11);

  //Constructors:
  ///////////////

  public TagChronicle(){
    super();
  }

  public TagChronicle(int binSize){
    super(binSize);
  }
  
  //Functions:
  ////////////

  /**
   * clean up a transit data list before it is added to the archive.
   * subclasses may wan to do something special to ensure data integrity.
   **/
  protected void cleanupTransitDataList(List lst){
    super.cleanupTransitDataList(lst);
    ensureConsistantCounts(lst);
    ensureConsistantTags(lst);
  }

  /** 
   * Walk the list to make sure that all of the counts are consistant.
   * Current implemenation just does a max operation.
   * @param lst List of TagTransitData objects that have been sorted
   * @return true iff changes have been made to lst
   **/
  protected boolean ensureConsistantCounts(List lst){
    if(lst.size()==0)
      return false;

    int max;
    boolean repair=false;
    //First pass determine max and if there is an inconsistancy
    Iterator i=lst.iterator();
    max=((TagTransitData)i.next()).getCount();
    while(i.hasNext()){
      TagTransitData ttd = (TagTransitData)i.next();
      if(ttd.getCount() != max){
	max=Math.max(max,ttd.getCount());
	repair=true;
      }
    }
    if(repair){
      logMessage(IMPORTANT,"Single asset is reported to have "+
			 "different counts in different tasks, using max.");
      //now walk the list changing stuff...
      i=lst.iterator();
      while(i.hasNext()){
	TagTransitData ttd=(TagTransitData)i.next();
	ttd.setCount(max);
      }
    }
    return repair;
  }

  /** 
   * Walk the list to make sure that all of the Tags are consistant.
   * Current implemenation just does a max operation.
   * @param lst List of TagTransitData objects that have been sorted
   * @return true iff changes have been made to lst
   **/
  protected boolean ensureConsistantTags(List lst){
    if(lst.size()==0)
      return false;

    String tag;
    boolean repair=false;
    //First pass determine max and if there is an inconsistancy
    Iterator i=lst.iterator();
    tag=((TagTransitData)i.next()).getTag();
    while(i.hasNext()){
      TagTransitData ttd = (TagTransitData)i.next();
      if(ttd.getTag() != tag){
	repair=true;
      }
    }
    if(repair){
      logMessage(IMPORTANT,"Single asset is reported to have "+
			 "different Tags in different tasks, using '"+
			 tag+"'");
      //now walk the list changing stuff...
      i=lst.iterator();
      while(i.hasNext()){
	TagTransitData ttd=(TagTransitData)i.next();
	ttd.setTag(tag);
      }
    }
    return repair;
  }

  public Set getTagSet(){
    return Collections.unmodifiableSet(tags);
  }

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected Tallier getNewTallier(){
    return new TagTally();
  }

  //Inner Classes:
  ////////////////

  /** Incoming data for each Tag transport task.**/
  public static abstract class TagTransitData extends TransitData{
    protected Position start;
    protected Position end;
    protected long startDate;
    protected long endDate;
    protected String tag;
    protected int count;

    public TagTransitData(Position start,
			   Position end,
			   long startDate,
			   long endDate,
			   String tag,
			   int count){
      this.start=start;
      this.end=end;
      this.startDate=startDate;
      this.endDate=endDate;
      if(tag!=null) {
	this.tag=tag.intern();
      } else{
	this.tag=null;
    }
      this.count=count;
    }

    public void setCount(int count){this.count=count;}
    public void setTag(String tag){this.tag=tag;}

    public Position getStartPosition(){return start;}
    public Position getEndPosition(){return end;}
    public long getStartDate(){return startDate;}
    public long getEndDate(){return endDate;}
    public String getTag(){return tag;}
    public int getCount(){return count;}
    public String toString(){
      return "[ " + (new Date(startDate)).toString() + " : " + start + "," +
	(new Date(endDate)).toString() + " : " + end + " | " + 
	getTag() + " | " + count + "]";
    }

    /**
     * Used to clone a copy of a TransitData, but set new values for its
     * positions and dates.  This function left abstract intentionally, to
     * force subclasses to redefine it.  This class is perfectly usable if
     * this function is defined with:
     * <code>
     *      return new TagTransitData(startP,endP,startD,endD,tag,count);
     * </code>
     * in the body (a simple subclass can be declared by doing so).
     **/
    public abstract TransitData cloneNewDatePos(Position startP,
						Position endP,
						long startD,
						long endD);
  }

  public interface TagTallier extends Tallier{

    /**return a map from tags (strings) to counts (Integers)**/
    public Map getTagToCountMap();

    /**get the count for a given tag**/
    public int getCount(String tag);
  }

  /**
   * Actual data for a time-loc bin.  In this case a hashtable of tags to ints
   **/
  public class TagTally implements TagTallier{

    protected Map tagToCount = new HashMap(11);
    
    public TagTally(){
    }

    public Map getTagToCountMap(){
      return tagToCount;
    }

    /** Return the count for the given Tag**/
    public int getCount(String tag){
      if(tags.contains(tag)){
	Integer i=(Integer)tagToCount.get(tag);
	if(i==null) {
	  return 0;
	} else {
	  return i.intValue();
    }
      }
      return 0;
    }

    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean increment(TransitData td){
      TagTransitData ttd = (TagTransitData)td;
      return updateTag(ttd.getTag().intern(),ttd.getCount());
    }

    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean decrement(TransitData td){
      TagTransitData ttd = (TagTransitData)td;
      return updateTag(ttd.getTag().intern(),-ttd.getCount());
    }

    protected boolean updateTag(String tag, int amount){
      //First add to overall set:
      tags.add(tag);
      //now update the specific one:
      Integer i = (Integer)tagToCount.get(tag);
      if(i==null){
	tagToCount.put(tag,new Integer(amount));
	if(amount < 0){
	  logMessage(MIDDLING,"Unexpected negative initial count in '"
			     + tag + "': " + amount);
	  return false;
	}
      }else{
	int n=i.intValue()+amount;
	tagToCount.put(tag,new Integer(n));
	if(n < 0){
	  logMessage(MIDDLING,"Unexpected negative update count in '"
			     + tag + "':" + amount);
	  return false;
	}
      }
      return true;
    }

    /** This should return a deep copy of this instance **/
    public Tallier deepClone(){
      TagTally ret = new TagTally();
      Iterator i=tagToCount.keySet().iterator();
      while(i.hasNext()){
	Object tag=i.next();
	Integer c=(Integer)tagToCount.get(tag);
	ret.tagToCount.put(tag, new Integer(c.intValue()));
      }
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
}
