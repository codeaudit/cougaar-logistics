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

import java.io.Serializable;

import java.awt.Insets;
import java.awt.Dimension;

import java.awt.Component;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

import java.util.Hashtable;

import javax.swing.JMenuBar;
import javax.swing.JMenu;

import javax.swing.event.InternalFrameEvent;

/***********************************************************************************************************************
<b>Description</b>: This class represents the internal frame components displayed in the Cougaar Desktop.  It is used
                    as the communication medium between the CougaarDesktopUI instance and the Cougaar Desktop which it
                    is being displayed in.

***********************************************************************************************************************/
public class CDesktopFrame extends javax.swing.JInternalFrame implements javax.swing.event.InternalFrameListener, java.beans.VetoableChangeListener
{
  private static Hashtable instanceTitles = new Hashtable(1);

	private CougaarDesktopUI component = null;

  private FrameInfo frameInfo = null;

  private Dimension deiconifiedSize = new Dimension();

  private JMenuBar menuBar = null;
  private boolean menuBarVisible = true;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a CDesktopFrame object.

  <br><b>Notes</b>:<br>
	                  - CDesktopFrame objects are generally constructed by the CougaarDesktop class and do not need to
	                    be instantiated by a user application.

  <br>
  @param desktopPane Desktop pane this frame is to be displayed within
  @param info Frame information such as CougaarDesktopUI object to display, etc.
	*********************************************************************************************************************/
	public CDesktopFrame(CDesktopPane desktopPane, FrameInfo info)
	{
		super(null, true, true, true, true);

    frameInfo = info;
    component = frameInfo.getComponent();

    try
    {
			// Need this to ensure frame is painted when dragged past scroll bars in show window contents while dragging
			// see frameDragged()
			addComponentListener(new ListenerAction(this, "frameDragged", new Object[] {}, ListenerAction.componentMoved));
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      // Add the frame to the desktop
			desktopPane.add(this);

      // Set up the Desktop Frame
      // Insets and minimum sizes compensate for the borders and title bar of the frame itself
      Insets frameInsets = getInsets();
			int width = ((getMinimumSize().width > frameInfo.componentSize.width) ? getMinimumSize().width : frameInfo.componentSize.width) + frameInsets.left + frameInsets.right;
			int height = getMinimumSize().height + frameInfo.componentSize.height;
			setSize(width, height);

  		setSelected(info.selected); // Must be set selected after the frame is added to the desktopPane (bug???)
      setResizable(component.isResizable());
      setTitle(component.getTitle());
	    setIcon(frameInfo.iconified);

      // Call start up methods on the component
  		if (component.isPersistable())
  		{
  		  component.setPersistedData(frameInfo.componentData);
  		}
      component.install(this);

	  	setLocation(frameInfo.frameLocation); // Must set the location after the component is installed (bug???)

      addInternalFrameListener(this);
//      addVetoableChangeListener(this);

			setVisible(true);
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }
	}

