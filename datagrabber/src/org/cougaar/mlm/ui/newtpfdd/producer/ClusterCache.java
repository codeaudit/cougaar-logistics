package org.cougaar.mlm.ui.newtpfdd.producer;

import java.io.IOException;
import java.io.StringReader;

import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.JOptionPane;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

// modern xerces references
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;


// old IBM XML jar references
// import com.ibm.xml.parsers.SAXParser;
// import org.xml.sax.AttributeList;
// import org.xml.sax.HandlerBase;
// import org.xml.sax.Parser;
// import org.xml.sax.helpers.ParserFactory;

/**
 *  Deals with properties that define host name, optionally pops up a dialog so can dynamically set host
 */
public class ClusterCache
{
    public static final int CLIENT_PORT = 1111;

    // Vector of valid cluster IDs
    private Vector clusterNames = null;
    
    // Hashtable mapping cluster IDs to URL
    private Hashtable clusterURLs;
    // Hashtables mapping cluster IDs to appropriate active Producer of that cluster
  //    private Hashtable logPlanCache;
  //    private Hashtable itineraryCache;
  //  protected AbstractProducer myProducer;
  
    private String host = "localhost";
    private boolean clientMode;

    // comma separated versions to build into above Vectors
    private String clusterList;
    private String allowOrgList;

  // defines default host when dialog comes up.
  private String defaultHostName = "localhost";
  // default root Cluster to ask for its subordinates
  private String rootCluster = "TRANSCOM";
  // are we asking the subordinates PSP for the subords of rootCluster?
  private boolean usingSubordinatesPSP;
  // turn off initial dialog and talk to default host
  private boolean hostPrompt = false;

  private boolean debug = true;

    public ClusterCache(boolean clientMode)
    {
	this(clientMode, null);
    }

    public ClusterCache(boolean clientMode, String clusterList, String allowOrgList)
    {
	host = null;
	clusterNames = null;
	this.clusterList = clusterList;
	this.allowOrgList = allowOrgList;
	this.clientMode = clientMode;
	clusterURLs = new Hashtable();
	//	logPlanCache = new Hashtable();
	//	itineraryCache = new Hashtable();
	Debug.out("CC:CC leave");
    }

  /** dummy required to have a distinct signature */
  public ClusterCache(boolean clientMode, String allowOrgList) {
	usingSubordinatesPSP = true;
	clusterNames = null;
	//	this.clusterList = clusterList;
	this.allowOrgList = allowOrgList;
	this.clientMode = clientMode;
	clusterURLs = new Hashtable();
	//	logPlanCache = new Hashtable();
	//	itineraryCache = new Hashtable();
	Debug.out("CC:CC leave");

	String clusterNameProp = System.getProperty ("org.cougaar.mlm.ui.newtpfdd.producer.rootCluster");
	if (clusterNameProp != null)
	  rootCluster = clusterNameProp;
	if (debug) 
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.ctor - root cluster now " + rootCluster);
	String defaultHostNameProp = 
	  System.getProperty ("org.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.defaultHostName");
	if (defaultHostNameProp != null) {
	  defaultHostName = defaultHostNameProp;
	  host = defaultHostName;
	}
	else {
	  String cougaarRCHost = getHostFromCougaarRC ();

	  if (cougaarRCHost != null) {
		defaultHostName = cougaarRCHost;
		host = defaultHostName;
	  }
	}
	
	hostPrompt = 
	  "true".equals(System.getProperty ("org.cougaar.mlm.ui.newtpfdd.producer.ClusterCache.hostPrompt", 
										 "true"));
	if (debug && !hostPrompt) 
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.ctor - skipping host dialog, connecting directly to " + defaultHostName);
	if (debug) {
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.ctor - root cluster property " + clusterNameProp + " - if null means use default");
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.ctor - default host " + defaultHostName);
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.ctor - " + ((hostPrompt) ? "showing" : "skipping") + " host prompt.");
	}
  }

  protected String getHostFromCougaarRC () {
	DBConfig dbConfig = new DBConfig ();
	return dbConfig.getHostName();
  }
  
    public String getHost()
    {
	return host;
    }

    public void setHost(String host)
    {
	this.host = host;
    }

  /** 
   * ask the user for the society host name 
   * defaults to localhost
   */
  public String guiSetHost()
  {
	if (!hostPrompt) {
	  host = defaultHostName;
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.guiSetHost - no host prompt, host is " + host);
	  return host;
	}
	
	host = (String)JOptionPane.showInputDialog(null,
											   "Enter cluster proxy server",
											   "Location",
											   JOptionPane.INFORMATION_MESSAGE,
											   null, null, defaultHostName);
	if ( host != null )
	  host = host.trim();
	return host;
  }
    
