/*
 * User: tom
 * Date: Aug 15, 2002
 * Time: 5:13:52 PM
 */
package test.org.cougaar.mlm.ui.grabber.webserver;

import java.net.Socket;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public class MockSocket extends Socket {
    private InputStream stream;

    public MockSocket(InputStream stream) {
        this.stream = stream;
    }

    public InputStream getInputStream() {
        return stream;
    }

    public static MockSocket createSocketWith(String data) {
        return new MockSocket(new ByteArrayInputStream((data + getCRLF()+ getCRLF()).getBytes()));
    }


    public static String getCRLF() {
        return System.getProperty("line.separator");
    }
}
