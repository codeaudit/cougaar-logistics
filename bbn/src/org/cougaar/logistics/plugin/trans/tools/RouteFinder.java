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

import org.cougaar.logistics.plugin.seanet.Location;
import org.cougaar.logistics.plugin.seanet.LocationImpl;
import org.cougaar.logistics.plugin.seanet.Network;
import org.cougaar.logistics.plugin.seanet.Node;

import java.util.*;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.util.log.Logger;

public class RouteFinder {
  Network network = new Network ();
  Map geolocToGeolocToRoute = new HashMap ();
  int numRoutes = 0;
  int numNodes = 0;
  int numLinks = 0;
  PlanningFactory factory;
  Logger logger;

  public RouteFinder (Logger logger) { 
    measureHelper = new GLMMeasure (logger); 
    this.logger = logger;
  }

  public void setFactory (PlanningFactory factory) { this.factory = factory; }

  /** caches routes after getting them from the handy seanet facility! */
  public Distance getDistance (GeolocLocation from, GeolocLocation to) { 
    return getRoute (from, to).getLength ();
  }

  public TransportationRoute getRoute (GeolocLocation from, GeolocLocation to) {
    Map toGeolocRouteMap = (Map) geolocToGeolocToRoute.get (from.getGeolocCode());
    TransportationRoute route = null;
    if (toGeolocRouteMap != null) {
      route = (TransportationRoute) toGeolocRouteMap.get(to.getGeolocCode());
    }
    else {
      geolocToGeolocToRoute.put (from, (toGeolocRouteMap = new HashMap ()));
    }

    if (route != null)
      return route;
    
    Location fromLoc = getLocation (from);
    Location toLoc   = getLocation (to);
    network.setFrom (fromLoc.getLatitude(), fromLoc.getLongitude ());
    network.setTo   (toLoc.getLatitude(),   toLoc.getLongitude ());

    route = makeRoute ();

    Vector nodes = new Vector (33);

    Iterator routeIter;
    
    routeIter = network.routeNoSourceOrDestination ();

    // create list of nodes
    /*
    System.out.print ("Full Route is ");
    for (Iterator iter = network.route ();iter.hasNext ();) {
      Location node = (Location) iter.next ();
      System.out.print ("(" + node.getLatitude () + "-" + node.getLongitude() + ")\t");
    }
    System.out.println ("");
    */

    /*
    System.out.print ("Partial Route is ");
    for (Iterator iter = network.routeNoSourceOrDestination (); iter.hasNext ();) {
      Location node = (Location) iter.next ();
      String name = (node instanceof Node) ? ((Node)node).getName () : "anon";

      System.out.println ("(" + name + " " + node.getLatitude () + "-" + node.getLongitude() + ")\t");
    }
    */

    for (;routeIter.hasNext ();) {
      Object nextObj = routeIter.next ();
      nodes.add (0, getNode((Location) nextObj));
    }
    
    route = makeRouteFromNodes (nodes);

    if (nodes.size () != route.getNodes().size ())
      logger.error ("ERROR - nodes in " + nodes.size () + " != nodes out " + route.getNodes().size());

    //    for (int i = 0; i < route.getNodes().size (); i++)
    //      System.out.println ("Out : " + ((TransportationNode) route.getNodes().get(i)).getGeolocLocation());

    return route;
  }

  /** this is inefficient, but not awful... */
  public TransportationRoute makeRouteWithPOEandPOD (TransportationRoute route, GeolocLocation POE, GeolocLocation POD) {
    Vector nodes = route.getNodes ();

    nodes.add (0, getNode (getLocation(POE)));
    nodes.add (   getNode (getLocation(POD)));

    TransportationRoute routeOut = makeRouteFromNodes (POE, POD, nodes);

    if (nodes.size () != routeOut.getNodes().size ())
      logger.error ("ERROR 2 - nodes in " + nodes.size () + " != nodes out " + routeOut.getNodes().size());
    
    //    for (int i = 0; i < routeOut.getNodes().size (); i++)
    //      System.out.println ("Out 2 : " + ((TransportationNode) routeOut.getNodes().get(i)).getGeolocLocation());

    return routeOut;
  }

  /** this is inefficient, but not awful... */
  protected TransportationRoute makeRouteFromNodes (GeolocLocation from, GeolocLocation to, Collection nodes) {
    TransportationNode source = null, destination = null; 
    TransportationNode last = null;
    TransportationNode node = null;
    Vector links = new Vector (); // has to be a vector because setLinks expects one...

    TransportationRoute route = makeRoute ();
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

    return route;
  }

  protected TransportationRoute makeRouteFromNodes (Collection nodes) {
    TransportationNode source = null, destination = null; 
    TransportationNode last = null;
    TransportationNode node = null;
    Vector links = new Vector (); // has to be a vector because setLinks expects one...

    TransportationRoute route = makeRoute ();
    // now link them
    for (Iterator iter = nodes.iterator (); iter.hasNext ();) {
      node = (TransportationNode) iter.next ();
      route.addNode (node);

      if (last != null)
	links.add (linkNodes (last, node));

      if (source == null) {
	source = node;
      }

      last = node;
    }

    destination = node;
    
    route.setLinks (links);
    route.setSource (source);
    route.setDestination (destination);

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
