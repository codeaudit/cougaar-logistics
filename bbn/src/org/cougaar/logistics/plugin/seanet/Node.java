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

import java.util.Vector;
import java.util.Iterator;

/**
   A Node in the network.  Nodes are connected by Link's.

   <p>The cost (distance) of the node is computed by its Network.

 **/

public class Node extends LocationImpl {

  /** A number larger than any distance, used to initialize cost. **/
  static final int HUGE = 100000;

  public Node(String name, double lat, double lon) {
    super(lat, lon);
    this.name = name;
  }

  String name;
  public String getName() { return this.name; }

  Vector links = new Vector(3,1);

  public Iterator getLinks() { return links.iterator(); }

  /** Add Unidirectional Link from this to node to. **/
  public void addLink(Node to, String name, int distance) {
    links.add(new Link(name, distance, to));
  }
  
  /** Minimum cost from From to here, so far. **/
  int cost;
  public int getCost() { return cost; }
  public void setCost(int newValue) { this.cost = newValue; }
  
  public void propagateToLinks(Vector fringe2) {
    int c1 = this.getCost();
    for(Iterator i = this.getLinks(); i.hasNext();) {
      Link link = ((Link) i.next());
      if (link.isOpen()) {
	int c2 = link.getToCost();
	if (c1 + link.getDistance() < c2) {
	  link.setToCost(c1 + link.getDistance());
	  link.setToBack(this);
	  fringe2.add(link.getToNode());
	}}}}

  /** Next node along best path backward to From. **/
  Node back;
  public Node getBack() { return this.back; }
  public void setBack(Node back) { this.back = back; }
  
  public String toString() {
    return "node(\"" + name + "\"," + latitude + "," + longitude + ")";
  }

}
