/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.inventory.data;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.ui.inventory.InventoryColorTable;

import java.io.Writer;

import java.util.*;


/**
 * <pre>
 *
 * The InventoryPreferenceData holds all the information necessary to
 *
 **/

public class InventoryPreferenceData implements Cloneable {
  public static final String LITERS_LABEL = "Liters";
  public static final String CASES_LABEL = LogisticsInventoryFormatter.WATER_UNIT;
  public static final String BOTTLES_LABEL = "Bottles";
  public static final String TONS_LABEL = LogisticsInventoryFormatter.AMMUNITION_UNIT;
  public static final String ROUNDS_LABEL = "Rounds";

  public static final String[] units = {CASES_LABEL,
                                        LITERS_LABEL,
                                        BOTTLES_LABEL,
                                        TONS_LABEL,
                                        ROUNDS_LABEL};


  public static final String PREF_DATA_HEADER_XML = "INVENTORY_PREFERENCE_DATA";
  public static final String CDAY_XML = "STARTUP_IN_CDAY_MODE";
  public static final String SHORTFALL_XML = "DISPLAY_SHORTFALL_WHEN_DETECTED";
  public static final String SHOW_INVENTORY_XML = "SHOW_INVENTORY_CHART";
  public static final String SHOW_REFILL_XML = "SHOW_REFILL_CHART";
  public static final String SHOW_DEMAND_XML = "SHOW_DEMAND_CHART";
  public static final String ROUND_XML = "ROUND_TO_WHOLES";
  public static final String AMMO_UNIT_XML = "AMMO_UNIT";
  public static final String WATER_UNIT_XML = "WATER_UNIT";
  public static final String COLOR_SCHEME_XML = "COLOR_SCHEME";


  public boolean startupWCDay;
  public boolean displayShortfall;

  public boolean showInventoryChart;
  public boolean showRefillChart;
  public boolean showDemandChart;

  public boolean roundToWholes;
  public int ammoUnit;
  public int waterUnit;

  public static final String COLOR_SCHEME_NORMAL = "Normal";
//  public static final String COLOR_SCHEME_RG_COLORBLIND = "Red Green Color Blind"; // red-green color blind
  public static final String COLOR_SCHEME_BW = "Black and White"; // black and white

  public static final String[] colorSchemes = {
    COLOR_SCHEME_NORMAL,
//    COLOR_SCHEME_RG_COLORBLIND,
    COLOR_SCHEME_BW
  };

  public String colorScheme;

  private InventoryColorTable colorTable;

  public InventoryPreferenceData() {
    colorTable = new InventoryColorTable();
    setDefaultValues();
  }

  public void setDefaultValues() {
    startupWCDay = true;
    displayShortfall = true;
    showInventoryChart = true;
    showRefillChart = true;
    showDemandChart = true;
    roundToWholes = false;
    ammoUnit = getUnit(TONS_LABEL);
    waterUnit = getUnit(CASES_LABEL);
    colorScheme = COLOR_SCHEME_NORMAL;
  }

  public String getAmmoUnitLabel() {
    return getUnitLabel(ammoUnit);
  }

  public String getWaterUnitLabel() {
    return getUnitLabel(waterUnit);
  }

  public String getPreferredUnit(InventoryData data) {
    String dataUnit = data.getUnit();
    String item = data.getItem();
    if (dataUnit.equals(LogisticsInventoryFormatter.AMMUNITION_UNIT)) {
      return getAmmoUnitLabel();
    } else if (item.equals(LogisticsInventoryFormatter.WATER_ITEM_ID)) {
      return getWaterUnitLabel();
    }
    return dataUnit;
  }

  public Object clone() {
    Object copy = null;
    try {
      copy = super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Yow! InventoryPreferenceData is not cloneable", e);
    }
    return copy;
  }


  public static int getUnit(String unitString) {
    for (int i = 0; i < units.length; i++) {
      if (units[i].equals(unitString)) {
        return i;
      }
    }
    throw new RuntimeException("Unknown Unit Type String:" + unitString);
  }

  public static String getUnitLabel(int unit) {
    if ((unit < 0) ||
        (unit >= units.length)) {
      throw new RuntimeException("Unknown unit type:" + unit);
    }
    return units[unit];
  }

