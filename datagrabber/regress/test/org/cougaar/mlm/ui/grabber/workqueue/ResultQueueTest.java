/*
 * User: tom
 * Date: Aug 9, 2002
 * Time: 9:57:15 AM
 */
package test.org.cougaar.mlm.ui.grabber.workqueue;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.workqueue.ResultQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Result;

public class ResultQueueTest extends TestCase {
    public ResultQueueTest(String name) {
        super(name);
    }

    public void testBasic() {
        ResultQueue rq = new ResultQueue();
        assertTrue(!rq.hasResult());
    }

    public void testAdd() {
        ResultQueue rq = new ResultQueue();
        rq.handleResult(new Result() {
            public int getID() {
                return 1;
            }
        });
        assertTrue(rq.hasResult());
        assertEquals(1, rq.getResult().getID());
    }
}
