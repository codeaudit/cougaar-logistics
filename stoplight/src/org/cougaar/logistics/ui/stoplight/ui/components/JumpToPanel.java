//
// While this isn't really a JPanel, it does set up and manage one so we call
// it JumpToPanel
//

//Title:        Your Product Name
//Version:
//Copyright:    Copyright (c) 1999
//Author:       CSE, Ltd.
//Company:      CSE
//Description:  Your description

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.gui.Tool;


import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.map.app.CTitledComboBox;
import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;

public class JumpToPanel implements Tool
{

  protected final String defaultToolKey = "jumptopanel";
  protected String toolKey = new String (defaultToolKey);

  private CTitledComboBox mycb = null;
  
  public JumpToPanel()
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

    String[] choices = {"Eritrea", "Pakistan", "CONUS", "World"};
    ActionListener cbl = new ActionListener()
    {
       public void actionPerformed(ActionEvent e)
       {
         JComboBox combo=(JComboBox)e.getSource();
         String newValue = (String)combo.getSelectedItem();
         setMapDisplayFor(newValue);
        }
    };

   mycb = new CTitledComboBox("Jump to", choices, cbl);

  }

    void setMapDisplayFor(String location) {
        float lat=15, lon=39, zoom=3500000;
        if (location.equalsIgnoreCase("Eritrea")) {
            lat=15.5f;
            lon=39.5f;
            zoom=4000000f;
        } else if (location.equalsIgnoreCase("Pakistan")) {
            lat=30.2f;
            lon=70.2f;
            zoom=9000000f;
        } else if (location.equalsIgnoreCase("CONUS")) {
            lat=40f;
            lon=-100f;
            zoom=25000000f;
        } else if (location.equalsIgnoreCase("World")) {
            lat=0f;
            lon=0f;
            zoom=165000000f;
        } else {
            System.err.println("Do not know about location: "+location);
            location="Default location";
        }
        
//        System.out.println ("\nJumpToPanel: getMapBean with root pane: " + mycb.getRootPane().toString() );
        ScenarioMapBean mymap = ScenarioMap.getMapBean(mycb.getRootPane());
        mymap.setScale(zoom);
        mymap.setCenter(new LatLonPoint(lat,lon));

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
      return mycb;
    }


}
