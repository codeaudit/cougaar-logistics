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
package org.cougaar.logistics.ui.stoplight.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.lib.uiframework.ui.components.CDesktopFrame;
import org.cougaar.lib.uiframework.ui.components.CFrame;
import org.cougaar.lib.uiframework.ui.inventory.InventorySelector;
import org.cougaar.lib.uiframework.ui.models.RangeModel;
import org.cougaar.lib.uiframework.ui.util.CougaarUI;
import org.cougaar.lib.uiframework.ui.util.VariableInterfaceManager;

/**
 * This popup is used to launch new cougaar UIs configured based on the
 * location in which the user invoked the popup menu.
 */
public class UILaunchPopup extends JPopupMenu
{
    private Hashtable configuration = new Hashtable();

    /**
     * Default constructor.  Create a new popup filled with selections for
     * launching other UIs.
     */
    public UILaunchPopup()
    {
        ActionListener selectionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    launchUI(((JMenuItem)e.getSource()).getText());
                }
            };
        JMenuItem stoplight = new JMenuItem(UIConstants.STOPLIGHT_UI_NAME);
        stoplight.addActionListener(selectionListener);
        add(stoplight);
        JMenuItem lineplot = new JMenuItem(UIConstants.LINEPLOT_UI_NAME);
        lineplot.addActionListener(selectionListener);
        add(lineplot);
        JMenuItem inventory = new JMenuItem(UIConstants.INVENTORY_UI_NAME);
        inventory.addActionListener(selectionListener);
        add(inventory);
    }

    /**
     * Set a new config property that the new UI should be configured to show.
     *
     * @param configKey the key of the new config property that the new UI
     *                  should be configured to show.
     * @param configValue the value of the new config property that the new UI
     *                    should be configured to show.
     */
    public void setConfigProperty(Object configKey, Object configValue)
    {
        configuration.put(configKey, configValue);
    }

    /**
     * Launch a new UI in the correct type of frame and configured based
     * on the configuration variables.
     *
     * @param uiName name of the UI to launch (use UIConstants)
     */
    public void launchUI(final String uiName)
    {
        final JComponent swingInvoker =
            (getInvoker() instanceof JComponent)?(JComponent)getInvoker():null;
        final Component root =
            (swingInvoker != null) ? swingInvoker.getTopLevelAncestor() : null;
        if (swingInvoker != null)
        {
            Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
            swingInvoker.setCursor(wait);
            root.setCursor(wait);
            if (swingInvoker.getParent() != null)
                swingInvoker.getParent().setCursor(wait);
        }
        (new Thread() {
            public void run()
            {
                // Bring up new UI in the proper type of frame
                JFrame myFrame = findJFrame();
                JFrame newUIsFrame;
                if (myFrame instanceof CDesktopFrame)
                {
                    newUIsFrame = myFrame;
                }
                else
                {
                    newUIsFrame =new CFrame(uiName, false);
                }

                CougaarUI newUI = null;
                if (uiName.equals(UIConstants.INVENTORY_UI_NAME))
                {
                    newUI = createInventoryUI();
                }
                else
                {
                    VariableInterfaceManager vim = null;
                    if (uiName.equals(UIConstants.STOPLIGHT_UI_NAME))
                    {
                        StoplightPanel slp = new StoplightPanel();
                        newUI = slp;
                        vim = slp.getVariableInterfaceManager();
                    }
                    else
                    {
                        LinePlotPanel lpp = new LinePlotPanel(true, true);
                        newUI = lpp;
                        vim = lpp.getVariableInterfaceManager();
                        lpp.adjustXScale(
                            (RangeModel)configuration.get("Time"));
                    }

                    AssessmentUI assessui = (AssessmentUI)newUI;
                    assessui.setIgnoreQueries(true);
                    Enumeration keys = configuration.keys();
                    while (keys.hasMoreElements())
                    {
                        Object key = keys.nextElement();
                        if (!uiName.equals(UIConstants.LINEPLOT_UI_NAME) ||
                            !key.equals("Time"))
                        {
                            vim.getDescriptor(key.toString()).
                                setValue(configuration.get(key));
                        }
                    }
                    assessui.setIgnoreQueries(false);
                    assessui.updateView();
                }

                if (newUIsFrame instanceof CDesktopFrame)
                {
                    CDesktopFrame cfc = (CDesktopFrame)newUIsFrame;
                    cfc.createInnerFrame(uiName, newUI);
                }
                else
                {
                    newUI.install(newUIsFrame);
                    newUIsFrame.setVisible(true);
                }

                // post configure inventory ui
                if (uiName.equals(UIConstants.INVENTORY_UI_NAME))
                {
                    configureInventoryUI(newUI);
                }

                if (getInvoker() != null)
                {
                    Cursor defaultc = Cursor.getDefaultCursor();
                    root.setCursor(defaultc);
                    swingInvoker.setCursor(defaultc);
                    if (swingInvoker.getParent() != null)
                        swingInvoker.getParent().setCursor(defaultc);
                }
            }
        }).start();
    }

    private CougaarUI createInventoryUI()
    {
        // adjust url the way the inventory ui likes it.
        // (i.e. lose the http://)
        String url = AssessmentDataSource.getNameServerUrl();
        url = url.substring(7);
        String host = url.substring(0, url.indexOf(":"));
        String port = url.substring(url.indexOf(":") + 1);

        // create asset name the way the inventory ui likes it.
        // (i.e. Description:UID)
        DefaultMutableTreeNode itemTN =
            (DefaultMutableTreeNode)configuration.get("Item");
        Hashtable itemHT = (Hashtable)itemTN.getUserObject();
        String description = itemHT.get("ITEM_ID").toString();
        String uid = itemHT.get("UID").toString();
        if (uid.startsWith("NSN/89") && !uid.startsWith("NSN/8970014"))
        {
            description += "  ";
        } else if (uid.startsWith("NSN/91500"))
        {
            description = description.substring(0, 19);
        }
        String asset = description + ":" + uid;

        // cluster
        DefaultMutableTreeNode orgTN =
            (DefaultMutableTreeNode)configuration.get("Org");

        // time range
        RangeModel timeRange = (RangeModel)configuration.get("Time");
        long startTime =
            AssessmentDataSource.convertCDateToMSec(timeRange.getMin())/1000;
        long endTime =
            AssessmentDataSource.convertCDateToMSec(timeRange.getMax())/1000;

        // provide warning if needed.
        if (!orgTN.isLeaf() || !itemTN.isLeaf())
        {
            StringBuffer warningMessage =
                new StringBuffer("Warning: Inventory UI does not ");
            if (!orgTN.isLeaf())
            {
                warningMessage.append("include organization subordinates");

                if (!itemTN.isLeaf())
                {
                    warningMessage.append(" or ");
                }
            }
            if (!itemTN.isLeaf())
            {
                warningMessage.append(
                    "provide aggregated views of item groupings");
            }
            warningMessage.append(".");

            JOptionPane.showMessageDialog(findJFrame(),
                                          warningMessage.toString(),
                                          "Inventory UI Warning",
                                          JOptionPane.WARNING_MESSAGE);
        }

        InventorySelector is =
            new InventorySelector(host, port, null, orgTN.toString(), asset,
                                  startTime, endTime);

        return is;
    }

    private void configureInventoryUI(CougaarUI cui)
    {
        InventorySelector is = (InventorySelector)cui;

        // configure inventory ui to use C-Time
        is.chart.setCDate(AssessmentDataSource.convertCDateToMSec(0));
        is.chart.setUseCDate(true);
    }

    private JFrame findJFrame()
    {
        if (getInvoker() != null)
        {
            Container parent = getInvoker().getParent();
            while (!(parent instanceof JFrame))
            {
                parent = parent.getParent();
            }
            return (JFrame)parent;
        }
        return null;
    }
}