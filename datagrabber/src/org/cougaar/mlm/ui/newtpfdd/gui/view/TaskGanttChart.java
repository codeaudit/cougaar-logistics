package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.util.Date;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.util.List;

import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;
import org.cougaar.mlm.ui.newtpfdd.util.Debug;
import org.cougaar.mlm.ui.newtpfdd.util.SwingQueue;

import org.cougaar.mlm.ui.newtpfdd.gui.component.TPFDDColor;

import org.cougaar.mlm.ui.newtpfdd.gui.component.GanttChart;
import org.cougaar.mlm.ui.newtpfdd.gui.component.LozengeRow;
import org.cougaar.mlm.ui.newtpfdd.gui.component.Lozenge;
import org.cougaar.mlm.ui.newtpfdd.gui.component.LozengeLabel;
import org.cougaar.mlm.ui.newtpfdd.gui.component.RowLabel;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetAssetDetailRequest;
import org.cougaar.mlm.ui.newtpfdd.gui.view.details.CarrierDetailRequest;

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

public class TaskGanttChart extends GanttChart
{
  private int originLozengeSize;
  private int destinationLozengeSize;

  public static final int DAYLEN = 1000 * 3600 * 24;
  private long last;
  private TaskModel taskModel;
  protected static final int DECORATOR_CIRCLE_ANCHOR_LEFT = 0;
  protected static final int DECORATOR_CIRCLE_ANCHOR_RIGHT = 1;
  protected static final int DECORATOR_TRIANGLE_ANCHOR_LEFT = 2;
  protected static final int DECORATOR_TRIANGLE_ANCHOR_RIGHT = 3;

  private double screenPercent = 0.15;
  private long endLozengeSize = 0;
    
