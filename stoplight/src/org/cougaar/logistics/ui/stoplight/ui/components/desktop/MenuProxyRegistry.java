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
<b>Description</b>: This class creates a MenuProxy object to be used by the Cougaar Desktop applicaiton for the
                    specified menu component.  The Cougaar Desktop applicaiton uses this registry to provide a seemless
                    transistion from displaying the current application's menu bar on its own window frame and
                    displaying the same application's menu on the Cougaar Desktop applicaiton menu bar.

***********************************************************************************************************************/
public class MenuProxyRegistry
{
  public static MenuProxy getProxy(Component component, Component parentComponent, ContainerListener listener)
  {
    // EBM: Twisted, but works!

    if (component instanceof JMenu)
    {
      if (parentComponent instanceof JMenuProxy)
      {
        return(new JMenuProxy((JMenu)component, (JMenu)parentComponent, listener));
      }
      else if (parentComponent instanceof JMenuExtender)
      {
        return(new JMenuProxy((JMenu)component, ((JMenuExtender)parentComponent).getParentMenu(), listener));
      }
      else if (parentComponent instanceof JMenuBar)
      {
        return(new JMenuProxy((JMenu)component, (JMenuBar)parentComponent, listener));
      }
      // JMenuExtenders will ONLY be added to JMenus, not to JMenuProxy objects
      else if (parentComponent instanceof JMenu)
      {
        return(new JMenuExtender((JMenu)component, (JMenu)parentComponent, listener));
      }
    }

    if (component instanceof JCheckBoxMenuItem)
    {
      if (parentComponent instanceof JMenu)
      {
        return(new JCheckBoxMenuItemProxy((JCheckBoxMenuItem)component, (JMenu)parentComponent, listener));
      }
      else if (parentComponent instanceof JMenuExtender)
      {
        return(new JCheckBoxMenuItemProxy((JCheckBoxMenuItem)component, ((JMenuExtender)parentComponent).getParentMenu(), listener));
      }
    }

    if (component instanceof JRadioButtonMenuItem)
    {
      if (parentComponent instanceof JMenu)
      {
        return(new JRadioButtonMenuItemProxy((JRadioButtonMenuItem)component, (JMenu)parentComponent, listener));
      }
      else if (parentComponent instanceof JMenuExtender)
      {
        return(new JRadioButtonMenuItemProxy((JRadioButtonMenuItem)component, ((JMenuExtender)parentComponent).getParentMenu(), listener));
      }
    }

    if (component instanceof JSeparator)
    {
      if (parentComponent instanceof JMenu)
      {
        return(new JSeparatorProxy((JSeparator)component, (JMenu)parentComponent, listener));
      }
      else if (parentComponent instanceof JMenuExtender)
      {
        return(new JSeparatorProxy((JSeparator)component, ((JMenuExtender)parentComponent).getParentMenu(), listener));
      }
    }

    if (component instanceof JMenuItem)
    {
      if (parentComponent instanceof JMenu)
      {
        return(new JMenuItemProxy((JMenuItem)component, (JMenu)parentComponent, listener));
      }
      else if (parentComponent instanceof JMenuExtender)
      {
        return(new JMenuItemProxy((JMenuItem)component, ((JMenuExtender)parentComponent).getParentMenu(), listener));
      }
    }
    
    String parentInfo = " Parent Component is null";
    if (parentComponent != null)
    {
      parentInfo = parentComponent.getClass() + ":" + parentComponent;
    }

    throw(new RuntimeException("Menu Component Types not supported: " + component.getClass() + ":" + component + "\n                               and: " + parentInfo));
  }
}

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton when a menu proxy that is to be displayed
                    has the same menu name (such as the File menu) as the Cougaar Desktop applicaiton's menu.  It adds
                    a custom separator label to the current Cougaar Desktop applicaiton menu and adds the menu options
                    from the application to the end of the current menu.

***********************************************************************************************************************/
class JMenuExtender extends JComponent implements MenuProxy, ActionListener, ChangeListener, PropertyChangeListener, ContainerListener, MenuListener
{
  protected Color shadow;
  protected Color highlight;
  protected Font font;

  private JMenu menuToProxy = null;
  private JMenu parentMenu = null;

  private ContainerListener listener = null;

  private Vector componentList = new Vector(0);

  private String text = "Current Window";
  private Dimension preferredSize = new Dimension(0, 0);

