/* **********************************************************************
 * 
 *    Use, duplication, or disclosure by the Government is subject to
 * 	     restricted rights as set forth in the DFARS.
 *  
 * 			   BBNT Solutions LLC
 * 			      A Part of  
 * 			         GTE      
 * 			  10 Moulton Street
 * 			 Cambridge, MA 02138
 * 			    (617) 873-3000
 *  
 * 	  Copyright 1998, 2000 by BBNT Solutions LLC,
 * 		A part of GTE, all rights reserved.
 *  
 * **********************************************************************
 * 
 * 
 * 
 * 
 * 
 * 
 * **********************************************************************
 */
package org.cougaar.logistics.ui.stoplight.ui.components;

import com.bbn.openmap.util.Debug;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.beans.*;
import java.io.Serializable;
import java.net.URL;

import com.bbn.openmap.*;
import com.bbn.openmap.event.*;
import com.bbn.openmap.gui.Tool;

import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap;

/**
 * Bean to zoom the Map.
 * <p>
 * This bean is a source for ZoomEvents.  It is a simple widget with a ZoomIn
 * button and a ZoomOut button.  When a button is pressed, the appropriate
 * zoom event is fired to all registered listeners.
 */
public class IconScalePanel extends JPanel implements Serializable, Tool
{

    public final static transient String scaleUpCmd = "scaleUp";
    public final static transient String scaleDownCmd = "scaleDown";

    protected transient JButton scaleUpButton, scaleDownButton;

    public final static transient String defaultToolKey = "iconscalepanel";
    protected String toolKey = defaultToolKey;

    private ActionListener actlis = new ActionListener()
    {
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                String command = e.getActionCommand();

//                System.out.println ("IconScalePanel: actionListener looking for map bean with parent: " + getParent().getParent().getParent().toString());
                ScenarioMapBean mapBean = ScenarioMap.getMapBean(getRootPane());

                if (command.equals(IconScalePanel.scaleUpCmd))
                {
                  mapBean.findPspIconLayer().changeIconScale (2.0f);
//                  ScenarioMap.mapBean.findPspIconLayer().changeIconScale (2.0f);
                }
	              else if (command.equals(IconScalePanel.scaleDownCmd))
                {
                  mapBean.findPspIconLayer().changeIconScale (-2.0f);
                }
              }
          };

    /**
     * Construct the IconScalePanel.
     */
    public IconScalePanel()
    {
      super();
//  	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	    scaleUpButton = addButton("scaleUp", "Scale Up", scaleUpCmd, actlis);
	    scaleDownButton = addButton("scaleDown", "Scale Down", scaleDownCmd, actlis);
    }

    /**
     * Add the named button to the panel.
     *
     * @param name GIF image name
     * @param info ToolTip text
     * @param command String command name
     *
     */
    protected JButton addButton(String name, String info, String command, ActionListener actlis)
    {
       
	        URL url = IconScalePanel.class.getResource(name + ".gif");
          
	        JButton b = new JButton(new ImageIcon(url, info));
	        b.setToolTipText(info);
	        b.setMargin(new Insets(0,0,0,0));
          b.setActionCommand(command);
	        b.addActionListener(actlis);
	        b.setBorderPainted(false);
	        add(b);
	        return b;
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
