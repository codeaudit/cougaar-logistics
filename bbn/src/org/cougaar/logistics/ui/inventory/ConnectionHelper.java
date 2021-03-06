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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import org.cougaar.util.OptionPane;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import javax.swing.*;

// This utility class could theoretically go in the util module.
// Note that this class is used in the datagrabber module,
// in the SD UI: RelationshipUILauncherFrame.java

/**
 * Creates connection between client and XML Plan Server.
 */
public class ConnectionHelper {
  private URL url;
  private URLConnection connection;
  static final String DebugPSP_package = "alpine/demo";
  static final String DebugPSP_id = "DEBUG.PSP";
  String clusterURL;
  boolean isDebugPSP = false;

  private Logger logger;

  /**
   * If you create a ConnectionHelper instance with 0 params.  must
   * call setConnection()... to intialize connection
   **/
  public ConnectionHelper() {
    logger = Logging.getLogger(this);
  }

  /**
   Connects to the debug PSP at the specified cluster,
   where cluster is specified by URL (host and port).
   */

  public ConnectionHelper(String clusterURL) throws MalformedURLException, IOException {
    this(clusterURL, DebugPSP_package, DebugPSP_id);
    isDebugPSP = true;
    logger = Logging.getLogger(this);
  }


  public ConnectionHelper(String clusterURL, String path) throws MalformedURLException, IOException {
    this.clusterURL = clusterURL;
    logger = Logging.getLogger(this);
    url = new URL(clusterURL + path);
    connection = url.openConnection();
    connection.setDoInput(true);
    connection.setDoOutput(true);

  }

  /**
   Connects to the specified PSP at the specified cluster,
   where cluster is specified by URL (host and port).
   */

  public ConnectionHelper(String clusterURL, String PSP_package, String PSP_id) throws MalformedURLException, IOException {
    this(clusterURL, PSP_package + "/" + PSP_id);
  }


  public void setConnection(String completeURLString) throws Exception {
    if (connection != null) {
      new RuntimeException("ConnectionHelper.connection already set!");
    }
    clusterURL = completeURLString; // messy -- but should be okay for now
    url = new URL(completeURLString);
    connection = url.openConnection();
    connection.setDoInput(true);
    connection.setDoOutput(true);
  }

  /**
   * getURL - returns URL for the connection
   *
   * @return URL for the connection
   */
  public URL getURL() {
    return url;
  }

  public void closeConnection() throws IOException {
    getInputStream().close();
    if (connection instanceof HttpURLConnection) {
      ((HttpURLConnection) connection).disconnect();
    }
  }

  /**
   Sends data on the connection.
   */

  public void sendData(String s) throws IOException {
    ((HttpURLConnection) connection).setRequestMethod("PUT");
    PrintWriter pw = new PrintWriter(connection.getOutputStream());
    pw.println(s);
    pw.flush();
  }

  /**
   Returns input stream for the connection.
   */

  public InputStream getInputStream() throws IOException {
    return connection.getInputStream();
  }

  public URLConnection getConnection() {
    return connection;
  }

  /**
   Sends data and returns response in buffer.
   */

