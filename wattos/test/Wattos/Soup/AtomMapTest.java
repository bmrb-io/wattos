/*
 * AtomMapTest.java
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
public class AtomMapTest extends TestCase {
    
    public AtomMapTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AtomMapTest.class);
        
        return suite;
    }

    /**
     * Test of main method, of class Wattos.Soup.AtomMap.
     */
    public void testMain() {
        General.setVerbosityToDebug();
        AtomMap am = new AtomMap();                
        assertEquals(true,am.readStarFile( null ));
        General.showDebug("Done with AtomMap map parsing, serializing and deserializing.");
        String atomName = (String) am.fromIUPAC.get(AtomMap.NOMENCLATURE_ID_XPLOR_IUPAC, "PHE", "QD" );
        assertEquals("HD#",atomName);
    }    
}
