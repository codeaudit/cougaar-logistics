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

import org.cougaar.mlm.ui.grabber.webserver.*;

import java.io.IOException;
import org.xml.sax.Attributes;

import java.io.File;

/**
 * Data for configuring the web server
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/8/01
 **/
public class WebServerConfig implements XMLable, DeXMLable{

  //Constants:
  ////////////

  public static final String NAME_TAG = "WebServerConfig";
  public static final String PORT_TAG = "Port";
  public static final String ROOT_TAG = "DocumentRoot";
  public static final String TIMEOUT_TAG = "Timeout";
  public static final String COMMAND_REFRESH_TAG = "CommandRefresh";
  public static final String VIEW_REFRESH_TAG = "ViewRefresh"; 

  public static final int CONTROLLER=1;
  public static final int VALIDATOR=2;
  public static final int COMPLETIONASSESSOR=3;
  public static final int DERIVED_TABLES=4;
  public static final int COMPARATOR=5;

  //Variables:
  ////////////
  
  /**The port to listen on**/
  protected int port;

  /** the web server's virtual root **/
  protected String root;

  /** timeout on client connections **/
  protected int timeout=10000;

  /** time for refreshes on commands **/
  protected int commandRefresh;

  /** time for refreshes on views **/
  protected int viewRefresh;

  protected CompletionPSPConfig completionPSPConfig;

  //Constructors:
  ///////////////

  public WebServerConfig(){
  }

  public WebServerConfig(int port, String root, int timeout){
    this.port=port;
    this.root=root;
    this.timeout=timeout;
  }

  //Members:
  //////////

  public String getBaseDirectory(int module){
    switch(module){
    case CONTROLLER:
      return "/controller";
    case VALIDATOR:
      return "/validator";
    case COMPLETIONASSESSOR:
      return "/completion_assessor";
    case DERIVED_TABLES:
      return "/derived_tables";
    case COMPARATOR:
      return "/comparator";
    default:
      return "unknown";
    }
  }

  public String getURL(int module, int command){
    String ret=getBaseDirectory(module)+"/";
    switch(module){
    case CONTROLLER:
      ret+=ControllerRequestHandler.getCommandName(command);
      break;
    case VALIDATOR:
      ret+=ValidatorRequestHandler.getCommandName(command);
      break;
    case COMPARATOR:
      ret+=ComparisonRequestHandler.getCommandName(command);
      break;
    case COMPLETIONASSESSOR:
      ret+=CompletionAssessorRequestHandler.getCommandName(command);
      break;
    case DERIVED_TABLES:
      ret+=DerivedTablesRequestHandler.getCommandName(command);
      break;
    default:
      ret+= "unknown";
    }
    return ret;
  }

  /** Get the name of the application.  Currently used in webpage headers**/
  public String getApplicationName(){
    return "DataGrabber";
  }

  public int getPort(){
    return port;
  }

  public int getTimeout(){
    return timeout;
  }

  public String getRoot(){
    if(root==null||root.length()<1)
      return "";
    if(root.charAt(root.length()-1)==File.separatorChar)
      return root.substring(0,root.length()-1);
    return root;
  }

  public int getCommandRefresh(){
    return commandRefresh;
  }

  public int getViewRefresh(){
    return viewRefresh;
  }

  public CompletionPSPConfig getCompletionPSPConfig(){
    return completionPSPConfig;
  }

  //XMLable:

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(NAME_TAG);
    w.tagln(PORT_TAG,Integer.toString(port));
    w.tagln(ROOT_TAG,root);
    w.tagln(TIMEOUT_TAG,Integer.toString(timeout));
    w.tagln(COMMAND_REFRESH_TAG, Integer.toString(commandRefresh));
    w.tagln(VIEW_REFRESH_TAG, Integer.toString(viewRefresh));
    completionPSPConfig.toXML(w);
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
      }else if(name.equals(PORT_TAG)){
	port=Integer.parseInt(data);
      }else if(name.equals(ROOT_TAG)){
	root=data;
      }else if(name.equals(TIMEOUT_TAG)){
	timeout=Integer.parseInt(data);
      }else if(name.equals(COMMAND_REFRESH_TAG)){
	commandRefresh=Integer.parseInt(data);
      }else if(name.equals(VIEW_REFRESH_TAG)){
	viewRefresh=Integer.parseInt(data);
      }else
	throw new UnexpectedXMLException("Unexpected open tag:"+name);
    }catch(NumberFormatException e){
      throw new UnexpectedXMLException("Cannot parse as number("+data+"):"+e);
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
    if(obj instanceof CompletionPSPConfig) {
      completionPSPConfig=(CompletionPSPConfig)obj;
    } else{
      throw new UnexpectedXMLException("Unexpected subobject:"+obj);
    }
  }

  //InnerClasses:
  ///////////////
}
