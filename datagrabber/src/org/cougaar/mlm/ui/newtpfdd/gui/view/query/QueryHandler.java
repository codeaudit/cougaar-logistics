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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.sql.DriverManager;
import java.sql.Connection;

import java.util.List;
import java.util.Set;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.DatabaseRun;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseState;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

public class QueryHandler {
  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.query.QueryHandler.debug", 
									   "false"));
  public QueryHandler() {
  }

  /** returns a forest */
  public Set performQuery(DatabaseConfig dbConfig, Query query) {

	if(debug) {
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "dbConfig " + dbConfig);
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "query    " + query);
	}
	
	QueryResponse response = query.getResponse(dbConfig.getConnection());
	    
	// You could put code here that handles Out-of-band info in QueryResponse
	return response.getTrees ();
  }

  public void cleanup() {
  }

  public Query createHierarchyQuery (DatabaseState dbState) {	
    return new HierarchyQuery (dbState.getRun());  
  }

  public Query createLightHierarchyQuery (DatabaseState dbState) {	
    return new HierarchyQuery (dbState.getRun(),false);  
  }

  public Query createUnitQuery      (DatabaseState dbState, FilterClauses filterClauses) {	
	return new UnitQuery (dbState.getRun(), filterClauses);  
  }

  public Query createCarrierQuery      (DatabaseState dbState, FilterClauses filterClauses) {	
	return new CarrierQuery (dbState.getRun(), filterClauses);  
  }

  public Query createTPFDDQuery      (DatabaseState dbState, FilterClauses filterClauses) {	
	return new TPFDDQuery (dbState.getRun(), filterClauses);  
  }

  public Query createFilterQuery      (DatabaseState dbState, FilterClauses filterClauses) {	
	return new FilterQuery (dbState.getRun(), filterClauses);  
  }

  public Query createListQuery      (DatabaseState dbState, FilterClauses filterClauses) {	
	return new ListQuery (dbState.getRun(), filterClauses);  
  }

  public Query createAssetCategoryQuery(DatabaseState dbState) {	
	return new AssetCategoryQuery (dbState.getRun());  
  }
  public Query createAssetInstanceQuery(DatabaseState dbState, FilterClauses filterClauses) {	
  	return new AssetInstanceQuery (dbState.getRun(), filterClauses);  
  }
    public Query createAssetTPFDDQuery(DatabaseState dbState, FilterClauses filterClauses) {
	return new AssetTPFDDQuery(dbState.getRun(), filterClauses);
    }

  public DatabaseRun getRecentRun (DatabaseConfig dbConfig) {
	Connection conn = dbConfig.createDBConnection (dbConfig.getHost(), dbConfig.getDatabase());
	SqlQuery runDatabaseQuery = new SqlQuery ();
	List runs = runDatabaseQuery.getRuns (dbConfig.getDatabase(), conn);
	if (runs.size () == 0)
	  return null;
	
	return (DatabaseRun) runs.get (runs.size ()-1);
  }
  
  public List getDatabases (DatabaseConfig dbConfig) {
	SqlQuery runDatabaseQuery = new SqlQuery ();
	return runDatabaseQuery.getDatabases (dbConfig);
  }

  public List getRuns (DatabaseConfig dbConfig, String database) {
	Connection conn = dbConfig.createDBConnection (dbConfig.getHost(), database);
	SqlQuery runDatabaseQuery = new SqlQuery ();
	return runDatabaseQuery.getRuns (database, conn);
  }
}

