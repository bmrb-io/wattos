/*
 * STARJoinTest.java
 * JUnit based test
 *
 * Created on March 1, 2006, 11:07 AM
 */

package Wattos.Star;

import junit.framework.*;
import Wattos.Utils.*;
import java.io.File;

/**
 *
 * @author jurgen
 */
public class STARJoinTest extends TestCase {
    
    public STARJoinTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(STARJoinTest.class);
        
        return suite;
    }

    /**
     * Test of filter method, of class Wattos.Star.STARFilter.
     */
    public void testJoin() {
        General.verbosity = General.verbosityDebug;
        //General.verbosity = General.verbosityNothing;
        String fs = File.separator;
        String baseInputName = "1brv_DOCR_small";
        //String baseInputName = "1brv_DOCR";
        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        File inputDir       = new File( wattosRoot,"data"+fs+"test_data" );
        File outputDir      = new File( wattosRoot,"tmp_dir" );
        File inputFile1     = new File( inputDir,baseInputName + ".str.gz" );
        File inputFile2     = new File( inputDir,baseInputName + ".str.gz" );
        File outputFile     = new File( outputDir,baseInputName + "_joined.str" );
        
        //String inputFileStr = "http://www.bmrb.wisc.edu/data_library/files/bmr4020.str";
        String inputFile1Str = inputFile1.toString();
        String inputFile2Str = inputFile2.toString();
        String outputFileStr = outputFile.toString();
        String[] args = { inputFile1Str, inputFile2Str, outputFileStr };
        long start = System.currentTimeMillis();
        boolean result = new STARJoin().join(args);
        long taken = System.currentTimeMillis() - start;
        General.showDebug("Join took  : " + taken + "(" + (taken/1000.0) + " sec)" );        
        assertEquals(true, result);        
    }
}
