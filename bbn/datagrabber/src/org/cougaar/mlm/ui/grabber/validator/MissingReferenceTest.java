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

import java.util.StringTokenizer;

/**
 * Looks for missing references accross tables.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class MissingReferenceTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected int failureLevel;
  protected String description;
  protected String baseName;

  protected String tableContain;
  protected String columnContain;
  protected String tableMissing;
  protected String columnMissing;

  protected String header;
  protected int type;

  protected String extraCondition;

  //Constructors:
  ///////////////

  public MissingReferenceTest(DBConfig dbConfig,
			      String baseName,
			      int failureLevel,
			      String tableContain,
			      String columnContain,
			      String tableMissing,
			      String columnMissing,
			      String header,
			      int type,
			      String description){
    super(dbConfig);
    this.baseName=baseName;
    this.failureLevel=failureLevel;
    this.tableContain=tableContain;
    this.columnContain=columnContain;
    this.tableMissing=tableMissing;
    this.columnMissing=columnMissing;
    this.header=header;
    this.type=type;
    this.description=description;
  }

  public MissingReferenceTest(DBConfig dbConfig,
			      String baseName,
			      int failureLevel,
			      String tableContain,
			      String columnContain,
			      String tableMissing,
			      String columnMissing,
			      String header,
			      int type,
			      String description,
			      String extraCondition){
    super(dbConfig);
    this.baseName=baseName;
    this.failureLevel=failureLevel;
    this.tableContain=tableContain;
    this.columnContain=columnContain;
    this.tableMissing=tableMissing;
    this.columnMissing=columnMissing;
    this.header=header;
    this.type=type;
    this.description=description;
    this.extraCondition=extraCondition;
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return failureLevel;
  }

  /**for gui**/
  public String getDescription(){
    return description;
  }

  /**Base name**/
  protected String getRawTableName(){
    return baseName;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();  
    sb.append("select distinct contain.");
    sb.append(columnContain);
    sb.append(" from ");
    sb.append(tableContain);
    sb.append("_");
    sb.append(run);
    sb.append(" contain left outer join ");
    sb.append(tableMissing);
    sb.append("_");
    sb.append(run);
    sb.append(" missing on contain.");
    sb.append(columnContain);
    sb.append("=missing.");
    sb.append(columnMissing);
    sb.append(" where missing.");
    sb.append(columnMissing);
    sb.append(" is null");
    if(extraCondition!=null){
      sb.append(" ");
      sb.append(extraCondition);
      /* Might need this to insert run # where ever ~
      StringTokenizer strTok=new StringTokenizer(extraCondition,"~",true);
      while(strTok.hasMoreElements()){
	String tok=strTok.nextToken();
	if(tok.equals("~"))
	  sb.append(run);
	else
	  sb.append(tok);
      }
      */
    }
    dbConfig.createTableSelect(s,getTableName(run),sb.toString());
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={header};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={type};
    return types;
  }

  //InnerClasses:
  ///////////////
}
