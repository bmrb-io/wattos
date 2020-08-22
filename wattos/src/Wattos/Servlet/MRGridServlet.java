package Wattos.Servlet;

/**
 *Created on February 7, 2002, 3:00 PM
 *
 *This software is copyright (c) 2005 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

import Wattos.Episode_II.*;
import Wattos.Utils.*;
import Wattos.CloneWars.UserInterface;
import Wattos.Database.*;
import java.text.DecimalFormat;

/**
 * Serves blocks of text from the DB of MR files. This will return html pages with user input deciding on the display.
 * Basically an overview of the files and part of files (blocks) that are of a certain type.
 *
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRGridServlet extends HttpServlet {
    private static final long serialVersionUID = 5504236505334962699L;

    Globals g = null;

    /** DB connection for the Episode_II package. */
    SQL_Episode_II sql_epiII = null;

    /**
     * This string signals that there is not a selection on the specific attribute. Right now chosen such that it
     * doesn't show up at all in the selection boxes of the html pages. Other sensible choices might be "n/a", "null".
     */
    static final String NOT_A_SELECTION_STRING = "";
    /**
     * The default number of items (blocks or files) that have to exist if the column is to be shown on the overview.
     */
    static final String MIN_ITEMS_DEFAULT = "100";

    /** Number of block rows to show maximum in the result tables */
    int MAX_ROWS_TO_SHOW = 50;
    // How many pages should be shown to pick from left and right of picked page.
    int number_of_pages_to_show_in_indexing = 5;

    /**
     * Use a a buffer size so the stream isn't blocking too long
     */
    static final int archiveBufferSize = 5 * 1024;

    /** Dimensions of the images put up */
    static final int small_width = 50;
    static final int small_height = 35;
    static final int large_width = 760;
    static final int large_height = 532;
    static final String id_image_small = "_pin";
    static final String id_image_medium = "";
    static final String id_image_large = "_xl";
    static final String DEFAULT_DB_USERNAME = "wattos1"; // was 2
    // static final String DEFAULT_DB_USERNAME = "jfdplay"; // use for local testing

    // Just a couple of larger and easy to find ones.
    static final String[] robotsForbiddenToArchive = new String[] { "Googlebot",
            "help.yahoo.com/help/us/ysearch/slurp", "msnbot", "spider", "ask.com"

    };

    /**
     * Initializes the servlet.
     *
     * @param config
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Get info like db connection
        g = new Globals();
        // Open Episode_II database connection
        initDb();
    }

    public void initDb() throws ServletException {

        /** Set the desired verbosity level */
        General.setVerbosityToDebug();
        General.showDebug("Now in initDb");
