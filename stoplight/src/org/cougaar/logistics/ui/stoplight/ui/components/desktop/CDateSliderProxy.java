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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.models.*;

/***********************************************************************************************************************
<b>Description</b>: This is an implementation of a SliderProxy for a CDateLabeledSlider slider UI control.  This can be
                    returned from a desktop component that implements the DateControllableSliderUI interface to give
                    control of the components slider to the Date Command Slider desktop component.

***********************************************************************************************************************/
class CDateSliderProxy implements PropertyChangeListener, SliderProxy
{
  private CDateLabeledSlider slider = null;

  private boolean enabled = false;

  private ChangeSupport changeSupport = new ChangeSupport();

  public CDateSliderProxy(CDateLabeledSlider slider)
  {
    this.slider = slider;
  }

  public void addChangeListener(ChangeListener listener)
  {
    changeSupport.addChangeListener(listener);
  }

  public void removeChangeListener(ChangeListener listener)
  {
    changeSupport.removeChangeListener(listener);
  }

  public void enableSlider(boolean enable)
  {
    enabled = enable;
    if (enable)
    {
      slider.getSlider().addPropertyChangeListener(this);
    }
    else
    {
      slider.getSlider().removePropertyChangeListener(this);
    }
  }

  public void dispose()
  {
    slider.getSlider().removePropertyChangeListener(this);
  }

  public float getMinLimit()
  {
    return(slider.getMinValue());
  }

  public float getMaxLimit()
  {
    return(slider.getMaxValue());
  }

  public void setRange(RangeModel range)
  {
    if (enabled)
    {
      float newValue = range.getFMin() + (range.getFMax() - range.getFMin())/2.0f;
      slider.getSlider().removePropertyChangeListener(this);
      slider.setValue(newValue);
      slider.getSlider().addPropertyChangeListener(this);
    }
  }

  public void setValue(float value)
  {
    if (enabled)
    {
      slider.getSlider().removePropertyChangeListener(this);
      slider.setValue(value);
      slider.getSlider().addPropertyChangeListener(this);
    }
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    changeSupport.fireChangeEvent(this);
  }
}
