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
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Control used to select view related features of a stoplight chart.
 * Used to select whether chart should use color, value, or both when
 * displaying a data point in a cell.<BR><BR>
 *
 * bounded properties: mode, fitHorizontally, fitVertically
 *
 */
public class CViewFeatureSelectionControl extends JPanel
{
    /** color selection string */
    public static String COLOR = "Color";

    /** value selection string */
    public static String VALUE = "Value";

    /** both selection string */
    public static String BOTH = "Both";

    private CRadioButtonSelectionControl modeControl = null;
    private static String[] selections = {COLOR, VALUE, BOTH};
    private AbstractButton fitHorizontallyControl = null;
    private AbstractButton fitVerticallyControl = null;

    /**
     * Default constructor.  Create new view feature selection control with
     * horizontal orientation.
     */
    public CViewFeatureSelectionControl()
    {
        super(new GridLayout(2, 1));

        modeControl =
            new CRadioButtonSelectionControl(selections, BoxLayout.X_AXIS);
        init();
    }

    /**
     * Create new view feature selection control.
     *
     * @param orientation BoxLayout.X_AXIS or BoxLayout.Y_AXIS
     */
    public CViewFeatureSelectionControl(int orientation)
    {
        super(new GridLayout(1, 2));

        modeControl =
            new CRadioButtonSelectionControl(selections, orientation);
        init();
    }

    /**
     * Not needed when compiling/running under jdk1.3
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    private void init()
    {
        modeControl.setSelectedItem(BOTH);
        add(modeControl);
        modeControl.addPropertyChangeListener("selectedItem",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    CViewFeatureSelectionControl.this.firePropertyChange(
                        "mode", e.getOldValue(), e.getNewValue());
                    boolean justColorSelected = e.getNewValue().
                        equals(CViewFeatureSelectionControl.COLOR);
                    fitHorizontallyControl.setEnabled(justColorSelected);
                    fitHorizontallyControl.setSelected(justColorSelected);
                    fitVerticallyControl.setEnabled(justColorSelected);
                    if (!justColorSelected)
                        fitVerticallyControl.setSelected(false);
                }
            });
        Box fitControls = new Box(BoxLayout.Y_AXIS);
        fitControls.add(Box.createVerticalStrut(8));
        fitHorizontallyControl = new JCheckBox("Fit Horizontally");
        fitControls.add(fitHorizontallyControl);
        fitVerticallyControl = new JCheckBox("Fit Vertically");
        fitControls.add(fitVerticallyControl);
        add(fitControls);
        addActionListenersToFitControls();
    }

    private void addActionListenersToFitControls()
    {
        fitHorizontallyControl.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    boolean newState = fitHorizontallyControl.isSelected();
                    CViewFeatureSelectionControl.this.firePropertyChange(
                        "fitHorizontally", !newState, newState);
                }
            });
        fitVerticallyControl.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e)
                {
                    boolean newState = fitVerticallyControl.isSelected();
                    CViewFeatureSelectionControl.this.firePropertyChange(
                        "fitVertically", !newState, newState);
                }
            });
    }

    /**
     * Convert panel based control to a pulldown menu.
     *
     * @param menuName label for menu
     * @return menu version of control
     */
    public JMenu convertToMenu(String menuName)
    {
        JMenu menu = new JMenu(menuName);

        ButtonGroup bg = modeControl.convertToMenuItems();
        Enumeration mis = bg.getElements();
        while (mis.hasMoreElements())
        {
            menu.add((JMenuItem)mis.nextElement());
        }
        menu.add(new JSeparator());

        boolean enabled = fitHorizontallyControl.isEnabled();
        fitHorizontallyControl =
            new JCheckBoxMenuItem(fitHorizontallyControl.getText(),
                                  fitHorizontallyControl.isSelected());
        fitHorizontallyControl.setMnemonic('H');
        fitHorizontallyControl.setEnabled(enabled);
        enabled = fitVerticallyControl.isEnabled();
        fitVerticallyControl =
            new JCheckBoxMenuItem(fitVerticallyControl.getText(),
                                  fitVerticallyControl.isSelected());
        fitVerticallyControl.setMnemonic('V');
        fitVerticallyControl.setEnabled(enabled);
        addActionListenersToFitControls();
        menu.add(fitHorizontallyControl);
        menu.add(fitVerticallyControl);

        return menu;
    }

    /**
     * Used to select whether chart should use color, value, or both when
     * displaying a data point in a cell.
     *
     * @param newMode CViewFeatureControl.COLOR, VALUE, or BOTH
     */
    public void setMode(String newMode)
    {
        modeControl.setSelectedItem(newMode);
    }

    /**
     * Used to get whether chart is using color, value, or both when
     * displaying a data point in a cell.
     *
     * @return CViewFeatureControl.COLOR, VALUE, or BOTH
     */
    public String getMode()
    {
        return (String)modeControl.getSelectedItem();
    }

    /**
     * If set to true the data cells will compress in width to fit all cells
     * in the viewport.  Otherwise, a horizontal scrollbar is used.
     *
     * @param newValue new state for checkbox
     */
    public void setFitHorizontally(boolean newValue)
    {
        fitHorizontallyControl.setSelected(newValue);
    }

    /**
     * If set to true the data cells will compress in width to fit all cells
     * in the viewport.  Otherwise, a horizontal scrollbar is used.
     *
     * @return current selected state of checkbox
     */
    public boolean getFitHorizontally()
    {
        return fitHorizontallyControl.isSelected();
    }

    /**
     * If set to true the data cells will compress in height to fit all cells
     * in the viewport.  Otherwise, a Vertical scrollbar is used.
     *
     * @param newValue new state for checkbox
     */
    public void setFitVertically(boolean newValue)
    {
        fitVerticallyControl.setSelected(newValue);
    }

    /**
     * If set to true the data cells will compress in height to fit all cells
     * in the viewport.  Otherwise, a Vertical scrollbar is used.
     *
     * @return current selected state of checkbox
     */
    public boolean getFitVertically()
    {
        return fitVerticallyControl.isSelected();
    }
}
