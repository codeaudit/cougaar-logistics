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

import java.beans.*;
import javax.swing.JFrame;

import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;

/**
 * A control bean used for selecting a range between to floats using sliders.
 * The two sliders are used to select a minimum and maximum value
 *
 * This bean has a bounded property:  "range".
 *
 * @see CMThumbSliderRangeControl
 */
public class CSliderRangeControl extends CMultipleSliderControl
{
    private static String[] sliderLabels = {"From", "To"};

    /** Needed for tracking old values for bounded range property */
    private RangeModel range = new RangeModel(0, 1);

    /**
     * Default constructor.  Creates new slider range control with
     * value range from 0 to 1.
     */
    public CSliderRangeControl()
    {
        super(sliderLabels, 0, 1);

        init();
    }

    /**
     * Creates new slider range control with value range based on
     * given parameters.
     *
     * @param minValue the minimum range value
     * @param maxValue the maximum range value
     */
    public CSliderRangeControl(float minValue, float maxValue)
    {
        super(sliderLabels, minValue, maxValue);

        init();
    }

    /**
     * Initialize the slider range control
     */
    private void init()
    {
        for (int i = 0; i < sliderLabels.length; i++)
        {
            CLabeledSlider slider = getSlider(i);

            // fire property change event when slider is adjusted
            slider.addPropertyChangeListener("value",
                                             new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e)
                    {
                        RangeModel newRange = getRange();
                        if (!range.equals(newRange))
                        {
                            firePropertyChange("range", range, newRange);
                            range = newRange;
                        }
                    }
                });
        }

        setRange(range);
    }

    /**
     * Not needed when compiling/running under jdk1.3
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

   /**
     * Set the selected range.
     *
     * @param r new range
     */
    public void setRange(RangeModel r)
    {
        getSlider(0).setValue(r.getMin());
        getSlider(1).setValue(r.getMax());

        RangeModel newRange = getRange();
        firePropertyChange("range", range, newRange);
        range = newRange;
    }

    /**
     * Get the selected range
     *
     * @return currently selected range
     */
    public RangeModel getRange()
    {
        return new RangeModel(Math.round(getSlider(0).getValue()),
                              Math.round(getSlider(1).getValue()));
    }


    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Range Slider Test");
        java.awt.Container c = frame.getContentPane();
        c.setLayout(new java.awt.GridLayout(0, 1));
        c.add(new CSliderRangeControl(0f, 1f));
        c.add(new CSliderRangeControl(0f, 200f));
        c.add(new CSliderRangeControl(200f, 1000f));
        frame.pack();
        frame.setVisible(true);
    }
}