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
