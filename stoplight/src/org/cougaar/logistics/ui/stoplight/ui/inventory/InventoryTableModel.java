/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;
import java.util.*;

import javax.swing.table.AbstractTableModel;

import org.cougaar.mlm.ui.data.UISimpleNamedSchedule;
import org.cougaar.mlm.ui.data.UISimpleInventory;
import org.cougaar.mlm.ui.data.UIQuantityScheduleElement;

import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;

/***********************************************************************************************************************
<b>Description</b>: InventoryTableModel for Inventory Management

<br><br><b>Notes</b>:<br> 

Usage:
 *     JTable table = new JTable(new InventoryTableModel(UISimpleInventory)));
 *     JScrollPane scrollpane = new JScrollPane(table);
									

***********************************************************************************************************************/
public class InventoryTableModel extends AbstractTableModel
{
  
  
  /*********************
  ** Public Variables
  **********************/
  
  /********************************************************************************************
  <b>Description</b>: long - Set the data time scale to seconds.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public static final long timeScale = 1000;

  /********************************************************************************************
  <b>Description</b>: double Set the bar data width to be 4 hours (data time scale is in seconds, so 4*60*60 is 4 hours).

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public static final double barWidth = 4.0*60.0*60.0;
  /********************************************************************************************
  <b>Description</b>: UISimpleInventory - the inventory data that we are working with.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public UISimpleInventory inventory;
  /********************************************************************************************
  <b>Description</b>: int  - number of rows in the table.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public int rowCount;
  /********************************************************************************************
  <b>Description</b>: Vector - holds vectors of "rows" which contain ScheduleTableEntry objects
                               which are quantity, start time, end time.
  
  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public Vector columns;
  /********************************************************************************************
  <b>Description</b>: Hashtable - keyed by inventory schedule name, contains Dataset elements.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public Hashtable dataSets = new Hashtable(1);
  /********************************************************************************************
  <b>Description</b>: DateFormat -  date format.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public DateFormat dateTimeFormater = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
  /********************************************************************************************
  <b>Description</b>: NumberFormat -  characteristics of display value.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public NumberFormat qtyFormater = NumberFormat.getInstance();
  /********************************************************************************************
  <b>Description</b>: RowObjectComparer - used to help sort arrays.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public static final RowObjectComparer rowObjectComparer = new RowObjectComparer();
  /********************************************************************************************
  <b>Description</b>: Vector the actual column names.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public Vector columnNames = null;
  /********************************************************************************************
  <b>Description</b>: String[]  final column names after massaging for display

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public String[] finalColumnNames;
  /********************************************************************************************
  <b>Description</b>: int number of rows coming in.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public int originalRows;
  /********************************************************************************************
  <b>Description</b>: int number of columns.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public int colCount;
  /********************************************************************************************
  <b>Description</b>: int - row max.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public int maxRows;
  /********************************************************************************************
  <b>Description</b>: int the number of rows calculated from incoming and calculated data.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public int allRows;
  /********************************************************************************************
  <b>Description</b>: Object[][] - the table data matrix.

  <br><br><b>Notes</b>:<br>
                    - 
  *********************************************************************************************/
  public Object[][] data;
  // ---------------------------------------------------------------------------------------------------------------------
  // Public Constructors
  // ---------------------------------------------------------------------------------------------------------------------
  /*********************************************************************************************************************
  <b>Description</b>: main constructor

  <br><b>Notes</b>:<br>
	                  -takes the passed in UISimpleInventory object and creates the table info
  <br>
  
  @param inventory UISimpleInventory the data object.
  
  

	*********************************************************************************************************************/
  public InventoryTableModel(UISimpleInventory inventory)
  {
   if(inventory != null)
   {
    //System.out.println("new inventory object   #1");
    this.inventory = inventory;
    dateTimeFormater.setCalendar(new InventoryChartBaseCalendar());
    qtyFormater.setMaximumIntegerDigits(10);
    qtyFormater.setMinimumIntegerDigits(2);
    qtyFormater.setMinimumFractionDigits(2);
    qtyFormater.setMaximumFractionDigits(2);
    qtyFormater.setGroupingUsed(false);
    columns = new Vector();
    Vector v = inventory.getSchedules();
    int index = ((v.size() + 1) );
    columnNames = new Vector(index);
    columnNames.add(inventory.getAssetName());
    //System.out.println("array size = " + index);
    int col = 0;
    allRows = 0;
    for (int i = 0; i < v.size(); i++)
    {
      UISimpleNamedSchedule namedSchedule =
        (UISimpleNamedSchedule)v.elementAt(i);
      //System.out.println("named sched " + namedSchedule.getName() + " " + col  + " " );
      String newLabel = InventoryScheduleNames.getName(namedSchedule.getName());

      if(newLabel != null)
      {
          columnNames.add(newLabel);
          //System.out.println("named sched " + namedSchedule.getName() + " " + col  + " " + newLabel);
          Vector s = namedSchedule.getSchedule();
          Vector rows = new Vector();
          for (int j = 0; j < s.size(); j++)
          {
            UIQuantityScheduleElement schedule = (UIQuantityScheduleElement)s.elementAt(j);
            rows.addElement(new ScheduleTableEntry(schedule.getQuantity(),
                     schedule.getStartTime(),
                     schedule.getEndTime()));
            //System.out.println("starttime = " + schedule.getStartTime()/timeScale + " is " + shortDate(schedule.getStartTime()));
            //System.out.println("rows " + schedule.getQuantity() + " " + schedule.getStartTime());
            allRows++;
            if(j > maxRows)
              maxRows = j;
          }
          //System.out.println("rows for above " + s.size());
          col += 1;
          columns.add(rows);
      }
    }

    //  build calculated columns
    String shortInName = null;
    String shortOutName = null;

    if(buildShortIn())  // shortfall in
    {
      shortInName = InventoryScheduleNames.SHORTFALL_DUE_IN;
    }
    if(buildShortOut())  // shortfall out
    {
      shortOutName = InventoryScheduleNames.SHORTFALL_DUE_OUT;
    }
    //if(InventoryChartUI.lookAheadDays > 0)
    //{
    //  buildOddData(InventoryChartUI.lookAheadDays, InventoryScheduleNames.PROJECTED_DUE_OUT, InventoryScheduleNames.CRITICAL_LEVEL, true);
    //}
    
    // new critical calculation = reorder * .8
    
    if(columnExists(InventoryScheduleNames.REORDER_LEVEL))
    {
    	double factor = 0.8;
    	Vector rv = null;
    	for(int i = 1; i < columnNames.size() ; i++)
      {
        if(columnNames.elementAt(i).equals(InventoryScheduleNames.REORDER_LEVEL))
        {
          rv = (Vector) columns.elementAt(i - 1);
          continue;
        }
      }
    	
    	Vector newColumn = multiplyRows(rv, factor, false);
    	if(newColumn != null)
      {
      	Vector newData = trimToOnhand(newColumn);
        columns.add(newData);
        columnNames.add(InventoryScheduleNames.CRITICAL_LEVEL);
        
      }
      
    }
    String generatedColumn = null;

    /*if(columnExists(InventoryScheduleNames.PROJECTED_DUE_OUT))
    {
      generatedColumn = combineDataColumns(InventoryScheduleNames.PROJECTED_DUE_OUT, InventoryScheduleNames.REQUESTED_DUE_OUT, "Experimental Column", true);
      buildOddData(InventoryChartUI.lookAheadDays, generatedColumn, InventoryScheduleNames.TARGET_LEVEL, true);
    }
    else if(columnExists(InventoryScheduleNames.REQUESTED_DUE_OUT))
    {
      generatedColumn = combineDataColumns(InventoryScheduleNames.REQUESTED_DUE_OUT, InventoryScheduleNames.PROJECTED_DUE_OUT, "Experimental Column", true);
      buildOddData(InventoryChartUI.lookAheadDays, generatedColumn, InventoryScheduleNames.TARGET_LEVEL, true);
    }
    */
    
    //  new target = (GOAL + REORDER) * .5
    
    if(columnExists(InventoryScheduleNames.GOAL_LEVEL) && columnExists(InventoryScheduleNames.REORDER_LEVEL))
    {
    	System.out.println("start target");
    	generatedColumn = combineDataColumns(InventoryScheduleNames.GOAL_LEVEL, InventoryScheduleNames.REORDER_LEVEL, "Experimental Column", true);
    	double factor = 0.5;
    	Vector rv = null;
    	for(int i = 1; i < columnNames.size() ; i++)
      {
        if(columnNames.elementAt(i).equals("Experimental Column"))
        {
          rv = (Vector) columns.elementAt(i - 1);
          continue;
        }
      }
    	
    	Vector newColumn = multiplyRows(rv, factor, false);
    	if(newColumn != null)
      {
      	Vector newData = trimToOnhand(newColumn);
        columns.add(newData);
        columnNames.add(InventoryScheduleNames.TARGET_LEVEL);
        
      }
      System.out.println("end target");
    }
    
    //  batch datasets for Projected Due In
    //                     Projected Requisitions - Projected Requested Due In
    //                     Approved Due In - Due In
    //                     Resupply Requisitions - Requested Due In
    //                     Requisitions - Requested Due Out
    //                     Approved Due Out - Due Out
    //                     Projected Due Out
    //                     Projections - Projected Requested Due Out

    batchDataset(InventoryScheduleNames.PROJECTED_DUE_IN);
    batchDataset(InventoryScheduleNames.PROJECTED_REQUESTED_DUE_IN);
    batchDataset(InventoryScheduleNames.DUE_IN);
    batchDataset(InventoryScheduleNames.REQUESTED_DUE_IN);
    batchDataset(InventoryScheduleNames.REQUESTED_DUE_OUT);
    batchDataset(InventoryScheduleNames.DUE_OUT);
    batchDataset(InventoryScheduleNames.PROJECTED_DUE_OUT);
    batchDataset(InventoryScheduleNames.PROJECTED_REQUESTED_DUE_OUT);

    rowCount = maxRows + 1;
    colCount = columns.size();
    originalRows = allRows;
    setColumnNames(columnNames, columnNames.size(), shortInName, shortOutName);
    int cols = columns.size();
    data = setColumnData(columns, cols, allRows);
    data = sortTheRow(data, allRows, colCount);
    buildDataSets();
   }
  }
  /*********************************************************************************************************************
  <b>Description</b>: Default constructor

  <br><b>Notes</b>:<br>
	                  - Puts dummy data into the table

  <br>

	*********************************************************************************************************************/
  public InventoryTableModel()
  {

    columns = new Vector();
    columnNames = new Vector(4);
    String[] names = {"On Hand", "Unconfirmed Due In", "Requested Due In", "Due Out" };
    for(int i = 0; i < 4; i++)
    {
      columnNames.add(names[i]);
    }
    setColumnNames(columnNames, columnNames.size(), null, null);
    data = new Object[4][columnNames.size()];
    for(int i = 0; i < 4; i++)
    {
      data[i][0] = new Double(System.currentTimeMillis());
      Vector rows = new Vector();
      for(int j = 1; j < columnNames.size(); j++)
      {
        data[i][j] = " ";

        rows.addElement(new ScheduleTableEntry(0,
                 System.currentTimeMillis(),
                 System.currentTimeMillis()));

      }
      columns.add(rows);

    }
    allRows = 4;

  }
  // ---------------------------------------------------------------------------------------------------------------------
  // Public Member Methods
  // ---------------------------------------------------------------------------------------------------------------------
  /**********************************************************************************************
  <b>Description</b>: trimToOnhand

  <br><b>Notes</b>:<br>
	                  -Trim the passed in data to the "on hand" values

  <br>
  
  @param dataVector Vector
  @return Vector

	**********************************************************************************************/
  public Vector trimToOnhand(Vector dataVector)
  {
  	
  	Vector onHandData = null;  //  to hold the data from the passed in column
  	Vector newData = new Vector();
    
    for(int i = 1; i < columnNames.size() ; i++)
    {
      if(columnNames.elementAt(i).equals("On Hand"))
      {
        onHandData = (Vector) columns.elementAt(i - 1);
      }
    }
    long onHandTime = 0;
    for(int i = 0; i < onHandData.size(); i++)
    {
    	ScheduleTableEntry d = (ScheduleTableEntry) onHandData.elementAt(i);
    	long thisTime = d.startTime;
    	double thisQuantity = d.quantity;
    	if(thisQuantity > 0)
    	{
    		onHandTime = thisTime;
    		break;
    	}
    }
    
    for(int i = 0; i < dataVector.size(); i++)
    {
    	ScheduleTableEntry d = (ScheduleTableEntry) dataVector.elementAt(i);
    	long thisTime = d.startTime;
    	double thisQuantity = d.quantity;
    	if(thisTime >= onHandTime)
    	{
    	  newData.add(d);
    	}
    }
    return newData;
  }
  

