
//Title:        Your Product Name
//Version:
//Copyright:    Copyright (c) 1999
//Author:       CSE, Ltd.
//Company:      CSE
//Description:  Your description

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.BorderFactory;

import com.bbn.openmap.gui.Tool;
import com.bbn.openmap.Environment;

import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.PspIconLayer;

public class RangeSliderPanel extends JPanel implements Tool
{

  public static CDateLabeledSlider rangeSlider = null;
  protected final String defaultToolKey = "rangesliderpanel";
  protected String toolKey = new String (defaultToolKey);

  public RangeSliderPanel()
  {
    try
    {
      jbInit();
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private void jbInit() throws Exception
  {
    final long THIRTYDAYS = 30 * 24 * 60 * 60;  // in seconds

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    long currSeconds;
    long endSeconds;

    currSeconds = System.currentTimeMillis() / 1000L;
    endSeconds = currSeconds + THIRTYDAYS;

    currSeconds = PspIconLayer.dateToMillis(Environment.get(PspIconLayer.iStartDate), currSeconds)/1000L;
    endSeconds = PspIconLayer.dateToMillis(Environment.get(PspIconLayer.iEndDate), endSeconds)/1000L;

    rangeSlider = new CDateLabeledSlider ( "Date", 10, (float) currSeconds, (float) endSeconds );

    rangeSlider.setValue(currSeconds);
    rangeSlider.setSliderRange(currSeconds, endSeconds);

    rangeSlider.setCDate(PspIconLayer.dateToMillis(Environment.get(PspIconLayer.iCDate), currSeconds)/1000L);

    rangeSlider.setLabelWidth(rangeSlider.getMinimumLabelWidth(rangeSlider.getLabel()));
    rangeSlider.setShowTicks(true);
    rangeSlider.setPreferredSize(new Dimension(350, rangeSlider.getPreferredSize().height));

    add((CLabeledSlider)rangeSlider);

    add(Box.createHorizontalStrut(5));
    Dimension buttonSize = new Dimension(20, 15);
    Insets buttonInsets = new Insets(0, 0, 0, 0);

    JButton stepDownButton = new JButton("<");
    stepDownButton.setMargin(buttonInsets);
    stepDownButton.setPreferredSize(buttonSize);
    add(stepDownButton);

    JButton stepUpButton = new JButton(">");
    stepUpButton.setMargin(buttonInsets);
    stepUpButton.setPreferredSize(buttonSize);
    add(stepUpButton);

    setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Time"));

//    updateTime (currSeconds);

    stepDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                rangeSlider.setValue(rangeSlider.getValue() - (60 * 60 * 24) );
            }
        });

    stepUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                rangeSlider.setValue(rangeSlider.getValue() + (60 * 60 * 24));
            }
        });

    // For constant updating as slider is adjusted
    rangeSlider.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e)
            {
                long newValue = ((Number)e.getNewValue()).longValue();
                updateTime( newValue );
            }
        });
  }


    private void updateTime(long newSecs)
    {

      long newMillis = newSecs * 1000;
//      System.out.println ("RangeSliderPanel:updateTime - getting mapBean with parent: " + getParent().getParent().toString() +"\t" + getParent().getParent() );
      PspIconLayer myLayer= ScenarioMap.getMapBean( getRootPane() ).findPspIconLayer();

      if (myLayer!=null)
      {
//        System.err.println("new time: " + newValue);
        myLayer.setTime(String.valueOf(newMillis));
        myLayer.repaint();
      }

      else
      {
        System.err.println("cannot set time on layer -- myLayer is null");
      }

    }

    /**
     * Tool interface method.  The retrieval key for this tool.
     *
     * @return String The key for this tool.
     */
    public String getKey()
    {
       return toolKey;
    }

    /**
     * Tool interface method. Set the retrieval key for this tool.
     *
     * @param aKey The key for this tool.
     */
    public void setKey(String aKey)
    {
      toolKey = aKey;
    }

    /**
     * Tool interface method. The retrieval tool's interface. This is
     * added to the tool bar.
     *
     * @return String The key for this tool.
     */
    public Container getFace()
    {
      return this;
    }

} 
