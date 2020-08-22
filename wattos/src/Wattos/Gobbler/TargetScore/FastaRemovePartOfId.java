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
 * Remove a part of the id.
 *Input  : Fasta file
 *part   : [orf_db_name, orf_db_id, orf_db_subid, molecule_name]
 *Output : Fasta file with orf id part nilled.
 *Note: if the file only has a single orf id then the same could be achieved
 *much easier with the unix command cut.
 * @author Jurgen F. Doreleijers
 */
public class FastaRemovePartOfId {
    
    /** Creates a new instance of MatchFastaSequences */
    public FastaRemovePartOfId() {
    }
    
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.FastaRemovePartOfId file_in_name part file_out_name");
        General.showOutput("       file_in_name     :   name of the fasta file to read.");
        General.showOutput("       part             :   [orf_db_name, orf_db_id, orf_db_subid, molecule_name].");
        General.showOutput("       file_out_name    :   name of the fasta file to write.");
        General.showOutput("Note: if the file only has a single orf id then the same could be achieved");
        General.showOutput("much easier with the unix command cut.");
        
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
        String part             = args[i++];
        String list_o_name      = args[i++];
        FastaList fl = new FastaList();
        
        General.showOutput("Reading file                              : " + list_i_name );
        if ( ! fl.readFastaFile(  list_i_name )) {
            General.doCodeBugExit("Failed to read first fasta file: " + list_i_name);
        }
        

        if ( ! fl.removePartOrfId( part )) {
            General.showError("failed to removePartOrfId");
            System.exit(1);
        }
        
        General.showOutput("Writing file                              : " + list_o_name );
        fl.writeFastaFile( list_o_name );
    }
}
