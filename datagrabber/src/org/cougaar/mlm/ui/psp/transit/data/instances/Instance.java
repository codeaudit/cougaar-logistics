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
package org.cougaar.mlm.ui.psp.transit.data.instances;

import org.cougaar.planning.servlet.data.xml.*;

import org.cougaar.core.util.UID;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.xml.sax.Attributes;

/**
 * A single instance leaving the Instance PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class Instance implements XMLable, DeXMLable, Serializable{

  //Constants:
  ////////////

  //Tags:
  public static final String NAME_TAG = "Instance";
  //Attr:

  protected static final String UID_ATTR = "UID";
  protected static final String ITEM_NOMEN_ATTR = "ItemNomen";
  protected static final String AGGREGATE_ATTR = "Agg";
  protected static final String PROTOTYPE_UID_ATTR = "PUID";
  protected static final String MANIFEST_UID_ATTR = "MUID";
  protected static final String OWNER_ID_ATTR="OwnerID";
  protected static final String NAME_ATTR="name";

  //Variables:
  ////////////

  public UID UID;
  /**item nomenclature, not to be confused with the proto's *type* nomen**/
  public String itemNomen;
  public long aggregateNumber;
  public String prototypeUID;
  public String ownerID;
  /**if a Container, MilVan, or Pallet**/
  public UID manifestUID;
  public boolean hasManifest;
  public List nomenclatures;
  public List typeIdentifications;
  public List weights;
  public String name;

  //Constructors:
  ///////////////

  public Instance(){
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
    w.sitagln(NAME_TAG, 
	      UID_ATTR,UID.toString(),
	      ITEM_NOMEN_ATTR,itemNomen,
	      AGGREGATE_ATTR,Long.toString(aggregateNumber),
	      PROTOTYPE_UID_ATTR,prototypeUID,
	      OWNER_ID_ATTR, ownerID,
	      MANIFEST_UID_ATTR,manifestUID.toString(),
	      NAME_ATTR,name);
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
	itemNomen=attr.getValue(ITEM_NOMEN_ATTR);
	aggregateNumber=Long.parseLong(attr.getValue(AGGREGATE_ATTR));
	prototypeUID=attr.getValue(PROTOTYPE_UID_ATTR);
	ownerID=attr.getValue(OWNER_ID_ATTR);
	manifestUID=UID.toUID(attr.getValue(MANIFEST_UID_ATTR));
	name=attr.getValue(NAME_ATTR);
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

  private static final long serialVersionUID = 237849283718923764L;
}
