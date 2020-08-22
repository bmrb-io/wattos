/*
 * File31Test.java
 * JUnit based test
 *
 * Created on November 30, 2005, 11:24 AM
 */

package Wattos.Star.NMRStar;

import java.io.File;
import java.net.URL;
import java.util.BitSet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.CloneWars.UserInterface;
import Wattos.Database.DBMS;
import Wattos.Soup.Gumbo;
import Wattos.Soup.Constraint.AssignStereo;
import Wattos.Soup.Constraint.Completeness;
import Wattos.Soup.Constraint.CompletenessLib;
import Wattos.Soup.Constraint.Constr;
import Wattos.Soup.Constraint.DistConstrList;
import Wattos.Soup.Constraint.Surplus;
import Wattos.Utils.DiffPrint;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Strings;

/**
 *
 * @author jurgen
 */
public class File31Test extends TestCase {

    String fs = File.separator;
    // Below parameters are true by default.
    //Note that the checks are dependent so e.g. for writeStar to be successful some (assume all) need to be done.
    boolean calcDist            = false;
    boolean calcCdih            = true;
    boolean getSurplus          = false;
    boolean doAssignment        = false;
    boolean doCompletenessCheck = false;
    boolean doClassification    = false;
    boolean sort                = false;
    boolean showTables          = false;
    boolean writeStar           = true;
    //int avg_method              = DistConstrList.DEFAULT_AVERAGING_METHOD_CENTER;
    int avg_method              = DistConstrList.DEFAULT_AVERAGING_METHOD_SUM;
    int monomers                = 1; // e.g. set to 2 for a symmetric dimer.

//    String baseInputName = "2hgh_wim_small_2007-05-25";
//    String baseInputName = "1ai0_rem_small_out";
//    String baseInputName = "2hgh_rem_small_out";
//    String baseInputName = "2hgh_rest_chris";
//    String baseInputName = "2knr_DOCR_small"; TODO:
    String baseInputName = "2kwb_DOCR_small";
//    String baseInputName = "2kwb_DOCR";
//    String baseInputName = "1brv_DOCR_small";
//    String baseInputName = "2hgh-nmrif_small";

    String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
    File inputDir       = new File( wattosRoot,"data"+fs+"test_data" );
    File outputDir      = new File( wattosRoot,"tmp_dir" );
    String outputFileName = baseInputName + "_out.str.gz";
    File outputFile     = new File( outputDir,outputFileName );
    UserInterface ui = UserInterface.init(true);
    DBMS dbms = ui.dbms;
    Gumbo gumbo = ui.gumbo;
    Constr constr   = ui.constr;
    boolean status = true;
    long start;
    long taken;

