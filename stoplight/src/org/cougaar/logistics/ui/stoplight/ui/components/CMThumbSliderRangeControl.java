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

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.*;
import javax.swing.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.COrderedLabeledMThumbSlider;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;

/**
 * A two thumbed slider bean that is used for selecting a range between two
 * values.  Area between thumbs that represents the selected range is
 * highlighted red.
 *
 * This bean has a bounded property:  "range"
 */
public class CMThumbSliderRangeControl extends COrderedLabeledMThumbSlider
{
    /** Needed for tracking old values for bounded thresholds property */
    private RangeModel range = new RangeModel(0, 30);

    private static final int NUMBER_OF_THUMBS = 2;

    /**
     * Default constructor.  Creates new range slider with minimum value 0 and
     * maximum value 30.
     */
    public CMThumbSliderRangeControl()
    {
        super(NUMBER_OF_THUMBS, 0, 30);

        init(0, 30);
    }

    /**
     * Creates a new range slider with given minimum and maximum values.
     *
     * @param minValue minimum setting for range
     * @param maxValue maximum value for range
     */
    public CMThumbSliderRangeControl(float minValue, float maxValue)
    {
        super(NUMBER_OF_THUMBS, minValue, maxValue);

        init(minValue, maxValue);
    }

    /**
     * Creates a new range slider with given minimum and maximum values.
     *
     * @param minValue minimum setting for range
     * @param maxValue maximum value for range
     */
    public CMThumbSliderRangeControl(double minValue, double maxValue)
    {
        super(NUMBER_OF_THUMBS, minValue, maxValue);

        init(minValue, maxValue);
    }

    /**
     * Initialize range control.
     *
     * @param minValue minimum setting for range
     * @param maxValue maximum value for range
     */
    private void init(double minValue, double maxValue)
    {
        slider.setFillColorAt(Color.red, 1);

        setRange(range);

        slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e)
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

    /**
     * Not needed when compiling/running under jdk1.3
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Set selected range
     *
     * @param range new range
     */
    public void setRange(RangeModel range)
    {
        slider.setValueAt(toSlider(range.getDMin()), 0);
        slider.setValueAt(toSlider(range.getDMax()), 1);

        RangeModel newRange = getRange();
        firePropertyChange("range", range, newRange);
        range = newRange;
    }

    /**
     * Get selected range
     *
     * @return the currently selected range.
     */
    public RangeModel getRange()
    {
        return new RangeModel(fromDSlider(slider.getValueAt(0)), fromDSlider(slider.getValueAt(1)));
    }

    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String args[])
    {
        JFrame frame = new JFrame();
        CMThumbSliderRangeControl rc = new CMThumbSliderRangeControl();
        frame.getContentPane().add(rc, BorderLayout.CENTER);
        frame.setSize(400, 100);
        frame.setVisible(true);
    }

}