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

import javax.swing.SwingUtilities;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

import javax.swing.JScrollPane;
import javax.swing.JLayeredPane;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

import javax.swing.UIManager;

import java.awt.Toolkit;

import java.awt.Component;
import java.awt.Container;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.AWTEventListener;
import java.awt.event.ContainerListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.AWTEvent;

import javax.swing.event.MouseInputListener;

import java.util.Vector;
import java.util.Hashtable;
import java.util.EventObject;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.FileNotFoundException;


import org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm.ObjectStorageManager;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

/***********************************************************************************************************************
<b>Description</b>: This class represents the Cougaar Desktop application.

***********************************************************************************************************************/
public class CougaarDesktop extends org.cougaar.logistics.ui.stoplight.ui.components.CFrame implements javax.swing.SwingConstants
{
	/*********************************************************************************************************************
  <b>Description</b>: Name of the desktop configuration file.  The configuration file contains information such as
                      desktop screen location/size and last desktop.
	*********************************************************************************************************************/
	public static final String DEFAULT_CONFIG_FILE_NAME = "DefaultCougaarDesktop.config";

  private ObjectStorageManager osm = new ObjectStorageManager();

	private String configFileName = null;

	private CDesktopPane desktopPane = null;
	private JScrollPane scrollPane = null;

  private DesktopConfig desktopConfig = null;
  private DesktopInfo desktopInfo = null;

	private JFileChooser fileChooser = new JFileChooser();
	
	private JMenu windowMenu = null;
	private JMenu toolMenu = null;
	private JMenu propertiesMenu = null;

	private JMenuItem printSelectedMenuItem = null;
	private JMenuItem closeMenuItem = null;
	
	
	// Used to place new frames
	private int count = 0;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a new Cougaar Desktop instance based on the specified configuration file name.

  <br><b>Notes</b>:<br>
	                  - Normally, this class is constructed by the main() method

  <br>
  @param configFileName File name of the desktop application configuration file
	*********************************************************************************************************************/
	public CougaarDesktop(String configFileName) throws Throwable
	{
    this.configFileName = configFileName;
    initCougaarDesktopData();

    setLookAndFeel(desktopConfig.currentLookAndFeel);

		addWindowListener(new ListenerAction(this, "exit", new Object[] {}, ListenerAction.windowClosing));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

//		Toolkit.getDefaultToolkit().addAWTEventListener(new ListenerAction(this, "mouseMoved", new Object[] {}, ListenerAction.eventDispatched, true), AWTEvent.MOUSE_MOTION_EVENT_MASK);

		setContentPane(getDesktopContainer());
		buildDesktopMenuBar();
		setBounds(desktopConfig.xLocation, desktopConfig.yLocation, desktopConfig.width, desktopConfig.height);

    // Must set visible before initializing desktop so the scroll pane slider positions can be set
		setVisible(true);

    resetDesktopTitle();
	  initDesktop(desktopInfo);

    // Need to repaint after initial view of desktop due to refresh problems (bug???)
    repaint();
	}

  // Themes menu needs to be set correctly (regardless if the current L&F is equal to the requested L&F)
  // The L&F menu needs to also be set (radio buttons) to the requested L&F
/*
  public void setLookAndFeel(String laf)
  {
    super.setLookAndFeel(laf);
    themesMenu.setEnabled(laf == metal);
    updateLookAndFeel();
  }*/

  protected void initCougaarDesktopData() throws IOException
  {
	  desktopConfig = new DesktopConfig(configFileName, this);

    if (desktopConfig.currentDesktopFileName == null)
    {
      desktopInfo = new DesktopInfo();
    }
    else
    {
      try
      {
        desktopInfo = DesktopInfo.load(desktopConfig.currentDesktopFileName);
      }
      catch (FileNotFoundException e)
      {
        desktopInfo = new DesktopInfo();
        desktopConfig.currentDesktopFileName = null;
      }
    }
  }

	protected Container getDesktopContainer()
	{
		// Create the desktopPane pane
		desktopPane = new CDesktopPane(this, desktopConfig.desktopWidth, desktopConfig.desktopHeight);

		// Make a scroll pane
		scrollPane = new JScrollPane(desktopPane);
    scrollPane.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
		scrollPane.getVerticalScrollBar().setUnitIncrement(desktopConfig.verticalScrollBarUnitIncrement);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(desktopConfig.horizontalScrollBarUnitIncrement);

		scrollPane.getVerticalScrollBar().getModel().setMinimum(0);
		scrollPane.getVerticalScrollBar().getModel().setMaximum(desktopConfig.desktopHeight);

		scrollPane.getHorizontalScrollBar().getModel().setMinimum(0);
		scrollPane.getHorizontalScrollBar().getModel().setMaximum(desktopConfig.desktopWidth);

		return(scrollPane);
	}

