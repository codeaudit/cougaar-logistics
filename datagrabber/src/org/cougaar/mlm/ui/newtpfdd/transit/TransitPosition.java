/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

/** Position of an object beeing moved between two other Positions
 * @author Benjamin Lubin; last modified by $Author: mthome $
 *
 * @since 11/14/00
 */
public class TransitPosition implements Position {
  protected Position from;
  protected Position to;
  
  public TransitPosition(Position from, Position to){
    this.from=from;
    this.to=to;
    if(from.equals(to))
      System.err.println("TransitPosition: Unexpected condition from == to");
  }
  
  public String getName(){
    return from.getName()+" -> "+to.getName();
  }

  /** 2/3 of the way to 'to' **/
  public float getLat(){
    return ( from.getLat()/3f + to.getLat()*2f/3f );
  }
  
  /** 2/3 of the way to 'to' **/
  public float getLon(){
    //For half way:  A/2 + B/2 + (|A|+|B|>180)?(A+B>0?-180:180):0

    float a1=from.getLon() / 3f;
    float a2=to.getLon() *2f / 3f;

    return a1+ a2 +
      ( ( (Math.abs(a1) + Math.abs(a2)) > 180 )?
	( ((a1+a2)>0)?-180:180):
	0);
  }
  
  public Position getPosition1(){return from;}
  public Position getPosition2(){return to;}
  
  public int hashCode(){
    return (from.hashCode() + to.hashCode())/2;
  }
  
  public boolean equals(Object o){
    return (o instanceof TransitPosition &&
	    from.equals( ((TransitPosition)o).from ) &&
	    to.equals( ((TransitPosition)o).to ));
  }
  public String toString(){
    return from + "->" + to;
  }
}
