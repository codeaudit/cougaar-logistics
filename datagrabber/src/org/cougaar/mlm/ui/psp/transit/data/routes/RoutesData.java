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
package org.cougaar.mlm.ui.psp.transit.data.routes;

import org.cougaar.core.util.UID;

import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Routes PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class RoutesData implements XMLable, DeXMLable, Serializable{

  //Constants:

  //Tags:
  public static final String NAME_TAG = "Routes";
  //Attr:

  //Variables:
  ////////////

  protected Map routes;

  //Constructors:
  ///////////////

  public RoutesData(){
    routes = new HashMap(89);
  }

  //Members:
  //////////

  public int numRoutes(){
    return routes.size();
  }

  public Route getRoute(UID uid){
    return (Route)routes.get(uid);
  }

  public void addRoute(Route r){
    routes.put(r.getUID(), r);
  }

  public Iterator getRoutesIterator(){
    return routes.values().iterator();
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);

    Iterator iter = getRoutesIterator();
    while(iter.hasNext()){
      Route r = (Route)iter.next();
      r.toXML(w);
    }

    w.cltagln(NAME_TAG);
  }

  //DeXMLable members:
  //------------------

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  public void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException{

    if(name.equals(NAME_TAG)){
    }else{
      throw new UnexpectedXMLException("Unexpected tag: "+name);    
    }
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException{
    return name.equals(NAME_TAG);
  }

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException{
    if(obj instanceof Route){
      addRoute((Route)obj);
    }else{
      throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
    }
  }
  //Inner Classes:

  private static final long serialVersionUID = 783209458761767671L;
}
