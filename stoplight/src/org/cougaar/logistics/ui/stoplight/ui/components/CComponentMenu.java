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
package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Vector;
import javax.swing.*;

import org.cougaar.logistics.ui.stoplight.ui.util.Selector;

/**
 * A popup menu bean that manages a set of components.  Each component has an
 * associated menu item in the popup menu.  The components are rendered as
 * pullright options from the menu items.  The popup menu can be used
 * to select a component by clicking on the components menu label.<BR><BR>
 *
 * This bean has bounded property:  "selectedItem" (the selected component)
 */
public class CComponentMenu extends JPopupMenu implements Selector
{
    private JMenu selectedMenu;

    /**
     * Default constructor.  Creates a new, empty CComponentMenu.
     */
    public CComponentMenu()
    {
        super();
    }

    /**
     * Adds a component as a pullright option to the popup menu.
     *
     * @param label menu label for this selector
     * @param c the component to add to the popup menu
     */
    public void addComponent(final String label, final Component c)
    {
        final JMenu menu = new JMenu(label);
        menu.add(c);
        menu.enableInputMethods(false); // Swing bug workaround
        add(menu);

        menu.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e)
                {
                    updateSelection(menu, true);
                }
            });

        selectedMenu = menu;
    }

    /**
     * Sets the selected component.  A selectedItem property change event will
     * fire based on this change.
     *
     * @param item either the component to select or the label associated with
     *             the component to select
     */
    public void setSelectedItem(Object item)
    {
        setSelectedItem(item, true);
    }

    /**
     * Sets the selected component
     *
     * @param item either the component to select or the label associated with
     *             the component to select
     * @param firePropChange if true, will fire a selectedItem property change
     *                       event based on this change.
     */
    public void setSelectedItem(Object item, boolean firePropChange)
    {
        Component[] comps = getComponents();
        for (int i = 0; i < comps.length; i++)
        {
            JMenu menu = (JMenu)comps[i];
            Selector s = (Selector)menu.getPopupMenu().getComponent(0);
            if (((item instanceof Component) && (s == item)) ||
                (item.toString().equalsIgnoreCase(menu.getText())))
            {
                updateSelection(menu, firePropChange);
                break;
            }
        }
    }

    /**
     * Gets the currently selected component
     *
     * @return the currently selected component(can be cast to Component class)
     */
    public Object getSelectedItem()
    {
        return selectedMenu.getPopupMenu().getComponent(0);
    }

    /**
     * Gets the menu label associated with the currently selected component.
     *
     * @return the menu label associated with the currently selected component.
     */
    public String getSelectedLabel()
    {
        return selectedMenu.getText();
    }

    /**
     * Get the menu item that is currently selected
     *
     * @return the menu item that is currently selected
     */
    public JMenu getSelectedMenu()
    {
        return selectedMenu;
    }

    /**
     * Gets an array of all components that have been added to this component
     * menu.
     *
     * @return an array of all components that have been added to this
     *         component menu.
     */
    public Component[] getAddedComponents()
    {
        Component[] comp = getComponents();

        for (int i = 0; i < comp.length; i++)
        {
            comp[i] = ((JMenu)comp[i]).getPopupMenu().getComponent(0);
        }

        return comp;
    }

    private Vector actionListeners = new Vector();

    /**
     * Adds an action listener that is fired whenever the user attempts to make
     * a selection (even if the selectedItem property did not change).
     *
     * @param al the new action listener
     */
     public void addActionListener(ActionListener al)
     {
        actionListeners.add(al);
     }

    /**
     * Removes a registered action listener.
     *
     * @param al the existing action listener
     */
    public void removeActionListener(ActionListener al)
    {
        actionListeners.remove(al);
    }

    /**
     * Reorder menu items such that selected item is at the bottom.
     */
    public void reorderMenuItems()
    {
        remove(selectedMenu);
        add(selectedMenu);
    }

    private void fireActionPerformed()
    {
        for (int i = 0; i < actionListeners.size(); i++)
        {
            ActionListener al = (ActionListener)actionListeners.elementAt(i);
            al.actionPerformed(new ActionEvent(this, 0, "selectedItemAction"));
        }
    }

    private void updateSelection(JMenu selectedMenu, boolean firePropChange)
    {
        if (selectedMenu != this.selectedMenu)
        {
            Component oldComponent = (Component)getSelectedItem();
            this.selectedMenu = selectedMenu;
            if (firePropChange)
            {
                firePropertyChange("selectedItem", oldComponent,
                                   getSelectedItem());
                fireActionPerformed();
            }
        }
    }
}