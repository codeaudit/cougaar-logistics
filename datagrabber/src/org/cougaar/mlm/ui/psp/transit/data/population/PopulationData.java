/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
