/*
 * User: tom
 * Date: Aug 9, 2002
 * Time: 10:02:35 AM
 */
package test.org.cougaar.mlm.ui.grabber.webserver;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.webserver.HttpRequest;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

import java.net.Socket;
import java.net.URLEncoder;
import java.io.*;

public class HttpRequestTest  extends TestCase {

    private static class MySocket extends Socket {

        private InputStream stream;

        public MySocket(InputStream stream) {
            this.stream = stream;
        }

        public InputStream getInputStream() {
            return stream;
        }
    }

    public HttpRequestTest(String name) {
        super(name);
    }

    public void testBasic() throws Throwable {
        HttpRequest h = new HttpRequest(1, new MySocket(null));
        assertEquals(1, h.getID());
        assertEquals(0, h.getCommand());
        assertNull(h.getTarget());
        assertNull(h.getVersion());
    }

    public void testCmd1() throws Throwable {
        HttpRequest h = new HttpRequest(1, createSocketWith("GET / HTTP/1.0"));
        h.readHeader(new StdLogger());
        assertEquals(1, h.getCommand());
    }

    public void testCmd2() throws Throwable {
        HttpRequest h = new HttpRequest(1, createSocketWith("HEAD / HTTP/1.0"));
        h.readHeader(new StdLogger());
        assertEquals(2, h.getCommand());
        assertEquals("HTTP/1.0", h.getVersion());
    }

    public void testVer1() throws Throwable {
        HttpRequest h = new HttpRequest(1, createSocketWith("HEAD / HTTP/1.0"));
        h.readHeader(new StdLogger());
        assertEquals("HTTP/1.0", h.getVersion());
    }

    public void testTarget() throws Throwable {
        HttpRequest h = new HttpRequest(1, createSocketWith("GET /foo?baz=bif HTTP/1.0"));
        h.readHeader(new StdLogger());
        assertEquals("/foo?baz=bif", h.getTarget());
    }

    public void testParam()  throws Throwable {
        // params are passed via HTTP headers
        HttpRequest h = new HttpRequest(1, createSocketWith("GET /foo HTTP/1.0" + getCRLF() + "baz:1"+ getCRLF() + "bif:2"));
        h.readHeader(new StdLogger());
        assertEquals("1", h.getParameter("baz"));
        assertEquals("2", h.getParameter("bif"));
    }

    private MySocket createSocketWith(String data) {
        return new MySocket(new ByteArrayInputStream((data + getCRLF()+ getCRLF()).getBytes()));
    }

    private String getCRLF() {
        return System.getProperty("line.separator");
    }

}
