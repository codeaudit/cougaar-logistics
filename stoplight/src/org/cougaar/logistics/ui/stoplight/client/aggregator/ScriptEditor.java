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
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.lib.uiframework.ui.components.CRadioButtonSelectionControl;

import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.Enum.*;

/**
 * Used by QueryEditor and AlertEditor for directly editing scripts.
 */
public class ScriptEditor extends JPanel
{
  protected final static int spacing = 5;
  protected int gridyCount = 1;
  protected JPanel xmlSelectorBox;
  protected CRadioButtonSelectionControl languageSelector;
  protected JTextArea script;

  public ScriptEditor () {
    super(new BorderLayout());
    createComponents();
  }

  public ScriptEditor (String title) {
    this();
    setBorder(BorderFactory.createTitledBorder(title));
  }

  public void setLanguage(Language l)
  {
    languageSelector.setSelectedItem(l.toString());
  }

  public Language getLanguage()
  {
    return Language.fromString((String)languageSelector.getSelectedItem());
  }

  public void setScript(String scriptString)
  {
    script.setText(scriptString);
  }

  public String getScript()
  {
    return script.getText();
  }

  /**
   *  Wants for implementation
   */
  public ScriptSpec getScriptSpec () {
    return null;
  }

  /**
   *  Wants for implementation
   */
  public void setScriptSpec (ScriptSpec ss) {
  }

  public void addAuxControl(Component c)
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.gridy = gridyCount++;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.insets = new Insets(0, spacing, 0, 0);
    xmlSelectorBox.add(c, gbc);
  }

  public Dimension getControlSize()
  {
    return xmlSelectorBox.getPreferredSize();
  }

  public void setControlSize(Dimension d)
  {
    xmlSelectorBox.setPreferredSize(d);
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

    setLayout(new BorderLayout());
    add(xmlSelectorBox, BorderLayout.EAST);
    add(scrolledScript, BorderLayout.CENTER);
  }

  protected CRadioButtonSelectionControl createLanguageSelector()
  {
    String[] scriptingLanguages =
      collectionToStringArray(Language.getValidValues());
    CRadioButtonSelectionControl control =
      new CRadioButtonSelectionControl(scriptingLanguages, BoxLayout.Y_AXIS);
    control.setSelectedItem(Language.SILK.toString());
    return control;
  }

  private String[] collectionToStringArray(Collection col)
  {
    String[] sa = new String[col.size()];
    int count = 0;
    for (Iterator i = col.iterator(); i.hasNext();)
    {
      sa[count++] = i.next().toString();
    }
    return sa;
  }
}