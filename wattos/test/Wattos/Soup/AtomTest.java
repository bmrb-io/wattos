/*
 * AtomTest.java
 * JUnit based test
 *
 * Created on December 6, 2005, 9:18 AM
 */

package Wattos.Soup;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.CloneWars.UserInterface;
import Wattos.Database.DBMS;
import Wattos.Utils.DiffPrint;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Strings;

/**
 *
 * @author jurgen
 */
public class AtomTest extends TestCase {
    
    UserInterface ui    = null;        
    DBMS dbms           = null;
    Gumbo gumbo         = null;
    Atom atom           = null;
    File inputDir       = null;
    String wattosRoot   = null;
    
    public AtomTest(String testName) {
        super(testName);
//        General.verbosity = General.verbosityNothing;
        General.verbosity = General.verbosityDebug;
        ui = UserInterface.init(true);
        init();
        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        inputDir = new File( wattosRoot,"data"+File.separator+"test_data" );
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AtomTest.class);        
        return suite;
    }
        
    public boolean init() {
        dbms = ui.dbms;
        gumbo = ui.gumbo;
        atom = gumbo.atom;
        return true;
    }

    public boolean read(String fileName) {
//        PdbFile pdbFile = new PdbFile(dbms);
//        File tmpDir         = new File( System.getProperty("java.io.tmpdir"));        
        String urlName = inputDir + File.separator + fileName;
        URL url = InOut.getUrlFileFromName( urlName );
//        String outputfileName = wattosRoot+File.separator+"tmp_dir"+File.separator+fileName+".out";
        boolean status = gumbo.entry.readPdbFormattedFile( url, AtomMap.NOMENCLATURE_ID_PDB );        
        if ( ! status ) {
            fail("Failed to read the PDB formatted file");
        }
        return true;
    }     

    
    public void testRoundToPdbPrecision() {
        
        //General.verbosity = General.verbosityNothing;
        General.verbosity = General.verbosityDebug;
        
        float[] c = new float[] {123.456789f,0,0};
        General.showDebug("Before rounding: " + PrimitiveArray.toString(c));
        General.showDebug("after          : " + PrimitiveArray.toString(GumboItem.roundToPdbPrecision(c)));
    }
    
    public void testCalcDistance() {
        
        General.verbosity = General.verbosityNothing;
        General.verbosity = General.verbosityDebug;
        
        float cutoffBonds   = 0.1F;
        int[] minModels             = new int[]     { 1,                    10,                   2 };
        boolean[] intraMolecular    = new boolean[] { true,                 true,                 false };
        boolean[] interMolecular    = new boolean[] { true,                 false,                true };
        int[] minResDiff            = new int[]     { 2,                    3,                    1 };
        float[] cutoff              = new float[]   { 3.0F,                 3.0F,                 2.8F };
        String[] fnList             = new String[]  { "1brv_small.pdb",     "1brv.pdb.gz",       "1olh_small.pdb.gz" };
        int[] expBondList           = new int[]     { 264,                  264,                  2812 };
        int[] expDistList           = new int[]     { 149,                  87,                   166 };
        // Note that the expected bonds are the same for the regular and small version 
        // of entry 1brv .
        String[] expBondListStrFile = new String[]  { "1brv_small_bond.txt","1brv_small_bond.txt","1olh_small_bond.txt" };
        String[] expDistListStrFile = new String[]  { "1brv_small_dist.txt","1brv_dist.txt",      "1olh_small_dist.txt" };
        
        int[] testList = new int[] { 0,1,2 };
        //int[] testList = new int[] { 1 };
        int testCount = testList.length;
        for (int it=0;it<testCount;it++) {
            int testId = testList[it];
            String fn = fnList[testId];
            General.showDebug("Reading file: "+ fn);
            if ( ! read(fn)) {
                fail("read pdb file: " + fn);
            }   

            //General.showDebug(dbms.toSTAR());
            // do bonds first in order to be able to exclude them later.
            boolean result = gumbo.entry.calcBond(cutoffBonds);
            assertEquals(true, result);
            int resultBondCount = gumbo.bond.selected.cardinality();
            assertEquals(expBondList[testId],resultBondCount);
            String bondListStr = gumbo.bond.toString(gumbo.bond.used);
            //General.showDebug("Found bonds:\n" + bondListStr);
            String fnBond = inputDir + File.separator + expBondListStrFile[testId];
            String bondListStrExp = InOut.getLines(fnBond,0,99999);
            if ( ! Strings.equalsIgnoreWhiteSpace( bondListStr, bondListStrExp)) {
                if ( ! DiffPrint.printDiff( bondListStr, bondListStrExp)) {
                    fail("DiffPrint.printDiff");
                }
                fail("Bond list representation is not as before.");
            }
            
            
            // do distance
            result = atom.calcDistance(cutoff[testId], minResDiff[testId], minModels[testId], intraMolecular[testId], interMolecular[testId]);
            assertEquals(true, result);
            int resultDistanceCount = gumbo.distance.selected.cardinality();
            assertEquals(expDistList[testId],resultDistanceCount);
            String distListStr = gumbo.distance.toString(gumbo.distance.used,true,cutoff[testId]);
            String fnDist = inputDir + File.separator + expDistListStrFile[testId];
            String distListStrExp = InOut.getLines(fnDist,0,99999);
            if ( ! Strings.equalsIgnoreWhiteSpace( distListStr, distListStrExp)) {
//                if ( ! DiffPrint.printDiff( distListStr, distListStrExp)) {
//                    fail("DiffPrint.printDiff");
//                }
                //fail("Dist list representation is not as before.");
            }
            
            // Init for next read.
            if ( ! ui.commandHub.InitAll()) {
                fail("ui.commandHub.InitAll");
            }
            init();
        }
    }
}