  /**********************************************************************************************
  <b>Description</b>: batchDataset

  <br><b>Notes</b>:<br>
	                  Make a new column from the passed in column by summing over days

  <br>
  
  @param columnToSum String - the column to batch
  @return DataSet

	**********************************************************************************************/
  public DataSet batchDataset(String columnToSum)
  {
    // intervalType 0 = day or greater
    //              1 = by hour
    String columnToMake = InventoryScheduleNames.BATCHED_HEADER + columnToSum;
    int intervalType = 0;
    Vector dataVector = null;
    Vector newColumn = new Vector(5);
    long lookAheadIncrement = 86400000;  // 1 day
    int lookahead = 1;
    for(int i = 1; i < columnNames.size() ; i++)
    {

      if(columnNames.elementAt(i).equals(columnToSum))
      {
        dataVector = (Vector) columns.elementAt(i - 1);
      }
    }
    ScheduleTableEntry newRow = null;

    if(dataVector != null)
    {
      int i = 0;
      while(i < dataVector.size())
      {
        double sumQuantity = 0;
        ScheduleTableEntry oldRow = (ScheduleTableEntry) dataVector.elementAt(i);
        long rawEntryTime = oldRow.startTime;
        Date rawIntervalStart = new Date(rawEntryTime);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(rawIntervalStart);
        //myData[(i*2)+0] = (double)(roundTimeToHours(cal)/timeScale);
//        long entryTime = roundTimeToHours(cal);
        long entryTime = truncateTimeToHours(cal);
        
        Date intervalStart = new Date(entryTime);

        if(intervalType == 0)
        {
// EBM Deprecated method
//          intervalStart.setHours(0);
          cal.setTime(intervalStart);
          cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
          intervalStart = cal.getTime();
        }
// EBM - the intervalStart comes from entryTime which has already been rounded to the nearest hour so min/sec/msec are 0
// Deprecated methods
//        intervalStart.setMinutes(0);
//        intervalStart.setSeconds(0);
        long thisIntervalStart = intervalStart.getTime();
        long thisIntervalEnd = thisIntervalStart + lookAheadIncrement;
        sumQuantity = oldRow.quantity;
        newRow = new ScheduleTableEntry(0, thisIntervalStart, thisIntervalEnd);
        int startIndex = i + 1;
        for(int j = startIndex; j < dataVector.size(); j++)
        {
          ScheduleTableEntry nextRow = (ScheduleTableEntry) dataVector.elementAt(j);
          //********************
          long rawStartTime = oldRow.startTime;
          Date rawStart = new Date(rawStartTime);
          cal = new GregorianCalendar();
          cal.setTime(rawStart);
          //myData[(i*2)+0] = (double)(roundTimeToHours(cal)/timeScale);
//          long startTime = roundTimeToHours(cal);
          long startTime = truncateTimeToHours(cal);

          //********************
          if(startTime > thisIntervalStart && nextRow.startTime < thisIntervalEnd)
          {
            sumQuantity += nextRow.quantity;
            i++;
          }
          else
            break;
        }
        i++;  // always add 1 since we look past this element above
        newRow.quantity = sumQuantity;
        newColumn.add(newRow);
        allRows++;
      }
      columns.add(newColumn);
      columnNames.add(columnToMake);
      //System.out.println("new Column - " + columnToMake);
    }
    return null;
  }

