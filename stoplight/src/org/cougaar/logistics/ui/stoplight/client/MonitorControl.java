/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MonitorControl extends JPanel
{
  private boolean monitor = false;
  private JButton monitorButton = null;

  public MonitorControl()
  {
    super();

    monitorButton = new JButton("Monitor");

    monitorButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          monitor = !monitor;
          updateColors();
          firePropertyChange("monitoring", !monitor, monitor);
        }
      });

    add(monitorButton);
  }

  private void updateColors()
  {
    monitorButton.setBackground(monitor ? Color.red : null);
    monitorButton.setForeground(monitor ? Color.white : null);
  }

  public void setMonitoring(boolean monitor)
  {
    this.monitor = monitor;
    updateColors();
    firePropertyChange("monitoring", !monitor, monitor);
  }

  public boolean isMonitoring()
  {
    return monitor;
  }

  /** For unit testing */
  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    MonitorControl mc = new MonitorControl();
    frame.getContentPane().add(mc);
    frame.pack();
    frame.show();
  }
}