/* 
 *
 * BlastFilter.java
 *
 * Created on December 12, 2002, 11:24 AM
 */

package Wattos.Gobbler.Converters;

import Wattos.Gobbler.*;
import Wattos.Utils.*; 

/**
 *Converts blast output to Java's binary format and csv format.
 *@author Jurgen F. Doreleijers
 * @version 1
 * @see Wattos.Gobbler
 */
public class BlastTxt2BinAndCsv {

    public static boolean debug = false;
    
    /** Creates new BlastFilter */
    public BlastTxt2BinAndCsv() {
    }       
    
    /** Convert blast output to binary and csv output 
     * @param args <PRE>
     * 1. input file name
     * 2. output file name binary
     * 3. output file name csv
     "<use the bmrb style of output true|false>"
     * </PRE>
     */
    public static void main (String args[]) {
        
        args = PrimitiveArray.stripDashedMembers( args );
        boolean testing = false;
        if ( testing ) {
            General.verbosity = General.verbosityDebug;
            General.showOutput("Setting variables for testing");
            String input_file_name      = "S:\\jurgen\\t.blast";
            String output_file_bin_name = "S:\\jurgen\\t.bin";
            String output_file_csv_name = "S:\\jurgen\\t.csv";
            boolean useBmrbStyle = true;
            int verbosity = 4;            
            args = new String[] {
                input_file_name,
                output_file_bin_name,
                output_file_csv_name,
                (Boolean.valueOf( useBmrbStyle ).toString()),
                (new Integer( verbosity ).toString())
            };
        }
        
        General.showOutput("BlastTxt2BinAndCsv version: 6/22/2005");
        if ( args.length != 5) {
            General.showError("Didn't find the expected number of arguments\n" +
            "Usage:  java -Xmx128m Wattos.Gobbler.Converters.BlastTxt2BinAndCsv " +
            "<blast text input file> \n" +
            "<blast bin output file> \n" +
            "<blast csv output file> \n" +
            "<use the bmrb style of output true|false>\n" +
            "<verbosity>");
            General.showError("verbosity:          0-9:0 no output, 2 normal, including warnings, 3 no warnings, 9 debug");
            General.showError("Arguments found are: " + PrimitiveArray.toString(args));
            System.exit(1);
        }
        String input_file_name      = args[0];
        String output_file_bin_name = args[1];
        String output_file_csv_name = args[2];
        boolean useBmrbStyle = Boolean.valueOf( args[3] ).booleanValue();
        int verbosity = Integer.parseInt( args[4] );
        
        General.verbosity = verbosity;
        
        General.showOutput("Reading input file from : "+input_file_name);
        General.showOutput("Writing bin file to     : "+output_file_bin_name);
        General.showOutput("Writing csv file to     : "+output_file_csv_name);
        
        boolean overall_status = true;        
        boolean status = false;        
        try { // Catch any throwable and report
            status = BlastParser.convertBlast(input_file_name, output_file_bin_name, verbosity);        
            if ( ! status ) {
                General.showError("Converting blast text output to binary representation: " + input_file_name);
                overall_status = false;
            }
            if ( status ) {
                status = BlastMatchLoLoL.convertBin2CsvSingleFile( output_file_bin_name, output_file_csv_name, useBmrbStyle );
                if ( ! status ) {
                    General.showError("Converting blast bin output to csv representation: " + output_file_bin_name);
                    overall_status = false;
                }
            }            
        } catch ( Throwable t ) {
            General.showThrowable(t);
        }
        
        if ( ! overall_status ) {
            General.showError("Failed in Wattos.Gobbler.Converters.BlastTxt2BinAndCsv.");
            System.exit(1);
        }
        General.showOutput("Done successfully with Wattos.Gobbler.Converters.BlastTxt2BinAndCsv");
    }
}
