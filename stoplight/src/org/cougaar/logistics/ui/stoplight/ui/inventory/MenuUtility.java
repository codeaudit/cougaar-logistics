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

package org.cougaar.logistics.ui.stoplight.ui.inventory;

import javax.swing.*;

public class MenuUtility
{
  public static JCheckBoxMenuItem addCheckMenuItem(JMenu menu, String label, char mnemonic, Action action, boolean checked)
  {
    JCheckBoxMenuItem mi = addCheckMenuItem(menu, label, action, checked);
    mi.setMnemonic(mnemonic);

    return(mi);
  }

  public static JCheckBoxMenuItem addCheckMenuItem(JMenu menu, String label, Action action, boolean checked)
  {
    JCheckBoxMenuItem mi = (JCheckBoxMenuItem)menu.add(new JCheckBoxMenuItem(label, checked));
    mi.addActionListener(action);

    if(action == null)
    {
      mi.setEnabled(false);
    }

    return(mi);
  }

  public static JMenuItem addMenuItem(JMenu menu, String label, char mnemonic, Action action)
  {
    JMenuItem mi = addMenuItem(menu, label, action);
    mi.setMnemonic(mnemonic);

    return(mi);
  }

  public static JMenuItem addMenuItem(JMenu menu, String label, Action action)
  {
    JMenuItem mi = (JMenuItem)menu.add(new JMenuItem(label));
    mi.addActionListener(action);

    if(action == null)
    {
      mi.setEnabled(false);
    }

    return(mi);
  }

  public static JCheckBoxMenuItem addCheckMenuItem(JMenu menu, String label, String actionCommand, Action action, boolean checked)
  {
    JCheckBoxMenuItem mi = (JCheckBoxMenuItem)menu.add(new JCheckBoxMenuItem(label, checked));
    mi.addActionListener(action);
    mi.setActionCommand(actionCommand);

    if(action == null)
    {
      mi.setEnabled(false);
    }

    return(mi);
  }

  public static JRadioButtonMenuItem addRadioButtonMenuItem(JMenu menu, String label, Action action, String actionCommand, boolean checked)
  {
    JRadioButtonMenuItem mi = (JRadioButtonMenuItem)menu.add(new JRadioButtonMenuItem(label, checked));
    mi.addActionListener(action);
    mi.setActionCommand(actionCommand);

    if(action == null)
    {
      mi.setEnabled(false);
    }

    return(mi);
  }
}
