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

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.models.*;

/***********************************************************************************************************************
<b>Description</b>: This class is the implementation of the Date Command Slider Cougaar Desktop component.  It provides
                    the display for a global desktop slider which can control the slider movement of other desktop 
                    components (simultaneously) that implement the DateControllableSliderUI interface.

***********************************************************************************************************************/
public class DateCommandSliderComponent extends ComponentFactory implements CougaarDesktopUI
{
	public String getToolDisplayName()
	{
	  return("Date Command Slider");
	}

	public CougaarDesktopUI create()
	{
	  return(this);
	}

  public boolean supportsPlaf()
  {
    return(true);
  }

  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public void install(CDesktopFrame f)
  {
    try
    {
      f.getContentPane().setLayout(new BorderLayout());
      f.getContentPane().add(new CommandSlider(f), BorderLayout.CENTER);
      f.revalidate();
      f.repaint();
    }
    catch (Throwable t)
    {
      t.printStackTrace();
    }
  }

  public boolean isPersistable()
  {
    return(false);
  }

  public Serializable getPersistedData()
  {
    return(null);
  }

  public void setPersistedData(Serializable data)
  {
  }

  public String getTitle()
  {
    return("Date Command Slider");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(250, 50));
  }

  public boolean isResizable()
  {
    return(true);
  }
}

class CommandSlider extends JPanel implements PropertyChangeListener
{
  private CMThumbSliderDateAndTimeRangeControl rangeControl = null;
  private CDateLabeledSlider valueControl = null;
  private CDesktopFrame frame = null;

  private static final int SINGLE_MODE = 1;
  private static final int DUAL_MODE = 2;
  private int MODE = DUAL_MODE;

  private JMenu windowMenu = null;

  private float minLimit = Float.MAX_VALUE;
  private float maxLimit = Float.MIN_VALUE;

  private Hashtable componentTable = new Hashtable(1);

  public CommandSlider(CDesktopFrame frame)
  {
    this.frame = frame;

		frame.addInternalFrameListener(new InternalFrameListener()
      {
        public void internalFrameOpened(InternalFrameEvent e)
        {
        }
      
        public void internalFrameClosing(InternalFrameEvent e)
        {
        }
      
        public void internalFrameClosed(InternalFrameEvent e)
        {
        }
      
        public void internalFrameIconified(InternalFrameEvent e)
        {
        }
      
        public void internalFrameDeiconified(InternalFrameEvent e)
        {
        }
      
        public void internalFrameActivated(InternalFrameEvent e)
        {
          listComponents();
        }
      
        public void internalFrameDeactivated(InternalFrameEvent e)
        {
        }
      });

    rangeControl = new CMThumbSliderDateAndTimeRangeControl(0.0f, 0.0f);
    rangeControl.setDynamicLabelsVisible(false);
    rangeControl.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
    rangeControl.setTimeScale(1000);

    valueControl = new CDateLabeledSlider("", 0, 0.0f, 0.0f);
    valueControl.setShowTicks(true);

    rangeControl.addPropertyChangeListener("range", this);
    valueControl.addPropertyChangeListener("value", this);

    this.setLayout(new BorderLayout());
    this.add(rangeControl, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		windowMenu = (JMenu)menuBar.add(new JMenu("Window"));
		windowMenu.setMnemonic(KeyEvent.VK_W);

		JMenu menu = (JMenu)menuBar.add(new JMenu("Control"));
		ButtonGroup group = new ButtonGroup();
		menu.setMnemonic(KeyEvent.VK_C);
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem("Single", false);
		menuItem.addActionListener(new ActionListener()
  		{
  		  public void actionPerformed(ActionEvent e)
  		  {
  		    if (((JRadioButtonMenuItem)e.getSource()).isSelected())
  		    {
    		    MODE = SINGLE_MODE;
            CommandSlider.this.remove(rangeControl);
            CommandSlider.this.add(valueControl, BorderLayout.CENTER);
            CommandSlider.this.revalidate();
            CommandSlider.this.repaint();
          }
  		  }
  		});
  	menu.add(menuItem);
  	group.add(menuItem);

		menuItem = new JRadioButtonMenuItem("Dual", true);
		menuItem.addActionListener(new ActionListener()
  		{
  		  public void actionPerformed(ActionEvent e)
  		  {
  		    if (((JRadioButtonMenuItem)e.getSource()).isSelected())
  		    {
    		    MODE = DUAL_MODE;
            CommandSlider.this.remove(valueControl);
            CommandSlider.this.add(rangeControl, BorderLayout.CENTER);
            CommandSlider.this.revalidate();
            CommandSlider.this.repaint();
          }
  		  }
  		});
  	menu.add(menuItem);
  	group.add(menuItem);

		frame.setJMenuBar(menuBar);
  }

  public void rangeLimitChangeNotification()
  {
    minLimit = Float.MAX_VALUE;
    maxLimit = Float.MIN_VALUE;

    for (Enumeration e=componentTable.elements(); e.hasMoreElements();)
    {
      SliderListener listener = (SliderListener)e.nextElement();

      if (listener.isSelected())
      {
        minLimit = (listener.getMinLimit() < minLimit) ? listener.getMinLimit() : minLimit;
        maxLimit = (listener.getMaxLimit() > maxLimit) ? listener.getMaxLimit() : maxLimit;
      }
    }

    minLimit = (minLimit == Float.MAX_VALUE) ? 0.0f : minLimit;
    maxLimit = (maxLimit == Float.MIN_VALUE) ? 0.0f : maxLimit;

    rangeControl.removePropertyChangeListener("range", this);
    rangeControl.setSliderRange(minLimit, maxLimit);
    rangeControl.addPropertyChangeListener("range", this);

    valueControl.removePropertyChangeListener("value", this);
    valueControl.setSliderRange(minLimit, maxLimit);
    valueControl.addPropertyChangeListener("value", this);
  }

  public void propertyChange(PropertyChangeEvent ev)
  {
    RangeModel range = rangeControl.getRange();

    switch (MODE)
    {
      case SINGLE_MODE:
        float value = valueControl.getValue();

        for (Enumeration e=componentTable.elements(); e.hasMoreElements();)
        {
          ((SliderListener)e.nextElement()).setValue(value);
        }

        float diff = (range.getFMax() - range.getFMin())/2.0f;
        RangeModel newRange = new RangeModel(value - diff, value + diff);
        rangeControl.removePropertyChangeListener("range", this);
        rangeControl.setRange(newRange);
        rangeControl.addPropertyChangeListener("range", this);
      break;

      case DUAL_MODE:
        for (Enumeration e=componentTable.elements(); e.hasMoreElements();)
        {
          ((SliderListener)e.nextElement()).setRange(range);
        }

        valueControl.removePropertyChangeListener("value", this);
        valueControl.setValue(range.getFMin() + (range.getFMax() - range.getFMin())/2.0f);
        valueControl.addPropertyChangeListener("value", this);
      break;
    }
  }

  private void listComponents()
  {
    minLimit = Float.MAX_VALUE;
    maxLimit = Float.MIN_VALUE;

    CDesktopFrame[] frameList = frame.getAllDesktopFrames();
    for (int i=0; i<frameList.length; i++)
    {
      if (!componentTable.containsKey(frameList[i]))
      {
        CougaarDesktopUI component = frameList[i].getComponent();
        if (component instanceof DateControllableSliderUI)
        {
          SliderProxy proxy = (SliderProxy)((DateControllableSliderUI)component).getDateControllableSlider();
          if (proxy != null)
          {
            SliderListener listener = new SliderListener(frameList[i], windowMenu, proxy, this, componentTable);
            componentTable.put(frameList[i], listener);
          }
        }
      }
      else
      {
        SliderListener listener = (SliderListener)componentTable.get(frameList[i]);

        if ((listener.getMinLimit() != listener.getMaxLimit()) && (listener.isSelected()))
        {
          minLimit = (listener.getMinLimit() < minLimit) ? listener.getMinLimit() : minLimit;
          maxLimit = (listener.getMaxLimit() > maxLimit) ? listener.getMaxLimit() : maxLimit;
        }
      }
    }

    minLimit = (minLimit == Float.MAX_VALUE) ? 0.0f : minLimit;
    maxLimit = (maxLimit == Float.MIN_VALUE) ? 0.0f : maxLimit;

    rangeControl.removePropertyChangeListener("range", this);
    rangeControl.setSliderRange(minLimit, maxLimit);
    rangeControl.addPropertyChangeListener("range", this);

    valueControl.removePropertyChangeListener("value", this);
    valueControl.setSliderRange(minLimit, maxLimit);
    valueControl.addPropertyChangeListener("value", this);
  }
}

class SliderListener extends JCheckBoxMenuItem implements ActionListener, InternalFrameListener, ChangeListener
{
  protected CDesktopFrame frame = null;
  protected JMenu menu = null;
  protected SliderProxy sliderProxy = null;
  protected CommandSlider commandSlider = null;

