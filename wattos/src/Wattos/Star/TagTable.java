/*
 * TagTable.java
 *
 * Created on April 25, 2003, 1:22 PM
 */

package Wattos.Star;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Star.MmCif.CIFCoord;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringSet;
import Wattos.Utils.Strings;


/**
 * The tagtable is an extension of a relation. Most of code in this package is in
 *this class.
 *@see Wattos.Database.Relation
 * @author Jurgen F. Doreleijers
 */
public class TagTable extends Relation {
    /**
     *
     */
    private static final long serialVersionUID = -323908324987659058L;
    public static final int         dataNodeType        = StarGeneral.DATA_NODE_TYPE_TAGTABLE;
    public static final String      STANDARD_TITLE      = "standard_"+StarGeneral.DATA_NODE_TYPE_DESCRIPTION[dataNodeType]+"_title";
    public static final boolean     STANDARD_IS_FREE    = true;

    /** Use a table or a simpler construct */
    public boolean      isFree;
    public StarNode     parent;
    /** When true the ordering should be taken from the physical ordering instead of the order column */
    public boolean      useDefaultOrdering;

    /** Creates a new instance of TagTable */
    public TagTable( String name, DBMS dbms ) throws Exception {
        super( name, dbms );
        init(name, dbms);
    }

    public boolean init(String name, DBMS dbms ) {
        super.init( name, dbms, null ); // not using RelationSet here so given 3rd argument is null.
        isFree   = STANDARD_IS_FREE;
        name     = STANDARD_TITLE;
        addColumnForOverallOrder();
        parent = new StarNode(); // Just give it an empty placeholder for some 'more global' props; overwrite
        // is usually in place.
        return true;
    }

    /** Makes a deep copy of all the data. Adds the order column needed for STAR data if not
     *present already and fills it sequentially for now.
     * The free/looped state is a guess from the number of rows (?>1) in use.
     *Note that the column lables aren't changed. If the table is printed they will be modified
     *as needed.
     */
    public boolean init(Relation relation) {

        // Add relation to dbms, note that this will remove the previous instance of the relation first.
        init( name, relation.dbms );                 // Do a full init first.
        /** By type and alphabetical order */
        sizeMax         = relation.sizeMax;
        sizeRows        = relation.sizeRows;
        columnDataType  = (HashMap)     relation.columnDataType.clone();
        columnOrder     = (ArrayList)   relation.columnOrder.clone();
        reserved        = (BitSet)      relation.reserved.clone();
        used            = (BitSet)      relation.used.clone();
        attr            = (HashMap)     relation.attr.clone();          // Contains the column references.
        indices         = (HashMap)     relation.indices.clone();

        isFree          = true;
        if ( relation.sizeRows > 1 ) {
            isFree          = false;
        }

        // Keep one if it's already available.
        if ( ! containsColumn( DEFAULT_ATTRIBUTE_ORDER_ID )) {
            if ( ! addColumnForOverallOrder()) {
                General.showError("In TagTable.init(relation) failed to add column for overall order id");
                return false;
            }
            if ( ! numberRowsPhysical(DEFAULT_ATTRIBUTE_ORDER_ID,used,0)) {
                General.showError("In TagTable.init(relation) failed to numberRowsPhysical");
                return false;
            }
        }
        return true;
    }

    /** Recursively looks for the top star node which has info on like the
     *preferred format. Usually it only takes a couple of look ups so it's
     *very fast.
     *Only for tagtable it returns null if tagtable is the last on the stack.
     */
    public StarNode getTopStarNode() {
        if ( parent == null ) {
            return null;
        } else {
            return parent.getTopStarNode();
        }
    }


    /** Return string in stead of output to a writer. Returns null if
     * unsuccessful.
     */
    public String toSTAR() {
        StringWriter stw = new StringWriter();
        boolean status = toSTAR(stw);
        if ( ! status ) {
            General.showError("Failed to toSTAR(stw) for tagtable: " + name);
            return null;
        }
        return stw.toString();
    }