	/*********************************************************************************************************************
  <b>Description</b>: Sets the title of this desktop frame.  CougaarDesktopUI components can use this method to change
                      the window title of the desktop frame.

  <br>
  @param title New title of the frame window
	*********************************************************************************************************************/
  public void setTitle(String title)
  {
    if (getTitle() != null)
    {
      instanceTitles.remove(getTitle());
    }
    
    int count = 2;
    title = (title == null || title.length() == 0) ? " " : title;
    String newTitle = title;
    while (instanceTitles.get(newTitle) != null)
    {
      newTitle = title + " (" + count + ")";
      count++;
    }

    instanceTitles.put(newTitle, newTitle);

    super.setTitle(newTitle);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Provides a method for turning on/off the desktop frame's menu bar visibility.

  <br>
  @param visible True if the menu bar should be visible, false otherwise
	*********************************************************************************************************************/
	public void setMenuBarVisible(boolean visible)
	{
	  menuBarVisible = visible;
	  setJMenuBar(menuBar);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Provides a method for setting the desktop frame's menu bar.

  <br>
  @param menuBar Menu bar to display on the desktop frame
	*********************************************************************************************************************/
	public void setJMenuBar(JMenuBar menuBar)
	{
	  this.menuBar = menuBar;

	  if (menuBar != null)
	  {
  	  if (menuBarVisible)
  	  {
  	    super.setJMenuBar(menuBar);
  	  }
  	  else
  	  {
  	    super.setJMenuBar(null);
  	  }
  	  
  	  menuBar.revalidate();
  	}
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the desktop frame's current menu bar.

  <br>
  @return Current menu bar
	*********************************************************************************************************************/
	public JMenuBar getJMenuBar()
	{
		return(menuBar);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
	public void frameDragged()
	{
		repaint();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the Cougaar Desktop instance the desktop frame is displayed within.

  <br>
  @return Cougaar Desktop instance
	*********************************************************************************************************************/
  public CougaarDesktop getDesktop()
  {
    return(((CDesktopPane)getDesktopPane()).getDesktop());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns all of the desktop frames displayed within the Cougaar Desktop instance of the current
                      desktop frame.

  <br>
  @return Array of all desktop frames
	*********************************************************************************************************************/
  public CDesktopFrame[] getAllDesktopFrames()
  {
    CDesktopPane desktopPane = (CDesktopPane)getDesktopPane();
    if (desktopPane != null)
    {
		  return(desktopPane.getAllDesktopFrames());
		}
		
		return(new CDesktopFrame[0]);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Creates a CougaarDesktopUI from the specified parameters.  The new CougaarDesktopUI component
                      will be displayed in a new desktop frame window.

  <br><b>Notes</b>:<br>
	                  - Desktop applications use this method to display secondary application windows

  <br>
  @param factoryName Fully qualified class name of the factory to use to build the CougaarDesktopUI object
  @param data Data used to initialize the component, or null if not needed
  @return CougaarDesktopUI object created
	*********************************************************************************************************************/
  public CougaarDesktopUI createTool(String factoryName, Serializable data)
  {
    return(((CDesktopPane)getDesktopPane()).getDesktop().createTool(factoryName, data));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the frame information of this desktop frame.  This method is generally used only by the
                      Cougaar Desktop when saving a desktop environment.

  <br>
  @return Frame information of the desktop frame
	*********************************************************************************************************************/
  public FrameInfo getFrameInfo()
  {
    getLocation(frameInfo.frameLocation);
    getContentPane().getSize(frameInfo.componentSize);

    // The menu bar takes space away from the content pane
    if (getJMenuBar() != null)
    {
      frameInfo.componentSize.height += getJMenuBar().getSize().height;
    }

    frameInfo.iconified = isIcon();
    frameInfo.selected = isSelected();

		if (component.isPersistable())
		{
		  frameInfo.componentData = component.getPersistedData();
		}
		else
		{
		  frameInfo.componentData = null;
		}

		return(frameInfo);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the CougaarDesktopUI instance of the desktop frame.  This method is generally used
                      only by the Cougaar Desktop when saving a desktop environment and tile managers when arranging
                      desktop windows by type.

  <br>
  @return CougaarDesktopUI instance of the desktop frame
	*********************************************************************************************************************/
  public CougaarDesktopUI getComponent()
  {
    return(frameInfo.getComponent());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameOpened(InternalFrameEvent e)
  {
//    System.out.println("internalFrameOpened");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameClosing(InternalFrameEvent e)
  {
//    System.out.println("internalFrameClosing");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameClosed(InternalFrameEvent e)
  {
//    System.out.println("internalFrameClosed");
    if (getTitle() != null)
    {
      instanceTitles.remove(getTitle());
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameIconified(InternalFrameEvent e)
  {
//    System.out.println("internalFrameIconified");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameDeiconified(InternalFrameEvent e)
  {
//    System.out.println("internalFrameDeiconified");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameActivated(InternalFrameEvent e)
  {
//    System.out.println("internalFrameActivated");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void internalFrameDeactivated(InternalFrameEvent e)
  {
//    System.out.println("internalFrameDeactivated");
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException
  {
  }

  private Dimension viewSize = new Dimension();
	/*********************************************************************************************************************
  <b>Description</b>: Frame listener callback.
	*********************************************************************************************************************/
  public void setMaximum(boolean toMax) throws PropertyVetoException
  {
    if (toMax)
    {
      getSize(deiconifiedSize);
    }

    super.setMaximum(toMax);

    if (toMax)
    {
      setSize(getDesktop().getDesktopViewSize(viewSize));
    }
    else
    {
      setSize(deiconifiedSize);
    }
  }
}
