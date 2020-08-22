package Wattos.Utils;

// This example is from _Java Examples in a Nutshell_. (http://www.oreilly.com)
// Copyright (c) 1997 by David Flanagan
// This example is provided WITHOUT ANY WARRANTY either expressed or implied.
// You may study, use, modify, and distribute it for non-commercial purposes.
// For any commercial use, see http://www.davidflanagan.com/javaexamples
/**
 *Modified the code to allow for unasked overwriting of files.
 * @author David Flanagan
 * @author Jurgen F. Doreleijers
 */
import java.io.*;

/**
 * This class is a standalone program to copy a file, and also defines a
 * static copy() method that other programs can use to copy files.
 **/
public class FileCopy {
    /** The main() method of the standalone program.  Calls copy(). */
    public static void main(String[] args) {
        if (args.length != 2)    // Check arguments
            General.showError( "Usage: java Wattos.Utils.FileCopy <source file> <destination>");
        else {
            // Call copy() to do the copy, and display any error messages it throws.
            File from_file = new File(args[0]);  // Get File objects from Strings
            File to_file = new File(args[1]);
            if ( ! copy( from_file, to_file, false, true ) ) {
                General.showError("Failed to copy file from: " + from_file + " to: " + to_file);
            } else {
                General.showOutput("Copied file from: " + from_file + " to: " + to_file);
            }
        }
    }
    
    /**
     * The static method that actually performs the file copy.
     * Before copying the file, however, it performs a lot of tests to make
     * sure everything is as it should be. If force is true and the output file
     * already exists then if the interactive bit is set the user will be prompted
     * for overwrite.
     */
    public static boolean copy(File from_file, File to_file, boolean force, boolean interactive) {

        if ( to_file.isDirectory() ) {
            //General.showDebug("Trying copy from file: " + from_file + " to directory: " + to_file);
        } else {
            //General.showDebug("Trying copy from file: " + from_file + " to file: " + to_file);
        }        
        // First make sure the source file exists, is a file, and is readable.
        if (!from_file.exists()) {
            General.showError("FileCopy: no such source file: " + from_file);
            return false;
        }
        if (!from_file.isFile()){
            General.showError("FileCopy: can't copy directory: " + from_file);
            return false;
        }
        if (!from_file.canRead()){
            General.showError("FileCopy: source file is unreadable: " + from_file);
            return false;
        }
                
        // If the destination is a directory, use the source file name
        // as the destination file name
        if (to_file.isDirectory())
            to_file = new File(to_file, from_file.getName());
        
        // If the destination exists, make sure it is a writeable file
        // and ask before overwriting it.  If the destination doesn't
        // exist, make sure the directory exists and is writeable.
        if (to_file.exists()) {
            if ( ! force ) {
                //General.showOutput("FileCopy: skipping file already present: " + to_file);
                return true;
            }
            if (!to_file.canWrite()){
                General.showError("FileCopy: destination file is unwriteable: " + from_file);
                return false;
            }
            if ( interactive ) {
                // Ask whether to overwrite it
                // Get the user's response.
                boolean answer = Strings.getInputBoolean("Overwrite existing file " + to_file + "? (Y/N): ");
                // Check the response.  If not a Yes, abort the copy.
                if (answer) {
                    General.showError("FileCopy: existing file was not overwritten.");
                    return false;
                }
            }
        }
        else {
            // if file doesn't exist, check if directory exists and is writeable.
            // If getParent() returns null, then the directory is the current dir.
            // so look up the user.dir system property to find out what that is.
            String parent = to_file.getParent();  // Get the destination directory
            if (parent == null) parent = System.getProperty("user.dir"); // or CWD
            File dir = new File(parent);          // Convert it to a file.
            if (!dir.exists()){
                General.showError("FileCopy: destination directory doesn't exist: " + parent);
                return false;
            }
            
            if (dir.isFile()){
                General.showError("FileCopy: destination is not a directory (but a file): " + parent);
                return false;
            }
            
            if (!dir.canWrite()){
                General.showError("FileCopy: destination directory is unwriteable: " + parent);
                return false;
            }
        }
        
        /** JFD adds:*/
        try {        
            from_file = from_file.getCanonicalFile();
            to_file = to_file.getCanonicalFile();
            if ( from_file.toString().equals( to_file.toString() ) ) {
                General.showError("FileCopy: input is the same as output file: " + from_file);
                return false;
            }
        } catch ( IOException t ) {
            General.showThrowable(t);
            return false;
        }
        
        // If we've gotten this far, then everything is okay.
        // So we copy the file, a buffer of bytes at a time.
        FileInputStream from = null;  // Stream to read from source
        FileOutputStream to = null;   // Stream to write to destination
        try {
            from = new FileInputStream(from_file);  // Create input stream
            to = new FileOutputStream(to_file);     // Create output stream
            byte[] buffer = new byte[4096];         // A buffer to hold file contents
            int bytes_read;                         // How many bytes in buffer
            // Read a chunk of bytes into the buffer, then write them out,
            // looping until we reach the end of the file (when read() returns -1).
            // Note the combination of assignment and comparison in this while
            // loop.  This is a common I/O programming idiom.
            while((bytes_read = from.read(buffer)) != -1) { // Read bytes until EOF
                to.write(buffer, 0, bytes_read);            //   write bytes
            }
            from.close(); // next statements might be redundant.
            to.close();
            from = null;
            to = null;            
        } catch ( FileNotFoundException t ) {
            General.showThrowable(t);
            return false;
        } catch ( IOException t ) {
            General.showThrowable(t);
            return false;
        }
        // Always close the streams, even if exceptions were thrown
        finally {
            if (from != null) try { from.close(); } catch (IOException e) { General.showThrowable(e); }
            if (to   != null) try { to.close();   } catch (IOException e) { General.showThrowable(e); }
        }
        return true;
    }
}
