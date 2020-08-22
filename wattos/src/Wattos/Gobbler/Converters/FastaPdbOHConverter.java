package Wattos.Gobbler.Converters;

import Wattos.Common.*;
import Wattos.Utils.*;

import java.io.*;
import java.util.regex.*;

/**
 *Purpose of this converter is to convert from the sequences of entries on hold
 *as provided by the
 *PDB.
 *
 * This converter will interpret the fasta records to a java class: orf. Mostly
 *containing a changed identifier set. 
 *<PRE>
NOTES:  -1- sequences of origin different than "protein" or "protein-het" will be 
            filtered out (e.g. nucleic and nuclei-het).
        -2- uncommon residues (like '(MSE)') will be replaced by X.
        -3- sequence shorter than 20 amino acids will be filtered out too.
 *  
 *
</PRE>
 * @author Jurgen F. Doreleijers
 * @see <A HREF="http://www.rcsb.org/pdb/cgi/statusQuery.cgi?full=1&format=SEQ">example at pdb (invalid link after website update in January 2006)</a>
 */
public class FastaPdbOHConverter {
    
    private Orf orf = new Orf();
    private OrfId orfid = new OrfId(); 
    public Pattern p_summary;
    public Matcher m_summary;
    private String regexp_summary;
    public Pattern p_nucelotides_only;
    public Matcher m_nucelotides_only;
    private String regexp_nucelotides_only;
    public Pattern p_good_seq_char;
    public Matcher m_good_seq_char;
    
    String PDB_DATABASE_IDENTIFIER      = "pdb";   
    
