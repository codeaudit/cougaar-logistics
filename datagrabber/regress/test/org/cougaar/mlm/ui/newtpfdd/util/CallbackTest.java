/*
 * User: tom
 * Date: Aug 13, 2002
 * Time: 11:18:35 AM
 */
package test.org.cougaar.mlm.ui.newtpfdd.util;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.newtpfdd.util.Callback;

public class CallbackTest extends TestCase {

    private boolean fooCalled;

    public CallbackTest(String name) {
        super(name);
    }

    public void testBasic() throws Throwable {
        fooCalled = false;
        Callback cb = new Callback(this, this.getClass().getDeclaredMethod("foo", new Class[] {Object.class}));
        assertEquals(this, cb.getTarget());
        assertEquals(this.getClass().getDeclaredMethod("foo", new Class[] {Object.class}), cb.getMethod());
        cb.execute(null);
        assertTrue(fooCalled);
    }

    public void foo(Object o) {
        fooCalled = true;
    }
}
