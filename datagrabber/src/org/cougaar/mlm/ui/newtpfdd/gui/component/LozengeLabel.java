/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/LozengeLabel.java,v 1.1 2002-05-14 20:41:06 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Sundar Narasimhan, Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.FontMetrics;


public class LozengeLabel
{
  // LozengeBar expects these constants to have values 0,1,2...n.
  // Changing the order will require fixing LozengeBar.paint().
  public static final int CENTER       = 0;
  public static final int LEFT         = 1;
  public static final int LEFT_MIDDLE  = 2;
  public static final int RIGHT        = 3;
  public static final int RIGHT_MIDDLE = 4;

  private String longText = null;
  private String shortText = null;
  private int myPosition = CENTER;

  public LozengeLabel( String text ) { longText = text; }
  public LozengeLabel( String text, int position )
  {
    this(text);
    setPosition(position);
  }
  public LozengeLabel( String lText, String sText, int position )
  {
    this(lText, position);
    setShortText(sText);
  }

  public int getPosition() { return myPosition; }
  public void setPosition( int position ) { myPosition = position; }

  public String getText() { return longText; }
  public void setText( String text ) { longText = text; }

  public String getShortText() { return shortText; }
  public void setShortText( String text ) { shortText = text; }

  public String bestFit( int width, FontMetrics fm) {
    if ( longText != null && fm.stringWidth(longText) <= width )
      return longText;
    else if ( shortText != null && fm.stringWidth(shortText) <= width )
      return shortText;
    else
      return null;
  }

    public String toString()
    {
	return "long: '" + longText + "' short: '" + shortText + " position: " + myPosition;
    }
}
