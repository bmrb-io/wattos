/*
 * GeneralTest.java
 * JUnit based test
 * Run like: java junit.textui.TestRunner test Wattos.Utils.GeneralTest
 *
 * Created on March 1, 2006, 9:55 AM
 */

package Wattos.Utils;

import junit.framework.*;

/**
 *
 * @author jurgen
 */
public class GeneralTest extends TestCase {

    public GeneralTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(GeneralTest.class);
        return suite;
    }

    /**
     * Test of showMemoryUsed method, of class Wattos.Utils.General.
     */
    public void testShowMemoryUsed() {
        // System.out.println("showMemoryUsed");
        General.verbosity = General.verbosityDebug;
        // General.showMemoryUsed();
        // General.showEnvironment();
        String startMsg = General.getStartMessage();
        this.assertTrue(startMsg.length() > 10);
        General.showOutput("start msg:\n" + startMsg);
        int svnRevision = General.getSvnRevision();
        General.showOutput("SVN revision: " + svnRevision);
        this.assertTrue(svnRevision > 100);
        // TODO review the generated test code and remove the default call to
        // fail.
        // fail("The test case is a prototype.");
    }

}
