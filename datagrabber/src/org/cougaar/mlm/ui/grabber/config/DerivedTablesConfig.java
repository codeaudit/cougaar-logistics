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
package org.cougaar.mlm.ui.grabber.config;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;

import org.cougaar.mlm.ui.grabber.derived.*;

import java.io.IOException;
import org.xml.sax.Attributes;

import java.io.File;

/**
 * Data for configuring derived table generation
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/12/01
 **/
public class DerivedTablesConfig implements XMLable, DeXMLable{

  //Constants:
  ////////////

  public static final String NAME_TAG = "DerivedTablesConfig";

  public static final String FIRST_LEG_TAG = "FirstLegTable";

  public static final String ROLLUP_TAG = "RollupTable";

  public static final String CARGO_TYPE_TAG = "CargoTypeTable";
  public static final String CARGO_INSTANCE_TAG = "CargoInstanceTable";
  public static final String CARGO_LEG_TAG = "CargoLegTable";

  public static final String CARRIER_TYPE_TAG = "CarrierTypeTable";
  public static final String CARRIER_INSTANCE_TAG = "CarrierInstanceTable";
  
  //Variables:
  ////////////

  boolean firstleg=false;
  
  boolean rollup=false;

  boolean cargoType=false;
  boolean cargoInstance=false;
  boolean cargoLeg=false;

  boolean carrierType=false;
  boolean carrierInstance=false;

  //Constructors:
  ///////////////

  public DerivedTablesConfig(){
  }

  //Members:
  //////////

  public void setDoAll(){
    firstleg=true;
    rollup=true;
    cargoType=true;
    cargoInstance=true;
    cargoLeg=true;
    carrierType=true;
    carrierInstance=true;
  }

  public boolean getDoFirstLeg(){return firstleg;}

  public boolean getDoRollup(){return rollup;}

  public boolean getDoCargoType(){return cargoType;}
  public boolean getDoCargoInstance(){return cargoInstance;}
  public boolean getDoCargoLeg(){return cargoLeg;}

  public boolean getDoCarrierType(){return carrierType;}
  public boolean getDoCarrierInstance(){return carrierInstance;}

  public boolean getDoTable(String tableName){
    if(tableName.equals(PrepareDerivedTables.FIRST_LEG)){
      return getDoFirstLeg();
    }else if(tableName.equals(PrepareDerivedTables.ROLLUP)) {
      return getDoRollup();
    } else if(tableName.equals(PrepareDerivedTables.CARGO_TYPE)) {
      return getDoCargoType();
    } else if(tableName.equals(PrepareDerivedTables.CARGO_INSTANCE)) {
      return getDoCargoInstance();
    } else if(tableName.equals(PrepareDerivedTables.CARGO_LEG)){ 
      return getDoCargoLeg();
    } else if(tableName.equals(PrepareDerivedTables.CARRIER_TYPE)){
      return getDoCarrierType();
    } else if(tableName.equals(PrepareDerivedTables.CARRIER_INSTANCE)){
      return getDoCarrierInstance();
    } else {
      return false;
    }
  }

  //XMLable:

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    w.tagln(FIRST_LEG_TAG,firstleg?"true":"false");
    w.tagln(ROLLUP_TAG,rollup?"true":"false");
    w.tagln(CARGO_TYPE_TAG,cargoType?"true":"false");
    w.tagln(CARGO_INSTANCE_TAG,cargoInstance?"true":"false");
    w.tagln(CARGO_LEG_TAG,cargoLeg?"true":"false");
    w.tagln(CARRIER_TYPE_TAG,carrierType?"true":"false");
    w.tagln(CARRIER_INSTANCE_TAG,carrierInstance?"true":"false");
    w.cltagln(NAME_TAG);
  }

  //DeXMLable:

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
      }else if(name.equals(FIRST_LEG_TAG)){
	firstleg=(data!=null&&data.equals("true"));
      }else if(name.equals(ROLLUP_TAG)){
	rollup=(data!=null&&data.equals("true"));
      }else if(name.equals(CARGO_TYPE_TAG)){
	cargoType=(data!=null&&data.equals("true"));
      }else if(name.equals(CARGO_INSTANCE_TAG)){
	cargoInstance=(data!=null&&data.equals("true"));
      }else if(name.equals(CARGO_LEG_TAG)){
	cargoLeg=(data!=null&&data.equals("true"));
      }else if(name.equals(CARRIER_TYPE_TAG)){
	carrierType=(data!=null&&data.equals("true"));
      }else if(name.equals(CARRIER_INSTANCE_TAG)){
	carrierInstance=(data!=null&&data.equals("true"));
      }else
	throw new UnexpectedXMLException("Unexpected open tag:"+name);
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Cannot parse as number("+data+"):"+e);
    }
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being deXMLized
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
    throw new UnexpectedXMLException("Unexpected subobject:"+obj);
  }

  //InnerClasses:
  ///////////////
}
