/*
 * StarGeneralTest.java
 * JUnit based test
 *
 * Created on January 31, 2006, 11:22 AM
 */

package Wattos.Star;

import junit.framework.*;
import Wattos.Database.*;
import Wattos.Utils.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author jurgen
 */
public class StarGeneralTest extends TestCase {

    public StarGeneralTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Test of init method, of class Wattos.Star.StarGeneral.
     */
    public void testInit() {
        General.setVerbosityToDebug();
        General.showDebug("Starting tests" );
        DBMS dbms = new DBMS();
//        assertEquals( doExample_datablocks( dbms ), true);
//        assertEquals( doExample_tagtable( dbms ), true );
//        assertEquals( doExample_starfilereader( dbms ), true);
        assertEquals( doExample_starfilereader2( dbms ), true);
        General.showDebug("Done with all tests" );
    }

    public static boolean doExample_datablocks( DBMS dbms ) {
        StarNode topNode = new StarNode();
        DataBlock db = new DataBlock();
        db.title = "test";
        topNode.datanodes.add(db);
        General.showDebug("Data block is: " + topNode.toSTAR() );
        return true;
    }


    public static boolean doExample_tagtable( DBMS dbms ) {
        int rowIdx;

        TagTable tT = null;
        try {
            tT = new TagTable("test table", dbms);
        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }
        tT.isFree = false;
        //String label_A = "_Saveframe_category";
        String label_A = "_File_characteristics.Sf_category";
        String label_B = "_abracadabra";
        tT.insertColumn( label_A, TagTable.DATA_TYPE_STRING, null);
        tT.insertColumn( label_B, TagTable.DATA_TYPE_STRING, null);

        int[] column_order = (int[]) tT.getColumn( TagTable.DEFAULT_ATTRIBUTE_ORDER_ID );
        String[] column_A = (String[]) tT.getColumn( label_A );
        String[] column_B = (String[]) tT.getColumn( label_B );

        rowIdx = tT.getNewRowId();
        column_order[rowIdx] = 0;
        column_A[rowIdx] = "A";
        column_B[rowIdx] = "Abracadabra";
        rowIdx = tT.getNewRowId();
        column_order[rowIdx] = 1;
        column_A[rowIdx] = "my thoughts exactly";
        column_B[rowIdx] = "not ### a \"\" comment";

        String tempje = tT.toSTAR();
        General.showDebug("Tagtable now: [\n" + tempje + "\n]" );

        String regExp = "(_Saveframe_category)|(_.+\\.Sf_category)";
        //String regExp = "\\.+ra";
        int i = tT.getColumnIdForRegExp(regExp);
        if ( i >= 0 ) {
            General.showDebug("Found a column");
        } else {
            General.showDebug("Found NO column");
        }


        return true;
    }

