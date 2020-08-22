package Wattos.Gobbler.Converters;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Database.Indices.*;
import cern.colt.list.*;
import java.util.regex.*;

/** Class for updating the database with matching PDB entries.
 *
  *<PRE>
Blast match example (only showing the query part):

             [    matching region   ]
       1     s     gap              t   n
Query: |-----|-----/  /--X----------|---|

The X denotes an uncommon residue which are to be ignored in the blast.
All query sequences are numbered from 1.

s := start of matching region
m := size of matching region including gaps
i := number of identical residues in matching region
n := end of query sequence; length of sequence
q := number of residues in the gaps combined
t := end of matching region ( s + m - q - 1 ) 
r := number of X residues in matching region combined

-0- No gaps are allowed in matching region (q=0).
-1- Any mismatch other than to an X will be scored 9.    
    If i - m + r < 0. No gaps are allowed.
-2- Leading and trailing mismatching regions of below size j will be scored 2.
    u = maximum(s,n-t) - 1    
    0: u = 0
    2: 0 < u <= j
    9: u > j
    Using t = m + s - 1. No gaps are allowed.

The minimal necessary set of elements to scoring are: s, n, q, r, m, and i.
A nice example of a sequence with an X is BMRB entry 3, PDB entry 1acp.
 *
 *
 *</PRE>
* @author Jurgen F. Doreleijers
 */
public class BMRBMatchWithPDBUpdate {

    // Relations to read
    private static String[] relationNames = {      
        
                                        "bmrb_main",    
                                        "bmrb_author",
                                        "bmrb_ligand",                                                      
                                        "bmrb_sequence",                                                      
                                        "pdb_main",     
                                        "pdb_model",                                        
                                        "pdb_author",
                                        "pdb_mr",      
                                        "pdb_nmr", 
                                        "pdb_ligand",
                                        "ets_pdb",      
                                        "ets_rcsb",  
                                        "override",
                                        "score",          
                                        "score_one2one",          
                                        "score_many2one",          
                                        "score_one2many",          
//                                        "score_many2many",       // will be copied from another.    
                                        "blast"          
    };        
//    private Relation bmrb_main = null;
    private Relation bmrb_author = null;
    private Relation bmrb_ligand = null;
    private Relation bmrb_sequence = null;
    private Relation pdb_main = null;
    private Relation pdb_author = null;
    private Relation pdb_mr = null;
    private Relation pdb_nmr = null;
    private Relation pdb_ligand = null;
    private Relation pdb_model = null;
    private Relation ets_pdb = null;
    private Relation ets_rcsb = null;
    private Relation override = null;
    private Relation score = null;
    private Relation score_one2one = null;
    private Relation score_many2one = null;
    private Relation score_one2many = null;
    private Relation score_many2many = null;
    private Relation blast = null;
    

    private IndexSortedInt      index_ets_rcsb_On_bmrb_id =null;
    private IndexSortedInt      index_score_On_bmrb_id    =null;
    private IndexSortedString   index_score_On_pdb_id     =null;
    private IndexSortedString   index_pdb_main_On_pdb_id  =null;
    private IndexSortedString   index_pdb_author_On_pdb_id  =null;
    private IndexSortedInt      index_bmrb_author_On_bmrb_id =null;
    private IndexSortedInt      index_blast_On_bmrb_id =null;   
    private IndexSortedString   index_blast_On_pdb_id =null;   
    private IndexSortedInt      index_bmrb_sequence_On_bmrb_id =null;
    private IndexSortedString   index_bmrb_sequence_On_query_orf_subid =null;    
    private IndexSortedString   index_pdb_ligand_On_pdb_id  =null;
    private IndexSortedInt      index_bmrb_ligand_On_bmrb_id =null;
//    private IndexSortedString   index_score_one2one_On_pdb_id  =null;

    // Convenience variables named after relation and column name. Relation name for score is dropped.
    private int[] ets_pdb_bmrb_id_list   = null;
    private String[] ets_pdb_pdb_id_list = null;
    private int[] model_count_type       = null;
    private BitSet hasMrBitSet           = null;
    private BitSet isNmrBitSet           = null;
//    private String[] pdb_main_pdb_id_list= null;
    private int[] ets_score_list         = null;
    private int[] author_score_list      = null;
    private int[] nmr_score_list         = null;
    private int[] blast_score_list       = null;
    private int[] ligand_score_list      = null;
    private int[] overall_score_list     = null;
    private int[] bmrb_id_list           = null;
    private String[] pdb_id_list         = null;
    private String[] blast_query_orf_subid_list     = null; 
    private int[]    blast_match_size               = null; 
    private int[]    blast_number_identities        = null; 
    private int[]    blast_number_gaps              = null; 
    private int[]    blast_query_orf_match_start    = null; 
    private String[] bmrb_sequence_sequence= null;     
    private int[][] scoreColumns         = null;
//    private int[] score_one2one_bmrb_id_list            = null;
//    private String[] score_one2one_pdb_id_list          = null;
//    private int[] score_one2one_overall_score_list      = null;
    
    DBMS dbms = new DBMS();
    
    public static final String  KNOWN_AA_NOT_REGEX     =   "ACDEFGHIKLMNPQRSTVWY";
    public static final String  BLAST_KNOWN_AA         =   "[" + KNOWN_AA_NOT_REGEX + "]";
    public static final ArrayList ligandListToIgnore = new ArrayList();
    