  /**********************************************************************************************
  <b>Description</b>: getDataSets

  <br><b>Notes</b>:<br>
	                  return the dataSets Hashtable

  <br>
  

  @return Hashtable

	**********************************************************************************************/

  public Hashtable getDataSets()
  {
    return(dataSets);
  }
  /**********************************************************************************************
  <b>Description</b>: getColumnName

  <br><b>Notes</b>:<br>
	                  

  <br>
  
  @param col int - the column to get
  @return String

	**********************************************************************************************/
  public String getColumnName(int col)
  {
     return finalColumnNames[col];

  }
  /**********************************************************************************************
  <b>Description</b>: getColumnCount(

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  @return int

	**********************************************************************************************/
  public int getColumnCount()
  {
    //System.out.println("column count = " + colCount);
    return colCount;
  }
  /**********************************************************************************************
  <b>Description</b>: getRowCount

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  @return int

	**********************************************************************************************/
  public int getRowCount() {
    return allRows;
  }
  /**********************************************************************************************
  <b>Description</b>: getColumnData

  <br><b>Notes</b>:<br>
	                  - get the data array for one column

  <br>
  
  @param col int - column to get data for
  @return double[]

	**********************************************************************************************/
  public double[] getColumnData(int col)
  {
    Vector scheduleList = (Vector) columns.elementAt(col - 1);
    double[] myData = new double[scheduleList.size()*2];
//    GregorianCalendar cal = new GregorianCalendar();
//    Date date = new Date();
    for(int i=0; i<scheduleList.size(); i++)
    {
      ScheduleTableEntry sched = (ScheduleTableEntry)scheduleList.elementAt(i);
//      date.setTime(sched.startTime);
//      cal.setTime(date);
//      myData[(i*2)+0] = (double)(roundTimeToHours(cal)/timeScale);
      myData[(i*2)+0] = (double)(sched.startTime/timeScale);
      myData[(i*2)+1] = sched.quantity;
    }
    return myData;
  }
  /**********************************************************************************************
  <b>Description</b>: getNSN

  <br><b>Notes</b>:<br>
	                  get the NSN string for this inventory

  <br>
  

  @return String

	**********************************************************************************************/
  public String getNSN()
  {
    String nsn = null;
    if (inventory != null)
    {
      nsn = inventory.getAssetName();
      int index = nsn.indexOf("NSN/");
      if(index == -1)
        return nsn;
      else
      //nsn = nsn.substring(nsn.indexOf("NSN/")).trim();
        return nsn.substring(index).trim();
    }

    return(nsn);
  }
  /**********************************************************************************************
  <b>Description</b>: getAssetName

  <br><b>Notes</b>:<br>
	                  get the asset name for this inventory

  <br>
  

  @return String

	**********************************************************************************************/
  public String getAssetName()
  {
     if(inventory != null)
       return inventory.getAssetName();
     else
       return  " ";
  }

