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
