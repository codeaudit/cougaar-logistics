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
package org.cougaar.mlm.ui.psp.transit.data.legs;

import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.core.util.UID;

import java.io.Writer;
import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
//import java.io.Serializable;
import java.nio.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.xml.sax.Attributes;

/**
 * A single instance leaving the Legs PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class Leg implements XMLable, DeXMLable /*, Externalizable - Serializable*/{

  //Constants:
  ////////////

  /**Unknown type... try not to use**/
  public static final int LEG_TYPE_UNKNOWN=0;
  /**Guaranteed should not overlap for a given asset or conveyance**/
  public static final int LEG_TYPE_TRANSPORTING=1;
  /**Loading cargo (cargo does not show up on this leg)**/
  public static final int LEG_TYPE_LOADING=2;
  /**Unloading cargo (cargo does not show up on this leg)**/
  public static final int LEG_TYPE_UNLOADING=3;
  /**Positioning to pick up cargeo (no cargo on this leg)**/
  public static final int LEG_TYPE_POSITIONING=4;
  /**Returning from dropping off cargeo (no cargo on this leg)**/
  public static final int LEG_TYPE_RETURNING=5;
  /**Refueling**/
  public static final int LEG_TYPE_REFUELING=6;

  //Tags:
  public static final String NAME_TAG = "Leg";
  protected static final String ASSET_TAG = "CarriedAsset";
  //Attr:

  protected static final String UID_ATTR = "UID";
  protected static final String START_TIME_ATTR = "STime";
  protected static final String END_TIME_ATTR = "ETime";
  protected static final String READYAT_ATTR = "ReadyTime";
  protected static final String EARLIEST_END_ATTR = "EariestEndTime";
  protected static final String BEST_END_ATTR = "BestEndTime";
  protected static final String LATEST_END_ATTR = "LatestEndTime";
  protected static final String START_LOC_ATTR = "SLoc";
  protected static final String END_LOC_ATTR = "ELoc";
  protected static final String LEG_TYPE_ATTR = "Type";
  protected static final String CONV_ID_ATTR = "ConvID";
  protected static final String ROUTE_ID_ATTR = "RouteID";
  protected static final String IS_DETAIL_ATTR= "IsDetail";

  protected static final boolean writePreferences = false;

  //Variables:
  ////////////

  public UID UID;
  public long startTime;
  public long endTime;
  public long readyAtTime;     // not sent
  public long earliestEndTime; // not sent
  public long bestEndTime;     // not sent
  public long latestEndTime;   // not sent
  public String startLoc;
  public String endLoc;
  /**Use LEG_TYPE_* constants**/
  public int legType;
  public UID conveyanceUID;
  public UID routeUID;

  /**this leg provides detail (that is additional information, 
   * ie what deck) for another existing leg that exactly mirrors it in time
   **/
  public boolean isDetail;

  protected List assetsOnLeg;

  public static int numStringFields = 5; 
  public static int numLongFields  = 2; 
  public static int numIntFields  = 2; 
  public static int numBooleanFields = 1;
  public static int maxStringLength = 80;

  //Constructors:
  ///////////////

  public static int count = 0;

  public Leg(){
    //    System.out.println ("Creating leg # " + count++);
    assetsOnLeg=new ArrayList();
  }

  //Members:
  //////////

  public boolean assetsCarried(){
    return assetsOnLeg==null;
  }

  public int numCarriedAssets(){
    if(assetsOnLeg==null)
      return 0;
    return assetsOnLeg.size();
  }

  public Iterator getCarriedAssetsIterator(){
    if(assetsOnLeg==null) {
      return Collections.EMPTY_LIST.iterator();
    } else {
      return assetsOnLeg.iterator();
    }
  }

  /** Get the UID of a carried asset**/
  public UID getCarriedAssetAt(int i){
    return (UID)assetsOnLeg.get(i);
  }

  public void addCarriedAsset(UID uid){
    if(assetsOnLeg==null)
      assetsOnLeg = new ArrayList();
    assetsOnLeg.add(uid);
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    if (writePreferences) {
      w.optagln(NAME_TAG,
		UID_ATTR, UID.toString(),
		START_TIME_ATTR, Long.toString(startTime),
		END_TIME_ATTR, Long.toString(endTime),
		READYAT_ATTR, Long.toString(readyAtTime),
		EARLIEST_END_ATTR, Long.toString(earliestEndTime),
		BEST_END_ATTR, Long.toString(bestEndTime),
		LATEST_END_ATTR,Long.toString(latestEndTime),
		START_LOC_ATTR, startLoc,
		END_LOC_ATTR, endLoc,
		LEG_TYPE_ATTR, Integer.toString(legType),
		CONV_ID_ATTR, conveyanceUID.toString(),
		ROUTE_ID_ATTR, routeUID.toString(),
		IS_DETAIL_ATTR, isDetail?"T":"F");
    }
    else {
      w.optagln(NAME_TAG,
		UID_ATTR, UID.toString(),
		START_TIME_ATTR, Long.toString(startTime),
		END_TIME_ATTR, Long.toString(endTime),
		START_LOC_ATTR, startLoc,
		END_LOC_ATTR, endLoc,
		LEG_TYPE_ATTR, Integer.toString(legType),
		CONV_ID_ATTR, conveyanceUID.toString(),
		ROUTE_ID_ATTR, routeUID.toString(),
		IS_DETAIL_ATTR, isDetail?"T":"F");
    }

    for(int i=0;i<numCarriedAssets();i++)
      w.tagln(ASSET_TAG,getCarriedAssetAt(i).toString());

    w.cltagln(NAME_TAG);
  }

  /**
   * Mandatory writeExternal method. 
   * @serialData 
   *             
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(UID);
    out.writeLong(startTime);
    out.writeLong(endTime);

    if (writePreferences) {
      out.writeLong(readyAtTime);
      out.writeLong(earliestEndTime);
      out.writeLong(bestEndTime);
      out.writeLong(latestEndTime);
    }

    out.writeObject(startLoc.intern());
    out.writeObject(endLoc.intern());
    out.writeInt(legType);
    out.writeObject(conveyanceUID);
    out.writeObject(routeUID);
    out.writeBoolean(isDetail);
    out.writeInt (assetsOnLeg.size());

    for (Iterator iter = assetsOnLeg.iterator(); iter.hasNext(); ) {
      out.writeObject(iter.next());
    }
  }

  /**
   * Mandatory writeExternal method. 
   * @serialData 
   *             
   */
  public int writeToBuffer(int index, 
			   char [] legStringBuffer,
			   long [] legLongBuffer,
			   int [] legIntBuffer,
			   boolean [] legBooleanBuffer
			   ) throws IOException {
    String uidString = UID.toString();
    System.arraycopy (uidString.toCharArray(), 0, 
		      legStringBuffer, index*numStringFields*maxStringLength, 
		      uidString.length()); // should do min of maxStringLength

    legLongBuffer[index*numLongFields] = startTime;
    legLongBuffer[index*numLongFields+1] = endTime;

    System.arraycopy (startLoc.toCharArray(), 0, 
		      legStringBuffer, (index*numStringFields + 1)*maxStringLength, 
		      startLoc.length());

    System.arraycopy (endLoc.toCharArray(), 0, 
		      legStringBuffer, (index*numStringFields + 2)*maxStringLength, 
		      endLoc.length());

    legIntBuffer[index*numIntFields] = legType;

    //    System.out.println ("leg type is " + legType);

    String conveyanceUIDString = conveyanceUID.toString();
    System.arraycopy (conveyanceUIDString.toCharArray(), 0, 
		      legStringBuffer, (index*numStringFields + 3)*maxStringLength, 
		      conveyanceUIDString.length());

    String routeUIDString = (routeUID != null) ? routeUID.toString() : "null";
    System.arraycopy (routeUIDString.toCharArray(), 0, 
		      legStringBuffer, (index*numStringFields + 4)*maxStringLength, 
		      routeUIDString.length());

    legBooleanBuffer[index*numBooleanFields] = isDetail;
    legIntBuffer[(index*numIntFields)+1] = assetsOnLeg.size();

    return assetsOnLeg.size();
  }

  public int writeToAssetBuffer(int index, 
				char [] assetStringBuffer) {
      for (Iterator iter = assetsOnLeg.iterator(); iter.hasNext(); ) {
	UID assetUID = (UID) iter.next();
	String assetUIDString = assetUID.toString();
	//      System.out.println ("asset uid "+ assetUIDString + " at " + (index + i)*maxStringLength);
	int assetUIDLength = assetUIDString.length();
	int arrayLocation = (index++)*maxStringLength; 
	if (assetUIDLength > maxStringLength) {
	  System.err.println ("Leg.writeToAssetBuffer - asset UID (" + assetUIDString + 
			      ") longer than max (" + maxStringLength + "), so truncating it.");
	  assetUIDLength = maxStringLength;
	}

	try {
	  System.arraycopy (assetUIDString.toCharArray(),  // from this array
			    0,                             // starting at 0 in the array
			    assetStringBuffer,             // to this buffer
			    arrayLocation,                 // starting at this point in the buffer
			    assetUIDLength);               // copy this many characters
	} catch (Exception e) {
	  // this should not happen
	  System.err.println ("Got exception " + e + "\n" + 
			      "writing asset uid " + assetUIDString + 
			      " to asset string buffer - it's probably longer than max length of " +
			      maxStringLength);
	}
	//      System.out.println ("asset buf after "+ new String (assetStringBuffer));
      }

    return index;
  }

  public void readFromBuffer(
			     int index, 
			     CharBuffer charBuffer, 
			     long [] legLongBuffer,
			     int [] legIntBuffer,
			     boolean [] legBooleanBuffer,
			     CharBuffer assetCharBuffer) {
    char temp [] = new char [maxStringLength];
    charBuffer.get(temp);
    UID = UID.toUID(new String(temp).trim());

    startTime = legLongBuffer[(index*numLongFields)];
    endTime   = legLongBuffer[(index*numLongFields)+1];

    charBuffer.get(temp);
    startLoc = new String (temp).trim();
    charBuffer.get(temp);
    endLoc   = new String (temp).trim();

    legType = legIntBuffer[(index*numIntFields)];
    charBuffer.get(temp);
    conveyanceUID = UID.toUID(new String(temp).trim());
    charBuffer.get(temp);
    String routeUIDString = new String(temp).trim();
    if (!routeUIDString.equals("null"))
      routeUID = UID.toUID(routeUIDString);
    isDetail = legBooleanBuffer[(index*numBooleanFields)];

    int numToRead = legIntBuffer[(index*numIntFields)+1];
    //    System.out.println ("Reading " + numToRead + " assets.");
    for (int i = 0; i < numToRead; i++) {
      assetCharBuffer.get(temp);
      addCarriedAsset (UID.toUID(new String(temp).trim()));
    }
  }

    /**
     * Mandatory readExternal method. Will read in the data that we wrote out
     * in the writeExternal method. MUST BE IN THE SAME ORDER and type as we
     * wrote it out. By the time, readExternal is called, an object of this 
     * class has already been created using the public no-arg constructor,
     * so this method is used to restore the data to all of the fields of the 
     * newly created object.
     */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    UID = (UID)in.readObject();
    startTime = in.readLong();
    endTime = in.readLong();

    if (writePreferences) {
      readyAtTime = in.readLong();
      earliestEndTime = in.readLong();
      bestEndTime = in.readLong();
      latestEndTime = in.readLong();
    }

    startLoc = (String) in.readObject();
    endLoc = (String) in.readObject();
    legType = in.readInt();
    conveyanceUID = (UID) in.readObject();
    routeUID = (UID) in.readObject();
    isDetail = in.readBoolean();

    int numToRead = in.readInt();

    for (int i = 0; i < numToRead; i++) {
      addCarriedAsset ((UID) in.readObject());
    }
  }

  public String toString () {
    StringBuffer buffer = new StringBuffer();
    
    buffer.append ("UID   " + UID);buffer.append("\n");
    buffer.append ("start " + new java.util.Date(startTime));buffer.append("\n");
    buffer.append ("end   " + new java.util.Date(endTime));buffer.append("\n");

    buffer.append ("startLoc      " + startLoc);buffer.append("\n");
    buffer.append ("endLoc        " + endLoc);buffer.append("\n");
    buffer.append ("legType       " + legType);buffer.append("\n");
    buffer.append ("conveyanceUID " + conveyanceUID);buffer.append("\n");
    buffer.append ("routeUID      " + routeUID);buffer.append("\n");
    buffer.append ("isDetail      " + isDetail);buffer.append("\n");
    buffer.append ("assets        " + assetsOnLeg);buffer.append("\n");
    
    return buffer.toString();
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
	startTime=Long.parseLong(attr.getValue(START_TIME_ATTR));
	endTime=Long.parseLong(attr.getValue(END_TIME_ATTR));

	if (writePreferences) {
	  readyAtTime=Long.parseLong(attr.getValue(READYAT_ATTR));
	  earliestEndTime=Long.parseLong(attr.getValue(EARLIEST_END_ATTR));
	  bestEndTime=Long.parseLong(attr.getValue(BEST_END_ATTR));
	  latestEndTime=Long.parseLong(attr.getValue(LATEST_END_ATTR));
	}

	startLoc=attr.getValue(START_LOC_ATTR);
	endLoc=attr.getValue(END_LOC_ATTR);
	legType=Integer.parseInt(attr.getValue(LEG_TYPE_ATTR));
	conveyanceUID=UID.toUID(attr.getValue(CONV_ID_ATTR));
	routeUID=UID.toUID(attr.getValue(ROUTE_ID_ATTR));
	isDetail=attr.getValue(IS_DETAIL_ATTR).equals("Y");
      }else if(name.equals(ASSET_TAG)){
	addCarriedAsset(UID.toUID(data));
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

  private static final long serialVersionUID = 289304982377717777L;
}
