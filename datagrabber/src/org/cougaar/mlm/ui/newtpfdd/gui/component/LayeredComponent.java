/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;


import java.awt.Component;


public class LayeredComponent extends Component
{
    private int layer = 0;

    public LayeredComponent(int layer)
    {
	this.layer = layer;
    }

    public void setLayer(int layer)
    {
	this.layer = layer;
    }

    public int getLayer()
    {
	return layer;
    }
}