//        General.showDebug("Wattos version: " + UserInterface.WATTOS_VERSION);
        System.setOut(System.err);
        General.showDebug("this message to System.out after redirect");
        g.setDbUserNameDerivedVariables(); // perhaps the db user name changed?
        General.showOutput(g.getMap());
        // General.showDebug("redirect out to System.out from System.err again.");
        // System.setOut( System.out );
        sql_epiII = new SQL_Episode_II(g);
        DBFSFile.g = g; // Update this one so they're taken from the right resource.

        if (sql_epiII.conn == null) {
            String msg = "Database access failed during initialization.\n"
                    + "Try again in a minute and please report the problem\n"
                    + "if the connection could again not be made, thank you.";
            General.showError(msg);
            throw new ServletException(msg);
        }
    }

    /** Destroys the servlet. */
    public void destroy() {
        sql_epiII.closeConnection();
    }

    /**
     * Show an the actual error message to client and error log.
     *
     * @param resp
     * @param message
     * @throws ServletException
     * @throws IOException
     */
    public void showActualError(HttpServletResponse resp, String message) throws ServletException, java.io.IOException {
        resp.setContentType("text/html");
        java.io.PrintWriter out = resp.getWriter();
        out.println("<P>ERROR: " + message);
        General.showError(message);
        General.showErrorDate(" at: ");
    }

    /**
     * Show a complete error message including header and footer.
     *
     * @param resp
     * @param message
     * @throws ServletException
     * @throws IOException
     */
    public void showCompleteError(HttpServletResponse resp, String message) throws ServletException,
            java.io.IOException {
        resp.setContentType("text/html");
        java.io.PrintWriter out = resp.getWriter();
        showHeader(resp);
        out.println("<P>ERROR: " + message);
        showFooter(resp);
    }

    /**
     * Stick some html code at the end.
     *
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void showFooter(HttpServletResponse resp) throws ServletException, java.io.IOException {

        java.io.PrintWriter out = resp.getWriter();

        String html_footer_text = g.getValueString("html_footer_text");

        Properties subs = new Properties();
        subs.setProperty("<!-- INSERT DATE HERE -->", java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.FULL, java.text.DateFormat.FULL).format(new java.util.Date()));
        subs.setProperty("<!-- INSERT DB_USERNAME HERE -->", g.getValueString("db_username"));
        html_footer_text = Strings.replaceMulti(html_footer_text, subs);
        out.println(html_footer_text);
    }

    /**
     * Stick some html code at the top.
     *
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void showHeader(HttpServletResponse resp) throws ServletException, java.io.IOException {

        java.io.PrintWriter out = resp.getWriter();
        String html_header_text = g.getValueString("html_header_text");
        Properties subs = new Properties();
        String title_html = "<H1><a href=\"" +
        // g.getValueString("servlet_mrgrid_absolute_url") +
                "MRGridServlet" + "\">NMR Restraints Grid</a></H1>";
        String url_image = "/" + g.getValueString("servlet_image_dir") + "/NMRRestraintsGridFlow.gif";
        String image_html = "<IMG SRC=\"" + url_image + "\" " + "border=0 "
                + "title=\"Data flows from the original raw data to parsed, to the databases DOCR and FRED\"";
        subs.setProperty("<!-- INSERT A TITLE HERE -->", title_html);
        subs.setProperty("<!-- INSERT AN IMAGE HERE -->", image_html);
        html_header_text = Wattos.Utils.Strings.replaceMulti(html_header_text, subs);
        out.println(html_header_text);
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *<OL>
     * <LI>Routine makes sure that invalid db user names return 404 as we don't want to have old cached urls picked up
     * by robots before. For debugging the code might have to be changed back here.
     *<LI>routine makes sure that robots don't get to request type archive (return 404).
     *<LI>invalid mrblock_ids are returning 404 as well
     *</OL>
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            java.io.IOException {

        // showCompleteError( resp,
        // "The database is off-line until Wed 3/22/06 approximately 20h00 CST. <BR>\n" +
        // "<P>The database is off-line for maintenance.\n" +
        // "<P>Please report this problem to the web master (e-mail address is accessible below)\n" +
        // "if you continue to experience problems, thank you.");
        // if ( true ) {
        // return;
        // }

        // Guaranteed to be connected from here on.
        HashMap options = new HashMap();

        General.showDebug("Processing options");
        if (!processOptions(req, resp, options)) {
            showCompleteError(resp, "Failed to process the options (request parameters).\n" + "Please contact admin");
            return;
        }

        String db_username = (String) options.get("db_username");
        if (db_username != null) {
            if (!(db_username.equals(DEFAULT_DB_USERNAME) || db_username.equals("wattos1"))) {
                // db_username.equals("jurgen")
                // )) {
                General.showDebug("Will not serve db_username: " + db_username);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                // showCompleteError( resp, "The db_username given: [" + db_username + "] " + "is not valid.\n" );
                return;
            }
        }
        if (db_username == null) {
            db_username = DEFAULT_DB_USERNAME;
        }

        if (!db_username.equals(g.getValueString("db_username"))) {
            g.m.put("db_username", db_username);
            if (db_username.equals("wattos1")) {
                g.m.put("db_password", "4I4KMS");
            }
            if (db_username.equals("wattos2")) {
                g.m.put("db_password", "4I4KMU");
            }
            if (db_username.equals("jurgen")) {
                g.m.put("db_password", "password");
            }
            General.showDebug("Changing DB Episode_II to db_username: [" + db_username + "]");
            initDb();
        }

        // Check if the db has a good connection, if not then try to reopen the connection
        // but return if not successful.
        if (!sql_epiII.testConnection()) {
            if (!sql_epiII.reconnect(g)) {
                // Report error and ask for user's patience.
                showCompleteError(
                        resp,
                        "The database is most likely not up. <BR>Trying to reopen the connection failed.\n"
                                + "<P>Please try again in a couple of minutes as the database might be off-line for maintenance.\n"
                                + "<P>Please report the problem to the web master (e-mail address is accessible below)\n"
                                + "if the connection could again not be made, thank you.");
                return;
            }
        }

        String request_type = (String) options.get("request_type");
        if (request_type.equals("grid")) {
            showGrid(resp, options);
            // } else if (request_type.equals("file_set") ) {
            // showFileSet(resp, options);
            // A file view is implemented as a block_set view.
            // } else if (request_type.equals("file") ) {
            // showFile(resp, options);
        } else if (request_type.equals("org_mr_file")) {
            showOrgMrFile(resp, options);
        } else if (request_type.equals("org_pdb_file")) {
            showOrgPdbFile(resp, options);
        } else if (request_type.equals("block_set")) {
            showBlockSet(resp, options);
        } else if (request_type.equals("block")) {
            ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");
            if (mrblock_ids == null || mrblock_ids.size() != 1) {
                General.showError("exactly 1 mrblock_id should be given");
                if (mrblock_ids == null) {
                    showCompleteError(resp, "none given now.");
                } else {
                    showCompleteError(resp, "number of ids given now: " + mrblock_ids.size());
                }
                return;
            }
            DBMRBlock mrb = new DBMRBlock();
            mrb.mrblock_id = ((Integer) mrblock_ids.get(0)).intValue();
            if (!sql_epiII.getMRBlock(mrb)) {
                General.showErrorDate("failed to get mrblock in showBlockTable at: ");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showBlock(resp, options);
        } else if (request_type.equals("archive")) {
            // Will return a zipped archive file containing a file for
            // each block.
            String userAgentString = req.getHeader("user-agent");
            if (userAgentString != null) {
                for (int i = 0; i < robotsForbiddenToArchive.length; i++) {
                    if (userAgentString.indexOf(robotsForbiddenToArchive[i]) >= 0) {
                        General.showDebug("Denying archive access for robot with user agent: " + userAgentString);
                        // for (int j=0; j<lEnumeration.size();j++) {
                        // String key = (String) lEnumeration.get(j);
                        // General.showOutput("key: " + key + " value: " + req.getHeader(key));
                        // }
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }
            showArchive(resp, options);
        } else {
            showCompleteError(resp, "Invalid request_type parameter value:" + request_type);
        }
    }

    /**
     * Present the results (if any) in an html table of variable sizes. This is kind of the top page.
     *
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showGrid(HttpServletResponse resp, HashMap options) throws ServletException, java.io.IOException {
        java.io.PrintWriter out = resp.getWriter();

        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();

        // Usual behavior
        if (!show_csv) {
            resp.setContentType("text/html");

            showHeader(resp);
            if (!showSelectionTable(out, options))
                return false;
            if (!showOverallResultTable(out, options))
                return false;

            // FINISH UP
            showFooter(resp);
        } else {
            // This is could be the mime type for the standard extension .csv
            resp.setContentType("text/plain");
            if (!showOverallResultTable(out, options))
                return false;
        }

        out.close();

        return true;
    }

    /**
     * Present a set of files.
     *
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return <CODE>true</CODE> for success.
     *
     *         protected boolean showFileSet( HttpServletResponse resp, HashMap options) throws ServletException,
     *         java.io.IOException { java.io.PrintWriter out = resp.getWriter();
     *
     *         boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();
     *
     *         // Usual behavior if ( ! show_csv ) { resp.setContentType("text/html"); showHeader(resp); if (!
     *         showFileSetTable(out, options) ) return false; showFooter(resp); } else {
     *         resp.setContentType("text/plain"); if (! showFileSetTable(out, options) ) return false; }
     *
     *         out.close();
     *
     *         return true; }
     */

    protected boolean showOrgMrFile(HttpServletResponse resp, HashMap options) throws ServletException,
            java.io.IOException {
        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        if (pdb_ids == null || pdb_ids.size() < 1) {
            showActualError(resp, "parameter pdb_ids absent perhaps");
            return false;
        }
        String pdb_id = (String) pdb_ids.get(0);
        if (pdb_id == null || !Wattos.Utils.Strings.is_pdb_code(pdb_id)) {
            showActualError(resp, "parameter pdb_ids failed to resolve to single pdb id");
            return false;
        }

        File mrDir = new File(g.getValueString("mr_dir"));
        String chars2And3 = pdb_id.substring(1, 3);
        File mrFile = new File(new File(mrDir, chars2And3), pdb_id + ".mr.gz");
        if (!mrFile.exists()) {
            showActualError(resp, "No MR file found for pdb_id: " + pdb_id);
            return false;
        }
        String text = InOut.readTextFromUrl(InOut.getUrlFileFromName(mrFile.toString()));
        if (text == null) {
            showActualError(resp, "Failed to read existing MR file for pdb_id: " + pdb_id);
            return false;
        }

        java.io.PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        // showHeader(resp);
        out.println("<PRE>");
        out.write(text);
        out.println("</PRE>");
        // showFooter(resp);
        out.close();
        return true;
    }

    protected boolean showOrgPdbFile(HttpServletResponse resp, HashMap options) throws ServletException,
            java.io.IOException {
        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        if (pdb_ids == null || pdb_ids.size() < 1) {
            showActualError(resp, "parameter pdb_ids absent perhaps");
            return false;
        }
        String pdb_id = (String) pdb_ids.get(0);
        if (pdb_id == null || !Wattos.Utils.Strings.is_pdb_code(pdb_id)) {
            showActualError(resp, "parameter pdb_ids failed to resolve to single pdb id");
            return false;
        }

        File pdbDir = new File(g.getValueString("pdb_dir"));
        String chars2And3 = pdb_id.substring(1, 3);
        File pdbFile = new File(new File(pdbDir, chars2And3), "pdb" + pdb_id + ".ent.gz");
        if (!pdbFile.exists()) {
            showActualError(resp, "No PDB file found for pdb_id: " + pdb_id);
            return false;
        }
        String text = InOut.readTextFromUrl(InOut.getUrlFileFromName(pdbFile.toString()));
        if (text == null) {
            showActualError(resp, "Failed to read existing PDB file for pdb_id: " + pdb_id);
            return false;
        }

        java.io.PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        out.println("<PRE>");
        out.write(text);
        out.println("</PRE>");
        out.close();
        return true;
    }

    /**
     * Present a set of blocks.
     *
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showBlockSet(HttpServletResponse resp, HashMap options) throws ServletException,
            java.io.IOException {
        java.io.PrintWriter out = resp.getWriter();

        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();

        // Usual behavior
        if (!show_csv) {
            resp.setContentType("text/html");
            showHeader(resp);
            if (!showBlockSetTable(out, options))
                return false;
            showFooter(resp);
        } else {
            resp.setContentType("text/plain");
            if (!showBlockSetTable(out, options))
                return false;
        }

        out.close();
        return true;
    }

    /**
     * Present one block with a leading table of the style of showBlockSet followed by the ASCII text in a preformatted
     * html block.
     *
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showBlock(HttpServletResponse resp, HashMap options) throws ServletException, java.io.IOException {
        java.io.PrintWriter out = resp.getWriter();

        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();

        boolean status = true;
        // Usual behavior
        if (!show_csv) {
            resp.setContentType("text/html");
            // showHeader(resp);
            if (!showBlockTable(out, options)) {
                status = false;
            }
            showFooter(resp);
        } else {
            resp.setContentType("text/plain");
            if (!showBlockTable(out, options)) {
                status = false;
            }
        }
        if (!status) {
            String message = "Failed to showBlockTable; please contact admin.";
            out.println("<P>ERROR: " + message);
        }

        out.close();
        return true;
    }

    /**
     * Will return one zipped archive file containing a file for each block plus an index file and a readme. Only the
     * ".zip" type of archive is supported through java as far as known today.
     *
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showArchive(HttpServletResponse resp, HashMap options) throws ServletException,
            java.io.IOException {
        // see: http://www.javaworld.com/javaworld/javatips/jw-javatip94.html and
        // http://www.inside-java.com/articles/servlets/zipservlet.htm
        int compressionLevel = Deflater.BEST_SPEED; // Perhaps NO_COMPRESSION? Such a joke but should be faster than:
                                                    // BEST_SPEED!

        String selection = getQuerySql(options);
        ArrayList al = sql_epiII.getMRBlockIds(options, selection);
        if (al == null || al.size() < 1) {
            String msg = "ERROR: fetch of list mrblock_ids failed to succeed.";
            if (al != null) {
                msg = "WARNING: fetch of list mrblock_ids came back empty.";
            }
            showCompleteError(resp, msg);
            return false;
        }

        // Buffer the README, main.css, and index.csv files
        HashMap optionsIndex = (HashMap) options.clone();
        optionsIndex.put("show_csv", new Boolean(true)); // override the default false
        optionsIndex.put("request_type", "block_set"); // override the current "archive" otherwise this cycles.
        String[] url_list = new String[] { g.getValueString("servlet_html_absolute_url") + "/readme.html",
                g.getValueString("servlet_html_absolute_url") + "/main.css",
                g.getValueString("servlet_mrgrid_absolute_url") + "?" + getQueryUrl(optionsIndex) };
        String[] new_file_name_list = new String[] { "readme.html", "main.css", "index.csv", };
        int MIN_BYTES_NEEDED = 50;
        String extraMsg = "<BR>Please contact us if you believe this could be an error.";
        byte[][] new_file_bytes_list = new byte[new_file_name_list.length][];
        for (int f = 0; f < url_list.length; f++) {
            String urlStr = url_list[f];
            URL url = InOut.getUrlFileFromName(urlStr);
            // General.showOutput("url to retrieve: [" + url.toString()+ "]");
            String text = InOut.readTextFromUrl(url);
            if (text == null) {
                showCompleteError(resp, "Unavailable text from url: " + url + extraMsg);
                return false;
            }
            byte[] ba = Strings.toByteArray(text);
            if (ba == null) {
                showCompleteError(resp, "Unable to translate text from url: " + url + extraMsg);
                return false;
            }
            if (ba.length < MIN_BYTES_NEEDED) {
                showCompleteError(resp, "Text less than " + MIN_BYTES_NEEDED + " chars from url: " + url + extraMsg);
                return false;
            }
            new_file_bytes_list[f] = ba;
        }

        resp.setContentType("application/zip");
        String date_str = Dates.getDateWithoutFunnyChars();
        resp.setHeader("Content-Disposition", "attachment; filename=" + "query_result_" + date_str + ".zip;");

        // After next line we can't switch back to reporting errors anymore.
        ServletOutputStream out = resp.getOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(out, archiveBufferSize);
        ZipOutputStream zout = new ZipOutputStream(bout);
        zout.setComment("Created at BMRB - http://www.bmrb.wisc.edu by Wattos");
        zout.setMethod(ZipOutputStream.DEFLATED);
        zout.setLevel(compressionLevel);

        // Write the buffered files
        for (int f = 0; f < url_list.length; f++) {
            zout.putNextEntry(new ZipEntry(new_file_name_list[f]));
            zout.write(new_file_bytes_list[f]);
            new_file_bytes_list[f] = null; // for gc.
            zout.closeEntry();
        }

        StringBuffer errors = new StringBuffer();
        for (Iterator i = al.iterator(); i.hasNext();) {
            DBMRBlock mrb = new DBMRBlock();
            int ii = ((Integer) i.next()).intValue();
            mrb.mrblock_id = ii;
            // Get the block text overwriting everything but the id.
            if (!sql_epiII.getMRBlock(mrb)) {
                String msg = "Failed to get a block: " + ii + " ; perhaps the block disappeared by DB update.";
                // General.showError(msg);
                errors.append(msg + General.eol);
                continue; // continue with other blocks but realize that the user got incomplete data!
            }
            // Write
            String fileName = mrb.fileName; // don't use full original name just the extension.
            String fileExtension = InOut.getFilenameExtension(fileName);
            zout.putNextEntry(new ZipEntry("block_" + mrb.mrblock_id + "." + fileExtension));
            // long time = System.currentTimeMillis();
            if (mrb.isTextBlock()) {
                ArrayList lines = mrb.getLines();
                for (Iterator l = lines.iterator(); l.hasNext();) {
                    String line_buffer = (String) l.next() + General.eol;
                    byte[] buffer = line_buffer.getBytes(); // this writes Unicode. Is that what's needed?
                    zout.write(buffer, 0, buffer.length);
                }
            } else {
                zout.write(mrb.content);
            }
            // time = System.currentTimeMillis() - time;
            // General.showDebug("Time for write on zipped stream: " + time);
            zout.closeEntry();
        }

        // Write the buffered errors if any.
        if (errors.length() != 0) {
            byte[] ba = Strings.toByteArray(errors.toString());
            if (ba == null) {
                General.showError("Unable to translate errors to bytes, skipping generation of error file.");
            } else {
                zout.putNextEntry(new ZipEntry("errors.txt"));
                zout.write(ba);
                zout.closeEntry();
            }
        }

        zout.finish();
        zout.close();
        out.close();
        return true;
    }

    /**
     * Present the results (if any) in an html table with a row for each block.
     *
     * @param out
     * @param options
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showBlockSetTable(PrintWriter out, HashMap options) {

        String selection = getQuerySql(options);
        DbTable table = sql_epiII.getMRBlockSetTable(options, selection);

        if (table == null) {
            return false;
        }

        int column_image = 0;
        int column_mrblock_id = 1;
        int column_pdb_id = 2;
        int column_bmrb_id = 3;
        int column_nrg_cing = 4;
        int column_in_recoord = 5;
        int column_in_dress = 6;
        // int column_stage = 7;
        // int column_position = 8;
        int column_program = 9;
        // int column_type = 10;
        int column_subtype = 11;
        int column_format = 12;

        table.insertColumn(column_image);
        table.setLabel(column_image, "image");
        table.insertColumn(column_nrg_cing);
        table.setLabel(column_nrg_cing, "cing");
        table.replaceStringByColumn(column_program, "n/a", "");
        table.replaceStringByColumn(column_subtype, "n/a", "");
        table.replaceStringByColumn(column_format, "n/a", "");
        table.setLabel(column_format, "subsubtype");

        // Decide on the print
        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();
        if (show_csv) { // save a lot of memory by not using a complex property object per value.
            out.println(table.toCsv(true));
            return true;
        }

        HashMap tmp_options = new HashMap(options);
        String base_url = g.getValueString("MRGridServlet");
        tmp_options.remove("page_id"); // Remove current page id from new urls
        String jump_url = base_url + "?" + getQueryUrl(tmp_options);
        String full_url;

        String links_string = "";
        String jump_string = "Blocks: ";
        String jump_form = "<FORM  METHOD=\"GET\"  ACTION=\"" + base_url + "\">\n";
        String jump_fields = "<INPUT TYPE=\"text\"   NAME=\"page_id\"  SIZE=\"3\">\n"
                + "<INPUT TYPE=\"submit\" VALUE=\"Submit\">\n";
        jump_fields += getQueryForm(tmp_options);
        // General.showOutput( "jump_fields :\n" + jump_fields);
        // General.showOutput("jump_url: " + jump_url);

        int page_id = ((Integer) options.get("page_id")).intValue();
        int number_of_blocks = table.sizeRows();
        // For 2 blocks per page (MAX_ROWS_TO_SHOW==2)
        // 0 blocks -> 1 page
        // 1 -> 1
        // 2 -> 1
        // 3 -> 2 pages etc.
        int number_of_pages_total = (number_of_blocks - 1) / MAX_ROWS_TO_SHOW + 1;
        if (page_id < 1 || page_id > number_of_pages_total) {
            out.println("Parameter: page id given: " + page_id + " isn't in range: [1," + number_of_pages_total + "]");
            return false;
        }
        // page 1 -> begin block 1
        // page 2 -> begin block 3
        // page 1 -> end block 2
        // page 2 -> end block 4
        int begin_block_number = (page_id - 1) * MAX_ROWS_TO_SHOW + 1;
        int end_block_number = Math.min(number_of_blocks, begin_block_number + MAX_ROWS_TO_SHOW - 1);
        /**
         * General.showOutput( "page_id                : " + page_id); General.showOutput( "number_of_blocks       : " +
         * number_of_blocks); General.showOutput( "begin_block_number     : " + begin_block_number); General.showOutput(
         * "end_block_number       : " + end_block_number); General.showOutput( "number_of_pages_total  : " +
         * number_of_pages_total); General.showOutput( General.eol);
         */
        int begin_row = begin_block_number - 1;
        int end_row = end_block_number - 1;

        jump_string += "<B>" + begin_block_number + "-" + end_block_number + "</B> of " + number_of_blocks
                + ". &nbsp;&nbsp;&nbsp;&nbsp;\n" + "Jump to page number: " + jump_fields
                + "&nbsp; (should be in range: 1 and " + number_of_pages_total + ")\n" + "</FORM>\n";

        if (number_of_pages_total > 1 && (!show_csv)) {
            if (!table.keepRowsFromTo(begin_row, end_row + 1)) {
                General.showOutput("Failed to do table.keepRowsFromTo with begin_row, end_row+1" + begin_row + ", "
                        + end_row + 1);
                return false;
            }
            links_string = jump_form + "Result page: ";
            String prev_string = "";
            String next_string = "";
            String jump_url_for_page;
            if (page_id > 1) {
                jump_url_for_page = jump_url + "&page_id=" + 1;
                prev_string = "<a href=\"" + jump_url_for_page + "\">First &lt;&lt;</a>&nbsp;";
                if (page_id > 2) {
                    jump_url_for_page = jump_url + "&page_id=" + (page_id - 1);
                    prev_string += " <a href=\"" + jump_url_for_page + "\">Previous &lt;</a>&nbsp;";
                }
            }
            if (page_id < number_of_pages_total) {
                if (page_id < (number_of_pages_total - 1)) {
                    jump_url_for_page = jump_url + "&page_id=" + (page_id + 1);
                    next_string += " <a href=\"" + jump_url_for_page + "\">> Next</a>&nbsp;";
                }
                jump_url_for_page = jump_url + "&page_id=" + number_of_pages_total;
                next_string += " <a href=\"" + jump_url_for_page + "\">>> Last (" + number_of_pages_total
                        + ")</a>&nbsp;";
            }

            int first_link = Math.max(1, page_id - number_of_pages_to_show_in_indexing);
            int last_link = Math.min(number_of_pages_total, page_id + number_of_pages_to_show_in_indexing);
            links_string += prev_string;
            for (int link = first_link; link <= last_link; link++) {
                jump_url_for_page = jump_url + "&page_id=" + link;
                // List link but don't include a link out for the current file_id
                if (link == page_id) {
                    links_string += " <B>" + link + "</B>";
                } else {
                    links_string += " <a href=\"" + jump_url_for_page + "\">" + link + "</a>";
                }
            }
            links_string += next_string;
            links_string += "<BR>\n" + jump_string;
        }

        HtmlTable htmltable = new HtmlTable(table);
        htmltable.setCommonDefaults();
        int row_max = htmltable.sizeRows();
        int column_max = htmltable.sizeColumns();
