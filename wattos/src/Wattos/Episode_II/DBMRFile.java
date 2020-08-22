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
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 * Holds the access to PDB MR files. A bit confusing might be the fact that
 *the data can be in the lines and/or in the blocks parameters.
 * @author  Jurgen F. Doreleijers
 * @version 0.1
 */


/** The class for a PDB MR type of file. Contains methods for checking etc.
 */
public class DBMRFile {
    /** File object.
     */
    public File        file;
    /** Associated id in database
     */
    public int         mrfile_id;
    /** Associated PDB entry code.
     */
    public String      pdb_id;
    /** Associated pdb entry id, maybe null
     */
    public int         entry_id;
    /** Type of data in file, e.g.: "STAR first pass", "original", "test", etc.
     *Must be defined but can have default value of "test" as it is initialized.
     */
    public String      detail;
    public Date        date_modified;

    /** The blocks in an annotated DBMRFile*/
    public ArrayList<DBMRBlock> blocks;
    /** The lines of an DBMRFile*/
    ArrayList lines;
    /**String that annotation comments have to start with.*/
    public static final String PREFIX = "##### BMRB adds: ";
    /** The minimum number of blocks that a valid annotated file should have.*/
    public static final int NUMBER_BLOCKS_MIN=1;
    /** The maximum number of blocks that a valid annotated file should have.*/
    public static final int NUMBER_BLOCKS_MAX=100;
    /** The maximum number of blocks that a valid annotated file can have before a warning is issued.*/
    public static final int NUMBER_BLOCKS_WARNING=20;

    public static final int BLOCK_ID_INDICATING_ALL = -1;

    /** Name the method so it can be called. Initialize the instance fields.
     */
    public void init()
    {
        file        = null;
        mrfile_id   = Wattos.Utils.General.NULL_FOR_INTS;
        pdb_id      = null;        // may be nilled.
        detail      = "default";
        date_modified = new Date(); // Set the date to the current date.
        entry_id    = Wattos.Utils.General.NULL_FOR_INTS;
        lines       = new ArrayList();
        blocks      = new ArrayList();
    }

    /** Name the method so it can be called. Initialize the instance fields.
     */
    public String toString()
    {
        String result;
        if ( file == null ) {
            result = "File name: null";
        } else {
            result = "File name: " + file.getPath();
        }
        return result;
    }

    /** Simple constructor, just setting the <CODE>file</CODE> attribute.
     * @param fname File name of the MR file.
     */
    public DBMRFile(String fname) {
        init();
        file = new File(fname);
    }

    /** Simple constructor     */
    public DBMRFile() {
        init();
    }

    /** Reinitializes the lines and blocks variables and sets the file variable to
     * the given argument.
     * @param file_new New file name.
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean renameTo(File file_new)
    {
        boolean status = file.renameTo(file_new);
        file = file_new;
        date_modified = new Date();
        return(status);
    }


    /** Wrapper method.
     */
    public void addBlock( int index, DBMRBlock mrb )
    {
        blocks.add(index, mrb );
    }

    /** Wrapper method.
     */
    public void addBlock( DBMRBlock mrb )
    {
        blocks.add( mrb );
    }



    /** Create blocks of text by using separation defined by user
     * @return <CODE>true</CODE> indicates success.
     *
     *If the lines variable contains no data take all the content
     *and put it in one block.
     */
    private boolean splitToBlocks()
    {
        General.showOutput("Doing: splitToBlocks");
        // Always one block at least.
        DBMRBlock mrb = new DBMRBlock();
        mrb.position = 0;
        mrb.fileName = file.getName();
        ArrayList linesMRB = new ArrayList();
        if ( lines == null ) {
            General.showError("Failed to split to blocks because no lines available.");
            return false;
        }

        for (Iterator i=lines.iterator();i.hasNext();)
        {
            String line = (String) i.next();
            // Check for start of block
            if ( line.startsWith(PREFIX) )
            {
                // Consider the previous block ended
                if ( linesMRB.size() > 0 ) {
                    General.showOutput("Number of lines in block: "+linesMRB.size());
                    mrb.fillContent( linesMRB );
                    blocks.add( mrb );
                }
                mrb = new DBMRBlock();
                linesMRB = new ArrayList();
                mrb.position = blocks.size();
                mrb.fileName = file.getName();
                if ( ! mrb.setSpecifier(line) ) {
                    General.showError("in DBMRFile.splitToBlocks found:");
                    General.showError("setting specifier -1-");
                    return false;
                }
                while (i.hasNext()) {
                    line = (String) i.next();
                    if ( line.startsWith(PREFIX )) {
                        if ( ! mrb.setSpecifier(line) ) {
                            General.showError("in DBMRFile.splitToBlocks found:");
                            General.showError("setting specifier -2-");
                            return false;
                        }
                    } else {
                        // End of defining block saving this line.
                        linesMRB.add( line );
                        break;
                    }
                }
            } else {
                linesMRB.add( line );
            }
        }

        if ( linesMRB.size() > 0 ) {
            General.showOutput("Number of lines in block: "+linesMRB.size());
            mrb.fillContent( linesMRB );
            blocks.add( mrb );
        }

        General.showDebug("Split text into " + blocks.size() + " blocks");
        if ( blocks.size() > NUMBER_BLOCKS_WARNING ) {
            General.showWarning("more than " + NUMBER_BLOCKS_WARNING + " text blocks is unusual");
        }
        return true;
    }


