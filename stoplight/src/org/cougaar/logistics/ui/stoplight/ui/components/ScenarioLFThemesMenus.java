
//Title:        Your Product Name
//Version:      
//Copyright:    Copyright (c) 1999
//Author:       CSE, Ltd.
//Company:      CSE
//Description:  Your description

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.util.Iterator;
import java.awt.*;
import javax.swing.JMenuItem;

import com.bbn.openmap.gui.*;


public class ScenarioLFThemesMenus extends AbstractOpenMapMenu implements MenuBarMenu
{

  public ScenarioLFThemesMenus()
  {
    setText ("Style");
    setMnemonic('y');
    // reuse theme and look and feel selection menus from CFrame
    add((JMenuItem)ScenarioFrame.getCFrameLookAndFeelPulldown());
    add((JMenuItem)ScenarioFrame.getCFrameThemesPulldown());

  }

      /**
     * This method does nothing, but is required as a part of
     * MenuInterface
     */
    public void findAndInit(Iterator it){}
    
    /**
     * This method does nothing, but is required as a part of
     * MenuInterface
     */
    public void findAndUnInit(Iterator it){}


 }
