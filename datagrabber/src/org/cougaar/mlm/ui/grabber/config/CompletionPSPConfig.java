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
package org.cougaar.mlm.ui.grabber.config;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import java.io.IOException;
import org.xml.sax.Attributes;
import java.util.*;

/**
 * Config file for the Completion PSP
 *
 * @since 3/4/01
 **/
public class CompletionPSPConfig extends PSPConfig{

  //Constants:
  ////////////

  public static final String NAME_TAG = "CompletionPSP";
  public static final String CLUSTER_TAG = "Cluster";

  //Variables:
  ////////////
  protected Set clusters;

  //Constructors:
  ///////////////

  public CompletionPSPConfig(){
    super();
    clusters=new HashSet(31);
  }

  public CompletionPSPConfig(CompletionPSPConfig config, String cluster) {
    urlConnection=new URLConnectionData(config.urlConnection, cluster);
  }

  //Members:
  //////////

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    urlConnection.toXML(w);
    w.cltagln(NAME_TAG);
  }

  public void addCluster(String clusterName){
    clusters.add(clusterName);
  }

  public Iterator getClusterIterator(){
    return clusters.iterator();
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
    if(name.equals(NAME_TAG)){
    }else if(name.equals(CLUSTER_TAG)){
      addCluster(data);
    }else{
      throw new UnexpectedXMLException("Unexpected tag: "+name);
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
    if(obj instanceof URLConnectionData){
      urlConnection=(URLConnectionData)obj;
      String cluster=urlConnection.getClusterName();
      if(cluster!=null&&!cluster.equals(""))
	addCluster(cluster);
    }else
      throw new UnexpectedXMLException("Unexpcted subobj:"+obj);
  }

  //InnerClasses:
  ///////////////
}
