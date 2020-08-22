package Wattos.Database.Indices;

/*
 * IndexStringToMany.java
 *
 * Created on June 18, 2003, 11:02 AM
 */

import Wattos.Utils.Comparators.ComparatorStringIntPair;
import Wattos.Utils.ObjectIntPair;
import java.io.*;
import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Database.*;
/**
 * @author Jurgen F. Doreleijers
 */
public class IndexSortedString extends Index implements Serializable {
 
    private static final long serialVersionUID = -1207795172754062330L;        
    
    String[] values = null;
//    public int[] rids      = null;
    
    /** Creates a new instance of IndexStringToMany */
    public IndexSortedString() {
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
        if ( ! (object instanceof String[]) ) {
            General.showError( "Object should be of type String[] but found: " + object.getClass().getName());
            return false;
        }
        String[] column = (String[]) object;
    
        int sizeRows = relation.sizeRows;
        
        /** Approach may be optimized by not using objects but right now, no
         * time to code it
         */
        // Fill arrays
        ArrayList list = new ArrayList( sizeRows ); /** Use capacity size for efficiency */        

        BitSet used = relation.used;
        for (int r=used.nextSetBit(0); r>=0; r=used.nextSetBit(r+1)) {

            String f = column[r];            
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
        Comparator comparator = new ComparatorStringIntPair();
        // Sorts on values column only.
        try {
            Collections.sort(list, comparator);
        } catch ( ClassCastException e ) {
            General.showThrowable(e);
            return false;
        }
        
        // Transfer back to original 
        values   = new String[sizeRows];
        rids     = new int[sizeRows];
        
        for ( int r=sizeRows-1; r > -1; r-- ) {
            ObjectIntPair pair = (ObjectIntPair) list.get(r);
            values[r]   = (String) pair.o;
            rids[r]     = pair.i;
        }
            
        return true;
    }

    /** 
     *Returns empty result if the key is not exactly met. This way the object 
     *result can be maintained.
     *Returns null if the list is not present or not sorted before.
     */
    public Object getRidList( Object o, int listType, Object result) {
        if ( result != null ) {
            General.showDebug("There seems to be a problem with recycling the result object when an empty list is to be returned");
            return null;            
        }

        if ( values == null ) {            
            General.showError("Code bug found in getRidList no values present in index: "+ Strings.getClassName(this));
            return null;
        }
        
        // Test data type requested versus given.
        if ( result != null ) {
            if ( ( listType == Index.LIST_TYPE_BITSET ) && !(result instanceof BitSet ) ) {
                General.showError("In IndexSortedString old instance of result isn't an instance of type BitSet");
                return null;
            } else if ( (listType == Index.LIST_TYPE_INT_ARRAY_LIST ) && !(result instanceof IntArrayList ) ) {
                General.showError("In IndexSortedString old instance of result isn't an instance of type IntArrayList");
                return null;
            }
        }
        
        String key = (String) o;
        //General.showOutput("Looking in values: " + PrimitiveArray.toString( values));
        //General.showOutput("for key: " + key);
        int idxFirst = Arrays.binarySearch(values, key);
        if ( idxFirst < 0 ) {
            //General.showOutput("no record with value: " + key);
            // Still preserve old result object, just null it.
            if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
                IntArrayList resultList;
                if ( result == null ) {
                    resultList = new IntArrayList();
                } else {
                    resultList = (IntArrayList) result;
                }
                resultList.setSize(0);
                return resultList;
            } else if ( listType == Index.LIST_TYPE_BITSET ) { 
                BitSet    resultBitSet;
                if ( result == null ) {
                    resultBitSet = new BitSet( 0 );
                } else {
                    resultBitSet = (BitSet) result;
                    resultBitSet.clear();
                }
                return resultBitSet;
            } 

            General.showError("list type requested is not supported: " + Index.list_type_names[listType] );
            return null;                
        }
        //General.showOutput("found first idx: " + idxFirst);
        
        // Scan backward
        int idx = idxFirst;
        int idxBegin = -1;            
        while ( idx > -1 ) {
            idxBegin = idx;
            //General.showOutput("found begin idx: " + idx);
            idx--;
            if ( idx < 0 || (!values[idx].equals( key ))) {
                idx = -1;
            }
        }
        // Scan forward
        idx = idxFirst;
        int idxEnd = -1;            
        while ( idx > -1 ) {
            idxEnd = idx;
            //General.showOutput("found end idx: " + idx);
            idx++;
            if ( idx >= values.length || (!values[idx].equals( key )) ) {
                idx = -1;
            }
        }
        //General.showOutput("idxEnd: " + idxEnd + " idxBegin: " + idxBegin);
        int newSize = idxEnd-idxBegin+1;
        //General.showOutput("newSize: " + newSize);
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
                return (BitSet) getRidList( value, LIST_TYPE_BITSET, null);
            }
            default: {
                General.showCodeBug("Operation not supported in getRidListForQuery: " + SQLSelect.operation_type_names[ operator ]);
                return null;
            }
        }
    }  
    
    /** 
     *Returns null if the key is not exactly met.
     *Returns null if the list is not present or not sorted before.
     *Returns only 1 rid.
     */
    public int getRid( Object o ) {
        return Arrays.binarySearch(values, (String) o); // inline if needed;-)
    }
    
    /** See super method  
     *Note that equals method needs to be used because strings can be redundant.  
     */
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
        String v;
        int ri;
        String v_prev = Defs.NULL_STRING_NULL;
        for ( int r=values.length-1; r > -1; r-- ) {
            v = values[r];
            ri = rids[r];
            if ( ( ! v.equals(v_prev) ) && selected.get(ri) ) {
                v_prev = v;
                //General.showDebug("Found a new value: " + v + " for rid: " + ri);
                result.set(ri);
            }
        }   
        return result;
    }    
    
    
}
