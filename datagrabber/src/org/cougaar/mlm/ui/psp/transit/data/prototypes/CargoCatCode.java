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
 *
 * @author Gordon Vidaver; last modified by: $Author: mthome $
 *
 * @since 06/24/02
 **/
public class CargoCatCode extends Prototype {

  //Constants:
  ////////////

  //Tags:
  public static final String NAME_TAG = "CargoCatCode";

  protected static final String CCC_DIM_ATTR = "CCCDim";

  protected static final String WEIGHT_ATTR = "Weight";
  protected static final String WIDTH_ATTR = "Width";
  protected static final String HEIGHT_ATTR = "Height";
  protected static final String DEPTH_ATTR = "Depth";
  protected static final String AREA_ATTR = "Area";
  protected static final String VOLUME_ATTR = "Volume";
  //Attr:

  //Variables:
  ////////////

  public String cargoCatCode;

  /**weight in grams**/
  public double weight;
  /**width in meters**/
  public double width;
  /**height in meters**/
  public double height;
  /**depth in meters**/
  public double depth;
  /**area in square meters**/
  public double area;
  /**volume in cubic meters**/
  public double volume;

  //Constructors:
  ///////////////

  public CargoCatCode(){}

  //Members:
  //////////

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG, 
	      UID_ATTR,UID, // prototype id
	      WEIGHT_ATTR,Double.toString(weight),
	      WIDTH_ATTR,Double.toString(width),
	      HEIGHT_ATTR,Double.toString(height),
	      DEPTH_ATTR,Double.toString(depth),
	      AREA_ATTR,Double.toString(area),
	      VOLUME_ATTR,Double.toString(volume),
	      CCC_DIM_ATTR,cargoCatCode);
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
	weight=Double.parseDouble(attr.getValue(WEIGHT_ATTR));
	width=Double.parseDouble(attr.getValue(WIDTH_ATTR));
	height=Double.parseDouble(attr.getValue(HEIGHT_ATTR));
	depth=Double.parseDouble(attr.getValue(DEPTH_ATTR));
	area=Double.parseDouble(attr.getValue(AREA_ATTR));
	volume=Double.parseDouble(attr.getValue(VOLUME_ATTR));
	cargoCatCode=attr.getValue(CCC_DIM_ATTR);
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

  private static final long serialVersionUID = -1420894105364512268L;
}
