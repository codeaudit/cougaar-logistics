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
package org.cougaar.mlm.ui.psp.transit.data.legs;

import org.cougaar.planning.servlet.data.xml.*;

import org.cougaar.core.util.UID;

import java.io.IOException;
import java.io.Writer;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.nio.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Legs PSP
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 1/28/01
 **/
public class LegsData implements XMLable, DeXMLable, Externalizable{

  //Constants:

  //Tags:
  public static final String NAME_TAG = "Legs";
  //Attr:

  //Variables:
  ////////////

  protected Map legs;

  //Constructors:
  ///////////////

  public LegsData(){
    legs = new HashMap(89);
  }

  //Members:
  //////////

  public int numLegs(){
    return legs.size();
  }

  public Leg getLeg(UID uid){
    return (Leg)legs.get(uid);
  }

  public void addLeg(Leg l){
    legs.put(l.UID, l);
  }

  public Iterator getLegsIterator(){
    return legs.values().iterator();
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    //    System.out.println ("Writing Legs Data to xml.");
    w.optagln(NAME_TAG);

    Iterator iter = getLegsIterator();
    while(iter.hasNext()){
      Leg li = (Leg)iter.next();
      li.toXML(w);
    }

    w.cltagln(NAME_TAG);
    //    System.out.println ("Finished Writing Legs Data to xml.");
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

    if(name.equals(NAME_TAG)){
    }else{
      throw new UnexpectedXMLException("Unexpected tag: "+name);    
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
    if(obj instanceof Leg){
      addLeg((Leg)obj);
    }else{
      throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
    }
  }

  /**
   * Mandatory writeExternal method. <p>
   *
   * Does all the writing manually -- creates type specific buffers to store
   * Leg fields in, and writes them once at the end of the method.  Massive
   * speed up compared to hitting the socket repeatedly. <p>
   *
   * Note also that it's speeded up by having all strings be 40 
   * characters long.  This may come back to bite us later, if any are proved to be longer. <p>
   *
   * That is, I don't have to parse every character, I can just jump forward in the string
   * buffer 40 characters at a time, slurp them up into a string, and continue.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    int numLegs = legs.values().size();
    out.writeInt (numLegs);
    char [] legStringBuffer = new char [numLegs * Leg.numStringFields * Leg.maxStringLength];
    long [] legLongBuffer   = new long [numLegs * Leg.numLongFields];
    int  [] legIntBuffer    = new int  [numLegs * Leg.numIntFields];
    boolean [] legBooleanBuffer = new boolean [numLegs * Leg.numBooleanFields];

    Iterator iter = getLegsIterator();
    int index = 0;
    int totalAssets = 0;

    while(iter.hasNext()){
      Leg li = (Leg)iter.next();
      totalAssets += li.writeToBuffer (index++, 
				       legStringBuffer, 
				       legLongBuffer, 
				       legIntBuffer, 
				       legBooleanBuffer);
    }

    char [] assetStringBuffer = new char [totalAssets * Leg.maxStringLength];
    index = 0;
    iter = getLegsIterator();
    while(iter.hasNext()){
      Leg li = (Leg)iter.next();
      index = li.writeToAssetBuffer (index, assetStringBuffer);
    }

    out.writeObject(legStringBuffer);
    out.writeObject(legLongBuffer);
    out.writeObject(legIntBuffer);
    out.writeObject(legBooleanBuffer);
    out.writeObject(assetStringBuffer);

    //    System.out.println ("legString " + new String(legStringBuffer));
    //    System.out.println ("legLong   " + legLongBuffer);
    //    System.out.println ("legInt    " + legIntBuffer);
    //    System.out.println ("legBoolean  " + legBooleanBuffer);
    //    System.out.println ("assetString " + new String(assetStringBuffer));
  }

    /**
     * Mandatory readExternal method. Will read in the data that we wrote out
     * in the writeExternal method. MUST BE IN THE SAME ORDER and type as we
     * wrote it out. By the time, readExternal is called, an object of this 
     * class has already been created using the public no-arg constructor,
     * so this method is used to restore the data to all of the fields of the 
     * newly created object.
     *
     * Reads from type-specific buffers to get field info.  Calls leg's readFromBuffer
     * to take the fields it needs from the buffers.
     */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int numLegs = in.readInt ();

    char [] legStringBuffer   = (char [])    in.readObject();
    long [] legLongBuffer     = (long [])    in.readObject();
    int  [] legIntBuffer      = (int  [])    in.readObject();
    boolean [] legBooleanBuffer  = (boolean []) in.readObject();
    char [] assetStringBuffer = (char [])    in.readObject();

    CharBuffer charBuffer = CharBuffer.allocate (legStringBuffer.length);
    String temp = new String(legStringBuffer);
    charBuffer.put (temp);
    charBuffer.rewind();
    //    System.out.println ("orig size " + legStringBuffer.length + " temp " + temp + " char Buffer is " + charBuffer);

    CharBuffer assetCharBuffer = CharBuffer.allocate (assetStringBuffer.length);
    temp = new String (assetStringBuffer);
    assetCharBuffer.put (temp);
    assetCharBuffer.rewind();
    //    System.out.println ("orig size " + assetStringBuffer.length + " temp " + temp + 
    //			" asset char Buffer is " + assetCharBuffer);

    // might want to switch to using LongBuffers for readability
    //    LongBuffer longBuffer = LongBuffer.allocate (legLongBuffer.length);
    //    longBuffer.put (legLongBuffer);

    for (int i = 0; i < numLegs; i++) {
      Leg li = new Leg ();
      li.readFromBuffer (i, 
			 charBuffer,
			 legLongBuffer,
			 legIntBuffer,
			 legBooleanBuffer,
			 assetCharBuffer);
      addLeg (li);
      //      System.out.println ("Leg #" + i + " is\n" + li);
    }
  }

  //Inner Classes:

  private static final long serialVersionUID = 175028348348559839L;
}
