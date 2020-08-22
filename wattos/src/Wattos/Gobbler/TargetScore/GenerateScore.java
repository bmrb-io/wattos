package Wattos.Gobbler.TargetScore;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Gobbler.*;
import Wattos.Database.*;

/** CESG target selection scoring algorithm. 
 * Loosely based on the VB code by Eldon Ulrich and Java code by Ip Kei Sam.
 *This class reads a blacklist and a csv file with scoring input summary results 
 *and puts out the scoring summary.
 */
public class GenerateScore {
            
    public static int minimum_overall_tier = 1;

    
    static float [] chip       = new float[3];
    static float [] mean       = new float[3];
    static float [] stddev     = new float[3];
    static int   [] score      = new   int[3];
    static int   [] chip_score = new   int[10];

    static { // Change these values if there is new data!
        mean[0] = 2069;
        mean[1] = 2093;
        mean[2] = 3123;
        stddev[0] = 5382;
        stddev[1] = 5412;
        stddev[2] = 7521;
    }

    public GenerateScore() {
    }
    
    public static boolean score(Relation scoringInputSummary, Relation blackList, 
        Relation scoringSummary) {
        // Start to fill the ids and thus create the right dimensions for the table.            
        if ( scoringSummary.getNewRows( scoringInputSummary.sizeRows ) == null ) {
            General.showError("Failed to allocate memory for new rows");
            return false;
        }
        
        General.showDebug("Creating a hash map for the elements from the black list with number of elements: " + blackList.used.cardinality());
        int[] codesBlackList    = (int[]) blackList.getColumn(0);
        int[] tierBlackList     = (int[]) blackList.getColumn(1);
        HashMap mapBlackList = new HashMap(blackList.sizeRows);
        for (int r=blackList.used.nextSetBit(0);r>=0;r=blackList.used.nextSetBit(r+1)) {
            mapBlackList.put( new Integer( codesBlackList[r] ), new Integer( tierBlackList[r]) );
        }
        General.showDebug("Created map with number of elements: " + mapBlackList.size() );
        if ( blackList.used.cardinality() != mapBlackList.size() ) {
            General.showError("The element counts aren't the same; expect some duplication");
            return false;
        }

        // Set defaults if not default by java already (int is already zero).
        //scoringSummary.setValueByColumn("CESG-ORF-ID",0);
        //scoringSummary.setValueByColumn("AGI-ID",0);
        //scoringSummary.setValueByColumn("Splice_form_ID",0);
        //scoringSummary.setValueByColumn("CESG-fragment-ID",0);
        scoringSummary.setValueByColumn("Residue_number",0);
        //scoringSummary.setValueByColumn("CESG_score",0);
        scoringSummary.setValueByColumn("Tier_designation",0);
        scoringSummary.setValueByColumn("structure_known",0); 
        scoringSummary.setValueByColumn("fragment_score",9); 
        scoringSummary.setValueByColumn("SG_group_score",0);
        scoringSummary.setValueByColumn("Protein_structure_class",0);
        scoringSummary.setValueByColumn("predicted_transmembrane_segment",0);
        scoringSummary.setValueByColumn("signal_peptide",0);
        scoringSummary.setValueByColumn("number_of_cys_residues",0);
        scoringSummary.setValueByColumn("Pfam_score",0);
        scoringSummary.setValueByColumn("low_complexity_percent",0);
        scoringSummary.setValueByColumn("new_fold_score",9);
        scoringSummary.setValueByColumn("gene_chip_data",0);
        scoringSummary.setValueByColumn("disorder_values",0);
        scoringSummary.setValueByColumn("solubility_prediction",0);
        scoringSummary.setValueByColumn("Blacklist",0);

        
        General.showDebug("Copying rows from input to summary: " + scoringInputSummary.sizeRows);
        // *****************   CESG-ORF-ID   ***************        
        scoringSummary.copyColumnBlock( scoringInputSummary, "CESG_ORF_ID", 0, 
            "CESG-ORF-ID", 0, scoringInputSummary.sizeRows);
        // *****************   AGI-ID   ***************
        scoringSummary.copyColumnBlock( scoringInputSummary, "gene_code", 0, 
            "AGI-ID", 0, scoringInputSummary.sizeRows);
        // *****************   SPLICE_FORM_ID   ***************
        scoringSummary.copyColumnBlock( scoringInputSummary, "splice_form_ID", 0, 
            "Splice_form_ID", 0, scoringInputSummary.sizeRows);
        // *****************   CESG-FRAGMENT-ID   ***************
        scoringSummary.copyColumnBlock( scoringInputSummary, "fragment_ID", 0, 
            "CESG-fragment-ID", 0, scoringInputSummary.sizeRows);
        // *****************   RESIDUE_NUMBER   ***************
        scoringSummary.copyColumnBlock( scoringInputSummary, "residue_number", 0, 
            "Residue_number", 0, scoringInputSummary.sizeRows);            

        int r = scoringInputSummary.used.nextSetBit(0);
        while ( r>=0 ) {                
            // Uncomment any that aren't needed if you want. Simply taken variable name from column header.
            int         CESG_ORF_ID                                 = scoringInputSummary.getValueInt(          r, "CESG_ORF_ID");
//            String      gene_code                                   = scoringInputSummary.getValueString(       r, "gene_code");
//            String      splice_form_ID                              = scoringInputSummary.getValueString(       r, "splice_form_ID");
//            String      fragment_ID                                 = scoringInputSummary.getValueString(       r, "fragment_ID");
            int         residue_number                              = scoringInputSummary.getValueInt(          r, "residue_number");
//            double      PDB_expectation_value                       = scoringInputSummary.getValueDouble(       r, "PDB_expectation_value");
            float       PDB_percent_identity                        = scoringInputSummary.getValueFloat(        r, "PDB_percent_identity");
            String      PDB_code                                    = scoringInputSummary.getValueString(       r, "PDB_code");
//            String      PDB_chain_id                                = scoringInputSummary.getValueString(       r, "PDB_chain_id");
//            float       PDB_number_of_identities                    = scoringInputSummary.getValueFloat(        r, "PDB_number_of_identities");
            float       PDB_length_of_matched_region                = scoringInputSummary.getValueFloat(        r, "PDB_length_of_matched_region");
//            boolean     SCOP_hit                                    = scoringInputSummary.getValueBit(          r, "SCOP_hit");
            String      SCOP_structural_class                       = scoringInputSummary.getValueString(       r, "SCOP_structural_class");
//            int         COG_number                                  = scoringInputSummary.getValueInt(          r, "COG_number");
//            String      human_orthologue_value                      = scoringInputSummary.getValueString(       r, "human_orthologue_value");
//            String      human_disease_value                         = scoringInputSummary.getValueString(       r, "human_disease_value");
            int         HMMTOP_predicted_transmembrane_segments     = scoringInputSummary.getValueInt(          r, "HMMTOP_predicted_transmembrane_segments");
            int         TMHMM_predicted_transmembrane_segments      = scoringInputSummary.getValueInt(          r, "TMHMM_predicted_transmembrane_segments");
            boolean     TMHMM_predicted_signal_peptide              = scoringInputSummary.getValueBit(          r, "TMHMM_predicted_signal_peptide");
            String      cellular_location                           = scoringInputSummary.getValueString(       r, "cellular_location");
            String      signalP_prediction                          = scoringInputSummary.getValueString(       r, "signalP_prediction");
//            String      fragment_score                              = scoringInputSummary.getValueString(       r, "fragment_score");
//            int         Pfam_domain_count                           = scoringInputSummary.getValueInt(          r, "Pfam_domain_count");
//            String      Pfam_family_size_max                        = scoringInputSummary.getValueString(       r, "Pfam_family_size_max");
//            String      VSV                                         = scoringInputSummary.getValueString(       r, "VSV");
//            String      cluster_members                             = scoringInputSummary.getValueString(       r, "cluster_members");
//            String      ProtoNet_family_ID                          = scoringInputSummary.getValueString(       r, "ProtoNet_family_ID");
//            String      ProtoNet_family_size                        = scoringInputSummary.getValueString(       r, "ProtoNet_family_size");
//            String      total_domains                               = scoringInputSummary.getValueString(       r, "total_domains");
//            String      CO_fragment_domain_correlation              = scoringInputSummary.getValueString(       r, "CO_fragment_domain_correlation");
//            String      KR_fragment_domain_correlation              = scoringInputSummary.getValueString(       r, "KR_fragment_domain_correlation");
            int         cys_residue_count                           = scoringInputSummary.getValueInt(          r, "cys_residue_count");
            float       low_complexity_percent                      = scoringInputSummary.getValueFloat(        r, "low_complexity_percent");
            int         length_longest_disorder_region              = scoringInputSummary.getValueInt(          r, "length_longest_disorder_region");
            float       fraction_disordered                         = scoringInputSummary.getValueFloat(        r, "fraction_disordered");
//            String      solubility_prediction                       = scoringInputSummary.getValueString(       r, "solubility_prediction");
            float       affy_25                                     = scoringInputSummary.getValueFloat(        r, "affy_25");
            float       Nimblegen_24                                = scoringInputSummary.getValueFloat(        r, "Nimblegen_24");
            float       Nimblegen_60                                = scoringInputSummary.getValueFloat(        r, "Nimblegen_60");
//            double      SG_expectation_value                        = scoringInputSummary.getValueDouble(       r, "SG_expectation_value");
            float       SG_percent_identity                         = scoringInputSummary.getValueFloat(        r, "SG_percent_identity");
//            float       SG_number_of_identities                     = scoringInputSummary.getValueFloat(        r, "SG_number_of_identities");
//            float       SG_length_of_matched_region                 = scoringInputSummary.getValueFloat(        r, "SG_length_of_matched_region");
            String      SG_group                                    = scoringInputSummary.getValueString(       r, "SG_group");
            String      SG_group_status                             = scoringInputSummary.getValueString(       r, "SG_group_status");
//            double      PDBoh_expectation_value                     = scoringInputSummary.getValueDouble(       r, "PDBoh_expectation_value");
            float       PDBoh_percent_identity                      = scoringInputSummary.getValueFloat(        r, "PDBoh_percent_identity");
            String      PDBoh_code                                  = scoringInputSummary.getValueString(       r, "PDBoh_code");
//            String      PDBoh_chain_id                              = scoringInputSummary.getValueString(       r, "PDBoh_chain_id");
//            float       PDBoh_number_of_identities                  = scoringInputSummary.getValueFloat(        r, "PDBoh_number_of_identities");
            float       PDBoh_length_of_matched_region              = scoringInputSummary.getValueFloat(        r, "PDBoh_length_of_matched_region");
//            float       CENSOR_fraction_homologous                  = scoringInputSummary.getValueFloat(        r, "CENSOR_fraction_homologous");
//            float       PsiPred_fraction_C                          = scoringInputSummary.getValueFloat(        r, "PsiPred_fraction_C");
//            float       PsiPred_fraction_H                          = scoringInputSummary.getValueFloat(        r, "PsiPred_fraction_H");
//            float       PsiPred_fraction_E                          = scoringInputSummary.getValueFloat(        r, "PsiPred_fraction_E");
//            int         Coils_count                                 = scoringInputSummary.getValueInt(          r, "Coils_count");
//            float       Coils_fraction                              = scoringInputSummary.getValueFloat(        r, "Coils_fraction");

            minimum_overall_tier = 1;
            
            // *****************   STRUCTURE_KNOWN   ***************
            boolean residue_number_Under_100 = residue_number < 100;
            if (
                (  (! Defs.isNullString( PDB_code )) &&
                   ( PDB_percent_identity >= 30 ) &&
                   (  residue_number_Under_100 ||
                     (PDB_length_of_matched_region>=100) )) ||
                (  (! Defs.isNullString( PDBoh_code )) &&
                   ( PDBoh_percent_identity >= 30 ) &&
                   (  residue_number_Under_100 ||
                     (PDBoh_length_of_matched_region>=100) ))
                ) {
                scoringSummary.setValue(r, "structure_known", 9);
                setOverallOverallTier(9);
            }

            /**
            if ( (! Defs.isNullString( SG_group )) &&
                   ( SG_percent_identity >= 30 ) &&
                   (  residue_number_Under_100 ||
                      (SG_length_of_matched_region>=100) )) {
                scoringSummary.setValue(r, "structure_known", 9);
                setOverallOverallTier(9);
            }
             */

            // *****************   SG_GROUP_SCORE   ***************
            if ( ! Defs.isNullString( SG_group ) ) {
                if (    SG_percent_identity >= 30 ) {
                    StringArrayList workDone = new StringArrayList( 
                        Arrays.asList( Strings.splitWithAllReturned(
                        SG_group_status.toLowerCase(),';')));
                    StringArrayList intersection = workDone.intersection( 
                        ComparatorBlastMatchTargetDB.killers);
		    if ( intersection.size() > 0 ) {
                        scoringSummary.setValue(r, "SG_group_score", 9);
                        setOverallOverallTier(9); // Only situation with an overall effect
                    } else {
                        if ( workDone.contains("hsqc")||
                             workDone.contains("crystallized")) {
                            scoringSummary.setValue(r, "SG_group_score", 8);
                        } else if (workDone.contains("purified")) {
                            scoringSummary.setValue(r, "SG_group_score", 7);
                        } else if (workDone.contains("soluble")) {
                            scoringSummary.setValue(r, "SG_group_score", 6);                            
                        }
                    }
                    //General.showDebug("SG_group_score: " + scoringSummary.getValueString(r, "SG_group_score"));
                }
            }

            // *****************   PROTEIN_STRUCTURE_CLASS   ***************
	    /** Possible values:
all alpha
all beta
coil_coil  ( and combinations with all other)
interspersed alpha/beta
membrane; surface or peptide
multi-domain
segregated alpha/beta
small
             */
            if ( (!Defs.isNullString( SCOP_structural_class)) && (
                    (SCOP_structural_class.indexOf( "membr" ) >= 0 ) ||
                    (SCOP_structural_class.indexOf( "coil_coil" ) >= 0 ) 
                    )) { 
                scoringSummary.setValue(r, "Protein_structure_class",9);
                setOverallOverallTier(7);
            }
            
            // *****************   PREDICTED_TRANSMEMBRANE_SEGMENT   ***************
            if ( ( HMMTOP_predicted_transmembrane_segments == 1 ) ||
                 (  TMHMM_predicted_transmembrane_segments == 1 ) ) {
                scoringSummary.setValue(r, "predicted_transmembrane_segment",1);
                setOverallOverallTier(3);                
            }
            if ( ( HMMTOP_predicted_transmembrane_segments >= 2 ) ||
                 (  TMHMM_predicted_transmembrane_segments >= 2 ) ) {
                scoringSummary.setValue(r, "predicted_transmembrane_segment",9);
                setOverallOverallTier(7);                
            }

            
            // *****************   SIGNAL_PEPTIDE   ***************	
            if (    TMHMM_predicted_signal_peptide ||
                    signalP_prediction.startsWith("Signal") || // ... peptide or anchor
                    cellular_location.equals("chloroplast") ||
                    cellular_location.equals("mitochondrion") ||
                    cellular_location.equals("secreted") ) {
                //General.showDebug("cellular_location: " + cellular_location + 
                //                " signalP_prediction:" + signalP_prediction);
                scoringSummary.setValue(r, "signal_peptide",9);
                setOverallOverallTier(7);                
            }                         
            
            
            // *****************   NUMBER_OF_CYS_RESIDUES   ***************
            switch ( cys_residue_count ) {
                case 0: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 0);
                    setOverallOverallTier(1);
                    break;
                }
                case 1: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 1);
                    setOverallOverallTier(1);
                    break;
                }
                case 2: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 2);
                    setOverallOverallTier(2);
                    break;
                }
                case 3: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 3);
                    setOverallOverallTier(2);
                    break;
                }
                case 4: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 4);
                    setOverallOverallTier(2);
                    break;
                }
                case 5:
                case 6: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 5);
                    setOverallOverallTier(3);
                    break;
                }
                case 7:
                case 8: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 6);
                    setOverallOverallTier(3);
                    break;
                }
                case 9:
                case 10: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 7);
                    setOverallOverallTier(4);
                    break;
                }
                default: {
                    scoringSummary.setValue(r, "number_of_cys_residues", 9);
                    setOverallOverallTier(8);
                }
            }
            // *****************   PFAM_SCORE   ***************
            // Not in use.
            
            // *****************   LOW_COMPLEXITY_PERCENT   ***************
            if ( low_complexity_percent < 5 ) {
                ;
            } else if ( low_complexity_percent < 10 ) {
                scoringSummary.setValue(r, "low_complexity_percent", 1);
                setOverallOverallTier(2);
            } else if ( low_complexity_percent < 15 ) {
                scoringSummary.setValue(r, "low_complexity_percent", 2);
                setOverallOverallTier(3);
            } else if ( low_complexity_percent < 20 ) {
                scoringSummary.setValue(r, "low_complexity_percent", 3);
                setOverallOverallTier(4);
            } else {
                scoringSummary.setValue(r, "low_complexity_percent", 9);
                setOverallOverallTier(8);
            }

            // *****************   NEW_FOLD_SCORE   ***************
            // Not in use.

            
            // *****************   GENE_CHIP_DATA   ***************            
            chip[0] = affy_25;
            chip[1] = Nimblegen_24;
            chip[2] = Nimblegen_60;
            for (int i=0; i<3; i++){ // i runs over chips.
                if (     chip[i]> mean[i]+ 3*stddev[i]) score[i]=0;
                else if (chip[i]> mean[i]+ 2*stddev[i]) score[i]=1;
                else if (chip[i]> mean[i]+ stddev[i])   score[i]=2;
                else if (chip[i]> mean[i])              score[i]=3;
                else if (chip[i]> mean[i]/2)            score[i]=4;
                else if (chip[i]> mean[i]/4)            score[i]=5;
                else if (chip[i]> mean[i]/7)            score[i]=6;
                else if (chip[i]> mean[i]/10)           score[i]=7;
                else if (chip[i]> 0)                    score[i]=8;
                else                                    score[i]=9;
            }
            /**
            General.showDebug("Mean   for the 3 chips: " + PrimitiveArray.toString( mean ));
            General.showDebug("S.d    for the 3 chips: " + PrimitiveArray.toString( stddev ));
            General.showDebug("Measur for the 3 chips: " + PrimitiveArray.toString( chip ));
            General.showDebug("Scores for the 3 chips: " + PrimitiveArray.toString( score ));
             */
            Arrays.fill(chip_score, 0); // faster than init.
            chip_score[score[0]]++;
            chip_score[score[1]]++;
            chip_score[score[2]]++;
            //General.showDebug("Scores for the 10 levels:" + PrimitiveArray.toString( chip_score ));

            int count = 0;
            int gene_chip_score = 0;
            for (int j=0; j<9; j++) {       // j runs over scores.
                count = count + chip_score[j];
                if ( count >= 2 ) {
                    gene_chip_score = j;
                    break;
                }
            }
            //General.showDebug("Chip score             : " + gene_chip_score);
            scoringSummary.setValue(r, "gene_chip_data", gene_chip_score);
            switch ( gene_chip_score ) {
                case 0:  
                case 1:  
                case 2:  {
                    break;
                }
                case 3:
                case 4:
                case 5: {
                    setOverallOverallTier(2);
                    break;
                }
                case 6: {
                    setOverallOverallTier(3);
                    break;
                }
                case 7: 
                case 8: {
                    setOverallOverallTier(4);
                    break;
                }
                case 9: {
                    setOverallOverallTier(7);
                    break;
                }
                default: {
                    General.showError("Shouldn't default on gene_chip_data being: " + gene_chip_score);
                    return false;
                }                        
            }
                    
            // *****************   DISORDER_VALUES   ***************
            if ( ! Defs.isNull(fraction_disordered)) {
                if ( fraction_disordered < 0.30 ) {
                    ;
                } else if ( fraction_disordered <= 0.35 ) {
                    scoringSummary.setValue(r, "disorder_values", 1);
                    setOverallOverallTier(1);
                } else if ( fraction_disordered <= 0.40 ) {
                    scoringSummary.setValue(r, "disorder_values", 2);
                    setOverallOverallTier(1);
                } else if ( fraction_disordered <= 0.45 ) {
                    scoringSummary.setValue(r, "disorder_values", 6);
                    setOverallOverallTier(4);
                } else if ( fraction_disordered <= 0.50 ) {
                    scoringSummary.setValue(r, "disorder_values", 7);
                    setOverallOverallTier(4);
                } else {
                    scoringSummary.setValue(r, "disorder_values", 9);
                    setOverallOverallTier(4);
                }
                // In addition check for special case:
                if ( (fraction_disordered > 0.30) &&
                     (length_longest_disorder_region >= 40) ) {
                    scoringSummary.setValue(r, "disorder_values", 9);
                    setOverallOverallTier(4);
                }
            }
            

            // *****************   SOLUBILITY_PREDICTION   ***************
            // Not in use.

            // *****************   BLACKLIST   ***************
            Integer tierIdBlackList = (Integer) mapBlackList.get( new Integer(CESG_ORF_ID) );
            if ( tierIdBlackList != null ) {
                scoringSummary.setValue(r, "Blacklist", tierIdBlackList.intValue());
                setOverallOverallTier(9);
            }

            // *****************   TIER_DESIGNATION   ***************
            scoringSummary.setValue(r, "Tier_designation", minimum_overall_tier); 

            // *****************   CESG_SCORE   ***************
            scoringSummary.setValue(r, "CESG_score", "'"+   
                scoringSummary.getValueInt(r,"Tier_designation")+
                scoringSummary.getValueInt(r,"structure_known")+
                scoringSummary.getValueInt(r,"fragment_score")+
                scoringSummary.getValueInt(r,"SG_group_score")+
                scoringSummary.getValueInt(r,"Protein_structure_class")+
                scoringSummary.getValueInt(r,"predicted_transmembrane_segment")+
                scoringSummary.getValueInt(r,"signal_peptide")+
                scoringSummary.getValueInt(r,"number_of_cys_residues")+
                scoringSummary.getValueInt(r,"Pfam_score")+
                scoringSummary.getValueInt(r,"low_complexity_percent")+
                scoringSummary.getValueInt(r,"new_fold_score")+
                scoringSummary.getValueInt(r,"gene_chip_data")+
                scoringSummary.getValueInt(r,"disorder_values")+
                scoringSummary.getValueInt(r,"solubility_prediction")+
                scoringSummary.getValueInt(r,"Blacklist")    +            
                "'"
             );
            
            r = scoringInputSummary.used.nextSetBit(++r);
            
        }
        return true;
    }

    /** Sets the highest of the current or given tier
     */
    public static void  setOverallOverallTier(int tier) {
        if ( tier > minimum_overall_tier ) {
            minimum_overall_tier = tier;
        }
    }

    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.GenerateScore \\");
        General.showOutput("  scoring_input_summary_file_name       scoring_input_summary_dtd_file_name \\");
        General.showOutput("  black_list_file_name                  black_list_dtd_file_name \\");
        General.showOutput("  scoring_summary_file_name             scoring_summary_dtd_file_name");
        General.showOutput("       scoring_input_summary_file_name      :   name of csv file with most of the required data for scoring (52 columns and counting).");
        General.showOutput("       scoring_input_summary_dtd_file_name  :   name of the file data type definitions for previous file.");
        General.showOutput("       black_list_file_name                 :   name of the file with entries not to include.");
        General.showOutput("       black_list_dtd_file_name             :   name of the file data type definitions for previous file.");        
        General.showOutput("       scoring_summary_file_name            :   name of csv file with the individual scores.");        
        General.showOutput("       scoring_summary_dtd_file_name        :   name of csv file with the individual scores.");        
        General.showOutput("Notes: Give a dot for a dtd that you don't want to specify.");        
        System.exit(1);
    }
    
    public static void main(String[] args) {
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;
        
        /** Checks of input */
        if ( args.length != 6 ) {
            General.showError("Need 6 arguments.");
            show_usage();
        }
        int i=0;
        String scoring_input_summary_file_name      = args[i++];
        String scoring_input_summary_dtd_file_name  = args[i++];
        String black_list_file_name                 = args[i++];
        String black_list_dtd_file_name             = args[i++];
        String scoring_summary_file_name            = args[i++];
        String scoring_summary_dtd_file_name        = args[i++];

        boolean containsHeaderRow = true;
        if ( black_list_dtd_file_name.equals(".")) {
            black_list_dtd_file_name = null;
        }
        if ( scoring_input_summary_dtd_file_name.equals(".")) {
            scoring_input_summary_dtd_file_name = null;
        }
        DBMS dbms = new DBMS();
        Relation scoringInputSummary = null;
        Relation blackList           = null;
        Relation scoringSummary      = null;
        try {
            scoringInputSummary = new Relation("scoringInputSummary", dbms);
            blackList           = new Relation("blackList", dbms);
            scoringSummary      = new Relation("scoringSummary", dbms);
        } catch ( Exception e ) {
            General.showThrowable(e);
            System.exit(1);
        }
        
        General.showOutput("Reading file                              : " + scoring_input_summary_file_name );
        if ( ! scoringInputSummary.readCsvFile(  scoring_input_summary_file_name, containsHeaderRow, scoring_input_summary_dtd_file_name)) {
            General.doCodeBugExit("Failed to read scoringInputSummary csv file: " + scoring_input_summary_file_name);
        }
        
        
        General.showOutput("Reading file                              : " + black_list_file_name );
        if ( ! blackList.readCsvFile(  black_list_file_name, false, black_list_dtd_file_name )) {
            General.doCodeBugExit("Failed to read blackList csv file: " + black_list_file_name);
        }
        
        
        
        if ( ! scoringSummary.insertColumnSetFromCsvFile(scoring_summary_dtd_file_name)) {
            General.doCodeBugExit("Failed to read scoring summary dtd file: " + scoring_summary_dtd_file_name);
        }
        if ( ! score(scoringInputSummary, blackList, scoringSummary)) {
            General.showError("Failed to score completely; no write done");
            System.exit(1);
        }
        
        General.showOutput("Writing scoring summary to file: " + scoring_summary_file_name);
        scoringSummary.writeCsvFile(scoring_summary_file_name, containsHeaderRow);        
        //scoringSummary.toCsv(scoring_summary_file_name);        
        General.showOutput("Done");
    }
}
