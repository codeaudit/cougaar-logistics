/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;
import org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell;
import org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterClauses;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;

import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

import com.bbn.openmap.MapBean;
import com.bbn.openmap.BufferedMapBean;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.layer.shape.ShapeLayer;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.gui.OMToolSet;
import com.bbn.openmap.Layer;
import com.bbn.openmap.InformationDelegator;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMRect;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.event.InfoDisplayEvent;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.event.NavMouseMode;
import com.bbn.openmap.event.ProjectionEvent;
import com.bbn.openmap.event.SelectMouseMode;
import com.bbn.openmap.layer.location.Location;
import com.bbn.openmap.layer.location.BasicLocation;

/**
 * Displays a route layer in openmap
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 4/19/01
 **/
public class RouteLayer extends Layer implements MapMouseListener {
  private static final int PIXEL_DISTANCE=Integer.getInteger("PIXEL_DISTANCE",10).intValue();

  protected NewTPFDDShell shell;
  protected DatabaseConfig dbConfig;
  protected int runID;

  private RouteData routeData;
  InformationDelegator infoDelegator;  

  /**
   *  A list of graphics to be painted on the map.
   */
  private OMGraphicList locations;
  private OMGraphicList segments;
  private OMGraphicList omgraphics;
  
  /**
   * Construct a default route layer.  Initializes omgraphics to
   * a new OMGraphicList, and invokes createGraphics to create
   * the canned list of routes.
   */
  public RouteLayer(RouteData rd, InformationDelegator infoDelegator) {
    omgraphics = new OMGraphicList();
    locations = new OMGraphicList();
    segments = new OMGraphicList();
	omgraphics.addOMGraphic (locations);
	omgraphics.addOMGraphic (segments);
	
    routeData=rd;
    createGraphics(omgraphics);
    setName(rd.getName());

	this.infoDelegator = infoDelegator;
  }

  public void setShell(NewTPFDDShell shell){
    this.shell=shell;
  }

  public void setDBConfig(DatabaseConfig dbConfig){
    this.dbConfig=dbConfig;
  }

  public void setRunID(int runID){
    this.runID=runID;
  }
  
  /**
   * Creates an OMLine from the given parameters.
   *
   * @param lat1 The line's starting latitude
   * @param lon1 The line's starting longitude
   * @param lat2 The line's ending latitude
   * @param lon2 The line's ending longitude
   * @param color The line's color
   *
   * @return An OMLine with the given properties
   */
  /*
  public OMLine createLine(float lat1, float lon1,
			   float lat2, float lon2,
			   Color color) {
    OMLine line = new OMLine(lat1, lon1, lat2, lon2,
			     OMGraphic.LINETYPE_GREATCIRCLE);
    line.setStroke(thinStroke);
    line.setLinePaint(color);
    return line;
  }
  */

  public Location createLocation(float lat, float lon,
				   String name){
    OMRect rect=new OMRect(lat,lon,-2,-2,2,2);
    rect.setFillPaint(Color.black);
    rect.setLinePaint(Color.black);
    rect.setVisible(true);
    Location l=new BasicLocation(lat,lon,name,rect);
    l.setShowName(true);
    l.setShowLocation(true);
    l.setVisible(true);
    return l;
  }
    
  /**
   * Clears and then fills the given OMGraphicList.  Creates
   * three lines for display on the map.
   *
   * @param graphics The OMGraphicList to clear and populate
   * @return the graphics list, after being cleared and filled
   */
  public OMGraphicList createGraphics(OMGraphicList graphics) {
    
    locations.clear();
    segments.clear();

    Iterator iter;
    iter=routeData.getLocationsIterator();
    while(iter.hasNext()){
      RouteLoc rl=(RouteLoc)iter.next();
      locations.addOMGraphic(createLocation(rl.getLat(),
					   rl.getLon(),
					   rl.getPrettyName()));
    }
    iter=routeData.getSegementsIterator();
    while(iter.hasNext()){
      RouteSegment rs=(RouteSegment)iter.next();
	  /*
      graphics.addOMGraphic(createLine(rs.getSLat(),
				       rs.getSLon(),
				       rs.getELat(),
				       rs.getELon(),
				       getModeColor(rs.getMode())));
	  */
	  segments.addOMGraphic(rs);
    }
    return graphics;
  }

  //----------------------------------------------------------------------
  // Layer overrides
  //----------------------------------------------------------------------
  
  /**
   * Renders the graphics list.  It is important to make this
   * routine as fast as possible since it is called frequently
   * by Swing, and the User Interface blocks while painting is
   * done.
   */
  public void paint(java.awt.Graphics g) {
    omgraphics.render(g);
  }
  
  //----------------------------------------------------------------------
  // ProjectionListener interface implementation
  //----------------------------------------------------------------------
  
  /**
   * Handler for <code>ProjectionEvent</code>s.  This function is
   * invoked when the <code>MapBean</code> projection changes.  The
   * graphics are reprojected and then the Layer is repainted.
   * <p>
   * @param e the projection event
   */
  public void projectionChanged(ProjectionEvent e) {
    omgraphics.project(e.getProjection(), true);
    repaint();
  }

  public MapMouseListener getMapMouseListener() {
	    return this;
  }

  public String[] getMouseModeServiceList() {
	    return new String[] {
	        NavMouseMode.modeID
	    };
  }
   
