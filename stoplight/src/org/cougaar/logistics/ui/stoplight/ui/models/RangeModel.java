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

/**
 * A simple range comprised of a minimum and maximum integer.
 */
public class RangeModel
{
    private int min = 0;
    private int max = 0;

    private float fMin = 0.0f;
    private float fMax = 0.0f;

    private double dMin = 0.0f;
    private double dMax = 0.0f;

    /**
     * Create a new range object
     *
     * @param min the minimum value for range.
     * @param max the maximum value for range.
     */
    public RangeModel(int min, int max)
    {
        this.min = min;
        this.max = max;
        fMin = (float)min;
        fMax = (float)max;
        dMin = (double)min;
        dMax = (double)max;
    }

    /**
     * Create a new range object
     *
     * @param min the minimum value for range.
     * @param max the maximum value for range.
     */
    public RangeModel(float min, float max)
    {
        fMin = min;
        fMax = max;
        dMin = (double)min;
        dMax = (double)max;
        this.min = Math.round(min);
        this.max = Math.round(max);
    }

    /**
     * Create a new range object
     *
     * @param min the minimum value for range.
     * @param max the maximum value for range.
     */
    public RangeModel(double min, double max)
    {
        dMin = min;
        dMax = max;
        fMin = (float)min;
        fMax = (float)max;
        this.min = (int)Math.round(min);
        this.max = (int)Math.round(max);
    }

    /**
     * Set the minimum value for range
     *
     * @param min the new minimum value for range
     */
    public void setMin(int min)
    {
        this.min = min;
        fMin = (float)min;
        dMin = (double)min;
    }

    /**
     * Get the minimum value for range
     *
     * @return the current minimum value for range
     */
    public int getMin()
    {
        return min;
    }

    /**
     * Set the maximum value for range
     *
     * @param max the new maximum value for range
     */
    public void setMax(int max)
    {
        this.max = max;
        fMax = (float)max;
        dMax = (double)max;
    }

    /**
     * Get the maximum value for range
     *
     * @return the current maximum value for range
     */
     public int getMax()
    {
        return max;
    }

    /**
     * Set the minimum value for range
     *
     * @param min the new minimum value for range
     */
    public void setFMin(float min)
    {
        fMin = min;
        dMin = (double)min;
        this.min = Math.round(min);
    }

    /**
     * Get the minimum value for range
     *
     * @return the current minimum value for range
     */
    public float getFMin()
    {
        return(fMin);
    }

    /**
     * Set the maximum value for range
     *
     * @param max the new maximum value for range
     */
    public void setFMax(float max)
    {
        fMax = max;
        dMax = (double)max;
        this.max = Math.round(max);
    }

    /**
     * Get the maximum value for range
     *
     * @return the current maximum value for range
     */
     public float getFMax()
    {
        return(fMax);
    }

    /**
     * Set the minimum value for range
     *
     * @param min the new minimum value for range
     */
    public void setDMin(double min)
    {
        fMin = (float)min;
        dMin = min;
        this.min = (int)Math.round(min);
    }

    /**
     * Get the minimum value for range
     *
     * @return the current minimum value for range
     */
    public double getDMin()
    {
        return(dMin);
    }

    /**
     * Set the maximum value for range
     *
     * @param max the new maximum value for range
     */
    public void setDMax(double max)
    {
        fMax = (float)max;
        dMax = max;
        this.max = (int)Math.round(max);
    }

    /**
     * Get the maximum value for range
     *
     * @return the current maximum value for range
     */
     public double getDMax()
    {
        return(dMax);
    }

    /**
     * represent range as string
     *
     * @return string representation of range
     */
    public String toString()
    {
        return (String.valueOf(dMin) + "-" + String.valueOf(dMax));
    }

    /**
     * check for equality between ranges
     *
     * @param o object to compare with range
     * @return true if range equals given object
     */
    public boolean equals(Object o)
    {
        if (o instanceof RangeModel)
        {
            RangeModel r = (RangeModel)o;
            if ((r.dMin == dMin) && (r.dMax == dMax))
            {
                return true;
            }
        }
        return false;
    }
}
