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
 * Match orfs based on the id.
 *Input 1: Fasta file
 *Input 2: Fasta file
 *Output : Fasta file with only those orfs in input 2 that are also in input 1.
 * Comparable to join -v 2 aa_tmp.txt nt_tmp.txt > join_tmp_v2.txt
 * but then it does it for the whole orf in stead of just the orf ids.
 * @author Jurgen F. Doreleijers
 */
public class FilterDuplicatesBetweenFastaFiles {
    
    /** Creates a new instance of MatchFastaSequences */
    public FilterDuplicatesBetweenFastaFiles() {
    }
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.FilterDuplicatesBetweenFastaFiles file_1_name file_2_name file_out_name");
        General.showOutput("       file_1_name    :   name of the first fasta file to read.");
        General.showOutput("       file_2_name    :   name of the second fasta file to read.");
        General.showOutput("       file_out_name  :   name of Fasta file with only those orfs from input 2 that have the same orf present in input 1.");
        General.showOutput("NOTES: to extract both ids as comma separated values is easy in Unix:");
        General.showOutput("            cut -d'|' -f2,6 --output-delimiter=\",\" file_out_name");
        General.showOutput("       similar to:");
        General.showOutput("            join -v 2 file_1_name file_2_name > file_out_name");
        
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
        int i=0;
        String list_1_name      = args[i++];
        String list_2_name      = args[i++];
        String list_o_name      = args[i++];
        FastaList fl_1 = new FastaList();
        FastaList fl_2 = new FastaList();
        
        General.showOutput("Reading file                              : " + list_1_name );
        if ( ! fl_1.readFastaFile(  list_1_name )) {
            General.doCodeBugExit("Failed to read first fasta file: " + list_1_name);
        }
        
        General.showOutput("Reading file                              : " + list_2_name );
        if ( ! fl_2.readFastaFile(  list_2_name )) {
            General.doCodeBugExit("Failed to read second fasta file: " + list_2_name);
        }
        
        fl_2.filterDuplicatesBetweenFastaFiles( fl_1 );        
        fl_1.clear();

        General.showOutput("Writing file                              : " + list_o_name );
        fl_2.writeFastaFile( list_o_name );
        
        General.showOutput("Done");
    }
}