    public File31Test(String testName) {
        super(testName);
        // Select to show no output if all goes well because the routine in normal mode has
        // to produce output. Set to debug when unit testing shows a bug.
        General.setVerbosityToDebug();
//        General.verbosity = General.verbosityNothing;
        //General.showEnvironment();

        General.showDebug("wattos root: " + wattosRoot);
        General.showDebug("inputDir: " + inputDir);

        if ( gumbo == null ) {
            fail("gumbo from ui still null");
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(File31Test.class);
        return suite;
    }

    public void test() {
        File input = new File(inputDir, baseInputName+".str.gz");
        URL url = InOut.getUrlFileFromName(input.toString());
        if ( url == null ) {
            fail("specify a valid name for input");
        }
        General.showDebug("url: " + url);
        start = System.currentTimeMillis();
        boolean doEntry                             = true;     // default true
        boolean doRestraints                        = true;     // default true
        boolean matchRestraints2Soup                = true;     // default true
        boolean matchRestraints2SoupByAuthorDetails = false;    // default false
        boolean removeUnlinkedRestraints            = true;     // default true
        boolean syncModels                          = true;     // default true

        status = gumbo.entry.readNmrStarFormattedFile(url,null,ui,doEntry,
                doRestraints,matchRestraints2Soup,matchRestraints2SoupByAuthorDetails,
                removeUnlinkedRestraints,syncModels);
        //status = file.toWattos(url);
        taken = System.currentTimeMillis() - start;
        General.showDebug( "to Wattos took: " + taken + "(" + (taken/1000.0) + " sec)" );
        //General.showMemoryUsed();
        if ( ! status ) {
            fail("Failed to convert nmr star 3.1 file to wattos.");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after reading in file.");
        } else {
            General.showDebug("DBMS is consistent after reading in file.");
        }

//        if ( false)
//            return;

        if ( calcDist && (!doCalcDist())) {
            fail("doCalcDist");
        }
        if ( calcCdih && (!doCalcCdih())) {
            fail("doCalcCdih");
        }
        if ( getSurplus && (!getSurplus())) {
            fail("getSurplus");
        }
        if ( doAssignment && (!doAssignment())) {
            fail("doAssignment");
        }
        if ( doCompletenessCheck && (!doCompletenessCheck())) {
            fail("doCompletenessCheck");
        }
        if ( doClassification && (!doClassification())) {
            fail("doClassification");
        }
        if ( sort && (!sort())) {
            fail("sort");
        }
        if ( showTables && (!showTables(gumbo, constr))) {
            fail("showTables");
        }
        if ( writeStar && (!writeStar())) {
            fail("writeStar");
        }
        return;
    }

    public boolean doCalcDist() {
        float   max_viol_report     = 0.1f;
        int violationCountExpected  = 44;
        String violListFileName = "1brv_DOCR_small_viol0_1.txt";
        violListFileName = inputDir + File.separator + violListFileName;
        // no changes below in block.
        if ( ! constr.dc.calcViolation() ) {
            fail("failed to calculate distances");
        }
        General.showDebug("Calculated distances");
        //General.showDebug( "Produced atoms:                 "       + gumbo.atom.mainRelation.toString() );
        //General.showDebug( "Produced constraints          : "       + constr.dc.mainRelation.toString() );
        BitSet sel = constr.dc.getSelectionWithViolation( max_viol_report );
        if ( sel == null ) {
            fail("Failed to select constraints with violations above: " + max_viol_report );
        }
        int violationCount = sel.cardinality();
        General.showDebug("From selected constraints numbered: " + constr.dc.used.cardinality() +
                " there are the following number of constraints with violations above the threshold: " + max_viol_report +
                " namely: " + violationCount);
        assertEquals(violationCountExpected, violationCount);
        // Show only the selected ones.
        String msg = constr.dc.toString(sel,true,false);
        String exp = InOut.getLines(violListFileName,0,99999);
        msg = Strings.dos2unix(msg);
        exp = Strings.dos2unix(exp);

//        General.showDebug( "Produced constraint violations: "       +  General.eol + msg);
        if ( ! msg.equals( exp)) {
            if ( ! DiffPrint.printDiff( msg, exp )) {
                fail("DiffPrint.printDiff");
            }
            fail("Violations representation is not as before.");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after distance violation analysis.");
        } else {
            General.showDebug("DBMS is consistent after distance violation analysis.");
        }
        return true;
    }

    public boolean doCalcCdih() {
        General.showDebug("Starting File31Test# doCalcCdih.");
        File violListFileName = new File(outputDir, "xxxx_DOCR_cdih_viol.txt");

        if ( ! ui.constr.cdih.calcSimpleConstraintViolation(5.0f, violListFileName.toString())) {
            General.showError("Failed CalcDihConstraintViolation");
            return false;
        }

        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after dihedral violation analysis.");
        } else {
            General.showDebug("DBMS is consistent after dihedral violation analysis.");
        }
        General.showDebug("Done File31Test# doCalcCdih.");
        return true;
    }


