/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

public class AggregateCargoType extends CargoType implements DimensionNode {
  private String name;
  private int aggNumber;
  private int totalAggNumber;
  double weight;  
  double width;  
  double height;  
  double depth;  
  double area;  
  double volume;  
  
    public AggregateCargoType(UIDGenerator generator, String dbuid) {
	  super (generator, dbuid);
    }

    public String getName() { return name; }
    public void setDisplayName(String n) { name = n; }

    public String getDisplayName() { return name + "(" + aggNumber + "/" + totalAggNumber + ")"; }

  public int getAggNumber() { return aggNumber;}
  public int getTotalAggNumber() { return totalAggNumber;}
  public void setAggNumber(int n) { aggNumber = n;}
  public void setTotalAggNumber(int n) { totalAggNumber = n;}
  public void incrementTotalAggNumber(int n) { totalAggNumber += n;}

  public double getVolume () { return volume;  }
  public void setVolume (double v) { volume = v; }
  public double getArea   () { return area;  }
  public void setArea (double a) { area = a; }
  public double getHeight () { return height;  }
  public void setHeight (double h) { height = h; }
  public double getWidth  () { return width;  }
  public void setWidth (double w) { width = w; }
  public double getDepth  () { return depth;  }
  public void setDepth (double d) { depth = d; }
  public double getWeight () { return weight;  }
  public void setWeight (double w) { weight = w; }
  public long getQuantity () { return aggNumber; }

  public String toString() {
    return getDisplayName() + " : " + getUID(); 
  }

}
