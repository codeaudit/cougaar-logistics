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
package org.cougaar.mlm.ui.newtpfdd.gui.view.node;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;

public class CargoInstance extends DBUIDNode implements DimensionNode {
  long quantity = 1;
  double weight;  
  double width;  
  double height;  
  double depth;  
  double area;  
  double volume;  
  String alpType;
  String container;

  public CargoInstance (UIDGenerator generator, String dbuid) {
    super (generator, dbuid);
  }

  public int getType () { return UIDGenerator.ASSET_INSTANCE; }
  public long getQuantity () { return quantity; }
  public void setQuantity (long q) { quantity = q; }

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
  public String getALPType () { return alpType; }
  public void setALPType (String type) { alpType = type; }
  public String getContainer () { return container; }
  public void setContainer (String container) { this.container=container; }

  public boolean isTransport() { return true; }
}

