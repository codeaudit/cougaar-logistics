/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
