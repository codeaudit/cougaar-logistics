/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.logistics.ui.stoplight.client;

import org.cougaar.lib.uiframework.ui.components.CDesktopFrame;
import org.cougaar.lib.uiframework.ui.inventory.InventorySelector;
import org.cougaar.lib.uiframework.ui.util.CougaarUI;
//import org.cougaar.lib.uiframework.ui.map.app.CMap;

/**
 * Blackjack Assessment Desktop UI main application class.  Includes
 * selectable stoplight and lineplot data views.<BR><BR>
 */
public class BJAssessmentDesktop extends CDesktopFrame
{
    /**
     * Default constructor.  Creates a new Blackjack Assessment Desktop UI with
     * selectable stoplight and lineplot views.
     */
    public BJAssessmentDesktop()
    {
        super("BlackJack Assessment UI");

//        addTool("Map View", 'M', CMap.class,
//                new Class[]{boolean.class}, new Object[]{new Boolean(true)});
        addTool(UIConstants.STOPLIGHT_UI_NAME, 'S', StoplightPanel.class,
                null, null);
        addTool(UIConstants.LINEPLOT_UI_NAME, 'L', LinePlotPanel.class,
                null, null);

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
        new BJAssessmentDesktop();
    }
}