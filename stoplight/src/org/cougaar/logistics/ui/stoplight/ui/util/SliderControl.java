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

public interface SliderControl
{
    /**
     * Get the minimum value of the slider control
     *
     * @return the minimum value of the slider control
     */
    public float getMinValue();

    /**
     * Set the minimum value of the slider control
     *
     * @param minValue the minimum value of the slider control
     */
    public void setMinValue(float minValue);

    /**
     * Get the maximum value of the slider control
     *
     * @return the maximum value of the slider control
     */
    public float getMaxValue();

    /**
     * Set the maximum value of the slider control
     *
     * @param maxValue the maximum value of the slider control
     */
    public void setMaxValue(float maxValue);

    /**
     * Adjusts all values such that thumbs are evenly distributed and ordered
     * from first to last.
     */
    public void evenlyDistributeValues();

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
    public float roundAndSetSliderRange(float newMinValue, float newMaxValue);
}