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
package org.cougaar.mlm.ui.grabber.validator;

import  org.cougaar.mlm.ui.grabber.logger.Logger;
import  org.cougaar.mlm.ui.grabber.config.DBConfig;
import  org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * C
 * @author Gordon Vidaver
 * @since 05/02/01
 **/
public class UnitPalletTest extends UnitMilvansTest{
  
  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public UnitPalletTest(DBConfig dbConfig, boolean g){
    super(dbConfig, g);
  }

  //Members:
  //////////

  /**for gui**/
  public String getDescription(){
    return "Number of pallets " + getSuffix ();
  }

  /**Base name**/
  protected String getRawTableName(){
    return "Unit"+getSuffix()+"PalletsAmount";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit","Pallets Moved"};
    return headers;
  }

  protected String getMatchString () { return "%463L%"; }

  //InnerClasses:
  ///////////////
}





