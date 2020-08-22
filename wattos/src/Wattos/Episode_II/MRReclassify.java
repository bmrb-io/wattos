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
import com.braju.format.*;              // printf equivalent
import Wattos.Utils.*;

/**
 *Main program for manually changing the classification for entries in the
 *annotated directory.
 *
 * @author  Jurgen F. Doreleijers
 * @version 0.1 
 */
public class MRReclassify {

    /** Maximum number of entries an annotator can do in one session.*/
    static final int ENTRIES_TOGO_MAX = 99999;
    /** Maximum number of entries an annotator will see as a suggestion */
    static final int ENTRIES_POSSIBLE_MAX = 20;

    static SQL_Episode_II sql_epiII = null;
    
    /** Start annotations in a loop.
     * @param g Global info for example on the location of files and preferred text editor.
     * @param classification Allowed block types as read from a csv file.
     */
    public static void loopOverEntries(Globals g, Classification classification) {
//        boolean status;
//        boolean testing              = g.getValueBoolean( "testing" );             
        String  mr_anno_dir          = g.getValueString(  "mr_anno_dir" );

        // Get the entry list in db
        ArrayList entries_ref     = getEntriesFromMRFiles( mr_anno_dir );
        
        int max_entries_to_show = ENTRIES_POSSIBLE_MAX;
        if ( entries_ref.size() < ENTRIES_POSSIBLE_MAX ) 
            max_entries_to_show = entries_ref.size();

        General.showOutput("Possible entries include:" + 
            entries_ref.subList(0,max_entries_to_show) );
        // Get the entries to do
        boolean single_annotate = Strings.getInputBoolean(
            "Change one entry(y) or consecutive entries(n)?");
        String pdb_entry_id = "";
        ArrayList entries_togo = new ArrayList();

        while ( ! Strings.is_pdb_code(pdb_entry_id) ) 
        {
            if ( single_annotate ) {
                pdb_entry_id = Strings.getInputString(
                    "Give entry code (e.g.: 1brv) to annotate: " );
            }
            else {
                pdb_entry_id = Strings.getInputString(
                    "Give entry code (e.g.: 1brv) to start annotation with: " );
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
            reclassifyEntry(pdb_entry_id, "a", g, classification);
            // Don't ask stupid questions
            if ( single_annotate || (!i.hasNext())) {
                break;
            }
            // Continue with next entry in series?
            if ( ! do_all  ) {
                do_all = Strings.getInputBoolean("Do all now?");
                if ( ! do_all ) {
                    if ( ! Strings.getInputBoolean("Do next entry?") )
                        break;
                }
            }
        }
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
    public static void reclassifyEntry(String pdb_entry_id, String archive_id, Globals g, Classification classification) {
        
        boolean status;
        int[] checks = {1,2,3,4};
        
        HashMap class_map = new HashMap();
        class_map.put(  new String[] { "XPLOR",             null, null, null }, 
                        new String[] { "XPLOR/CNS",         null, null, null } );
        class_map.put(  new String[] { "BIOSYM",            null, null, null }, 
                        new String[] { "DISCOVER",          null, null, null } );
        class_map.put(  new String[] { "DIANA",             null, null, null }, 
                        new String[] { "DYANA/DIANA",       null, null, null } );
        class_map.put(  new String[] { "MARDIGRAS",         null, null, null }, 
                        new String[] { "MARDIGRAS/CORMA",   null, null, null } );
        class_map.put(  new String[] { null,                "mapping table",        null, null }, 
                        new String[] { null,                "nomenclature mapping", null, null } );
        
        // Target location
        String dir = null;
        String fs  = File.separator;
//        File file_new = null;
        
        dir = g.getValueString(  "mr_anno_dir" );
        
        String filename = dir + fs + pdb_entry_id + ".mr";
        DBMRFile mrf = new DBMRFile(filename);

        // Read it
        status = mrf.readFromFile();
        if ( ! status ) {
            General.showError("in MRReclassify.reclassifyEntry found:");
            General.showError("Read NOT successful. Skipping this entry.");
            return;
        }
        
        // Reclassify it
        int changes = mrf.reclassify(class_map);
        if ( changes == -1 ) {
            General.showError("in MRReclassify.reclassifyEntry found:");
            General.showError("Reclassification not successful" );
            return;
        }            
        if ( changes == 0 ) {
            General.showOutput("No changes so the file doesn't need to be rewritten.");
            return;
        }

        // Redundant check if program and settings are correct.
        status = mrf.check(checks,classification,g);
        if ( ! status ) {
            General.showError("in MRReclassify.reclassifyEntry found:");
            General.showError("Checks not successful." );
            return;
        }

        // Write the file with block classification.
        status = mrf.deleteFileFromDisk();
        if ( ! status ) {
            General.showError("in MRReclassify.reclassifyEntry found:");
            General.showError("Delete not successful; information lost!" );
            General.showError("Exiting to prevent more loses." );
            System.exit(1);
            return;
        }
        
        status = mrf.writeToFile( false );
        if ( ! status ) {
            General.showError("in MRReclassify.reclassifyEntry found:");
            General.showError("Write not successful" );
            return;
        }
    }    
    
    /** The entry point for the program MRAnnotator.
     * @param args Ignored.
     */
    public static void main (String[] args) 
    {
        // Change some of the standard settings defined in the Globals class
        Globals g = new Globals();
        
            // Read in the classifications possible for annotation.
        Classification classi = new Classification();                
        if (!classi.readFromCsvFile("Data/classification.csv")){
            General.showError("in MRReclassify.main found:");
            General.showError("reading classification file.");
            System.exit(1);
        }
        /** Very simple loop as a menu.
         *Start over every time a sequence of entries has been done.
         *Very fast though to get the info again so not a performance issue.
         */
        boolean repeat = true;
        while ( repeat ) {
            loopOverEntries(g, classi);
            repeat = Strings.getInputBoolean("Start over?");
        }
    }
} 
