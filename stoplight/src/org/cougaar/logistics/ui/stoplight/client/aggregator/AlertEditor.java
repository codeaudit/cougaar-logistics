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