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
package org.cougaar.mlm.ui.psp.transit.data.population;

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
 * Represents the data leaving the PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/24/01
 **/
public class PopulationData implements XMLable, DeXMLable, Serializable{

  //Variables:
  ////////////

  public static final String NAME_TAG = "Population";

  protected Map conveyancePrototypes;

  protected Map conveyanceInstances;

  //Constructors:
  ///////////////

  public PopulationData(){
    conveyancePrototypes = new HashMap(89);
    conveyanceInstances = new HashMap(89);
  }

  //Members:
  //////////

  public int numPrototypes(){
    return conveyancePrototypes.size();
  }

  public int numInstances(){
    return conveyanceInstances.size();
  }

  public ConveyancePrototype getPrototype(String uid){
    return (ConveyancePrototype)conveyancePrototypes.get(uid);
  }

  public ConveyanceInstance getInstance(UID uid){
    return (ConveyanceInstance)conveyanceInstances.get(uid);
  }

  public void addPrototype(ConveyancePrototype cp){
    conveyancePrototypes.put(cp.UID, cp);
  }

  public void addInstance(ConveyanceInstance ci){
    conveyanceInstances.put(ci.UID, ci);
  }

  public Iterator getPrototypesIterator(){
    return conveyancePrototypes.values().iterator();
  }

  public Iterator getInstancesIterator(){
    return conveyanceInstances.values().iterator();
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    Iterator protoIter = getPrototypesIterator();
    while(protoIter.hasNext()){
      ConveyancePrototype proto = 
        (ConveyancePrototype)protoIter.next();
      proto.toXML(w);
    }
    Iterator instIter = getInstancesIterator();
    while(instIter.hasNext()){
      ConveyanceInstance inst = 
        (ConveyanceInstance)instIter.next();
      inst.toXML(w);
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
    if(!name.equals(NAME_TAG))
      throw new UnexpectedXMLException("Unexpected tag: "+name);    
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
    if(obj instanceof ConveyancePrototype){
      addPrototype((ConveyancePrototype)obj);
    }else if(obj instanceof ConveyanceInstance){
      addInstance((ConveyanceInstance)obj);
    }else{
      throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
    }
  }
  //Inner Classes:

  private static final long serialVersionUID = 781203498712387244L;
}
