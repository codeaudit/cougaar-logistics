/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;



import java.io.*;
import java.util.*;
import java.awt.Paint;
import com.bbn.openmap.Layer;
import com.bbn.openmap.Environment;
import com.bbn.openmap.event.*;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.proj.Projection;

import java.awt.event.*;
import java.awt.*;
import java.awt.datatransfer.*;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.MapBean;
import com.bbn.openmap.proj.Projection;


import org.cougaar.planning.ldm.plan.*;
// Do not go to the datagrabber for these constants.
// Copied locally to AssetTypeConstants file
//import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.logistics.ui.stoplight.ui.components.RangeSliderPanel;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.InventoryDataProvider;
import org.cougaar.logistics.ui.stoplight.ui.map.ScenarioMapBean;
import org.cougaar.logistics.ui.stoplight.ui.map.app.*;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.CGMVecIcon;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.OMCGM;
import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.OMWideLine;
import org.cougaar.logistics.ui.stoplight.ui.map.util.*;

import javax.swing.*;
import javax.swing.table.*;

public class PspIconLayer extends PspIconLayerBase implements MapMouseListener, DragGestureListener, org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.DropTarget
{
  public static transient final String iStartDate = "pspicon.initialStartDate";
  public static transient final String iEndDate = "pspicon.initialEndDate";
  public static transient final String iCDate = "pspicon.initialCDate";

  public long initialStart = System.currentTimeMillis();
  public long initialEnd = initialStart * 1000L + (30L * 24L * 60L * 60L * 1000L);
  public long initialCDate = initialEnd;

  private Hashtable unitRoutes = new Hashtable (50); // if they display more than 50 at once it'll be confused

  private ScenarioMap scenarioMap = null;

  long time;               
  final static public String EPOCH="Epoch";
  final static public String LAST_TIME="End Time";
  public PspIconLayerModel myState;
  private static final String PSPICONLAYER_SAVEFILE = "PspIconLayer.ser";
  
  private Hashtable relationHash = new Hashtable();

  public void setScenerioMap(ScenarioMap scenarioMap)
  {
    this.scenarioMap = scenarioMap;
  }

  public void addRelation (String id, Vector rels)
  {

    if (id != null)
    {
      if (rels == null)
        rels = new Vector();

      relationHash.put(id, rels);
    }
  }

  public Vector getRelationships (String id)
  {
     if (id != null)
       return (Vector) relationHash.get(id);
     else
       return new Vector();

  }


  OMGraphic findClosest(int x, int y, float limit)
  {
    return myState.findClosest(x,y,limit);
  }

  Iterator markerIterator()
  {
      return myState.markerIterator();
  }

  Unit getUnit(OMGraphic omgr)
  {
    return myState.getUnit(omgr);
  }

  public long getTime() { return time; }

  public void setTime(String ltime)
  {

    if (ltime == null && time == 0)
    {
      // time has never been set
      ltime = Long.toString ( (long) RangeSliderPanel.rangeSlider.getValue() * 1000L );
    }

    if (ltime != null)
    {
//      System.err.println("PspIconLayer Setting time to: "+ltime);

      if (ltime.equalsIgnoreCase(EPOCH))
      {
        time=Long.MIN_VALUE;
      }
      else if (ltime.equalsIgnoreCase(LAST_TIME))
      {
        time=Long.MAX_VALUE;
      }
      else
      {
        Long tmp=Long.decode(ltime);
        time=(tmp==null)?Long.MIN_VALUE:tmp.longValue();
      }
    }

//    System.err.println("PspIconLayer:setTime is: "+time);

    myState.setTime(time);

//    synchronized (graphics) // synchronized against paint()
    {
      createGraphics(graphics);
      addRoutes (graphics);
    }

    repaintLayer();

  }

  private void addRoutes (OMGraphicList grafics)
  {

    Collection coll = unitRoutes.values();

    for ( Iterator itr = coll.iterator(); itr.hasNext(); )
    {
      Vector rGs = (Vector) itr.next();

      for (int ii = 0; ii < rGs.size(); ii ++)
      {
        grafics.add((OMGraphic) rGs.get(ii));
      }

    }

  }

  private Vector dropTargets = new Vector(1);
  private Vector flavors = new Vector(1);

  /**
   * Construct the layer.
   */
  public PspIconLayer ()
  {
    super();
    initMyState();
    createGraphics(graphics);

    dropTargets.add(this);

    flavors.add(ObjectTransferable.getDataFlavor(InventoryDataProvider.class));

    (new DragAndDropSupport()).addDropTarget(this);
  }

