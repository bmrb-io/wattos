package Wattos.Database.Indices;

/*
 * IndexStringToMany.java
 *
 * Created on June 18, 2003, 11:02 AM
 */

import Wattos.Utils.Comparators.ComparatorIntIntPair;
import Wattos.Utils.ObjectIntPair;
import java.io.*;
import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Database.*;
/**
 * @author Jurgen F. Doreleijers
 */
public class IndexSortedInt extends Index implements Serializable {
 
    private static final long serialVersionUID = -1207795172754062330L;            

    /** Sorted list of values with the smallest value at index 0 */
    public int[] values = null;
    /** Parallel array to the values array with the record ids. Only rids of 
     used records are included.*/
//    public int[] rids   = null;

    private BitSet bs = new BitSet();
    private BitSet bs_2 = new BitSet();
    
    /** Creates a new instance of IndexStringToMany */
    public IndexSortedInt() {
        indexType = INDEX_TYPE_SORTED;
    }
        
    
    /**
     * @param show_elements
     * @param show_keys
     */    
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
        if ( ! relation.containsColumn(columnLabel)) {
            General.showError( "Can't update index for nonexisting column: " + columnLabel);
            return false;
        }
        Object object = relation.getColumn( columnLabel );
        if ( ! (object instanceof int[]) ) {
            if ( object == null ) {
                General.showError( "Object should be of type int[] but found: " + null);
                return false;
            }
            General.showError( "Object should be of type int[] but found: " + object.getClass().getName());
            return false;
        }
        int[] column = (int[]) object;
    
        int sizeRows = relation.sizeRows;
        
        /** Approach may be optimized by not using objects but right now, no
         *time to code it.
         */
        // Fill arrays
        ArrayList list = new ArrayList( sizeRows ); /** Use capacity size for efficiency */        

        BitSet used = relation.used;
        for (int r=used.nextSetBit(0); r>=0; r=used.nextSetBit(r+1)) {

            Integer v = new Integer( column[r] );
            //General.showDebug("in updateIndex found at row: " + r + " an integer:" + v);
            
            ObjectIntPair pair = new ObjectIntPair( v, r );
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
        Comparator comparator = new ComparatorIntIntPair();
        try {
            // Sorts on values column only.
            Collections.sort(list, comparator );
        } catch ( ClassCastException e ) {
            General.showThrowable(e);
            return false;
        }
        
        // Transfer back to original 
        values = new int[sizeRows];
        rids   = new int[sizeRows];
        
        for ( int r=sizeRows-1; r > -1; r-- ) {
            ObjectIntPair pair = (ObjectIntPair) list.get(r);
            values[r]   = ((Integer) pair.o).intValue();
            rids[r]     = pair.i;
        }            
        return true;
    }

