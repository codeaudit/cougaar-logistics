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
