/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

/*
  Creates connection between client and Plan Server Providers.
*/


package org.cougaar.mlm.ui.newtpfdd.producer;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Hashtable;

import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;


public class ConnectionHelper
{
    private URL url;
    private URLConnection connection;
    static final String DebugPSP_package = "alpine/demo";
    static final String DebugPSP_id = "DEBUG.PSP";
    String clusterURL;
    boolean isDebugPSP = false;

    /**
       Connects to the debug PSP at the specified cluster, 
       where cluster is specified by URL (host and port).
    */

    public ConnectionHelper(String clusterURL) {
	this(clusterURL, DebugPSP_package, DebugPSP_id);
	isDebugPSP = true;
    }

    /**
       Connects to the specified PSP at the specified cluster, 
       where cluster is specified by URL (host and port).
    */
    public ConnectionHelper(String clusterURL, String PSP_package, String PSP_id)
    {
	this.clusterURL = clusterURL;
	try {
	    url = new URL(clusterURL + PSP_package + "/" + PSP_id);
	    //	    Debug.out("CH:CH url is " + url);
	}
	catch ( MalformedURLException e ) {
	    OutputHandler.out("CH:CH Bad URL: " + url + ": " + e);
	    return;
	}
	try {
	     connection = url.openConnection();
	}
	catch ( IOException e ) {
	    OutputHandler.out("CH:gCIAU Error: opening URL: " + clusterURL + ": " + e);
	    return;
	}
	connection.setDoInput(true);
	connection.setDoOutput(true);
    }

    /**
       Sends data on the connection.
    */
    public void sendData(String s) throws IOException
    {
	OutputStream out = connection.getOutputStream();
	//	Debug.out("CH: sD got output stream");
	PrintWriter pw = new PrintWriter(out);
	pw.println(s);
	pw.flush();
    }

    /**
       Returns input stream for the connection.
    */
    public InputStream getInputStream() throws IOException
    {
	return connection.getInputStream();
    }

    /**
       Sends data and returns response in buffer.
    */
    public byte[] getResponse() throws IOException
    {
	ByteArrayOutputStream os = new ByteArrayOutputStream();
	InputStream is = getInputStream();
	byte b[] = new byte[512];
	int len;
	while( (len = is.read(b,0,512)) > -1 )
	os.write(b, 0, len);
	return os.toByteArray();
    }

    /**
       Returns a list of cluster identifiers from the debug PSP
       located at the cluster specified in this class's constructor.
    */
    public Hashtable getClusterIdsAndURLs()
    {
	ConnectionHelper debugConnection;
	Hashtable results = new Hashtable();

	if ( !isDebugPSP ) {
	    int i = clusterURL.lastIndexOf(":");
	    if ( i == -1 ) {
		OutputHandler.out("CH:gCIAU Bad URL: " + clusterURL);
		return null;
	    }
	    debugConnection = new ConnectionHelper(clusterURL.substring(0, i + 1) + "5555/");
	}
	else
	    debugConnection = this;
	try {
	    debugConnection.sendData("LIST_CLUSTERS");
	}
	catch ( IOException e ) {
	    OutputHandler.out("CH:gCIAU Error: sending LIST_CLUSTERS request: " + e);
	    return null;
	}

	ObjectInputStream ois;
	try {
	    ois = new ObjectInputStream(debugConnection.getInputStream());
	}
	catch ( StreamCorruptedException e ) {
	    OutputHandler.out("CH:gCIAU Error: Corrupted data stream: " + e);
	    return null;
	}
	catch ( IOException e ) {
	    OutputHandler.out("CH:gCIAU Error: data stream: " + e);
	    return null;
	}
	while ( true ) {
	    String nextName, nextURL;
	    try {
		nextName = (String)ois.readObject();
		nextURL = (String)ois.readObject();
	    }
	    catch ( ClassNotFoundException e ) {
		OutputHandler.out("CH:gCIAU Error: (Should NOT happen) readObject failed: " + e);
		return null;
	    }
	    catch ( IOException e ) {
		break; // we're just at the end of the stream
	    }
	    results.put(nextName, nextURL);
	}
    
	return results;
    }

  /**
   * just prints out what the url is
   */
  public String toString () {
	return "" + url;
  }
}
