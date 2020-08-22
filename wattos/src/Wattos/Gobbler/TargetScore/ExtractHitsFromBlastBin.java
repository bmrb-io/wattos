/*
 * ExtractHitsFromBlastBin.java
 */

package Wattos.Gobbler.TargetScore;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Gobbler.*;
import Wattos.Common.*;

/** This class reads all binary (*.bin) BlastMatchLoL files in a directory and writes
 * one binary file with multiple BlastMatchLoL objects.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class ExtractHitsFromBlastBin {
    
    // Minimum value for a match to be considered. Default setting in blast
    // is 10 or 1. Todo check.
    public static double E_VALUE_CUT_OFF = 1E-06;
    
    public ExtractHitsFromBlastBin() {
    }       

    /** Convert binary files into fastr files using method in BlastMatchLoL
     * @return  status */
    public static boolean convertBin2Fastr( String input_dir_name,
        String output_dir_name, boolean extractFromTargetDB ) {
        
        boolean printHashes = false;
        final int PRINT_HASH_SIZE = 10;
        final int PRINT_HASH_ROW_SIZE = 80;
        
        String fs = File.separator;
        
        File homedir = new File( input_dir_name );
        // Implement the interface as an inner class for convenience; it's such a small baby.
        FilenameFilter filefilter = new FilenameFilter() {
            int MIN_FILE_SIZE = 100;
            public boolean accept( File d, String name) {
                boolean status_1 = name.endsWith( BlastMatch.EXTENSION_BLAST_OUTPUT_BIN );
                if ( ! status_1 ) {
                    return false;
                }
                String fullfilename = d.getAbsoluteFile() + File.separator + name;
                File f = new File( fullfilename );
                //General.showDebug("checking file: [" + f.getAbsolutePath() + "]");
                boolean status_2 = f.length() > MIN_FILE_SIZE;
                if ( ! status_2 ) {
                    General.showWarning("skipping small file: [" + name +
                    "] length in bytes is: " + f.length() + " but should be larger than: " + MIN_FILE_SIZE);
                }
                return status_1 && status_2;
            }
        };
        String[] bin_files = homedir.list(filefilter);
        
        if ( bin_files == null ) {
            General.showError("Finding blast bin files.");
            return false;
        }
        
        if ( bin_files.length == 0 ) {
            General.showError("Finding zero blast bin files in this dir.");
            return false;
        }
        
        /** Sort the files according to there name */
        Arrays.sort( bin_files );
        
        General.showOutput("Converting files: " + bin_files.length );
        General.showOutput("Converting files: " + Strings.toString( bin_files ) );
        
        try {
            for (int i=0; i<bin_files.length; i++) {

                String input_file_name = input_dir_name + fs + bin_files[i];
                FileInputStream file_in = new FileInputStream( input_file_name );
                ObjectInputStream in = new ObjectInputStream( file_in );

                String output_file_name = output_dir_name + fs + InOut.changeFileNameExtension( bin_files[i], "fastr");
                BufferedWriter bw = new BufferedWriter( new FileWriter( output_file_name));
                PrintWriter outputWriter = new PrintWriter( bw );
                if ( outputWriter == null ) {
                    General.showError("initializing PrintWriter");
                    return false;
                }
                if ( outputWriter.checkError() ) {
                    General.showError("output got an error before starting.");
                    return false; 
                }

                
                General.showOutput("Converting file : " + input_file_name );
                General.showDebug("to file          : " + output_file_name );
                
                int m=0;
                while ( true ) {
                    Object o = InOut.readObjectOrEOF( in );
                    if ( o.equals( InOut.END_OF_FILE_ENCOUNTERED ) ) {
                        //General.showOutputNoEol("Reached end of bin file: " + input_bin_file_name);
                        break;
                    }
                    BlastMatchList blastmatch_list = (BlastMatchList) o;
                    if ( blastmatch_list == null ) {
                        break;
                    }
                    // Show we're doing something.
                    if ( printHashes ) {
                        if ( m != 0 ) {
                            if ( (m % PRINT_HASH_SIZE) == 0 ) {
                                General.showOutputNoEol("#");
                            }
                            if ((m % (PRINT_HASH_SIZE*PRINT_HASH_ROW_SIZE)) == 0 )  {
                                General.showOutput("");
                            }
                        }
                    }
                    
                    // Next routine modifies list so watch out.
                    BlastMatch bm = getBestHitForTargetSelection( blastmatch_list, extractFromTargetDB );
                    if ( blastmatch_list.match_list.size() < 1 ) {
                        General.showDebug("No hit in blastmatchlist : " + m + " : " + blastmatch_list.toString());                        
                        continue;
                    }
                                        
                    FastResult fastr =  new FastResult();
                    fastr.oiList.orfIdList = blastmatch_list.query_orf_id_list.orfIdList;
                    OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0);
                    fastr.result.add(  orfId.orf_db_name );
                    fastr.result.add(  orfId.orf_db_id );
                    fastr.result.add(  orfId.orf_db_subid );
                    fastr.result.add(  orfId.molecule_name );
                    fastr.result.add(  new Double(bm.expectation_value) );
                    fastr.result.add(  new Integer(bm.match_size) );
                    fastr.result.add(  new Integer(bm.number_identities) );
                    fastr.result.add(  new Float(100.0d*bm.number_identities/bm.match_size) );
                    outputWriter.write( fastr.toFastr());
                    m++;
                }                
            
                outputWriter.close();
                in.close();
                //General.showMemoryUsed();
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }
    
    
       
    public static BlastMatch getBestHitForTargetSelection(BlastMatchList blastmatch_list, boolean
        extractFromTargetDB ) {
        
        //Removing matches below cut-off.
        for (int i=0;i<blastmatch_list.match_list.size();i++) {
            BlastMatch bm = (BlastMatch) blastmatch_list.match_list.get(i);            
            if (bm.expectation_value>E_VALUE_CUT_OFF) {
//                OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0); // assume only 1
                //General.showDebug("Removing hit above e value cut off: " + bm.expectation_value + " for orf: " + orfId);
                blastmatch_list.match_list.remove(i);
                i--; // do it agin sam.
            }
        }

        //Removing self hit (from CESG with identical id (irrespectively of "Go." prefix)
        OrfId queryOrfId = (OrfId) blastmatch_list.query_orf_id_list.orfIdList.get(0);
        for (int i=0;i<blastmatch_list.match_list.size();i++) {
            BlastMatch bm = (BlastMatch) blastmatch_list.match_list.get(i);
            OrfId orfId = (OrfId) bm.subject_orf_id_list.orfIdList.get(0); // assume only 1
            if (orfId.orf_db_name.startsWith("CESG")) {
                if ( GoId.equalGoId( queryOrfId.orf_db_id, orfId.orf_db_id) ) {
                    //General.showDebug("Removing self hit: " + orfId);
                    blastmatch_list.match_list.remove(i);                
                    i--; // do it again sam.
                }
            }
        }

        if (extractFromTargetDB) {
            blastmatch_list.sortByWorkDoneValueStructuralGenomics();
        }
        //General.showDebug("blastmatch list (D): " + blastmatch_list.toString());

        if ( blastmatch_list.match_list.size() < 1 ) {
            return null;
        }
        BlastMatch bm = (BlastMatch) blastmatch_list.match_list.get(0);
        return bm;
    }
    
    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.ExtractHitsFromBlastBin ");
        General.showOutput("<blast binary input  dir pathname> ");
        General.showOutput("<blast fastr  output dir pathname> ");
        General.showOutput("<extract From TargetDB>             | true or false");
        System.exit(1);
    }
    
    
    /** Convert binary output to fastr.
     *USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.ExtractHitsFromBlastBin
<blast binary input  dir pathname> 
<blast fastr  output dir pathname> 
<extractFromTargetDBStr>
     */
    public static void main (String args[]) {

        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;

        if ( args.length != 3) {
            General.showOutput("Gave number of arguments: " + args.length );
            show_usage();
        }
        
        String input_dirname    = args[0];
        String output_dirname   = args[1];
        String extractFromTargetDBStr = args[2];
        
        General.showOutput("Reading  files in dir                   : "+input_dirname);
        General.showOutput("Writing  files in dir                   : "+output_dirname);
        
        boolean extractFromTargetDB = Strings.parseBoolean( extractFromTargetDBStr );
        boolean status = convertBin2Fastr( input_dirname, output_dirname, extractFromTargetDB  );
        if ( ! status ) {
            General.showError( "in Wattos.Gobbler.TargetScore.ExtractHitsFromBlastBin.");
            System.exit(1);
        } else {
            General.showOutput("Done successfully with Wattos.Gobbler.TargetScore.ExtractHitsFromBlastBin");
        }
    }
}
