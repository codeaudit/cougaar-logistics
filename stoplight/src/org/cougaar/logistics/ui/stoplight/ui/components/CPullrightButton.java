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
 * A button that when pushed will present a pullright menu containing a
 * selection control.  The label of the button is automatically set to the
 * selected item in the selection control.
 *
 * This bean has bounded property:  "selectedItem".
 */
public class CPullrightButton extends JButton implements Selector
{
    private Selector selectorControl = null;
    private JPopupMenu pullright = null;

    public CPullrightButton()
    {
        super();
    }

    /**
     * When look and feel or theme is changed, this method is called.  It
     * ensures that the child dialog's look and feel is updated as well.
     */
    public void updateUI()
    {
        super.updateUI();

        if (pullright != null)
        {
            SwingUtilities.updateComponentTreeUI(pullright);
        }
    }

    /**
     * Pops up a popup menu in the name of this CPullrightButton at the given
     * location.
     *
     * @param x x location on screen to display popup
     * @param y y location on screen to display popup
     */
    public void popupMenu(int x, int y)
    {
        // Don't popup off screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension pullrightD = pullright.getPreferredSize();
        int popupX = x + pullrightD.width;
        int popupY = y + pullrightD.height;
        if (popupX > screen.width) x = screen.width - pullrightD.width;
        if (popupY > screen.height) y = screen.height - pullrightD.height;

        pullright.setLocation(x, y);
        pullright.setInvoker(this);
        pullright.setVisible(true);
    }

    /**
     * Set the control used for making selection
     *
     * @param selectorControl the selection control
     */
     public void setSelectorControl(final Selector selectorControl)
     {
        this.selectorControl = selectorControl;
        setText(selectorControl.getSelectedItem().toString());
        pullright = new PullrightMenu(this, (Component)selectorControl);

        selectorControl.addPropertyChangeListener("selectedItem",
                                     new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    setText(selectorControl.getSelectedItem().toString());
                }
            });

        selectorControl.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    pullright.setVisible(false);
                }
            });

        addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    Point p = getLocationOnScreen();
                    Dimension screen =
                        Toolkit.getDefaultToolkit().getScreenSize();
                    Dimension pullrightD = pullright.getPreferredSize();
                    int popupX = p.x + pullrightD.width;
                    int popupY = p.y + pullrightD.height;
                    popupMenu((popupX > screen.width) ?
                       p.x - pullrightD.width : p.x + getSize().width,
                       (popupY > screen.height) ?
                       p.y-pullrightD.height + getSize().height:p.y);
                }
             });
     }

    /**
     * Get the control used for making selection
     *
     * @return the selection control
     */
     public Selector getSelectorControl()
     {
        return selectorControl;
     }

    /**
     * Get the currently selected node.
     *
     * @return the currently selected node
     *         (can cast to type DefaultMutableTreeNode)
     */
    public Object getSelectedItem()
    {
        return selectorControl.getSelectedItem();
    }

    /**
     * Set the selected node.
     *
     * @param selectedItem the new node
     *                     (can be of type String or DefaultMutableTreeNode)
     */
    public void setSelectedItem(Object selectedItem)
    {
        selectorControl.setSelectedItem(selectedItem);
    }

    /**
     * Add a property change listener that will be fired whenever a property is
     * changed.
     *
     * @param pcl the new property change listener
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl)
    {
        super.addPropertyChangeListener(pcl);

        if (selectorControl != null)
        {
            selectorControl.addPropertyChangeListener(pcl);
        }
    }

    /**
     * Remove a property change listener
     *
     * @param pcl the property change listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl)
    {
        super.removePropertyChangeListener(pcl);

        if (selectorControl != null)
        {
            selectorControl.removePropertyChangeListener(pcl);
        }
    }

    /**
     * Add a property change listener that will be fired whenever the
     * given property is changed.
     *
     * @param name the name of the property to listen for changes in.
     * @param pcl the new property change listener
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener pcl)
    {
        super.addPropertyChangeListener(name, pcl);

        if (selectorControl != null)
        {
            selectorControl.addPropertyChangeListener(name, pcl);
        }
    }

    /**
     * Remove a property change listener
     *
     * @param name the name of the property to listen for changes in.
     * @param pcl the new property change listener
     */
    public void removePropertyChangeListener(String name,
                                             PropertyChangeListener pcl)
    {
        super.removePropertyChangeListener(name, pcl);

        if (selectorControl != null)
        {
            selectorControl.removePropertyChangeListener(name, pcl);
        }
    }

    /**
     * This private class defines the JPopup menu that is used to display
     * selection control.
     */
    private class PullrightMenu extends JPopupMenu
    {
        public PullrightMenu(Component parent, Component control)
        {
            super("Select Node");
            setInvoker(parent);
            PullrightMenu.this.setBorderPainted(true);
            PullrightMenu.this.add(control);
        }

        public void cancel()
        {
            firePopupMenuCanceled();
            PullrightMenu.this.setVisible(false);
        }

        /**
         * Messaged when the menubar selection changes to activate or
         * deactivate this menu. This implements the
         * <code>javax.swing.MenuElement</code> interface.
         * Overrides <code>MenuElement.menuSelectionChanged</code>.
         *
         * @param isIncluded  true if this menu is active, false if
         *        it is not
         * @see MenuElement#menuSelectionChanged(boolean)
         */
        /*
        public void menuSelectionChanged(boolean isIncluded)
        {
            if (getInvoker() instanceof JButton)
            {
                JButton inv = (JButton)getInvoker();
                System.out.println(inv);
                if (inv.hasFocus())
                    return;
            }
            super.menuSelectionChanged(isIncluded);
        }
        */

    }

    /**
     * main for unit test
     *
     * @param args ignored arguments
     */
    public static void main(String[] args)
    {
        final CPullrightButton prb = new CPullrightButton();
        CSliderSelector ss =
            new CSliderSelector("Set New Time:", new String[]{"C"}, 0, 100);
        prb.setSelectorControl(ss);
        ss.addPropertyChangeListener("selectedItem",
                                     new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    Number n = (Number)((Vector)e.getNewValue()).elementAt(0);
                    prb.setText("C" + ((n.intValue() > 0) ? "+" : "")  +
                                n.intValue());
                }
            });
        Vector defaultValue = new Vector();
        defaultValue.add(new Integer(50));
        ss.setSelectedItem(defaultValue);

        JFrame frame = new JFrame();
        frame.getContentPane().add(prb);
        frame.pack();
        frame.setVisible(true);
    }
}
