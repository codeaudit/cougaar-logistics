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
 * Holds a Chronicle of data about the location of a UNIT's assets
 *
 * @author Benjamin Lubin; last modified by $Author: gvidaver $
 *
 * @since 11/15/00
 */
public class UnitChronicle extends TagChronicle{

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public UnitChronicle(){
    super();
  }

  public UnitChronicle(int binSize){
    super(binSize);
  }
  
  //Functions:
  ////////////

  /**
   * This function returns a new Tallier object that records the low-level
   * data.
   **/
  protected Tallier getNewTallier(){
    return new UnitTally();
  }

  //Inner Classes:
  ////////////////

  /** Incoming data for each unit transport task.**/
  public static class UnitTransitData extends TagTransitData{
    public UnitTransitData(Position start,
			   Position end,
			   long startDate,
			   long endDate,
			   String unit,
			   int count){
      super(start,end,startDate,endDate,
	    unit.startsWith("UIC/")?unit.substring(4).intern():unit.intern(),
	    count);
    }

    /**
     * Used to clone a copy of a TransitData, but set new values for its
     * positions and dates
     **/
    public TransitData cloneNewDatePos(Position startP,
				       Position endP,
				       long startD,
				       long endD){
      return new UnitTransitData(startP,endP,startD,endD,tag,count);
    }
  }

  /**
   * Actual data for a time-loc bin.
   **/
  public class UnitTally extends TagTally{

    public UnitTally(){
    }
  }
}
