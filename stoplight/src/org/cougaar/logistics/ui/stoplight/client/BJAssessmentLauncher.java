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

import org.cougaar.logistics.ui.stoplight.ui.components.CFrameLauncher;
import org.cougaar.logistics.ui.stoplight.ui.inventory.InventorySelector;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;
//import org.cougaar.logistics.ui.stoplight.ui.map.app.CMap;

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