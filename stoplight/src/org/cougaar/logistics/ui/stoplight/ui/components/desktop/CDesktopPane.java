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

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.plaf.metal.MetalLookAndFeel;

import javax.swing.JLayeredPane;
import javax.swing.JInternalFrame;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

import javax.swing.UIManager;

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop application to display a MDI interface for desktop
                    frames.

<br><br><b>Notes</b>:<br>
									- Tile managers also use this class

***********************************************************************************************************************/
public class CDesktopPane extends javax.swing.JDesktopPane implements javax.swing.Scrollable, javax.swing.SwingConstants
{
	private CougaarDesktop desktop = null;

	private JPanel backgroundPanel = new JPanel();;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a CDesktopPane object assigned to the specified Cougaar Desktop with the specified
                      width and height.

  <br><b>Notes</b>:<br>
	                  - The Cougaar Desktop typically constructs an instance of this class
	                  - The width or height of the pane can be larger than the display area and, as such, will be placed
	                    within a scrollable pane

  <br>
  @param desktop Desktop the pane will be associated with
  @param desktopWidth Width of the desktop in pixels
  @param desktopHeight Height of the desktop in pixels
	*********************************************************************************************************************/
	public CDesktopPane(CougaarDesktop desktop, int desktopWidth, int desktopHeight)
	{
		this.desktop = desktop;

		// Set the total desktop size
		setPreferredSize(new Dimension(desktopWidth, desktopHeight));
		// Set dragging style
//	 	desktop.putClientProperty("JDesktopPane.dragMode", "outline"); // Show window outline while dragging
		putClientProperty("JDesktopPane.dragMode", "faster"); // Show window contents while dragging
		
		backgroundPanel.setSize(desktopWidth, desktopHeight);
		backgroundPanel.setOpaque(false);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the Cougaar Desktop application associated with this pane.

  <br>
  @return Cougaar Desktop application instance
	*********************************************************************************************************************/
  public CougaarDesktop getDesktop()
  {
    return(desktop);
  }

	/*********************************************************************************************************************
  <b>Description</b>: This method is called by the Cougaar Desktop application to set the background image and
                      tile properties of the desktop pane.

  <br>
  @param backgroundImage Absolute file path to the background display image
  @param tiled True if the image should be titled, false otherwise
	*********************************************************************************************************************/
  public void setBackground(String backgroundImage, boolean tiled)
  {
    remove(backgroundPanel);
    backgroundPanel.removeAll();

    if (backgroundImage != null)
    {
  		ImageIcon imageIcon = new ImageIcon(backgroundImage);
  
  		// Add a tiled background image
  		if (tiled)
  		{
        Dimension size = backgroundPanel.getSize();

  			int width = (int)(size.width/imageIcon.getIconWidth());
  			int height = (int)(size.height/imageIcon.getIconHeight());
  			backgroundPanel.setLayout(new GridLayout(height, width));
  			for (int i=0; i<height; i++)
  			{
  				for (int j=0; j<width; j++)
  				{
  					backgroundPanel.add(new JLabel(imageIcon));
  				}
  			}
  		}
  		else // Center the background image on the desktop
  		{
  			backgroundPanel.setLayout(new BorderLayout());
  			backgroundPanel.add(new JLabel(imageIcon), BorderLayout.CENTER);
  		}

  		add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
  	}
	}

	/**
	* When look and feel or theme is changed, this method is called.  It
	* overrides a Cougaar theme color for the desktop.
	* (otherwise resulting color combo is extra harsh).
	*/
	public void updateUI()
	{
	  // Some hack
		if ((desktop != null) && (UIManager.getLookAndFeel() instanceof MetalLookAndFeel) && desktop.getCurrentTheme().getName().startsWith("Cougaar"))
		{
			setBackground(Color.gray);
		}
		else
		{
			setBackground(null);
		}

		super.updateUI();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Scrollable interface callback.
	*********************************************************************************************************************/
	public Dimension getPreferredScrollableViewportSize()
	{
		return(getPreferredSize());
	}

	/*********************************************************************************************************************
  <b>Description</b>: Scrollable interface callback.
	*********************************************************************************************************************/
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return(50);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Scrollable interface callback.
	*********************************************************************************************************************/
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		if (orientation == VERTICAL)
		{
			return(visibleRect.height);
		}
		else
		{
			return(visibleRect.width);
		}
	}

	/*********************************************************************************************************************
  <b>Description</b>: Scrollable interface callback.
	*********************************************************************************************************************/
	public boolean getScrollableTracksViewportWidth()
	{
		return(false);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Scrollable interface callback.
	*********************************************************************************************************************/
	public boolean getScrollableTracksViewportHeight()
	{
		return(false);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns all of the desktop frames displayed within the desktop pane.

  <br>
  @return Array of all desktop frames
	*********************************************************************************************************************/
  public CDesktopFrame[] getAllDesktopFrames()
  {
		JInternalFrame[] ifs = getAllFrames();
		CDesktopFrame[] frameList = new CDesktopFrame[ifs.length];
		for (int i=0; i<ifs.length; i++)
		{
		  frameList[i] = (CDesktopFrame)ifs[i];
		}
		
		return(frameList);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns currently selected desktop frame.

  <br>
  @return Currently selected frame
	*********************************************************************************************************************/
  public CDesktopFrame getSelectedDesktopFrame()
  {
		JInternalFrame selectedFrame = null;
		JInternalFrame[] ifs = getAllFrames();
		for (int i=0; i<ifs.length; i++)
		{
			if (ifs[i].isSelected())
			{
				selectedFrame = ifs[i];
				break;
			}
		}

		return((CDesktopFrame)selectedFrame);
  }
}
