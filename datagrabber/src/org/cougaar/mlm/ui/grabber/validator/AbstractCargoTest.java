/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 3:07:13 PM
 */
package org.cougaar.mlm.ui.grabber.validator;

import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Statement;
import java.sql.SQLException;

public abstract class AbstractCargoTest extends Test {
    public AbstractCargoTest(DBConfig dbConfig) {
        super(dbConfig);
    }

    protected void constructTable(Logger l, Statement s, int run)
      throws SQLException{
      createTable(s, run);
      insertResults(l,s,run);
    }
    protected abstract void createTable(Statement s, int run) throws SQLException;
    protected abstract void insertResults (Logger l, Statement s, int run);

    public int failureLevel(){
      return RESULT_INFO;
    }

    public boolean showMultipleGraphs () { return false; }
    public int getXAxisColumn () { return 1; }
    public int getZAxisColumn () { return -1; }
    public boolean hasThirdDimension () { return false; }
}