    /** Interpret the filename to a pdb id. The filename has to start with the
     *four characters that form the code.
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean setPdbIdFromFileName()
    {
        pdb_id = file.getName().substring(0,4);
        if ( ! Strings.is_pdb_code(pdb_id)) {
            General.showWarning("Can't get pdb id from filename's first four characters.");
            General.showWarning("File name: [" + file.getName() + "]");
            return false;
        }
        return true;
    }

    /** Set all block text types for this file to the given string */
    public boolean setBlockTextType( String text_type )
    {
        for ( Iterator i=blocks.iterator(); i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            mrb.text_type = text_type;
        }

        return true;
    }

    /** Put the DBMRFile object into database.
     * @return <CODE>true</CODE> indicates success.
     * @param sql_epiII  */
    public boolean writeToDB( SQL_Episode_II sql_epiII)
    {
        //General.showOutput("Putting DBMRFile object into database.");

        boolean status = sql_epiII.putMRFile( this );
        return status;
    }

    /** Get the DBMRFile object from the database.
     * @return <CODE>true</CODE> indicates success.
     * @param sql_epiII
     * @param mrfile_id
     */
    public boolean readFromDB( SQL_Episode_II sql_epiII, int mrfile_id )
    {
        //General.showOutput("Getting DBMRFile object from database.");

        boolean status = sql_epiII.getMRFile( this, mrfile_id);
        return( status );
    }


    public boolean readFromFile()
    {
        return readFromFile(null);
    }


     /** Read from text file and split to blocks
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean readFromFile(File dir)
    {
        lines = new ArrayList();
        blocks= new ArrayList();

        if ( ! setPdbIdFromFileName() ) {
            General.showError("in DBMRFile.readFromFile found failed to setPdbIdFromFileName.");
            General.showError("error reading file: "+file.getPath()+" in dir: " + dir);
            return false;
        }
        boolean status;
//        General.showOutput("Reading file from dir: " + dir);
        String fileNameExtension = InOut.getFilenameExtension(file);
        if ( InOut.fileNameExtensionCanBeText(fileNameExtension)) {
            General.showOutput("Reading file: " + file.getPath() + " as a textual file.");
            status = readFromTxtFile(dir);
        } else {
            General.showOutput("Reading file: " + file.getPath() + " as a binary file.");
            status = readFromBinFile(dir);
        }
        date_modified = new Date();
        if ( !status ) {
            General.showError("Failed to load an MRFile from: " + file);
        }
        return status;
    }

     /** Read from text file and split to blocks
     * @return <CODE>true</CODE> indicates success.
     */
    private boolean readFromTxtFile( File dir )
    {
        File fullfile = null;
        if ( dir == null ) {
            fullfile = file;
        } else {
            fullfile = new File( dir, file.getName() );
        }
        try {
            FileReader fr = new FileReader( fullfile.getPath());
            BufferedReader in = new BufferedReader( fr );
            String line;
            int line_count = 0;
            while((line = in.readLine()) != null ) {
                line_count++;
                lines.add( line );
            }
            General.showOutput("Read " + line_count + " lines.");
            in.close();
            fr.close(); // perhaps redundant statements follow
            /**
            General.sleep(1000);
            fr = null;
            in = null;
            General.doFullestGarbageCollection();
             */
        } catch (IOException e) {
            General.showThrowable(e);
            General.showError("in DBMRFile.readFromFile found:");
            General.showError("error reading file: "+file.getPath());
            return false;
        }
        boolean status = splitToBlocks();
        return status;
    }

