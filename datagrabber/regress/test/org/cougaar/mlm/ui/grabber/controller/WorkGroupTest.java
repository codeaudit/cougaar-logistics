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
        assertEquals(0, grp.size());
    }

    public void testAddRemove() {
        WorkGroup grp = new WorkGroup(new WorkQueue(new StdLogger(), new MyResultHandler()), new ResultQueue(new StdLogger()));
        grp.add(1, "foo");
        assertEquals("foo", grp.getDesc(1));
        assertEquals(1, grp.size());
        grp.remove(1);
        assertEquals(0, grp.size());
    }
}
