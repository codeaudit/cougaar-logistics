/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
import java.beans.*;
import java.text.DecimalFormat;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.event.*;

import com.bbn.openmap.gui.Tool;

/**
 * A bean that creates a swing slider with a static label and a dynamic label.
 * The dynamic label shows the current value selected.  This slider supports
 * setting of floating point values as well as integer values.
 *
 * This bean has a bounded property: "value".
 */
public class CLabeledSlider extends JPanel implements Tool
{
    protected int fidelity = 1000;

    /** the spacing between major tick marks on slider */
    protected int majorTickSpacing = fidelity/10;

    /** the static label */
    protected JLabel label;

    /** the dynamic label */
    protected JLabel valueLabel;

    /** the slider */
    protected JSlider slider;

    /** panel that holds the static and dynamic labels */
    protected JPanel labelPanel;

    /** format used to format the dynamic value label */
    protected DecimalFormat labelFormat;

    /** minimum value of slider */
    protected float minValue = 0f;

    /** maximum value of slider */
    protected float maxValue = 0f;

    /** the floating point value that corresponds to 1 unit on the slider */
    protected float unit =  0f;

    protected final String defaultToolKey = "clabeledslider";
    protected String toolKey = new String (defaultToolKey);

    /**
     * default constructor.  Creates new labeled slider with "No Name" as label
     * and range from 0 to 2.
     */
    public CLabeledSlider()
    {
        super(new BorderLayout());

        init("No Name", 100, 0, 2);
    }

    /**
     * Creates new labeled slider.
     *
     * @param labelString the static label for the slider
     * @param labelWidth the width of the area to reserve for the label.
     *                   Needed for lining up the labeled sliders on top of
     *                   each other.
     * @param minValue   The minimum value of the slider
     * @param maxValue   The maximum value of the slider
     */
    public CLabeledSlider(String labelString, int labelWidth, float minValue,
                         float maxValue)
    {
        super(new BorderLayout());

        init(labelString, labelWidth, minValue, maxValue);
    }

    /**
     * Sets the static label of the slider
     *
     * @param labelString new static label
     */
    public void setLabel(String labelString)
    {
        label.setText(labelString);
    }

    /**
     * Sets the width of the static label
     *
     * @param labelWidth the width of the area to reserve for the label.
     *                   Needed for lining up the labeled sliders on top of
     *                   each other.
     */
    public void setLabelWidth(int labelWidth)
    {
        labelPanel.setPreferredSize(null);
        labelPanel.setPreferredSize(
            new Dimension(labelWidth,
                          (int)labelPanel.getPreferredSize().getHeight()));
    }

    /**
     * Calculate the minimum label width for given string
     *
     * @param labelString label to calculate with for
     * @return the minimum width for this label
     */
     public int getMinimumLabelWidth(String labelString)
     {
        int minLabelWidth =
            getFontMetrics(label.getFont()).stringWidth(labelString+"00000");
        return minLabelWidth;
     }

    /**
     * Gets the static label of the slider
     *
     * @return labelString the static label
     */
    public String getLabel()
    {
        return label.getText();
    }

    /**
     * Set the fidelity of this slider (i.e. the number of selectable values)
     *
     * @param fidelity the new fidelity of this slider (i.e. the number of
     *                 selectable values)
     */
    public void setFidelity(int fidelity)
    {
        this.fidelity = fidelity;
        slider.setMaximum(fidelity);
        majorTickSpacing = fidelity/10;
        slider.setMinorTickSpacing(majorTickSpacing/2);
        slider.setMajorTickSpacing(majorTickSpacing);
        setSliderRange(minValue, maxValue);
    }

    /**
     * Get the minimum value of this slider
     *
     * @return the minimum value of this slider
     */
    public float getMinValue()
    {
        return minValue;
    }

    /**
     * Set the minimum value of this slider
     *
     * @param minValue the minimum value of this slider
     */
    public void setMinValue(float minValue)
    {
        setSliderRange(minValue, maxValue);
    }

    /**
     * Get the maximum value of this slider
     *
     * @return the maximum value of this slider
     */
    public float getMaxValue()
    {
        return maxValue;
    }

    /**
     * Set the maximum value of this slider
     *
     * @param maxValue the maximum value of this slider
     */
    public void setMaxValue(float maxValue)
    {
        setSliderRange(minValue, maxValue);
    }

    /**
     * Get the actual JSlider that is wrapped by this component
     *
     * @return the actual JSlider that is wrapped by this component
     */
    public JSlider getSlider()
    {
        return slider;
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
        float difference = newMaxValue - newMinValue;
        double log10 = Math.log(10);
        if (difference == 0)
        {
            float adjustment =
                (float)Math.pow(10, Math.log((double)newMaxValue)/log10);
            newMaxValue += adjustment;
            newMinValue -= adjustment;
            difference = newMaxValue - newMinValue;
        }
        float shift =
            (float)Math.pow(0.1, Math.floor(Math.log(difference)/log10));
        newMinValue = (float)(Math.floor(newMinValue * shift) / shift);
        newMaxValue = (float)(Math.ceil(newMaxValue * shift) / shift);
        setSliderRange(newMinValue, newMaxValue);
        return shift;
    }

