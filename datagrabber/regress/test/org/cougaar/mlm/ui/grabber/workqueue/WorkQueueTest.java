/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
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
