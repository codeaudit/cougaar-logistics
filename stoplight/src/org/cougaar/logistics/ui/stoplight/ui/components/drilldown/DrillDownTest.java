/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.components.drilldown;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DrillDownTest
{
  public static void main(String[] args)
  {
    JFrame frame = new JFrame("DrillDown Test");
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    DrillableLabel label = new DrillableLabel("Stack");
    DrillDownStack stack = new DrillDownStack(label);
    stack.addDrillDown(label, label, null);
    
    panel.add(stack, BorderLayout.CENTER);
    
    frame.getContentPane().add(panel);
    
    frame.setSize(300, 300);

    frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        System.exit(0);
      }
    });

    frame.show();
  }
}

class DrillableLabel extends JLabel implements DrillDown
{
  private static long count = 0;

  private String displayString = null;

  public DrillableLabel()
  {
  }

  public DrillableLabel(String display)
  {
    displayString = display + ": " + count++;
    setText(displayString);
  }

  public DrillDown getNextDrillDown(MouseEvent e)
  {
    DrillableLabel label = new DrillableLabel();
    label.setData(displayString);
    
    return(label);
  }

  public void setData(Object data)
  {
    displayString = data + ": " + count++;
    setText(displayString);
  }

  public Component activate(DrillDownStack drillDownStack)
  {
    drillDownStack.addDrillDown(this, this, null);

    return(this);
  }

  public String toString()
  {
    return(getClass().getName() + "@" + hashCode() + " " + displayString);
  }
}
