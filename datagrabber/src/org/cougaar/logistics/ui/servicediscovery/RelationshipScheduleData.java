/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.logistics.ui.servicediscovery;


import java.util.*;
import java.io.Serializable;
import java.text.SimpleDateFormat;


public class RelationshipScheduleData implements Serializable {

    final public static String DATE_FORMAT_STRING = "yyyy-MM-dd kk:mm:ss";
    final public static TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_STRING);

    protected final static int RELATIONSHIP_NUM_ITEMS = 6;
    protected final static int ROLE_IND = 0;
    protected final static int COUNT_IND = 1;
    protected final static int START_IND = 2;
    protected final static int END_IND = 3;
    protected final static int COLOR_IND = 4;
    protected final static int ORG_IND = 5;

    private long startCDay;
    private java.util.HashSet instanceSet;
    private ArrayList relationshipMapping;

    private String sourceAgent;

    public RelationshipScheduleData() {
        this(Long.MIN_VALUE, "");
    }

    public RelationshipScheduleData(long aCDay, String theSourceAgent) {
        setStartCDay(aCDay);
        this.sourceAgent = theSourceAgent;
        this.instanceSet = new HashSet();
        this.relationshipMapping = new ArrayList();
        dateFormatter.setTimeZone(GMT_TIME_ZONE);
    }

    public void setStartCDay(long aCDay) {
        this.startCDay = aCDay;
    }

    public long getStartCDay() {
        return this.startCDay;
    }

    public String getSourceAgent() {
        return this.sourceAgent;
    }

    public ArrayList getRelationshipMapping() {
        return relationshipMapping;
    }


    public static String formatDateString(Date date) {
        return (dateFormatter.format(date));
    }


    public void addRelationship(String role,
                                int count,
                                long startTime,
                                long endTime,
                                String color,
                                String org) {
        addRelationship(role, count, new Date(startTime), new Date(endTime), color, org);
    }

    public void addRelationship(String role,
                                int count,
                                Date startDate,
                                Date endDate,
                                String color,
                                String org) {

        String countStr = "#" + count;
        String startDateStr = formatDateString(startDate);
        String endDateStr = formatDateString(endDate);

        addRelationship(role, countStr, startDateStr, endDateStr, color, org);

    }


    protected synchronized void addRelationship(String role,
                                                String count,
                                                String startDate,
                                                String endDate,
                                                String color,
                                                String org) {

        if (!instanceSet.contains(role)) {
            instanceSet.add(role);
        }
        String[] relationship = new String[RELATIONSHIP_NUM_ITEMS];
        relationship[ROLE_IND] = role;
        relationship[COUNT_IND] = count;
        relationship[START_IND] = startDate;
        relationship[END_IND] = endDate;
        relationship[COLOR_IND] = color;
        relationship[ORG_IND] = org;

        relationshipMapping.add(relationship);
    }

    public Iterator getInstances() {
        ArrayList instanceList = new ArrayList();
        ArrayList sortList = new ArrayList(instanceSet);
        Collections.sort(sortList);
        Iterator instIT = sortList.iterator();
        while (instIT.hasNext()) {
            String role = (String) instIT.next();
            String[] instances = new String[2];
            instances[0] = role;
            instances[1] = role;
            instanceList.add(instances);
        }
        return instanceList.iterator();
    }

    public Iterator getRelationships() {
        return relationshipMapping.iterator();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Source Agent:  " + getSourceAgent() + "\n");
        sb.append("Start Date:  " + new Date(getStartCDay()) + "\n");
        sb.append("\nInstances:");
        Iterator i;
        String[] sa;
        for (i = getInstances(); i.hasNext();) {
            sa = (String[]) i.next();
            sb.append("\n  ");
            for (int j = 0; j < sa.length; j++) {
                sb.append(sa[j]).append(", ");
            }
        }
        sb.append("\nRelatioships:");
        for (i = getRelationships(); i.hasNext();) {
            sa = (String[]) i.next();
            sb.append("\n  ");
            for (int j = 0; j < sa.length; j++) {
                sb.append(sa[j]).append(", ");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        Date now = new Date();
        RelationshipScheduleData aData = new RelationshipScheduleData(7891, "2-501-AVBN");
        System.out.println("The date now in GMT is: |" + aData.formatDateString(now) + "|");
        System.out.println("Adding data now");
        Date bd = new Date();
        try {
            bd = dateFormatter.parse("1960-11-24 16:04:00");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        aData.addRelationship("AviationMaintenanceProvider", 1, bd, now, "red", "CCAD");

    }

}