  /**********************************************************************************************
  <b>Description</b>: sortTheRow

  <br><b>Notes</b>:<br>
	                  sort the table array

  <br>
  
  @param timearray Object[][] - the array to sort
  @param rows int
  @param cols int
  @return Object[][] 

	**********************************************************************************************/
  public Object[][] sortTheRow(Object[][] timearray, int rows, int cols)
  {
    Object[][] newdata = new Object[rows][cols];
    Object[][] mergedData;
    RowObject[] ro = new RowObject[rows];
    int newRows = 0;
    for(int i = 0; i < rows; i++)
    {

      // do each row as a rowobject

      Object[] oldArray = new Object[cols];
      for(int j = 0; j < cols; j++)
      {
        if((Double)timearray[i][j] != null)
        {
          oldArray[j] = (Double)timearray[i][j];  //  for each row
        }
        else
        {
          oldArray[j] = " ";
        }
      }
      ro[i] = new RowObject(rows, cols, oldArray); // array for each row [i], array of row


    }
     ArraySorter.sort(ro, rowObjectComparer);

     //  merge time equal rows (adjusted to within a minute)

     long time = 0;
     newRows = 0;
     RowObject[] sortedRo = new RowObject[rows];
     for(int i = 0; i < rows; i++)   // for each sorted row (ro)
     {
      Object[] values = new Object[cols];
      for(int l = 0; l < cols; l++)
      {
        values[l] = ro[i].columnValues[l];
      }
      RowObject rowObj = new RowObject(rows, cols, values);
      Object arrayValues[] = (Object[])rowObj.columnValues;
      long colZero = (long)((Double)values[0]).doubleValue();
      long minutes = colZero/60000;
      long rounded = java.lang.Math.round( minutes);
      if(rounded == time) // if this one is the same time as the last one built
      {
        sortedRo[newRows - 1].mergeUp(values);

      }
      else
      {
        sortedRo[newRows] = new RowObject(rows, cols, values); // array for each row [i], array of row
        time = rounded;
        newRows++;
      }
     }
     allRows = newRows;

     newdata = new Object[newRows][cols];
     for (int k = 0; k < newRows; k++)
     {

      for(int z = 0; z < cols; z++)
      {
        newdata[k][z] = sortedRo[k].columnValues[z];
      }

     }

    return newdata;

  }
  /**********************************************************************************************
  <b>Description</b>: buildOddData

  <br><b>Notes</b>:<br>
	                  build a new column, summing over days

  <br>
  
  @param lookahead int - number of days to look ahead
  @param columnToSum String
  @param columnToMake String
  @param sum boolean true = add false = subtract
  @return void

	**********************************************************************************************/
  public void buildOddData(int lookahead, String columnToSum, String columnToMake, boolean sum)
  {
    Vector dataVector = null;
    Vector newColumn = new Vector(5);
    long lookAheadIncrement = 86400000;  // 1 day
    //System.out.println("dataset " + columnToMake);
    for(int i = 1; i < columnNames.size() ; i++)
    {

      if(columnNames.elementAt(i).equals(columnToSum))
      {
        dataVector = (Vector) columns.elementAt(i - 1);
      }
    }
    ScheduleTableEntry newRow = null;

    if(dataVector != null)
    {
      for(int i = 0; i < dataVector.size(); i++)
      {
        double sumQuantity = 0;
        ScheduleTableEntry oldRow = (ScheduleTableEntry) dataVector.elementAt(i);
        long entryTime = oldRow.startTime;
        sumQuantity = oldRow.quantity;
        newRow = new ScheduleTableEntry(0, entryTime, entryTime);
        //System.out.println("entrytime " + entryTime + " i= " + i);
        for(int j = i + 1; j < dataVector.size(); j++)
        {
          ScheduleTableEntry nextRow = (ScheduleTableEntry) dataVector.elementAt(j);
          //System.out.println("new row time " + nextRow.startTime + " j= " + j);
          if(nextRow.startTime < (entryTime + (lookahead * lookAheadIncrement)))
          {
            if(sum)
              sumQuantity += nextRow.quantity;
            else
              sumQuantity -= nextRow.quantity;
          }
          else
            break;
         //System.out.println("sumQuantity " + sumQuantity);
        }
        newRow.quantity = sumQuantity;
        newColumn.add(newRow);
        allRows++;
      }
      columns.add(newColumn);
      columnNames.add(columnToMake);

    }
  }
  /**********************************************************************************************
  <b>Description</b>: buildShortIn

  <br><b>Notes</b>:<br>
	                  combine columns to build shortfall-in column

  <br>
  

  @return boolean

	**********************************************************************************************/
  public boolean buildShortIn()
  {

    //  new column = requested due in - due in - unconfirmed due in + previous new column
    //  get index of column names
    //  get index of data (getColumnData[i])
    //  compute using rows that are equal dates

    String rdi = InventoryScheduleNames.REQUESTED_DUE_IN;
    String di = InventoryScheduleNames.DUE_IN;
    String udi = InventoryScheduleNames.UNCONFIRMED_DUE_IN;
    int rdiIndex = 0;
    int diIndex = 0;
    int udiIndex = 0;
    Vector rdiVector = null;
    Vector diVector = null;
    Vector udiVector = null;

    for(int i = 1; i < columnNames.size() - 1; i++)
    {
      if(columnNames.elementAt(i).equals(rdi))
      {
        rdiIndex = i;
        rdiVector = (Vector) columns.elementAt(i - 1);
        continue;
      }
      if(columnNames.elementAt(i).equals(di))
      {
        diIndex = i;
        diVector = (Vector) columns.elementAt(i - 1);
        continue;
      }
      if(columnNames.elementAt(i).equals(udi))
      {
        udiIndex = i;
        udiVector = (Vector) columns.elementAt(i - 1);
        continue;
      }
    }

    //  indexes are how we get to the data

    if(rdiIndex != 0 || diIndex != 0 || udiIndex !=0)
    {
      Vector newRow = null;
      if(udiVector == null & rdiVector != null && diVector != null)
        newRow = calcNewOutRow(rdiVector, diVector, false, true);
      else if(diVector == null && rdiVector != null && udiVector == null)
        newRow = calcNewOutRow(rdiVector, null, true, true);
      else if(rdiVector != null && diVector != null && udiVector != null)
      {
//        newRow = calcNewInRow(rdiVector, diVector, udiVector);
        newRow = calcNewOutRow(rdiVector, diVector, false, false);
        newRow = calcNewOutRow(newRow, udiVector, false, true);
      }
      else if(rdiVector != null && udiVector != null && diVector == null)
        newRow = calcNewOutRow(rdiVector, udiVector, false, true);
      else
        return false;
      if(newRow != null)
      {
        columns.add(newRow);
        columnNames.add(InventoryScheduleNames.SHORTFALL_DUE_IN);

        return true;
      }
      else
        return false;
    }
    else
      return false;

  }
  /**********************************************************************************************
  <b>Description</b>: calcNewInRow

  <br><b>Notes</b>:<br>
	                  Combine input vector to make a new column

  <br>
  
  @param rv Vector input data vector
  @param dv Vector input data vector
  @param uv Vector input data vector
  @return Vector

	**********************************************************************************************/
  public Vector calcNewInRow(Vector rv, Vector dv, Vector uv)
  {
    if(rv == null || dv == null || uv == null)
    {
      return null;
    }
    int i = 0;
    int j = 0;
    int k = 0;
    boolean iDone = false;
    boolean jDone = false;
    boolean kDone = false;
    Vector row = new Vector();

    //find the first objects
    //  start after time for first r.startTime

    ScheduleTableEntry r = (ScheduleTableEntry) rv.elementAt(i);
    ScheduleTableEntry d = null;
    ScheduleTableEntry u = null;

    double previousValue = 0;
    long thisTime = r.startTime;
    double thisValue = 0;


      d = (ScheduleTableEntry) dv.elementAt(0);
      if(d.startTime < r.startTime)
      {
        thisTime = d.startTime;
      }

      u = (ScheduleTableEntry) uv.elementAt(0);thisTime = r.startTime;
      if(u.startTime < d.startTime)
      {
        thisTime = u.startTime;
      }




    //  enter with first r object and d and u objects which are greater or equal in time values


    while(true)
    {
      if(iDone && jDone && kDone)
        break;

      //  start a row
      thisValue = 0;
      ScheduleTableEntry newRow = new ScheduleTableEntry(0, thisTime, thisTime);  // thisTime has been set to the next time to make a row
      if(r.startTime == thisTime)
      {
        thisValue += r.quantity;
        i++;

        if(i >= rv.size())
        {
          iDone = true;
System.out.println("i");  
        }
        else
          r = (ScheduleTableEntry) rv.elementAt(i);
      }
      if(d.startTime == thisTime)
      {
        thisValue -= d.quantity;
        j++;

        if(j >= dv.size())
        {
          jDone = true;
System.out.println("j");
        }
        else
          d = (ScheduleTableEntry) dv.elementAt(j);
      }
      if(u.startTime == thisTime)
      {
        thisValue -= u.quantity;
        k++;

        if(k >= uv.size())
        {
          kDone = true;
System.out.println("k");
        }
        else
          u = (ScheduleTableEntry) uv.elementAt(k);
      }

      // don't allow zero

      thisValue += previousValue;

      previousValue = thisValue;
      if(thisValue >= 0)
        newRow.quantity = thisValue;
      else
        newRow.quantity = 0;
        
      //  we can make this row

      row.add(newRow);
      allRows++;

      //  get the next time value

      if(r.startTime > thisTime && betterTime(r, d, jDone) && betterTime(r, u, kDone))
      {
        thisTime = r.startTime;
      }
      if(d.startTime > thisTime && betterTime(d, r, iDone) && betterTime(d, u, kDone))
      {
        thisTime = d.startTime;
      }
      if(u.startTime > thisTime && betterTime(u, r, iDone) && betterTime(u, d, jDone))
      {
        thisTime = u.startTime;
      }
    }

    return row;

  }
  /**********************************************************************************************
  <b>Description</b>: betterTime

  <br><b>Notes</b>:<br>
	                  indicate if time 2 is greater than time 1

  <br>
  @param x ScheduleTableEntry
  @param m ScheduleTableEntry
  @param done boolean - forces return true

  @return boolean

	**********************************************************************************************/
  public boolean betterTime(ScheduleTableEntry x, ScheduleTableEntry m, boolean done)
  {
    if(x.startTime <= m.startTime || done)
      return true;
    else
      return false;
  }
  /**********************************************************************************************
  <b>Description</b>: buildShortOut

  <br><b>Notes</b>:<br>
	                  combine columns to build a new column - shortfall out

  <br>
  

  @return boolean

	**********************************************************************************************/
  public boolean buildShortOut()
  {
    //  new column = requested due in - due in - unconfirmed due in + previous new column
    //  get index of column names
    //  get index of data (getColumnData[i])
    //  compute using rows that are equal dates
    String rdi = InventoryScheduleNames.REQUESTED_DUE_OUT;
    String prdi = InventoryScheduleNames.PROJECTED_REQUESTED_DUE_OUT;
    String di = InventoryScheduleNames.DUE_OUT;
    String pdo = InventoryScheduleNames.PROJECTED_DUE_OUT;

    int rdiIndex = 0;
    int diIndex = 0;
    int prdiIndex = 0;
    int pdoIndex = 0;

    Vector rdiVector = null;
    Vector diVector = null;
    Vector prdiVector = null;
    Vector pdoVector = null;

    for(int i = 1; i < columnNames.size() ; i++)
    {
      if(columnNames.elementAt(i).equals(rdi))
      {
        rdiIndex = i;
        rdiVector = (Vector) columns.elementAt(i - 1);

        continue;
      }
      if(columnNames.elementAt(i).equals(di))
      {
        diIndex = i;
        diVector = (Vector) columns.elementAt(i - 1);

        continue;
      }

      if(columnNames.elementAt(i).equals(prdi))
      {
        prdiIndex = i;
        prdiVector = (Vector) columns.elementAt(i - 1);

        continue;
      }

      if(columnNames.elementAt(i).equals(pdo))
      {
        pdoIndex = i;
        pdoVector = (Vector) columns.elementAt(i - 1);

        continue;
      }

    }


      Vector newRow = null;
      Vector combinedVectorA = null;
      Vector combinedVectorB = null;

      //  projected requested due out - projected due out
      //System.out.println("prdo and pdo");
      combinedVectorA = calcNewOutRow(prdiVector, pdoVector, false, false);
      //  requested due out - due out
      //System.out.println("rdo and do");
      combinedVectorB = calcNewOutRow(rdiVector, diVector, false, false);

      //add the new vector elements ( combinedA + combinedB)
      //System.out.println("a + b");
      newRow = calcNewOutRow(combinedVectorA, combinedVectorB, true, true);


      if(newRow != null)
      {
        columns.add(newRow);
        columnNames.add(InventoryScheduleNames.SHORTFALL_DUE_OUT);

        return true;
      }
      else
        return false;

  }
/**********************************************************************************************
  <b>Description</b>: combineDataColumns

  <br><b>Notes</b>:<br>
	                  - get index of column names
                      get index of data (getColumnData[i])
                      compute using rows that are equal dates

  <br>
  
  @param columnA String
  @param columnB String
  @param newColumn String
  @param op boolean - add or subtract
  @return String

	**********************************************************************************************/
public String combineDataColumns(String columnA, String columnB, String newColumn, boolean op)
{

    //  get index of column names
    //  get index of data (getColumnData[i])
    //  compute using rows that are equal dates


    int aIndex = 0;
    int bIndex = 0;

    Vector aVector = null;
    Vector bVector = null;
    Vector newRow = null;

    for(int i = 1; i < columnNames.size() ; i++)
    {
      if(columnNames.elementAt(i).equals(columnA))
      {
        aIndex = i;
        aVector = (Vector) columns.elementAt(i - 1);

        continue;
      }
      if(columnNames.elementAt(i).equals(columnB))
      {
        bIndex = i;
        bVector = (Vector) columns.elementAt(i - 1);

        continue;
      }

    }
    if(aVector != null)
      newRow = calcNewOutRow(aVector, bVector, op, false);
    if(newRow != null)
      {
        columns.add(newRow);
        columnNames.add(newColumn);

        return newColumn;
      }
      else
        return null;


}

/**********************************************************************************************
  <b>Description</b>: calcNewOutRow

  <br><b>Notes</b>:<br>
	                  add or subtract columns to get new data vector
                    
  <br>
  
  @param rv Vector
  @param dv Vector
  @param sum boolean - true = add false = subtract
  @param previous boolean - make a running total
  @return Vector

	**********************************************************************************************/
public Vector calcNewOutRow(Vector rv, Vector dv, boolean sum, boolean previous)
  {
    //System.out.println("calcNewRowOut ");
    //  if both rv and dv are non null, the elements are summed(sum = true)
    //  or subtracted(sum = false)
    //  if previous = true then the values are a running total
    //  if rv is null and sum = false, a negative total may be returned for each value
    //  if sum = true, no negative values will be returned

    int i = 0;
    int j = 0;

    boolean iDone = false;
    boolean jDone = false;

    Vector row = new Vector();

    //find the first objects
    //  start after time for first r.startTime

    ScheduleTableEntry r = null;
    ScheduleTableEntry d = null;


    double previousValue = 0;
    long thisTime = 0;
    double thisValue = 0;


    if(rv == null && dv == null)
      return null;

    if(dv != null)
    {
      //System.out.println("dv.size = " + dv.size());
      d = (ScheduleTableEntry) dv.elementAt(0);
      thisTime = d.startTime;
    }
    else
      jDone = true;


   if(rv != null)
    {
      //System.out.println("rv.size = " + rv.size());
      r = (ScheduleTableEntry) rv.elementAt(0);
      if(d != null && r.startTime > d.startTime)
      {
        thisTime = d.startTime;
      }
      else
        thisTime = r.startTime;
    }
    else
      iDone = true;

    //  enter with first r object and d and u objects which are greater or equal in time values


    while(true)
    {
      if(iDone && jDone)
        break;

      //  start a row
      thisValue = 0;
      ScheduleTableEntry newRow = new ScheduleTableEntry(0, thisTime, thisTime);  // thisTime has been set to the next time to make a row
      if(r != null)
      {
        if(r.startTime == thisTime)
        {
          thisValue += r.quantity;
          //System.out.println("r value added to row" + thisValue);
          i++;

          if(i >= rv.size())
            iDone = true;
          else
            r = (ScheduleTableEntry) rv.elementAt(i);
        }
      }
      if(d != null)
      {
        if(d.startTime == thisTime)
        {
          if(sum)
          {
            thisValue += d.quantity;
            //System.out.println("d value added to row" + thisValue);
          }
          else
          {
            thisValue -= d.quantity;
            //System.out.println("d value subtracted from row" + thisValue);
          }

          j++;

          if(j >= dv.size())
            jDone = true;
          else
            d = (ScheduleTableEntry) dv.elementAt(j);
        }
      }

      // could be zero if r is null

      if(previous)
      {
        thisValue += previousValue;
        previousValue = thisValue;
      }
      if(thisValue >= 0)
        newRow.quantity = thisValue;
      else if(!sum)
        newRow.quantity = thisValue;
      else
        newRow.quantity = 0;
      //System.out.println("newRow.quantity " + newRow.quantity);

      //  we can make this row

      row.add(newRow);
      allRows++;
      if(d != null && r != null)
      {
        //  get the next time value

        if(r.startTime > thisTime && betterTime(r, d, jDone))
        {
          thisTime = r.startTime;
        }
        if(d.startTime > thisTime && betterTime(d, r, iDone))
        {
          thisTime = d.startTime;
        }
      }
      else if(r != null)
        thisTime = r.startTime;
      else
        thisTime = d.startTime;

    }

    return row;
  }

/**********************************************************************************************
  <b>Description</b>: multiplyRows

  <br><b>Notes</b>:<br>
	                  multiply row by a given factor

  <br>
  
  @param rv Vector - input data
  @param factor double - factor to multiply by
  @param filterZero boolean - filter out zero data
  @return void

	**********************************************************************************************/
public Vector multiplyRows(Vector rv, double factor, boolean filterZero)
  {
    Vector row = new Vector();
       
    ScheduleTableEntry r = null;
    
    long thisTime = 0;
    double thisValue = 0;
    
    if(rv == null)
      return null;
      
    //  enter with first r object and d and u objects which are greater or equal in time values
    
    for(int i = 0; i < rv.size(); i++)
    {
    	thisValue = 0;
    	ScheduleTableEntry newRow = new ScheduleTableEntry(0, thisTime, thisTime);  // thisTime has been set to the next time to make a row
      r = (ScheduleTableEntry) rv.elementAt(i);
      if(r.quantity == 0 && filterZero)
        continue;           // don't calc a point for this one
      if(r != null)
      {
        thisValue = r.quantity * factor;
        thisTime = r.startTime;
      }
                
      if(thisValue >= 0)
        newRow.quantity = thisValue;
      else
        newRow.quantity = 0;
      //System.out.println("newRow.quantity " + newRow.quantity);
      newRow.startTime = thisTime;
      //  we can make this row

      row.add(newRow);
      allRows++;
    }

    return row;
  }
  /**********************************************************************************************
  <b>Description</b>: columnExists

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  @return boolean

	**********************************************************************************************/
  public boolean columnExists(String col)
  {
    boolean value = false;
    for(int i= 0; i < columnNames.size(); i++)
    {
      if(((String)columnNames.elementAt(i)).equals(col))
        value = true;
    }
    return value;

  }

