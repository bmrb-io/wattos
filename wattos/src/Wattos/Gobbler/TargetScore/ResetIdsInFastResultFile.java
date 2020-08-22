/*
 * ResetIdsInFastResultFile.java
 *
 *
 * Created on February 1, 2005, 3:28 PM
 */

package Wattos.Gobbler.TargetScore;

import Wattos.Common.*;
import Wattos.Utils.*;


/**
 * Replace first orfid with one mapped in the fasta input file
 *Input 1: FastResult file
 *Input 2: Fasta file
 *Output : FastResult file with results from fast result input and one orf id from fasta input.
 *Output will only contain orfs for which the mapping was available in the fasta file.
 * @author Jurgen F. Doreleijers
 */
public class ResetIdsInFastResultFile {
    
    /** Creates a new instance of MatchFastaSequences */
    public ResetIdsInFastResultFile() {
    }
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.ResetIdsInFastResultFile file_fastresult_in_name file_fasta_in_name file_fastresult_out_name");
        General.showOutput("       file_fastresult_in_name    :   name of the fastresult file to re-id.");
        General.showOutput("       file_fasta_in_name         :   name of the fasta file with mappings for the original ids in the fastresult file.");
        General.showOutput("       file_fastresult_out_name   :   name of fastresult file with orf id from file_fasta_in_name and results from fastresult input file.");
        General.showOutput("NOTES:  - to extract id and the first two results fields as comma separated values is easy in Unix:");
        General.showOutput("        cut -d'|' -f2,6,7 --output-delimiter=\",\" file_fastresult_out_name");        
        General.showOutput("        - The first id in the fastresult file will become the second id in the fasta mapping file.");        
        General.showOutput("        - >|A|||.. with >|A|||...||B|||... will become: >|B|||... ");        
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
//        boolean printMatchesAsCsv = false;
        int i=0;
        String file_fastresult_in_name      = args[i++];
        String file_fasta_in_name           = args[i++];
        String file_fastresult_out_name     = args[i++];
        FastResultList frl = new FastResultList();
        FastaList      fl  = new FastaList();
        
        General.showOutput("Reading Fastr file                        : " + file_fastresult_in_name );
        if ( ! frl.readFastrFile( file_fastresult_in_name )) {
            General.doCodeBugExit("Failed to read first fastr file: " + file_fastresult_in_name);
        }
        
        General.showOutput("Reading Fasta file                        : " + file_fasta_in_name );
        if ( ! fl.readFastaFile(  file_fasta_in_name )) {
            General.doCodeBugExit("Failed to read second fasta file: " + file_fasta_in_name);
        }
        frl.replaceOrfIdFromFastaMapping( fl );
        
        General.showOutput("Writing file                              : " + file_fastresult_out_name );
        frl.writeFastrFile( file_fastresult_out_name );            
        
        General.showOutput("Done");
    }
}
