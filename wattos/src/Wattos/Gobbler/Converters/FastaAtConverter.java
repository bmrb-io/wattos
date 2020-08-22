package Wattos.Gobbler.Converters;

import Wattos.Common.*;
import Wattos.Utils.*;

import java.io.*;
import java.util.regex.*;

/**
 *Purpose of this converter is to convert from the sequences of entries from at for
 *coding and with introns.
 *>At1g01010.1 68414.m00001 no apical meristem (NAM) family protein contains Pfam PF02365: No apical meristem (NAM) domain; similar to NAC domain protein NAM GB: AAD17313 GI:4325282 from [Arabidopsis thaliana]
ATGGAGGATCAAGTTGGGTTTGGGTTCCGTCCGAACGACGAGGAGCTCGTTGGTCACTAT
CTCCGTAACAAAATCGAAGGAAACACTAGCCGCGACGTTGAAGTAGCCATCAGCGAGGTC
AACATCTGTAGCTACGATCCTTGGAACTTGCGCTTCCAGTCAAAGTACAAATCGAGAGAT
 *
 * This converter will interpret the fasta records to a java class: orf. Mostly
 *containing a changed identifier set. 
 * @author Jurgen F. Doreleijers
 */
public class FastaAtConverter {
    
    private Orf orf = new Orf();
    private OrfId orfid = new OrfId(); 
    public Pattern p_summary;
    public Matcher m_summary;
    private String regexp_summary;
    public Pattern p_nucelotides_only;
    public Matcher m_nucelotides_only;
//    private String regexp_nucelotides_only;
    public Pattern p_good_seq_char;
    public Matcher m_good_seq_char;
        
    // Global to this class so decision can be relayed from method.
    String molecule_type    = "";
    int length              = 0;
    int total_count         = 0;
    int error_count         = 0;
    int warning_count       = 0;

    
    /** Creates a new instance of FastaPdbConverter */
    public FastaAtConverter() {
        /** Using new regexp in standard java 1.4...
         */
        try {
            //String line  = ">At1g01010.1 68414.m00001 no apical meristem (NAM) family protein contains Pfam PF02365: No apical meristem (NAM) domain; similar to NAC domain protein NAM GB: AAD17313 GI:4325282 from [Arabidopsis thaliana]";
            regexp_summary = "^>([\\w\\.]+) ([\\w\\.]+) .+$";
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
>gi|4507653|ref|NP_000358.1| thiopurine S-methyltransferase [Homo sapiens]
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
        
            // Should match: >Q6UFZ3/4-241 PF00244.8;14-3-3;
            // regexp_summary = "^>(\\w+)/(\\d+-\\d+)) +(PF\\w+);(.*)$;";
        orfid.orf_db_name       = "tigr";        
        orfid.orf_db_id         = m_summary.group(1);
        orfid.orf_db_subid      = m_summary.group(2);;
        orfid.molecule_name     = "";
                            
        return status;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        boolean testing = false;
        General.showOutput( "Starting FastaAtConverter.convert version 1");
        
        FastaAtConverter c = new FastaAtConverter();
        
        if ( testing ) {
            String line  = ">At1g01010.1 68414.m00001 no apical meristem (NAM) family protein contains Pfam PF02365: No apical meristem (NAM) domain; similar to NAC domain protein NAM GB: AAD17313 GI:4325282 from [Arabidopsis thaliana]";
            c.parseSummaryLine(line);
            General.showOutput("line: [" + line + "]");
            General.showOutput("orf id 1: [\n" + c.orfid.toFasta() + "]");            
            General.showOutput("orf     : [\n" + c.orf.toFasta() + "]");            
        } else {
            if ( args.length != 2) {
                General.showOutput("\nUsage:  java Wattos.Gobbler.Converters.FastaPfamAConverter " +
                "<fasta input file> <fasta output file>\n");
                System.exit(1);
            }
            String input_filename = args[0];
            String output_filename = args[1];
            General.showOutput("Reading  file        : "+input_filename);
            General.showOutput("Writing  file        : "+output_filename);
            boolean status = c.convert( input_filename, output_filename );
            if ( ! status ) {
                General.showError( "in FastaRefSeqConverter.convert.");
            }
        }
        General.showOutput("Done");
    }    
}
