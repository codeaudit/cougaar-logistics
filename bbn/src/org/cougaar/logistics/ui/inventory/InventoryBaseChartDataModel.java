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

package org.cougaar.logistics.ui.inventory;

import java.util.ArrayList;

import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.LabelledChartDataModel;
import com.klg.jclass.chart.ChartDataSupport;
import com.klg.jclass.chart.ChartDataEvent;
import com.klg.jclass.chart.ChartDataManageable;
import com.klg.jclass.chart.ChartDataManager;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.TimeUtils;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

/**
 * <pre>
 *
 * The InventoryBaseChartDataModel is the base abstract superclass of all
 * the data models that underly the charts in the gui.
 * Its the bridge between the data and the interface necessary
 * to use the jchart software.
 *
 *
 * @see InventoryLevelChartDataModel
 *
 **/

public abstract class InventoryBaseChartDataModel extends ChartDataSupport
        implements ChartDataManageable,
        ChartDataModel,
        LabelledChartDataModel {

    private final static long MILLIS_IN_DAY = TimeUtils.MSEC_PER_DAY;
    private final static long MILLIS_IN_HOUR = TimeUtils.MSEC_PER_HOUR;

    protected int nSeries;
    protected double xvalues[][];
    protected double yvalues[][];
    protected String[] seriesLabels;
    protected String[] scheduleNames;

    protected int minBucket = -1;
    protected int maxBucket = 0;
    protected int bucketDays = 1;
    protected long bucketSize = MILLIS_IN_DAY;

    protected int nValues = 0;

    protected String legendTitle;

    protected InventoryData inventory;
    protected InventoryPreferenceData prefData;

    protected double unitFactor;

    protected boolean valuesSet;

    protected Logger logger;

    protected boolean useCDay = false;
    protected long baseCDayTime = 0L;
    protected long baseTime = 0L;

    public ChartDataManager getChartDataManager() {
        return (ChartDataManager) this;
    }

    public String getDataSourceName() {
        return legendTitle;
    }


    public void resetInventory(InventoryData newInventory) {
        inventory = newInventory;
        baseCDayTime = inventory.getStartCDay();
        baseTime = (useCDay) ? baseCDayTime : InventoryChartBaseCalendar.getBaseTime();
        unitFactor = (InventoryChart.getConversionTable().getConversionForData(newInventory,prefData)).getFactor();
        valuesSet = false;
        initValues();
        fireChartDataEvent(ChartDataEvent.RELOAD, 0, 0);
    }

    public void setDisplayCDay(boolean doUseCDay) {
        if (doUseCDay != useCDay) {
            useCDay = doUseCDay;
            if (inventory != null) {
                resetInventory(inventory);
            }
        }
    }


    public void setInitialDisplayCDay(boolean doUseCDay) {
	useCDay = doUseCDay;
    }

    /**
     * Retrieves the specified x-value series
     * This returns the nominal getXSeries of the super class
     * @param index data series index
     * @return array of double values representing x-value data
     */
    public double[] getRealXSeries(int index) {
        return getXSeries(index);
    }

    /**
     * Retrieves the specified y-value series
     * The nth asset.
     * This returns the nominal getYSeries of the super class
     * This aids the ProjectionChartDataModel which
     * has a real projected dimension which is added
     * to actual demand to give an artificial
     * bar type chart.
     * @param index data series index
     * @return array of double values representing y-value data
     */
    public synchronized double[] getRealYSeries(int index) {
        return getYSeries(index);
    }

    /**
     * Retrieves the specified x-value series
     * Start and end times of the schedule for each asset
     * @param index data series index
     * @return array of double values representing x-value data
     */
    public double[] getXSeries(int index) {
        initValues();
        if (xvalues == null) {
            logger.error("InventoryChartDataModel ERROR getXSeries no xvalues?");
        }
        return xvalues[index];
    }

    /**
     * Retrieves the specified y-value series
     * The nth asset
     * @param index data series index
     * @return array of double values representing y-value data
     */
    public synchronized double[] getYSeries(int index) {
        initValues();
        if (yvalues == null) {
            logger.error("InventoryChartDataModel ERROR getYSeries no yvalues?");
        }
        return yvalues[index];
    }

    /**
     * Retrieves the number of data series.
     */
    public int getNumSeries() {
        initValues();
        return nSeries;
    }


    public String[] getPointLabels() {
        return null;
    }

    public String[] getSeriesLabels() {
        return seriesLabels;
    }

    public void initValues() {
        if (!valuesSet) {
            setValues();
        }
    }

    public abstract void setValues();

    public int computeBucketFromTime(long time) {
        long graphTime = (time - baseTime);
        int bucket = (int) (graphTime / bucketSize);
        if (graphTime < 0) {
            bucket--;
        }
        return bucket;
    }

    public void computeCriticalNValues() {

        minBucket = 0;
        maxBucket = 0;
        bucketDays = 1;
        nValues = 0;

        if (inventory == null) return;

        InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
                inventory.getSchedules().get(LogisticsInventoryFormatter.INVENTORY_LEVELS_TAG);
        ArrayList levels = schedHeader.getSchedule();

        if (levels.size() == 0) {
            return;
        }

        bucketDays = -1;
        maxBucket = Integer.MIN_VALUE;
        minBucket = Integer.MIN_VALUE;

        for (int i = 0; i < levels.size(); i++) {
            InventoryScheduleElement level = (InventoryScheduleElement) levels.get(i);
            long startTime = level.getStartTime();
            long endTime = level.getEndTime();
            bucketSize = endTime - startTime;
            int startBucket = computeBucketFromTime(startTime);
            int endBucket = computeBucketFromTime(endTime);

            if (bucketDays == -1) {
                if (bucketSize >= MILLIS_IN_DAY) {
                    bucketDays = (int) (bucketSize / MILLIS_IN_DAY);
                } else {
                    bucketDays = (int) (bucketSize / MILLIS_IN_HOUR);
                }
            }
            if (minBucket == Integer.MIN_VALUE) {
                minBucket = startBucket;
            } else if (startBucket < minBucket) {
                minBucket = startBucket;
            }
            maxBucket = Math.max(endBucket, maxBucket);
        }
        if (bucketDays >= 1) {
            nValues = (maxBucket - minBucket + 1) / bucketDays;

            if (logger.isDebugEnabled()) {
                logger.debug("computeNValues:minBucket=" + minBucket + " maxBucket=" + maxBucket + " bucketDays=" + bucketDays + " nValues=" + nValues);
            }
        }
    }

    public void prefDataChanged(InventoryPreferenceData origData,
                                InventoryPreferenceData newData) {
      prefData=newData;
    }
}

