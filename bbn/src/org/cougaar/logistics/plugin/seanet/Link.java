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
