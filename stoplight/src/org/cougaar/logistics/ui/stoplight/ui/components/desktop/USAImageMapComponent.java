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
import java.util.Vector;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.net.URL;
import java.net.URLConnection;
import java.net.ConnectException;

import java.awt.*;
import java.awt.datatransfer.*;

import java.io.Serializable;
import java.io.InputStream;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CDesktopFrame;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;

public class USAImageMapComponent extends org.cougaar.logistics.ui.stoplight.ui.components.desktop.ComponentFactory implements org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI, DragSource
{

  private JTextField textField = new JTextField();
  public Hashtable mapLocationObjects = new Hashtable();

  // Drag & Drop supporting class
  private DragAndDropSupport dndSupport = new DragAndDropSupport();

  public final static int WINDOW_WIDTH_SIZE = 398;
  public final static int WINDOW_HEIGHT_SIZE = 238;

  private USAImagePanel panel;


  // ------------------- DragSource Interface ----------------------------

  public Vector getSourceComponents()
  {

    Enumeration keyVals = mapLocationObjects.keys();
    Vector components = new Vector(1);

    components.add (panel);

    return(components);

  }

  public boolean dragFromSubComponents()
  {
    return(true);
  }

  public Object getData(Component componentAt, Point location)
  {

//    System.out.println ("getData: location is: " + location.x + " " + location.y );

    Collection cVals = mapLocationObjects.values();

    for (Iterator it = cVals.iterator(); it.hasNext() ; )
    {
      ClusterNetworkMetrics cnm = (ClusterNetworkMetrics) it.next();
      if (cnm.pointWithin(location.x, location.y) )
      {
        return cnm;
      }
    }

    return null;

  }

  public void dragDropEnd(boolean success)
  {
  }



  private USAReceiveLocations pspThreads[];

  public void install(CDesktopFrame f)
  {

	  panel = new USAImagePanel(this);

    f.getContentPane().add(panel);

    // Add the drag source
    dndSupport.addDragSource(this);

    hookUpPSPs();

  }


  private void hookUpPSPs()
  {

    // if we have time we'll hook up to alpine/demo/CLUSTER.PSP and get the
    // cluster that way
    Vector clusterNames = getClusterNames();

    pspThreads = new USAReceiveLocations[clusterNames.size()];

    for (int ii = 0; ii < clusterNames.size(); ii ++)
    {
//      System.out.println ("starting PSP to cluster: " + (String) clusterNames.get(ii) );
      pspThreads[ii] = new USAReceiveLocations (this, (String) clusterNames.get(ii));
      pspThreads[ii].start();
    }

  }

  private Vector getClusterNames ()
  {
    Hashtable ht = new Hashtable();
    InputStream is = null;
	  String clusterURL = new String ("http://localhost:5555/alpine/demo/CLUSTERS.PSP");

    Vector retVec = new Vector();

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

			is = urlCon.getInputStream();

      String streamData = new String();

      while (streamData.lastIndexOf("</HTML>") == -1)
      {
        streamData += readLine(is);
      }

//      System.out.println ("Stream data is: " + streamData);
       try
       {
         StringTokenizer st = new StringTokenizer (streamData);

         String throwAway, cName;
         while ( (throwAway = st.nextToken("$")) != null)
         {
           cName = new String(st.nextToken("/"));
//           System.out.println ("clustername is: " + cName);
           // use the hashtable to clear out duplicates
           ht.put (cName, new String());
         }
       }
       catch (java.util.NoSuchElementException nsee)
       {
         // we must be done
       }

       Enumeration en = ht.keys();

       while (en.hasMoreElements())
       {
         retVec.add(en.nextElement());
       }

    }

 		catch (ConnectException e)
		{
      System.err.println ("USAImageMapComponent unable to connect to CLUSTERS.PSP, no cluster icons can be displayed");
		}

		catch (Exception e)
		{
      System.err.println ("USAImageMapComponent exception: " + e.toString());
			e.printStackTrace();
		}

