/*
 * File31Test.java
 * JUnit based test
 *
 * Created on November 30, 2005, 11:24 AM
 */

package Wattos.Star.MmCif;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.CloneWars.UserInterface;
import Wattos.Database.DBMS;
import Wattos.Soup.Gumbo;
import Wattos.Utils.General;
import Wattos.Utils.InOut;

/**
 *
 * @author jurgen
 */
public class CIFCoordTest extends TestCase {

    String fs = File.separator;
    /** This entry is a beauty to test with. It consists of:
     * NON-SYMMETRIC INSULIN (dimeric) HEXAMER (total of 12 chains)
     *  FORMUL  13   ZN    2(ZN1 2+)     2 asym IDs
        FORMUL  14  IPH    6(C6 H6 O1)
        FORMUL  15  HOH   *2(H2 O1)      1 asym ID!
In remediated mmCIF: 21 asyms (chains)
        A N N 1 ?
        B N N 2 ?
        C N N 1 ?
        D N N 2 ?
        E N N 1 ?
        F N N 2 ?
        G N N 1 ?
        H N N 2 ?
        I N N 1 ?
        J N N 2 ?
        K N N 1 ?
        L N N 2 ? # last of peptides
        M N N 3 ?
        N N N 3 ? # last of Zn
        O N N 4 ?
        P N N 4 ?
        Q N N 4 ?
        R N N 4 ?
        S N N 4 ?
        T N N 4 ? # last of IPH
        U N N 5 ? # water     */
//    String pdb_id = "1ai0";
    String pdb_id = "1j6t"; // Used straight up (only 3 models).
//    String pdb_id = "2hgh";
    String baseInputName = pdb_id;
//    String baseInputName = pdb_id + "_rem_small";
    String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
    File inputDir       = new File( wattosRoot,"data"+fs+"test_data" );
    File outputDir      = new File( wattosRoot,"tmp_dir" );
    String outputFileName = baseInputName + "_out.str";
    File outputFile     = new File( outputDir,outputFileName );
    UserInterface ui = UserInterface.init(true);
    DBMS dbms = ui.dbms;
    Gumbo gumbo = ui.gumbo;
//    Constr constr   = ui.constr;
    boolean status = true;
    long start;
    long taken;

    public CIFCoordTest(String testName) {
        super(testName);
        // Select to show no output if all goes well because the routine in normal mode has
        // to produce output. Set to debug when unit testing shows a bug.
        General.setVerbosityToDebug();
//        General.verbosity = General.verbosityNothing;
        //General.showEnvironment();

        General.showDebug("wattos roottie: " + wattosRoot);
        General.showDebug("inputDir: " + inputDir);

        if ( gumbo == null ) {
            fail("gumbo from ui still null");
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CIFCoordTest.class);
        return suite;
    }

    public void test() {
        File input = new File(inputDir, baseInputName+".cif.gz");
        URL url = InOut.getUrlFileFromName(input.toString());
        if ( url == null ) {
            fail("specify a valid name for input");
        }
        start = System.currentTimeMillis();
        boolean syncModels = true;
        status = gumbo.entry.readmmCIFFormattedFile(url,ui,syncModels);
        taken = System.currentTimeMillis() - start;
        General.showDebug( "to Wattos took: " + taken + "(" + (taken/1000.0) + " sec)" );
        if ( ! status ) {
            fail("Failed to convert mmcif file to wattos.");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after reading in file.");
        } else {
            General.showDebug("DBMS is consistent after reading in file.");
        }
        if ( !writeStar()) {
            fail("writeStar");
        }
        return;
    }

    /**
     */
    public boolean writeStar() {
        start = System.currentTimeMillis();
        if ( ! gumbo.entry.writeNmrStarFormattedFileSet( outputFile.toString(), null, ui) ) {
            fail("Failed to convert TO nmr star 3.0 file FROM wattos.");
        }
        taken = System.currentTimeMillis() - start;
        General.showDebug( "to STAR took: " + taken + "(" + (taken/1000.0) + " sec)" );

//        File listFile               = new File( inputDir,   outputFileName);// reference
//        String msg = InOut.getLines(outputFile.toString(),0,99999);         // produced
//        String exp = InOut.getLines(listFile.toString(),0,99999);           // expected
//        msg = Strings.dos2unix(msg);
//        exp = Strings.dos2unix(exp);
//        if ( ! Strings.equalsIgnoreWhiteSpace(exp,msg)) {
//            if ( ! DiffPrint.printDiff( msg, exp)) {
//                fail("DiffPrint.printDiff");
//            }
//            fail("Output STAR representation is not as before.");
//        }
//
        return true;
    }


}
