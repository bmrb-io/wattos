/*
 * NMRSTAREntry.java
 *
 * Created on January 7, 2003, 11:09 AM
 *Thu Nov 10 14:47:47 CST 2005
 */

package Wattos.Common;

import com.braju.format.*;              // printf equivalent

import java.util.*;
import EDU.bmrb.starlibj.*;
import Wattos.Utils.*;
import Wattos.Gobbler.*;
import Wattos.Gobbler.Converters.*;

/**
 * Code needed to extract and update the blast related items in the STAR file.
 * Modifications todo are to go from one to many databases against which to match.
 * @author Jurgen F. Doreleijers
 * @version Thu Nov 10 14:47:47 CST 2005
 */
public class NMRSTAREntry {

    static private Class starlibj_package_class_SaveFrameNode ;// save some time;
    static private Class starlibj_package_class_DataLoopNode ;// save some time;
    static final int    POSITION_DATABASE_NAME              = 0;
    static final int    POSITION_QUERY_TO_SUBMITTED         = 3;
    static final int    POSITION_IDENTITY                   = 5;
    static final int    MAX_NON_PDB_HITS_PER_DB             = 5;
    static final boolean SELECT_PDB_HITS_BY_ALPHANUMERICAL_SORTING = true;
    static final int MAX_LENGTH_MOL_NAME = 120;

    static {
        try {
            starlibj_package_class_SaveFrameNode = Class.forName( StarValidity.pkgName()+".SaveFrameNode");
            starlibj_package_class_DataLoopNode = Class.forName( StarValidity.pkgName()+".DataLoopNode");
        } catch( ClassNotFoundException e ) {
	    General.showOutput("Class not found exception:" + e.getMessage() );
	    General.showThrowable(e);
	}
    }

    SaveFrameNode t;

    /** Creates a new instance of NMRSTAREntry */
    public NMRSTAREntry() {
        General.showError("do not instantiate the NMRSTAREntry Class");
    }

    /** Returns a list of saveframes of specified category
     */
    public static VectorCheckType getSaveFramesByCategory( StarNode snInput, String category ) {

        VectorCheckType list = snInput.searchForTypeByTagValue( starlibj_package_class_SaveFrameNode,
            NMRSTARDefinitions.SF_CATEGORY_TAG, category );
        if ( list.size() < 1 ) {
            General.showWarning("no saveframes with category: [" + category + "] found." );
            return null;
        }
        return list;
    }

    /** Returns first of a list of saveframes of specified category.
     */
    public static SaveFrameNode getFirstSaveFrameByCategory( StarNode snInput, String category ) {
        VectorCheckType list = getSaveFramesByCategory( snInput, category );
        if ( list == null ) {
            return null;
        }
        if ( list.size() != 1 ) {
            //General.showDebug("found more or less than 1 saveframe with category: [" + category + "] found." );
        }
        SaveFrameNode sfn = (SaveFrameNode) list.firstElement();
        return sfn;
    }


    public static String getAccessionNumber( StarFileNode sfnInput ) {
        SaveFrameNode sfn = getFirstSaveFrameByCategory( sfnInput,
            NMRSTARDefinitions.ENTRY_INFORMATION_VAL );
        String number_str = getTagValue(sfn, NMRSTARDefinitions.ACCESSION_NUM_VAL );
        //General.showDebug("found accession number: " + number_str);
        return number_str;
    }

    /** Returns just the value of the deepest tag with the given name.
     * Returns null in case of error.
     */
    public static String getTagValue( StarNode sn, String tagName ) {
        VectorCheckType list = sn.searchByName( tagName );
        if ( list == null || list.size()==0 ) {
            return null;
        }
        DataItemNode sn_tag = (DataItemNode) list.firstElement();
        return sn_tag.getValue();
    }

