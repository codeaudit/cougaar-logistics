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
package org.cougaar.mlm.ui.psp.transit.data.prototypes;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.planning.servlet.data.xml.*;

import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Prototypes PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class PrototypesData implements XMLable, DeXMLable, Serializable{

  //Constants:

  //Tags:
  public static final String NAME_TAG = "Prototypes";
  //Attr:

  //Variables:
  ////////////

  protected Map prototypes;
  protected Set cccEntries;

  //Constructors:
  ///////////////

  public PrototypesData(){
    prototypes = new HashMap(89);
    cccEntries = new HashSet(89);
  }

  //Members:
  //////////

  public int numPrototypes(){
    return prototypes.size();
  }
  public int numCCCEntries(){
    return cccEntries.size();
  }

  public Prototype getPrototype(String uid){
    return (Prototype)prototypes.get(uid);
  }

  public void addPrototype(Prototype p){
    prototypes.put(p.UID, p);
    /*
    System.out.println (this + " - Known prototypes :");
    for (Iterator iter = prototypes.keySet().iterator(); iter.hasNext (); ) {
      Object key = iter.next();
      Prototype value = (Prototype) prototypes.get (key);
      System.out.println ("\t" + key + "->" + value.alpTypeID + "-" + value.nomenclature);
    }
    */
  }

  public void addCCCEntry(CargoCatCode ccc){
    cccEntries.add (ccc);

    /*
    System.out.println (this + " - Known cccs :");
    for (Iterator iter = cccEntries.iterator(); iter.hasNext (); ) {
      Object cccEntry = iter.next();
      System.out.println ("\t" + cccEntry);
    }
    */
  }

  public Iterator getPrototypesIterator(){
    return prototypes.values().iterator();
  }

  public Iterator getCCCEntriesIterator(){
    return cccEntries.iterator();
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
      Prototype proto = (Prototype)protoIter.next();
      proto.toXML(w);
    }

    Iterator iter = getCCCEntriesIterator();
    while(iter.hasNext()){
      Collection stuff = (Collection)iter.next();
      for (Iterator iter2 = stuff.iterator (); iter2.hasNext(); ) {
	CargoCatCode ccc = (CargoCatCode)iter2.next();
	ccc.toXML(w);
      }
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
    if(obj instanceof Prototype){
      addPrototype((Prototype)obj);
    }
    else if(obj instanceof CargoCatCode) {
      CargoCatCode ccc = (CargoCatCode)obj;
      addCCCEntry(ccc);
    }else{
      throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
    }
  }
  //Inner Classes:

  private static final long serialVersionUID = 132323441544324234L;
}
