/* 
 * BlastMatchLoL.java
 *
 * Created on December 12, 2002, 10:45 AM
 */

package Wattos.Gobbler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import Wattos.Gobbler.Converters.BlastParser;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Strings;

import com.Ostermiller.util.CSVPrinter;

/**
 * This container class contains no objects just static methods. This class
 *has methods for dealing with collections of BlastMatchLoL objects. The whole
 *collection is likely to be larger than computer RAM memory should hold.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class BlastMatchLoLoL implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 8676225900363465050L;

	/** Convert one binary file into CSV file using method in BlastMatchLoL
     * @param input_file_name
     * @param output_file_name
     * @param useBmrbStyle
     * @return  status */
    public static boolean convertBin2CsvSingleFile( String input_file_name,
        String output_file_name, boolean useBmrbStyle ) {
            
        boolean printHashes = false;
        final int PRINT_HASH_SIZE = 10;
        final int PRINT_HASH_ROW_SIZE = 80;
        
//        String fs = File.separator;
        
        try {
            FileInputStream file_in = new FileInputStream( input_file_name );
            //General.showOutput("Using buffered input stream: default");
            BufferedInputStream bis = new BufferedInputStream( file_in ); 
            ObjectInputStream in    = new ObjectInputStream( bis );

            FileOutputStream file_out   = new FileOutputStream( output_file_name );
            BufferedOutputStream bout   = new BufferedOutputStream( file_out );
            CSVPrinter csvOut           = new CSVPrinter(bout);

            General.showOutput("Converting file : " + input_file_name );

            if ( General.verbosity >= General.verbosityDetail ) {
                printHashes = true;
            }

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
                //General.showDebug("Object is: " + o.toString());
                General.showDebug("Reading binary object of blastmatch_list with id: " + m);
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
                if ( useBmrbStyle ) {
                    blastmatch_list.toBmrbStyle();
                }
                blastmatch_list.toCsv(csvOut);
                m++;
            }                
            bout.close();// CSVPrinter flushes after each write so no worries?
            in.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }

            
    /** Convert binary files into CSV files using method in BlastMatchLoL.
     *Note that the see tag below doesn't work when compiling the javadoc outside netbeans.
     */
    public static boolean convertBin2Csv( String input_dir_name,
        String output_dir_name, boolean useBmrbStyle ) {
        
        boolean printHashes = true;
        final int PRINT_HASH_SIZE = 10;
        final int PRINT_HASH_ROW_SIZE = 80;
        
        String fs = File.separator;
        
        File homedir = new File( input_dir_name );
        // Implement the interface as an inner class for convenience; it's such a small baby.
        FilenameFilter filefilter = new FilenameFilter() {
            int MIN_FILE_SIZE = 100;
            public boolean accept( File d, String name) {
                boolean status_1 = name.endsWith( "." + BlastMatch.EXTENSION_BLAST_OUTPUT_BIN );
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
            General.showError("finding blast bin files.");
            return false;
        }
        
        if ( bin_files.length == 0 ) {
            General.showError("finding zero blast bin files in this dir.");
            return false;
        }
        
        /** Sort the files according to there name */
        Arrays.sort( bin_files );
        
        //General.showDebug("version 0");
        General.showOutput("Converting files: " + bin_files.length );
        General.showOutput("Converting files: " + Strings.toString( bin_files ) );
        
        try {
            for (int i=0; i<bin_files.length; i++) {

                String input_file_name = input_dir_name + fs + bin_files[i];
                FileInputStream file_in = new FileInputStream( input_file_name );
                ObjectInputStream in = new ObjectInputStream( file_in );

                String output_file_name = output_dir_name + fs + InOut.changeFileNameExtension( bin_files[i], "csv");
                FileOutputStream file_out = new FileOutputStream( output_file_name );
                BufferedOutputStream bout = new BufferedOutputStream( file_out );
                CSVPrinter csvOut = new CSVPrinter(bout);
                
                General.showOutput("Converting file : " + input_file_name );
                //General.showDebug("to file         : " + output_file_name );
                
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
                    if ( useBmrbStyle ) {
                        blastmatch_list.toBmrbStyle();
                    }
                    General.showDebug("blastmatch list: " + blastmatch_list.toString());
                    blastmatch_list.toCsv(csvOut);
                    m++;
                }                
                bout.close();// CSVPrinter flushes after each write so no worries?
                in.close();
                //General.showMemoryUsed();
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }
    

    /** Convert a bunch of blast text files, should all reside in one directory and end
     *      with standard extension. Converts them to many binary instances on files.
     * @param input_file_dir_name
     * @param output_file_dir_name
     */
    public static boolean convertBlastTxt2Bin( String input_file_dir_name, String output_file_dir_name, int
        verbosity ) {
        
//        BlastMatchLoL       blastmatch_lol=null;
//        FileOutputStream    file_out;
//        ObjectOutputStream  out;
        boolean status = false;
        boolean overall_status = true;
        
        String fs = File.separator;
        
        File homedir = new File( input_file_dir_name );
        // Implement the interface as an inner class for convenience; it's such a small baby.
        FilenameFilter filefilter = new FilenameFilter() {
            int MIN_FILE_SIZE = 1000;
            public boolean accept( File d, String name) {
                boolean status_1 = name.endsWith( BlastMatch.EXTENSION_BLAST_OUTPUT_TXT );
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
        String[] blast_files = homedir.list(filefilter);
        
        if ( blast_files == null ) {
            General.showError("finding blast files.");
            return false;
        }
        
        if ( blast_files.length == 0 ) {
            General.showError("finding zero blast files in this dir.");
            return false;
        }
        
        /** Sort the files according to there name */
        Arrays.sort( blast_files );
        
        General.showOutput("Converting files: " + blast_files.length );
        General.showOutput("Converting files: " + Strings.toString( blast_files ) );
        
        for (int i=0; i<blast_files.length; i++) {
            String full_name = input_file_dir_name + fs + blast_files[i];
            String output_file_name = output_file_dir_name + fs + InOut.changeFileNameExtension( blast_files[i], "bin" );

            General.showOutput("Converting blast text output to bin: " + i + " " + blast_files[i]);
            status = false;
            try {
                status = BlastParser.convertBlast(full_name, output_file_name, verbosity);
            // Catch all and simply restart, all should be tried
            } catch ( Throwable t ) {
                General.showThrowable(t);
            }
            if ( ! status ) {
                General.showError("Converting blast text output to binary representation."+ i + " " + blast_files[i]);
                overall_status = false;
            }
        }
        return overall_status;
    }
    
    
    /** Creates new BlastMatchLoL */
    public BlastMatchLoLoL() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    }
    
    /**
     * public void setBmrbStyle( boolean v ) {
     * bmrbStyle = v;
     * }
     */
}