    /** Creates formatted STAR output */
    public boolean toSTAR( Writer w) {
//        General.showDebug("Now in TagTable.toSTAR for table: " + name);
        char spaceBetweenLoopedValues = ' ';
        StringBuffer sb         = new StringBuffer();
        String loopIndent       = Strings.createStringOfXTimesTheCharacter(' ', parent.general.loopIdentSize);
        String freeIndent       = Strings.createStringOfXTimesTheCharacter(' ', parent.general.freeIdentSize);
        String tagNameIndent    = Strings.createStringOfXTimesTheCharacter(' ', parent.general.tagnamesIdentSize);

        int lastColumnIdx       = sizeColumns();
//        int valueCount          = 0;
//        int valueId             = 0;
//        int count               = 0;

        if ( sizeColumns() == 0 ) {
            General.showError("Attempted to render empty TagTable to STAR, even no order column.");
            return false;
        }

        // columnOrder.toArray() doesn't allow the cast to String[] it seems.
        String[] tagNames = PrimitiveArray.asStringArray( columnOrder );
        int maxTagNameSize = Strings.getMaxSizeStrings( tagNames );

        int orderColumnIdx = getColumnIdx( DEFAULT_ATTRIBUTE_ORDER_ID );
        if ( orderColumnIdx < 0 ) {
            General.showError("No order column found");
            return false;
        }
        if ( sizeColumns() == 1 ) { // empty table.
            General.showWarning("Attempted to render empty TagTable to STAR.");
            return true;
        }

//        if ( orderColumnIdx != 0 ) {
//            General.showWarning("Order column not found at zero-ed position but at: " + orderColumnIdx);
//        }
        if ( sizeRows == 0 ) {
            General.showWarning("Attempted to render empty TagTable (no rows)");
            return true;
        }
        if ( (sizeRows > 1) && isFree ) {
            General.showDebug("The tagtable: ["+name+"] contains more than 1 row so the type was reset from free to looped.");
            isFree = false;
        }

        Relation prtRelation = null;
        try {
            // Free tags here
            if ( isFree ) {
                for (int c=0; c<lastColumnIdx;c++)  {
                    if ( c == orderColumnIdx ) {
                        continue;
                    }
                    // Just format it such that it will take the least space.
                    sb.append( freeIndent );
                    String label = getColumnLabel(c);
                    int labelLength = label.length();
                    // In the case of a non tag name label reformat it simply.
                    // Might need more work for more complex labels, e.g. containing spaces.
                    if ( ! label.startsWith("_")) {
                        sb.append( '_' );
                        labelLength++;
                    }
                    sb.append( label );
                    sb.append( Strings.getSpacesString(maxTagNameSize + 2 - labelLength));
                    sb.append( StarGeneral.addQuoteStyle( getValueString(0,c)));
                    sb.append( '\n' );
                }
                sb.append( '\n' );
                w.write(sb.toString());
                return true;
            }

            // Loop tag names here
//            sb.append( "\n" ); // trying to match the whitespace to Steve's starlib
            sb.append( loopIndent );
            sb.append( "loop_\n" ); // trying to match the whitespace to Steve's starlib

            /** Skip first column as it is reserved for the order of things */
            for (int c=0; c<lastColumnIdx;c++)  {
                if ( c == orderColumnIdx ) {
                    continue;
                }
                sb.append( tagNameIndent );
                String label = getColumnLabel(c);
                if ( ! label.startsWith("_")) {
                    sb.append( '_' );
                }
                sb.append( label );
                sb.append( '\n' );
            }
            sb.append( '\n' );

            int[] map = null;
            if ( ! useDefaultOrdering ) {
                map = getRowOrderMap(-1); // -1 for default ordering.
//                General.showDebug("In Tagtable using map: " + PrimitiveArray.toString(map));
                if ( (map!=null) && (map.length!=sizeRows)) {
                    General.showError("(A) The obtained used row map list length: " + map.length  + " isn't of the right length: " + sizeRows);
                    General.showError("used bitset: "+ PrimitiveArray.toString(used));
                    General.showError("Occured in toSTAR method of table with first tag: " + columnOrder.get(1));
                    int[] order = getColumnInt( Relation.DEFAULT_ATTRIBUTE_ORDER_ID );
                    if ( order !=  null ) {
                        General.showError("order ints : "+ PrimitiveArray.toString(order));
                    }
                    map = null;
                }
                if ( map == null ) {
                    General.showWarning("Failed to get the row order sorted out for table: "+name+"; using physical ordering.");
                }
            }
            if ( map == null ) {
                map = getUsedRowMapList();
                if ( map == null ) {
                    General.showError("Failed to get the used row map list so not writing this table.");
                    return false;
                }
            }
            if ( map.length != sizeRows ) {
                General.showError("(B) The obtained used row map list length: " + map.length  + " isn't of the right length: " + sizeRows);
                return false;
            }

            prtRelation = new Relation(dbms.getNextRelationName(),dbms);
            if ( prtRelation == null ) {
                General.showDebug("Failed to get temporary relation for printing.");
            }
            HashMap             namesAndTypes;
            ArrayList           order;
            // INTRO
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            for (int c=0; c<lastColumnIdx;c++)  {
                if ( c == orderColumnIdx ) {
                    continue;
                }
                String label = getColumnLabel(c);
                namesAndTypes.put( getColumnLabel(c), new Integer(Relation.DATA_TYPE_STRINGNR));
                order.add(label);
            }
            if ( ! prtRelation.insertColumnSet(0, namesAndTypes, order, null, null)) {
                General.showError("Failed to insertColumnSet for STAR printing");
                return false;
            }
            // make the relation of the same row dimensions
            if ( prtRelation.sizeMax < sizeMax ) { // check to prevent a warning.
                prtRelation.ensureCapacity( sizeMax );
            }
            prtRelation.used.or( used );
            /** Note that in the next object the rows are sequentially used
             *without interuption.
             */
            String[][] colList = new String[lastColumnIdx][];
            StringSet[] colSetList = new StringSet[lastColumnIdx];

            // Move the values to String and add quotes if needed.
            for (int c=0; c<lastColumnIdx;c++)  {
                if ( c == orderColumnIdx ) {
                    continue;
                }
                String label = getColumnLabel(c);
                /** Safe the references for below */
                colList[c]      =prtRelation.getColumnString(label);
                colSetList[c]   =prtRelation.getColumnStringSet(label);
                boolean colContainsAQuotedValue = false;
                /** Take a local variable */
                String[]  col = colList[c];
                StringSet colSet = colSetList[c];
                Object    colOrg = getColumn(label);
                int dataType = getColumnDataType(label);
                String v = null;

                for (int r=0;r<sizeRows;r++)  {
                    // Safe some work on all but the first element
                    if ( r!=0 ) {
                        // Compare 2 elements in the same column
                        if ( equalElements(colOrg, dataType, map[r],map[r-1])) {
                            col[r] = col[r-1]; // use old String representation.
                            continue;
                        }
                    }
                    // 33% of time in this routine is spend by getValueString so it can
                    // easily be optimized by first doing conversion to String only column
                    // outside of this loop. It would save a lot of objects too.
                    v = StarGeneral.addQuoteStyle(getValueString(map[r],c));
                    col[r] = colSet.intern(v); // just keep one reference per string per column.
                }
                if ( colSet.containsQuotedValue()) {
                    colContainsAQuotedValue = true;
                }

                String[] colUnique = colSet.getStringArray();
                int maxSizeElementsCol = Strings.getMaxSizeStrings(colUnique);
                //General.showDebug("Found largest string in column: " + label + " is: " + maxSizeElementsCol);
                //String fmt = "%-"+maxSizeElementsCol+"s";
                HashMap paddedValues = new HashMap();
                char[] charsReusable = new char[maxSizeElementsCol];
                String padded = null;
//                Parameters p = new Parameters(); // for printf
                boolean leftAlign = true;
                v = col[0];
                if ( Strings.startLooksLikeANumber(v) ) {
                    leftAlign = false;
                }

                for (int r=0;r<sizeRows;r++)  {
                    v = col[r];
                    padded = (String) paddedValues.get(v);
                    if ( padded != null ) {
                        col[r]=padded; // reuse existing value
                        continue;
                    }
                    if ( v == null ) {
                        General.showCodeBug("Failed getting translation to a (un-)quoted string value.");
                        General.showCodeBug("For relation: " + name + " at column: " + label + " and row: "
                                + r);
                        return false;
                    }
                    if ( v.length() != charsReusable.length ) {
                        if ( Strings.growToSize(v,leftAlign,
                                charsReusable,colContainsAQuotedValue) ) {
                            padded = new String(charsReusable);
                        } else {
                            General.showError("Failed Strings.growToSize");
                            return false;
                        }
                    } else {
                        padded = v;
                    }
                    // simple thing needed here.
                    if ( padded == null ) {
                        General.showError("Failed padding by Strings.growToSize or otherwise.");
                        return false;
                    }
                    col[r]=padded;
                    paddedValues.put(v,padded);
                }
            }


            // Move the values to String and add quotes if needed.
            for (int r=0; r<sizeRows;r++)  {
                sb.append( tagNameIndent );
                for (int c=0; c<lastColumnIdx;c++)  {
                    if ( c == orderColumnIdx ) {
                        continue;
                    }
                    sb.append( colList[c][r] );
                    sb.append( spaceBetweenLoopedValues );
                }
                sb.append( '\n' );
            }

            if ( parent.general.showStopTag ) {
                sb.append( loopIndent );
                sb.append( "stop_" );
                sb.append( '\n' );
                sb.append( '\n' );
            }

            w.write(sb.toString());
        } catch ( Throwable t) {
            General.showThrowable( t );
            return false;
        } finally {
            if ( (prtRelation!=null) && dbms.containsRelation(prtRelation.name)) {
                if ( ! dbms.removeRelation(prtRelation)) {
                    General.showError("Failed dbms.removeRelation(prtRelation)");
                }
            }
        }
//        General.showDebug("Leaving TagTable.toSTAR for table: " + name);
        return true;
    }

