/*
 * MRSTARFileTest.java
 * JUnit based test
 *
 * Created on February 2, 2006, 12:00 PM
 */

package Wattos.Episode_II;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import EDU.bmrb.starlibj.StarFileNode;
import EDU.bmrb.starlibj.StarParser;
import Wattos.Database.DBMS;
import Wattos.Star.StarFileReader;
import Wattos.Star.NMRStar.File31;
import Wattos.Utils.General;
import Wattos.Utils.InOut;

/**
 *
 * @author jurgen
 */
public class MRSTARFileTest extends TestCase {
    
    public MRSTARFileTest(String testName) {
        super(testName); 
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(MRSTARFileTest.class);
//        General.verbosity = General.verbosityNothing;
        General.setVerbosityToDebug();
        return suite;
    }
    
    /**
     * Test of main method, of class Wattos.Episode_II.MRSTARFile.
     * NOTE THAT THE CODE WILL PRODUCE THE FOLLOWING ERROR MESSAGES:
     *"ERROR: please check the input. An error was recorded."
     * AS PART OF THE TESTING PROCEDURE
     */
    public void tttestInit() {
        String fs = File.separator;

        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        File inputDir       = new File( wattosRoot,"data"+fs+"test_data" );
        File outputDir      = new File( wattosRoot,"tmp_dir" );
        General.showDebug("inputDir: " + inputDir);
        
        String[][] args = {
                {"MR format",    "comment",         "n/a",    "n/a",         null, null},                
                {"DYANA/DIANA",  "dipolar coupling","n/a",   "n/a",          null, null},
                {"DYANA/DIANA",  "distance",        "NOE",   "simple",       null, null},
                {"DYANA/DIANA",  "dihedral angle",  "n/a",   "n/a",          null, null},
            
                {"DYANA/DIANA",  "distance",       "NOE",    "simple",       null, null},
                {"DISCOVER",     "dihedral angle", "n/a",    "n/a",          null, null},
                
                {"XPLOR/CNS",    "distance",       "NOE",    "ambi",         null, null},
                {"XPLOR/CNS",    "dihedral angle", "n/a",    "n/a",          null, null},
                {"XPLOR/CNS",    "dipolar coupling","n/a",   "n/a",          null, null},
                
                {"AMBER",        "dihedral angle", "n/a",    "n/a",          null, null},
                {"AMBER",        "distance",       "hydrogen bond","ambi",   null, null},
                {"AMBER",        "dipolar coupling","n/a",   "n/a",          null, null},

                {"DISCOVER",     "distance",       "NOE",    "ambi",         null, null},
                {"DISCOVER",     "dihedral angle", "n/a",    "n/a",          null, null},
//                {"DISCOVER",     "dipolar coupling","n/a",   "n/a",          null, null}, //1ykg contains only discover rdcs

                {"EMBOSS",       "distance",       "NOE",    "simple",       null, null},
                {"EMBOSS",       "dihedral angle", "n/a",    "n/a",          null, null},
//                {"DISCOVER",     "dipolar coupling","n/a",   "n/a",          null, null}, //1ykg contains only discover rdcs

        };
        // Parallel to args
        String[] pdb_id = { 
                "2hgh", // cyana
                "2hgh",
                "2hgh",
                "2hgh",
                
                "1wix", 
                "1brv",
                
                "1cjg", // xplor
                "1bbx",
                "2bjc",
                
                "2hou", // amber
                "2hou",
                "1kr8",

                "1ykg", // discover
                "1ykg",
//                "1kr8",

                "1iv6", // emboss
                "1iv6",
//                "1kr8",
                
        };
        // Parallel to args
        int j = 0;
        String[] fi = {
                pdb_id[j++]+"_general_comment.txt", 
                pdb_id[j++]+"_dyana_rdc.txt", 
                pdb_id[j++]+"_dyana_dis.txt", 
                pdb_id[j++]+"_dyana_dih.txt", 
                
                pdb_id[j++]+"_noe_small.txt", 
                pdb_id[j++]+"_dih.txt",
                
                pdb_id[j++]+"_noe_ambi_xplor.txt",     // xplor
                pdb_id[j++]+"_dih_xplor.tbl",                
                pdb_id[j++]+"_dipolar_xplor.tbl",                

                pdb_id[j++]+"_dih.txt",                // amber
                pdb_id[j++]+"_dis.txt",                
                pdb_id[j++]+"_dip.txt",                

                pdb_id[j++]+"_distance_discover.txt",                // discover
                pdb_id[j++]+"_dihedral_discover.txt",                
//                pdb_id[j++]+"_dip.txt",                

                pdb_id[j++]+"_distance_emboss.txt",                // emboss
                pdb_id[j++]+"_dihedral_emboss.txt",                
//                pdb_id[j++]+"_dip.txt",                
};
        
        // Parallel to args
        int[][] statusExp = {
                {0,0},      // comments
                {0,98},     // 2hgh rdc; includes an error
                {0,33},     // 2hgh dis; includes an error
                {0,24},     // 2hgh dih; includes an error
                
                {0,5},      // 1wix distance
                {0,29},     // 1brv dihedral
                
                {0,122},    // 1cjg distance xplor
                {0,7},      // 1bbx dihedral
                {0,1},      // 2bjc dipolar
                
                {0,66},     // 2hou dihedral amber
                {0,52},     // 2hou distance
                {0,2},      // 1kr8 dipolar

                {0,8},     // 1ykg distance discover
                {0,3},     // 1ykg dihedral 
//                {0,2},      // 1kr8 dipolar

                {0,3},     // 1iv6 distance emboss
                {0,2},     // 1iv6 dihedral 
//                {0,2},      // 1iv6 dipolar
        };
        
        int startTestIdx = 12;   // test to start with;    
        int countTest = 1;      // tests todo;   
        boolean doAllTests = false;
        if ( doAllTests ) {
            startTestIdx = 0;
            countTest = statusExp.length;
        }
        
        int endTestIdx = startTestIdx + countTest;
        for (int i=startTestIdx;i<endTestIdx;i++) {
            File fileIn  = new File(inputDir,fi[i]);
            MRSTARFile mrf = new MRSTARFile();
            // READ
            General.showDebug("Parsing file: " + fileIn.toString());
            args[i][4] = pdb_id[i];
            args[i][5] = fileIn.toString();
            int[] status = mrf.read(args[i]);           
            assertEquals(statusExp[i][0],status[0]); // exit status 0 is success
            assertEquals(statusExp[i][1],status[1]); // # restraint parsed
            // WRITE
            String fileBase = InOut.getFilenameBase(fileIn);            
            File fileOut = new File( outputDir, fileBase+".str");
//            General.showDebug("Writing file: " + fileOut.toString());
            if ( ! mrf.write( fileOut.toString())) {
                fail("Writing the NMR-STAR file.");
            }        
        }
    }
        
