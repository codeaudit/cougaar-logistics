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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

import java.util.Hashtable;
import java.util.Enumeration;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import javax.swing.SwingUtilities;

import java.net.URL;
import java.net.URLConnection;
import java.net.ConnectException;

import org.cougaar.glm.map.MapLocationInfo;

import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;

public class PspIconKeepAlive extends Thread
{

  private PspIconLayerModel layerModel = null;
  private InputStream is = null;

  private boolean ignoreNow = true;

  public boolean enoughAlready = false;

  public PspIconKeepAlive(PspIconLayerModel myModel)
  {
    layerModel = myModel;
  }

  public void run()
	{
	  String clusterURL = null;

		try
		{
      // first get our initial hashtable
      sendRoutedHTML ("all");

      // then start the keep alive for updates

  	  clusterURL = layerModel.uriString + "/LOCATEKEEPALIVE.PSP";
//			System.out.println("connecting to keepalive");

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

      long ignoreTime = System.currentTimeMillis();


			while ( (streamData = readLine()) != null && ! enoughAlready)
			{
      
				// Because this is a keep alive PSP, there may be inserted characters (of the form <ACK>)
				// to keep the reader of the stream from closing it during periods of inactivity
				dataIndex = streamData.lastIndexOf("<DATA ");

				if (dataIndex == -1)
				{
					continue;
				}

        String orgKey = streamData.substring(dataIndex + 6, streamData.lastIndexOf(">"));
//        System.out.println("PspIconKeepAlive got update for " + orgKey);

        if (!ignoreNow)
        {
          sendRoutedHTML(orgKey);
        }
        else
        {
          if ((System.currentTimeMillis() - ignoreTime) > (10 * 1000) )
          {
            ignoreNow = false;
          }
        }
			}

		}

		catch (ConnectException e)
		{
      System.err.println ("PspIconLayer unable to connect to " + clusterURL + ", no realtime icon data available");
		}

		catch (Exception e)
		{
      System.err.println ("PspIconKeepAlive exception: " + e.toString());
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

  private void sendRoutedHTML (String wKey)
  {

    try
    {

//			String uriString = "http://localhost:5555/$MapInfo/LOCATE.PSP";
			String uriString = layerModel.uriString + "/LOCATE.PSP";

//      System.err.println("Attempting to read from URL: "+uriString);
      URL url = new URL(uriString);

      URLConnection urlCon = url.openConnection();

      // Setup the communication parameters with the specifed URL
      urlCon.setDoOutput(true);
      urlCon.setDoInput(true);
      urlCon.setAllowUserInteraction(false);

      // Send the parameter (GET/POST data) to the specified URL
      DataOutputStream server = new DataOutputStream(urlCon.getOutputStream());
      server.writeBytes(wKey);
      server.close();

      InputStream is = urlCon.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(is);

      if ( wKey.equals ("all"))
      {
        Hashtable allOrgMLIs = (Hashtable) ois.readObject();

//        System.out.println ("got Hashtable, allOrgMLIs size is: " + allOrgMLIs.size() );
        // transfer the hashtable to internal storage structure
        layerModel.load (allOrgMLIs);
        layerModel.myOwningLayer.setTime( null );
        SwingUtilities.invokeLater(layerRefresh);
      }

		  else
			{

			  MapLocationInfo mli = (MapLocationInfo) ois.readObject();

			  if(mli != null)
			  {

          synchronized (layerModel.schedImplByUnit)
          {
            layerModel.updateOneMli (mli);
          }

          layerModel.myOwningLayer.setTime (null);

        }

			  System.out.println("PspIconKeepAlive: updated MapLocationInfo for " + wKey);
		  }

    }

		catch (ConnectException e)
		{
       System.err.println ("PspIconKeepAlive: unable to get location info from LocationCluster plugIn");
		}

		catch (Exception e)
		{
      System.err.println ("PspIconKeepAlive sending exception: " + e.toString());
			e.printStackTrace();
		}


   }

   private Runnable layerRefresh = new Runnable()
   {

     public void run()
     {

       if (layerModel.myOwningLayer.getRootPane() != null)
       {

 //        System.out.println ("\nPspIconKeepAlive: looking for mapBean with parent of: " + layerModel.myOwningLayer.getRootPane().toString() );

         ScenarioMapBean mapBean = ScenarioMap.getMapBean( layerModel.myOwningLayer.getRootPane() );
         mapBean.setProjection( mapBean.getProjection() );

       }
       // else there's nothing to refresh

     }
   };

}

