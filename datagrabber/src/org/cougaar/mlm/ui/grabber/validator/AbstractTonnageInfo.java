/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 2:41:10 PM
 */
package org.cougaar.mlm.ui.grabber.validator;

import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Statement;
import java.sql.SQLException;

public abstract class AbstractTonnageInfo extends Test {
    public final String COL_TONNAGE = "Tonnage";
    public final String COL_CLASS = "Class";
    public final String COL_TYPE = "Type";
    public final String COL_UNIT = "Unit";
    public final String COL_LOCATION = "Location";

    protected boolean showUnit;
    protected boolean showClass;
    protected boolean showType;
    protected boolean showLocation;

    public AbstractTonnageInfo(DBConfig dbConfig) {
        super(dbConfig);
    }

    protected void init(boolean showUnit, boolean showClass, boolean showType, boolean showLocation) {
        this.showUnit = showUnit;
        this.showClass = showClass;
        this.showType = showType;
        this.showLocation = showLocation;
    }

    public String getDescription(String prefix){
      return prefix +(showUnit?"Unit":"")+
        (showClass?"Class":"")+(showType?"Type":"")+(showLocation?"Location":"");
    }

    protected String getRawTableName(){
      return (showUnit?"Unit":"")+(showClass?"Class":"")+(showType?"Type":"")+
        (showLocation?"Location":"")+"AvgTonnageInfo";
    }

    public int failureLevel(){
      return RESULT_INFO;
    }

    /**Actually do the query and build the table**/
    protected void constructTable(Logger l, Statement s, int run)
      throws SQLException{
      createTable(s, run);
      insertResults(l,s,run);
    }

    protected abstract void createTable(Statement s, int run) throws SQLException;
    protected abstract void insertResults (Logger l, Statement s, int run);

}
