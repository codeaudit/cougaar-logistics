
package org.cougaar.logistics.ui.stoplight.client.aggregator;

import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import javax.swing.*;

import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.Enum.*;

public class AggScriptEditor extends ScriptEditor {
  protected JTextField collateBy;
  protected JLabel collateLabel;
  protected JCheckBox aggFullFormat;

  public AggScriptEditor () {
    super();
  }

  public AggScriptEditor (String title) {
    super(title);
  }

  protected void createComponents () {
    xmlSelectorBox = new JPanel();
    xmlSelectorBox.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 1;
    gbc.weighty = 1;
    languageSelector = createLanguageSelector();
    xmlSelectorBox.add(languageSelector, gbc);

    script = new JTextArea();
    JScrollPane scrolledScript = new JScrollPane(script);

    // don't let preferred size of scrolled pane increase when text is added
    // to text area
    scrolledScript.setPreferredSize(scrolledScript.getPreferredSize());

    collateBy = new JTextField();
    aggFullFormat = new JCheckBox("Full Format");
    addAuxControl(aggFullFormat);
    aggFullFormat.addActionListener(new FullFormatEar());

    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0));
    p.add(collateLabel = new JLabel("Collate by:  "), BorderLayout.WEST);
    p.add(collateBy, BorderLayout.CENTER);

    JPanel q = new JPanel(new BorderLayout());
    q.add(p, BorderLayout.SOUTH);
    q.add(scrolledScript, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(xmlSelectorBox, BorderLayout.EAST);
    add(q, BorderLayout.CENTER);
  }

  public String getCollateKeys () {
    if (!aggFullFormat.isSelected())
      return collateBy.getText();
    return null;
  }

  public void setCollateKeys (String keys) {
    if (keys != null)
      collateBy.setText(keys);
    else
      collateBy.setText("");
  }

  public AggType getAggType () {
    return aggFullFormat.isSelected() ? AggType.AGGREGATOR : AggType.MELDER;
  }

  public void setFullFormat (boolean f) {
    aggFullFormat.setSelected(f);
  }

  public ScriptSpec getScriptSpec () {
    String text = getScript();
    if (containsScript(text))
      return new ScriptSpec(
        getLanguage(), getAggType(), text, getCollateKeys());
    else
      return null;
  }

  private static boolean containsScript (String s) {
    if (s == null || s.length() == 0)
      return false;
    return new StringTokenizer(s, " \t\r\n").hasMoreTokens();
  }

  public void setScriptSpec (ScriptSpec ss) {
    // if this is not an aggregation script, use blanks
    if (ss == null || ss.getType() != ScriptType.AGGREGATOR) {
      setScript("");
      setCollateKeys("");
    }
    else {
      setScript(ss.getText());
      setLanguage(ss.getLanguage());
      setFullFormat(ss.getAggType() == AggType.AGGREGATOR);
      setCollateKeys(ss.getAggIdString());
    }
  }

  private class FullFormatEar implements ActionListener {
    public void actionPerformed (ActionEvent ae) {
      boolean state = !aggFullFormat.isSelected();
      collateBy.setEnabled(state);
      collateLabel.setEnabled(state);
    }
  }
}