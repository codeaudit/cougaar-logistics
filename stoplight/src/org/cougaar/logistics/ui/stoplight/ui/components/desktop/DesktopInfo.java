/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

import java.util.Vector;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;

/***********************************************************************************************************************
<b>Description</b>: This class represents a desktop configuration file.  It holds information such as current
                    applications open and scroll pane window size and location.

<br><br><b>Notes</b>:<br>
									- This class is saved and loaded as a serialized file

***********************************************************************************************************************/
public class DesktopInfo implements Serializable
{
  private static final String[] factoryList = new String[]
    {
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.DesktopTestComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.NChartUIComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.DnDUIComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.BlackJackUIComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.DnDSourceTestGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.DnDTargetTestGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.DnDFileTargetGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.JTableDnDGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.RemoteFileNodeGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.PspIconComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarSocietyExplorerComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.MenuTestGUI",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.DateCommandSliderComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.USAImageMapComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm.OSMDataEntryTestComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm.OSMDataQueryTestComponent",
      "org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm.OSMDataListenerTestComponent"

    };
  
	/*********************************************************************************************************************
  <b>Description</b>: Name of the desktop.
	*********************************************************************************************************************/
	public String desktopName = "Cougaar Desktop";

	/*********************************************************************************************************************
  <b>Description</b>: Width of the desktop within the Cougaar Desktop application scroll pane.
	*********************************************************************************************************************/
	public int desktopWidth = 1024;

	/*********************************************************************************************************************
  <b>Description</b>: Height of the desktop within the Cougaar Desktop application scroll pane.
	*********************************************************************************************************************/
	public int desktopHeight = 768;

	/*********************************************************************************************************************
  <b>Description</b>: Vertical location of the Cougaar Desktop application scroll pane.
	*********************************************************************************************************************/
	public int scrollPaneVerticalPosition = 0;

	/*********************************************************************************************************************
  <b>Description</b>: Horizontal location of the Cougaar Desktop application scroll pane.
	*********************************************************************************************************************/
	public int scrollPaneHorizontalPosition = 0;

	/*********************************************************************************************************************
  <b>Description</b>: Location of the image to use as the Cougaar Desktop application scroll pane background.
	*********************************************************************************************************************/
	public String backgroundImage = "images/CougaarLogo.gif";

	/*********************************************************************************************************************
  <b>Description</b>: Indicator as to whether or not tile the Cougaar Desktop application scroll pane background image.
	*********************************************************************************************************************/
	public boolean tiledBackground = false;

	/*********************************************************************************************************************
  <b>Description</b>: List of all of the available component/application factories.
	*********************************************************************************************************************/
	public Vector componentFactories = new Vector(0);

	/*********************************************************************************************************************
  <b>Description</b>: List of all of the available component/application factories.
	*********************************************************************************************************************/
  public FrameInfo[] frameInfoList = null;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a desktop configuration instance with default settings.
	*********************************************************************************************************************/
  public DesktopInfo()
  {
  	for (int i=0; i<factoryList.length; i++)
    {
      componentFactories.add(factoryList[i]);
    }
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Saves (serializes) this configuration file as the specified file name.

  <br>
  @param fileName File name to save configuration to
	*********************************************************************************************************************/
	public void save(String fileName) throws IOException
	{
  	FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.close();
		fos.close();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Loads (deserializes) this configuration file from the specified file name.

  <br>
  @param fileName File name to load configuration from
  @return Desktop configuration object
	*********************************************************************************************************************/
	public static DesktopInfo load(String fileName) throws IOException
	{
	  DesktopInfo desktopInfo = null;

	  try
	  {
  		FileInputStream fis = new FileInputStream(fileName);
  		ObjectInputStream ois = new ObjectInputStream(fis);
      desktopInfo = (DesktopInfo)ois.readObject();
      fis.close();
      ois.close();
    }
    // We're already in the class so this exception will never occur
    catch (ClassNotFoundException e)
    {
    }

		return(desktopInfo);
	}
}
