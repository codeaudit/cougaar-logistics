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

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

import org.cougaar.lib.uiframework.ui.components.CRadioButtonSelectionControl;

public class UpdateEditor extends JDialog
{
    private static final boolean defaultUsePushMethod = true;
    private static final int defaultPullRate = 10;

    public static boolean usePushMethod = defaultUsePushMethod;
    public static int pullRate = defaultPullRate;

    private static final String
      PUSH_METHOD = "Push (Updates sent as soon as change is detected)";
    private static final String
      PULL_METHOD = "Pull (Updates gathered periodically at a set rate)";
    private static final int spacing = 10;
    private int closeAction = JOptionPane.CANCEL_OPTION;

    private CRadioButtonSelectionControl methodSelector = null;
    private JPanel continuousMethodPanel = null;
    private JPanel pullRateBox = null;
    private JTextField pullRateTextField = null;

    private UpdateEditor(Frame owner)
    {
        super(owner, true);
        setTitle("Monitor Methodology Editor");
        createComponents();
        pack();
        setLocationRelativeTo(owner);
        show();
    }

    public static int showDialog(Frame owner)
    {
        UpdateEditor ae = new UpdateEditor(owner);
        return ae.getCloseAction();
    }

    public int getCloseAction()
    {
        return closeAction;
    }

    private void createComponents()
    {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // Settings Panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        continuousMethodPanel = new JPanel(new GridBagLayout());
        continuousMethodPanel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createTitledBorder("Monitor Configuration"),
          BorderFactory.createEmptyBorder(0, spacing, spacing, spacing)));
        String[] continuousMethods = {PUSH_METHOD, PULL_METHOD};
        methodSelector =
          new CRadioButtonSelectionControl(continuousMethods,BoxLayout.Y_AXIS);
        continuousMethodPanel.add(methodSelector, gbc);
        pullRateBox = new JPanel();
        pullRateBox.setLayout(new BoxLayout(pullRateBox, BoxLayout.X_AXIS));
        pullRateBox.add(Box.createHorizontalStrut(spacing * 2));
        pullRateBox.add(new JLabel("Pull Rate (wait period in seconds): "));
        pullRateBox.add(pullRateTextField = new JTextField(5));
        continuousMethodPanel.add(pullRateBox, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(
          BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing));
        JButton resetButton = new JButton("Reset to defaults");
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createGlue());
        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(spacing));
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);

        // High Level Layout
        c.add(continuousMethodPanel, BorderLayout.CENTER);
        c.add(buttonPanel, BorderLayout.SOUTH);

        // Event Handling
        methodSelector.addPropertyChangeListener("selectedItem",
          new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent e)
              {
                updateEnabledState();
              }
            });

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    updateConfigurationFromUI();
                    closeAction = JOptionPane.OK_OPTION;
                    dispose();
                }
            });

        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    closeAction = JOptionPane.CANCEL_OPTION;
                    dispose();
                }
            });

        resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    resetToDefaults();
                }
            });

        updateUIFromConfiguration();
    }

    private void updateEnabledState()
    {
        boolean pullMethodSelected =
          ((methodSelector.getSelectedItem() != null) &&
            methodSelector.getSelectedItem().equals(PULL_METHOD));

        setEnabled(pullRateBox, pullMethodSelected);
    }

    private void updateUIFromConfiguration()
    {
        methodSelector.setSelectedItem(usePushMethod ? PUSH_METHOD :
                                       PULL_METHOD);
        pullRateTextField.setText(String.valueOf(pullRate));
    }

    private void updateConfigurationFromUI()
    {
        usePushMethod = methodSelector.getSelectedItem().equals(PUSH_METHOD);
        pullRate = Integer.parseInt(pullRateTextField.getText());
    }

    private void resetToDefaults()
    {
        methodSelector.setSelectedItem(defaultUsePushMethod ? PUSH_METHOD :
                                       PULL_METHOD);
        pullRateTextField.setText(String.valueOf(defaultPullRate));
    }

    private void setEnabled(Component c, boolean enabled)
    {
        c.setEnabled(enabled);

        if (c instanceof Container)
        {
            Container con = (Container)c;
            for (int i = 0; i < con.getComponentCount(); i++)
            {
                setEnabled(con.getComponent(i), enabled);
            }
        }
    }

    /**
     * main for unit testing
     */
    public static void main(String args[])
    {
        JFrame frame = new JFrame();
        if (UpdateEditor.showDialog(frame) ==
            JOptionPane.OK_OPTION)
        {
            System.out.println("OK Selected");
        }
    }
}