  /**********************************************************************************************
  <b>Description</b>: getValueAt

  <br><b>Notes</b>:<br>
	                  get the data for a given point

  <br>
  
  @param row int
  @param col int
  @return Object

	**********************************************************************************************/
  public Object getValueAt(int row, int col)
  {

    try{
    if(data[row][col] != null)
    {

        if(col == 0)
          return shortDate((long)((Double)data[row][col]).doubleValue());
        else
        {
          Class dataClass = data[row][col].getClass();
          if(!dataClass.getName().equals("java.lang.String"))
             return qtyFormater.format(((Double)data[row][col]).doubleValue());
          else
             return " ";
        }
    }
    else
      return " ";
    }
    catch(Exception e)
    {
      return null;
    }

  }  // end of getValueAt
  // ---------------------------------------------------------------------------------------------------------------------
  // Private Member Methods
  // ---------------------------------------------------------------------------------------------------------------------
  /**********************************************************************************************
  <b>Description</b>: buildDataSets

  <br><b>Notes</b>:<br>
	                  Build the DataSet Objects from the inventory data

  <br>
  

  @return void

	**********************************************************************************************/
  private void buildDataSets()
  {
    try
    {
      int cols = getColumnCount();
      DataSet dataSet = null;
      double[] data = null;
      String name = null;
      String group = null;
      for (int i=1; i<cols; i++)
      {
        name = getColumnName(i);
        group = InventoryScheduleNames.getGroup(name);
        if (group != null)
        {
          dataSet = InventoryScheduleNames.buildDataSet(name, group, getColumnData(i));
          dataSets.put(name, dataSet);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  /**********************************************************************************************
  <b>Description</b>: setColumnNames

  <br><b>Notes</b>:<br>
	                  - Set the display column names

  <br>
  

  @return void

	**********************************************************************************************/
  private void setColumnNames(Vector names, int columnIndex, String shortInName, String shortOutName)
  {
    int newIndex = columnIndex;

    finalColumnNames = new String[names.size()];
    for(int i = 0; i < names.size(); i++)
    {
      finalColumnNames[i] = (String)names.elementAt(i);
      //System.out.println("set column " + finalColumnNames[i]);
      colCount = i + 1;
    }

}
/**********************************************************************************************
  <b>Description</b>: setColumnData

  <br><b>Notes</b>:<br>
	                  build the table data matrix from the Vector

  <br>
  
  @param columnData Vector
  @param numberOfColumns int
  @param numberOfRows int
  @return Object[][]

	**********************************************************************************************/
  private Object[][] setColumnData(Vector columnData, int numberOfColumns, int numberOfRows)
  {
    Object[][] newData = new Object[numberOfRows ][numberOfColumns + 1];
    //System.out.println("number of cols " + numberOfColumns + " number of rows " + numberOfRows);
    int absRow = 0;
    GregorianCalendar cal = new GregorianCalendar();
    Date date = new Date();
    for(int i = 0; i < columnData.size(); i++)
    {

      Vector nextRow = (Vector)columnData.elementAt(i);
      for(int j = 0; j < nextRow.size(); j++)
      {
        ScheduleTableEntry s = (ScheduleTableEntry) nextRow.elementAt(j);
        date.setTime(s.startTime);
        cal.setTime(date);
//        newData[absRow][0] = new Double(roundTimeToHours(cal));
        newData[absRow][0] = new Double(truncateTimeToHours(cal));
        newData[absRow++][i + 1] = new Double((double)s.quantity);
      }

    }
    allRows = absRow;
    return newData;
  }

  
  /**********************************************************************************************
  <b>Description</b>: shortDate

  <br><b>Notes</b>:<br>
	                  return the date as a string

  <br>
  

  @return String

	**********************************************************************************************/
  private String shortDate(long time) 
  {
	  if (time < 0) return "";
	  String sdate = dateTimeFormater.format(new Date(time));
	  // map '9/8/00 12:00 AM' to ' 9/8/00 12:00 AM'
	  while(sdate.length()<17)
	  {
	      sdate = " "+sdate;
	  }
	  return sdate;
  }

  /**********************************************************************************************
  <b>Description</b>: roundTimeToHours

  <br><b>Notes</b>:<br>
	                  

  <br>
  
  @param cal GregorianCalendar
  @return long

	**********************************************************************************************/

  private long roundTimeToHours(GregorianCalendar cal)
  {
    if (cal.get(Calendar.MINUTE) > 29)
    {
      cal.add(Calendar.HOUR, 1);
    }

    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);

    return(cal.getTime().getTime());
  }
  /**********************************************************************************************
  <b>Description</b>: truncateTimeToHours

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  @return long

	**********************************************************************************************/
  private long truncateTimeToHours(GregorianCalendar cal)
  {
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);

    return(cal.getTime().getTime());
  }
  
  /**********************************************************************************************
  <b>Description</b>: getColumnClass

  <br><b>Notes</b>:<br>
	                  
                      JTable uses this method to determine the default renderer/
									    editor for each cell.  If we didn't implement this method,
									    then the last column would contain text ("true"/"false"),
									    rather than a check box.
									   

  <br>
  

  @return Class

	**********************************************************************************************/
  public Class getColumnClass(int c)
  {
      return getValueAt(0, c).getClass();
  }
  /**
   * <b>Description</b>: RowObjectComparer
   * 
   * <br><b>Notes</b>:<br>  supports array sorting
   * 
   * <br>
   */
  public static class RowObjectComparer implements ArraySorter.Comparer
  {
    public int compare(Object a, Object b)
    {
      return ((RowObject)a).compareTo((RowObject)b);
    }
  }
}  // end of tablemodel
  /**********************************************************************************************
  <b>Description</b>: ScheduleTableEntry

  <br><b>Notes</b>:<br>
	                  The data class

  <br>
	**********************************************************************************************/
  class ScheduleTableEntry {
    String name;
    String assetName;

    long startTime;
    double quantity;
    long endTime;
    /**********************************************************************************************
  <b>Description</b>: Constructor

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  

	**********************************************************************************************/
    public ScheduleTableEntry(String name, String assetName) {
      this.name = name;
      this.assetName = assetName;
      quantity = 0;
      startTime = -1;
      endTime = -1;
    }
    /**********************************************************************************************
  <b>Description</b>: Constructor

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  

	**********************************************************************************************/
    public ScheduleTableEntry(String name) {
      this(name, null);
    }
    /**********************************************************************************************
  <b>Description</b>: Constructor

  <br><b>Notes</b>:<br>
	                  

  <br>
  

  

	**********************************************************************************************/
    public ScheduleTableEntry(double quantity, long startTime, long endTime) {
      name = null;
      assetName = null;
      this.startTime = startTime;
      this.quantity = quantity;
    }
  }

