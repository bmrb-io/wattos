/*
 * PdbFileReaderTest.java
 * JUnit based test
 *
 * Created on November 30, 2005, 10:04 AM
 */

package Wattos.Soup;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.CloneWars.UserInterface;
import Wattos.Utils.General;
import Wattos.Utils.InOut;

/**
 *
 * @author jurgen
 */
public class PdbFileReaderTest extends TestCase {
    
    public PdbFileReaderTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(PdbFileReaderTest.class);        
        return suite;
    }
/*
    public void testInitConvenienceVariables() {
        PdbFile instance = null;
 
        boolean expResult = true;
        boolean result = instance.initConvenienceVariables();
        assertEquals(expResult, result);
    }
 
    public void testWriteFile() {
        String file_name = "";
        PdbFile instance = null;
 
        boolean expResult = true;
        boolean result = instance.writeFile(file_name);
        assertEquals(expResult, result);
    }
 */
    
    public void testReadFile() {
        //General.verbosity = General.verbosityNothing;
        //General.verbosity = General.verbosityDebug;
        
        String fileNameBase = "1olh_small";
        
        boolean doWrite = true; // don't use in junit testing as it's not necessarily installed.
//        String atomNomenclatureFlavor = "";
//        PdbFile instance = null;
                
        UserInterface ui = UserInterface.init(true);        
//        DBMS dbms = ui.dbms;
        Gumbo gumbo = ui.gumbo;
        Entry entry = gumbo.entry;
        //PdbFile pdbFile = new PdbFile(dbms);
        long time = System.currentTimeMillis();
        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        File inputDir       = new File( wattosRoot,"data"+File.separator+"test_data" );
//        File tmpDir         = new File( System.getProperty("java.io.tmpdir"));        
        String urlName = inputDir + File.separator + fileNameBase + ".pdb.gz";
        URL url = InOut.getUrlFileFromName( urlName );
        File outputDir      = new File( wattosRoot,"tmp_dir" );
        String outputfileName = new File(outputDir,fileNameBase+"_wattos.pdb").toString();
        boolean status = entry.readPdbFormattedFile( url, AtomMap.NOMENCLATURE_ID_PDB );        
        time = System.currentTimeMillis() - time;
        if ( ! status ) {
            fail("Failed to read the PDB formatted file");
        }
        General.showDebug("Time for reading: " + time);
        if ( doWrite ) {
            //General.showDebug( dbms.toString(true));
            time = System.currentTimeMillis();
            Boolean generateStarFileToo = new Boolean(true);
            General.showDebug("Writing file: " + outputfileName);
            status = entry.writePdbFormattedFileSet( outputfileName, generateStarFileToo, ui );
            time = System.currentTimeMillis() - time;
            if ( ! status ) {
                fail("Failed to write the PDB formatted file");
            }
            General.showDebug("Time for writing: " + time);
        }
        // delete it again
        entry.mainRelation.removeRowCascading(0, true);
        // next file to test with is AMBER
        urlName = inputDir + File.separator + "1bau_amber.pdb.gz";
        url = InOut.getUrlFileFromName( urlName ); 
        assertEquals(true, entry.readPdbFormattedFile( url, AtomMap.NOMENCLATURE_ID_PDB ));                
    }
    
    
    public void testTranslateAtomNameToPdb() {
        String inputName = "HD2'";
        String expResult = "'HD2";
        String result = PdbFile.translateAtomNameToPdb(inputName);
        assertEquals(expResult, result);
    }
    
    
    public void testTranslateAtomNameFromPdb() {
        String atomName = "'H5'";
        char[] buf = atomName.toCharArray();        
        PdbFile.translateAtomNameFromPdb( buf, 0 );
        String atomNameNew = new String( buf );
        
        if ( ! atomNameNew.equals("H5''")) {
            General.showDebug("Name before translateAtomName: [" + atomName + "]");
            General.showDebug("Name after  translateAtomName: [" + atomNameNew + "]");
            fail( "Expected [H5''] but got: [" + atomNameNew + "]");
        }
    }
    
    /*
    public void testTranslateResidueNameFromPDB() {
        char[] buf = null;
        int startIdx = 0;
     
        PdbFile.translateResidueNameFromPDB(buf, startIdx);
    }
     
    public void testTranslateAtomCharge() {
        char[] buf = null;
        int startIdx = 0;
     
        float expResult = 0.0F;
        float result = PdbFile.translateAtomCharge(buf, startIdx);
        assertEquals(expResult, result);
    }
     */
}
