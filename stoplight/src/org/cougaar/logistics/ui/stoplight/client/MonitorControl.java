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