/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import javax.swing.SwingUtilities;

import java.net.URL;
import java.net.URLConnection;
import java.net.ConnectException;

import org.cougaar.glm.map.MapLocationInfo;

import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;

public class USAReceiveLocations extends Thread
{


  private USAImageMapComponent imageMapComp = null;
  private String clusterName;
  private InputStream is = null;

  public USAReceiveLocations(USAImageMapComponent myComponent, String cluster)
  {
    imageMapComp = myComponent;
    clusterName = new String (cluster); // must include the '$' prefix
  }

  public void run()
	{

	  String clusterURL = new String ("http://localhost:5555/" + clusterName + "/IPSTAT.PSP");

		try
		{

			// Create the connection to the specified URL
			URL url = new URL(clusterURL);
			URLConnection urlCon = url.openConnection();

			// Set the connection parameters
			urlCon.setDoInput(true);
			urlCon.setAllowUserInteraction(false);

			// Connect to the URL and get the stream
			urlCon.connect();

      String streamData = null;
			int dataIndex = 0;

			is = urlCon.getInputStream();

			while ( (streamData = readLine()) != null)
			{

				// Because this is a keep alive PSP, there may be inserted characters (of the form <ACK>)
				// to keep the reader of the stream from closing it during periods of inactivity
				dataIndex = streamData.lastIndexOf("<DATA ");

				if (dataIndex == -1)
				{
					continue;
				}

        String dataString = streamData.substring (dataIndex + 6, streamData.lastIndexOf(">"));

//        System.out.println ("USAReceiveLocations got update for " + dataString);

        imageMapComp.load (dataString);

			}

		}

		catch (ConnectException e)
		{
      System.err.println ("USAReceiveLocations unable to connect to " + clusterURL + ", no icon will be displayed for this cluster");
		}

		catch (Exception e)
		{
      System.err.println ("USAReceiveLocations exception: " + e.toString());
			e.printStackTrace();
		}

  }


  /*********************************************************************************************************************
  <b>Description</b>: Reads a line from the input stream.

  <br><b>Notes</b>:<br>
	                  - This method was created to fix a difference in implementation between Netscape and Internet
	                  	Explorer concerning java.io.Reader.readLine() methods.

  <br>
  @return The first new-line terminated string from the stream or null if the end of the stream is reached or an
  				error occurs
	*********************************************************************************************************************/
	private String readLine() throws InterruptedException
	{
		String buffer = "";
		int ch = -1;

    try
    {
    	// Loop until the end of the line is found and then return
		  while ((ch = is.read()) != -1)
		  {
			  buffer += (char)ch;
			  if ((char)ch == '\n')
			  {
				  return(buffer);
			  }
      }
    }

    catch (Exception e)
    {
    }

		// End of stream or error
		return(null);
	}

}