	protected void buildDesktopMenuBar() throws Throwable
	{
		menuBar.remove(fileMenu);
		Component[] components = menuBar.getComponents();
		menuBar.removeAll();

		menuBar.add(getFileMenu());
		menuBar.add(getWindowMenu());
		menuBar.add(getToolMenu());
		menuBar.add(getConfigMenu());

		for (int i=0; i<components.length; i++)
		{
			menuBar.add((JMenu)components[i]);
		}
	}
	
	protected JMenu getFileMenu() throws Throwable
	{
		// Replace the old File menu with the new file menu
		fileMenu.removeAll();
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem menuItem = null;
		JMenu menu = null;


		menuItem = new JMenuItem("New Desktop");
		menuItem.setMnemonic(KeyEvent.VK_N);
		menuItem.addActionListener(new ListenerAction(this, "newDesktopDialog", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(menuItem);

    fileMenu.addSeparator();

		menuItem = new JMenuItem("Load Desktop");
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.addActionListener(new ListenerAction(this, "loadDesktopDialog", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(menuItem);


		menuItem = new JMenuItem("Save Desktop");
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.addActionListener(new ListenerAction(this, "saveDesktopDialog", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(menuItem);


		menuItem = new JMenuItem("Save Desktop As ...");
		menuItem.setMnemonic(KeyEvent.VK_A);
		menuItem.addActionListener(new ListenerAction(this, "saveDesktopAsDialog", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(menuItem);


    fileMenu.addSeparator();


		menu = new JMenu("Print...");
		menu.setMnemonic(KeyEvent.VK_P);
		fileMenu.add(menu);
		menuItem = new JMenuItem("Entire Desktop");
		menuItem.setMnemonic(KeyEvent.VK_D);
		menuItem.addActionListener(new ListenerAction(this, "printEntireDesktop", new Object[] {}, ListenerAction.actionPerformed));
		menu.add(menuItem);
		printSelectedMenuItem = new JMenuItem("Selected View");
		printSelectedMenuItem.setMnemonic(KeyEvent.VK_V);
		printSelectedMenuItem.addActionListener(new ListenerAction(this, "printSelectedView", new Object[] {}, ListenerAction.actionPerformed));
		menu.add(printSelectedMenuItem);


		closeMenuItem = new JMenuItem("Close Selected Window");
		closeMenuItem.setMnemonic(KeyEvent.VK_C);
		closeMenuItem.addActionListener(new ListenerAction(this, "closeSelectedWindow", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(closeMenuItem);
		
		propertiesMenu = new JMenu("View Properties");
		menuItem = new JMenuItem("All");
		menuItem.addActionListener(new ListenerAction(this, "getPropertiesEntireDesktop", new Object[] {}, ListenerAction.actionPerformed));
		propertiesMenu.add(menuItem);
		fileMenu.add(propertiesMenu);

    fileMenu.addSeparator();


		menuItem = new JMenuItem("Exit");
		menuItem.setMnemonic(KeyEvent.VK_X);
		menuItem.addActionListener(new ListenerAction(this, "exit", new Object[] {}, ListenerAction.actionPerformed));
		fileMenu.add(menuItem);


		return(fileMenu);
	}

	protected JMenu getWindowMenu() throws Throwable
	{
		JMenuItem menuItem = null;
		windowMenu = (JMenu)menuBar.add(new JMenu("Window"));
		windowMenu.setMnemonic(KeyEvent.VK_W);

		menuItem = new JCheckBoxMenuItem("Show Menu On Window");
		menuItem.setMnemonic(KeyEvent.VK_W);
		((JCheckBoxMenuItem)menuItem).setState(true);
		menuItem.addActionListener(new ListenerAction(this, "showMenuOnWindow", new Object[] {menuItem}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem("Snap Selected Window To Center");
		menuItem.setMnemonic(KeyEvent.VK_A);
		((JCheckBoxMenuItem)menuItem).setState(desktopConfig.snapWindowToCenter);
		menuItem.addActionListener(new ListenerAction(this, "snapSelectedWindowToCenter", new Object[] {menuItem}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

		menuItem = new JMenuItem("Tile Horizontal By Type");
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.addActionListener(new ListenerAction(this, "tileHorizontalByClass", new Object[] {}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

		menuItem = new JMenuItem("Tile Vertical By Type");
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.addActionListener(new ListenerAction(this, "tileVerticalByClass", new Object[] {}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

		menuItem = new JMenuItem("Tile Horizontal By Size");
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.addActionListener(new ListenerAction(this, "tileHorizontalBySize", new Object[] {}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

		menuItem = new JMenuItem("Tile Vertical By Size");
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.addActionListener(new ListenerAction(this, "tileVerticalBySize", new Object[] {}, ListenerAction.actionPerformed));
		windowMenu.add(menuItem);

    windowMenu.addSeparator();

		return(windowMenu);
	}

	protected JMenu getToolMenu() throws Throwable
	{
		JMenuItem menuItem = null;
		toolMenu = new JMenu("Tools");
		toolMenu.setMnemonic(KeyEvent.VK_T);

		return(toolMenu);
	}

	protected JMenu getConfigMenu() throws Throwable
	{
		JCheckBoxMenuItem menuItem = null;
		JMenu menu = new JMenu("Config");
		menu.setMnemonic(KeyEvent.VK_C);


		menuItem = new JCheckBoxMenuItem("Auto Save Desktop On Exit");
		menuItem.setMnemonic(KeyEvent.VK_A);
		menuItem.setState(desktopConfig.autoSaveDesktop);
		menuItem.addActionListener(new ListenerAction(this, "setAutoSaveDesktopOnExit", new Object[] {menuItem}, ListenerAction.actionPerformed));
		menu.add(menuItem);


		return(menu);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void snapSelectedWindowToCenter(JCheckBoxMenuItem menuItem)
	{
		desktopConfig.snapWindowToCenter = menuItem.getState();
	}

  private JMenuBar currentWindowMenuBar = null;

  private boolean showWindowMenu = true;

  private Hashtable proxyList = new Hashtable(1);

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void showMenuOnWindow(JCheckBoxMenuItem menuItem)
	{
		showWindowMenu = menuItem.getState();
		
		CDesktopFrame[] frameList = desktopPane.getAllDesktopFrames();
		for(int i=0; i<frameList.length; i++)
		{
		  frameList[i].setMenuBarVisible(showWindowMenu);
		}
		
		showMenus();
	}

  private void showMenus()
  {
    if (showWindowMenu)
    {
      removeMenuProxies();
    }
    else
    {
      CDesktopFrame frame = desktopPane.getSelectedDesktopFrame();
      if (frame != null)
      {
        addMenuProxies(frame.getJMenuBar());
      }
    }
  }

  private ContainerListener currentWMBContainerListener = new ContainerListener()
    {
      public void componentAdded(ContainerEvent e)
      {
        refeshMenuProxies();
      }

      public void componentRemoved(ContainerEvent e)
      {
        refeshMenuProxies();
      }
      
      private void refeshMenuProxies()
      {
        removeMenuProxies();
        CDesktopFrame frame = desktopPane.getSelectedDesktopFrame();
        if (frame != null)
        {
          addMenuProxies(frame.getJMenuBar());
        }
      }
    };

  private void addMenuProxies(JMenuBar menuBarToProxy)
  {
    currentWindowMenuBar = menuBarToProxy;
    if (currentWindowMenuBar != null)
    {
      Component[] menus = currentWindowMenuBar.getComponents();
      for (int i=0; i<menus.length; i++)
      {
        Component parentComponent = getMenu(((JMenu)menus[i]).getText());
        if (parentComponent == null)
        {
          parentComponent = menuBar;
        }

        MenuProxy menuProxy = MenuProxyRegistry.getProxy(menus[i], parentComponent, currentWMBContainerListener);
        
        proxyList.put(menus[i], menuProxy);
      }
    }

    // Need to do this after menu components are added
    currentWindowMenuBar.addContainerListener(currentWMBContainerListener);
    menuBar.addContainerListener(currentWMBContainerListener);

		menuBar.revalidate();
		menuBar.repaint();
  }

  private void removeMenuProxies()
  {
    if (currentWindowMenuBar != null)
    {
      currentWindowMenuBar.removeContainerListener(currentWMBContainerListener);
      menuBar.removeContainerListener(currentWMBContainerListener);

      Component[] menus = currentWindowMenuBar.getComponents();
      for (int i=0; i<menus.length; i++)
      {
        MenuProxy proxy = (MenuProxy)proxyList.remove(menus[i]);
        if (proxy != null)
        {
          proxy.dispose();
        }
      }

      currentWindowMenuBar = null;

  		menuBar.revalidate();
  		menuBar.repaint();
    }
  }

  private JMenu getMenu(String menuName)
  {
    Component[] menus = menuBar.getComponents();
    for (int i=0; i<menus.length; i++)
    {
      // Return only non-JMenuProxy objects
      if ((((JMenu)menus[i]).getText().equals(menuName)) && !(menus[i] instanceof JMenuProxy))
      {
        return((JMenu)menus[i]);
      }
    }

    return(null);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame action callback method.
	*********************************************************************************************************************/
	public void frameActivated(CDesktopFrame frame)
	{
    showMenus();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame action callback method.
	*********************************************************************************************************************/
	public void frameDeactivated(CDesktopFrame frame)
	{
	  if (!showWindowMenu)
	  {
      removeMenuProxies();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Frame action callback method.
	*********************************************************************************************************************/
	public void frameClosed(CDesktopFrame frame)
	{
	  if (!showWindowMenu)
	  {
      removeMenuProxies();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void tileHorizontalByClass()
	{
	  TileManager htm = new HorizontalClassTileManager();
	  htm.tile(desktopPane);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void tileVerticalByClass()
	{
	  TileManager vtm = new VerticalClassTileManager();
	  vtm.tile(desktopPane);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void tileHorizontalBySize()
	{
	  TileManager htm = new HorizontalSizeTileManager();
	  htm.tile(desktopPane);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void tileVerticalBySize()
	{
	  TileManager vtm = new VerticalSizeTileManager();
	  vtm.tile(desktopPane);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void setAutoSaveDesktopOnExit(JCheckBoxMenuItem menuItem)
	{
		desktopConfig.autoSaveDesktop = menuItem.getState();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void newDesktopDialog()
	{
		if ((!desktopConfig.autoSaveDesktop) || (desktopConfig.currentDesktopFileName == null))
		{
			switch (JOptionPane.showConfirmDialog(this, "Save Current Desktop?", "Save Desktop", JOptionPane.YES_NO_CANCEL_OPTION))
			{
				case JOptionPane.YES_OPTION:
					if (!saveDesktopDialog())
					{
					  return;
					}
				break;

				case JOptionPane.NO_OPTION:
				break;

				case JOptionPane.CANCEL_OPTION:
				return;
			}
		}
		else
		{
			if (!saveDesktopDialog())
			{
			  return;
			}
		}

    try
    {
      loadDesktop(null);

      desktopConfig.save(configFileName);
		}
		catch (Throwable t)
		{
		  JOptionPane.showMessageDialog(this, t, "Error Creating New Desktop", JOptionPane.ERROR_MESSAGE);
			t.printStackTrace();
		}
  }

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void loadDesktopDialog()
	{
		if ((!desktopConfig.autoSaveDesktop) || (desktopConfig.currentDesktopFileName == null))
		{
			switch (JOptionPane.showConfirmDialog(this, "Save Current Desktop?", "Save Desktop", JOptionPane.YES_NO_CANCEL_OPTION))
			{
				case JOptionPane.YES_OPTION:
  				if (!saveDesktopDialog())
  				{
  				  return;
  				}
				break;

				case JOptionPane.NO_OPTION:
				break;

				case JOptionPane.CANCEL_OPTION:
				return;
			}
		}
		else
		{
			if (!saveDesktopDialog())
			{
			  return;
			}
		}


    String fileName = null;
		switch (fileChooser.showOpenDialog(this))
		{
			case JFileChooser.APPROVE_OPTION:
				if ((fileName = fileChooser.getSelectedFile().getName()) == null)
				{
          return;
        }
      break;

			case JFileChooser.CANCEL_OPTION:
			  return;
		}

    try
    {
      loadDesktop(fileName);

      desktopConfig.save(configFileName);
		}
		catch (Throwable t)
		{
		  JOptionPane.showMessageDialog(this, t, "Error Loading Desktop", JOptionPane.ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

  protected void loadDesktop(String fileName) throws IOException
  {
    if (fileName == null)
    {
  	  desktopInfo = new DesktopInfo();
    }
    else
    {
      desktopInfo = DesktopInfo.load(fileName);
    }

    closeAllDesktopFrames();

	  desktopConfig.currentDesktopFileName = fileName;
    resetDesktopTitle();
	  initDesktop(desktopInfo);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.  Returns false if the save operation was canceled.
	*********************************************************************************************************************/
	public boolean saveDesktopDialog()
	{
    String fileName = desktopConfig.currentDesktopFileName;
		if (fileName == null)
		{
			switch (fileChooser.showSaveDialog(this))
			{
				case JFileChooser.APPROVE_OPTION:
					fileName = fileChooser.getSelectedFile().getName();
				break;

				case JFileChooser.CANCEL_OPTION:
				return(false);
			}
		}

    try
    {
  		saveDesktop(fileName);
		}
		catch (Throwable t)
		{
		  JOptionPane.showMessageDialog(this, t, "Error Saving Desktop", JOptionPane.ERROR_MESSAGE);
			t.printStackTrace();
		}
		
		return(true);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void saveDesktopAsDialog()
	{
    String fileName = null;
		switch (fileChooser.showSaveDialog(this))
		{
			case JFileChooser.APPROVE_OPTION:
				fileName = fileChooser.getSelectedFile().getName();
			break;

			case JFileChooser.CANCEL_OPTION:
			return;
		}

    try
    {
  		saveDesktop(fileName);
		}
		catch (Throwable t)
		{
		  JOptionPane.showMessageDialog(this, t, "Error Saving Desktop", JOptionPane.ERROR_MESSAGE);
			t.printStackTrace();
		}
	}

	protected void saveDesktop(String fileName) throws IOException
	{
		CDesktopFrame[] frameList = desktopPane.getAllDesktopFrames();
		desktopInfo.frameInfoList = new FrameInfo[frameList.length];

		for(int i=0; i<frameList.length; i++)
		{
		  desktopInfo.frameInfoList[i] = frameList[i].getFrameInfo();
		}

		desktopInfo.scrollPaneVerticalPosition = scrollPane.getVerticalScrollBar().getModel().getValue();
		desktopInfo.scrollPaneHorizontalPosition = scrollPane.getHorizontalScrollBar().getModel().getValue();

		desktopInfo.desktopWidth = desktopPane.getSize().width;
		desktopInfo.desktopHeight = desktopPane.getSize().height;

    desktopInfo.save(fileName);

	  desktopConfig.currentDesktopFileName = fileName;
    desktopConfig.save(configFileName);
		
		resetDesktopTitle();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void exit()
	{
	  try
	  {
		if ((!desktopConfig.autoSaveDesktop) || (desktopConfig.currentDesktopFileName == null))
  		{
  			switch (JOptionPane.showConfirmDialog(this, "Save Current Desktop?", "Exit Desktop", JOptionPane.YES_NO_CANCEL_OPTION))
  			{
  				case JOptionPane.YES_OPTION:
  					if (!saveDesktopDialog())
  					{
  					  return;
  					}
  				break;
  
  				case JOptionPane.NO_OPTION:
  				break;
  
  				case JOptionPane.CANCEL_OPTION:
  				return;
  			}
  		}
  		else
  		{
				if (!saveDesktopDialog())
				{
				  return;
				}
  		}

      desktopConfig.save(configFileName);
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }

    closeAllDesktopFrames();

		System.exit(0);
	}

	protected void initDesktop(DesktopInfo info)
	{
    desktopPane.setBackground(info.backgroundImage, info.tiledBackground);
		desktopPane.setPreferredSize(new Dimension(info.desktopWidth, info.desktopHeight));
		desktopPane.revalidate();

    buildToolMenu();

    if (info.frameInfoList != null)
    {
  		for (int i=0, isize=info.frameInfoList.length; i<isize; i++)
  		{
        addDesktopFrame(info.frameInfoList[i]);
  		}
    }

		scrollPane.getVerticalScrollBar().getModel().setValue(info.scrollPaneVerticalPosition);
		scrollPane.getHorizontalScrollBar().getModel().setValue(info.scrollPaneHorizontalPosition);
  }

	protected void buildToolMenu()
	{
		JMenuItem menuItem = null;
	  toolMenu.removeAll();


	  try
	  {
/*  		menuItem = new JMenuItem("Add ...");
  		menuItem.setMnemonic(KeyEvent.VK_N);
  		menuItem.addActionListener(new ListenerAction(this, "addTool", new Object[] {}, ListenerAction.actionPerformed));
  		toolMenu.add(menuItem);
  
  
      toolMenu.addSeparator();*/
  
  
      String factoryName = null;
  		for (int i=0, isize=desktopInfo.componentFactories.size(); i<isize; i++)
  		{
    	  try
    	  {
    	    factoryName = (String)desktopInfo.componentFactories.elementAt(i);
    			menuItem = new JMenuItem(ComponentFactoryRegistry.getFactory(factoryName).getToolDisplayName());
    			menuItem.addActionListener(new ListenerAction(this, "createTool", new Object[] {factoryName}, ListenerAction.actionPerformed));
    			toolMenu.add(menuItem);
      	}
      	catch (Throwable th)
      	{
      	  th.printStackTrace();
      	}
  		}
  	}
  	catch (Throwable t)
  	{
  	  t.printStackTrace();
  	}
	}
/*
	public void addTool()
	{
		createComponentDialog.showDialog();
		String toolName = createComponentDialog.getName();

		if (toolName != null)
		{
			JMenuItem menuItem = new JMenuItem(toolName);
			menuItem.addActionListener(new ListenerAction(this, "createTool", new Object[] {toolName}, ListenerAction.actionPerformed));
			menu.add(menuItem);
		}

		buildToolMenu();
	}
*/

	/*********************************************************************************************************************
  <b>Description</b>: Builds a CougaarDesktopUI instance from the specified factory and adds it to a desktop frame and
                      displays it .

  <br>
  @param factoryName Name of the factory
  @return Created instance of CougaarDesktopUI object
	*********************************************************************************************************************/
	public CougaarDesktopUI createTool(String factoryName)
	{
	  FrameInfo info = new FrameInfo(factoryName, getNextWindowLocation(), false, true);

    addDesktopFrame(info);
    
    return(info.getComponent());
	}

	/*********************************************************************************************************************
  <b>Description</b>: Builds a CougaarDesktopUI instance from the specified factory with persisted data and adds it to
                      a desktop frame and displays it .

  <br>
  @param factoryName Name of the factory
  @param data Persisted data
  @return Created instance of CougaarDesktopUI object
	*********************************************************************************************************************/
	public CougaarDesktopUI createTool(String factoryName, Serializable data)
	{
	  FrameInfo info = new FrameInfo(factoryName, data, getNextWindowLocation(), false, true);

    addDesktopFrame(info);
    
    return(info.getComponent());
	}

  private Point getNextWindowLocation()
  {
		// Attempt to stagger new desktop frames that do not have a previous location in the currently viewed area
		Rectangle viewLocation = scrollPane.getViewport().getViewRect();
		int pixelIncrement = count*30;
		int x = viewLocation.x + pixelIncrement;
		int y = viewLocation.y + pixelIncrement;

		// Keep at least the title bar of the frame in the visible window
		if ((pixelIncrement > viewLocation.width - 30) || (pixelIncrement > viewLocation.height - 30))
		{
			x = viewLocation.x;
			y = viewLocation.y;
			count = 0;
		}

		count++;
		
		return(new Point(x, y));
  }

	private void addDesktopFrame(FrameInfo info)
	{
	  CDesktopFrame frame = new CDesktopFrame(desktopPane, info);

	  try
	  {
  		JMenuItem menuItem = new JMenuItem(frame.getTitle());
  		menuItem.addActionListener(new ListenerAction(this, "selectWindow", new Object[] {frame}, ListenerAction.actionPerformed));
  		frame.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, new ListenerAction(this, "windowTitleChanged", new Object[] {frame, menuItem}, ListenerAction.propertyChange));
  		frame.addInternalFrameListener(new ListenerAction(windowMenu, "remove", new Object[] {menuItem}, ListenerAction.internalFrameClosed));
  		frame.addInternalFrameListener(new ListenerAction(this, "frameActivated", new Object[] {frame}, ListenerAction.internalFrameActivated));
  		frame.addInternalFrameListener(new ListenerAction(this, "frameDeactivated", new Object[] {frame}, ListenerAction.internalFrameDeactivated));
  		frame.addInternalFrameListener(new ListenerAction(this, "frameClosed", new Object[] {frame}, ListenerAction.internalFrameClosed));
  		windowMenu.add(menuItem);
      
      // for properties menu
      
      JMenuItem PropMenuItem = new JMenuItem(frame.getTitle());
      PropMenuItem.addActionListener(new ListenerAction(this, "getFrameProperties", new Object[] {frame}, ListenerAction.actionPerformed));
      propertiesMenu.add(PropMenuItem);
      
		  frame.setMenuBarVisible(showWindowMenu);
  		showMenus();
  	}
  	catch (Throwable t)
  	{
  	  t.printStackTrace();
  	}

    // Must repaint after adding frames to the desktop to prevent "ghost" frames on the desktop (bug???)
    desktopPane.repaint();
	}

  protected void resetDesktopTitle()
  {
    String fileTitle = " (New)";
    if (desktopConfig.currentDesktopFileName != null)
    {
      fileTitle = " (" + desktopConfig.currentDesktopFileName + ")";
		}

	  setTitle(desktopInfo.desktopName + fileTitle);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
  public void printEntireDesktop()
  {
		printComponent((Component)this);
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
  public void getPropertiesEntireDesktop()
  {
  	CDesktopFrame[] frameList = desktopPane.getAllDesktopFrames();
  	String title = "All Supported Frames";
    String msg = "";
  	for(int i = 0; i < frameList.length; i++)
  	{
  		CDesktopFrame frame = frameList[i];
  		CougaarDesktopUI component = frame.getComponent();
  		if(component instanceof CougaarDesktopPropertiesUI)
  	  {
  	  	//msg += System.getProperty("line.separator");
  	  	msg += '\n';
  	  	msg += '\n';
  	  	msg += frame.getTitle() + " Properties";
  	  	//msg += System.getProperty("line.separator");
  	  	msg += '\n';
  	  	msg += '\n';
  	  	msg += ((CougaarDesktopPropertiesUI)component).getProperties(); 
  	  	msg += '\n';
  	  	msg += "__________________";
  	  }
  	}
  	JOptionPane.showMessageDialog(this, msg, title, JOptionPane.PLAIN_MESSAGE);
  	//System.out.println("get all properties");
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
  public void getFrameProperties(CDesktopFrame frame)
  {
  	CougaarDesktopUI component = frame.getComponent();
  	if(component instanceof CougaarDesktopPropertiesUI)
  	{
  		
  		JOptionPane.showMessageDialog(this, ((CougaarDesktopPropertiesUI)component).getProperties(), frame.getTitle() + " Properties", JOptionPane.PLAIN_MESSAGE);
  		/*System.out.println("result of getproperties " + ((CougaarDesktopPropertiesUI)component).getProperties()); 
  		String msg = ((CougaarDesktopPropertiesUI)component).getProperties();
        String defaultString = "1";
        if ((messageString = OptionPane.showInputDialog(frame, msg, "Chart Number", 3, null, null, defaultString)) == null)
        {
          return;
        }
        */
  	}
  	System.out.println("Get frame properties " + frame.getTitle());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
  public void printSelectedView()
  {
		CDesktopFrame selectedFrame = desktopPane.getSelectedDesktopFrame();
		
		if (selectedFrame != null)
		{
			printComponent((Component)selectedFrame);
		}
  }

	/*********************************************************************************************************************
  <b>Description</b>: Print interface callback method.
	*********************************************************************************************************************/
  public void printComponent(final Component component)
  {
		(new Thread()
			{
				public void run()
				{
					CougaarDesktop.super.printComponent(component);
				}
			}).start();
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void selectWindow(CDesktopFrame frame)
	{
		// Make frame visible (non-icon) and selected and move the viewing area to where the frame is located
		try
		{
			frame.setIcon(false);

      // If the frame is not maximized, we may need to adjust the current view's scroll bars so the selected frame
      // is within the view
      if (!frame.isMaximum())
      {
        // Get the location and size of the selected window frame
        Point location = frame.getLocation();
        Dimension frameSize = frame.getSize();

        // From the frame location and dimensions construct a required viewing area (the area required to be inside
        // the current viewing area to keep the scroll bars from being adjusted)
        Rectangle viewArea = new Rectangle(location, frameSize);
        // The required size of view area is the title bar size minus 2 times the minimum width of the frame
        // The required location of the view area is the current y position and the current x position plus the minimum
        // width of the frame
        // (This formula insures a good chunk of the title bar must be displayed)
        if (frameSize.width > frame.getMinimumSize().width)
        {
          viewArea.x = viewArea.x + frame.getMinimumSize().width;
          viewArea.width = viewArea.width - frame.getMinimumSize().width;
        }
        viewArea.height = frame.getMinimumSize().height;

        // If the current viewing area does not completely contain the required viewing area, move the scroll bars
        // to place the frame within the desired view
        Rectangle currentView = scrollPane.getViewport().getViewRect();
        if (!currentView.contains(viewArea))
        {

          // Try to make viewing are centered on the selected window location
          if (desktopConfig.snapWindowToCenter)
          {
            location.x = (currentView.width < frameSize.width) ? location.x : location.x - (currentView.width - frameSize.width)/2;
            location.y = (currentView.height < frameSize.height) ? location.y : location.y - (currentView.height - frameSize.height)/2;
          }
          else // Try to move the viewing area a little as possible to put the selected window in full view
          {
            // Window is to the right and its width is smaller than the width of the viewing area
            if ((location.x > currentView.x) && (currentView.width > frameSize.width))
            {
              // If the selected frame is not within current viewing area's x range, adjust the viewing area
              if ((location.x + frameSize.width) > (currentView.x + currentView.width))
              {
                location.x = currentView.x + ((location.x + frameSize.width) - (currentView.x + currentView.width));
              }
              else // Otherwise, keep the viewing area's x position the same
              {
                location.x = currentView.x;
              }
            }

            // Window is to the bottom and its height is smaller than the height of the viewing area
            if ((location.y > currentView.y) && (currentView.height > frameSize.height))
            {
              // If the selected frame is not within current viewing area's y range, adjust the viewing area
              if ((location.y + frameSize.height) > (currentView.y + currentView.height))
              {
                location.y = currentView.y + ((location.y + frameSize.height) - (currentView.y + currentView.height));
              }
              else // Otherwise, keep the viewing area's y position the same
              {
                location.y = currentView.y;
              }
            }
          }

          // Set the new location of the viewing area
  				scrollPane.getHorizontalScrollBar().getModel().setValue(location.x);
  				scrollPane.getVerticalScrollBar().getModel().setValue(location.y);
        }
      }

      // Set the frame as selected
			frame.setSelected(true);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void windowTitleChanged(CDesktopFrame frame, JMenuItem menuItem)
  {
    menuItem.setText(frame.getTitle());
  }

  protected void closeAllDesktopFrames()
  {
		CDesktopFrame[] frameList = desktopPane.getAllDesktopFrames();
		for(int i=0; i<frameList.length; i++)
		{
			try
			{
				frameList[i].setClosed(true);
			}
  		catch (Throwable t)
  		{
  			t.printStackTrace();
  		}
  	}
	}

	/*********************************************************************************************************************
  <b>Description</b>: Menu callback method.
	*********************************************************************************************************************/
	public void closeSelectedWindow()
	{
		if (desktopPane.getSelectedDesktopFrame() != null)
		{
			try
			{
				desktopPane.getSelectedDesktopFrame().setClosed(true);
			}
  		catch (Throwable t)
  		{
  			t.printStackTrace();
  		}
		}
	}



/*	public void mouseMoved(EventObject e)
	{
		Component component = (Component)e.getSource();
		// Title bar type of internal frame
		if (component instanceof BasicInternalFrameTitlePane)
		{
			Point point = ((MouseEvent)e).getPoint();
			point = SwingUtilities.convertPoint(component, point, scrollPane.getViewport());

			if (!inBounds(point))
			{
				System.out.println("Mouse outside: " + point);
				scrollPane.getVerticalScrollBar().getModel().setValue(scrollPane.getVerticalScrollBar().getModel().getValue() + 30);
				scrollPane.getHorizontalScrollBar().getModel().setValue(scrollPane.getHorizontalScrollBar().getModel().getValue() + 30);
				scrollPane.getViewport().repaint();
			}
		}
	}

	private boolean inBounds(Point point)
	{
		Dimension size = scrollPane.getViewport().getSize();
		
		return((point.x >= 0) && (point.y >=0) && (point.x <= size.width) && (point.y <= size.height));
	}*/

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current desktop size using the provided Dimension object.

  <br>
  @param dim Dimension object to store the result in
  @return Current imensions of the desktop
	*********************************************************************************************************************/
  public Dimension getDesktopViewSize(Dimension dim)
  {
		Rectangle viewLocation = scrollPane.getViewport().getViewRect();
		dim.width = viewLocation.width;
		dim.height = viewLocation.height;

		return(dim);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the Object Storage Manager.  The OSM is used to send data between applications and to
                      provide object storage/query support.

  <br>
  @return OSM instance
	*********************************************************************************************************************/
	public ObjectStorageManager getOSM()
	{
	  return(osm);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current theme.

  <br>
  @return Current theme
	*********************************************************************************************************************/
	public DefaultMetalTheme getCurrentTheme()
	{
		return(currentTheme);
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current look and feel.

  <br>
  @return Current look and feel
	*********************************************************************************************************************/
	public String getCurrentLookAndFeel()
	{
    return(UIManager.getLookAndFeel().getClass().getName());
	}


	/*********************************************************************************************************************
  <b>Description</b>: Main method for starting the Cougaar Desktop application.
	*********************************************************************************************************************/
	public static void main(String[] args)
	{
		try
		{
			CougaarDesktop frame = new CougaarDesktop(DEFAULT_CONFIG_FILE_NAME);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
}
