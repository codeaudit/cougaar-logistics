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

package org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.cougaar.logistics.ui.stoplight.ui.util.SliderControl;

/**
  * This class is used to create muliple thumbed sliders whose value order
  * is enforced (slider(n) must be less than or equal to slider(n+1)).
  * Also, current value labels are added that float above each slider thumb.
  */
public class COrderedLabeledMThumbSlider
    extends JPanel implements SliderControl
{
    protected static int FIDELITY = 1000;
    protected static final int MAJOR_TICK_SPACING = FIDELITY/5;
    protected int numThumbs = 0;
    protected float minValue = 0f;
    protected float maxValue = 0f;
    protected float unit =  0f;

    protected double minDValue = 0f;
    protected double maxDValue = 0f;
    protected double unitD =  0f;

    protected CMThumbSlider slider;
    protected DecimalFormat labelFormat;

    protected static Icon invisibleIcon = new ImageIcon(new byte[]{});

    /**
     * Default constructor.  Create a new mulitple thumbed slider with 5
     * thumbs, min value of 0, max value of 1, and unique track colors between
     * each thumb.
     */
    public COrderedLabeledMThumbSlider()
    {
        super(new BorderLayout());

        this.numThumbs = 5;
        initialize(0, 1);

        slider.setFillColorAt(Color.red, 0);
        slider.setFillColorAt(Color.orange, 1);
        slider.setFillColorAt(Color.yellow, 2);
        slider.setFillColorAt(Color.green, 3);
        slider.setFillColorAt(Color.blue, 4);
        slider.setTrackFillColor(Color.magenta);
        evenlyDistributeValues();
    }

    /**
     * Create a new mulitple thumbed slider with the given number of thumbs
     *
     * @param numThumbs the number of thumb for slider control.
     * @param minValue the minimum value for this slider
     * @param maxValue the maximum value for this slider
     */
    public COrderedLabeledMThumbSlider(int numThumbs, float minValue, float maxValue)
    {
        super(new BorderLayout());

        this.numThumbs = numThumbs;
        initialize(minValue, maxValue);
    }

    /**
     * Create a new mulitple thumbed slider with the given number of thumbs
     *
     * @param numThumbs the number of thumb for slider control.
     * @param minValue the minimum value for this slider
     * @param maxValue the maximum value for this slider
     */
    public COrderedLabeledMThumbSlider(int numThumbs, double minValue, double maxValue)
    {
        super(new BorderLayout());

        this.numThumbs = numThumbs;
        initialize(minValue, maxValue);
    }

    /**
     * Get the slider control contained by this labeled panel
     *
     * @return the slider control contained by this labeled panel
     */
    public CMThumbSlider getSlider()
    {
        return slider;
    }

    /**
     * Called to initialize component with minimum and maximum range values.
     *
     * @param minValue the minimum value for this slider
     * @param maxValue the maximum value for this slider
     */
    protected void initialize(double minValue, double maxValue)
    {
        slider = new CMThumbSlider(numThumbs);
        slider.setMaximum(FIDELITY);
        slider.setOpaque(false);
        slider.putClientProperty( "JSlider.isFilled", Boolean.TRUE );
        add(slider, BorderLayout.CENTER);

        setSliderRange(minValue, maxValue);

        for (int i = 0; i < numThumbs; i++)
        {
            BoundedRangeModel model = slider.getModelAt(i);
            model.addChangeListener(new OrderChangeListener(i));
        }

        slider.setMinorTickSpacing(MAJOR_TICK_SPACING / 2);
        slider.setMajorTickSpacing(MAJOR_TICK_SPACING);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);

        slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e)
                {
                    validate();
                    repaint();
                }
            });

        adjustValueLabelHeight();
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
     * Adjusts all values such that thumbs are evenly distributed and ordered
     * from first to last.
     */
    public void evenlyDistributeValues()
    {
        double defaultSeperation = (maxDValue - minDValue)/(numThumbs + 1);
        for (int i = 0; i < numThumbs; i++)
        {
            slider.setValueAt(toSlider(minDValue + defaultSeperation * (i + 1)), i);
        }
        SwingUtilities.updateComponentTreeUI(this);
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
     * Get the minimum value of this slider
     *
     * @return the minimum value of this slider
     */
    public double getMinDValue()
    {
        return minDValue;
    }

    /**
     * Set the minimum value of this slider
     *
     * @param minValue the minimum value of this slider
     */
    public void setMinValue(float minValue)
    {
        setSliderRange(minValue, maxDValue);
    }

    /**
     * Set the minimum value of this slider
     *
     * @param minValue the minimum value of this slider
     */
    public void setMinValue(double minValue)
    {
        setSliderRange(minValue, maxDValue);
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
     * Get the maximum value of this slider
     *
     * @return the maximum value of this slider
     */
    public double getMaxDValue()
    {
        return maxDValue;
    }

    /**
     * Set the maximum value of this slider
     *
     * @param maxValue the maximum value of this slider
     */
    public void setMaxValue(float maxValue)
    {
        setSliderRange(minDValue, maxValue);
    }

    /**
     * Set the maximum value of this slider
     *
     * @param maxValue the maximum value of this slider
     */
    public void setMaxValue(double maxValue)
    {
        setSliderRange(minDValue, maxValue);
    }

    /**
     * Set the minimum and maximum value of this slider.
     *
     * @param minValue the new minimum value of this slider
     * @param maxValue the new maximum value of this slider
     */
    public void setSliderRange(double minValue, double maxValue)
    {
        // Try to maintain the same values if possible
        Vector currentValues = new Vector();
        for (int i = 0; i < numThumbs; i++)
        {
//            currentValues.add(new Float(fromSlider(slider.getValueAt(i))));
            currentValues.add(new Double(fromDSlider(slider.getValueAt(i))));
        }

        this.minDValue = minValue;
        this.maxDValue = maxValue;
        this.minValue = (float)minValue;
        this.maxValue = (float)maxValue;

        unit = (float)((maxValue - minValue) / FIDELITY);
        unitD = (maxDValue - minDValue) / FIDELITY;
        
        if (Math.abs(maxDValue) > 10)
        {
            labelFormat = new DecimalFormat("####");
        }
        else
        {
            labelFormat = new DecimalFormat("##.##");
        }

        int sliderMin = toSlider(minDValue);
        int sliderMax = toSlider(maxDValue);
        for (int i = 0; i < numThumbs; i++)
        {
            BoundedRangeModel model = slider.getModelAt(i);
            model.setMaximum(sliderMax);
            model.setMinimum(sliderMin);
        }

        Hashtable valueLabels = new Hashtable();
        for (int i = 0; i <= FIDELITY; i += MAJOR_TICK_SPACING)
        {
//            valueLabels.put(new Integer(i), new JLabel(labelFormat.format(fromSlider(i))));
            valueLabels.put(new Integer(i), new JLabel(labelFormat.format(fromDSlider(i))));
        }
        slider.setLabelTable(valueLabels);

        // Set sliders to old current values
        for (int i = 0; i < currentValues.size(); i++)
        {
            Number currentValue = (Number)currentValues.elementAt(i);
            slider.setValueAt(toSlider(currentValue.doubleValue()), i);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    /**
     * When look and feel or theme is changed, this method is called.  It
     * resizes the component based on new font sizes.
     */
    public void updateUI()
    {
        super.updateUI();

        adjustValueLabelHeight();
    }

    protected void adjustValueLabelHeight()
    {
      if (dynamicLabelsVisible)
      {
        if (dynamicLabelStrut != null)
        {
          remove(dynamicLabelStrut);
        }

        // Create space for value labels in north quad. of component
        int fontHeight = getFontMetrics(MetalLookAndFeel.getSystemTextFont()).getHeight();
        dynamicLabelStrut = Box.createVerticalStrut(fontHeight);
        add(dynamicLabelStrut, BorderLayout.NORTH);
      }
      else if (dynamicLabelStrut != null)
      {
        remove(dynamicLabelStrut);
      }
    }

    protected Component dynamicLabelStrut = null;

    protected boolean dynamicLabelsVisible = true;

	/*********************************************************************************************************************
  <b>Description</b>: Enables/disables the dynamic labels above thumbs.

  <br>
  @param visible True if the dynamc labels should be visible, false otherwise
	*********************************************************************************************************************/
    public void setDynamicLabelsVisible(boolean visible)
    {
      dynamicLabelsVisible = visible;
      adjustValueLabelHeight();
    }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/disables drawing the tic labels below the slider track.

  <br>
  @param draw True if the tic labels should be drawn, false otherwise
	*********************************************************************************************************************/
    public void setDrawTickLabels(boolean draw)
    {
      slider.setPaintLabels(draw);
      slider.setPaintTicks(draw);
    }

	/*********************************************************************************************************************
  <b>Description</b>: Shows the thumb at the specified index.

  <br>
  @param index Index of the thumb to show
	*********************************************************************************************************************/
    public void showThumbAt(int index)
    {
        slider.setThumbRendererAt(null, index);
    }

	/*********************************************************************************************************************
  <b>Description</b>: Hides the thumb at the specified index.

  <br>
  @param index Index of the thumb to hide
	*********************************************************************************************************************/
    public void hideThumbAt(int index)
    {
        slider.setThumbRendererAt(invisibleIcon, index);
    }

    /**
     * Paints floating value labels over thumbs of slider.
     *
     * Also includes a workaround for an appearent Swing bug.  Without this
     * workaround, JSlider will not appear when placed in some types of top
     * level containers (e.g. PopupMenus).  Only first call to this method will
     * invoke workaround functionality.
     */
    public void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      if (dynamicLabelsVisible)
      {
        g.setFont(MetalLookAndFeel.getSystemTextFont());
        FontMetrics fm = getFontMetrics(g.getFont());

        // paint dynamic value labels on component
        for (int i = 0; i < numThumbs; i++)
        {
          if (slider.getThumbRendererAt(i) != invisibleIcon)
          {
//            String label = labelFormat.format(fromSlider(slider.getValueAt(i)));
            String label = labelFormat.format(fromDSlider(slider.getValueAt(i)));
            int labelWidth = fm.stringWidth(label);
            int borderOffset = (getBorder() == null) ? 0 : getBorder().getBorderInsets(this).left;
            int thumbXLoc = slider.getThumbXLoc(i) + borderOffset;
            g.drawString(label, thumbXLoc - (labelWidth / 2), (int)slider.getLocation().getY()-5);
          }
        }
      }

      // Swing bug workaround
      if (updateUIAfterPaint)
      {
        SwingUtilities.updateComponentTreeUI(this);
        updateUIAfterPaint = false;
      }
    }

    protected boolean updateUIAfterPaint = true;

    /**
     * get value of thumb at given index.
     *
     * @param index index of thumb
     * @return value of thumb at given index
     */
    public float getValueAt(int index)
    {
        return fromSlider(slider.getValueAt(index));
    }

    /**
     * get value of thumb at given index.
     *
     * @param index index of thumb
     * @return value of thumb at given index
     */
    public double getDValueAt(int index)
    {
        return fromDSlider(slider.getValueAt(index));
    }

    /**
     * set value of thumb at given index.
     *
     * @param d new value for thumb at given index
     * @param index index of thumb
    */
    public void setValueAt(double d, int index)
    {
        slider.setValueAt(toSlider(d), index);
    }

    /**
     * Translates from float value to slider's integer value.
     *
     * @param d "value" float value
     * @return slider's integer value
     */
    public int toSlider(double d)
    {
        return((int)Math.round((d - minDValue) / unitD));
    }

    /**
     * Translates from slider's integer value to float value.
     *
     * @param i slider's integer value
     * @return "value" float value
     */
    public float fromSlider(int i)
    {
        return minValue + (unit * i);
    }

    /**
     * Translates from slider's integer value to float value.
     *
     * @param i slider's integer value
     * @return "value" float value
     */
    public double fromDSlider(int i)
    {
        return(minDValue + (unitD * i));
    }

    /**
     * Private class used to ensure that the user doesn't select invalid
     * values (slider(n) must be less than or equal to slider(n+1))
     */
    class OrderChangeListener implements ChangeListener
    {
        int myPosition = 0;

        public OrderChangeListener(int myPosition)
        {
            this.myPosition = myPosition;
        }

        public void stateChanged(ChangeEvent e)
        {
            int myValue = slider.getModelAt(myPosition).getValue();

            for (int i = 0; i < myPosition; i++)
            {
                BoundedRangeModel otherModel = slider.getModelAt(i);
                if (otherModel.getValue() > myValue)
                {
                    otherModel.setValue(myValue);
                }
            }

            for (int i = myPosition + 1; i < numThumbs; i++)
            {
                BoundedRangeModel otherModel = slider.getModelAt(i);
                if (otherModel.getValue() < myValue)
                {
                    otherModel.setValue(myValue);
                }
            }
        }
    }
}
