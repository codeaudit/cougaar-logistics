/*
 * User: tom
 * Date: Aug 20, 2002
 * Time: 1:46:58 PM
 */
package test.org.cougaar.mlm.ui.grabber.controller;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;

public class FailureRunResultTest extends TestCase {

    public FailureRunResultTest(String name) {
        super(name);
    }

    public void test1() {
        FailureRunResult frr = new FailureRunResult(1, 2, "foo", true);
        assertEquals(1, frr.getID());
        assertEquals(2, frr.getRunID());
        assertEquals("foo", frr.getReason());
        assertTrue(frr.getError());
    }
}
