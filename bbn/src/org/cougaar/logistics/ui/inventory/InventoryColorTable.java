/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.inventory;

import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

import java.awt.Color;
import java.util.Hashtable;

import com.klg.jclass.chart.JCFillStyle;
import com.klg.jclass.chart.JCLineStyle;
import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.JCSymbolStyle;


public class InventoryColorTable {
  private static final String BACKGROUND = "Background";
  private static final String FILL_PATTERN = "Fill Pattern";
  private static final String LINE_WIDTH = "Line Width";
  private static final String OUTLINE = "Outline";
  private static final String SYMBOL = "Symbol";
  private static final String SYMBOL_COLOR = "Symbol Color";
  private static final String SYMBOL_SIZE = "Symbol Size";

  private Hashtable colorTables; // maps color schemes (Strings) to color tables (hashtables)

  // this specifies the order in which the labels appear in the gui
  private static final String[] labels =
      {InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL,
       TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL,
       TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL,
       TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL,
       OrgActivityChartDataModel.OFFENSIVE_SERIES_LABEL,
       OrgActivityChartDataModel.DEFENSIVE_SERIES_LABEL,
       RequisitionsChartDataModel.REQUISITION_SERIES_LABEL,
       RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL,
       ProjectionsChartDataModel.PROJECTION_SERIES_LABEL,
       ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL,
       ShortfallChartDataModel.SHORTFALL_SERIES_LABEL
       //TODO: Remove all references to BelowZeroHighlightChartDataModel
       //       BelowZeroHighlightChartDataModel.BELOW_ZERO_SERIES_LABEL
      };

  // TODO: this should create all of the color schemes that we define
  // then the get color method could either take a color scheme argument
  // or we could define a global color scheme
  public InventoryColorTable() {
    colorTables = new Hashtable();

    colorTables.put(InventoryPreferenceData.COLOR_SCHEME_NORMAL, initNormalColorTable());
//    colorTables.put(InventoryPreferenceData.COLOR_SCHEME_RG_COLORBLIND, initRGColorBlindColorTable());
    colorTables.put(InventoryPreferenceData.COLOR_SCHEME_BW, initBWColorTable());
  }