  public JMenuExtender(JMenu menuToProxy, JMenu parentMenu, ContainerListener listener)
  {
    this.menuToProxy = menuToProxy;
    this.parentMenu = parentMenu;
    this.listener = listener;

    parentMenu.add(this);

    Component[] components = menuToProxy.getMenuComponents();
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

    setComponentsAlwaysDisabled(!menuToProxy.isEnabled());

    parentMenu.addActionListener(this);
    parentMenu.addMenuListener(this);

    menuToProxy.addChangeListener(this);
//    menu.addPropertyChangeListener(this);
    menuToProxy.getPopupMenu().addContainerListener(this);

    shadow = UIManager.getColor("Separator.shadow");
    highlight = UIManager.getColor("Separator.highlight");
    font = UIManager.getFont("MenuBar.font");
  }

  private void setComponentsAlwaysDisabled(boolean disabled)
  {
    for (int i=0, isize=componentList.size(); i<isize; i++)
    {
      ((MenuProxy)componentList.elementAt(i)).setAlwaysDisabled(disabled);
    }
  }

  public void actionPerformed(ActionEvent e)
  {
    menuToProxy.doClick();
//    System.out.println("JMenuExtender: " + menuToProxy.hashCode());
  }

  public void menuCanceled(MenuEvent e)
  {
  }

  public void menuDeselected(MenuEvent e)
  {
  }

  public void menuSelected(MenuEvent e)
  {
//    menuToProxy.doClick();
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

  public void dispose()
  {
    for (int i=0, isize=componentList.size(); i<isize; i++)
    {
      ((MenuProxy)componentList.elementAt(i)).dispose();
    }

    parentMenu.remove(this);
    parentMenu.removeActionListener(this);
    parentMenu.removeMenuListener(this);

    menuToProxy.removeChangeListener(this);
//    menuItem.removePropertyChangeListener(this);
    menuToProxy.getPopupMenu().removeContainerListener(this);
  }

  public void setAlwaysDisabled(boolean disabled)
  {
  }

  public JMenu getParentMenu()
  {
    return(parentMenu);
  }

  public void paintComponent(Graphics g)
  {
    Rectangle s = g.getClipBounds();
  	drawGroove(g, 0, 0, s.width, s.height, shadow, highlight);

    Font f = g.getFont();  	
    Color c = g.getColor();  	
  	g.setFont(font);
  	g.setColor(Color.blue);
  	
  	FontMetrics fm = g.getFontMetrics(font);
  	int baseLine = fm.getDescent();
  	Rectangle bounds = fm.getStringBounds(text, g).getBounds();

  	g.drawString(text, s.width/2 - bounds.width/2, s.height - ((s.height - bounds.height)/2 + baseLine));

  	g.setFont(f);
  	g.setColor(c);
  }

  public Dimension getPreferredSize()
  { 
//    return(preferedSize);
    BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = bi.createGraphics();
    Rectangle bounds = font.getStringBounds(text, g.getFontRenderContext()).getBounds();
    
    preferredSize.width = bounds.width;
//    preferredSize.height = bounds.height;

    JMenuItem b = (JMenuItem)parentMenu;
    ButtonModel model = b.getModel();
    preferredSize.height = b.getHeight();

    Insets insets = b.getInsets();
    preferredSize.width += insets.left;
    preferredSize.height += insets.top;
    preferredSize.width += insets.right;
    preferredSize.height += insets.bottom;

//System.out.println(bounds);
//System.out.println(preferredSize);

    return(preferredSize);
  }

  public Dimension getMinimumSize(JComponent c)
  {
    return null;
  }

  public Dimension getMaximumSize(JComponent c)
  {
    return null;
  }

  public static void drawGroove(Graphics g, int x, int y, int w, int h, Color shadow, Color highlight)
  {
      Color oldColor = g.getColor();  // Make no net change to g
      g.translate(x, y);

      g.setColor(shadow);
      g.drawRect(0, 0, w-2, h-2);

      g.setColor(highlight);
      g.drawLine(1, h-3, 1, 1);
      g.drawLine(1, 1, w-3, 1);

      g.drawLine(0, h-1, w-1, h-1);
      g.drawLine(w-1, h-1, w-1, 0);

      g.translate(-x, -y);
      g.setColor(oldColor);
  }
}



/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton to implement a proxy for a menu item.

***********************************************************************************************************************/
class JMenuItemProxy extends JMenuItem implements MenuProxy, ActionListener, ChangeListener, PropertyChangeListener
{
  private JMenuItem menuItem = null;
  private JMenu parentMenu = null;

  private ContainerListener listener = null;

  private boolean alwaysDisabled = false;

  public JMenuItemProxy(JMenuItem menuItem, JMenu parentMenu, ContainerListener listener)
  {
    super(menuItem.getText(), menuItem.getIcon());
    setEnabled(menuItem.isEnabled());
    setMnemonic(menuItem.getMnemonic());

    this.menuItem = menuItem;
    this.parentMenu = parentMenu;
    this.listener = listener;

    parentMenu.add(this);
    
    this.addActionListener(this);

    menuItem.addChangeListener(this);
//    menuItem.addPropertyChangeListener();
  }

