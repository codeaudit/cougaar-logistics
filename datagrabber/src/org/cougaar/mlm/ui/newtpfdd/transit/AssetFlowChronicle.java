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

import org.cougaar.mlm.ui.newtpfdd.transit.TagChronicle.TagTransitData;
import org.cougaar.mlm.ui.newtpfdd.transit.TagChronicle.TagTally;
import org.cougaar.mlm.ui.newtpfdd.transit.TagChronicle.TagTallier;
import org.cougaar.mlm.ui.newtpfdd.transit.Chronicle.Frame;

/**
 * Holds a Chronicle of data about the flow of a Assets
 *
 *
 * @since 12/01/00
 */
public class AssetFlowChronicle extends AssetClassChronicle{

  //Variables:
  ////////////

  protected boolean byAssetClass = true; //Otherwise there is just one 
                                         //column in the chart

  protected boolean useWeight = false; //should we multiply count by weight?

  protected Map posToTotalTally = new HashMap();

  public static String singleTagName = "Flow";

  //Constructors:
  ///////////////

  public AssetFlowChronicle(int binSize, boolean byAssetClass, 
			    boolean useWeight){
    super(binSize);
    this.byAssetClass=byAssetClass;
    this.useWeight=useWeight;
  }
  
  //Functions:
  ////////////

  /** Return a mapping of positions to their totals **/
  public TagTallier getTotalTallyForPos(Position pos){
    return (TagTallier)posToTotalTally.get(pos);
  }

  /**
   * This function provided so you can provide your own Frame subclass.
   **/
  protected Frame getNewFrame(){
    return new AssetFlowFrame();
  }

  /**
   * clean up a transit data list before it is added to the archive.
   * subclasses may wan to do something special to ensure data integrity.
   **/
  protected void cleanupTransitDataList(List lst){
    removeOverlappingTime(lst);

    ensureConsistantCounts(lst);
    ensureConsistantTags(lst);

    if(useWeight)
      ensureConsistantWeights(lst);
  }

  /** 
   * Walk the list to make sure that all of the weights are consistant.
   * Current implemenation just does a max operation.
   * @param lst List of TagTransitData objects that have been sorted
   * @return true iff changes have been made to lst
   **/
  protected boolean ensureConsistantWeights(List lst){
    float epsilon = 10*Float.MIN_VALUE;
    
    if(lst.size()==0)
      return false;

    float max;
    boolean repair=false;
    //First pass determine max and if there is an inconsistancy
    Iterator i=lst.iterator();
    AssetFlowTransitData aftd = (AssetFlowTransitData)i.next();
    max=aftd.getWeight();
    if(max < epsilon){
      logMessage(IMPORTANT,"Asset is reported to have zero "+
			 "weight, using 1.0");
      max=1f;
      aftd.setWeight(max);
    }
    while(i.hasNext()){
      aftd = (AssetFlowTransitData)i.next();
      //floating point == check
      if(Math.abs(aftd.getWeight() - max) > epsilon){
	max=Math.max(max,aftd.getWeight());
	repair=true;
      }
    }
    if(repair){
      logMessage(IMPORTANT,"Single asset is reported to have "+
			 "different counts in different tasks, using max.");
      //now walk the list changing stuff...
      i=lst.iterator();
      while(i.hasNext()){
	aftd=(AssetFlowTransitData)i.next();
	aftd.setWeight(max);
      }
    }
    return repair;
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

    int startIndex = getIndexBelowEq(s); //just increment the bin below
                                         //the starting point.
    Frame f=(Frame)frames.get(startIndex);

    //NOTE If you use the other "add" functions, make sure to subclass them
    //in AssetFlowFrame, defined below
    f.addStartData(td);
  }

  public static int getAssetClassNumber(String assetClass){
    logStaticMessage(CRITICAL,
	       "Error: must use getAssetClassNumber(boolean, String)");
    return 0;
  }

  public static int getAssetClassNumber(boolean byAssetClass, 
					String assetClass){
    if(byAssetClass){
      for(int i=0;i<assetClassNames.length;i++)
	if(assetClass.equals(assetClassNames[i]))
	  return i;
    }
    return 0; //if byAssetClass, then unknown otherwise its the only answer...
  }

  public Set getTagSet(){
    if(byAssetClass)
      return assetClassSet;
    //Just return the single tag:
    return Collections.singleton(singleTagName);
  }

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected Tallier getNewTallier(){
    return new AssetFlowTally(byAssetClass, useWeight);
  }

  //Inner Classes:
  ////////////////

  /** 
   * Represents one bin of time in a Chronicle
   **/
  public class AssetFlowFrame extends Frame{
    
    public AssetFlowFrame(){
      total = null;
    }
    
    protected AssetFlowFrame(Map posToTallier){
      total = null;
      this.posToTallier = posToTallier;
    }

    /** increment the Tallier at the starting position; increment total **/
    public void addStartData(TransitData td){
      Tallier t = (Tallier)posToTallier.get(td.getStartPosition());
      if(t==null){
	t=getNewTallier();
	posToTallier.put(td.getStartPosition(), t);
      }
      t.increment(td);

      updatePosToTotal((AssetFlowTally)t,
		       (AssetFlowTransitData)td);
    }

    //NOTE If you use the other "add" functions, make sure to subclass them

