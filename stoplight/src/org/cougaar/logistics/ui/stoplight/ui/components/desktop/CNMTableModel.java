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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.JOptionPane;
import java.util.Vector;

import org.cougaar.logistics.ui.stoplight.ui.components.graph.DataSet;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.PolygonFillableDataSet;



public class CNMTableModel extends DefaultTableModel 
{
	      private boolean DEBUG = true;
	      public Vector receivePackets = new Vector();
	      public Vector transmitPackets = new Vector();
	      public Vector lostPackets = new Vector();
	      Object[][] data = null;
        /*{
            {"", ""}, 
             
            {"", ""} 
            
        };*/
        String[] columnNames;
        
        public CNMTableModel(DataSet dataSet)
        {
        	columnNames = new String[2];
        	columnNames[0] = "X";
        	columnNames[1] = "Y";
        	double arrayData[] = dataSet.getData();
        	//data = new Object[arrayData.length/2][arrayData.length/2];
        	data = new Object[arrayData.length/2][2];
        	//System.out.println("%%%% array " + arrayData.length);
        	int j = 0;
        	for(int i = 0; i < arrayData.length;i = i + 2)
        	{
        		//System.out.println("%%%% loop " + arrayData[i] + " i " + i + " j " + j);
        		data[j][0] = new Double(arrayData[i]);
        		//System.out.println("%%%% loop " + arrayData[i+1] + " i " + i + " j " + j);
        		data[j][1] = new Double(arrayData[i+1]);
        		j++;
        	}
        }
        
                
        public CNMTableModel()
        {
        	columnNames = new String[6];
        	columnNames[0] = "Cluster";
        	columnNames[1] = "IP Address";
        	columnNames[2] = "TimeStamp";
        	columnNames[3] = "Tx";
        	columnNames[4] = "Rx";
        	columnNames[5] = "Dropped";
        	
        	
          try
          {
            PolygonFillableDataSet rcvPacketsDS =  new PolygonFillableDataSet(new double[0], 0, false);
            PolygonFillableDataSet xmitPacketsDS =  new PolygonFillableDataSet(new double[0], 0, false);
            PolygonFillableDataSet lostPacketsDS =  new PolygonFillableDataSet(new double[0], 0, false);
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
        
      public int addToTable(String cluster, String ip, Vector rowData)
      {
      	int returnVal = 0;
      	for(int i = 0; i < getRowCount(); i++)
      	{
      		//System.out.println("%%%% cluster is " + cluster + " value is " + getValueAt(i, 0));
      		if(cluster.equals(getValueAt(i, 0)) && ip.equals(getValueAt(i,1)))
      		{
      			removeRow(i);
      			insertRow(i, rowData);
      			return i;
      			
      		}
      		else
      		  returnVal = -1;
      	}
      	addRow(rowData);
      	//System.out.println("%%%% returning " + returnVal);
      	return returnVal;
      }
      
      public void addData(ClusterNetworkMetrics cMetrics)
      {
      	
        	int arrayLength = cMetrics.timeStamp.size();
        	//System.out.println("%%%% metrics array size is " + arrayLength);
        	Vector newData = new Vector(6);
        	newData.add(cMetrics.clusterName);
        	newData.add(cMetrics.ipAddress);
        	newData.add(((Double)cMetrics.timeStamp.elementAt(arrayLength - 1)).toString());
        	newData.add(cMetrics.ipSentPackets.elementAt(arrayLength - 1));
        	newData.add(cMetrics.ipRcvPackets.elementAt(arrayLength - 1));
        	newData.add(cMetrics.ipLostPackets.elementAt(arrayLength - 1));
        	 
        	int replaceRow = addToTable( cMetrics.clusterName, cMetrics.ipAddress, newData);  
        	int thisRowElement = getRowCount() - 1;
        	        	
        	double rdsData[] = new double[2*arrayLength];
        	        	
        	//  Create a Dataset from rcv packets
        	try
        	{
        		int j = 0;
	        	for(int i = 0; i < arrayLength; i++)
	        	{
	        		rdsData[j++] = ((Double)cMetrics.timeStamp.elementAt(i)).doubleValue()/1000.0;
	        		rdsData[j++] = (new Double((String)cMetrics.ipRcvPackets.elementAt(i)).doubleValue());
	        		//System.out.println("%%%% y value " + rdsData[j-1]);
	        	}
	        	
	        	PolygonFillableDataSet rcvPacketsDS =  new PolygonFillableDataSet(rdsData, arrayLength, false);
	        	rcvPacketsDS.dataName = cMetrics.clusterName + " " + "Receive Packets";
	        	rcvPacketsDS.yAxisLabel = "Receive Packets";
	        	rcvPacketsDS.title = cMetrics.clusterName;
	        	
	        	
	        	double xdsData[] = new double[2*arrayLength];
	        	j = 0;
	        	for(int i = 0; i < arrayLength; i++)
	        	{
	        		xdsData[j++] = ((Double)cMetrics.timeStamp.elementAt(i)).doubleValue()/1000.0;
	        		xdsData[j++] = (new Double((String)cMetrics.ipSentPackets.elementAt(i)).doubleValue());
	        		
	        	}
	        	
	        	PolygonFillableDataSet xmitPacketsDS =  new PolygonFillableDataSet(xdsData, arrayLength, false);
	        	xmitPacketsDS.dataName = cMetrics.clusterName + " " + "Transmit Packets";
	        	xmitPacketsDS.yAxisLabel = "Transmit Packets";
	        	xmitPacketsDS.title = cMetrics.clusterName;
	        	
	        	
	        	double ldsData[] = new double[2*arrayLength];
	        	j = 0;
	        	for(int i = 0; i < arrayLength; i++)
	        	{
	        		ldsData[j++] = ((Double)cMetrics.timeStamp.elementAt(i)).doubleValue()/1000.0;
	        		ldsData[j++] = (new Double((String)cMetrics.ipLostPackets.elementAt(i)).doubleValue());
	        		
	        	}
	        	
	        	PolygonFillableDataSet lostPacketsDS =  new PolygonFillableDataSet(ldsData, arrayLength, false);
	        	lostPacketsDS.dataName = cMetrics.clusterName + " " + "Lost Packets";
	        	lostPacketsDS.yAxisLabel = "Lost Packets";
	        	lostPacketsDS.title = cMetrics.clusterName;
	        	
	        	
	        	if(replaceRow == -1 || replaceRow == 0)
	        	{
	        	  transmitPackets.add(xmitPacketsDS);
	        	  receivePackets.add(rcvPacketsDS);
	        	  lostPackets.add(lostPacketsDS);
	        	}
	        	else
	        	{
	        		transmitPackets.removeElementAt(replaceRow);
	        		transmitPackets.add(replaceRow, xmitPacketsDS);
	        		receivePackets.removeElementAt(replaceRow);
	        		receivePackets.add(replaceRow, rcvPacketsDS);
	        		lostPackets.removeElementAt(replaceRow);
	        		lostPackets.add(replaceRow, lostPacketsDS);
	        	}
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
      }
        
        public int getColumnCount() {
            return columnNames.length;
        }
        
        

        public String getColumnName(int col) {
            return columnNames[col];
        }

        
         /* JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return false;
            
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
           
        }
        public Object getValueAt(int row, int col) {
        	if(data != null)
            return data[row][col];
          else
            return super.getValueAt(row, col);
        }
        
        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i=0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j=0; j < numCols; j++) {
                    //System.out.print("  " + data[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
  }