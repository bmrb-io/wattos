/*
 * STARFilterTest.java
 * JUnit based test
 *
 * Created on March 1, 2006, 11:07 AM
 */

package Wattos.Star;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.Utils.General;
import Wattos.Utils.InOut;

/**
 *
 * @author jurgen
 */
public class STARFilterTest extends TestCase {
    
    String fs = File.separator;
    String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
    File inputDir       = new File( wattosRoot,"data"+fs+"test_data" );
    File outputDir      = new File( wattosRoot,"tmp_dir" );
    
    public STARFilterTest(String testName) {
        super(testName);
        General.verbosity = General.verbosityDebug;
        //General.verbosity = General.verbosityNothing;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(STARFilterTest.class);
        
        return suite;
    }

    /**
     * Test of filter method, of class Wattos.Star.STARFilter.
     */
    public void testFilter() {
        String baseInputName = "1brv_DOCR_small";
        //String baseInputName = "1brv_DOCR";
        File inputFile      = new File( inputDir, baseInputName + ".str.gz" );
        File outputFile     = new File( outputDir,baseInputName + "_out.str" );
        File filterFile     = new File( inputDir, "filter_rules.str" );
        
//        String inputFileStr = "M:\\jurgen\\DOCR_big_tmp_\\link\\1q56\\1q56_full.str";
        //String inputFileStr = "http://www.bmrb.wisc.edu/data_library/files/bmr4020.str";
        String inputFileStr = inputFile.toString();
        String outputFileStr = outputFile.toString();
        String filterFileStr = filterFile.toString();
        String[] args = { inputFileStr, outputFileStr, filterFileStr};
//        String[] args = { inputFileStr, outputFileStr, "."};
        long start = System.currentTimeMillis();
        new STARFilter().filter(args);
        long taken = System.currentTimeMillis() - start;
        General.showDebug("Filter took  : " + taken + "(" + (taken/1000.0) + " sec)" );        
//        assertEquals(true, result);        
    }
    
    
//    public void testStarSingleRowTTest() {
//        General.setVerbosityToDebug();        
//        File inputFile      = new File( inputDir, "star_single_row_tt_test.str" );
//        File outputFile     = new File( outputDir,"star_single_row_tt_test_out.str" );        
//        String inputFileStr = inputFile.toString();
//        String outputFileStr = outputFile.toString();
//        String[] args = { inputFileStr, outputFileStr, "."};        
//        new STARFilter().filter(args);
//        String in = InOut.getLines(inputFileStr,0,99999);
//        String ou = InOut.getLines(outputFileStr,0,99999);
//        if ( ! Strings.equalsIgnoreWhiteSpace( in, ou)) {
//            if ( ! DiffPrint.printDiff( in, ou)) {
//                fail("DiffPrint.printDiff");
//            }
//            fail("star single row tt test representation is not as before.");
//        }
//        
//    }   
    
}
