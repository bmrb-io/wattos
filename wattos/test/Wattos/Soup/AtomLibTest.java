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
public class AtomLibTest extends TestCase {
    
    public AtomLibTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AtomLibTest.class);
        
        return suite;
    }

    /**
     * Test of main method, of class Wattos.Soup.AtomLib.
     */
    public void testMain() {
        General.setVerbosityToDebug();
        AtomLib am = new AtomLib();                
        assertEquals(true,am.readStarFile( null ));
        General.showDebug("Done with AtomLib map parsing.");
    }    
}