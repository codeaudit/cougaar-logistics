package org.cougaar.logistics.plugin.seanet;

import java.util.Iterator;
import java.util.Vector;

/** A sea route network.  It uses a standard network of nodes and
    links provided by NodeData.class.  The Panama and Suez canals can
    be opened and closed.

    <p> The network should be used as a stateful calculator of
    distances and routes:

    <ul>

    <li> Use setFrom(lat, lon) to set a from location and compute
    distances from it and the network nodes.

    <li> Use setTo(lat, lon) to set a to location.

    <li> Use distance() to compute the distance in nautical miles
    between from and to.

    <li> Use route() to return an iterator of Location's of the route
    from to to from.  The locations along the route are only valid
    while the from point has not change.

    </ul>

 **/
public class Network {

  int now = 0;

  public Network() {
    this((new NodeData()).getNodes());
  }

  public Network(Vector nodes) {
    this.nodes = nodes;
    this.fringe1 = new Vector(nodes.size());
    this.fringe2 = new Vector(nodes.size());
  }

  Link getLink(int i, int j) {
    return ((Link)((Node) nodes.elementAt(i)).links.elementAt(j));
  }

  public void setPanamaCanal(boolean isOpen) {
    getLink(0,0).setIsOpen(isOpen);
    getLink(1,0).setIsOpen(isOpen);
  }

  public boolean getPanamaCanal() {
    return getLink(0,0).isOpen();
  }
  public void setSuezCanal(boolean isOpen) {
    getLink(2,0).setIsOpen(isOpen);
    getLink(3,0).setIsOpen(isOpen);
  }

  public boolean getSuezCanal() {
    return getLink(2,0).isOpen();
  }
  Vector nodes;
  public Iterator getNodes() { return nodes.iterator(); }

  Vector fringe1, fringe2;

  double fromLat, fromLon;
  Node from;
  public void setFrom(double lat, double lon) {
    this.fromLat = lat;
    this.fromLon = lon;
    this.setFrom(this.closestNode(lat, lon));
  }

  double toLat, toLon;
  Node to;
  public void setTo(Node to) { this.to = to; }
  public void setTo(double lat, double lon) {
    this.toLat = lat;
    this.toLon = lon;
    this.setTo(this.closestNode(lat, lon));
  }

  public double distance(double fromLat, double fromLon,
			 double toLat, double toLon) {
    this.setFrom(fromLat, fromLon);
    return this.distance(toLat,toLon);
  }

  public double distance(double toLat, double toLon) {
    this.setTo(toLat, toLon);
    return distance();
  }

  public double distance() {
    return this.to.distance(this.toLat, this.toLon) +
      to.getCost() + 
      this.from.distance(this.fromLat, this.fromLon);
  }

/**
   Search the entire net so distance and routes to the from point can
   be computed easily.
 **/
  public void setFrom(Node from) {
    this.now = this.now + 1;
    this.from = from;
    for(Iterator i = getNodes(); i.hasNext();)
      ((Node) i.next()).setCost(Node.HUGE);
    from.setCost(0);

    fringe1.setSize(0);
    fringe1.add(from);
    fringe2.setSize(0);
    
    propagate(fringe1, fringe2);
    fringe1.setSize(0);
    fringe2.setSize(0);
  }

  void propagate(Vector fringe1, Vector fringe2) {
    fringe2.setSize(0);
    for(Iterator i1 = fringe1.iterator(); i1.hasNext();)
      ((Node) i1.next()).propagateToLinks(fringe2);
    if (fringe2.size() > 0) propagate(fringe2, fringe1);
  }

  /**
   * Find the closest Node in the network to this location.
   */
  public Node closestNode(double latitude, double longitude) {
    GreatCircle gc = new GreatCircle(latitude, longitude);
    Node minNode = null;
    double minDistance = Node.HUGE;
    Iterator i = this.getNodes();
    while (i.hasNext()) {
      Node n = ((Node) i.next());
      gc.setPoint2(n.getLatitude(), n.getLongitude());
      double d = gc.distanceNM();
      if (d < minDistance) {
	minNode = n;
	minDistance = d;
      }
    }
    return minNode;
  }

  public Iterator route() {
    return new SequenceIterator
      (new SingletonIterator (new LocationImpl(toLat, toLon)),
       new SequenceIterator (new RouteIterator(this.to),
			     new SingletonIterator
			       (new LocationImpl(fromLat, fromLon))));
  }

  public Iterator routeNoSource() {
    return new SequenceIterator (new RouteIterator(this.to),
				 new SingletonIterator
				   (new LocationImpl(fromLat, fromLon)));
  }


  public Iterator routeNoSourceOrDestination() {
    return new RouteIterator(this.to);
  }

  public Iterator routeNoDestination() {
    return new SequenceIterator
      (new SingletonIterator (new LocationImpl(toLat, toLon)),
       new RouteIterator(this.to));
  }

  public class RouteIterator implements Iterator {
    int now;
    Node here;

    public RouteIterator(Node here) {
      this.here = here;
      this.now = Network.this.now;
    }
    
    void check() {
      if (this.now != Network.this.now)
	throw new java.util.ConcurrentModificationException
	  ("Underlying network has changed.");
    }
    public boolean hasNext() {
      check();
      return this.here != null;
    }

    public Object next() {
      check();
      Object result = this.here;
      if (this.here == Network.this.from) {
	this.here = null;
      }
      else {
	this.here = this.here.getBack();
      }
      return result;
    }

    public void remove() { throw new UnsupportedOperationException(); }
  }
}
