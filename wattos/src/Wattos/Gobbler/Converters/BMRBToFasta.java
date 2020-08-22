/*
 * BMRBToFasta.java
 *
 * Created on January 7, 2003, 9:54 AM
 */
package Wattos.Gobbler.Converters;

import java.io.*;
import java.util.*;
import com.braju.format.*;              // printf equivalent
import Wattos.Common.*;
import Wattos.Utils.*;
import Wattos.Utils.Comparators.*;
import EDU.bmrb.starlibj.*;

/**
 * Extract the polymer sequences from a BMRB entry in STAR format to a FASTA file.
 * @author Jurgen F. Doreleijers
 */
public class BMRBToFasta {
    static final String fs = File.separator;
    static final String STAR_FILE_EXTENSION = ".str";
    static StarParser myParser = null;
    // Will override standard definition in BlastDefinitions.java.
    static int MINIMUM_SEQUENCE_LENGTH = 15 ;
    static Parameters p = new Parameters(); // for printf

    static StringIntMap countMap = new StringIntMap();
    
    /** Creates a new instance of BMRBToFasta */
    public BMRBToFasta() {
    }

    /** Writes to writer the entries fasta format or nothing in case no sequence
     *could be found.
     */
    public static boolean convertEntry( FileInputStream inStream, Writer writer, boolean doAll ) {
        
        //General.showDebug("reinit star parser");
        StarParser.ReInit( inStream );
        
        try {
            StarParser.StarFileNodeParse(Wattos.Star.StarGeneral.sp);
        } catch ( ParseException t ) {
            General.showThrowable(t);
        }
        
        StarFileNode sfnInput = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();
        
        String accessionNumber = NMRSTAREntry.getAccessionNumber( sfnInput );
        if ( accessionNumber == null ) {
            General.showError("failed to get accession number from file");
            return false;
        }
        
        // Returns a arraylist of orf.
        ArrayList polymerList = NMRSTAREntry.getPolymerSequences( sfnInput, doAll );
        if ( polymerList == null) { // be explicit on the size clause.
            General.showWarning("failed to find the list of polymer sequences for entry: " + accessionNumber);
            return true;
        }

        // Set the db ids in the resulting orfs and render it to fasta
        for (Iterator it=polymerList.iterator();it.hasNext();) {
            Orf orf = (Orf) it.next();
            if ( orf.orf_id_list.orfIdList.size() < 1 ) {
                General.showError( "In convertEntry");
                General.showError( "Skipping entry id: " + accessionNumber);              
            }
            //General.showDebug("Found number of orf_ids: " + orf.orf_id_list.orfIdList.size() );
            OrfId orfId = (OrfId) orf.orf_id_list.orfIdList.get(0);
            orfId.orf_db_id = accessionNumber;            
            if ( ! BlastDefinitions.hasValidSequenceForBlastDb( orf.sequence, true, MINIMUM_SEQUENCE_LENGTH) ) {
                Parameters p = new Parameters(); // for printf
                p.add( orfId.orf_db_id );
                p.add( Strings.substringCertainEnd(orfId.orf_db_subid,0,20) );
                Format.printf( " Skipping entry id: %5s subid %-20s\n", p);
                continue;
            }
            
            try {
                writer.write( orf.toFasta() );
                countMap.increment( accessionNumber );
            } catch ( IOException e ) {
                General.showThrowable(e);
                return false;
            }
        }                
        //General.showOutput( "countMap: " + Strings.toString(countMap));
        return true;
    }
    
    /** Returns true only if all entries were successfully converted
     */
    public static boolean convertAllEntries( String dir_name, String output_file_name, boolean doAll ) {
        
        FileInputStream  inStream = null;
        boolean append = false; // Don't append
        int error_count = 0;
        String[] starfiles = null;
        BufferedWriter writer = null;
        
        try {
            File directory_file_object = new File(dir_name);
            FilenameFilter ff = new FilenameFilter() {
                public boolean accept( File d, String name ) { 
                    return name.endsWith( STAR_FILE_EXTENSION );
                }
            };
            starfiles = directory_file_object.list( ff );

            if ( starfiles == null ) {
                General.showError("directory didn't exist or some other error while");
                General.showError( "getting a listing from directory: " + dir_name);
                return false;
            }

            if ( starfiles.length == 0 ) {
                General.showError("no star files (files with extension: " + STAR_FILE_EXTENSION + ")");
                General.showError( "in directory: " + dir_name);
                return false;
            }
            /** Sort the files according to the number enclosed in the name */
            ComparatorEnclosedNumber c = new ComparatorEnclosedNumber();
            Arrays.sort( starfiles, c );
            
            General.showOutput("Number of star files todo: " + starfiles.length );        

            writer = new BufferedWriter( new FileWriter(output_file_name, append) );
            General.showOutput("Writing  file: "+output_file_name);
        } catch ( IOException e ) {
            General.showThrowable(e);
            return false;
        }
        
        for (int i=0;i<starfiles.length;i++) {
            String input_file_name = dir_name + fs + starfiles[i];
            //General.showOutput("Doing file: "+input_file_name);
            try {
                inStream =  new FileInputStream( input_file_name );
                boolean status = BMRBToFasta.convertEntry( inStream, writer, doAll );            
                if ( ! status ) {
                    General.showError( "in convert for file: " + input_file_name);
                    error_count++;
                }
                inStream.close();
            } catch ( IOException e ) {
                General.showThrowable(e);
                General.showError( "in convert for file: " + input_file_name);
                error_count++;
            // Then catch just anything so we always continue
            } catch ( Throwable t ) {
                General.showThrowable(t);
                General.showError( "in convert for file: " + input_file_name);
                error_count++;
            }            
        }
         
        try {
            writer.close(); 
        } catch ( IOException e ) {
            General.showThrowable(e);
            General.showError( "in closing output file: " + output_file_name);
            error_count++;
        }

        if ( error_count != 0 ) {
            General.showError("Found total number of errors: " + error_count);            
            return false;
        }
        return true;        
    }
    
    
    static public void showUsage() {
        General.showOutput("Usage: java Wattos.Gobbler.Converters.BMRBToFasta");
        General.showOutput("        [-a] <dir name in> <file name out>");
        General.showOutput("-a: extract all sequences [default is proteins only]");
        System.exit(1);
    }     
    
    
    /**
     * Will convert all files ending with .str in given directory.
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        boolean doAll = false;
        String input_dir_name="";
        String output_file_name="";

        General.showOutput("Wattos.Gobbler.ConvertersBMRBToFasta -version 1.1-");        
        if ( !((args.length == 2) || (args.length == 3 ) )) {
            showUsage();
        }
        if ( args.length == 2 ) {
            input_dir_name = args[0];
            output_file_name = args[1];
        } else {
            if ( ! args[0].startsWith("-") ) {
                General.showOutput("argument has to start with - but is: [" + args[0] + "]" );        
                showUsage();
            }                
            doAll = args[0].equalsIgnoreCase("-a");
            input_dir_name = args[1];
            output_file_name = args[2];
        }

        if ( ! BMRBToFasta.convertAllEntries( input_dir_name, output_file_name, doAll )) {
            General.showError("At least 1 BMRB entry failed to be converted; exited with error status 1");
            System.exit(1);
        }
        General.showOutput( "countMap: " + BMRBToFasta.countMap.toString());
        General.showOutput( "countMap sum: " + BMRBToFasta.countMap.sum());

        General.showOutput("Done");        
    }    
}
