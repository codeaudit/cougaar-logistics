/*
 * User: tom
 * Date: Aug 9, 2002
 * Time: 8:15:02 AM
 */
package test.org.cougaar.mlm.ui.grabber.workqueue;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
import org.cougaar.mlm.ui.grabber.logger.Logger;
/*
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
*/

public class WorkQueueTest extends TestCase {

    public WorkQueueTest(String name) {
        super(name);
    }

    public void testBasic() {
        WorkQueue wq = new WorkQueue(new StdLogger(), null);
        assertTrue(wq.getWorkIDToStatusMap().isEmpty());
        assertEquals(0, wq.getNumActiveWork());
        assertTrue(!wq.isBusy());
        assertEquals(0, wq.numActiveThreads());
        assertEquals(0, wq.numThreads());
        assertEquals(0, wq.numInactiveThreads());
    }

    public void testID() {
        WorkQueue wq = new WorkQueue(new StdLogger(), null);
        int first = wq.getValidID();
        assertTrue(wq.getValidID() > first);
    }

    public void testWork() {
        WorkQueue wq = new WorkQueue(new StdLogger(), null);
        final int id = wq.getValidID();
        wq.enque(new Work() {
            public int getID() {
                return id;
            }

            public String getStatus() {
                return "test";
            }

            public void halt() {
            }

            public Result perform(Logger l) {
                try {
                    Thread.currentThread().sleep(2000);
                } catch (Exception e) {}
                return null;
            }
        });
        assertTrue(wq.isBusy());
        assertTrue(wq.getWorkIDToStatusMap().containsKey(new Integer("1")));
        assertEquals(1, wq.numActiveThreads());
    }
}
