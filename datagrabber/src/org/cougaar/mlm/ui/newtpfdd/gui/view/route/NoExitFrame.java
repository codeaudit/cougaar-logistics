/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.bbn.openmap.gui.OpenMapFrame;
import com.bbn.openmap.Environment;
import com.bbn.openmap.InformationDelegator;

import java.awt.event.ComponentListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import java.util.Iterator;

public class NoExitFrame extends OpenMapFrame {

  public NoExitFrame () { super(); }
  
  public NoExitFrame (String title) {
	super(title);
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  protected void processWindowEvent(WindowEvent e) 
  {
	if (e.getID() != WindowEvent.WINDOW_CLOSING) {
	  super.processWindowEvent(e);
	} else {
	  dispose();
    }
  }

  public void setInfoDeligator(InformationDelegator id){
    if(id instanceof ComponentListener){
      addComponentListener((ComponentListener)id);
    }
  }
}
