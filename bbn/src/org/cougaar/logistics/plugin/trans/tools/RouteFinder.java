package org.cougaar.logistics.plugin.trans.tools;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.NewSeaLinkPG;
import org.cougaar.glm.ldm.asset.SeaLinkPGImpl;
import org.cougaar.glm.ldm.asset.TransportationLink;
import org.cougaar.glm.ldm.asset.TransportationNode;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;
import org.cougaar.glm.ldm.plan.NewNamedPosition;
import org.cougaar.glm.ldm.plan.GeolocLocationImpl;
import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.glm.seanet.Location;
import org.cougaar.glm.seanet.LocationImpl;
import org.cougaar.glm.seanet.Network;
import org.cougaar.glm.seanet.Node;

import java.util.*;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.util.log.Logger;

public class RouteFinder {
  Network network = new Network ();
  Map geolocToGeolocToRoute = new HashMap ();
  int numRoutes = 0;
  int numNodes = 0;
  int numLinks = 0;
  RootFactory factory;

  public RouteFinder (Logger logger) { measureHelper = new GLMMeasure (logger); }

  public void setFactory (RootFactory factory) { this.factory = factory; }

  /** caches routes after getting them from the handy seanet facility! */
  public Distance getDistance (GeolocLocation from, GeolocLocation to) { 
    return getRoute (from, to, true).getLength ();
  }

  public TransportationRoute getRoute (GeolocLocation from, GeolocLocation to, boolean includeDestination) {
    Map toGeolocRouteMap = (Map) geolocToGeolocToRoute.get (from.getGeolocCode());
    TransportationRoute route = null;
    if (toGeolocRouteMap != null)
      route = (TransportationRoute) toGeolocRouteMap.get(to.getGeolocCode());
    else 
      geolocToGeolocToRoute.put (from, (toGeolocRouteMap = new HashMap ()));

    if (route != null)
      return route;
    
    Location fromLoc = getLocation (from);
    Location toLoc   = getLocation (to);
    network.setFrom (fromLoc.getLatitude(), fromLoc.getLongitude ());
    network.setTo   (toLoc.getLatitude(),   toLoc.getLongitude ());

    route = makeRoute ();

    TransportationNode source = null, destination = null; 
    TransportationNode last = null;

    Vector nodes = new Vector (33);

    // create list of nodes
    for (Iterator iter = (includeDestination ? network.route () : network.routeNoDestination ());iter.hasNext ();) {
      Object nextObj = iter.next ();
      nodes.add (0, getNode((Location) nextObj));
    }

    TransportationNode node = null;
    Vector links = new Vector ();
    // now link them
    for (Iterator iter = nodes.iterator (); iter.hasNext ();) {
      node = (TransportationNode) iter.next ();
      route.addNode (node);

      if (last != null)
	links.add (linkNodes (last, node));

      if (source == null) {
	source = node;
	((NewGeolocLocation)source.getGeolocLocation ()).setGeolocCode (from.getGeolocCode ());
	((NewNamedPosition) source.getGeolocLocation ()).setName (from.getName ());
      }

      last = node;
    }

    destination = node;
    ((NewGeolocLocation)destination.getGeolocLocation ()).setGeolocCode (to.getGeolocCode ());
    ((NewNamedPosition) destination.getGeolocLocation ()).setName (to.getName ());
    
    route.setLinks (links);
    route.setSource (source);
    route.setDestination (destination);
    
    toGeolocRouteMap.put(to.getGeolocCode(), route);

    return route;
  }

  protected Location getLocation (GeolocLocation geoloc) {
    return new LocationImpl (geoloc.getLatitude().getDegrees (),
			     geoloc.getLongitude().getDegrees ());
  }

  public int i = 0;

  protected TransportationNode getNode (Location loc) {
    TransportationNode node = makeNode ();

    String name = (loc instanceof Node) ? ((Node)loc).getName () : "node-"+i++;
    node.setGeolocLocation (new GeolocLocationImpl (Latitude.newLatitude(loc.getLatitude()),
						    Longitude.newLongitude(loc.getLongitude()),
						    name));
    return node;
  }

  protected TransportationLink linkNodes (TransportationNode first, TransportationNode second) {
    TransportationLink link = makeLink ();
    link.setOrigin (first);
    link.setDestination (second);
    first.addLink (link);
    NewSeaLinkPG seaLinkPG = new SeaLinkPGImpl ();
    seaLinkPG.setLinkLength (measureHelper.distanceBetween (first.getGeolocLocation(), 
							    second.getGeolocLocation()));
    link.setSeaLinkPG (seaLinkPG);
    return link;
  }
  
  protected TransportationRoute makeRoute () {
    return (TransportationRoute) factory.createInstance ("TRANSPORT_ROUTE", "Route-" + numRoutes++);
  }

  protected TransportationNode makeNode () {
    TransportationNode node = 
      (TransportationNode) factory.createInstance ("TRANSPORT_NODE", "Node-" + numNodes++);

    return node;
  }

  protected TransportationLink makeLink () {
    TransportationLink link = 
      (TransportationLink) factory.createInstance ("TRANSPORT_SEALINK", "Link-" + numLinks++);

    return link;
  }

  protected GLMMeasure measureHelper;
}


