/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.seanet;

/**
   A unidirectional link from a Node to another Node, the "toNode".
   The distance (nautical miles) between the Node's is stored as an int.
   The link will not be traversed if it is not open.
   Methods named "*To*()" operate on the toNode.
 **/

public class Link {
  String name;
  public String getName() { return name; }
  
  int distance;
  public int getDistance() { return distance; }
  
  Node toNode;
  public Node getToNode() { return toNode; }
  
  boolean isOpen = true;
  public boolean isOpen() { return isOpen; }
  public void setIsOpen(boolean newValue) { this.isOpen = newValue;}

  public Link (String name, int distance, Node toNode) {
    this.name = name;
    this.distance = distance;
    this.toNode = toNode;
  }

  public String toString() {
    return "{Link: " + name + " (" + distance + ") ->" + toNode;
  }

  public int getToCost()        { return this.getToNode().getCost(); }
  public void setToCost(int c)  { this.getToNode().setCost(c); }
  public void setToBack(Node n) { this.getToNode().setBack(n); }
}