    // Mouse Listener events
    ////////////////////////

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e MouseEvent
     * @return true if the listener was able to process the event.
     */
    public boolean mousePressed(MouseEvent e) {
      return maybeShowPopup(e);
    }
 
    /**
     * Invoked when a mouse button has been released on a component.
     * @param e MouseEvent
     * @return true if the listener was able to process the event.
     */
  public boolean mouseReleased(MouseEvent e) {	  
    return maybeShowPopup(e);
  }

  protected boolean maybeShowPopup(MouseEvent e){
    if(e.isPopupTrigger()){
      OMGraphic line = segments.findClosest(e.getX(), e.getY(), PIXEL_DISTANCE);
      if (line != null) {
	if(line instanceof CarrierInstanceSegment){
	  CarrierInstanceSegment cis=(CarrierInstanceSegment)line;
	  Map legdescs=cis.getLegDescriptionMap();
	  List legOrder=cis.getLegOrder();
	  JPopupMenu m=createPopup(legdescs, legOrder);
	  m.show(e.getComponent(),e.getX(),e.getY());
	}
      }
      return true;
    }
    return false;
  }
  
  protected JPopupMenu createPopup(Map legDescs, List legOrder){
    JPopupMenu m = new JPopupMenu();
    for(int i=0;i<legOrder.size();i++){
      final String legID=(String)legOrder.get(i);
      String desc=(String)legDescs.get(legID);
      JMenuItem mi=new JMenuItem(desc);
      mi.addActionListener(new ActionListener(){
	  public void actionPerformed(ActionEvent e){
	    FilterClauses filterClauses = new FilterClauses();
	    addAllAssetsOnLeg(filterClauses,legID);
	    if(shell!=null)
	      shell.showTPFDDView(filterClauses, true, false);
	  }
	});
      m.add(mi);
    }
    return m;
  }

  protected void addAllAssetsOnLeg(FilterClauses fc, String legID){
    try{
      Statement s = dbConfig.getConnection().createStatement();
      ResultSet rs= s.executeQuery(getAssetsForLegSQL(runID,legID));
      while(rs.next()){
	String assetID=rs.getString(1);
	fc.addCargoInstance(assetID);
      }
    }catch(SQLException e){
      System.err.println(e);
    }
  }

  protected String getAssetsForLegSQL(int runID, String legID){
    String sql=
      "select "+DGPSPConstants.COL_ASSETID+
      " from "+
      Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID)+
      " where "+
      DGPSPConstants.COL_LEGID+"="+"'"+legID+"'";
    return sql;
  }

    /**
     * Invoked when the mouse has been clicked on a component.
     * The listener will receive this event if it successfully
     * processed <code>mousePressed()</code>, or if no other listener
     * processes the event.  If the listener successfully processes
     * <code>mouseClicked()</code>, then it will receive the next
     * <code>mouseClicked()</code> notifications that have a click
     * count greater than one.
     * <p>
     * NOTE: We have noticed that this method can sometimes be
     * erroneously invoked.  It seems to occur when a light-weight AWT
     * component (like an internal window or menu) closes (removes
     * itself from the window hierarchy).  A specific OpenMap example
     * is when you make a menu selection when the MenuItem you select
     * is above the MapBean canvas.  After making the selection, the
     * mouseClicked() gets invoked on the MouseDelegator, which passes
     * it to the appropriate listeners depending on the MouseMode.
     * The best way to avoid this problem is to not implement anything
     * crucial in this method.  Use a combination of
     * <code>mousePressed()</code> and <code>mouseReleased()</code>
     * instead.
     * @param e MouseEvent
     * @return true if the listener was able to process the event.
     */
    public boolean mouseClicked(MouseEvent e) {	  return false;	}

    /**
     * Invoked when the mouse enters a component.
     * @param e MouseEvent
     */
    public void mouseEntered(MouseEvent e) {}
 
    /**
     * Invoked when the mouse exits a component.
     * @param e MouseEvent
     */
    public void mouseExited(MouseEvent e) {}

    // Mouse Motion Listener events
    ///////////////////////////////

    /**
     * Invoked when a mouse button is pressed on a component and then 
     * dragged.  The listener will receive these events if it
     * successfully processes mousePressed(), or if no other listener
     * processes the event.
     * @param e MouseEvent
     * @return true if the listener was able to process the event.
     */
    public boolean mouseDragged(MouseEvent e) {	  return false;
    }

    /**
     * Invoked when the mouse button has been moved on a component
     * (with no buttons down).
     * @param e MouseEvent
     * @return true if the listener was able to process the event.
     */
    public boolean mouseMoved(MouseEvent e) {
      OMGraphic line = segments.findClosest(e.getX(), e.getY(), PIXEL_DISTANCE);
      if (line != null) {
	//		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "RouteLayer.mousePressed - found " + line);
	// setup the info event
	if (line instanceof RouteSegment) {
	  String infoLine = ((RouteSegment)line).getInfo();
	  InfoDisplayEvent info = new InfoDisplayEvent(this, infoLine);
	  // ask the infoDelegator to display the info
	  infoDelegator.requestInfoLine(info);
	}
      }
      return false;
    }
  
    /**
     * Handle a mouse cursor moving without the button being pressed.
     * This event is intended to tell the listener that there was a
     * mouse movement, but that the event was consumed by another
     * layer.  This will allow a mouse listener to clean up actions
     * that might have happened because of another motion event
     * response.
     */
    public void mouseMoved() {}
}
