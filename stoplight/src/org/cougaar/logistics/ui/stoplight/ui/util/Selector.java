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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/**
 * Interface to designate a control that can be used to select a single item
 * (Object) in some way.
 */
public interface Selector
{
    /**
     * Get the item that is currently selected.
     *
     * @return the item that is currently selected
     */
    public Object getSelectedItem();

    /**
     * Set the item to be selected.
     *
     * @param selectedItem the item to be selected.
     */
    public void setSelectedItem(Object selectedItem);

    /**
     * Add a property change listener that is fired whenever a given property
     * changes.
     *
     * @param propertyName the property to listen for changes to.
     * @param al the new property change listener
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener al);

    /**
     * Add a property change listener that is fired whenever a property
     * changes.
     *
     * @param al the new property change listener
     */
    public void addPropertyChangeListener(PropertyChangeListener al);

    /**
     * Removes a property change listener that is fired whenever a given
     * property changes.
     *
     * @param propertyName the property to listen for changes to.
     * @param al the existing property change listener
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener al);

    /**
     * Removes a property change listener that is fired whenever a property
     * changes.
     *
     * @param al the existing property change listener
     */
    public void removePropertyChangeListener(PropertyChangeListener al);

    /**
     * Adds an action listener that is fired whenever the user attempts to make
     * a selection (even if the selectedItem property did not change).
     *
     * @param al the new action listener
     */
     public void addActionListener(ActionListener al);

    /**
     * Removes a registered action listener.
     *
     * @param al the existing action listener
     */
     public void removeActionListener(ActionListener al);
}
