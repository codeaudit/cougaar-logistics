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
import java.io.Serializable;


/** Incoming data for each transport task.
 * @author Benjamin Lubin; last modified by $Author: gvidaver $
 *
 * @since 11/14/00
 */
public abstract class TransitData implements Serializable, Comparable{
  public abstract Position getStartPosition();
  public abstract Position getEndPosition();
  public abstract long getStartDate();
  public abstract long getEndDate();
  
  public int compareTo(Object o){
    return (int)(getStartDate() - ((TransitData)o).getStartDate());
  }

  /**
   * Used to clone a copy of a TransitData, but set new values for its
   * positions and dates
   **/
  public abstract TransitData cloneNewDatePos(Position startP,
					      Position endP,
					      long startD,
					      long endD);
}
