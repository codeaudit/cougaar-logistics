/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
/* (swing1.1.1) */
package org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;



/**
 */
public class BasicMThumbSliderUI extends BasicSliderUI
                implements MThumbSliderAdditional {

  MThumbSliderAdditionalUI additonalUi;
  MouseInputAdapter mThumbTrackListener;


  public static ComponentUI createUI(JComponent c)    {
    return new BasicMThumbSliderUI((JSlider)c);
  }


  public BasicMThumbSliderUI()   {
    super(null);
  }

  public BasicMThumbSliderUI(JSlider b)   {
    super(b);
  }


  public void installUI(JComponent c)   {
    additonalUi = new MThumbSliderAdditionalUI(this);
    additonalUi.installUI(c);
    mThumbTrackListener = createMThumbTrackListener((JSlider) c);
    super.installUI(c);
  }

  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
    additonalUi.uninstallUI(c);
    additonalUi = null;
    mThumbTrackListener = null;
  }

  protected MouseInputAdapter createMThumbTrackListener( JSlider slider ) {
    return additonalUi.trackListener;
  }

  protected TrackListener createTrackListener( JSlider slider ) {
    return null;
  }

  protected ChangeListener createChangeListener( JSlider slider ) {
    return additonalUi.changeHandler;
  }

  protected void installListeners( JSlider slider ) {
    slider.addMouseListener(mThumbTrackListener);
    slider.addMouseMotionListener(mThumbTrackListener);
    slider.addFocusListener(focusListener);
    slider.addComponentListener(componentListener);
    slider.addPropertyChangeListener( propertyChangeListener );
    slider.getModel().addChangeListener(changeListener);
  }

  protected void uninstallListeners( JSlider slider ) {
    slider.removeMouseListener(mThumbTrackListener);
    slider.removeMouseMotionListener(mThumbTrackListener);
    slider.removeFocusListener(focusListener);
    slider.removeComponentListener(componentListener);
    slider.removePropertyChangeListener( propertyChangeListener );
    slider.getModel().removeChangeListener(changeListener);
  }

  protected void calculateGeometry() {
    super.calculateGeometry();
    additonalUi.calculateThumbsSize();
    additonalUi.calculateThumbsLocation();
  }



  protected void calculateThumbLocation() {}




  Rectangle zeroRect = new Rectangle();

  public void paint( Graphics g, JComponent c ) {

    Rectangle clip = g.getClipBounds();
    thumbRect = zeroRect;

    super.paint( g, c );

    int thumbNum = additonalUi.getThumbNum();
    Rectangle[] thumbRects = additonalUi.getThumbRects();

    for (int i=thumbNum-1; 0<=i; i--) {
      if ( clip.intersects( thumbRects[i] ) ) {
        thumbRect = thumbRects[i];

        paintThumb( g );

      }
    }
  }


  public void scrollByBlock(int direction)    {}
  public void scrollByUnit(int direction) {}

  //
  // MThumbSliderAdditional
  //
  public Rectangle getTrackRect() {
    return trackRect;
  }

  public Dimension getThumbSize() {
    return super.getThumbSize();
  }

  public int xPositionForValue(int value) {
    return super.xPositionForValue( value);
  }

  public int yPositionForValue(int value) {
    return super.yPositionForValue( value);
  }

}

