/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

  /*
  protected void findAndInit(Iterator it) {
	super.findAndInit (it);

	JMenuBar menuBar = getRootPane().getJMenuBar ();
	if (menuBar == null) {
	  System.out.println ("Could not get menu bar");
	}
	else {
	  JMenu menu = menuBar.getMenu (1);
	  menu.remove (2);
	}
  }
  */
}
