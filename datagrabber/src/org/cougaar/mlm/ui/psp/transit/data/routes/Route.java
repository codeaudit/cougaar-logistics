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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.xml.sax.Attributes;

/**
 * A single instance leaving the Routes PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class Route implements XMLable, DeXMLable, Serializable{

  //Constants:
  ////////////

  //Tags:
  public static final String NAME_TAG = "Route";
  protected static final String SEGMENT_TAG = "Segment";
  //Attr:

  protected static final String UID_ATTR = "UID";
  protected static final String SEGMENT_LOC_ATTR= "LocID";

  //Variables:
  ////////////

  UID UID;
  List segments;
  
  //Constructors:
  ///////////////

  public Route(){
    segments=new ArrayList();
  }

  //Members:
  //////////

  public void setUID(UID UID){
    this.UID=UID;
  }

  public UID getUID(){
    return UID;
  }

  public int numSegments(){
    return segments.size();
  }

  public Iterator getSegmentLocIDIterator(){
      return segments.iterator();
  }

  /** Get the UID of a location within this route**/
  public String getSegmentLocIDAt(int i){
    return (String)segments.get(i);
  }

  public void addSegmentLocID(String locID) {
    segments.add(locID);
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG, UID_ATTR, getUID().toString());

    for(int i=0;i<numSegments();i++)
      w.sitagln(SEGMENT_TAG,
	      SEGMENT_LOC_ATTR,
	      getSegmentLocIDAt(i));

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

    try{
      if(name.equals(NAME_TAG)){
	UID=UID.toUID(attr.getValue(UID_ATTR));
      }else if(name.equals(SEGMENT_TAG)){
        String attrVal = attr.getValue(SEGMENT_LOC_ATTR);
        if (attrVal != null) {
          addSegmentLocID(attrVal);
        }
      }else{
	throw new UnexpectedXMLException("Unexpected tag: "+name);    
      }
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Could not parse number: "+e);
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
    throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
  }
  //Inner Classes:

  private static final long serialVersionUID = 423059766189372098L;
}
