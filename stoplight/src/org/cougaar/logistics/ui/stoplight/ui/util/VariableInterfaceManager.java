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
package org.cougaar.logistics.ui.stoplight.ui.util;

import java.util.Enumeration;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.cougaar.logistics.ui.stoplight.ui.components.CComboSelector;
import org.cougaar.logistics.ui.stoplight.ui.components.CComponentMenu;
import org.cougaar.logistics.ui.stoplight.ui.components.CMenuButton;
import org.cougaar.logistics.ui.stoplight.ui.components.CPullrightButton;
import org.cougaar.logistics.ui.stoplight.ui.components.CRLabel;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;
import org.cougaar.logistics.ui.stoplight.ui.models.VariableModel;

/**
 * Used to manage a set of variable interfaces.  Variable interfaces are
 * either associated with the x or y axis of a graph or they are fixed.
 * Variable interface manager can be used to swap the roles and values of
 * variables either by way of user action or programmatically.  It can also be
 * used with a query generator to parameterize a query that populates a
 * database table model.
 *
 * @see org.cougaar.logistics.ui.stoplight.ui.models.DatabaseTableModel
 */
public class VariableInterfaceManager
{
    private VariableModel[] variableDescriptors;
    private Vector variableListeners = new Vector();
    private CRLabel xAxisLabel = new CRLabel("X Axis");
    private CRLabel yAxisLabel = new CRLabel("Y Axis");