    // Global to this class so decision can be relayed from method.
    String molecule_type    = "";
    int length              = 0;
    int total_count         = 0;
    int error_count         = 0;
    int warning_count       = 0;

    
    /** Creates a new instance of FastaPdbConverter */
    public FastaPdbOHConverter() {
        /** Using new regexp in standard java 1.4...
         */
        try {
            // Should match: ">1MW6 Entity 2"            
            //regexp_summary = "^>(\\S{4})_( |\\S+) +mol:(\\S+) length:(\\d+) +(.+)$";
            regexp_summary = "^>(\\S{4}) Entity +(\\d+).*$";
            p_summary = Pattern.compile(regexp_summary);
            m_summary = p_summary.matcher(""); // default matcher on empty string.                        
            regexp_nucelotides_only = "[ACGTUX]+"; // Include X so it still matches partially unknown nucleotide sequences.
            p_nucelotides_only = Pattern.compile(regexp_nucelotides_only);
            m_nucelotides_only = p_nucelotides_only.matcher(""); // default matcher on empty string.                        
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);            
        }
        orf.init();
        orfid.init();
        orf.orf_id_list.orfIdList.add( orfid );        
    }    
    
    public boolean convert( String input_filename, String output_filename ) {
        
        /** Keeps track of whether the orf being parsed had a valid header line
         */
        boolean parse_status = true;
        
        try {
            BufferedWriter bw = new BufferedWriter( new FileWriter( output_filename));
            PrintWriter outputWriter = new PrintWriter( bw );

            FileReader fr = new FileReader( input_filename );
            LineNumberReader inputReader = new LineNumberReader( fr );
            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( !inputReader.ready() ) {
                General.showDebug("input not ready or just empty..");
                return false; 
            }
            String line = inputReader.readLine();
            while ( line != null ) {
                //General.showDebug("Processing line: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? The first one
                 * would be where ...
                 */                
                if ( ( line.length() > 0 ) && 
                     ( line.charAt(0) == FastaDefinitions.FASTA_START_CHAR )) {
                    if ( inputReader.getLineNumber() != 1 ) {
                        orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
                        orf.sequence = orf.sequence.replaceAll("\\(\\S{3}\\)","X"); // Replace e.g. '(MSE)' by 'X'
                        orf.sequence = orf.sequence.replaceAll("\\?","X");
                        total_count++;
                        if ( goodEntry(parse_status) ) {
                            outputWriter.write( orf.toFasta() );
                        }
                        //outputWriter.flush();  // just for debugging...
                        parse_status = true;
                        orf.init();
                        orfid.init();
                        orf.orf_id_list.orfIdList.add( orfid );
                    }
                    parse_status = parseSummaryLine( line ); // Work on next item.                    
                    if ( ! parse_status ) {
                        General.showError("Will skip this entry because of parse error.");
                    }                   
                } else {
                    orf.sequence = orf.sequence + line;
                }
                line = inputReader.readLine();
            }
            // Handle last sequence in file.
            orf.sequence = orf.sequence.replaceAll("\\s",""); 
            orf.sequence = orf.sequence.replaceAll("\\(\\S{3}\\)","X"); // Replace e.g. '(MSE)' by 'X'
            orf.sequence = orf.sequence.replaceAll("\\?","X");
            total_count++;
            if ( goodEntry(parse_status) ) {
                outputWriter.write( orf.toFasta() );
            }
            // Close it all.
            inputReader.close();
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        if ( error_count > 0 ) {
            General.showError("Skipped entries because of parse errors:   " + error_count);
        }
        General.showOutput("       Skipped entries because of criteria:       " + warning_count);
        General.showOutput("       Written entries                    :       " + (total_count - error_count - warning_count) );
        General.showOutput("       out of total entries               :       " + total_count);
        return true;
    }
    
    /** Qualifying method
     */
    public boolean goodEntry( boolean status ) {
        if ( ! status ) {
            error_count++;
            return false;
        }

        m_nucelotides_only.reset( orf.sequence );
        if ( m_nucelotides_only.matches() ) {        
            General.showWarning("Not using sequence that looks like a nucleic acid sequence     : " + orf.sequence );
            warning_count++;
            return false;
        }
        status = BlastDefinitions.hasValidSequenceForBlastDb( orf.sequence, true );
        if ( !status ) {
            General.showWarning("Not good entry because not hasValidSequenceForBlastDb          : " + orf.sequence );
            warning_count++;
            return false;
        }
            
        return true;
    }
    
    
    /** Parse to an orf_id lines like:
     *<PRE>
>1MW6 Entity 2
     </PRE>
     * Returns true if no problems were found. A false returned means the entry should
     *not be used!
     */
    public boolean parseSummaryLine( String line ) {
        
        boolean status = true;
        
        if ( line == null ) {
            General.showError("Processing line: [" + line + "]");
            return false;
        }
        
        m_summary.reset( line );// default matcher on empty string.        
        status = m_summary.matches();
        if ( ! status ) {
            General.showError("Processing line: [" + line + "]");            
            General.showError("unexpectted formatting overall.");            
            General.showError("didn't match regular expression:[" + regexp_summary + "]");            
            return status;
        }
        
        orfid.orf_db_name       = PDB_DATABASE_IDENTIFIER;
        
        orfid.orf_db_id         = m_summary.group(1);
        orfid.orf_db_subid      = m_summary.group(2); // Can be blank        
        
        if ( ! Strings.is_pdb_code( orfid.orf_db_id ) ) {
            General.showError("Processing line: [" + line + "]");            
            General.showError("doesn't look like a pdb code: [" + orfid.orf_db_id  +"]");            
            status = false;
        }

        orfid.orf_db_subid = orfid.orf_db_subid.trim(); // Changes any matched " " to "".
                            
        return status;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        boolean testing = false;
        FastaPdbOHConverter c = new FastaPdbOHConverter();
        
        if ( testing ) {
            if ( false) {  // true for testing only
                String line         = ">1MW6 Entity 2";
                String line2        = "CGCGCCCAGCTTTCCCAGCTAATAAACTAAAAAC";
                c.parseSummaryLine(line);
                General.showOutput("line: [" + line + "]");
                General.showOutput("orf: [\n" + c.orf.toFasta() + "]");            
            } 
        } else {
            if ( args.length != 2) {
                General.showOutput("\nUsage:  java Wattos.Gobbler.Converters.FastaPdbOHConverter " +
                "<fasta input file> <fasta output file>\n");
                System.exit(1);
            }
            String input_filename = args[0];
            String output_filename = args[1];
            General.showOutput("Reading  files in dir: "+input_filename);
            General.showOutput("Writing  file        : "+output_filename);
            boolean status = c.convert( input_filename, output_filename );
            if ( ! status ) {
                General.showError( "in convert.");
            }
        }
        General.showOutput("Done");
    }    
}
