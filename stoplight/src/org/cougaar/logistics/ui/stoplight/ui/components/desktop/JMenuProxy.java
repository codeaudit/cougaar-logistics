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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton when a menu proxy that is to be displayed
                    has a different menu name than what is currently on the Cougaar Desktop applicaiton's menu.  It
                    adds a new menu to the Cougaar Desktop applicaiton current menu bar that represents the currently
                    selected application's menu.

***********************************************************************************************************************/
public class JMenuProxy extends JMenu implements MenuProxy, ActionListener, ChangeListener, PropertyChangeListener, ContainerListener, MenuListener
{
  private JMenu menu = null;
  private Container parent = null;

  private ContainerListener listener = null;

  private Vector componentList = new Vector(0);

  private boolean alwaysDisabled = false;

  public JMenuProxy(JMenu menu, Container parent, ContainerListener listener)
  {
    super(menu.getText());
//    super(menu.getText(), menu.isTearOff());
    setEnabled(menu.isEnabled());
    setIcon(menu.getIcon());
    setMnemonic(menu.getMnemonic());

    this.menu = menu;
    this.parent = parent;
    this.listener = listener;

    parent.add(this);
    
    Component[] components = menu.getMenuComponents();
    for (int i=0; i<components.length; i++)
    {
      try
      {
        MenuProxy component = MenuProxyRegistry.getProxy(components[i], this, listener);
        componentList.add(component);
      }
      catch (Exception e)
      {
        System.out.println(e);
      }
    }
    
    this.addActionListener(this);
    this.addMenuListener(this);
    
    menu.addChangeListener(this);
//    menu.addPropertyChangeListener(this);
    menu.getPopupMenu().addContainerListener(this);
  }

  public void actionPerformed(ActionEvent e)
  {
    menu.doClick();
//    System.out.println("JMenuProxy: " + menu.hashCode());
  }

  public void menuCanceled(MenuEvent e)
  {
  }

  public void menuDeselected(MenuEvent e)
  {
  }

  public void menuSelected(MenuEvent e)
  {
//    menu.doClick();
  }

  public void stateChanged(ChangeEvent e)
  {
    listener.componentAdded(null);
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    listener.componentAdded(null);
  }

  public void componentAdded(ContainerEvent e)
  {
    listener.componentAdded(null);
  }

  public void componentRemoved(ContainerEvent e)
  {
    listener.componentAdded(null);
  }

  public void setAlwaysDisabled(boolean disabled)
  {
    alwaysDisabled = disabled;
    this.setEnabled(menu.isEnabled());
  }

  public void setEnabled(boolean enabled)
  {
    if (alwaysDisabled)
    {
      super.setEnabled(false);
    }
    else
    {
      super.setEnabled(enabled);
    }
  }

  public void dispose()
  {
    for (int i=0, isize=componentList.size(); i<isize; i++)
    {
      ((MenuProxy)componentList.elementAt(i)).dispose();
    }

    this.removeActionListener(this);
    this.removeMenuListener(this);

    menu.removeChangeListener(this);
//    menu.removePropertyChangeListener(this);
    menu.getPopupMenu().removeContainerListener(this);

    parent.remove(this);
  }
}