    static Pattern patern_aa = null;
    static Matcher matcher_aa = null;
    static {   
        ligandListToIgnore.add( "ACE" );
        ligandListToIgnore.add( "NH2" );
        try {
            patern_aa = Pattern.compile(BLAST_KNOWN_AA, Pattern.COMMENTS);            
            matcher_aa = patern_aa.matcher("");
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);
        }
    }
        
    public boolean initConvenienceVariables() {
        // Initialize convenience variables.
//        bmrb_main   = dbms.getRelation("bmrb_main");
        bmrb_author = dbms.getRelation("bmrb_author");
        bmrb_ligand = dbms.getRelation("bmrb_ligand");
        bmrb_sequence = dbms.getRelation("bmrb_sequence");
        pdb_main    = dbms.getRelation("pdb_main");
        pdb_author  = dbms.getRelation("pdb_author");
        pdb_mr      = dbms.getRelation("pdb_mr");
        pdb_nmr     = dbms.getRelation("pdb_nmr");
        pdb_ligand  = dbms.getRelation("pdb_ligand");
        pdb_model   = dbms.getRelation("pdb_model");
        ets_pdb     = dbms.getRelation("ets_pdb");
        ets_rcsb    = dbms.getRelation("ets_rcsb");
        override    = dbms.getRelation("override");
        score       = dbms.getRelation("score");
        score_one2one= dbms.getRelation("score_one2one");
        score_one2many= dbms.getRelation("score_one2many");
        score_many2one= dbms.getRelation("score_many2one");
        //score_many2many= dbms.getRelation("score_many2many");
        blast       = dbms.getRelation("blast");
        
        index_ets_rcsb_On_bmrb_id = (IndexSortedInt)    ets_rcsb.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
        index_score_On_bmrb_id    = (IndexSortedInt)    score.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
        index_score_On_pdb_id     = (IndexSortedString) score.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        index_pdb_main_On_pdb_id  = (IndexSortedString) pdb_main.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        index_pdb_author_On_pdb_id  = (IndexSortedString) pdb_author.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        index_bmrb_author_On_bmrb_id= (IndexSortedInt)    bmrb_author.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
        index_blast_On_bmrb_id      = (IndexSortedInt)    blast.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
        index_blast_On_pdb_id                   = (IndexSortedString)         blast.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        index_bmrb_sequence_On_bmrb_id          = (IndexSortedInt)    bmrb_sequence.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
        index_bmrb_sequence_On_query_orf_subid  = (IndexSortedString) bmrb_sequence.getIndex("query_orf_subid",Index.INDEX_TYPE_SORTED);
        index_pdb_ligand_On_pdb_id              = (IndexSortedString)    pdb_ligand.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        index_bmrb_ligand_On_bmrb_id            = (IndexSortedInt)      bmrb_ligand.getIndex("bmrb_id",Index.INDEX_TYPE_SORTED);
//        index_score_one2one_On_pdb_id           = (IndexSortedString) score_one2one.getIndex("pdb_id",Index.INDEX_TYPE_SORTED);
        
        ets_pdb_bmrb_id_list   = (int[])       ets_pdb.getColumn("bmrb_id");
        ets_pdb_pdb_id_list = (String[])       ets_pdb.getColumn("pdb_id");
//        pdb_main_pdb_id_list= (String[])      pdb_main.getColumn("pdb_id");
        
        blast_query_orf_subid_list= (String[])blast.getColumn("query_orf_subid");        
        blast_match_size          = (int[])   blast.getColumn("match_size");        
        blast_number_identities   = (int[])   blast.getColumn("number_identities");                        
        blast_number_gaps         = (int[])   blast.getColumn("number_gaps");                        
        blast_query_orf_match_start=(int[])   blast.getColumn("query_orf_match_start");                        

        ets_score_list         = (int[])         score.getColumn("ets_score");
        author_score_list      = (int[])         score.getColumn("author_score");
        nmr_score_list         = (int[])         score.getColumn("nmr_score");
        blast_score_list       = (int[])         score.getColumn("blast_score");
        ligand_score_list         = (int[])      score.getColumn("ligand_score");
        overall_score_list     = (int[])         score.getColumn("overall_score");
        bmrb_id_list           = (int[])         score.getColumn("bmrb_id");
        pdb_id_list         = (String[])         score.getColumn("pdb_id");

        bmrb_sequence_sequence = (String[])      bmrb_sequence.getColumn("sequence");
//        score_one2one_bmrb_id_list              = (int[])    score_one2one.getColumn("bmrb_id");
//        score_one2one_pdb_id_list               = (String[]) score_one2one.getColumn("pdb_id");
//        score_one2one_overall_score_list        = (int[])    score_one2one.getColumn("overall_score");

        if (pdb_main.containsColumn("model_count_type") ) {
            model_count_type       = (int[])      pdb_main.getColumn("model_count_type");
            hasMrBitSet           = (BitSet)      pdb_main.getColumn("has_mr");
            isNmrBitSet           = (BitSet)      pdb_main.getColumn("is_nmr");            
        }
        
        scoreColumns = new int[][] {    ets_score_list,
                                        author_score_list,
                                        nmr_score_list,
                                        blast_score_list,
                                        ligand_score_list };
        return true;
    }

    public boolean combineInfo() {
        
        initConvenienceVariables();
        General.showOutput("Combining info from tables");
        General.showOutput("Adding columns to pdb_main table.");
        pdb_main.insertColumn("model_count_type", Relation.DATA_TYPE_INT,null);
        pdb_main.insertColumn("has_mr", Relation.DATA_TYPE_BIT,null);
        pdb_main.insertColumn("is_nmr", Relation.DATA_TYPE_BIT,null);
        
        General.showOutput("Adding pdb_model info to pdb_main table.");
        if ( ! SQLSelect.update1(dbms, pdb_main, pdb_model, 
            "model_count_type", "model_count_type", "pdb_id", "pdb_id")) {
            General.showError("Failed to merge info in pdb_model to pdb_main");
            return false;
        }         
        
        General.showOutput("Updating pdb_main with pdb_nmr info.");
        if ( ! SQLSelect.update0(dbms, pdb_main, pdb_nmr, 
            "is_nmr", "pdb_id", "pdb_id", Boolean.valueOf(true))) {
            General.showError("Failed to merge info in pdb_nmr to pdb_main");
            return false;
        }
        General.showOutput("Found number of PDB NMR entries: " + pdb_main.getColumnBit("is_nmr").cardinality());        
                
        General.showOutput("Updating pdb_main with pdb_mr info.");
        if ( ! SQLSelect.update0(dbms, pdb_main, pdb_mr, 
            "has_mr", "pdb_id", "pdb_id", Boolean.valueOf(true))) {
            General.showError("Failed to merge info in pdb_mr to pdb_main");
            return false;
        }
        General.showOutput("Found number of PDB MR entries: " + pdb_main.getColumnBit("has_mr").cardinality());        
         
        Boolean is_nmr_true = Boolean.valueOf(true);
        boolean ignore = true;
        General.showOutput("Inserting blast info into score.");
        if ( ! SQLSelect.insertInto1( dbms, 
            score, blast, pdb_main, 
            "pdb_id", "pdb_id",
            new String[] {"bmrb_id", "pdb_id"}, 
            new String[] {},
            "is_nmr", is_nmr_true,
            ignore)) {
            General.showError("Failed to merge info in blast to score");
            return false;                
        }
        initConvenienceVariables(); // rows were added
        
        General.showOutput("Assigning a blast score for blast targets.");
        // for each row in score
        for (int rid_score=score.used.nextSetBit(0);rid_score>=0;rid_score=score.used.nextSetBit(rid_score+1)) {
            int bmrb_id   = bmrb_id_list[ rid_score ];            
            String pdb_id = pdb_id_list[  rid_score ];
            Integer bmrb_idInt = new Integer( bmrb_id );
            // Get the blast matches for this target.
            BitSet rid_blast_bmrb_id_bs   = (BitSet) index_blast_On_bmrb_id.getRidList(  bmrb_idInt,     Index.LIST_TYPE_BITSET, null );
            BitSet rid_blast_pdb_id_bs    = (BitSet) index_blast_On_pdb_id.getRidList(  pdb_id,     Index.LIST_TYPE_BITSET, null );
            rid_blast_bmrb_id_bs.and(rid_blast_pdb_id_bs);
            BitSet rid_blast_both_bs = rid_blast_bmrb_id_bs; // rewrite for clarity.

            // for each blast match check the conditions.
            // all hits in score so far have at least 1 blast match.
            for (int rid_blast = rid_blast_both_bs.nextSetBit(0); rid_blast>=0; rid_blast = rid_blast_both_bs.nextSetBit(rid_blast+1)) {
                String blast_query_orf_subid = blast_query_orf_subid_list[rid_blast];
                // Get the BMRB sequence for this match.
                int rid_bmrb_sequence = index_bmrb_sequence_On_bmrb_id.getRid( 
                    bmrb_id, index_bmrb_sequence_On_query_orf_subid, blast_query_orf_subid);                
                if ( rid_bmrb_sequence < 0 ) {
                    General.showWarning("Failed to find in bmrb_sequence bmrb id: " + bmrb_id + General.eol +
                        "when looking at blast target with pdb code: " + pdb_id + General.eol +
                        "for blast_query_orf_subid: " + blast_query_orf_subid + General.eol +
                        "assigning a score of 9");
                    blast_score_list[rid_score] = 9;
                    continue;
                }
                int test_score = getBlastScore(
                    bmrb_sequence_sequence[ rid_bmrb_sequence ],
                    rid_blast
                );
                if ( test_score < 0 ) {
                    General.showError("Failed to getBlastScore for sequence: " + bmrb_sequence_sequence[ rid_bmrb_sequence ]);
                    General.showWarning("with bmrb id: " + bmrb_id + General.eol +
                        "when looking at blast target with pdb code: " + pdb_id + General.eol +
                        "for blast_query_orf_subid: " + blast_query_orf_subid + General.eol +
                        "assigning a score of 9");
                    //return false;
                    test_score = 9;
                }
                // Pick the highest blast score over all sequences per entry.
                if ( blast_score_list[rid_score] == 0 ||
                    Defs.isNull( blast_score_list[rid_score]) ||
                    test_score > blast_score_list[rid_score] ) {
                    blast_score_list[rid_score] = test_score;
                }
            }            
        }
        
        General.showOutput("Inserting ets info into score.");
        // for each row in ets_pdb (skipping non nmr hits)
        for (int ridEts_pdb=ets_pdb.used.nextSetBit(0);ridEts_pdb>=0;ridEts_pdb=ets_pdb.used.nextSetBit(ridEts_pdb+1)) {
            int bmrb_id   = ets_pdb_bmrb_id_list[ ridEts_pdb ];
            String pdb_id = ets_pdb_pdb_id_list[ ridEts_pdb ];
            int ridPdb_main = index_pdb_main_On_pdb_id.getRid( pdb_id );
            if ( ! isNmrBitSet.get( ridPdb_main ) ) {
                /**
                General.showDebug("pdb id in ets isn't of nmr origin; skipping hit between: " + bmrb_id + " and: " + pdb_id );
                General.showDebug("ridPdb_main: " + ridPdb_main);                
                General.showDebug("pdb_main: " + pdb_main);                
                 */
                continue;
            }
            // Is it also in ets_rcsb? Assume 1 to 1 relationship here.
            int ridEts_rcsb = index_ets_rcsb_On_bmrb_id.getRid( bmrb_id );
            if ( Defs.isNull( ridEts_rcsb ) ) {
                General.showError("Failed to get ridEts_rcsb for bmrb id: " +  bmrb_id);
                return false;
            }
            boolean inEtsRcsb = false;
            if ( ridEts_rcsb >= 0 ) {
                inEtsRcsb = true;
            }
            // Find (new) location in score
            int ridScore = index_score_On_bmrb_id.getRid(bmrb_id,index_score_On_pdb_id,pdb_id);
            if ( Defs.isNull( ridScore ) ) {
                General.showError("Failed to index_score_On_bmrb_id.getRid(bmrb_id,index_score_On_pdb_id,pdb_id) for bmrb id: " +  bmrb_id);
                return false;
            }
            if ( ridScore < 0 ) {
                //General.showOutput("No row in score yet for " + bmrb_id + ", " + pdb_id + " , creating one.");
                ridScore = score.getNewRowId();
                score.setValue(ridScore,"bmrb_id",bmrb_id);
                score.setValue(ridScore,"pdb_id",pdb_id);
                // Only score back ETS only targets!
                if ( ! inEtsRcsb ) {
                    score.setValue(ridScore,"ets_score",5);
                }
            }      
        }    
        General.showOutput("Found number of targets in score table: " + score.used.cardinality());        
        initConvenienceVariables(); // rows were added
        
        General.showOutput("Assigning an author score.");
        IntArrayList rid_bmrb_author_list = new IntArrayList();
        IntArrayList rid_pdb_author_list  = new IntArrayList();
        ArrayList bmrb_authors = new ArrayList();
        ArrayList pdb_authors = new ArrayList();
        // for each row in score
        for (int rid_score=score.used.nextSetBit(0);rid_score>=0;rid_score=score.used.nextSetBit(rid_score+1)) {
            int bmrb_id   = bmrb_id_list[ rid_score ];            
            String pdb_id = pdb_id_list[  rid_score ];
            Integer bmrb_idInt = new Integer( bmrb_id );
            rid_bmrb_author_list.setSize(0);
            rid_pdb_author_list.setSize(0);
            rid_bmrb_author_list = (IntArrayList) index_bmrb_author_On_bmrb_id.getRidList( bmrb_idInt, Index.LIST_TYPE_INT_ARRAY_LIST, null );
            rid_pdb_author_list  = (IntArrayList) index_pdb_author_On_pdb_id.getRidList(  pdb_id,     Index.LIST_TYPE_INT_ARRAY_LIST, null );
            if ( rid_bmrb_author_list == null || rid_pdb_author_list == null ) {
                General.showError("Null value for rid_bmrb_author_list || rid_pdb_author_list for score row");
                General.showError(score.toStringRow(rid_score));
                author_score_list[rid_score] = 9;
                continue;
            }
            bmrb_author.getValueStringArray(bmrb_authors,rid_bmrb_author_list,"author_family_name");
            pdb_author.getValueStringArray( pdb_authors, rid_pdb_author_list, "author_family_name");
            int authorScore = getScoreMatchingAuthorLists( bmrb_authors, pdb_authors );
            if ( Defs.isNull( authorScore )) {
                General.showError("Failed to get author score: " + bmrb_authors + " and: " + pdb_authors);
                continue;
            }
            if ( authorScore != 0 ) {
	            author_score_list[rid_score] = authorScore;
            }
        }

        General.showOutput("Assigning a ligand score.");
        IntArrayList rid_bmrb_ligand_list = new IntArrayList();
        IntArrayList rid_pdb_ligand_list  = new IntArrayList();
        ArrayList bmrb_ligands = new ArrayList();
        ArrayList pdb_ligands = new ArrayList();
        // for each row in score
        for (int rid_score=score.used.nextSetBit(0);rid_score>=0;rid_score=score.used.nextSetBit(rid_score+1)) {
            int bmrb_id   = bmrb_id_list[ rid_score ];            
            String pdb_id = pdb_id_list[  rid_score ];
            //General.showDebug("Looking at: " + score.toStringRow(rid_score));
            Integer bmrb_idInt = new Integer( bmrb_id );
            rid_bmrb_ligand_list = (IntArrayList) index_bmrb_ligand_On_bmrb_id.getRidList( bmrb_idInt, Index.LIST_TYPE_INT_ARRAY_LIST, null );
            rid_pdb_ligand_list  = (IntArrayList) index_pdb_ligand_On_pdb_id.getRidList(   pdb_id,     Index.LIST_TYPE_INT_ARRAY_LIST, null );
            if ( rid_bmrb_ligand_list == null || rid_pdb_ligand_list == null ) {
                //General.showDebug("Null value for rid_bmrb_ligand_list || rid_pdb_ligand_list for score row");
                //General.showDebug(score.toStringRow(rid_score));
                ligand_score_list[rid_score] = 9;
                continue;
            }
            if ( ! ( bmrb_ligand.getValueStringArray(bmrb_ligands, rid_bmrb_ligand_list,"ligand_code") &&
                     pdb_ligand.getValueStringArray( pdb_ligands,  rid_pdb_ligand_list, "ligand_code"))) {
                General.showError("Can't get the ligands for both");
                return false;
            }
            /**
            if ( pdb_id.equalsIgnoreCase("1brv")) {
                General.showDebug("bmrb_ligands: " + bmrb_ligands);
                General.showDebug("bmrb_ligands: " + Strings.toString(bmrb_ligands));
                General.showDebug("bmrb_ligand size: " + bmrb_ligands.size());
                General.showDebug("pdb_ligands: " + pdb_ligands);
                General.showDebug("pdb_ligands: " + Strings.toString(pdb_ligands));
                General.showDebug("pdb_ligand size: " + pdb_ligands.size());
            }
             */
            if ( ! matchingLigandLists( bmrb_ligands, pdb_ligands) ) {
                ligand_score_list[rid_score] = 9;
            }
        }

        General.showOutput("Assigning overall score.");        
        // for each row in score
        int max_score;
        for (int rid_score=score.used.nextSetBit(0);rid_score>=0;rid_score=score.used.nextSetBit(rid_score+1)) {
            max_score = 0;
            for (int s=0;s<scoreColumns.length;s++) {
                int score = scoreColumns[s][rid_score];
                //General.showDebug("Found a score of: " + score + " and max is: " + max_score);
                if ( Defs.isNull(score) || (score<max_score) ) {
                    // ignore
                } else {
                    max_score=score;
                }
            }
            overall_score_list[rid_score] = max_score;
            String pdb_id = pdb_id_list[  rid_score ];
            int ridPdb_main = index_pdb_main_On_pdb_id.getRid( pdb_id );
            // Adjust targets without an MR file by +2
            if ( ! hasMrBitSet.get( ridPdb_main )) {
                if ( overall_score_list[rid_score] < 8 ) { // don't exceed 8.
                    overall_score_list[rid_score]++;
                }
                if ( overall_score_list[rid_score] < 8 ) { // don't exceed 8.
                    overall_score_list[rid_score]++;
                }
            }            
            // Adjust targets with a single model by +1
            if ( model_count_type[ ridPdb_main ] == 1 ) {
                //General.showWarning("Trying to add 1 for score for model_count_type == 1, for pdb id: " + pdb_id);
                if ( overall_score_list[rid_score] < 8 ) { // don't exceed 8.
                    overall_score_list[rid_score]++;
                }
            }            
        }
        
        General.showOutput("Override overall score from override table.");        
        // for each row in override
        for (int rid_override=override.used.nextSetBit(0);rid_override>=0;rid_override=override.used.nextSetBit(rid_override+1)) {
            //General.showDebug("Looking at row on rid: " + rid_override + " : " + override.toStringRow(rid_override));
            int bmrb_id   = override.getValueInt(    rid_override, "bmrb_id" );            
            String pdb_id = override.getValueString( rid_override, "pdb_id" ); 
//            Integer bmrb_idInt = new Integer( bmrb_id );
            //General.showDebug("Found bmrb_id,pdb_id: " + bmrb_id + ", " + pdb_id);
            int ridScore = index_score_On_bmrb_id.getRid(bmrb_id, index_score_On_pdb_id, pdb_id);
            //General.showDebug("Found ridScore: " + ridScore);
            if ( Defs.isNull( ridScore ) ) {
                General.showError("Failed to index_score_On_bmrb_id.getRid(bmrb_id,index_score_On_pdb_id,pdb_id) for bmrb id: " +  bmrb_id);
                return false;
            }
            if ( ridScore < 0 ) {
                //General.showDebug("No row in score yet, creating one for override.");
                ridScore = score.getNewRowId();
                score.setValue(ridScore,"bmrb_id",bmrb_id);
                score.setValue(ridScore,"pdb_id",pdb_id);
            }      
            score.copyValue(override, rid_override, "overall_score", 
                            ridScore, "overall_score");
        }
        General.showOutput("Found number of targets in score table after overrides: " + score.used.cardinality());        
        
        General.showOutput("Populate the many-to-many relation");
        dbms.copyRelation(score, "score_many2many", true);
        score_many2many = dbms.getRelation("score_many2many");
        initConvenienceVariables(); // rows were added
        BitSet toRemove = new BitSet();
        int[] score_many2many_overall_score_list = (int[]) score_many2many.getColumn("overall_score");        
        for (int rid_score=score_many2many.used.nextSetBit(0);rid_score>=0;rid_score=score_many2many.used.nextSetBit(rid_score+1)) {
            if ( score_many2many_overall_score_list[rid_score] >= 9 ) {
                toRemove.set(rid_score);
            }
        }        
        General.showOutput("Removing number of targets with score 9: " + toRemove.cardinality());                
        score_many2many.removeRows(toRemove, false, false);        
        String[] columnsToKeep = new String[] { "bmrb_id", "pdb_id", "overall_score" };
        ArrayList columnsToKeepList = new ArrayList(Arrays.asList(columnsToKeep));
        score_many2many.removeColumnsExcept( columnsToKeepList );
        
        
        General.showOutput("Populate the one-to-many relation");
        BitSet score_rid_bs_distinct = SQLSelect.getDistinct( dbms, score, "bmrb_id", score.used);
        // Find lowest overall score for each BMRB id, varying the pdb_id. This will give no duplicate BMRB ids
        // but it will give duplicate PDB ids.
        for (int score_rid=score_rid_bs_distinct.nextSetBit(0); score_rid>=0; score_rid=score_rid_bs_distinct.nextSetBit(score_rid+1)) {
            int bmrb_id = bmrb_id_list[score_rid];
            Integer bmrb_idInt = new Integer( bmrb_id );
            BitSet bmrb_id_bs = (BitSet) index_score_On_bmrb_id.getRidList(bmrb_idInt,Index.LIST_TYPE_BITSET,null);            
            int score_rid_best = Defs.NULL_INT;
            int score_best = Defs.NULL_INT; // keeps track of successful target.
            String pdb_id = Defs.EMPTY_STRING;
            //General.showOutput("TEST: bmrb_id: " + bmrb_id );
            int score_test;
            String pdb_id_test;
            for (int score_rid2=bmrb_id_bs.nextSetBit(0); score_rid2>=0; score_rid2=bmrb_id_bs.nextSetBit(score_rid2+1)) {
                score_test = overall_score_list[score_rid2];
                //General.showOutput("TEST: score_test: " + score_test + " and score_best: " + score_best);
                if ( score_test >= 9 ) {
                    continue;
                }
                if ( score_test < score_best || Defs.isNull(score_best) ) {
                    // no questions asked.
                    score_rid_best = score_rid2;
                    pdb_id = pdb_id_list[score_rid2];
                    score_best = score_test;
                } else if ( score_test == score_best ) {
                    pdb_id_test = pdb_id_list[score_rid2];
                    if ( pdb_id_test.compareTo(pdb_id) < 0 ) {
                        score_rid_best = score_rid2;
                        pdb_id = pdb_id_list[score_rid2];                        
                        score_best = score_test;
                    }                    
                }
            }
            if ( ! Defs.isNull(score_best) ) {
                int rid = score_one2many.getNewRowId();
                score_one2many.setValue(rid,"bmrb_id",bmrb_id);
                score_one2many.setValue(rid,"pdb_id",pdb_id);
                score_one2many.setValue(rid,"overall_score",new Integer(overall_score_list[score_rid_best]));
            }
        }

        General.showOutput("Populate the many-to-one relation");
        score_rid_bs_distinct = SQLSelect.getDistinct( dbms, score, "pdb_id", score.used);
        // Find lowest overall score for each PDB id, varying the BMRB id. This will give no duplicate PDB ids
        // but it will give duplicate BMRB ids.
        for (int score_rid=score_rid_bs_distinct.nextSetBit(0); score_rid>=0; score_rid=score_rid_bs_distinct.nextSetBit(score_rid+1)) {
            String pdb_id = pdb_id_list[score_rid];
            //General.showOutput("Doing pdb entry -1-: " + pdb_id);
            BitSet pdb_id_bs = (BitSet) index_score_On_pdb_id.getRidList(pdb_id,Index.LIST_TYPE_BITSET,null);            
            int score_rid_best = Defs.NULL_INT;
            int score_best = Defs.NULL_INT; // keeps track of successful target.
            int bmrb_id = Defs.NULL_INT;
            int score_test, bmrb_id_test;
//            String pdb_id_test;
            for (int score_rid2=pdb_id_bs.nextSetBit(0); score_rid2>=0; score_rid2=pdb_id_bs.nextSetBit(score_rid2+1)) {
                score_test = overall_score_list[score_rid2];
                if ( score_test >= 9 ) {
                    continue;
                }
                //General.showOutput("Doing pdb entry -2- : " + pdb_id + "score_rid2: " + score_rid2 + " score_test: " + score_test);
                if ( score_test < score_best || Defs.isNull(score_best) ) {
                    // no questions asked.
                    score_rid_best = score_rid2;
                    bmrb_id = bmrb_id_list[score_rid2];
                    score_best = score_test;
                } else if ( score_test == score_best ) {
                    bmrb_id_test = bmrb_id_list[score_rid2]; // Take the lowest bmrb id if the scores are even.
                    if ( bmrb_id_test < bmrb_id ) {
                        score_rid_best = score_rid2;
                        bmrb_id = bmrb_id_list[score_rid2];                        
                        score_best = score_test;
                    }                    
                }
            }
            if ( ! Defs.isNull(score_best) ) {
                int rid = score_many2one.getNewRowId();
                score_many2one.setValue(rid,"bmrb_id",bmrb_id);
                score_many2one.setValue(rid,"pdb_id",pdb_id);
                score_many2one.setValue(rid,"overall_score",new Integer(overall_score_list[score_rid_best]));
            }
        }

        
        General.showOutput("Populate the one-to-one relation");
        score_rid_bs_distinct = SQLSelect.getDistinct( dbms, score, "bmrb_id", score.used);
        HashMap pdb_id_accepted_map = new HashMap(); // keep track of which ids have been accepted before.
        int count_distinct_bmrb = score_rid_bs_distinct.cardinality();
        int[] distinct_bmrb_id_sorted_list = new int[ count_distinct_bmrb ];
        int i=0;
        for (int score_rid=score_rid_bs_distinct.nextSetBit(0); score_rid>=0; score_rid=score_rid_bs_distinct.nextSetBit(score_rid+1)) {
            distinct_bmrb_id_sorted_list[ i ] = bmrb_id_list[score_rid];
            i++;
        }
        Arrays.sort(distinct_bmrb_id_sorted_list);
        
        // Find lowest overall score for each BMRB id, varying the pdb_id. This will give no duplicate BMRB ids
        // but it will give duplicate PDB ids.
        for (i=0; i<count_distinct_bmrb; i++) {
            int bmrb_id = distinct_bmrb_id_sorted_list[i];
            Integer bmrb_idInt = new Integer( bmrb_id );
            BitSet bmrb_id_bs = (BitSet) index_score_On_bmrb_id.getRidList(bmrb_idInt,Index.LIST_TYPE_BITSET,null);            
            int score_rid_best = Defs.NULL_INT;
            int score_best = Defs.NULL_INT; // keeps track of successful target.
            String pdb_id = Defs.EMPTY_STRING;
            //General.showOutput("TEST: bmrb_id: " + bmrb_id );
            int score_test;
            String pdb_id_test;
            for (int score_rid2=bmrb_id_bs.nextSetBit(0); score_rid2>=0; score_rid2=bmrb_id_bs.nextSetBit(score_rid2+1)) {
                pdb_id_test = pdb_id_list[score_rid2];
                if ( pdb_id_accepted_map.containsKey(pdb_id_test) ) { // skip hits already accepted.
                    continue;
                }
                score_test = overall_score_list[score_rid2];
                //General.showOutput("TEST: score_test: " + score_test + " and score_best: " + score_best);
                if ( score_test >= 9 ) {
                    continue;
                }
                if ( score_test < score_best || Defs.isNull(score_best) ) {
                    // no questions asked.
                    score_rid_best = score_rid2;
                    pdb_id = pdb_id_list[score_rid2];
                    score_best = score_test;
                } else if ( score_test == score_best ) {                    
                    if ( pdb_id_test.compareTo(pdb_id) < 0 ) {
                        score_rid_best = score_rid2;
                        pdb_id = pdb_id_list[score_rid2];                        
                        score_best = score_test;
                    }                    
                }
            }
            if ( ! Defs.isNull(score_best) ) {
                int rid = score_one2one.getNewRowId();
                score_one2one.setValue(rid,"bmrb_id",bmrb_id);
                score_one2one.setValue(rid,"pdb_id",pdb_id);
                score_one2one.setValue(rid,"overall_score",new Integer(overall_score_list[score_rid_best]));
                pdb_id_accepted_map.put(pdb_id,null);
            }
        }

        
        Relation[] sortRelations = new Relation[] { score, score_one2many, score_many2one, score_many2many };
        for ( i=0;i<sortRelations.length;i++) {
            Relation r = sortRelations[i];
            if ( ! r.copyToOrderColumn("bmrb_id",0)) {
                General.showError("Failed to add order column to relation: " + r.name);
                return false;
            }
            if ( ! r.reorderRows()) {
                General.showError("Failed to reorder rows for relation: " + r.name);
                return false;
            }
        }   
        
            
        General.showOutput("Removing unneeded relations");     
        dbms.removeRelation(pdb_author);
        dbms.removeRelation(pdb_model);
        dbms.removeRelation(pdb_nmr);
        dbms.removeRelation(pdb_mr);
        dbms.removeRelation(pdb_ligand);
        dbms.removeRelation(bmrb_author);
        dbms.removeRelation(bmrb_ligand);
        dbms.removeRelation(bmrb_sequence);
        dbms.removeRelation(ets_rcsb);
        dbms.removeRelation(ets_pdb);
        dbms.removeRelation(blast);
        dbms.removeRelation(override);      
        //dbms.removeRelation(bmrb_main);      
        //dbms.removeRelation(pdb_main);      
        
        columnsToKeep = new String[] { "pdb_id" };
        columnsToKeepList = new ArrayList(Arrays.asList(columnsToKeep));
        pdb_main.removeColumnsExcept(columnsToKeepList);
        return true;
    }
    

 /**
 *Returns -1 on error. Cut off leading/trailing end j is currently set to 25.
 */  
    public int getBlastScore(String bmrb_sequence, int rid_blast ) {
        final int j = 15; // Maximum leading/trailing mismatches or missing regions.

        int q = blast_number_gaps[                  rid_blast];
        if ( q > 0 ) {
            //General.showDebug("Any gap will be scored 9.");
            return 9;
        }
        int s = blast_query_orf_match_start[        rid_blast];
        if ( s == 0 ) {
            General.showError("Found query_orf_match_start [" + s + "to be <1 whereas definitions say it should be >=1");
            General.showError("Rid: " + rid_blast + " BMRB sequence: " + bmrb_sequence);
            return -1;
        }
        int m = blast_match_size[                   rid_blast];
        int i = blast_number_identities[            rid_blast];
        int n = bmrb_sequence.length();
        int t = m + s - 1; // could leave q out as it's zero anyway.
        int ss = s -1;
        if ( (ss<0 ) || (ss>=n ) || (ss>=t ) || (t>n)) {
            General.showCodeBug("StringIndexOutOfBounds for BMRB sequence: [" + bmrb_sequence + "]");
            General.showCodeBug("s,m,  i,n,  t   "+s+","+m+"  ,"+i+","+n+"  ,"+t);
            return -1;
        }
        String match_region = bmrb_sequence.substring(s-1,t); // remember Java counts from zero
        if ( match_region.length() != m ) {
            General.showCodeBug("Can't count  bmrb_sequence.substring(s,t+1)");
            return -1;
        }
        int r = m - Strings.countStrings( match_region, matcher_aa );
        int u = Math.max(s, (n-t)) - 1;
        
        //General.showDebug("s,m,  i,n,  t,r,  u   "+s+","+m+"  ,"+i+","+n+"  ,"+t+","+r+"  ," +u);
        if ( i < (m-r) ) {
            //General.showDebug("Any mismatch other than to an X will be scored 9.");
            return 9;
        }        
        if ( u == 0 ) {
            //General.showDebug("Scored 0.");
            return 0;            
        } else if ( u <= j ) {
            //General.showDebug("Leading and trailing mismatching regions of equal to or under size " + j + " is scored 2.");
            return 0;            
        }
        //General.showDebug("Leading and trailing mismatching regions of over size " + j + " is scored 9.");
        return 9;
    }
        
   /** Return Defs.NULL_INT on error. if at least 1 author matches.
    *Score when
    *0      All authors match
    *1      At least 50 % of the BMRB authors matches
    *2      At least 1 author matches
    *9      No authors match.
     */
    public int getScoreMatchingAuthorLists( ArrayList bmrb_author, ArrayList pdb_author) {
        Strings.toLowerCase(bmrb_author);
        Strings.toLowerCase(pdb_author);
        int match_count = 0;
        for (int i=0;i<bmrb_author.size();i++) {
            String bmrb_a= (String) bmrb_author.get(i);
            //General.showDebug("Looking for bmrb author: " + bmrb_a + " in pdb list: " + Strings.toString(pdb_author)); 
            if ( pdb_author.contains(bmrb_a)) {
                match_count++;
            }
        }
        if ( match_count == bmrb_author.size() && pdb_author.size() == bmrb_author.size()) {
            return 0;
        }
        float match_fraction = match_count / (float) bmrb_author.size();
        if ( match_fraction >= 0.5f ) {
            return 1;
        }
        if ( match_count>= 1 ) {
            return 2;
        }        
        return 9;
    }
    
    /** // Remove ace/nh2 'ligands.
     */
    public static boolean removeLigandsToIgnore( ArrayList ligand_list ) {
        for (int i=0;i<ligand_list.size();i++) {
            String ligand = (String) ligand_list.get(i);
            if ( ligandListToIgnore.contains( ligand )) {
                ligand_list.remove(i);
                i--;
            }
        }
        return true;
    }

    /** Return true if all matches.
     */
    public static boolean matchingLigandLists( ArrayList bmrb_ligand_list, ArrayList pdb_ligand_list) {
        
        // Note that the bmrb ligand list contains only the non redundant ones.
        // Therefore make the pdb ligand list unique too before comparison.
        StringArrayList sal = new StringArrayList(pdb_ligand_list);
        sal.make_unique();
        pdb_ligand_list = (ArrayList) sal;
        
        // Remove ace/nh2 'ligands.
        if ( ! removeLigandsToIgnore( bmrb_ligand_list) ||
             ! removeLigandsToIgnore( pdb_ligand_list) ) {
            return false;
        }
        if ( bmrb_ligand_list.size() != pdb_ligand_list.size() ) {
            return false;
        }
        if ( bmrb_ligand_list.size() == 0) {
            return true;
        }
        Collections.sort(bmrb_ligand_list); // Sorting allows fast look up later.
        Collections.sort(pdb_ligand_list);
        
        for (int i=0;i<bmrb_ligand_list.size();i++) {
            String bmrb_l= (String) bmrb_ligand_list.get(i);
            String pdb_l= (String) pdb_ligand_list.get(i);
            //General.showDebug("Looking for bmrb ligand: " + bmrb_l + " and pdb ligand: " + pdb_l); 
            if ( ! bmrb_l.equals(pdb_l)) {
                return false;
            }
        }
        return true;
    }
    
    /** Write the csv files */
    public boolean update() {
        File outputDir = new File( "..","results");
        String outputDirStr = outputDir.toString();
        General.showOutput("Writing relations to csv files in dir: " + outputDirStr);
        boolean containsHeaderRow       = true;
        boolean containsPhysicalColumn  = false;
        boolean containsSelected        = true;
        boolean containsOrder           = false;
        boolean useActualNULL           = false; 
        return dbms.writeCsvRelationList(outputDirStr, containsHeaderRow,
           containsPhysicalColumn, containsSelected, containsOrder, useActualNULL );        
    }
    
    public boolean readAll() {
        File dtdDir = new File( ".","dtd");
        String dtdDirStr = dtdDir.toString();
        boolean checkConsistency = true;
        boolean showChecks = false;
        boolean status = dbms.readCsvRelationList( relationNames, ".", dtdDirStr, 
            checkConsistency, showChecks );
        if ( ! status ) {
            General.showError("Couln't readRelationList from . and " + dtdDirStr);
            return true;
        }
        // Now show any remain errors (actually redundant)
        //dbms.foreignKeyConstrSet.checkConsistencySet(true, true);
        General.showOutput("DBMS is consistent now");
        return true;
    }
        
    static public void showUsage() {
        General.showOutput("Usage: java -Xmx128m Wattos.Gobbler.Converters.BMRBMatchWithPDBUpdate <verbosity>");
        General.showOutput( "   Make sure the csv files for the following relations are present:\n" +
            Strings.toString( relationNames ));
        General.showOutput( "   They need to exist in the current working directory and a dtd directory");
        General.showOutput( "   needs to be present in the cwd. A ../results dir also needs to be present");        
        System.exit(1);
    }     
    
    
    /**
     * Will update all star files in given directory with the info in
     *the binary files in the given directory if needed.
     * @param args the command line arguments.
     */
    public static void main(String[] args) throws Exception {

        General.showOutput("Starting BMRBMatchWithPDBUpdate version 0.1");        
        final int EXPECTED_ARGUMENT_COUNT = 1;
        if ( args.length != EXPECTED_ARGUMENT_COUNT ) {
            General.showError("Expected " + EXPECTED_ARGUMENT_COUNT + " arguments but got: " + args.length);
            //General.showError("First argument: " + args[0]);
            General.showError("Last argument : " + args[args.length-1]);
            showUsage();
        }
        /**
        String[] files = new String[EXPECTED_ARGUMENT_COUNT-1];
        System.arraycopy(args, 0, files,  0, EXPECTED_ARGUMENT_COUNT-1 );
        if ( ! InOut.filesExist( files ) ) {
            General.showError("At least one of the given parameters isn't a existing directory or file");
            System.exit(1);            
        }
         */
        General.verbosity = Integer.parseInt( args[EXPECTED_ARGUMENT_COUNT-1] );        
                
              
        BMRBMatchWithPDBUpdate m = new BMRBMatchWithPDBUpdate();
        
        if ( ! m.readAll() ) {
            General.showError( ": in BMRBMatchWithPDBUpdate.readAll");        
            System.exit(1);
        }            
        if ( ! m.combineInfo() ) {
            General.showError( ": in BMRBMatchWithPDBUpdate.combineInfo");        
            System.exit(1);
        }
        
        if ( ! m.update() ) {
            General.showError( ": in BMRBMatchWithPDBUpdate.update");        
            System.exit(1);
        }
        
        General.showOutput("Done");      
    }    
} 
