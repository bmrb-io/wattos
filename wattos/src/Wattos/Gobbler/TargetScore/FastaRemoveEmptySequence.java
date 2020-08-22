/*
 * MatchFastaSequences.java
 *
 *
 * Created on February 1, 2005, 3:28 PM
 */

package Wattos.Gobbler.TargetScore;

import Wattos.Common.*;
import Wattos.Utils.*;

/**
 * Remove those orfs that have an empty sequence.
 *Input  : Fasta file
 *Output : Fasta file without orfs with no sequence
 * @author Jurgen F. Doreleijers
 */
public class FastaRemoveEmptySequence {
    
    /** Creates a new instance of MatchFastaSequences */
    public FastaRemoveEmptySequence() {
    }
    
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java Wattos.Gobbler.TargetScore.FastaRemoveEmptySequence file_in_name file_out_name");
        General.showOutput("       file_in_name     :   name of the fasta file to read.");
        General.showOutput("       file_out_name    :   name of the fasta file to write.");
        
        System.exit(1);
    }
    
    /**
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        
        /** Checks of input */
        if ( args.length != 2 ) {
            General.showError("Need 2 arguments.");
            show_usage();
        }
        int i = 0;
        String list_i_name      = args[i++];
        String list_o_name      = args[i++];
        FastaList fl = new FastaList();
        
        General.showOutput("Reading file                              : " + list_i_name );
        if ( ! fl.readFastaFile(  list_i_name )) {
            General.doCodeBugExit("Failed to read first fasta file: " + list_i_name);
        }
        

        if ( ! fl.removeEmptySequence( )) {
            General.showError("failed to removeEmptySequence");
            System.exit(1);
        }
        
        General.showOutput("Writing file                              : " + list_o_name );
        fl.writeFastaFile( list_o_name );
    }
}
