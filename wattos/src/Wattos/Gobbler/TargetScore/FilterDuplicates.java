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
 * Remove an orf from a fasta list if the sequence or the id is the same.
 *Input  : Fasta file
 *part   : [sequence, id]
 *Output : Fasta file without duplicate orfs.
 * @author Jurgen F. Doreleijers
 */
public class FilterDuplicates {
    
    /** Creates a new instance of MatchFastaSequences */
    public FilterDuplicates() {
    }
    
    
    
    public static void show_usage() {
        General.showError("Code needs to be done still.");
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.FilterDuplicates file_in_name part file_out_name");
        General.showOutput("       file_in_name     :   name of the fasta file to read.");
        General.showOutput("       part             :   [sequence, id].");
        General.showOutput("       file_out_name    :   name of the fasta file to write.");
        General.showOutput("Note: Code is unfinished.");        
        System.exit(1);
    }
    
    /**
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        
        /** Checks of input */
        if ( args.length != 3 ) {
            General.showError("Need 3 arguments.");
            show_usage();
        }
        int i = 0;
        String list_i_name      = args[i++];
                                       i++;
        String list_o_name      = args[i++];
        FastaList fl = new FastaList();
        
        General.showOutput("Reading file                              : " + list_i_name );
        if ( ! fl.readFastaFile(  list_i_name )) {
            General.doCodeBugExit("Failed to read first fasta file: " + list_i_name);
        }
        
/**
        if ( ! fl.filterDuplicates( part )) {
            General.showError("failed to filterDuplicates");
            System.exit(1);
        }
   
 */     
        General.showOutput("Writing file                              : " + list_o_name );
        fl.writeFastaFile( list_o_name );
    }
}
