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

    public void testBasic2() {
        SuccessRunResult srr = new SuccessRunResult(1, 2, true, "foo");
        assertEquals(1, srr.getID());
        assertEquals(2, srr.getRunID());
        assertTrue(srr.getWarning());
        assertEquals("foo", srr.getReason());
    }

}
