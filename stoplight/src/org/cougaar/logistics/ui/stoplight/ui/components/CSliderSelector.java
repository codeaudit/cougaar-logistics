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
import java.util.Vector;
import javax.swing.*;

import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.CMThumbSlider;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.COrderedLabeledMThumbSlider;
import org.cougaar.logistics.ui.stoplight.ui.util.Selector;
import org.cougaar.logistics.ui.stoplight.ui.util.SliderControl;

/**
 * A UI control bean that is a single value slider control that implements the
 * Selector interface.
 *
 * This bean has bounded property:  "selectedItem" (a Vector of Numbers)
 *
 */
public class CSliderSelector extends JPanel implements Selector, SliderControl
{
    private Vector selectedValue = null;
    private JButton okButton;
    private SliderControl sliderControl;
    private int numThumbs = 0;
    private boolean plaf = false;

    /**
     * Default constructor.  Create a new slider selector that doesn't support
     * pluggable look and feel.
     */
    public CSliderSelector()
    {
        super(new BorderLayout());
        init("Select values:",
             new Color[]{Color.yellow, Color.blue, Color.red}, 0, 1);
    }

    /**
     * Create a new mulitple thumbed slider control.  Number of thumbs is
     * trackColorFills.length - 1.  Track is colored as specified between
     * thumbs.  Will use COrderedLabelMThumbSlider for slider control so
     * only metal look and feel is supported.
     *
     * @param title title for selector
     * @param trackColorFills the number of thumb for slider control.
     * @param minValue the minimum value for this slider
     * @param maxValue the maximum value for this slider
     */
    public CSliderSelector(String title, Color[] trackColorFills,
                           float minValue, float maxValue)
    {
        super(new BorderLayout());
        init(title, trackColorFills, minValue, maxValue);
    }

    /**
     * Creates new slider control using the given labels.  Will use
     * CMultipleSliderControl for slider control so it will support pluggable
     * look and feel.
     *
     * @param title title for selector
     * @param sliderLabels an array of labels for the sliders
     * @param minValue the minimum value for all sliders
     * @param maxValue the maximum value for all sliders
     *
     * @see CMultipleSliderControl
     */
    public CSliderSelector(String title, String[] sliderLabels, float minValue,
                           float maxValue)
    {
        super(new BorderLayout());
        init(title, sliderLabels, minValue, maxValue);
    }

    /**
     * Get the minimum value of the slider control
     *
     * @return the minimum value of the slider control
     */
    public float getMinValue()
    {
        return sliderControl.getMinValue();
    }

    /**
     * Set the minimum value of the slider control
     *
     * @param minValue the minimum value of the slider control
     */
    public void setMinValue(float minValue)
    {
        sliderControl.setMinValue(minValue);
    }

    /**
     * Get the maximum value of the slider control
     *
     * @return the maximum value of the slider control
     */
    public float getMaxValue()
    {
        return sliderControl.getMaxValue();
    }

    /**
     * Set the maximum value of the slider control
     *
     * @param maxValue the maximum value of the slider control
     */
    public void setMaxValue(float maxValue)
    {
        sliderControl.setMaxValue(maxValue);
    }

    /**
     * Adjusts all values such that thumbs are evenly distributed and ordered
     * from first to last.
     */
    public void evenlyDistributeValues()
    {
        sliderControl.evenlyDistributeValues();
    }

    /**
     * Adjusts min and max values to nice, round numbers that divide nicely
     * by 10. (for nice tick labels)
     *
     * @param newMinValue the minimum value that must be selectable on this
     *                    slider
     * @param newMaxValue the maximum value that must be selectable on this
     *                    slider
     * @return a value that represents the decimal shift used to adjust values
     *         (e.g. 0.001, 100, 1000)
     */
    public float roundAndSetSliderRange(float newMinValue, float newMaxValue)
    {
        return sliderControl.roundAndSetSliderRange(newMinValue, newMaxValue);
    }

