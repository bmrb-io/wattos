package Wattos.Star;

import Wattos.Database.*;
import java.util.*;
import java.io.*;
import java.net.*;
import Wattos.Utils.*;

/**
 * Reader for star files. Most of code from Dimitri Maziuk, BMRB. Some code taken from John L. Moreland
 * (http://mbt.sdsc.edu) Note that the underlying STAR Lexer is very inefficient for data with semicolon quoted texts
 * found out on 2006-08-01. Latest update: 2003-10-03 Prints out tokens.
 * 
 * @author dmaziuk
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class StarFileReader implements ErrorHandler, ContentHandler {

    StarNode topNode = null;
    StarNode upperNode = null; // Can be null (in case parsing of items outside of sf) or data node (in case of parsing
    // items in sf).
    StarNode currentNode = null; // Can be data node or saveframe.
    TagTable currentTt = null;
    DBMS dbms = null;

    /** Stupid limitation of 100 prevented reading mmCIF */
    private static final int MAX_NUMBER_COLUMNS = 100*100;

    protected String urlString = null;
    // private long expectedInputBytes = 1;

    /** physical row that has some space free */
    int nextFreeRow;
    /**
     * physical row that was used before nextFreeRow. It's important that it be an existing/valid row like the first one
     * in this case.
     */
    int previousFreeRow;
    /** Number of rows that have been allocated so far. Rows are numbered starting at 0. */
    int nextFreeRowCount;
    /** Column that has some space free, excluding the first column which is kept for the order. */
    int nextFreeColumn;
    /** index of the last filled column */
    int lastColumnIdx;
    int[] orderColumn;
    String[][] valueTable;
    StringSet[] valueTableSet;

    public StarFileReader(DBMS dbms) {
        init(dbms);
    }

    public boolean init(DBMS dbms) {
        // General.setVerbosityToDebug();
        this.dbms = dbms;
        topNode = new StarNode();
        currentNode = topNode;
        topNode.general.setStarFlavor(StarGeneral.STANDARD_FLAVOR_CIF); // Remains unless _stop codon observed.
        valueTable = null;
        valueTableSet = null;
        boolean status = initTagTableVariables();
        return status;
    }

    public boolean initTagTableVariables() {
        nextFreeRow = 0;
        previousFreeRow = 0;
        nextFreeRowCount = 0;
        nextFreeColumn = 1;
        return true;
    }

    public void updateCachedArrays() {
        // General.showDebug("updating the cached array references");
        orderColumn = (int[]) currentTt.attr.get(TagTable.DEFAULT_ATTRIBUTE_ORDER_ID);
        for (int c = 1; c <= lastColumnIdx; c++) {
            valueTable[c] = (String[]) currentTt.getColumn(c);
        }
        // valueTableSet doesn't need updating.
    }

    /** Returns true for success */
    public boolean createNewTable() {
        // General.showDebug("CREATING NEW TAG TABLE");
        try {
            currentTt = new TagTable(dbms.getNextRelationName(), dbms);
        } catch (Exception e) {
            General.showThrowable(e);
            General.showError("failed to initialize a tagtable.");
            return false;
        }
        // General.showDebug("New tT name: " + currentTt.name);
        orderColumn = (int[]) currentTt.attr.get(TagTable.DEFAULT_ATTRIBUTE_ORDER_ID);
        lastColumnIdx = 0;
        currentTt.parent = currentNode;
        currentNode.datanodes.add(currentTt);
        // General.showDebug("New tT added to parent with name: " + currentNode.title);
        initTagTableVariables();
        valueTable = new String[MAX_NUMBER_COLUMNS][];
        valueTableSet = new StringSet[MAX_NUMBER_COLUMNS];
        return true;
    }

    public boolean startData(int line, String id) {
        // General.showOutput( line + ": start of data block " + id );
        upperNode = currentNode;
        currentNode = new DataBlock();
        currentNode.title = id;
        currentNode.setStarParent(upperNode);
        upperNode.datanodes.add(currentNode);
        return false;
    }

    /**
     * Called on EOF.
     * 
     * @param line
     *            line number
     * @param id
     *            block id
     */
    public boolean endData(int line, String id) {
        // General.showOutput( line + ": end of data block " + id );
        return false;
    }

    /**
     * Called on start of saveframe (parser encounters save_<name>).
     * 
     * @param line
     *            line number
     * @param name
     *            saveframe name
     * @return true to stop parsing
     */
    public boolean startSaveFrame(int line, String name) {
        // General.showOutput( line + ": start of saveframe " + name );
        // Switch into new mode.
        upperNode = currentNode;
        currentNode = new SaveFrame();
        currentNode.title = name;
        currentNode.setStarParent(upperNode);
        upperNode.datanodes.add(currentNode);
        return false;
    }

    /**
     * Called on end of saveframe (parser encounters save_).
     * 
     * @param line
     *            line number
     * @param name
     *            saveframe name
     * @return true to stop parsing
     */
    public boolean endSaveFrame(int line, String name) {
        // General.showOutput( line + ": end of saveframe " + name );
        currentNode = upperNode;
        upperNode = null;
        /**
         * Current tag table needs to be nilled at end in order for a new one to be created in a new saveframe; JFD adds
         * 2003-10-03
         */
        currentTt = null;
        return false;
    }

    /**
     * Called when parser encounters a comment.
     * 
     * @param line
     *            line number
     * @param text
     *            comment text
     * @return true to stop parsing
     */
    public boolean comment(int line, String text) {
        // General.showOutput( line + ": comment " + text );
        return false;
    }

    /**
     * Called when parser encounters a tag name.
     * 
     * @return true to stop parsing
     */
    public boolean tagName(int line, String text) {
//        General.showDebug(line + ": tag name " + text);
        // Start a new table if needed.
        if ((currentTt == null) || ((!currentTt.isFree) && (currentTt.sizeRows != 0))) {
//            General.showDebug(line
//                    + ": creating a new tagtable because no tT present yet or after some special condition");
            createNewTable();
        }
        // Add the column to the tagtable using the tag name as the column label.
        currentTt.insertColumn(text, TagTable.DATA_TYPE_STRINGNR, null);
        lastColumnIdx++;
//        General.showDebug("NEW: lastColumnIdx            : " + lastColumnIdx);
        valueTable[lastColumnIdx] = (String[]) currentTt.attr.get(text);
        valueTableSet[lastColumnIdx] = currentTt.getColumnStringSet(text);
        return false;
    }

    /**
     * Called on start of loop (parser encounters loop_).
     * 
     * @param line
     *            line number
     * @return true to stop parsing
     */
    public boolean startLoop(int line) {
        // General.showOutput( line + ": start of loop" );
        // Always create an empty tag table.
        createNewTable();
        currentTt.isFree = false;
        return false;
    }

    /**
     * Called on end of loop (parser encounters stop_). Actually doesn't happen with CIF files so it's ignored for STAR
     * files too.
     * 
     * @param line
     *            line number
     * @return true to stop parsing
     */
    public boolean endLoop(int line) {
        // General.showOutput( line + ": end of loop" );
        // General.showDebug("2: isFree: " + currentTt.isFree );
        // A way to guess what the STAR flavor should be is based on the
        // presence of the _stop tag.
        topNode.general.setStarFlavor(StarGeneral.STANDARD_FLAVOR_NMRSTAR);
        return false;
    }

    /**
     * Called when parser encounters a tag-value pair. Note that parser returns a "fake" DataItemNode for values in a
     * loop.
     * 
     * @param currentValue
     *            data
     * @return true to stop parsing
     */
    public boolean data(String currentValue) {
        // General.showDebug("I got value: " + currentValue);
        /**
         * Get new row; Also add the order id
         */
        if (nextFreeColumn == 1) {
            int currentRowSizeMax = currentTt.sizeMax;
            previousFreeRow = nextFreeRow;
            // Will automatically reserve 100 new rows if running out.
            nextFreeRow = currentTt.getNextReservedRow(previousFreeRow);
            if (nextFreeRow == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                nextFreeRow = currentTt.getNextReservedRow(previousFreeRow); // now it should be fine.
                // NO ACTION TAKEN ON THIS CLAUSE AT THE MOMENT!
            }
            if (nextFreeRow == -1) {
                General.doCodeBugExit("There's a bug in the relation getNextReservedRow routine");
                return true;
            }

            // Update the cached arrays if the size changed. And set the order id (which is the only integer typed
            // column at this point
            if (currentTt.sizeMax != currentRowSizeMax) {
                updateCachedArrays();
            }
            orderColumn[nextFreeRow] = nextFreeRowCount;
            // General.showDebug("started a new row: " + nextFreeRowCount + " and data item is: " + currentValue);
            nextFreeRowCount++;
        }

        /**
         * Optimized so we don't store different objects for the case where the string is the same as the string in the
         * same column on the previous row. This speeds things up enormously and uses less memory. Can be speeded up
         * further... previousFreeRow is zero for the first row so it will return the standard value (null).
         */

        String previousValue = valueTable[nextFreeColumn][previousFreeRow];
        if (previousValue != null && previousValue.equals(currentValue)) {
            // General.showDebug("Used previous string: " + previousValue);
            valueTable[nextFreeColumn][nextFreeRow] = previousValue;
        } else {
            valueTable[nextFreeColumn][nextFreeRow] = valueTableSet[nextFreeColumn].intern(currentValue);
        }

        // General.showDebug("add value for row, column: " + nextFreeRow + " " + nextFreeColumn + " " +
        // valueTable[nextFreeColumn][nextFreeRow]);
        nextFreeColumn++;
        // Wrap column index if needed.
        if (nextFreeColumn > lastColumnIdx) {
            if (!currentTt.isFree) {
                // Don't wrap the column index for free tables; more might be coming.
                nextFreeColumn = 1;
            }
        }
        return false;
    }

    /**
     * Called when parser encounters a possible error
     * 
     * @param line
     *            line number
     * @param col
     *            column number
     * @param msg
     *            error message
     * @return true signals parser to stop parsing
     */
    public boolean warning(int line, int col, String msg) {
        General.showError("WARN:" + line + ":" + col + ":" + msg);
        return false;
    }

    /**
     * Called when parser encounters an error.
     * 
     * @param line
     *            line number
     * @param col
     *            column number
     * @param msg
     *            error message
     */
    public void error(int line, int col, String msg) {
        General.showError("ERR:" + line + ":" + col + ":" + msg);
    }

    /** Parse the file */
    public StarNode parse(URL url) {

        if (!InOut.availableUrl(url)) {
            General.showWarning("Url doesn't exist for name: " + url.toString());
            return null;
        }

        if (!hasCorrrectExtension(url)) {
            General.showWarning("Url doesn't have correct extention for name: " + url.toString());
            return null;
        }
        BufferedInputStream bufferedInputStream = InOut.getBufferedInputStream(url);
        return parse(bufferedInputStream);
    }

    /**
     * Convenience method.
     */
    public StarNode parse(String buf) {
        try {
            buf = Strings.dos2unix(buf); // \r\n -> \n
            buf = Strings.mac2unix(buf); // \r -> \n
            StringReader sr = new StringReader(buf);
            STARLexer lex = new STARLexer(sr);
            STARParser parser = new STARParser(lex);
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            if (!parser.parse()) {
                General.showError("Parsing error");
                return null;
            }
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }
        boolean status = postProcess();
        if (!status) {
            General.showError("Failed to post process after reading the STAR file");
            return null;
        }
        return topNode;

    }

    /**
     * Parse the buffered inputstream by first putting it into memory.
     */
    public StarNode parse(BufferedInputStream inputStream) {
        String buf = InOut.readTextFromInputStream(inputStream);
        return parse(buf);
    }

    /**
     * For now only the cancelation of all reserved rows is needed.
     */
    public boolean postProcess() {
        // Get a list of all TagTables
        ArrayList list = topNode.getTagTableList(StarGeneral.WILDCARD, StarGeneral.WILDCARD, StarGeneral.WILDCARD,
                StarGeneral.WILDCARD);
        if (list == null) {
            General.showWarning("Found no tag tables; unusual");
            return true;
        }
        // General.showDebug("Found number of tables: " + list.size());
        int i = list.size() - 1;
        while (i >= 0) {
            TagTable tT = (TagTable) list.get(i);
            boolean status = tT.cancelAllReservedRows();
            if (!status) {
                General.showCodeBug("Failed to cancel reserved rows on relation: " + tT.toString());
                return false;
            }
            i--;
        }
        return true;
    }

    /**
     * Returns true if the loader is capable of loading the resource, or false otherwise.
     */
    public boolean hasCorrrectExtension(URL url) {
        String name = url.toString();
        // General.showDebug("StarFileReader.hasCorrrectExtension(String): " + name);
        if (!(name.endsWith(".str") || name.endsWith(".str.gz") || name.endsWith(".star") || name.endsWith(".star.gz")
                || name.endsWith(".txt") || name.endsWith(".txt.gz") || name.endsWith(".cif")
                || name.endsWith(".cif.gz") || name.endsWith(".CIF") || name.endsWith(".CIF.gz"))) {
            General.showDebug("can't load a star file with name: " + name);
            General.showDebug("it has to end with str, star, cif, or CIF");
            return false;
        }
        return true;
    }
}
