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

import java.sql.Statement;
import java.sql.SQLException;

/**
 * Looks for bad asset classes
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class BadAssetClassTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public BadAssetClassTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_WARNING;
  }

  /**for gui**/
  public String getDescription(){
    return "Invalid or unknown asset class";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "badassetclass";
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    //Not using constants for now -- should be improved...
    sb.append(
"select distinct prototypeid, alptypeid, alpnomenclature, assetclass "+
"from assetprototype_"+run+" "+
"where assetclass not between 1 and 12");

    dbConfig.createTableSelect(s,getTableName(run),sb.toString());
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Prototype ID","ALP Type ID", 
		      "ALP Nomenclature", "Asset Class"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_ASSET_PROTO,
		 TYPE_STRING,
		 TYPE_STRING,
		 TYPE_ENUM_1};
    return types;
  }

  //Likely to be over-ridden:
  //=========================

  /**Render enums**/
  protected String renderEnum(int whichEnum, int enum){
    switch(whichEnum){
    case TYPE_ENUM_1:
      return enum==DGPSPConstants.ASSET_CLASS_UNKNOWN?"Unknown":
	Integer.toString(enum);
    }
    return Integer.toString(enum);
  }

  //InnerClasses:
  ///////////////
}


