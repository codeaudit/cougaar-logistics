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
package org.cougaar.mlm.ui.newtpfdd.gui.view.node;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;

public class CargoInstance extends DBUIDNode implements DimensionNode {
  long quantity = 1;
  double weight;  
  double width;  
  double height;  
  double depth;  
  String alpType;
  String container;

  public CargoInstance (UIDGenerator generator, String dbuid) {
    super (generator, dbuid);
  }

  public int getType () { return UIDGenerator.ASSET_INSTANCE; }
  public long getQuantity () { return quantity; }
  public void setQuantity (long q) { quantity = q; }

  public double getVolume () { return width*depth*height;  }
  public double getArea   () { return width*depth;  }
  public double getHeight () { return height;  }
  public void setHeight (double h) { height = h; }
  public double getWidth  () { return width;  }
  public void setWidth (double w) { width = w; }
  public double getDepth  () { return depth;  }
  public void setDepth (double d) { depth = d; }
  public double getWeight () { return weight;  }
  public void setWeight (double w) { weight = w; }
  public String getALPType () { return alpType; }
  public void setALPType (String type) { alpType = type; }
  public String getContainer () { return container; }
  public void setContainer (String container) { this.container=container; }

  public boolean isTransport() { return true; }
}