    public Object getRidList( Object o, int listType) {
        return getRidList( o, listType, null);
    }
    /** 
     * @param o Value to look for. Needs to be an encapsulated data type like Integer for int.
     * @param listType Selects the output object as a BitSet, int[], or so.
     * @param result If null then a new result object will be created. If not null the
     *object type should match the requested object type. The elements of the result object
     *will overwritten only for the rid that are found. 
     *Calling method doesn't needs to clear the result object.
     *
     * @return empty result if the key is not exactly met.
     * null in case of error: if the list is not present or not sorted before.
     */
    public Object getRidList( Object o, int listType, Object result) {
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
        
        int key = ((Integer) o).intValue();
        int idxFirst = Arrays.binarySearch(values, key);
        if ( idxFirst < 0 ) {
            if ( result == null ) {
                //General.showDebug("Returning empty rid list as no records for key: " + key);
                if ( listType == Index.LIST_TYPE_BITSET ) {
                    return new BitSet();
                } else if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
                    return new IntArrayList();
                }
            } else {
                if ( listType == Index.LIST_TYPE_BITSET ) {
                    BitSet resultSet = (BitSet) result;
                    //resultSet.clear(0,resultSet.size()); too expensive
                    return resultSet;
                } else if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
                    IntArrayList resultList = (IntArrayList) result;
                    //resultList.clear(); too expensive
                    return resultList;
                }
            }                
        }
        //General.showDebug("Found first idx: " + idxFirst);
        
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
        
        int newSize = idxEnd-idxBegin+1;
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

    /** Very fast since they're sorted.
     *Returns Defs.NULL_INT if values are not initialized.
     */
    public int getRidValueMinInt() {
        if ( values == null ) {            
            General.showError("Code bug found in getRidValueMinInt no values present in index: "+ Strings.getClassName(this));
            return Defs.NULL_INT;
        }              
        /**
        General.showDebug("List of values is: " + PrimitiveArray.toString( values ));
        General.showDebug("List of rid is: " + PrimitiveArray.toString( rids));        
         */
        return rids[ 0 ];
    }
    
    /** Very fast since they're sorted.
     *Returns Defs.NULL_INT if values are not initialized.
     */
    public int getRidValueMaxInt() {
        if ( values == null ) {            
            General.showError("Code bug found in getRidValueMaxInt no values present in index: "+ Strings.getClassName(this));
            return Defs.NULL_INT;
        }                
        return rids[ values.length-1 ];
    }
    
    /** 
     * @param searchKeys A BitSet used as value where the position in the list of the set bits are interpretted as the 
     *int values for which the column needs to be searched and the row ids to be returned for. It will nest 
     *calls to getRidList for individual values (and ranges later on for speed).
     *<P>The input parameter searchKeys is not changed.
     *<P>
     * @return null if the key is not exactly met.
     * null if the list is not present or not sorted before.
     */
    public Object getRidList( BitSet searchKeys ) {
        if ( values == null ) {            
            General.showError("Code bug found in getRidList no values present in index: "+ Strings.getClassName(this));
            return null;
        }        
        
        BitSet result = new BitSet( values.length );
        int i = searchKeys.nextSetBit(0);
        // This loop needs to be replaced by a more sophisticated algorithm
        // that is more efficient for selections on ranges like {10..20} which
        // require only 1 lookup/call to getRidList i.s.o. 11 or so.
        while ( i > -1 ) {
            //General.showDebug("checking in child for parent id: " + i);
            BitSet temp = (BitSet) getRidList( new Integer(i), LIST_TYPE_BITSET );
            // Make sure the result wasn't empty in which case the function returned null.
            if ( temp != null ) {
                result.or( temp ); // Great function; it iterates over longs that have a native function |=
            }
            i++;
            i = searchKeys.nextSetBit(i);
        }
        return result;
    }
    
    
    /** See super method    */
    public BitSet getRidListForQuery( int operator, Object value ) {
        switch ( operator ) {
            case SQLSelect.OPERATION_TYPE_EQUALS: {
                return (BitSet) getRidList( value, LIST_TYPE_BITSET);
            }
            default: {
                General.showCodeBug("Operation not supported in getRidListForQuery: " + SQLSelect.operation_type_names[ operator ]);
                return null;
            }
        }
    }  
    
    /** Returns the first rid for the value or -1 to indicate it's absence */
    public int getRid(int key) {        
        return Arrays.binarySearch(values, key); // inline if needed;-)
    }    
    
    /** Returns first rid for which condition is met.
     *Returns Defs.NULL_INT on error or -1 if condition isn't met.
     */
    public int getRid(int key, IndexSortedString other, String keyStr) {   
        BitSet result = getRidBitSet(key, other, keyStr);    
        if ( result == null ) {
            return Defs.NULL_INT;
        }
        return result.nextSetBit(0);
    }    

    /** Returns all rids for which condition is met. The method recycles any
     *bitsets reused so modifications persist. Returns null on error.
     */
    public BitSet getRidBitSet(int key, IndexSortedString other, String keyStr) {   
        //Using a recycled bs object in the future if possible.
        //bs.clear();
        bs = (BitSet) getRidList(new Integer(key),LIST_TYPE_BITSET,null);
        //General.showDebug("In getRidBitSet found result on int: " + PrimitiveArray.toString(bs));
        //General.showDebug("In getRidBitSet found this index: " + this.toString(true,true));
        if ( bs == null ) {
            General.showError("Failed to get rid list of first index" + this);
            return null;
        }
        bs_2.clear();
        bs_2 = (BitSet) other.getRidList(keyStr,LIST_TYPE_BITSET,null);
        //General.showDebug("In getRidBitSet found result on String: " + PrimitiveArray.toString(bs_2));
        //General.showDebug("In getRidBitSet found other index: " + other.toString(true,true));
        if ( bs_2 == null ) {
            General.showError("Failed to get rid list of second index" + other);
            return null;
        }
        bs.and(bs_2);
        //General.showDebug("In getRidBitSet found result: " + PrimitiveArray.toString(bs));
        return bs;
    }

    
    /** See super method    */
    public BitSet getDistinct( BitSet selected ) {
        BitSet result = new BitSet();
        if ( values == null ) {            
            General.showCodeBug("in getDistinct no values present in index: "+ Strings.getClassName(this));
            return null;
        }
        if ( rids == null ) {            
            General.showCodeBug("in getDistinct no rids present in index: "+ Strings.getClassName(this));
            return null;
        }
        int v;
        int ri;
        int v_prev = Defs.NULL_INT;
        for ( int r=values.length-1; r > -1; r-- ) {
            v = values[r];
            ri = rids[r];
            if ( ( v != v_prev ) && selected.get(ri) ) {
                v_prev = v;
                //General.showDebug("Found a new value: " + v + " for rid: " + ri);
                result.set(ri);
            }
        }   
        return result;
    }    
}
