/*
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
public class PseudoLibTest extends TestCase {

    public PseudoLibTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PseudoLibTest.class);

        return suite;
    }

    public void testInit() {
        //General.verbosity = General.verbosityNothing;
        //General.verbosity = General.verbosityDebug;

        AtomNomenLib instance = new AtomNomenLib();

        if (! instance.readStarFile( null )) {
            General.showError(" in AtomNomenLib.main found:");
            General.showError(" reading AtomNomenLib star file.");
            System.exit(1);
        }
        General.showDebug(" read AtomNomenLib star file.");

//        if ( false ) {
////            General.showDebug("Found -1- : " + instance.fromAtoms.get("PHE", "HD1"));
////            General.showDebug("Found:-2- : " + instance.toAtoms.get("PHE", "QD"));
////            General.showDebug("Found:-3- : " + instance.pseudoAtomType.get("PHE", "QD"));
//        }

       General.showDebug("Done with AtomNomenLib parsing.");
    }
}
