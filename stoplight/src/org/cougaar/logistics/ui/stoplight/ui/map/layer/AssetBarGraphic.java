/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.text.*;



import com.bbn.openmap.proj.Projection;

import com.bbn.openmap.omGraphics.OMGraphic;

import org.cougaar.logistics.ui.stoplight.ui.components.RangeSliderPanel;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.DataSet;
import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.VecIcon;

public class AssetBarGraphic extends OMGraphic
{
  public static final String ON_HAND = "On Hand";
  public static final Color PLOT_COLOR = new Color(25, 25, 112);

//  public static double max = 0.0;
  public double max = 0.0;

  private ScenarioMap map = null;
  private Vector dataSetList = new Vector(0);
  private Hashtable assetList = new Hashtable(1);
  private String assetName = null;

  private boolean visible = true;

  private DecimalFormat dformat = new DecimalFormat("0.00");

  public AssetBarGraphic(ScenarioMap map, Hashtable dataSets, String assetName)
  {
    this.map = map;
    this.assetName = assetName;

    if (dataSets != null)
    {
      dataSetList.add(dataSets);
      assetList.put(assetName, dataSets);
    }
  }

  public void addDataSets(Hashtable dataSets, String assetName)
  {
    this.assetName = assetName;

    if (dataSets != null)
    {
      dataSetList.add(dataSets);
      assetList.put(assetName, dataSets);
    }
  }

  public void setCurrentAsset(String assetName)
  {
    this.assetName = assetName;
  }

  public String getCurrentAsset()
  {
    return(assetName);
  }

  public Enumeration getAssetNameList()
  {
    return(assetList.keys());
  }

  public Vector getDataSets()
  {
    return(dataSetList);
  }

  public boolean isVisible()
  {
    return(visible);
  }

  public void setVisible(boolean visible)
  {
    this.visible = visible;
  }

  public boolean generate(Projection proj)
  {
    return(true);
  }

  public void render(Graphics g)
  {
    // Don't know where to render here
  }

  public void render(Graphics g, Rectangle textBounds)
  {
    Hashtable dataSets = (Hashtable)assetList.get(assetName);

    if (!visible || (assetName == null) || (assetName.length() == 0) || (dataSets == null))
    {
      return;
    }

    Color c = g.getColor();
    Font f = g.getFont();

    int x, y;

    g.setFont(f.deriveFont(10.0f));
    FontMetrics fm = g.getFontMetrics();
    double quantity = ((dataSets != null) && (dataSets.get(ON_HAND) != null)) ?
    ((DataSet)dataSets.get(ON_HAND)).getClosestPoint((double) RangeSliderPanel.rangeSlider.getValue() , 0.0)[1] : 0.0;

    // Asset name

    Rectangle2D nameBounds = fm.getStringBounds(" " + assetName + " ", g);
    x = (int)((textBounds.x + textBounds.width/2) - (nameBounds.getWidth()/2.0));
    y = (int)(textBounds.y - nameBounds.getHeight());

    nameBounds.setRect(x, y, nameBounds.getWidth(), nameBounds.getHeight());

    g.setColor(Color.white);
    g.fillRect((int)nameBounds.getX(), (int)nameBounds.getY(), (int)nameBounds.getWidth(), (int)nameBounds.getHeight());
    g.setColor(Color.blue);
    g.drawString(" " + assetName + " ", x, y + (int)nameBounds.getHeight() - fm.getMaxDescent());
    g.setColor(Color.black);
    g.drawRect((int)nameBounds.getX(), (int)nameBounds.getY(), (int)nameBounds.getWidth(), (int)nameBounds.getHeight());



    // Asset bar

    g.setColor(Color.white);
    x = (int)((textBounds.x + textBounds.width/2) - (12/2.0));
    y = (int)(nameBounds.getY() - 24);
    g.fillRect(x, y, 12, 24);
    g.setColor(PLOT_COLOR);
    int pixels = (int)(20 * (quantity/max));
    pixels = ((quantity > 0.0) && (pixels < 1)) ? 1 : pixels;
    g.fillRect(x+2, y+2+(20-pixels), 8, pixels);
    g.setColor(Color.black);
    g.drawRect(x, y, 12, 24);



    // Asset quantity

    String dataString = " " + dformat.format(quantity) + " ";
    Rectangle2D dataBounds = fm.getStringBounds(dataString, g);
    x = (int)((textBounds.x + textBounds.width/2) - (dataBounds.getWidth()/2.0));
    y = (int)(textBounds.y - dataBounds.getHeight() - nameBounds.getHeight() - 24);

    dataBounds.setRect(x, y, dataBounds.getWidth(), dataBounds.getHeight());

    g.setColor(Color.white);
    g.fillRect((int)dataBounds.getX(), (int)dataBounds.getY(), (int)dataBounds.getWidth(), (int)dataBounds.getHeight());
    g.setColor(Color.blue);
    g.drawString(dataString, x, y + (int)dataBounds.getHeight() - fm.getMaxDescent());
    g.setColor(Color.black);
    g.drawRect((int)dataBounds.getX(), (int)dataBounds.getY(), (int)dataBounds.getWidth(), (int)dataBounds.getHeight());





    g.setFont(f);
    g.setColor(c);
  }

  public float distance(int x, int y)
  {
    return(Float.POSITIVE_INFINITY);
  }
}
