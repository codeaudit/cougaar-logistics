/*
 * User: tom
 * Date: Aug 15, 2002
 * Time: 5:09:04 PM
 */
package test.org.cougaar.mlm.ui.grabber.webserver;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.grabber.webserver.RequestHandlerFactory;
import org.cougaar.mlm.ui.grabber.webserver.HttpRequest;
import org.cougaar.mlm.ui.grabber.webserver.RequestHandler;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.logger.IDLogger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
import org.cougaar.mlm.ui.grabber.config.DataGrabberConfig;

public class RequestHandlerFactoryTest extends TestCase {

    public RequestHandlerFactoryTest(String name) {
        super(name);
    }

    public void testBasic() throws Throwable {
        Controller c = new Controller(new StdLogger(), new DataGrabberConfig());
        RequestHandlerFactory rhf = new RequestHandlerFactory(c);
        RequestHandler rh = rhf.getRequestHandler(null, null, null, new HttpRequest(0, MockSocket.createSocketWith("GET /index.html HTTP/1.0")));
        assertEquals(RequestHandlerFactory.ErrorRequestHandler.class, rh.getClass());
    }
}
