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
 * Looks for overlapping legs.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class OverlappingLegsTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public OverlappingLegsTest(DBConfig dbConfig){
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
    return "Overlapping legs for a given carrier (Long to run)";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "overlappinglegs";
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    //Not using constants for now -- should be improved...
    sb.append(
"select distinct q.legid, q.starttime, q.endtime, q.convid "+
"from conveyedleg_"+run+" q, conveyedleg_"+run+" s "+
"where "+
"   (q.convid = s.convid) "+
"and(q.legid != s.legid) "+
"and(((q.starttime between s.starttime and s.endtime) "+
"     and q.starttime != s.starttime and q.starttime != s.endtime) "+
"    or "+
"    ((q.endtime between s.starttime and s.endtime) "+
"     and q.endtime != s.starttime and q.endtime != s.endtime)) "+
"order by q.convid");
    dbConfig.createTableSelect(s,getTableName(run),sb.toString());
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Leg ID", "Start time", "End time", "Conveyance id"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_LEG,
		 TYPE_DATETIME,
		 TYPE_DATETIME,
		 TYPE_CONV};
    return types;
  }

  //InnerClasses:
  ///////////////
}