    /**
     * Set the minimum and maximum value of this slider.
     *
     * @param minValue the new minimum value of this slider
     * @param maxValue the new maximum value of this slider
     */
    public void setSliderRange(float minValue, float maxValue)
    {
        // Try to maintain the same value if possible
        float currentValue = getValue();

        this.minValue = minValue;
        this.maxValue = maxValue;
        unit = (maxValue - minValue) / fidelity;

        if (Math.abs(maxValue) > 10)
        {
            labelFormat = new DecimalFormat("####");
        }
        else
        {
            labelFormat = new DecimalFormat("##.##");
        }

        Hashtable valueLabels = new Hashtable();
        for (int i = 0; i <= fidelity; i += majorTickSpacing)
        {
            valueLabels.put(new Integer(i),
                            new JLabel(labelFormat.format(fromSlider(i))));
        }
        slider.setLabelTable(valueLabels);

        valueLabel.setText(getStringValue());
        setValue(currentValue);
    }

    /**
     * Initializes new labeled slider.
     *
     * @param labelString the static label for the slider
     * @param labelWidth the width of the area to reserve for the label.
     *                   Needed for lining up the labeled sliders on top of
     *                   each other.
     * @param minValue   The minimum value of the slider
     * @param maxValue   The maximum value of the slider
     */
    protected void init(String labelString, int labelWidth, float minValue,
                      float maxValue)
    {
        labelPanel = new JPanel(new BorderLayout());
        label = new JLabel(labelString + ": ");
        label.setVerticalAlignment(JLabel.TOP);
        labelPanel.add(label, BorderLayout.WEST);
        valueLabel = new JLabel();
        labelPanel.add(valueLabel, BorderLayout.CENTER);
        setLabelWidth(labelWidth);
        valueLabel.setVerticalAlignment(JLabel.TOP);
        valueLabel.setHorizontalAlignment(JLabel.RIGHT);
        add(labelPanel, BorderLayout.WEST);

        slider = new JSlider(0, fidelity);
        majorTickSpacing = fidelity/10;
        slider.setMinorTickSpacing(majorTickSpacing/2);
        slider.setMajorTickSpacing(majorTickSpacing);
        add(slider, BorderLayout.CENTER);

        slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e)
                {
                    float oldValue = Float.parseFloat(valueLabel.getText());
                    valueLabel.setText(getStringValue());
                    LabeledSlider:firePropertyChange("value", oldValue,
                                                     getValue());
                }
            });

        setSliderRange(minValue, maxValue);
    }

    /**
     * Gets formated string representation of the value of slider
     *
     * return string representation of the value of slider
     */
    private String getStringValue()
    {
        return labelFormat.format(getValue());
    }

    /**
     * Set whether to show labeled tick marks under slider.
     *
     * @param showTicks if true, slider will have labeled tick marks.
     */
    public void setShowTicks(boolean showTicks)
    {
        slider.setPaintLabels(showTicks);
        slider.setPaintTicks(showTicks);
    }

    /**
     * Get whether slider is showing labeled tick marks.
     *
     * @return if true, slider is showing labeled tick marks.
     *
     */
    public boolean getShowTicks()
    {
        return slider.getPaintTicks();
    }

    /**
     * Returns the value at which the slider is currently set.
     *
     * return the value at which the slider is currently set
     */
    public float getValue()
    {
        return fromSlider(slider.getValue());
    }

    /**
     * Sets the value of the slider
     *
     * @param value the new value for the slider.
     */
    public void setValue(float value)
    {
        float oldValue = getValue();
        slider.setValue(toSlider(value));
        firePropertyChange("value", oldValue, getValue());
    }

    /**
     * Adds a change listener to the slider
     *
     * @param changeListener change listener to add to slider
     */
    public void addChangeListener(ChangeListener changeListener)
    {
        slider.addChangeListener(changeListener);
    }

    /**
     * Remove a change listener from the slider
     *
     * @param changeListener change listener to remove from slider
     */
    public void removeChangeListener(ChangeListener changeListener)
    {
        slider.removeChangeListener(changeListener);
    }

    /**
     * Translates from float value to slider's integer value.
     *
     * @param f "value" float value
     * @return slider's integer value
     */
    private int toSlider(float f)
    {
        return Math.round((f - minValue) / unit);
    }

    /**
     * Translates from slider's integer value to float value.
     *
     * @param i slider's integer value
     * @return "value" float value
     */
    protected float fromSlider(int i)
    {
        return minValue + (unit * i);
    }

   /**
    * This is a workaround for an appearent Swing bug.  Without this method,
    * JSlider will not appear when placed in some types of top level
    * containers (e.g. PopupMenus).  Only first call to this method will
    * invoke workaround functionality.
    *
    * @param g graphics context for this component.
    */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (updateUIAfterPaint)
        {
            SwingUtilities.updateComponentTreeUI(this);
            updateUIAfterPaint = false;
        }
    }
    private boolean updateUIAfterPaint = true;

    /** 
     * Tool interface method.  The retrieval key for this tool.
     *
     * @return String The key for this tool.
     */
    public String getKey()
    {
       return toolKey;
    }

    /**
     * Tool interface method. Set the retrieval key for this tool.
     *
     * @param aKey The key for this tool.
     */
    public void setKey(String aKey)
    {
      toolKey = aKey;
    }

    /**
     * Tool interface method. The retrieval tool's interface. This is
     * added to the tool bar.
     *
     * @return String The key for this tool.
     */
    public Container getFace()
    {
      return this;
    }

    /**
     * main for unit testing
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Slider Test");
        Container c = frame.getContentPane();
        c.setLayout(new GridLayout(0, 1));
        c.add(new CLabeledSlider("Test", 80, 0f, 1f));
        c.add(new CLabeledSlider("Test", 80, 0f, 200f));
        c.add(new CLabeledSlider("Test", 80, 200f, 1000f));
        frame.pack();
        frame.setVisible(true);
    }
}