  public void actionPerformed(ActionEvent e)
  {
    menuItem.doClick();
//    System.out.println("Proxy: " + menuItem.hashCode());
  }

  public void stateChanged(ChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
  }

  public void setAlwaysDisabled(boolean disabled)
  {
    alwaysDisabled = disabled;
    this.setEnabled(menuItem.isEnabled());
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
    menuItem.removeChangeListener(this);
//    menuItem.removePropertyChangeListener(this);
    parentMenu.remove(this);
  }
}

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton to implement a proxy for a menu check box
                    item.

***********************************************************************************************************************/
class JCheckBoxMenuItemProxy extends JCheckBoxMenuItem implements MenuProxy, ActionListener, ChangeListener, PropertyChangeListener
{
  private JCheckBoxMenuItem menuItem = null;
  private JMenu parentMenu = null;

  private ContainerListener listener = null;

  private boolean alwaysDisabled = false;

  public JCheckBoxMenuItemProxy(JCheckBoxMenuItem menuItem, JMenu parentMenu, ContainerListener listener)
  {
    super(menuItem.getText(), menuItem.getIcon());
    setEnabled(menuItem.isEnabled());
    setMnemonic(menuItem.getMnemonic());
    setSelected(menuItem.isSelected());
    
    this.menuItem = menuItem;
    this.parentMenu = parentMenu;
    this.listener = listener;

    parentMenu.add(this);

    this.addActionListener(this);

    menuItem.addChangeListener(this);
//    menuItem.addPropertyChangeListener();
  }

  public void actionPerformed(ActionEvent e)
  {
    menuItem.doClick();
//    System.out.println("Proxy: " + menuItem.hashCode());
  }

  public void stateChanged(ChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
    setSelected(menuItem.isSelected());
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
  }

  public void setAlwaysDisabled(boolean disabled)
  {
    alwaysDisabled = disabled;
    this.setEnabled(menuItem.isEnabled());
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
    menuItem.removeChangeListener(this);
//    menuItem.removePropertyChangeListener(this);
    parentMenu.remove(this);
  }
}

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton to implement a proxy for a menu radio button
                    item.

***********************************************************************************************************************/
class JRadioButtonMenuItemProxy extends JRadioButtonMenuItem implements MenuProxy, ActionListener, ChangeListener, PropertyChangeListener
{
  private JRadioButtonMenuItem menuItem = null;
  private JMenu parentMenu = null;

  private ContainerListener listener = null;

  private boolean alwaysDisabled = false;

  public JRadioButtonMenuItemProxy(JRadioButtonMenuItem menuItem, JMenu parentMenu, ContainerListener listener)
  {
    super(menuItem.getText(), menuItem.getIcon());
    setEnabled(menuItem.isEnabled());
    setMnemonic(menuItem.getMnemonic());
    setSelected(menuItem.isSelected());
    
    this.menuItem = menuItem;
    this.parentMenu = parentMenu;
    this.listener = listener;

    parentMenu.add(this);

    this.addActionListener(this);

    menuItem.addChangeListener(this);
//    menuItem.addPropertyChangeListener();
  }

  public void actionPerformed(ActionEvent e)
  {
    menuItem.doClick();
//    System.out.println("Proxy: " + menuItem.hashCode());
  }

  public void stateChanged(ChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
    setSelected(menuItem.isSelected());
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    setEnabled(menuItem.isEnabled());
    setText(menuItem.getText());
    setIcon(menuItem.getIcon());
    setMnemonic(menuItem.getMnemonic());
  }

  public void setAlwaysDisabled(boolean disabled)
  {
    alwaysDisabled = disabled;
    this.setEnabled(menuItem.isEnabled());
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
    menuItem.removeChangeListener(this);
//    menuItem.removePropertyChangeListener(this);
    parentMenu.remove(this);
  }
}

/***********************************************************************************************************************
<b>Description</b>: This class is used by the Cougaar Desktop applicaiton to implement a proxy for a menu separator
                    item.

***********************************************************************************************************************/
class JSeparatorProxy extends JSeparator implements MenuProxy
{
  private JSeparator separator = null;
  private JMenu parentMenu = null;

  private ContainerListener listener = null;

  public JSeparatorProxy(JSeparator separator, JMenu parentMenu, ContainerListener listener)
  {
    this.separator = separator;
    this.parentMenu = parentMenu;
    this.listener = listener;

    parentMenu.add(this);
  }

  public void setAlwaysDisabled(boolean disabled)
  {
  }

  public void dispose()
  {
    parentMenu.remove(this);
  }
}
