/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory;

import java.util.Hashtable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import java.awt.Component;
import javax.swing.JOptionPane;

import java.io.InputStream;
import java.io.ObjectInputStream;

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

public class InventoryConnectionManager implements InventoryDataSource
{

    static final String SERV_ID = "log_inventory";
    final public static String ASSET = "ASSET";
    final public static String ASSET_AND_CLASSTYPE =ASSET + ":" + "CLASS_TYPE:";
    final public static String GET_ALL_CLASS_TYPES = "All";
    final public static String[] ASSET_CLASS_TYPES = {GET_ALL_CLASS_TYPES,"Ammunition","BulkPOL","Subsistence","ClassVIIIMedical","PackagedPOL","Consumable"};

    private Component parentComponent;
    private String servProt;
    private String servHost;
    private String servPort;
    Hashtable orgURLs;

    private String invXMLStr;

    private Logger logger;

    public InventoryConnectionManager(Component parent) {
	this(parent,"http","localhost","8800");
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
    }   

    public static InventoryConnectionManager queryUserForConnection(Component parent) {
	InventoryConnectionManager returnManager = new InventoryConnectionManager(parent);
	if(returnManager.getOrgHostAndPort()) {
	    return returnManager;
	}

	return null;
    }

    public String getCurrentInventoryData() { return invXMLStr; };

    public String getInventoryData(String orgName, String assetName) {
	InputStream is = null;
	String orgURL = (String)orgURLs.get(orgName);
	
    
	logger.info("ExecutingQuery: " + assetName + " to: " + orgURL +
                       " for: " + SERV_ID);

	try {
	    ConnectionHelper connection = 
		new ConnectionHelper( orgURL, SERV_ID);
	    
	    connection.sendData(assetName);
	    is = connection.getInputStream();
	} catch (Exception e) {
	    displayErrorString(e.toString());
	}
	
	invXMLStr = null;
	try {
	    ObjectInputStream p = new ObjectInputStream(is);
	    invXMLStr = (String)p.readObject();
	} catch (Exception e) {
	    displayErrorString("getInventoryData:Object read exception: " + e);
	}

	//parse here

	return invXMLStr;
    }

    public Vector getAssetNames(String orgName, String supplyType) {
	String orgURL = (String)orgURLs.get(orgName);
	//When querying for all just ASSET
	String queryStr=ASSET;

	if((supplyType != null) &&
	   (!(supplyType.equals(GET_ALL_CLASS_TYPES)))) {
	    queryStr = (ASSET_AND_CLASSTYPE + supplyType);
	}

	//logger.debug("Submitting: " + queryStr + " to: " + orgURL +
	//                   " for: " + SERV_ID);
	InputStream is = null;
	try {
	    ConnectionHelper connection = 
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
	    assetNames = (Vector)p.readObject();
	} catch (Exception e) {
	    displayErrorString("Object read exception: " + "_2_" + e.toString());
	    return null;
	}
	Collections.sort(assetNames);
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

    public Vector getOrgNames() {
	logger.debug("Getting Org List");
	try {
	    ConnectionHelper connection = new ConnectionHelper(getURLString());
	    orgURLs = connection.getClusterIdsAndURLs();
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

    private Vector getSortedOrgNames() {
	if(orgURLs != null) {
	    Enumeration names = orgURLs.keys();
	    Vector vNames = new Vector();
	    while (names.hasMoreElements())
		vNames.addElement(names.nextElement());
	    Collections.sort(vNames);
	    return vNames;
	}
	return null;
    }

    private static void displayErrorString(String reply) {
	JOptionPane.showMessageDialog(null, reply, reply, 
				      JOptionPane.ERROR_MESSAGE);
    }    

    public String getURLString() {
	return servProt + "://" + servHost + ":" + servPort + "/";
    }
    
    public boolean getOrgHostAndPort() {

	String msg = "Enter cluster Log Plan Server location as host:port";
	String s = ConnectionHelper.getClusterHostPort(null,msg,servHost,servPort);
	if ((s == null) || (s.trim().equals(""))) {
	    displayErrorString("Entered Nothing. Cannot get connection.");
	    return false;
	}
	s = s.trim();
	if (s.length() != 0) {
	    String[] substrings = s.split("://");
	    if(substrings.length >= 2) {
		servProt = substrings[0];
		String hostAndPort = substrings[1];
		int i = hostAndPort.indexOf(":");
		if (i != -1) {
		    servHost = hostAndPort.substring(0, i);
		    servPort = hostAndPort.substring(i+1);
		    System.out.println("getOrgHostAndPort url = " + getURLString());
		    return true;
		}
	    }
	}
	displayErrorString("Improper Format for url.  Cannot get connection.");
	return false;
    }
}


