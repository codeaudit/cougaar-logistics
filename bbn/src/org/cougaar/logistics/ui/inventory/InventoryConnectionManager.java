/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.inventory;

import java.util.Hashtable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import java.awt.Component;
import javax.swing.JOptionPane;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.io.StringWriter;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * <pre>
 *
 * The InventoryConnectionManager manages the connnection between
 * the LogisticsInventoryGUI and the servlet.   It gets the data
 * necessary to populate the gui screens.    The GUI asks for data
 * the connection manager gets it and the Frame and Panels are populated
 * with the returned data.
 *
 * @see InventoryDataSource
 * @see InventoryFileManager
 * @see InventoryUIFrame
 *
 **/

public class InventoryConnectionManager implements InventoryDataSource {

  public static final String SERV_ID = "log_inventory";
  public static final int INITIAL_XML_SIZE = 800000; 

  final public static String ASSET = "ASSET";
  final public static String ASSET_AND_CLASSTYPE = ASSET + ":" + "CLASS_TYPE:";
  final public static String GET_ALL_CLASS_TYPES = "ALL";
  final public static String[] ASSET_CLASS_TYPES = {GET_ALL_CLASS_TYPES, "Ammunition", "BulkPOL", "Subsistence", "ClassVIIIMedical", "PackagedPOL", "Consumable"};

  protected Component parentComponent;
  protected String servProt;
  protected String servHost;
  protected String servPort;
  protected Hashtable orgURLs;

  protected Hashtable visitedOrgURLs;

  protected String invXMLStr;

  protected Logger logger;

  public InventoryConnectionManager(Component parent) {
    this(parent, "http", "localhost", "8800");
  }

  public InventoryConnectionManager(Component parent,
                                    String targetProtocol,
                                    String targetHost,
                                    String targetPort) {
    parentComponent = parent;
    servHost = targetHost;
    servPort = targetPort;
    servProt = targetProtocol;
    logger = Logging.getLogger(this);
    visitedOrgURLs = new Hashtable();

// Support HTTPS with client-cert authentication
    doSecureUserAuthInit();
  }


  // Invoke the NAI security code, if available
  private void doSecureUserAuthInit() {
    String securityUIClass = System.getProperty("org.cougaar.ui.userAuthClass");

    if (securityUIClass == null) {
      securityUIClass = "org.cougaar.core.security.userauth.UserAuthenticatorImpl";
    }

    Class cls = null;
    try {
      cls = Class.forName(securityUIClass);
    } catch (ClassNotFoundException e) {
      if (logger.isInfoEnabled())
        logger.info("Not using secure User Authentication: " + securityUIClass);
    } catch (ExceptionInInitializerError e) {
      if (logger.isWarnEnabled())
        logger.warn("Unable to use secure User Authentication: " + securityUIClass + ". ", e);
    } catch (LinkageError e) {
      if (logger.isInfoEnabled())
        logger.info("Not using secure User Authentication: " + securityUIClass);
    }

    if (cls != null) {
      try {
        cls.newInstance();
      } catch (Exception e) {
        if (logger.isWarnEnabled())
          logger.warn("Error using secure User Authentication (" + securityUIClass + "): ", e);
      }
    }
  }

  public static InventoryConnectionManager queryUserForConnection(Component parent, InventoryDataSource dataSource) {
    InventoryConnectionManager returnManager;

    if ((dataSource != null) &&
        (dataSource instanceof InventoryConnectionManager)) {
      returnManager = (InventoryConnectionManager) dataSource;
    } else {
      returnManager = new InventoryConnectionManager(parent);
    }
    if (returnManager.getOrgHostAndPort()) {
      return returnManager;
    }

    return null;
  }

  public String getCurrentInventoryData() {
    return invXMLStr;
  }

  public String getInventoryData(String orgName, String assetName) {
    InputStream is = null;
    ConnectionHelper connection = null;
    String orgURL = (String) orgURLs.get(orgName);

    if (logger.isInfoEnabled()) {
      logger.info("ExecutingQuery: " + assetName + " to: " + orgURL +
                  " for: " + SERV_ID);
    }

    //System.out.println("InventoryConnectionManager>>getInventoryData-ExecutingQuery: org-"+ orgName + " asset-" + assetName + " to: " + orgURL + " for: " + SERV_ID);

    try {
      connection =
          new ConnectionHelper(orgURL, SERV_ID);
      connection.sendData(assetName);
      is = connection.getInputStream();
    } catch (Exception e) {
      displayErrorString(e.toString());
    }

    invXMLStr = null;
    StringWriter writer = new StringWriter(INITIAL_XML_SIZE);
    try {
      //ObjectInputStream p = new ObjectInputStream(is);
      //invXMLStr = (String)p.readObject();
      BufferedReader p = new BufferedReader(new InputStreamReader(is, Charset.forName("ASCII")));
      writer.write(p.readLine() + "\n");
      String currLine = p.readLine();
      while (currLine != null) {
        writer.write(currLine + "\n");
        currLine = p.readLine();
      }
      p.close();
      writer.flush();
      invXMLStr = writer.toString();
      connection.closeConnection();
    } catch (Exception e) {
      displayErrorString("getInventoryData:Object read exception: " + e);
    }

    //parse here

    return invXMLStr;
  }

