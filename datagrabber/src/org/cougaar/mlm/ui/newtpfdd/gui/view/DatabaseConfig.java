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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.sql.DriverManager;
import java.sql.Connection;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;
import org.cougaar.mlm.ui.grabber.config.DBConfig;

/** now mainly uses DBConfig to hold database config information */
public class DatabaseConfig {
  protected String host;
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.producer.DatabaseConfig.debug", 
									   "false"));

  protected Connection connection;
  protected String database;
  protected DBConfig dbConfig = new DBConfig ();
  
  public DatabaseConfig(String host) {
	if (debug)
	  System.out.println ("DatabaseConfig - ctor, host " + host);

	this.host = host;

	// set the user -- explicit system property masks out cougaar.rc parameter, sets it in DBConfig object
	String user  = System.getProperty("org.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbUser");
	if (user != null && user.length () > 0)
	  try { dbConfig.openTag (DBConfig.USER_TAG, null, user); } catch (Exception e) {e.printStackTrace();}
	
	// set the password
	String password = 
	  System.getProperty("org.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbPassword");
	if (password != null)
	  try { dbConfig.openTag (DBConfig.PASS_TAG, null, password); } catch (Exception e) {e.printStackTrace();}

	// confirm driver is correct
	try {
	  Class.forName(System.getProperty("org.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverName"));
	} catch (Exception e) {
	  if (dbConfig.getDriverClassName () == null)
		System.err.println("WARNING: Failed to Load Driver.  None specified.\n"+e);
	  else {
		try { Class.forName(dbConfig.getDriverClassName ()); 
		} catch (Exception f) {
		  System.err.println("WARNING: Failed to Load Driver ("+ dbConfig.getDriverClassName() + ").\n"+f);
		}
	  }
	}

	// set connection type -- what kind of database are we using?  MySQL or ORACLE?
	String connectionTypeName = 
	  System.getProperty("org.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.dbDriverType");
	if (connectionTypeName != null && connectionTypeName.length () > 0){
	  // set connection type
	  try { dbConfig.openTag (DBConfig.SYNTAX_TAG, null, connectionTypeName); } catch (Exception e) {e.printStackTrace();}
	}
	
	String database = System.getProperty("org.cougaar.mlm.ui.newtpfdd.producer.DataGrabberProducer.database");

	if (database != null && database.length () > 0) {
	  try { 
		dbConfig.openTag (DBConfig.URL_TAG, null, "jdbc:mysql://" + host + "/" + database); 
		setDatabase (database);
	  } catch (Exception e) {e.printStackTrace();}
	} else {
	  // set initial database to be one from cougaar.rc file
	  setDatabase (dbConfig.getDatabaseName());
	}
  
	connection = createDBConnection(host, database);
  }

  public String getHost () { return host; }
  public String getDatabase () { return database; }
  public void   setDatabase (String database) { this.database = database; }
	
  public Connection getConnection () {
	return connection;
  }

  /** closes the old connection (gc will do it too, but why wait?) */
  public void setConnection (Connection conn) {
	try {
	  if (connection != null)
		connection.close();
	  connection = conn;
	} catch (Exception e) {
	  System.err.println("DatabaseConfig : WARNING - Failed to close DB Connection.\n"+e);
	}
  }

  public int getConnectionType () {  return dbConfig.getSyntax(); }

  public boolean usingMySql  () { return getConnectionType() == DBConfig.MYSQL; }
  public boolean usingOracle () { return getConnectionType() == DBConfig.ORACLE; }

  /** create a new database connection, mainly using settings from dbConfig */
  public Connection createDBConnection(String host, String database) {
	Connection connect = null;

	String dbURL = "jdbc:mysql://" + host + "/";
	
	if (database != null)
	  dbURL += database;

	if (debug) 
	  System.out.println ("DatabaseConfig.createdDBConnection - connecting to \n\tdatabase at\t<" + 
						  dbURL + ">\n\tuser\t<" + dbConfig.getUser() + ">\n\tpassword\t<" + dbConfig.getPassword() + ">.");
	
	try {
	  connect = DriverManager.getConnection(dbURL, dbConfig.getUser(), dbConfig.getPassword());
	} catch (Exception e) {
	  System.err.println("DatabaseConfig : WARNING - Failed to create DB Connection.\n"+e);
	  connect = null;
	}
	return connect;
  }

  public DBConfig getDBConfig () {	return dbConfig;  }

}

