/*
 * AtomLibTest.java
 * JUnit based test
 *
 * Created on March 21, 2006, 9:58 AM
 */

package Wattos.Soup;

import junit.framework.*;
import Wattos.Utils.*;

/**
 *
 * @author jurgen
 */
public class AtomLibAmberTest extends TestCase {
    
    public AtomLibAmberTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AtomLibAmberTest.class);
        
        return suite;
    }

    /**
     * Test of main method, of class Wattos.Soup.AtomLib.
     */
    public void testMain() {
        General.setVerbosityToDebug();
        AtomLibAmber am = new AtomLibAmber();                
        assertEquals(true,am.readStarFile( null ));
        General.showDebug("Done with AtomLibAmber map parsing.");
    }    
}