  // ----------------- DropTarget Support

  public Vector getTargetComponents()
  {
    return(dropTargets); 
  }

  public boolean dropToSubComponents()
  {
    return(true);
  }

  public boolean readyForDrop(Component componentAt, Point dragPoint, DataFlavor flavor)
  {
    boolean ready = false;

    if (areaSelected && selG.contains(scenarioMap.getMapBean(getRootPane()).getProjection(), dragPoint))
    {
      ready = true;
    }
    else
    {
      OMGraphic graphic = findClosest(dragPoint.x, dragPoint.y, 5.0f);
      if (graphic != null)
      {
        ready = true;
      }
    }

    return(ready);
  }

  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean dropable)
  {
    // Do nothing here
  }

  public void dropData(Component componentAt, Point dragPoint, DataFlavor flavor, Object droppedData)
  {
    // Find the closest units
    Vector unitNames = getSelectedUnitNamesAt(dragPoint);

    new MultiUnitAssetQuery(unitNames, (InventoryDataProvider)droppedData);
  }

  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    return(flavors);
  }

  private Hashtable unitDataSetList = new Hashtable(1);

  class MultiUnitAssetQuery implements Runnable
  {
    private Vector unitNames = null;
    private InventoryDataProvider dataProvider = null;

    public MultiUnitAssetQuery(Vector unitNames, InventoryDataProvider dataProvider)
    {
      this.unitNames = unitNames;
      this.dataProvider = dataProvider;

      Thread thread = new Thread(this);
      thread.setPriority(((thread.getPriority()-2) < Thread.MIN_PRIORITY) ? Thread.MIN_PRIORITY : (thread.getPriority()-2));
      thread.start();
    }
    
    public void run()
    {
      for (int i=0, isize=unitNames.size(); i<isize; i++)
      {
        String unitName = (String)unitNames.elementAt(i);
        Hashtable dataSets = dataProvider.getInventoryData(unitName, null);
        String assetName = dataProvider.getDefaultAssetName();

        AssetBarGraphic assetG = (AssetBarGraphic)unitDataSetList.get(unitName);
        if (assetG == null)
        {
          assetG = new AssetBarGraphic(scenarioMap, dataSets, assetName);
          unitDataSetList.put(unitName, assetG);
        }
        else
        {
          assetG.addDataSets(dataSets, assetName);
        }

        myState.setAssetBarGraphic(unitName, assetG);
      }

      setMaxValue();

      repaintLayer();
      repaint();
    }
  }

  private void setMaxValue()
  {
    Vector dataSetList = null;
    Hashtable dataSets = null;
    DataSet dataSet = null;
    double max = 0.0;
    for (Enumeration e = unitDataSetList.elements(); e.hasMoreElements();)
    {
      dataSetList = ((AssetBarGraphic)e.nextElement()).getDataSets();
      for (int i=0, isize=dataSetList.size(); i<isize; i++)
      {
        dataSets = (Hashtable)dataSetList.elementAt(i);
        for (Enumeration e2 = dataSets.elements(); e2.hasMoreElements();)
        {
          dataSet = (DataSet)e2.nextElement();
          max = (dataSet.getYmax() > max) ? dataSet.getYmax() : max;
        }
      }
    }    

    // now set the max value in all the AssetBarGraphics
    for (Enumeration e = unitDataSetList.elements(); e.hasMoreElements();)
    {
      AssetBarGraphic abg = (AssetBarGraphic)e.nextElement();
      abg.max = max;
    }

    // AssetBarGraphic.max = max;
  }


  void initMyState()
  {

    myState = new PspIconLayerModel (this);

    initialStart = dateToMillis(Environment.get(iStartDate), initialStart);
    initialEnd = dateToMillis(Environment.get(iEndDate), initialEnd);

    initialCDate = dateToMillis(Environment.get(iCDate), initialCDate);

  }

	public static void save(String saveFile, Serializable object) throws IOException
	{

    if (saveFile == null || saveFile.length() == 0)
      saveFile = new String (PSPICONLAYER_SAVEFILE);


  	FileOutputStream fos = new FileOutputStream(saveFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(object);
		oos.close();
		fos.close();

	}


	public static Object restore (FileInputStream fis) throws IOException
	{
	  Object object = null;

	  try
	  {
  		ObjectInputStream ois = new ObjectInputStream(fis);
      object = ois.readObject();
      fis.close();
      ois.close();
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
    }

		return(object);
	}

  public static long dateToMillis(String dateString, long defaultTime)
  {
    long time = defaultTime;
    
    try
    {
      int month = 0;
      int day = 0;
      int year = 0;
      
      StringTokenizer tokenizer = new StringTokenizer(dateString.trim(), "/", false);
      
      month = Integer.parseInt(tokenizer.nextToken()) -1;
      day = Integer.parseInt(tokenizer.nextToken());
      year = Integer.parseInt(tokenizer.nextToken());
      
      GregorianCalendar cal = new GregorianCalendar();
      cal.set(year, month, day);
      
      time = cal.getTime().getTime();
    }
    catch (Exception e)
    {
      System.err.println("Unable to convert date to millis: " + e);
    }
    
    return(time);
  }


    Vector tempVect = new Vector(300);
    private static final Comparator graphicOrderComparator = new Comparator()
      {
        VecIcon icon1 = null;
        VecIcon icon2 = null;

        public int compare(Object o1, Object o2)
        {
          icon1 = (VecIcon)o1;
          icon2 = (VecIcon)o2;

          if (icon1.locationNumber < icon2.locationNumber)
            return(-1);

          if (icon1.locationNumber > icon2.locationNumber)
            return(1);

          return(0);
        }
      };
  

  protected SelectionGraphic selG = new SelectionGraphic(Color.yellow);

    protected void createGraphics (OMGraphicList list)
    {

      tempVect.clear();
      for (Iterator it=myState.markerIterator(); it.hasNext(); )
      {
        tempVect.add(it.next());
      }

      Collections.sort(tempVect, graphicOrderComparator);

      list.clear();

      if (list.getTraverseMode() == list.FIRST_ADDED_ON_TOP)
    	{
        list.add(selG);

        for (int i=tempVect.size()-1; i>-1; i--)
        {
          list.add((OMGraphic)tempVect.elementAt(i));
        }
    	}

    	else
    	{
        for (int i=0, isize=tempVect.size(); i<isize; i++)
        {
          list.add((OMGraphic)tempVect.elementAt(i));
        }

        list.add(selG);
    	}

    }


  public boolean mouseClicked (MouseEvent e)
  {

    Point findPoint = e.getPoint();
    OMGraphic omg = findClosest (findPoint.x, findPoint.y, 10.0f);

    int lineType = OMGraphic.LINETYPE_STRAIGHT;
    Color lineColor = Color.white;

    if (omg != null)
    {
      Unit closestUnit = getUnit (omg);
      System.out.println ("closest unit to mouse click is: " + closestUnit.getLabel() );

      String unitId = closestUnit.getLabel();

      if (hasRouteDisplayed (unitId) )
      {
         // if this unit has a route displayed then we want to get rid of it
         unitRoutes.remove(unitId);

         setTime (null); // redisplay without the new route
         repaint();
         
         return true;
      }

      //
      // unit is not currently displayed, display it
      //
      
      // look up his route based on his name
      //      RoutingTable rt = RouteJdbcConnector.findRoutes(unitId, DGPSPConstants.CONV_TYPE_SHIP );
      RoutingTable rt = RouteJdbcConnector.findRoutes(unitId, AssetTypeConstants.ASSET_TYPE_SHIP );
      if (rt == null)
      {
        System.out.println (" no sea legs found for unit: " + unitId);
      }
      else
      {
        System.out.println (" found " + rt.elats.size() + " segment sea-based leg for " + rt.getOrg() );

        // construct every start and end as a OMWideLine
        // the rest are end points
        for (int ii = 0, pp = 2; ii < rt.elats.size(); ii ++)
        {
//        System.out.println (" adding line:  ii " + ii + ": \n" +
//                            "\t" + (float) ( (Double) rt.slats.get(ii)).doubleValue() +
//                            "\t" + (float) ( (Double) rt.slons.get(ii)).doubleValue() +
//                            "\n\t" + (float) ( (Double) rt.elats.get(ii)).doubleValue() +
//                            "\t" +      (float) ( (Double) rt.elons.get(ii)).doubleValue() );

          switch ( ( (Integer) rt.types.get(ii)).intValue())
          {

            case AssetTypeConstants.ASSET_TYPE_PLANE:
              lineType = OMGraphic.LINETYPE_GREATCIRCLE;
              lineColor = Color.yellow;
            break;

            case AssetTypeConstants.ASSET_TYPE_SHIP:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.blue;
            break;

            case AssetTypeConstants.ASSET_TYPE_TRUCK:
            case AssetTypeConstants.ASSET_TYPE_SELF_PROPELLABLE:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = new Color (0.5f, 0.25f, 0.0f);
            break;

            case AssetTypeConstants.ASSET_TYPE_TRAIN:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.black;
            break;

            default:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.white;
            break;

          }

          OMWideLine oml = new OMWideLine (
                                  (float) ( (Double) rt.slats.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.slons.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.elats.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.elons.get(ii)).doubleValue(),
                                  lineType );

          oml.setLinePaint(lineColor);
//          oml.setLineColor (lineColor); deprecated with OpenMap 4.2
          oml.setWidth(2.0f);

          oml.addArrowHead(true);

          graphics.add (oml);

          addRouteGraphic (unitId, oml);

        }

      }

      //
      // plane routes
      //
      rt = RouteJdbcConnector.findRoutes(unitId, AssetTypeConstants.ASSET_TYPE_PLANE );
      if (rt == null)
      {
        System.out.println (" no air legs found for unit: " + unitId);
      }
      else
      {
        System.out.println (" found " + rt.elats.size() + " segment air-based leg for " + rt.getOrg() );

        // construct every start and end as a OMWideLine
        // the rest are end points
        for (int ii = 0, pp = 2; ii < rt.elats.size(); ii ++)
        {
//        System.out.println (" adding line:  ii " + ii + ": \n" +
//                            "\t" + (float) ( (Double) rt.slats.get(ii)).doubleValue() +
//                            "\t" + (float) ( (Double) rt.slons.get(ii)).doubleValue() +
//                            "\n\t" + (float) ( (Double) rt.elats.get(ii)).doubleValue() +
//                            "\t" +      (float) ( (Double) rt.elons.get(ii)).doubleValue() );
          switch ( ( (Integer) rt.types.get(ii)).intValue())
          {

            case AssetTypeConstants.ASSET_TYPE_PLANE:
              lineType = OMGraphic.LINETYPE_GREATCIRCLE;
              lineColor = Color.yellow;
            break;

            case AssetTypeConstants.ASSET_TYPE_SHIP:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.blue;
            break;

            case AssetTypeConstants.ASSET_TYPE_TRUCK:
            case AssetTypeConstants.ASSET_TYPE_SELF_PROPELLABLE:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = new Color (0.5f, 0.25f, 0.0f);

            case AssetTypeConstants.ASSET_TYPE_TRAIN:
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.black;
            break;

            default:
            //  System.out.println ("unknown type: " + lineType);
              lineType = OMGraphic.LINETYPE_STRAIGHT;
              lineColor = Color.white;
            break;

          }

          OMWideLine oml = new OMWideLine (
                                  (float) ( (Double) rt.slats.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.slons.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.elats.get(ii)).doubleValue(),
                                  (float) ( (Double) rt.elons.get(ii)).doubleValue(),
                                  lineType );

          oml.setLinePaint(lineColor);
          oml.setWidth(2.0f);
          oml.addArrowHead(true);

          graphics.add (oml);

          addRouteGraphic (unitId, oml);

        }
      }

      repaintLayer();

      repaint();

      return true;

    }

    return (false);
  }


  protected Point point1, point2;

  public boolean mousePressed(MouseEvent e)
  {
    if (e.getSource() instanceof MapBean)
    {

      System.out.println ("PspIconLayer:mousePressed - getting mapbean from parent: " + getParent().toString() );
      if (areaSelected && !selG.contains(scenarioMap.getMapBean(getRootPane()).getProjection(), e.getPoint()))
      {
        hideSelectedArea();
      }

      if (!areaSelected)
      {
        // set the new first point
        point1 = e.getPoint();
        // ensure the second point isn't set.
        point2 = null;
      }

      return(true);
    }
    
    return(false);
  }

  public boolean mouseDragged(MouseEvent e)
  {
    if (e.getSource() instanceof MapBean)
    {
      if (areaSelected || dragging || findClosest(point1.x, point1.y, 5.0f) != null)
      {
        return(false);
      }

  		int dx = Math.abs(e.getPoint().x - point1.x);
  		int dy = Math.abs(e.getPoint().y - point1.y);
  
  		// Don't bother drawing if the rectangle is too small
  		if ((dx < 5) && (dy < 5))
  		{
  		  if (point2 != null)
  		  {
          paintRectangle((MapBean)e.getSource(), point1, point2);
          point2 = null;
  		  }

  		  return(false);
  		}


      // clean up the old rectangle, since point2 has the old value.
      paintRectangle((MapBean)e.getSource(), point1, point2);	
      // paint new rectangle
      point2 = e.getPoint();
      paintRectangle((MapBean)e.getSource(), point1, point2);

      return(true);
    }

    return(false);
  }

  public boolean mouseReleased(MouseEvent e)
  {
    if ((point1 != null && point2 != null) && e.getSource() instanceof MapBean)
    {
      if (areaSelected || dragging)
      {
        return(false);
      }

      MapBean map = (MapBean)e.getSource();

      paintRectangle(map, point1, point2);


      Rectangle selectedArea = new Rectangle();
      selectedArea.x = (point1.x < point2.x) ? point1.x : point2.x;
      selectedArea.y = (point1.y < point2.y) ? point1.y : point2.y;
      selectedArea.width = Math.abs(point1.x - point2.x);
      selectedArea.height = Math.abs(point1.y - point2.y);

      LatLonPoint p1 = map.getProjection().inverse(selectedArea.x, selectedArea.y);
      LatLonPoint p2 = map.getProjection().inverse(selectedArea.x + selectedArea.width, selectedArea.y + selectedArea.height);
      showSelectedArea(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());

      return(true);
    }

    return(false);
  }

  public void showSelectionDialog(MapBean map)
  {
    Vector namedLocationTimeList = getSelectedUnits(map, true);
    IconDialog iconDialog = IconDialog.getDialog(this);
    iconDialog.setTableData(namedLocationTimeList);
    iconDialog.setTitle("Selected Area Unit Visibility");
    iconDialog.setVisible(true);
  }

  public void showSelectedArea(float lat1, float lon1, float lat2, float lon2)
  {
    selG.setSelection(lat1, lon1, lat2, lon2);
    selG.setVisible(true);
    selG.setNeedToRegenerate(true);
    repaintLayer();
    repaint();

    areaSelected = true;
  }

  public void hideSelectedArea()
  {
    selG.setVisible(false);
    selG.setNeedToRegenerate(true);
    repaintLayer();
    repaint();

    areaSelected = false;
  }

  private boolean areaSelected = false;

  private Rectangle selectedArea = new Rectangle();

  private Vector getSelectedUnits(MapBean map, boolean showAll)
  {
    Vector namedLocationTimeList = new Vector(0);
    
    if (areaSelected)
    {
      Projection projection = map.getProjection();
      selectedArea = selG.getGraphicBounds(projection, selectedArea);
      NamedLocationTime nltm = null;
      for (Iterator it=myState.nls.iterator(); it.hasNext(); )
      {
        nltm = (NamedLocationTime)it.next();
        if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
        {
          if ((nltm.getUnit().getGraphic().isVisible() || showAll) && selectedArea.contains(projection.forward(nltm.getLatitude(), nltm.getLongitude())))
          {
            namedLocationTimeList.add(nltm);
          }
        }
      }
    }
    
    return(namedLocationTimeList);
  }

  private boolean dragging = false;

  public void dragGestureRecognized(DragGestureEvent e)
  {
		if (e.getDragAction() == DnDConstants.ACTION_MOVE)
		{
      // Find the closest units
      Vector data = getSelectedUnitNamesAt(e.getDragOrigin());

      if (data != null)
      {
    		try
    		{
    			// initial cursor, transferrable, dsource listener
    			e.startDrag(java.awt.dnd.DragSource.DefaultCopyNoDrop, new ObjectTransferable(data), new DSListener());
          dragging = true;
    		}
    		catch(Exception ex)
    		{
    			System.err.println(ex);
    			ex.printStackTrace();
    		}
      }
    }
  }

  private Vector getSelectedUnitNamesAt(Point dragPoint)
  {
    Vector data = null;

    ScenarioMapBean mapBean = scenarioMap.getMapBean (getRootPane());
    
    if (areaSelected && selG.contains(mapBean.getProjection(), dragPoint))
    {
		  data = getSelectedUnits(mapBean, false);
		  for (int i=0, isize=data.size(); i<isize; i++)
		  {
		    VecIcon icon = (VecIcon)((NamedLocationTime)data.elementAt(i)).getUnit().getGraphic();
		    data.setElementAt(icon.getLabel(), i);
		  }
    }
    else
    {
      OMGraphic graphic = findClosest(dragPoint.x, dragPoint.y, 5.0f);
      if (graphic != null)
      {
  		  data = new Vector(0);
  		  data.add(((VecIcon)graphic).getLabel());
      }
    }
    
    return(data);
  }

	// DSListener a listener that will track the state of the DnD operation
	class DSListener implements DragSourceListener
	{
		public void dragEnter(DragSourceDragEvent e)
		{
			DragSourceContext context = e.getDragSourceContext();
	
			//intersection of the users selected action, and the source and target actions
			int myaction = e.getDropAction();
	
			if (myaction == DnDConstants.ACTION_MOVE)
			{
				context.setCursor(java.awt.dnd.DragSource.DefaultCopyDrop);	  
			}
			else
			{
				context.setCursor(java.awt.dnd.DragSource.DefaultCopyNoDrop);	  	
			}
		}
	
		public void dragDropEnd(DragSourceDropEvent e)
		{
      dragging = false;
		}
	
		public void dragOver(DragSourceDragEvent e)
		{
		}
	
		public void dragExit(DragSourceEvent e)
		{
		}
	
		public void dropActionChanged(DragSourceDragEvent e)
		{
		}
	}

  public void changeIconScale (float sc)
  {

    // doesn't matter which we are setting, it's a static
    OMCGM.changePercentScale (sc);

    // change all the visible ones
    for (int ii = 0; ii < graphics.size(); ii ++)
    {
      OMGraphic omg = graphics.getOMGraphicAt(ii);
      if (omg instanceof CGMVecIcon)
      {
        ( (CGMVecIcon) omg).updateScale();

      }
    }

    repaint();

  }


  public void showIconDialog()
  {
    if (myState == null)
    {
      return;
    }

    IconDialog iconDialog = IconDialog.getDialog(this);
    Vector namedLocationTimeList = new Vector(0);
    NamedLocationTime nltm = null;
   Hashtable temp = new Hashtable(300);

   synchronized (myState.schedImplByUnit)  // don't want anybody updating this while we sift through them
   {
     for (Enumeration enum=myState.schedImplByUnit.keys(); enum.hasMoreElements();)
     {
       ScheduleImpl simpl = (ScheduleImpl) myState.schedImplByUnit.get((String) enum.nextElement());
       NamedLocationTime anyNltm = (NamedLocationTime) simpl.get(0);
//       namedLocationTimeList.add(anyNltm);
        temp.put(anyNltm.getUnit().getLabel(), anyNltm);
     }

    for (Iterator it=myState.nls.iterator(); it.hasNext(); )
    {
      nltm = (NamedLocationTime)it.next();
      if (nltm!=null && nltm.getUnit()!=null && nltm.getUnit().getGraphic()!=null)
      {
        temp.put(nltm.getUnit().getLabel(), nltm);
      }
    }

    for (Enumeration enum=temp.elements(); enum.hasMoreElements();)
    {
      nltm = (NamedLocationTime)enum.nextElement();
      namedLocationTimeList.add(nltm);
     }
   }

    iconDialog.setTableData(namedLocationTimeList);
    iconDialog.setTitle("All Unit Visibility");
    iconDialog.setVisible(true);
  }

  protected void paintRectangle(MapBean map, Point pt1, Point pt2)
  {
    Graphics g = map.getGraphics();
    g.setXORMode(java.awt.Color.lightGray);
    g.setColor(java.awt.Color.darkGray);
    
    if (pt1 != null && pt2 != null)
    {
      g.drawRect(pt1.x < pt2.x ? pt1.x : pt2.x, pt1.y < pt2.y ? pt1.y : pt2.y, Math.abs(pt2.x - pt1.x), Math.abs(pt2.y - pt1.y));
//      g.drawRect(pt1.x < pt2.x ? pt1.x + (pt2.x - pt1.x)/2 - 1 : pt2.x + (pt1.x - pt2.x)/2 - 1, pt1.y < pt2.y ? pt1.y + (pt2.y - pt1.y)/2 - 1 : pt2.y + (pt1.y - pt2.y)/2 - 1, 2, 2);
    }
  }


  public void addRouteGraphic (String unitId, OMGraphic oml)
  {

     boolean newRoute = false;
     Vector completeRoute = (Vector) unitRoutes.get (unitId);

     if ( completeRoute == null )
     {
       newRoute = true;
       completeRoute = new Vector(15);
     }

     completeRoute.add(oml);

     if (newRoute)
       unitRoutes.put(unitId, completeRoute);

  }

  public boolean hasRouteDisplayed (String unitId)
  {
    if (unitRoutes.get(unitId) == null)
      return false;
    else
      return true;
  }

  public void clearAllRouteGraphics()
  {

    unitRoutes.clear();

    setTime (null);

    repaint();
    
  }

}
