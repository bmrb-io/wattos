package Wattos.Gobbler.Converters;

import Wattos.Utils.*;

import Wattos.Gobbler.BlastMatchLoLoL;

/** This class reads all binary (*.bin) BlastMatchLoL files in a directory and writes
 * multiple csv files.
 * @see Wattos.Gobbler
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class BlastBin2Csv {

    public static boolean debug = false;
    
    public BlastBin2Csv() {
    }       
    
    
    /** Convert binary output to Csv.
     */
    public static void main (String args[]) {

        // Catch wrong number of arguments or help requests

        if ( args.length != 3) {
            General.showOutput("\nUsage:  java -Xmx512m Wattos.Gobbler.Converters.BlastBin2Csv " +
                "<blast binary output dir pathname> " +
                "<blast csv    output dir pathname> " +
                "<use the bmrb style of output true|false>"
            );
            General.showOutput("Gave number of arguments: " + args.length );
            System.exit(1);
        }
        General.verbosity = General.verbosityOutput;
        
        String input_dirname    = args[0];
        String output_dirname   = args[1];
        boolean useBmrbStyle = Boolean.valueOf( args[2] ).booleanValue();
        
        General.showOutput("Reading  files in dir                   : "+input_dirname);
        General.showOutput("Writing  files in dir                   : "+output_dirname);
        //General.showOutput("Using BMRB style                : "+useBmrbStyle);
        
        boolean status = BlastMatchLoLoL.convertBin2Csv( input_dirname, output_dirname, useBmrbStyle );
        if ( ! status ) {
            General.showError( "in BlastMatchLoLoL.convertBin2Csv.");
            System.exit(1);
        } else {
            General.showOutput("Done successfully with Wattos.Gobbler.Converters.BlastBin2Csv");
        }
    }
}
