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

import java.awt.event.*;
import javax.swing.JComboBox;

import org.cougaar.logistics.ui.stoplight.ui.util.Selector;

/**
 * This class is a JComboBox that implements the Selector
 * interface.  <BR><BR>
 *
 * This bean has bound property:  "selectedItem"
 */
public class CComboSelector extends JComboBox implements Selector
{
    private Object selectedItem;

    /**
     * Default constructor.  Create a new, empty combo box.
     */
    public CComboSelector()
    {
        super();
        init();
    }

    /**
     * Create a new combo selector filled with the given selection options.
     *
     * @param items array of objects to use as selections.
     */
    public CComboSelector(Object[] items)
    {
        super(items);
        init();
    }

    private void init()
    {
        selectedItem = getSelectedItem();
        addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    Object oldSelectedItem = selectedItem;
                    selectedItem = getSelectedItem();
                    if (selectedItem == null)
                    {
                        selectedItem = oldSelectedItem;
                        setSelectedItem(selectedItem);
                    }
                    firePropertyChange("selectedItem", oldSelectedItem,
                                       selectedItem);
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
}