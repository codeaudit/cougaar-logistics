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