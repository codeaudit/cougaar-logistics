/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
package org.cougaar.logistics.ui.inventory;

import java.awt.Color;
import java.util.Hashtable;


public class InventoryColorTable {
  Hashtable colorTable;

  public InventoryColorTable() {
    colorTable = new Hashtable();
    
    twoPut(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL,
	   Color.magenta);

    twoPut(InventoryLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL,
	   Color.black);

    twoPut(InventoryLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL,
	   new Color(255, 255, 204)); // light yellow

    twoPut(InventoryLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + "_SYMBOL",
	   Color.blue);

    twoPut(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL,
	   new Color(255,0,0));  //red?

    twoPut(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL,
	   new Color(255,200,200));  //light red?

    twoPut(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL,
	   new Color(0,0,255));  //blue

    twoPut(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL,
	   new Color(170,170,255));  //light blue?

  }

  private void twoPut(String key, Color value) {
    colorTable.put(key, value);
  }

  public Color get(String s) {
      //if (colorTable.get(s) == null)
      //return Color.white;
    return (Color) colorTable.get(s);
  }

}

