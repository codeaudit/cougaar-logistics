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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.DBConnectionProvider;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

/** now mainly uses DBConfig to hold database config information */
public class DatabaseConfig implements DBConnectionProvider {
  protected String host;
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.producer.DatabaseConfig.debug", 
									   "false"));

  protected Connection connection;
  protected String database;
  protected DBConfig dbConfig = new DBConfig ();
  
  public DatabaseConfig(String host) {
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "DatabaseConfig - ctor, host " + host);

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
	
  public int getNumDBConnections () { return 1; }

  public List getAllDBConnections () { 
    List list = new ArrayList();
    list.add(connection);
    return list;
  }

  public Connection getDBConnection () {
    return getConnection ();
  }

  public Statement createStatement () throws SQLException {
    return getConnection ().createStatement();
  }

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
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "DatabaseConfig.createdDBConnection - connecting to \n\tdatabase at\t<" +
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