    private void init(String title, String[] sliderLabels,
                      float minValue, float maxValue)
    {
        plaf = true;
        numThumbs = sliderLabels.length;
        sliderControl =
            new CMultipleSliderControl(sliderLabels, minValue, maxValue);
        init(title);
    }

    private void init(String title, Color[] trackColorFills,
                      float minValue, float maxValue)
    {
        numThumbs = trackColorFills.length - 1;
        sliderControl =
            new COrderedLabeledMThumbSlider(numThumbs, minValue, maxValue);
        CMThumbSlider mthumbSlider =
            ((COrderedLabeledMThumbSlider)sliderControl).getSlider();
        for (int i = 0; i < (trackColorFills.length - 1); i++)
        {
            mthumbSlider.setFillColorAt(trackColorFills[i], i);
        }
        mthumbSlider.setTrackFillColor(
            trackColorFills[trackColorFills.length - 1]);
        init(title);
    }

    private void init(String title)
    {
        setLayout(new BorderLayout(10, 10));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        okButton = new JButton("OK");
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createGlue());
        add(new JLabel(title), BorderLayout.NORTH);
        add((JComponent)sliderControl, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        updateValue();
        resetSize();

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    updateValue();
                }
            });
    }

    /**
     * When look and feel or theme is changed, this method is called.  It
     * ensures that the control size is updated as well.
     */
     public void updateUI()
    {
        super.updateUI();

        resetSize();
    }

    /**
     * Set control to new value
     *
     * @param newSelection new values for control (must be Vector of Numbers)
     */
    public void setSelectedItem(Object newSelection)
    {
        if (newSelection instanceof Vector)
        {
            Vector v = (Vector)newSelection;

            for (int i = 0; i < v.size(); i++)
            {
                Object obj = v.elementAt(i);
                if (obj instanceof Number)
                {
                    Number newNumber = (Number)obj;
                    setValueAt(newNumber.floatValue(), i);
                }
            }
        }
        updateValue();
    }

    /**
     * Get currently selected value
     *
     * @return currently selected value (can be cast to Vector of Numbers)
     */
    public Object getSelectedItem()
    {
        return selectedValue;
    }

    private void resetSize()
    {
        if (sliderControl != null)
        {
            ((JComponent)sliderControl).setPreferredSize(null);
            ((JComponent)sliderControl).setPreferredSize(
                new Dimension(500, ((JComponent)sliderControl).
                                    getPreferredSize().height));
        }
    }

    /**
     * Adds an action listener that is fired whenever the user attempts to make
     * a selection (even if the selectedItem property did not change).
     *
     * @param al the new action listener
     */
     public void addActionListener(ActionListener al)
     {
        okButton.addActionListener(al);
     }

    /**
     * Removes a registered action listener.
     *
     * @param al the existing action listener
     */
    public void removeActionListener(ActionListener al)
    {
        okButton.removeActionListener(al);
    }

    /**
     * get value of thumb at given index.
     *
     * @param index index of thumb
     * @return value of thumb at given index
     */
    public float getValueAt(int index)
    {
        if (plaf)
        {
            return ((CMultipleSliderControl)sliderControl).
                    getSlider(index).getValue();
        }
        else
        {
            return ((COrderedLabeledMThumbSlider)sliderControl).
                    getValueAt(index);
        }
    }

    /**
     * set value of thumb at given index.
     *
     * @param f new value for thumb at given index
     * @param index index of thumb
     */
    public void setValueAt(float f, int index)
    {
        if (plaf)
        {
            ((CMultipleSliderControl)sliderControl).
                getSlider(index).setValue(f);
        }
        else
        {
            ((COrderedLabeledMThumbSlider)sliderControl).setValueAt(f, index);
        }
    }

    private void updateValue()
    {
        Object oldValue = selectedValue;
        selectedValue = new Vector();
        for (int i = 0; i < numThumbs; i++)
        {
            float newValue = getValueAt(i);
            selectedValue.add(new Float(newValue));
        }
        firePropertyChange("selectedItem", oldValue, selectedValue);
    }
}