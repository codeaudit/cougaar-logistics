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

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.cougaar.logistics.ui.stoplight.ui.util.Selector;

/**
 * A UI control bean that is a button that is labeled with the control's
 * current value.  Upon pressing the button the user is presented with a
 * dialog that contains a JTree from which the user can select a node.
 *
 * This bean has bounded property:  "selectedItem"
 */
public class CTreeButton extends CPullrightButton implements Selector
{
    private CNodeSelector ns = null;

    /**
     * Default constructor.  Create a new tree button with a simple default
     * tree.
     */
    public CTreeButton()
    {
        super();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("parent");
        root.add(new DefaultMutableTreeNode("child1"));
        root.add(new DefaultMutableTreeNode("child2"));

        init(root, root);
    }

    /**
     * Create a new tree button based on the given parameters
     *
     * @param root         the root of the tree to select from
     * @param selectedNode the node to have selected
     */
    public CTreeButton(DefaultMutableTreeNode root,
                       DefaultMutableTreeNode selectedNode)
    {
        super();

        init(root, selectedNode);
    }

    /**
     * Initialize tree button
     */
    private void init(DefaultMutableTreeNode root,
                      DefaultMutableTreeNode selectedNode)
    {
        ns = new CNodeSelector(root);
        ns.setSelectedItem(selectedNode);
        setSelectorControl(ns);
    }

    /**
     * Add a control to be contained in (but not managed by) this control
     *
     * @param control externally managed control
     */
    public void addIncludedControl(Component control)
    {
        ns.addIncludedControl(control);
    }

    /**
     * Set a new root for this tree button.  Sets equivilant selection
     * in new tree if possible.
     *
     * @param root new root for this tree button
     */
    public void setRoot(DefaultMutableTreeNode root)
    {
        ns.setRoot(root);
    }

    /**
     * Set whether the root node of JTree should be shown.
     *
     * @param visible true if root node should be shown.
     */
    public void setRootVisible(boolean visible)
    {
        ns.setRootVisible(visible);
    }

    /**
     * Expand the first level of the JTree
     */
    public void expandFirstLevel()
    {
        ns.expandFirstLevel();
    }

}