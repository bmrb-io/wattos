/*
 * PseudoLibTest.java
 * JUnit based test
 *
 * Created on January 10, 2006, 11:18 AM
 */

package Wattos.Soup;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.Utils.General;

/**
 *
 * @author jurgen
 */
public class AtomNomenLibTest extends TestCase {
    
    public AtomNomenLibTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AtomNomenLibTest.class);        
        return suite;
    }

    public void testInit() {
//        General.verbosity = General.verbosityNothing;
        General.verbosity = General.verbosityDebug;
        AtomNomenLib instance = new AtomNomenLib();
        assertTrue(instance.readStarFile( null ));

        General.showDebug(" read AtomNomenLib star file.");
        General.showDebug(instance.toString());
        General.showDebug("Done with AtomNomenLib parsing.");
    }    
}
