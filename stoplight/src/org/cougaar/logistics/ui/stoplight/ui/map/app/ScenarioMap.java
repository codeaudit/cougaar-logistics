/* **********************************************************************
 * 
 *    Use, duplication, or disclosure by the Government is subject to
 *              restricted rights as set forth in the DFARS.
 *  
 *                            BBNT Solutions LLC
 *                               A Part of  
 *                                  GTE      
 *                           10 Moulton Street
 *                          Cambridge, MA 02138
 *                             (617) 873-3000
 *  
 *           Copyright 1998, 2000 by BBNT Solutions LLC,
 *                 A part of GTE, all rights reserved.
 *  
 * **********************************************************************
 *
 * ***********************************************************************/

package org.cougaar.logistics.ui.stoplight.ui.map.app;

import java.awt.BorderLayout;
import java.awt.Color;

import java.io.*;
import java.util.Properties;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import com.bbn.openmap.*;
import com.bbn.openmap.PropertyHandler;

import com.bbn.openmap.proj.Mercator;
import com.bbn.openmap.proj.ProjectionFactory;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.ArgParser;

import org.cougaar.logistics.ui.stoplight.ui.components.ScenarioFrame;
import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;
import org.cougaar.util.ConfigFinder;

public class ScenarioMap implements CougaarUI
{
  
    private static Hashtable mapBeans = new Hashtable(); // ScenarioMapBean mapBean;
    private ScenarioMapBean localMapBean = null;

    public ScenarioMap ()
    {
      PropertyHandler propertyHandler = new PropertyHandler();
      initialize ( propertyHandler);
    }

    public ScenarioMap(PropertyHandler propertyHandler)
    {
      initialize (propertyHandler);
    }

    private void initialize (PropertyHandler propertyHandler)
    {
        localMapBean = new ScenarioMapBean();

        localMapBean.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // Initialize the map projection, scale, center with user prefs or
        // defaults
        String projName = Environment.get ( Environment.Projection,
                                            Mercator.MercatorName );
        int projType = ProjectionFactory.getProjType(projName);
        localMapBean.setProjectionType(projType);
        localMapBean.setScale(Environment.getFloat(Environment.Scale,
                                              Float.POSITIVE_INFINITY));
        localMapBean.setCenter(new LatLonPoint(
            Environment.getFloat(Environment.Latitude, 0f),
            Environment.getFloat(Environment.Longitude, 0f)
            ));

        MapHandler beanHandler = new MapHandler();

        try {
            beanHandler.add(propertyHandler);
            propertyHandler.createComponents(beanHandler);
            beanHandler.add(localMapBean);
        } catch (MultipleSoloMapComponentException msmce) {
            Debug.error("OpenMapNG: tried to add multiple components of the same type when only one is allowed! - " + msmce);
        }

//        System.out.println ("\nadding map bean with parent of: " + localMapBean.getParent().getParent().getParent().toString() );

        addMapBean (localMapBean.getParent().getParent().getParent(), localMapBean);

    }

    /**
     * Install this user interface in the passed in JInternalFrame.
     * Required for implementation of CougaarUI interface.
     *
     * @param f internal frame to which this user interface should be added
     */

    public void install(JFrame f)
    {
      ScenarioFrame.setVisibleFlag(false);

      f.setContentPane(ScenarioFrame.getOpenMapContentPane());
      f.setJMenuBar((JMenuBar)ScenarioFrame.getOpenMapJMenuBar());
    }

    /**
     * Install this user interface in the passed in JFrame.
     * Required for implementation of CougaarUI interface.
     *
     * @param f frame to which this user interface should be added
     */

    public void install(JInternalFrame f)
    {

        ScenarioFrame.setVisibleFlag(false);
        f.setContentPane(ScenarioFrame.getOpenMapContentPane());

        // remove laf and themes menus from inner frame
        JMenuBar mb = (JMenuBar) ScenarioFrame.getOpenMapJMenuBar();
        mb.remove(5);
        mb.remove(5);
        f.setJMenuBar(mb);

//        System.out.println ("\nroot pane: " + f.getRootPane().toString()  );
        addMapBean (f.getRootPane(), localMapBean);
    }


    /**
     * Returns true if this UI supports pluggable look and feel.  Otherwise,
     * only Metal look and feel support is assumed.
     * Required for implementation of CougaarUI interface.
     *
     * @return true if UI supports pluggable look and feel.
     */
    public boolean supportsPlaf()
    {
        return true;
    }

    public static void addMapBean (Object parentObj, ScenarioMapBean mapBean)
    {
      mapBeans.put( parentObj, mapBean);
    }

    public static ScenarioMapBean getMapBean (Object parentObj)
    {
      ScenarioMapBean smb = (ScenarioMapBean) mapBeans.get (parentObj);
      if (smb == null)
        System.out.println ("no map bean for parent object: " + parentObj.toString() );

      return smb;
    }

    static public void main(String args[]) {

        ArgParser ap = new ArgParser("ScenarioMap");
        String propArgs = null;
        ap.add("properties","A resource, file path or URL to properties file\n Ex: http://myhost.com/xyz.props or file:/myhome/abc.pro\n See Java Documentation for java.net.URL class for more details",1);

        ap.parse(args);

        String[] arg = ap.getArgValues("properties");
        if (arg != null) {
            propArgs = arg[0];
        }

        Debug.init();
        PropertyHandler propertyHandler=null;
        if (propArgs!=null) {
//                    com.bbn.openmap.layer.util.LayerUtils.getResourceOrFileOrURL("com.bbn.openmap.app.OpenMap", propArgs);

            try {
                java.net.URL propURL = com.bbn.openmap.layer.util.LayerUtils.getResourceOrFileOrURL("org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap", propArgs);
                propertyHandler = new PropertyHandler(propURL);
            } catch (java.net.MalformedURLException murle) {
                Debug.error(murle.getMessage());
                murle.printStackTrace();
                propertyHandler = new PropertyHandler();
            } catch(IOException ioe) {
                Debug.error(ioe.getMessage());
                ioe.printStackTrace();
                propertyHandler = new PropertyHandler();
            }
        } else {
            propertyHandler = new PropertyHandler();
        }

        new ScenarioMap(propertyHandler);

    }

}