     /** Read from text file and split to blocks
     * @return <CODE>true</CODE> indicates success.
     */
    private boolean readFromBinFile( File dir )
    {
        DBMRBlock mrb = new DBMRBlock();
        mrb.position = 0;
        mrb.fileName = dir.getName();
        blocks.add( mrb );
        lines = null;
        File fullfile = null;
        if ( dir == null ) {
            fullfile = file;
        } else {
            fullfile = new File( dir, file.getName() );
        }
        try {
            int fileSize = (int) fullfile.length();
            mrb.content = new byte[ fileSize ];
            General.showDebug("Attempting to read from file with full name: " + fullfile + " and length: " + fileSize);
            DataInputStream in = new DataInputStream(new FileInputStream( fullfile ));
            in.readFully(mrb.content);
            in.close();
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        General.showOutput("Read " + mrb.content.length + " bytes.");
        return true;
    }


     /** Write text to file with given file name.
      * @return <CODE>true</CODE> indicates success.
      * @param file_name Output filename.
     * @param useLines If true is specified then the content of the lines will be written.
     * Otherwise the contents of the blocks will be written. In the first case
     * the concept of the blocks is lost.
      */
    public boolean writeToFile( String file_name, boolean useLines)
    {
        return( writeToFile(useLines) );
    }

     /** Write text to file with given file name.
      * @return <CODE>true</CODE> indicates success.
     * @param useLines If true is specified then the content of the lines will be written.
     * Otherwise the contents of the blocks will be written. In the first case
     * the concept of the blocks is lost.
     */
    public boolean writeToFile( boolean useLines)
    {
        boolean force_delete = false; // Remove existing copies if need be.
        return( writeToFile(useLines, force_delete) );
    }


    /** Added for functionality without violating previous contract. See doc on namesakes */
    public boolean writeToFile( boolean useLines, boolean force_delete ) {
        return writeToFile( useLines, force_delete, true );
    }

    /** Write text to file.
     * @return <CODE>true</CODE> indicates success.
     * @param force_delete If true then any existing files will be
     * overwritten.
     * @param useLines If true is specified then the content of the lines will be written.
     * Otherwise the contents of the blocks will be written. In the first case
     * the concept of the blocks is lost.
     * @param useAnnotation If false is specified then the annotation will NOT be shown. This is
     *useful for writing out bare star files for instance.
     */
    public boolean writeToFile( boolean useLines, boolean force_delete, boolean useAnnotation )
    {
        if ( file==null ) {
            General.showError("in DBMRFile.writeToFile found:");
            General.showError("No file object present");
            return false;
        }

        if ( file.exists() ) {
            if ( force_delete ) {
                General.showWarning("deleting original file: "+file.getPath());
                file.delete();
            } else {
                if ( ! deleteFileFromDiskConfirmed() ) {
                    General.showError("in DBMRFile.writeToFile found:");
                    General.showError("error writing file: "+file.getPath());
                    General.showError("because old file failed to be deleted");
                    return false;
                }
            }
        }

        General.showOutput("Writing file: " + file.getPath());
        try {
            FileWriter fw = new FileWriter( file.getPath());
            BufferedWriter out = new BufferedWriter( fw );
            if ( useLines ) {
                for (Iterator i=lines.iterator();i.hasNext();) {
                    // End of line was deleted when reading so the Unix eol is
                    // assumed and added here.
                    out.write(i.next().toString()+General.eol);
                }
            } else {
//                int count=0;
                for (Iterator i=blocks.iterator();i.hasNext();) {
                    //General.showOutput("Writing block: " + (count++));
                    DBMRBlock b = (DBMRBlock) i.next();
                    if ( useAnnotation ) {
                        String annotation = b.getAnnotationString();
                        out.write(annotation);
                    }
                    String block = b.toString();
                    out.write(block);
                }
            }
            out.close();
            fw.close(); // redundant statements follow perhaps
            /**
            General.sleep(1000);
            out = null;
            fw = null;
            General.doFullestGarbageCollection();
             */
        } catch (IOException e) {
            General.showThrowable(e);
            General.showError("in DBMRFile.writeToFile found:");
            General.showError("writing file: "+file.getPath());
            return false;
        }
        return true;
    }


    /** Write individual blocks of text to separate files. The file name will
     *be appended with a number indicating the position [0-number of blocks-1].
     * @return <CODE>true</CODE> indicates success.
     * @param force_delete If true then any existing files will be
     * overwritten.
     * @param useLines If true is specified then the content of the lines will be written.
     * Otherwise the contents of the blocks will be written. In the first case
     * the concept of the blocks is lost.
     */
    public boolean writeBlocksToFiles( boolean useLines, boolean force_delete )
    {
        if ( file==null ) {
            General.showError("in DBMRFile.writeBlocksToFiles found:");
            General.showError("No file object present");
            return false;
        }

        if ( file.exists() ) {
            General.showWarning("deleting original file: "+file.getPath());
            if ( force_delete ) {
                file.delete();
            } else {
                if ( ! deleteFileFromDiskConfirmed() ) {
                    General.showError("in DBMRFile.writeBlocksToFiles found:");
                    General.showError("error writing file: "+file.getPath());
                    General.showError("because old file failed to be deleted");
                    return false;
                }
            }
        }

        General.showOutput("writing file: " + file.getPath());
        try {
            BufferedWriter out = new BufferedWriter( new FileWriter(
                file.getPath()));
            if ( useLines ) {
                for (Iterator i=lines.iterator();i.hasNext();) {
                    // End of line was deleted when reading so the Unix eol is
                    // assumed and added here.
                    out.write(i.next().toString()+General.eol);
                }
            } else {
//                int count=0;
                for (Iterator i=blocks.iterator();i.hasNext();) {
                    //General.showOutput("writing block: " + (count++));
                    DBMRBlock b = (DBMRBlock) i.next();
                    String annotation = b.getAnnotationString();
                    String block = b.toString();
                    out.write(annotation);
                    out.write(block);
                }
            }
            out.close();
        } catch (IOException e) {
            General.showThrowable(e);
            General.showError("in DBMRFile.writeBlocksToFiles found:");
            General.showError("writing file: "+file.getPath());
            return false;
        }

        return true;
    }


    /**
     * Only delete a file when confirmed
     * Return status denotes successful deletion of file
     * Will be false if user denies to delete it or some error occurred
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean deleteFileFromDiskConfirmed()
    {
        boolean status=false;
        if ( Strings.getInputBoolean("Delete file: " + file.getPath()) ) {
            General.showOutput("Deleting file: " + file.getPath());
            status = file.delete();
        }
        return( status );
    }


    /**
     * Delete a file unconfirmed.
     * Return status denotes successful deletion of file
     * Will be false if some error occurred...
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean deleteFileFromDisk()
    {
        if ( file == null ) {
            General.showError("The file object is null. File not deleted.");
            return false;
        }

        /**
        File tmp_file = new File( file.getPath() );

        // Give up all file handles and collect them.
        file = null;
	Runtime.getRuntime().gc(); // force garbage collection.
        */
        if ( ! file.exists() ) {
            General.showError("in DBMRFile.deleteFileFromDisk");
            General.showError("The file didn't exist.");
            General.showError("file name is: " + file.getPath());
            return false;
        }

        if ( ! file.delete() ) {
            General.showError("in DBMRFile.deleteFileFromDisk");
            General.showError("failed to delete mrfile from in progress dir.");
            General.showError("If on Windows it is quite possible other applications have");
            General.showError("a lock on it and Windows doesn't let you delete it.");
            General.showError("Solution: close any suspect applications and delete by hand.");
            return false;
        }

        return true;
    }


