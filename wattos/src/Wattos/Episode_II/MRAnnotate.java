/*
 * Episode_II.java
 *
 * Created on December 4, 2001, 11:46 AM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import java.io.*;
import java.util.*;
import com.braju.format.*; // printf equivalent
import Wattos.Utils.*;

/**
 *Main program for annotating MR files for PDB entries. Tips for JEdit usage: - If you get messages like:
 * STDERR>2:42:05 PM [error] KeyEventTranslator: Invalid key stroke: Redefine the keyboard shortcut to something like
 * ESC.
 *
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRAnnotate {

    /** Maximum number of entries an annotator can do in one session. */
    static final int ENTRIES_TOGO_MAX = 20;
    /** Maximum number of entries an annotator will see as a suggestion */
    static final int ENTRIES_POSSIBLE_MAX = ENTRIES_TOGO_MAX;

    static SQL_Episode_II sql_epiII = null;

    /**
     * Start annotations in a loop.
     *
     * @param g
     *            Global info for example on the location of files and preferred text editor.
     * @param classification
     *            Allowed block types as read from a csv file.
     */
    public static void annotateLoop(Globals g, Classification classification, int star_version) {
        boolean status;
        // boolean testing = g.getValueBoolean( "testing" );
        String mr_dir = g.getValueString("mr_dir");
        String wwPDB_dir = g.getValueString("wwPDB_dir");
        String mr_anno_progress_dir = g.getValueString("mr_anno_progress_dir");
        String pdb_entry_id = "";

        Parameters p = new Parameters(); // Printf parameters

        // Get the entry list in archive
        ArrayList entries_archive = getEntriesFromMRFiles(mr_dir, true);
        Format.printf("Found %6d entries in dir %s\n", p.add(entries_archive.size()).add(mr_dir));
        // In progress entries
        ArrayList entries_in_progress = getEntriesFromMRFiles(mr_anno_progress_dir, false);
        p = new Parameters();
        Format.printf("Found %6d entries in dir %s\n", p.add(entries_in_progress.size()).add(mr_anno_progress_dir));
        // Annotated entries
        ArrayList entries_annotated = getEntriesFromClassifiedMRFiles();
        p = new Parameters();
        Format.printf("Found %6d entries in database\n", p.add(entries_annotated.size()));
        // General.showOutput(Strings.toString(entries_annotated));
        // Get the entry list in wwPDB archive
        ArrayList entries_wwPDB_archive = getEntriesFromWwPdbFiles(wwPDB_dir, true);
        Format.printf("Found %6d entries in wwPDB dir %s\n", p.add(entries_wwPDB_archive.size()).add(wwPDB_dir));

        // Find and match entries to be deleted
        // as they no longer exist in the main archive; so they're obsolete.
        ArrayList entries_todelete = new ArrayList();
        entries_todelete.addAll(entries_annotated);
        entries_todelete.removeAll(entries_archive);
        if (entries_todelete.size() > 0) {
            status = removeMRFilesInteractivelyFromDB(entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.annotateLoop found:");
                General.showError("removeMRFilesInteractively failed");
                System.exit(1);
            }
        } else {
            General.showOutput("No annotated     entries obsolete now");
        }
        entries_annotated.removeAll(entries_todelete);

        // Same for entries in the 'in progress' dir
        entries_todelete = new ArrayList();
        entries_todelete.addAll(entries_in_progress);
        entries_todelete.removeAll(entries_archive);
        if (entries_todelete.size() > 0) {
            status = removeMRFilesInteractively(mr_anno_progress_dir, entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.annotateLoop found:");
                General.showError("removeMRFilesInteractively failed");
                System.exit(1);
            }
        } else {
            General.showOutput("No 'in progress' entries obsolete now");
        }
        entries_in_progress.removeAll(entries_todelete);

        // Same for entries in the 'in progress' dir
        entries_todelete = new ArrayList();
        entries_todelete.addAll(entries_wwPDB_archive);
        entries_todelete.removeAll(entries_archive);
        if (entries_todelete.size() > 0) {
            status = removeWwPdbFilesInteractively(wwPDB_dir, entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.annotateLoop found:");
                General.showError("removeWwPdbFilesInteractively failed");
                System.exit(1);
            }
        } else {
            General.showOutput("No wwPDB entries obsolete now");
        }

        // Entries to be finished after initial tries
        General.showOutput("Entries in progress:             [" + entries_in_progress.size() + "]");

        // Entries to be started a new
        ArrayList entries_tostart = new ArrayList();
        entries_tostart.addAll(entries_archive);
        entries_tostart.removeAll(entries_annotated);
        entries_tostart.removeAll(entries_in_progress);
        General.showOutput("Entries to start anew:           [" + entries_tostart.size() + "]");

        // Get the archive from the user that he/she wants to work on
        ArrayList allows = new ArrayList();
        allows.add("m");
        allows.add("i");
        allows.add("d");
        allows.add("c");
        allows.add("q");
        String archive_id = Strings.getInputString(
                "Annotate from mirror(m), in progress(i), database(d)?\nClean DBFS files(c) or quit(q)", allows);
        ArrayList entries_ref = null;
        if (archive_id.equals("m"))
            entries_ref = entries_tostart;
        else if (archive_id.equals("i"))
            entries_ref = entries_in_progress;
        else if (archive_id.equals("d"))
            entries_ref = entries_annotated;
        else if (archive_id.equals("c")) {
            General.showOutput("Starting to makeDBFSConsistent");
            if (!sql_epiII.makeDBFSConsistent()) {
                General.showError("Failed makeDBFSConsistent, please contact maintainer with this message");
            }
            return;
        } else if (archive_id.equals("q")) {
            return;
        }

        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if (entries_ref.size() < ENTRIES_POSSIBLE_MAX)
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" + entries_ref.subList(0, max_entries_to_show));
        String entry_example = "";
        if (entries_ref.size() > 0) {
            entry_example = (String) entries_ref.get(0);
        } else {
            entry_example = "n/a";
        }
        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean("Annotate one entry(y) or consecutive entries(n)?");

        boolean use_all = true;
        ArrayList entries_user = new ArrayList();

        if (!single_annotate) {
            use_all = Strings.getInputBoolean("Use all from above (y) or specify your own list(n)");
            if (!use_all) {
                String user_entry_list_string = Strings.getInputString("Enter list of entries space/comma separated: ");
                user_entry_list_string = user_entry_list_string.replaceAll(",", " "); // just for the heck of it.

                StringTokenizer tokens = new StringTokenizer(user_entry_list_string, " ");
                while (tokens.hasMoreTokens()) {
                    pdb_entry_id = tokens.nextToken();
                    // Skip those that are not valid or present.
                    if (!Strings.is_pdb_code(pdb_entry_id)) {
                        General.showWarning("Entry code given doesn't look like a PDB code");
                        continue;
                    }
                    if (!entries_ref.contains(pdb_entry_id)) {
                        General.showWarning("Entry doesn't seem to exist in the archive given.");
                        continue;
                    }
                    entries_user.add(pdb_entry_id);
                }
                // Use this info to start the sequence by.
                if (entries_user.size() > 0) {
                    pdb_entry_id = (String) entries_user.get(0);
                }
            }
        }

        ArrayList entries_togo = new ArrayList();

        while (!Strings.is_pdb_code(pdb_entry_id)) {
            if (single_annotate) {
                pdb_entry_id = Strings.getInputString("Give entry code (e.g.: " + entry_example + ") to annotate: ");
            } else {
                pdb_entry_id = Strings.getInputString("Give entry code (e.g.: " + entry_example
                        + ") to start annotation with: ");
            }
            if (pdb_entry_id.equals(".")) {
                pdb_entry_id = entries_ref.get(0).toString();
            }
            if (!Strings.is_pdb_code(pdb_entry_id)) {
                General.showWarning("Entry code given doesn't look like a PDB code");
                pdb_entry_id = "";
            }
            if (!entries_ref.contains(pdb_entry_id)) {
                General.showWarning("Entry doesn't seem to exist in the archive given.");
                pdb_entry_id = "";
            }
        }
        // Just do this single one
        if (single_annotate) {
            entries_togo.add(pdb_entry_id);
        } else
        // Select all the entries following, starting with the one selected
        {
            // Get all entries possible or those specified by the user.
            if (use_all) {
                entries_togo.addAll(entries_ref);
            } else {
                entries_togo.addAll(entries_user);
            }
            // Set the selected one to the first position
            General.rotateCollectionToFirst(entries_togo, pdb_entry_id);
            // Truncate the list so the annotator doesn't get a full screen.
            // Remove the last element until done.
            while (entries_togo.size() > ENTRIES_TOGO_MAX) {
                entries_togo.remove(entries_togo.size() - 1);
            }
            General.showOutput("Entry series in the order they will be presented:");
            General.showOutput(entries_togo.toString());
        }

        // Finally actually do this set.
        for (Iterator i = entries_togo.iterator(); i.hasNext();) {
            General.showOutput("\n\n");
            pdb_entry_id = i.next().toString();
            annotateEntry(pdb_entry_id, archive_id, g, classification, star_version);
            // Don't ask stupid questions
            if (single_annotate || (!i.hasNext())) {
                break;
            }
            // Continue with next entry in series?
            if (!Strings.getInputBoolean("Continue annotation series?")) {
                break;
            }
        }
    }

    /**
     * Annotate one entry. User will be informed of whether the annotation conforms to allowed changes and will be given
     * the option to correct any mishaps remaining in the specified text editor.
     *
     * @param pdb_entry_id
     *            The PDB entry code of the MR file to be annotated.
     * @param archive_id
     *            The id for the archive to be used as the original for the annotation. Allowed values are a, i, and r;
     *            see code.
     * @param g
     *            Global info for example on the location of files and preferred text editor.
     * @param classification
     *            Allowed block types as read from a csv file.
     */
    public static void annotateEntry(String pdb_entry_id, String archive_id, Globals g, Classification classification,
            int star_version) {

        boolean status;
        int[] checks = { 1, 2, 3, 4 };
        String fs = File.separator;

        ArrayList al = new ArrayList();
        al.add(pdb_entry_id);

        // Source location
        String dir = null;
        String filename = null;
        DBMRFile mrf = null;

        // Target location
        // File file_new = null;

        dir = g.getValueString("mr_anno_progress_dir");
        filename = dir + fs + pdb_entry_id + ".mr";
        mrf = new DBMRFile(filename);
        mrf.detail = SQL_Episode_II.FILE_DETAIL_CLASSIFIED;
        mrf.pdb_id = pdb_entry_id;

        // Perhaps move it first to 'in progress' (i)
        if (archive_id.equals("m") || archive_id.equals("d")) {
            // Confirmed delete the target if present in the in progress dir
            if (mrf.file.exists()) {
                if (archive_id.equals("d")) {
                    General.showWarning("in MRAnnotate.annotateEntry");
                    General.showError("WARNING: file exists in 'in progress' dir that should be'");
                }
                status = mrf.deleteFileFromDiskConfirmed();
                if (!status) {
                    General.showError("in MRAnnotate.annotateEntry found:");
                    General.showError("Deleting the MR file failed.");
                    General.showError("Skipping this entry.");
                    return;
                }
            }
        }

        // Copy the file from the archive
        if (archive_id.equals("m")) {
            String chars2And3 = pdb_entry_id.substring(1, 3);
            String filename_org = g.getValueString("mr_dir") + fs + chars2And3 + fs + pdb_entry_id + ".mr.gz";
            // status = FileCopy.copy( new File(filename_org), mrf.file, true, true);
            status = InOut.gunzipFile(new File(filename_org), mrf.file);
            if (!status) {
                General.showError("in MRAnnotate.annotateEntry found:");
                General.showError("Gunzipping the MR file failed.");
                General.showError("Skipping this entry.");
                return;
            }
        }

        // Move the file from the database into the 'in progress' dir
        // The assumption here is that this does not need to be done in 1
        // transaction because the second step is pretty trivial.
        if (archive_id.equals("d")) {
            General.showOutput("Get the MR file_id from the pdb code and detail value.");
            ArrayList mrfile_ids = sql_epiII.getMRFileIdsByPDBIdByDetail(pdb_entry_id,
                    SQL_Episode_II.FILE_DETAIL_CLASSIFIED);
            if (mrfile_ids.size() < 1) {
                General.showError("in MRAnnotate.annotateEntry");
                General.showError("failed to get 1 or more mrfile ids for pdb entry code:" + pdb_entry_id);
                General.showError("Skipping this entry.");
                return;
            }
            // Use the first one only
            if (mrfile_ids.size() > 1) {
                General.showWarning("in MRAnnotate.annotateEntry");
                General.showError("WARNING: got more mrfile ids for pdb entry code:" + pdb_entry_id);
                General.showError("WARNING: will just use the first one");
            }
            mrf.mrfile_id = ((Integer) mrfile_ids.get(0)).intValue();

            // Step I
            // Copy the file from the database to memory
            General.showOutput("Copy the MR file from the database.");
            status = sql_epiII.getMRFile(mrf);
            if (!status) {
                General.showError("in MRAnnotate.annotateEntry");
                General.showError("failed to get mrfile from db.");
                General.showError("Skipping this entry.");
                return;
            }
            // Reset these values because the "get" will have overwritten them.
            mrf.detail = SQL_Episode_II.FILE_DETAIL_CLASSIFIED;
            mrf.pdb_id = pdb_entry_id;

            // Step II
            // Copy the file from memory to the file system.
            boolean useLines = false; // The data is already split when in the db.
            mrf.writeToFile(useLines);

            // Step III
            // Delete the copy in the database to insure consistency
            General.showOutput("Removing all the files associated with this PDB entry from the database.");
            status = sql_epiII.deleteEntryByPDBIds(al);
            if (!status) {
                General.showError("in MRAnnotate.annotateEntry");
                General.showError("failed to delete mrfile from db.");
                General.showError("used pdb_entry_id:" + pdb_entry_id);
                General.showError("Skipping this entry.");
                return;
            }
        }

        boolean continue_edit = true;
        // Continue as long as the user wants to or the file doesn't have errors.
        while (continue_edit) {
            status = mrf.edit(g);
            if (!status) {
                General.showError("in MRAnnotate.annotateEntry found:");
                General.showError("Error editing this entry. Please remove temporary files");
                General.showError("Skipping this entry.");
                return;
            }
            status = mrf.readFromFile();
            if (!status) {
                General.showError("in MRAnnotate.annotateEntry found:");
                General.showError("Read NOT successful. Please remove temporary files");
                General.showError("Skipping this entry.");
                return;
            }
            mrf.setBlockTextType(SQL_Episode_II.BLOCK_DETAIL_RAW);
            status = mrf.check(checks, classification, g);

            // Commit to database if possible
            if (status) {
                General.showOutput("Checks successful.");
                General.showOutput("Moving the MR file to the database.");
                status = mrf.writeToDB(sql_epiII);
                if (!status) {
                    General.showError("MR file write to DB NOT successful.");
                    General.showOutput("Keeping the MR file in the 'in progress' directory for now.");
                } else {
                    // The next method does the actual work of converting the data and
                    // returning a list of converted file(s) back.
                    ArrayList files_conv = mrf.doConversions(classification, star_version);

                    // Check for error status. An empty list is fine.
                    if (files_conv == null) {
                        status = false;
                    } else {
                        // An empty list is fine.
                        if (files_conv.size() == 0) {
                            General.showWarning("No resulting files.");
                        }
                    }
                    if (status) {
                        // Write the individual converted files to the database.
                        status = DBMRFile.putFilesInDatabase(classification, files_conv, sql_epiII);
                    }

                    if (status) {
                        // All was well and we don't need to continue the edit.
                        General.showOutput("Deleting MR file from the in progress dir:" + mrf);
                        if (!mrf.deleteFileFromDisk()) {
                            General.showError("in MRAnnotate.annotateEntry");
                            General.showError("failed to delete mrfile from in progress dir.");
                            General.showError("Skipping this entry.");
                        } else {
                            General.showOutput("Successfully deleted MR file from the in progress dir.");
                        }
                        continue_edit = false;
                    } else {
                        General.showError("conversions/write to DB NOT successful.");
                        General.showError("removing MR files from the database.");

                        status = sql_epiII.deleteEntryByPDBIds(al);
                        if (!status) {
                            General.showError("in MRAnnotate.annotateEntry");
                            General.showError("failed to delete mrfile from db.");
                            General.showError("used pdb_entry_id:" + pdb_entry_id);
                            General.showError("Skipping this entry.");
                        }

                        General.showOutput("Keeping the MR file in the 'in progress' directory.");
                        continue_edit = Strings.getInputBoolean("Finish this entry(y) or skip for now(n)");
                    }
                }
            } else {
                General.showWarning("Checks not successful.");
                // Did we start from m
                General.showOutput("Keeping the MR file in the 'in progress' directory.");
                continue_edit = Strings.getInputBoolean("Finish this entry(y) or skip for now(n)");
            }
        }
    }

    /**
     * Remove a bunch of mr files from a directory. Will print a line mentioning the delete for each entry. Assumes
     * standard naming scheme for entries as usual.
     *
     * @param dir
     *            Directory from which the MR files are to be deleted.
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFiles(String dir, ArrayList entries_todelete) {

        for (Iterator i = entries_todelete.iterator(); i.hasNext();) {
            String entry_code = (String) i.next();
            String fname = dir + File.separator + entry_code + ".mr";
            File f = new File(fname);
            if (!f.delete()) {
                General.showError("in MRAnnotate.removeMRFiles found:");
                General.showError("Deleting the annotated MR file [" + fname + "]");
                return false;
            } else {
                General.showOutput("Deleted the annotated MR file for entry: [" + entry_code + "]");
            }
        }
        return true;
    }

    /**
     * Remove a bunch of entry dirs from a directory. Will print a line mentioning the delete for each entry. Assumes
     * standard naming scheme for entries as usual.
     *
     * @param dir
     *            Directory from which the MR files are to be deleted.
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeWwPdbFiles(String dir, ArrayList entries_todelete) {

        for (Iterator i = entries_todelete.iterator(); i.hasNext();) {
            String entry_code = (String) i.next();
            String char23 = entry_code.substring(1, 3);
            String dname = dir + File.separator + char23 + File.separator + entry_code;
            File f = new File(dname);
            if (!InOut.deleteDirectoryRecursively(f)) {
                General.showError("in MRAnnotate.removeWwPdbFiles found:");
                General.showError("Deleting the wwPDB entry dir [" + dname + "]");
                return false;
            } else {
                General.showOutput("Deleted the wwPDB entry dir for entry: [" + entry_code + "]");
            }
        }
        return true;
    }

    /**
     * Remove a bunch of mr files from the database. Will print a line mentioning the delete for each entry. Assumes
     * standard naming scheme for entries as usual meaning that the mrfiles should have the detail value set to:
     * FILE_DETAIL_CLASSIFIED
     *
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesFromDB(ArrayList entries_todelete) {

        /**
         * original boolean status = sql_epiII.deleteMRFilesByPDBIdsByDetail( entries_todelete,
         * SQL_Episode_II.FILE_DETAIL_CLASSIFIED );
         */
        boolean status = sql_epiII.deleteEntryByPDBIds(entries_todelete);
        if (!status) {
            General.showError("in MRAnnotate.removeMRFilesFromDB found:");
            General.showError("Deleting the annotated MR files:" + entries_todelete);
            return false;
        } else {
            General.showOutput("Deleted the annotated MR files:" + entries_todelete);
        }
        return true;
    }

    /**
     * Uses <code>removeMRFiles</code> but will first list the files to be deleted and will then ask for confirmation.
     *
     * @param dir
     *            Directory from which the MR files are to be deleted.
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeWwPdbFilesInteractively(String dir, ArrayList entries_todelete) {

        // String reply = "bogus";
        boolean status;

        General.showOutput("Entries in wwPDB to delete:            [" + entries_todelete.size() + "]");
        General.showOutput("Entries in wwPDB to delete: " + entries_todelete.toString());

        General.showWarning("answering yes to the following question will lead to");
        General.showWarning("deleting work that might not be recovered!");

        String prompt = "Delete ALL the wwPDB files for the above entries from the directory:" + dir + "?";
        if (Strings.getInputBoolean(prompt)) {
            General.showOutput("Deleting the wwPDB files listed above.");
            status = removeWwPdbFiles(dir, entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.removeWwPdbFilesInteractively found:");
                General.showError("Deleting the wwPDB files failed.");
                return false;
            }
        }
        return true;
    }

    /**
     * Uses <code>removeMRFiles</code> but will first list the files to be deleted and will then ask for confirmation.
     *
     * @param dir
     *            Directory from which the MR files are to be deleted.
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesInteractively(String dir, ArrayList entries_todelete) {

        // String reply = "bogus";
        boolean status;

        General.showOutput("Entries to delete:            [" + entries_todelete.size() + "]");
        General.showOutput("Entries to delete: " + entries_todelete.toString());

        General.showWarning("answering yes to the following question will lead to");
        General.showWarning("deleting work that might not be recovered!");

        String prompt = "Delete ALL the MR files for the above entries from the directory:" + dir + "?";
        if (Strings.getInputBoolean(prompt)) {
            General.showOutput("Deleting the annotated MR files listed above.");
            status = removeMRFiles(dir, entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.removeMRFilesInteractively found:");
                General.showError("Deleting the annotated MR files failed.");
                return false;
            }
        }
        return true;
    }

    /**
     * Uses <code>removeMRFiles</code> but will first list the files to be deleted and will then ask for conformation.
     *
     * @param entries_todelete
     *            List of entry codes for which the MR files are to be deleted.
     * @return <CODE>true</CODE> only if all operations were successful.
     */
    public static boolean removeMRFilesInteractivelyFromDB(ArrayList entries_todelete) {

        // String reply = "bogus";
        boolean status;

        General.showOutput("Entries to delete:            [" + entries_todelete.size() + "]");
        General.showOutput("Entries to delete: " + entries_todelete.toString());

        General.showWarning("answering yes to the following question will lead to");
        General.showWarning("deleting work that might not be recovered!");

        String prompt = "Delete ALL the MR files for the above entries from the DB?";
        if (Strings.getInputBoolean(prompt)) {
            General.showOutput("Deleting the annotated MR files listed above.");
            status = removeMRFilesFromDB(entries_todelete);
            if (!status) {
                General.showError("in MRAnnotate.removeMRFilesInteractively found:");
                General.showError("Deleting the annotated MR files failed.");
                return false;
            }
        }
        return true;
    }

    /**
     * Will look for all entry directories in a given directory and those for which xxxx seems to be a valid PDB code
     * will use them. It will issue warnings for files where xxxx doesn't seem to be a PDB code. Expects a hierarchy
     * like /ab/1abc/1abc.str.gz. where it will return entries for all dirs like /ab/1abc
     *
     * @param dir
     *            Directory from which the files are to be taken.
     * @return List of entries in the directory for which there are files named as described. The list of PDB entry
     *         codes returned is sorted too.
     */
    public static ArrayList getEntriesFromWwPdbFiles(String dir, boolean warnEmpty) {

        ArrayList entries = new ArrayList();

        ArrayList<File> fileList = InOut.getFilesRecursive(new File(dir), true);
        if (fileList == null) {
            General.showWarning("Found NO entries in directory: " + dir);
            return entries;
        }

        // ArrayList str_files = new ArrayList();
        for (int i = 0; i < fileList.size(); i++) {
            File f = fileList.get(i);
            String s = f.toString();
            // differentiate
            // DEBUG: Found file: /Users/jd/NRG/wwPDB/.DS_Store
            // DEBUG: Found file: /Users/jd/NRG/wwPDB/ab/1abc/1abc.str.gz
            // DEBUG: Found file: /Users/jd/NRG/wwPDB/ab/1abc/1abc_ccpn.tgz
            // DEBUG: Found file: /Users/jd/NRG/wwPDB/ab/1abc
            // DEBUG: Found file: /Users/jd/NRG/wwPDB/ab
            if (!f.isDirectory()) {
                continue;
            }
            int lengthPath = s.length();
            String entry_code = s.substring(lengthPath - 4, lengthPath);
            // General.showDebug("Found directory: " + s + " considering entry_code: " + entry_code);
            if (Wattos.Utils.Strings.is_pdb_code(entry_code)) {
                entries.add(entry_code);
            } else {
                // General.showDebug("Skipping this dir.");
                // General.showDebug("String doesn't look like a pdb code");
            }
        }

        if (warnEmpty && entries.size() == 0) {
            General.showWarning("Found files but no NO entries on file in directory: " + dir);
            return entries;
        }

        Collections.sort(entries);
        return entries;
    }

    /**
     * Will look for all files named xxxx.mr in the given directory and those for which xxxx seems to be a valid PDB
     * code will use them as an PDB MR file. It will issue warnings for files ending with .mr for which xxxx doesn't
     * seem to be a PDB code.
     *
     * @param dir
     *            Directory from which the MR files are to be taken.
     * @return List of entries in the directory for which there are files named as described. The list of PDB entry
     *         codes returned is sorted too.
     */
    public static ArrayList getEntriesFromMRFiles(String dir, boolean warnEmpty) {

        ArrayList entries = new ArrayList();

        ArrayList fileList = InOut.getFilesRecursive(new File(dir), false);
        if (fileList == null) {
            General.showWarning("Found NO files in directory: " + dir);
            return entries;
        }
        ArrayList mr_files = new ArrayList();
        for (int i = 0; i < fileList.size(); i++) {
            File f = (File) fileList.get(i);
            String s = f.toString();
            if (s.endsWith(".mr") || s.endsWith(".mr.gz")) {
                mr_files.add(f);
            }
        }
        if (warnEmpty && mr_files.size() == 0) {
            General.showWarning("Found files but no NO entries on file in directory: " + dir);
            return entries;
        }

        // Check if the code conforms
        for (int i = 0; i < mr_files.size(); i++) {
            File f = (File) mr_files.get(i);
            String fname = f.getName(); // e.g. 1brv.mr.gz
            String entry_code = fname.substring(0, 4);
            // Check whether that's reasonable by matching against reg.exp.
            if (Wattos.Utils.Strings.is_pdb_code(entry_code)) {
                entries.add(entry_code);
            } else {
                General.showWarning("Skipping this file.");
                General.showWarning("String for filename [" + fname + "] doesn't look like a pdb code: " + entry_code);
            }
        }
        Collections.sort(entries);
        return entries;
    }

    /**
     * Will look for all mrfiles in the database that have the standard details: mrfile.details =
     * SQL_Episode_II.FILE_DETAIL_CLASSIFIED
     *
     * @return List of entries in the directory for which there are files named as described. The list of PDB entry
     *         codes returned is sorted too.
     */
    public static ArrayList getEntriesFromClassifiedMRFiles() {
        ArrayList entries = sql_epiII.getPDBIdFromMRFileByDetail(SQL_Episode_II.FILE_DETAIL_CLASSIFIED);
        if (entries == null) {
            General.showError("getting entry codes from the db.");
            return (entries);
        }
        Collections.sort(entries);
        return entries;
    }

    /**
     * The entry point for the program MRAnnotator.
     *
     * @param args
     *            Ignored.
     */
    public static void main(String[] args) {
        // Change some of the standard settings defined in the Globals class
        General.verbosity = General.verbosityOutput;
//        General.setVerbosityToDebug();
        Globals g = new Globals();
        g.showMap();
        // Open Episode_II database connection
        Properties db_properties = new Properties();
        db_properties.setProperty("db_conn_string", g.getValueString("db_conn_string"));
        db_properties.setProperty("db_username", g.getValueString("db_username"));
        db_properties.setProperty("db_driver", g.getValueString("db_driver"));
        db_properties.setProperty("db_password", g.getValueString("db_password"));
        sql_epiII = new SQL_Episode_II(db_properties);

        General.showOutput("Opened sql connection:" + sql_epiII);

        // Read in the classifications possible for annotation.
        Classification classi = new Classification();
        if (!classi.readFromCsvFile("Data/classification.csv")) {
            General.showError("in MRAnnotate.main found:");
            General.showError("reading classification file.");
            System.exit(1);
        }
        /**
         * Very simple loop as a menu. Start over every time a sequence of entries has been done. Very fast though to
         * get the info again so not a performance issue.
         */
        boolean repeat = true;
        int star_version = NmrStar.STAR_VERSION_DEFAULT;
        while (repeat) {
            annotateLoop(g, classi, star_version);
            repeat = Strings.getInputBoolean("Start over?");
        }
    }
}
