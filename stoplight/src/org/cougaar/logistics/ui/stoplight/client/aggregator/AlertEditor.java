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
package org.cougaar.logistics.ui.stoplight.client.aggregator;

import java.awt.*;
import javax.swing.*;

import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.uiframework.ui.components.CFrame;

public class AlertEditor extends JPanel
{
  private JTextField alertName;
  private ScriptEditor alertScriptEditor;

  private final static int spacing = 10;

  public AlertEditor()
  {
    super(new BorderLayout(spacing, spacing));
    createComponents();
  }

  /**
   * Set all fields based on passed in alert
   */
  public void setFromAlertDescriptor(AlertDescriptor ad)
  {
    alertName.setText(ad.getName());
    alertScriptEditor.setScript(ad.getScript());
  }

  /**
   * Create new alert descriptor object based on contents of form
   */
  public AlertDescriptor createAlertDescriptor()
  {
    AlertDescriptor ad = new AlertDescriptor(alertScriptEditor.getLanguage(),
                                             alertScriptEditor.getScript());
    ad.setName(alertName.getText());

    return ad;
  }

  private void createComponents()
  {
    Box nameBox = new Box(BoxLayout.X_AXIS);
    nameBox.add(new JLabel("Alert Name: "));
    alertName = new JTextField();
    nameBox.add(alertName);
    add(nameBox, BorderLayout.NORTH);

    alertScriptEditor = new ScriptEditor("Alert Script");
    add(alertScriptEditor, BorderLayout.CENTER);
  }

  /**
   * For unit testing
   */
  public static void main(String[] args)
  {
    CFrame testFrame = new CFrame("Alert", true);
    AlertEditor ae = new AlertEditor();
    testFrame.getContentPane().add(ae, BorderLayout.CENTER);
    testFrame.show();
  }
}