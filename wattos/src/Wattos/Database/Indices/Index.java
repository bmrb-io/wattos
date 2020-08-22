/*
 * Index.java
 *
 * Created on June 18, 2003, 9:53 AM
 */

package Wattos.Database.Indices;

import java.io.*;
import java.util.*;
import Wattos.Database.*;
import Wattos.Utils.*;

/**
 * An index can be 1 to 1 or 1 to many. The key can be of any type: e.g. int, float, String, Object.
 * The record identifier (abbreviated as rid) can only be of type int.
 * <P>
 * The indexes are implemented as collections of native types so they should be very
 * fast and cheap on memory. There are hashed and sorted indexes of bit, int, short,
 * float, String, and other objects.
 * @author Jurgen F. Doreleijers
 */
public class Index implements Serializable  {
    
    private static final long serialVersionUID = -1207795172754062330L;    
    
    public static final int INDEX_TYPE_INVALID      = -1;
    public static final int INDEX_TYPE_HASHED       = 0; // Hard-coded to be the first one! Don't assign a different value
    public static final int INDEX_TYPE_SORTED       = 1; // Play away with the other ones.
    public static final int INDEX_TYPE_DEFAULT      = INDEX_TYPE_HASHED;
    public static final String[] index_type_names = { "INDEX_TYPE_HASHED", "INDEX_TYPE_SORTED" };
    public static final int INDEX_TYPE_COUNT        = index_type_names.length;
    
    
    
    public static final int LIST_TYPE_INVALID           = 0;
    public static final int LIST_TYPE_BITSET            = 1;
    public static final int LIST_TYPE_INT_ARRAY_LIST    = 2;
    public static final int LIST_TYPE_DEFAULT           = 3;
    
    public static final String[] list_type_names = {
        "LIST_TYPE_INVALID", "LIST_TYPE_BITSET", "LIST_TYPE_INT_ARRAY_LIST", "LIST_TYPE_DEFAULT" };
    
    public int     indexType       = INDEX_TYPE_DEFAULT;

    /** Only used in the sorted indexes */
    public int[] rids     = null;
    
    public String toString() {
        return toString( false, false );
    }

    public String toString(boolean show_elements, boolean show_keys ) {
        General.showError("Call to toString in Index needs to be overwritten; code bug");
        return null;
    }
    
    public static boolean isValidIndexType( int index_type ) {
        if ( index_type < 0 || index_type >= INDEX_TYPE_COUNT ) {
            return false;
        }
        return true;
    }
    
    public static void showInvalidIndexType( int index_type ) {
        if ( ! isValidIndexType( index_type ) ) {
            return;
        }
        General.showError("Invalid type is numbered: " + index_type );
        General.showError("Allowed types are named in order: " + Strings.toString( index_type_names ));
    }
    
    public boolean updateIndex(Relation relation, String columnLabel) {
        General.showError("Call to updateIndex(Relation relation, String columnLabel) in Index needs to be overwritten; code bug");
        return false;
    }
    
    public boolean updateIndex(Relation relation, String columnLabel_Int, String columnLabel_String) {
        General.showError("Call to updateIndex(Relation relation, String columnLabel_Int, String columnLabel_String) in Index needs to be overwritten; code bug");
        return false;
    }
    
    public int getRid( Object o ) {
        General.showError("Call to getRid in Index needs to be overwritten; code bug");
        General.showError("Index: " + this);
        return -1;
    }

    public Object getRidList( BitSet o ) {
        General.showError("Call to getRidList( BitSet o ) in Index needs to be overwritten; code bug");
        return null;
    }

    public Object getRidList( Object o, boolean distinct, int listType ) {
        General.showError("Call to getRidList in Index needs to be overwritten; code bug");
        return null;
    }
   
    public Object getRidList( Object o, int listType, Object result) {
        General.showError("Call to getRidList in Index needs to be overwritten; code bug");
        return null;
    }
    
    /** This Method should implement a structured query like:
     *SELECT [DISTINCT] * FROM this_table
     *WHERE this_column >= 7.3
     *In this example the following arguments need to be given:
     *operation: OPERATION_TYPE_GREATER_THAN_OR_EQUAL
     *threshold: 7.3 encapsulated in a Float object
     *For string operations sorted lists are less useful in general
     *but still possible with this method signature using operation type:
     *OPERATION_TYPE_EQUALS_REGULAR_EXPRESSION.
     */
    public BitSet getRidListForQuery( int operator, Object value ) {
        General.showError("Call to getRidListForQuery in Index needs to be overwritten; code bug");
        General.showError("Called for index: " + this.toString(false,false));
        return null;
    }
    
    /** Based on the index return distinct elements in the selection (not destroying 
     *the selection.
     *Example:<PRE>T is for true. Assumed is the data type of integers.
*Rid    Val     Sel     Unique -> (0,1,3,4,5)
*0      1       T       T
*1      4       T       T
*2      2              
*3      3              
*4      5       T       T
*5      0              
*6      3       T       
*7      0       T       T
*

*Index
Val     Rid
0       5
0       7
1       0
2       2
3       3
3       6
4       1
5       4
     *</PRE>
     *The algorithm scans (from end for speed) the sorted values in the index. It's reasonably 
     *fast without generation of many objects. Doesn't return nulls if only present as
     *the very last value in the sorted order.
     *
     *Use the method of the same name in the SQLSelect class for extra checking/wrapping.
     */
    public BitSet getDistinct( BitSet selected ) {
        General.showError("Call to getDistinctSorted in Index needs to be overwritten; code bug");
        return null;
    }    
}