    /** Find the correct tagtable in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no tagtable is
     * present in the tree.
     */
    public TagTable getTagTable(String columnName) {

        if ( columnName.equals(StarGeneral.WILDCARD) || containsColumn(columnName) ) {
            //General.showDebug("found table with column name: " + columnName);
            return this;
        } else {
            return null;
        }
    }

    /** Find the correct tagtable in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no tagtable is
     * present in the tree.
     */
    public TagTable getTagTableRegExp(String columnNameRegExp) {

        if ( columnNameRegExp.equals(StarGeneral.WILDCARD) || containsColumn(columnNameRegExp) ) {
            //General.showDebug("found table with column name: " + columnName);
            return this;
        }
        Pattern p_general_format = Pattern.compile(columnNameRegExp, Pattern.COMMENTS);
        Matcher m_general_format = p_general_format.matcher(""); // default matcher on empty string.


        int lastColumnIdx       = sizeColumns();
        for (int c=1; c<lastColumnIdx;c++)  {
            m_general_format.reset( getColumnLabel(c));
            boolean status = m_general_format.matches();
            if ( status ) {
                //General.showDebug("Matched column label: " + getColumnLabel(c) + " with regexp: " + columnNameRegExp);
                return this;
            }
        }
        return null;
    }


    /** According to the data types specified in the dictionary
     *translate it assuming that all are strings of type non-redundant
     */
    public boolean translateToNativeTypesByDict( StarDictionary starDict , boolean isMMCIF) {

        boolean overallStatus = true;
        if ( ! isMMCIF ) {
            String sFCategory = null;
            if ( ! (parent instanceof SaveFrame )) {
                General.showWarning("Can't translate data type to native by dictionary for tagtables outside a saveframe at this moment");
                return overallStatus;
            }
            if ( parent == null ) {
                General.showWarning("Can't translate data type to native by dictionary for tagtables with no parent at this moment");
                return overallStatus;
            }
            SaveFrame parentSf = (SaveFrame) parent;
            sFCategory = parentSf.getCategory();
            //* HashOfHashes fromStar2D -> ArrayList
            //*      "Saveframe category" -> "Tag name" -> ("Wattos relation", "Wattos name", "Wattos data type")
            if ( sFCategory == null ) {
                General.showWarning("Can't translate data type to native by dictionary for tagtables with parent of unknown sf category at this moment.");
                return overallStatus;
            }

            if ( !starDict.fromStar2D.containsKey( sFCategory )) {
//                General.showDebug("Skipping translation of saveframe category (no data type definitions in dictionary): " + sFCategory );
                /**
                General.showDebug("Definitions exist for saveframe categories: " +
                    Strings.toString( starDict.fromStar.keySet().toArray() ));
                 */
                return overallStatus;
            }
            HashMap fromStarCached = (HashMap) starDict.fromStar2D.get( sFCategory );

            for (int i=0; i<sizeColumns(); i++) {
                String label = null;
                try {
                    label = getColumnLabel(i);
                    // Skip the order column
                    if ( label.equals(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)) {
                        continue;
                    }
                    int columnDataType = getColumnDataType(label);
                    if ( columnDataType != Relation.DATA_TYPE_STRINGNR ) {
                        General.showWarning("Original data type should be stringnr but found: " + Relation.dataTypeList[columnDataType] + " Skipping this column");
                        continue;
                    }
                    ArrayList info = (ArrayList) fromStarCached.get(label);
                    if ( info == null ) {
                        if ( label.endsWith(".Entry_ID") ||
                             label.endsWith("_list_ID") ||
                             label.endsWith(".ID") ) {
                            continue;
                        }
//                        General.showDebug("Tag doesn't occur in dictionary: " + label + " Skipping this column");
                        continue;
                    }
                    String columnDataTypeString = (String) info.get( StarDictionary.POSITION_WATTOS_DATATYPE );
                    // Reuse variable for new data type
                    columnDataType = dataTypeArrayList.indexOf( columnDataTypeString );
                    if ( columnDataType < 0 ) {
                        General.showWarning("Tag has a data type listed in dictionary that is not supported: " + columnDataTypeString + " Skipping this column");
                        General.showWarning("Known are: " + Strings.concatenate( dataTypeList, "," ) );
                        continue;
                    }
                    if ( columnDataType == getColumnDataType(label) ) {
                        //General.showDebug("Tag has a data type as listed in dictionary: " + columnDataTypeString + " No need to convert this column");
                        continue;
                    }
                    boolean status = convertDataTypeColumn( i, columnDataType, null );
                    if ( ! status ) {
                        General.showError("Column with tag: " + label + " had conversion error for at least 1 of the rows. Column was not converted. Old data untouched.");
                        overallStatus = false;
                        continue;
                    }
                } catch ( Exception e ) {
                    General.showError("in translateToNativeTypesByDict for tag table: " + name + " and column: " + label);
                    General.showThrowable(e);
                    overallStatus = false;
                }
            } // end of loop per column.
        } else {
            for (int i=0; i<sizeColumns(); i++) {
                String label = null;
                try {
                    label = getColumnLabel(i);
                    // Skip the order column
                    if ( label.equals(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)) {
                        continue;
                    }
                    int columnDataType = getColumnDataType(label);
                    if ( columnDataType != Relation.DATA_TYPE_STRINGNR ) {
                        General.showWarning("Original data type should be stringnr but found: " + Relation.dataTypeList[columnDataType] + " Skipping this column");
                        continue;
                    }
                    ArrayList info = (ArrayList) starDict.fromCIF.get(label);
                    if ( info == null ) {
//                        General.showDebug("Tag doesn't occur in dictionary: " + label + " Skipping this column");
                        continue;
                    }
                    String columnDataTypeString = (String) info.get( StarDictionary.POSITION_WATTOS_DATATYPE );
                    if ( CIFCoord.dataTypeTranslationExceptions.contains(label)) {
//                        General.showDebug("Skipping special case of incompatible data types for: " + label);
                        continue;
                    }
                    // Reuse variable for new data type
                    columnDataType = dataTypeArrayList.indexOf( columnDataTypeString );
                    if ( columnDataType < 0 ) {
                        General.showWarning("Tag has a data type listed in dictionary that is not supported: " + columnDataTypeString + " Skipping this column");
                        General.showWarning("Known are: " + Strings.concatenate( dataTypeList, "," ) );
                        continue;
                    }
                    if ( columnDataType == getColumnDataType(label) ) {
                        //General.showDebug("Tag has a data type as listed in dictionary: " + columnDataTypeString + " No need to convert this column");
                        continue;
                    }
                    boolean status = convertDataTypeColumn( i, columnDataType, null );
                    if ( ! status ) {
                        General.showError("Column with tag: " + label + " had conversion error for at least 1 of the rows. Column was not converted. Old data untouched.");
                        overallStatus = false;
                        continue;
                    }
                } catch ( Exception e ) {
                    General.showError("in translateToNativeTypesByDict for tag table: " + name + " and column: " + label);
                    General.showThrowable(e);
                    overallStatus = false;
                }
            } // end of loop per column.
        }
        return overallStatus;
    }

