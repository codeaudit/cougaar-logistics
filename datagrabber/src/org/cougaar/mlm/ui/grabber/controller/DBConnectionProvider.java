package org.cougaar.mlm.ui.grabber.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public interface DBConnectionProvider {
  Connection getDBConnection();
  Statement createStatement() throws SQLException;
  int getNumDBConnections ();
  List getAllDBConnections ();
}
