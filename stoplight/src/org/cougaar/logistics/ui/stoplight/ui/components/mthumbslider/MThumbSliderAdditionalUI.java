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

package org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;

public class MThumbSliderAdditionalUI {

  CMThumbSlider  mSlider;
  BasicSliderUI ui;
  Rectangle[]   thumbRects;
  int           thumbNum;
  private transient boolean isDragging;
  Icon thumbRenderer;

  Rectangle trackRect;

  ChangeHandler changeHandler;
  TrackListener trackListener;

  public MThumbSliderAdditionalUI(BasicSliderUI ui)   {
    this.ui      = ui;
  }

  public void installUI(JComponent c)   {
    mSlider    = (CMThumbSlider)c;
    thumbNum   = mSlider.getThumbNum();
    thumbRects = new Rectangle[thumbNum];
    for (int i=0; i<thumbNum; i++) {
      thumbRects[i] = new Rectangle();
    }
    isDragging = false;
    trackListener = new MThumbSliderAdditionalUI.TrackListener(mSlider);
    changeHandler = new ChangeHandler();
  }

  public void uninstallUI(JComponent c) {
    thumbRects = null;
    trackListener = null;
    changeHandler = null;
  }

  protected void calculateThumbsSize() {
    Dimension size = ((MThumbSliderAdditional)ui).getThumbSize();
    for (int i=0; i<thumbNum; i++) {
      thumbRects[i].setSize( size.width, size.height );
    }
  }

  protected void calculateThumbsLocation() {
    for (int i=0; i<thumbNum; i++) {
      if ( mSlider.getSnapToTicks() ) {
        int tickSpacing = mSlider.getMinorTickSpacing();
        if (tickSpacing == 0) {
          tickSpacing = mSlider.getMajorTickSpacing();
        }
        if (tickSpacing != 0) {
          int sliderValue  = mSlider.getValueAt(i);
          int snappedValue = sliderValue;
          //int min = mSlider.getMinimumAt(i);
          int min = mSlider.getMinimum();
          if ( (sliderValue - min) % tickSpacing != 0 ) {
            float temp = (float)(sliderValue - min) / (float)tickSpacing;
            int whichTick = Math.round( temp );
            snappedValue = min + (whichTick * tickSpacing);
            mSlider.setValueAt( snappedValue , i);
          }
        }
      }
      trackRect = getTrackRect();
      if ( mSlider.getOrientation() == JSlider.HORIZONTAL ) {
        int value = mSlider.getValueAt(i);
        int valuePosition = ((MThumbSliderAdditional)ui).xPositionForValue(value);
        thumbRects[i].x = valuePosition - (thumbRects[i].width / 2);
        thumbRects[i].y = trackRect.y;
      }
      else {
        int valuePosition = ((MThumbSliderAdditional)ui).yPositionForValue(mSlider.getValueAt(i));     // need
        thumbRects[i].x = trackRect.x;
        thumbRects[i].y = valuePosition - (thumbRects[i].height / 2);
      }
    }
  }

  public int getThumbNum() {
    return thumbNum;
  }

  public Rectangle[] getThumbRects() {
    return thumbRects;
  }

  private static Rectangle unionRect = new Rectangle();

  public void setThumbLocationAt(int x, int y, int index)  {
    Rectangle rect = thumbRects[index];
    unionRect.setBounds( rect );

    rect.setLocation( x, y );
    SwingUtilities.computeUnion( rect.x, rect.y, rect.width, rect.height, unionRect );
    mSlider.repaint( unionRect.x, unionRect.y, unionRect.width, unionRect.height );
  }

  public Rectangle getTrackRect() {
    return ((MThumbSliderAdditional)ui).getTrackRect();
  }

