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