    public boolean getSurplus() {
        File refFile               = new File( inputDir,  baseInputName + "_surplus.txt");// reference
        File summaryAssignBase     = new File( outputDir, baseInputName ); // to generate

        int surplusCountExpected  = 24;
        // no changes below in block.

        //General.showDebug("minimum distance in redundantlib is: " + RedundantLib.LOWER_DISTANCE_MINIMUM);
        Surplus surplus = new Surplus(ui);
        float thresholdRedundancy           = Surplus.THRESHOLD_REDUNDANCY_DEFAULT; // percentage
        boolean updateOriginalConstraints   = false;  // e.g. when an impossible target distance is found reset it to null.
        boolean onlyFilterFixed             = false; // DON'T CHANGE for redundancy check outside completeness check you don't want this.
        boolean append                      = false;
        boolean writeNonRedundant           = true;
        boolean writeRedundant              = true;
        boolean removeSurplus               = false;
        String fileNameBase                 = summaryAssignBase.toString();
        BitSet sel = surplus.getSelectionSurplus(
                constr.dc.used,
                thresholdRedundancy,
                updateOriginalConstraints,
                onlyFilterFixed,
                avg_method,
                monomers,
                fileNameBase,
                append,
                writeNonRedundant,
                writeRedundant,
                removeSurplus
                );
        if ( sel == null ) {
            fail("Surplus analysis");
        }
        int surplusCount  = sel.cardinality();
        String msg = constr.dc.toString(sel,false,true);
        String exp = InOut.getLines(refFile.toString(),0,99999);
        msg = Strings.dos2unix(msg);
        exp = Strings.dos2unix(exp);

//        General.showDebug( "Found constraint surplus:"       +  General.eol + msg);
        assertEquals(surplusCountExpected, surplusCount);
         if ( ! msg.equals( exp)) {
            if ( ! DiffPrint.printDiff( msg, exp)) {
                fail("DiffPrint.printDiff");
            }
            fail("Surplus representation is not as before.");
         }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after surplus analysis.");
        } else {
            General.showDebug("DBMS is consistent after surplus analysis.");
        }
        return true;
    }

    public boolean doCompletenessCheck() {
        File summaryFileNameCompleteness = new File(baseInputName + "_sumCompl.str");
        File refFile                     = new File(inputDir,  summaryFileNameCompleteness.toString()); // reference
        summaryFileNameCompleteness      = new File(outputDir, summaryFileNameCompleteness.toString());
        File file_name_base_dc           = new File(outputDir, baseInputName);

        Completeness completeness = new Completeness(ui);
        float max_dist_expectedOverall= 4f;
        float min_dist_observed      = 2f;
        float max_dist_observed      = 6f;
        int   numb_shells_observed   = 8;
        float min_dist_expected      = 2;
        float max_dist_expected      = 5f;
        int   numb_shells_expected   = 6;
        float avg_power_models       = 1.0f;

//        boolean double_count    = false;
        boolean use_intra       = false;
        boolean write_dc_lists  = false;
        String ob_file_name = CompletenessLib.STR_FILE_NAME;
        status = completeness.doCompletenessCheck(
                max_dist_expectedOverall,
                min_dist_observed,
                max_dist_observed,
                numb_shells_observed,
                min_dist_expected,
                max_dist_expected,
                numb_shells_expected,
                avg_power_models,
                avg_method,
                monomers,
                use_intra,
                ob_file_name,
                summaryFileNameCompleteness.toString(),
                write_dc_lists,
                file_name_base_dc.toString()
                );
        if ( status ) {
            General.showDebug("Done with the completeness check");
        } else {
            fail("Failed the completeness check");
        }
        String msg = InOut.getLines(summaryFileNameCompleteness.toString(),0,99999);
        String exp = InOut.getLines(refFile.toString(),0,99999);
        msg = Strings.dos2unix(msg);
        exp = Strings.dos2unix(exp);
        if ( ! msg.equals( exp)) {
            if ( ! DiffPrint.printDiff( msg, exp)) {
                fail("DiffPrint.printDiff");
            }
            fail("Completeness representation in STAR file is not as before.");
        }

        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after Completeness check.");
        } else {
            General.showDebug("DBMS is consistent after Completeness check.");
        }
        return true;
    }

    public boolean doAssignment() {
        File summaryFileNameAssign  = new File( outputDir, baseInputName + "_sumAssign.str" ); // to generate
        File listFile               = new File( inputDir,   "1brv_DOCR_small_sumAssign.str");// reference
        // no changes below in block.

        //General.showDebug("minimum distance in redundantlib is: " + RedundantLib.LOWER_DISTANCE_MINIMUM);
        AssignStereo assignStereo = new AssignStereo(ui);
        float energy_abs_criterium                          = 0.1f;
        float energy_rel_criterium                          = 0f;
        float model_criterium                               = 50f;
        float single_model_violation_deassign_criterium     = 0.5f;
        float multi_model_violation_deassign_criterium      = 0.3f;
        float multi_model_rel_violation_deassign_criterium  = 49f;
        status = assignStereo.doAssignStereo(
                energy_abs_criterium,
                energy_rel_criterium,
                model_criterium,
                single_model_violation_deassign_criterium,
                multi_model_violation_deassign_criterium,
                multi_model_rel_violation_deassign_criterium,
                summaryFileNameAssign.toString()
                );
        assertEquals(true, status);
        String msg = InOut.getLines(summaryFileNameAssign.toString(),0,99999); // produced
        String exp = InOut.getLines(listFile.toString(),0,99999);          // expected
        msg = Strings.dos2unix(msg);
        exp = Strings.dos2unix(exp);
        if ( ! exp.equals(msg)) {
            if ( ! DiffPrint.printDiff( msg, exp)) {
                fail("DiffPrint.printDiff");
            }
            fail("Assignment representation is not as before.");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after AssignStereo analysis.");
        } else {
            General.showDebug("DBMS is consistent after AssignStereo analysis.");
        }
        return true;
    }

    public boolean doClassification() {
        if ( ! constr.dc.getClassification(constr.dc.selected)) {
            fail("Failed to get classification");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after Classification.");
        } else {
            General.showDebug("DBMS is consistent after Classification.");
        }
        return true;
    }

    public boolean sort() {
        //constr.dc.order( constr.dc.selected );
        if ( ! constr.dc.sortAll( constr.dc.selected, 0)) {
            fail( "Failed to do sort.");
        } else {
            General.showDebug( "Sorting done.");
        }
        if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            fail("DBMS is NOT consistent after Sorting.");
        } else {
            General.showDebug("DBMS is consistent after Sorting.");
        }
        return true;
    }



    /**
     * gumbo.atom.mainRelation.removeRow(1,false);
     * General.showOutput("Removed atom 1 (rid 2) from main atom relation.");
     */

    /**
     * BitSet rowSet = (BitSet) gumbo.model.mainRelation.used.clone();
     * rowSet.clear(1);
     * rowSet.clear(0);
     * gumbo.model.mainRelation.removeRowsCascading(rowSet, false );
     * General.showOutput("Removed (cascading) all model from main model relation: " + PrimitiveArray.toString(rowSet));
     */


    /**
     * status = file.toWattos(url);
     * if ( ! status ) {
     * General.showError("Failed to convert nmr star 3.0 file to wattos for the second time.");
     * System.exit(1);
     * }
     */

    /** Remove some models
     * start = System.currentTimeMillis();
     * BitSet rowSet = (BitSet) gumbo.model.mainRelation.used.clone();
     * rowSet.clear(0);
     * //rowSet.clear(1);
     * rowSet.clear(2);
     * gumbo.model.mainRelation.removeRowsCascading(rowSet, true);
     * /** Remove some residues
     * rowSet = (BitSet) gumbo.res.mainRelation.used.clone();
     * rowSet.clear(0);
     * //rowSet.clear(1);
     * rowSet.clear(2);
     * gumbo.res.mainRelation.removeRowsCascading(rowSet, true);
     * taken = System.currentTimeMillis() - start;
     * General.showDebug( "removing unwanted junk took: " + taken + "(" + (taken/1000.0) + " sec)" );
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

        return true;
    }

    public static boolean showTables(Gumbo gumbo, Constr constr ) {
        /**
         * String outputFileName2 = "1b4c_clean_mol_system_out2.str";
         * if ( Wattos.Gobbler.Converters.FormatNMRStarExternal.convert( outputFileName, outputFileName2) != 0 ) {
         * General.showError("Failed to convert TO nmr star 3.0 file FROM wattos.");
         * }
         */

        /**
         * General.showDebug( "Produced entries: "     + gumbo.entry.mainRelation.toString() );
         * General.showDebug( "Produced models: "      + gumbo.model.mainRelation.toString() );
         * General.showDebug( "Produced mols: "        + gumbo.mol.mainRelation.toString() );
         * General.showDebug( "Produced residues: "    + gumbo.res.mainRelation.toString() );
         */
        //General.showDebug( "Produced atoms: "       + gumbo.atom.mainRelation.toString() );

        //General.showDebug( "Produced dc atom: "       + constr.dc.distConstrAtom.toString() );
        //General.showDebug( "Produced dc memb: "       + constr.dc.distConstrMemb.toString() );
        //General.showDebug( "Produced dc node: "       + constr.dc.distConstrNode.toString() );
        //General.showDebug( "Produced dc: "            + constr.dc.mainRelation.toString() );
        General.showDebug( "Produced dc list: "       + constr.dcList.mainRelation.toString() );
        return true;
    }
}
