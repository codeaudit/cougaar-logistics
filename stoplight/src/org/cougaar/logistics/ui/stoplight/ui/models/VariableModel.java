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
package org.cougaar.logistics.ui.stoplight.ui.models;

import java.awt.Component;
import javax.swing.JPanel;

import org.cougaar.logistics.ui.stoplight.ui.util.Selector;

/**
 * Used to represent information about a particular variable managed by the
 * variable interface manager.
 */
public class VariableModel
{
    /** var state -> variable represents the x axis of the graph or matrix */
    public final static int X_AXIS = 0;

    /** var state -> variable represents the y axis of the graph or matrix */
    public final static int Y_AXIS = 1;

    /** var state -> variable is fixed and not associated with either axis */
    public final static int FIXED = 2;

    private String name;
    private Selector selectComponent;
    private boolean swappable;
    private int state;
    private boolean horizontal;
    private int labelWidth = 0;
    private JPanel control = null;

    /**
     * Create a new variable descriptor based on the given parameters
     *
     * @param name            name of variable
     * @param selectComponent selector that is used to select values for this
     *                        variable
     * @param swappable       true if the variable should be allowed to swap
     *                        location (and type) with other variables
     * @param state           X_AXIS, Y_AXIS, or FIXED
     * @param horizontal      true if variable label should be horizontally
     *                        aligned with variable selector.  False is
     *                        variable label should be vertically aligned
     *                        with variable selector
     * @param labelWidth      width of the varableLabel (if 0, use preferred)
     */
    public VariableModel(String name, Selector selectComponent,
                         boolean swappable, int state, boolean horizontal,
                         int labelWidth)
    {
        this.name = name;
        this.selectComponent = selectComponent;
        this.swappable = swappable;
        this.state = state;
        this.horizontal = horizontal;
        this.labelWidth = labelWidth;
    }

    /**
     * Get the name of variable
     *
     * @return name of variable
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the component used to select variable value
     *
     * @return component used to select variable value
     */
    public Component getSelectComponent()
    {
        return (Component)selectComponent;
    }

    /**
     * Get the selector used to select variable value
     *
     * @return selector used to select variable value
     */
    public Selector getSelector()
    {
        return selectComponent;
    }

    /**
     * Check whether variable is swappable
     *
     * @return true if variable is swappable
     */
    public boolean isSwappable()
    {
        return swappable;
    }

    /**
     * Check whether variable is horizontal
     *
     * @return true if variable is horizontal
     */
    public boolean isHorizontal()
    {
        return horizontal;
    }

    /**
     * Get current state of variable (X_AXIS, Y_AXIS, or FIXED)
     *
     * @return current state of variable (X_AXIS, Y_AXIS, or FIXED)
     */
    public int getState()
    {
        return state;
    }

    /**
     * Get the label width for this variable location
     *
     * @return the label width for this variable location
     */
    public int getLabelWidth()
    {
        return labelWidth;
    }

    /**
     * Set the control that is used to manage variable and change it's state
     *
     * @param control control that is used to manage variable and change it's
     *                state
     */
    public void setControl(JPanel control)
    {
        this.control = control;
    }

    /**
     * Set the control that is used to manage variable and change it's state
     *
     * @return control that is used to manage variable and change it's state
     */
    public JPanel getControl()
    {
        return control;
    }

    /**
     * Returns string representation of variable (it's name)
     *
     * @return string representation of variable (it's name)
     */
    public String toString()
    {
        return name;
    }

    /**
     * Swap control, state, and orientation of this variable descriptor with
     * given variable descriptor
     *
     * @param otherVD variable descriptor to swap with
     */
    public void swapLocations(VariableModel otherVD)
    {
        JPanel dummyControl = otherVD.control;
        otherVD.control = control;
        control = dummyControl;

        int dummyState = otherVD.state;
        otherVD.state = state;
        state = dummyState;

        boolean dummyHorizontal = otherVD.horizontal;
        otherVD.horizontal = horizontal;
        horizontal = dummyHorizontal;
    }

    /**
     * Get the curent selected value of this variable
     *
     * @return  the curent selected value of this variable
     */
    public Object getValue()
    {
        return selectComponent.getSelectedItem();
    }

    /**
     * Set the selected value of the variable
     *
     * @param value new selected value for this variable
     */
    public void setValue(Object value)
    {
        selectComponent.setSelectedItem(value);
    }
}
