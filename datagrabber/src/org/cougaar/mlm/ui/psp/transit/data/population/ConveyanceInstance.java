/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import org.cougaar.planning.servlet.data.xml.*;

import org.cougaar.core.util.UID;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.xml.sax.Attributes;

/**
 * Represents the data leaving the PSP
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 1/24/01
 **/
public class ConveyanceInstance implements XMLable, DeXMLable, Serializable{

  //Variables:
  ////////////

  //Tags:
  public static final String NAME_TAG = "ConveyanceInstance";
  protected static final String UID_TAG = "UID";
  protected static final String PROTOTYPE_TAG = "Prototype";
  protected static final String BUMPER_TAG = "BumperNo";
  protected static final String ITEM_NOMEN_TAG = "ItemNomen";
  protected static final String HOME_TAG = "HomeBase";
  protected static final String OWNER_TAG = "Owner";
  protected static final String SELF_PROP_TAG = "SelfProp";

  //Variables:
  public UID UID;
  public String prototypeUID; // may not look like a Cougaar UID
  public String bumperNo;
  /**item nomenclature, not to be confused with the proto's *type* nomen**/
  public String itemNomen;
  public String homeLocID;
  public String ownerID;
  public boolean selfPropelled;

  //Constructors:
  ///////////////

  public ConveyanceInstance(){
  }

  //Members:
  //////////

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);

    w.tagln(UID_TAG,UID.toString());
    w.tagln(PROTOTYPE_TAG,prototypeUID);
    w.tagln(BUMPER_TAG,bumperNo);
    w.tagln(ITEM_NOMEN_TAG,itemNomen);
    w.tagln(HOME_TAG,homeLocID);
    w.tagln(OWNER_TAG,ownerID);
    w.tagln(SELF_PROP_TAG, selfPropelled?"T":"F");

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
      }else if(name.equals(UID_TAG)){
	UID=UID.toUID(data);
      }else if(name.equals(PROTOTYPE_TAG)){
	prototypeUID=data;
      }else if(name.equals(BUMPER_TAG)){
	bumperNo=data;
      }else if(name.equals(ITEM_NOMEN_TAG)){
	itemNomen=data;
      }else if(name.equals(HOME_TAG)){
	homeLocID=data;
      }else if(name.equals(OWNER_TAG)){
	ownerID=data;
      }else if(name.equals(SELF_PROP_TAG)){
	selfPropelled=data.equals("T");
      }else{
	throw new UnexpectedXMLException("Unexpected tag: "+name);    
      }
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Malformed Number: " + 
				       name + " : " + data);
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
  }

  //Inner Classes:
  private static final long serialVersionUID = 800400062501234567L;
}
