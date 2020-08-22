/*
 * Created on March 25, 2003, 5:56 PM
 */

package Wattos.Database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexHashedBitToMany;
import Wattos.Database.Indices.IndexHashedIntToMany;
import Wattos.Database.Indices.IndexHashedObjectToMany;
import Wattos.Database.Indices.IndexSortedFloat;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Database.Indices.IndexSortedShort;
import Wattos.Database.Indices.IndexSortedString;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.ObjectIntPair;
import Wattos.Utils.Objects;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.StringSet;
import Wattos.Utils.Strings;
import Wattos.Utils.Comparators.ComparatorIntIntPair;
import Wattos.Utils.Wiskunde.Statistics;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;

import com.Ostermiller.util.ExcelCSVParser;

/**
 * This class can be used as a relation (also called entity in the entity-relationship
 * model, ER) data model. This is by far the largest class in Wattos.
 * It is optimized to be fast for
 * working with a large number of rows (millions) and a small number of columns (hundreds).
 * Size changes are pretty expensive but access is fast. Memory requirements are very
 * optimal since almost no redundant objects are used. Access does require a lot of
 * array index calculations which each individually are bound checked.
 * <P>
 * The rows may be unordered but the columns are ordered although they are usually addressed
 * by the label. Rows may be ordered by using an extra column with the
 * keyword defined by: DEFAULT_ATTRIBUTE_ORDER_ID (currently "orderId").
 * Normally the rows are returned
 * by their physical ordering in the primitive arrays, skipping any not used rows.
 * <P>
 * Access to columns can be done on the basis of a String typed label on the columns.
 * <P>
 * Access to rows is guaranteed to always be to the same elements. The method getNewRowId returns
 * an index to a row that is available to be used again. The method removeRow just sets
 * the row to "not being used" and resets the elements to their default value which is the
 * same as their null values for data types that support nulls and to the Java default values
 * for the others.
 * <P>
 * This class requires you to specify the data type per column; it can not handle data
 * types different per cell. The following data types are supported:
 * <OL>
 * <LI>BitSet- This is more optimal than boolean[] usually.
 * <LI>char[] -
 * <LI>byte[] -
 * <LI>short[] -
 * <LI>int[] -
 * <LI>float[] -
 * <LI>double[] -
 * <LI>String[] - and others.
 * </OL>
 * The default values for the cells is the default value of primitive data type; e.g.
 * and 0.0 for float and null for all objects like String.
 * <P>
 * The contract of this class is very loose/dangerous/flexible however you want to
 * call it. Certain restrictions should be kept in mind:
 * <OL>
 * <LI>Pay attention to call the methods to StringSet in order to really get unique strings.
 * <LI>Use the method setValuesToDefaultByRowRange as an example on how to interact with
 * the data encapsulated by this class.
 * <LI>The choice made was to not add another level of indirection because it would cost an additional
 * array access (with Java range checking done). This does mean the objects above this have to
 * keep track of the direct indices and should the array be reordered; they all need to be
 * notified. Reordering can be done by using a special column labeled by a value hard-
 * coded by the variable: DEFAULT_ATTRIBUTE_ORDER_ID
 * <LI>Arrays of items can be maintained by e.g. a linked list of elements encoded in
 * here (for the forward/backward indices) AND the structure above it (for the first/last and
 * number of elements in the array).<BR>
 * The class LinkedListArray is included here for that special purpose. When a row is removed
 * the instances of linkedlistarray will be corrected accordingly.
 * </OL>
 * <P>
 * Objects.deepCopy can be used to do cloning.
 * @author Jurgen F. Doreleijers
 */
public class Relation implements Serializable  {

    /** Faking this variable makes the serializing not worry
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    /** For speed precode the types.*/
    public static final int DATA_TYPE_INVALID       = -1;
    public static final int DATA_TYPE_BIT           = 0;
    public static final int DATA_TYPE_CHAR          = 1;
    public static final int DATA_TYPE_BYTE          = 2;
    public static final int DATA_TYPE_SHORT         = 3;    // Latest addition to family.
    public static final int DATA_TYPE_INT           = 4;
    public static final int DATA_TYPE_FLOAT         = 5;
    public static final int DATA_TYPE_DOUBLE        = 6;
    /** See class LinkedListArray; used for cheap implementation of certain type of ListsOfLists */
    public static final int DATA_TYPE_LINKEDLIST    = 7;
    /** See class LinkedListArrayInfo */
    public static final int DATA_TYPE_LINKEDLISTINFO= 8;
    /** Regular String[] construct */
    public static final int DATA_TYPE_STRING        = 9;
    /** Non-redundant strings in this list only. */
    public static final int DATA_TYPE_STRINGNR      = 10;
    public static final int DATA_TYPE_ARRAY_OF_INT  = 11;
    public static final int DATA_TYPE_ARRAY_OF_FLOAT= 12;
    public static final int DATA_TYPE_ARRAY_OF_STR  = 13;
    public static final int DATA_TYPE_OBJECT        = 14; // Data type within one column better be the same still. Because of comparator.
    /** String representation of the data types as specified before
     */
    public static final String[] dataTypeList = {   "BIT",
                                                    "CHAR",
                                                    "BYTE",
                                                    "SHORT",
                                                    "INTEGER",
                                                    "FLOAT",
                                                    "DOUBLE",
                                                    "LINKEDLIST",
                                                    "LINKEDLISTINFO",
                                                    "STRING",
                                                    "STRINGNR",
                                                    "INT ARRAY",
                                                    "FLOAT ARRAY",
                                                    "STR ARRAY",
                                                    "OBJECT"    };
    /** String representation of the data types in SQL as specified before
     */
    public static final String[] dataTypeListSQL = {"BOOLEAN",
                                                    "CHAR(1)",
                                                    "SMALLINT",
                                                    "SMALLINT",
                                                    "INT",
                                                    "FLOAT",
                                                    "DOUBLE",
                                                    null, //"LINKEDLIST",
                                                    null, //"LINKEDLISTINFO",
                                                    "LONGTEXT",
                                                    "LONGTEXT",
                                                    null, //"INT ARRAY",
                                                    null, //"FLOAT ARRAY",
                                                    null, //"STR ARRAY",
                                                    null};//"OBJECT"
    public static final ArrayList dataTypeArrayList = new ArrayList( Arrays.asList( dataTypeList ));
    public static final boolean[] supportsNulls = {
        false,  //BIT
        true,   //CHAR
        false,  //BYTE
        true,   //INTEGER
        true,   //FLOAT
        true,   //DOUBLE
        true,   //LINKEDLIST
        true,   //LINKEDLISTINFO
        true,   //STRING
        true,   //STRINGNR
        true,   //INT ARRAY
        true,   //STR ARRAY
        true};  //OBJECT

    /** A standard attribute that is commonly present but not required. It's of type bit.*/
    public static final String      DEFAULT_ATTRIBUTE_SELECTED         = "selected";

    /** A standard attribute to indicate that the column referred to is the physical array.
     *don't use it any more; use null pointer instead for the column.
     */
    public static final String      DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN  = "physicalColumn";

    /** A standard attribute to indicate that the column referred to is the physical array */
    public static final String      DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX  = "_id";

    /** Pretty small initial size; resize as necessary. Idea
     *is to be able to handle millions of rows.*/
    public static final int        DEFAULT_MAXSIZE                 = 100;
    /** See routine: getNextReservedRow*/
    public static final int        DEFAULT_GROWTH_SIZE_RESERVATIONS= 100;
    /** The number of columns should not be very large probably less than 100.
     *The variable is actually not used at this point.*/
    //public static final int        DEFAULT_MAXSIZE_COLUMNS         = 5;
    /** Default percentage the total number of rows will grow. Kind of largish,
     *maybe it should be more like 30%
     */
    public float                   DEFAULT_SIZE_CHANGE_PERCENTAGE  = 50.0f;
    /**In case no label for a column is given this will be used. At the moment you
     always have to give one so it's not used.*/
    //public static final String     DEFAULT_LABEL_PREFIX            = "Label_";
    /** Used for the implicit label for StringSet. Instances don't need to
     * grow when all others do.*/
    public static final String     STANDARD_STRING_SET_POSTFIX     = "Set";
    /** Standard name for the relation */
    public static final String     STANDARD_RELATION_NAME       = "relationName";
    public static final String     ROW_NOT_USED_STRING          = "Not used";
    public static final String     ROW_WITHOUT_COLUMNS_STRING   = "No columns present";

    /** Used for larger sections of code where performance is not an issue. Usually the
     * debug code is just commented out when done though.
     */
//    private static final boolean DEBUG = true;

    /**A numeric value to indicate it's not a real index */
    public static final int NOT_AN_INDEX = LinkedListArray.NOT_AN_INDEX;

    /**A string value to indicate it's not a real index. Note it isn't declared final but it is static */
    public static String NOT_AN_INDEX_STRING = LinkedListArray.NOT_AN_INDEX_STRING;
    /**A one-character string value to indicate true */
    public static final String TRUE  = "T";
    /**A string value to indicate true */
    public static final String FALSE = "F";

    /** For use in parent-child modelling with connected relations. NOT USED ANYMORE */
    public static final String      DEFAULT_ATTRIBUTE_PARENT           = "parent";
    public static final String      DEFAULT_ATTRIBUTE_CHILD_LIST       = "childList";
    public static final String      DEFAULT_ATTRIBUTE_SIBLING_LIST     = "siblingList";
    /** For use to do selections on rows. */
    public static final boolean     DEFAULT_ATTRIBUTE_SELECTED_VALUE   = false;

    /** The name of the object modeled by the row. */
    public static final String      DEFAULT_ATTRIBUTE_NAME             = "name";
    /** The type of the object modeled by the row. e.g. single for a bond between 2 atoms*/
    public static final String      DEFAULT_ATTRIBUTE_TYPE             = "type";

    /** The number of the object not over the whole relation but among it's siblings (under 1 parent).
     *The numbering starts at 1 in most but not all cases.*/
    public static final String      DEFAULT_ATTRIBUTE_NUMBER           = "number";

    /** The value of an item e.g. the bond lenght of a bond object.
    */
    public static final String      DEFAULT_ATTRIBUTE_VALUE            = "value";
    /** The value of an item e.g. the bond lenght of a bond object.
    */
    public static final String      DEFAULT_ATTRIBUTE_VALUE_1          = DEFAULT_ATTRIBUTE_VALUE + "_1";
    /** List of values over models.
    */
    public static final String      DEFAULT_ATTRIBUTE_VALUE_1_LIST     = DEFAULT_ATTRIBUTE_VALUE + "_1_list";
    /** The value of an item e.g. the bond lenght standard deviation of a bond object.
    */
    public static final String      DEFAULT_ATTRIBUTE_VALUE_2          = DEFAULT_ATTRIBUTE_VALUE + "_2";
    /** The total number of elements within a subset of the column */
    public static final String      DEFAULT_ATTRIBUTE_COUNT           = "count";
    /** Used for overall ordering of elements, order starts at row numbered 0. Might not be
     *that useful if the order isn't meaningful over all elements in the table. Usually
     *it's better to have ordering with less scope.
     */
    public static final String      DEFAULT_ATTRIBUTE_ORDER_ID         = "order";
    /** Used to identify an element */
    public static final String      DEFAULT_ATTRIBUTE_ID               = "identifier";
    public static final int         DEFAULT_ATTRIBUTE_PARENT_VALUE     = NOT_AN_INDEX;
    /** Value indicating that the relation's physical size grew after calling
     *a routine like (at the moment only): getNextReservedRow.
     */
    public static final int         DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW     = -2;
    /** Value indicating that the column is not sorted consecutively. */
    public static final int         DEFAULT_VALUE_INDICATION_COLUMN_CONSECUTIVE     = -1;
    /** Value indicating that the column sorting test encountered an error. */
    public static final int         DEFAULT_VALUE_INDICATION_COLUMN_ORDER_CHECK_ERROR   = -2;


    /** Name of the relation */
    public String name;

    /** Other objects that should all be native arrays with using parallel indices as
     * the above. */
    public HashMap attr;

    /** Cached id for the data type in a column.*/
    public HashMap columnDataType;

    /** Name for the column. Only place where order of columns is defined and matters.*/
    public ArrayList columnOrder;

    /** Actual size of the underlying primitive arrays at this point.
     *Note that the actual size of a BitSet is determined by the JVM implementation.
     */
    public int sizeMax;

    /** The number of items in the list that are used excluding those
     * never allocated and those deleted.     */
    public int sizeRows;

    /** For deletion an item is set to 'not used' and all elements in the many
     arrays are initialized to their standard values.  */
    public BitSet used;

    /** A standard attribute that is commonly present but not required. It's of type bit.
     *It can be used to claim and unclaim sets of rows more efficiently. This class
     *provides some convenience classes to the column implementing this feature so
     *reservations can be efficiently claimed and cancelled.
     * For use to do reservation on rows. A value of true means the row is reserved meaning
     *it's also marked as in use but the data in it has not been touched yet. Doing away
     *with a reservation is simply flipping the bit on this and the used bitset.
     *If the variable is not in use it should be a default initialized BitSet taking
     *very little memory.
     */
    public BitSet reserved;

    /** A hashmap on column label and then an array on index type.
     * One column can have both a hashed and a sorted index.
     * If no index for a column exists there should not be an key for the column (very
     *strict about that).
     */
    public HashMap indices;

    /** A relation is always part of a DB, organized by a DBMS*/
    public DBMS dbms;

    /** Parent if this relation is in a Relation set
     *otherwise null.     */
    public RelationSet relationSetParent;

    /** Creates a new instance of Relation */
    public Relation( String name, DBMS dbms ) throws Exception {
        boolean status = init( name, dbms, null );
        if ( !status ) {
            throw new Exception( "Failed to initialize relation in dbms" );
        }
    }

    /** Creates a new instance of Relation */
    public Relation( String name, DBMS dbms, RelationSet relationSetParent ) throws Exception {
        boolean status = init( name, dbms, relationSetParent );
        if ( !status ) {
            throw new Exception( "Failed to initialize relation in dbms with parent: " + relationSetParent);
        }
    }

    /** Creates a new instance of Relation
     * @param maxRowSize The maximum number of rows that the relation can hold
     * before it automatically resizes.
     */
    public Relation(String name, int maxRowSize, DBMS dbms ) throws Exception {
        boolean status = init(name, maxRowSize,  dbms, null );
        if ( !status ) {
            throw new Exception( "Failed to initialize relation in dbms with relation name: " + name);
        }
    }

    /** Initialize the instance data.
     */
    public boolean init( String name, DBMS dbms, RelationSet relationSetParent ) {
        return init(name, DEFAULT_MAXSIZE, dbms, relationSetParent );
    }

    /** Initialize the instance data given a maximum size of the number of rows.
     * @param maxSizeNew The maximum number of rows that the relation can hold
     * before it automatically resizes.
     */
    public boolean init(String name, int maxSizeNew, DBMS dbms, RelationSet relationSetParent ) {

        this.name           = name;
        this.dbms           = dbms;
        this.relationSetParent = relationSetParent;
        sizeRows            = 0;
        sizeMax             = maxSizeNew;
        attr                = new HashMap();
        columnDataType      = new HashMap();
        columnOrder         = new ArrayList();
        used                = new BitSet(sizeMax);
        reserved            = new BitSet(sizeMax);
        indices             = new HashMap();
        //hash                = new int[sizeMax];

        if ( dbms == null ) {
            General.showError("dbms ref should be not null.");
            return false;
        }

        // Remove a possible earlier instance.
        if ( dbms.containsRelation( name ) ) {
            dbms.removeRelation( this );
        }

        // Add it again.
        boolean status = dbms.addRelation( this ); // Before adding it only the name needs to be set.
        if ( ! status ) {
            General.showError("Failed to register the relation with the DBMS.");
            return false;
        }
        return true;
    }

    /** Can be null if the relation is not in a RelationSet.*/
    public RelationSet getRelationSetParent() {
        return relationSetParent;
    }

    /** Can be null if the relation is not in a RelationSet or
     *if RelationSet doesn't have a parent.
     */
    public RelationSoS getRelationSoSParent() {
        if ( relationSetParent == null ) {
            return null;
        }
        return relationSetParent.getRelationSoSParent();
    }

    /** The number of columns in the relation. A convenience method.
     * @return size of the relation in columns
     */
    public int sizeColumns() {
        return columnOrder.size();
    }