    public void testInit2() {
        // Do more post processing using Wattos API to STAR files because it's
        // easier to extend. Takes a back and forth trip but that's ok.
        //TO WATTOS API
        String buf = "data_test    save_testing  loop_ _Study.ID 1 stop_   save_";
        DBMS dbms = new DBMS();
        StarFileReader sfr = new StarFileReader(dbms); 
        Wattos.Star.StarNode sn = sfr.parse( buf );
        assertTrue(sn!=null);
                
        
        //FROM WATTOS API
        for (int i=0;i<2;i++) {
            buf = sn.toSTAR();
//            General.showDebug("Parsed: "+sn.toSTAR());
            BufferedReader br = new BufferedReader(new StringReader(buf));
            try {
                StarParser.ReInit(br); //not needed unless called again.
                StarParser.StarFileNodeParse(Wattos.Star.StarGeneral.sp);
                // perhaps better to call from STATIC class but this works.
            } catch (Throwable t) {
                General.showThrowable(t);
                fail();
            }
            StarFileNode sfn = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();
            assertTrue(sfn!=null);
        }
    }        

    
    public void tttestEnterStandardIDs() {
        General.setVerbosityToDebug();
        String buf = "data_test "+
        
        "save_testing  "+
        "_Study_list.Sf_category  study_list " +
        "loop_ _Study.Name yyy stop_ "+
        "save_ "+
        
        "save_entry_information  "+
        "_Entry.Sf_category  entry_information " +
        "loop_ _Related_entries.Database_name PDB stop_ "+
        "save_";
                     ;
        DBMS dbms = new DBMS();
        StarFileReader sfr = new StarFileReader(dbms); 
        Wattos.Star.StarNode sn = sfr.parse( buf );
        assertTrue(sn!=null);                             
        assertTrue(File31.enterStandardIDs(sn, "XXX"));
//        General.showDebug("After enterStandardIDs: "+sn.toSTAR());
    }   
    
    
}