  public String getColorScheme() {
    return colorScheme;
  }

  public InventoryColorTable getColorTable() {
    return colorTable;
  }

  public String toXMLString() {
    String xmlString = "<" + PREF_DATA_HEADER_XML + ">\n";
    xmlString = xmlString + "<" + CDAY_XML + ">" + startupWCDay + "</" + CDAY_XML + ">\n";
    xmlString = xmlString + "<" + SHORTFALL_XML + ">" + displayShortfall + "</" + SHORTFALL_XML + ">\n";
    xmlString = xmlString + "<" + SHOW_INVENTORY_XML + ">" + showInventoryChart + "</" + SHOW_INVENTORY_XML + ">\n";
    xmlString = xmlString + "<" + SHOW_REFILL_XML + ">" + showRefillChart + "</" + SHOW_REFILL_XML + ">\n";
    xmlString = xmlString + "<" + SHOW_DEMAND_XML + ">" + showDemandChart + "</" + SHOW_DEMAND_XML + ">\n";
    xmlString = xmlString + "<" + ROUND_XML + ">" + roundToWholes + "</" + ROUND_XML + ">\n";
    xmlString = xmlString + "<" + AMMO_UNIT_XML + ">" + getUnitLabel(ammoUnit) + "</" + AMMO_UNIT_XML + ">\n";
    xmlString = xmlString + "<" + WATER_UNIT_XML + ">" + getUnitLabel(waterUnit) + "</" + WATER_UNIT_XML + ">\n";
    xmlString = xmlString + "<" + COLOR_SCHEME_XML + ">" + colorScheme + "</" + COLOR_SCHEME_XML + ">\n";
    xmlString = xmlString + "<" + PREF_DATA_HEADER_XML + ">\n";
    return xmlString;
  }

  public void parseValuesFromXMLString(String xmlString) {
    String[] lines = xmlString.split("\\n");
    for (int i = 0; i < lines.length; i++) {
      String currLine = lines[i];
      String tag = getTagName(currLine);
      if (!(tag.equals(PREF_DATA_HEADER_XML))) {
        String[] tagValueTag = currLine.split(">");
        String[] valueTag = tagValueTag[1].split("<");
        String value = valueTag[0];
        setTagValue(tag, value);
      }
    }
  }

  public void setTagValue(String tag, String value) {
    if (tag.equals(COLOR_SCHEME_XML)) {
      colorScheme = value;
    } else if (tag.equals(AMMO_UNIT_XML)) {
      ammoUnit = getUnit(value);
    } else if (tag.equals(WATER_UNIT_XML)) {
      waterUnit = getUnit(value);
    } else if (tag.equals(CDAY_XML)) {
      startupWCDay = parseBoolean(value);
    } else if (tag.equals(SHORTFALL_XML)) {
      displayShortfall = parseBoolean(value);
    } else if (tag.equals(SHOW_INVENTORY_XML)) {
      showInventoryChart = parseBoolean(value);
    } else if (tag.equals(SHOW_REFILL_XML)) {
      showRefillChart = parseBoolean(value);
    } else if (tag.equals(SHOW_DEMAND_XML)) {
      showDemandChart = parseBoolean(value);
    } else if (tag.equals(ROUND_XML)) {
      roundToWholes = parseBoolean(value);
    } else {
      throw new RuntimeException("Unknown tag:" + tag + " with value:" + value);
    }

  }


  private static String stripTag(String line) {
    int start = 0;
    String tag = line.split(">")[0];
    int end = tag.length();
    if (tag.startsWith("</")) {
      start = 2;
    } else if (tag.startsWith("<")) {
      start = 1;
    }
    if (tag.endsWith(">")) {
      end--;
    }
    return tag.substring(start, end);
  }

  private static String getTagName(String line) {
    String tag = stripTag(line);
    String words[] = tag.split("\\s");
    return words[0];
  }

  private static boolean parseBoolean(String aBoolStr) {
    if (aBoolStr.trim().toLowerCase().equals("true")) {
      return true;
    } else {
      return false;
    }
  }
}