    /** Read a CIF file into STAR tagtables and all; then
     *map the items in the largest table (coordinates) to native types.
     */
    public static boolean doExample_starfilereader( DBMS dbms ) {

        String fileName     = "1brv_DOCR_small.str.gz";
        String fileNameOut  = "1brv_DOCR_wattos.str";
        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        File inputDir       = new File( wattosRoot,"data"+File.separator+"test_data" );
        String urlName      = inputDir + File.separator + fileName;
        URL    url          = InOut.getUrlFileFromName(urlName);
        File tmpDir         = new File( System.getProperty("java.io.tmpdir"));
        String outputfileName = tmpDir.toString()+File.separator+fileNameOut;

        StarFileReader sfr = new StarFileReader(dbms);
        General.showMemoryUsed();
        // parse
        long start = System.currentTimeMillis();
        StarNode sn = sfr.parse( url );
        long taken = System.currentTimeMillis() - start;
        General.showDebug("STARLexer: " + taken + "(" + (taken/1000.0) + " sec)" );
        //General.showMemoryUsed();
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        General.showDebug("Parse successful");
        General.showMemoryUsed();


        // data type modifications on column
        start = System.currentTimeMillis();

        ArrayList tTList = (ArrayList) sn.getTagTableList(StarGeneral.WILDCARD, StarGeneral.WILDCARD, StarGeneral.WILDCARD, "_Atom_site.Model_ID");
        if ( tTList == null ) {
            General.showError("Expected a match but none found");
            return false;
        }
        if ( tTList.size() != 1 ) {
            General.showError("Expected exactly 1 match but found: " + tTList.size() );
            return false;
        }
        TagTable tT = (TagTable) tTList.get(0);
        tT.convertDataTypeColumn("_Atom_site.Cartn_x",               TagTable.DATA_TYPE_FLOAT, null );
        tT.convertDataTypeColumn("_Atom_site.Cartn_y",               TagTable.DATA_TYPE_FLOAT, null );
        tT.convertDataTypeColumn("_Atom_site.Cartn_z",               TagTable.DATA_TYPE_FLOAT, null );

        tT.convertDataTypeColumn("_Atom_site.ID",                       TagTable.DATA_TYPE_INT, null );
        tT.convertDataTypeColumn("_Atom_site.Label_comp_index_ID",      TagTable.DATA_TYPE_INT, null );

        if ( tT.containsColumn( "_Atom_site.Model_ID" ) ) {
            tT.convertDataTypeColumn("_Atom_site.Model_ID",               TagTable.DATA_TYPE_INT, null );
        }
        taken = System.currentTimeMillis() - start;
        General.showDebug("STAR convert: " + taken + "(" + (taken/1000.0) + " sec)" );
        //General.showMemoryUsed();

        if ( tT.columnContainsNull("_Atom_site.Cartn_y", false, 2)) { // report max of 2 nulls
            General.showDebug("contained a null value in column: " + "_atom_site.Cartn_y");
        }

        if ( tT.containsNull(true, 1)) { // report max of 1 null per column
            General.showDebug("contained a null value in table");
        }

        /**
        ArrayList columnNamesToKeep = new ArrayList();
        columnNamesToKeep.add(Relation.DEFAULT_ATTRIBUTE_ORDER_ID);
        columnNamesToKeep.add("_Atom_site.Cartn_x");
        columnNamesToKeep.add("_Atom_site.Cartn_y");
        columnNamesToKeep.add("_Atom_site.Cartn_z");
        columnNamesToKeep.add("_Atom_site.id");
        columnNamesToKeep.add("_Atom_site.label_entity_id");
        columnNamesToKeep.add("_Atom_site.label_seq_id");
        columnNamesToKeep.add("_Atom_site.pdbx_PDB_model_num");
        tT.keepOnlyColumns( columnNamesToKeep );
         */

//        Relation relation = (Relation) tT; // Cast it to super class of TagTable

        // Delete all atoms in two of the models
//        if ( false ) {
//            start = System.currentTimeMillis();
//            BitSet modelList = new BitSet();
//            modelList.set( 2 );
//            modelList.set( 4 );
//            BitSet rowList = SQLSelect.selectBitSet(dbms, relation, "_Atom_site.pdbx_PDB_model_num",
//                    SQLSelect.OPERATION_TYPE_EQUALS, modelList, false);
//            General.showDebug("Will delete atoms in models: " + PrimitiveArray.toString( modelList ));
//            General.showDebug("Will delete atoms in       : " + PrimitiveArray.toString( rowList ));
//            General.showDebug("Number of atoms to delete: " +  PrimitiveArray.countSet( rowList ));
//            General.showDebug("Number of atoms before: " +  relation.sizeRows);
//            relation.removeRows(rowList,true,true);
//            General.showDebug("Number of atoms after: " +  relation.sizeRows);
//            taken = System.currentTimeMillis() - start;
//            General.showDebug("models remove took: " + taken + "(" + (taken/1000.0) + " sec)" );
//        }

        /**
        tT.convertDataTypeColumn("_Conformer_ID",               TagTable.DATA_TYPE_INT, null );
        tT.convertDataTypeColumn("_Mol_system_component_ID",    TagTable.DATA_TYPE_INT, null );
        tT.convertDataTypeColumn("_Residue_seq_code",           TagTable.DATA_TYPE_INT, null );
        tT.convertDataTypeColumn("_Residue_PDB_seq_code",       TagTable.DATA_TYPE_INT, null );
        //tT.convertDataTypeColumn("_Residue_PDB_seq_code",       TagTable.DATA_TYPE_STRING, "test %5d test");
        tT.convertDataTypeColumn("_Atom_cartn_x",       TagTable.DATA_TYPE_FLOAT, null );
        tT.convertDataTypeColumn("_Atom_cartn_y",       TagTable.DATA_TYPE_FLOAT, null );
        tT.convertDataTypeColumn("_Atom_cartn_z",       TagTable.DATA_TYPE_FLOAT, null );
         */

        // unparse to screen
        //General.showDebug("DBMS is:" + General.eol + dbms.toString() );
        try {
            General.showDebug("Writing table to: " + outputfileName);
            FileWriter fw = new FileWriter(outputfileName);
            start = System.currentTimeMillis();
            if ( ! tT.toSTAR( fw )) {
                fail("writing table as star.");
            }
            fw.close();
            taken = System.currentTimeMillis() - start;
            General.showDebug("table to STAR: " + taken + "(" + (taken/1000.0) + " sec)" );
        } catch ( Throwable t ) {
            General.showThrowable(t);
        }
        return true;
    }


    /** Read an NMR-STAR file with many different quote styles and compare output
     *again.
     */
    public static boolean doExample_starfilereader2( DBMS dbms ) {

        String fileName     = "moreQuotes.str";
        String fileNameOut  = "moreQuotes_out.str";
        String wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        File inputDir       = new File( wattosRoot,"data"+File.separator+"test_data" );
        String urlName      = inputDir + File.separator + fileName;
        URL    url          = InOut.getUrlFileFromName(urlName);
        File tmpDir         = new File( System.getProperty("java.io.tmpdir"));
        String outputfileName = tmpDir.toString()+File.separator+fileNameOut;

        StarFileReader sfr = new StarFileReader(dbms);
        General.showMemoryUsed();
        // parse
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            fail("parse unsuccessful.");
        }
        try {
            General.showDebug("Writing table to: " + outputfileName);
            FileWriter fw = new FileWriter(outputfileName);
            if ( ! sn.toSTAR( fw )) {
                fail("writing table as star.");
            }
            fw.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            fail("parse unsuccessful.");
        }

        String msg = InOut.getLines(urlName.toString(),0,99999);
        String exp = InOut.getLines(outputfileName.toString(),0,99999);
        msg = Strings.dos2unix(msg);
        exp = Strings.dos2unix(exp);
        if ( ! Strings.equalsIgnoreWhiteSpace(msg,exp)) {
            General.showError("Some of the following changes are significant beyond whitespace changes");
            General.showError("> is expected and ");
            General.showError("< is found");
            if ( ! DiffPrint.printDiff( msg, exp)) {
                fail("DiffPrint.printDiff");
            }
            fail("Funny quotes representation in STAR file not as before.");
        }

        return true;
    }
}
