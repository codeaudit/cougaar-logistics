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

import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;

import org.xml.sax.Attributes;

/**
 * Factory for generating DataGrabberConfig from xml
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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