    /**
     * Create a new variable interface manager based on given parameters
     *
     * @param variables      variable descriptors for the variables that need
     *                       to be managed
     * @param useMenuButtons true if variable manager should use CMenuButtons
     *                       for variable management; otherwise CComboSelectors
     *                       will be used.
     */
    public VariableInterfaceManager(VariableModel[] variables,
                                    boolean useMenuButtons)
    {
        this.variableDescriptors = variables;

        // Variable selection logic
        ActionListener cbListener = new SelectionListener();
        PropertyChangeListener varListener = new VariableSelectionListener();

        // Create a control for each variable
        for (int i = 0; i < variableDescriptors.length; i++)
        {
            final VariableModel v = variableDescriptors[i];

            JPanel variableControl = new JPanel();
            if (v.isHorizontal())
            {
                variableControl.setLayout(
                    new BoxLayout(variableControl, BoxLayout.X_AXIS));
            }
            else
            {
                variableControl.setLayout(new GridLayout(2, 1, 0, 10));
            }

            JComponent variableLabel = null;
            if (v.isSwappable())
            {
                JComponent selector;

                if (useMenuButtons)
                {
                    CMenuButton mbSelector = createMenuButton();
                    mbSelector.setSelectedItem(v);
                    mbSelector.addPropertyChangeListener("selectedItem",
                                                         varListener);
                    selector = mbSelector;
                }
                else
                {
                    JComboBox cbSelector = createCombo();
                    cbSelector.setSelectedItem(v);
                    cbSelector.addActionListener(cbListener);
                    cbSelector.setMinimumSize(new Dimension(0,0));
                    selector = cbSelector;
                }

                selector.putClientProperty("ID", v);
                JPanel selectorBox = new JPanel(new BorderLayout());
                if (v.isHorizontal())
                {
                    selectorBox.setLayout(
                        new BoxLayout(selectorBox, BoxLayout.X_AXIS));
                    selectorBox.add(selector);
                    if (!useMenuButtons)
                    {
                        selectorBox.add(new JLabel(": "));
                        selectorBox.add(Box.createGlue());
                    }
                }
                else
                {
                    selectorBox.add(selector, BorderLayout.CENTER);
                }
                variableControl.add(selectorBox);
                variableLabel = selectorBox;
            }
            else
            {
                variableLabel = new JLabel(v.getName() + ": ");
                variableControl.add(variableLabel);
            }

            if ((v.getLabelWidth() != 0) && (!useMenuButtons))
            {
                variableLabel.setPreferredSize(
                    new Dimension(v.getLabelWidth(),
                                  variableLabel.getPreferredSize().height));
                variableLabel.setMaximumSize(variableLabel.getPreferredSize());
            }

            if (!v.isSwappable() || !useMenuButtons)
            {
                variableControl.add(v.getSelectComponent());
            }
            v.setControl(variableControl);

            if (v.isHorizontal())
            {
                variableControl.add(Box.createGlue());
            }

            v.getSelector().addPropertyChangeListener("selectedItem",
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e)
                    {
                        for (int x = 0; x < variableListeners.size(); x++)
                        {
                            VariableListener vl = (VariableListener)
                                variableListeners.elementAt(x);
                            vl.variableChanged(v);
                        }
                    }
                });
        }

        // update axis variable labels when values change
        updateAxisLabels();
        addVariableListener(
            new VariableInterfaceManager.VariableListener() {
                public void variableChanged(VariableModel vm)
                {
                    updateAxisLabels();
                }
                public void variablesSwapped(VariableModel vm1,
                                             VariableModel vm2)
                {
                    updateAxisLabels();
                }
            });

        // Allow invocation of x and y controls from x and y labels
        xAxisLabel.addMouseListener(
            new LabelPopupListener(VariableModel.X_AXIS));
        yAxisLabel.addMouseListener(
            new LabelPopupListener(VariableModel.Y_AXIS));
    }

    private class LabelPopupListener extends MouseAdapter
    {
        private int varType;

        public LabelPopupListener(int varType)
        {
            this.varType = varType;
        }

        public void mouseReleased(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                VariableModel vm = (VariableModel)
                    getDescriptors(varType).nextElement();
                Selector s = getVariableSelector(vm.getControl());
                if (e.getSource() instanceof Component)
                {
                    Component source = (Component)e.getSource();
                    Point p = source.getLocationOnScreen();
                    int x = p.x + e.getX() - 10;
                    int y = p.y + e.getY() - 10;
                    if (s instanceof CMenuButton)
                    {
                        ((CMenuButton)s).popupMenu(x, y);
                    }
                    else if (vm.getSelector() instanceof CPullrightButton)
                    {
                        ((CPullrightButton)vm.getSelector()).popupMenu(x, y);
                    }
                }
            }
        }
    }

    /**
     * Set the x axis to a managed variable descriptor
     *
     * @param variableName name of variable to set as x axis variable
     */
    public void setXAxis(String variableName)
    {
        VariableModel xDescriptor = (VariableModel)
            getDescriptors(VariableModel.X_AXIS).nextElement();
        setLocation(xDescriptor, variableName);
    }

    /**
     * Set the y axis to a managed variable descriptor
     *
     * @param variableName name of variable to set as y axis variable
     */
    public void setYAxis(String variableName)
    {
        VariableModel yDescriptor = (VariableModel)
            getDescriptors(VariableModel.Y_AXIS).nextElement();
        setLocation(yDescriptor, variableName);
    }

    /**
     * Get descriptors of a given type (i.e. state)
     *
     * @param type type of descriptors to get (VariableModel.X_AXIS,
     *             VariableModel.Y_AXIS, VariableModel.FIXED)
     * @return list of variable descriptors that match given type
     */
    public Enumeration getDescriptors(int type)
    {
        Vector descriptors = new Vector();

        for (int i = 0; i < variableDescriptors.length; i++)
        {
            if (variableDescriptors[i].getState() == type)
                descriptors.add(variableDescriptors[i]);
        }

        return descriptors.elements();
    }

    /**
     * Get a variable descriptor by name
     *
     * @param name name of descriptor to get
     * @return descriptor that has given name (null if not found)
     */
    public VariableModel getDescriptor(String name)
    {
        for (int i = 0; i < variableDescriptors.length; i++)
        {
            if (name.equals(variableDescriptors[i].getName()))
                return variableDescriptors[i];
        }

        return null;
    }

    public CRLabel getXAxisLabel()
    {
        return xAxisLabel;
    }

    public CRLabel getYAxisLabel()
    {
        return yAxisLabel;
    }

    /**
     * Add a new variable listener to this manager.  Changes to managed
     * variable values and roles will trigger methods in these listeners
     *
     * @param vl variable listener to add
     */
    public void addVariableListener(VariableListener vl)
    {
        variableListeners.add(vl);
    }

    /**
     * Rmove an installed variable listener from this manager.
     *
     * @param vl variable listener to remove
     */
    public void removeVariableListener(VariableListener vl)
    {
        variableListeners.remove(vl);
    }

    /**
     * Changes to managed variable values and roles will trigger methods in
     * listeners that implement this interface and are added to manager.
     */
    public interface VariableListener
    {
        /**
         * Called when a managed variable value is changed.
         *
         * @param vm the variable that has new value
         */
        public void variableChanged(VariableModel vm);

        /**
         * Called when two managed varables swamp roles/positions via
         * variable management controls.
         *
         * @param vm1 one of the two variables that swapped role and position
         * @param vm2 one of the two variables that swapped role and position
         */
        public void variablesSwapped(VariableModel vm1, VariableModel vm2);
    }

    private CComboSelector createCombo()
    {
        CComboSelector combo = new CComboSelector();

        for (int i = 0; i < variableDescriptors.length; i++)
        {
            if (variableDescriptors[i].isSwappable())
            {
                combo.addItem(variableDescriptors[i]);
            }
        }
        return combo;
    }

    private CComponentMenu swappableComponentMenu = null;
    private CMenuButton createMenuButton()
    {
        CMenuButton mb = new CMenuButton();

        if (swappableComponentMenu == null)
        {
            swappableComponentMenu = new CComponentMenu();
            for (int i = 0; i < variableDescriptors.length; i++)
            {
                if (variableDescriptors[i].isSwappable())
                {
                    Selector newSelector =
                        getRootSelector(variableDescriptors[i]);

                    swappableComponentMenu.
                        addComponent(variableDescriptors[i].toString(),
                                     (Component)newSelector);
                }
            }
        }

        mb.setSelectorMenu(swappableComponentMenu);
        return mb;
    }

    private void setLocation(VariableModel vDescriptor,
                             String variableName)
    {
        if (!vDescriptor.getName().equals(variableName))
        {
            JPanel vControl = vDescriptor.getControl();
            Selector vSelector = getVariableSelector(vControl);
            vSelector.setSelectedItem(getDescriptor(variableName));
        }
    }

    private static Selector getVariableSelector(JPanel panel)
    {
        Selector s = null;

        if (panel.getComponentCount() > 0)
        {
            Container c = (Container)panel.getComponent(0);
            if (c.getComponentCount() > 0)
            {
                s = (Selector)c.getComponent(0);
            }
        }
        return s;
    }

    private class SelectionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            JComboBox modifiedCB = (JComboBox)e.getSource();
            Boolean react = (Boolean)modifiedCB.getClientProperty("REACT");
            if ((react != null) && !react.booleanValue())
            {
                modifiedCB.putClientProperty("REACT", new Boolean(true));
            }
            else
            {
                VariableModel oldValue =
                    (VariableModel)modifiedCB.getClientProperty("ID");
                VariableModel newValue =
                    (VariableModel)modifiedCB.getSelectedItem();
                modifiedCB.putClientProperty("ID", newValue);

                if (oldValue != newValue)
                {
                    JComboBox relatedCB =
                        (JComboBox)getVariableSelector(newValue.getControl());
                    relatedCB.putClientProperty("REACT", new Boolean(false));
                    relatedCB.setSelectedItem(oldValue);
                    relatedCB.putClientProperty("ID", oldValue);
                    swapDescriptors(oldValue, newValue);

                    for (int i = 0; i < variableListeners.size(); i++)
                    {
                        VariableListener vl = (VariableListener)
                            variableListeners.elementAt(i);
                        vl.variablesSwapped(oldValue, newValue);
                    }
                }
            }
        }

        private void swapDescriptors(VariableModel v1,
                                     VariableModel v2)
        {
            v1.swapLocations(v2);
            swapControls(v1.getControl(), v2.getControl());
        }


        private void swapControls(JPanel panel1, JPanel panel2)
        {
            Component comp1 = panel1.getComponent(1);
            Component comp2 = panel2.getComponent(1);

            panel1.remove(1);
            panel2.remove(1);
            panel1.add(comp2, 1);
            panel2.add(comp1, 1);

            // relayout, repaint
            panel1.revalidate();
            panel2.revalidate();
            panel1.repaint();
            panel2.repaint();
        }
    }

    private class VariableSelectionListener implements PropertyChangeListener
    {
        public void propertyChange(final PropertyChangeEvent e)
        {
            CMenuButton source = (CMenuButton)e.getSource();
            VariableModel vm = (VariableModel)source.getClientProperty("ID");

            Boolean react = (Boolean)source.getClientProperty("REACT");
            if ((react != null) && !react.booleanValue())
            {
                source.putClientProperty("REACT", new Boolean(true));
            }
            else
            {
                // find other JMenuButton that must be modified
                // i.e. the one whose variable now matches this one.
                Selector sourcesNewSelector = (Selector)e.getNewValue();

                VariableModel otherVm = null;
                for (int i = 0; i < variableDescriptors.length; i++)
                {
                    Selector otherVmsSelector =
                        getRootSelector(variableDescriptors[i]);
                    if (sourcesNewSelector == otherVmsSelector)
                    {
                        otherVm = variableDescriptors[i];
                        break;
                    }
                }

                source.putClientProperty("ID", otherVm);
                final CMenuButton otherMb =
                    (CMenuButton)getVariableSelector(otherVm.getControl());
                otherMb.putClientProperty("REACT", new Boolean(false));
                otherMb.setSelectedItem(e.getOldValue());
                otherMb.putClientProperty("ID", vm);
                vm.swapLocations(otherVm);

                for (int i = 0; i < variableListeners.size(); i++)
                {
                    VariableListener vl = (VariableListener)
                        variableListeners.elementAt(i);
                    vl.variablesSwapped(vm, otherVm);
                }
           }
        }
    }

    /**
     * Unwrap CPullright buttons
     */
    private Selector getRootSelector(VariableModel vm)
    {
        Selector rootSelector = vm.getSelector();
        if (rootSelector instanceof CPullrightButton)
        {
            rootSelector =
                ((CPullrightButton)rootSelector).getSelectorControl();
        }

        return rootSelector;
    }

    private void updateAxisLabels()
    {
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    updateAxisLabel(VariableModel.X_AXIS);
                    updateAxisLabel(VariableModel.Y_AXIS);
                }
            });
    }

    private void updateAxisLabel(int axis)
    {
        CRLabel xLabel =
            (axis == VariableModel.X_AXIS) ? xAxisLabel : yAxisLabel;
        VariableModel vm = (VariableModel)getDescriptors(axis).nextElement();
        Selector vs = getVariableSelector(vm.getControl());
        Selector s = vm.getSelector();
        CMenuButton mb = (CMenuButton)
            ((vs instanceof CMenuButton) ? vs : null);
        CPullrightButton pb = (CPullrightButton)
            ((s instanceof CPullrightButton) ? s : null);
        xLabel.setText((mb==null) ?
                       (vm.getName() + ": " +
                            ((pb==null) ? vm.getValue() : pb.getText())) :
                        mb.getText());
        setUpArrowControl(vm, xLabel);
    }

    private void setUpArrowControl(final VariableModel vm,
                                   final CRLabel upArrow)
    {
        upArrow.removeMouseListener(
            (MouseListener)upArrow.getClientProperty("UPLISTENER"));

        if (vm.getValue() instanceof DefaultMutableTreeNode)
        {
            final Object parent =
                ((DefaultMutableTreeNode)vm.getValue()).getParent();
            if (parent != null)
            {
                // \u25b2 is not supported by cougaar font
                //upArrow.setText(upArrow.getText() + "  \u25b2   ");
                upArrow.setText(upArrow.getText() + "  ^   ");
                MouseListener ml = new MouseAdapter() {
                        public void mouseClicked(MouseEvent e)
                        {
                            int lo = upArrow.getOrientation();
                            if (((lo == CRLabel.LEFT_RIGHT) &&
                                (e.getX() > (upArrow.getSize().width - 20))) ||
                                ((lo == CRLabel.DOWN_UP) && (e.getY() < 20)))
                            {
                                vm.setValue(parent);
                            }
                        }
                    };
                upArrow.putClientProperty("UPLISTENER", ml);
                upArrow.addMouseListener(ml);
            }
        }
    }

    /**
     * Return a string that describes the current settings of all variable
     * interfaces.
     *
     * @return a string that describes the current settings of all variable
     *         interfaces.
     */
    public String toString()
    {
        StringBuffer currentSettings = new StringBuffer();

        VariableModel xAxis =
            (VariableModel)getDescriptors(VariableModel.X_AXIS).nextElement();
        VariableModel yAxis =
            (VariableModel)getDescriptors(VariableModel.Y_AXIS).nextElement();

        currentSettings.append(yAxis);
        currentSettings.append(" [" + getValueString(yAxis) + "] vs ");
        currentSettings.append(xAxis);
        currentSettings.append(" [" + getValueString(xAxis) + "] where ");
        Enumeration vds = getDescriptors(VariableModel.FIXED);
        while(vds.hasMoreElements())
        {
            VariableModel v = (VariableModel)vds.nextElement();
            currentSettings.append(v + " = " + getValueString(v));
            if (vds.hasMoreElements()) currentSettings.append(" and ");
        }

        return currentSettings.toString();
    }

    private String getValueString(VariableModel vm)
    {
        Object value = vm.getValue();

        if (value instanceof RangeModel)
        {
            RangeModel rm = (RangeModel)value;
            return rm.getMin() + "-" + rm.getMax();
        }

        return value.toString();
    }
}