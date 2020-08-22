package Wattos.Database.Indices;

/*
 * IndexStringToMany.java
 *
 * Created on June 18, 2003, 11:02 AM
 */

import Wattos.Utils.Comparators.ComparatorFloatIntPair;
import Wattos.Utils.ObjectIntPair;
import java.io.*;
import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Database.*;
/**
 * @author Jurgen F. Doreleijers
 */
public class IndexSortedFloat extends Index implements Serializable {
 
    private static final long serialVersionUID = -1207795172754062330L;        
    
    float[] values = null;
    
    /** Creates a new instance of IndexStringToMany */
    public IndexSortedFloat() {
        indexType = INDEX_TYPE_SORTED;
    }
        
    
    public String toString(boolean show_elements, boolean show_keys ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "Number of elements is: ");
        sb.append( values.length );
        if ( show_elements ) {
            sb.append( General.eol);
            for ( int r=0; r<values.length; r++ ) {
                sb.append( values[r] );
                sb.append( ' ' );
                sb.append( rids[r] );
                sb.append( General.eol );
            }            
        }
        return sb.toString();
    }
        
        
    public boolean updateIndex(Relation relation, String columnLabel) { 
        Object object = relation.getColumn( columnLabel );
        if ( ! (object instanceof float[]) ) {
            General.showError( "Object should be of type float[] but found: " + object.getClass().getName());
            return false;
        }
        float[] column = (float[]) object;
    
        int sizeRows = relation.sizeRows;
        
        /** Approach may be optimized by not using objects but right now, no
         * time to code it
         */
        // Fill arrays
        ArrayList list = new ArrayList( sizeRows ); /** Use capacity size for efficiency */        

        BitSet used = relation.used;
        for (int r=used.nextSetBit(0); r>=0; r=used.nextSetBit(r+1)) {

            Float f = new Float( column[r] );
            if ( f == null ) {
                General.showError("Code bug 1 found in updateIndex.");
            }
            //General.showDebug("found float:" + f);
            
            ObjectIntPair pair = new ObjectIntPair( f, r );
            if ( pair == null ) {
                General.showError("Code bug 2 found in updateIndex.");
            }
                
            list.add( pair );
        }
        if ( list.size() != sizeRows ) {
            General.showError("Code bug found in updateIndex sizes don't match.");
            General.showError("rows in relation found are: " + sizeRows + " and: " + list.size() );
            return false;
        }
        
        // Use a fast comparator
        Comparator comparator = new ComparatorFloatIntPair();
        // Sorts on values column only.
        try {
            Collections.sort(list, comparator);
        } catch ( ClassCastException e ) {
            General.showThrowable(e);
            return false;
        }
        
        // Transfer back to original 
        values = new float[sizeRows];
        rids     = new int[sizeRows];
        
        for ( int r=sizeRows-1; r > -1; r-- ) {
            ObjectIntPair pair = (ObjectIntPair) list.get(r);
            values[r]   = ((Float) pair.o).floatValue();
            rids[r]     = pair.i;
        }
            
        return true;
    }

    /** 
     *Returns null if the key is not exactly met.
     *Returns null if the list is not present or not sorted before.
     */
    public Object getRidList( Object o, int listType) {
        if ( values == null ) {            
            General.showError("Code bug found in getRidList no values present in index: "+ Strings.getClassName(this));
            return null;
        }
        float key = ((Float) o).floatValue();
        int idxFirst = Arrays.binarySearch(values, key);
        if ( idxFirst < 0 ) {
            //General.showDebug("no record with value: " + key);
            return null;
        }
        //General.showDebug("found first idx: " + idxFirst);
        
        // Scan backward
        int idx = idxFirst;
        int idxBegin = -1;            
        while ( idx > -1 ) {
            idxBegin = idx;
            //General.showDebug("found begin idx: " + idx);
            idx--;
            if ( idx < 0 || values[idx] != key ) {
                idx = -1;
            }
        }
        // Scan forward
        idx = idxFirst;
        int idxEnd = -1;            
        while ( idx > -1 ) {
            idxEnd = idx;
            //General.showDebug("found end idx: " + idx);
            idx++;
            if ( idx >= values.length || values[idx] != key ) {
                idx = -1;
            }
        }
        
        if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
            int[] elements = new int[idxEnd-idxBegin+1];
            int j = 0;
            for (int i=idxBegin;i<=idxEnd;i++ ) {                                
                elements[j] = rids[i];
                j++;
            }
            IntArrayList list = new IntArrayList();
            list.elements(elements);
            return list;
        }
        
        if ( listType == Index.LIST_TYPE_BITSET ) {
            BitSet list = new BitSet();
            // Only set those that need setting.
            for (int i=idxBegin;i<=idxEnd;i++ ) {                
                list.set( rids[i] );
            }
            return list;
        }
         
        General.showError("list type requested is not supported: " + Index.list_type_names[listType] );
        return null;
    }
    
    /** 
     * @param o Value to look for. Needs to be an encapsulated data type like Integer for int.
     * @param listType Selects the output object as a BitSet, int[], or so.
     * @param result shoulbe be null
     * @return empty result is possible.
     * null in case of error: if the list is not present or not sorted before.
     */
    private Object getRidList( Object o, int listType, Object result, int operator ) {
        
        if (!(  
                operator == SQLSelect.OPERATION_TYPE_EQUALS ||
                operator == SQLSelect.OPERATION_TYPE_GREATER_THAN ||
                operator == SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL ||
                operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN ||
                operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN_OR_EQUAL 
                )) {
            General.showCodeBug("invalid operator in IndexSortedFloat.getRidList: " + SQLSelect.operation_type_names[operator]);
            return null;                        
        }
        if ( result != null ) {
            General.showError("There seems to be a problem with recycling the result object when an empty list is to be returned");
            return null;            
        }
        
        if ( values == null ) {            
            General.showCodeBug("in getRidList no values present in index: "+ Strings.getClassName(this));
            return null;
        }

        // Test data type requested versus given.
        if ( result != null ) {
            if ( ( listType == Index.LIST_TYPE_BITSET ) && !(result instanceof BitSet ) ) {
                General.showError("In IndexSortedInt old instance of result isn't an instance of type BitSet");
                return null;
            } else if ( (listType == Index.LIST_TYPE_INT_ARRAY_LIST ) && !(result instanceof IntArrayList ) ) {
                General.showError("In IndexSortedInt old instance of result isn't an instance of type IntArrayList");
                return null;
            }
        }
        
        float key = ((Float) o).floatValue();
//        General.showDebug("Looking for key: " + key);
        int idxFirst = Arrays.binarySearch(values, key);
//        General.showDebug("Arrays.binarySearch(values, key) gave: " + idxFirst);
        
        int idxBegin = -1;            
        int idxEnd = -1;            
        if ( idxFirst >= 0 ) {
//            General.showDebug("Found first idx: " + idxFirst);

            // Scan backward
            int idx = idxFirst;
            while ( idx > -1 ) {
                idxBegin = idx;
//                General.showDebug("found begin idx: " + idx);
                idx--;
                if ( idx < 0 || values[idx] != key ) {
                    idx = -1;
                }
            }
            // Scan forward
            idx = idxFirst;
            while ( idx > -1 ) {
                idxEnd = idx;
//                General.showDebug("found end idx: " + idx);
                idx++;
                if ( idx >= values.length || values[idx] != key ) {
                    idx = -1;
                }
            }
            // eg searching for key 2.0 we would have found 
            // idxBegin: 2
            // idxEnd  : 3 (inclusive)        
    //        [0.0,1.0,2.0,2.0,3.0,4.0]
    //         0   1   2   3   4   5
            if ( operator == SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL) {
                idxEnd = values.length - 1;
            } else if ( operator == SQLSelect.OPERATION_TYPE_GREATER_THAN ) {
                idxBegin = idxEnd+1;
                if ( idxBegin >= values.length ) {
                    idxFirst = -1; // indication of empty result
                }
                idxEnd   = values.length - 1;            
            } else if ( operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN ) {
                idxEnd   = idxBegin-1;  
                if ( idxEnd < 0 ) {
                    idxFirst = -1; // indication of empty result
                }
                idxBegin = 0;
            } else if ( operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN_OR_EQUAL ) {
                idxBegin = 0;
            } else if ( operator == SQLSelect.OPERATION_TYPE_EQUALS) {
                ;// done
            }
        } else { // exact value was not found as will often be the case with floats.
            // returned (-(<i>insertion point</i>) - 1)
            int idxInsertionPoint = -1 - idxFirst;
            idxFirst = idxInsertionPoint; // reset it because it will be checked for a negative value below to indicate empty result.
//            General.showDebug("idxInsertionPoint is: " + idxInsertionPoint);
            if ( operator == SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL ||
                 operator == SQLSelect.OPERATION_TYPE_GREATER_THAN
                    ) {
                idxBegin = idxInsertionPoint;
                idxEnd = values.length - 1;
            } else if ( operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN ||
                        operator == SQLSelect.OPERATION_TYPE_SMALLER_THAN_OR_EQUAL) {
                idxBegin = 0;
                idxEnd = idxInsertionPoint;
            } else if ( operator == SQLSelect.OPERATION_TYPE_EQUALS ) {
                idxFirst = -1;// indication of empty result
            }     
            
            if ( idxBegin >= values.length) {
                idxFirst = -1;// indication of empty result
            }
            if ( idxEnd < 0 ) { // Happens when insertion point = 0 and smaller than operation is selected.
                idxFirst = -1;// indication of empty result
            }
        }
//        General.showDebug("idxBegin: "+idxBegin);
//        General.showDebug("idxEnd  : "+idxEnd);
//        General.showDebug("idxEnd  after looking for nulls.: "+idxEnd);
        if ( idxFirst >= 0 ) {
            while ( idxEnd >=0 && Defs.isNull( values[idxEnd]) ) {
                idxEnd--;
            }
        }
        int newSize = idxEnd-idxBegin+1; // can be negative too.
        if ( newSize <= 0 ) {
            idxFirst = -1;// indication of empty result
        }
        if ( idxFirst < 0 ) {
//            General.showDebug("Returning empty rid list as no records for query with key: " + key);
            if ( listType == Index.LIST_TYPE_BITSET ) {
                return new BitSet();
            } else if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
                return new IntArrayList();
            }
        }

        
        if ( newSize > values.length ) {
            General.showCodeBug("Found result ["+newSize+"] to be larger than possible ["+values.length +"]");
            return null;
        }
        
        if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
            IntArrayList    resultList;
            if ( result == null ) {
                resultList      = new IntArrayList();
                int[] elements = new int[newSize];
                int j = 0;
                for (int i=idxBegin;i<=idxEnd;i++ ) {                                
                    elements[j++] = rids[i];
                }
                resultList.elements(elements);
            } else {
                resultList = (IntArrayList) result;
                //resultList.clear();                 not needed because it will be overwritten and trimmed anyway.
                resultList.ensureCapacity( newSize );       // involves a System.arraycopy when resize is really needed.
                int[] elements = resultList.elements();     // just returning ref to container.
                int j = 0;
                for (int i=idxBegin;i<=idxEnd;i++ ) {                                
                    elements[j++] = rids[i];
                }
                resultList.elements( elements );            // just sets container ref to variable
                resultList.setSize(newSize);                // just changes 1 instance variable because previous method takes the size from the elements array.
            }
            return resultList;
        } else if ( listType == Index.LIST_TYPE_BITSET ) {            
            BitSet    resultBitSet;
            if ( result == null ) {
                resultBitSet = new BitSet( newSize );
            } else {
                resultBitSet = (BitSet) result;
                resultBitSet.clear();
            }
            for (int i=idxBegin;i<=idxEnd;i++ ) {                
                resultBitSet.set( rids[i] );
            }
            return resultBitSet;
        }
         
        General.showError("list type requested is not supported: " + Index.list_type_names[listType] );
        return null;
    }

    /** See super method    */
    public BitSet getRidListForQuery( int operator, Object value ) {
        switch ( operator ) {
            case SQLSelect.OPERATION_TYPE_EQUALS: {
                return (BitSet) getRidList( value, Index.LIST_TYPE_BITSET, null, operator);
            }
            case SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL: {
                return (BitSet) getRidList( value, Index.LIST_TYPE_BITSET, null, operator);
            }
            case SQLSelect.OPERATION_TYPE_GREATER_THAN: {
                return (BitSet) getRidList( value, Index.LIST_TYPE_BITSET, null, operator);
            }
            case SQLSelect.OPERATION_TYPE_SMALLER_THAN: {
                return (BitSet) getRidList( value, Index.LIST_TYPE_BITSET, null, operator);
            }
            case SQLSelect.OPERATION_TYPE_SMALLER_THAN_OR_EQUAL: {
                return (BitSet) getRidList( value, Index.LIST_TYPE_BITSET, null, operator);
            }
            default: {
                General.showCodeBug("Operation not supported in getRidListForQuery: " + SQLSelect.operation_type_names[ operator ]);
                return null;
            }
        }
    }

    public float[] getValues() {
        return values;
    }

    public int[] getRids() {
        return rids;
    }
    
}
