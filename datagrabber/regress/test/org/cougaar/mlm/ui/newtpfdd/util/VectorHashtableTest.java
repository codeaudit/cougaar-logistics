/*
 * User: tom
 * Date: Aug 13, 2002
 * Time: 11:28:37 AM
 */
package test.org.cougaar.mlm.ui.newtpfdd.util;

import junit.framework.TestCase;
import org.cougaar.mlm.ui.newtpfdd.util.VectorHashtable;

import java.util.Vector;

public class VectorHashtableTest extends TestCase {
    public VectorHashtableTest(String name) {
        super(name);
    }

    public void testBasic() {
        VectorHashtable vh = new VectorHashtable();
        vh.put("1", "a");
        vh.put("1", "b");
        Vector v = vh.vectorGet("1");
        assertEquals(2, v.size());
        assertEquals("a", v.get(0));
        assertEquals("b", v.get(1));
    }

    public void testRemove() {
        VectorHashtable vh = new VectorHashtable();
        vh.put("1", "a");
        vh.put("1", "b");
        vh.findAndRemove("1");
        assertNull(vh.vectorGet("1"));
    }

    public void testGetWithCreate() {
        VectorHashtable vh = new VectorHashtable();
        assertTrue(vh.vectorGetWithCreate("1").isEmpty());
    }

    public void testVectorPut() {
        VectorHashtable vh = new VectorHashtable();
        assertNull(vh.vectorPut("1", "a"));
    }
}
