/*
 * User: tom
 * Date: Aug 13, 2002
 * Time: 10:46:03 AM
 */
package test.org.cougaar.mlm.ui.grabber.controller;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.controller.WorkGroup;
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;
import org.cougaar.mlm.ui.grabber.workqueue.ResultHandler;
import org.cougaar.mlm.ui.grabber.workqueue.ResultQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

public class WorkGroupTest extends TestCase {


    private static class MyResultHandler implements ResultHandler {
        public void handleResult(Result r) {
        }
    }
    public WorkGroupTest(String name) {
        super(name);
    }

    public void testBasic() {
        WorkGroup grp = new WorkGroup(new WorkQueue(new StdLogger(), new MyResultHandler()), new ResultQueue(new StdLogger()));
        assertTrue(grp.isEmpty());
        assertEquals("", grp.getDesc(1));
    }

    public void testAdd() {
        WorkGroup grp = new WorkGroup(new WorkQueue(new StdLogger(), new MyResultHandler()), new ResultQueue(new StdLogger()));
        grp.add(1, "foo");
        assertEquals("foo", grp.getDesc(1));
    }
}
