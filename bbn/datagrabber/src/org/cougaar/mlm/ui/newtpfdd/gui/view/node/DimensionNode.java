package org.cougaar.mlm.ui.newtpfdd.gui.view.node;

public interface DimensionNode {
  public double getVolume ();
  public double getArea   ();
  public double getHeight ();
  public void setHeight (double h);
  public double getWidth  ();
  public void setWidth (double w);
  public double getDepth  ();
  public void setDepth (double d);
  public double getWeight ();
  public void setWeight (double w);
  public long getQuantity ();
}