  public byte[] getResponse() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    InputStream is = getInputStream();
    byte b[] = new byte[512];
    int len;
    while ((len = is.read(b, 0, 512)) > -1)
      os.write(b, 0, len);
    return os.toByteArray();
  }

  /**
   Returns a list of cluster identifiers from the debug PSP
   located at the cluster specified in this class's constructor.
   */

  protected Hashtable getClusterIdsAndURLs(java.awt.Component parent,
					   String querySuffix, 
					   String agentPath, 
					   boolean addBackURLs) throws MalformedURLException, ClassNotFoundException, IOException {
    Hashtable results = new Hashtable();

    /*
get host:port from clusterURL
open url connection to host:port/agents?all&text
get input stream from connection
wrap in BufferedReader
read lines of response into temporary ArrayLIst (get agent-names)
fill "results" with (agent-name, http://host:port/$ + agent-name + /)
return results

     */

    int p = clusterURL.lastIndexOf(":");
    URL url = new URL(clusterURL + querySuffix);
    //logger.debug(url.toString());
    connection = null;
    InputStream in;

    boolean triedLocal = false;

    while (results.isEmpty()) {
      try {
        connection = url.openConnection();
        in = connection.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        try {
	  if(addBackURLs) {
	    String iterPath = agentPath;
	    int dotIndex = iterPath.indexOf(".");
	    while((dotIndex >= 0) &&
		  (dotIndex+1 < iterPath.length())) {
	      iterPath = iterPath.substring(dotIndex + 1);
	      String u = ((clusterURL.substring(0, p + 1)) + "8800/$." + iterPath + "/");
	      results.put("." + iterPath,u);
	      dotIndex = iterPath.indexOf(".");
	    }
	    results.put(".",((clusterURL.substring(0, p + 1)) + "8800/$./"));
	  }

          while (input != null) {
            String n = input.readLine();
            if (n == null)
              break;
            String u = ((clusterURL.substring(0, p + 1)) + "8800/$" + n + "/");
            //logger.debug(u);
            results.put(n, u);
          }
          input.close();
        } catch (IOException e) {
          if (logger.isErrorEnabled()) {
            logger.error("Error reading agent list: " + e.getMessage());
          }
        }
      } catch (IOException e) {
        if (logger.isErrorEnabled()) {
          logger.error("Error reaching agents list: " + e.getMessage());
        }

        if ((results.isEmpty()) &&
            (!triedLocal)) {
          int yesOrNo =
              JOptionPane.showConfirmDialog(parent,
                                            "Could not connect to gather all Society Organizations.   Try local servlet?",
                                            "Warning",
                                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

          if (yesOrNo == JOptionPane.YES_OPTION) {
            url = new URL(clusterURL + querySuffix);
            triedLocal = true;
            continue;
          }
        }
      }
      break;
    }

    return results;
  }



  /**
   Returns a list of cluster identifiers from the debug PSP
   located at the cluster specified in this class's constructor.
   */

  public Hashtable getClusterIdsAndURLs(java.awt.Component parent,String agentPath) throws MalformedURLException, ClassNotFoundException, IOException {
      return getClusterIdsAndURLs(parent,
				  "agents?format=text&suffix=" + agentPath,
				  agentPath,
				  true);

  }


  /**
   Returns a list of cluster identifiers from the debug PSP
   located at the cluster specified in this class's constructor.
   */

  public Hashtable getAllClusterIdsAndURLs(java.awt.Component parent) throws MalformedURLException, ClassNotFoundException, IOException {
      return getClusterIdsAndURLs(parent,
				  "agents?format=text",
				  ".",
				  false);
  }



  /**
   * getClusterInfo - get the location of the cluster
   * Only used when running in an application
   */
  public static String getClusterHostPort(java.awt.Component parent) {
    return getClusterHostPort(parent,
                              "Enter location of a cluster.",
                              "localhost",
                              "8800");
  }

  public static String getClusterHostPort(java.awt.Component parent,
                                          Object msgString,
                                          String host,
                                          String port) {
    return (String)
        OptionPane.showInputDialog(parent,
                                   msgString,
                                   "Cluster Location",
                                   OptionPane.QUESTION_MESSAGE,
                                   //null, null, "localhost:5555");
                                   null, null, "http://" + host + ":" + port);

  }

  public static Hashtable getClusterInfo(java.awt.Component parent) {
    return getClusterInfo(parent,
                          "Enter location of a cluster.",
                          "localhost",
                          "8800");
  }

  public static Hashtable getClusterInfo(java.awt.Component parent,
                                         Object msg,
                                         String hostName,
                                         String port) {
    String host = getClusterHostPort(parent, msg, hostName, port);
    if (host == null) {
      return null;
    }
    try {
      host = host.trim();
      if (host.length() == 0) {
        //host = "localhost:5555";
        host = "http://" + "localhost:8800";
      } else {
        if (host.indexOf(':') < 0) {
          //host += ":5555";
          host += ":8800";
        }
      }
      ConnectionHelper connection = new ConnectionHelper(host + "/");
      return connection.getClusterIdsAndURLs(parent,".");
    } catch (Exception e) {
      return null;
    }
  }
}

