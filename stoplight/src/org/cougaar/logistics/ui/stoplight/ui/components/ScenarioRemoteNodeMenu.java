/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.beans.*;
import java.beans.beancontext.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import com.bbn.openmap.*;
import com.bbn.openmap.image.*;

import com.bbn.openmap.gui.*;

import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;

public class ScenarioRemoteNodeMenu extends AbstractOpenMapMenu
    implements MenuBarMenu {

    private String defaultText = "Remote Node";
    private int defaultMnemonic= 'R';

    private JDialog changeLInfoDialog = null;
    private JTextField urlField = new JTextField();

    public ScenarioRemoteNodeMenu() {
        super();
        setText(defaultText);
        setMnemonic(defaultMnemonic);
        createAndAdd();
    }
    
    /** Create and add default menu items */
    public void createAndAdd()
    {

        changeLInfoDialog = new JDialog((JFrame)null, "LocInfo Node", true);
        changeLInfoDialog.setResizable(false);
        changeLInfoDialog.getContentPane().setLayout(new BorderLayout());
        changeLInfoDialog.getContentPane().add(getChangeLINodePanel(), BorderLayout.CENTER);
        changeLInfoDialog.pack();



        JMenuItem chgli = add(new JMenuItem("Change Location Info Node"));
        chgli.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
//              changeLInfoDialog.setLocationRelativeTo(frame);

              changeLInfoDialog.show();

            }
        });
        chgli.setActionCommand("changeLINode");

    }

  private JPanel getChangeLINodePanel()
  {
    JPanel panel = new JPanel(new GridLayout(3, 4));
    JButton button = new JButton("OK");

    button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            String urlText = urlField.getText();
            // tell the psp icon keep alive about the new location
            ScenarioMap.getMapBean(getRootPane()).findPspIconLayer().myState.changeLocationInfoNode(urlText);
           }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }

          changeLInfoDialog.hide();
        }
      });

    panel.add(new JLabel(" ", SwingConstants.CENTER));
    panel.add(new JLabel("Change Location Info Node", SwingConstants.CENTER));

    panel.add(new JLabel("New URI"));
    panel.add(urlField);
    panel.add(button,BorderLayout.SOUTH);
    
    return(panel);
  }



    /**
     * This method does nothing, but is required as a part of
     * MenuInterface
     */
    public void findAndUnInit(Iterator it){}

    /**
     * This method does nothing, but is required as a part of
     * MenuInterface
     */
    public void findAndInit(Iterator it){}

    /**
     * When this method is called, it sets the given BeanContext on
     * menu items that need to find objects to get their work done.
     * Note: Menuitems are not added to beancontext
     */
    public void setBeanContext(BeanContext in_bc) throws PropertyVetoException {
        super.setBeanContext(in_bc);
        if(!Environment.isApplication()) { //running as an Applet
            return;
        }
//        if (spMenu != null) spMenu.setMapHandler(getMapHandler());
//        if (jpegMenuItem != null) jpegMenuItem.setMapHandler(getMapHandler());
//        if (gifMenuItem != null) gifMenuItem.setMapHandler(getMapHandler());
    }
}
