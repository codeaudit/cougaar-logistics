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
package org.cougaar.mlm.ui.psp.transit.data.prototypes;

import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import org.xml.sax.Attributes;

/**
 * A single prototype leaving the Prototypes PSP or the Instance PSP
 * Those leaving the Prototypes PSP are 'true' prototypes (info straight
 * from ALP Prototypes.  For these the parentUID field should be empty.
 * Those leaving the Instance PSP are 'sub' prototypes.  That is, they are
 * derivations from the 'true' prototypes with some of the fields changed
 * to fit a specific instance.  In this case, the parentUID field should
 * reflect the 'true' prototype from which the 'sub' prototype is derived.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 1/28/01
 **/
public class Prototype implements XMLable, DeXMLable, Serializable{

  //Constants:
  ////////////

  public static final int ASSET_CLASS_UNKNOWN = 0;
  public static final int ASSET_CLASS_1 = 1;
  public static final int ASSET_CLASS_2 = 2;
  public static final int ASSET_CLASS_3 = 3;
  public static final int ASSET_CLASS_4 = 4;
  public static final int ASSET_CLASS_5 = 5;
  public static final int ASSET_CLASS_6 = 6;
  public static final int ASSET_CLASS_7 = 7;
  public static final int ASSET_CLASS_8 = 8;
  public static final int ASSET_CLASS_9 = 9;
  public static final int ASSET_CLASS_10 = 10;
  public static final int ASSET_CLASS_CONTAINER = 11;
  public static final int ASSET_CLASS_PERSON = 12;

  /**All assets that are NOT MilVan, Container, Palet**/
  public static final int ASSET_TYPE_ASSET = 0;
  /**All assets that are MilVan, Container, Palet**/
  public static final int ASSET_TYPE_CONTAINER = 1;

  //Tags:
  public static final String NAME_TAG = "Prototype";
  //Attr:

  protected static final String UID_ATTR = "UID";
  protected static final String PUID_ATTR = "PUID";
  protected static final String ACLASS_ATTR = "AClass";
  protected static final String ATYPE_ATTR = "AType";
  protected static final String ALP_TYPEID_ATTR = "TypeID";
  protected static final String ALP_NOMEN_ATTR = "Nomen";
  protected static final String IS_LOW_FIDELITY = "LowFi";

  //Variables:
  ////////////

  public String UID;
  /**
   * This is the UID of the 'true' prototype that produced this modified
   * 'sub' prototype if applicable (see discussion above).
   **/
  public String parentUID;
  /**use ASSET_CLASS constants**/
  public int assetClass;
  /**use ASSET_TYPE constants**/
  public int assetType;

  /**type id, e.g. NSN**/
  public String alpTypeID;
  /**type nomenclature, not to be confused with the instance's *item* nomen**/
  public String nomenclature;
  /**indicates whether is a level-2 aggregate**/
  public boolean isLowFidelity;

  //Constructors:
  ///////////////

  public Prototype(){
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
    String isLow = (isLowFidelity ? "true" : "false");
    w.optagln(NAME_TAG, 
	      UID_ATTR,UID,
	      PUID_ATTR,parentUID,
	      ACLASS_ATTR,Integer.toString(assetClass),
	      ATYPE_ATTR,Integer.toString(assetType),
	      ALP_TYPEID_ATTR,alpTypeID,
	      ALP_NOMEN_ATTR,nomenclature,
	      IS_LOW_FIDELITY,isLow);
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
	UID=attr.getValue(UID_ATTR);
	parentUID=attr.getValue(PUID_ATTR);
	assetClass=Integer.parseInt(attr.getValue(ACLASS_ATTR));
	assetType=Integer.parseInt(attr.getValue(ATYPE_ATTR));
	alpTypeID=attr.getValue(ALP_TYPEID_ATTR);
	nomenclature=attr.getValue(ALP_NOMEN_ATTR);
	isLowFidelity=attr.getValue(IS_LOW_FIDELITY).equalsIgnoreCase("true");
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

  private static final long serialVersionUID = 542783926395274474L;
}