  //
  // FUnction called by tpfdd client
  // to set the host, ie the aggregation server machine
  //
  public String clientGuiSetHost(String defHost)
  {
	if (!hostPrompt) {
	  host = defaultHostName;
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "ClusterCache.clientGuiSetHost - no host prompt, host is " + host);
	  return host;
	}
	
    host = (String)JOptionPane.showInputDialog(null,
             "Enter Aggregation Server Location","Location",
             JOptionPane.INFORMATION_MESSAGE,
             null, null, defaultHostName);
    if ( host != null )
      host = host.trim();
    return host;
  }
    
  /*
  public Vector getallowOrgNames()
    {
	if ( allowOrgNames == null ) {
	    try {
		allowOrgNames = new Vector();
		if ( allowOrgList != null ) {
		    ClusterList allowParser = new ClusterList(allowOrgList);
		    String[] allowNameArray = allowParser.getNames();
		    for ( int i = 0; i < allowNameArray.length; i++ )
			allowOrgNames.add(allowNameArray[i]);
		}
		Debug.out("CC:gAON Allowing non-carrier itineraries from: " + allowOrgNames);
	    }
	    catch ( Exception e ) {
		OutputHandler.out(ExceptionTools.toString("CC:gAON", e));
	    }
	}
	return allowOrgNames;
    }

  */

  public Vector getclusterNames() {
	if ( clusterNames == null ) {
	  if (usingSubordinatesPSP) {
		setClusterNames ();
		return clusterNames;
	  }
	  /*
	  else {
	    try {
		  clusterNames = new Vector();
		  if ( clientMode )
		    clusterNames.add("Aggregation");
		  else { // aggregation server, talks to real clusters, but configured manually
		    ClusterList parser = new ClusterList(clusterList);
		    String[] nameArray = parser.getNames();
		    for ( int i = 0; i < nameArray.length; i++ )
			  clusterNames.add(nameArray[i]);
		  }
	    }
	    catch ( Exception e ) {
		  OutputHandler.out(ExceptionTools.toString("CC:init", e));
	    }
	  }
	  */
	}
	return clusterNames;
  }

  public Vector determineClusterFromSubordinatesPSP(){
    Vector names = new Vector();
    
    ConnectionHelper helper = 
      new ConnectionHelper (getClusterURL (rootCluster),
			    PSPClientConfig.PSP_package, 
			    PSPClientConfig.SubordinatesPSP_id + "?MODE=1");
    try {
      // talk to PSP
      String response = new String (helper.getResponse());
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "got response " + response);
      // modern xerces jar
      SAXParser parser = new SAXParser();
      // old IBM XML jar
      // Parser parser = ParserFactory.makeParser("com.ibm.xml.parsers.SAXParser");
      // modern xerces jar
      parser.setContentHandler (new ClusterNameHandler(names));
      // old IBM XML jar
      // parser.setDocumentHandler (new ClusterNameHandler(names));

      parser.parse (new InputSource (new StringReader (response)));
    }
    catch ( IOException e ) {
      OutputHandler.out("ClusterCache.setSubordinates - IOError in connection, url was : " + helper + 
			"\nError was: " + e);
    } catch (Exception e) {
      System.err.println (e.getMessage());
      e.printStackTrace();
    }
    return names;
  }

  /**
   *  As a side effect, fills in the clusterNames Vector
   */
  protected void setClusterNames () {
    if (clientMode) {
      clusterNames = new Vector ();
      clusterNames.add("Aggregation");
      return;
    }
    clusterNames=determineClusterFromSubordinatesPSP();
  }

  // for modern Xerces jar
  private class ClusterNameHandler extends DefaultHandler {
  // for old (June 1999) IBM XML jar
  // public static class ClusterNameHandler extends HandlerBase {
    Vector clusterNames;
    public ClusterNameHandler(Vector v){
      clusterNames=v;
    }
	// for modern Xerces jar
        public void startElement (String uri, String local, String name, Attributes atts) throws SAXException {
	// for old (June 1999) IBM XML jar
        // public void startElement (String name, AttributeList atts) {
	  if (name.equals ("org")) {
		clusterNames.add (atts.getValue ("name"));
	  }
	}
  }

    public String getClusterURL(String clusterName)
    {
	return "http://" + host + ":5555/$" + clusterName + "/";
    }
    
    public boolean isValidClusterName(String clusterName)
    {
	return getclusterNames().contains(clusterName);
    }

  /** Test as standalone class  -- gets the names of the subordinates of rootCluster */
  public static void main (String args[]) {
	ClusterCache cache = new ClusterCache (false, "");
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Talked to the psp, and the subords of " + cache.host + " are " + cache.getclusterNames ());
  }
}
