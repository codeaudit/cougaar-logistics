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

import java.util.Hashtable;

/**
 * A hashtable whose toString method returns the value of a specified property.
 * Used in JTreeInterpreter for user data object in generated
 * DefaultMutableTreeNodes.
 */
public class SelectableHashtable extends Hashtable
{
    private String selectedProperty = null;

    /**
     * Create a new selectable hashtable whose toString method will return
     * the value of a specified property.
     *
     * @param selectedProperty property to use for toString
     */
    public SelectableHashtable(String selectedProperty)
    {
        this.selectedProperty = selectedProperty;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toString()
    {
        return get(selectedProperty).toString();
    }

    /**
     * Set the name of the property to use for toString.
     *
     * @param selectedProperty new name of the property to use for toString.
     */
    public void setSelectedProperty(String selectedProperty)
    {
        this.selectedProperty = selectedProperty;
    }

    /**
     * Get the current name of the property being used for toString.
     *
     * @return the current name of the property being used for toString.
     */
    public String getSelectedProperty()
    {
        return selectedProperty;
    }
}