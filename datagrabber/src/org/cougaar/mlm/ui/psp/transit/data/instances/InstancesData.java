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

import org.cougaar.core.util.UID;

import org.cougaar.mlm.ui.psp.transit.data.prototypes.Prototype;
import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Externalizable;
import java.io.Serializable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.nio.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Instances PSP.  This can either be
 * Instance objects, or 'sub' prototypes (that is prototypes derived from
 * a prototype returned by the Prototypes PSP, but containing some altered
 * values (usually a container whose weight has changed etc).  Make sure
 * such prototypes have their parentUID field filled in.
 *
 * @since 1/28/01
 **/
public class InstancesData implements XMLable, DeXMLable, /* Serializable */ Externalizable {

  //Constants:

  //Tags:
  public static final String NAME_TAG = "Instances";
  //Attr:

  //Variables:
  ////////////

  protected Map instances;

  //Constructors:
  ///////////////

  public InstancesData(){
    instances = new HashMap(89);
  }

  //Members:
  //////////

  public int numInstances(){
    return instances.size();
  }

  public Instance getInstance(UID uid){
    return (Instance)instances.get(uid);
  }

  public Iterator getInstancesIterator(){
    return instances.values().iterator();
  }

  public void addInstance(Instance i){
    instances.put(i.UID, i);
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);

    Iterator iter = getInstancesIterator();
    while(iter.hasNext()){
      Instance inst = (Instance)iter.next();
      inst.toXML(w);
    }

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
    if(obj instanceof Instance){
      addInstance((Instance)obj);
    }else{
      throw new UnexpectedXMLException("Unknown object:" + name + ":"+obj);
    }
  }
  //Inner Classes:

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
    try {
    int numInstances = instances.values().size();
    out.writeInt (numInstances);
    
    //    System.out.println ("Writing " + numInstances + " instances.");

    char [] instanceStringBuffer = new char [numInstances * Instance.numString * Instance.maxStringLength];
    long [] instanceLongBuffer   = new long [numInstances * Instance.numLong];
    int  [] instanceIntBuffer    = new int  [numInstances * Instance.numInt];
    boolean [] instanceBooleanBuffer = new boolean [numInstances * Instance.numBoolean];

    Iterator iter = getInstancesIterator();
    int index = 0;
    int totalManifestItems = 0;

    List agentNames   = new ArrayList ();
    int [] uidAgentIndex = new int  [numInstances*Instance.numUID];
    long [] uidLong      = new long [numInstances*Instance.numUID];

    while(iter.hasNext()){
      Instance instance = (Instance)iter.next();
      int numInManifest = instance.writeToBuffer (index++, 
						   agentNames,
						   uidAgentIndex,
						   uidLong,
						   instanceStringBuffer, 
						   instanceLongBuffer, 
						   instanceIntBuffer, 
						   instanceBooleanBuffer);
      totalManifestItems += numInManifest;

      if (instance.hasManifest && numInManifest == 0)
	  System.err.println ("Huh? Instance " + instance.UID + 
			      " says it has a manifest, but 0 items in the list.");
    }

    char [] nomenStringBuffer  = new char [totalManifestItems * Instance.maxStringLength];
    char [] typeIDStringBuffer = new char [totalManifestItems * Instance.maxStringLength];
    double [] weightDoubleBuffer = new double [totalManifestItems];
    char [] receiversStringBuffer = new char [totalManifestItems * Instance.maxStringLength];

    index = 0;
    iter = getInstancesIterator();
    int i = 0;
    while(iter.hasNext()){
      Instance instance = (Instance)iter.next();

      //      System.out.println ("total manifest items " + totalManifestItems);

      if (instance.hasManifest)
	index = instance.writeToManifestBuffers (index, 
						 nomenStringBuffer, 
						 typeIDStringBuffer, 
						 receiversStringBuffer,
						 weightDoubleBuffer);
      //      System.out.println ("Intance #" + i++ + " is\n" + instance);
    }

    //    System.out.println ("AgentNames is " + agentNames);
    //    System.out.println ("instance string is " + new String (instanceStringBuffer));
    //    System.out.println ("nomen string is " + new String (nomenStringBuffer));

    out.writeObject(agentNames);
    out.writeObject(uidAgentIndex);
    out.writeObject(uidLong);
    out.writeObject(instanceStringBuffer);
    out.writeObject(instanceLongBuffer);
    out.writeObject(instanceIntBuffer);
    out.writeObject(instanceBooleanBuffer);
    out.writeObject(nomenStringBuffer);
    out.writeObject(typeIDStringBuffer);
    out.writeObject(receiversStringBuffer);
    out.writeObject(weightDoubleBuffer);
    } catch (Exception e) { e.printStackTrace (); }
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
    int numInstances = in.readInt ();

    List agentNames   = (List) in.readObject ();
    int  [] uidAgentIndex = (int  []) in.readObject ();
    long [] uidLong       = (long []) in.readObject ();

    char [] instanceStringBuffer   = (char [])    in.readObject();
    long [] instanceLongBuffer     = (long [])    in.readObject();
    int  [] instanceIntBuffer      = (int  [])    in.readObject();
    boolean [] instanceBooleanBuffer  = (boolean []) in.readObject();

    char   [] nomenStringBuffer  = (char [])    in.readObject();
    char   [] typeIDStringBuffer = (char [])    in.readObject();
    char   [] receiverStringBuffer = (char [])  in.readObject();

    double [] weightDoubleBuffer = (double [])  in.readObject();

    CharBuffer charBuffer = CharBuffer.allocate (instanceStringBuffer.length);
    String temp = new String(instanceStringBuffer);
    charBuffer.put (temp);
    charBuffer.rewind();

    CharBuffer nomenBuffer = CharBuffer.allocate (nomenStringBuffer.length);
    temp = new String(nomenStringBuffer);
    nomenBuffer.put (temp);
    nomenBuffer.rewind();

    CharBuffer typeIDBuffer = CharBuffer.allocate (typeIDStringBuffer.length);
    temp = new String(typeIDStringBuffer);
    typeIDBuffer.put (temp);
    typeIDBuffer.rewind();

    CharBuffer receiverBuffer = CharBuffer.allocate (receiverStringBuffer.length);
    temp = new String(receiverStringBuffer);
    receiverBuffer.put (temp);
    receiverBuffer.rewind();

    int manifestsSoFar = 0;
    for (int i = 0; i < numInstances; i++) {
      Instance instance = new Instance ();
      manifestsSoFar = instance.readFromBuffer (i, 
						manifestsSoFar,
						agentNames,
						uidAgentIndex,
						uidLong,
						charBuffer,
						instanceLongBuffer,
						instanceIntBuffer,
						instanceBooleanBuffer,
						nomenBuffer,
						typeIDBuffer,receiverBuffer,
						weightDoubleBuffer);
      addInstance (instance);
      //      System.out.println ("Intance #" + i + " is\n" + instance);
    }
  }

  private static final long serialVersionUID = 998172342837427164L;
}
