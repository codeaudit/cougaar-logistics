/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.logistics.ui.stoplight.client;

import org.cougaar.lib.uiframework.ui.components.CFrameLauncher;
import org.cougaar.lib.uiframework.ui.inventory.InventorySelector;
import org.cougaar.lib.uiframework.ui.util.CougaarUI;
//import org.cougaar.lib.uiframework.ui.map.app.CMap;

/**
 * Blackjack Assessment UI main application class.  Includes
 * selectable stoplight and lineplot data views.<BR><BR>
 */
public class BJAssessmentLauncher extends CFrameLauncher
{
    /**
     * Default constructor.  Creates a new Blackjack Assessment launcher UI
     * with selectable stoplight and lineplot views.
     */
    public BJAssessmentLauncher()
    {
        super("BlackJack Assessment UI");

//        addTool("Map View", 'M', CMap.class,
//                new Class[]{boolean.class}, new Object[]{new Boolean(true)});
        addTool(UIConstants.STOPLIGHT_UI_NAME, 'S',
                StoplightPanel.class, null, null);
        addTool(UIConstants.LINEPLOT_UI_NAME, 'L',
                LinePlotPanel.class, null, null);

        // inventory ui
        Class[] constructorSig = {String.class, String.class, String.class,
                           String.class, String.class, Long.TYPE, Long.TYPE};
        Configurator config = new Configurator() {
                public Object[] createConstParameters()
                {
                    // pull host and port out of name server URL
                    String url = AssessmentDataSource.getNameServerUrl();
                    url = url.substring(7);
                    String host = url.substring(0, url.indexOf(":"));
                    String port = url.substring(url.indexOf(":") + 1);
                    return new Object[] {host, port, null, null, null,
                                         new Long(0), new Long(0)};
                }
                public void configure(CougaarUI ui)
                {
                    InventorySelector is = (InventorySelector)ui;

                    // configure inventory ui to use C-Time
                    is.chart.setCDate(
                      AssessmentDataSource.convertCDateToMSec(0));
                    is.chart.setUseCDate(true);
                }
            };
        addTool(UIConstants.INVENTORY_UI_NAME, 'I',
                InventorySelector.class, constructorSig, config);

        pack();
        setVisible(true);

        // load item and org trees
        Object dummy = AssessmentDataSource.orgTree;
    }

    /**
     * Main for launching application.
     *
     * @param args ignored
     */
     public static void main(String[] args)
     {
        new BJAssessmentLauncher();
    }
}