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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.util.Enumeration;

import java.awt.Graphics;
import javax.swing.JPanel;
import java.applet.Applet;
import java.awt.*;
import java.net.URL;

public class USAImagePanel extends JPanel
{
  USAImageMapComponent myComp;

  USAImagePanel(USAImageMapComponent imComp)
  {
    super();

    myComp = imComp;
  }

  protected void paintComponent (Graphics g)
  {

    super.paintComponent(g);

    Toolkit tk = Toolkit.getDefaultToolkit();

    Image usaIm = tk.getImage ("smallConus.gif");

    if (usaIm != null)
      g.drawImage (usaIm, 0,0, this); // Component, in our superclass, implements ImageObserver
    else
      System.err.println ( "USA image file not found!");

    Font font = new Font ("Arial", Font.BOLD, 12);
    g.setFont(font);

    synchronized (myComp.mapLocationObjects)
    {
      Enumeration eKeys = myComp.mapLocationObjects.keys();
      while (eKeys.hasMoreElements())
      {
        ClusterNetworkMetrics cnm = (ClusterNetworkMetrics) myComp.mapLocationObjects.get(eKeys.nextElement());
        if (cnm.xCoord != 0 && cnm.yCoord != 0)
        {
          String dispText = new String (cnm.clusterName);
          g.drawString(dispText, cnm.xCoord + 10, cnm.yCoord + 5);
          Image xspot = tk.getImage("smalldot.gif");

          int imgX = cnm.xCoord, imgY = cnm.yCoord;
          if (cnm.xCoord > 4)
            imgX = imgX -=5;
          if (cnm.yCoord > 4)
            imgY -= 5;
          g.drawImage(xspot, imgX, imgY, this);
        }
      }
    }

/*
    String om = new String ("OrderManager");
    Font font = new Font ("Arial", Font.BOLD, 8);
    g.setFont(font);
    g.drawString(om,100, 100);

    Image xspot = tk.getImage("selectX.gif");
    g.drawImage(xspot, 80, 95, this);
*/

  }


}