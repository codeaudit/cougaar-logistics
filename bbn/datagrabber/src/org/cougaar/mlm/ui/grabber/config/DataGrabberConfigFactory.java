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

import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;

import org.xml.sax.Attributes;

/**
 * Factory for generating DataGrabberConfig from xml
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2//01
 **/
public class DataGrabberConfigFactory implements DeXMLableFactory{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DataGrabberConfigFactory(){
  }

  //Members:
  //////////

  /**
   * This is a look ahead to see if we should start a sub object.
   * The caller will first call this function on startElement.  If
   * this function returns null, the startElement will be reported
   * to the current object with a call to openTag(...).  Otherwise
   * this function should return a new DeXMLable subobject that
   * further output will be deligated to, until the subobject returns
   * true from a call to endElement().
   *
   * @param name startElement tag
   * @param attr startElement attributes
   * @return a new DeXMLable subobject if a subobject should be created,
   * otherwise null.
   **/
  public DeXMLable beginSubObject(DeXMLable curObj, String name, 
				  Attributes attr)
    throws UnexpectedXMLException{
    if(curObj==null){
      if(name.equals(DataGrabberConfig.NAME_TAG)){
	return new DataGrabberConfig();
      }
    }else if(curObj instanceof DataGrabberConfig){
      if(name.equals(DBConfig.NAME_TAG)){
	return new DBConfig();
      }else if(name.equals(WebServerConfig.NAME_TAG)){
	return new WebServerConfig();
      }else if(name.equals(HierarchyPSPConfig.NAME_TAG)){
	return new HierarchyPSPConfig();
      }else if(name.equals(DataGathererPSPConfig.NAME_TAG)){
	return new DataGathererPSPConfig();
      }else if(name.equals(DerivedTablesConfig.NAME_TAG)){
	return new DerivedTablesConfig();
      }
    }else if(curObj instanceof WebServerConfig){
      if(name.equals(CompletionPSPConfig.NAME_TAG)){
	return new CompletionPSPConfig();
      }
    }else if(curObj instanceof HierarchyPSPConfig){
      if(name.equals(URLConnectionData.NAME_TAG)){
	 return new URLConnectionData();
      }
    }else if(curObj instanceof DataGathererPSPConfig){
      if(name.equals(URLConnectionData.NAME_TAG)){
	 return new URLConnectionData();
      }
    }else if(curObj instanceof CompletionPSPConfig){
      if(name.equals(URLConnectionData.NAME_TAG)){
	 return new URLConnectionData();
      }
    }
    return null;
  }

  //InnerClasses:
  ///////////////
}
