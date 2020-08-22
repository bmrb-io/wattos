/*
 * MoleculeTest.java
 * JUnit based test
 *
 * Created on March 20, 2006, 3:09 PM
 */

package Wattos.Soup;

import junit.framework.*;
import Wattos.Utils.*;

/**
 *
 * @author jurgen
 */
public class MoleculeTest extends TestCase {
    
    public MoleculeTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(MoleculeTest.class);
        
        return suite;
    }

    /**
     * Test of toChain method, of class Wattos.Soup.Molecule.
     */
    public void testToChain() {
        //System.out.println("toChain");
        General.verbosity = General.verbosityDebug;
//        int i = 28;
        int i = 1;
        
//        String expResult = "AB";
        String expResult = "A";
        String result = Molecule.toChain(i);
        assertEquals(expResult, result);
    }
}
