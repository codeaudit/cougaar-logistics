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

import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * A UI control used to select features of a graph.  Currently only supports
 * toggling the show grid option.
 */
public class CGraphFeatureSelectionControl extends JPanel
{
    /** the checkbox used to get user input on whether to show grid. */
    private JCheckBox showGridCB = new JCheckBox("Show Grid");

    /** default constructor.  Will create new control. */
    public CGraphFeatureSelectionControl()
    {
        super();

        add(showGridCB);
    }

    /**
     * Set selected state of show grid checkbox.
     *
     * @param showGrid new state of show grid checkbox.
     */
    public void setShowGrid(boolean showGrid)
    {
        showGridCB.setSelected(showGrid);
    }

    /**
     * Get selected state of show grid checkbox.
     *
     * @return the state of show grid checkbox.
     */
    public boolean getShowGrid()
    {
        return showGridCB.isSelected();
    }

    /**
     * Add action listener that fires whenever any graph feature is changed
     *
     * @param al action listener to add
     */
    public void addActionListener(ActionListener al)
    {
        showGridCB.addActionListener(al);
    }

    /**
     * Remove action listener that fires whenever any graph feature is changed
     *
     * @param al action listener to remove
     */
    public void removeActionListener(ActionListener al)
    {
        showGridCB.removeActionListener(al);
    }
}