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

package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.util.StringTokenizer;

import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;
import org.cougaar.util.OptionPane;
import org.cougaar.util.ThemeFactory;

import org.cougaar.logistics.ui.stoplight.ui.components.CFrame;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

public class InventoryChartUI implements CougaarUI
{
  public static InventorySelector inventorySelector = null;

  public static final NSNItemUnits itemUnits = new NSNItemUnits("ItemUnits.txt");

  static int lookAheadDays = 0;
  static boolean tableView = false;

  public InventoryChartUI()
  {
  }

  public void startup(String cluster, String asset, long startTime, long endTime, String inHost, String inPort)
  {
    startup(cluster, asset, startTime, endTime, inHost, inPort, 0);
  }

  public void startup(String cluster, String asset, long startTime, long endTime, String inHost, String inPort, int look)
  {
    //  if cluster is null bring up default table
    //  if asset is null tell selector to show first asset
    //  otherwise pass start and end time to selector

    lookAheadDays = look;
    boolean displayFirstAsset = false;
    String fileName = null;
    String clusterHost = inHost;
    String clusterPort = inPort;
    String hp = clusterHost + ":" + clusterPort;
    if(cluster == null)
      callTheSelector(null, hp, cluster, asset, false, startTime, endTime);
    else if(asset == null)
    {
      displayFirstAsset = true;
      callTheSelector(null, hp, cluster, asset, true, startTime, endTime);
    }
    else
      callTheSelector(null, hp, cluster, asset, false, startTime, endTime);
  }


  public void populate(String cluster, String port)
  {
    if(inventorySelector != null)
      inventorySelector.populate(cluster, port);
  }

  /** If called with an argument, then it is the data filename.
   */
  public static void main(String[] args)
  {
    //ThemeFactory.establishMetalTheme();
    boolean buildFile = false;
    String fileName = null;
    String hp = null;
    String clusterHost = "localhost";
    String clusterPort = "8800";
    String thisCluster = null;
    String thisAsset = null;
    long startTime = 0;
    long endTime = 0;
    int argumentSelector = 0;
    String argument = null;
    if (args.length != 0)
       argument = (String)args[argumentSelector].toUpperCase();

    while(argumentSelector < args.length)
    {
      argument = (String)args[argumentSelector].toUpperCase();
      if(argument.equals("F"))
      {
        hp = clusterHost + ":" + clusterPort;
        fileName = args[argumentSelector + 1];
        argumentSelector += 2;
      }

      else if(argument.equals("H"))
      {
        hp = args[argumentSelector + 1];
        argumentSelector += 2;
      }
      else if(argument.equals("L"))
      {
        lookAheadDays = (new Integer(args[argumentSelector + 1])).intValue();
        argumentSelector += 2;
      }
      else if(argument.equals("T"))
      {
        tableView = true;
        argumentSelector += 1;
        
      }
      else if(argument.equals("C"))
      {
        String clusterAsset = args[argumentSelector + 1];
        StringTokenizer st = new StringTokenizer (args[argumentSelector + 1], ".");
        thisCluster = st.nextToken();
        thisAsset = st.nextToken();
        argumentSelector += 2;
      }
      else
      {
        System.out.println("Invalid Argument - " + args[argumentSelector]);
        break;
      }

    }

    callTheSelector(fileName, hp, thisCluster, thisAsset , false, startTime, endTime);

  }

  public static void callTheSelector(String file, String hp, boolean buildFile)
  {
    long startTime = 0;
    long endTime = 0;
    callTheSelector (file, hp, null, null, false, startTime, endTime);
  }

  public static void callTheSelector(String file, String hp, String cluster, String asset, boolean displayFirstAsset, long start, long end)
  {

      String clusterHost = null;
      String clusterPort = null;
      String msg = "Enter cluster Log Plan Server location as host:port";
      String s = null;
      if(hp == null)
         s = getClusterHostPort(clusterHost + ":" + clusterPort, msg);
      else
        s = hp;

     if(s != null)
     {
        s = s.trim();
        if (s.length() != 0)
        {
          int i = s.indexOf(":");
          if (i != -1)
          {
            clusterHost = s.substring(0, i);
            clusterPort = s.substring(i+1);
          }
        }
      }

     
     CFrame frame = new CFrame("Inventory/Capacity", true);
     inventorySelector = new InventorySelector(clusterHost, clusterPort, file, cluster, asset, start, end);
     inventorySelector.install(frame);
     
  }

  public static String getClusterHostPort(String defaultString, Object msg)
  {
    return null;
  }

  public void install(JFrame frame)
  {
    lookAheadDays = 4;
    inventorySelector = new InventorySelector(null, null, null, null, null, 0, 0);
    inventorySelector.install(frame);
  }

  public void install(JInternalFrame frame)
  {
    lookAheadDays = 4;
    inventorySelector = new InventorySelector(null, null, null, null, null, 0, 0);
    inventorySelector.install(frame);
  }

  public boolean supportsPlaf()
  {
    return(inventorySelector.supportsPlaf());
  }
}
