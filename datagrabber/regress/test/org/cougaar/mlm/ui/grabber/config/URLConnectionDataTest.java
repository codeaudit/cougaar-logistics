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
 * Date: Aug 22, 2002
 * Time: 12:53:29 PM
 */
package test.org.cougaar.mlm.ui.grabber.config;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;

public class URLConnectionDataTest extends TestCase  {
    public URLConnectionDataTest(String name) {
        super(name);
    }

    public void testConstructor1() {
        URLConnectionData ucd = new URLConnectionData("http", "host", 80, "cluster", "pkg", "id", true, true, 10);
        assertEquals("http", ucd.getProtocol());
        assertEquals("host", ucd.getHost());
        assertEquals(80, ucd.getPort());
        assertEquals("cluster", ucd.getClusterName());
        assertEquals(10, ucd.getTimeout());
    }

    public void testConstructor2() {
        URLConnectionData ucd = new URLConnectionData("host", 80, "cluster", "pkg", "id", true, true, 10);
        assertEquals("http", ucd.getProtocol());
        assertEquals("host", ucd.getHost());
        assertEquals(80, ucd.getPort());
        assertEquals("cluster", ucd.getClusterName());
        assertEquals(10, ucd.getTimeout());
    }
}
