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

import org.cougaar.mlm.ui.grabber.connect.HierarchyConnection;
import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;

import java.io.IOException;
import org.xml.sax.Attributes;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Wraps configuration for the Hierarchy PSP grabber
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/8/01
 **/
public class HierarchyPSPConfig extends PSPConfig{

  //Constants:
  ////////////

  public static final String NAME_TAG = "HierarchyPSP";
  public static final String CLUSTER_TAG = "Cluster";

  public static final String SOCIETY_ATTR="Society";
  public static final String VIRTUAL_ROOT_ATTR="VirtualRoot";
  public static final String VIRTUAL_ROOT_NAME_ATTR="VirtualRootName";

  //Variables:
  ////////////

  protected int society;
  protected String virtualRoot;
  protected String virtualRootName;
  protected Set clusters;

  //Constructors:
  ///////////////

  public HierarchyPSPConfig(){
    clusters=new HashSet(31);
  }

  /**Creates shallow copy, except the URLConnection is cloned using the
   * new cluster name
   **/
  public HierarchyPSPConfig(HierarchyPSPConfig hPSPConfig,
			    String cluster){  
    urlConnection=new URLConnectionData(hPSPConfig.urlConnection, cluster);
    this.society=hPSPConfig.society;
    this.virtualRoot=hPSPConfig.virtualRoot;
    this.virtualRootName=hPSPConfig.virtualRootName;
    this.clusters=hPSPConfig.clusters;
  }

  //Members:
  //////////

  public int getSociety(){
    return society;
  }

  public String getRoot(){
    if(getVirtualRoot()!=null){
      return getVirtualRoot();
    }else if(clusters.size()>0){
      return (String)(clusters.iterator().next());
    }else
      return "None";
  }

  public boolean forgeRoot(){
    return getVirtualRoot()!=null;
  }

  /**Returns null if there is no virtual root**/
  public String getVirtualRoot(){
    if(virtualRoot==null)
      return null;
    if(virtualRoot.equals(""))
      virtualRoot=null;
    if(clusters.size()>1&&virtualRoot==null)
      virtualRoot="Universal";
    return virtualRoot;
  }

  public String getVirtualRootName(){
    if(virtualRootName==null||virtualRootName.equals(""))
      return getVirtualRoot();
    return virtualRootName;
  }

  public void addCluster(String clusterName){
    clusters.add(clusterName);
  }

  public Iterator getClusterIterator(){
    return clusters.iterator();
  }

  /**Determine if we are loading from a file**/
  public boolean sourceIsFile(){
    if (urlConnection != null)
      return urlConnection.sourceIsFile();
    else {
      System.out.println ("HierarchyPSPConfig.sourceIsFile -- NOTE : urlConnection is not yet made!");
      return false;
    }
  }

  //XMLable:

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG, SOCIETY_ATTR, Integer.toString(society));
    urlConnection.toXML(w);
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
    if(name.equals(NAME_TAG)){
      society=HierarchyConnection.getSociety(attr.getValue(SOCIETY_ATTR));
      if(society==HierarchyConstants.SOC_UNKNOWN)
	throw new UnexpectedXMLException("Unknown society value: "+
					 attr.getValue(SOCIETY_ATTR));
      virtualRoot=attr.getValue(VIRTUAL_ROOT_ATTR);
      virtualRootName=attr.getValue(VIRTUAL_ROOT_NAME_ATTR);
    }else if(name.equals(CLUSTER_TAG)){
      addCluster(data);
    }else
      throw new UnexpectedXMLException("Unexpected tag: "+name);
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
