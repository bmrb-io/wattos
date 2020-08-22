/*
 * Created on January 7, 2003, 9:54 AM
 */
package Wattos.Gobbler.Converters;

import java.io.*;
import java.util.*;
import Wattos.Common.*;
import Wattos.Gobbler.*;
import EDU.bmrb.starlibj.*;
import Wattos.Utils.*;

/** Class for updating the loop in STAR files with the blast homology hits.
<H1>Selection criteria and sorting rules for Blast Hits in BMRB entries</H1>


<P>Formulas for calculating some items:
<P>Using the following symbol definitions:
<UL>
<LI>
<B>ML</B>: match length
<LI>
<B>QL</B>: query length
<LI>
<B>SL</B>: subject length
<LI>
<B>NI</B>: number of identities
<LI>
<B>NP</B>: number of positives
</UL>

<UL>
<LI>
<B>_Sequence_query_to_submitted_percentage</B>: 100% x QL / SL. Can be in the range 0.00% - 200% (please check).
<LI>
<B>_Sequence_identity</B>: 100% x NI / ML, properly rounded to integer.
<LI>
<B>_Sequence_positive</B>: 100% x NP / ML, properly rounded to integer.
</UL>

<P>Selection criteria (Using symbols as defined above):
<UL>
<LI>
Any number of PDB hits but only the first chain when the molecule name
for that chain is ordered alphanumerically. WARNING: this can cause
better hits (by E value) to be removed in favor of worse hits but is very
rare and therefor ignored now.
<LI>
Only 5 hits of each database other than PDB. The hits with the smallest
expectation value should be retained within sublists by database name.
<LI>
ML/QL >= 50 %.
<LI>
NI/ML >= 98 % (using rounding before comparison).
</UL>

<P>Sorting rules:
<UL>
<LI>
PDB hits before hits in other databases.
<LI>
By database name.
<LI>
By unrounded sequence query to submitted_percentage. Implemented as sort on subject length since
within one blastmatch list the query lengths are the same.
<LI>
By rounded sequence identity percentage
<LI>
By database id
<LI>
By molecule name
</LI>
</UL>

<P>Formatting rules:
<UL>
<LI>
<B>_Database_name</B>: One of: PDB, DBJ, EMBL, GenBank, GI, PIR, PRF, REF, SWISS-PROT or unchanged from Blast output.
<LI>
<B>_Database_accession_code</B>: Uppercase if PDB; otherwise unchanged from Blast output.
<LI>
<B>_Database_entry_mol_name</B>: Unchanged from Blast output. I.e. for PDB the chain id isn't parsed out.
<LI>
<B>_Sequence_query_to_submitted_percentage</B>: %.2f in standard printf notation plus trailing percentage symbol.
<LI>
<B>_Sequence_subject_length</B>: simple int.
<LI>
<B>_Sequence_identity</B>: %.0f in standard printf notation plus trailing percentage symbol.
<LI>
<B>_Sequence_positive</B>: %.0f in standard printf notation plus trailing percentage symbol.
<LI>
<B>_Sequence_homology_expectation_value</B>: unchanged from Blast output (complicated to reproduce; looked at blast code).
</UL>

<P>Future work:
<UL>
<LI>
Delete the percentage symbol for values to the tags: _Sequence_query_to_submitted_percentage, 
_Sequence_identity, and
_Sequence_positive.
<LI>
Introduction of a database sub id as required to get to the exact locus in some databases, e.g. the
PDB.</UL>

<P>Bugs solved:
<UL>
<LI>
<B>2003-02-25</B>The values to the following tags were always identical:
_Sequence_identity
_Sequence_positive
<LI>
<B>2003-02-25</B>For old nucleic acid entries the following loops remained in the entry. 
Reported on this day for correction by annotators.
<LI>
<B>2003-02-25</B>For some databases the id was not extracted and simply set to "?". This
caused many valid hits to be removed from the list because they had the same id. The Id is
now stripped from the molecule name and inserted to the id field.
<B>2003-02-26</B>For entry 4104 containing a complex of nucleic acids and proteins the protein
block was not annotated by blast hits. Probably a problem in other entries too.
<B>2003-03-06</B>For swiss prot entries the id cannot be retrieved from the molecule name as for 
pir and prf db entries.
</UL>

<P>Update strategy:
<UL>
<LI>
For all qualifying proteins (e.g. > 20 aa) the saveframe will be checked for the tags:
_Sequence_homology_query_date and _Sequence_homology_query_revised_last_date. If these tags
do not exist, they will be inserted with current dates. The _Sequence_homology_query_revised_last_date
tag will only be updated if the "blast loop" needed to be inserted anew, updated, or deleted.
<LI>
The _Sequence_homology_query_date value will be updated every time a query was done even
though it might not have changed the hits.
</UL>

 * @author Jurgen F. Doreleijers
 */
