package Wattos.Gobbler.TargetScore;

import Wattos.Utils.*;
import Wattos.Common.*;
import java.io.*;

/**
 * Given a regular expression for the residue type(s) the program
 * will count them in the input fasta file and return a
 * fasta like file called fastr file for "fasta-like result" file.
 * The code is fast enough to run on many-Mb files.
 * @author Jurgen F. Doreleijers
 *@see Wattos.Common.FastResult
 */
public class CountResidueTypes {
    
    public CountResidueTypes() {
    }
    
    
    /**
     * Will keep simple stats on file overall too.
     * @param input_filename
     * @param output_filename
     * @param regexp_sub_seq
     */
    public static boolean count( String input_filename, String output_filename, String regexp_sub_seq ) {
                
        int count_read = 0;
        
        try {
            FileReader fr = new FileReader( input_filename );
            LineNumberReader inputReader = new LineNumberReader( fr );
            BufferedWriter bw = new BufferedWriter( new FileWriter( output_filename));
            PrintWriter outputWriter = new PrintWriter( bw );

            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( ! inputReader.ready() ) {
                General.showDebug("input not ready or just empty..");
                return false; 
            }
            
            FastResult fastResult = new FastResult();
            Orf orf = new Orf();
            String line = inputReader.readLine();
//            String sequence = "";
            
            while ( line != null ) {
                //General.showDebug("Processing line: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? */                
                if ( !(( line.length() > 0 ) && 
                       ( line.charAt(0) == FastaDefinitions.FASTA_START_CHAR ))) {
                    orf.sequence = orf.sequence + line;
                    line = inputReader.readLine();
                    continue;
                }                
                count_read++;
                // First line doesn't need a save of previous orf.
                if ( inputReader.getLineNumber() != 1 ) {
                    orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
                    if ( orf.sequence.length() == 0 ) {
                        General.showWarning("Sequence is empty for orf: " + orf.toFasta());
                    }
                    int r1 = Strings.countStrings( orf.sequence, regexp_sub_seq );
                    if ( r1 < 0 ) {
                        General.showError("Failed to do getCountFromSeq with regexp_sub_seq: [" + regexp_sub_seq + "]");
                        General.showError("on sequence: ["+orf.sequence+"]");
                        return false;
                    }
                    fastResult.result.add( new Integer(orf.sequence.length()) );
                    fastResult.result.add( new Integer(r1) );
                    fastResult.result.add( new Float(100*r1/((float)orf.sequence.length())));
                    fastResult.oiList = orf.orf_id_list;
                    outputWriter.write( fastResult.toFastr() );                            
                    orf.init();
                    fastResult.init();
                }
                boolean status = orf.orf_id_list.readFasta( line ); // Read header.
                if ( ! status ) {
                    General.showError("Will skip this entry because of parse error.");
                }                   
                line = inputReader.readLine();
            }
            // Handle last sequence in file.
            orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
            int r1 = Strings.countStrings( orf.sequence, regexp_sub_seq );
            if ( r1 < 0 ) {
                General.showError("Failed to do getCountFromSeq with regexp_sub_seq: [" + regexp_sub_seq + "]");
                return false;
            }
            fastResult.result.add( new Integer(orf.sequence.length()) );
            fastResult.result.add( new Integer(r1) );
            fastResult.result.add( new Float(100*r1/((float)orf.sequence.length())));
            fastResult.oiList = orf.orf_id_list;
            outputWriter.write( fastResult.toFastr() );                            
            inputReader.close();
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Read sequences   : " + count_read);
        return true;        
    }
        
    /**
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( args.length != 3) {            
            General.showOutput("\nUsage:  java Wattos.Gobbler.TargetScore.CountResidueTypes.\n" +
                               "<fasta input file>   <fastr output file> <reg-exp sub sequence>\n" );
            System.exit(1);
        }
        General.verbosity = General.verbosityDebug;
        String input_filename       = args[0];
        String output_filename      = args[1];
        String regexp               = args[2];
        
        General.showOutput("Reading  file        : "+input_filename     );
        General.showOutput("Writing  file        : "+output_filename    );
        boolean status = CountResidueTypes.count( input_filename, output_filename, regexp );
        General.showOutput("Done");
        if ( ! status ) {
            General.doCodeBugExit("Failed to count the residue types");
        }
    }
    
}
