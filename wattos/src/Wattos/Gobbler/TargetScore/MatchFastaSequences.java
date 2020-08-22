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
 * Match orfs based on the sequence.
 *Input 1: Fasta file
 *Input 2: Fasta file
 *Output : Fasta file with orf id from input 2 and orf sequence from input 1
 *Output will only contain orfs for which the sequences occured in both
 *input files.
 * @author Jurgen F. Doreleijers
 */
public class MatchFastaSequences {
    
    /** Creates a new instance of MatchFastaSequences */
    public MatchFastaSequences() {
    }
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.MatchFastaSequences [-outputFastr] file_1_name file_2_name file_out_name");
        General.showOutput("       -outputFastr   :   set for fastr file output.");
        General.showOutput("       file_1_name    :   name of the first file to read.");
        General.showOutput("       file_2_name    :   name of the second file to read.");
        General.showOutput("       file_out_name  :   name of Fasta file with orf id from input 2 and orf sequence from input 1.");
        General.showOutput("NOTES: to extract both ids as comma separated values is easy in Unix:");
        General.showOutput("cut -d'|' -f2,6 --output-delimiter=\",\" file_out_name");
        
        System.exit(1);
    }
    
    /**
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        
        /** Checks of input */
        if ( args.length < 3 || args.length > 4 ) {
            General.showError("Need 3 or 4 arguments.");
            show_usage();
        }
        boolean printMatchesAsCsv = false;
        int i=0;
        if (  args.length == 4 ) {
            String v = args[i++];
            if ( v.equalsIgnoreCase("-outputFastr") ) {
                printMatchesAsCsv = true;
            } else {
                General.showError("Option failed to be matched to true; leave it out for false");
            }            
        }
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
        
        if ( ! printMatchesAsCsv ) {
            fl_1.replaceOrfIdListFrom( fl_2 );        
            fl_2.clear();

            General.showOutput("Writing file                              : " + list_o_name );
            fl_1.writeFastaFile( list_o_name );
        } else {
            FastResultList frl = fl_1.combineOnSequenceToFastResultList( fl_2 );
            General.showOutput("Writing file                              : " + list_o_name );
            frl.writeFastrFile( list_o_name );            
        }
        
        //General.showOutput("Done");
    }
}
