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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import javax.swing.JPanel;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

public class LegendPanel extends JPanel {
  private static final Dimension myPreferredSize = new Dimension(600,25);
  public LegendPanel() {
    this.setBackground(TPFDDColor.TPFDDDarkGray);
	add(new Legend ()); // so we get a panel centered inside another panel
  }

  class Legend extends JPanel {
	public Dimension getPreferredSize() { return myPreferredSize; }

	public void paint(Graphics g) {
	  g.setColor(Color.white);
	  int height = this.getSize ().height*3/4;

	  int boxHeight = 10;
	  int boxWidth  = 60;
	  int itemWidth = 120;
	  int leftOffset = boxWidth+10;
	  int margin = 5;
	
	  g.drawString("Roll-up", leftOffset+margin, height);
	  g.drawString("Air", leftOffset+(1*itemWidth)+margin, height);
	  g.drawString("Sea", leftOffset+(2*itemWidth)+margin, height);
	  g.drawString("Ground", leftOffset+(3*itemWidth)+margin, height);
	  g.drawString("Level-2", leftOffset+(4*itemWidth)+margin, height);

	  int bottom = height-boxHeight;

	  g.setColor(TPFDDColor.TPFDDPurple);
	  g.fillRect(leftOffset-boxWidth, bottom, boxWidth, boxHeight);

	  g.setColor(TPFDDColor.TPFDDBlue);
	  g.fillRect(leftOffset+(1*itemWidth)-boxWidth, bottom, boxWidth, boxHeight);

	  g.setColor(TPFDDColor.TPFDDGreen);
	  g.fillRect(leftOffset+(2*itemWidth)-boxWidth, bottom, boxWidth, boxHeight);

	  g.setColor(TPFDDColor.TPFDDDullerYellow);
	  g.fillRect(leftOffset+(3*itemWidth)-boxWidth, bottom, boxWidth, boxHeight);

	  g.setColor(Color.black);
	  g.fillRect(leftOffset+(4*itemWidth)-boxWidth, bottom, boxWidth, boxHeight);

	}
  }
}