    /** Edit the file with our favorite text editor.
     * @param g Global setting for e.g. the preferred editor.
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean edit( Globals g ) {
        String cmd = g.getValueString("editor");
        /** Hard to give position in system independent way
            // Used to be 110, right?
            String option = "-geometry 80x50-0+0";
            String osName = System.getProperty("os.name" );
            if ( osName.equals("Windows NT") || osName.equals("Windows 98")
                || osName.equals("Windows 2000") ) {
                option = "";
            }
         */
        cmd = cmd + " " + file.getPath();

        int status = OSExec.exec(cmd);
        if ( status == 0 )
            return true;
        else
            return false;
    }

    /** Check if the file is <B>ok</B> to enter the annotated directory.
     * Returns <CODE>true</CODE> only if it is.
     * This method will in turn call several checking methods,
     * some of which might flunk the overall tests whereas other will
     * merely give warnings to the user, allowing for correction/ignore them.
     * @param check_list Selects by id which checks will be performed.
     * @param classification Contains the allowed block types.
     * @param g Contains info to be passed on for example for the preferred editor.
     * @return <CODE>true</CODE> indicates success.
     */
    public boolean check(int check_list[],
        Classification classification, Globals g )
    {
        int     errors_total      = 0;
        int     warnings_total    = 0;
//        int     errors;
//        int     warnings;
        /** The first element contains the number of errors and the second
         *contains the number of warnings produced by the various checks.
         *At this moment the warnings are not really used.
         */
        int[]   status = {0, 0};
        int     check_id;

        if ( check_list.length == 0 ) {
            General.showWarning("check got empty check list");
            return true;
        }

        for ( int i=0;i<check_list.length;i++ ) {
            check_id = check_list[i];
            switch(check_id)
            {
                case 1:
                    status = checkBlockCount();
                    errors_total    += status[0];
                    warnings_total  += status[1];
                    break;
                case 2:
                    status = checkBlockTypes(classification);
                    errors_total    += status[0];
                    warnings_total  += status[1];
                    break;
                case 3:
                    status = checkBlockEmptiness();
                    errors_total    += status[0];
                    warnings_total  += status[1];
                    break;
                case 4:
                    status = checkDiff(g);
                    errors_total    += status[0];
                    warnings_total  += status[1];
                    break;
                default:
                    General.showError("in DBMRFile.check found:");
                    General.showError("no check with id: " + check_id);
                    break;
            }
        }

        if (errors_total!=0) {
            General.showError("Found one or more errors by checking.");
            return false;
        }
        return true;
    }


    /** Modify the composite type according to the classification map given.
     *For most types no change might be required. The return value is the number of
     *changes or -1 for an error.
     */
    public int reclassify( HashMap class_map )
    {
        date_modified = new Date();

        int count = 0;
        int changed = 0;

        try {
        for ( Iterator i=blocks.iterator();i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            /*
            General.showOutput("Block " + mrb.getType());
            General.showOutput("Checking block [" + count +
                "] starting near line [" + startOfBlock(count) + "]");
             */
            Set keys = class_map.keySet();

            for ( Iterator k=keys.iterator();k.hasNext(); ) {
                String[] type = (String[]) k.next();
                //General.showOutput("Checking against type: " + type);
                if ( mrb.hasBlockType(type) ) {
                    String[] new_type = (String[]) class_map.get(type);
                    //General.showOutput("Reclassifing from: " + mrb.getType());
                    mrb.setTypeWithoutNulls(new_type);
                    //General.showOutput("               to:   " + mrb.getType());
                    changed++;
                }
            }
            count++;
        }
        } catch ( Exception e ) {
            General.showError("Caught an exception when trying to do the reclassification");
            e.printStackTrace(System.err);
            return -1;
        }
        General.showOutput("^^^^^^^^Changed:"+changed);
        return changed;
    }


    /** Check if the block types are of legal combinations of the 4 levels as
     * taken from a cvs file with the definitions.
     *In addition, it also checks if the properties are reasonable.
     * @param classification Contains the allowed block types.
     * @return Array of the number of errors and warnings.
     * @see Classification
     */
    public int[] checkBlockTypes( Classification classification )
    {
        int[]   status = {0, 0};
        // Block numbers start at 0
        int     count  = 0;
        General.showDebug("Check: checkBlockTypes");

        for ( Iterator i=blocks.iterator();i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            /*
            General.showOutput("Block " + mrb.getType());
            General.showOutput("Checking block [" + count +
                "] starting near line [" + startOfBlock(count) + "]");
             */
            if ( ! mrb.hasValidBlockType(classification) ) {
                status[0]++;
                General.showError("wrong type for block [" + count +
                    "] starting near line [" + startOfBlock(count) + "]");
                General.showError("type found: " + mrb.getType());
            }
            if ( ! mrb.hasValidBlockOtherProp() ) {
                status[0]++;
                General.showError("wrong other properties for block [" + count +
                    "] starting near line [" + startOfBlock(count) + "]");
                General.showError("prop found: " + mrb.other_prop.toString());
            }
            count++;
        }
        if ( status[0] != 0 ) {
            General.showError("in DBMRFile.checkBlockTypes found:");
            General.showError("number of blocks invalid: " + status[0]);
        }
        return(status);
    }

    /** Check if there is at least 1 block and no more than 100 blocks. These
     * default numbers can be changed in the code.
     * @return Array of the number of errors and warnings.
     * see NUMBER_BLOCKS_MIN
     * see NUMBER_BLOCKS_MAX
     */
    public int[] checkBlockCount()
    {
        int[]   status = {0, 0};
        // Block numbers start at 0
        General.showDebug("Check: checkBlockCount");

        if ( blocks.size() < NUMBER_BLOCKS_MIN ) {
            status[0]++;
            General.showError("in DBMRFile.checkBlockCount found:");
            General.showError("Number of blocks found: " + blocks.size());
            General.showError("Expected a positive number of blocks");
        }
        if ( blocks.size() > NUMBER_BLOCKS_MAX ) {
            status[0]++;
            General.showError("in DBMRFile.checkBlockCount found:");
            General.showError("Number of blocks found: " + blocks.size());
            General.showError("Expected a number of blocks smaller than:"+
                NUMBER_BLOCKS_MAX );
        }
        return(status);
    }


    /** Checks if there are only changes w.r.t. original file that are all allowed.
     * Compares two MR files and return true if only changes have been made
     * that are consistent with the rules for annotation:
     * <UL>
     * <LI>No deletions from original
     * <LI>Only additions start with PREFIX string (##### BMRB adds: --------)
     * </UL>
     * This function will use the <CODE>Diff</CODE> class for thanks to
     * Stuart D. Gathman who translated the GNU diff 1.15 algorithm. His code is
     * incorporated in the
     * {@link Wattos.Utils.Diff} and
     * {@link Wattos.Utils.DiffPrint} classes and has been customized in
     * the class: <CODE>MRAnnoDiff</CODE>.
     * @param g Used to get to the original MR file's location.
     * @return Array of the number of errors and warnings.
     */
    public int[] checkDiff(Globals g)
    {
        String[] a;
        String[] b;
        int[]   status = {0, 0};
        General.showDebug("Check: checkDiff");

        // Use the pdb_id and globals to get the original filename
        String chars2And3 = pdb_id.substring(1,3);
        String file_org_nameZ = g.getValueString("mr_dir") +
            File.separator +
            chars2And3 +
            File.separator +
            pdb_id + ".mr.gz";
        String text = InOut.readTextFromUrl( InOut.getUrlFileFromName(file_org_nameZ));
        if ( text == null ) {
            General.showError("in DBMRFile.checkDiff failed to uncompress file: " + file_org_nameZ);
            status[0]++;
            return status;
        }

        // Just read them again...
        try {
            a = DiffPrint.slurpString(text);
            b = DiffPrint.slurp(file.getPath());
        } catch ( IOException e ) {
            General.showError("in DBMRFile.checkDiff found:");
            General.showError("Reading (one of) the two files:\n" +
                    file_org_nameZ + " (original)\n"+file.getPath() + " (annotated) ");
            status[0]++;
            return status;
        }
        Diff d = new Diff(a,b);
        boolean edstyle = true;

        Diff.change script = d.diff_2(edstyle);
        // No changes at all is also not good.
        if (script == null) {
            General.showError("in DBMRFile.checkDiff found:");
            General.showError("No difference at all between the two files:\n" +
                    file_org_nameZ + " (original)\n"+file.getPath() + " (annotated) ");
            status[0]++;
            return(status);
        }

        MRAnnoDiff p = new MRAnnoDiff(a,b);
        // See code in DiffPrint.AnPrint
        if ( ! p.hasOnlyChangesAnnotationAllowed(script) ) {
            status[0]++;
        }

        return(status);
    }


    /** Checks if the block is empty. A block is considered empty if it contains
     * no more than white-space characters or no lines at all.
     * @return Array of the number of errors and warnings.
     */
    public int[] checkBlockEmptiness()
    {
        int[]   status = {0, 0};
        // Block numbers start at 0
        int     count  = 0;

        General.showDebug("Check: checkBlockEmptiness");
        for ( Iterator i=blocks.iterator();i.hasNext();) {
            //General.showOutput("Checking block [" + count +
            //    "] starting near line [" + startOfBlock(count) + "]");
            DBMRBlock mrb = (DBMRBlock) i.next();
            if ( mrb.isEmpty() ) {
                status[0]++;
                General.showError("in DBMRFile.checkBlockEmptiness found:");
                General.showError("only whitespace in block [" + count +
                    "] starting near line [" + startOfBlock(count) + "]");
            }
            count++;
        }
        if ( status[0] != 0 ) {
            General.showError("in DBMRFile.checkBlockEmptiness found:");
            General.showError("number of blocks empty: " + status[0]);
        }
        return(status);
    }


    /** Returns the line number where the block approximately starts.
     * The algorithm assumes that the definitions are each 4 lines which is not
     * required for a valid annotation but will usually be correct.
     * @param block_id The block to be examined.
     * @return The line number where the block approximately starts.
     */
    public int startOfBlock( int block_id )
    {
        return -1;
    }
    /**
        // Line numbers start at 1
        int line_count = 1;
        int j=0;
        Iterator i=blocks.iterator();
        /** Count the number of lines occupied by the previous blocks if any
         *
        while ( j < block_id ) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            line_count += mrb.lines.size() + 4;
            j++;
        }
        return(line_count);
    }
     */


    /** see doc on doConversions. This method can be called with individual
     * blocks or for all blocks together when the parameter pos is BLOCK_ID_INDICATING_ALL.
     * Generates the text of a star file which gets put into this class as the
     * first and only block.
     * @param classification As usual
     * @param pos Position of the block or an indication sentinel for all blocks.
     * @param star_version As usual
     * @return An converted file with one or more blocks. At least 1.
     * If nothing was converted, it will return null.
     */
    public DBMRFile doConversion( Classification classification, int pos, int star_version ) {

        MRSTARFile mrstrfile = new MRSTARFile();
        boolean status = mrstrfile.init( this, classification, pos, star_version);
        if ( ! status ) {
            //General.showDebug(" Skipping conversion of block: " + pos );
            return null;
        }

        DBMRBlock mrb_new = new DBMRBlock();
        if ( pos != BLOCK_ID_INDICATING_ALL ) {
            DBMRBlock mrb = (DBMRBlock) blocks.get( pos );
            mrb_new.setType( (String[]) mrb.type );
        } else {
            mrb_new.setType( DBMRBlock.FULL_ENTRY_TYPES );
        }
        mrb_new.item_count = mrstrfile.getItemCount();
        if ( Defs.isNull( mrb_new.item_count ) ) {
            General.showWarning("Failed to count items in NMR-STAR object; setting count to database null");
            //mrb_new.item_count = Defs.NULL_INT; already done in routine: mrstrfile.getItemCount();
            // just checked it ;-)
        }

        mrb_new.text_type = SQL_Episode_II.BLOCK_DETAIL_PARSED;
        mrb_new.type[DBMRBlock.PROGRAM_ID] = "STAR";
        mrb_new.setStrings( mrstrfile.toString() );
//        if ( file == null ) {
//            General.showWarning("(This used to be an error but down-graded) File object file is null in DBMRFile.doConversion");
////            return null;
//        }
        if (Defs.isNull(mrb_new.mrblock_id)) {
            mrb_new.fileName = "star.str";
        } else {
            mrb_new.fileName = pdb_id + ".str";
        }

        DBMRFile mrf_new = new DBMRFile();
        mrf_new.pdb_id = pdb_id;
        mrf_new.detail = SQL_Episode_II.FILE_DETAIL_PARSED; // to be removed.
        mrf_new.blocks.add( mrb_new );
        return mrf_new;
    }


    /** Routine assumes the data is already in blocks. It returns a list of
     *converted files as DBMRFile objects or null in case of an error.
     */
    public ArrayList doConversions( Classification classification, int star_version ) {

//        boolean status;
        // Do blocks one by one if a converter is available
        ArrayList files_converted = new ArrayList();

        // Do seperate blocks.
        // Use the position argument to indicate only 1 block needs to be translated.
        int pos = 0;
        for ( Iterator i= blocks.iterator(); i.hasNext(); ) {
            i.next();
            DBMRBlock mrb = blocks.get(pos);
            General.showOutput("Trying to convert block: " + pos + " of type: " + mrb.getBlockType());
            DBMRFile mrf_new = doConversion( classification, pos, star_version );
            if ( mrf_new != null ) {
                //General.showDebug("Conversion done for block: " + pos );
                files_converted.add(mrf_new);
            }
            pos++;
        }

        // Repeat the conversions and put the results for all blocks together
        // The BLOCK_ID_INDICATING_ALL value for the position will trigger this. I know this will
        // double the time needed for the conversions. Speed up in future.
        DBMRFile mrf_new = doConversion( classification, BLOCK_ID_INDICATING_ALL, star_version );
        if ( mrf_new != null ) {
            files_converted.add(mrf_new);
        } else {
            General.showError("failed to convert to a single STAR file for all blocks");
        }
        // Later on the converted/not converted blocks will be glued together
        // and the resulting 1 file be appended to the returned list still.
        //General.showDebug("converted blocks count: " + files_converted.size() );

        return files_converted;
    }


    /** Puts a list of files into the database.
     */
    public static boolean putFilesInDatabase( Classification classification,
        ArrayList files, SQL_Episode_II sql_epiII ) {

        // Write the individual converted files to the database.
        boolean status = true;
        int index = 0;
        for ( Iterator i=files.iterator(); i.hasNext();) {
            index++;
            DBMRFile mrf_new = (DBMRFile) i.next();
            status = mrf_new.writeToDB(sql_epiII);
            if ( ! status ) {
                General.showError("(putFilesInDatabase) Skipping after error with file of index # " + index + ".");
                break;
            }
        }
        return status;
    }


    /** Self test; no non-interactive tests available.
     * Great for checking method in interactive way. Just enable the test given.
     * @param args Ignored.
     */
    public static void main (String[] args)
    {
        // test of most sub routines
        General.verbosity = General.verbosityDebug;
        General.showOutput("Starting test of check routines." );
        Globals g = new Globals();

        // Open Episode_II database connection
        Properties db_properties = new Properties();
        db_properties.setProperty( "db_conn_string",g.getValueString( "db_conn_string" ));
        db_properties.setProperty( "db_username",   g.getValueString( "db_username" ));
        db_properties.setProperty( "db_driver",     g.getValueString( "db_driver" ));
        db_properties.setProperty( "db_password",   g.getValueString( "db_password" ));
//        SQL_Episode_II sql_epiII = new SQL_Episode_II( db_properties );

        // Classifications that are possible
        Classification classi = new Classification();
        if (!classi.readFromCsvFile("Data/classification.csv")){
            General.showError("in DBMRFile.main found:");
            General.showError("reading classification file.");
            System.exit(1);
        }

//        String pdb_code = "1cou";
//        boolean status;

        if ( true ) {

            DBMRFile mrf = new DBMRFile();
            mrf.pdb_id = "1ba5";

            DBMRBlock mrb = new DBMRBlock();
            mrb.setType( new String[] {"n/a", "comment", "n/a", "n/a"} );
            ArrayList lines = new ArrayList();
            lines.add( "Test" );
            mrb.fillContent( lines );
            General.showOutput("MRB looks like: " + mrb.toString() );
            mrf.blocks.add( mrb );
            mrb = new DBMRBlock();
            mrb.setType( new String[] {"EMBOSS", "distance", "NOE", "simple"} );
            lines = new ArrayList();
            lines.add( ";HA(I)-HN(I+1)");
            lines.add( "1    4   GLN   HA      1   5   ALA   HN   1.0  1.0  -2.0  5.0" );
            mrb.fillContent( lines );
            mrf.blocks.add( mrb );
            int[] check_list = {1,2,3};
            mrf.check(check_list, classi, g);
            General.showOutput("done with checks in test routine");
            ArrayList al = mrf.doConversions(classi, NmrStar.STAR_VERSION_DEFAULT);
            General.showOutput("al looks like: [" + al + "]" );
            General.showOutput("al2 looks like: [" + ((DBMRFile) al.get(2)).blocks.get(0) + "]" );
        }

//        if ( false ) {
//             String file_name = "I:\\pdbmirror2\\mr_anno_progress\\1kx7.mr";
//             DBMRFile mrfile = new DBMRFile(file_name);
//             General.showOutput("Name: " + mrfile);
//             if ( mrfile.deleteFileFromDisk() ) {
//                 General.showOutput("Deleted: " + file_name);
//             } else {
//                 General.showOutput("NOT Deleted: " + file_name);
//             }
//        }
//        if ( false ) {
//             String dir = "S:\\wattos\\mr_anno_test";
//             String file_name     = "1cou.mr";
//             String file_name_new = "1cou_new.mr";
//	     //FileCopy.copy(new File(file_name), new File(file_name_new),true, false);
//             /** Get the info back from the DB
//              */
//             //General.showOutput("Copied to file: " + file_name_new);
//             DBMRFile mrf = new DBMRFile(file_name);
//             mrf.readFromFile(new File(dir));
//             if ( true ) {
//                int[] check_list = {1,2,3,4};
//                mrf.check(check_list, classi, g);
//             }
//             mrf.file = new File( file_name_new );
//             mrf.writeToFile(false, true);
//
//             General.showOutput("Removing mrf: " + mrf);
//             //mrf.deleteFileFromDisk();
//             mrf.writeToDB( sql_epiII );
//
//             DBMRFile mrf_new = new DBMRFile();
//             mrf_new.mrfile_id = mrf.mrfile_id;
//            // Get the info back from the DB
//            status = mrf_new.readFromDB(sql_epiII, mrf_new.mrfile_id );
//            if ( status ) {
//                General.showOutput("Read from DB successful" );
//            } else {
//                General.showError("in DBMRFile.main found:");
//                General.showError(
//                    "ERROR: Read from DB was not successful" +
//                    "for mrfile_id:" + mrf_new.mrfile_id);
//            }
//
//            boolean useLines = false;
//            mrf_new.file = new File( file_name_new );
//            mrf_new.writeToFile( useLines, true );
//        }
//         if ( false ) {
//            // Use default detail id.
//            String detail = "default";
//            ArrayList mrfile_ids = sql_epiII.getMRFileIdsByPDBIdByDetail(pdb_code, detail );
//            if ( mrfile_ids.size() < 1 ) {
//                General.showError("in DBMRFile.main found:");
//                General.showError("Insert in DB not successful" );
//                System.exit(1);
//            }
//            if ( mrfile_ids.size() > 1 ) {
//                General.showWarning("Getting first mrfile only" );
//            }
//            General.showOutput("Got the following MRFileIds: " + mrfile_ids);
//            General.showOutput("Using the first.");
//            int mrfile_id = ((Integer) mrfile_ids.get(0)).intValue();
//            DBMRFile mrf_new = new DBMRFile();
//            mrf_new.mrfile_id = mrfile_id;
//            sql_epiII.getMRFile( mrf_new );
//        }
       General.showOutput("Finished all selected check routines." );
    }
}
