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
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 1/28/01
 **/
public class Instance implements XMLable, DeXMLable /*, Externalizable*/ {

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
  public List receivers;
  public String name;

  public static int numUID = 2;
  public static int numString = 3;
  public static int numLong = 1;
  public static int numBoolean = 1;
  public static int numInt = 1; // all manifest lists are the same length
  public static int maxStringLength = 60;

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
    out.writeObject ((name != null) ? name.intern() : "no_name");

    //  out.writeObject(manifestUID);
    out.writeBoolean(hasManifest);

    out.writeInt ((nomenclatures != null) ? nomenclatures.size() : 0);
    if (nomenclatures != null) {
      for (Iterator iter = nomenclatures.iterator(); iter.hasNext(); ) {
	Object obj = iter.next();
	out.writeObject(obj);
      }
    }

    if (typeIdentifications != null) {
      for (Iterator iter = typeIdentifications.iterator(); iter.hasNext(); ) {
	out.writeObject(iter.next());
      }
    }

    if (weights != null) {
      for (Iterator iter = weights.iterator(); iter.hasNext(); ) {
	out.writeObject(iter.next());
      }
    }

    out.writeInt ((receivers != null) ? receivers.size() : 0);
    if (receivers == null && nomenclatures != null)
      System.err.println ("Instance " + UID + " - inconsistent receivers was null, but nomen was not.");
    if (receivers != null) {
      for (Iterator iter = receivers.iterator(); iter.hasNext(); ) {
	Object obj = iter.next();
	out.writeObject(obj);
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
    name         = ((String) in.readObject()).intern();

    //    manifestUID = ((String) in.readObject()).intern();
    hasManifest = in.readBoolean();

    int numToRead = in.readInt();
    if (numToRead > 0) nomenclatures = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++) {
      String nomen = (String) in.readObject();
      if (nomen == null) {
	System.err.println ("Got a null nomenclature for Instance " + UID);
      }
      else
	nomenclatures.add (nomen.intern());
    }

    if (numToRead > 0) typeIdentifications = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++)
      typeIdentifications.add (((String) in.readObject()).intern());

    if (numToRead > 0) weights = new ArrayList(numToRead);
    for (int i = 0; i < numToRead; i++)
      weights.add (in.readObject()); // should be instances of Mass

    numToRead = in.readInt();
    if (numToRead > 0) {
      receivers = new ArrayList(numToRead);
    }
    for (int i = 0; i < numToRead; i++) {
      receivers.add (in.readObject());
    }
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
				    char [] receiverStringBuffer,
				    double [] weightDoubleBuffer) {
    Iterator iter2 = typeIdentifications.iterator();
    Iterator iter3 = weights.iterator();
    Iterator iter4 = receivers.iterator();

    for (Iterator iter = nomenclatures.iterator(); iter.hasNext(); ) {
      String nomen  = (String) iter.next();
      if (nomen == null) nomen = "no_nomen";
      int nomenLen = (nomen.length() > maxStringLength) ? maxStringLength : nomen.length();

      String typeID = (String) iter2.next ();
      if (typeID == null) nomen = "no_type";
      int typeLen = (typeID.length() > maxStringLength) ? maxStringLength : typeID.length();

      Mass weight   = (Mass)   iter3.next ();

      String receiver = (String) iter4.next ();
      if (receiver == null) nomen = "no_receiver";
      int receiverLen = (receiver.length() > maxStringLength) ? maxStringLength : receiver.length();

      System.arraycopy (nomen.toCharArray(), 0, 
			nomenStringBuffer, (index)*maxStringLength, 
			nomenLen);

      System.arraycopy (typeID.toCharArray(), 0, 
			typeIDStringBuffer, (index)*maxStringLength, 
			typeLen);

      System.arraycopy (receiver.toCharArray(), 0, 
			receiverStringBuffer, (index)*maxStringLength, 
			receiverLen);

      if (weight != null)
	  weightDoubleBuffer[index] = weight.getKilograms ();
      else
	  weightDoubleBuffer[index] = 0.0d;
      index++;
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
			     CharBuffer receiverCharBuffer,
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
      receivers     = new ArrayList ();
      weights       = new ArrayList ();

      for (int i = 0; i < numNomens; i++) {
	nomenCharBuffer.get(temp);
	nomenclatures.add (new String(temp).trim());

	typeIDCharBuffer.get(temp);
	typeIdentifications.add (new String(temp).trim());

	receiverCharBuffer.get(temp);
	receivers.add (new String(temp).trim());

	weights.add (new Mass (weightDoubleBuffer[manifestsSoFar + i], Mass.KILOGRAMS));
      }
    }

    return manifestsSoFar + numNomens;
  }

  public String toString () {
    StringBuffer buffer = new StringBuffer();
    
    buffer.append ("UID     " + UID);buffer.append("\n");
    buffer.append ("nomen   " + itemNomen);buffer.append("\n");
    buffer.append ("name    " + name);buffer.append("\n");
    buffer.append ("aggre # " + aggregateNumber);buffer.append("\n");
    buffer.append ("prototypeUID " + prototypeUID);buffer.append("\n");
    buffer.append ("ownerID      " + ownerID);buffer.append("\n");

    if (nomenclatures != null) {
	for (int i = 0; i < nomenclatures.size(); i++) {
	    buffer.append ("m #" + i + " nomen  " + nomenclatures.get(i));buffer.append("\n");
	    buffer.append ("m #" + i + " type   " + typeIdentifications.get(i));buffer.append("\n");
	    buffer.append ("m #" + i + " receiv " + receivers.get(i));buffer.append("\n");
	    buffer.append ("m #" + i + " weight " + weights.get(i));buffer.append("\n");
	}
    }

    return buffer.toString();
  }

  //Inner Classes:

  private static final long serialVersionUID = 237849283718923764L;
}