  static boolean debug = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.debug", 
				       "false"));
  static boolean useReadyAtForOrigin = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TaskGanttChart.useReadyAtForOrigin", 
				       "false"));
  // Set up decorator values
  static {
    DEC_RELATIVE_SIZE = 0.2f;
	
    decShapes = new Shape[4];
    decShapes[0] = new Ellipse2D.Float(-5.0f, -5.0f, 10.0f, 10.0f);
    decShapes[1] = new Ellipse2D.Float(-5.0f, -5.0f, 10.0f, 10.0f);
    GeneralPath tri = new GeneralPath();
    tri.moveTo(-5.0f, 10.0f);
    tri.lineTo(5.0f, 10.0f);
    tri.lineTo(0.0f, 0.0f);
    tri.lineTo(-5.0f, 10.0f);
    decShapes[2] = tri;
    decShapes[3] = tri;

    decYRelHeights = new float[4];
    decYRelHeights[0] = 0.5f;
    decYRelHeights[1] = 0.5f;
    decYRelHeights[2] = 0.5f;
    decYRelHeights[3] = 0.5f;

    decAttachLeft = new boolean[4];
    decAttachLeft[0] = true;
    decAttachLeft[1] = false;
    decAttachLeft[2] = true;
    decAttachLeft[3] = false;
  }
    
  public TaskGanttChart(TaskModel taskModel)
  {
    super();
    this.taskModel = taskModel;
    try{
      String size=System.getProperty("originLozengeSize","150");
      originLozengeSize=Integer.parseInt(size);
    }catch(Exception e){
      System.err.println("Could not parse originLozengeSize, using 150");
      originLozengeSize=150;
    }
    try{
      String size=System.getProperty("destinationLozengeSize","150");
      destinationLozengeSize=Integer.parseInt(size);
    }catch(Exception e){
      System.err.println("Could not parse destinationLozengeSize, using 150");
      destinationLozengeSize=150;
    }
  }

  public long getEndLozengeSize() {
    if (endLozengeSize == 0) {
      long span = taskModel.getMaxTaskEnd() - taskModel.getMinTaskStart();
      if(debug)
	System.out.println("SPAN: "+span);
      endLozengeSize = (long)(span * screenPercent);
      if(debug)
	System.out.println("Size: "+endLozengeSize);
    }
    if (endLozengeSize > DAYLEN) endLozengeSize = DAYLEN;
    return endLozengeSize;
  }
    
  public void fitToView()
  {
    Runnable fitToViewRunnable = new Runnable()
      {
	public void run()
	{
	  long span = taskModel.getMaxTaskEnd() - taskModel.getMinTaskStart();
	  setVirtualXSize(span + 2 * getEndLozengeSize());
	  //setVirtualXSize(taskModel.getMaxTaskEnd() - taskModel.getMinTaskStart() + 2 * TaskGanttChart.DAYLEN);
	  setVirtualXLocation(taskModel.getMinTaskStart() - getEndLozengeSize());
	}
      };
    SwingQueue.invokeLater(fitToViewRunnable);
  }

  public void zoomIn() {
    Runnable runnable = new Runnable()
      {
	public void run()
	{
	  long originalSize = getVirtualXSize ();
	  setVirtualXSize(originalSize/2);
	  setVirtualXLocation (getVirtualXLocation ()+originalSize/4);
	}
      };
    SwingQueue.invokeLater(runnable);
  }

  public void zoomOut() {
    Runnable runnable = new Runnable()
      {
	public void run()
	{
	  long originalSize = getVirtualXSize ();
	  setVirtualXSize(originalSize*2);
	  setVirtualXLocation (getVirtualXLocation ()-originalSize/2);
	}
      };
    SwingQueue.invokeLater(runnable);
  }
    
  // ** GanttChart Overrides **
  public void firingComplete()
  {
    fitToView();
    super.firingComplete();
  }

  public Object readItem(int row)
  {
    // Debug.out("TaskGanttChart:rI enter " + row);
    Node assetInstance = (Node)taskModel.getChild(taskModel.getRoot(), row);
    if (debug)
      System.out.println ("TaskGanttChart.readItem - row " + row + " is " + assetInstance);
	
    return assetInstance;
  }

  protected LozengeRow makeLozengeRow(Object o)  {
    Vector legs = new Vector();
    Vector waystations = new Vector ();

    // Create legs
    DBUIDNode assetInstanceNode = (DBUIDNode) o;
    LozengeRow lozrow = new LozengeRow(this, assetInstanceNode);
    Vector taskleaves = new Vector();
    //	Node t = null, oldt = null;
    Node leg = null;
	
    if (debug) 
      System.out.println ("TaskGanttChart.makeLozengeRow - node " + assetInstanceNode + " has " +
			  taskModel.getChildCount(assetInstanceNode) + " children ");

    for ( int i = 0; i < taskModel.getChildCount(assetInstanceNode); i++ ) {
      leg = (Node) taskModel.getChild (assetInstanceNode,i);
      if (debug) 
	System.out.println ("TaskGanttChart.makeLozengeRow - making transport leg " + leg);

      Lozenge loz = createTransportLozenge (leg);
      addLegDecorator (leg, loz);
      setLegLabel     (leg, loz);
      setLegColor     (leg, loz);
	  
      legs.add(loz);
      taskleaves.add(leg);		// keep track of final legs so we can compute way stations
    }
	
    // Render way stations
    if ( !taskleaves.isEmpty() && !(taskModel instanceof AssetModel))
      waystations = makeWayStations (taskleaves);

    for ( Iterator ileg = legs.iterator(); ileg.hasNext(); )
      lozrow.addLozenge((Lozenge)(ileg.next()));
    for ( Iterator iway = waystations.iterator(); iway.hasNext(); )
      lozrow.addLozenge((Lozenge)(iway.next()));
	
    return lozrow;
  }

  protected Lozenge createTransportLozenge (Node leg/*, boolean isFirstLeg*/) {
    Lozenge loz = new Lozenge(this, leg);
    loz.setLeftTipType(Lozenge.SQUARE_TIP);
    loz.setRightTipType(Lozenge.SQUARE_TIP);

    loz.setLozengeVirtualXLocation(getStartValue(leg));
    loz.setLozengeVirtualXSize(getEndValue(leg) - getStartValue(leg));
    String lStr = Node.longDate(getStartValueDate(leg));
    String rStr = Node.longDate(getEndValueDate(leg));

    String location = null;

    if ((((LegNode)leg).getLegType () != DGPSPConstants.LEG_TYPE_TRANSPORTING) &&
	(((LegNode)leg).getLegType () != DGPSPConstants.LEG_TYPE_POSITIONING)) {
      location = leg.getFromName();
      loz.setRelativeHeight(1.0);
    } else {
      location = leg.getFromName() +"->"+ leg.getToName();
      loz.setRelativeHeight(0.3);
    }
	
    if (debug) 
      System.out.println ("createTransportLozenge s " + getStartValue(leg) +
			  " xsize " + (getEndValue(leg) - getStartValue(leg)) + " s " + 
			  lStr + " e " + rStr);

    LegNode legNode = (LegNode) leg;
    String carrier = legNode.getCarrierType () + ": " + legNode.getCarrierName ();

    loz.setLozengeDescription(leg.getDisplayName () + " : " +
			      location + " [" + lStr + " - " + rStr + "]" + " on " +
			      carrier);
    return loz;
  }

  protected void addLegDecorator (Node leg, Lozenge loz) {
    if ( leg.getBestEnd() != null )
      loz.addDecorator(DECORATOR_TRIANGLE_ANCHOR_RIGHT, leg.getBestEnd().getTime());
    if ( leg.getEarlyEnd() != null )
      loz.addDecorator(DECORATOR_CIRCLE_ANCHOR_RIGHT, leg.getEarlyEnd().getTime(), 
		       leg.getActualEnd().getTime() < leg.getEarlyEnd().getTime());
    if ( leg.getLateEnd() != null )
      loz.addDecorator(DECORATOR_CIRCLE_ANCHOR_RIGHT, leg.getLateEnd().getTime(), 
		       leg.getActualEnd().getTime() > leg.getLateEnd().getTime());
    //	if ( leg.getReadyAt() != null )
    //	  loz.addDecorator(DECORATOR_CIRCLE_ANCHOR_LEFT, leg.getReadyAt().getTime(),
    //					   leg.getActualStart().getTime() < leg.getReadyAt().getTime());
  }

  protected void setLegLabel (Node leg, Lozenge loz) {
    LegNode legNode = (LegNode) leg;
    String shortName;
    String longName;

    if (taskModel instanceof AssetModel) {
      if (legNode.getFromCode().equals(legNode.getToCode()))
	shortName = legNode.getFromCode();
      else
	shortName = legNode.getFromCode() + "->" + legNode.getToCode();

      if (legNode.getFromName().equals(legNode.getToName()))
	longName = legNode.getFromName();
      else
	longName = legNode.getFromName() + "->" + legNode.getToName();
    } else {
      shortName = legNode.getCarrierName ();
      longName  = legNode.getCarrierType () + ": " + shortName;
    }

    if (debug)
      System.out.println ("Setting leg label " + longName);
	  
    loz.addLozengeLabel(new LozengeLabel(longName, shortName, LozengeLabel.CENTER));
  }
  
  protected void setLegColor (Node leg, Lozenge loz) {
    switch( leg.getMode() ) {
    case Node.MODE_GROUND:
      loz.setForeground(TPFDDColor.TPFDDDullerYellow);
      break;
    case Node.MODE_SEA:
      loz.setForeground(TPFDDColor.TPFDDGreen);
      // These are the only legs for which we know carrier names right now
      break;
    case Node.MODE_AIR:
      if ((((LegNode)leg).getLegType () == DGPSPConstants.LEG_TYPE_TRANSPORTING) ||
	  (((LegNode)leg).getLegType () == DGPSPConstants.LEG_TYPE_POSITIONING))
	loz.setForeground(TPFDDColor.TPFDDBlue);
      else
	loz.setForeground(Color.orange);

      break;
    case Node.MODE_AGGREGATE:
      loz.setForeground(TPFDDColor.TPFDDPurple);
      break;
    case Node.MODE_UNKNOWN:
      loz.setForeground(Color.gray);
      break;
    default:
      loz.setForeground(Color.red);
    }
  }

  protected Vector makeWayStations (Vector taskleaves) {
    Lozenge loz;
    Vector waystations = new Vector ();
    if (debug) 
      System.out.println ("TaskGanttChart.makeLozengeRow - making way stations.");

    Node lastLeg = (Node) taskleaves.get(taskleaves.size()-1);
	
    // Create origin
    waystations.add (makeOrigin ((Node)taskleaves.get(0)));
    // Create destination
    waystations.add (makeDestination (lastLeg));

    // Create waypoints
    Enumeration e = taskleaves.elements();
    Node prevLeg, nextLeg;
    nextLeg = (Node)(e.nextElement());
    while ( e.hasMoreElements() ) {
      prevLeg = nextLeg;
      nextLeg = (Node)(e.nextElement());

      // Skip ill-formed waypoints
      if ( getStartValue(nextLeg) < getEndValue(prevLeg) ) {
	if (debug)
	  System.out.println ("TaskGanttChart.makeWayStations - WARNING - skipping ill-formed leg, next leg " + 
			      nextLeg + " overlaps prev leg " + prevLeg);
	continue;
      }
				
      if ( prevLeg.getToName().compareTo(nextLeg.getFromName()) == 0 )
	waystations.add (makeContiguous (lastLeg, prevLeg, nextLeg));
      else {
	waystations.add (makeLeftBroken  (lastLeg, prevLeg, nextLeg));
	waystations.add (makeRightBroken (lastLeg, prevLeg, nextLeg));
      }
    }
    return waystations;
  }
  
  protected Lozenge makeOrigin (Node leg) {
    Lozenge loz = new Lozenge(this, leg);
    if(originLozengeSize>=0)
      loz.setFixedWidth(originLozengeSize,Lozenge.FWJ_RIGHT);
    loz.setLeftTipType(Lozenge.POINTED_TIP);
    loz.setRightTipType(Lozenge.SQUARE_TIP);
    if (useReadyAtForOrigin) {
      long readyAt = getReadyAtValue(leg);
      loz.setLozengeVirtualXLocation(readyAt);
      loz.setLozengeVirtualXSize(getStartValue(leg) - readyAt);
      if (debug) {
	System.out.println ("makeOrigin - xloc " + readyAt + " xsize " + (getStartValue(leg) - readyAt));
	if (readyAt == getStartValue(leg)) 
	  System.out.println ("TaskGanttChart.makeOrigin - NOTE - readyAt same as start for " + leg);
      }

      if (loz.getLozengeVirtualXSize () == 0)
	loz.setLozengeVirtualXSize(getEndLozengeSize());
    }
    else {
      loz.setLozengeVirtualXLocation(getStartValue(leg) - getEndLozengeSize());
      loz.setLozengeVirtualXSize(getEndLozengeSize());
      if (debug) 
	System.out.println ("makeOrigin - xloc " + (getStartValue(leg)-getEndLozengeSize()) + " xsize " + getEndLozengeSize());

    }
	
    loz.setRelativeHeight(1.0);
    loz.addLozengeLabel(new LozengeLabel(leg.getFromName(), leg.getFromCode(), LozengeLabel.CENTER));
    String tilStr   = Node.longDate(getStartValueDate(leg));
    String readyStr = Node.longDate(getReadyAtValueDate(leg));

    loz.setLozengeDescription("Origin : " + leg.getFromName() + 
			      (useReadyAtForOrigin ? " ready at " + readyStr : "") + " departs at " + tilStr);

    loz.setForeground(TPFDDColor.TPFDDBurgundy);
    return loz;
  }
	
  protected Lozenge makeDestination (Node leg) {
    Lozenge loz = new Lozenge(this, leg);
    if(destinationLozengeSize>=0)
      loz.setFixedWidth(destinationLozengeSize,Lozenge.FWJ_LEFT);
    loz.setLeftTipType(Lozenge.SQUARE_TIP);
    loz.setRightTipType(Lozenge.POINTED_TIP);
    loz.setLozengeVirtualXLocation(getEndValue(leg));
    loz.setLozengeVirtualXSize(getEndLozengeSize());
    loz.setRelativeHeight(1.0);
    loz.addLozengeLabel(new LozengeLabel(leg.getToName(), leg.getToCode(), LozengeLabel.CENTER));
    String sinceStr = Node.longDate(getEndValueDate(leg));
    loz.setLozengeDescription("Destination : " + leg.getToName() + ", arrives at " + sinceStr);
    loz.setForeground(TPFDDColor.TPFDDBlueGreen);

    return loz;
  }
	
  protected Lozenge makeContiguous (Node leg, Node prevLeg, Node nextLeg) {
    Lozenge loz = new Lozenge(this, leg);
    loz.setLozengeVirtualXLocation(getEndValue(prevLeg));
    loz.setRelativeHeight(1.0);

    loz.setLeftTipType(Lozenge.SQUARE_TIP);
    loz.setRightTipType(Lozenge.SQUARE_TIP);
    // if geolocs match
    loz.setForeground(Color.orange);
    loz.setLozengeVirtualXSize(getStartValue(nextLeg) - getEndValue(prevLeg));
    loz.addLozengeLabel(new LozengeLabel(prevLeg.getToName(), prevLeg.getToCode(),
					 LozengeLabel.CENTER));
    String lStr = Node.longDate(getEndValueDate(prevLeg));
    String rStr = Node.longDate(getStartValueDate(nextLeg));
    //	loz.setLozengeDescription(leg.getDisplayName () + " : " + prevLeg.getToName() + " " + "[" + lStr + "-" + rStr + "]");
    loz.setLozengeDescription("Preparing : " + prevLeg.getToName() + " " + "[" + lStr + "-" + rStr + "]");

    return loz;
  }

  protected Lozenge makeLeftBroken (Node leg, Node prevLeg, Node nextLeg) {
    Lozenge loz = new Lozenge(this, leg);
    loz.setLeftTipType(Lozenge.SQUARE_TIP);
    loz.setRightTipType(Lozenge.BROKEN_TIP);
    long halfWidth = (getStartValue(nextLeg) - getEndValue(prevLeg)) / 2;
    loz.setForeground(Color.red);
    loz.setLozengeVirtualXSize(halfWidth);
    loz.addLozengeLabel(new LozengeLabel(prevLeg.getToName(), prevLeg.getToCode(),
					 LozengeLabel.RIGHT));
    String lStr = Node.longDate(getEndValueDate(prevLeg));
    loz.setLozengeDescription(leg.getDisplayName () + " : " + prevLeg.getToName() + " " + "[" + lStr + "-???]");

    return loz;
  }

  protected Lozenge makeRightBroken (Node leg, Node prevLeg, Node nextLeg) {
    Lozenge loz = new Lozenge(this, leg);

    loz.setForeground(Color.red);
    loz.setLeftTipType(Lozenge.BROKEN_TIP);
    loz.setRightTipType(Lozenge.SQUARE_TIP);
    long halfWidth = (getStartValue(nextLeg) - getEndValue(prevLeg)) / 2;
    loz.setLozengeVirtualXLocation(getStartValue(nextLeg) - halfWidth);
    loz.setLozengeVirtualXSize(halfWidth);
    loz.setRelativeHeight(1.0);
    loz.addLozengeLabel(new LozengeLabel(nextLeg.getFromName(), nextLeg.getFromCode(),
					 LozengeLabel.LEFT));
    String rStr = Node.longDate(getStartValueDate(nextLeg));
    loz.setLozengeDescription(leg.getDisplayName () + " : " + nextLeg.getFromName() + " " + " [???-" + rStr + "]");

    return loz;
  }

  protected LozengeRow makeLabelRow(Object o)
  {
    Node node = (Node)o;
    String unitName = node.getUnitName();
    String prefix = (unitName != null) ? (unitName + ":") : "";
    String s = prefix + node.getDisplayName();

    if (debug) 
      System.out.println ("TaskGanttChart.makeLabelRow - label " + s + " for node " + node);
	
    myLabelPanel.setVirtualXLocation(0);
    myLabelPanel.setVirtualXSize(2 * getEndLozengeSize());
    LozengeLabel lozLabel = new LozengeLabel(s);//, LozengeLabel.LEFT);
    //	lozLabel.setPosition (LozengeLabel.LEFT);
	
    Lozenge loz = new Lozenge(this);
    loz.setForeground(Color.black);
    loz.setLeftTipType(Lozenge.SQUARE_TIP);
    loz.setRightTipType(Lozenge.SQUARE_TIP);
    loz.setLozengeVirtualXLocation(0);
    loz.setLozengeVirtualXSize(2 * getEndLozengeSize());
    loz.setLozengeDescription(node.getLongName());
    loz.addLozengeLabel(lozLabel);
    LozengeRow lozRow = new LozengeRow(this, (DBUIDNode)o);
    lozRow.setVirtualXLocation(0);
    lozRow.setVirtualXSize(2 * getEndLozengeSize());
    lozRow.addLozenge(loz);
    return lozRow;
  }

  private long getStartValue(Node t) {
    return t.getActualStart().getTime();
  }
  private long getReadyAtValue(Node t) {
    return t.getReadyAt().getTime();
  }
  private Date getReadyAtValueDate(Node t) {
    return t.getReadyAt();
  }
  private long getEndValue(Node t) {
    return t.getActualEnd().getTime();
  }
  private Date getStartValueDate(Node t) {
    return t.getActualStart();
  }
  private Date getEndValueDate(Node t) {
    return t.getActualEnd();
  }

  public void doPopup(LozengeRow row) {
    DBUIDNode curNode = row.getCargoInstanceNode();
    String dbuid=taskModel.getDBUID(curNode);
    if(curNode.getType()==UIDGenerator.ASSET_INSTANCE){
      AssetDetailRequest adr = new AssetAssetDetailRequest(dbuid);
      ((PopupDialogSupport) taskModel.getDBState()).showAssetDetailView (adr);
    }else if(curNode.getType()==UIDGenerator.CARRIER_INSTANCE){
      CarrierDetailRequest cdr=new CarrierDetailRequest(dbuid);
      ((PopupDialogSupport) taskModel.getDBState()).showCarrierDetailView(cdr);
    }
  }
}
