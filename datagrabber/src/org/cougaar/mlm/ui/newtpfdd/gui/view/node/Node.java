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
package org.cougaar.mlm.ui.newtpfdd.gui.view.node;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.text.SimpleDateFormat;

//import org.w3c.dom.Element;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;

public class Node implements Serializable, Cloneable {

    // Tree Hierarchy Information
    private transient List children = new ArrayList(); // of UID
    private long parentUID; // parents name
    private long nodeUID;   // self-identifier

    // Real Data
  private String nomen;
  private String unitName;    // = pretty_name
    private String unitNameUID;  
    private String from;
    private String to;
    private String fromCode;
    private String toCode;

  // consider making these longs
    private Date readyAt;     // preference
    private Date earlyEnd;    // preference
    private Date bestEnd;     // preference
    private Date lateEnd;     // preference
    private Date actualStart; // allocated time 
    private Date actualEnd;   // allocated time 

    // Display Information
    private int mode = MODE_UNKNOWN;
    private String displayName;
    private String longName;

  private boolean nodeQueried = false;
  
    // Static finals for Mode Field
    public static final int MODE_SEA = 1;
    public static final int MODE_AIR = 2;
    public static final int MODE_GROUND = 3;
    public static final int MODE_SELF = 4;
    public static final int MODE_ITINERARY = 7;
    public static final int MODE_AGGREGATE = 8;
    public static final int MODE_UNKNOWN = 9;

    // Constructors
    public Node(long nodeUID) {
	  this.nodeUID = nodeUID;
    }
    public Node() {
    }

    // G/SETTERS FOR TREE STUFF
    public long getUID() { return nodeUID; }
    public void setUID(long uid) { nodeUID = uid; }

    public long getParentUID() { return parentUID; }
    public void setParentUID(long parentUID) { this.parentUID = parentUID; }

  public void addChild(long childUID) {
	children.add(new Long(childUID));
  }
    public void removeChild(long childUID) {
	  children.remove(new Long (childUID));
    }
  public void clearChildren () {
	children.clear();
  }
  public int getChildCount() {
	  return children.size();
    }
    public long getChildUID(int index) {
	  if ( index < 0 || index >= children.size() ) {
	    System.err.println("Node.getChild using bad index: "+index);
	    return 0;
	  }
	  return ((Long)children.get(index)).longValue();
    }
    public int indexOf(long childUID) {
	  return children.indexOf(new Long (childUID));
    }
    public boolean hasChildren() {
	  return !children.isEmpty();
    }
    public List getChildren()
    {
	  return children;
    }

    // G/SETTERS FOR REAL DATA
  
    public String getNomen() { return nomen; }; 
    public void setNomen(String nomen) { this.nomen = nomen; }

    public String getUnitName() { return unitName; }; 
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getUnitNameUID() { return unitNameUID; }; 
    public void setUnitNameUID(String unitNameUID) { this.unitNameUID = unitNameUID; }