  public class ChangeHandler implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      if ( !isDragging ) {
        calculateThumbsLocation();
        mSlider.repaint();
      }
    }
  }

  public class TrackListener extends MouseInputAdapter {
    protected transient int offset;
    protected transient int currentMouseX, currentMouseY;
    protected Rectangle adjustingThumbRect = null;
    protected int adjustingThumbIndex;
    protected CMThumbSlider   slider;
    protected Rectangle trackRect;

    TrackListener(CMThumbSlider slider) {
      this.slider = slider;
    }

    protected transient boolean lockScroll = false;
    protected transient int offset2 = 0;

    public void mousePressed(MouseEvent e)
    {
      if ( !slider.isEnabled() )
      {
        return;
      }
      currentMouseX = e.getX();
      currentMouseY = e.getY();
      slider.requestFocus();

      if ((adjustingThumbIndex = pointInsideThumbs(currentMouseX, currentMouseY)) > -1)
      {
        lockScroll = true;
        isDragging = true;
        slider.setValueIsAdjusting(true);
        adjustingThumbRect = thumbRects[adjustingThumbIndex];

        switch (slider.getOrientation())
        {
          case JSlider.VERTICAL:
               offset = currentMouseY - adjustingThumbRect.y;
               offset2 = currentMouseY - thumbRects[adjustingThumbIndex+1].y;
               break;
          case JSlider.HORIZONTAL:
               offset = currentMouseX - adjustingThumbRect.x;
               offset2 = currentMouseX - thumbRects[adjustingThumbIndex+1].x;
               break;
        }
      }
      else
      {
        for (int i=0; i<thumbNum; i++)
        {
          Rectangle rect = thumbRects[i];
          if (rect.contains(currentMouseX, currentMouseY))
          {
            switch (slider.getOrientation())
            {
              case JSlider.VERTICAL:
                   offset = currentMouseY - rect.y;
                   offset2 = 0;
                   break;
              case JSlider.HORIZONTAL:
                   offset = currentMouseX - rect.x;
                   offset2 = 0;
                   break;
            }

            lockScroll = false;
            isDragging = true;
            slider.setValueIsAdjusting(true);
            adjustingThumbRect = rect;
            adjustingThumbIndex = i;

            break;
          }
        }
      }
    }

    public void mouseMoved(MouseEvent e)
    {
      if (isDragging)
      {
        return;
      }

      if (pointInsideThumbs(e.getX(), e.getY()) > -1)
      {
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      else
      {
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }

    private int pointInsideThumbs(int x, int y)
    {
      if (((MetalMThumbSliderUI)ui).getTrackBounds().contains(x, y))
      {
        for (int i=0; i<thumbNum-1; i++)
        {
          switch (slider.getOrientation())
          {
            case JSlider.VERTICAL:
              if ((thumbRects[i+1].y + thumbRects[i+1].height < y) && (y < thumbRects[i].y))
              {
                return(i);
              }
            break;

            case JSlider.HORIZONTAL:
              if ((thumbRects[i].x + thumbRects[i].width < x) && (x < thumbRects[i+1].x))
              {
                return(i);
              }
            break;
          }
        }
      }

      return(-1);
    }

    public void mouseDragged( MouseEvent e )
    {
      if (!slider.isEnabled() || !isDragging || !slider.getValueIsAdjusting() || adjustingThumbRect == null)
      {
        return;
      }

      currentMouseX = e.getX();
      currentMouseY = e.getY();
      trackRect = getTrackRect();

      moveThumb(adjustingThumbIndex, offset);
      if (lockScroll)
      {
        moveThumb(adjustingThumbIndex +1, offset2);
      }
    }

    private void moveThumb(int thumbIndex, int dragOffset)
    {
      int thumbMiddle = 0;
      Rectangle rect = thumbRects[thumbIndex];

      switch (slider.getOrientation())
      {
        case JSlider.VERTICAL:
          int halfThumbHeight = rect.height / 2;
          int thumbTop    = currentMouseY - dragOffset;
          int trackTop    = trackRect.y;
          int trackBottom = trackRect.y + (trackRect.height - 1);

          thumbTop = Math.max( thumbTop, trackTop    - halfThumbHeight );
          thumbTop = Math.min( thumbTop, trackBottom - halfThumbHeight );

          setThumbLocationAt(rect.x, thumbTop, thumbIndex);

          thumbMiddle = thumbTop + halfThumbHeight;
          mSlider.setValueAt( ui.valueForYPosition( thumbMiddle ) , thumbIndex);
          calculateThumbsLocation(); // PHF (so thumbs can push each other around)
          break;

        case JSlider.HORIZONTAL:
          int halfThumbWidth = rect.width / 2;
          int thumbLeft  = currentMouseX - dragOffset;
          int trackLeft  = trackRect.x;
          int trackRight = trackRect.x + (trackRect.width - 1);

          thumbLeft = Math.max( thumbLeft, trackLeft  - halfThumbWidth );
          thumbLeft = Math.min( thumbLeft, trackRight - halfThumbWidth );

          setThumbLocationAt( thumbLeft, rect.y, thumbIndex);

          thumbMiddle = thumbLeft + halfThumbWidth;
          mSlider.setValueAt( ui.valueForXPosition( thumbMiddle ), thumbIndex );
          calculateThumbsLocation(); // PHF (so thumbs can push each other around)
          break;
      }
    }

    public void mouseReleased(MouseEvent e)
    {
      if (!slider.isEnabled())
      {
        return;
      }

      lockScroll = false;
      offset = 0;
      offset2 = 0;
      isDragging = false;
      mSlider.setValueIsAdjusting(false);
      mSlider.repaint();
    }

    public boolean shouldScroll(int direction) {
      return false;
    }
  }
}