    /** Returns a list of polymer sequences. Depending on the option doAll; all or
     *just the proteins sequences will be returned.
     *Returns null if no saveframes with polymers could be found.
     *Returns an empty list if polymer was found but wasn't of the correct type and doAll
     *wasn't set.
     */
    public static ArrayList getPolymerSequences( StarFileNode sfnInput, boolean doAll ) {

        ArrayList result_list = new ArrayList();

        VectorCheckType saveframe_list = sfnInput.searchForTypeByName( starlibj_package_class_SaveFrameNode,
            NMRSTARDefinitions.RESIDUE_SEQUENCE_TAG );
        if ( saveframe_list.size() < 1 ) {
            General.showWarning("no saveframes with tag: [" + NMRSTARDefinitions.RESIDUE_SEQUENCE_TAG + "] found." );
            return null;
        }

        // create the orfs
        for ( int i=0;i<saveframe_list.size();i++ ) {
            //General.showDebug("found polymer: " + i);
            // Do specific stuff if the standard "do only proteins" is selected
            SaveFrameNode saveframenode = (SaveFrameNode) saveframe_list.elementAt(i);

            String mol_class = getTagValue( saveframenode,
                NMRSTARDefinitions.POLYMER_CLASS_TAG );
            if ( mol_class == null ) {
                General.showError("failed to find the polymer class." );
                continue;
            }

            String mol_name_common = getTagValue( saveframenode,
                NMRSTARDefinitions.MOLECULE_NAME_TAG );
            if ( mol_name_common == null ) {
                General.showWarning("failed to find the common name for the molecule." );
                mol_name_common = "";
            }

            String sequence = getTagValue( saveframenode,
                NMRSTARDefinitions.RESIDUE_SEQUENCE_TAG );
            if ( sequence == null ) {
                General.showError("failed to find the sequence for the molecule." );
                continue;
            }

            // Get just the saveframe name with the 'save_'
            String saveframe_name = saveframenode.getLabel().substring(5);
            if ( saveframe_name == null ) {
                General.showError("failed to find the saveframe name for the molecule." );
                continue;
            }
            //General.showDebug("found sf name: [" + saveframe_name + "]");

            if ( ! doAll ) {
                if ( ! mol_class.equals( NMRSTARDefinitions.PROTEIN_CLASS ) ) {
                    //General.showDebug("skipping non protein." );
                    // Skip this hit.
                    continue;
                }
            }

            // Construct orfs and all
            Orf orf = new Orf();
            OrfId orf_id = new OrfId();
            orf.orf_id_list.orfIdList.add( orf_id );
            result_list.add( orf );

            // Populate them.
            orf.sequence = Strings.deleteAllWhiteSpace( sequence );
            orf_id.orf_db_name      = "bmrb";
            orf_id.orf_db_subid     = saveframe_name;
            orf_id.molecule_name    = mol_name_common;
        }
        return result_list;
    }


    public static void movePdbHitsToFront( Table sequence_table ) {

        int nrows = sequence_table.sizeRows();
        int position_next_PdbHit = 0;
        for (int r=0; r < nrows; r++) {
            String db_name = (String) sequence_table.getValue(r, POSITION_DATABASE_NAME);
            if ( db_name.equalsIgnoreCase("PDB") ) {
                if ( position_next_PdbHit == r ) {
                    // Already at right spot
                } else {
                    sequence_table.moveRow(r, position_next_PdbHit);
                    General.showDebug("Moved row from/to:" + r + "/" + position_next_PdbHit);
                }
                position_next_PdbHit++;
            }
        }
    }

