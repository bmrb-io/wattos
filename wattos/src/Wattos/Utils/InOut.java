/*
 * Utils.java
 *
 * Created on December 6, 2001, 11:37 AM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Utils;

import Wattos.Database.Defs;
import com.braju.format.*; // printf equivalent
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*; //import java.text.*;
import java.util.regex.*;

/**
 * I/O related methods including file copy, file filters, environment settings, conversions between writer/reader and
 * in/out streams etc.
 *
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class InOut {

    public static String END_OF_FILE_ENCOUNTERED = "END_OF_FILE_ENCOUNTERED";
    /**
     * Factor by which the expected number of bytes increases after decompression. It's kind of a guess for text files
     * like star/pdb formatted files. It may be used to back calculate the real size of a file.
     */
    public static final float DEFAULT_COMPRESSION_FACTOR = 5;
    /** Normally we assume the OS is linux */
    public static boolean isOSLinux = true;
    public static boolean isOSWindows = false;

    public static final ArrayList fileNameExtensionsText;
    /** Not used yet but opposite of above */
    public static final ArrayList fileNameExtensionsBina;

    static {
        fileNameExtensionsText = new ArrayList();
        fileNameExtensionsBina = new ArrayList();
        fileNameExtensionsText.addAll(Arrays.asList(new String[] { "aco", "awk", "bat", "c", "cc", "csh", "f", "html",
                "java", "lol", "mr", "mtf", "pdb", "perl", "py", "readme", "seq", "sh", "sql", "str", "star", "txt",
                "tbl", "upl", "xml" }));
        fileNameExtensionsBina.addAll(Arrays.asList(new String[] { "bin", "doc", "gz", "tar", "tgz", "zip", "Z" }));
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            isOSWindows = true;
        } else if (osName.startsWith("Linux") || osName.startsWith("SunOS") || osName.startsWith("Mac OS")) {
            isOSWindows = false;
        } else {
            /**
             * General.showOutput("Any Operating System that is not:\n"+ "- Windows\n"+ "- Linux or SunOS or Mac OS\n"+
             * "is currently not supported by Wattos.Utils.InOut.\n"+
             * "The OS of this machine is determined by Java as: \n" + osName); ;
             */
        }

    }

    /**
     * Allow instantiation but there are no non-static methods except those shared by all objects. Needed for method
     * getInstallationDir.
     */
    public InOut() {
    }

    /**
     *
     * @return true for error
     */
    public static boolean chmod(File f, String mod) {
        String cmd = "chmod " + mod + " " + f.toString();
        General.showDebug("Executing command: [" + cmd + "]");
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                General.showDebug(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Returns true if all filenames are of files/directories that already exist.
     */
    public static boolean filesExist(String[] file_names) {
        if (file_names == null) {
            General.showWarning("Filename list is: " + null);
            return false;
        }
        for (int i = 0; i < file_names.length; i++) {
            if (file_names[i] == null) {
                General.showWarning("Filename is: " + null);
                return false;
            }
            File f = new File(file_names[i]);
            if (!f.exists()) {
                General.showWarning("File/dir didn't exist: " + f);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the argument is a directory that could successfully be scanned for any empty sub dirs that would
     * then be deleted. Routine calls itself recursively.
     */
    public static boolean removeEmptySubDirs(File dir) {

        // General.showOutput("Testing recursive empty dir delete of: " + dir );

        if (!dir.exists()) {
            General.showWarning("Dir didn't exist: " + dir);
            return false;
        }

        if (!dir.isDirectory()) {
            General.showError("Dir given isn't a dir: " + dir);
            return false;
        }
        String[] list = dir.list();
        if (list == null) {
            General.showError("Failed to get a dir listing for dir: " + dir);
            General.showError("The listing is needed because if the dir isn't empty it can't and shouldn't be deleted");
            return false;
        }

        if (list.length == 0) {
            // General.showDebug("Found an empty list for files in this dir: " + dir );
            return true;
        }

        for (int fileId = 0; fileId < list.length; fileId++) {
            File potentialSubDir = new File(dir, list[fileId]);
            // General.showDebug("Looking at file or subdir: " + potentialSubDir);
            if (potentialSubDir.isDirectory()) {
                // General.showDebug("Doing recursive remove of empty subDirs in subDir: " + potentialSubDir );
                if (!removeEmptySubDirs(potentialSubDir)) {
                    General.showError("Failed doing recursive remove of empty subDirs in subDir: " + potentialSubDir);
                    return false;
                }
                if (isEmptyDir(potentialSubDir)) {
                    // General.showDebug("Doing remove of empty subDir: " + potentialSubDir );
                    boolean status = potentialSubDir.delete();
                    if (!status) {
                        if (potentialSubDir.exists()) {
                            General.showError("empty subDir could really not be deleted: " + potentialSubDir);
                            return false;
                        }
                        General
                                .showWarning("According to Java's File.delete() the empty subDir failed to be deleted but it was deleted... This might be related to a remote drive because it always works on a local drive.: "
                                        + potentialSubDir);
                    }
                }
            } else {
                // General.showDebug("Skipping non-directory: " + potentialSubDir);
            }
        }

        return true;
    }

    /**
     * returns true if file is a directory AND the dir doesn't contain other dirs or directories.
     */
    static boolean isEmptyDir(File dir) {
        if (!dir.exists()) {
            General.showWarning("Dir didn't exist: " + dir);
            return false;
        }

        if (!dir.isDirectory()) {
            return false;
        }
        String[] list = dir.list();
        if (list == null) {
            General.showError("Failed to get a dir listing for dir: " + dir);
            return false;
        }
        if (list.length == 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns a list of File objects with full path names of all the files (not directories) in the given dir. Will
     * call itself.
     * @param includeDirectories TODO
     */
    public static ArrayList getFilesRecursive(File dir, boolean includeDirectories) {
        if (!dir.isDirectory()) {
            General.showError("Given File instance isn't a directory.");
            return null;
        }
        ArrayList result = new ArrayList();
        String[] list = dir.list();
        for (int i = 0; i < list.length; i++) {
            File potFile = new File(dir, list[i]);
            if (potFile.isDirectory()) {
                ArrayList tempList = getFilesRecursive(potFile, includeDirectories);
                if (tempList == null) {
                    General.showError("Failed to get files recursively from dir: " + potFile);
                    return null;
                }
                result.addAll(tempList);
            }
            if (potFile.isFile()) {
                result.add(potFile);
            } else if ( includeDirectories ) {
                if ( potFile.isDirectory() ) {
                    result.add(potFile);
                }
            }
        }
        return result;
    }

    /**
     * Asks the user with a specific prompt (or null) to provide a valid file name. Files may be specified by url
     * notation: see <CODE>java.net.URL</CODE> or <scheme>://<authority><path>?<query>#<fragment> as in:
     * http://archive.ncsa.uiuc.edu:80/SDG/Software/Mosaic/Demo/url-primer.html?i=j#FAQ scheme http authority
     * archive.ncsa.uiuc.edu:80 path /SDG/Software/Mosaic/Demo/url-primer.html query i=j fragment FAQ
     *
     * or by pathname as in : /share/jurgen/handy.txt toURI : file:///share/jurgen/handy.txt or
     * file://S:\jurgen\handy.txt uri.toURL : same just recommended by API to go through URI for abstract pathnames.
     * abstract meaning without scheme (e.g. ftp).
     *
     *See method getUrlFileFromName to see how the canonicalization is done.
     *
     * @param prompt
     *            What should the user be asked for.
     * @return null in case of failure
     */
    public static URL getUrlFile(BufferedReader in, String prompt) {

        if (prompt == null) {
            prompt = "Enter a name (with path if outside cwd or url) of an existing file";
        }

        String tmpName = Strings.getInputString(in, prompt, null);
        if (tmpName == null) {
            General.showWarning("invalid input string: null");
            return null;
        }

        return getUrlFileFromName(tmpName);
    }

    /**
     * To be extended later with checks for like writability etc.
     */
    public static String getPossibleFileName(BufferedReader in, String prompt) {

        if (prompt == null) {
            prompt = "Enter a name (with path if outside cwd or url) of a file";
        }
        String tmpName = Strings.getInputString(in, prompt, null);
        if (tmpName == null) {
            General.showWarning("invalid input string: null");
            return null;
        }

        // File file = null;
        try {
            new File(tmpName);
        } catch (Exception e) {
            General.showError("Failed to get a valid file name from input: " + tmpName);
            General.showThrowable(e);
            return null;
        }
        return tmpName;
    }

    /**
     * Modifies a string to url assuming that a file is meant when no scheme is given. Scheme's allowed so far are file,
     * http, and ftp. Add others later. Returns null on error. Returns Defs.NULL_URL_DOT for a dot.
     */
    public static URL getUrlFileFromName(String fn) {

        URL url = null;
        if (fn == null) {
            General.showWarning("invalid input string: null");
            return null;
        }
        if (fn.equals(Defs.NULL_STRING_DOT)) {
            return Defs.NULL_URL_DOT;
        }

        /** Check to see if it's abstract and needs the scheme still. */
        try {
            if (!(fn.startsWith("file:") || fn.startsWith("http:") || fn.startsWith("ftp:"))) {
                // Assume it's a pathname without scheme that should be interpretted as a file name.
                File file = new File(fn);
                String fileNameCanonical = file.getCanonicalPath(); // this might throw exceptions
                file = new File(fileNameCanonical);
                // General.showDebug("Got file with canonical  name: " + file.getPath());
                URI uri = file.toURI(); // It's recommended to convert through URI now.
                // General.showDebug("Got uri with             name: " + uri);
                url = uri.toURL();
            } else {
                url = new URL(fn);
            }
        } catch (Throwable e) {
            General.showThrowable(e);
            General.showError("Failed to construct a URL from given string: " + fn);
            return null;
        }
        // General.showOutput("Got url with             name: " + url);
        return url;
    }

    /**
     * Convenience method; will overwrite original
     */
    public static PrintWriter getPrintWriter(String output_filename) {
        return getPrintWriter(output_filename, false);
    }

    /**
     * Only supports real files for the moment.
     */
    public static PrintWriter getPrintWriter(String output_filename, boolean append) {
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(output_filename, append)));
        } catch (Throwable t) {
            General.showThrowable(t);
            return null;
        }
        return printWriter;
    }

    /**
     * Returns null on any error. Materializes text several time so watch out for very large texts.
     */
    public static String readTextFromUrl(URL url) {
        try {
            BufferedInputStream bis = getBufferedInputStream(url);
            if (bis == null) {
                General.showError("Failed InOut.readTextFromUrl for: " + url.toString());
                return null;
            }
            return readTextFromInputStream(bis);
        } catch (Exception e) {
            General.showCodeBug(e.getMessage());
            General.showError("Failed to readTextFromUrl from URL: " + url);
            return null;
        }
    }

    /**
     * Returns null on any error. Materializes text several time so watch out for very large texts. Input will be
     * buffered wrapped by this method.
     */
    public static String readTextFromInputStream(InputStream is) {
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            if (bis == null) {
                General.showError("Failed InOut.readTextFromInputStream for: " + is.toString());
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            StringBuffer sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(General.eol);
                line = br.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            General.showCodeBug(e.getMessage());
            if (is == null) {
                General.showError("Failed to readTextFromInputStream because given InputStream is null");
            } else {
                General.showError("Failed to readTextFromInputStream from : " + is.toString());
            }
            return null;
        }
    }

    /**
     * Works for .Z and .gz files
     *
     * @param in
     * @param out
     * @return
     */
    public static boolean gunzipFile(File in, File out) {
        try {
            URL url = getUrlFileFromName(in.toString());
            if (url == null) {
                General.showError("Failed to get URL from: " + in.toString());
                return false;
            }
            BufferedInputStream bis = getBufferedInputStream(url);
            if (bis == null) {
                General.showError("Failed to get bis from: " + url.toString());
                return false;
            }
            FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            // Next piece of code comes from FileCopy.java in same package.
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytes_read);
            }
            bos.close(); // needed
            bis.close();
            fos.close();
        } catch (Throwable t) {
            General.showThrowable(t);
            return false;
        }

        return true;

    }

    /**
     * Can read from a url from any scheme (e.g. file, http, and ftp). Also handles compressed input when in gzipped or
     * .Z format. For that to work the file name should end in .gz or .Z though. Use existsUrl() to check if the stream
     * could be created. doesn't use the magic number from the file header to determine file type as
     * org.biojava.utils.io.InputStreamProvider does.
     */
    public static BufferedInputStream getBufferedInputStream(URL url) {
        String urlString = null;
        BufferedInputStream bufferedInputStream = null;
        URLConnection urlConnection = null;

        // Throws an exception if no data could be read from the connection
        // Kind of weird that the connection was already made then??
        // Okay, the doc says it should be considered a 2 step procedure.
        try {
            urlConnection = url.openConnection();
            urlConnection.connect(); // otherwise done implicitely
            int expectedInputBytes = urlConnection.getContentLength();
            // General.showDebug("Expecting number of (perhaps compressed) bytes: " + expectedInputBytes);
            if (expectedInputBytes < 0) {
                // General.showWarning("Expected number of input bytes is unknown");
            }
            InputStream inputStream = urlConnection.getInputStream();
            bufferedInputStream = null;
            if (inputStream != null) {
                urlString = url.toString();
            }
            if (urlString.endsWith(".gz")) {
                GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                bufferedInputStream = new BufferedInputStream(gzipInputStream);
            } else if (urlString.endsWith(".Z")) {
                UncompressInputStream uis = new UncompressInputStream(inputStream);
                bufferedInputStream = new BufferedInputStream(uis);
            } else {
                bufferedInputStream = new BufferedInputStream(inputStream);
            }
        } catch (FileNotFoundException e) {
            General.showWarning("File not found for url: " + url.toString());
            // General.showWarning("Null for inputstream returned -1-.");
            return null;
            // Other exceptions?
        } catch (Throwable t) {
            General.showThrowable(t);
            // General.showWarning("Message: " + t.getMessage());
            General.showWarning("Connection to url could't be made; does the resource exist; is the net available?");
            // General.showWarning("Null for inputstream returned -2-.");
            return null;
        }

        return bufferedInputStream;
    }

    /**
     * convenience method see method getBufferedInputStream
     */
    public static BufferedReader getBufferedReader(URL url) {
        BufferedInputStream bis = getBufferedInputStream(url);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        return br;
    }

    /**
     * Checks to see if a url resource is exists and is readible. This method wastes a connections and should probably
     * be recoded.
     */
    public static boolean availableUrl(URL url) {
        BufferedInputStream bis = getBufferedInputStream(url);
        if (bis == null) {
            return false;
        }
        return true;
    }

    /**
     * In case of zipped content the value returned is multiplied by a factor: DEFAULT_COMPRESSION_FACTOR. Returns a
     * negative number if the size failed to be estimated. More specifically, a -2 will be returned if the resource is a
     * regular file.
     */
    public static int getContentLength(URL url, boolean correctForCompression) {
        URLConnection urlConnection = null;
        int expectedInputBytes = -1;
        try {
            urlConnection = url.openConnection();
            urlConnection.connect(); // otherwise done implicitely
            expectedInputBytes = urlConnection.getContentLength();
        } catch (Throwable t) {
            General.showWarning("For url: " + url);
            General.showWarning("Connection could't be made; does the resource exist; is the net available?");
            if (url.toString().startsWith("file:/")) {
                General.showError("Failed to get file size even for a regular file object.");
                File f = getFile(url);
                if (f == null) {
                    General.showError("Failed to even get a file object for url.");
                } else if (!getFile(url).exists()) {
                    General.showError("File doesn't exist for url.");
                }
                return -2;
            }
            return -1;
        }

        if (correctForCompression && isCompressedUrl(url)) {
            expectedInputBytes *= DEFAULT_COMPRESSION_FACTOR;
        }
        return expectedInputBytes;
    }

    /**
     * Any compressed extension indicator like .gz returns true.
     */
    public static boolean isCompressedUrl(URL url) {
        String name = url.toString();
        if (name.endsWith(".gz") || name.endsWith(".zip") || name.endsWith(".tgz") || name.endsWith(".Z")) {
            return true;
        }
        return false;
    }

    /**
     * Any serializable object can be written to a file.
     */
    public static boolean writeObject(Object o, String output_file_name) {
        // TypeDesc.printType(o.getClass());
        // General.showOutput("Writing object to file: " + output_file_name);
        General.flushOutputStream();
        try {
            FileOutputStream file_out = new FileOutputStream(output_file_name);
            ObjectOutputStream out = new ObjectOutputStream(file_out);
            out.writeObject(o);
            out.close();
            // General.showMemoryUsed();
        } catch (Throwable t) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }

    /**
     * doesn't work if url can't be converted into file. returns null on error
     */
    public static File getFile(URL url) {
        URI u = null;
        try {
            u = new URI(url.toString());
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }
        File f = new File(u);
        return f;
    }

    /**
     * Reads an object from a resource starting from the package in which the specified class exists.
     */
    public static Object readObjectFromResource(String resource_name, Class cl) {
        Object o = null;
        try {
            InputStream is = cl.getResourceAsStream(resource_name);
            if (is == null) {
                General.showError("Failed to get resource as stream with name: " + resource_name);
                return null;
            }
            ObjectInputStream in = new ObjectInputStream(is);
            o = readObjectOrEOF(in);
            is.close();
        } catch (Throwable t) {
            General.showThrowable(t);
            return null;
        }

        if (o == null) {
            General.showError("Failed to load object from resource with name: " + resource_name);
            return null;
        }
        return o;
    }

    /** Convenience method allowing EOL */
    public static Object readObjectOrEOF(String file_name) {
        boolean allowEOF = true;
        return readObjectOrEOF(file_name, allowEOF);
    }

    /**
     * Reads an object and returns null if EOF is found.
     */
    public static Object readObject(String file_name) {
        boolean allowEOF = false;
        return readObjectOrEOF(file_name, allowEOF);
    }

    /** Convenience method allowing EOL if chosen. */
    public static Object readObjectOrEOF(String file_name, boolean allowEOF) {
        // General.showDebug("Reading object from file: " + file_name);
        Object o = null;
        try {
            FileInputStream is = new FileInputStream(file_name);
            if (is == null) {
                General.showError("Failed to get file as stream with name: " + file_name);
                return null;
            }
            ObjectInputStream in = new ObjectInputStream(is);
            o = readObjectOrEOF(in, allowEOF);
            is.close();
        } catch (Throwable t) {
            General.showThrowable(t);
            return null;
        }
        if (o == null) {
            General.showError("Failed to load object from file with name: " + file_name);
            return null;
        }
        return o;
    }

    /** Convenience method allowing EOL */
    public static Object readObjectOrEOF(ObjectInputStream in) {
        boolean allowEOF = true;
        return readObjectOrEOF(in, allowEOF);
    }

    /** Convenience method disallowing EOL */
    public static Object readObject(ObjectInputStream in) {
        boolean allowEOF = false;
        return readObjectOrEOF(in, allowEOF);
    }

    // Returns object if successfully read or a null if any error; including
    // EOF was encountered.
    public static Object readObjectOrEOF(ObjectInputStream in, boolean allowEOF) {
        Object o = null;
        try {
            o = in.readObject();
        } catch (EOFException e) {
            if (allowEOF) {
                return END_OF_FILE_ENCOUNTERED;
            }
            General.showThrowable(e);
            General.showOutput("End of file reached.");
            return null;
        } catch (OptionalDataException e) {
            if (e.eof) {
                General.showThrowable(e);
                General.showOutput("OptionalDataException found.");
                o = END_OF_FILE_ENCOUNTERED;
                return o;
            } else {
                General.showThrowable(e);
                return null;
            }
        } catch (Throwable t) {
            General.showThrowable(t);
            return null;
        }
        return o;
    }

    /**
     * Convenience method.
     */
    public static File changeFileNameExtension(File file, String extension_new) {
        if (file == null) {
            return null;
        }
        String fileNameNew = changeFileNameExtension(file.toString(), extension_new);
        if (fileNameNew == null) {
            return null;
        }
        File result = new File(fileNameNew);
        return result;
    }

    /**
     * If there was a filename extension change it.
     */
    public static String changeFileNameExtension(String file_name, String extension_new) {
        String[] file_name_parts = file_name.split("\\.");
        // If there was a filename extension change it.
        if (file_name_parts.length > 0) {
            file_name_parts[file_name_parts.length - 1] = extension_new;
        }
        return Strings.concatenate(file_name_parts, ".");
    }

    /**
     * Check to see if a file could contain text based on the file name extension that is given. Note that zipped files
     * aren't text directly
     */
    public static boolean fileNameExtensionCanBeText(String extension) {
        return fileNameExtensionsText.contains(extension);
    }

    /**
     * Insert a file number before extension. E.g. test.str, 89, t, 3 gives test_089.str. Doesn't work with multiple
     * extensions like test.str.gz
     */
    public static String addFileNumberBeforeExtension(String file_name, int number, boolean usePadding, int paddingCount) {
        File file = new File(file_name);
        String path = file.getParent();
        if (path == null) {
            path = "";
        }
        String ext = getFilenameExtension(file);
        if (ext == null) {
            ext = "";
        }
        String base = getFilenameBase(file);
        if (base == null) {
            base = "";
        }
        String num = null;
        if (usePadding) {
            Parameters p = new Parameters(); // Printf parameters
            String fmtStr = "%";
            if (usePadding) {
                fmtStr += "0";
                if (paddingCount > 1) {
                    fmtStr += Integer.toString(paddingCount);
                }
            }
            fmtStr += "d";
            // General.showDebug("Formatting: " + fmtStr);
            try {
                p.add(number);
                num = com.braju.format.Format.sprintf(fmtStr, p);
            } catch (Exception e) {
                General.showThrowable(e);
                num = null;
            }
        }
        if (num == null) {
            num = Integer.toString(number);
        }
        /*
         * General.showDebug("For file  : " + file_name); General.showDebug("Found path: " + path );
         * General.showDebug("Found base: " + base ); General.showDebug("Found num : " + num );
         * General.showDebug("Found ext : " + ext );
         */
        String result = path;
        if (result.length() != 0) {
            result += File.separator;
        }
        result += base + "_" + num;
        if (!ext.equals(Defs.EMPTY_STRING)) {
            result += "." + ext;
        }
        return result;

    }

    /**
     * Checks age for an existing file.
     *
     * @param d
     *            Directory in which the file will be that is to be checked.
     * @param name
     *            file name to be checked.
     * @param x
     *            Number of days the file may be old in order to qualify.
     * @return Returns <CODE>true</CODE> if the file exists and is newer than x days. Returns <CODE>false</CODE> if file
     *         doesn't exist or is older than x days.
     */
    public static boolean FileNewerThanXDays(File d, String name, int x) {
        // First check if the file disqualifies on the basis of
        // its modification time stamp.
        File f = new File(d, name);
        // DateFormat defaultDate = DateFormat.getDateInstance();
        Date lastModified = new Date(f.lastModified());

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        // Subtract a number of days
        cal.add(Calendar.DATE, -1 * x);
        Date limitModification = cal.getTime();
        if (lastModified.after(limitModification)) {
            // General.showOutput("Thus the file is newer than the limit"+f.toString());
            return true;
        } else {
            // General.showOutput("Thus the file is older than the limit"+f.toString());
            return false;
        }
    }

    /**
     * Checks if the file is a gif file.
     */
    public static class GifFilenameFilter implements FilenameFilter {
        /**
         * Checks if the file is a gif file.
         *
         * @return <CODE>true</CODE> if the file has a filename extension of "gif".
         * @param d
         *            Directory where the file resides.
         * @param name
         *            File name to be analyzed.
         */
        public boolean accept(File d, String name) {
            return (name.endsWith(".gif"));
        }
    }

    /**
     * Checks if the file is a pdb file.
     */
    public static class PDBIdDirFilenameFilter implements FilenameFilter {
        /**
         * Checks if the file is a pdb file.
         *
         * @return <CODE>true</CODE> if the file has a filename where the base could be a pdb code. The directory should
         *         also exist.
         * @param d
         *            Directory where the file resides.
         * @param name
         *            File name to be analyzed.
         */
        public boolean accept(File d, String name) {
            return (d.isDirectory() && Wattos.Utils.Strings.is_pdb_code(name));
        }
    }

    /**
     * Finds files that match a regular expression. Adapted from Jodd project.
     *
     * @see <A HREF="http://jodd.sourceforge.net">Jodd project< /A>
     */
    public static class RegExpFilenameFilter implements FilenameFilter {

        private Pattern pattern = null;

        public RegExpFilenameFilter(String regexp) {
            if (regexp != null) {
                pattern = Pattern.compile(regexp);
            } else {
                pattern = null;
            }
        }

        public boolean accept(File dir, String name) {
            if (pattern == null) { // no pattern specified -> no matching possible
                return true; // therefore file is ok
            }
            File ffull = new File(dir, name);
            Matcher matcher = pattern.matcher(ffull.getName()); // regexp match
            return matcher.matches();
        }
    }

    /**
     * Returns gif files that have a timestamp as recent as X days.
     *
     * @see #accept
     */
    public static class Gif_NewerThanXDays_FilenameFilter implements FilenameFilter {
        /**
         * Number of days the file may be old to qualify. Default of one week; doesn't really make sense.
         */
        int x = 7;

        /**
         * Constructor with initialization value.
         *
         * @param x
         *            see x
         */
        public Gif_NewerThanXDays_FilenameFilter(int x) {
            this.x = x;
        }

        /**
         * Checks if the file is a gif file.
         *
         * @return <CODE>true</CODE> if all of the below is satisfied: <LI>file exists <LI>has extension gif <LI>is
         *         newer than x number of days
         * @param d
         *            Directory where the file resides.
         * @param name
         *            File name to be analyzed.
         */
        public boolean accept(File d, String name) {
            // First check if the directory disqualifies on the basis of
            // its modification timestamp.
            if (!FileNewerThanXDays(d, name, x)) {
                // General.showOutput("The file is older than the limit :"+ name);
                return false;
            }
            // General.showOutput("The file is newer than the limit :"+ name);
            return (name.endsWith(".gif"));
        }
    }

    /**
     * Returns directories of PDB ids that have a timestamp as recent as X days.
     *
     * @see #accept
     */
    public static class PDBIdDir_NewerThanXDays_FilenameFilter implements FilenameFilter {
        /**
         * Number of days the file may be old to qualify. Default of one week; doesn't really make sense.
         */
        int x = 7;

        // Constructor with initialization value
        /**
         * Constructor with initialization value.
         *
         * @param x
         *            see x
         */
        public PDBIdDir_NewerThanXDays_FilenameFilter(int x) {
            this.x = x;
        }

        /**
         * @param d
         *            Directory where the sub dir resides.
         * @param name
         *            subdir name.
         * @return <CODE>true</CODE> if pdb id is newer than x days.
         */
        public boolean accept(File d, String name) {
            // First check if the directory disqualifies on the basis of
            // its modification timestamp.
            if (!FileNewerThanXDays(d, name, x))
                return false;
            // General.showOutput("Thus the file is newer than the limit");
            return (d.isDirectory() && Wattos.Utils.Strings.is_pdb_code(name));
        }
    }

    /**
     * Extracts the file content from starting to end lines given as parameters. A negative value for end will be taken
     * as no limit on the end.
     *
     * @param file_name
     *            file to be analyzed.
     * @return the file name extentsion. E.g. mr for a file named: "/tmp/1brv.mr".
     */
    public static String getLines(String file_name, int start, int end) {
        if (start < 1) {
            start = 0;
        }
        if (end < start) {
            end = start;
        }
        if (end < 0) {
            end = Integer.MAX_VALUE;
        }
        File f = new File(file_name);
        if (!(f.exists() && f.canRead() && f.isFile())) {
            General.showError("Failed to getLines because file is absent, unreadible, or not a File.");
            return null;
        }
        int lines_read = 0;
        General.showDebug("Reading file: " + file_name);

        StringBuffer sb = new StringBuffer();

        try {
            LineNumberReader r = new LineNumberReader(new FileReader(f));
            String line = r.readLine();
            lines_read++;
            while ((lines_read <= end) && (line != null)) {
                if (lines_read >= start) {
                    sb.append(line + General.eol);
                }
                line = r.readLine();
                lines_read++;
            }
        } catch (FileNotFoundException e) {
            General.showError("File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            General.showError("IOException found: " + e.getMessage());
            return null;
        }

        return sb.toString();
    }

    /**
     * Convenience method.
     *
     * @see #getLines
     */
    public static ArrayList getLineList(String file_name, int start, int end) {
        ArrayList list = new ArrayList();
        if (start < 1) {
            start = 0;
        }
        if (end < 0) {
            end = Integer.MAX_VALUE;
        }
        if (end < start) {
            end = start;
        }
        File f = new File(file_name);
        int lines_read = 0;

        try {
            LineNumberReader r = new LineNumberReader(new FileReader(f));
            String line = r.readLine();
            lines_read++;
            /**
             * General.showDebug("Read line: " + lines_read + "["+line+"]"); if ( line == null ) {
             * General.showDebug("Read empty line" ); } if ( lines_read <= end ) {
             * General.showDebug("Read line before ending." ); } General.showDebug("lines_read: " + lines_read );
             * General.showDebug("end:        " + end);
             */
            while ((lines_read <= end) && (line != null)) {
                // General.showDebug("now in while loop." );
                if (lines_read >= start) {
                    list.add(line);
                }
                line = r.readLine();
                lines_read++;
                // General.showDebug("Read line: " + lines_read + "["+line+"]");
            }
        } catch (FileNotFoundException e) {
            General.showError("File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            General.showError("IOException found: " + e.getMessage());
            return null;
        }

        return list;
    }

    /**
     * Reformats a file name's string representation to double backslashes
     */
    public static String getFileDoubleBackSlash(File file) {
        String file_name = file.toString();
        return file_name.replaceAll("\\\\", "\\\\\\\\"); // this is unbelievably stupid in Java
    }

    /**
     * Extracts the filename extensions with multiple dots if present. Returns empty string if there is no extension.
     *
     * @param file
     *            the file to be analyzed.
     * @return the file name extentsion. E.g. mr for a file named: "/tmp/1brv.mr".
     */
    public static String getFilenameExtension(File file) {
        String file_name = file.toString();
        return getFilenameExtension(file_name);
    }

    /**
     * Convenience method see namesake.
     */
    public static String getFilenameExtension(String file_name) {
        int idx = file_name.lastIndexOf((int) '.');
        if (idx < 0) {
            return Defs.EMPTY_STRING;
        }
        return (file_name.substring(idx + 1));
    }

    public static String getFilenameBase(File file) {
        if ( file == null ) {
            General.showCodeBug("Got null File argument in getFilenameBase");
            return null;
        }
        String file_name = file.toString();
        return getFilenameBase(file_name);
    }

    /**
     * Extracts the filename base. Deals with multiple dots by recursion.
     *
     * @param file_name
     *            the file to be analyzed.
     * @return the file name base. E.g.: /tmp/1brv.mr.gz -> 1brv
     *         http://localhost:8084/NRG/servlet/MRGridServlet?db_username=wattos -> MRGridServlet
     */
    public static String getFilenameBase(String file_name) {
        if ( file_name == null ) {
            General.showCodeBug("Got null string argument in getFilenameBase");
            return null;
        }
        file_name = file_name.replaceAll("\\\\", "/"); // ain't that a trick, trying to match a single backslash
        // Find the last slash (platform independent) if any
        int startIdx = file_name.lastIndexOf("/");
        if (startIdx == -1) {
            startIdx = 0;
        } else {
            // Point just behind slash
            startIdx++;
        }
        // does it end with a slash or is the file name an empty string.
        if (startIdx == file_name.length()) {
            return "";
        }

        // Find the last dot if any
        int endIdx = file_name.lastIndexOf('.');
        if (endIdx == -1) {
            endIdx = file_name.length();
        }

        String result = file_name.substring(startIdx, endIdx);

        /**
         * Use recursion for any remaining dots.
         */
        if (result.indexOf('.') >= 0) {
            return getFilenameBase(new File(result));
        }

        return result;
    }

    /**
     * Copy files.
     *
     * @param recursively
     *            If true recurse into directories and copy all files found. The directory structure is recreated. The
     *            base name c: or / on unix can't be given as in. The code hasn't been extensively checked.
     */
    public static boolean copyFiles(File in, File out, boolean recursively, boolean force, boolean interactive) {
        boolean status;
        try {
            in = in.getCanonicalFile();
            out = out.getCanonicalFile();

            General.showOutput("Testing copy from: " + in + " to: " + out);

            if (in.toString().equals(out.toString())) {
                if (!out.isDirectory()) {
                    General.showError("input is the same as output file: " + out);
                    return false;
                }
            }

            if (!in.exists()) {
                General.showError("Input file doesn't exist");
                return false;
            }
            if (!in.canRead()) {
                General.showError("Input file isn't readible");
                return false;
            }

            if (!(in.isFile() || in.isDirectory())) {
                General.showError("File: " + in + " is not a regular file or directory");
                return false;
            }

            if (in.isDirectory()) {
                // General.showDebug("Found directory: " + in);
                if (!recursively) {
                    General.showOutput("Ignoring directory because not recursive copy.");
                    return true;
                }
                if (out.isFile()) {
                    General.showError("Can't copy directory to a file: " + out);
                    return false;
                }
                // The output directory should be created in the named output dir
                // E.g. cp -r c:\tmp s:\test creates here: s:\test\tmp
                // The base name c: or / on unix can't be given as 'in' because
                // a name is needed.
                // FileCopy can take care of this.
                if (out.exists()) {
                    // General.showDebug("Found out to exist: " + out);
                    if (out.isFile()) {
                        General.showError("Can't copy directory to a file.");
                        return false;
                    }
                    // out is a dir and exists
                    out = new File(out, in.getName());
                    if (out.exists()) {
                        if (out.isFile()) {
                            General.showError("Can't copy directory to a file.");
                            return false;
                        }
                    } else {
                        // General.showDebug("Trying to create sub directory: " + out);
                        if (!out.mkdir()) {
                            General.showError("Can't create directory: " + out);
                            return false;
                        }
                    }
                } else {
                    // Try to create the out dir.
                    // Does the parent exist?
                    File parentOut = out.getParentFile();
                    if (!parentOut.isDirectory()) {
                        General.showError("Parent directory doesn't exist (or is a file) (create dir first): "
                                + parentOut);
                        return false;
                    }
                    if (!out.mkdir()) {
                        General.showError("Can't create directory: " + out);
                        return false;
                    }
                }
            }

            // General.showDebug("NOW HERE" );
            /** Recap: the output is a file or an existing directory */
            if (in.isDirectory()) {
                if (out.isFile()) {
                    General
                            .showError("Cannot create directory: " + out
                                    + " because file with that name already exists");
                    return false;
                }
                File[] fileList = in.listFiles(); // no specific order
                for (int i = 0; i < fileList.length; i++) {
                    File file = fileList[i];
                    if (file.isDirectory() && recursively) {
                        status = copyFiles(file, out, recursively, force, interactive);
                        if (!status) {
                            General.showError("Cannot copy files from: " + file + " to " + out);
                            return false;
                        }
                    } else {
                        status = FileCopy.copy(file, out, force, interactive);
                        if (!status) {
                            General.showError("Cannot copy single file from: " + file + " to " + out);
                            return false;
                        }
                    }
                }
            } else {
                status = FileCopy.copy(in, out, force, interactive);
                if (!status) {
                    General.showError("Cannot copy single file from: " + in + " to " + out);
                    return false;
                }
            }
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        }
        return true;
    }

    /**
     * Replace a file with the text replaced according to subs as defined in Wattos.Utils.Strings.replaceMulti. Takes
     * regular expressions!
     */
    public static boolean replaceMultiInFile(File file, Properties subs) {
        char[] buf = readTextFromFile(file);
        if (buf == null) {
            General.showError("Failed to read text from file: " + file);
            return false;
        }
        String text = new String(buf);
        text = Strings.replaceMulti(text, subs);
        if (text == null) {
            General.showError("Failed to do replacement in text: " + text);
            return false;
        }
        buf = text.toCharArray();
        boolean force = true;
        boolean interactive = false;
        boolean status = writeTextToFile(file, buf, force, interactive);
        if (!status) {
            General.showError("Failed to do write text to file: " + file);
            return false;
        }
        return true;
    }

    /** Returns the text in an array */
    public static char[] readTextFromFile(File file) {
        char[] buf = null;
        FileReader r = null;
        try {
            r = new FileReader(file);
            int fileLength = (int) file.length();
            buf = new char[fileLength];
            int c = r.read(buf, 0, fileLength); // Obviously not something done on large files.
            if (c != fileLength) {
                General.showError("Not read the expected number of chars: " + fileLength + " but: " + c);
                return null;
            }
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException e) {
                    ;
                }
        }
        return buf;
    }

    /**
     * Convenience method
     *
     * @see #writeTextToFile(File, char[], boolean, boolean)
     */
    public static boolean writeTextToFile(File file, String s, boolean force, boolean interactive) {
        return writeTextToFile(file, s.toCharArray(), force, interactive);
    }

    /**
     * Writes a char array to a file overwriting if needed. If interactive is not set then an existing file will not be
     * overwritten.
     */
    public static boolean writeTextToFile(File file, char[] buf, boolean force, boolean interactive) {
        FileWriter w = null;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            General.showError("Parent directory of file doesn't exist: " + parent.toString());
            return false;
        }
        if (file.exists() && !force) {
            if (!interactive) {
                General.showOutput("File already exists, use force to overwrite");
                return false;
            }
            if (!Strings.getInputBoolean("Overwrite file: " + file)) {
                General.showOutput("File not overwritten: " + file);
                return false;
            }
        }
        try {
            w = new FileWriter(file);
            // int fileLength = buf.length;
            w.write(buf); // Obviously not something done on large files.
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        } finally {
            if (w != null)
                try {
                    w.close();
                } catch (IOException e) {
                    General.showThrowable(e);
                    return false;
                }
        }
        return true;
    }

    /**
     * Class is adapted from http://www.rgagnon.com/javadetails/java-0150.html. JDK1.5 has a System.getenv("PATH") so
     * that could be easier still.
     */
    public static String getEnvVar(String var) {
        Properties env = getEnvVars();
        if (env == null) {
            General.showError("Can't get environment variables");
            return null;
        }
        return (String) env.get(var);
    }

    /**
     * Class is adapted from http://www.rgagnon.com/javadetails/java-0150.html. JDK1.5 has a System.getenv("PATH") so
     * that could be easier still.
     * Setting up the runtime instance allow us to retrieve env by shell rather than java.
     */
    public static Properties getEnvVars() {
        // General.showDebug("Doing getEnvVars");
        Process p = null;
        Properties envVars = new Properties();
        Runtime r = Runtime.getRuntime();
        String osName = System.getProperty("os.name");

        try {
            if (osName.startsWith("Windows")) {
                // p = r.exec( "command.com /c set" ); # Nobody is using old 98 anymore right?
                p = r.exec("cmd.exe /c set");
            } else if (osName.equals("Linux") || osName.equals("SunOS") || osName.startsWith("Mac OS")) {
                p = r.exec("env");
            } else {
                General.showOutput("Any Operating System that is not:");
                General.showOutput("- Windows");
                General.showOutput("- Linux, Mac or SunOS");
                General.showOutput("is currently not supported by Wattos.Utils.InOut.getEnvVars");
                General.showOutput("The OS of this machine is determined by Java as: " + osName);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf('=');
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1);
                envVars.setProperty(key, value);
                // System.out.println( key + " = " + value );
            }
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }
        // General.showOutput("Got getEnvVars: " +envVars.toString());

        return envVars;
    }

    /**
     * Self test; tests the method: <CODE>getFilenameExtension</CODE>
     *
     * @param args
     *            ignored.
     */
    public static void main(String[] args) {

        General.verbosity = General.verbosityDebug;
        General.showOutput("Doing tests");

//        if (false) {
//            File file = new File("testing" + File.separator + "test.gif");
//            General.showOutput("Extension of file: [" + file + "] is: [" + getFilenameExtension(file) + "]");
//        }
//        if (false) {
//            int max_age_in_days = 7;
//            String filename = args[0];
//            File d = new File(".");
//            General.showOutput("File: [" + filename + "]");
//            if (FileNewerThanXDays(d, filename, max_age_in_days)) {
//                General.showOutput("Newer than [" + max_age_in_days + "] days");
//            } else {
//                General.showOutput("Older than [" + max_age_in_days + "] days");
//            }
//        }
//        if (false) {
//            int max_age_in_days = 7;
//            String dirname = args[0];
//            File d = new File(dirname);
//            General.showOutput("dir: [" + d + "]");
//            String[] gif_file_names = d.list(new Gif_NewerThanXDays_FilenameFilter(max_age_in_days));
//            for (int i = 0; i < gif_file_names.length; i++)
//                General.showOutput("Young giffies: [" + gif_file_names[i] + "]");
//        }
//
//        if (false) {
//            String fn = "/test/testing.txt";
//            General.showOutput("fn: [" + fn + "]");
//            General.showOutput("fn: [" + changeFileNameExtension(fn, "t") + "]");
//        }
//
//        if (false) {
//            // String fn = "S:\\jurgen\\tmp\\test\\..\\handy.txt.gz"; // on windows
//            // String fn = "/share/jurgen/tmp/test/../handy.txt"; // on unix
//
//            String fn = "http://archive.ncsa.uiuc.edu:80/SDG/Software/Mosaic/Demo/url-primer.html"; // on windows
//
//            General.showOutput("file name: [" + fn + "]");
//            URL url = getUrlFileFromName(fn);
//            General.showOutput("url      : [" + url + "]");
//            BufferedInputStream bis = getBufferedInputStream(url);
//            General.showDebug("BIS is: " + bis);
//            General.showOutput("url exists: " + availableUrl(url));
//        }
//        if (false) {
//            File in = new File(args[0]);
//            File out = new File(args[1]);
//            General.showOutput("Testing copy from: " + in + " to: " + out);
//            copyFiles(in, out, true, false, false);
//            General.showOutput("Done with copy");
//        }
//        if (false) {
//            File in = new File("S:\\test_dir\\test.txt");
//            General.showOutput("Testing adding of number in: " + in);
//            General.showOutput("Result: " + addFileNumberBeforeExtension(in.toString(), 99, true, 3));
//        }
//        if (false) {
//            // File topDir = new File( "M:\\bfiles\\jurgen" );
//            // File topDir = new File( "M:\\bfiles\\jurgen" );
//            File topDir = new File("C:\\temp\\t1");
//            General.showOutput("Testing recursive empty dir delete of: " + topDir);
//            boolean status = removeEmptySubDirs(topDir);
//            if (status) {
//                General.showOutput("Succeeded in delete");
//            } else {
//                General.showError("Failed in delete");
//            }
//        }
//        if (false) {
//            File in = new File("test.txt");
//            General.showOutput("Testing replace in: " + in);
//            Properties subs = new Properties();
//            subs.setProperty("WATTOSROOT", "nieuw");
//            boolean status = replaceMultiInFile(in, subs);
//            if (status) {
//                General.showOutput("Done with replace");
//            } else {
//                General.showOutput("Failed on replace");
//            }
//        }
//        if (false) {
//            File dir = new File("M:\\bfiles");
//            String regexp = "wa.*";
//            General.showOutput("Reg exp files: " + regexp);
//            General.showOutput("Dir: " + dir);
//            if (!dir.isDirectory()) {
//                General.showError("Given File instance isn't a directory.");
//                System.exit(1);
//            }
//            RegExpFilenameFilter ff = new RegExpFilenameFilter(regexp);
//            String[] list = dir.list(ff);
//
//            General.showOutput("Found files: " + Strings.toString(list));
//            General.showOutput("Found number of files: " + list.length);
//        }
//        if (false) {
//            Properties p = getEnvVars();
//            General.showOutput("the current environment settings are : " + Strings.toString(p));
//        }
//        if (true) {
//            General.showOutput("the current system settings are : " + Strings.toString(System.getProperties()));
//        }
//        if (false) {
//            String fn = "http://localhost:8084/NRG/servlet/MRGridServlet?db_username=wattos";
//            // String fn = "/test/testing.txt.t";
//            General.showOutput("fn: [" + fn + "]");
//            General.showOutput("fn: [" + getFilenameBase(fn) + "]");
//        }
        if (true) {
            File f = new File("S:\\jurgen\\CloneWars\\DOCR1000\\_tmp_unb_\\sql", "entry_main.csv");
            General.showOutput("f : [" + f + "]");
            General.showOutput("fn: [" + getFileDoubleBackSlash(f) + "]");
        }
    }

    /**
     * Extracts the filename itself.
     *
     * @param file_name
     *            the file to be analyzed.
     * @return the file name base. E.g.: /tmp/1brv.mr.gz -> 1brv.mr.gz
     */
    public static String getFileName(String fi) {
        return (new File(fi)).getName();
    }

    static public boolean deleteDirectoryRecursively(File path) {
        General.showDebug("Removing path: " + path);
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectoryRecursively(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }
}
