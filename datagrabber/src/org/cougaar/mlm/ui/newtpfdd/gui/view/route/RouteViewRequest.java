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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import org.cougaar.mlm.ui.grabber.controller.Controller;

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract class for all classes that produce data for the
 * RouteView
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2//01
 **/
public abstract class RouteViewRequest{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public RouteViewRequest(){

  }

  //Members:
  //////////
  public abstract String getTitle();

  public abstract RouteData getRouteData(DatabaseConfig dbConfig, int runID);

  protected String getTableName(String baseName, int runID){
    return Controller.getTableName(baseName,runID);
  }

  protected Statement getStatement(DatabaseConfig dbConfig)throws SQLException{
    return dbConfig.getConnection().createStatement();
  }

  //InnerClasses:
  ///////////////
}