    public String getFrom() { return from; }; 
    public String getFromName() { return from; }; 
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }; 
    public String getToName() { return to; }; 
    public void setTo(String to) { this.to = to; }

    public String getFromCode() { return fromCode; }; 
    public void setFromCode(String fromCode) { this.fromCode = fromCode; }

    public String getToCode() { return toCode; }; 
    public void setToCode(String toCode) { this.toCode = toCode; }

    public Date getReadyAt() { return readyAt; }; 
    public void setReadyAt(Date readyAt) { this.readyAt = readyAt; }
    
    public Date getEarlyEnd() { return earlyEnd; }; 
    public void setEarlyEnd(Date earlyEnd) { this.earlyEnd = earlyEnd; }
    
    public Date getBestEnd() { return bestEnd; }; 
    public void setBestEnd(Date bestEnd) { this.bestEnd = bestEnd; }
    
    public Date getLateEnd() { return lateEnd; }; 
    public void setLateEnd(Date lateEnd) { this.lateEnd = lateEnd; }
    
    public Date getActualStart() { return actualStart; }; 
    public void setActualStart(Date actualStart) { this.actualStart = actualStart; }
    
    public Date getActualEnd() { return actualEnd; }; 
    public void setActualEnd(Date actualEnd) { this.actualEnd = actualEnd; }

    // G/SETTERS FOR DISPLAY STUFF
    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }
    public void setModeFromConveyType(int convType) { 
	  switch (convType) {
	  case DGPSPConstants.CONV_TYPE_UNKNOWN:
		mode = MODE_UNKNOWN;
		break;
	  case DGPSPConstants.CONV_TYPE_TRUCK:
	  case DGPSPConstants.CONV_TYPE_TRAIN:
	  case DGPSPConstants.CONV_TYPE_SELF_PROPELLABLE:
		mode = MODE_GROUND;
		break;
	  case DGPSPConstants.CONV_TYPE_PLANE:
		mode = MODE_AIR;
		break;
	  case DGPSPConstants.CONV_TYPE_SHIP:
	  case DGPSPConstants.CONV_TYPE_DECK:
		mode = MODE_SEA;
		break;
	  case DGPSPConstants.CONV_TYPE_PERSON:
		mode = MODE_AIR;
		break;
	  case DGPSPConstants.CONV_TYPE_FACILITY:
		mode = MODE_GROUND;
		break;
	  default:
		mode = MODE_UNKNOWN;
		break;
	  }
	}

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLongName() { return longName; }
    public void setLongName(String longName) { this.longName = longName; }

    public String toString() {
	  return displayName + " : " + nodeUID; 
    }

    // DATE OUTPUT STUFF
    private static SimpleDateFormat shortFormat = new SimpleDateFormat("M/d");
    public static String shortDate(Date date) { return shortFormat.format(date); }  
    private static SimpleDateFormat longFormat = new SimpleDateFormat("M/d/yy HH:mm");
    public static String longDate(Date date) { return longFormat.format(date); }

    // MISCELLANEOUS UTILITIES
  public boolean isTransport() { return false; }

    public boolean isStructural() {
	return !isTransport();
    }
    public boolean isRoot() {
	return nodeUID == TPFDDConstants.ROOTID;
    }

  public boolean isLeaf () {
	return !hasChildren();
  }

  public boolean wasQueried () {
	return nodeQueried; 
  }
  public void setWasQueried (boolean val) {
	nodeQueried = val; 
  }
  
  UIDGenerator generator = UIDGenerator.getGenerator();
  
  /*
  public boolean equals (Object other) {
	if (this == other)
	  return true;
	String otherDBUID = generator.getDBUID (((Node) other).getUID());
	String thisDBUID  = generator.getDBUID (getUID());
	return thisDBUID.equals (otherDBUID);
  }
  */
  
    // POTENTIAL ISSUES
    // 1) Tasks may need an update system so that when they change they can
    //    alert someone so they can be redrawn or whatever. This existed as
    //    proxyChangeNotify in a previous incarnation.

    // The following is some crappy stuff leftover from Node kept for compatibility
    // Try to not use this stuff and if possible get rid of it
    public static final int ROLLUP = 1;
    public static final int ROLLUP_FORCED_ROOT = 2;
    public static final int EQUIPMENT = 3;
    public static final int BY_CARRIER_TYPE = 4;
    public static final int BY_CARRIER_NAME = 5;
    public static final int BY_CARGO_TYPE = 6;
    public static final int BY_CARGO_NAME = 7;
    public static final int CARRIER_TYPE = 8;
    public static final int CARRIER_NAME = 9;
    public static final int CARGO_TYPE = 10;
    public static final int CARGO_NAME = 11;
    public static final int LAST_STRUCTURE_CODE = 11;

    public static final int ITINERARY = 20;
    public static final int ITINERARY_LEG = 21;
    public static final int TASK = 22;
}

