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
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.IOException;

public class HttpRequestTest  extends TestCase {

    private static class MyInputStream extends InputStream {

        private String data;
        private InputStream stream;

        public void setData(String data) {
            this.data = data;
            stream = new StringBufferInputStream(data);
        }

        public int read() throws IOException {
            return stream.read();
        }

        public int read(byte[] b) throws IOException {
            return stream.read(b);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return stream.read(b, off, len);
        }

        public int available() throws IOException {
            return stream.available();
        }

        public boolean markSupported()  {
            return stream.markSupported();
        }

        public void mark(int lim) {
            stream.mark(lim);
        }

        public void reset() throws IOException {
            stream.reset();
        }

        public long skip(long n) throws IOException {
            return stream.skip(n);
        }

        public void close() throws IOException {
            stream.close();
        }

    }
    private static class MySocket extends Socket {

        private InputStream stream;

        public MySocket() {
            stream = new MyInputStream();
        }

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
        HttpRequest h = new HttpRequest(1, new MySocket());
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

    private MySocket createSocketWith(String data) {
        MyInputStream stream = new MyInputStream();
        stream.setData(data + System.getProperty("line.separator")+ System.getProperty("line.separator"));
        MySocket socket = new MySocket(stream);
        return socket;
    }

}
