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
import java.awt.Insets;

import javax.swing.JTextField;

import com.bbn.openmap.MouseDelegator;
import com.bbn.openmap.gui.*;

public class ScenarioOMToolSet extends OMToolSet
{

  public ScenarioOMToolSet()
  {
   super();
  }

  protected void addScaleEntry(String command, String info, String entry)
  {

    scaleField = new JTextField(entry);

	  scaleField.setPreferredSize(new Dimension(75, 25));
	  scaleField.setMinimumSize(new Dimension(75, 25));
	  scaleField.setMaximumSize(new Dimension(90, 25));

	  scaleField.setToolTipText(info);
	  scaleField.setMargin(new Insets(0,0,0,0));
        scaleField.setActionCommand(command);
	  scaleField.addActionListener(this);
	  face.add(scaleField);
  }


   public void addMouseModes(MouseDelegator md)
   {
     if (md != null)
     {
       ScenarioMouseModePanel mmp = new ScenarioMouseModePanel(md);
	     face.add(mmp);
     }
	 }

}
