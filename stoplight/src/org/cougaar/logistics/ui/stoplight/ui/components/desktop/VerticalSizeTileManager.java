/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

import java.awt.Dimension;
import java.util.*;

/***********************************************************************************************************************
<b>Description</b>: This class is an implementation of the tile manager interface that attempts to tile windows by
                    vertical alignment based on the vertical size of each frame's window.

***********************************************************************************************************************/
public class VerticalSizeTileManager implements TileManager
{
  class PixelFrameSizeCompare implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      Dimension size1 = ((CDesktopFrame)o1).getSize();
      Dimension size2 = ((CDesktopFrame)o2).getSize();
      
      if ((size1.width * size1.height) < (size2.width * size2.height))
      {
        return(1);
      }

      if ((size1.width * size1.height) > (size2.width * size2.height))
      {
        return(-1);
      }

      return(0);
    }
    
    public boolean equals(Object obj)
    {
      return(this == obj);
    }
  }
  
  private Vector removeLargest(Vector frameList, double percent)
  {
    Collections.sort(frameList, new PixelFrameSizeCompare());
    Vector largest = new Vector(1);
    if (frameList.size() > 0)
    {
      CDesktopFrame biggestFrame = (CDesktopFrame)frameList.remove(0);
//biggestFrame.setTitle(biggestFrame.getSize().width + "x" + biggestFrame.getSize().width + " " + biggestFrame.getSize().width * biggestFrame.getSize().height + " " + "100%");
      largest.add(biggestFrame);
      Dimension bigestSize = biggestFrame.getSize();
      for (int i=0, isize=frameList.size(); i<isize; i++)
      {
        CDesktopFrame frame = (CDesktopFrame)frameList.get(i);
        Dimension size = frame.getSize();
        
//frame.setTitle(size.width + "x" + size.width + " " + (bigestSize.width * bigestSize.height * percent) + " " + size.width * size.height);

        if (((bigestSize.width * bigestSize.height * percent) > (size.width * size.height)) || (i+1 == isize))
        {
          for (int j=0, jsize=i; j<jsize; j++)
          {
            largest.add(frameList.remove(0));
            i--;
            isize--;
          }
          break;
        }
      }
    }
    return(largest);
  }
  
  private int getMaxWidth(Vector frameList)
  {
    int width = 0;
    for (int i=0, isize=frameList.size(); i<isize; i++)
    {
      CDesktopFrame frame = (CDesktopFrame)frameList.elementAt(i);
      width = (width < frame.getSize().width) ? frame.getSize().width : width;
    }
    
    return(width);
  }
  
  public void tile(CDesktopPane desktopPane)
  {
    CDesktopFrame[] frames = desktopPane.getAllDesktopFrames();
    Vector frameList = new Vector(Arrays.asList(frames));
    
    Dimension d = desktopPane.getSize();
    int currentX = 0;
    int currentY = 0;

    while (frameList.size() > 0)
    {
      Vector largest = removeLargest(frameList, 0.70);
      int maxWidth = getMaxWidth(largest);

      // Move to the next row
      currentX = currentX + maxWidth;
      currentY = 0;

      // Set the frame sizes and move them on the desktop
      for (int i=0, isize=largest.size(); i<isize; i++)
      {
        CDesktopFrame frame = (CDesktopFrame)largest.elementAt(i);
        frame.setSize(maxWidth, frame.getSize().height);
        frame.setLocation(currentX - maxWidth, currentY);
        currentY += frame.getSize().height;
      }

      // Determie if the total height of the frames will fit in the desktop, and fix it if it cannot
      d.height = (d.height < currentY) ? currentY : d.height;
    }

    // Determie if the total width of the frames will fit in the desktop, and fix it if it cannot
    d.width = (d.width < currentX) ? currentX : d.width;

		desktopPane.setPreferredSize(d);
		desktopPane.revalidate();
  }
}
