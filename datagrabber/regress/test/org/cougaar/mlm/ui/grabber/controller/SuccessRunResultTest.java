/*
 * User: tom
 * Date: Aug 13, 2002
 * Time: 10:58:18 AM
 */
package test.org.cougaar.mlm.ui.grabber.controller;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

public class SuccessRunResultTest extends TestCase {

    public SuccessRunResultTest(String name) {
        super(name);
    }

    public void testBasic() {
        SuccessRunResult srr = new SuccessRunResult(1, 2);
        assertEquals(1, srr.getID());
        assertEquals(2, srr.getRunID());
    }

}
