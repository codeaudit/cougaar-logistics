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
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.cougaar.logistics.ui.stoplight.ui.util.Selector;

/**
 * A UI control bean that is a single select JTree in a JPanel.  Control
 * implements Selector interface
 *
 * This bean has bounded property:  "selectedItem"
 */
public class CNodeSelector extends JPanel implements Selector
{
    private DefaultMutableTreeNode selectedNode;
    private CNodeSelectionControl nsc;
    private JPanel includedControlPanel;
    private JPanel northPanel;

    /**
     * Default constructor.  Create a new tree with a simple default
     * tree.
     */
    public CNodeSelector()
    {
        super(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("parent");
        root.add(new DefaultMutableTreeNode("child1"));
        root.add(new DefaultMutableTreeNode("child2"));
        selectedNode = root;

        init(root);
    }

    /**
     * Create a new node selector.
     *
     * @param root root node of tree that models tree from which the user will
     *             select nodes
     */
    public CNodeSelector(DefaultMutableTreeNode root)
    {
        super(new BorderLayout());
        this.selectedNode = root;

        init(root);
    }

    public void updateUI()
    {
        super.updateUI();

        if (northPanel != null)
        {
            Color back = nsc.getBackground();
            setBackground(northPanel, new Color(back.getRGB()));
        }
    }

    private void setBackground(Component c, Color newColor)
    {
        c.setBackground(newColor);

        if (c instanceof Container)
        {
            Container con = (Container)c;
            for (int i = 0; i < con.getComponentCount(); i++)
            {
                setBackground(con.getComponent(i), newColor);
            }
        }
    }

    /**
     * Initialize control
     *
     * @param root root node of tree that models tree from which the user will
     *             select nodes
     */
    private void init(DefaultMutableTreeNode root)
    {
        nsc = new CNodeSelectionControl(root);
        JScrollPane scrolledNSC = new JScrollPane(nsc);
        add(scrolledNSC, BorderLayout.CENTER);
        northPanel = new JPanel(new BorderLayout());
        northPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        includedControlPanel = new JPanel();
        includedControlPanel.setLayout(
          new BoxLayout(includedControlPanel, BoxLayout.Y_AXIS));
        northPanel.add(includedControlPanel, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);
        setPreferredSize(new Dimension(400, 200));

        nsc.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e)
                {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        nsc.getLastSelectedPathComponent();
                    if (node == null) return;

                    DefaultMutableTreeNode oldSelectedNode = selectedNode;
                    selectedNode = node;
                    firePropertyChange("selectedItem", oldSelectedNode,
                                       selectedNode);
                }
            });

        nsc.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                    int selRow = nsc.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath =
                        nsc.getPathForLocation(e.getX(), e.getY());
                    if(selRow != -1)
                    {
                        fireActionPerformed();
                    }
                }
            });
    }

    /**
     * Set a new root for this tree.  Sets equivilant selection
     * in new tree if possible.
     *
     * @param root new root for this tree
     */
    public void setRoot(DefaultMutableTreeNode root)
    {
        String oldSelectedItem = getSelectedItem().toString();
        DefaultTreeModel dtm = (DefaultTreeModel)nsc.getModel();
        dtm.setRoot(root);
        setSelectedItem(oldSelectedItem);
    }

    /**
     * Not needed when compiling/running under jdk1.3
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Add a control to be contained in (but not managed by) this control
     *
     * @param control externally managed control
     */
    public void addIncludedControl(Component control)
    {
        includedControlPanel.add(control);
        updateUI();

        control.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                fireActionPerformed();
            }
          });
    }

    /**
     * Sets if root node should be visible and selectable in tree selection
     * control.
     *
     * @param visible true if root node should be both visible and selectable
     */
    public void setRootVisible(boolean visible)
    {
        nsc.setRootVisible(visible);
    }

    /**
     * Expand the first level of the tree.
     */
    public void expandFirstLevel()
    {
        TreeNode root = (TreeNode)nsc.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++)
        {
            Object[] tp = {root, root.getChildAt(i)};
            nsc.expandPath(new TreePath(tp));
        }
    }

    /**
     * Get the currently selected node.
     *
     * @return the currently selected node
     *         (can cast to type DefaultMutableTreeNode)
     */
    public Object getSelectedItem()
    {
        return selectedNode;
    }

    /**
     * Set the selected node.
     *
     * @param selectedItem the new node
     *                     (can be of type String or DefaultMutableTreeNode)
     */
    public void setSelectedItem(Object selectedItem)
    {
        if (selectedItem instanceof DefaultMutableTreeNode)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedItem;
            nsc.setSelectionPath(new TreePath(node.getPath()));
        }
        else if (selectedItem instanceof String)
        {
            DefaultMutableTreeNode node =
                findNode((DefaultMutableTreeNode)nsc.getModel().getRoot(),
                         selectedItem.toString());
            if (node != null)
            {
                nsc.setSelectionPath(new TreePath(node.getPath()));
            }
        }
    }

    private Vector actionListeners = new Vector();

    /**
     * Adds an action listener that is fired whenever the user attempts to make
     * a selection (even if the selectedItem property did not change).
     *
     * @param al the new action listener
     */
     public void addActionListener(ActionListener al)
     {
        actionListeners.add(al);
     }

    /**
     * Removes a registered action listener.
     *
     * @param al the existing action listener
     */
    public void removeActionListener(ActionListener al)
    {
        actionListeners.remove(al);
    }

    private void fireActionPerformed()
    {
        for (int i = 0; i < actionListeners.size(); i++)
        {
            ActionListener al = (ActionListener)actionListeners.elementAt(i);
            al.actionPerformed(new ActionEvent(this, 0, "selectedItemAction"));
        }
    }

    private DefaultMutableTreeNode
        findNode(DefaultMutableTreeNode currentNode, String nodeString)
    {
        if (currentNode.getUserObject().toString().equals(nodeString))
        {
            return currentNode;
        }
        if (!currentNode.isLeaf())
        {
            for (int i = 0; i < currentNode.getChildCount(); i++)
            {
                DefaultMutableTreeNode foundNode =
                    findNode((DefaultMutableTreeNode)
                             currentNode.getChildAt(i), nodeString);
                if (foundNode != null) return foundNode;
            }
        }

        return null;
    }
}