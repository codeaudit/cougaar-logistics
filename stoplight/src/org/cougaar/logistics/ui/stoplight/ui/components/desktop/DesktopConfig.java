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

import java.awt.Toolkit;
import java.awt.Rectangle;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.cougaar.logistics.ui.stoplight.ui.components.CFrame;

/***********************************************************************************************************************
<b>Description</b>: This class represents the Cougaar Desktop applicaiton configuration file.  It holds information
                    such as application window size and location as well as last desktop.  The Cougaar Desktop
                    application will load and save its instance of this class automatically.

<br><br><b>Notes</b>:<br>
									- This class is saved and loaded as an ASCII text file

***********************************************************************************************************************/
public class DesktopConfig
{
	/*********************************************************************************************************************
  <b>Description</b>: Pixel increment associated with the up/down arrows of a scroll pane.
	*********************************************************************************************************************/
	public static final int verticalScrollBarUnitIncrement = 50;

	/*********************************************************************************************************************
  <b>Description</b>: Pixel increment associated with the left/right arrows of a scroll pane.
	*********************************************************************************************************************/
	public static final int horizontalScrollBarUnitIncrement = 50;

  private transient CougaarDesktop desktop = null;

  private NVFileReader desktopConfigParameters = null;

	/*********************************************************************************************************************
  <b>Description</b>: X pixel location of the application window.
	*********************************************************************************************************************/
	public int xLocation = 50;

	/*********************************************************************************************************************
  <b>Description</b>: Y pixel location of the application window.
	*********************************************************************************************************************/
	public int yLocation = 50;

	/*********************************************************************************************************************
  <b>Description</b>: Width, in pixels, of the application window.
	*********************************************************************************************************************/
	public int width = Toolkit.getDefaultToolkit().getScreenSize().width - 100;

	/*********************************************************************************************************************
  <b>Description</b>: Height, in pixels, of the application window.
	*********************************************************************************************************************/
	public int height = Toolkit.getDefaultToolkit().getScreenSize().height - 100;

	/*********************************************************************************************************************
  <b>Description</b>: Width, in pixels, of the desktop within the application scroll pane.
	*********************************************************************************************************************/
	public int desktopWidth = 1024;

	/*********************************************************************************************************************
  <b>Description</b>: Height, in pixels, of the desktop within the application scroll pane.
	*********************************************************************************************************************/
	public int desktopHeight = 768;

	/*********************************************************************************************************************
  <b>Description</b>: Indicator as to whether or not to automatically save the current desktop when exiting.
	*********************************************************************************************************************/
	public boolean autoSaveDesktop = false;

	/*********************************************************************************************************************
  <b>Description</b>: Indicator as to whether or not try and make viewing are centered on the selected window
                      location, otherwise, try to move the viewing area a little as possible to put the selected
                      window in full view.  A window is "selected" from the Windows menu.
	*********************************************************************************************************************/
	public boolean snapWindowToCenter = true;

	/*********************************************************************************************************************
  <b>Description</b>: The current look and feel model.
	*********************************************************************************************************************/
  public String currentLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";

	/*********************************************************************************************************************
  <b>Description</b>: The configuration file name of the last desktop to be saved/loaded in the desktop application.
	*********************************************************************************************************************/
	public String currentDesktopFileName = null;

//	public Vector componentFactoryClass = null;

	/*********************************************************************************************************************
  <b>Description</b>: Reads the specified desktop application configuration file and constructs a configuration object
                      from it.

  <br><b>Notes</b>:<br>
	                  - If the file name given does not exist, default values will be used

  <br>
  @param configFileName Configuration file name
  @param desktop Desktop application the configuration file describes
	*********************************************************************************************************************/
	public DesktopConfig(String configFileName, CougaarDesktop desktop)
	{
	  this.desktop = desktop;

	  desktopConfigParameters = new NVFileReader(configFileName, "=");

	  xLocation = desktopConfigParameters.getInt("xLocation", xLocation);
	  yLocation = desktopConfigParameters.getInt("yLocation", yLocation);
	  width = desktopConfigParameters.getInt("width", width);
	  height = desktopConfigParameters.getInt("height", height);
	  desktopWidth = desktopConfigParameters.getInt("desktopWidth", desktopWidth);
	  desktopHeight = desktopConfigParameters.getInt("desktopHeight", desktopHeight);

	  autoSaveDesktop = desktopConfigParameters.getBoolean("autoSaveDesktop", autoSaveDesktop);
	  snapWindowToCenter = desktopConfigParameters.getBoolean("snapWindowToCenter", snapWindowToCenter);

	  currentLookAndFeel = desktopConfigParameters.getString("currentLookAndFeel", currentLookAndFeel);

	  currentDesktopFileName = desktopConfigParameters.getString("currentDesktopFileName", currentDesktopFileName);

//	  componentFactoryClass = desktopConfigParameters.getStringValues("componentFactoryClass", new Vector(0));

//	  ComponentFactoryRegistry.loadFactoryClasses(componentFactoryClass);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Saves the desktop application configuration file with the specified file name.

  <br><b>Notes</b>:<br>
	                  - If the file name given does not exist, it will be created

  <br>
  @param configFileName Configuration file name
	*********************************************************************************************************************/
	public void save(String configFileName)
	{
		Rectangle bounds = desktop.getBounds();
		xLocation = bounds.x;
		yLocation = bounds.y;
		width = bounds.width;
		height = bounds.height;

	  desktopConfigParameters.clear();

    desktopConfigParameters.addValue("xLocation", "" + xLocation);
    desktopConfigParameters.addValue("yLocation", "" + yLocation);
    desktopConfigParameters.addValue("width", "" + width);
    desktopConfigParameters.addValue("height", "" + height);

    desktopConfigParameters.addValue("desktopWidth", "" + desktopWidth);
    desktopConfigParameters.addValue("desktopHeight", "" + desktopHeight);

    desktopConfigParameters.addValue("autoSaveDesktop", "" + autoSaveDesktop);
    desktopConfigParameters.addValue("snapWindowToCenter", "" + snapWindowToCenter);

    desktopConfigParameters.addValue("currentLookAndFeel", desktop.getCurrentLookAndFeel());

    desktopConfigParameters.addValue("currentDesktopFileName", currentDesktopFileName);
/*
	  for (int i=0, isize=componentFactoryClass.size(); i<isize; i++)
	  {
      desktopConfigParameters.addValue("componentFactoryClass", (String)componentFactoryClass.elementAt(i));
	  }
*/
	  desktopConfigParameters.saveNVFile(configFileName, "=");
	}
}