  protected Hashtable hash = null;
  
  private PropertyChangeListener titleChangeListener = new PropertyChangeListener()
    {
      public void propertyChange(PropertyChangeEvent e)
      {
        setText(SliderListener.this.frame.getTitle());
      }
    };
  
  public SliderListener(CDesktopFrame frame, JMenu menu, SliderProxy sliderProxy, CommandSlider commandSlider, Hashtable hash)
  {
    this.frame = frame;
    this.menu = menu;
    this.sliderProxy = sliderProxy;
    this.commandSlider = commandSlider;
    this.hash = hash;

    sliderProxy.addChangeListener(this);

    setText(frame.getTitle());
    setSelected(false);
    
    addActionListener(this);
    frame.addInternalFrameListener(this);
    frame.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, titleChangeListener);
  	menu.add(this);
  }

  public void internalFrameOpened(InternalFrameEvent e)
  {
  }

  public void internalFrameClosing(InternalFrameEvent e)
  {
  }

  public void internalFrameClosed(InternalFrameEvent e)
  {
    frame.removePropertyChangeListener(JInternalFrame.TITLE_PROPERTY, titleChangeListener);
    menu.remove(this);
  }

  public void internalFrameIconified(InternalFrameEvent e)
  {
  }

  public void internalFrameDeiconified(InternalFrameEvent e)
  {
  }

  public void internalFrameActivated(InternalFrameEvent e)
  {
  }

  public void internalFrameDeactivated(InternalFrameEvent e)
  {
  }

  public void dispose()
  {
    frame.removePropertyChangeListener(JInternalFrame.TITLE_PROPERTY, titleChangeListener);
    menu.remove(this);
    hash.remove(frame);
    commandSlider.rangeLimitChangeNotification();

    sliderProxy.removeChangeListener(this);
    
    sliderProxy.dispose();
  }

  public void stateChanged(ChangeEvent e)
  {
    if (isSelected())
    {
      commandSlider.rangeLimitChangeNotification();
    }
  }

  public void actionPerformed(ActionEvent e)
  {
    sliderProxy.enableSlider(isSelected());
    commandSlider.rangeLimitChangeNotification();
  }

  public float getMinLimit()
  {
    return(sliderProxy.getMinLimit());
  }

  public float getMaxLimit()
  {
    return(sliderProxy.getMaxLimit());
  }

  public void setRange(RangeModel range)
  {
    sliderProxy.setRange(range);
  }

  public void setValue(float value)
  {
    sliderProxy.setValue(value);
  }
}
