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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 * Someday this will be a useful wrapper for a JTree.  Currently, it doesn't
 * really add much.
 */
public class CNodeSelectionControl extends JTree
{
    /**
     * Default constructor.  Creates a new CNodeSelectionControl with default
     * contents.
     */
    public CNodeSelectionControl()
    {
        super();

        getSelectionModel().
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * Create a new JTree using the given tree model for data.
     *
     * @param top root node of tree model
     */
    public CNodeSelectionControl(DefaultMutableTreeNode top)
    {
        super(top);

        getSelectionModel().
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * In jdk1.3 this will override a method in JTree and make it so the
     * selection will not change when the selected node's branch is collapsed.
     * (fix for bug #380) This will have no effect under jdk1.2.
     */
    protected boolean removeDescendantSelectedPaths(TreePath path,
                                                    boolean includePath)
    {
        return false;
    }

    /**
     * Someday this will convert from one tree structure to another
     *
     * @param t root node of tree model to be converted.
     * @return root node of converted tree model
     */
    private static DefaultMutableTreeNode convertTree(DefaultMutableTreeNode t)
    {
        return t;
    }
}