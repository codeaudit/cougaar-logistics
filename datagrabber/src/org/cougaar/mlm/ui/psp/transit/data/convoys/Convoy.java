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
package org.cougaar.mlm.ui.psp.transit.data.convoys;

import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.core.util.UID;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.Attributes;

/**
 * A single instance leaving the Convoys PSP
 *
 * @since 1/28/01
 **/
public class Convoy implements XMLable, DeXMLable, Serializable{

  //Constants:
  ////////////

  //Tags:
  public static final String NAME_TAG = "Convoy";
  protected static final String CONVEYANCE_ID_TAG = "ConvID";
  //Attr:

  protected static final String UID_ATTR = "UID";
  protected static final String START_TIME_ATTR = "STime";
  protected static final String END_TIME_ATTR = "ETime";
  protected static final String PRETTY_NAME_ATTR= "PName";

  //Variables:
  ////////////

  String UID;
  long startTime;
  long endTime;
  String prettyName;
  List conveyances;
  
  //Constructors:
  ///////////////

  public Convoy(){
    conveyances=new ArrayList();
  }

  //Members:
  //////////

  public void setPrettyName(String pname){
    prettyName=pname;
  }

  public String getPrettyName(){
    return prettyName;
  }

  public void setUID(String UID){
    this.UID=UID;
  }

  public String getUID(){
    return UID;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public int numConveyances(){
    return conveyances.size();
  }

  public Iterator getConveyanceIDIterator(){
      return conveyances.iterator();
  }

  /** Get the UID of a conveyance that is a member of this convoy**/
  public UID getConveyanceIDAt(int i){
    return (UID)conveyances.get(i);
  }

  public void addConveyanceID(UID conveyanceID) {
    conveyances.add(conveyanceID);
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG, UID_ATTR, getUID(),
	      START_TIME_ATTR, Long.toString(getStartTime()),
	      END_TIME_ATTR, Long.toString(getEndTime()),
	      PRETTY_NAME_ATTR, getPrettyName());

    for(int i=0;i<numConveyances();i++)
      w.tagln(CONVEYANCE_ID_TAG,getConveyanceIDAt(i).toString());
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
	UID=attr.getValue(UID_ATTR);
	startTime=Long.parseLong(attr.getValue(START_TIME_ATTR));
	endTime=Long.parseLong(attr.getValue(END_TIME_ATTR));
	prettyName=attr.getValue(PRETTY_NAME_ATTR);
      }else if(name.equals(CONVEYANCE_ID_TAG)){
	UID uid = org.cougaar.core.util.UID.toUID(data);
        addConveyanceID(uid);
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

  private static final long serialVersionUID = 789234877979200120L;
}



