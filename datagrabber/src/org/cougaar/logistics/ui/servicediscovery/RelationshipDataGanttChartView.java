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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */


package org.cougaar.logistics.ui.servicediscovery;

import org.cougaar.mlm.ui.newtpfdd.gui.view.SimpleGanttChartView;
import org.cougaar.mlm.ui.newtpfdd.gui.view.TaskModel;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseState;

import java.util.Date;
import java.util.Iterator;

/**
 * The file GanttChartView reads a CSV file and displays
 * the chart.
 * <p>
 * Blank lines and line starting with "#" are ignored.
 * <p>
 * A blank start date tells the reader to copy the end date
 * from the prior line.  This is handy if your input file
 * only records the end time of the event.
 * <p>
 * A blank end date tells the reader to copy the start date
 * from the current line.  This is handy if the event is
 * instantaneous.
 * <p>
 * Example file:
 * <pre>
 * # comment
 * A, 2002-10-08 01:03:00, 2002-10-08 02:50:00, grey, foo
 * B, 2002-10-08 01:00:00, 2002-10-08 01:30:00, red, bar
 * B, , 2002-10-08 03:05:00, blue, baz
 * </pre>
 */
public class RelationshipDataGanttChartView extends SimpleGanttChartView {

    public void launch() {
        super.launch(getStartDate(), getTitle());
    }

    protected RelationshipScheduleData data;


    public RelationshipDataGanttChartView(RelationshipScheduleData rsData) {
        this.data = rsData;
    }

    protected Iterator getInstances() {
        return data.getInstances();
    }

    protected Iterator getLegs() {
        return data.getRelationships();
    }

    protected String getTitle() {
        return "Relationships of " + data.getSourceAgent();
    }

    protected Date getStartDate() {
        return new Date(data.getStartCDay());
    }

    /**
     * Create the task model.
     */
    protected TaskModel createTaskModel() {
        return new SimpleTaskModel((DatabaseState) new RelationshipDatabaseState());
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Title:  " + getTitle() + "\n");
        sb.append(data.toString());
        return sb.toString();
    }
}