public class BMRBHomologyUpdate {

    static final String fs = File.separator;
  
    static final String BMRB_DB_NAME            = "bmrb";
    static final String STAR_FILE_PREFIX        = "bmr";
    static final String STAR_FILE_EXTENSION     = "str";
    static final String BIN_FILE_EXTENSION      = "bin";
    static StarParser myParser = null;

    static final public float  MIN_PERCENTAGE_MATCH_LENGTH_QUERY_LENGTH   = 50.00f;
    static final public float  MIN_PERCENTAGE_IDENTITY                    = 97.50f; // or 98% after round off
    
    /** Creates a new instance of BMRBToFasta */
    public BMRBHomologyUpdate() {
    }
 
    public static boolean removeSelfHit( BlastMatchList bml ) {
        OrfId orf_id = (OrfId) bml.query_orf_id_list.orfIdList.get(0);
        String query_orf_db_id = orf_id.orf_db_id;
        for (int i=0;i<bml.match_list.size();i++) {
            //General.showDebug("Looking at hit:" + i );
            BlastMatch bm = (BlastMatch) bml.match_list.get(i);
            OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0);
            if ( orfId.orf_db_id.equals( query_orf_db_id) ) {
                //General.showDebug("Removing self hit to BMRB database with id: " + orfId.orf_db_id);
                bml.match_list.remove(i);
                return true;
            }       
        }
        General.showDebug("Didn't find self hit to exclude which should have been present");
        General.showDebug("for orf: [" + orf_id.toString() + "]");
        return true;
    }
    
    public static boolean convertEntry( BlastMatchList blastmatch_list, String star_dir_name,
        ArrayList starfiles ) {
            
        final int FILES_TO_SHOW = 3;
        
        FileInputStream inStream =  null;            
        
        OrfId orf_id = (OrfId) blastmatch_list.query_orf_id_list.orfIdList.get(0);
        if ( ! orf_id.orf_db_name.equals( BMRB_DB_NAME ) ) {
            General.showError("db name is not bmrb but: [" + orf_id.orf_db_name + "]");
            General.showError("for orf: [" + orf_id.toString() + "]");
            return false;
        }
        if ( ! removeSelfHit( blastmatch_list )) {
            General.showError("db name is not bmrb but: [" + orf_id.orf_db_name + "]");
            General.showError("for orf: [" + orf_id.toString() + "]");
            return false;
        }

        String input_star_file_name = STAR_FILE_PREFIX + orf_id.orf_db_id + "." + STAR_FILE_EXTENSION;
        //General.showOutput("Doing star file: " + input_star_file_name);
        
        if ( ! starfiles.contains( input_star_file_name)  ) {
            int endIdx = FILES_TO_SHOW;
            if ( starfiles.size() < FILES_TO_SHOW ) {
                endIdx = starfiles.size();
            }
            ArrayList starfiles_small = new ArrayList( starfiles.subList(0,endIdx) );
            General.showDebug("file does not exist in archive: " + input_star_file_name );
            General.showDebug( "First " + endIdx + " files that do exist in the archive are: " +
                starfiles_small.toString() );
            return true;
        }
        input_star_file_name = star_dir_name + fs + input_star_file_name;
        try {
            inStream =  new FileInputStream( input_star_file_name );
        
            StarParser.ReInit( inStream );
            General.showDebug("Parsing input star file: " + input_star_file_name);
            StarParser.StarFileNodeParse(Wattos.Star.StarGeneral.sp);        
            inStream.close();
            //General.showDebug("Parsing done");
            
            StarFileNode sfnInput = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();

            // Returns a arraylist of orf or a valid empty table.
            Table sequence_table = NMRSTAREntry.getSequenceHomologyData( sfnInput, 
                orf_id.orf_db_id,
                orf_id.orf_db_subid );
            if ( sequence_table == null ) {
                General.showError("failed to find the SequenceHomologyData (not even empty) for:");
                General.showError("BMRB entry: " + orf_id.orf_db_id );
                General.showError("saveframe : " + orf_id.orf_db_subid );
                return false;
            }
            //General.showDebug("Found sequence_table: " + sequence_table.toString());

            // Update STAR tree.
            boolean status = NMRSTAREntry.setSequenceHomologyData( sfnInput,
                orf_id.orf_db_id,
                orf_id.orf_db_subid, sequence_table, blastmatch_list );

            if ( ! status ) {
                General.showError("in NMRSTAREntry.setSequenceHomologyData");
                General.showError("BMRB entry: " + orf_id.orf_db_id );
                General.showError("saveframe : " + orf_id.orf_db_subid );
                return false;
            }
            //General.showDebug("Done: setSequenceHomologyData");

            // Writing out.
            String tmp_star_file_name = input_star_file_name + ".tmp";
            //General.showDebug("Writing out temporary star file: " + tmp_star_file_name);
            FileOutputStream outStream =  new FileOutputStream( tmp_star_file_name );            
            StarUnparser myUnparser = new StarUnparser( outStream );
            myUnparser.setFormatting( true );
            myUnparser.writeOut( sfnInput, 0 );        
            myUnparser = null;
            outStream.close();
            
            // Use formatNMRSTAR to reformat the way we like it; overwriting the
            // original
            int status_return = ExternalPrograms.formatNMRStar( tmp_star_file_name, input_star_file_name );
            if ( status_return != 0 ) {
                General.showError("FormatNMRStarExternal failed (is the executable in path?).");
                General.showError("Keeping temp file for inspection.");
                General.showError("BMRB entry: " + orf_id.orf_db_id );
                General.showError("saveframe : " + orf_id.orf_db_subid );
                return false;
            }
            // Delete temp file.
//            File tmp_file = new File( tmp_star_file_name );
            //tmp_file.delete();            
        } catch ( Throwable t ) {
            General.showError("in convertEntry; skipping this file: " + input_star_file_name);
            General.showError("BMRB entry: " + orf_id.orf_db_id );
            General.showError("saveframe : " + orf_id.orf_db_subid );
            General.showThrowable(t);
            return false;
        }
        
        return true;
    }
    
   

    /** Returns true only if all entries were successfully converted
     */
    public static boolean updateAllEntries( String star_dir_name, String bin_dir_name ) {
        
        boolean printHashes = false;
        final int PRINT_HASH_SIZE = 2;
        final int PRINT_HASH_ROW_SIZE = 80;
        
        
//        FileInputStream  inStream = null;
        int error_count = 0;
        String entry_last_error = null;
        String[] binfiles = null;
        String[] starfiles = null;
//        BufferedWriter writer = null;
        
        File star_dir = new File(star_dir_name);
        FilenameFilter ff_star = new FilenameFilter() {
            public boolean accept( File d, String name ) { 
                return name.endsWith( "." + STAR_FILE_EXTENSION );
            }
        };
        starfiles = star_dir.list( ff_star );
        if ( starfiles == null ) {
            General.showError("directory didn't exist or some other error while");
            General.showError( "getting a listing from directory: " + star_dir_name);
            return false;
        }
        if ( starfiles.length == 0 ) {
            General.showError("no star files (files with extension: " + "." + STAR_FILE_EXTENSION + ")");
            General.showError( "in directory                : " + star_dir_name);
            General.showError( "in directory (absolute path): " + star_dir.getAbsolutePath());
            return false;
        }

        ArrayList starfiles_list = new ArrayList( Arrays.asList( starfiles ) );
        
        File bin_dir = new File(bin_dir_name);
        FilenameFilter ff_bin = new FilenameFilter() {
            public boolean accept( File d, String name ) { 
                return name.endsWith( "." + BIN_FILE_EXTENSION );
            }
        };
        binfiles = bin_dir.list( ff_bin );

        if ( binfiles == null ) {
            General.showError("directory didn't exist or some other error while");
            General.showError( "getting a listing from directory: " + bin_dir_name);
            return false;
        }

        if ( binfiles.length == 0 ) {
            General.showError("no bin files (files with extension: " + BIN_FILE_EXTENSION + ")");
            General.showError( "in directory: " + bin_dir_name);
            return false;
        }

        General.showOutput("Number of bin  files todo: " + binfiles.length );        
        General.showOutput("Number of star files todo: " + starfiles.length );        
        
        for (int i=0;i<binfiles.length;i++) {
            String input_bin_file_name = bin_dir_name + fs + binfiles[i];
            File f = new File( input_bin_file_name );            
            General.showOutput("Reading bin file: "+f.getName());
            
            try {
                ObjectInputStream in = new ObjectInputStream( new FileInputStream( input_bin_file_name ) );
                int m=0;
                while ( true ) {
                    Object o = InOut.readObjectOrEOF( in );
                    if ( o.equals( InOut.END_OF_FILE_ENCOUNTERED ) ) {
                        //General.showOutputNoEol("Reached end of bin file: " + input_bin_file_name);
                        break;
                    }
                    BlastMatchList blastmatch_list = (BlastMatchList) o;
                    // Show we're doing something.
                    if ( printHashes ) {
                        if ( m != 0 ) {
                            if ( (m % PRINT_HASH_SIZE) == 0 ) {
                                General.showOutputNoEol("#");                        
                            }
                            if ((m % (PRINT_HASH_SIZE*PRINT_HASH_ROW_SIZE)) == 0 )  {
                                General.showOutput("");                        
                            }
                        }
                    }
                    // Reached the end of the file.
                    if ( blastmatch_list == null ) { 
                        General.showError("read a null reference for blastmatch_list object from file");                        
                        return false;
                    }   
                    // If an entry contains multiple qualifying polymers; it's processed
                    // multiple times which is slightly inefficient.
                    boolean status = convertEntry( blastmatch_list, star_dir_name, starfiles_list );
                    if ( ! status ) {
                        OrfId orfId = (OrfId) (blastmatch_list.query_orf_id_list.orfIdList.get(0));
                        entry_last_error = orfId.orf_db_id;
                        General.showError("Found error while processing blastmatch_list with first orf db id: " +  entry_last_error );
                        error_count++;
                    }
                    m++;
                }
                if ( printHashes ) {
                    General.showOutput("");                        
                }
                in.close();
            } catch ( Throwable t ) {
                General.showThrowable(t);
                return false;
            }            
        }
        
        if ( error_count != 0 ) {
            General.showError("Found total number of entries in error: " + error_count);            
            General.showError("Last error for orf db id              : " + entry_last_error );
            return false;
        }
        return true;        
    }
    
    
    static public void showUsage() {
        General.showOutput("Usage: java Wattos.Gobbler.Converters.BMRBHomologyUpdate");
        General.showOutput("    <dir name star files> <base dir name bin files> ");
        General.showOutput("    <verbosity>");
        General.showOutput("base dir name bin files     E.g. /big/jurgen/condor/blastio/processed/weekly/nr");
    }     
    
    
    /**
     * Will update all star files in given directory with the info in
     *the binary files in the given directories if needed.
     *The number of directories with 
     * @param args the command line arguments.
     */
    public static void main(String[] args) throws Exception {
        if ( args.length != 3 ) {
            showUsage();
            System.exit(1);
        }
        String star_dir_name = args[0];
        String bin_dir_name = args[1];
        int verbosity = Integer.parseInt( args[2] );        
        General.verbosity = verbosity; // switch into debugging mode if needed

        General.showOutput("Starting BMRBHomologyUpdate version 2007-03-29");        
        boolean status = BMRBHomologyUpdate.updateAllEntries( star_dir_name, bin_dir_name );
        if ( ! status ) {
            General.showError( ": in BMRBHomologyUpdate.updateAllEntries");        
            System.exit(1);
        }
        General.showOutput("Done");        
    }    
}
 
