package org.cougaar.mlm.ui.grabber.controller;

import java.sql.Connection;
import java.util.List;

public interface DBConnectionProvider {
  public Connection getDBConnection();
  public int getNumDBConnections ();
  public List getAllDBConnections ();
}
