/*
 * TranslateFasta.java
 *
 * Created on March 17, 2005, 5:00 PM
 */

package Wattos.Common;

import Wattos.Utils.*;

/**
 *Program to translate from a nucleic acid to protein sequence or vice versa.
 *<PRE>
 *USAGE: java Wattos.Common.TranslateFasta [-proteinToDNA] file_in_name file_out_name
 *</PRE>
 * @author  jurgen
 */
public class TranslateFasta {
    
    /** Creates a new instance of TranslateFasta */
    public TranslateFasta() {
    }
    
    public static void show_usage() {
        General.showOutput("USAGE: java Wattos.Common.TranslateFasta [-proteinToDNA] file_in_name file_out_name");
        General.showOutput("       -proteinToDNA   :   reverse the translation [optional].");
        General.showOutput("       file_in_name   :   name of the first file to read.");
        General.showOutput("       file_out_name  :   name of the file to write.");
        System.exit(1);
    }
    
    /**
     * Main of class can be used to translate a fasta files. The resulting file will have
     * the sequence in protein space of DNA space if such selected.
     * @param args Two file names with qualifying paths.
     *
        USAGE: java Wattos.Common.TranslateFasta [-proteinToDNA] file_in_name file_out_name
               -proteinToDNA   :   reverse the translation [optional].
               file_in_name   :   name of the first file to read.
               file_out_name  :   name of the file to write.
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        //General.verbosity = General.verbosityOutput;
        boolean proteinToDNA = false;
        int i = 0;
        if ( args.length < 2 ) {
            General.showError("Need at least 2 arguments.");
            show_usage();
        }        
        if ( args[0].equals("-proteinToDNA") ) {
            proteinToDNA = true;
            i++;
            /** Checks of input */
            if ( args.length != 3 ) {
                General.showError("Need 3 arguments.");
                show_usage();
            }        
        } else {
            /** Checks of input */
            if ( args.length != 2 ) {
                General.showError("Need 2 arguments.");
                show_usage();
            }                    
        }

        String list_i_name      = args[i++];
        String list_o_name      = args[i++];
        FastaList fal = new FastaList();
        
        General.showOutput("Reading file: " + list_i_name );
        if ( ! fal.readFastaFile(  list_i_name )) {
            General.doCodeBugExit("Failed to read first fastr file: " + list_i_name);
        }
        if ( ! fal.translateSequence( ! proteinToDNA) ) {
            General.doCodeBugExit("Failed to translate the DNA sequence to protein or vice versa: " + list_i_name);
        }
        General.showOutput("Writing file: " + list_o_name );
        fal.writeFastaFile( list_o_name );
    }
}
