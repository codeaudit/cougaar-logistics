/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * ShortfallSummary is an object that summarizizes which inventories of
 * a particular inventory have shortfall.
 */

public class ShortfallInventory implements java.io.Serializable, Publishable {


    private String invID;
    private String unitOfIssue;
    private int numDemandSupply = 0;
    private int numResupplySupply = 0;
    private int numDemandProj = 0;
    private int numResupplyProj = 0;
    private int numTempResupplySupply = 0;
    private int numTempDemandSupply = 0;
    private int numTempResupplyProj = 0;
    private int numTempDemandProj = 0;


    private boolean unexpected = true;

    private ArrayList shortfallPeriods;


    /**
     * Constructor
     *
     * @param anInvID String identifier of the inventory (item/NSN)
     * @param uoi     The Unit of Issue for this Shortfall Inventory
     */


    public ShortfallInventory(String anInvID,
                              String uoi) {
        invID = anInvID;
        unitOfIssue = uoi;
        shortfallPeriods = new ArrayList();
    }

    /**
     * The inventory id for this inventory.
     *
     * @return The identifier for this inventory
     * @see org.cougaar.logistics.servlet.LogisticsInventoryServlet>>getNomenclature(inv);
     */
    public String getInvID() {
        return invID;
    }

    /**
     * The unit of issue for the asset contained in this inventory
     *
     * @return String the unit of issue for the asset of this inventory.
     */
    public String getUnitOfIssue() {
        return unitOfIssue;
    }


    /**
     * The number of demand supply tasks asked for by customers that had some form of shortfall. Shortfall being
     * an allocation result that does not deliver the requested amount of demand at the time asked.
     *
     * @return int - The number of demand tasks asked for by customers that had some form of shortfall
     */

    public int getNumDemandSupply() {
        return numDemandSupply;
    }

    /**
     * The number of resupply supply tasks asked of the supplier that has some form of shortfall.  Shortfall being
     * an allocation result the amounts to not delivering the requested amount of refill at the time requested.
     *
     * @return int - The number of resupply supply tasks asked of the supplier that has some form of shortfall
     */
    public int getNumResupplySupply() {
        return numResupplySupply;
    }

    /**
     * The number of demand project supply tasks asked for by customers that had some form of shortfall
     *
     * @return int The number of demand project supply tasks asked for by customers that had some form of shortfall
     */
    public int getNumDemandProj() {
        return numDemandProj;
    }

    /**
     * The number of refill project supply tasks asked for of supplier that has some form of shortfall
     *
     * @return int The number of refill project supply tasks asked for of supplier that has some form of shortfall
     */
    public int getNumResupplyProj() {
        return numResupplyProj;
    }

    /**
     * The total number of demand tasks (both supply and project supply) that had some form of shortfall
     *
     * @return int The total number of demand tasks (both supply and project supply) that had some form of shortfall
     */
    public int getNumDemand() {
        return numDemandSupply + numDemandProj;
    }

    /**
     * The total number of refill tasks requested from supplier (both actuals and projections) that had some form of shortfall
     *
     * @return int The total number of refill tasks requested from supplier (both actuals and projections) that had some form of shortfall
     */
    public int getNumRefill() {
        return numResupplyProj + numResupplySupply;
    }

    /**
     * The total number of projections demand and refill that have some form of shortfall
     *
     * @return int The total number of projections demand and refill that have some form of shortfall
     */
    public int getNumProjection() {
        return numDemandProj + numResupplyProj;
    }

    /**
     * The total number of actual supply tasks demand and refill that have some form of shortfall
     *
     * @return int The total number of actual supply tasks demand and refill that have some form of shortfall
     */
    public int getNumActual() {
        return numDemandSupply + numResupplySupply;
    }

    /**
     * The total number of refill supply tasks that weren't filled on time, but eventually do get completely filled -
     * thus temporary shortfall
     *
     * @return int The total number of refill supply tasks that eventually do get completely filled and thus temporary shortfall
     */
    public int getNumTempResupplySupply() {
        return numTempResupplySupply;
    }

    /**
     * Get the total number of demand supply tasks that do not get filled on time, but eventually do get completely filled
     *
     * @return int - the total number of demand supply tasks that do not get filled on time, but eventually do get completely filled
     */
    public int getNumTempDemandSupply() {
        return numTempDemandSupply;
    }

    /**
     * Get the total number of refill projections that do not get filled on time, but eventually do get completely filled.
     *
     * @return int - the total number of refill projections that do not get filled on time, but eventually do get completely filled.
     */
    public int getNumTempResupplyProj() {
        return numTempResupplyProj;
    }

    /**
     * Get the total number of demand projections that do not get filled on time, but eventually do get completely filled.
     *
     * @return int - the total number of demand projections that do not get filled on time, but eventually do get completely filled.
     */
    public int getNumTempDemandProj() {
        return numTempDemandProj;
    }

    /**
     * Get the total number of all tasks that contribute to permanent shortfall equivilent usually to failed tasks.
     *
     * @return - the total number of all tasks that contribute to permanent shortfall equivilent usually to failed tasks.
     */
    public int getNumPermShortfall() {
        return (getNumTotalShortfall() -
                getNumTempShortfall());
    }

    /**
     * Get the total number of all tasks that contribute to temporary shortfall.  IE tasks that have shortfall but recover.
     *
     * @return the total number of all tasks that contribute to temporary shortfall.  IE tasks that have shortfall but recover.
     */
    public int getNumTempShortfall() {
        return (getNumTempResupplySupply() +
                getNumTempDemandSupply() +
                getNumTempResupplyProj() +
                getNumTempDemandProj());
    }