  private Hashtable initNormalColorTable() {
    Hashtable colorTable = new Hashtable();
    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL,
                   new Color(51, 153, 0));
    colorTable.put(TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL,
                   Color.black);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL,
                   Color.yellow);

    colorTable.put(InventoryLevelChartDataModel.class.getName() + OUTLINE,
                   new Color(51, 102, 0));
    colorTable.put(InventoryShortfallChartDataModel.class.getName() + OUTLINE,
                   new Color(153, 0, 0));
    colorTable.put(OrgActivityChartDataModel.class.getName() + OUTLINE,
                   Color.gray);


    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(2));
    colorTable.put(TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(2));
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(2));


    colorTable.put(OrgActivityChartDataModel.ORG_ACTIVITY_SERIES_LABEL,
                   Color.white);    // not used
    colorTable.put(OrgActivityChartDataModel.OFFENSIVE_SERIES_LABEL,
                   Color.white);
    colorTable.put(OrgActivityChartDataModel.DEFENSIVE_SERIES_LABEL,
                   Color.lightGray);
    colorTable.put(ShortfallChartDataModel.SHORTFALL_SERIES_LABEL,
                   new Color(255, 0, 0));  //red?
    colorTable.put(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL,
                   new Color(0, 230, 0));  //green?
    colorTable.put(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL,
                   new Color(200, 255, 200));  //light green?
    colorTable.put(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL,
                   new Color(0, 0, 255));  //blue
    colorTable.put(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL,
                   new Color(170, 170, 255));  //light blue?
    /** TODO remove BelowZeroHighlightChartDataModel refs
     colorTable.put(BelowZeroHighlightChartDataModel.BELOW_ZERO_SERIES_LABEL,
     new Color(255, 255, 240)); // light yellow
     */

    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL,
                   new Integer(JCSymbolStyle.VERT_LINE));
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_COLOR,
                   Color.blue);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_SIZE,
                   new Integer(4));

    return colorTable;
  }

  // for debugging, use arbitrary different colors, so we can distinguish which color table is in use
  private Hashtable initRGColorBlindColorTable() {
    Hashtable colorTable = new Hashtable();
    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL,
                   Color.orange);
    colorTable.put(TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL,
                   Color.green);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL,
                   Color.cyan);

    colorTable.put(OrgActivityChartDataModel.class.getName() + OUTLINE,
                   Color.gray);


    colorTable.put(OrgActivityChartDataModel.ORG_ACTIVITY_SERIES_LABEL,
                   Color.white);    // not used
    colorTable.put(OrgActivityChartDataModel.OFFENSIVE_SERIES_LABEL,
                   Color.blue);
    colorTable.put(OrgActivityChartDataModel.DEFENSIVE_SERIES_LABEL,
                   Color.green);
    colorTable.put(ShortfallChartDataModel.SHORTFALL_SERIES_LABEL,
                   Color.orange);  //red?
    colorTable.put(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL,
                   Color.green);  //green?
    colorTable.put(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL,
                   Color.red);  //light green?
    colorTable.put(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL,
                   Color.cyan);  //blue
    colorTable.put(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL,
                   Color.orange);  //light blue?
    /** TODO remove BelowZeroHighlightChartDataModel refs
     colorTable.put(BelowZeroHighlightChartDataModel.BELOW_ZERO_SERIES_LABEL,
     Color.green); // light yellow
     */

    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL,
                   new Integer(JCSymbolStyle.DOT));
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_COLOR,
                   Color.magenta);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_SIZE,
                   new Integer(4));

    return colorTable;
  }

  private Hashtable initBWColorTable() {
    Hashtable colorTable = new Hashtable();

    // these are represented as lines
    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL,
                   Color.black);
    colorTable.put(TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL,
                   Color.black);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL,
                   Color.black);
    colorTable.put(ShortfallChartDataModel.SHORTFALL_SERIES_LABEL,
                   Color.black);  //red

    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(4));
    colorTable.put(TargetReorderLevelChartDataModel.REORDER_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(2));
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + LINE_WIDTH,
                   new Integer(1));

    colorTable.put(OrgActivityChartDataModel.class.getName() + OUTLINE,
                   Color.gray);


    colorTable.put(OrgActivityChartDataModel.ORG_ACTIVITY_SERIES_LABEL,
                   Color.white);    // not used
    colorTable.put(OrgActivityChartDataModel.OFFENSIVE_SERIES_LABEL,
                   Color.white);
    colorTable.put(OrgActivityChartDataModel.DEFENSIVE_SERIES_LABEL,
                   Color.lightGray);

    /** TODO remove BelowZeroHighlightChartDataModel refs
     colorTable.put(BelowZeroHighlightChartDataModel.BELOW_ZERO_SERIES_LABEL,
     new Color(255, 255, 240)); // light yellow
     */

    // these are represented as bar charts
    colorTable.put(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL,
                   Color.black);
    colorTable.put(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL,
                   Color.black);
    colorTable.put(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL,
                   Color.black);
    colorTable.put(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL,
                   Color.black);

    colorTable.put(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL + BACKGROUND,
                   Color.white);
    colorTable.put(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL + BACKGROUND,
                   Color.white);
    colorTable.put(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL + BACKGROUND,
                   Color.white);
    colorTable.put(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL + BACKGROUND,
                   Color.white);

    colorTable.put(RequisitionsChartDataModel.REQUISITION_SERIES_LABEL + FILL_PATTERN,
                   new Integer(JCFillStyle.STRIPE_45));
    colorTable.put(RequisitionsChartDataModel.REQUISITION_ALLOCATION_SERIES_LABEL + FILL_PATTERN,
                   new Integer(JCFillStyle.STRIPE_135));
    colorTable.put(ProjectionsChartDataModel.PROJECTION_SERIES_LABEL + FILL_PATTERN,
                   new Integer(JCFillStyle.PER_75));
    colorTable.put(ProjectionsChartDataModel.PROJECTION_ALLOCATION_SERIES_LABEL + FILL_PATTERN,
                   new Integer(JCFillStyle.PER_25));
    colorTable.put(InventoryLevelChartDataModel.INVENTORY_LEVEL_SERIES_LABEL + FILL_PATTERN,
                   new Integer(JCFillStyle.PER_25));

    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL,
                   new Integer(JCSymbolStyle.DOT));
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_COLOR,
                   Color.black);
    colorTable.put(TargetReorderLevelChartDataModel.TARGET_LEVEL_SERIES_LABEL + SYMBOL_SIZE,
                   new Integer(5));


    return colorTable;
  }

  /**
   * Get the color to use in a chart.
   * @param colorScheme the color scheme in use; see InventoryPreferenceData for definitions
   * @param s the series or symbol name; one of the labels above
   * @return color to use
   */
  public Color get(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    return (Color) colorTable.get(s);
  }

  /**
   * Get the color to use in a chart.
   * @param colorScheme the color scheme in use; see InventoryPreferenceData for definitions
   * @param view the chart data view ; the data model class name is used to diff on outline
   * @return color to use
   */
  public Color getOutlineColor(String colorScheme, ChartDataView view) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Color outlineColor = (Color) colorTable.get(view.getDataSource().getClass().getName() + OUTLINE);
    if (outlineColor == null) {
      return Color.black;
    }
    return outlineColor;
  }

  /**
   * Get the background color to use, if the fill pattern is not solid.
   * @param colorScheme the color scheme in use; see InventoryPreferenceData for definitions
   * @param s the series or symbol name; one of the labels above
   * @return color to use
   */
  public Color getBackgroundColor(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    return (Color) colorTable.get(s + BACKGROUND);
  }

  /**
   * Get the fill pattern to use.
   * @param colorScheme the color scheme in use; see InventoryPreferenceData for definitions
   * @param s the series or symbol name; one of the labels above
   * @return color to use
   */
  public int getFillPattern(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Integer fillPattern = (Integer) colorTable.get(s + FILL_PATTERN);
    if (fillPattern == null)
      return JCFillStyle.SOLID; // default
    return fillPattern.intValue();
  }

  public int getLineWidth(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Integer lineWidth = (Integer) colorTable.get(s + LINE_WIDTH);
    if (lineWidth == null)
      return 1; // default
    return lineWidth.intValue();
  }

  public int getSymbolShape(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Integer symbolStyle = (Integer) colorTable.get(s + SYMBOL);
    if (symbolStyle == null) {
      return JCSymbolStyle.NONE;
    }
    return symbolStyle.intValue();
  }

  public int getSymbolSize(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Integer symbolSize = (Integer) colorTable.get(s + SYMBOL_SIZE);
    if (symbolSize == null) {
      return 4;
    }
    return symbolSize.intValue();
  }

  public Color getSymbolColor(String colorScheme, String s) {
    Hashtable colorTable = (Hashtable) colorTables.get(colorScheme);
    Color symColor = (Color) colorTable.get(s + SYMBOL_COLOR);
    if (symColor == null) {
      return symColor.black;
    }
    return symColor;
  }

  public String[] getColorLabels() {
    return labels;
  }

}

