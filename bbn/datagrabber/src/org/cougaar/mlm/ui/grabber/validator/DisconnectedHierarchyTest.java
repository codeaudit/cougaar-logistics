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
import  org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;

/**
 * Looks for a disconnect in the Hierarchy
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class DisconnectedHierarchyTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DisconnectedHierarchyTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_ERROR;
  }

  /**for gui**/
  public String getDescription(){
    return "Nodes with no superior which are not root";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "disconnectedhierarchy";
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();

	String orgTable = Controller.getTableName(HierarchyConstants.ORG_TABLE_NAME,run);
	String orgRootsTable = Controller.getTableName(HierarchyConstants.ORGDESCEND_TABLE_NAME,run);
	String orgID = HierarchyConstants.COL_ORGID;
	String relID = HierarchyConstants.COL_RELID;

    sb.append(
			  "select distinct sup." + orgID +
			  "\nfrom " + orgTable + " sup "+
			  "left outer join " + orgTable + " sub "+
			  "on sub." + relID + "=sup." + orgID +
			  " left outer join " + orgRootsTable + " root "+
			  "on sup." + orgID + " = root." + orgID +
			  "\nwhere sub." + relID + " is null "+
			  "and root." + orgID + " is null");

	if (debug)
	  System.out.println ("DisconnectedHierarchyTest.constructTable - Sql is \n" + sb);
	
    dbConfig.createTableSelect(s,getTableName(run),sb.toString());
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Org ID"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_ORG};
    return types;
  }

  protected boolean debug = "true".equals(System.getProperty ("DisconnectedHierarchyTest.debug", "false"));
  
  //InnerClasses:
  ///////////////
}