    return retVec;

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
	private String readLine(InputStream is) throws InterruptedException
	{
		String buffer = new String("");
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


	public String getToolDisplayName()
	{
	  return("USA Map Component");
	}

	public CougaarDesktopUI create()
	{
	  return(this);
	}

  public boolean supportsPlaf()
  {
    return(true);
  }

  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public boolean isPersistable()
  {
    return(false);
  }

  public Serializable getPersistedData()
  {
    return(null);
  }

  public void setPersistedData(Serializable data)
  {
  }

  public String getTitle()
  {
    return("USA Image Map");
  }

  public Dimension getPreferredSize()
  {
//    return(new Dimension(395, 238));
    return(new Dimension(WINDOW_WIDTH_SIZE, WINDOW_HEIGHT_SIZE));
  }

  public boolean isResizable()
  {
    return(false);
  }

  public void load (String parseMe)
  {

    synchronized (mapLocationObjects)
    {

      ClusterNetworkMetrics cnm;
      StringTokenizer st = new StringTokenizer (parseMe, ":");

      String firstField = st.nextToken();

      if (!firstField.endsWith("not ready"))
      {
        cnm = (ClusterNetworkMetrics) mapLocationObjects.get(firstField);
        if (cnm == null)
        {
          cnm = new ClusterNetworkMetrics();
          cnm.clusterName = new String (firstField);
          mapLocationObjects.put (cnm.clusterName, cnm);

          cnm.ipAddress = new String ( st.nextToken() );
          cnm.ipSentPackets.add (new String ( st.nextToken()));
          cnm.ipRcvPackets.add (new String ( st.nextToken()) );
          cnm.ipLostPackets.add (new String (st.nextToken()));
          cnm.lat = Double.parseDouble( st.nextToken());
          cnm.lon = Double.parseDouble( st.nextToken());
          cnm.timeStamp.add(new Double (st.nextToken()));
        }

        else
        {
          // we already have the base data so we can ignore most of this
          String whoCares = st.nextToken(); // ip address
          String sentPackStr = new String (st.nextToken());
          String recvPackStr = new String (st.nextToken());
          String lostPackStr = new String (st.nextToken());
          whoCares = st.nextToken();  // latitude
          whoCares = st.nextToken();  // longitude
          Double timeStampDbl = new Double (st.nextToken());

          boolean inserted = false;
          for (int ii = cnm.timeStamp.size() -1; ii >= 0; ii --)
          {
            if ( timeStampDbl.compareTo( (Double) cnm.timeStamp.get(ii)) > 0)
            {
//              inserted = true;
              int insertIdx = ii + 1;

              cnm.timeStamp.add(insertIdx, timeStampDbl);
              cnm.ipLostPackets.add(insertIdx, lostPackStr);
              cnm.ipRcvPackets.add (insertIdx, recvPackStr);
              cnm.ipSentPackets.add(insertIdx, sentPackStr);

              break;
            }
          }

/*
          if (! inserted)
          {
            // must be largest, just append this one
            cnm.timeStamp.add(timeStampDbl);
            cnm.ipLostPackets.add(lostPackStr);
            cnm.ipRcvPackets.add (recvPackStr);
            cnm.ipSentPackets.add(sentPackStr);
          }
*/
/*
System.out.println ("\nSorted results:");
          for (int ii = 0; ii < cnm.timeStamp.size(); ii ++)
          {
            System.out.println ( "\tTime: " + ( (Double) (cnm.timeStamp.get(ii))).toString() );
          }
*/
        }


//        System.out.print ("lon: " + cnm.lon);
//        System.out.println ("\t lat: " + cnm.lat);
        if (cnm.lon < -135.0 || cnm.lon > -50.0 ||
            cnm.lat < 13.0 || cnm.lat > 53.0 )
        {
          cnm.xCoord = 0;
          cnm.yCoord = 0;
        }
        else
        {
          cnm.xCoord = (int) ((( 135.0 + cnm.lon) / 85.0 * (double) USAImageMapComponent.WINDOW_WIDTH_SIZE) + 0.5);
          cnm.yCoord = (int) (((53.0 - cnm.lat) / 40.0 * (double) USAImageMapComponent.WINDOW_HEIGHT_SIZE) + 0.5);
//          System.out.print ("calculated x: " + cnm.xCoord);
//          System.out.println ("\t y: " + cnm.yCoord);
        }
      }

    }
  }

}
