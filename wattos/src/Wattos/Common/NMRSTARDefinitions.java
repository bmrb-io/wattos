/*
 * NMRSTAR_Definitions.java
 *
 * Created on January 7, 2003, 10:22 AM
 */

package Wattos.Common;

import Wattos.Utils.*;

/**
 * Contains the definition of the STAR tag names and certain standard values
 * which in principle should be all to change when the STAR dictionary changes
 * those.
 *
 * @author Jurgen F. Doreleijers
 */
public class NMRSTARDefinitions {
    
    public final static String RESIDUE_SEQUENCE_TAG  = "_Mol_residue_sequence";
    public final static String RESIDUE_COUNT_TAG     = "_Residue_count";
    public final static String POLYMER_CLASS_TAG     = "_Mol_polymer_class";
    public final static String MOLECULE_NAME_TAG     = "_Name_common";
    public final static String MOLSYSTEM_NAME_TAG    = "_Mol_system_name";
    public final static String SF_CATEGORY_TAG       = "_Saveframe_category";
    public final static String ENTRY_INFORMATION_VAL = "entry_information";
    public final static String ACCESSION_NUM_VAL     = "_BMRB_accession_number";
    public final static String MOLECULAR_SYSTEM_VAL  = "molecular_system";
    public final static String PROTEIN_CLASS         = "protein";
    public final static String DNA_CLASS             = "DNA";
    public final static String RNA_CLASS             = "RNA";
    public final static String HOMOLOGY_QUERY_DATE   = "_Sequence_homology_query_date";
    public final static String HOMOLOGY_REV_DATE     = "_Sequence_homology_query_revised_last_date";
    // 1 Loop
    public final static String HOMOLOGY_DB_NAME                     = "_Database_name";
    public final static String HOMOLOGY_DB_CODE                     = "_Database_accession_code";
    public final static String HOMOLOGY_DB_MOL_NAME                 = "_Database_entry_mol_name";    
    public final static String HOMOLOGY_SEQUENCE_PERCENTAGE         = "_Sequence_query_to_submitted_percentage";
    public final static String HOMOLOGY_SEQUENCE_SUBJECT_LENGTH     = "_Sequence_subject_length";
    public final static String HOMOLOGY_SEQUENCE_IDENTITY           = "_Sequence_identity";
    public final static String HOMOLOGY_SEQUENCE_POSIVTIVE          = "_Sequence_positive";
    public final static String HOMOLOGY_SEQUENCE_EVALUE             = "_Sequence_homology_expectation_value";
    // Number of elements in the loop
    public final static int HOMOLOGY_SEQUENCE_LOOP_COLUMN_COUNT             = 8;
        
    /** Creates a new instance of NMRSTAR_Definitions */
    public NMRSTARDefinitions() {
    }

    /** Create an empty table with tag names corresponding to the star table.
     */
    public static Table createHomologySequenceTable() {
        int ncols = NMRSTARDefinitions.HOMOLOGY_SEQUENCE_LOOP_COLUMN_COUNT;
        int nrows = 0;
        Table table = new Table( nrows, ncols );
        // Set the labels by striping the leading underscore.
        table.setLabel( 0, HOMOLOGY_DB_NAME.substring(1) );
        table.setLabel( 1, HOMOLOGY_DB_CODE.substring(1) );
        table.setLabel( 2, HOMOLOGY_DB_MOL_NAME.substring(1) );
        table.setLabel( 3, HOMOLOGY_SEQUENCE_PERCENTAGE.substring(1) );
        table.setLabel( 4, HOMOLOGY_SEQUENCE_SUBJECT_LENGTH.substring(1) );
        table.setLabel( 5, HOMOLOGY_SEQUENCE_IDENTITY.substring(1) );
        table.setLabel( 6, HOMOLOGY_SEQUENCE_POSIVTIVE.substring(1) );
        table.setLabel( 7, HOMOLOGY_SEQUENCE_EVALUE.substring(1) );
        return table;
    }
    
}
