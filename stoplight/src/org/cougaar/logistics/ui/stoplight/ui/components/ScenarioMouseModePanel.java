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
package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.Dimension;

import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JPopupMenu;

import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

import com.bbn.openmap.*;
import com.bbn.openmap.gui.*;
import com.bbn.openmap.event.*;

public class ScenarioMouseModePanel extends MouseModePanel
{

  public ScenarioMouseModePanel ()
  {
    super ();
  }
  
  public ScenarioMouseModePanel (MouseDelegator md)
  {
    super (md);
  }

    protected void setPanel(MouseDelegator md){
        if (titleButton != null){
            remove(titleButton);
        }
        titleButton = new JButton();
        titleButton.addActionListener(this);

        String activeMode = md.getActiveMouseModeID();
        MapMouseMode[] modes = md.getMouseModes();

        rbs = new JPopupMenu("Mouse Modes");
        dim = titleButton.getMinimumSize();
        for (int i=0; i<modes.length; i++) {
            JMenuItem rb = new JMenuItem(modes[i].getID());
            rb.setActionCommand(newMouseModeCmd);
            rb.setName(modes[i].getID());
            rb.setBorderPainted(false);
/*
            if (Debug.debugging("mousemode")){
                Debug.output("MouseModePanel.setPanel(): Adding " +
                             rb.getName() + " button to menu.");
            }
*/
            rb.addActionListener(this);
            if (activeMode.equals(modes[i].getID())){
/*
                if (Debug.debugging("mousemode")){
                    Debug.output("MouseModePanel.setPanel: Setting " +
                                 activeMode + " to active");
                }
*/
                rb.setSelected(true);
                titleButton.setText(activeMode);
            }
            rbs.add(rb);
        }
        this.setMinimumSize(dim);

        if (modes.length > 0) {
            border = new TitledBorder(new EtchedBorder(), "Mouse Mode");
            setBorder(border);
        }

        titleButton.setActionCommand(mouseModeCmd);
        titleButton.setBorderPainted(false);
        add(titleButton);
        // HACK - the button keeps changing size depending on which
        // choice is made.  I'd like to set the size based on the
        // size of the largest mouse mode name, but I can't figure out
        // when that's available, before we actually have to present
        // the button.
//        this.setPreferredSize(new Dimension(140, 45));
        this.setPreferredSize(new Dimension(105, 45));
        this.revalidate();
    }
}
