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

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.*;
import javax.swing.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.COrderedLabeledMThumbSlider;
import org.cougaar.logistics.ui.stoplight.ui.models.StoplightThresholdModel;

/**
 * Four thumbed slider control bean used for selecting the thresholds that
 * designate color changes on a stoplight chart.  The four sliders are used
 * to select min yellow, min green, max green, and max yellow values.  The
 * area between the thumbs that represents a particular range that should be
 * encoded in a single color is colored with that color.<BR><BR>
 *
 * ---red---|----yellow----|----green----|-----yellow----|----red----<BR><BR>
 *
 * This bean has a bounded property:  "thresholds".
 */
public class CMThumbSliderThresholdControl extends COrderedLabeledMThumbSlider
{
    /** Needed for tracking old values for bounded thresholds property */
    private StoplightThresholdModel thresholds = new StoplightThresholdModel();

    private static final int NUMBER_OF_THUMBS = 4;
    private boolean upperThresholds = true;

    /**
     * Default constructor.  Creates a new threshold slider with a minimum
     * value of 0 and a maximum value of 2.
     */
    public CMThumbSliderThresholdControl()
    {
        super(NUMBER_OF_THUMBS, 0, 2);

        init(0, 2);
    }

    /**
     * Creates a new threshold slider with given minimum and maximum values.
     *
     * @param minValue minimum value for thresholds
     * @param maxValue minimum value for thresholds
     */
    public CMThumbSliderThresholdControl(float minValue, float maxValue)
    {
        super(NUMBER_OF_THUMBS, minValue, maxValue);

        init(minValue, maxValue);
    }

    /**
     * Initialize the threshold slider
     *
     * @param minValue minimum value for thresholds
     * @param maxValue minimum value for thresholds
     */
    private void init(float minValue, float maxValue)
    {
        slider.setFillColorAt(Color.red, 0);
        slider.setFillColorAt(Color.yellow, 1);
        slider.setFillColorAt(Color.green, 2);
        slider.setFillColorAt(Color.yellow, 3);
        slider.setTrackFillColor(Color.red);

        slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e)
                {
                    StoplightThresholdModel newThresholds = getThresholds();
                    if (!thresholds.equals(newThresholds))
                    {
                        firePropertyChange("thresholds", thresholds,
                                           newThresholds);
                        thresholds = newThresholds;
                    }
                }
            });

        evenlyDistributeValues();
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
     * Set the selected thresholds.
     *
     * @param tr new thresholds
     */
    public void setThresholds(StoplightThresholdModel tr)
    {
        slider.setValueAt(toSlider(tr.getYellowMin()), 0);
        slider.setValueAt(toSlider(tr.getGreenMin()), 1);
        if (upperThresholds)
        {
          slider.setValueAt(toSlider(tr.getGreenMax()), 2);
          slider.setValueAt(toSlider(tr.getYellowMax()), 3);
        }

        StoplightThresholdModel newThresholds = getThresholds();
        firePropertyChange("thresholds", thresholds, newThresholds);
        thresholds = newThresholds;
    }

    /**
     * Get the selected thresholds
     *
     * @return currently selected thresholds
     */
    public StoplightThresholdModel getThresholds()
    {
        StoplightThresholdModel th = null;

        if (upperThresholds)
        {
            th = new StoplightThresholdModel(fromSlider(slider.getValueAt(0)),
                                             fromSlider(slider.getValueAt(1)),
                                             fromSlider(slider.getValueAt(2)),
                                             fromSlider(slider.getValueAt(3)));
        }
        else
        {
            th = new StoplightThresholdModel(fromSlider(slider.getValueAt(0)),
                                             fromSlider(slider.getValueAt(1)),
                                             Float.POSITIVE_INFINITY,
                                             Float.POSITIVE_INFINITY);
        }

        return th;
    }

    /**
     * Set whether or not to impose upper thresholds
     *
     * @param upperThresholds if false, no upper threholds will be imposed
     *                          i.e. the top two thumbs will be deactivated
     *                          and will disappear.
     */
    public void setUpperThresholds(boolean upperThresholds)
    {
        this.upperThresholds = upperThresholds;

        if (upperThresholds)
        {
            slider.setFillColorAt(Color.yellow, 3);
            slider.setTrackFillColor(Color.red);
            showThumbAt(2);
            showThumbAt(3);
        }
        else
        {
            slider.setFillColorAt(Color.green, 3);
            slider.setTrackFillColor(Color.green);
            hideThumbAt(2);
            hideThumbAt(3);
        }

        setThresholds(getThresholds());
    }

    /**
     * Get whether or not control is imposing upper thresholds
     *
     * @return if true, no upper threholds are being imposed
     *         i.e. the top two thumbs are deactivated and not visible
     */
    public boolean getUpperThresholds()
    {
        return upperThresholds;
    }

    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String args[])
    {
        JFrame frame = new JFrame();
        CMThumbSliderThresholdControl tc = new CMThumbSliderThresholdControl();
        frame.getContentPane().add(tc, BorderLayout.CENTER);
        frame.setSize(400, 100);
        frame.setVisible(true);
    }

}