    /**
     * Get the total number of tasks that contain shortfall ie the request for an amount is not met on time.
     *
     * @return int - the total number of tasks that contain shortfall ie the request for an amount is not met on time.
     */
    public int getNumTotalShortfall() {
        return ((getNumDemandSupply() +
                getNumResupplySupply() +
                getNumDemandProj() +
                getNumResupplyProj()));
    }

    /**
     * Filled in by shortfall servlet.   This is whether or not there is shortfall expected at this org/inventory after
     * the rules of discounting are applied.   IE Shortfall of projections in the UA are expected shortfall and therefore discounted.
     *
     * @return boolean true if unexpected shortfall exists at this inventory.
     */
    public boolean getUnexpected() {
        return unexpected;
    }


    /**
     * Below are the mutators for the above numbers.  Most are set in the LogisticsInventoryBG and UALogisticsBG.
     * The setUnexpected mutator is called in the servlet.data.shortfall methods in ShortfallShortData and LatShortfallShortData.
     */

    public void setUnexpected(boolean isUnexpected) {
        unexpected = isUnexpected;
    }

    public void setNumDemandSupply(int numDemandSupply) {
        this.numDemandSupply = numDemandSupply;
    }

    public void setNumResupplySupply(int numResupplySupply) {
        this.numResupplySupply = numResupplySupply;
    }

    public void setNumDemandProj(int numDemandProj) {
        this.numDemandProj = numDemandProj;
    }

    public void setNumResupplyProj(int numResupplyProj) {
        this.numResupplyProj = numResupplyProj;
    }

    public void setNumTempDemandSupply(int numDemandTemp) {
        this.numTempDemandSupply = numDemandTemp;
    }

    public void setNumTempResupplySupply(int numResupplyTemp) {
        this.numTempResupplySupply = numResupplyTemp;
    }

    public void setNumTempDemandProj(int numDemandTemp) {
        this.numTempDemandProj = numDemandTemp;
    }

    public void setNumTempResupplyProj(int numResupplyTemp) {
        this.numTempResupplyProj = numResupplyTemp;
    }

    public void addShortfallPeriod(ShortfallPeriod aPeriod) {
        this.shortfallPeriods.add(aPeriod);
    }

    public ArrayList getShortfallPeriods() {
        return shortfallPeriods;
    }

    /**
     * Return the max percent shortfall qty of all the shortfall periods.  This percent shortfall is the max of all
     * totalDemandQty-totalFilledQty/totalDemandQty * 100 percentiles.
     *
     * @return int the max percent shortfall of all the shortfall periods.
     */
    public int getMaxPercentShortfall() {
        int maxShortfall = 0;
        Iterator periodsIt = shortfallPeriods.iterator();
        while (periodsIt.hasNext()) {
            ShortfallPeriod period = (ShortfallPeriod) periodsIt.next();
            maxShortfall = Math.max(maxShortfall,
                    ((int) period.getPercentShortfall()));
        }
        return maxShortfall;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(getInvID());
        sb.append("\nUnitOfIssue=" + getUnitOfIssue());
        sb.append("\nNumDemand=" + getNumDemand());
        sb.append(",NumResupplySupply=" + getNumResupplySupply());
        sb.append("\nNumDemandProj=" + getNumDemandProj());
        sb.append(",NumResupplyProj=" + getNumResupplyProj());
        sb.append(",NumTempDemandSupply=" + getNumTempDemandSupply());
        sb.append(",NumTempResupplySupply=" + getNumTempResupplySupply());
        sb.append(",NumTempDemandProj=" + getNumTempDemandProj());
        sb.append(",NumTempResupplyProj=" + getNumTempResupplyProj());
        sb.append("\nNumPerm=" + getNumPermShortfall());
        return sb.toString();
    }

    /**
     * Determines equality of shortfall inventory by comparing all the slot and shortfall periods.  If all equivlent then
     * these two shortall inventories are equal.
     *
     * @param si The compared to ShortfallInvenotry
     * @return true if this ShortfallInvenotry and passed on are equivilent
     */
    public boolean equals(ShortfallInventory si) {
        return ((this.getInvID().equals(si.getInvID())) &&
                (this.getNumDemand() == si.getNumDemand()) &&
                (this.getNumResupplySupply() == si.getNumResupplySupply()) &&
                (this.getNumDemandProj() == si.getNumDemandProj()) &&
                (this.getNumResupplyProj() == si.getNumResupplyProj()) &&
                (this.getNumTempDemandSupply() == si.getNumTempDemandSupply()) &&
                (this.getNumTempResupplySupply() == si.getNumTempResupplySupply()) &&
                (this.getNumTempDemandProj() == si.getNumTempDemandProj()) &&
                (this.getNumTempResupplyProj() == si.getNumTempResupplyProj()) &&
                (hasEqualShortfallPeriods(si.getShortfallPeriods())));
    }

    /**
     * Helper function to compare shortfall periods in a shortfall inventory
     *
     * @param otherShortfallPeriods - shortfall periods in the compared with shortfall inventory
     * @return true if they are all equivilent.
     */
    public boolean hasEqualShortfallPeriods(ArrayList otherShortfallPeriods) {
        Iterator myPeriods = getShortfallPeriods().iterator();
        outer_loop: while (myPeriods.hasNext()) {
            ShortfallPeriod myPeriod = (ShortfallPeriod) myPeriods.next();
            Iterator otherPeriods = otherShortfallPeriods.iterator();
            boolean found = false;
            while (otherPeriods.hasNext()) {
                ShortfallPeriod otherPeriod = (ShortfallPeriod) otherPeriods.next();
                if (myPeriod.equals(otherPeriod)) {
                    continue outer_loop;
                }
            }
            return false;
        }
        return true;
    }

    public boolean isPersistable() {
        return true;
    }
}
