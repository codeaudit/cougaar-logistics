/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/VirtualX.java,v 1.2 2003-02-03 22:27:59 mthome Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

/**
  * The VirtualX interface is meant to be implemented by
  * Components (or other graphical elements.)  VirtualX
  * adds an extra dimension, an X-coordinate of type long.
  * <p>
  * <b>Suggested Usage:</b>
  * Graphical elements using VirtualX exist on a long,
  * virtual X-axis.  The mapping between virtual coordinates
  * and actual screen coordinates is determined by the
  * relation between VirtualX Components and their Containers.
  * <p>
  * For example, suppose we have a VirtualX Container with
  * VirtualXLocation=0, VirtualXSize=10000 which contains a
  * VirtualX Component with VirtualXLocation=5000, VirtualXSize=5000.
  * Then the Component will occupy the entire right half of the
  * Container, however big it may be on-screen.
  * <p>
  * <b>Note:</b> VirtualX doesn't specify behavior in the Y direction.
  * A VirtualX Container is free to do whatever it wishes.  For example,
  * the alpgui Gantt chart uses one VirtualX Container for each row of
  * lozenges.  Each lozenge follows the VirtualX convention for horizontal
  * placement but fills the vertical space completely.
  */

public interface VirtualX
{

  /**
   * Returns the location of the left edge of this element
   * in VirtualX space.
   *
   * @return A <code>long</code> value for the left edge of this element
   * @see org.cougaar.mlm.ui.newtpfdd.gui.component.GanttChart#setVirtualXLocation
   * @see #setVirtualXLocation
   */
  public abstract long getVirtualXLocation();
  public abstract long getVirtualXSize();
  public abstract void setVirtualXLocation( long newLocation );
  public abstract void setVirtualXSize( long newSize );
}
