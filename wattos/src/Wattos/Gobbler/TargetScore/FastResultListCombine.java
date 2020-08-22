package Wattos.Gobbler.TargetScore;

import Wattos.Utils.*;
import Wattos.Common.*;
import java.io.*;
import java.util.*;

/** Program to add parameters associated with a sequence to another of such (fastr) files.
 *@see Wattos.Common.FastResultList
 */
public class FastResultListCombine {
    
    /** Creates a new instance of FastResult */
    public FastResultListCombine() {
    }
            
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.FastResultListCombine base_name default_add_file file_out_name [list of extensions]");
        General.showOutput("       base_name            : base name of the files to read.");
        General.showOutput("       default_add_file     : full name of the default file to read if given file doesn't exist.");
        General.showOutput("       file_out_name        : name of the file to write.");
        General.showOutput("       [list of extensions] : a space separated list of filename extensions (skip the assumed '_' separator.");
        System.exit(1);
    }
    
    /**
     * Can be used to combine many fastr files together fast. The resulting file will have
     * a fixed number of columns for the result section which is equal to the sum of the number of
     * columns in the first record of each file.
     * The results will be matched on the basis of the first orf id comparing only the fields:
     * orf_db_id and orf_db_sub_id assuming that the orf_db_names are already the same. 
     * The code will check that the the orf_db_names are the same for the first record.
     * <BR>
     * Notes:<BR>
     * <PRE>
     * This works well for under a million records. E.g.:
     * -rw-r--r--    1 jurgen   None     20,724,080 Jan 27 17:01 u.fastr
     * S:\jurgen\BioInf\tmp_unb_>java -Xmx256m Wattos.Common.FastResultList u.fastr d.fastr
     * Starting
     * Read records: 279580
     * Wrote records: 279580
     * Done
     * </PRE>
     * @param args Two file names with qualifying paths.
     *
USAGE: see usage routine code
     */
    public static void main(String[] args) {
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;
        /** Checks of input */
        if ( args.length < 4 ) {
            General.showError("Need at least 4 arguments in order to do something useful.");
            show_usage();
        }                    
        String base_name        = args[0];
        String default_add_file = args[1];
        String file_o_name      = args[2];
        ArrayList list = new ArrayList (Arrays.asList( args ));
        list.remove(0);
        list.remove(0);
        list.remove(0);
        
        FastResultList frl_1 = new FastResultList();
        
        String list_1_name = base_name + "_" + (String) list.get(0) + ".fastr";
        list.remove(0);
        
        General.showOutput("Reading FIRST file: " + list_1_name );
        if ( ! frl_1.readFastrFile(  list_1_name )) {
            General.doCodeBugExit("Failed to read first fastr file: " + list_1_name);
        }

        for (int i=0;i<list.size();i++) {
            FastResultList frl_2 = new FastResultList();
            String list_2_name = base_name + "_" + (String) list.get(i) + ".fastr";
            File file_2 = new File(list_2_name);
            if ( ! file_2.exists() ) {
                General.showWarning("using default add file for file specified didn't exist: " + list_2_name);
                list_2_name = default_add_file;
            }
            General.showOutput("Reading file: " + list_2_name );
            if ( ! frl_2.readFastrFile(  list_2_name )) {
                General.doCodeBugExit("Failed to read fastr file: " + list_2_name);
            }
            General.showDebug("Copying all columns from second to first.");
            if ( ! frl_1.combineResultsById( frl_2, true )) {
                General.doCodeBugExit("Failed to combineResultsById");
            }
        }
         
        General.showOutput("Writing file: " + file_o_name );
        frl_1.writeFastrFile( file_o_name );
    }
}
