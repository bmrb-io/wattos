/* 
 *
 * BlastFilter.java
 *
 * Created on December 12, 2002, 11:24 AM
 */

package Wattos.Gobbler.Converters;

import Wattos.Gobbler.BlastMatchLoLoL;  
import Wattos.Utils.*;

/**
 *Converts blast output to Java's binary format.
 *@author Jurgen F. Doreleijers
 * @version 1
 */
public class BlastTxt2Bin{

    public static boolean debug = false;
    
    /** Creates new BlastFilter */
    public BlastTxt2Bin() {
    }       
    
    /** Convert blast output to binary output of BlastMatchLoL instance.
     * @param args <PRE>
     * 1. input files dir name of many binary blast output
     * 2. output file name of 1 binary output of BlastMatchLoL instance
     * </PRE>
     */
    public static void main (String args[]) {
        if ( args.length != 3) {
            General.showOutput("\nUsage:  java -Xmx512m Wattos.Gobbler.Converters.BlastTxt2Bin " +
            "<blast text input  directory> " +
            "<blast bin output directory> " +
            "<verbosity>");
            General.showOutput("verbosity:          0-9:0 no output, 2 normal, including warnings, 3 no warnings, 9 debug");
            System.exit(1);
        }
        String input_files_dir_name = args[0];
        String output_files_dir_name = args[1];
        int verbosity = Integer.parseInt( args[2] );
        General.showOutput("Reading  files from dir: "+input_files_dir_name);
        General.showOutput("Writing files to dir  : "+output_files_dir_name);
        boolean status = BlastMatchLoLoL.convertBlastTxt2Bin( input_files_dir_name, output_files_dir_name, verbosity );
        if ( ! status ) {
            General.showError( "in BlastMatchLoLoL.convertBlastTxt2Bin.");
            System.exit(1);
        } else {
            General.showOutput("Done successfully with Wattos.Gobbler.Converters.BlastTxt2Bin");
        }
    }
}
