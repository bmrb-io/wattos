package Wattos.Episode_II;

import java.io.*;
import java.util.*;
import com.braju.format.*;              // printf equivalent
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 *Main program for manually loading MR files into db.
 *
 * @author  Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRInterloop {

    /** Maximum number of entries an annotator can do in one session.*/
    static final int ENTRIES_TOGO_MAX = 99999;
    /** Maximum number of entries an annotator will see as a suggestion */
    static final int ENTRIES_POSSIBLE_MAX = 20;

    static SQL_Episode_II sql_epiII = null;
    // Use a global variable for this in order not to need to create more
    // than 1 which destroys the buffer when reading commands from an
    // external file piped in.
    static BufferedReader in = null;

    /** Start annotations in a loop.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static void loadLoop(Globals g, Classification classification) {
//        "Directory with the mr files to be loaded: " );
        String mr_anno_dir = ".";

        // Get the entry list in db
        ArrayList entries_ref     = getEntriesFromMRFiles( mr_anno_dir );

        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if ( entries_ref.size() < ENTRIES_POSSIBLE_MAX )
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" +
            entries_ref.subList(0,max_entries_to_show) );
        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean( in,
            "Load one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();
        boolean use_all = true;

        ArrayList entries_user = new ArrayList();

        if ( ! single_annotate ) {
            use_all = Strings.getInputBoolean( in,
                "Use all from above (y) or specify your own list(n)");
            if ( ! use_all ) {
                String user_entry_list_string = Strings.getInputString(
                    "Enter list of entries space/comma separated: " );
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
                // Use this info to start the sequence by.
                if ( entries_user.size() > 0 ) {
                    pdb_entry_id = (String) entries_user.get(0);
                }
            }
        }

        while ( ! Strings.is_pdb_code(pdb_entry_id) ) {
            pdb_entry_id = Strings.getInputString( in,
            "Give entry code (e.g.: 1brv) to do first (or . for first): ", null );

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
        if ( single_annotate ) {
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
            // Truncate the list so the annotator doesn't get a full screen.
            // Remove the last element until done.
            while ( entries_togo.size() > ENTRIES_TOGO_MAX ) {
                entries_togo.remove(entries_togo.size()-1);
            }
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }

        boolean do_all = false;
        // Finally actually do this set.
        for (Iterator i=entries_togo.iterator(); i.hasNext();) {
            pdb_entry_id = i.next().toString();
            loadEntry(pdb_entry_id, "a", mr_anno_dir, g, classification);
            // Don't ask stupid questions
            if ( single_annotate || (!i.hasNext())) {
                break;
            }
            // Continue with next entry in series?
            if ( ! do_all  ) {
                do_all = Strings.getInputBoolean( in, "Do all now?");
                if ( ! do_all ) {
                    if ( ! Strings.getInputBoolean( in, "Do next entry?") )
                        break;
                }
            }
        }
    }



    /** Read from a directory the entries present and asks which ones to do.
     */
    public static void load_DOCR_FRED_Db_Loop(Globals g, Classification classification) {
//        boolean status;
//        boolean testing              = g.getValueBoolean( "testing" );
        String  docr_fred_db_dir     = null;
        // Get the entry list in db
        ArrayList entries_ref     = null;
        while ( docr_fred_db_dir == null ) {
            docr_fred_db_dir = Strings.getInputString( in,
                    "Give directory name with entries to load: ", null );
            if ( docr_fred_db_dir == null ) {
                General.showWarning("failed to get a directory name");
                continue;
            }
            // Get the entry list in db.
            entries_ref     = getEntriesFromDir( new File(docr_fred_db_dir, "DOCR" ));
            if ( (entries_ref == null) || (entries_ref.size() < 1)) {
                General.showWarning("failed to get entries in directory: " + docr_fred_db_dir);
                docr_fred_db_dir = null;
            }
        }
        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if ( entries_ref.size() < ENTRIES_POSSIBLE_MAX )
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" +
            entries_ref.subList(0,max_entries_to_show) );
        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean( in,
            "Load one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();

        while ( ! Strings.is_pdb_code(pdb_entry_id) )
        {
            pdb_entry_id = Strings.getInputString( in,
                "Give entry code (e.g.: 1brv) to do first (or . for first): ", null );

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
        if ( single_annotate ) {
            entries_togo.add(pdb_entry_id);
        } else
        // Select all the entries following, starting with the one selected
        {
            entries_togo.addAll(entries_ref);
            // Set the selected one to the first position
            General.rotateCollectionToFirst(entries_togo, pdb_entry_id);
            // Truncate the list so the annotator doesn't get a full screen.
            // Remove the last element until done.
            while ( entries_togo.size() > ENTRIES_TOGO_MAX ) {
                entries_togo.remove(entries_togo.size()-1);
            }
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }

        boolean do_all = false;
        // Finally actually do this set.
        for (Iterator i=entries_togo.iterator(); i.hasNext();) {
            pdb_entry_id = i.next().toString();
            if ( ! load_DOCR_FRED_Entry(pdb_entry_id, g, docr_fred_db_dir, classification) ) {
                // Stop after one failed load
                General.showError("Loading entry of DOCR/FRED db.");
            }
            // Don't ask stupid questions
            if ( single_annotate || (!i.hasNext())) {
                break;
            }
            // Continue with next entry in series?
            if ( ! do_all  ) {
                do_all = Strings.getInputBoolean( in, "Do all now?");
                if ( ! do_all ) {
                    if ( ! Strings.getInputBoolean( in, "Do next entry?") )
                        break;
                }
            }
        }
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


    /** loop to dump desired list of entries from db to file, overwritten
     * possibly existing files.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static void dumpLoop ( Globals g, Classification classification) {
//        boolean status;
//        boolean testing              = g.getValueBoolean( "testing" );

        String dump_dir = null;
        boolean isGoodDir = false;
        do {
            dump_dir = Strings.getInputString( in,
                "Enter directory name in OS specific notation\n" +
                "E.g. for unix:     /share/tmp\n" +
                "E.g. for windows:  S:\\tmp\n" +
                "here: ", null );
            // Check if dir exists and is a dir
            isGoodDir = true;
            File f = new File( dump_dir );
            if ( ! f.exists() ) {
                General.showWarning("directory name given doesn't exist.");
                isGoodDir = false;
            } else if ( ! f.isDirectory()) {
                General.showWarning("directory name given is not a directory.");
                isGoodDir = false;
            }
        } while ( ! isGoodDir );

        boolean do_incremental_only = Strings.getInputBoolean( in,
            "Do incremental backup only(y) or do all classified mr files (n)? :");

        ArrayList entries_ref     = null;

        if ( do_incremental_only ) {
            int max_number_days_old = Strings.getInputInt( in,
                "Enter the maximum number of days the file may be old? :", null);
            entries_ref     = getEntriesFromClassifiedMRFilesNewerThanDays( max_number_days_old );
        } else {
            // Get the entry list in db
            entries_ref     = getEntriesFromClassifiedMRFiles();
        }

        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if ( entries_ref.size() < ENTRIES_POSSIBLE_MAX )
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" +
            entries_ref.subList(0,max_entries_to_show) );


        if ( entries_ref.size() < 1 ) {
            General.showOutput("None to do so prematurely exiting" );
            System.exit(0);
        }

        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean( in,
            "Dump one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();

        // Just take the first one in batch mode
        if ( single_annotate ) {
        while ( ! Strings.is_pdb_code(pdb_entry_id) )
        {
            if ( single_annotate ) {
                pdb_entry_id = Strings.getInputString( in,
                    "Give entry code (e.g.: 1brv) to do: ", null );
            }
            else {
                pdb_entry_id = Strings.getInputString( in,
                    "Give entry code (e.g.: 1brv) to do: ", null );
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
        } else {
            pdb_entry_id = (String) entries_ref.get(0);
        }

        // Just do this single one
        if ( single_annotate ) {
            entries_togo.add(pdb_entry_id);
        } else
        // Select all the entries following, starting with the one selected
        {
            entries_togo.addAll(entries_ref);
            // Set the selected one to the first position
            General.rotateCollectionToFirst(entries_togo, pdb_entry_id);
            // Truncate the list so the annotator doesn't get a full screen.
            // Remove the last element until done.
            while ( entries_togo.size() > ENTRIES_TOGO_MAX ) {
                entries_togo.remove(entries_togo.size()-1);
            }
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }

        // Finally actually do this set.
        for (Iterator i=entries_togo.iterator(); i.hasNext();) {
            pdb_entry_id = i.next().toString();
            dumpEntry(pdb_entry_id, dump_dir, g, classification);
        }
    }


    /** Dump one entry.
     * @param pdb_entry_id The PDB entry code of the MR file to be annotated.
     * Allowed values are a, i, and r; see code.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static void dumpEntry( String pdb_entry_id, String dir,
        Globals g, Classification classification) {

        // Target location
        String fs  = File.separator;
//        File file_new = null;
        boolean status = false;

        String filename = dir + fs + pdb_entry_id + ".mr";
        DBMRFile mrf = new DBMRFile(filename);

        General.showOutput("Get the MR file_id from the pdb code and detail value.");
        ArrayList mrfile_ids = sql_epiII.getMRFileIdsByPDBIdByDetail(pdb_entry_id,
            SQL_Episode_II.FILE_DETAIL_CLASSIFIED);
        if ( mrfile_ids.size() < 1 ) {
            General.showError("in MRAnnotate.dumpEntry");
            General.showError("failed to get 1 or more mrfile ids for pdb entry code:" + pdb_entry_id);
            General.showError("Skipping this entry.");
            return;
        }
        // Use the first one only
        if ( mrfile_ids.size() > 1 ) {
            General.showWarning("in MRAnnotate.dumpEntry");
            General.showError( "WARNING: got more mrfile ids for pdb entry code:" + pdb_entry_id);
            General.showError( "WARNING: will just use the first one");
        }
        mrf.mrfile_id = ((Integer) mrfile_ids.get(0)).intValue();

        // Read from DB
        status = sql_epiII.getMRFile( mrf );
        if ( ! status ) {
            General.showError("in MRAnnotate.dumpEntry");
            General.showError("failed to get mrfile from db.");
            General.showError("Skipping this entry.");
            return;
        }

        // Write to directory
        boolean useLines = false; // The data is already split when in the db.
        boolean force_delete = true; // Remove existing copies if need be.
        mrf.writeToFile(useLines, force_delete);
    }


    /** Get files into db for one entry.
     * @param pdb_entry_id The PDB entry code of the MR file to be annotated.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static boolean load_DOCR_FRED_Entry( String pdb_entry_id,
    Globals g, String docr_fred_db_dir, Classification classification) {

//        boolean status;
        int[] checks = {1,2,3}; // don't do diff check.
        // Note that the extra prop for the mrblock of lowerbounds only will be set for CYANA files
        // based on the file extension: .lol.
        ArrayList files = new ArrayList();
        files.add( new String[] {"DOCR","entry",        "full",             "n/a",  "STAR","$X_project.str"} );
        files.add( new String[] {"DOCR","entry",        "full",             "n/a",  "XML","$X_project.xml.tgz"} );

        // CNS
        files.add( new String[] {"DOCR","sequence",     "n/a",              "n/a",  "XPLOR/CNS","$X_DOCR_$N.py"} );
        files.add( new String[] {"DOCR","coordinate",   "ensemble",         "n/a",  "XPLOR/CNS","$X_cns.pdb"} );
        files.add( new String[] {"DOCR","distance",     "NOE",              "ambi", "XPLOR/CNS","$X_distance_NOE_na_$N.tbl"} );
        files.add( new String[] {"DOCR","distance",     "hydrogen bond",    "ambi", "XPLOR/CNS","$X_distance_HB_na_$N.tbl"} );
        files.add( new String[] {"DOCR","distance",     "general distance", "ambi", "XPLOR/CNS","$X_distance_general_distance_na_$N.tbl"} );
        files.add( new String[] {"DOCR","dihedral angle","n/a",             "n/a",  "XPLOR/CNS","$X_dihedral_na_na_$N.tbl"} );
        files.add( new String[] {"DOCR","dipolar coupling","n/a",           "n/a",  "XPLOR/CNS","$X_dipolar_coupling_na_na_$N.tbl"} );

        // CYANA
        files.add( new String[] {"DOCR","sequence",     "n/a",              "n/a",  "DYANA/DIANA","$X_sequence.seq"} );
        files.add( new String[] {"DOCR","distance",     "NOE",              "ambi", "DYANA/DIANA","$X_distance_NOE_na_$N.upl"} );
        files.add( new String[] {"DOCR","distance",     "NOE",              "ambi", "DYANA/DIANA","$X_distance_NOE_na_$N.lol"} );
        files.add( new String[] {"DOCR","distance",     "hydrogen bond",    "ambi", "DYANA/DIANA","$X_distance_HB_na_$N.upl"} );
        files.add( new String[] {"DOCR","distance",     "general distance", "ambi", "DYANA/DIANA","$X_distance_general_distance_na_$N.upl"} );
        files.add( new String[] {"DOCR","distance",     "general distance", "ambi", "DYANA/DIANA","$X_distance_general_distance_na_$N.lol"} );
        files.add( new String[] {"DOCR","distance",     "hydrogen bond",    "ambi", "DYANA/DIANA","$X_distance_HB_na_$N.lol"} );
        files.add( new String[] {"DOCR","dihedral angle","n/a",             "n/a",  "DYANA/DIANA","$X_dihedral_na_na_$N.aco"} );
        files.add( new String[] {"DOCR","dipolar coupling","n/a",           "n/a",  "DYANA/DIANA","$X_dipolar_coupling_na_na_$N.upl"} );

        // FRED
        files.add( new String[] {"FRED","entry",        "full",             "n/a",  "STAR","$X_project.str"} );
        files.add( new String[] {"FRED","entry",        "full",             "n/a",  "XML","$X_project.xml.tgz"} );
        files.add( new String[] {"FRED","check",        "stereo assignment",    "distance",  "Wattos",      "$X_assign.str"} );
        files.add( new String[] {"FRED","check",        "surplus",              "distance",  "Wattos",      "$X_surplus.str"} );
        files.add( new String[] {"FRED","check",        "violation",            "distance",        "Wattos",      "$X_dist_viol.str"} );
        files.add( new String[] {"FRED","check",        "violation",            "dihedral angle",  "Wattos",      "$X_dihed_viol.str"} );
        files.add( new String[] {"FRED","check",        "completeness",         "distance",  "Wattos",      "$X_compl.str"} );


        if ( ! sql_epiII.deleteFilesDOCRFRED( pdb_entry_id )) {
            General.showError("Failed to remove any old files for this entry in DOCR/FRED");
            return false;
        }
        for (int i=0;i<files.size();i++) {
            String[] fileChars = (String[]) files.get(i);
            int x = 0;
            String db          = fileChars[x++];
            String type        = fileChars[x++];
            String subtype     = fileChars[x++];
            String format      = fileChars[x++];
            String program     = fileChars[x++];
            String fn          = fileChars[x++];

            File active_db_dir = new File(docr_fred_db_dir, db );
            File entry_dir = new File( active_db_dir, pdb_entry_id);

            ArrayList expandedSet = new ArrayList();
            fn = fn.replaceAll("\\$X", pdb_entry_id);
            fn = fn.replaceAll("\\$N", "[0-9]+"); // turn it into a regular expression
//            File f             = new File( fn );
//            String fileName = InOut.getFilenameBase(f);

            // Look for the expanded set.
            InOut.RegExpFilenameFilter ff = new InOut.RegExpFilenameFilter(fn);
            String[] list = entry_dir.list(ff);
            if ( list == null ) {
                General.showError("Failed to check for files that follow regexp: " + fn + " Abstract pathname does not denote a directory, or if an I/O error occured");
                continue;
            }
            expandedSet.addAll( Arrays.asList(list));

            General.showDebug("For fn            : " + fn);
            General.showDebug("Found expanded Set: " + Strings.toString( expandedSet ));
            for (int j=0;j<expandedSet.size();j++) {
                String filename = (String) expandedSet.get(j);
                File fullFileName = new File( entry_dir, filename );
                // some debugging.
                General.showDebug("In dir: " + db + " found file: " + filename);

                if ( fullFileName.length() == 0 ) {
                    General.showWarning("Skipping empty file: " + fullFileName.toString());
                    continue;
                }
                DBMRFile mrf = new DBMRFile(filename);
                if ( ! mrf.readFromFile(entry_dir) ) {
                    General.showError("in MRInterloop.load_DOCR_FRED_Entry found:");
                    General.showError("Read NOT successful. Skipping this entry.");
                    return false;
                }

                mrf.detail = SQL_Episode_II.FILE_DETAIL_CONVERTED;
                mrf.setBlockTextType(SQL_Episode_II.BLOCK_DETAIL_CONVERTED);
                if ( db.equalsIgnoreCase("fred")) {
                    mrf.detail = SQL_Episode_II.FILE_DETAIL_FILTERED;
                    mrf.setBlockTextType(SQL_Episode_II.BLOCK_DETAIL_FILTERED);
                }
                if ( mrf.blocks.size() < 1 ) {
                    General.showError("No blocks in mrf file; can't be");
                    return false;
                }
                DBMRBlock mrb = (DBMRBlock) mrf.blocks.get(0);
                mrb.setType( new String[] {
                    program,
                    type,
                    subtype,
                    format
                });
                mrb.fileName = filename;

                /** Extract the number of restraints and any other existing countable items.
                 * See method: MRSTARFile.getItemCount().
                 */
                mrb.item_count = Defs.NULL_INT;
                if ( program.equals("STAR")) {
                    mrb.item_count = MRSTARFile.getItemCount(fullFileName.toString());
                    if ( Defs.isNull( mrb.item_count) ) {
                        General.showError("failed to get item count for file with full name: " + fullFileName);
                        return false;
                    }
                    General.showOutput("Counted items: " + mrb.item_count);
                }
                String fileNameExtension = InOut.getFilenameExtension(mrb.fileName);
                if ( fileNameExtension.equals("lol")) {
                    General.showDebug("Doing: mrb.setOtherProp(DBMRBlock.OTHER_PROP_LOWER_ONLY,Defs.STRING_TRUE)");
                    if ( ! mrb.setOtherProp(DBMRBlock.OTHER_PROP_LOWER_ONLY,Defs.STRING_TRUE)) {
                        General.showError("in MRInterloop.load_DOCR_FRED_Entry found:");
                        General.showError("setOtherProp for .lol file not successfull, aborting entry." );
                        return false;
                    }
                }
                // do these so we don't insert junk.
                if ( ! mrf.check(checks, classification, g)) {
                    General.showError("in MRInterloop.load_DOCR_FRED_Entry found:");
                    General.showError("Checks not successfull, aborting entry." );
                    return false;
                }
                if ( mrf.writeToDB( sql_epiII ) ) {
                    General.showOutput("Insert in DB successful" );
                } else {
                    General.showError("in MRInterloop.load_DOCR_FRED_Entry found:");
                    General.showError("Insert in DB not successful, aborting entry." );
                    return false;
                }
            }
        }
        return true;
    }


    /** Annotate one entry.
     * User will be informed of whether the annotation conforms to allowed changes
     * and will be given the option to correct any mishaps remaining in the specified
     * text editor.
     * @param pdb_entry_id The PDB entry code of the MR file to be annotated.
     * @param archive_id The id for the archive to be used as the original for the annotation.
     * Allowed values are a, i, and r; see code.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static void loadEntry( String pdb_entry_id, String archive_id, String dir,
        Globals g, Classification classification) {

        boolean status;
        // Recommended to do all 4 checks and check individual errors before
        // disablig check 4
        //int[] checks = {1,2,3,4};
        int[] checks = {1,2,3};

        // Target location
//        String dir = n/ull;
        String fs  = File.separator;
//        File file_new = null;

//        dir = g.getValueString( "mr_anno_dir" );

        String filename = dir + fs + pdb_entry_id + ".mr";
        DBMRFile mrf = new DBMRFile(filename);

        status = mrf.readFromFile(null);
        if ( ! status ) {
            General.showError("in MRInterloop.loadEntry found:");
            General.showError("Read NOT successful. Skipping this entry.");
            return;
        }
        status = mrf.check(checks,classification,g);

        mrf.detail = SQL_Episode_II.FILE_DETAIL_CLASSIFIED; // Read: "classified";
        mrf.setBlockTextType(SQL_Episode_II.BLOCK_DETAIL_RAW);

        // Commit to annotated directory if needed
        if ( status ) {
            //General.showOutput("Checks successful." );
            status = mrf.writeToDB( sql_epiII );
            if ( status ) {
                General.showOutput("Insert in DB successful" );
            } else {
                General.showError("in MRInterloop.loadEntry found:");
                General.showError("Insert in DB not successful" );
                return;
            }

        } else {
            General.showError("in MRInterloop.loadEntry found:");
            General.showError("Checks not successful." );
        }
    }


    /** Convert one entry.
     * @param pdb_entry_id The PDB entry code of the MR file to be converted.
     * @param archive_id The id for the archive to be used as the original for the annotation.
     * Allowed values are a, i, and r; see code.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static boolean convertEntry( String pdb_entry_id, String archive_id,
        Globals g, Classification classification, String mr_convert_dir, String str_convert_dir, int star_version ) {

        boolean status;
        int[] checks = {1,2,3,4};

        // Target location
        String dir = null;
        String fs  = File.separator;
//        File file_new = null;

        dir = mr_convert_dir;

        String filename = dir + fs + pdb_entry_id + ".mr";
        DBMRFile mrf = new DBMRFile(filename);

        // Read into blocks.
        status = mrf.readFromFile();
        if ( ! status ) {
            General.showError("in MRInterloop.convertEntry found:");
            General.showError("Read NOT successful. Skipping this entry.");
            return false;
        }
        status = mrf.check(checks,classification,g);
        if ( ! status ) {
            General.showError("in MRInterloop.convertEntry found:");
            General.showError("Checks NOT successful. Skipping this entry.");
            return false;
        }

        // Convert the original file into 1 star file.
        DBMRFile str_file = mrf.doConversion(classification,DBMRFile.BLOCK_ID_INDICATING_ALL,star_version);
        String str_filename = str_convert_dir + fs + pdb_entry_id + ".str";
        str_file.file = new File(str_filename);
        status = str_file.writeToFile(false, true, false);
        if ( ! status ) {
            General.showError("in MRInterloop.convertEntry found:");
            General.showError("writeToFile NOT successful. Skipping this entry.");
            return false;
        }
        return true;
    }


    /** Will look for all files named xxxx_project.str in the given directory with sub dir:
     * 'DOCR' and those for which xxxx seems to be a valid PDB code will use them as an PDB MR file.
     * It will issue
     * warnings for files ending with .mr for which xxxx doesn't seem to be a PDB code.
     * @param dir Directory from which the MR files are to be taken.
     * @return List of entries in the directory for which there are files named as described.
     * The list of PDB entry codes returned is sorted too.
     */
    public static ArrayList getEntriesFromDir( File dir ) {

        Parameters p = new Parameters(); // Printf parameters
        ArrayList entries = new ArrayList();

        General.showDebug("Looking for star project file in: " + dir);
        String[] subDirs = dir.list();

        if (subDirs ==  null) {
            General.showWarning("Found NO entries on file in directory: " + dir);
            return (entries);
        }

        Format.printf("Found %6d entries in dir %s\n",
            p.add( subDirs.length).add( dir ));

//        File f;
        String entry_code;

        // Check if the code conforms
        for (int i=0; i<subDirs.length; i++) {
            entry_code = subDirs[i];
            // Check whether that's reasonable by matching against reg.exp.
            if ( Wattos.Utils.Strings.is_pdb_code( entry_code ))  {
                entries.add( entry_code );
            } else {
                General.showWarning("Skipping this dir.");
                General.showWarning("String for dir ["+entry_code+
                    "] doesn't look like a pdb code.");
            }
        }
        Collections.sort(entries);
        return entries;
    }

    /** Will look for all files named xxxx.mr in the given directory and those for which
     * xxxx seems to be a valid PDB code will use them as an PDB MR file. It will issue
     * warnings for files ending with .mr for which xxxx doesn't seem to be a PDB code.
     * @param dir Directory from which the MR files are to be taken.
     * @return List of entries in the directory for which there are files named as described.
     * The list of PDB entry codes returned is sorted too.
     */
    public static ArrayList getEntriesFromMRFiles( String dir ) {

        Parameters p = new Parameters(); // Printf parameters
        ArrayList entries = new ArrayList();

        File rdir = new File( dir );
        String[] mr_files = rdir.list( new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith( ".mr" ); }
        });

        if (mr_files ==  null) {
            General.showWarning("Found NO entries on file in directory: " + dir);
            return (entries);
        }

        Format.printf("Found %6d entries in dir %s\n",
            p.add( mr_files.length).add( dir ));

        File f;
        String fname, entry_code;

        // Check if the code conforms
        for (int i=0; i<mr_files.length; i++) {
            f = new File(mr_files[i]);
            fname = f.getPath();
            entry_code = fname.substring(0,4);
            // Check whether that's reasonable by matching against reg.exp.
            if ( Wattos.Utils.Strings.is_pdb_code( entry_code ) )
            {
                entries.add( entry_code );
            } else
            {
                General.showWarning("Skipping this file.");
                General.showWarning("String for filename ["+fname+
                    "] doesn't look like a pdb code: " + entry_code);
            }
        }
        Collections.sort(entries);
        return (entries);
    }



    /** Start annotations in a loop.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static boolean convertLoop ( Globals g, Classification classification) {
        boolean status;
//        boolean testing              = g.getValueBoolean( "testing" );
        String mr_convert_dir        = null;
        String str_convert_dir       = null;
        int overall_convert_errors = 0;

        int star_version = NmrStar.STAR_VERSION_INVALID;
        while ( star_version == NmrStar.STAR_VERSION_INVALID ) {
            star_version = Strings.getInputInt(in, "Give NMR-STAR version id: 2.1.1(0), 3.0(1), 3.1(2)", null);
            if ( ( star_version < 0 ) || (star_version > 2 )) {
                General.showWarning("star version should be in range [0,2] but given: " + star_version );
                star_version = NmrStar.STAR_VERSION_INVALID;
            }
        }

        while ( mr_convert_dir == null ) {
            mr_convert_dir = Strings.getInputString(in, "Give input dir?", null);
            File file = new File( mr_convert_dir );
            if ( ! ( file.exists() && file.isDirectory() ) ) {
                General.showWarning("dir doesn't exist or isn't a dir: " + mr_convert_dir );
                mr_convert_dir = null;
            }
        }

        while ( str_convert_dir == null ) {
            str_convert_dir = Strings.getInputString(in, "Give output dir?", null);
            File file = new File( str_convert_dir );
            if ( ! ( file.exists() && file.isDirectory() ) ) {
                General.showWarning("dir doesn't exist or isn't a dir: " + str_convert_dir );
                str_convert_dir = null;
            }
        }

        // Get the entry list on disk
        ArrayList entries_ref     = getEntriesFromMRFiles( mr_convert_dir );

        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if ( entries_ref.size() < ENTRIES_POSSIBLE_MAX )
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" +
            entries_ref.subList(0,max_entries_to_show) );
        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean( in,
            "Convert one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();

        while ( ! Strings.is_pdb_code(pdb_entry_id) )
        {
            if ( single_annotate ) {
                pdb_entry_id = Strings.getInputString( in,
                    "Give entry code (e.g.: 1brv) to annotate: ", null );
            }
            else {
                pdb_entry_id = Strings.getInputString( in,
                    "Give entry code (e.g.: 1brv or .) to start annotation with: ", null );
            }
            // Just use the first one if a . is given.
            if ( pdb_entry_id.endsWith(".") ) {
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
        if ( single_annotate ) {
            entries_togo.add(pdb_entry_id);
        } else
        // Select all the entries following, starting with the one selected
        {
            entries_togo.addAll(entries_ref);
            // Set the selected one to the first position
            General.rotateCollectionToFirst(entries_togo, pdb_entry_id);
            // Truncate the list so the annotator doesn't get a full screen.
            // Remove the last element until done.
            while ( entries_togo.size() > ENTRIES_TOGO_MAX ) {
                entries_togo.remove(entries_togo.size()-1);
            }
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }

        boolean do_all = false;
        // Finally actually do this set.
        for (Iterator i=entries_togo.iterator(); i.hasNext();) {
            pdb_entry_id = i.next().toString();
            status = convertEntry(pdb_entry_id, "a", g, classification, mr_convert_dir, str_convert_dir, star_version);
            if ( ! status ) {
                General.showError("Failed to convert entry: " + pdb_entry_id );
                overall_convert_errors++;
            }
            // Don't ask stupid questions
            if ( single_annotate || (!i.hasNext())) {
                break;
            }
            // Continue with next entry in series?
            if ( ! do_all  ) {
                do_all = Strings.getInputBoolean( in, "Do all now?");
                if ( ! do_all ) {
                    if ( ! Strings.getInputBoolean( in, "Do next entry?") )
                        break;
                }
            }
        }
        if ( overall_convert_errors > 0 ) {
            General.showOutput("Number of entries with conversion errors: " + overall_convert_errors);
        } else {
            General.showOutput("Number of entries with conversion errors: " + overall_convert_errors);
        }
        return (overall_convert_errors == 0);
    }


    /** The entry point for the program MRInterloop.
     * @param args Ignored.
     */
    public static void main (String[] args)
    {
        General.showOutput("MRInterloop, version 2.4");
        General.setVerbosityToDebug();

        // Change some of the standard settings defined in the Globals class
        Globals g = new Globals();
        g.m.put( "verbosity",         new Integer( 2 ) );
        General.showOutput( g.getMap() );
        // Open Episode_II database connection
        sql_epiII = new SQL_Episode_II( g );

        General.showOutput("Opened sql connection:" + sql_epiII );

            // Read in the classifications possible for annotation.
        Classification classi = new Classification();
        if (!classi.readFromCsvFile("Data/classification.csv")){
            General.showError("in MRInterloop.main found:");
            General.showError("reading classification file.");
            System.exit(1);
        }
        /** Very simple loop as a menu.
         *Start over every time a sequence of entries has been done.
         *Very fast though to get the info again so not a performance issue.
         */
        in = new BufferedReader(new InputStreamReader(System.in));

        boolean repeat = true;
        ArrayList options = new ArrayList();
        options.add("y");
        options.add("n");
        options.add("c");
        options.add("l");
        while ( repeat ) {
            String mode = Strings.getInputString( in,
                "Load database (y)\ndump database(n)\nconvert all MR files in directory(c) or\n" +
                "load DOCR/FRED db files (l) ", options, null);
            if ( mode.startsWith("y") ) {
                loadLoop(g, classi);
            } else if ( mode.startsWith("n")) {
                dumpLoop(g, classi);
            } else if (mode.startsWith("c")) {
                convertLoop(g, classi);
            } else if (mode.startsWith("l")) {
                load_DOCR_FRED_Db_Loop(g, classi);
            } else {
                General.showError("in MRInterloop.main found: unallowed mode: " + mode);
            }
            repeat = Strings.getInputBoolean(in, "Start over?");
        }
    }
}
