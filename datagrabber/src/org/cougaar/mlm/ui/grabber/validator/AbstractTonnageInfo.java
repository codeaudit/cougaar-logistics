/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 2:41:10 PM
 */
package org.cougaar.mlm.ui.grabber.validator;

import org.cougaar.mlm.ui.grabber.config.DBConfig;

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

    protected String getRawTableName(){
      return (showUnit?"Unit":"")+(showClass?"Class":"")+(showType?"Type":"")+
        (showLocation?"Location":"")+"AvgTonnageInfo";
    }

    public int failureLevel(){
      return RESULT_INFO;
    }

}
