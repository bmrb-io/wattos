/*
 * DBFS.java
 *
 * Created on May 26, 2004, 3:38 PM
 */

package Wattos.Database;

import com.braju.format.*;              // printf equivalent
import cern.colt.list.*;
import java.io.*;
import java.util.*;

import Wattos.Episode_II.*;
import Wattos.Utils.*;

/**
 *This class encodes how files in a separate filesystem are stored.
 *The algorithm is simple and extendable to large volumes of data. Each file has
 *a number and a file extension. The number can be a full positive integer, so up
 *to 2 billion something. Assuming a directory structure is efficient up to when about 10^3
 *elements (files or directories) exist in it, the following scheme is used:<BR>
 *Translate the file name to the nearest 3, 6, or 9 digit number with zeros prepended:
 *<PRE>
 *E.g. file: 1.txt                      ->                   001.dat  -> /001.dat
 *           12345.xml.gz               ->                012345.dat  -> /012/012345.dat
 *           123456789.xml.gz           ->             123456789.dat  -> /123/456/123456789.dat
 *</PRE>
 *And so on if ints could store more than 9 digits. The last item shows into which directory
 *the file would end up. If the directory doesn't exist it will be created. When the file is deleted as
 *the last file in the directory the directory itself needs to be deleted to keep the tree optimal.
 *
 *This way not too many directories get created: 10^6 for 10^9 files and they only get created if/when
 *needed. E.g. if the number of files is < 10^6 only 10^3 directories get created.
 *In one directory there will only be 10^3 files and 10^3 directories max. which is deemed optimal.
 *The method doesn't allow data to be spread out over multiple partitions but logical partitions
 *can be made very large nowadays.
 *
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class DBFSFile {

    /** Maximum number of digits that make up the maximum number of
     *files in the directory. Don't change this value unless you reinstall the database.*/
    private static final int NUMBER_DIGITS_PER_DIRECTORY = 3;

    /** Maximum number of files per directory which is 10**NUMBER_DIGITS_PER_DIRECTORY */
    private static final int MAX_FILES_PER_DIRECTORY = (int) Math.pow(10,NUMBER_DIGITS_PER_DIRECTORY);

    /** The root of the dbfs change if a different range of ids needs to be used
     */
    private String dbfsRoot;

    /** The actual file object which can't be modified directly
     */
    private File file;
    /** Number for the file unique across the whole database, e.g. not just for a
     *specific table or column unless the dbfsRoot is adjusted
     */
    private int fileId;
    public static Globals g = new Globals(); // Too expensive to create multiple copies.
    /** Using the parameter below will result in slightly more efficient dir. structure
     *at the cost of having to check for it all the time
     */
    private static boolean ENSURE_MINIMUM_NUMBER_DIRS = false;

    /** Creates a new instance of DBFS */
    public DBFSFile(int fileId ) {
        this.fileId = fileId;
        init();
    }


    public boolean init() {
        dbfsRoot = g.getValueString("dbfs_dir");
        String fullFileName = getFullFileName();
        if ( fullFileName == null ) {
            General.showError("Failed to form a valid full file name from id: " + fileId );
            return false;
        }
        file = new File(fullFileName);
        /**
        General.showDebug("Number of files per directory is     : " + MAX_FILES_PER_DIRECTORY);
        General.showDebug("Number of digits per directory is    : " + NUMBER_DIGITS_PER_DIRECTORY);
         */
        return true;
    }

    /** Returns a sorted list of all dbfs ids that exist in the filesystem.
     *The list is compiled by doing a recursive directory listing.
     */
    public static IntArrayList getFiles_in_fs() {
        IntArrayList result = new IntArrayList();
        File dir = new File( g.getValueString("dbfs_dir"));
        ArrayList files = InOut.getFilesRecursive(dir, false);
        if ( files == null ) {
            General.showError("Failed to InOut.getFilesRecursive from dir: " + dir);
            return null;
        }
        // Now convert the full file names to dbfs ids
        try {
            for (int i=0;i<files.size();i++) {
                File f = (File) files.get(i);
                if ( ! InOut.getFilenameExtension(f).equals("dat")) {
                    General.showWarning("Skipping file without dat extension: ["+ f.getPath()+"]");
                    continue;
                }
                result.add( Integer.parseInt( InOut.getFilenameBase(f) ));
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        }
        result.sort();
        return result;
    }


    /** simply removes any and all empty sub dir.
     *Returns true if all are analyzed and deleted if applicable.
     */
    public boolean cleanEmptyDirs() {
        return InOut.removeEmptySubDirs( new File(dbfsRoot) );
    }


    /** Returns the full file name with dbfs root and possible subdirectories prepended.
     */
    public String getFullFileName() {
        if ( fileId < 0 ) {
            General.showError("Failed to format a full file name for negative id");
            return null;
        }

        Parameters p = new Parameters(); // Printf parameters
        StringBuffer sb = new StringBuffer( );
        int fileIdLeft = fileId;
        int digitCount = NUMBER_DIGITS_PER_DIRECTORY;

        // Get the path
        while ( fileIdLeft >= MAX_FILES_PER_DIRECTORY ) { // Need to insert a subdir.
            fileIdLeft /= MAX_FILES_PER_DIRECTORY;
            p.add( fileIdLeft % MAX_FILES_PER_DIRECTORY );
            sb.insert( 0, Format.sprintf("%0"+NUMBER_DIGITS_PER_DIRECTORY+"d", p)+File.separator); // could be cached for speed.
            digitCount += NUMBER_DIGITS_PER_DIRECTORY;
        }
        sb.insert(0, dbfsRoot + File.separator );

        // Get the file name.
        p.add( fileId );
        sb.append( Format.sprintf("%0"+digitCount+"d", p) );
        sb.append( ".dat" );
        return sb.toString();
    }

    /** returns true if containing dir already existed or if it could be
     *created including any needed additional parents.
     */
    public boolean createContainingDirIfNeeded() {
        File dir = file.getParentFile();
        if ( dir.exists() ) {
            //General.showDebug("Dir already existed: " + dir );
            return true;
        }
        return dir.mkdirs();
    }

    /** Returns true if dir already didn't exist or if it could be removed.
     *Needs only to be called by the delete method.
     */
    private boolean deleteContainingDirIfNeeded() {
        File dir = file.getParentFile();
        if ( ! dir.exists() ) {
            General.showWarning("Dir didn't exist: " + dir );
            return true;
        }

        if ( ! dir.isDirectory() ) {
            General.showError("Parent file isn't a dir: " + dir );
            return false;
        }
        String[] list = dir.list();
        if ( list == null ) {
            General.showError("Failed to get a dir listing for dir: " + dir );
            General.showError("The listing is needed because if the dir isn't empty it can't and shouldn't be deleted");
            return false;
        }
        if ( list.length != 0 ) {
            return dir.delete();
        }
        return true;
    }


    /** Removes a bunch of them
     */
    public static boolean delete( int[] dbfsList ) {
        for (int i=0;i<dbfsList.length;i++) {
            DBFSFile f = new DBFSFile( dbfsList[i]);
            if ( ! f.delete()) {
                General.showError("Failed to delete dbfs file: " + f);
                General.showError("Giving up others." );
                return false;
            }
        }
        return true;
    }


    /** Deletes
     *the file and depending on the settings (default false) also the directory if it is empty afterwards.
     *Returns true only if successfull
     */
    public boolean delete() {
        if ( ! file.delete() ){
            General.showError("Failed to delete DBFSFile: " + file.toString());
            return false;
        }
        if ( ENSURE_MINIMUM_NUMBER_DIRS ) {
            if ( ! deleteContainingDirIfNeeded()) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return file.toString();
    }

    /**
     * Getter for property fileId.
     * @return Value of property fileId.
     */
    public int getFileId() {
        return fileId;
    }

    /**
     * Setter for property fileId.
     * @param fileId New value of property fileId.
     */
    public void setFileId(int fileId) {
        this.fileId = fileId;
        init();
    }

    /**
     * Getter for property dbfsRoot.
     * @return Value of property dbfsRoot.
     */
    public String getDbfsRoot() {
        return dbfsRoot;
    }

    /**
     * Setter for property dbfsRoot.
     * @param dbfsRoot New value of property dbfsRoot.
     */
    public void setDbfsRoot(String dbfsRoot) {
        this.dbfsRoot = dbfsRoot;
        init();
    }

    /**
     * Getter for property file.
     * @return Value of property file.
     */
    public java.io.File getFile() {
        return file;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        General.showOutput("Doing tests");
        //int fileId = 12345678;
        int fileId = 100;

        DBFSFile dbfsFile = new DBFSFile(fileId);
        File from_file = new File( "M:\\bfiles\\README.txt" );

//        if ( false ) {
//            General.showError("Don't execute these tests unless you know what you're doing; perhaps you might destroy the archive.");
//        }
        // Create a new set of dbfs files by copying a certain file to it.
        if ( true ) {
            for (fileId=9;fileId<10;fileId++) {
//            for (fileId=1000*1000-2;fileId<1000*1000*1+100;fileId++) {
                dbfsFile = new DBFSFile( fileId);
                General.showOutput("File name is: " + dbfsFile );
                //createContainingDir(testFile);
                //General.showOutput("Creating dir if needed: " + createContainingDir(testFile));
                dbfsFile.createContainingDirIfNeeded();
                FileCopy.copy(from_file, dbfsFile.file, true, false);
                //General.showOutput("Copying a file to it: " + FileCopy.copy(from_file, testFile, true, false));
            }
        }

        // Delete a dbfs file and the directory it is in if needed. Making sure not to delete the
        // top directory.
//        if ( false ) {
//            for (fileId=0;fileId<2000;fileId++) {
//                dbfsFile = new DBFSFile( fileId);
//                General.showOutput("Removing file with name: " + dbfsFile );
//                dbfsFile.delete();
//            }
//        }
//
//        if ( false ) {
//            General.showOutput("Removing all empty (sub-)directories: " + dbfsFile.cleanEmptyDirs());
//        }
        General.showOutput("Done.");
    }
}
