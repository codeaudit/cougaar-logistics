/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
