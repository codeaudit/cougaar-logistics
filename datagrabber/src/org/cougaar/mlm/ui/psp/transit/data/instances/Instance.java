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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Writer;

import java.nio.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.planning.ldm.measure.Mass;

import org.xml.sax.Attributes;

/**
 * A single instance leaving the Instance PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 1/28/01
 **/
public class Instance implements XMLable, DeXMLable, Externalizable {

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
  public String ownerID; // counts as UID
  /**if a Container, MilVan, or Pallet**/
  public UID manifestUID; // not written/read
  public boolean hasManifest;
  public List nomenclatures;
  public List typeIdentifications;
  public List weights;
  public String name;

  public static int numUID = 2;
  public static int numString = 3;
  public static int numLong = 1;
  public static int numBoolean = 1;
  public static int numInt = 1; // all manifest lists are the same length
  public static int maxStringLength = 40;

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

  /**
   * Mandatory writeExternal method. 
   * @serialData 
   *             
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(UID);
    out.writeObject((itemNomen != null) ? itemNomen.intern() : "no_nomenclature");
    out.writeLong(aggregateNumber);

    out.writeObject(prototypeUID.intern());
    out.writeObject(ownerID.intern());

    //  out.writeObject(manifestUID);
    out.writeBoolean(hasManifest);

    out.writeInt ((nomenclatures != null) ? nomenclatures.size() : 0);
    if (nomenclatures != null) {
      for (Iterator iter = nomenclatures.iterator(); iter.hasNext(); ) {
	out.writeObject(iter.next());
      }
    }

    out.writeInt ((typeIdentifications != null) ? typeIdentifications.size() : 0);
    if (typeIdentifications != null) {
      for (Iterator iter = typeIdentifications.iterator(); iter.hasNext(); ) {
	out.writeObject(iter.next());
      }
    }

    out.writeInt ((weights != null) ? weights.size() : 0);
    if (weights != null) {
      for (Iterator iter = weights.iterator(); iter.hasNext(); ) {
	out.writeObject(iter.next());
      }
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
    itemNomen = ((String) in.readObject()).intern();
    aggregateNumber = in.readLong();

    prototypeUID = ((String) in.readObject()).intern();
    ownerID      = ((String) in.readObject()).intern();

    //    manifestUID = ((String) in.readObject()).intern();

    int numToRead = in.readInt();
    if (numToRead > 0) nomenclatures = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++)
      nomenclatures.add (((String) in.readObject()).intern());

    numToRead = in.readInt();
    if (numToRead > 0) typeIdentifications = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++)
      typeIdentifications.add (((String) in.readObject()).intern());

    numToRead = in.readInt();
    if (numToRead > 0) weights = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++)
      weights.add (in.readObject()); // should be instances of Mass
  }

  /**
   * Mandatory writeExternal method. 
   * @serialData 
   *             
   */
  public int writeToBuffer(int index, 
			   List agentNames,
			   int [] uidAgentIndex,
			   long [] uidLong,
			   char [] instanceStringBuffer,
			   long [] instanceLongBuffer,
			   int [] instanceIntBuffer,
			   boolean [] instanceBooleanBuffer
			   ) throws IOException {
    // UIDs
    int agentIndex;
    if ((agentIndex = agentNames.indexOf(UID.getOwner().intern())) == -1) {
      agentNames.add (UID.getOwner().intern());
      agentIndex = agentNames.indexOf(UID.getOwner());
    }

    uidAgentIndex[index*numUID] = agentIndex;
    uidLong[index*numUID] = UID.getId();

    if ((agentIndex = agentNames.indexOf(ownerID.intern())) == -1) {
      agentNames.add (ownerID.intern());
      agentIndex = agentNames.indexOf(ownerID);
    }

    uidAgentIndex[(index*numUID)+1] = agentIndex;
    uidLong[(index*numUID)+1] = 0;

    // Strings
    String nomen = (itemNomen != null) ? itemNomen : "no_nomen";
    System.arraycopy (nomen.toCharArray(), 0, 
		      instanceStringBuffer, (index*numString + 0)*maxStringLength, 
		      nomen.length());

    String proto = (prototypeUID != null) ? prototypeUID : "no_proto";

    System.arraycopy (proto.toCharArray(), 0, 
		      instanceStringBuffer, (index*numString + 1)*maxStringLength, 
		      proto.length());

    String name2 = (name != null) ? name : "no_name";

    System.arraycopy (name2.toCharArray(), 0, 
		      instanceStringBuffer, (index*numString + 2)*maxStringLength, 
		      name2.length());

    // longs

    instanceLongBuffer[index*numLong] = aggregateNumber;

    // booleans

    instanceBooleanBuffer[index*numBoolean] = hasManifest;

    // ints

    if (hasManifest) 
      instanceIntBuffer[index*numInt] = nomenclatures.size();

    return instanceIntBuffer[index*numInt];
  }

  public int writeToManifestBuffers(int index, 
				    char [] nomenStringBuffer,
				    char [] typeIDStringBuffer,
				    double [] weightDoubleBuffer) {
    Iterator iter2 = typeIdentifications.iterator();
    Iterator iter3 = weights.iterator();
    for (Iterator iter = nomenclatures.iterator(); iter.hasNext(); ) {
      String nomen  = (String) iter.next();
      String typeID = (String) iter2.next ();
      Mass weight   = (Mass)   iter3.next ();

      System.arraycopy (nomen.toCharArray(), 0, 
			nomenStringBuffer, (index)*maxStringLength, 
			nomen.length());

      System.arraycopy (typeID.toCharArray(), 0, 
			typeIDStringBuffer, (index)*maxStringLength, 
			typeID.length());

      weightDoubleBuffer[index++] = weight.getKilograms ();
    }
    return index;
  }

  public int readFromBuffer(
			     int index, 
			     int manifestsSoFar,
			     List agentNames,
			     int [] uidAgentIndex,
			     long [] uidLong,
			     CharBuffer charBuffer, 
			     long [] instanceLongBuffer,
			     int [] instanceIntBuffer,
			     boolean [] instanceBooleanBuffer,
			     CharBuffer nomenCharBuffer,
			     CharBuffer typeIDCharBuffer,
			     double [] weightDoubleBuffer) {
    // UIDs
    UID     = new UID((String) agentNames.get(uidAgentIndex[index*numUID]), 
		      uidLong[index*numUID]);
    ownerID = (String) agentNames.get(uidAgentIndex[(index*numUID)+1]);

    // Strings
    char temp [] = new char [maxStringLength];
    charBuffer.get(temp);
    itemNomen = new String(temp).trim();

    charBuffer.get(temp);
    prototypeUID = new String(temp).trim();

    charBuffer.get(temp);
    name = new String(temp).trim();

    // longs

    aggregateNumber = instanceLongBuffer[(index*numLong)];

    // booleans 

    hasManifest     = instanceBooleanBuffer[(index*numBoolean)];

    // ints

    int numNomens = instanceIntBuffer[index*numInt];

    if (hasManifest) {
      nomenclatures = new ArrayList ();
      typeIdentifications = new ArrayList ();
      weights       = new ArrayList ();

      for (int i = 0; i < numNomens; i++) {
	nomenCharBuffer.get(temp);
	nomenclatures.add (new String(temp).trim());

	typeIDCharBuffer.get(temp);
	typeIdentifications.add (new String(temp).trim());

	weights.add (new Mass (weightDoubleBuffer[manifestsSoFar + i], Mass.KILOGRAMS));
      }
    }

    return manifestsSoFar + numNomens;
  }

  //Inner Classes:

  private static final long serialVersionUID = 237849283718923764L;
}
