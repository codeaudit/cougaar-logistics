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
package org.cougaar.mlm.ui.grabber.config;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import java.io.IOException;
import org.xml.sax.Attributes;

/**
 * Produces URL necessary for a given PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2//01
 **/
public class URLConnectionData implements XMLable, DeXMLable{

  //Constants:
  ////////////

  public static final String NAME_TAG = "URLConnection";
  public static final String HOST_TAG = "Host";
  public static final String PORT_TAG = "Port";
  public static final String CLUSTER_TAG = "ClusterName";
  public static final String PACKAGE_TAG = "Package";
  public static final String PSPID_TAG = "PspID";
  public static final String XML_TAG = "UseXML";
  public static final String FILE_TAG = "ThisFileInstead";
  public static final String USE_PSPS_TAG = "UsePsps";
  public static final String TIMEOUT_TAG = "Timeout";

  public static final long DEFAULT_TIMEOUT = 120000; //millis - two minutes

  //Variables:
  ////////////

  protected String host=null;
  protected int port=0;
  protected String clusterName=null;
  protected String pspPackage=null;
  protected String pspID=null;
  protected boolean transferByXML=true;
  protected String fileName=null;
  protected boolean usePSPs=false; // use servlets by default
  protected long timeout=DEFAULT_TIMEOUT; // two minutes

  //Constructors:
  ///////////////

  public URLConnectionData(){}

  public URLConnectionData(String host,
			   int port,
			   String clusterName,
			   String pspPackage,
			   String pspID,
			   boolean transferByXML,
			   boolean usePSPs,
			   long timeout){    
    this.host=host;
    this.port=port;
    this.clusterName=clusterName;
    this.pspPackage=pspPackage;
    this.pspID=pspID;
    this.transferByXML=transferByXML;
    this.usePSPs=usePSPs;
    this.timeout=timeout;
  }

  public URLConnectionData(String fileName, boolean transferByXML){
    this.fileName=fileName;
    this.transferByXML=transferByXML;
  }

  public URLConnectionData(URLConnectionData ucd, String clusterName){
    this.clusterName=clusterName;
    this.host=ucd.host;
    this.port=ucd.port;
    this.pspPackage=ucd.pspPackage;
    this.pspID=ucd.pspID;
    this.fileName=ucd.fileName;
    this.transferByXML=ucd.transferByXML;
    this.usePSPs=ucd.usePSPs;
    this.timeout=ucd.timeout;
  }

  //Members:
  //////////

  public String getHost(){
    return host;
  }

  public int getPort(){
    return port;
  }

  public String getClusterName(){
    return clusterName;
  }

  protected String getPSPPackage(){
    return pspPackage;
  }

  /**This should return the PSP id for this specific psp**/
  protected  String getPSPID(){
    return pspID;
  }

  /**If we should load from a file instead of a http url this
   * will give the filename, otherwise null.
   **/
  public String getFileName(){
    return fileName;
  }

  /**true iff we are reading from a file**/
  public boolean sourceIsFile(){
    return !(fileName==null||fileName.equals(""));
  }

  /**Determine if we should use XML**/
  public boolean transferByXML(){
    return transferByXML;
  }

  /**Are we using PSPs or Servlets?**/
  public boolean usingPSPs(){
    return usePSPs;
  }

  /** how long should we wait for data on this URL? */
  public long getTimeout () { return timeout; }

  /**
   * PSP URL: "http://"+ host+":"+port+"/$"+clusterName+"/"+
   * PSP_Package+"/"+PSP_id;
   **/
  public String getURL(){
    String pspPackage = getPSPPackage();
    boolean hasPackage = (pspPackage.length () > 0);

    return "http://" + getHost() +":"+ Integer.toString(getPort())
      +"/$"+ getClusterName() +
      ((hasPackage) ? ("/"+ pspPackage) : "") +"/"+ getPSPID();
  }


  //XMLable:

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    w.tagln(HOST_TAG,host);
    w.tagln(PORT_TAG,port);
    w.tagln(CLUSTER_TAG,clusterName);
    w.tagln(PACKAGE_TAG,pspPackage);
    w.tagln(PSPID_TAG,pspID);
    w.tagln(XML_TAG,transferByXML?"Y":"N");
    w.tagln(FILE_TAG,fileName);
    w.tagln(USE_PSPS_TAG,usePSPs?"Y":"N");
    w.tagln(TIMEOUT_TAG,timeout);
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
    try{
      if(name.equals(NAME_TAG)){
      }else if(name.equals(HOST_TAG)){
	host=data;
      }else if(name.equals(PORT_TAG)){
	if(data!=null && !data.equals(""))
	  port=Integer.parseInt(data);
      }else if(name.equals(CLUSTER_TAG)){
	clusterName=data;
      }else if(name.equals(PACKAGE_TAG)){
	pspPackage=data;
      }else if(name.equals(PSPID_TAG)){
	pspID=data;
      }else if(name.equals(XML_TAG)){
	transferByXML=data.equals("Y");
      }else if(name.equals(USE_PSPS_TAG)){
	usePSPs=data.equals("Y");
      }else if(name.equals(TIMEOUT_TAG)){
	timeout=Long.parseLong(data);
      }else if(name.equals(FILE_TAG)){
	fileName=data;
      }else
	throw new UnexpectedXMLException("Unexpected tag: "+name);
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Could not parse number:"+data);
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
    throw new UnexpectedXMLException("Unexpcted subobj:"+obj);
  }

  //InnerClasses:
  ///////////////
}

