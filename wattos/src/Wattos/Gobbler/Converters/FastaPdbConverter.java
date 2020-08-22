/*
 * FastaPdbConverter.java
 *
 * Created on December 30, 2002, 1:45 PM
 */

package Wattos.Gobbler.Converters;

import Wattos.Common.*;
import Wattos.Utils.*;

import java.io.*;
import java.util.regex.*;

/**
 *Purpose of this converter is to convert from the sequences as provided by the
 *PDB.
 *<P>
 * This converter will interpret the fasta records to a java class: orf. Mostly
 *containing a changed identifier set. 
 *<PRE>
NOTES:  -1- sequences of origin different than "protein" or "protein-het" will be 
            filtered out (e.g. nucleic and nuclei-het).
        -2- sequence shorter than 20 amino acids will be filtered out too.
</PRE>
 * @author Jurgen F. Doreleijers
 * @see <A HREF="ftp://ftp.rcsb.org/pub/pdb/derived_data/pdb_seqres.txt">example at pdb</a>
 */
public class FastaPdbConverter {
    
    private Orf orf = new Orf();
    private OrfId orfid = new OrfId(); 
    public Pattern p_summary;
    public Matcher m_summary;
    private String regexp_summary;
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
    public FastaPdbConverter() {
        /** Using new regexp in standard java 1.4...
         */
        try {
            regexp_summary = "^>(\\S{4})_( |\\S+) +mol:(\\S+) length:(\\d+) +(.+)$";
            p_summary = Pattern.compile(regexp_summary);
            m_summary = p_summary.matcher(""); // default matcher on empty string.                        
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
                        total_count++;
                        if ( goodEntry(parse_status ) ) {
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
            orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
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
        
        if (!(  molecule_type.equals( "protein")        ||
                molecule_type.equals( "protein-het"))) {
            warning_count++;
            return false;
        }
        
        if ( ! BlastDefinitions.hasValidSequenceForBlastDb( orf.sequence, true )) {
            warning_count++;
            return false;
        }
        return true;
    }
    
    
    /** Parse to an orf_id lines like:
     *<PRE>
>100d_A mol:nucleic length:10     DNA/RNA Chimeric Hybrid Duplex (5'-R(Cp*)-D(C
>101m_  mol:protein length:154     Myoglobin
     </PRE>
     * Returns true if no problems were found. A false returned means the entry should
     *not be used!
     */
    public boolean parseSummaryLine( String line ) {
        
        String length_str = "";
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
        molecule_type           = m_summary.group(3);
        length_str              = m_summary.group(4);
        orfid.molecule_name     = m_summary.group(5);
        
        if ( ! Strings.is_pdb_code( orfid.orf_db_id ) ) {
            General.showError("Processing line: [" + line + "]");            
            General.showError("doesn't look like a pdb code: [" + orfid.orf_db_id  +"]");            
            status = false;
        }

        orfid.orf_db_subid = orfid.orf_db_subid.trim(); // Changes any matched " " to "".

        if ( ! (molecule_type.equals( "nucleic")        ||
                molecule_type.equals( "nucleic-het")    ||
                molecule_type.equals( "protein")        ||
                molecule_type.equals( "protein-het"))) {
            General.showError("Processing line: [" + line + "]");            
            General.showError("molecule type is not standard: [" + molecule_type  +"]");            
            status = false;
        }
        
        try {
            length = Integer.parseInt( length_str );
        } catch ( Throwable t ) {
            General.showError("length is not properly formatted: [" + length_str + "]");            
            length = 0;
            status = false;
        }            
                    
        return status;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        boolean testing = false;
        FastaPdbConverter c = new FastaPdbConverter();
        
        if ( testing ) {
            if ( false) {  // true for testing only
                String line         = ">1234_a:1     mol:nucleic length:10     DNA/RNA Chimeric Hybrid Duplex (5'-R(Cp*)-D(C";
                c.parseSummaryLine(line);
                General.showOutput("line: [" + line + "]");
                General.showOutput("orf: [\n" + c.orf.toFasta() + "]");            
            } 
        } else {
            if ( args.length != 2) {
                General.showOutput("\nUsage:  java Wattos.Gobbler.Converters.FastaPdbConverter " +
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
