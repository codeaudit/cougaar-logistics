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

package org.cougaar.logistics.ui.inventory.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.Map;

/**
 * An icon used in the Color Preferences tab.
 */
public class ColoredBox implements Icon {
  /** The width of the outline, if any **/
  private static final int STROKE_WIDTH = 1;
  /** The margin around the rectangle to prevent clipping **/
  private static final int MARGIN = STROKE_WIDTH / 2;
  /** The main color of the rectangle. **/
  Color color;
  /** The size of the rectangle (width and height) **/
  int width;
  /** Should the outline be drawn. **/
  protected boolean drawOutline = true;
  /** Hints on how to render the rectangle. **/
  static final Map hints =
    Collections.singletonMap(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
  /** A wide stroke to draw the outline if any. **/
  static final Stroke wideStroke = new BasicStroke(STROKE_WIDTH);

  /**
   * Create a Colored rectangle icon.
   * @param c the color
   * @param width the width
   **/
  public ColoredBox(Color c, int width) {
    this.color = c;
    this.width = width;
  }

  /**
   * Paint the icon into the given Component at the specified
   * location.
   **/
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = (Graphics2D) g;
    Color oldColor = g2d.getColor();
    RenderingHints oldHints = g2d.getRenderingHints();
    g2d.addRenderingHints(hints);
    g2d.setColor(color);
    g2d.fillRect(x + MARGIN, y + MARGIN, width - 2 * MARGIN, width - 2 * MARGIN);
    if (drawOutline) {
      Stroke oldStroke = g2d.getStroke();
      g2d.setStroke(wideStroke);
      g2d.setColor(Color.darkGray);
      g2d.drawRect(x + MARGIN, y + MARGIN, width - 2 * MARGIN, width - 2 * MARGIN);
      g2d.setStroke(oldStroke);
    }
    g2d.setColor(oldColor);
    g2d.setRenderingHints(oldHints);
  }
  
  public int getIconWidth() { return width; }
  
  public int getIconHeight() { return width; }

}