    /** Delete last hits for non-pdb entries. Could be faster if the test isOkHit
     *would be done before splitting the entries.
     */
    public static void deleteSurplus( BlastMatchList bml ) {

        StringIntMap db_count = new StringIntMap();
        StringIntMap pdb_count = new StringIntMap();
        for (int i=0;i<bml.match_list.size();) {
//            General.showDebug("Looking at hit:" + i );
            BlastMatch bm = (BlastMatch) bml.match_list.get(i);
            OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0);
            if ( ! isOkHit( bm, bml.query_orf_length ) ) {
//                General.showDebug("Removed blastmatch because doesn't fullfill requirements (returned false on isOkHit method):" + bm.toString());
                bml.match_list.remove(i);
                // Don't increase i.
                continue;
            }
            if ( orfId.orf_db_name.equalsIgnoreCase("PDB") ) {
                if ( ! pdb_count.containsKey( orfId.orf_db_id )) {
                    pdb_count.addString( orfId.orf_db_id, 0);
                }
                int pdb_hits_count = pdb_count.getInt( orfId.orf_db_id);
                if ( SELECT_PDB_HITS_BY_ALPHANUMERICAL_SORTING ) {
                    // Keep all.
//                        General.showDebug("Keeping pdb hit (A):" + i );
                        pdb_count.addString( orfId.orf_db_id, pdb_hits_count+1);
                        i++;
                        continue;
                } else {
                    if ( pdb_hits_count == 0 ) {
//                        General.showDebug("Keeping pdb hit (B):" + i );
                        pdb_count.addString( orfId.orf_db_id, pdb_hits_count+1);
                        i++;
                        continue;
                    } else {
                        //General.showDebug("Removed row because same pdb hit already occured for id:" + orfId.orf_db_id);
                        bml.match_list.remove(i);
                        continue;
                    }
                }
            } else {
                if ( ! db_count.containsKey( orfId.orf_db_name )) {
                    db_count.addString( orfId.orf_db_name, 0 );
                }
                int db_hits_count = db_count.getInt( orfId.orf_db_name);
                if ( db_hits_count < MAX_NON_PDB_HITS_PER_DB ) {
                    //General.showDebug("Keeping row although non pdb hit:" + i );
                    db_count.addString( orfId.orf_db_name, db_hits_count+1);
                    //General.showDebug("Now have number of non pdb hits for db name: " + orfId.orf_db_name + " : " +
                    //  db_count.getInt( orfId.orf_db_name));
                    i++;
                } else {
//                    General.showDebug("Removed row because too many non pdb hits found: " + db_hits_count + " but only allowed: " + MAX_NON_PDB_HITS_PER_DB);
                    bml.match_list.remove(i);
                    continue;
                    // Don't increase i.
                    // At the end i should be the size of the array and the loop should exit.
                }
            }
        }
//        General.showDebug("pdb hits                         :\n" + Strings.toString( pdb_count) );
//        General.showDebug("non-pdb hits                     :\n" + Strings.toString( db_count) );
    }

    /** Delete hits to same pdb entry.
     */
    public static void deleteSurplusPdb( BlastMatchList bml ) {
        for (int i=1;i<bml.match_list.size();) {
            BlastMatch bm = (BlastMatch) bml.match_list.get(i);
            OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0);
            BlastMatch bm_prev = (BlastMatch) bml.match_list.get(i-1);
            OrfId orfId_prev = (OrfId) bm_prev.subject_orf_id_list.orfIdList.get(0);

//            General.showDebug("comparing pdb hit:" + orfId.orf_db_name      + ", " + orfId.orf_db_id);
//            General.showDebug("with      pdb hit:" + orfId_prev.orf_db_name + ", " + orfId_prev.orf_db_id);
            if ( orfId.orf_db_name.equalsIgnoreCase("PDB") &&
                 orfId_prev.orf_db_name.equalsIgnoreCase("PDB") &&
                 orfId.orf_db_id.equalsIgnoreCase( orfId_prev.orf_db_id ) ) {
//                General.showDebug("removed duplicate pdb hit:" + i);
                bml.match_list.remove(i);
                // don't increase i.
            } else {
                i++;
            }
        }
    }

    /** Checks if hit fullfills criteria
     */
    public static boolean isOkHit( BlastMatch bm, int query_orf_length ) {

//        OrfId subject_first_orf_id = (OrfId)  bm.subject_orf_id_list.orfIdList.get(0);

        float percentage_match_length_over_query_length     = (100.0f * bm.match_size)        / query_orf_length;
        float percentage_identity                           = (100.0f * bm.number_identities) / bm.match_size;

        // Add if they fullfill requirements.
        if ((percentage_match_length_over_query_length  >= BMRBHomologyUpdate.MIN_PERCENTAGE_MATCH_LENGTH_QUERY_LENGTH) &&
            (percentage_identity                        >= BMRBHomologyUpdate.MIN_PERCENTAGE_IDENTITY)) {
//            General.showDebug("hit was kept.");
            return true;
        }
        // Report
//        if ( false ) {
//            General.showDebug("hit was filtered out for: ");
//            General.showDebug("blast_match.match_size                   : " + bm.match_size);
//            General.showDebug("query_orf_length                         : " + query_orf_length);
//            General.showDebug("percentage_match_length_over_query_length: " + percentage_match_length_over_query_length);
//            General.showDebug("percentage_identity                      : " + percentage_identity);
//        }
        return false;
    }


    /** Translate hits in gi to original db and splits the matches on multiple subject
     *ids. Maintain the order in which they were listed in the blast output.
     */
    public static boolean modifyBlastMatchForBMRB( BlastMatchList blastmatch_list ) {



        /** reformat the subject orf id
         */
        for (int i=0; i < blastmatch_list.match_list.size(); i++) {
            BlastMatch bm = (BlastMatch) blastmatch_list.match_list.get(i);
            for (int j=0;j<bm.subject_orf_id_list.orfIdList.size();j++) {
                OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(j);
                // Translate the database name always.
                if ( orfId.orf_db_name.equals("bmrb")) {
                     orfId.orf_db_name = orfId.orf_db_name.toUpperCase();
                }

                if ( orfId.orf_db_name.equals("gi") ) {
                    boolean status = reformatOrfIdForBMRB( orfId );
                    if ( ! status ) {
                        General.showError("in modifyBlastMatchForBMRB.");
                        return false;
                    }
                }
            }
        }
        /** split for multiple subject orf ids so each match has only 1 subject
         *orf id.
         */
        for (int i=0; i < blastmatch_list.match_list.size(); i++) {
            BlastMatch bm = (BlastMatch) blastmatch_list.match_list.get(i);
            for (int j=(bm.subject_orf_id_list.orfIdList.size()-1);  j>0; ) {
                // Create a new blastmatch and add it to the list. This is rather wastefull but makes
                // for short simple code.
                BlastMatch bm_new = (BlastMatch) Wattos.Utils.Objects.deepCopy( bm );
                // Remove all and then add j. This could be speed improved.
                bm_new.subject_orf_id_list = new OrfIdList();
                bm_new.subject_orf_id_list.orfIdList.add( bm.subject_orf_id_list.orfIdList.get(j) );
                // These new elements added will be checked by this routine again but that's ok.
                // Important to add them just after the one we're checking.
                blastmatch_list.match_list.add( i+1, bm_new );
                bm.subject_orf_id_list.orfIdList.remove(j);
                j--;
            }
        }
        return true;
    }

    /** Translate
     */
    public static boolean reformatOrfIdForBMRB( OrfId orfId  ) {

        // Translate the database name always.
        orfId.orf_db_name = DatabaseDefinitions.getBmrbFromNrDb( orfId.orf_db_name );

        // Only do the hits for the nr database.
        if ( ! orfId.orf_db_name.equals(("GI") )) {
            return true;
        }

        String[] parts = orfId.molecule_name.split("\\|");
        if ( parts.length < 3 ) {
            General.showError("in reformatOrfIdForBMRB.");
            General.showError( "Expected 3 or more parts splitted by bar but found: " + parts.length );
            General.showError( "Input orf id: " + orfId.toFasta() );
            return false;
        }
        orfId.orf_db_name   = parts[0];
        orfId.orf_db_id     = parts[1];
        orfId.orf_db_subid  = "";
        orfId.molecule_name = parts[2].trim();

        if ( ( orfId.orf_db_id.length() == 0 ) &&
             ( orfId.orf_db_name.equalsIgnoreCase("PIR") ||
               orfId.orf_db_name.equalsIgnoreCase("PRF") ) ) {
            orfId.orf_db_id = Strings.getFirstWord( orfId.molecule_name );
            orfId.molecule_name = Strings.stripFirstWord( orfId.molecule_name );
        }

        orfId.toBmrbStyle();
        // No time to investigate why the trailing gi remains...
        //if ( orfId.molecule_name.endsWith("gi") ) {
        //    orfId.molecule_name = orfId.molecule_name.substring(0,orfId.molecule_name.length()-2).trim();
        //}

        return true;
    }

    /** Translate
     */
    public static boolean reformatOrfIdForBMRB( OrfIdList orfIdList  ) {
        for (int i=0;i<orfIdList.orfIdList.size();i++) {
            OrfId orfId = (OrfId) orfIdList.orfIdList.get(i);
            boolean status = reformatOrfIdForBMRB( orfId );
            if ( ! status ) {
                return false;
            }
        }
        return true;
    }

    /** Sets the required info in the starfilenode depending on input. Return false
     *if the set failed.
     */
    public static boolean setSequenceHomologyData( StarFileNode sfnInput,
        String bmrb_id, String saveframe_name,
        Table sequence_table, BlastMatchList blastmatch_list ) {


        //General.showDebug("Table now: [" + blastmatch_list.toString() + "]");
        // For hits in gi reformat the info.
        modifyBlastMatchForBMRB( blastmatch_list );
        //General.showDebug("Table after modifyBlastMatchForBMRB: [" + blastmatch_list.toString() + "]");

        // Delete surplus of rows.
        deleteSurplus( blastmatch_list );
        //General.showDebug("Truncated to: [" + blastmatch_list.toString() + "]");

        // Sort the list for BMRB use according to code in BlastMatch.compareTo method.
        Collections.sort( blastmatch_list.match_list );
        //General.showDebug("Sorted to: [" + blastmatch_list.toString() + "]");

        // Delete pdb duplicates after they've been sorted...
        if ( SELECT_PDB_HITS_BY_ALPHANUMERICAL_SORTING ) {
            deleteSurplusPdb( blastmatch_list );
//            General.showDebug("PDB Tr.to: [" + blastmatch_list.toString() + "]");
        }

        Table sequence_table_new = createNMR_STAR_SequenceHomologyTableNative( blastmatch_list );
        if ( sequence_table_new == null ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("Failed to get a table for the sequence homology info");
            return false;
        }
//        General.showDebug("Created table: [" + sequence_table_new + "]");


        // Check if an update needs to be done.
        boolean doUpdate = true;
        if ( sequence_table_new.equalsByStringRepresentation( sequence_table ) ) {
            doUpdate = false;
        }
        /**
        if ( doUpdate ) {
            General.showDebug("Blast output requires updating of STAR loop" );
            General.showDebug("Created new table: [" + sequence_table_new + "]");
            General.showDebug("Got old     table: [" + sequence_table + "]");
        } else {
            General.showDebug("Blast output is the same as in STAR loop" );
        }
         */

        // Get the saveframe node
        VectorCheckType saveframe_list = sfnInput.searchByName( "save_" + saveframe_name );
        if ( saveframe_list.size() < 1 ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("no saveframes with name: [" + saveframe_name + "] found." );
            return false;
        }
        SaveFrameNode saveframenode = (SaveFrameNode) saveframe_list.elementAt(0);

        // Store the 2 pieces of info and remove the 3 pieces of info from the saveframe.
        // Query date
        VectorCheckType query_date_sn_list = saveframenode.searchByName( NMRSTARDefinitions.HOMOLOGY_QUERY_DATE );
//        String query_date_value = null;
        if ( query_date_sn_list.size() < 1 ) {
            General.showDebug("For BMRB id: " + bmrb_id);
            General.showDebug("no tag with name: [" + NMRSTARDefinitions.HOMOLOGY_QUERY_DATE + "] found." );
        } else {
            for (int i=0;i<query_date_sn_list.size();i++) {
                if ( i > 0 ) {
                    General.showWarning("For BMRB id: " + bmrb_id);
                    General.showWarning("Didn't expect to remove more than one tag for: " + NMRSTARDefinitions.HOMOLOGY_QUERY_DATE + " but did.");
                }
                DataItemNode query_date_sn = (DataItemNode) query_date_sn_list.elementAt(i);
//                query_date_value = query_date_sn.getValue();
                saveframenode.removeElement( query_date_sn );
            }
        }

        // Revised date
        VectorCheckType rev_date_sn_list = saveframenode.searchByName( NMRSTARDefinitions.HOMOLOGY_REV_DATE );
        String rev_date_value = null;
        if ( rev_date_sn_list.size() < 1 ) {
            General.showDebug("For BMRB id: " + bmrb_id);
            General.showDebug("no tag with name: [" + NMRSTARDefinitions.HOMOLOGY_REV_DATE + "] found." );
        } else {
            DataItemNode rev_date_sn = (DataItemNode) rev_date_sn_list.elementAt(0);
            rev_date_value = rev_date_sn.getValue();
            saveframenode.removeElement( rev_date_sn );
        }

        // Loop
        VectorCheckType dataloopnode_list = saveframenode.searchForTypeByName( starlibj_package_class_DataLoopNode,
            NMRSTARDefinitions.HOMOLOGY_DB_NAME );
        // Remove if it exists.
        if ( dataloopnode_list.size() > 0 ) {
            saveframenode.removeElement( dataloopnode_list.elementAt(0) );
        }

        // Insert query date
        DataItemNode query_date_sn = new DataItemNode( NMRSTARDefinitions.HOMOLOGY_QUERY_DATE, Dates.getDateBMRBStyle() );
        saveframenode.addElement( query_date_sn );

        // Insert rev date
        if ( doUpdate || ( rev_date_value == null ) ) {
            rev_date_value = Dates.getDateBMRBStyle();
        }
        DataItemNode rev_date_sn = new DataItemNode( NMRSTARDefinitions.HOMOLOGY_REV_DATE, rev_date_value);
        saveframenode.addElement( rev_date_sn );

        //Then insert the loop if needed. Always deleting old table!
        if ( sequence_table_new.sizeRows() > 0 ) {
            //General.showDebug("New table to be inserted:\n" + sequence_table_new);
            DataLoopNode dataloopnode = NmrStar.toSTAR( sequence_table_new );
            saveframenode.addElement( dataloopnode );
        } else {
            //General.showWarning("New table to be inserted is empty:\n" + sequence_table_new);
        }
        return true;
    }

    /** Returns a table with the matches found. Returns empty table if
     *none where found for the given saveframe name. Returns null to indicate an error.
     */
    public static Table getSequenceHomologyData( StarFileNode sfnInput,
        String bmrb_id, String saveframe_name ) {

        VectorCheckType saveframe_list = sfnInput.searchByName( "save_" + saveframe_name );
        if ( saveframe_list.size() < 1 ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("no saveframes with name: [" + saveframe_name + "] found." );
            return null;
        }
        if ( saveframe_list.size() > 1 ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("more saveframes with name: [" + saveframe_name + "] found." );
            return null;
        }

        /** Create empty table with correct labels and all.
         */
        Table table = NMRSTARDefinitions.createHomologySequenceTable();

        /** Get the info for the table for real
         */
        SaveFrameNode saveframenode = (SaveFrameNode) saveframe_list.elementAt(0);
        VectorCheckType dataloopnode_list = saveframenode.searchForTypeByName( starlibj_package_class_DataLoopNode,
            NMRSTARDefinitions.HOMOLOGY_DB_NAME );
        if ( dataloopnode_list.size() < 1 ) {
            //General.showDebug("no data loop node list containing tag name: [" + NMRSTARDefinitions.HOMOLOGY_DB_NAME + "] found." );
            // Needs to be the same number of columns as if it existed because otherwise the comparison
            // doesn't behave.
            return table;
        }
        if ( dataloopnode_list.size() > 1 ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("more data loop node lists containing tag name: [" + NMRSTARDefinitions.HOMOLOGY_DB_NAME + "] found." );
            return null;
        }
        DataLoopNode        sequence_homology_loop  = (DataLoopNode) dataloopnode_list.elementAt(0);
//        LoopNameListNode    lnln                    = (LoopNameListNode) sequence_homology_loop.getNames().elementAt(0);
        LoopTableNode       ltn                     = sequence_homology_loop.getVals();

        int nrows = ltn.size();
        int ncols = ltn.elementAt(0).size();
        if ( ncols != table.sizeColumns() ) {
            General.showError("For BMRB id: " + bmrb_id);
            General.showError("Found inconsistent number of columns for sequence homology table: " + ncols);
            General.showError("Expected number of columns for sequence homology table          : " + table.sizeColumns() );
            return null;
        }
        // Iterate over the rows
        for (int r=0;r<nrows;r++) {
            // Iterate over the columns.
            table.addRow();
            for (int c=0;c<ncols;c++) {
                table.setValue(r,c, ltn.elementAt(r).elementAt(c).getValue());
            }
        }

        return table;
    }

    /** The info as in NMR-STAR v2.1.1;
     *The routine also filters according to BMRB standards.
     */
    public static Table createNMR_STAR_SequenceHomologyTableNative( BlastMatchList blastmatch_list ) {

        String[] label = { NMRSTARDefinitions.HOMOLOGY_DB_NAME,
                NMRSTARDefinitions.HOMOLOGY_DB_CODE,
                NMRSTARDefinitions.HOMOLOGY_DB_MOL_NAME,
                NMRSTARDefinitions.HOMOLOGY_SEQUENCE_PERCENTAGE,
                NMRSTARDefinitions.HOMOLOGY_SEQUENCE_SUBJECT_LENGTH,
                NMRSTARDefinitions.HOMOLOGY_SEQUENCE_IDENTITY,
                NMRSTARDefinitions.HOMOLOGY_SEQUENCE_POSIVTIVE,
                NMRSTARDefinitions.HOMOLOGY_SEQUENCE_EVALUE };
        int nrows = blastmatch_list.match_list.size();
        int ncols = 8;
        Table t = new Table( 0, ncols);
        for ( int c=0;c<ncols;c++ ) {
            t.setLabel(c, label[c].substring(1));
        }

//        OrfId orf_id = (OrfId) blastmatch_list.query_orf_id_list.orfIdList.get(0);

        for ( int r=0;r<nrows;r++ ) {
//            General.showDebug("Blast now here index: " + r);
            BlastMatch blast_match = (BlastMatch) blastmatch_list.match_list.get(r);
            if ( blastmatch_list.match_list == null ) {
                General.showError("Blast match is -null- for index: " + r);
                return null;
            }
            OrfId subject_first_orf_id = (OrfId)  blast_match.subject_orf_id_list.orfIdList.get(0);

            String queryToSubmittedPercentage = calc_QueryToSubmittedPercentage(
                blastmatch_list.query_orf_length, blast_match.subject_orf_length );
            String identity = calc_Percentage(blast_match.number_identities, blast_match.match_size);
            String positive = calc_Percentage(blast_match.number_positives,  blast_match.match_size);
            String expect   = calc_Expect(blast_match.expectation_value);
            String accession_code = subject_first_orf_id.orf_db_id;
            if ( accession_code.length() == 0 ) {
                accession_code = "?";
            }
            String molName = subject_first_orf_id.molecule_name;
            if ( molName.length() > MAX_LENGTH_MOL_NAME ) {
                molName = molName.substring(0,MAX_LENGTH_MOL_NAME);
            }
            t.addRow();
            int row_id = t.sizeRows() - 1;
            t.setValue(row_id,0, subject_first_orf_id.orf_db_name );
            t.setValue(row_id,1, accession_code );
            t.setValue(row_id,2, molName );
            t.setValue(row_id,3, queryToSubmittedPercentage );
            t.setValue(row_id,4, Integer.toString( blast_match.subject_orf_length ));
            t.setValue(row_id,5, identity);
            t.setValue(row_id,6, positive);
            t.setValue(row_id,7, expect);
        }
        return t;
    }


    public static String calc_QueryToSubmittedPercentage( int query_length, int subject_length ) {
        if ( subject_length == 0 ) {
            return "NaN";
        }
        Parameters p = new Parameters(); // Printf parameters
        p.add( 100.0 * query_length / subject_length );
        return Format.sprintf("%.2f", p);
    }

    public static String calc_Percentage( int teller, int denominator ) {
        if ( denominator == 0 ) {
            return "NaN";
        }
        Parameters p = new Parameters(); // Printf parameters
        //General.showDebug("fraction is: " + ((100.0 * teller) / denominator) );
        p.add( (100.0 * teller) / denominator + 0.5 );
        return Format.sprintf("%d", p);
    }

    /** Using formatting as in blast output.
     */
    public static String calc_Expect( double expect ) {
        String str = null;
        Parameters p = new Parameters(); // Printf parameters
        p.add( expect );
        /** Excerpt from blast source code:

/*---------------------------------------------------------------------------
Boolean print_score_eonly(FloatHi evalue, CharPtr buf)
{
  Char eval_buff[101];

  buf[0] = '\0';
  eval_buff[0] = '\0';
  if (evalue < 1.0e-180)     sprintf(eval_buff, "0.0");
  else if (evalue < 1.0e-99) sprintf(eval_buff, "%2.0e", evalue);
  else if (evalue < 0.0009)  sprintf(eval_buff, "%3.0e", evalue);
  else if (evalue < 0.1)     sprintf(eval_buff, "%4.3f", evalue);
  else if (evalue < 1.0)     sprintf(eval_buff, "%3.2f", evalue);
  else if (evalue < 10.0)    sprintf(eval_buff, "%2.1f", evalue);
  else                       sprintf(eval_buff, "%5.0f", evalue);
  if (eval_buff[0] != '\0') {
    sprintf(buf, "%s", eval_buff);
    return TRUE;
  }
  return FALSE;
}
         */
        if ( expect < 1.0e-180 ) {
            str = "0.0";
        } else if (expect< 1.0e-99 ) {
            str = Format.sprintf("%2.0e", p);
        } else if (expect< 0.0009 ) {
            str = Format.sprintf("%3.0e", p);
        } else if (expect< 0.1 ) {
            str = Format.sprintf("%4.3f", p);
        } else if (expect< 1.0 ) {
            str = Format.sprintf("%3.2f", p);
        } else if (expect< 10.0 ) {
            str = Format.sprintf("%2.1f", p);
        } else {
            str = Format.sprintf("%5.0f", p);
        }
        // For the cases where the exponent is included, we prefere 9e-28 over 9.0E-28. It's actually
        // a stray from the normal sprintf implementation
        // This can easily be speeded up by replacing the regular expression by a find& replace manually.
        str = str.replaceFirst("\\.e", "e");
        return str.trim();
    }

}
