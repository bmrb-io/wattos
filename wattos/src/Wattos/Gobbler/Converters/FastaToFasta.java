/*  
 * FastaToFastaForBlast.java
 *
 * Created on February 10, 2003, 3:00 PM
 */

package Wattos.Gobbler.Converters;

import Wattos.Common.*;
import Wattos.Utils.*;

/**
 *Program to reformat fasta files. Run main without arguments to see the
 *way to call it.
 *@see Wattos.Common.FastaDefinitions#convertFastaFile
 * @author Jurgen F. Doreleijers
 */
public class FastaToFasta {
    
    /** Creates a new instance of FastaToFastaForBlast */
    public FastaToFasta() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( args.length != 6) {
            
            General.showOutput("\nUsage:  java Wattos.Gobbler.Converters.FastaToFasta\n" +
            "<fasta input file>         <fasta output file>\n" +
            "<fasta input file flavor>  <fasta output file flavor>\n" +
            "<filter_type>              <verbosity>" );
            General.showOutput("<fasta input file flavor>  cesg");
            General.showOutput("<filter_type>              none|blastdb-suitable");
            General.showOutput("<verbosity>                [0-9]");
            System.exit(1);
        }
        String input_filename       = args[0];
        String output_filename      = args[1];
        String input_flavor         = args[2];
        String output_flavor        = args[3];
        String filter_type          = args[4];
        int verbosity               = Integer.parseInt( args[5] );
        General.showOutput("Reading  file        : "+input_filename     +" with flavor: " + input_flavor);
        General.showOutput("Writing  file        : "+output_filename    +" with flavor: " + output_flavor);
        General.showOutput("Using filter type    : "+filter_type);

        
        boolean status = FastaDefinitions.convertFastaFile( input_filename, output_filename, input_flavor,
           output_flavor, filter_type, verbosity );
        if ( ! status ) {
            General.showError( "in convertFastaFile.");
            System.exit(1);
        }        
        General.showOutput("Done successfully.");
    }
    
}
