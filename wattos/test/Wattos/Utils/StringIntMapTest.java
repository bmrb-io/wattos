/*
 * StringIntMapTest.java
 * JUnit based test
 *
 * Created on November 22, 2005, 2:49 PM
 */

package Wattos.Utils;

import junit.framework.*;

/**
 *
 * @author jurgen
 */
public class StringIntMapTest extends TestCase {
    
    public StringIntMapTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(StringIntMapTest.class);
        
        return suite;
    }

    public void testSum() {
        StringIntMap instance = new StringIntMap();
        
        instance.addString("a",1);
        instance.addString("b",2);
        int expResult = 3;
        int result = instance.sum();
        General.showDebug("Result is: " + result + "(expected 3)");
        assertEquals(expResult, result);
    }    
}

