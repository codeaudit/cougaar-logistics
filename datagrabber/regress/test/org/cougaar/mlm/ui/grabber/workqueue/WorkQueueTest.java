/*
 * User: tom
 * Date: Aug 9, 2002
 * Time: 8:15:02 AM
 */
package test.org.cougaar.mlm.ui.grabber.workqueue;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;

public class WorkQueueTest extends TestCase {

    public WorkQueueTest(String name) {
        super(name);
    }

    public void testBasic() {
        WorkQueue wq = new WorkQueue(null, null);
        assertTrue(wq.getWorkIDToStatusMap().isEmpty());
        assertEquals(0, wq.getNumActiveWork());
        assertTrue(!wq.isBusy());
        assertEquals(0, wq.numActiveThreads());
        assertEquals(0, wq.numThreads());
        assertEquals(0, wq.numInactiveThreads());
    }

    public void testID() {
        WorkQueue wq = new WorkQueue(null, null);
        int first = wq.getValidID();
        assertTrue(wq.getValidID() > first);
    }


}