  public Vector getAssetNames(String orgName, String supplyType) {
    String orgURL = (String) orgURLs.get(orgName);
//When querying for all just ASSET
    String queryStr = ASSET;

    if ((supplyType != null) &&
        (!(supplyType.equals(GET_ALL_CLASS_TYPES)))) {
      queryStr = (ASSET_AND_CLASSTYPE + supplyType);
    }

//logger.debug("Submitting: " + queryStr + " to: " + orgURL +
//                   " for: " + SERV_ID);

System.out.println("Submitting: " + queryStr + " to: " + orgURL +
                   " for: " + SERV_ID);

    ConnectionHelper connection = null;
    InputStream is = null;
    try {
      connection =
          new ConnectionHelper(orgURL, SERV_ID);

      connection.sendData(queryStr);

      is = connection.getInputStream();
    } catch (Exception e) {
      displayErrorString(e.toString());
      return null;
    }
    Vector assetNames = null;
    try {
      ObjectInputStream p = new ObjectInputStream(is);
      assetNames = (Vector) p.readObject();
      p.close();
      connection.closeConnection();
    } catch (Exception e) {
      displayErrorString("Object read exception: " + "_2_" + e.toString());
      return null;
    }
    Collections.sort(assetNames);
    if((assetNames != null) &&
       (!assetNames.isEmpty())) {
	visitedOrgURLs.put(orgName,orgURL);
    }
    return assetNames;
  }

  public String[] getSupplyTypes() {
    return ASSET_CLASS_TYPES;
  }

  public String getDefaultOrganizationName() {
    Vector orgNames = getSortedOrgNames();
    if (orgNames == null) return null;
    return (String) orgNames.elementAt(1);
  }


  public Vector getOrgNames(String agentPath,String orgPopMethod) {
      if(orgPopMethod.equals(InventorySelectionEvent.ORGS_HIST)) {
        orgURLs = visitedOrgURLs;
	return getSortedOrgNames();
      }
      else if(orgPopMethod.equals(InventorySelectionEvent.ORGS_ALL)) {
        return getOrgNames(agentPath,true);
      }
      String newAgentPath = agentPath;
        if(newAgentPath == null) {
            newAgentPath = ".";
      }
      return getOrgNames(newAgentPath,false);
  }


  protected Vector getOrgNames(String agentPath,boolean getAll) {
    logger.debug("Getting Org List");
    ConnectionHelper connection = null;
    try {
      connection = new ConnectionHelper(getURLString());
      if(getAll) {
	  orgURLs = connection.getAllClusterIdsAndURLs(parentComponent);
      }
      else {
        orgURLs = connection.getClusterIdsAndURLs(parentComponent, agentPath);
      }
      connection.closeConnection();
      connection = null;
      if (orgURLs == null) {
        logger.warn("No ORG/Agents");
        return null;
      }
    } catch (Exception e) {
      logger.error(e.toString());
      return null;
    }
    return getSortedOrgNames();
  }

  protected Vector getSortedOrgNames() {
    if (orgURLs != null) {
      Enumeration names = orgURLs.keys();
      Vector vNames = new Vector();
      while (names.hasMoreElements())
        vNames.addElement(names.nextElement());
      Collections.sort(vNames);
      return vNames;
    }
    return null;
  }

  protected void displayErrorString(String reply) {
    JOptionPane.showMessageDialog(parentComponent, reply, reply,
                                  JOptionPane.ERROR_MESSAGE);
  }

  public String getURLString() {
    return servProt + "://" + servHost + ":" + servPort + "/";
  }

  public boolean getOrgHostAndPort() {

    String msg = "Enter cluster Log Plan Server location as host:port";
    String s = ConnectionHelper.getClusterHostPort(parentComponent, msg, servHost, servPort);
    if ((s == null) || (s.trim().equals(""))) {
      displayErrorString("Entered Nothing. Cannot get connection.");
      return false;
    }
    s = s.trim();
    if (s.length() != 0) {
      String[] substrings = s.split("://");
      if (substrings.length >= 2) {
        servProt = substrings[0];
        String hostAndPort = substrings[1];
        int i = hostAndPort.indexOf(":");
        if (i != -1) {
          servHost = hostAndPort.substring(0, i);
          servPort = hostAndPort.substring(i + 1);
          //System.out.println("getOrgHostAndPort url = " + getURLString());
          return true;
        }
      }
    }
    displayErrorString("Improper Format for url.  Cannot get connection.");
    return false;
  }
}


