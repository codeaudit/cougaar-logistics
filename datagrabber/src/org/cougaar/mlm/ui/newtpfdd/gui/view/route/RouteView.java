/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import java.util.Properties;

import com.bbn.openmap.MapBean;
import com.bbn.openmap.BufferedMapBean;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.gui.OMToolSet;
import com.bbn.openmap.Layer;
import com.bbn.openmap.InformationDelegator;
import com.bbn.openmap.MouseDelegator;
import com.bbn.openmap.MultipleSoloMapComponentException;
import com.bbn.openmap.MapHandler;
import com.bbn.openmap.PropertyHandler;
import com.bbn.openmap.LayerHandler;

import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.event.NavMouseMode;
import com.bbn.openmap.layer.shape.ShapeLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMLine;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TooManyListenersException;

/**
 * Displays route informatation in Openmap
 *
 * @since 4/19/01
 **/
public class RouteView{

  //Constants:
  ////////////

  //Variables:
  ////////////

  private DatabaseConfig dbConfig;
  private RouteData routeData;

  private NewTPFDDShell shell;

  private int runID;

  //Constructors:
  ///////////////

  public RouteView(DatabaseConfig dbConfig,
		   NewTPFDDShell shell,
		   int runID,
		   RouteViewRequest rvr){
    //    super("Route View for: "+rvr.getTitle());
    this.dbConfig=dbConfig;
    this.shell=shell;
    this.runID=runID;
    routeData = rvr.getRouteData(dbConfig,runID);
    setupGUI();
  }

  //Members:
  //////////

  protected void setupGUI(){
    // Create a MapBean
    MapBean mapBean = new BufferedMapBean();
    mapBean.setBorder(new BevelBorder(BevelBorder.LOWERED));

    MapHandler beanHandler = new MapHandler();
    PropertyHandler propertyHandler = new PropertyHandler();
    
    try {
      beanHandler.add(propertyHandler);
      propertyHandler.createComponents(beanHandler);
      beanHandler.add(mapBean);
    } catch (MultipleSoloMapComponentException msmce) {
      System.err.println("RouteView: tried to add multiple components "+
			 "of the same type when only one is allowed! - " + 
			 msmce);
    }

    LayerHandler lh=null;
    InformationDelegator infoDelegator = null;
    NoExitFrame nef=null;

    Iterator iter=beanHandler.iterator();
    while(iter.hasNext()){
      Object o=iter.next();
      if(o instanceof LayerHandler){
	lh=(LayerHandler)o;
      }
      if(o instanceof InformationDelegator){
	infoDelegator = (InformationDelegator) o;
      }
      if(o instanceof NoExitFrame){
	nef=(NoExitFrame)o;
      }
    }
    if(lh==null){
      System.err.println("RouteView - Could not find LayerHandler!\n" + 
			 "Is the openmap.properties file in the scripts directory?");
    }
    if(infoDelegator==null){
      System.err.println("RouteView - Could not find InformationDelegator!\n" + 
			 "Is the openmap.properties file in the scripts directory?");
    }
    if(nef==null){
      System.err.println("RouteView - Could not find NoExitFrame!\n" + 
			 "Is the openmap.properties file in the scripts directory?");
    }

    //Tell the frame about the deligator.
    nef.setInfoDeligator(infoDelegator);

    //Create thee route layer
    RouteLayer rl = new RouteLayer(routeData, infoDelegator);

    rl.setShell(shell);
    rl.setDBConfig(dbConfig);
    rl.setRunID(runID);

    //Add the route layer
    //    mapBean.add(rl);
    lh.addLayer(rl,0);

    //    getContentPane().setLayout(new BorderLayout());

    /*
    
    // Create a ShapeLayer to show world political boundaries.
    // Set the properties of the layer.  This assumes that the
    // datafiles "dcwpo-browse.shp" and "dcwpo-browse.ssx" are in
    // a path specified in the CLASSPATH variable.  These files
    // are distributed with OpenMap and reside in the toplevel
    // "share" subdirectory.
    ShapeLayer shapeLayer = new ShapeLayer();
    Properties shapeLayerProps = new Properties();
    shapeLayerProps.put("political.prettyName", "Political Solid");
    shapeLayerProps.put("political.lineColor", "000000");
    shapeLayerProps.put("political.fillColor", "BDDE83");
    shapeLayerProps.put("political.shapeFile", "dcwpo-browse.shp");
    shapeLayerProps.put("political.spatialIndex", "dcwpo-browse.ssx");
    shapeLayer.setProperties("political", shapeLayerProps);

    // Add the political layer to the map
    mapBean.add(shapeLayer);
    
    // Set the map's center
    mapBean.setCenter(new LatLonPoint(43.0f, -95.0f));
    
    // Set the map's scale 1:120 million
    mapBean.setScale(120000000f);

    */

    /*
    // Create the directional and zoom control tool	
    OMToolSet omts = new OMToolSet();

    // Associate the tool with the map
    omts.setupListeners(mapBean);

    // Create an OpenMap toolbar
    ToolPanel toolBar = new ToolPanel();

    // Add the tool to the toolbar
    toolBar.add(omts);

    // Add the tool bar to the frame
    getContentPane().add(toolBar, BorderLayout.NORTH);

    // Add the map to the frame
    getContentPane().add(mapBean, BorderLayout.CENTER);

    //    rv.setSize(640, 480);
    //    rv.setVisible(true);

    */
  }

  //InnerClasses:
  ///////////////
}