    protected void updatePosToTotal(AssetFlowTally current,
				    AssetFlowTransitData aftd){
      AssetFlowTally aft = (AssetFlowTally)posToTotalTally.
	get(aftd.getStartPosition());
      if(aft==null){
	aft=(AssetFlowTally)getNewTallier();
	posToTotalTally.put(aftd.getStartPosition(), aft);
      }
      aft.setMax(current, aftd.getAssetClass());
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
      return new AssetFlowFrame(m);
    }
  }

  /** Incoming data for each asset class transport task.**/
  public static class AssetFlowTransitData extends AssetClassTransitData{
    protected float weight;

    public AssetFlowTransitData(Position start,
				 Position end,
				 long startDate,
				 long endDate,
				 int assetClass,
				 int count,
				 float weight){
      super(start,end,startDate,endDate,assetClass,count);
      this.weight=weight;
    }

    public void setWeight(float weight){this.weight=weight;}
    public float getWeight(){return weight;}
    
    /**
     * Used to clone a copy of a TransitData, but set new values for its
     * positions and dates
     **/
    public TransitData cloneNewDatePos(Position startP,
				       Position endP,
				       long startD,
				       long endD){
      return new AssetFlowTransitData(startP,endP,startD,endD,
				      assetClass,count, weight);
    }
  }

  /**
   * Actual data for a time-loc bin.  In this case an array of integers, one
   * per assetClass.
   **/
  public class AssetFlowTally implements TagTallier{

    protected float[] assetClassCounts;
    protected boolean byAssetClass;
    protected boolean useWeight;

    public AssetFlowTally(boolean byAssetClass, boolean useWeight){
      this.byAssetClass=byAssetClass;
      this.useWeight=useWeight;
      if(byAssetClass){
	assetClassCounts=new float[assetClassNames.length];
	for(int i=0;i<assetClassCounts.length;i++)
	  assetClassCounts[i]=0;
      }else{
	assetClassCounts=new float[1];
	assetClassCounts[0]=0f;
      }
    }

    public Map getTagToCountMap(){
      HashMap map = new HashMap(assetClassCounts.length);
      if(byAssetClass){
	for(int i=0;i<assetClassCounts.length;i++){
	  if(assetClassCounts[i]>0)
	    map.put(assetClassNames[i],new Integer((int)Math.ceil(assetClassCounts[i])));
	}
      }else{
	return getSingletonMap(singleTagName,
			       new Integer((int)Math.ceil(assetClassCounts[0])));
      }
      return map;
    }

    /** Return the count for a given asset class**/
    public int getCount(String assetClass){
      if(byAssetClass){
	int ac = getAssetClassNumber(byAssetClass,assetClass);
	return (int)Math.ceil(assetClassCounts[ac]);
      }else{
	return (int)Math.ceil(assetClassCounts[0]);
      }
    }

    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean increment(TransitData td){
      AssetFlowTransitData aftd = (AssetFlowTransitData)td;
      float amount=aftd.getCount() * (useWeight?aftd.getWeight():1f);
      return updateAssetFlow(aftd.getAssetClass(),amount);
    }

    /** 
     * Called for each piece of transit data that is recieved 
     **/
    public boolean decrement(TransitData td){
      AssetFlowTransitData aftd = (AssetFlowTransitData)td;
      float amount=aftd.getCount() * (useWeight?aftd.getWeight():1f);
      return updateAssetFlow(aftd.getAssetClass(),-amount);
    }

    protected boolean updateAssetClass(int assetClass, int amount){
      logMessage(CRITICAL,"Error: updateAssetClass(int, int) was called "+
			 "instead of updateAssetFlow(int, float)");
      return false;
    }

    protected boolean updateAssetFlow(int assetClass, float amount){
      assetClass = byAssetClass?assetClass:0;
      assetClassCounts[assetClass]+=amount;
      if(assetClassCounts[assetClass] < 0){
	logMessage(MIDDLING,"Unexpected negative count in '"
			     + assetClass + "': " + amount);
	  return false;
      }
      return true;
    }

    /** 
     * set this Tally to the max of this tally and the Tally passed in as an
     * argument for the assetClass supplied.
     **/
    public void setMax(AssetFlowTally current, int assetClass){
      assetClass = byAssetClass?assetClass:0;
      assetClassCounts[assetClass]=
	Math.max(assetClassCounts[assetClass],
		 current.assetClassCounts[assetClass]);
    }

    /** This should return a deep copy of this instance **/
    public Tallier deepClone(){
      AssetFlowTally ret=new AssetFlowTally(byAssetClass,useWeight);

      /* We don't want to copy the data -- each bin will have to calculate
         the flow independently -- it doesn't carry over to the next bin.
      for(int i=0;i<assetClassCounts.length;i++)
	ret.assetClassCounts[i]=assetClassCounts[i];
      */
      return ret;
    }

    public String toString(){
      StringBuffer sb = new StringBuffer();
      sb.append("(");
      for(int i=0;i<assetClassCounts.length;i++){
	sb.append(i);
	sb.append(" = ");
	sb.append(assetClassCounts[i]);
	sb.append(",");
      }
      sb.append(")");
      return sb.toString();
    }
  }

  /** we require this function as we may need to compile on 1.2.2 and
   * Collections.singletonMap(Object,Object) is not available til 1.3
   **/
  public static Map getSingletonMap(Object key, Object value){
    //Total hack to avoid having to write the class...
    Map ret=new HashMap(1);
    ret.put(key,value);
    return ret;
  }
}