    /**
     *Returns true when label given is: DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN
     * @return
     */
    public boolean containsColumn( String label) {
        if ( label == null ) {
            return false;
        }
        // Common case:
        if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN ) ) {
            return true;
        }

        // Checks if element exists and if so returns it's index or -1 if it doesn't
        // exist.

        int idx = columnOrder.indexOf( label );
        if ( idx == -1 ) {
            return false;
        }
        return true;
    }

    /**
     *Returns true when label given is: DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN
     */
    public boolean containsColumnRegexName( String labelRegex ) {
        if ( labelRegex == null ) {
            return false;
        }
        // Common case:
        if ( labelRegex.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN ) ) {
            return true;
        }

        for (int i=0;i<columnOrder.size();i++) {
            if ( ((String) columnOrder.get(i)).matches(labelRegex)) {
                return true;
            }
        }
        return true;
    }


    /** Change the maximum number of rows the relation can hold. Both shrinking and growing
     * can be done.
     * @param maxSizeNew New maximum size of the rows.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean resizeCapacity(int maxSizeNew) {

        /** Destroy all indices if any available */
        removeIndices();

        try {
            /** Do them one by one so garbage collection can take place in between
             *without throwing "out of memory" errors.             */
            for (int c=0;c<sizeColumns();c++) {
                String label = getColumnLabel(c);
                int dataType = getColumnDataType(label);
                Object object = attr.get(label);
                //General.showDebug("resizing column with label:: [" + label  + "]");
                switch ( dataType ) {
                    /**
                    case DATA_TYPE_BIT: {
                        //General.showDebug("skipping resize of object type   : [" + dataTypeList[dataType]  + "]");
                        break;
                    }
                     */
                    case DATA_TYPE_LINKEDLIST: {
                        //General.showDebug("resizing object type             : [" + dataTypeList[dataType]  + "]");
                        LinkedListArray lla = (LinkedListArray) object;
                        boolean status = lla.resizeCapacity(maxSizeNew);
                        if ( ! status  ) {
                            General.showError("resizeCapacity on linkedlistarray failed");
                            return false;
                        }
                        break;
                    }
                    case DATA_TYPE_LINKEDLISTINFO: {
                        //General.showDebug("resizing object type             : [" + dataTypeList[dataType]  + "]");
                        LinkedListArrayInfo llai = (LinkedListArrayInfo) object;
                        boolean status = llai.resizeCapacity(maxSizeNew);
                        if ( ! status  ) {
                            General.showError("resizeCapacity on linkedlistarrayinfo failed");
                            return false;
                        }
                        break;
                    }
                    default: {
                        //General.showDebug("resizing object type             : [" + dataTypeList[dataType]  + "]");
                        Object object_new = PrimitiveArray.resizeArray( object, maxSizeNew);
                        if ( object_new == null ) {
                            General.showError("resizeArray failed; perhaps columns are of different size now; inconsistent state of program; stop program if possible.");
                            return false;
                        }
                        if ( ! PrimitiveArray.fillArrayNulls( object_new, sizeMax, maxSizeNew) ) {
                            General.showError("Failed to fill array extension with default nill values.");
                            return false;
                        }
                        attr.put(label, object_new ); // does a replace.
                    }
                }
            }
            /**hash = PrimitiveArray.resizeArray( hash, maxSizeNew);
            if ( hash == null ) {
                General.showError("resizeArray failed; perhaps columns are of different size now; inconsistent state of program; stop program if possible.");
                return false;
            }

            if ( ! PrimitiveArray.fillArrayNulls( hash, sizeMax, maxSizeNew) ) {
                General.showError("Failed to fill array extension with default nill values.");
                return false;
            }
             */
        } catch ( OutOfMemoryError e ) {
            // Maybe later recover gracefully without resizing and returning false;
            General.doOutOfMemoryExit(e);
        }
        sizeMax = maxSizeNew;
        // Let RelationSet parent know that the size changed (if parent exists).
        if ( relationSetParent != null ) {
            relationSetParent.doSizeUpdateRelation(name);
        }
        return true;
    }


    /** Grow the list to have the specified maximum capacity. If the current
     * capacity is higher than the requested, nothing will change.
     * @param maxSizeNew New maximum size of the rows.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean ensureCapacity(int maxSizeNew) {

        /** Check if the change is as expected:
         *-a- In the right direction +*+ or -*- is always positive.
         *-b- Some change at all         */
        if (maxSizeNew <= sizeMax ) {
            General.showWarning("Current size is larger than (or equal to) the requested size. Not resizing.");
            return true;
        }

        boolean status = resizeCapacity(maxSizeNew);
        return status;
    }

    /** Very fast method intended. Returns false if the data type given is
     *  DATA_TYPE_LINKEDLIST or DATA_TYPE_LINKEDLISTINFO.
     */
    public boolean equalElements( Object col, int dataType, int r1, int r2 ) {
        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = (BitSet) col;
                if ( temp.get(r1) == temp.get(r2) ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = (byte[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) col;
                if ( temp[r1] == temp[r2] ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_STRING: {
                String[] temp = (String[]) col;
                if ( temp[r1] == null ) {
                    if ( temp[r2] == null ) {
                        return true;
                    }
                    return false;
                }
                if ( temp[r1].equals(temp[r2]) ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_STRINGNR: {
                String[] temp = (String[]) col;
                if ( temp[r1] == null ) {
                    if ( temp[r2] == null ) {
                        return true;
                    }
                    return false;
                }
                if ( temp[r1].equals(temp[r2]) ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = (Object[]) col;
                if ( temp[r1] == null ) {
                    if ( temp[r2] == null ) {
                        return true;
                    }
                    return false;
                }
                if ( temp[r1].equals(temp[r2]) ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_LINKEDLIST: {
                General.showWarning("In Relation.equalElements encountered uncoded DATA_TYPE_LINKEDLIST");
                return false;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                General.showWarning("In Relation.equalElements encountered uncoded DATA_TYPE_LINKEDLISTINFO");
                return false;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                int[][] temp = (int[][]) col;
                if ( temp[r1] == null ) {
                    if ( temp[r2] == null ) {
                        return true;
                    }
                    return false;
                }
                if ( temp[r1].equals(temp[r2]) ) {
                    return true;
                }
                return false;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                float[][] temp = (float[][]) col;
                if ( temp[r1] == null ) {
                    if ( temp[r2] == null ) {
                        return true;
                    }
                    return false;
                }
                if ( temp[r1].equals(temp[r2]) ) {
                    return true;
                }
                return false;
            }
        }
        General.showError("code bug in equalElements for data type id: [" + dataType  + "]");
        General.showError("unknown type. Known are: " +
            Strings.concatenate( dataTypeList, "," ) );
        return false;
    }

    /** Same as namesake method this time based on the percentage given.
     * The maximum number of rows allowed will be increased by a percentage as given.
     * @param growthPercentage The growth rate.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean ensureCapacityByPercentage(float growthPercentage) {
        int maxSizeNew = (int) ( sizeMax * ( 1.0f + growthPercentage/100.0f ));
        if (maxSizeNew == sizeMax ) {
            General.showWarning("growth should be at least one; doing just that.");
            maxSizeNew++;
        }
        if ((growthPercentage *  (maxSizeNew - sizeMax) < 0 )) {
            General.showError("inconsistent growth:");
            General.showError("growthPercentage : "+growthPercentage);
            General.showError("maxsize was : "+sizeMax);
            General.showError("maxsize is  : "+maxSizeNew);
            return false;
        }
        if (growthPercentage  < 0 ) {
            General.showError("code error, growth should be positive for now.");
            General.showError("growthPercentage : "+growthPercentage);
            General.showError("maxsize was : "+sizeMax);
            General.showError("maxsize is  : "+maxSizeNew);
            return false;
        }
        return ensureCapacity( maxSizeNew );
    }

    /** Append a DATA_TYPE_STRINGNR column without fkc.*/
    public boolean insertColumn(String label) {
        return insertColumn( label, DATA_TYPE_STRINGNR, null);
    }

    /** Append a column using namesake method.*/
    public boolean insertColumn(String label, int dataType, ForeignKeyConstr foreignKeyConstr) {
        return insertColumn( sizeColumns(), label, dataType, foreignKeyConstr);
    }

    /** Create new int[] in a relation with fixed size for a list of rows.
     */
    public static boolean createIntArrays( Relation relation, String columnLabel, BitSet todo, int intArraySize ) {
        if ( relation.getColumnDataType( columnLabel ) != DATA_TYPE_ARRAY_OF_INT) {
            General.showError("Failed to get int[][] because data type doesn't match.");
            return false;
        }
        int[][] LoL = (int[][]) relation.getColumn( columnLabel );
        for (int i=todo.nextSetBit(0); i>=0; i=todo.nextSetBit(i+1))  {
            int[] L = new int[intArraySize];
            Arrays.fill( L, Defs.NULL_INT); // little bit slower but worth the diff.
            LoL[ i ] = L;
        }
        return true;
    }


    /** Add a column to keep track of the ordering of the rows. */
    public boolean addColumnForOverallOrder() {
        if ( getColumnIdx(DEFAULT_ATTRIBUTE_ORDER_ID) >= 0 ) {
            General.showWarning("Already an order column present.");
            if ( getColumnIdx(DEFAULT_ATTRIBUTE_ORDER_ID) == 0 ) {
                return true;
            } else {
                General.showError("Order column present is not at required first position.");
                return false;
            }
        }
        insertColumn(0, DEFAULT_ATTRIBUTE_ORDER_ID, DATA_TYPE_INT, null);
        return true;
    }

    /** Remove all but the given columns. Columns will be deleted in the reverse order they
     *'re present.
     * Failure to do a column delete is reported and the deletions will stop.
     */
    public boolean keepOnlyColumns( ArrayList columnNames ) {
        for (int i=columnOrder.size()-1;i>=0;i--) {
            String label = (String) columnOrder.get(i);
            //check is done using equals method of the objects for correct string comparison
            if ( ! columnNames.contains( label ) ) {
                Object column = removeColumn(label);
                if ( column == null ) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     *The ForeignKeyConstrSet and namesAndValues may be null.
     * @param index New column index. All others shift to the right.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean insertColumnSet(int index, HashMap namesAndTypes, ArrayList order,
        HashMap namesAndValues, ForeignKeyConstrSet foreignKeyConstrSet) {

        if ( namesAndValues == null ) {
            namesAndValues = new HashMap();
        }
//        General.showDebug("namesAndTypes keys: " + Strings.toString( namesAndTypes.keySet()));
//        General.showDebug("order:              " + Strings.toString( namesAndTypes));
        int columnNewSize = order.size();
        /** Consistency checks */
        if ( namesAndTypes.size() != columnNewSize ||
             namesAndValues.size() > columnNewSize ) {
            General.showError("In this relation:" + this);
            General.showError("failed to insert set of columns because input is inconsistent.");
            General.showError("namesAndTypes().size(" +namesAndTypes.size()+ ") != columnNewSize("+columnNewSize+") or");
            General.showError("namesAndValues().size("+namesAndValues.size()+") > columnNewSize(" +columnNewSize+").");
            return false;
        }


        int c = index;
        for (int col=0;col<columnNewSize;col++) {
            String label = (String) order.get(col);
            int dataType = ((Integer) namesAndTypes.get( label )).intValue();
            ForeignKeyConstr foreignKeyConstr = null;
            if ( foreignKeyConstrSet != null ) {
                // The next command will usually set the variable to null which indicates
                // the column is not a key into a foreign relation
                foreignKeyConstr = foreignKeyConstrSet.getForeignKeyConstrFrom(name, label);
            }
            boolean status = insertColumn(c, label, dataType, foreignKeyConstr);
            if ( ! status ) {
                General.showError("failed to insert a new column for column number: "+c+"; not inserting more columns now");
                return false;
            }
            if ( ! namesAndValues.containsKey(label) ) {
                //General.showDebug("NOT setting a standard value for column with label: " + label);
            } else {
                Object value = namesAndValues.get( label );
                //General.showDebug("Setting a standard value: " + value + " for column with label: " + label);
                switch ( dataType ) {
                    case DATA_TYPE_BIT:
                    case DATA_TYPE_CHAR:
                    case DATA_TYPE_BYTE:
                    case DATA_TYPE_SHORT:
                    case DATA_TYPE_INT:
                    case DATA_TYPE_FLOAT:
                    case DATA_TYPE_DOUBLE:
                    case DATA_TYPE_STRING:
                    case DATA_TYPE_STRINGNR:
                    case DATA_TYPE_OBJECT: {
                        setValueByColumn(label,value);
                        break;
                    }
                    case DATA_TYPE_LINKEDLIST: {
                        General.showError("code bug in insertColumnSet for label: [" + label + "] and type id: [" + dataType  + "]");
                        General.showError("doesn't make sense to set this type of column to just 1 int.");
                        break;
                    }
                    case DATA_TYPE_LINKEDLISTINFO: {
                        General.showError("code bug in insertColumnSet for label: [" + label + "] and type id: [" + dataType  + "]");
                        General.showError("doesn't make sense to set this type of column to just 1 int.");
                        break;
                    }
                    case DATA_TYPE_ARRAY_OF_INT: {
                        General.showError("code bug in insertColumnSet for label: [" + label + "] and type id: [" + dataType  + "]");
                        General.showError("doesn't make sense to set this type of column to just 1 int.");
                        break;
                    }
                    case DATA_TYPE_ARRAY_OF_FLOAT: {
                        General.showError("code bug in insertColumnSet for label: [" + label + "] and type id: [" + dataType  + "]");
                        General.showError("doesn't make sense to set this type of column to just 1 int.");
                        break;
                    }
                    default: {
                        General.showError("code bug in insertColumnSet for label: [" + label + "] and type id: [" + dataType  + "]");
                        General.showError("unknown type. Known are: " +
                            Strings.concatenate( dataTypeList, "," ) );
                        return false;
                    }
                }
            }
            c++;
        }
        return true;
    }

    /**
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean insertColumnSetFromCsvFile(String dtd_file_name) {

        General.showOutput("Reading from dtd file    : " + dtd_file_name);
        Relation dtd = null;

        try {
            dtd = new Relation("dtd_" + dtd_file_name, dbms);
            if ( ! dtd.readCsvFile(dtd_file_name, false, null)) {
                General.showError("failed to read dtd");
                return false;
            }
            dtd.renameColumn(0, "columnName");
            dtd.renameColumn(1, "columnDataType");

            int columns     = dtd.used.cardinality();
            for (int c=0;c<columns;c++) {
                String columnName  = dtd.getValueString(c, "columnName");
                String dataTypeStr = dtd.getValueString(c, "columnDataType");

                insertColumn(-1,columnName,DATA_TYPE_STRING,null);
                int dataType = dataTypeArrayList.indexOf( dataTypeStr );
                if ( dataType < 0 ) {
                    General.showError("For column: " + columnName + " ("+c+")");
                    General.showError("Data type listed in dtd that is not supported: " + dataTypeStr + " Skipping read.");
                    General.showWarning("Known are: " + Strings.concatenate( dataTypeList, "," ) );
                    continue;
                }
                if ( ! dataTypeStr.equals( "STRING") ) {
                    convertDataTypeColumn(columnName, dataType, null );
                }
                //General.showDebug("Inserted column: " + columnName + " of type: " + dataTypeStr);
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }


    /** Replace a column useful for changing the data type of the column.
     * @param dataType Precoded integer data type.
     * @param column This is the column object except in the case where dataType is STRINGNR
     *  because then two objects are encapsulated.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean replaceColumn(String label, int dataType, Object column) {
        if ( ! hasColumn( label )  ) {
            General.showError("in -a- replaceColumn label NOT already present for label: [" +
                label + "] and type: [" + dataTypeList[dataType] + "]. So didn't replace with another column with this label.");
            return false;
        }
        int index = getColumnIdx( label );
        if ( index < 0 ) {
            General.showError("-b- in replaceColumn label NOT already present for label: [" +
                label + "] and type: [" + dataTypeList[dataType] + "]. So didn't replace with another column with this label.");
            return false;
        }

        /** Removing the index for this column */
        if ( containsIndex(label) ) {
            removeIndex(label);
        }

        // Do explicit remove if the previous data type was stringnr because new type might not be so.
        if ( getColumnDataType(label) == DATA_TYPE_STRINGNR ) {
            attr.remove( label + STANDARD_STRING_SET_POSTFIX );
        }

        // Replace the column. The old one will be gc-ed.
        if ( dataType == DATA_TYPE_STRINGNR ) {
            Object realColumn       =              ((ArrayList) column).get(0);
            StringSet ss            = (StringSet) (((ArrayList) column).get(1));
            attr.put( label, realColumn);
            attr.put( label + STANDARD_STRING_SET_POSTFIX, ss );
        } else {
            attr.put( label, column );
        }

        columnDataType.put(label, new Integer(dataType) );
        return true;
    }

    /** Returns -1 on failure. */
    public int getColumnIdx( String label ) {
        return columnOrder.indexOf( label );
    }

    /** Returns DATA_TYPE_INVALID on failure. Also able to return int for the physical column
     *when specified in the default way.
     */
    public int getColumnDataType( String label ) {
        // Cover the case where the physical column is specified.
        if ( label == null ) {
            return DATA_TYPE_INT;
        }
        if ( label.equals(DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
            General.showCodeBug("Shouldn't use: " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + " any more to indicate physical column; use null instead");
            return DATA_TYPE_INVALID;
        }

        if ( ! columnDataType.containsKey( label )) {
            return DATA_TYPE_INVALID;
        }

        Object o = columnDataType.get(label);
        if ( o == null ) {
            General.showError("code bug; got null for data type for column with label: "+label);
            return DATA_TYPE_INVALID;
        }
        int dataType = ((Integer) o).intValue();
        if ( dataType == DATA_TYPE_INVALID ) {
            General.showError("code bug; got DATA_TYPE_INVALID for column with label: "+label);
            return DATA_TYPE_INVALID;
        }
        return dataType;
    }

    /** Returns the equivalent SQL data type for this column.
     */
    public String getColumnDataTypeSQL( String label ) {
        int dataType = getColumnDataType( label );
        if ( dataType == DATA_TYPE_INVALID) {
            General.showError("Failed to get valid internal data type");
            return null;
        }
        return dataTypeListSQL[dataType];
    }

    /** Returns null on failure. */
    public String getColumnLabel( int index ) {
        if ( ! isValidColumnIdx(index) ) {
            General.showError("in getColumnLabel: given index is not valid for columns: " + index);
            return null;
        }
        String label = (String) columnOrder.get(index);
        return label;
    }

    /** Adds an index for a specific column of either single record ids (rids) or [rids].
     */
    public boolean addIndex( String label, int index_type ) {
        int dataType = getColumnDataType(label);
        Index index = null;
        boolean status = true;

        if ( ! Index.isValidIndexType( index_type ) ) {
            Index.showInvalidIndexType(index_type);
            return false;
        }
        //General.showDebug("index_type id : " + index_type );
        //General.showDebug("index_type is : " + Index.index_type_names[ index_type ]);
        // Initialize the right type of index.
        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                if ( index_type == Index.INDEX_TYPE_SORTED ) { // sorting or hashing doesn't make a difference for this data type.
                    index = new IndexHashedBitToMany();
                } else if ( index_type == Index.INDEX_TYPE_HASHED ) {
                    index = new IndexHashedBitToMany();
                } else {
                    General.showError("code bug no index for bit in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
                    status = false;
                }
                break;
            }
            case DATA_TYPE_SHORT: {
                if ( index_type == Index.INDEX_TYPE_SORTED ) {
                    index = new IndexSortedShort();
                } else {
                    General.showError("code bug no hashed index for shorts in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
                    status = false;
                }
                break;
            }
            case DATA_TYPE_INT: {
                if ( index_type == Index.INDEX_TYPE_SORTED ) {
                    index = new IndexSortedInt();
                } else {
                    index = new IndexHashedIntToMany();
                }
                break;
            }
            case DATA_TYPE_STRINGNR:
            case DATA_TYPE_STRING: {
                if ( index_type == Index.INDEX_TYPE_SORTED ) {
                    index = new IndexSortedString();
                } else {
                    index = new IndexHashedObjectToMany();
                }
                break;
            }
            case DATA_TYPE_FLOAT: {
                if ( index_type == Index.INDEX_TYPE_SORTED ) {
                    index = new IndexSortedFloat();
                } else {
                    General.showError("code bug no hashed index for floats in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
                    status = false;
                }
                break;
            }
            default: {
                General.showError("code bug data type not supported for indexing in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
                status = false;
            }
        }

        /** Check 1 type of error. */
        if ( ! status ) {
            General.showError("some overall bug in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
            return false;
        }

        if ( index == null ) {
            General.showError("code bug in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
            General.showError("index could not be created even though the code says so.");
            return false;
        }

        status = index.updateIndex(this, label);
        if ( ! status ) {
            General.showError("code bug in addIndex for label: [" + label + "] and type id: [" + dataType  + "]");
            General.showError("index could not be updated.");
            return false;
        }

        // All's well add the index to the relation class.
        Index[] al = (Index[]) indices.get( label );
        if ( al == null ) {
            al = new Index[Index.INDEX_TYPE_COUNT];
            indices.put( label, al );
        }
        al[index_type] = index;
        return true;
    }



    /** Insert a column for a certain variable type; before the given position.
     * @param index New column index. All others shift to the right. If -1 it will append at the end.
     * @param label Column name; has to be unique.
     * @param dataType Precoded integer data type.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean insertColumn(int index, String label, int dataType, ForeignKeyConstr foreignKeyConstr ) {

        if ( hasColumn( label )  ) {
            General.showWarning("in insertColumn label already present for label: [" +
                label + "] and type: [" + dataTypeList[dataType] + "]. So didn't add another column with this label.");
            return false;
        }

        if ( index > sizeColumns() ) {
            General.showWarning("in insertColumn the given index for the new column ("+index+") is larger than the current size: [" +
                sizeColumns() + "] that's impossible. No column added.");
            return false;
        }

        if ( index == -1 ) {
            index = sizeColumns();
        }

        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = new BitSet();
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_CHAR: {
                char[] temp = new char[sizeMax];
                Arrays.fill(temp, Defs.NULL_CHAR);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = new byte[sizeMax];
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = new short[sizeMax];
                Arrays.fill(temp, Defs.NULL_SHORT);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_INT: {
                int[] temp = new int[sizeMax];
                Arrays.fill(temp, Defs.NULL_INT);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = new float[sizeMax];
                Arrays.fill(temp, Defs.NULL_FLOAT);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = new double[sizeMax];
                Arrays.fill(temp, Defs.NULL_DOUBLE);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_LINKEDLIST: {
                LinkedListArray temp = new LinkedListArray(sizeMax);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                LinkedListArrayInfo temp = new LinkedListArrayInfo(sizeMax);
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_STRING: {
                String[] temp = new String[sizeMax];
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_STRINGNR: {
                String[] temp = new String[sizeMax];
                attr.put( label, temp );
                // For stringnr we always use the StringSet class which gives best performance for
                // collections of strings with high redundancy.
                StringSet tempSet = new StringSet();
                attr.put( label + STANDARD_STRING_SET_POSTFIX, tempSet );
                break;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                // Note that the inner arrays aren't initialized in order to save on objects.
                // This step just creates one array and space for sizeMax pointers to arrays to be created.
                int[][] temp = new int[sizeMax][];
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                // Note that the inner arrays aren't initialized in order to save on objects.
                // This step just creates one array and space for sizeMax pointers to arrays to be created.
                float[][] temp = new float[sizeMax][];
                attr.put( label, temp );
                break;
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = new Object[sizeMax];
                attr.put( label, temp );
                break;
            }
            default: {
                General.showError("code bug in insertColumn on relation: " + name + " for label: [" + label + "] and type id: [" + dataType  + "]");
                General.showError("unknown type. Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return false;
            }
        }

        columnOrder.add( index, label );
        columnDataType.put(label, new Integer(dataType) );
        if ( foreignKeyConstr != null ) {
            boolean status = dbms.foreignKeyConstrSet.addForeignKeyConstr( foreignKeyConstr );
            if ( ! status ) {
                General.showError("failed to add foreign key constraint to dbms.");
                return false;
            }
        }

        // If the column is the column designated as the selection column
        // then fill it with the default value.
        if ( label.equals( DEFAULT_ATTRIBUTE_SELECTED ) ) {
            setValueByColumn( label, Boolean.valueOf(DEFAULT_ATTRIBUTE_SELECTED_VALUE));
        }
        return true;
    }


    /** Set the values in the primitive Arrays to their defaults given the start
     * index and end indices. This method is the most efficient one!
     * @param idxStart inclusive
     * @param idxEnd exclusive
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValuesToDefaultByRowRange( int idxStart, int idxEnd ) {

        /** Destroy all indices if available */
        removeIndices();

        int sizeColumns = sizeColumns();
        for (int c=0;c<sizeColumns;c++) {
            String label = getColumnLabel(c);
            int dataType = getColumnDataType(label);
            switch ( dataType ) {
                case DATA_TYPE_BIT: {
                    BitSet temp = (BitSet) attr.get( label );
                    temp.clear( idxStart, idxEnd );
                    break;
                }
                case DATA_TYPE_CHAR: {
                    char[] temp = (char[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, Defs.NULL_CHAR);
                    break;
                }
                case DATA_TYPE_BYTE: {
                    byte[] temp = (byte[]) attr.get( label );
                    byte bye = 0; // don't know why this is needed.
                    Arrays.fill(temp, idxStart, idxEnd, bye);
                    break;
                }
                case DATA_TYPE_SHORT: {
                    short[] temp = (short[]) attr.get( label );
                    //General.showOutput("Size of int array is: " + temp.length);
                    Arrays.fill(temp, idxStart, idxEnd, Defs.NULL_SHORT);
                    break;
                }
                case DATA_TYPE_INT: {
                    int[] temp = (int[]) attr.get( label );
                    //General.showOutput("Size of int array is: " + temp.length);
                    Arrays.fill(temp, idxStart, idxEnd, Defs.NULL_INT);
                    break;
                }
                case DATA_TYPE_FLOAT: {
                    float[] temp = (float[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, Defs.NULL_FLOAT);
                    break;
                }
                case DATA_TYPE_DOUBLE: {
                    double[] temp = (double[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, Defs.NULL_DOUBLE);
                    break;
                }
                case DATA_TYPE_LINKEDLIST: {
                    LinkedListArray temp = (LinkedListArray) attr.get( label );
                    temp.setValuesToDefaultByRowRange(idxStart, idxEnd);
                    break;
                }
                case DATA_TYPE_LINKEDLISTINFO: {
                    LinkedListArrayInfo temp = (LinkedListArrayInfo) attr.get( label );
                    temp.setValuesToDefaultByRowRange(idxStart, idxEnd);
                    break;
                }
                case DATA_TYPE_STRING: {
                    String[] temp = (String[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, null);
                    break;
                }
                case DATA_TYPE_STRINGNR: {
                    String[] temp = (String[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, null);
                    break;
                }
                case DATA_TYPE_ARRAY_OF_INT: {
                    int[][] temp = (int[][]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, null);
                    break;
                }
                case DATA_TYPE_ARRAY_OF_FLOAT: {
                    float[][] temp = (float[][]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, null);
                    break;
                }
                case DATA_TYPE_OBJECT: {
                    Object[] temp = (Object[]) attr.get( label );
                    Arrays.fill(temp, idxStart, idxEnd, null);
                    break;
                }
                default: {
                    General.showError("code bug in setValuesToDefaultByRowRange for column label: [" + label + "] and type id: [" + dataType  + "]");
                    General.showError("unknown type. Known are: " +
                        Strings.concatenate( dataTypeList, "," ) );
                    return false;
                }
            }
        }
        return true;
    }

    /** Set the values in the primitive Arrays to their defaults given the start
     * index and end indices. This method is not as efficient as the one where
     * the start and end indices are given but still quite good!
     * @param idxList List of rows to do.
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValuesToDefaultByRowRange( int[] idxList ) {

        /** Destroy all indices if available */
        removeIndices();

        int sizeColumns = sizeColumns();
        int sizeList = idxList.length;
        for (int c=0;c<sizeColumns;c++) {
            String label = getColumnLabel(c);
            int dataType = getColumnDataType(label);
            switch ( dataType ) {
                case DATA_TYPE_BIT: {
                    BitSet temp = (BitSet) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp.clear(idxList[i]);
                    }
                    break;
                }
                case DATA_TYPE_CHAR: {
                    char[] temp = (char[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = Defs.NULL_CHAR;
                    }
                    break;
                }
                case DATA_TYPE_BYTE: {
                    byte[] temp = (byte[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = 0;
                    }
                    break;
                }
                case DATA_TYPE_SHORT: {
                    short[] temp = (short[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = Defs.NULL_SHORT;
                    }
                    break;
                }
                case DATA_TYPE_INT: {
                    int[] temp = (int[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = Defs.NULL_INT;
                    }
                    break;
                }
                case DATA_TYPE_FLOAT: {
                    float[] temp = (float[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = Defs.NULL_FLOAT;
                    }
                    break;
                }
                case DATA_TYPE_DOUBLE: {
                    double[] temp = (double[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = Defs.NULL_DOUBLE;
                    }
                    break;
                }
                case DATA_TYPE_LINKEDLIST: {
                    LinkedListArray temp = (LinkedListArray) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        int idx = idxList[i];
                        temp.forwardList[  idx ] = LinkedListArray.NOT_AN_INDEX;
                        temp.backwardList[ idx ] = LinkedListArray.NOT_AN_INDEX;
                    }
                    break;
                }
                case DATA_TYPE_LINKEDLISTINFO: {
                    LinkedListArrayInfo temp = (LinkedListArrayInfo) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        int idx = idxList[i];
                        temp.firstList[  idx ]  = LinkedListArray.NOT_AN_INDEX;
                        temp.lastList[ idx ]    = LinkedListArray.NOT_AN_INDEX;
                        temp.countList[ idx ]   = 0;
                    }
                    break;
                }
                case DATA_TYPE_STRING: {
                    String[] temp = (String[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = null;
                    }
                    break;
                }
                case DATA_TYPE_STRINGNR: {
                    String[] temp = (String[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = null;
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_INT: {
                    int[][] temp = (int[][]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = null;
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_FLOAT: {
                    float[][] temp = (float[][]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = null;
                    }
                    break;
                }
                case DATA_TYPE_OBJECT: {
                    Object[] temp = (Object[]) attr.get( label );
                    for (int i=0;i<sizeList;i++) {
                        temp[ idxList[i] ] = null;
                    }
                    break;
                }
                default: {
                    General.showError("code bug in setValuesToDefaultByRowRange for column with label: [" + label + "] and type id: [" + dataType  + "]");
                    General.showError("unknown type. Known are: " +
                        Strings.concatenate( dataTypeList, "," ) );
                    return false;
                }
            }
        }
        return true;
    }


    /** Set the values in the primitive Arrays to their defaults given the start
     * index and end indices. This method is just as efficient as the one where
     * the start and end indices are given; very good!
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValuesToDefaultByRowRange( BitSet rowSetToRemove ) {

        /** Destroy all indices if available */
        removeIndices();

        int sizeColumns = sizeColumns();
        //int sizeList = idxList.length;
        for (int c=0;c<sizeColumns;c++) {
            String label = getColumnLabel(c);
            int dataType = getColumnDataType(label);
            switch ( dataType ) {
                case DATA_TYPE_BIT: {
                    BitSet temp = (BitSet) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        temp.clear(i,j);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_CHAR: {
                    char[] temp = (char[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_CHAR);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_BYTE: {
                    byte[] temp = (byte[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,(byte) 0); //stupid cast.
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_SHORT: {
                    short[] temp = (short[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_SHORT);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_INT: {
                    int[] temp = (int[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_INT);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_FLOAT: {
                    float[] temp = (float[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_FLOAT);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_DOUBLE: {
                    double[] temp = (double[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_DOUBLE);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_LINKEDLIST: {
                    LinkedListArray temp = (LinkedListArray) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp.forwardList, i,j,LinkedListArray.NOT_AN_INDEX);
                        Arrays.fill(temp.backwardList,i,j,LinkedListArray.NOT_AN_INDEX);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_LINKEDLISTINFO: {
                    LinkedListArrayInfo temp = (LinkedListArrayInfo) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp.firstList, i,j,LinkedListArray.NOT_AN_INDEX);
                        Arrays.fill(temp.lastList,  i,j,LinkedListArray.NOT_AN_INDEX);
                        Arrays.fill(temp.countList, i,j,0);
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_STRING: {
                    String[] temp = (String[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_STRING_NULL); // just null
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_STRINGNR: {
                    String[] temp = (String[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,Defs.NULL_STRING_NULL); // just null
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_INT: {
                    int[][] temp = getColumnIntList( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,null); // just null
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_FLOAT: {
                    float[][] temp = getColumnFloatList( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,null); // just null
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                case DATA_TYPE_OBJECT: {
                    Object[] temp = (Object[]) attr.get( label );
                    for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
                        int j = rowSetToRemove.nextClearBit(i+1);
                        if ( j < 0 ) j = rowSetToRemove.length();
                        Arrays.fill(temp,i,j,null); // just null
                        i=rowSetToRemove.nextSetBit(j+1);
                    }
                    break;
                }
                default: {
                    General.showError("code bug in setValuesToDefaultByRowRange for column with label: [" + label + "] and type id: [" + dataType  + "]");
                    General.showError("unknown type. Known are: " +
                        Strings.concatenate( dataTypeList, "," ) );
                    return false;
                }
            }
        }
        return true;
    }



    /** Simply calls setValuesToDefaultByRowRange
     * This method is the least efficient one!
     * @param idx Row id.
     * @return <CODE>true</CODE> for success and <CODE>false</CODE> for things like memory problems.
     */
    public boolean setValuesToDefaultByRow( int idx ) {
        int[] idxList = { idx };
        return setValuesToDefaultByRowRange(idxList);
    }


    /** Move the values in the original rows to the new rows which should already
     * be claimed. The values in the new list will obviously be overwritten and
     * the original rows will be deleted.
     * Columns of type linked list will be adjusted according to the mapping done.
     * Access is private because outside usage should not depend on it.
     *
     * @param idxListOrg Row numbers for the from part.
     * @param idxListNew Row numbers for the to part.
     * @return <CODE>true</CODE> for success.
     */
    public boolean moveRows( int[] idxListOrg, int[] idxListNew ) {

        int sizeColumns = sizeColumns();
        int idxListOrgSize = idxListOrg.length;
        int idxListNewSize = idxListNew.length;
        if ( idxListOrgSize != idxListNewSize ) {
            General.showError("org list size :"+idxListOrgSize
                    +" is not new list size: " + idxListNewSize + ". Not copying rows.");
            return false;
        }

        /** Destroy all indices if any available */
        removeIndices();

        for (int c=0;c<sizeColumns;c++) {
            String label = getColumnLabel(c);
            int dataType = getColumnDataType(label);
            switch ( dataType ) {
                case DATA_TYPE_BIT: {
                    BitSet temp = (BitSet) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp.set( idxListNew[r], temp.get( idxListOrg[r] ));
                    }
                    break;
                }
                case DATA_TYPE_CHAR: {
                    char[] temp = (char[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_BYTE: {
                    byte[] temp = (byte[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_SHORT: {
                    short[] temp = (short[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_INT: {
                    int[] temp = (int[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_FLOAT: {
                    float[] temp = (float[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_DOUBLE: {
                    double[] temp = (double[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_LINKEDLIST: {
                    LinkedListArray temp = (LinkedListArray) attr.get( label );
                    temp.moveRows( idxListOrg, idxListNew );// something special to maintain linked list array
                    break;
                }
                case DATA_TYPE_LINKEDLISTINFO: {
                    LinkedListArrayInfo temp = (LinkedListArrayInfo) attr.get( label );
                    temp.moveRows( idxListOrg, idxListNew );// something special to maintain linked list array
                    break;
                }
                case DATA_TYPE_STRING: {
                    String[] temp = (String[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_STRINGNR: {
                    String[] temp = (String[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_INT: {
                    int[][] temp = (int[][]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_ARRAY_OF_FLOAT: {
                    float[][] temp = (float[][]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                case DATA_TYPE_OBJECT: {
                    Object[] temp = (Object[]) attr.get( label );
                    for (int r=0;r<idxListOrgSize;r++) {
                        temp[idxListNew[r]] = temp[idxListOrg[r]];
                    }
                    break;
                }
                default: {
                    General.showError("code bug in moveRows for column id: [" + c + "] and type id: [" + dataType  + "]");
                    General.showError("unknown type. Known are: " +
                        Strings.concatenate( dataTypeList, "," ) );
                    return false;
                }
            }
        }
        /** Remove the original list of rows then but without updating the linked list arrays
         *because those are already done by it's own moveRows method
         */
        removeRows(idxListOrg, false);
        return true;
    }

    /** Low efficiency method; use direct access in stead to set the value of cells by column.
     * This method has to fetch the column every time which is kind of inefficient.
     *Note: For non-redundant strings the value should be interned before calling this routine.
     * @param row Row id.
     * @param label Column label.
     * @param value Value to set to.
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValue( int row, String label, Object value) {
        if ( ! hasColumn( label )  ) {
            General.showWarning("in setValue column not present: [" + label + "]");
            return false;
        }
        int columnIdx = columnOrder.indexOf( label );

        return setValue( row, columnIdx, value);
    }

    public boolean setValue( int row, int columnIdx, int value) {
        return setValue( row, getColumnLabel(columnIdx), new Integer(value));
    }
    public boolean setValue( int row, String label , int value) {
        return setValue( row, label, new Integer(value));
    }
    public boolean setValue( int row, String label , float value) {
        return setValue( row, label, new Float(value));
    }
    public boolean setValue( int row, String label , boolean value) {
        return setValue( row, label, new Boolean(value));
    }

    /** Low efficiency method; use direct access in stead to set the value of cells by column.
     * This method has to fetch the column every time which is kind of inefficient.
     *Note: For non-redundant strings the value should be interned before calling this routine.
     *Note2: If the value is null then NOTHING will be done and true will be returned.
     * @param row Row id.
     * @param value Value to set to.
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValue( int row, int columnIdx, Object value) {
        if ( ! used.get( row )) {
            General.showError("in setValue row index is not in use: [" + row + "]. Not changing value.");
            return false;
        }

        /** Destroy all indices if any available */
        removeIndices();

        String label = getColumnLabel(columnIdx);
        int dataType = getColumnDataType(label);

        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = (BitSet) attr.get( label );
                if ( ! (value instanceof Boolean)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Boolean" );
                    return false;
                }
                boolean valueTemp = ((Boolean) value).booleanValue();
                if ( valueTemp ) {
                    temp.set(row);
                } else {
                    temp.clear(row);
                }
                break;
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) attr.get( label );
                if ( ! (value instanceof Character)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Character" );
                    return false;
                }
                char valueInType = Defs.NULL_CHAR;
                if ( value != null ) {
                    valueInType = ((Character) value).charValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = (byte[]) attr.get( label );
                if ( ! (value instanceof Byte)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Byte" );
                    return false;
                }
                byte valueInType = Defs.NULL_BYTE;
                if ( value != null ) {
                    valueInType = ((Byte) value).byteValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) attr.get( label );
                if ( ! (value instanceof Short )) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Short" );
                    return false;
                }
                short valueInType = Defs.NULL_SHORT;
                if ( value != null ) {
                    valueInType = ((Short) value).shortValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) attr.get( label );
                if ( ! (value instanceof Integer)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Integer" );
                    return false;
                }
                int valueInType = Defs.NULL_INT;
                if ( value != null ) {
                    valueInType = ((Integer) value).intValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) attr.get( label );
                if ( ! (value instanceof Float)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Float" );
                    return false;
                }
                float valueInType = Defs.NULL_FLOAT;
                if ( value != null ) {
                    valueInType = ((Float) value).floatValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) attr.get( label );
                if ( ! (value instanceof Double)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Double" );
                    return false;
                }
                double valueInType = Defs.NULL_DOUBLE;
                if ( value != null ) {
                    valueInType = ((Double) value).doubleValue();
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_LINKEDLIST: {
                General.showWarning("in setValue for column with label: [" + label + "]");
                General.showWarning("given value is LinkedList can't set value for this type.");
                return false;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                General.showWarning("in setValue for column with label: [" + label + "]");
                General.showWarning("given value is LinkedListInfo can't set value for this type.");
                return false;
            }
            case DATA_TYPE_STRING:
            case DATA_TYPE_STRINGNR: {
                String[] temp = (String[]) attr.get( label );
                if ( ! (value instanceof String)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is String" );
                    return false;
                }
                String valueInType = Defs.NULL_STRING_NULL;
                if ( value != null ) {
                    valueInType = (String) value;
                }
                temp[row] = valueInType;
                break;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                int[][] temp = (int[][]) attr.get( label );
                if ( ! (value instanceof int[])) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is int[]" );
                    return false;
                }
                temp[row] = (int[]) value;
                break;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                float[][] temp = (float[][]) attr.get( label );
                if ( ! (value instanceof float[])) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is float[]" );
                    return false;
                }
                temp[row] = (float[]) value;
                break;
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = (Object[]) attr.get( label );
                if ( ! (value instanceof Object)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is of type: " + value.getClass().getName() + " but column is Object" );
                    return false;
                }
                temp[row] = value; // no cast needed..
                break;
            }
            default: {
                General.showError("code bug in setValue for value: [" + value + "] and type id: [" + dataType  + "]");
                General.showError("unknown type. Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return false;
            }
        }
        return true;
    }

    public boolean setValueByColumn(String label, int value) {
        return setValueByColumn(label, new Integer( value));
    }

    public boolean setValueByColumn(String label, float value) {
        return setValueByColumn(label, new Float( value));
    }


    /** Set to given value for all rows of specified column.
     * For efficiency the attributes will be set for
     * all rows even those not in use at the moment.
     * @param label Column label specifies which column to operate on.
     * @param value Value to set to.
     * @return <CODE>true</CODE> for success.
     */
    public boolean setValueByColumn(String label, Object value) {
        if ( ! hasColumn( label )  ) {
            General.showWarning("in setValueByColumn label not present: [" + label + "]");
            return false;
        }

        /** Destroy index if available */
        if ( containsIndex(label) ) removeIndex(label);

        int dataType = getColumnDataType(label);

        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = (BitSet) attr.get( label );
                if ( ! (value instanceof Boolean)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Boolean like column but is of type: " + value.getClass().getName());
                    return false;
                }
                boolean valueTemp = ((Boolean) value).booleanValue();
                temp.set(0,temp.length(),valueTemp);
                break;
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) attr.get( label );
                if ( ! (value instanceof Character)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Character like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Character) value).charValue());
                break;
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = (byte[]) attr.get( label );
                if ( ! (value instanceof Byte)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Byte like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Byte) value).byteValue());
                break;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) attr.get( label );
                if ( ! (value instanceof Short)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Short like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Short) value).shortValue());
                break;
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) attr.get( label );
                if ( ! (value instanceof Integer)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Integer like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Integer) value).intValue());
                break;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) attr.get( label );
                if ( ! (value instanceof Float)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Float like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Float) value).floatValue());
                break;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) attr.get( label );
                if ( ! (value instanceof Double)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Double like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, ((Double) value).doubleValue());
                break;
            }
            case DATA_TYPE_LINKEDLIST: {
                General.showWarning("in setValue for column with label: [" + label + "]");
                General.showWarning("given value is not of LinkedList can't set value for this type.");
                return false;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                General.showWarning("in setValue for column with label: [" + label + "]");
                General.showWarning("given value is not of LinkedListInfo can't set value for this type.");
                return false;
            }
            case DATA_TYPE_STRING: {
                String[] temp = (String[]) attr.get( label );
                if ( ! (value instanceof String)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of String like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, value);
                break;
            }
            case DATA_TYPE_STRINGNR: {
                String[] temp = (String[]) attr.get( label );
                if ( ! (value instanceof String)) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of String like column but is of type: " + value.getClass().getName());
                    return false;
                }
                StringSet ss = (StringSet) attr.get( label + STANDARD_STRING_SET_POSTFIX);
                if ( ss != null ) {
                    value = ss.intern( (String) value );
                } else {
                    General.showWarning("No stringset found for column with label: " + label );
                    General.showWarning("Will use a possibly redundant string.");
                }
                Arrays.fill( temp, value);
                break;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                int[][] temp = (int[][]) attr.get( label );
                if ( ! (value instanceof int[])) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of int[] like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, value);
                break;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                float[][] temp = (float[][]) attr.get( label );
                if ( ! (value instanceof float[])) {
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of float[] like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, value);
                break;
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = (Object[]) attr.get( label );
                if ( ! (value instanceof Object)) { // Actually impossible
                    General.showWarning("in setValue for column with label: [" + label + "]");
                    General.showWarning("given value is not of Object like column but is of type: " + value.getClass().getName());
                    return false;
                }
                Arrays.fill( temp, value);
                break;
            }
            default: {
                General.showError("code bug in setValue for value: [" + value + "] and type id: [" + dataType  + "]");
                General.showError("unknown type. Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @see #removeRows
     */
    public boolean removeRow(int row, boolean updateLinkedLists) {
        if ( ! used.get( row )) {
            General.showError("in removeRow row already deleted for index: [" + row + "]. Not changing that.");
            return false;
        }

        /** Destroy all indices if any available */
        removeIndices();

        if ( updateLinkedLists ) {
            for (int i=0;i<columnDataType.size();i++) {
                String label = getColumnLabel(i);
                int dataType = getColumnDataType(label);
                if ( dataType == DATA_TYPE_LINKEDLIST ) {
                    LinkedListArray ll = (LinkedListArray) getColumn( i );
                    ll.removeRow( row );
                }
            }
        }
        used.clear(row);
        reserved.clear(row);
        sizeRows--;

        boolean status = setValuesToDefaultByRow(row);
        if ( !status ) {
            General.showError("failed to setValuesToDefaultByRow for row:"+ row);
            return false;
        }
        /** Make the sort conditional on whether or not there is an order column
         */
        if ( containsColumn(DEFAULT_ATTRIBUTE_ORDER_ID) ) {
            status = reorderRows();
            if ( !status ) {
                General.showError("failed to reorderRows after removal of row:"+ row);
                return false;
            }
        }
        return true;
    }


    /**
     *
     * @see #removeRowsCascading
     */
    public boolean removeRowCascading(int rowToDelete, boolean updateLinkedLists) {
        if ( rowToDelete >= 0 && rowToDelete < sizeMax && used.get(rowToDelete) ) {
            BitSet rowSet = new BitSet();
            rowSet.set( rowToDelete );
            return removeRowsCascading(rowSet, updateLinkedLists);
        }
        General.showError("in removeRowCascading for rowToDelete: " + rowToDelete);
        General.showError("the remove failed to be done.");
        return false;
    }


    /**
     *
     * @see DBMS#removeRowsCascading
     */
    public boolean removeRowsCascading(BitSet rowSet, boolean updateLinkedLists) {
        boolean status = dbms.removeRowsCascading(name, rowSet);
        if ( ! status ) {
            General.showError("in Relation.removeRowCascading.");
        }
        return status;
    }

    /**
     *
     * @see DBMS#removeRowsCascading
     */
    public boolean removeRowsCascading(int[] rowList, boolean updateLinkedLists) {
        return removeRowsCascading( PrimitiveArray.toBitSet(rowList, sizeMax), updateLinkedLists);
    }

    /**
     *
     * @see DBMS#removeRowsCascading
     */
    public boolean removeRowsCascading(int rid, boolean updateLinkedLists) {
        return removeRowsCascading( new int[] { rid }, updateLinkedLists);
    }

    /**
     *
     * @see DBMS#removeRowsCascading
     */
    public boolean removeRowsCascading(IntArrayList rowList, boolean updateLinkedLists) {
        return removeRowsCascading( PrimitiveArray.toBitSet(rowList, sizeMax), updateLinkedLists);
    }


    /**
     *
     * @see #removeRows
     */
    public boolean removeRows(int[] rowList, boolean updateLinkedLists ) {
        return removeRows( PrimitiveArray.toBitSet( rowList, rowList[rowList.length-1] ), updateLinkedLists, true );
    }

    /** Convenience method see removeRows
     */
    public boolean removeAllRowsExcept(BitSet rowSetToKeep, boolean updateLinkedLists, boolean resetDefaultValues ) {
        BitSet rowSetToRemove =(BitSet) rowSetToKeep.clone();
        rowSetToRemove.flip(0,sizeMax);
        rowSetToRemove.and(used); // don't remove those not in use (they're already set to false
        return removeRows(rowSetToRemove, updateLinkedLists, resetDefaultValues );
    }


   /**
     * Remove will not just mark the item as not in use. All elements in the many
     * arrays are initialized to their standard values unless specified otherwise.<BR>
     * If more rows than exist are tried to be removed then the argument will be
     * adjusted and a warning is issued detailing the problem.<BR>
     * @return <CODE>true</CODE> for success.
     * @param rowSetToRemove Will not be changed unless you picked the BitSets used or reserved for
    *them which do need to be adjusted.
     */
    public boolean removeRows(BitSet rowSetToRemove, boolean updateLinkedLists, boolean resetDefaultValues ) {
        boolean status = true;

        /** If the input is the same as the used or reserved BitSet then
         *issue a warning but continue for debugging purposes
         *
        if ( rowSetToRemove == used ) {
            General.showWarning("In Relation.removeRows the set to remove is the same as the Relation.used for relation with name: "+name);
        }
        if ( rowSetToRemove == reserved ) {
            General.showWarning("In Relation.removeRows the set to remove is the same as the Relation.reserved for relation with name: "+name);
        }
         */

        /** Make a copy anyway because it needs to be and-ed with the used set.
         */
        rowSetToRemove = (BitSet) rowSetToRemove.clone();
        rowSetToRemove.and(used);

        /** Destroy all indices if any available */
        removeIndices();

        if ( updateLinkedLists && (rowSetToRemove.cardinality() > 0)) {
            // TODO recode this if linked lists will be used in the future.
            int[] rowList = PrimitiveArray.toIntArray( rowSetToRemove );
            for (int i=0;i<columnDataType.size();i++) {
                String label = getColumnLabel(i);
                int dataType = getColumnDataType(label);
                if ( dataType == DATA_TYPE_LINKEDLIST ) {
                    LinkedListArray ll = (LinkedListArray) getColumn( i );
                    boolean status_new  = ll.removeRows( rowList );
                    if ( ! status_new ) {
                        General.showError("in LinkedListArray.removeRows");
                        return false;
                    }
                }
            }
        }

        int countDeleted = 0; // Used for debugging
        for (int i=rowSetToRemove.nextSetBit(0); i>=0; )  {
            // Find stretch of set bits indicating rows that need to be removed.
            int j = rowSetToRemove.nextClearBit(i+1);

            if ( j >= 0 ) {
                // Remove from row i to j-1
            } else {
                // Remove from row i to end
                j = sizeRows;
            }
            int rowsToDelete = j - i;
            used.clear(i,j);
            reserved.clear(i,j);
            sizeRows        -= rowsToDelete;
            countDeleted    += rowsToDelete; // Used for debugging
            i=rowSetToRemove.nextSetBit(j+1);
        }
        //General.showDebug("Rows deleted: " + countDeleted); // Used for debugging
        /** Old slow code:
        for (int r=0;r<rowList.length;r++) {
            int row = rowList[r];
            if ( ! used.get( row )) {
                General.showError("in removeRow row already deleted for index: [" + row + "]. Not changing that; continuing with others.");
                status0 = false;
            } else {
                used.clear(row);
                sizeRows--;
            }
        }
         */

        if ( resetDefaultValues ) {
            if ( ! setValuesToDefaultByRowRange(rowSetToRemove) ) {
                General.showError("failed to setValuesToDefaultByRowRange.");
                status = false;
            }
        }

        if ( containsColumn( Relation.DEFAULT_ATTRIBUTE_ORDER_ID )) {
            boolean status_new = reorderRows();
            if ( !status_new ) {
                General.showError("failed to reorderRows after removal of rows:"+ PrimitiveArray.toString( rowSetToRemove ));
                status = false;
            }
        }
        return status;
    }


    /** Remove all columns in relation except those named in the argument list.
     *The ordering column should be listed in the argument in order to be maintained.
     *No check done to see if argument contains valid column names.
     */
    public boolean removeColumnsExcept( ArrayList columnsToKeep ) {
        boolean status = true;
        // Do remove from the back cause items might have shifted during the flight
        for ( int i=columnOrder.size()-1; i>=0; i--) {
            String columnName = (String) columnOrder.get(i);
            //General.showDebug("Considering remove of column: " + columnName);
            boolean toKeep =  columnsToKeep.contains( columnName );
            if ( ! toKeep ) {
                //General.showDebug("Removing column: " + columnName);
                if ( removeColumn( columnName ) == null ) {
                    General.showError("Failed to remove column with name: " + columnName);
                    status = false;
                }
            }
            //General.showDebug("Done column remove " + i + "          " + (!containsColumn(columnName)) + " for: " + columnName);
        }
        return status;
    }

    /**
     * @param label
     * @return <CODE>column</CODE> for success or null for failure.
     */
    public Object removeColumn(String label) {
        int columnIdx = getColumnIdx( label );
        if ( columnIdx == -1 ) {
            General.showError("requested to remove column for label: [" + label +
                "] but label doesn't exist in column label list: " + Strings.toString(columnOrder));
            return null;
        }
        columnOrder.remove( columnIdx );
        columnDataType.remove( label );

        if ( containsIndex(label) ) {
            removeIndex(label);
        }

        if ( dbms == null ) {
            General.showError("DEBUG dbms is null");
            return null;
        } else if ( dbms.foreignKeyConstrSet == null ) {
            General.showError("couldn'dbms.foreignKeyConstrSet.. is null.");
            return null;
        } else if ( dbms.foreignKeyConstrSet.containsForeignKeyConstrFrom( name, label )) {
            boolean status = dbms.foreignKeyConstrSet.removeForeignKeyConstrFrom( name, label );
            if ( ! status ) {
                General.showError("failed to remove foreign key constraint from dbms.foreignKeyConstrSet.. Continuing..");
                return null;
            }
        }

        Object o = attr.remove( label );
        if ( o == null ) {
            General.showError("requested to remove column for label: [" + label +
                "] but label doesn't exist in column labels (b).");
            return null;
        }

        return o;
    }

    /** Fast convenience method */
    public boolean hasColumn( String label) {
        return attr.containsKey( label );
        /**
        if ( ! status ) {
            General.showDebug("Relation: " + name + " aparently doesn't contain column with name: " + label);
            General.showDebug("     Columns present: " + attr.keySet().toString());
        }
        return status;
         */
    }
    /** Returns the column object if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return Column object (e.g. int[]) or <CODE>null</CODE> for failure.
     */
    public Object getColumn( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + " can't be returned with getColumn.");
            } else {
                General.showError("In getColumn, column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        return temp;
    }

    /** Returns the column int[] if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return <CODE>null</CODE> for failure.
     */
    public int[] getColumnInt( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + " can't be returned with getColumnInt.");
            } else {
                General.showError("In getColumnInt(Int), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof int[]) ) {
            General.showError("In getColumnInt(String), column not of type int[] for label: [" + label + "]");
            General.showError("Type is: " + dataTypeList[ getColumnDataType(label) ]);
            return null;
        }
        return (int[]) temp;
    }

    /** Returns the column int[] if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return <CODE>null</CODE> for failure.
     */
    public float[] getColumnFloat( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + "can't be returned with getColumnFloat.");
            } else {
                General.showError("In getColumnFloat(), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof float[]) ) {
            General.showError("In getColumnFloat(String), column not of type float[] for label: [" + label + "]");
            General.showError("Type is: " + dataTypeList[ getColumnDataType(label) ]);
            return null;
        }
        return (float[]) temp;
    }


    /** Returns the column float[][] if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return <CODE>null</CODE> for failure.
     */
    public float[][] getColumnFloatList( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + "can't be returned with getColumnFloat.");
            } else {
                General.showError("In getColumnFloat(), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof float[][]) ) {
            General.showError("In getColumnFloatList(String), column not of type float[][] for label: [" + label + "]");
            General.showError("Type  is: " + dataTypeList[ getColumnDataType(label) ]);
            General.showError("Class is: " + temp.getClass());
            return null;
        }
        return (float[][]) temp;
    }

    /** Returns the column int[][] if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return <CODE>null</CODE> for failure.
     */
    public int[][] getColumnIntList( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + "can't be returned with getColumnFloat.");
            } else {
                General.showError("In getColumnInt(), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof int[][]) ) {
            General.showError("In getColumnFloatList(String), column not of type int[][] for label: [" + label + "]");
            General.showError("Type is: " + dataTypeList[ getColumnDataType(label) ]);
            return null;
        }
        return (int[][]) temp;
    }

    /** Returns the column BitSet if there is one with the specified name.
     * @param label Name of the column (is unique).
     * @return <CODE>null</CODE> for failure.
     */
    public BitSet getColumnBit( String label) {
        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + "can't be returned with getColumnBit.");
            } else {
                General.showError("In getColumn(Bit), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof BitSet) ) {
            General.showError("In getColumnBit(String), column not of type BitSet for label: [" + label + "]");
            General.showError("Type is: " + dataTypeList[ getColumnDataType(label) ]);
            return null;
        }
        return (BitSet) temp;
    }


    /** Returns the StringSet column if there is one with the specified name plus standard extension.
     * @param label Name of the column (is unique) without postfix.
     */
    public String[] getColumnString( String label) {

        Object temp = attr.get( label );
        if ( temp == null ) {
            // uncommon case
            if ( label.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + " can't be returned with getColumn.");
            } else {
                General.showError("In getColumn(String), column not found for label: [" + label + "] for relation with name: " + name);
            }
            return null;
        }
        if ( !(temp instanceof String[])) {
            General.showError("In getColumnString(String), column not of type String[] for label: [" + label + "] for relation with name: " + name);
            return null;
        }
        return (String[]) temp;
    }


    /** Returns the StringSet column if there is one with the specified name plus standard extension.
     * @param label Name of the column (is unique) without postfix.
     */
    public StringSet getColumnStringSet( String label) {
        String labelNew = label + STANDARD_STRING_SET_POSTFIX;
        Object temp = attr.get( labelNew );
        if ( temp == null ) {
            // uncommon case
            if ( labelNew.equals( DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN )) {
                General.showError("column " + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN + " can't be returned with getColumn.");
            } else {
                General.showError("In getColumn(StringSet), column not found for label: [" + labelNew + "] for relation with name: " + name);
            }
            return null;
        }
        if ( temp instanceof StringSet ) {
            return (StringSet) temp;
        }
        General.showError("In getColumnStringSet(String), column not found for label: [" + labelNew + "] for relation with name: " + name);
        return null;
    }


    /** Returns the column object if there is one with the specified regular expression
     or -1 if there isn't.
     */
    public int getColumnIdForRegExp( String labelRegExp ) {
        Pattern p_general_format = Pattern.compile(labelRegExp, Pattern.COMMENTS);
        Matcher m_general_format = p_general_format.matcher(""); // default matcher on empty string.
        for (int c=sizeColumns()-1; c>=0 ;c--)  {
            m_general_format.reset( getColumnLabel(c));
            boolean status = m_general_format.matches();
            if ( status ) {
//                General.showDebug("Matched column label: " + getColumnLabel(c) + " with regexp: " + labelRegExp);
                return c;
            }
        }
        return -1;
    }

    /** Returns the column object if there is one with the specified id.
     * @return Column object (e.g. int[]) or <CODE>null</CODE> for failure.
     * @param column Column int id.
     */
    public Object getColumn( int column) {
        if ( column < 0 || column >= sizeColumns() ) {
            General.showError("column index not in range [0,"+sizeColumns()+">");
            return null;
        }

        String label = getColumnLabel( column );
        if ( label == null ) {
            General.showError("failed to get column for column index: [" + column + "]");
            return null;
        }
        return getColumn(label);
    }


    /** Assigns a name to a column by int id reference.
     * Range checking is done.
     * @param column Column int id.
     * @param label New column name.
     * @return <CODE>true</CODE> for success.
     */
    public boolean renameColumn( int column, String label) {
        if ( column < 0 || column >= sizeColumns() ) {
            General.showError("column index not in range [0,"+sizeColumns()+">");
            return false;
        }
        String oldLabel = getColumnLabel( column );
        return renameColumn(oldLabel, label);
    }

    /** Assigns a new name to the column as specified by the old name. It will check that
     * the new name is unique and that the old name existed.
     * @param labelOld Column name of the column to be renamed.
     * @param label New column name.
     * @return <CODE>true</CODE> for success.
     */
    public boolean renameColumn( String labelOld, String label) {

        //General.showDebug("TODO: recode the renaming of column names in relations for feature of fkc on real columns");

        // Order
        int column = columnOrder.indexOf(labelOld);
        if ( column < 0 ) {
            General.showError("no column found for old label:"+labelOld);
            return false;
        }
        if ( column >= sizeColumns() ) {
            General.showError("code bug/shouldn't happen.");
            return false;
        }
        columnOrder.set(column, label);

        // Main attr
        Object o = attr.remove(labelOld);
        if ( o == null ) {
            General.showError("failed to remove object for old label: [" + labelOld + "]");
            return false;
        }
        attr.put(label, o);

        // Data type
        o = columnDataType.remove(labelOld);
        if ( o == null ) {
            General.showError("failed to remove datatype for old label: [" + labelOld + "]");
            return false;
        }
        columnDataType.put(label, o);

        // DBMS
        boolean status = dbms.foreignKeyConstrSet.renameColumn( name, labelOld, label );
        if ( ! status ) {
            General.showError("failed to rename column in fkc set for old label: [" + labelOld + "]");
            return false;
        }

        return true;
    }


    /** For backward compatibility and smaller selections */
    public int[] getNewRowIdList(int freeRowCount) {
        if ( freeRowCount < 1 ) {
            General.showError("Asked for less than 1 new row id: " + freeRowCount);
            return null;
        }
        BitSet result = getNewRows(freeRowCount);
        if ( result == null ) {
            General.showError("Failed to get new row ids: " + freeRowCount);
            return null;
        }
        return PrimitiveArray.toIntArray(result);
    }

    /** Look for new row ids to place items. Returns a list of indices to them or null to
     * indicate some failure. This routine will consequently mark the rows then as used
     *but not selected. It will not use the reservation system.
     * @param freeRowCount
     **/
    public BitSet getNewRows(int freeRowCount) {
        //General.showDebug("getting new list of rows of size:" + freeRowCount);
        if ( freeRowCount < 1 ) {
            General.showWarning("requested less than 1 new rows in getNewRowIdList: " + freeRowCount);
            return null;
        }

        /** Bit expensive for small selections but small amount of real objects. */
        BitSet result = new BitSet(sizeRows);

        /** Destroy all indices if any available */
        removeIndices();

        // Make sure we have enough rows free from beginning. Very important so no checks
        // on that are needed below in routine.
        int leftFree = sizeMax - sizeRows;
        // When resizing resize to hold at least a percentage (like 30%) larger
        // than requested.
        if ( leftFree < freeRowCount ) {
            int newSize = (int) ((sizeRows + freeRowCount) * ( 100.0f + DEFAULT_SIZE_CHANGE_PERCENTAGE ) / 100.0f);
            //General.showDebug("resizing from " + sizeMax + " to " + newSize);
            //General.showDebug("really used before " + sizeRows);
            ensureCapacity( newSize );
        }

        // The following algorithm is very fast for any but the case of single gaps.
        int rowsCountCollected = 0;
        int endIdx =0;

        // The loop should stop before reaching beyond the end.
        while ( rowsCountCollected < freeRowCount ) {
            int startIdx = used.nextClearBit(endIdx);
            if ( startIdx == -1 ) {
                General.showError("Found no spot although there is room.");
                return null;
            }
            endIdx = used.nextSetBit(startIdx); // exclusive.
            if ( endIdx == -1 ) { // If there's no end to the stretch of empty rows; that's great.
                endIdx = sizeMax; // exclusive
            }
            int maxRangeSize = endIdx - startIdx;
            int sizeStillNeeded = freeRowCount - rowsCountCollected;
            int rangeSize = Math.min( maxRangeSize, sizeStillNeeded );
            endIdx = startIdx + rangeSize;
            int rowsToAdd = endIdx - startIdx; // same as sizeStillNeeded right?

            if ( rangeSize < 1 ) {
                General.showError("expect range size to be at least 1 but found: " + rangeSize);
                return null;
            }
            // Fill the result list and keep track of how many were collected (for the same price.).
            // This makes BitSet a fast structure!!!!
            used.set(startIdx,endIdx);
            result.set(startIdx,endIdx);
            rowsCountCollected  += rowsToAdd;
            sizeRows            += rowsToAdd;
        }
        //General.showDebug("free element list:" + PrimitiveArray.toString(result));
        return result;
    }

    /** Convenience method actually using getNewRowIdList.
     * @return  one index for a new row or -1 to indicate failure.*/
    public int getNewRowId() {
        int[] idxList = getNewRowIdList( 1 );
        if ( idxList == null ) {
            return -1;
        }
        return idxList[0];
    }

    /** Default is to show headers and data types and even rows that are not selected if such a property
     *exists or not.*/
    public String toString() {
        return toString( true, true, true, true, true, false );
    }

    /**
     *
     * @return a LoL
     */
    public String[][] toStringOfString() {
        String[][] result = new String[sizeRows][sizeColumns()-1];
        for (int i=0;i<sizeRows;i++) {
            for (int j=1;j<sizeColumns();j++) {
                result[i][j-1] = getValueString(i, j);
            }
        }
        return result;
    }

    /** Convenience method for calling without headers */
    public String toStringRow(int row) {
        return toStringRow(row, false);
    }

    /**
     *Still need to program correct quote styles for csv format if that becomes important.
     * @return string representation of the row as a comma separated list in csv format.
     *Returns ROW_NOT_USED_STRING if the row is not used. Returns ROW_WITHOUT_COLUMNS_STRING
     *if there are no columns in the table.
     */
    public String toStringRow(int row, boolean showColumnLabels) {
        if ( ! used.get(row) ) {
            return ROW_NOT_USED_STRING;
        }
        if ( sizeColumns() == 0 ) {
            return ROW_WITHOUT_COLUMNS_STRING;
        }

        StringBuffer sb = new StringBuffer(); // Reserve some estimated space.

        if ( showColumnLabels ) {
            sb.append( "[Header] " );
            for (int c=0;c<sizeColumns();c++) {
                sb.append( getColumnLabel(c) );
                sb.append(",");
            }
            sb.deleteCharAt(sb.length()-1); // Deleting it afterwards saves some
                                            // test times.
            sb.append(General.eol);
        }

        sb.append( "["+row+"] " );
        for (int c=0;c<sizeColumns();c++) {
            sb.append( getValueString(row,c) );
            sb.append(',');
        }
        sb.deleteCharAt(sb.length()-1); // Deleting it afterwards saves some test times.

        return sb.toString();
    }

    /** This will create a string representation. The code in here should not
     * need to be optimized because it is never intended to use this for millions of
     * rows in an efficient way. Returns null in case of error.
     * @return string representation of the header.
     * @param show_data_types
     * @param show_header  */
    public String toString(boolean show_header,
        boolean show_data_types,
        boolean show_fkcs,
        boolean show_indices,
        boolean show_rows,
        boolean show_selected_only) {

        int sizeColumns = sizeColumns();
        if ( sizeColumns < 1 ) {
            return    "---  Relation        : " + name + " has NO columns ---\n";
        }

        BitSet selected = null;
        if ( show_selected_only ) {
            selected = (BitSet) attr.get(DEFAULT_ATTRIBUTE_SELECTED);
            if ( selected == null ) {
                General.showError("requested to show only those rows that are selected but there's no 'selected' column");
                return null;
            }
        }

        StringBuffer sb = new StringBuffer();

        // Header
        if (show_header) {
            boolean containsForeignKeyConstraints = false;
            boolean containsindices               = false;
            sb.append("---  Relation        : " + name + " ---\n");
            sb.append("---  Column Labels   : ");
            for (int i=0;i<sizeColumns;i++) {
                String label = getColumnLabel(i);
                sb.append( label );
                ForeignKeyConstr fkc = dbms.foreignKeyConstrSet.getForeignKeyConstrFrom(name,label);
                if ( fkc != null ) {
                    containsForeignKeyConstraints = true;
                }
                if ( indices.containsKey( label ) ) {
                    containsindices = true;
                }
                if ( i < ( sizeColumns - 1 ) )
                    sb.append(",");
                else
                    sb.append(" ---\n");
            }
            // Datatypes
            if (show_data_types) {
                sb.append("---  Data Types      : ");
                for (int i=0;i<sizeColumns;i++) {
                    String label = getColumnLabel(i);
                    int dataType = getColumnDataType(label);
                    sb.append(dataTypeList[dataType]);
                    //sb.append("("+dataType+")");
                    if ( i < ( sizeColumns - 1 ) )
                        sb.append(",");
                    else
                        sb.append(" ---\n");
                }
            }
            // Foreign Key Constraints
            if ( containsForeignKeyConstraints && show_fkcs ) {
                sb.append("---  Foreign Key Constraints   :\n");
                for (int i=0;i<sizeColumns;i++) {
                    String label = getColumnLabel(i);
                    ForeignKeyConstr fkc = dbms.foreignKeyConstrSet.getForeignKeyConstrFrom(name,label);
                    if ( fkc != null ) {
                        sb.append( "\t" );
                        sb.append( fkc.toString() );
                        sb.append( General.eol );
                    }
                }
            }
            // indices
            if ( containsindices && show_indices ) {
                sb.append("---  indices                   :\n");
                for (int i=0;i<sizeColumns;i++) {
                    String label = getColumnLabel(i);
                    if ( indices.containsKey( label )) {
                        // Check all elements in array to see if there is such an index
                        Index[] al = (Index[]) indices.get( label );
                        for (int j=0;j<Index.INDEX_TYPE_COUNT;j++) {
                            Index index = al[j];
                            if ( index == null ) { // will skip zero-th element
                                continue;
                            }
                            sb.append( "\tColumn " );
                            sb.append( label );
                            sb.append( " has index. " );
                            sb.append( index.toString() );
                            sb.append( General.eol );
                        }
                    }
                }
            }
        }

        if ( show_rows ) {
            if ( sizeRows < 1 ) {
                sb.append("---  Empty Relation (" + sizeColumns + " columns but no rows) ---\n");
                return sb.toString();
            }

            // Rows
            int rowWrittenCount = 0;
            for (int r=0;r<sizeMax;r++) {
                if ( ! used.get( r )) {
                    continue;
                }
                // Show selected only if requested.
                if ( show_selected_only && !selected.get( r ) ) {
                    continue;
                }
                sb.append( toStringRow( r ) ); // Get the row representation.
                sb.append(General.eol);
                rowWrittenCount++;
            }
            if ( rowWrittenCount != sizeRows && (!show_selected_only) ) {
                sb.append("ERROR: rowWrittenCount("+rowWrittenCount+") != sizeRows("+ sizeRows +")");
                //return null;
            }
        }
        return sb.toString();
    }

    /** Convenience method
     */
    public String toSTAR() {
        return toSTAR(used);
    }

    /** For the requested rows a loop or for a single row the free values will be generated.
     *For empty relations it will return an empty string.
     *Errors will lead to a return value of null.
     */
    public String toSTAR(BitSet todo) {
        String result = null;
        try {
            BitSet toRemove =(BitSet) todo.clone();
            toRemove.flip(0,sizeMax);
            Relation copyMainRelation = (Relation) Objects.deepCopy(this);
            copyMainRelation.removeRows(toRemove, false, false );
            if ( copyMainRelation.used.cardinality() == 0 ) {
                return Defs.EMPTY_STRING;
            }
            //result = copyMainRelation.toString(true, true, true, true, true, false);

            TagTable tT = new TagTable("tempjeToSTAR", dbms);
            tT.init(copyMainRelation);
            tT.isFree = false;
            result = tT.toSTAR();
            if ( result == null || result.length() == 0 ) {
                General.showError("failed to render tag table to STAR");
                return null;
            }
            dbms.removeRelation(tT);
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        }
        return result;
    }


    /** Returns null on error. Returns the average and sd of a set of values when
     *there are more than 1 valid values. If there are no values then no average
     *can be calculated so the average returned will be Defs.NULL. Same if there
     *is only one value; no sd but Defs.NULL.
     */
    public float[] getAvSd( String label ) {
        float[] list = getFlatListFloat( label );
        if ( list == null ) {
            General.showError("Failed to getFlatList for label: " + label);
            return null;
        }
        return Statistics.getAvSd(list);
    }

    /** Returns an array without gaps and only valid elements.
     */
    public float[] getFlatListFloat( String label ) {
        float[] col = getColumnFloat(label);
        if ( col == null ) {
            General.showError("Failed to get float column in getFlatList for label: " + label);
            return null;
        }
        FloatArrayList flatList = new FloatArrayList(sizeRows);
        flatList.setSize(sizeRows); // max size so no resize will be needed.
        int j=0;
        float v=Defs.NULL_FLOAT;
        for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
            v=col[i];
            if ( Defs.isNull(v)) {
                continue;
            }
            flatList.setQuick(j,v);
            j++;
        }
        flatList.setSize(j); // correct size now.
        return PrimitiveArray.toFloatArray(flatList);
    }

    /** Returns an array without gaps and only valid elements.
     */
    public int[] getFlatListInt( String label ) {
        int[] col = getColumnInt(label);
        if ( col == null ) {
            General.showError("Failed to get int column in getFlatListInt for label: " + label);
            return null;
        }
        IntArrayList flatList = new IntArrayList(sizeRows);
        flatList.setSize(sizeRows); // max size so no resize will be needed.
        int j=0;
        int v=Defs.NULL_INT;
        for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
            v=col[i];
            if ( Defs.isNull(v)) {
                continue;
            }
            flatList.setQuick(j,v);
            j++;
        }
        flatList.setSize(j); // correct size now.
        return PrimitiveArray.toIntArray(flatList);
    }

    /** Convenience method; Don't use this where speed is important. */
    public String getValueString( int row, String label) {
        return getValueString( row, getColumnIdx( label));
    }

    /** Fast method, no checks.*/
    public double getValueDouble( int row, String label) {
        return ((double[]) attr.get(label))[row];
    }

    /** Fast method, no checks.*/
    public double getValueDoubleSafe( int row, String label) {
        int dataType = getColumnDataType(label);

        // Sanity checks
        if ( dataType == DATA_TYPE_INVALID ) {
            General.showWarning("Failed to get data type for column at position: " + label);
            return Defs.NULL_DOUBLE;
        }
        if ( ! used.get( row ) ) {
            General.showWarning("Tried to get a string value from relation ("+name+"), row ("+row+") that is not in use at column id: " + label);
            return Defs.NULL_DOUBLE;
        }

        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = (BitSet) attr.get(label);
                boolean value = temp.get( row );
                if ( value ) {
                    return 0d;
                } else {
                    return 1d;
                }
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) attr.get(label);
                char value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_DOUBLE;
                } else {
                    return (double) value;
                }
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = (byte[]) attr.get(label);
                byte value = temp[row];
                return (double) value;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) attr.get(label);
                short value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_DOUBLE;
                }
                return (double) value;
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) attr.get(label);
                int value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_DOUBLE;
                }
                return (double) value;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) attr.get(label);
                float value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_DOUBLE;
                }
                return (double) value;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) attr.get(label);
                double value = temp[row];
                return value;
            }
        }
        General.showError("Failed getValueDoubleSafe");
        return Defs.NULL_DOUBLE;
    }

    /** Fast method, no checks.*/
    public float getValueFloat( int row, String label) {
        return ((float[]) attr.get(label))[row];
    }
    /** Fast method, no checks.*/
    public int getValueInt( int row, String label) {
        return ((int[]) attr.get(label))[row];
    }
    /** Fast method, no checks.*/
    public boolean getValueBit( int row, String label) {
        return ((BitSet) attr.get(label)).get(row);
    }
    /** Fast method, no checks.*/
    public char getValueChar( int row, String label) {
        return ((char[]) attr.get(label))[row];
    }
    /** Fast method, no checks.*/
    public byte getValueByte( int row, String label) {
        return ((byte[]) attr.get(label))[row];
    }

    /** Fast method, no checks.*/
    public Object getValue( int row, String label) {
        int dataType = getColumnDataType(label);
        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                return Boolean.valueOf( getValueBit( row, label));
            }
            case DATA_TYPE_CHAR: {
                return new Character( getValueChar( row, label));
            }
            case DATA_TYPE_BYTE: {
                return new Byte( getValueByte( row, label));
            }
            case DATA_TYPE_SHORT: {
                return new Byte( getValueByte( row, label));
            }
            case DATA_TYPE_INT: {
                return new Integer( getValueInt( row, label));
            }
            case DATA_TYPE_FLOAT: {
                return new Float( getValueFloat( row, label));
            }
            case DATA_TYPE_DOUBLE: {
                return new Double( getValueDouble( row, label));
            }
            case DATA_TYPE_LINKEDLIST: {
                return null;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                return null;
            }
            case DATA_TYPE_STRING: {
                return getValueString( row, label);
            }
            case DATA_TYPE_STRINGNR: {
                return getValueString( row, label);
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                return null;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                return null;
            }
            case DATA_TYPE_OBJECT: {
                return null;
            }
            default: {
                General.showError("code bug in getValue for row: [" + row + "] and colum: [" + label  + "]");
                Object col = attr.get(label);
                General.showDebug("Object type for column: " + label + " is " + col.getClass().getName());
                General.showError("Unknown type: " + dataType + ". Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return null;
            }
        }
    }


   /** Convenience Class; Don't use this where speed is important.
     *It will translate null values into dots for data types that support null values.
     * @param row
     * @param column
     *  */
    public String getValueString( int row, int column) {
        String label = getColumnLabel(column);
        int dataType = getColumnDataType(label);

        // Sanity checks
        if ( dataType == DATA_TYPE_INVALID ) {
            General.showWarning("Failed to get data type for column at position: " + column);
            return Defs.NULL_STRING_DOT;
        }
        if ( ! used.get( row ) ) {
            General.showWarning("Tried to get a string value from relation ("+name+"), row ("+row+") that is not in use at column id: " + column);
            return Defs.NULL_STRING_DOT;
        }

        // Allow no exceptions to be cast up.
        try {
        //if ( false ) {
            //General.showDebug("getting string value for row: " + row + " and column: " + column);
            //General.showDebug("dataType is: " + dataType + " and label is: " + label);
        //}
        //Object col = attr.get(label);
        //General.showDebug("Object type for column: " + label + " is " + col.getClass().getName());
        switch ( dataType ) {
            case DATA_TYPE_BIT: {
                BitSet temp = (BitSet) attr.get(label);
                boolean value = temp.get( row );
                if ( value ) {
                    return TRUE;
                } else {
                    return FALSE;
                }
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) attr.get(label);
                char value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_STRING_DOT;
                } else {
                    return Character.toString( value );
                }
            }
            case DATA_TYPE_BYTE: {
                byte[] temp = (byte[]) attr.get(label);
                byte value = temp[row];
                return Byte.toString( value );
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) attr.get(label);
                short value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_STRING_DOT;
                }
                return Short.toString( value );
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) attr.get(label);
                int value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_STRING_DOT;
                }
                return Integer.toString( value );
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) attr.get(label);
                float value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_STRING_DOT;
                }
                return Float.toString( value);
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) attr.get(label);
                double value = temp[row];
                if ( Defs.isNull( value ) ) {
                    return Defs.NULL_STRING_DOT;
                }
                return Double.toString( temp[row]);
            }
            case DATA_TYPE_LINKEDLIST: {
                LinkedListArray temp = (LinkedListArray) attr.get(label);
                return temp.toString(row);
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                LinkedListArrayInfo temp = (LinkedListArrayInfo) attr.get(label);
                return temp.toString(row);
            }
            case DATA_TYPE_STRING: {
                String[] temp = (String[]) attr.get(label);
                String value = temp[row];
                if ( Defs.isNullString( value )) {
                    return Defs.NULL_STRING_DOT;
                }
                return value;
            }
            case DATA_TYPE_STRINGNR: {
                String[] temp = (String[]) attr.get(label);
                String value = temp[row];
                if ( Defs.isNullString( value )) {
                    return Defs.NULL_STRING_DOT;
                }
                return value;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                int[][] temp = (int[][]) attr.get(label);
                int[] value = temp[row];
                if ( value == null ) {
                    return Defs.NULL_STRING_DOT;
                }
                return PrimitiveArray.toString( temp[row] );
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                float[][] temp = (float[][]) attr.get(label);
                float[] value = temp[row];
                if ( value == null ) {
                    return Defs.NULL_STRING_DOT;
                }
                return PrimitiveArray.toString( temp[row] );
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = (Object[]) attr.get(label);
                Object value = temp[row];
                if ( value == null ) {
                    return Defs.NULL_STRING_DOT;
                }
                //return value.toString();
                return "OBJECT";
            }
            default: {
                General.showError("code bug in getValueString for row: [" + row + "] and colum: [" + column  + "]");
                Object col = attr.get(label);
                General.showDebug("Object type for column: " + label + " is " + col.getClass().getName());
                General.showError("Unknown type: " + dataType + ". Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return null;
            }
        }
        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Caught error in convenience method: getValueString( int row, int column)");
            General.showDebug("getting string value for row: " + row + " and column: " + column);
            General.showDebug("dataType is: " + dataType + " and label is: " + label);
            Object col = attr.get(label);
            General.showDebug("Object type for column: " + label + " is " + col.getClass().getName());
            return null;
        }
    }

    /** After this routine the indices of the linked list if any contained should be
     * corrected from externally by reindexing the linked list on the basis of the
     * returned map.
     * <PRE>
     *        0 1 2 3 4 5 6 7 8 9
     * rows = {a, ,b, , ,A, ,B, , } then using reduceCapacity(5) results in:
     * rows = {a,B,b,A, , , , , , }
     * result = {0,7,2,5,-1}
     * </PRE>
     *  Check code as there is a BUG in doing this 11-30-2005. TODO
     * @param newSize
     *
    public int[] reduceCapacity(int newSize) {
        if ( newSize < sizeRows ) {
            int maxSizeNew = (int) ( sizeRows * ( 1.0f + DEFAULT_SIZE_CHANGE_PERCENTAGE/100.0f ));
            General.showWarning("newSize (" +newSize+") < sizeRows("+sizeRows+") is not allowed; will try to trim to smallest efficient("+maxSizeNew+").");
            return reduceCapacity( maxSizeNew);
        }
        if ( newSize == sizeRows && sizeRows == sizeMax ) {
            int maxSizeNew = (int) ( sizeRows * ( 1.0f + DEFAULT_SIZE_CHANGE_PERCENTAGE/100.0f ));
            General.showWarning("newSize (" +newSize+") = sizeRows("+sizeRows+") is not allowed when the rows are all full; will try to trim to smallest efficient("+maxSizeNew+").");
            return reduceCapacity( maxSizeNew);
        }

        /** Destroy all indices if any available *
        removeIndicesAll();

        //General.showDebug("Reducing capacity from: " + sizeMax + " to : " + newSize);
        int oldSizeMax = sizeMax;
        int[] result = new int[oldSizeMax];
        // Move elements from the end into the gaps untill all gaps are filled.
        // The method is not intended to preserve the order. In fact, it will invert the order
        // and spread element over the relation.
        // Will mark them as in use so make sure the old ones get deleted.
        int[] itemsOldIdxList = getFilledFromBackRowIdList( newSize );
        if ( itemsOldIdxList == null) {
            General.showError("Failed to get the row ids from the back.");
            return null;
        }
        /** If there's none to move then just truncate the array
         *
        int[] itemsNewIdxList = new int[0];
        if ( itemsOldIdxList.length != 0 ) {
            itemsNewIdxList = getNewRowIdList( itemsOldIdxList.length );
            if ( itemsNewIdxList == null ) {
                General.showError("Failed to get the row ids for free rows.");
                return null;
            }
            boolean status = moveRows( itemsOldIdxList, itemsNewIdxList);
            if ( ! status ) {
                General.showError("Failed to move the rows.");
                return null;
            }
            /** Format the result array; which might be wastefull for small capacity changes
             *but is very fast for usage afterwards.
             *The unused/unchanged:*
            Arrays.fill(result, LinkedListArray.NOT_AN_INDEX );
            for (int i=itemsOldIdxList.length-1;i>-1;i--){
                result[ itemsOldIdxList[i] ] = itemsNewIdxList[i];
            }
        }
        boolean status = resizeCapacity( newSize ); // very fast.
        if ( ! status ) {
            General.showError("Failed to resize capacity.");
            return null;
        }
        return result;
    }
     */

    /** Correct the indices in the maps.
     *DON'T HAVE USED IT YET; not tested either.
     *
    public boolean remapLinkedLists(int[] map) {
        for (int i=0;i<columnDataType.size();i++) {
            String label = getColumnLabel(i);
            int dataType = getColumnDataType(label);
            if ( dataType == DATA_TYPE_LINKEDLIST ) {
                LinkedListArray ll = (LinkedListArray) getColumn( i );
                ll.remap( map );
            }
        }
        return true;
    }
     */


    /** Return a list of indices containing the used elements at the end of array.
     *The routine will return element at index newSize at most. It can not
     *return an empty list. The order is with the lowest indices in the lowest
     *slots.
     *corrected from externally by reindexing the linked list on the basis of the
     *returned map
     */
    public int[] getFilledFromBackRowIdList(int newSize) {
        if ( newSize <= 0 ) {
            General.showError("given new size should be at least one but is given as: " + newSize);
            return null;
        }
        int[] result = new int[sizeRows]; // Largest size possible is sizeRows. Will be truncated later.
        // Routine can be optimized when requesting more than 1 element from "used"
        // or when there would be a reverse function of getNextSetBit...
        int idx = sizeMax - 1;
        int i=0;
        while ( idx >= newSize ) {
            if ( used.get( idx ) ) {
                result[i] = idx;
                i++;
            }
            idx--;
        }
//        int resultSize = i;
        //General.showDebug("result: with max:("+i+") " + PrimitiveArray.toString(result));
        // Trim the result to the number of elements needed for trim.
        result = PrimitiveArray.resizeArray(result, i);
        //General.showDebug("result: " + PrimitiveArray.toString(result));
        result = PrimitiveArray.invertArray(result);
        //General.showDebug("result: " + PrimitiveArray.toString(result));
        return result;
    }

    /** This routine can cleverly remove rows in the child relation and updating the linkedlist array in the child
     * and the linkedlistarrayinfo at this level.
     * @param row Row for parent in this relation.
     * @param childToDeleteList Array of indices to the children to be deleted.
     * @param child Relation of the child.
     * @return <CODE>true</CODE> for success.
     */
    public boolean deleteChildren(int row, int[] childToDeleteList, Relation child ) {
        LinkedListArrayInfo childInfo    = (LinkedListArrayInfo)      attr.get( DEFAULT_ATTRIBUTE_CHILD_LIST );
        LinkedListArray     siblingList  = (LinkedListArray)    child.attr.get( DEFAULT_ATTRIBUTE_SIBLING_LIST );

        int[] fullList = new int[childInfo.countList[ row ]];
        if ( ! siblingList.getList( childInfo.firstList[ row ], fullList ) ) {
            General.showError("failed to get list of children in deleteChildren; not deleting children");
            return false;
        }

        // Delete the atoms but do update the linked list of siblings because it can't be done outside.
        if ( ! child.removeRows(childToDeleteList, true) ) {
            General.showError("failed to remove children in deleteChildren; not deleting children");
            return false;
        }

        int[] newList  = PrimitiveArray.minus( fullList, childToDeleteList);
        if ( ! childInfo.set(row, newList) ) {
            General.showError("failed to set new list of children in deleteChildren; inconsistent program state");
            return false;
        }
        return true;
    }

    /** This routine can cleverly remove rows in the child relation and updating the linkedlist array in the child
     * and the linkedlistarrayinfo at this level.
     * @param row Row for parent in this relation.
     * @param child Relation of the child.
     * @return <CODE>true</CODE> for success.
     */
    public boolean deleteChildren(int row, Relation child ) {
        LinkedListArrayInfo childInfo    = (LinkedListArrayInfo)      attr.get( DEFAULT_ATTRIBUTE_CHILD_LIST );
        LinkedListArray     siblingList  = (LinkedListArray)    child.attr.get( DEFAULT_ATTRIBUTE_SIBLING_LIST );

        int[] fullList = new int[childInfo.countList[ row ]];
        if ( ! siblingList.getList( childInfo.firstList[ row ], fullList ) ) {
            General.showError("failed to get list of children in deleteChildren; not deleting children");
            return false;
        }

        // Delete the atoms but don't even try to update the linked list of siblings
        // because that's done from here in a scalable way.
        if ( ! child.removeRows(fullList, false) ) {
            General.showError("failed to remove children in deleteChildren; not deleting children");
            return false;
        }

        if ( ! childInfo.set(row, new int[0]) ) {
            General.showError("failed to set new list of children in deleteChildren; inconsistent program state");
            return false;
        }
        return true;
    }

    /** Checks if all mentioned elements are actually columns in the current relation.
     */
    public boolean reorderColumns( ArrayList columnOrderNew ) {
        boolean status = true;
        if ( columnOrderNew.size() != columnOrder.size() ) {
            General.showError("When trying to reorder the columns: given number of new columns failed to match current set.");
            General.showError("columnOrder   ["+columnOrder.size()   +"]: " + Strings.toString(columnOrder ));
            General.showError("columnOrderNew["+columnOrderNew.size()+"]: " + Strings.toString(columnOrderNew));
            ArrayList columnOrderClone = (ArrayList) columnOrder.clone();
            StringArrayList columnOrderSAL = new StringArrayList(columnOrderClone);
            ArrayList columnOrderNewClone = (ArrayList) columnOrderNew.clone();
            StringArrayList columnOrderNewSAL = new StringArrayList(columnOrderNewClone);
            StringArrayList diff = columnOrderSAL.difference(columnOrderNewSAL);
            General.showError("diff: " + diff);
            status = false;
        }
        for ( int i=0; i<columnOrderNew.size(); i++) {
            String columnName = (String) columnOrder.get(i);
            if ( !containsColumn( columnName ) ) {
                General.showError("When trying to reorder the columns: failed to find column named: " + columnName);
                status = false;
            }
        }
        if ( ! status ) {
            return false;
        }
        columnOrder.removeAll( columnOrderNew );
        columnOrder.addAll(0, columnOrderNew );
        return true;
    }

    /**
     * The argument set E doesn't need to contain all columns though in contrast to the requerement
     * in above method reorderColumns. It may even contain more elements.
     * Order the set W of Wattos tagnames such that:
     * Let U=W-E (tagnames in W but not in E)
     *     V=WnE (tagnames in both W and E)
     *     W=UuV by definition
     *  - order of tagnames in U unchanged.
     *  - tagnames in U to precede those in V.
     */
    public boolean reorderSomeColumns( StringArrayList salE ) {
//        General.showDebug("Sorting columns by: " + salE.toString());
        StringArrayList salW = new StringArrayList(columnOrder);
        StringArrayList salU = salW.difference(salE);   // order within this list is maintained
        StringArrayList salV = salE.intersection(salW); // order within this list is maintained
        columnOrder.clear();
//        General.showDebug("Adding tagnames in W but not in E :"+ salU.toString());
        columnOrder.addAll( salU );
//        General.showDebug("Adding tagnames in both W and E   :"+ salV.toString());
        columnOrder.addAll( salV );
        return true;
    }


    /**
     *see: #renumberRows(String,BitSet,int)
     */
    public boolean reorderRows() {
        //if ( true ) return false;
        return renumberRows( DEFAULT_ATTRIBUTE_ORDER_ID, used, 0);
    }


    /** Fast routine that fills the given column with the number of the physical
     *order starting with the given number (zero should be used for renumbering
     *the order column used for the child class TagTable).
     */
    public boolean numberRowsPhysical( String columnName, BitSet rowsToDo, int startNumber ) {
        int rowsToDoCount = rowsToDo.cardinality(); // expensive.
        if ( rowsToDoCount == 0 ) {
            General.showDebug("No rows in relation: " + name + " so no need for called numberRowsPhysical.");
            return true;
        }

        if ( ! containsColumn( columnName ) ) {
            General.showError("Failed numberRowsPhysical as the column to renumber: " +
                columnName + " doesn't exist in this relation: " + name );
            return false;
        }

        // Check to see if there are any rows todo that are not in use.
        // If speed is an issue this check can be removed.
        BitSet temp = (BitSet) rowsToDo.clone();
        temp.andNot(used);
        if ( temp.cardinality() > 0 ) {
            General.showCodeBug("There are rows to renumber that are not in use: " + temp.cardinality());
            return false;
        }

        int[] column = getColumnInt( columnName );
        if ( column == null ) {
            General.showCodeBug("Failed to get an int[] column in Relation.numberRowsPhysical for name: " + columnName);
            return false;
        }
        // Temp array for info to be sorted
        // Reset the order id on the basis of the order of the sorted Physical Row ID.
        int count = startNumber;
        for (int i=rowsToDo.nextSetBit(0); i>=0; i=rowsToDo.nextSetBit(i+1))  {
            column[ i ] = count;
            count++;
        }
        return true;
    }

    /** Sorts rows by one or more columns */
    public boolean sortByColumns( String[] sortColumnList ) {
        if ( sortColumnList.length != 1 ) {
            General.showCodeBug("Routine only capable of sorting on one column as of yet");
            return false;
        }
        General.showDebug("Sorting on column(s): " + Strings.toString(sortColumnList));
        Index[] indexList = new Index[sortColumnList.length];
        for ( int c=0;c<sortColumnList.length;c++) {
            if ( ! containsColumn(sortColumnList[c])) {
                General.showError("No such column: " + sortColumnList[c]);
                return false;
            }
            indexList[c] = getIndex(sortColumnList[c], Index.INDEX_TYPE_SORTED);
            if ( indexList[c] == null ) {
                General.showError("Failed to get index for column: " + sortColumnList[c]);
                return false;
            }
        }
        Index mainIndex = indexList[0];
        General.showDebug("Main index: " + mainIndex.toString(true, true));

        int[] orderColumn = getColumnInt(DEFAULT_ATTRIBUTE_ORDER_ID);

//        General.showDebug("row order column: " +PrimitiveArray.toString(orderColumn));
        if ( orderColumn == null ) {
            insertColumnPhysical(0, DEFAULT_ATTRIBUTE_ORDER_ID);
            General.showDebug("insertColumnPhysical done");
            orderColumn = getColumnInt(DEFAULT_ATTRIBUTE_ORDER_ID);
        }

        if ( mainIndex.rids.length != used.cardinality() ) {
            General.showError("main index length : " + mainIndex.rids.length);
            General.showError("used.cardinality(): " + used.cardinality());
            return false;
        }
        int r_idx = -1;
        for ( int r=0;r<mainIndex.rids.length;r++) {
            r_idx = used.nextSetBit(r_idx+1);
            setValue(mainIndex.rids[r], DEFAULT_ATTRIBUTE_ORDER_ID, r_idx);
        }


        int[] orderColumnReduced = new int[ mainIndex.rids.length ];
        System.arraycopy( orderColumn, 0, orderColumnReduced, 0, mainIndex.rids.length);
        General.showDebug("sorted row order column: " +PrimitiveArray.toString(orderColumnReduced));
        return true;
    }
    /** Renumber a column to new values. Kind of expensive method.
     *Can be useful after removing a bunch of rows. The numbering
     *will start as specified by the calling parameter startNumber.
     *If the rowsToDo contains unused rows then false will be returned.
     *
     *<PRE>
     *If there is missing ids in the column they will be filled:
     *  [1,4,5] -> [0,1,2]
     *If there are duplicates then an arbitrary (based on physical ordering) order
     *will be imposed.
     *  [9,9,9] -> [0,1,2]
     *If there are unused rows in the middle they are obviously skipped
     *  [9,,9] ->  [0,,1]
     *</PRE>
     *Returns false if there was no numbering column by the given name.
     *Returns true if no rows are present in this relation yet.
     *Returns true even if there was no ordering column; the one indicated by DEFAULT_ATTRIBUTE_ORDER_ID.     */
    public boolean renumberRows( String columnName, BitSet rowsToDo, int startNumber ) {

        int rowsToDoCount = rowsToDo.cardinality(); // expensive.
        if ( rowsToDoCount == 0 ) {
            //General.showDebug("No rows in relation: " + name + " so no need for called renumberRows.");
            return true;
        }

        if ( ! containsColumn( columnName ) ) {
            General.showError("Failed renumberRows as the column to renumber: " +
                columnName + " doesn't exist in this relation: " + name );
            return false;
        }

        // Check to see if there are any rows todo that are not in use.
        // If speed is an issue this check can be removed.
        BitSet temp = (BitSet) rowsToDo.clone();
        temp.andNot(used);
        if ( temp.cardinality() > 0 ) {
            General.showCodeBug("There are rows to renumber that are not in use: " + temp.cardinality());
            return false;
        }

        int[] column = getColumnInt( columnName );
        if ( column == null ) {
            General.showCodeBug("Failed to get an int[] column in Relation.renumberRows for name: " + columnName);
            return false;
        }
        // Temp array for info to be sorted
        ArrayList tempList = new ArrayList();
        tempList.ensureCapacity(rowsToDoCount);
        // Fill temp array; kind of expensive.
        for (int i=rowsToDo.nextSetBit(0); i>=0; i=rowsToDo.nextSetBit(i+1))  {
            ObjectIntPair pair = new ObjectIntPair(new Integer(column[i]),i);
            tempList.add( pair );
        }

        // sort collection based on the old id
        Collections.sort( tempList, new ComparatorIntIntPair());
        // Reset the order id on the basis of the order of the sorted Physical Row ID.
        int count = startNumber;
        for (int i=0; i<rowsToDoCount;i++)  {
            ObjectIntPair pair = (ObjectIntPair) tempList.get(i);
            column[ pair.i ] = count;
            count++;
        }

        return true;
    }

    /**
     * Renumber a column to new values if previously existing.
     *Returns false if there was an error.
     *Returns true on success.
     */
    public boolean renumberRowsByOffset( String columnName, int offset ) {

        if ( ! containsColumn( columnName ) ) {
            General.showError("Failed renumberRowsByOffset as the column to renumber: " +
                columnName + " doesn't exist in this relation: " + name );
            return false;
        }

        int[] column = getColumnInt( columnName );
        if ( column == null ) {
            General.showCodeBug("Failed to get an int[] column in renumberRowsByOffset for name: " + columnName);
            return false;
        }

        int value = Defs.NULL_INT;
        for (int i=this.used.nextSetBit(0); i>=0; i=this.used.nextSetBit(i+1))  {
            value = column[i];
            if ( Defs.isNull(value) ) {
                continue;
            }
            column[i] += offset;
        }
        return true;
    }

    /** Convenience method */
    public int[] getRowOrderMap( String label ) {
        int colId = getColumnIdx( label );
        if ( colId < 0 ) {
            General.showError("Expected order column to be the first column (0) but didn't found it.");
            return null;
        }
        return getRowOrderMap( colId );
    }

    /** Use the given column to reconstruct the order of the tuples. The method assumes that the column contains
     *a list of randomly ordered elements 0 through n-1 with each element occurring exactly once. The method
     *is very fast; order of N for one scan.
     *If the order is messed up this routine shouldn't fail but print the error
     *and return null;
     *The routine will only include rows that actually have a non-null value for the order column.
     *More explicitly: it will return a map of length null if the order column contains null values.
     *Give a negative number like -1 to use the column that has the default name for the order column.
     */
    public int[] getRowOrderMap( int orderColumnIdx ) {
        int countDone = 0;
        if ( orderColumnIdx < 0 ) {
            orderColumnIdx = getColumnIdx( DEFAULT_ATTRIBUTE_ORDER_ID);
            if ( orderColumnIdx < 0 ) {
                General.showError("Failed to find a column for the order with the name: " + DEFAULT_ATTRIBUTE_ORDER_ID);
                return null;
            }
        }
//        General.showDebug("Doing getRowOrderMap for a sizeRows: " + sizeRows);
        int[] map = new int[ sizeRows ]; // could become smaller still.
        int[] order = getColumnInt( getColumnLabel(orderColumnIdx));
//        General.showDetail("Found order map of lenght: " + order.length + " with elements: " + PrimitiveArray.toString(order));
//        General.showDetail("Found order map of physical lenght: " + order.length );
        if ( order ==  null ) {
            General.showError("column at: " + orderColumnIdx + " doesn't contain ints so it can't be used for keeping order.");
            return null;
        }
        Arrays.fill( map, NOT_AN_INDEX ); // Note those that have not been set yet.
        for (int i=0;i<sizeMax;i++) {
            if ( used.get(i) ) {
                int order_i = order[i];
                if ( Defs.isNull( order_i )) {
                    continue; // if elements are skipped then the map needs to be resized.
                }
                // Check if order_i is possible..... we don't want to crash the program if the order is lost.
                if ( order_i < 0 || order_i >= sizeRows ) {
                    General.showError("In relation: " + name);
                    General.showError("(0) impossible row order id: " + order_i +" should be in range of [0,"+sizeRows+">.");
                    return null;
                }
                if ( map[ order_i ] == NOT_AN_INDEX ) {
                    map[ order_i ] = i;
                    countDone++;
                } else {
                    General.showError("Already found a row (physical address: " + map[ order[i] ] +")");
                    General.showError("with row id: " + order[i] + ". Now at row with physical address : " + i);
                    return null;
                }
            }
        }
        // Next code shouldn't be giving hits...
        for (int i=0;i<countDone;i++) {
            if ( map[ i ] == NOT_AN_INDEX ) {
                General.showError("Code bug. Found no row with row id: " + i );
                return null;
            }
        }
        int[] mapReduced = new int[ countDone ];
        System.arraycopy( map, 0, mapReduced, 0, countDone);
//        General.showDetail("Found map of length: " + mapReduced.length + " with elements: " + PrimitiveArray.toString(mapReduced));
//        General.showDebug("Found map of length: " + mapReduced.length );
        if ( mapReduced.length == 0 ) {
            General.showWarning("Found map of lenght: 0" );
            General.showWarning("Perhaps the order column was not filled with anything?");
        }
        return mapReduced;
    }


    /** Use the given column to reconstruct the order of the tuples. The method assumes that the column contains
     *a list of randomly ordered elements 0 through n-1 with each element occurring exactly once. The method
     *is very fast; order of N for one scan.
     *If the order is messed up this routine shouldn't fail but print the error
     *and return null;
     *Method doesn't change order in given column name.
     */
    public int[] getRowOrderMap( String label, BitSet elementsToOrder ) {

        int countDone = 0;
        int countToOrder = elementsToOrder.cardinality();

        int[] map = new int[ countToOrder ];
        int[] order = getColumnInt(label);
        if ( order == null ) {
            General.showError("Failed to get order column");
            return null;
        }
        Arrays.fill( map, NOT_AN_INDEX ); // Note those that have not been set yet.
        for (int i=elementsToOrder.nextSetBit(0);i>-1;i=elementsToOrder.nextSetBit(i+1)) {
            if ( ! used.get(i) ) {
                General.showError("Can't order element not in use: " + i);
                return null;
            }
            int order_i = order[i];
            // Check if order_i is possible..... we don't want to crash the program if the order is lost.
            if ( order_i < 0 || order_i >= countToOrder ) {
                General.showError("In relation: " + name);
                General.showError("(1) impossible row order id: " + order_i +" should be in range of [0,"+countToOrder+">.");
                return null;
            }
            if ( map[ order_i ] == NOT_AN_INDEX ) {
                map[ order_i ] = i;
                countDone++;
            } else {
                General.showError("Already found a row (physical address: " + map[ order[i] ] +")");
                General.showError("with row id: " + order[i] + ". Now at row with physical address : " + i);
                return null;
            }
        }
        // Next code shouldn't be giving hits...
        for (int j=0;j<countToOrder;j++) {
            if ( map[ j ] == NOT_AN_INDEX ) {
                General.showError("Code bug. Found no row with row id: " + j );
                return null;
            }
        }
        if ( countDone != countToOrder ) {
            General.showError("number of to order rows not identical to the given size.");
            General.showError("used: " + countDone + " and sizeRows: " + countToOrder );
            return null;
        }
        return map;
    }


    /** Returns a list of the rows that are in use. In the order of the physical
     *addresses.
     */
    public int[] getUsedRowMapList() {
        return PrimitiveArray.toIntArray( used );
    }


    /** Just checking
     */
    public boolean isValidColumnIdx( int columnIdx ) {
        if ( ( columnIdx < 0 ) || ( columnIdx > (sizeColumns()-1))) {
            return false;
        }
        return true;
    }

    /** See namesake method */
    public boolean convertDataTypeColumn( String label, int dataType, String format ) {
        int columnIdx = getColumnIdx(label);
        if ( ! isValidColumnIdx( columnIdx ) ) {
             General.showError("given columnName doesn't exist: " + label );
             General.showError("Available column names are: " + Strings.toString( columnOrder ));
             return false;
        }
        return convertDataTypeColumn( columnIdx, dataType, format );
    }

    /** Will attempt to convert all columns if the argument isn't null.
     *prints a warning if the argument is null.
     */
    public boolean convertDataTypeAllColumn( int[] dataTypeList ) {
        if ( dataTypeList == null ) {
            General.showWarning("Data type list is null; skipping conversion of all columns");
            return true;
        }
        for (int c=0;c<dataTypeList.length;c++) {
            boolean status = convertDataTypeColumn( c, dataTypeList[c], null );
            if ( ! status ) {
                return false;
            }
        }
        return true;
    }

    /** Will attempt to convert the data type of a certain column to the given
     *data type. Index will be dropped if present.
     *For string values the data will be formatted so that they can be read in by
     *MySQL. E.g. for an original boolean the result will be 'T' or 'F'.
     */
    public boolean convertDataTypeColumn( int columnIdx, int dataType, String format ) {

        if ( ! isValidColumnIdx( columnIdx ) ) {
             General.showError("given columnIdx doesn't exist" );
             return false;
        }
        String label = getColumnLabel(columnIdx);

        if ( containsIndex( label ) ) {
            removeIndex( label );
        }

        int originalDataType = getColumnDataType(label);

        if ( dataType == originalDataType ) {
            General.showWarning("data type to convert to is the same as the original; nothing done for column labeled: " + label);
            return true;
        }

        Object column = null;
        Object columnIn = getColumn(columnIdx);

        try {
            switch (  originalDataType ) {
                case DATA_TYPE_STRINGNR:
                case DATA_TYPE_STRING: {
                    switch (  dataType ) {
                        case DATA_TYPE_BIT: {
                            column = PrimitiveArray.convertString2Bit( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_SHORT: {
                            column = PrimitiveArray.convertString2Short( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_INT: {
                            column = PrimitiveArray.convertString2Int( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_FLOAT: {
                            column = PrimitiveArray.convertString2Float( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_DOUBLE: {
                            column = PrimitiveArray.convertString2Double( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_STRINGNR: {
                            if ( format != null ) {
                                column = PrimitiveArray.convertString2String( columnIn, dataType, format );
                            } else {
                                column = PrimitiveArray.convertString2StringNR( columnIn, dataType );
                            }
                            break;
                        }
                        case DATA_TYPE_STRING: {
                            if ( format != null ) {
                                column = PrimitiveArray.convertString2String( columnIn, dataType, format );
                            } else {
                                if ( originalDataType == DATA_TYPE_STRINGNR ) {
                                    column = getColumn(columnIdx);
                                }
                            }
                            break;
                        }
                        case DATA_TYPE_ARRAY_OF_INT: {
                            column = PrimitiveArray.convertString2ArrayOfInt( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_ARRAY_OF_FLOAT: {
                            column = PrimitiveArray.convertString2ArrayOfFloat( columnIn, dataType );
                            break;
                        }
                        case DATA_TYPE_ARRAY_OF_STR: {
                            column = PrimitiveArray.convertString2ArrayOfString( columnIn, dataType );
                            break;
                        }
                        default: {
                            General.showError("-b- uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                case DATA_TYPE_INT: {
                    //General.showDebug("Now really using from data type int");
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            //General.showDebug("Now really using to data type String");
                            column = PrimitiveArray.convertInt2String( columnIn, dataType, format );
                            break;
                        }
                        default: {
                            General.showError("-c-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                case DATA_TYPE_FLOAT: {
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            column = PrimitiveArray.convertFloat2String( columnIn, dataType, format );
                            break;
                        }
                        default: {
                            General.showError("-d-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                case DATA_TYPE_DOUBLE: {
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            column = PrimitiveArray.convertDouble2String( columnIn, dataType, format );
                            break;
                        }
                        default: {
                            General.showError("-d-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                case DATA_TYPE_BIT: {
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            // Ensure the right size
                            BitSet columnIn2 = new BitSet(sizeMax);
                            columnIn2.or( (BitSet) columnIn );
                            column = PrimitiveArray.convertBit2String( columnIn2, dataType, format );
                            break;
                        }
                        default: {
                            General.showError("-d-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                case DATA_TYPE_OBJECT: {
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            column = PrimitiveArray.convertObject2String( columnIn );
                            break;
                        }
                        default: {
                            General.showError("-d-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                    break;
                }
                default: {
                    General.showDebug("-a-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                    General.showDebug("Returning an empty result");
                    switch (  dataType ) {
                        case DATA_TYPE_STRING: {
                            column = new String[sizeMax];
                            break;
                        }
                        default: {
                            General.showError("-e-uncoded conversion attempted from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
                            return false;
                        }
                    }
                }
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            General.showDebug("Converting data for relation   :" + name);
            General.showDebug("Converting data type for column:" + getColumnLabel(columnIdx));
            General.showDebug("Original data type is          :" + dataTypeList[ originalDataType]);
            General.showDebug("New data type is               :" + dataTypeList[ dataType]);
            General.showDebug("Format is                      :" + format);
            return false;
        }

        if ( column == null ) {
            General.showError("conversion failed from " + dataTypeList[originalDataType]  + " to " + dataTypeList[dataType]  );
            General.showError("will maintain column in old data format"  );
            return false;
        }

        // Replace the old column with the new.
        replaceColumn( label, dataType, column );

        return true;
    }



    /** not very useful other than testing? */
    public boolean convertDataTypeAllColumnsString2StringNR() {
       for (int c=0;c<sizeColumns();c++) {
            String label = getColumnLabel(c);
            if ( getColumnDataType(label) == DATA_TYPE_STRING ) {
                General.showDebug("converting column: " + label);
                convertDataTypeColumn(c, DATA_TYPE_STRINGNR, null );
            }
        }
        return true;
    }

    public void updateIndicesAll() {
        updateIndex(null,-1);
    }

    /** Actually completely reconstruct the index. If the column argument is
     *null then do all columns. If the index type is -1 then do all that are
     *present.
     */
    public boolean updateIndex( String columnLabel, int indexType ) {

        boolean status;
        // Do all columns for which there are indices.
        if ( columnLabel == null ) {
            Set keys = indices.keySet();
            for (Iterator it=keys.iterator();it.hasNext();) {
                String key = (String) it.next();
                status = updateIndex(key, indexType);
                if ( ! status ) {
                    General.showError("Failed to add index for column labeled: " + key);
                    return false;
                }
            }
            return true;
        }

        if ( ! hasColumn(columnLabel)) {
            General.showError("No column with label: " + columnLabel );
            return false;
        }

        // Do the update for one column for one index type
        if ( indexType > -1 ) {
            Index index = getIndex(columnLabel, indexType);
            status = index.updateIndex(this, columnLabel);
            if ( ! status ) {
                General.showError("Failed to update index for column labeled: " + columnLabel);
                return false;
            }
        // Recursive call for multiple index types on this one column.
        } else {
            Index[] al = (Index[]) indices.get( columnLabel );
            if ( al == null ) {
                return true;
            }
            for (int j=0;j<Index.INDEX_TYPE_COUNT;j++) {
                Index index = al[j];
                if ( index == null ) {
                    continue;
                }
                updateIndex( columnLabel, j);
            }
        }

        return true;
    }

    /** Checks if for this column there is an index of the specified type */
    public boolean containsIndex(String columnLabel, int indexType) {
        Object index = getIndex( columnLabel, indexType );
        if ( index == null ) {
            return false;
        }
        return true;
    }

    /** Checks if for this column there is at least one index */
    public boolean containsIndex(String columnLabel) {
        if ( indices.containsKey( columnLabel )) {
            return true;
        }
        return false;
    }

    /** Checks to see if there are any nulls in the table. It will check per column
     *starting at the first column.
     */
    public boolean containsNull(boolean doReportNulls, int numberToReportPerColumn) {
        boolean overall_status = false;
        for (Iterator it=columnOrder.iterator();it.hasNext();) {
            String label = (String) it.next();
            boolean status = columnContainsNull( label, doReportNulls, numberToReportPerColumn);
            if ( status ) {
                overall_status = true;
                if ( ! doReportNulls ) {
                    return overall_status;
                }
                // keep reporting.
            }
        }
        return overall_status;
    }

    public boolean columnIsNullable( String label) {
        int dataType = getColumnDataType(label);
        if ( ! supportsNulls[ dataType ] ) {
            General.showDebug("found column " + label + " of type: " + dataTypeList[dataType] +
            " that doesn't support nulls so returning false.");
            return false;
        }
        return true;
    }

    /** Minimum number of reported nulls is 1 in the case the boolean is set */
    public boolean columnContainsNull(String label, boolean doReportNulls, int numberToReport) {
        int reportedNulls = 0;

        if ( ! containsColumn( label ) ) {
            General.showError( "no column with label: " + label + " in columnContainsNull; so no null");
            return false;
        }

        Object column = getColumn( label );
        int dataType = getColumnDataType(label);
        if ( ! columnIsNullable(label) ) {
            return false;
        }

        int columnIdx = getColumnIdx(label);

        switch ( dataType ) {
            case DATA_TYPE_BIT:
            case DATA_TYPE_BYTE: {
                return false; // explicitelyl listed but already checked for above.
            }
            case DATA_TYPE_CHAR: {
                char[] temp = (char[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNull( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]"+ " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_SHORT: {
                short[] temp = (short[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNull( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_INT: {
                int[] temp = (int[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNull( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_FLOAT: {
                float[] temp = (float[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1)) {
                    if ( Defs.isNull( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_DOUBLE: {
                double[] temp = (double[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNull( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_LINKEDLIST: {
                return false;
            }
            case DATA_TYPE_LINKEDLISTINFO: {
                return false;
            }
            case DATA_TYPE_STRING: {
                String[] temp = (String[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNullString( temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            // Next one can be speeded up a lot!
            case DATA_TYPE_STRINGNR: {
                String[] temp = (String[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( Defs.isNullString(temp[i] )) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_ARRAY_OF_INT: {
                int[][] temp = (int[][]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( temp[i] == null ) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_ARRAY_OF_FLOAT: {
                float[][] temp = (float[][]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                    if ( temp[i] == null ) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            case DATA_TYPE_OBJECT: {
                Object[] temp = (Object[]) column;
                for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1)) {
                    if ( temp[i] == null ) {
                        if ( ! doReportNulls ) return true;
                        General.showDebug("found a null value in column: " + getColumnLabel(columnIdx) + "["+columnIdx+"]" + " in row: " + toStringRow(i) );
                        reportedNulls++;
                        if ( reportedNulls >= numberToReport ) {
                            return true;
                        }
                    }
                }
                break;
            }
            default: {
                General.showError("code bug in columnContainsNull for colum: [" + column  + "]");
                General.showError("Unknown type: " + dataType + ". Known are: " +
                    Strings.concatenate( dataTypeList, "," ) );
                return false;
            }
        }
        if ( reportedNulls > 0 ) {
            return true;
        }
        return false;
    }


    /** If the index doesn't already exist; create it */
    public Index getIndex(String columnLabel, int index_type ) {
        if ( (index_type < 0 ) ||
             (index_type >= Index.INDEX_TYPE_COUNT )) {
            General.showError("Index type requested from getIndex is out of bounds:" + index_type);
            return null;
        }

        Index[] al = (Index[]) indices.get( columnLabel );
        if ( (al == null) || (al[ index_type ]==null)) {
            if ( ! containsColumn( columnLabel ) ) {
                General.showError("Code bug in getIndex failed to addIndex for unexisting colum: [" + columnLabel  + "]");
                General.showError("This relation is:\n" + toString(true, true, true, true, false, false ));
                return null;
            }

            if ( ! addIndex( columnLabel, index_type )) {
                General.showError("Code bug in getIndex failed to addIndex for colum: [" + columnLabel  + "]");
                General.showError("and index_type: [" + index_type + "]");
                General.showError("and index_type name : [" + Index.index_type_names[ index_type ] + "]");
                return null;
            }
            return getIndex(columnLabel, index_type ); // I hope this doesn't cycle.;-) It did once;-(
        }
        Index index = al[ index_type ];
        if ( index == null ) {
            General.showCodeBug("Returned index is STILL null.");
            General.showCodeBug("Requested index for: columnLabel: " + columnLabel + " and index_type: " +
               Index.list_type_names[ index_type ] );
            //General.showCodeBug("Got index list: " + Strings.toString( al));
        }
        return index;
    }



    /** Using the sorted int index on a column to find rids with exact matches and then
     *return the int VALUES in the corresponding rows of a DIFFERENT column.
     */
    public IntArrayList getValueListBySortedIntIndex(
        IndexSortedInt index,
        int indexValue,
        String columnLabelValue,
        IntArrayList result ) {

        if ( index == null ) {
            General.showCodeBug("Got null as argument for sorted index in relation: " + name);
            return null;
        }
        IntArrayList rows = (IntArrayList) index.getRidList(  new Integer(indexValue),
            Index.LIST_TYPE_INT_ARRAY_LIST, null);
        if ( rows == null ) {
            General.showDebug("Failed to get list of rows for value" );
            return null;
        }
        //General.showDebug("In getValueListBySortedIntIndex got rids: " + PrimitiveArray.toString( rows ));
        //General.showDebug("now looking for the associated values in the column with label: " + columnLabelValue);
        int[] values = getColumnInt( columnLabelValue );
        if ( values == null ) {
            General.showCodeBug("Failed to get values column with label: " + columnLabelValue);
            return null;
        }

        int setSize = rows.size();
        if ( result == null ) {
            result = new IntArrayList(setSize);
        }
        result.setSize( setSize );
        for (int i=0;i<setSize;i++) {
            result.setQuick( i, values[ rows.getQuick(i) ]);
        }
//        General.showDebug("Found the following rids of values: " + PrimitiveArray.toString( result ));
        if ( result.size() < 1 ) {
            General.showDebug("Didn't find any result in Relation.getValueList_IntArrayList_BySortedIntIndex for index value: " + indexValue);
        }
        return result;
    }


    /** Removes all indices if the columnLabel argument is null
     */
    public boolean removeIndex(String columnLabel) {
        if ( columnLabel == null ) {
            return removeIndices();
        }

        //General.showDebug("clearing index for column label: " + columnLabel);
        if ( indices.containsKey( columnLabel ) ) {
            indices.remove(columnLabel);
            return true;
        } else  if ( hasColumn(columnLabel)) {
            General.showError("No index present for column labeled: " + columnLabel);
        } else {
            General.showError("No column present labeled: " + columnLabel);
            General.showError("so no index there either.");
        }
        return false;
    }

    /** Clears all indices */
    public boolean removeIndices() {
        indices.clear();
        return true;
    }


    /** Returns the name of the row i. This is not the column name dummy. */
    public String getName( int i ) {
        return ((String[]) getColumn(  DEFAULT_ATTRIBUTE_NAME ))[i];
    }

    /**
     * Get the next reserved id or the argument in case that one hasn't been taken either.
     * If no more reservations exist then make DEFAULT_GROWTH_SIZE_RESERVATIONS (was 100) more
     * reservations first. We take a hardcoded number so the algorithm is efficient,
     * not too greedy and we don't have to store the initial reservation size which
     * would allow better guesses.
     * This might cause the overall table to physically grow of course, in which case
     * the convenience variables need to be reset! To indicate that the table grew
     * the returned rid will be DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW.
     * Note that the indices on this relation will be nilled if the relation grew.
     * @param currentReservedRid Last returned reserved rid or zero to start scan.
     * @return Next reserved id or Defs.NULL_INT in case of a code bug or
     * DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW in case the relation grew.
     * Call again when that happens.
     */
    public int getNextReservedRow( int currentReservedRid ) {
        int nextRid = reserved.nextSetBit( currentReservedRid );
        // most common situation where there were still reserved rows left.
        if ( nextRid >= 0 ) {
            reserved.clear( nextRid );
            return nextRid;
        }

        /** Make sure none are left. Maybe the method argument was bogus.
         *We don't resize often so checking doesn't matter.
         */
        //General.showDebug("Found no reserved rows looking forward from id (inclusive): " + currentReservedRid);
        nextRid = reserved.nextSetBit( 0 );
        if ( nextRid >= 0 ) {
            reserved.clear( nextRid );
            return nextRid;
        }

        //General.showDebug("Found no reserved rows left at all.");
        /** We ran out for sure. Extend the reservation.*/
        // No bits left set in old variable and
        // the new var is larger than the old one so no combination is needed.
        //General.showOutput("Reserving another batch of " + DEFAULT_GROWTH_SIZE_RESERVATIONS + " rows.");
        int oldMaxSize = sizeMax;
        reserved = getNewRows( DEFAULT_GROWTH_SIZE_RESERVATIONS );
        if ( reserved == null ) {
            General.showCodeBug("Failed to get new rows in Relation.getNextReservedRow for relation with name: " + name);
            return -1;
        }

        // See if the arrays grew, which means the caller might want to know that.
        // The caller needs to call again which is a minor draw-back.
        if ( oldMaxSize != sizeMax ) {
            removeIndices();
            return DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW;
        }
        nextRid = reserved.nextSetBit( 0 );
        if ( nextRid < 0 ) {
            General.showCodeBug("Failed to get next reserved row even after successful expansion.");
            return -1;
        }

        reserved.clear( nextRid );
        return nextRid;
    }

    /** All rows that were reserved before will be deleted; in other words their
     *reservation will be cancelled.
     */
    public boolean reserveRows( int rowsToReserve ) {
        if ( reserved == null ) {
            General.showCodeBug("Reservation variable was null this is bad because we check for existing reservations before making new ones." );
            return false;
        }

        // It's ok to call remove with no set bits in selection.
        boolean status = removeRows( reserved, false, false );
        if ( ! status ) {
            reserved = new BitSet();
            General.showCodeBug("Failed to remove rows. Not reserving any new ones.");
            return false;
        }

        reserved = getNewRows(rowsToReserve);
        if ( reserved == null ) {
            reserved = new BitSet();
            General.showCodeBug("Failed to make reservation for number of rows of: " + rowsToReserve);
            return false;
        }
        return true;
    }

    /** All rows that were reserved before will be deleted; in other words their
     *reservation will be cancelled.
     */
    public boolean reserveRow( int rowToReserve ) {
        if ( reserved == null ) {
            General.showCodeBug("Reservation variable was null this is bad because we check for existing reservations before making new one." );
            return false;
        }

        if ( used.get( rowToReserve ) ) {
            General.showWarning("Row to reserve is already in use for rid: " + rowToReserve );
            return false;
        }

        reserved.set(rowToReserve);
        return true;
    }

    /** Remove the row and reserve it.
     *reservation will be cancelled.
     */
    public boolean rereserveRow( int rowToReserve ) {
        if ( reserved == null ) {
            General.showCodeBug("Reservation variable was null this is bad because we check for existing reservations before making new one." );
            return false;
        }

        if ( ! used.get( rowToReserve ) ) {
            General.showWarning("Row to REreserve is not in use for rid: " + rowToReserve );
            return false;
        }

        boolean status = removeRow( rowToReserve, true );
        if ( ! status ) {
            General.showCodeBug("Row to REreserve failed to be removed for rid: " + rowToReserve );
            return false;
        }

        status = reserveRow(rowToReserve);
        if ( ! status ) {
            General.showCodeBug("Row to REreserve failed to be reserved for rid: " + rowToReserve );
            return false;
        }
        return true;
    }

    /** All rows that were reserved before will now cheaply be deleted; in other words their
     *reservation will be cancelled.
     */
    public boolean cancelAllReservedRows() {
        if ( reserved == null ) {
            General.showCodeBug("Reservation variable was null this is bad because we check for reservations before cancelling any." );
            reserved = new BitSet();
            return false;
        }

        // It's ok to call remove with no set bits in selection.
        // Resetting all values to their defaults.
        boolean status = removeRows( reserved, false, true );
        if ( ! status ) {
            reserved = new BitSet();
            General.showCodeBug("Failed to remove rows. Assume rows do exist now and cancel reservations..");
            return false;
        }

        reserved = new BitSet();
        return true;
    }

    /** Based on a relations' name return the name for a column
     *that refers to the physcial column. Simply appending a string to the end.
     *E.g. atom_main -> atom_main_id and
     *dist_constr_viol_avg -> dist_constr_viol_avg_id
     */
    public String getDefaultColumnNameForPhysical() {
        return name + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX;
    }


    /** Shifts all values except if they're nulls.
     */
    public boolean shiftValuesInColumnBlockInt( String label, int start, int end, int shift ) {

        int[] intColumn = (int[]) getColumnInt(label);
        if ( intColumn == null ) {
            General.showError("Failed to get int[] column for label: " + label);
            return false;
        }
        for (int rid=start; rid< end; rid++) {
            if ( ! Defs.isNull( intColumn[ rid ] ) ) { // slowest component (inline code by hotspot/jit compiler?)
                intColumn[ rid ] += shift;
            }
        }
        return true;
    }

    /** Order in source column can start at -shift whereas ordering in target should start at 0
     */
    public boolean copyToOrderColumn( String label, int shift ) {
         boolean status;
        if ( getColumnDataType(label) != DATA_TYPE_INT ) {
            General.showWarning("The given column: " + label + " is not of type int; which is the only type supported now.");
            General.showWarning("The data type of the column is: " + dataTypeList[ getColumnDataType(label)]);
            return false;
        }
        // Ensure presence of an order column
        if ( ! containsColumn( DEFAULT_ATTRIBUTE_ORDER_ID )) {
            status = addColumnForOverallOrder();
            if ( ! status ) {
                General.showWarning("Failed to add column for overall order. Not using column for order: "+label);
                return false;
            }
        }

        status = copyColumnBlock( this,  label, 0, DEFAULT_ATTRIBUTE_ORDER_ID, 0, sizeMax);
        if ( ! status ) {
            General.showWarning("Failed to copy column block to order column from column: "+label);
            return false;
        }
        status = shiftValuesInColumnBlockInt( DEFAULT_ATTRIBUTE_ORDER_ID, 0, sizeMax, shift);
        if ( ! status ) {
            General.showWarning("Failed to (shiftValuesInColumnBlockInt) copy column block to order column from column: "+label);
            return false;
        }

        if ( ! isSortedFromOneInColumn( label ) ) {
            //General.showDebug("The given column: " + label + " still needs sorting.");
            return true;
        }
        return true;
    }

    /** Checks if the ordering in the column is natural e.g. starting from one and skipping no number going
    up. If the argument is null then the default column will be checked. Returns false if no such
    column is present. Method does allow for gaps in the relation.
    *Returns true if column is empty (no rows used).
    */
   public boolean isSortedFromOneInColumn( String label ) {
       if ( label == null ) {
           label = DEFAULT_ATTRIBUTE_ORDER_ID;
       }
       if ( ! containsColumn(label) ) {
           General.showError( "Not isSortedFromOneInColumn because column not present: " + label);
           return false;
       }

       Object column = getColumn(label);
       if ( getColumnDataType(label) != DATA_TYPE_INT ) {
           General.showCodeBug("Data type not supported in isSortedFromOneInColumn " +
               dataTypeList[getColumnDataType(label)] + " for column: " + label);
           return false;
       }
       int[] col = (int[]) column;
       int k = 1;
       for (int i=used.nextSetBit(0); i>=0; )  {
           if ( col[i] != k ) {
               //General.showDebug("In column: " + label + " on used row: " + i + " value was not sorted");
               //General.showDebug("Expected: " + k + " but found: " + col[i]);
               return false;
           }
           k++;
           i=used.nextSetBit(i+1);
       }
       return true;
   }

   /** Checks if the ordering in the column is consecutive.
   *Returns row idx for violating row or -1 if all ok, -2 for error.
   */
  public int isConsecutiveInColumn( String label ) {
        if (label == null) {
            label = DEFAULT_ATTRIBUTE_ORDER_ID;
        }
        if (!containsColumn(label)) {
            General.showError("Not isConsecutiveInColumn because column not present: " + label);
            return DEFAULT_VALUE_INDICATION_COLUMN_ORDER_CHECK_ERROR;
        }

        Object column = getColumn(label);
        if (getColumnDataType(label) != DATA_TYPE_INT) {
            General.showCodeBug("Data type not supported in isConsecutiveInColumn "
                    + dataTypeList[getColumnDataType(label)] + " for column: " + label);
            return DEFAULT_VALUE_INDICATION_COLUMN_ORDER_CHECK_ERROR;
        }
        int[] col = (int[]) column;
        int firstUsedRowIdx = used.nextSetBit(0);
        int k = col[firstUsedRowIdx];
        for (int i = used.nextSetBit(0); i >= 0; i = used.nextSetBit(i + 1)) {
            int l = col[i];
            if (l == k)
                continue;
            if (l == k + 1) {
                k += 1;
                continue;
            }
            General.showDebug("In column: " + label + " on used row: " + i + " value was not sorted");
            General.showDebug("Expected: " + k + " (or one higher) but found: " + l);
            return i;
        }
        return DEFAULT_VALUE_INDICATION_COLUMN_CONSECUTIVE;
  }

    /** Returns true if all elements in given column are of the same value
     *given. Returns false for many reasons; e.g. column doesn't exist or
     *is of the wrong type.
     */
    public boolean areAllElementsOfIntValue( String label, int value ) {
        if ( ! containsColumn(label) ) {
            General.showError("In Relation.areAllElementsOfIntValue failed to find column with label: " + label);
            return false;
        }
        if ( getColumnDataType(label) != DATA_TYPE_INT ) {
            General.showError("In Relation.areAllElementsOfIntValue column is not of data type int but: " + dataTypeList[getColumnDataType(label)] );
            return false;
        }
        IndexSortedInt idx = (IndexSortedInt) getIndex(label, Index.INDEX_TYPE_SORTED);
        if ( idx == null ) {
            General.showError("Failed to get a sorted index for label: " + label + " in relation with name: "+name);
            return false;
        }
        int[] column = getColumnInt(label);
        if ( column == null ) {
            General.showError("Failed to get an int[] column for label: " + label + " in relation with name: "+name);
            return false;
        }

        if(( column[idx.getRidValueMinInt()] != value) ||
           ( column[idx.getRidValueMaxInt()] != value)) {
            General.showDebug("Found a min value for the column: "+column[idx.getRidValueMinInt()]+" different than the value expected: " + value + " or: ");
            General.showDebug("found a max value for the column: "+column[idx.getRidValueMaxInt()]);
            return false;
        }
        return true;
    }

    /** Append data from a different relation to this relation. Should be fast.
     *Creates space as needed. The columns to be copied are specified in the argument .
     *Columns in the original table will be maintained and extended with default values if
     *not copied from the other relation. If the columns don't exist in this relation
     *and the check bit is not set they will be created. Otherwise an error is printed
     *and false returned.
     *Only used rows in the other relation will be used. Unused rows in this relation
     *will be filled as required.
     */
    public boolean append( Relation otherRelation, int startIdxOther, int endIdxOther,
        ArrayList columnOrderOther, boolean allowNewColumns ) {
        // Check input
        if ( columnOrderOther == null ) {
            General.showError("Need a valid columnOrderOther argument");
            return false;
        }
        if ( startIdxOther >=  endIdxOther ) {
            General.showError("Found startIdxOther >=  endIdxOther:" + startIdxOther + " >= " +  endIdxOther );
            return false;
        }

        // Ensure all required columns in this relation are added (before or here).
        for (int c=0;c<columnOrderOther.size();c++) {
            String label = (String) columnOrderOther.get(c) ;
            if ( ! containsColumn(label )) {
                if ( ! allowNewColumns ) {
                    General.showDebug("Not allowed to add new column: " + label);
                    return false;
                }
                General.showDebug("Adding new column: " + label);
                insertColumn(label, getColumnDataType(label), null);
            }
        }



        /** Split the otherRelation into blocks that can be copied fast. Hopefully
         *even in one blow!
         */
        // Get first block.
        int startIdxOtherBlock  = otherRelation.used.nextSetBit(   startIdxOther );
        if ( startIdxOtherBlock == -1 ) {
            General.showWarning("No rows in use from startIdxOther: " + startIdxOther + ". None to copy then" );
            return true;
        }
        int endIdxOtherBlock    = otherRelation.used.nextClearBit( startIdxOtherBlock );
        if ( endIdxOtherBlock == -1 || endIdxOtherBlock > endIdxOther ) { // All are in use; trust the caller on the size (maybe bad;-).
            endIdxOtherBlock = endIdxOther;
        }

        /** Start iteration. Iterating should be fast if data is fragmented.*/
        while ( true ) {

            // Make room; coming through!
            int blockOtherSize = endIdxOtherBlock - startIdxOtherBlock;
            //General.showDebug("Using src block from: " + startIdxOtherBlock + " to: " + endIdxOtherBlock);
            int maxSizeNew = blockOtherSize + sizeRows;
            if ( maxSizeNew > sizeMax ) {
                ensureCapacity(maxSizeNew);
            }

            int otherSizeDone  = 0;
            int startIdxBlock  = 0;
            int endIdxBlock    = 0;
            while ( otherSizeDone < blockOtherSize ) {
                /** Find space in this relation */
                startIdxBlock  = used.nextClearBit(endIdxBlock);
                if ( startIdxBlock == -1 ) {
                    General.showError("No rows in use from startIdxBlock: " + startIdxBlock + ". But space should have existed. -A-" );
                    return false;
                }
                endIdxBlock    = used.nextSetBit( startIdxBlock );
                if ( endIdxBlock == -1 ) { // All are in use; trust the caller on the size (maybe bad;-).
                    endIdxBlock = sizeMax;
                }
                // Adjust size of block to adjust to the maximum
                // of the other block size to append.
                if ( (endIdxBlock-startIdxBlock) > (blockOtherSize - otherSizeDone) ) {
                    endIdxBlock = startIdxBlock + blockOtherSize - otherSizeDone;
                }
                int blockSize = endIdxBlock - startIdxBlock ;
                //General.showDebug("Using dst block from: " + startIdxBlock + " to: " + endIdxBlock);

                // Mark the rows as used
                used.set( startIdxBlock, endIdxBlock);
                sizeRows += endIdxBlock - startIdxBlock;
                // Just do those that are needed.
                for (int c=0;c<columnOrderOther.size();c++ ) {
                    String label = (String) columnOrderOther.get(c);
                    // Call private method for actual work.
                    //General.showDebug("Appending column with label: " + label );
                    copyColumnBlock( otherRelation, label, startIdxOtherBlock+otherSizeDone, label, startIdxBlock, blockSize );
                }
                otherSizeDone += blockSize;
            }

            //General.showDebug("Copied rows number: " + otherSizeDone );

            // Prepare for next iteration.
            startIdxOtherBlock  = otherRelation.used.nextSetBit(   endIdxOtherBlock );
            if ( ( startIdxOtherBlock == -1) || ( startIdxOtherBlock >= endIdxOther )) {
                break; // Only abort condition in while loop!
            }
            endIdxOtherBlock    = otherRelation.used.nextClearBit( startIdxOtherBlock );
            if ( endIdxOtherBlock == -1 || endIdxOtherBlock > endIdxOther ) { // All are in use; trust the caller on the size (maybe bad;-).
                endIdxOtherBlock = endIdxOther;
            }
        }

        /// NEED TO REINTERN THE STRINGS AFTER APPEND.
        return true;
    }

    /** Copies data from all columns including selected but
     *excluding used. It would be considerable faster if multiple rows could be copied. TODO
     */
    public boolean copyRow( int src, int dst ) {
        if ( ! used.get( src ) ) {
            General.showWarning("Tried to copy from a row that's not in use. Will not continue.");
            return false;
        }

        if ( ! used.get( dst ) ) {
            General.showWarning("Tried to copy to a row that's not in use. Will not continue.");
            return false;
        }

        if ( ( dst < 0 ) || ( dst >= sizeMax )) {
            General.showWarning("Destination is smaller than 0 or >= sizeMax: " + sizeMax + " It's: " + dst);
            return false;
        }

        int sizeColumns = sizeColumns();
        for (int column=0;column<sizeColumns;column++) {
            String label = getColumnLabel(column);
            int dataType = getColumnDataType(label);

            // Sanity checks
            if ( dataType == DATA_TYPE_INVALID ) {
                General.showWarning("Failed to get data type for column at position: " + column);
                return false;
            }
            switch ( dataType ) {
                case DATA_TYPE_BIT: {
                    BitSet temp = (BitSet) attr.get(label);
                    temp.set( dst, temp.get( src ));
                    continue;
                }
                case DATA_TYPE_CHAR: {
                    char[] temp = (char[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_BYTE: {
                    byte[] temp = (byte[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_SHORT: {
                    short[] temp = (short[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_INT: {
                    int[] temp = (int[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_FLOAT: {
                    float[] temp = (float[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_DOUBLE: {
                    double[] temp = (double[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_LINKEDLIST: {
                    General.showWarning("LinkedListArray doesn't have a clone method yet. copy row didn't do anything.");
                    return false;
                }
                case DATA_TYPE_LINKEDLISTINFO: {
                    General.showWarning("LinkedListArrayInfo doesn't have a clone method yet. copy row didn't do anything.");
                    return false;
                }
                case DATA_TYPE_STRING: {
                    String[] temp = (String[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_STRINGNR: {
                    String[] temp = (String[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                case DATA_TYPE_ARRAY_OF_INT: {
                    int[][] temp = (int[][]) attr.get(label);
                    if ( temp[src] == null ) {
                        temp[dst] = null;
                    } else {
                        temp[dst] = (int[]) temp[src].clone(); // use clone for a deep copy
                    }
                    continue;
                }
                case DATA_TYPE_ARRAY_OF_FLOAT: {
                    float[][] temp = (float[][]) attr.get(label);
                    if ( temp[src] == null ) {
                        temp[dst] = null;
                    } else {
                        temp[dst] = (float[]) temp[src].clone(); // use clone for a deep copy
                    }
                    continue;
                }
                case DATA_TYPE_OBJECT: {
                    Object[] temp = (Object[]) attr.get(label);
                    temp[dst] = temp[src];
                    continue;
                }
                default: {
                    General.showError("code bug in copyRow for row: [" + src + "] and colum: [" + column  + "]");
                    Object col = attr.get(label);
                    General.showDebug("Object type for column: " + label + " is " + col.getClass().getName());
                    General.showError("Unknown type: " + dataType + ". Known are: " +
                        Strings.concatenate( dataTypeList, "," ) );
                    return false;
                }
            }
        }
        return true;
    }

    /** Needs more efficient algorithm for bitset copy
     */
    public boolean copyColumnBlock( Relation srcRelation,  String label_src, int src_position,
                                                           String label_dst, int dst_position, int length ) {
        int dataType = getColumnDataType(label_dst);
        if ( dataType == DATA_TYPE_INVALID ) {
            General.showCodeBug("Invalid data type for relation in dst column: " + label_dst);
            return false;
        }

        int dataTypeSrc = srcRelation.getColumnDataType(label_src);
        if ( dataTypeSrc == DATA_TYPE_INVALID ) {
            General.showCodeBug("Invalid data type for relation in dst column: " + label_dst);
            return false;
        }

        if ( dataTypeSrc != dataType ) {
            General.showCodeBug("Data types differ between src column: " + label_src + " type: " + dataType   + " (" + dataTypeList[dataType]+")");
            if ( (dataTypeSrc >= 0) && (dataTypeSrc < dataTypeList.length) ) {
                General.showCodeBug("                      and dst column: " + label_dst + " type: " + dataTypeSrc+ " (" + dataTypeList[dataTypeSrc]+")");
            } else {
                General.showCodeBug("                      and dst column: " + label_dst + " type: " + dataTypeSrc+ " (NaN)");
            }
            return false;
        }

        Object src = srcRelation.getColumn( label_src );
        Object dst = getColumn( label_dst );

        try {
            switch ( dataType ) {
                case DATA_TYPE_CHAR:         {
                    System.arraycopy((char[])     src, src_position, (char[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_BYTE:         {
                    System.arraycopy((byte[])     src, src_position, (byte[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_SHORT:        {
                    System.arraycopy((short[])     src, src_position, (short[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_INT:          {
                    System.arraycopy((int[])     src, src_position, (int[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_FLOAT:        {
                    System.arraycopy((float[])     src, src_position, (float[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_DOUBLE:       {
                    System.arraycopy((double[])     src, src_position, (double[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_STRING:
                case DATA_TYPE_STRINGNR:
                case DATA_TYPE_OBJECT: { // Just copies a reference.
//                    General.showDebug("Doing System.arraycopy((Object[])");
                    System.arraycopy((Object[])     src, src_position, (Object[])       dst, dst_position, length );
                    break;
                }
                case DATA_TYPE_BIT: { // Very inefficient algorithm to be redone.
                    BitSet srcTemp = (BitSet) src;
                    BitSet dstTemp = (BitSet) dst;
                    int src_position_end = src_position+length;
                    int shift = dst_position-src_position;
                    int j = src_position+shift; // tried to optimize.
                    for (int i=src_position;i<src_position_end;i++ ) {
                        dstTemp.set( j, srcTemp.get(i));
                        j++; // might be fastest to increment by 1 i.s.o. recalculate by + operator.
                    }
                    break;
                }
                default: {
                    General.showCodeBug("Data type not supported in copyColumnBlock: " + dataType);
                    return false;
                }
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Data not copied in copyColumnBlock for destination label: " + label_dst);
            General.showError("Data type : " + dataTypeList[dataType]);
            return false;
        }
        return true;
    }

    /**
     *If containsHeaderRow is false then the default labels will be used.
     *dtd file if not existing defaults to all elements being STRING.
     *If it does exist,  it should be formatted as a csv file with the first two
     *columns having the column name and the column data type as strings.
     */
    public boolean readCsvFile( String file_name, boolean containsHeaderRow, String dtd_file_name) {

        //General.showDebug("Reading from file    : " + file_name);
        //General.showDebug(" with header         : " + containsHeaderRow);
        //General.showDebug(" and dtd             : " + dtd_file_name);
        Relation dtd = null;


        try {
            if ( dtd_file_name != null ) {
                dtd = new Relation("dtd_" + file_name, dbms);
                // some recursion.
                if ( ! dtd.readCsvFile(dtd_file_name, false, null)) {
                    General.showError("failed to read dtd");
                    return false;
                }
                dtd.renameColumn(0, "columnName");
                dtd.renameColumn(1, "columnDataType");
                // In case of fkcs
                if ( dtd.sizeColumns() == 4 ) {
                    dtd.renameColumn(2, "foreignRelationName");
                    dtd.renameColumn(3, "foreignColumnName");
                }
            }

            FileReader fr = new FileReader( file_name );
            if ( fr == null ) {
                General.showWarning("Failed to open resource as a stream from location: " + file_name);
                return false;
            }

            BufferedReader br = new BufferedReader( fr );
            //CSVParser parser = new CSVParser(br);
            ExcelCSVParser parser = new ExcelCSVParser(br);
            if ( parser == null ) {
                General.showWarning("Failed to open Excel CSV parser from location: " + file_name);
                return false;
            }
            // Parse the data. If this is too expensive then revise this routine so it
            // uses the line by line parser method getLine(). Method chosen here is to do
            // it with minimal programming effort.
            String[][] values = parser.getAllValues();
            if ( values == null ) {
                General.showError("no data read from csv file");
                return false;
            }
            if ( values.length <= 0 ) {
                General.showError("number of rows found: " + values.length);
                General.showError("but expected at least      : " + 1);
                return false;
            }
            // The following array will be overwritten as needed; merely a place holder at this point.
            ArrayList columnLabels = new ArrayList( Arrays.asList( values[0] ) );
            if ( containsHeaderRow ) {
                columnLabels = new ArrayList( Arrays.asList( values[0] ) );
                Strings.chomp( columnLabels );
            }
            int rows        = values.length;
            int columns     = PrimitiveArray.getMaxColumns( values );
            if (columns<1) {
                General.showError("Maximum number of columns found: " + columns);
                General.showError("but expected at least      : " + 1);
                return false;
            }
            if ( dtd != null ) {
                if ( dtd.sizeRows != columns ) {
                    General.showError("Found number of columns defined in dtd: " + dtd.sizeRows );
                    General.showError("which doesn't match the maximum number of columns in file: " + columns );
                    return false;
                }
            }

            if ( containsHeaderRow ) {
                rows--;
            }
            //General.showDebug("Read number of rows (excluding possible header): " + rows);
            //General.showDebug("Read number of columns                         : " + columns);

            // Keep name
            init(name,rows,dbms,null);

            BitSet bs = null;
            if ( rows > 0 ) {
                bs = getNewRows( rows ); // result isn't actually used
                if ( bs == null ) {
                    General.showError("Failed to get new rows");
                }
            }

            for (int c=0;c<columns;c++) {
                String columnName = "column_label_" + c;
        String dataTypeStr = "STRING";
                if ( dtd != null ) {
                    columnName  = dtd.getValueString(c, "columnName");
                    columnName = columnName.trim();
                    dataTypeStr = dtd.getValueString(c, "columnDataType");
                    dataTypeStr = dataTypeStr.trim();

                } else if (containsHeaderRow) {
                    columnName = values[0][c];
                }
                insertColumn(-1,columnName,DATA_TYPE_STRING,null);
                int r = 0;
                if ( containsHeaderRow ) {
                    r = 1;
                }
                int i=0;
                try { // let's find out where the mistakes are if any.
                    for (;i<rows;i++) {
                        // Skip any row whose columns aren't long enough
                        // this test obviously slows things down.
                        if ( c < values[r].length ) {
                            setValue(i,columnName,values[r][c]);
                        }
                        r++;
                    }
                } catch (Throwable t) {
                    General.showThrowable(t);
                    General.showError("Failed to set values in relation for values at row: " +r + " and column: " +c );
                    return false;
                }
                int dataType = dataTypeArrayList.indexOf( dataTypeStr );
                if ( dataType < 0 ) {
                    General.showError("Data type listed in dtd is not supported: '" + dataTypeStr + "' Skipping read.");
                    General.showError("For column: " + columnName + " ("+c+")");
                    General.showWarning("Known are: " + Strings.concatenate( dataTypeList, "," ) );
                    continue;
                }
                if ( ! dataTypeStr.equals( "STRING") ) {
                    convertDataTypeColumn(columnName, dataType, null );
                }
                // After data type is converted the fkc can be added.
                if ( (dtd != null) && (dtd.sizeColumns()==4) ) {
                    String foreignRelationName = dtd.getValueString(c, "foreignRelationName");
                    String foreignColumnName   = dtd.getValueString(c, "foreignColumnName");
                    foreignRelationName = foreignRelationName.trim();
                    foreignColumnName   = foreignColumnName.trim();
                    if ( ! Defs.isNullString( foreignRelationName )) {
                        // Add a fkc for this column
                        if ( foreignColumnName.equalsIgnoreCase(DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN)) {
                            foreignColumnName = null; // indicating the physical column.
                        }
                        ForeignKeyConstr fkc = new ForeignKeyConstr(dbms,
                            name, columnName,
                            foreignRelationName, foreignColumnName);
                        if ( ! dbms.foreignKeyConstrSet.addForeignKeyConstr(fkc)) {
                            General.showError("Failed to add fkc: " + fkc);
                            General.showError("Dtd: " + dtd.toString());

                            return false;
                        }
                    }
                }
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        if ( dtd_file_name != null ) {
            if ( ! dbms.removeRelation(dtd)) {
                General.showError("Failed to remove dtd for relation: " + name);
                return false;
            }
        }

        return true;
    }


    /** Doesn't check for valid values. Just a convenience method not optimized for
     *speed.
     */
    public boolean copyValue(Relation other, int otherRid, String otherColumnName,
                                int toRid, String toColumnName) {
        Object value = other.getValue(otherRid, otherColumnName);
        if ( ! setValue(toRid, toColumnName, value)) {
            General.showError("Failed to setValue for relation: " + name +
                " column: " + otherColumnName + " and value: " + value);
            return false;
        }
        return true;
    }

    /** Actually inserts a real column with simply the indexes as elements.
     * Can be useful for instance for keeping track of order.
     */
    private boolean insertColumnPhysical(int idx, String colName ) {
        if ( ! insertColumn(idx, colName, DATA_TYPE_INT,null)) {
            General.showError("Failed to insertColumn for: " + colName );
            return false;
        }
        int[] physCol = getColumnInt( colName );
        if ( physCol == null ) {
            General.showError("Failed to getColumnInt for: " + colName );
            return false;
        }
        for (int r=0;r<sizeMax;r++) {
            physCol[r] = r;
        }
        return true;
    }

    /**
    Using order column when present.
     *using string values that can be read in by MySQL. E.g. for boolean use "F"
    */
    public String[][] toStringValues(
        boolean containsPhysicalColumn, boolean containsSelected,
        boolean containsOrder, boolean useActualNULL) {
        //General.showDebug("now in toStringValues");
        Relation tmpRelation = (Relation) Objects.deepCopy( this ); // expensive.
        tmpRelation.name = name + "_tmp";
        int rows = used.cardinality();

        if ( containsPhysicalColumn ) {
            String colName = name + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX;
            if ( ! tmpRelation.insertColumnPhysical(0, colName )) {
                General.showError("Failed to insertColumnPhysical for relation: " + colName);
                return null;
            }
        }
        int columns = 0;
        for (int c=0;c<tmpRelation.sizeColumns();c++) {
            String columnName = tmpRelation.getColumnLabel(c);
            if ( columnName.equals(DEFAULT_ATTRIBUTE_ORDER_ID)) {
                if ( ! containsOrder ) {
                    continue;
                }
            }
            if ( columnName.equals(DEFAULT_ATTRIBUTE_SELECTED)) {
                if ( ! containsSelected ) {
                    continue;
                }
            }
            if ( tmpRelation.getColumnDataType(columnName) != DATA_TYPE_STRING ) {
                //General.showDebug("translating native to String values for column: " + c);
                if ( ! tmpRelation.convertDataTypeColumn(columnName, DATA_TYPE_STRING, null )) {
                    General.showError("Can't do conversion");
                    return null;
                }
            }
            columns++;
        }
        String[][] result = new String[rows][columns];
        int[] map = null;
        // Use the original relation for the order map because the tmpRelation one is destroyed.
        if ( containsColumn(DEFAULT_ATTRIBUTE_ORDER_ID ) ) {
            map = getRowOrderMap( DEFAULT_ATTRIBUTE_ORDER_ID );
            if ( map == null ) {
                General.showError("Failed to convert the order column to an int[]");
                return null;
            }
        } else {
            map = PrimitiveArray.toIntArray( tmpRelation.used ); // use physical ordering in stead.
            if ( map == null ) {
                General.showError("Failed to convert the used BitSet to an int[]");
                return null;
            }
        }
        //General.showDebug("Map is: " + PrimitiveArray.toString(map));
        int c_actual = 0;
        for (int c=0;c<tmpRelation.sizeColumns();c++) {
            String columnName = tmpRelation.getColumnLabel(c);
            //General.showDebug("Working on column:" + columnName + " (" + c + ")");

            if ( columnName.equals(DEFAULT_ATTRIBUTE_ORDER_ID)) {
                if ( ! containsOrder ) {
                    continue;
                }
            }
            if ( columnName.equals(DEFAULT_ATTRIBUTE_SELECTED)) {
                if ( ! containsSelected ) {
                    continue;
                }
            }
            String[] column = tmpRelation.getColumnString(columnName);
            if ( column == null ) {
                General.showError("failed to get String[] column in tmpRelation for column: " + columnName + " (" + c + ")");
                return null;
            }
            try {
                // Switch outside the loop for speed.
                if ( ! useActualNULL ) {
                    for (int r=(rows-1);r>=0;r--) {
                        result[r][c_actual] = column[  map[r] ];
                    }
                } else {
                    String tmpStr = null;
                    for (int r=(rows-1);r>=0;r--) {
                        //General.showDebug("Using: result["+r+"]["+c+"] = column["+idx+"]");
                        tmpStr = column[  map[r] ];
                        if ( Defs.isNullString(tmpStr) ) {
                            //General.showOutput("For column: " + columnName + " substituted null value (\\N).");
                            tmpStr = DBMS.DEFAULT_SQL_NULL_STRING_REPRESENTATION;
                        }
                        result[r][c_actual] = tmpStr;
                    }
                }
            } catch ( Throwable t) {
                General.showError("In relation: " + name + " for column: " + columnName);
                General.showError("Map used: " + PrimitiveArray.toString(map));
                General.showThrowable(t);
                return null;
            }
            c_actual++;
        }
        // remove the relation from dbms so it can be gc-ed.
        dbms.removeRelation( tmpRelation );
        return result;
    }

    /** convenience method.
     */
    public boolean writeCsvFile( String file_name, boolean containsHeaderRow) {
        boolean containsPhysicalColumn = false;
        boolean containsSelected       = true;
        boolean containsOrder          = false;
        boolean useActualNULL          = false;
        return writeCsvFile( file_name,
            containsHeaderRow, containsPhysicalColumn,
            containsSelected,  containsOrder,
            useActualNULL);
    }

    /** Writes a csv file with a set of columns that can be influenced by the parameters given.
     */
    public boolean writeCsvFile( String file_name,
        boolean containsHeaderRow, boolean containsPhysicalColumn,
        boolean containsSelected,  boolean containsOrder,
        boolean useActualNULL) {
        try {
            FileWriter writer = new FileWriter(file_name);
            BufferedWriter bw = new BufferedWriter(writer);
            //ExcelCSVPrinter printer = new ExcelCSVPrinter(bw); Failed to be used because of speed.
            int columnCount = columnOrder.size();
            if ( containsHeaderRow ) {
                boolean firstColumnWritten = false;
                if ( containsPhysicalColumn ) {
                    bw.write(name + DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX+",");
                }
                for (int c=0;c<columnCount;c++) {
                    String columnName = getColumnLabel(c);
                    if ( columnName.equals(DEFAULT_ATTRIBUTE_ORDER_ID)) {
                        if ( ! containsOrder ) {
                            continue;
                        }
                    }
                    if ( columnName.equals(DEFAULT_ATTRIBUTE_SELECTED)) {
                        if ( ! containsSelected ) {
                            continue;
                        }
                    }
                    if ( firstColumnWritten ) {
                        bw.write(',');
                    } else {
                        firstColumnWritten = true;
                    }
                    bw.write(columnName);
                }
                bw.write( General.eol );
            }
            if ( used.cardinality() == 0 ) {
                General.showDebug("No rows in use for relation: " + name + " so no data written to csv file");
                bw.close();
                writer.close();
                return true;
            }
            String[][] values = toStringValues(
                containsPhysicalColumn, containsSelected,
                containsOrder,          useActualNULL );
            if ( values == null ) {
                bw.close();
                writer.close();
                General.showError("Failed to transfer all data in relation to String[][] values");
                return false;
            }
            //General.showDebug("Changing null values to empty strings so the writer can deal with these values");
            Strings.changeNullsToEmpties( values );

            //General.showDebug("Reformat to csv.");
            String str = Strings.toCsv(values);
            if ( str == null ) {
                General.showDebug("failed to convert to csv format");
                bw.close();
                writer.close();
                return false;
            }
            //General.showDebug("Starting to write to file: " + file_name );
            bw.write(str);


            //General.showDebug("Written header: " + containsHeaderRow);
            //General.showDebug("Written number of records: " + used.cardinality() +
            //" for number of columns (excluding possible header): " + columnCount);
            bw.close();
            writer.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }

    /** Materializes some strings in the given array. Array MUST exist and will be nilled by
     this method. If an empty rids list is given then the result will be an empty list.
     */
    public boolean getValueStringArray( ArrayList result, IntArrayList rids, String label) {
        if ( result == null ) {
            General.showError("In getValueStringArray result should not be null.");
            General.showError("In relation: " + name);
            return false;
        }
        if ( ! containsColumn(label)) {
            General.showError("Can't getValueStringArray for unexisting column with label: " + label);
            General.showError("In relation: " + name);
            return false;
        }
        if ( !(getColumnDataType(label) == DATA_TYPE_STRINGNR || // most common one first in check.
               getColumnDataType(label) == DATA_TYPE_STRING ) ) {
            General.showError("Can't getValueStringArray for non string datatype of column with label" + label);
            return false;
        }
        result.clear();
        int rids_size = rids.size();
        if (rids_size==0) {
            return true;
        }
        result.ensureCapacity(rids_size);
        for (int i=0; i<rids_size; i++)  {
            String value = getValueString(rids.getQuick(i), label);
            result.add( value );
        }
        return true;
    }

    /** Very little checking done */
    public int getIntSum(String label) {
        int[] columFlat = getFlatListInt(label);
        if ( columFlat == null ) {
            return -1;
        }
        int result = PrimitiveArray.getSum(columFlat);
        return result;
    }

    /** Scans the relation for rows that have identical values for all given
     *columns and deletes (as the final step) these rows.
     *Only works on columns with data type String and int.
     *Check algorithm for easy extensions. The algorithm isn't
     *100% garanteed as the row is hashed by the comma
     *separated string representation of the individual values.
     */
    public boolean removeDuplicates( String[] columnNames) {
        Object[] columnList = new Object[columnNames.length];
        BitSet duplicates = new BitSet();
        for (int i=0;i<columnNames.length;i++) {
            String label = columnNames[i];
            int dataType = getColumnDataType(label);
            if ( dataType != DATA_TYPE_INT && dataType != DATA_TYPE_STRINGNR) {
                General.showError("Failed to removeDuplicates for relation: " + name +
                    " and non int or string column: " + label );
                return false;
            }
            columnList[i] = getColumn(label);
            if ( columnList[i] == null ) {
                General.showError("Failed to get column for relation: " + name +
                    " and column: " + label );
                return false;
            }
        }
        HashMap prev = new HashMap();
        StringBuffer value = new StringBuffer();

        Object columnObject;
        String key;
        for (int rid=used.nextSetBit(0);rid>=0;rid=used.nextSetBit(rid+1)) {
            value.setLength(0);
            for (int i=0;i<columnNames.length;i++) {
                value.append(',');
                columnObject = columnList[i];
                if ( columnObject instanceof int[] ) {
                    value.append(((int[]) columnObject)[rid]);
                } else if ( columnObject instanceof String[] ) {
                    value.append(((String[]) columnObject)[rid]);
                }
            }
            key = value.toString();

            if ( prev.containsKey(key) ) {
                //General.showDebug("Found duplicate: " + key);
                duplicates.set(rid);
            } else {
                //General.showDebug("Found new value: " + key);
                prev.put(key, null);
            }
        }
        //General.showDebug("Removing duplicates numbered: " + duplicates.cardinality());
        if ( ! removeRows(duplicates, true, true )) {
            General.showError("Failed to Removing duplicates for relation: " + name +
                " and columns: " + Strings.toString( columnNames ));
            return false;
        }
        return true;
    }
}