//        General.showDebug("Working on table of size: " + row_max + " rows and columns: " + column_max);

        // Do formatting and get rid of null strings.
        for (int c = 0; c < column_max; c++) {
//             General.showDebug("working on col: " + c);
            // General.showMemoryUsed();
            htmltable.setCellAttributeByColumn(c, "align", "\"Left\"");
            htmltable.setCellAttributeByColumn(c, "valign", "\"Middle\"");
            for (int r = 0; r < row_max; r++) {
                if (htmltable.getValue(r, c) == null)
                    htmltable.setValue(r, c, "");
            }
        }

        htmltable.setValueHtmlCodeByRow(-1, "<B>", "</B>");
        // Setup the links, this is a little expensive for when there are
        // thousands of rows but not too bad...
        for (int r = 0; r < row_max; r++) {
            // MRBlock id
            tmp_options = new HashMap(options);
            // Use string representation although it's an integer.
            ArrayList mrblock_ids = new ArrayList();
            mrblock_ids.add(new Integer(htmltable.getValue(r, column_mrblock_id).toString()));
            tmp_options.put("mrblock_ids", mrblock_ids);
            tmp_options.put("request_type", "block");
            full_url = base_url + "?" + getQueryUrl(tmp_options);
            htmltable.setValueHtmlCode(r, column_mrblock_id, "<A HREF=\"" + full_url + "\">", "</A>");
        }

        String servlet_molgrap_dir = g.getValueString("servlet_molgrap_dir");
        // Setup the image links.
        for (int r = 0; r < row_max; r++) {
            // Use string representation although it's an integer.
            String pdb_id = (String) htmltable.getValue(r, column_pdb_id);
            String pdb_subid = pdb_id.substring(1, 3);

            String full_url_image_small = servlet_molgrap_dir + "/pic/" + pdb_subid + "/" + pdb_id + id_image_small
                    + ".gif";

            String full_url_image_large = servlet_molgrap_dir + "/pic/" + pdb_subid + "/" + pdb_id + id_image_large
                    + ".gif";

            htmltable.setValue(r, column_image, "<IMG SRC=\"" + full_url_image_small + "\" " + "width=" + small_width
                    + " height=" + small_height + " border=0 " + ">");
            htmltable.setValueHtmlCode(r, column_image, "<A HREF=\"" + full_url_image_large + "\">", "</A>");
        }

        // Setup the bmrb links. Do before pdb mutalation.
        String bmrb_url = g.getValueString("bmrb_url");
        // String dbmatch = g.getValueString("dbmatch");
        String url_bmrb_file = null;
        // Setup the bmrb links. Do before pdb mutalation.
        for (int r = 0; r < row_max; r++) {
            // Use string representation although it's an integer.
            String bmrb_id = (String) htmltable.getValueString(r, column_bmrb_id);
            // String pdb_id = (String) htmltable.getValueString(r,column_pdb_id);
            if (bmrb_id.length() != 0) {
                /**
                 * url_bmrb_file = dbmatch + pdb_id; htmltable.setValue( r, column_bmrb_id, "search" );
                 */
                // } else {
                url_bmrb_file = bmrb_url + bmrb_id;
                htmltable.setValue(r, column_bmrb_id, bmrb_id);
                htmltable.setValueHtmlCode(r, column_bmrb_id, "<A HREF=\"" + url_bmrb_file + "\">", "</A>");
            }
        }

        // Setup the recoord links. Do before pdb mutalation.
        String recoord_url = g.getValueString("recoord_url");
        String url_recoord_file = null;
        for (int r = 0; r < row_max; r++) {
            Object in_recoordObject = htmltable.getValue(r, column_in_recoord);
            Boolean in_recoord = new Boolean(false);
            if (in_recoordObject instanceof Boolean) {
                in_recoord = (Boolean) in_recoordObject;
            }
            if (!htmltable.isEmptyValue(in_recoord)) {
                String pdb_id = (String) htmltable.getValueString(r, column_pdb_id);
                htmltable.setValue(r, column_in_recoord, "recoord");
                url_recoord_file = recoord_url + "/" + pdb_id + ".html";
                htmltable.setValueHtmlCode(r, column_in_recoord, "<A HREF=\"" + url_recoord_file + "\">", "</A>");
            } else {
                htmltable.setValue(r, column_in_recoord, "");
            }
        }
        // Setup the dress links. Do before pdb mutalation.
        String dress_url = g.getValueString("dress_url");
        String url_dress_file = null;
        for (int r = 0; r < row_max; r++) {
            Object in_dressObject = htmltable.getValue(r, column_in_dress);
            Boolean in_dress = new Boolean(false);
            if (in_dressObject instanceof Boolean) {
                in_dress = (Boolean) in_dressObject;
            }
            if (!htmltable.isEmptyValue(in_dress)) {
                String pdb_id = (String) htmltable.getValueString(r, column_pdb_id);
                htmltable.setValue(r, column_in_dress, "dress");
                url_dress_file = dress_url + pdb_id;
                htmltable.setValueHtmlCode(r, column_in_dress, "<A HREF=\"" + url_dress_file + "\">", "</A>");
            } else {
                htmltable.setValue(r, column_in_dress, "");
            }
        }
        // Setup the NRG-CING links. Do before pdb mutalation.
        String nrg_cing_url = g.getValueString("nrg_cing_url");
        String url_cing_file = null;
        for (int r = 0; r < row_max; r++) {
            String pdb_id = (String) htmltable.getValueString(r, column_pdb_id);
            htmltable.setValue(r, column_nrg_cing, "cing");
            url_cing_file = nrg_cing_url.replaceAll("%s", pdb_id);
            url_cing_file = url_cing_file.replaceAll("%t", pdb_id.substring(1,3));
            htmltable.setValueHtmlCode(r, column_nrg_cing, "<A HREF=\"" + url_cing_file + "\">", "</A>");
        }

        // Setup the pdb links.
        for (int r = 0; r < row_max; r++) {
            String pdb_id = (String) htmltable.getValue(r, column_pdb_id);
            tmp_options = new HashMap(options);
            tmp_options.put("request_type", "org_mr_file");
            ArrayList pdb_ids = new ArrayList();
            pdb_ids.add(pdb_id);
            tmp_options.put("pdb_ids", pdb_ids);
            String full_url_1 = base_url + "?" + getQueryUrl(tmp_options);
            tmp_options.put("request_type", "org_pdb_file"); // changes value set before.
            String full_url_2 = base_url + "?" + getQueryUrl(tmp_options);
            String imageHtml = " " + "<A rel=\"nofollow\" HREF=\"" + full_url_1 + "\">" + "<img SRC=\"" + "/"
                    + g.getValueString("servlet_image_dir") + "/" + "r.gif\" " + "title=\"PDB restraint file\" "
                    + "width=\"20\" height=\"20\" border=\"0\"></A>" + "<A rel=\"nofollow\" HREF=\"" + full_url_2
                    + "\">" + "<img SRC=\"" + "/" + g.getValueString("servlet_image_dir") + "/" + "c.gif\" "
                    + "title=\"PDB coordinate file\" " + "width=\"20\" height=\"20\" border=\"0\"></A> ";
            htmltable.apendToValue(r, column_pdb_id, imageHtml);
        }

        out.println("<P>Result table<BR>");
        if (!htmltable.removeAnyEmptyColumn()) {
            return false;
        }
        /**
         * tmp_options = new HashMap( options ); tmp_options.put("show_csv", new Boolean(true)); full_url = base_url +
         * "?" + getQueryUrl ( tmp_options ); out.println( "<A HREF=\"" + full_url + "\">"); out.println( "<img SRC=\""
         * + "/" + g.getValueString("servlet_image_dir") + "/" + "expcsv.gif\" " +
         * //"alt=\"Save to comma-separated-values file.\" " + // tooltips should go through title tag
         * "title=\"Save to comma-separated-values file.\" " + "width=\"23\" height=\"22\" border=\"0\">"); out.println(
         * "&nbsp;(Save to comma-separated-values file)</A><BR>
         * ");
         */

        out.println(" ");
        tmp_options = new HashMap(options);
        tmp_options.put("request_type", "archive");
        full_url = base_url + "?" + getQueryUrl(tmp_options);
        out.println("<NOINDEX>"); // THIS causes a lot of overhead without benefit.
        out.println("<A rel=\"nofollow\" HREF=\"" + full_url + "\">"); // added nofollow for robots not to index again,
                                                                       // probably in vain.
        out.println("<img SRC=\"" + "/" + g.getValueString("servlet_image_dir") + "/" + "zip.gif\" "
                + "title=\"Save to zip file containing files for each block.\" " +
                // "Save the table as csv file too in order to make sense of the block ids.\" " +
                "width=\"18\" height=\"19\" border=\"0\">");
        out.println("&nbsp;(Save to zip file containing files for each block)</A><BR>");
        out.println("</NOINDEX>");

        out.println("<BR>");

        out.println(links_string);
        out.println(htmltable.toHtml(true));
        return true;
    }

    /**
     * Misnomer as it doesn't return a table but name was chosen in accordance to the general scheme that showXXXXtable
     * gets called by showXXXX.
     *
     * @param out
     * @param options
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showBlockTable(PrintWriter out, HashMap options) {
        ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");
        if (mrblock_ids == null || mrblock_ids.size() != 1) {
            General.showError("exactly 1 mrblock_id should be given");
            if (mrblock_ids == null)
                General.showError("none given now.");
            else
                General.showError("number of ids given now: " + mrblock_ids.size());
            return false;
        }

        int mrblock_id = ((Integer) mrblock_ids.get(0)).intValue();
        DBMRBlock mrb = new DBMRBlock();
        mrb.mrblock_id = mrblock_id;
        if (!sql_epiII.getMRBlock(mrb)) {
            General.showErrorDate("ailed to get mrblock in showBlockTable at: ");
            return false;
        }
        String pdb_id = sql_epiII.getPdbId(mrb);
        String pdb_subid = pdb_id.substring(1, 3);

        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();

        // Usual behavior
        if (!show_csv) {
            // Show 1 large image in the header.
            String image_str = "";
            if (pdb_id != null && Strings.is_pdb_code(pdb_id)) {
                String full_url_image_medium = g.getValueString("servlet_molgrap_dir") + "/pic/" + pdb_subid + "/"
                        + pdb_id + id_image_medium + ".gif";
                String full_url_image_large = g.getValueString("servlet_molgrap_dir") + "/pic/" + pdb_subid + "/"
                        + pdb_id + id_image_large + ".gif";
                image_str = "<A HREF=\"" + full_url_image_large + "\">" + "<IMG SRC=\"" + full_url_image_medium + "\" "
                        + "border=0 " + "title=\"Image of entry with pdb id " + pdb_id
                        + ". Click to get larger image.\" " + "></A>";
            }
            String html_header_text = g.getValueString("html_header_text");
            Properties subs = new Properties();
            subs.setProperty("<!-- INSERT A TITLE HERE -->", "<H1><a href=\"" + g.getValueString("MRGridServlet")
                    + "\">NMR Restraints Grid</a></H1>");
            subs.setProperty("<!-- INSERT AN IMAGE HERE -->", image_str);
            html_header_text = Wattos.Utils.Strings.replaceMulti(html_header_text, subs);
            out.println(html_header_text);
            // Show a nice table any way.
            out.println("<P>");
            showBlockSetTable(out, options);
            out.println("<P><HR><P>");

            out.println("<PRE>");
            if (mrb.isTextBlock()) {
                out.println(mrb.toString());
            } else {
                out.println("Looking at file: " + mrb.fileName);
                out
                        .println("The data in this block can't be rendered as text, please save and open with appropriate program.");
            }
            out.println("</PRE>");
        } else {
            showBlockSetTable(out, options);
        }
        return true;
    }

    /**
     * Present the results (if any) in an html table of variable sizes. This is the main table.
     *
     * @param out
     * @param options
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showOverallResultTable(PrintWriter out, HashMap options) {
        String query_sql = getQuerySql(options);
        ArrayList ret = sql_epiII.getMRItemCounts(options, query_sql);
        // ArrayList ret = sql_epiII.getMRItemCounts( show_blocks, pdb_ids, bmrb_ids,
        // block_text_types, file_details );

        if (ret == null) {
            return false;
        }

        String base_url = g.getValueString("MRGridServlet");
        // String query_url = null;
        String full_url = null;

        // Use this instead of cloning to get a 'deep' copy
        // This standard set of options will be copied later.
        HashMap std_options = new HashMap(options);
        HashMap tmp_options = null;

        out.println("<P>");
        /**
         * Check for a single related BMRB entry.
         */
        int bmrb_id = sql_epiII.getSingleBMRBId(options, query_sql);
        if (bmrb_id < 0) {
            out.println("ERROR: Failed to do: sql_epiII.getSingleBMRBId");
            return false;
        }
        if (bmrb_id > 0) {
            out.println("Related BMRB entry: ");
            String url_bmrb_file = g.getValueString("bmrb_url") + bmrb_id;
            out.println("<A HREF=\"" + url_bmrb_file + "\">" + bmrb_id + "</A><BR>");
        }

        /** Show the number of parsed constraints. */
        int parsed_constraint_count = sql_epiII.getCountParsedRestraints();
        if (parsed_constraint_count < 0) {
            out.println("ERROR: Failed to do: sql_epiII.getCountParsedRestraints");
            return false;
        }
        int parsed_entry_count = sql_epiII.getCountParsedEntries();
        if (parsed_entry_count < 0) {
            out.println("ERROR: Failed to do: sql_epiII.getCountParsedEntries");
            return false;
        }
        String pattern = "0,000,000";
        DecimalFormat nf = new DecimalFormat(pattern);
        String usedStr = nf.format(parsed_constraint_count);
        tmp_options = new HashMap(std_options);
        ArrayList programs = new ArrayList();
        ArrayList types = new ArrayList();
        ArrayList subtypes = new ArrayList();
        ArrayList block_text_types = new ArrayList();
        programs.add("STAR");
        types.add("entry");
        subtypes.add("full");
        block_text_types.add("2-parsed");
        tmp_options.put("programs", programs);
        tmp_options.put("types", types);
        tmp_options.put("subtypes", subtypes);
        tmp_options.put("block_text_types", block_text_types);
        full_url = base_url + "?" + getQueryUrl(tmp_options);
        out.println("There are " + "<A HREF=\"" + full_url + "\">" + usedStr + "</A> parsed constraints in "
                + "<A HREF=\"" + full_url + "\">" + parsed_entry_count + "</A> entries");

        // Change the new request type from grid to a set.
        // boolean show_blocks = ((Boolean) options.get("show_blocks")).booleanValue();
        // if ( show_blocks ) {
        std_options.put("request_type", "block_set");
        // } else {
        // std_options.put("request_type", "file_set");
        // }

        DbTable table_1 = (DbTable) ret.get(0);
        DbTable table_2 = (DbTable) ret.get(1);
        DbTable table_3 = (DbTable) ret.get(2);
        DbTable table_4 = (DbTable) ret.get(3);

        /*
         * delete after testing is done out.println("<H4>Table I:</H4>" ); out.println(table_1.toHtml());
         * out.println("<H4>Table II:</H4>" ); out.println(table_2.toHtml()); out.println("<H4>Table III:</H4>" );
         * out.println(table_3.toHtml()); out.println("<H4>Table IV:</H4>" ); out.println(table_4.toHtml());
         */

        int program_count = table_2.sizeRows();
        int types_count = table_3.sizeRows();

        // A column for total, type, subtype, and format
        int column_min = 5;
        int column_max = program_count + column_min;

        // A row for each result tuple of table IV plus the total row
        int row_min = 1; // Total row
        int row_max = types_count + row_min;

        HtmlTable htmltable = new HtmlTable(row_max, column_max);
        htmltable.setCommonDefaults();
        // Reset border though
        htmltable.table_attributes.setProperty("border", "1");

        // StringBuffer cell_value = new StringBuffer();

        int column_total_header = 0;
        int column_type = 1;
        int column_subtype = 2;
        int column_format = 3;
        int column_total = 4;
        int column_first_program = 5;

        int row_total = 0;
        int row_first_types = 1;

        // Cell Attributes
        htmltable.setCellAttributeByTable("align", "\"Left\"");
        // Number values should all be right aligned.
        htmltable.setCellAttributeByRow(row_total, "align", "\"Right\"");
        for (int c = column_first_program - 1; c < column_max; c++) {
            htmltable.setCellAttributeByColumn(c, "align", "\"Right\"");
        }

        // Cell values
        // This will initialize the table values to something that will work with rowspan.
        htmltable.setValueByTable(HtmlTable.EMPTY_CELL_HTML_TABLE);

        htmltable.setLabel(column_total_header, "");
        htmltable.setLabel(column_type, "Type");
        htmltable.setLabel(column_subtype, "Subtype");
        htmltable.setLabel(column_format, "Subsubtype");
        htmltable.setLabel(column_total, "<B>Total</B>");
        // Overall count
        htmltable.setValue(row_total, column_total, table_1.getValue(0, 0));
        full_url = base_url + "?" + getQueryUrl(std_options);
        htmltable.setValueHtmlCode(row_total, column_total, "<A HREF=\"" + full_url + "\">", "</A>");

        htmltable.setValue(row_total, column_total_header, "Total");

        /*
         * Counts by program id is the number of the program as it is printed across the table.The columns that have
         * less than set minimum total occurrences will bedeleted here.
         */
        HashMap program_name_to_id = new HashMap();
        int program_ok_count = 0;
        int min_items = Integer.parseInt(MIN_ITEMS_DEFAULT);
        Object tmp = options.get("min_items");
        if (tmp != null)
            min_items = ((Integer) tmp).intValue();
        for (int c = 0; c < program_count; c++) {
            String program = (String) table_2.getValue(c, 0);
            int total = ((Integer) table_2.getValue(c, 1)).intValue();
            if (total <= min_items) {
                // Delete the column in the table reserved for this program.
                htmltable.removeColumn(column_first_program + program_ok_count);
                continue;
            }
            program_ok_count++;
            tmp_options = new HashMap(std_options);
            programs = new ArrayList();
            programs.add(program);
            tmp_options.put("programs", programs);
            full_url = base_url + "?" + getQueryUrl(tmp_options);
            htmltable.setLabel(column_first_program + (program_ok_count - 1), program);
            htmltable.setValue(row_total, column_first_program + (program_ok_count - 1), table_2.getValue(c, 1));
            htmltable.setValueHtmlCode(row_total, column_first_program + (program_ok_count - 1), "<A HREF=\""
                    + full_url + "\">", "</A>");
            program_name_to_id.put(program, new Integer(program_ok_count - 1));
        }

        // by types
        HashMap types_to_id = new HashMap();
        for (int r = 0; r < types_count; r++) {
            // The collapsed type is unique no doubt; always!
            String type = (String) table_3.getValue(r, 0);
            String subtype = (String) table_3.getValue(r, 1);
            String format = (String) table_3.getValue(r, 2);

            String types_concatenated = type + "_" + subtype + "_" + format;
            tmp_options = new HashMap(std_options);
            types = new ArrayList();
            subtypes = new ArrayList();
            ArrayList formats = new ArrayList();
            types.add(type);
            subtypes.add(subtype);
            formats.add(format);
            tmp_options.put("types", types);
            tmp_options.put("subtypes", subtypes);
            tmp_options.put("formats", formats);
            // General.showOutput("tmp_options:" + tmp_options );
            full_url = base_url + "?" + getQueryUrl(tmp_options);
            htmltable.setValue(row_first_types + r, column_type, type);
            htmltable.setValue(row_first_types + r, column_subtype, subtype);
            htmltable.setValue(row_first_types + r, column_format, format);
            htmltable.setValue(row_first_types + r, column_total, table_3.getValue(r, 3));
            htmltable.setValueHtmlCode(row_first_types + r, column_total, "<A HREF=\"" + full_url + "\">", "</A>");
            types_to_id.put(types_concatenated, new Integer(r));
        }

        // by both
        int both_count = table_4.sizeRows();
        for (int r = 0; r < both_count; r++) {

            String program = (String) table_4.getValue(r, 0);
            String type = (String) table_4.getValue(r, 1);
            String subtype = (String) table_4.getValue(r, 2);
            String format = (String) table_4.getValue(r, 3);

            String types_concatenated = type + "_" + subtype + "_" + format;

            tmp_options = new HashMap(std_options);
            programs = new ArrayList();
            types = new ArrayList();
            subtypes = new ArrayList();
            ArrayList formats = new ArrayList();
            programs.add(program);
            types.add(type);
            subtypes.add(subtype);
            formats.add(format);
            tmp_options.put("programs", programs);
            tmp_options.put("types", types);
            tmp_options.put("subtypes", subtypes);
            tmp_options.put("formats", formats);
            // General.showOutput("tmp_options:" + tmp_options );
            full_url = base_url + "?" + getQueryUrl(tmp_options);
            // Default that will not bomb anything.
            int program_id = 0;
            try {
                program_id = ((Integer) program_name_to_id.get(program)).intValue();
            } catch (Exception e) {
                // This is normal behavior for db tuples that have no
                // corresponding program any more. These tuples will not be shown.
                continue;
            }
            int types_id = ((Integer) types_to_id.get(types_concatenated)).intValue();
            htmltable.setValue(row_first_types + types_id, column_first_program + program_id, table_4.getValue(r, 4));
            htmltable.setValueHtmlCode(row_first_types + types_id, column_first_program + program_id, "<A HREF=\""
                    + full_url + "\">", "</A>");
        }
        htmltable.setValueHtmlCodeByRow(row_total, "<B>", "</B>");
        htmltable.setValueHtmlCodeByColumn(column_total, "<B>", "</B>");

        htmltable.replaceStringByColumn(column_subtype, "n/a", "");
        htmltable.replaceStringByColumn(column_format, "n/a", "");

        // Decide on the print
        boolean show_csv = ((Boolean) options.get("show_csv")).booleanValue();
        if (show_csv) {
            out.println(htmltable.toCsv(true));
        } else {
            // If the parameter csv was present replace it's value otherwise
            // append the parameter/value pair preceded by the given String.
            // The extra '&' doesn't seem to matter at least
            // for MS Internet Explorer 5.5 (NT) and Netscape 6 (Linux)
            // Use regular expression to match.
            tmp_options = new HashMap(options);
            tmp_options.put("show_csv", new Boolean(true));
            full_url = base_url + "?" + getQueryUrl(tmp_options);

            out.println("<P>Result table");
            /**
             * out.println( "<A HREF=\"" + full_url + "\">"); out.println( "<img SRC=\"" + "/" +
             * g.getValueString("servlet_image_dir") + "/" + "expcsv.gif\" " +
             * //"alt=\"Save to comma-separated-values file\" " + "title=\"Save to comma-separated-values file\" " +
             * "width=\"23\" height=\"22\" border=\"0\">"); out.println(
             * "&nbsp;(Save to comma-separated-values file)</A>");
             */
            out.println("<BR>");
            out.println(htmltable.toHtml(true));
        }

        return true;
    }

    /**
     * Constructs a standardized SQL selection string to occur in the WHERE clause of queries (or somewhere else;-) in
     * stead of a stub code (1=1).
     *
     * @param options
     * @return SQL selection string for the given selections as defined in the arguments' hashmap.
     */
    public static String getQuerySql(HashMap options) {
        /**
         * Retrieve standard set off options cut and paste from routine: MRGridServlet.showSelectionTable. Don't change
         * these next block just anywhere.
         */
        // {
        // Object tmp = null;
        // String request_type = (String) options.get("request_type");
        // String db_username = (String) options.get("db_username");
        // boolean show_csv = false;
        // boolean show_blocks = true;
        // int verbosity = 2;
        // int min_items = Integer.parseInt(MIN_ITEMS_DEFAULT);
        // tmp = options.get("show_csv"); if ( tmp != null ) show_csv = ((Boolean) tmp).booleanValue();
        // tmp = options.get("show_blocks"); if ( tmp != null ) show_blocks = ((Boolean) tmp).booleanValue();
        // tmp = options.get("verbosity"); if ( tmp != null ) verbosity = ((Integer) tmp).intValue();
        // tmp = options.get("min_items"); if ( tmp != null ) min_items = ((Integer) tmp).intValue();
        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        ArrayList bmrb_ids = (ArrayList) options.get("bmrb_ids");
        ArrayList block_text_types = (ArrayList) options.get("block_text_types");
        ArrayList file_details = (ArrayList) options.get("file_details");

        ArrayList programs = (ArrayList) options.get("programs");
        ArrayList types = (ArrayList) options.get("types");
        ArrayList subtypes = (ArrayList) options.get("subtypes");
        ArrayList formats = (ArrayList) options.get("formats");

        ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");
        // }

        // Compile the extra selections for later replacement of the stub
        ArrayList sel = new ArrayList();
        StringBuffer selection_part = new StringBuffer();
        // PDB ID
        if (pdb_ids != null && (pdb_ids.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = pdb_ids.iterator(); i.hasNext();) {
                selection_part.append("e.pdb_id='" + i.next() + "' ");
                if (i.hasNext()) {
                    selection_part.append("OR\n");
                }
            }
            sel.add(selection_part.toString());
        }
        // BMRB ID
        if (bmrb_ids != null && (bmrb_ids.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = bmrb_ids.iterator(); i.hasNext();) {
                selection_part.append("e.bmrb_id=" + i.next() + " ");
                if (i.hasNext()) {
                    selection_part.append("OR\n");
                }
            }
            sel.add(selection_part.toString());
        }

        // Block text type
        if (block_text_types != null && (block_text_types.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = block_text_types.iterator(); i.hasNext();) {
                selection_part.append("b.text_type='" + i.next() + "' ");
                if (i.hasNext()) {
                    selection_part.append("OR\n");
                }
            }
            sel.add(selection_part.toString());
        }

        // File detail
        if (file_details != null && (file_details.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = file_details.iterator(); i.hasNext();) {
                selection_part.append("f.detail='" + i.next() + "' ");
                if (i.hasNext()) {
                    selection_part.append("OR\n");
                }
            }
            sel.add(selection_part.toString());
        }

        // Program
        if (programs != null && (programs.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = programs.iterator(); i.hasNext();) {
                selection_part.append("b.program='" + i.next() + "' ");
                if (i.hasNext())
                    selection_part.append("OR\n");
            }
            sel.add(selection_part.toString());
        }

        // Type
        if (types != null && (types.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = types.iterator(); i.hasNext();) {
                selection_part.append("b.type='" + i.next() + "' ");
                if (i.hasNext())
                    selection_part.append("OR\n");
            }
            sel.add(selection_part.toString());
        }

        // Subtypes
        if (subtypes != null && (subtypes.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = subtypes.iterator(); i.hasNext();) {
                selection_part.append("b.subtype='" + i.next() + "' ");
                if (i.hasNext())
                    selection_part.append("OR\n");
            }
            sel.add(selection_part.toString());
        }

        // Format
        if (formats != null && (formats.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = formats.iterator(); i.hasNext();) {
                selection_part.append("b.format='" + i.next() + "' ");
                if (i.hasNext())
                    selection_part.append("OR\n");
            }
            sel.add(selection_part.toString());
        }

        // MRBlockID
        if (mrblock_ids != null && (mrblock_ids.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = mrblock_ids.iterator(); i.hasNext();) {
                selection_part.append("b.mrblock_id='" + i.next() + "' ");
                if (i.hasNext())
                    selection_part.append("OR\n");
            }
            sel.add(selection_part.toString());
        }

        // Combine them all
        StringBuffer sel_sb = new StringBuffer();
        if (sel != null && sel.size() > 0) {
            for (Iterator i = sel.iterator(); i.hasNext();) {
                sel_sb.append("(" + i.next() + ") ");
                if (i.hasNext()) {
                    sel_sb.append("AND ");
                }
            }
        } else {
            // Insert bogus selection (always evaluates to true)
            sel_sb.append(SQL_Generic.STUB_SQL_STRING_TRUE);
        }

        String selection = sel_sb.toString();

        return selection;
    }

    /**
     * Replace the s at the end of some plural parameter names.
     */
    public static String getSingularParameterName(String par) {
        if (par.equals("types")) {
            par = "type";
        } else if (par.equals("subtypes")) {
            par = "subtype";
        } else if (par.equals("formats")) {
            par = "format";
        } else if (par.equals("programs")) {
            par = "program";
        } else if (par.equals("pdb_ids")) {
            par = "pdb_id";
        } else if (par.equals("bmrb_ids")) {
            par = "bmrb_id";
        } else if (par.equals("block_text_types")) {
            par = "block_text_type";
        } else if (par.equals("file_details")) {
            par = "file_detail";
        } else if (par.equals("mrblock_ids")) {
            par = "mrblock_id";
        }
        return par;
    }

    /**
     * Return text for hidden form input parameters derived from the given options. They're nicely sorted by
     * alphabetical ordering
     */
    public static String getQueryForm(HashMap options) {
        StringArrayList al = new StringArrayList(options.keySet());
        al.sort();
        String result = "";
        for (int i = 0; i < al.size(); i++) {
            Object key = al.get(i);
            Object value = options.get(key);
            String valueStr = value.toString();
            String keyStr = key.toString();
            keyStr = getSingularParameterName(keyStr);
            if (value instanceof ArrayList) {
                ArrayList valueArrayList = (ArrayList) value;
                for (int j = 0; j < valueArrayList.size(); j++) {
                    valueStr = valueArrayList.get(j).toString();
                    result += "<INPUT TYPE=hidden NAME=\"" + keyStr + "\" VALUE=\"" + valueStr + "\">\n";
                }
            } else {
                if (!valueIsEmpty(value)) {
                    result += "<INPUT TYPE=hidden NAME=\"" + keyStr + "\" VALUE=\"" + valueStr + "\">\n";
                }
            }
        }
        return result;
    }

    /**
     * Constructs a standardized URL selection string to occur in the query part of the URL (after the ?).
     *<P>
     * The following parameters are encoded/decoded before presented: request_type, block_text_type, file_details,
     * programs, types, subtypes, formats. The coding scheme is that of java.net.URLEncoder which mostly does the space
     * character ' ' to '+'.
     */
    public static String getQueryUrl(HashMap options) {
        /**
         * Retrieve standard set off options cut and paste from routine: MRGridServlet.showSelectionTable. Don't change
         * these next block just anywhere.
         */
        // {
        Object tmp = null;

        String request_type = (String) options.get("request_type");
        String db_username = (String) options.get("db_username");
        boolean show_csv = false;
        // boolean show_blocks = false;
        int verbosity = 2;
        // int min_items = Integer.parseInt(MIN_ITEMS_DEFAULT);

        tmp = options.get("show_csv");
        if (tmp != null)
            show_csv = ((Boolean) tmp).booleanValue();
        // tmp = options.get("show_blocks"); if ( tmp != null ) show_blocks = ((Boolean) tmp).booleanValue();
        tmp = options.get("verbosity");
        if (tmp != null)
            verbosity = ((Integer) tmp).intValue();
        // tmp = options.get("min_items"); if ( tmp != null ) min_items = ((Integer) tmp).intValue();

        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        ArrayList bmrb_ids = (ArrayList) options.get("bmrb_ids");
        ArrayList block_text_types = (ArrayList) options.get("block_text_types");
        ArrayList file_details = (ArrayList) options.get("file_details");

        ArrayList programs = (ArrayList) options.get("programs");
        ArrayList types = (ArrayList) options.get("types");
        ArrayList subtypes = (ArrayList) options.get("subtypes");
        ArrayList formats = (ArrayList) options.get("formats");

        ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");
        // }

        /*
         * Compile the extra selections for later replacement of the stub.Leaving out those selections which are
         * standard, e.g. show_csv=false
         */

        StringArrayList sel = new StringArrayList();
        StringBuffer selection_part = new StringBuffer();

        if (request_type != null)
            sel.add("request_type=" + encodeStringForUrl(request_type));

        if (db_username != null)
            sel.add("db_username=" + encodeStringForUrl(db_username));

        // if ( show_blocks )
        // sel.add("show_blocks=true");

        if (show_csv)
            sel.add("show_csv=true");

        if (verbosity >= 0 && verbosity <= 9 && verbosity != 2)
            sel.add("verbosity=" + verbosity);

        // PDB ID
        if (pdb_ids != null && (pdb_ids.size() > 0)) {
            selection_part.setLength(0);
            selection_part.append("pdb_id=");
            for (Iterator i = pdb_ids.iterator(); i.hasNext();) {
                selection_part.append(i.next());
                if (i.hasNext()) {
                    selection_part.append(",");
                }
            }
            sel.add(selection_part.toString());
        }
        // BMRB ID
        if (bmrb_ids != null && (bmrb_ids.size() > 0)) {
            selection_part.setLength(0);
            selection_part.append("bmrb_id=");
            for (Iterator i = bmrb_ids.iterator(); i.hasNext();) {
                selection_part.append(i.next());
                if (i.hasNext()) {
                    selection_part.append(",");
                }
            }
            sel.add(selection_part.toString());
        }

        // Block text type
        if (block_text_types != null && (block_text_types.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = block_text_types.iterator(); i.hasNext();) {
                selection_part.append("block_text_type=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("&");
                }
            }
            sel.add(selection_part.toString());
        }

        // File detail
        if (file_details != null && (file_details.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = file_details.iterator(); i.hasNext();) {
                selection_part.append("file_detail=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // Program
        if (programs != null && (programs.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = programs.iterator(); i.hasNext();) {
                selection_part.append("program=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // Type
        if (types != null && (types.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = types.iterator(); i.hasNext();) {
                selection_part.append("type=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // Subtype
        if (subtypes != null && (subtypes.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = subtypes.iterator(); i.hasNext();) {
                selection_part.append("subtype=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // Format
        if (formats != null && (formats.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = formats.iterator(); i.hasNext();) {
                selection_part.append("format=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // MRBlock_id
        if (mrblock_ids != null && (mrblock_ids.size() > 0)) {
            selection_part.setLength(0);
            for (Iterator i = mrblock_ids.iterator(); i.hasNext();) {
                selection_part.append("mrblock_id=" + encodeStringForUrl(i.next().toString()));
                if (i.hasNext()) {
                    selection_part.append("?");
                }
            }
            sel.add(selection_part.toString());
        }

        // Combine them all
        StringBuffer sel_sb = new StringBuffer();
        if (sel != null && sel.size() > 0) {
            // sel_sb.append("?");
            sel.sort();
            for (Iterator i = sel.iterator(); i.hasNext();) {
                sel_sb.append(i.next());
                if (i.hasNext())
                    sel_sb.append("&");
            }
        }

        String selection = sel_sb.toString();
        // General.showOutput("Query Url : ["+selection+"]");
        return selection;
    }

    /**
     * Get and check multiple selection options multiple aspect not all implemented yet.
     *
     * @param out
     * @param options
     * @return <CODE>true</CODE> for success.
     */
    protected boolean showSelectionTable(PrintWriter out, HashMap options) {
        /**
         * Retrieve standard set off options cut and paste from routine: MRGridServlet.showSelectionTable. Don't change
         * these next block just anywhere.
         */
        // {
        Object tmp = null;

        // String request_type = (String) options.get("request_type");
        String db_username = (String) options.get("db_username");
        // boolean show_csv = false;
        // boolean show_blocks = false;
        // int verbosity = 2;
        int min_items = Integer.parseInt(MIN_ITEMS_DEFAULT);

        // tmp = options.get("show_csv"); if ( tmp != null ) show_csv = ((Boolean) tmp).booleanValue();
        // tmp = options.get("show_blocks"); if ( tmp != null ) show_blocks = ((Boolean) tmp).booleanValue();
        // tmp = options.get("verbosity"); if ( tmp != null ) verbosity = ((Integer) tmp).intValue();
        tmp = options.get("min_items");
        if (tmp != null)
            min_items = ((Integer) tmp).intValue();

        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        ArrayList bmrb_ids = (ArrayList) options.get("bmrb_ids");
        ArrayList block_text_types = (ArrayList) options.get("block_text_types");
        // ArrayList file_details = (ArrayList) options.get("file_details");

        // ArrayList programs = (ArrayList) options.get("programs");
        // ArrayList types = (ArrayList) options.get("types");
        // ArrayList subtypes = (ArrayList) options.get("subtypes");
        // ArrayList formats = (ArrayList) options.get("formats");
        //
        // ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");
        // }

        // Default settings
        String pdb_id_default = NOT_A_SELECTION_STRING;
        String bmrb_id_default = NOT_A_SELECTION_STRING;
        String block_text_type_default = NOT_A_SELECTION_STRING;
        String min_items_default = MIN_ITEMS_DEFAULT;
        // boolean show_blocks_default = false;

        // Choose previous selection as it came in
        if (pdb_ids.size() > 0) {
            StringArrayList pdb_ids_sal = new StringArrayList(pdb_ids);
            pdb_id_default = Strings.toString(pdb_ids_sal.toStringArray(), false, false, ",");
        }
        if (bmrb_ids.size() > 0) {
            StringArrayList bmrb_ids_sal = new StringArrayList(bmrb_ids);
            bmrb_id_default = Strings.toString(bmrb_ids_sal.toStringArray(), false, false, ",");
        }

        if (block_text_types.size() > 0) {
            block_text_type_default = (String) block_text_types.get(0);
        }
        min_items_default = Integer.toString(min_items);

        // Possible options
        ArrayList block_text_type_options = new ArrayList();
        ArrayList file_detail_options = new ArrayList();
        block_text_type_options.add(NOT_A_SELECTION_STRING);
        file_detail_options.add(NOT_A_SELECTION_STRING);

        // Try to populate from DB
        ArrayList block_text_type_options_db = sql_epiII.getMRBlockTextTypes();
        if (block_text_type_options_db != null) {
            // Append them to the end of the list.
            block_text_type_options.addAll(block_text_type_options.size(), block_text_type_options_db);
        } else {
            General.showError("DB access error from showSelectionTable (getMRBlockTextTypes)");
            return false;
        }

        /**
         * Form method get is best suited for - inexperienced programmers like me and - performance of small forms with
         * a few short fields. The standard encoding format "application/x-www-form-urlencode" is fine for this
         * application so no need to set the enctype attribute of the form tag.
         */
        out.println("<FORM METHOD=\"GET\" ACTION=\"" + g.getValueString("MRGridServlet") + "\">");

        StringBuffer cell_value = new StringBuffer();

        int counter = 0;
        int row_pdb_id = counter++;
        int row_bmrb_id = counter++;
        int row_block_text_type = counter++;
        // int row_file_detail = counter++;
        // int row_show_blocks = counter++;
        int row_min_items = counter++;
        int row_form_buttons = counter++; // needs to be last or change code.

        int column_prompt = 0;
        int column_input = 1;
        int column_help = 2; // needs to be last or change code.

        HtmlTable htmltable = new HtmlTable(row_form_buttons + 1, column_help + 1);
        htmltable.setCommonDefaults();
        // Reset border though
        htmltable.table_attributes.setProperty("border", "0");

        // Cell Attributes
        htmltable.setCellAttributeByColumn(column_prompt, "align", "\"Right\"");
        htmltable.setCellAttributeByColumn(column_input, "align", "\"Left\"");
        htmltable.setCellAttributeByColumn(column_help, "align", "\"Left\"");
        htmltable.setCellAttributeByColumn(column_prompt, "width", "\"25%\"");
        htmltable.setCellAttributeByColumn(column_input, "width", "\"15%\"");
        htmltable.setCellAttributeByColumn(column_help, "width", "\"60%\"");

        // Cell values

        // This will initialize the table values to something that will work with rowspan.
        String html_location = g.getValueString("servlet_html_dir");

        htmltable.setValueByTable(HtmlTable.EMPTY_CELL_HTML_TABLE);
        htmltable.setValueByColumn(column_help, HtmlTable.NOT_PRINTABLE_CELL_HTML_TABLE);

        htmltable.setValue(row_pdb_id, column_prompt, "PDB id (or list)");
        /**
         * htmltable.setValue(row_pdb_id, column_input, "<TEXTAREA name=\"pdb_id\""+ "\" rows=\"1\" cols=\"9\">" +
         * pdb_id_default + "</TEXTAREA>" );
         */
        htmltable.setValue(row_pdb_id, column_input, "<input type=\"text\" name=\"pdb_id\" value=\"" + pdb_id_default
                + "\" size=\"9\">");
        htmltable
                .setValue(
                        row_pdb_id,
                        column_help,

                        "<P><B>What is the NMR Restraints Grid?</B>\n"
                                + "<p>The NMR Restraints Grid contains the original NMR data as collected for over 2500\n"
                                + "protein and nucleic acid structures with corresponding PDB entries.\n"
                                + "In addition to the original restraints, most of the distance, dihedral angle and RDC restraint data (>85%)\n"
                                + "were parsed, and those in over 500 entries were converted\n"
                                + "and filtered. The converted and filtered data sets constitute the \n"
                                + "DOCR and FRED databases respectively as described in these\n"
                                + "<A HREF=\"/"
                                + html_location
                                + "/howto.html#References\">references</A>.\n"
                                +

                                "<P>For tips on using this interface and how to link to it, check the\n"
                                + "<A HREF=\"/"
                                + html_location
                                + "/howto.html\">howto</A>.\n"
                                + "A block is a section of data of similar type such as hydrogen bond distance restraints or RDCs.\n"
                                + General.eol);
        htmltable.setCellAttribute(row_pdb_id, column_help, "rowspan", String.valueOf(htmltable.sizeRows()));
        htmltable.setCellAttribute(row_pdb_id, column_help, "valign", "Top");

        htmltable.setValue(row_bmrb_id, column_prompt, "BMRB id (or list)");
        htmltable.setValue(row_bmrb_id, column_input, "<input type=\"text\" name=\"bmrb_id\" value=\""
                + bmrb_id_default + "\" size=\"9\">");

        htmltable.setValue(row_block_text_type, column_prompt, "Stage");
        cell_value = new StringBuffer();
        int maxOptionItems = block_text_type_options.size();
        cell_value.append("<select name=\"block_text_type\" multiple size=" + maxOptionItems + ">");
        for (Iterator i = block_text_type_options.iterator(); i.hasNext();) {
            String option_name = (String) i.next();
            if (option_name.equals(block_text_type_default) || block_text_types.contains(option_name)) {
                cell_value.append("<option selected=\"\">" + option_name + "</option>");
            } else {
                cell_value.append("<option              >" + option_name + "</option>");
            }
        }
        cell_value.append("</select>");
        htmltable.setValue(row_block_text_type, column_input, cell_value.toString());

        htmltable.setValue(row_min_items, column_prompt,
                "Hide the grouped block counts for the software formats if there are less blocks than:");
        htmltable.setValue(row_min_items, column_input, "<input type=\"text\" name=\"min_items\" value=\""
                + min_items_default + "\" size=\"4\">");

        htmltable.setValue(row_form_buttons, column_input, "<INPUT TYPE=\"submit\" VALUE=\"Submit\">");
        out.println(htmltable.toHtml(false));
        if (db_username != null) {
            out.println("<INPUT type=hidden name=db_username value=" + encodeStringForUrl(db_username));
        }
        out.println("</FORM>");
        return true;
    }

    protected static boolean valueIsEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof Boolean) {
            return false;
        }

        if (o instanceof Integer) {
            return false;
        }

        if (o instanceof ArrayList) {
            ArrayList a = (ArrayList) o;
            if (a.size() > 0) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Get and check selection options. Combinations of options don't always make sense. The code will determine what is
     * returned, mostly based on the parameter request_type.
     *
     * @param req
     * @param resp
     * @param options
     * @throws ServletException
     * @throws IOException
     * @return true for success.
     */
    protected boolean processOptions(HttpServletRequest req, HttpServletResponse resp, HashMap options)
            throws ServletException, java.io.IOException {

        // Just store the query string to make the calls a little more simpler
        // by reducing the parameter relaying. Not actually used anymore.
        // options.put("query_string", req.getQueryString());

        options.put("request_type", "grid");
        options.put("db_username", DEFAULT_DB_USERNAME);
        // options.put("show_blocks", new Boolean(false));
        options.put("show_csv", new Boolean(false));
        options.put("verbosity", new Integer(2));
        options.put("min_items", new Integer(MIN_ITEMS_DEFAULT));
        options.put("page_id", new Integer(1)); // For results which page are we going to be on.

        // Setup to handle multiple selections in the future.
        options.put("pdb_ids", new ArrayList());
        options.put("bmrb_ids", new ArrayList());
        options.put("block_text_types", new ArrayList());
        options.put("file_details", new ArrayList());

        // Setup to handle multiple selections in the future.
        options.put("programs", new ArrayList());
        options.put("types", new ArrayList());
        options.put("subtypes", new ArrayList());
        options.put("formats", new ArrayList());

        // Setup to handle multiple selections in the future.
        options.put("mrblock_ids", new ArrayList());

        // The following are not implemented as arrays because the getParameter
        // function was temporarily put on the depreciated list by Sun
        // and gave compilatin warnings.
        String[] request_type = req.getParameterValues("request_type");
        String[] db_username = req.getParameterValues("db_username");
        // String[] show_blocks_str = req.getParameterValues("show_blocks");
        String[] show_csv_str = req.getParameterValues("show_csv");
        String[] verbosity_str = req.getParameterValues("verbosity");
        String[] min_items_str = req.getParameterValues("min_items");
        String[] page_id_str = req.getParameterValues("page_id");

        String[] pdb_id = req.getParameterValues("pdb_id");
        String[] bmrb_id_str = req.getParameterValues("bmrb_id");
        String[] block_text_type = req.getParameterValues("block_text_type");
        // String[] file_detail = req.getParameterValues("file_detail");

        String[] program = req.getParameterValues("program");
        String[] type = req.getParameterValues("type");
        String[] subtype = req.getParameterValues("subtype");
        String[] format = req.getParameterValues("format");

        String[] mrblock_id_str = req.getParameterValues("mrblock_id");

        ArrayList pdb_ids = (ArrayList) options.get("pdb_ids");
        ArrayList bmrb_ids = (ArrayList) options.get("bmrb_ids");
        ArrayList block_text_types = (ArrayList) options.get("block_text_types");
        ArrayList file_details = (ArrayList) options.get("file_details");

        ArrayList programs = (ArrayList) options.get("programs");
        ArrayList types = (ArrayList) options.get("types");
        ArrayList subtypes = (ArrayList) options.get("subtypes");
        ArrayList formats = (ArrayList) options.get("formats");

        ArrayList mrblock_ids = (ArrayList) options.get("mrblock_ids");

        // Show query form and results by default
        if (request_type != null && (request_type.length > 0))
            options.put("request_type", request_type[0]);

        // Show the default database by default (duh)
        if (db_username != null && (db_username.length > 0))
            options.put("db_username", db_username[0]);

        // Display files(default) or blocks?
        // if (show_blocks_str != null && (show_blocks_str.length > 0) &&
        // show_blocks_str[0].equals(Defs.STRING_TRUE))
        // options.put("show_blocks", new Boolean(true));
        if (show_csv_str != null && (show_csv_str.length > 0) && show_csv_str[0].equals(Defs.STRING_TRUE))
            options.put("show_csv", new Boolean(true));

        // VERBOSITY
        try {
            if (verbosity_str != null && (verbosity_str.length > 0)) {
                int verbosity = Integer.parseInt(verbosity_str[0]);
                if (verbosity >= 0 && verbosity <= 9)
                    options.put("verbosity", new Integer(verbosity));
            }
        } catch (NumberFormatException e) {
            ;
        }

        // MIN_ITEMS
        try {
            if (min_items_str != null && (min_items_str.length > 0)) {
                int min_items = Integer.parseInt(min_items_str[0]);
                if (min_items >= 0 && min_items <= 999999)
                    options.put("min_items", new Integer(min_items));
            }
        } catch (NumberFormatException e) {
            ;
        }

        // PAGE_ID
        try {
            if (page_id_str != null && (page_id_str.length > 0)) {
                int page_id = Integer.parseInt(page_id_str[0]);
                if (page_id > 0 && page_id <= 999999)
                    options.put("page_id", new Integer(page_id));
            }
        } catch (NumberFormatException e) {
            ;
        }

        // PDB ID
        if (pdb_id != null) {
            pdb_id = Strings.splitAllNoEmpties(pdb_id, " ");
            for (int i = 0; i < pdb_id.length; i++) {
                if (pdb_id[i].equals(NOT_A_SELECTION_STRING)) {
                    continue;
                }
                pdb_id[i] = Strings.replace(pdb_id[i].toLowerCase().trim(), "['\"\\s,<>]", " ");
                if (!Strings.is_pdb_code(pdb_id[i])) {
                    showActualError(resp, "Parameters: pdb_id failed to resolve to a valid pdb code or is ''.\n"
                            + "Does it conform to the regular expression pattern: "
                            + "<PRE>^[:digit:][:alnum:]{3}$</PRE><BR>\n" + "Value give was: [" + pdb_id[i]
                            + "] (put in lower case and trimmed for spurious white spaces).");
                    return false;
                }
                pdb_ids.add(pdb_id[i]);
            }
        }

        // BMRB ID
        if (bmrb_id_str != null) {
            bmrb_id_str = Strings.splitAllNoEmpties(bmrb_id_str, " ");
            for (int i = 0; i < bmrb_id_str.length; i++) {
                String bmrb_id = Strings.replace(bmrb_id_str[i], "['\"\\s,<>]", " ");
                if (!bmrb_id.equals(NOT_A_SELECTION_STRING)) {
                    bmrb_id = bmrb_id.trim();
                    if (!Strings.is_bmrb_code(bmrb_id)) {
                        showActualError(resp, "Parameters: bmrb_id failed to resolve to a valid bmrb code.\n"
                                + "Value give was: [" + bmrb_id + "] (trimmed for spurious white spaces).");
                        return false;
                    }
                    bmrb_ids.add(bmrb_id);
                }
            }
        }
        // BLOCK TEXT TYPE
        if (block_text_type != null) {
            for (int i = 0; i < block_text_type.length; i++) {
                String value = decodeStringFromUrl(block_text_type[i]);
                value = Strings.replace(value, "['\"\\s,<>]", " ");
                General.showDebug("block_text_type[i] :" + value);
                if (!value.equals(NOT_A_SELECTION_STRING)) {
                    block_text_types.add(value);
                }
            }
        }
        // FILE DETAIL
        /**
         * if ( file_detail != null) { for ( int i=0;i<file_detail.length;i++ ) { String value =
         * decodeStringFromUrl(file_detail[i]); General.showDebug("file_detail[i] :"+value ); if ( !
         * value.equals(NOT_A_SELECTION_STRING) ) file_details.add( value ); } }
         */
        file_details.addAll(block_text_types);

        // PROGRAM
        if (program != null) {
            for (int i = 0; i < program.length; i++) {
                String value = decodeStringFromUrl(program[i]);
                value = Strings.replace(value, "['\"\\s,<>]", " ");
                General.showDebug("program[i] :" + value);
                if (!value.equals(NOT_A_SELECTION_STRING))
                    programs.add(value);
            }
        }
        // TYPE
        if (type != null) {
            for (int i = 0; i < type.length; i++) {
                String value = decodeStringFromUrl(type[i]);
                value = Strings.replace(value, "['\"\\s,<>]", " ");
                General.showDebug("type[i] :" + value);
                if (!value.equals(NOT_A_SELECTION_STRING))
                    types.add(value);
            }
        }
        // SUBTYPE
        if (subtype != null) {
            for (int i = 0; i < subtype.length; i++) {
                String value = decodeStringFromUrl(subtype[i]);
                value = Strings.replace(value, "['\"\\s,<>]", " ");
                General.showDebug("subtype[i] :" + value);
                if (!value.equals(NOT_A_SELECTION_STRING))
                    subtypes.add(value);
            }
        }
        // FORMAT
        if (format != null) {
            for (int i = 0; i < format.length; i++) {
                String value = decodeStringFromUrl(format[i]);
                value = Strings.replace(value, "['\"\\s,<>]", " ");
                General.showDebug("format[i] :" + value);
                if (!value.equals(NOT_A_SELECTION_STRING))
                    formats.add(value);
            }
        }

        // MRBLOCK_ID
        if (mrblock_id_str != null) {
            for (int i = 0; i < mrblock_id_str.length; i++) {
                if (!mrblock_id_str[i].equals(NOT_A_SELECTION_STRING)) {
                    int mrblock_id = 0;
                    try {
                        mrblock_id = Integer.parseInt(mrblock_id_str[i]);
                    } catch (NumberFormatException e) {
                        showActualError(resp, "Parameter: mrblock_id [" + Strings.replace(mrblock_id_str[i], "['\"\\s,<>]", " ")
                                + "] failed to resolve to a positive integer value.\n" + "Please check.");
                        return false;
                    }
                    if (mrblock_id < 1) {
                        showActualError(resp, "Parameter: mrblock_id given was negative. Please correct.\n");
                        return false;
                    }
                    if (mrblock_id > 9999999) {
                        showActualError(resp, "Parameter: mrblock_id given was larger than 9999999. Please correct.\n");
                        return false;
                    }
                    mrblock_ids.add(new Integer(mrblock_id));
                }
            }
        }
        return true;
    }

    /** Handles the HTTP <code>GET</code> method. */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            java.io.IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method. */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            java.io.IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return (g.getValueString("MRGridServlet") + " servlet from BMRB");
    }

    public static String encodeStringForUrl(String in) {
        try {
            return java.net.URLEncoder.encode(in, "UTF-8");
        } catch (Throwable t) {
            General.showThrowable(t);
        }
        return null;
    }

    public static String decodeStringFromUrl(String in) {
        try {
            return java.net.URLDecoder.decode(in, "UTF-8");
        } catch (Throwable t) {
            General.showThrowable(t);
        }
        return null;
    }
}
