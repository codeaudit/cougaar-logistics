/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