    /** According to the data formatting specified in the dictionary
     *translate it. Since fancy printing is done by com.braju package
     *it isn't fast. Limit the amount of columns to do it on or expect the
     *slow-down.
     */
    public boolean translateToStarFormattingByDict( StarDictionary starDict ) {

        boolean overallStatus = true;
        if ( parent == null ) {
            General.showWarning("Parent of tagtable is not known. Not fatal but can't translate values to STAR for it.");
            return true;
        }
        if ( !(parent instanceof SaveFrame )) {
            General.showWarning("Parent of tagtable is not of type SaveFrame. Not fatal but can't translate values to STAR for it.");
            return true;
        }
        SaveFrame parentSf = (SaveFrame) parent;
        String sFCategory = parentSf.getCategory();
        // We'll do a rather stupid double lookup.
        //* HashOfHashes fromStar2D -> ArrayList
        //*      "Saveframe category" -> "Tag name" -> ("Wattos relation", "Wattos name", "Wattos data type")
        /*
         *Maps TO nmr-star from wattos:
         * HashOfHashes toStar2D -> ArrayList
         *      "Wattos relation" -> "Wattos name" -> ("Saveframe category", "Tag name", "Text format")
         */
        if ( (sFCategory==null) || !starDict.fromStar2D.containsKey( sFCategory ) ) {
            General.showWarning("No definitions for saveframe category: " + sFCategory );
            General.showDebug("Definitions exist for saveframe categories: " +
                Strings.toString( starDict.fromStar.keySet().toArray() ));
            return overallStatus;
        }
        HashMap fromStarCached = (HashMap) starDict.fromStar2D.get( sFCategory );

        for (int i=0; i<sizeColumns(); i++) {
            try {
                String label = getColumnLabel(i);
                // Skip the order column if present
                if ( label.equals(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)) {
                    continue;
                }
                ArrayList info = (ArrayList) fromStarCached.get(label);
                if ( info == null ) {
                    General.showWarning("Tag doesn't occur in dictionary: " + label + " Skipping this column");
                    continue;
                }
                String wattosRelationName   = (String) info.get(StarDictionary.POSITION_WATTOS_RELATION );
                String wattosName           = (String) info.get(StarDictionary.POSITION_WATTOS_NAME );
                String columnDataTypeString = (String) info.get(StarDictionary.POSITION_WATTOS_DATATYPE );

                ArrayList infoStar = (ArrayList) starDict.toStar2D.get( wattosRelationName, wattosName);
                if ( infoStar == null ) {
                    General.showWarning("Tag doesn't occur in dictionary for wattosRelationName, wattosName: " + wattosRelationName + " and " +  wattosName);
                    continue;
                }
//                String columnSaveframeCategory  = (String) infoStar.get(StarDictionary.POSITION_STAR_CATEGORY );
                String columnTagName            = (String) infoStar.get(StarDictionary.POSITION_STAR_TAG_NAME );
                String columnTextFormat         = (String) infoStar.get(StarDictionary.POSITION_STAR_TAG_FORMAT );

                if ( ! label.equals( columnTagName ) ) {
                    General.showError("Failed a consistency check. Dictionary has different STAR tag names in different maps: " + label + " and " + columnTagName );
                    return false;
                }

                int columnDataTypeWattos = dataTypeArrayList.indexOf( columnDataTypeString );
                if ( columnDataTypeWattos < 0 ) {
                    General.showWarning("Tag has a data type listed in dictionary that is not supported: " + columnDataTypeString + " Skipping this column");
                    General.showWarning("Known are: " + Strings.concatenate( dataTypeList, "," ) );
                    continue;
                }

                if ( columnTextFormat.length() < 1 ) {
                    //General.showDebug("Skipping tag without text format listed in STAR: " + label);
                    continue;
                }
                //General.showDebug("Formating for text format: " + columnTextFormat);

                int columnDataType = getColumnDataType(label);

                if (!((columnDataType == Relation.DATA_TYPE_FLOAT )||
                      (columnDataType == Relation.DATA_TYPE_STRING )||
                      (columnDataType == Relation.DATA_TYPE_STRINGNR )||
                      (columnDataType == Relation.DATA_TYPE_BIT )||
                      (columnDataType == Relation.DATA_TYPE_INT ))) {
                    General.showCodeBug("Only floats, string, stringnr, bit, and integers can be printed in a formatted way now.");
                    General.showError("Data type of column is: " + dataTypeArrayList.get(columnDataType ));
                    General.showError("Format is: " + columnTextFormat );
                    overallStatus = false;
                    continue;
                }

                boolean status = convertDataTypeColumn( i, Relation.DATA_TYPE_STRING, columnTextFormat);
                if ( ! status ) {
                    General.showError("Column with tag: " + label + " had conversion error for at least 1 of the rows. Column was not converted. Old data untouched.");
                    General.showError("Data type of column is: " + dataTypeArrayList.get(columnDataType ));
                    General.showError("Format is: " + columnTextFormat );
                    overallStatus = false;
                    continue;
                }
            } catch ( Exception e ) {
                General.showError("error in translateToNativeTypesByDict for tag table with name: " + name );
                General.showThrowable(e);
                overallStatus = false;
            }
        }
        return true;
    }

}
