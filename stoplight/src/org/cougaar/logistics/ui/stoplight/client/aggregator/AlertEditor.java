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