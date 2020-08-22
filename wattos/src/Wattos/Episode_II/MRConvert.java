/*
 * MRConvert.java
 *
 * Created on June 14, 2002, 11:35 AM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;

/**
 * Checks the database and picks up all the original MR files.
 *Does all available conversions and puts the end results in temporary database tables.
 *Copies the end results to regular database tables in one transaction.
 *
 * @author  Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRConvert {
    
    static SQL_Episode_II sql_epiII = null;
    
    // Use a global variable for this in order not to need to create more
    // than 1 which destroys the buffer when reading commands from an
    // external file piped in.
    static BufferedReader in = null;
    
    static Classification classi = null;
    
    /** Remove a bunch of mr files from a directory.
     * Will print a line mentioning the delete for each entry.
     * Assumes standard naming scheme for entries as usual.
     * @param dir Directory from which the MR files are to be deleted.
     * @param entries_todelete List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFiles( String dir, ArrayList entries_todelete ) {
        
        for (Iterator i=entries_todelete.iterator(); i.hasNext();) {
            String entry_code = (String) i.next();
            String fname = dir + File.separator + entry_code + ".mr";
            File f = new File(fname);
            if ( ! f.delete() ) {
                General.showError("in MRAnnotate.removeMRFiles found:");
                General.showError("Deleting the annotated MR file ["+fname+"]");
                return false;
            } else {
                General.showOutput("Deleted the annotated MR file for entry: ["+entry_code+"]");
            }
        }
        return true;
    }
    
    
    /** Remove a bunch of mr files from the database.
     * Will print a line mentioning the delete for each entry.
     * Assumes standard naming scheme for entries as usual meaning that
     *the mrfiles should have the detail value set to: FILE_DETAIL_CLASSIFIED
     * @param entries_todelete List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesFromDB( ArrayList entries_todelete ) {
        
        boolean status = sql_epiII.deleteMRFilesByPDBIdsByDetail(
        entries_todelete, SQL_Episode_II.FILE_DETAIL_CLASSIFIED );
        if ( ! status ) {
            General.showError("in MRAnnotate.removeMRFilesFromDB found:");
            General.showError("Deleting the annotated MR files:" + entries_todelete);
            return false;
        } else {
            General.showOutput("Deleted the annotated MR files:" + entries_todelete);
        }
        return true;
    }
    
    
    /** Uses <code>removeMRFiles</code> but will first list the files to be deleted
     * and will then ask for conformation.
     * @param dir Directory from which the MR files are to be deleted.
     * @param entries_todelete List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesInteractively( String dir, ArrayList entries_todelete ) {
        
//        String reply = "bogus";
        boolean status;
        
        General.showOutput("Entries to delete:            [" + entries_todelete.size() + "]");
        General.showOutput("Entries to delete: " + entries_todelete.toString() );
        
        General.showWarning("answering yes to the following question will lead to");
        General.showWarning("deleting work that might not be recovered!");
        
        String prompt = "Delete ALL the MR files for the above entries from the directory:"+dir+"?";
        if ( Strings.getInputBoolean( in, prompt ) ) {
            General.showOutput("Deleting the annotated MR files listed above.");
            status = removeMRFiles( dir, entries_todelete );
            if ( ! status ) {
                General.showError("in MRAnnotate.removeMRFilesInteractively found:");
                General.showError("Deleting the annotated MR files failed.");
                return false;
            }
        }
        return true;
    }
    
    
    /** Will look for all mrfiles in the database that have the standard details:
     * mrfile.details = SQL_Episode_II.FILE_DETAIL_CLASSIFIED
     * @return List of entries in the directory for which there are files named as described.
     * The list of PDB entry codes returned is sorted too.
     */
    public static ArrayList getEntriesFromClassifiedMRFilesNewerThanDays( int max_days ) {
        
        ArrayList entries = sql_epiII.getPDBIdFromMRFileByDetailNewerThanDays(
        SQL_Episode_II.FILE_DETAIL_CLASSIFIED, max_days );
        
        if (entries == null) {
            General.showError("getting entry codes from the db.");
            return (entries);
        }
        Collections.sort(entries);
        return (entries);
    }
    
    
    /** Uses <code>removeMRFiles</code> but will first list the files to be deleted
     * and will then ask for conformation.
     * @param entries_todelete List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesInteractivelyFromDB( ArrayList entries_todelete ) {
        
//        String reply = "bogus";
        boolean status;
        
        General.showOutput("Entries to delete:            [" + entries_todelete.size() + "]");
        General.showOutput("Entries to delete: " + entries_todelete.toString() );
        
        General.showWarning("answering yes to the following question will lead to");
        General.showWarning("deleting work that might not be recovered!");
        
        String prompt = "Delete ALL the MR files for the above entries from the DB?";
        if ( Strings.getInputBoolean( in, prompt ) ) {
            General.showOutput("Deleting the annotated MR files listed above.");
            status = removeMRFilesFromDB( entries_todelete );
            if ( ! status ) {
                General.showError("in MRAnnotate.removeMRFilesInteractively found:");
                General.showError("Deleting the annotated MR files failed.");
                return false;
            }
        }
        return true;
    }
    
    
    /** Will look for all files named xxxx.mr in the given directory and those for which
     * xxxx seems to be a valid PDB code will use them as an PDB MR file. It will issue
     * warnings for files ending with .mr for which xxxx doesn't seem to be a PDB code.
     * @param dir Directory from which the MR files are to be taken.
     * @return List of entries in the directory for which there are files named as described.
     * The list of PDB entry codes returned is sorted too.
     */
    public static ArrayList getEntriesFromMRFiles( String dir ) {
        
        ArrayList entries = new ArrayList();
        
        File rdir = new File( dir );
        String[] mr_files = rdir.list( new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith( ".mr" ); }
        });
        
        if (mr_files ==  null) {
            General.showWarning("Found NO entries on file in directory: " + dir);
            return (entries);
        }
        
        File f;
        String fname, entry_code;
        
        // Check if the code conforms
        for (int i=0; i<mr_files.length; i++) {
            f = new File(mr_files[i]);
            fname = f.getPath();
            entry_code = fname.substring(0,4);
            // Check whether that's reasonable by matching against reg.exp.
            if ( Wattos.Utils.Strings.is_pdb_code( entry_code ) ) {
                entries.add( entry_code );
            } else {
                General.showWarning("Skipping this file.");
                General.showWarning("String for filename ["+fname+
                "] doesn't look like a pdb code: " + entry_code);
            }
        }
        Collections.sort(entries);
        return (entries);
    }
    
    
    /** Will look for all mrfiles in the database that have the standard details:
     * mrfile.details = SQL_Episode_II.FILE_DETAIL_CLASSIFIED
     * @return List of entries in the directory for which there are files named as described.
     * The list of PDB entry codes returned is sorted too.
     */
    public static ArrayList getEntriesFromClassifiedMRFiles() {
        
        ArrayList entries = sql_epiII.getPDBIdFromMRFileByDetail( SQL_Episode_II.FILE_DETAIL_CLASSIFIED );
        
        if (entries == null) {
            General.showError("getting entry codes from the db.");
            return (entries);
        }
        Collections.sort(entries);
        return (entries);
    }
    
    
    /* Empty temporary tables; and check if they exist. */
    public static boolean emptyTempTables() {
        ArrayList temp_tables = new ArrayList();
        // Since the mrblock depends on mrfile it actually
        // could be skipped but this way the program will also
        // check if the tables exist and give an error back if any doesn't.
        temp_tables.add( "mrfile" );
        temp_tables.add( "mrblock" );
        boolean status = sql_epiII.emptyTables( temp_tables, false );
        return status;
    }
    
    
    /** Loop to convert desired list of entries in db to db, possibly overwritten
     * existing data. This method uses transaction management.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static ArrayList doLoop(Globals g, Classification classification, int star_version) {
//        boolean testing              = g.getValueBoolean( "testing" );
        
        boolean do_incremental_only = Strings.getInputBoolean( in,
        "Do incremental conversions only(y) or do all classified mr files (n)? :");
        
        ArrayList entries_ref     = null;
        ArrayList entries_user     = new ArrayList();
        
        // Reset this to get info from original table.
        sql_epiII.SQL_table_prefix = "";
        
        if ( do_incremental_only ) {
            int max_number_days_old = Strings.getInputInt( in,
            "Enter the maximum number of days the entries may be old? :", null);
            entries_ref     = getEntriesFromClassifiedMRFilesNewerThanDays( max_number_days_old );
        } else {
            // Get the entry list in db
            entries_ref     = getEntriesFromClassifiedMRFiles();
        }
        
        sql_epiII.SQL_table_prefix = "temp_";
        
        if ( entries_ref.size() < 1 ) {
            General.showOutput("None to do so returning now to main loop." );
            return entries_ref;
        }
        
        General.showOutput("Possible entries include:" + entries_ref );
        
        // Get the entries to do
        boolean single_do = Strings.getInputBoolean( in,
            "Do one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();
        
        
        boolean use_all = Strings.getInputBoolean(in,
            "Use all from above (y) or specify your own list(n)");
        if ( ! use_all ) {
            String user_entry_list_string = Strings.getInputString(in,
                "Enter list of entries space/comma separated: ", null );
            user_entry_list_string = user_entry_list_string.replace(',', ' '); // just for the heck of it.
            General.showDebug("Transformed to : " +user_entry_list_string);
            StringTokenizer tokens = new StringTokenizer( user_entry_list_string, 
                " ");
            while ( tokens.hasMoreTokens() ) {
                pdb_entry_id = tokens.nextToken();
                // Skip those that are not valid or present.
                if ( ! Strings.is_pdb_code(pdb_entry_id) ) {
                    General.showWarning("Entry code given doesn't look like a PDB code");
                    continue;
                }
                if ( ! entries_ref.contains(pdb_entry_id) ) {
                    General.showWarning("Entry doesn't seem to exist in the archive given.");
                    continue;
                }
                entries_user.add( pdb_entry_id );
            }
        }
        
        // Pick the entry to do (first)
        while ( ! Strings.is_pdb_code(pdb_entry_id) ) {
            pdb_entry_id = Strings.getInputString( in,
            "Give entry code (e.g.: 1brv) to do (or . for first): ", null );
            if ( pdb_entry_id.equals(".") ) {
                pdb_entry_id = (String) entries_ref.get(0);
            }
            if ( ! Strings.is_pdb_code(pdb_entry_id) ) {
                General.showWarning("Entry code given doesn't look like a PDB code");
                pdb_entry_id = "";
            }
            if ( ! entries_ref.contains(pdb_entry_id) ) {
                General.showWarning("Entry doesn't seem to exist in the archive given.");
                pdb_entry_id = "";
            }
        }
        
        // Just do this single one
        if ( single_do ) {
            entries_togo.add(pdb_entry_id);
        } else
            // Select all the entries following, starting with the one selected
        {
            // Get all entries possible or those specified by the user.
            if ( use_all ) {
                entries_togo.addAll(entries_ref);            
            } else {
                entries_togo.addAll(entries_user);            
            }
            // Set the selected one to the first position
            General.rotateCollectionToFirst(entries_togo, pdb_entry_id);
            // Remove the last element until done.
            int entries_togo_max = Strings.getInputInt( in,
            "Give maximum number of entries to do (0-" + entries_togo.size() + ") : ", null );
            while ( entries_togo.size() > entries_togo_max ) {
                entries_togo.remove(entries_togo.size()-1);
            }
            
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }
        
        boolean status = true;
        
        // Finally actually do this set.
        int j = 0;
        for (Iterator i=entries_togo.iterator(); i.hasNext();) {
            pdb_entry_id = i.next().toString();
            General.showOutput("\n\nDoing entry " + pdb_entry_id + " " + j++);
            status = doEntry(pdb_entry_id, g, classification, star_version);
            if ( ! status ) {
                General.showError("doEntry failed, skipping remaining entries");
                break;
            }
        }
        
        if ( ! status ) {
            return null;
        } else {
            return entries_togo;
        }
    }
    
    
    /** Dump one entry.
     * @param pdb_entry_id The PDB entry code of the MR file to be annotated.
     * Allowed values are a, i, and r; see code.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static boolean doEntry(String pdb_entry_id, Globals g, Classification classification, int star_version) {
        
        /*
        // Target location
        // Get a temporary path to a file in the directory as set in Globals
        String tmp_dir = g.getValueString("tmp_dir");
        String fs  = File.separator;
        File file_new = new File();
        file_new.createTempFile( pdb_entry + "_original_block", "txt", tmp_dir);
        file_new.deleteOnExit(); // this file will not actually populated.
        String file_new_path = file_new.getPath(); // e.g. /tmp/1brv_original_block.txt
         */
        boolean status = false;
        DBMRFile mrf = new DBMRFile();
        
        General.showOutput("Get the MR file_id from the pdb code and detail value.");
        sql_epiII.SQL_table_prefix = "";
        ArrayList mrfile_ids = sql_epiII.getMRFileIdsByPDBIdByDetail(pdb_entry_id,
        SQL_Episode_II.FILE_DETAIL_CLASSIFIED);
        sql_epiII.SQL_table_prefix = "temp_";
        if ( mrfile_ids.size() < 1 ) {
            General.showError("in MRConvert.doEntry");
            General.showError("failed to get 1 or more mrfile ids for pdb entry code:" + pdb_entry_id);
            General.showError("Skipping this entry.");
            return false;
        }
        // Use the first one only
        if ( mrfile_ids.size() > 1 ) {
            General.showWarning("in MRAnnotate.doEntry");
            General.showError( "WARNING: got more mrfile ids for pdb entry code:" + pdb_entry_id);
            General.showError( "WARNING: will just use the first one");
        }
        mrf.mrfile_id = ((Integer) mrfile_ids.get(0)).intValue();
        
        // Read from DB
        sql_epiII.SQL_table_prefix = "";
        status = sql_epiII.getMRFile( mrf );
        sql_epiII.SQL_table_prefix = "temp_";
        
        if ( ! status ) {
            General.showError("in MRAnnotate.dumpEntry");
            General.showError("failed to get mrfile from db.");
            General.showError("Skipping this entry.");
            return false;
        }
        
        // The next method does the actual work of converting the data and
        // returning a list of converted file(s) back.
        ArrayList files_conv = mrf.doConversions(classi, star_version);
        
        // Check for error status. An empty list is fine.
        if ( files_conv == null ) {
            General.showError("in MRAnnotate.dumpEntry from doConversions");
            General.showError("Skipping this entry.");
            return false;
        }
        
        // An empty list is fine.
        if ( files_conv.size() == 0) {
            General.showError( "WARNING: No resulting files.");
        }
        
        // Write the individual converted files to the database.
        status = DBMRFile.putFilesInDatabase(classi, files_conv, sql_epiII);
        
        return status;
    }
    
    
    /** The entry point for the program java -Xmx256m Wattos.Episode_II.MRConvert.
     * @param args Ignored.
     */
    public static void main(String[] args) {
        // Change some of the standard settings defined in the Globals class
        Globals g = new Globals();
        g.showMap();
        sql_epiII = new SQL_Episode_II( g );
//        General.verbosity = General.verbosityDebug;
        
        General.showOutput("Opened an sql connection:" + sql_epiII );
        
        // Read in the classifications possible for annotation.
        classi = new Classification();
        if (!classi.readFromCsvFile("Data/classification.csv")){
            General.showError("in MRAnnotate.main found:");
            General.showError("reading classification file.");
            System.exit(1);
        }
        /** Very simple loop as a menu.
         *Start over every time a sequence of entries has been done.
         *Very fast though to get the info again so not a performance issue.
         */
        
        in = new BufferedReader(new InputStreamReader(System.in));
        
        do {
            
            // Work on temporary tables.
            // This is the way to indicate on which set of tables to operate on.
            // The string below indicates it will be the temporary tables.
            sql_epiII.SQL_table_prefix = "temp_";
            // Perhaps something left over so empty it. This will also check
            // to see if tables exist.
            boolean status = emptyTempTables();
            if ( ! status ) {
                General.showError("trying to empty the temporary db tables.");
                System.exit(1);
            }
            
            int star_version = NmrStar.STAR_VERSION_DEFAULT;
            General.showOutput("Using NMR-STAR version: " + NmrStar.STAR_VERSION_NAMES[star_version]);
            ArrayList entries_done = doLoop(g, classi, star_version);
            if ( entries_done == null ) {
                General.showError("in doLoop.");
                continue;
            }
            
            if ( entries_done.size() == 0 ) {
                General.showWarning("no entries done.");
                continue;
            }
            
            General.showOutput("Done entries: " + entries_done.size());
            
            
            // In 1 transaction with a try/catch construct for safety
            sql_epiII.setAutoCommit(false);
            boolean transaction_status;
            try {
                // -1- delete the old converted MR files if any
                sql_epiII.SQL_table_prefix = "";
                transaction_status = sql_epiII.deleteFilesNotClassified( entries_done );
                sql_epiII.SQL_table_prefix = "temp_";
                if ( transaction_status ) {
                    General.showDebug("-2- Copy temporary content to regular tables.");
                    transaction_status = sql_epiII.copyFromSpecialToRegularTables();
                    if ( ! transaction_status ) {
                        General.showError("trying to copy data from temporary to regular db tables.");
                    }
                } else {
                    General.showError("trying to delete old converted files from the regular db tables.");
                } 
                
                if ( ! transaction_status ) {
                    throw new Exception();
                }
            } catch ( Exception e ) //every possible exception
            {
                General.showThrowable(e);
                // Rollback the transaction if previous steps where not successful.
                General.showError("Will try to roll back the transaction.");
                transaction_status = sql_epiII.rollbackTransaction();
                if ( ! transaction_status ) {
                    General.showError("rolling back the transaction. Aborting program.");
                    break;
                }
            }
            
            // Reset commit so the doLoop can be quick.
            sql_epiII.setAutoCommit(true);
            General.showDebug("-3- Delete temporary content.");
            status = emptyTempTables();
            if ( ! status ) {
                General.showError("trying to empty the temporary db tables.");
                break;
            }
        } while ( Strings.getInputBoolean(in, "Start again?") );
    }
}
