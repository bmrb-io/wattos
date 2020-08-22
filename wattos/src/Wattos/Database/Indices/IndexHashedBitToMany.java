/*
 * IndexStringToMany.java
 *
 * Created on June 18, 2003, 11:02 AM
 */

package Wattos.Database.Indices;


import java.io.*;
import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 * @author Jurgen F. Doreleijers
 */
public class IndexHashedBitToMany extends Index implements Serializable  {
    
    private static final long serialVersionUID = -1207795172754062330L;    
        
    IntArrayList trueMap = new IntArrayList();
    IntArrayList falseMap = new IntArrayList();
    public IndexHashedBitToMany() {
        init();
    }
    
    public void init() {
        trueMap.setSize(0);
        falseMap.setSize(0);
    }
    

    public IntArrayList getRidListForQuery( Object o, Object threshold, Object operation ) {
        return null;
    }
    
    public IntArrayList getRidList( Object i) {
        boolean b = ((Boolean) i).booleanValue();
        if ( b ) {
            return trueMap;
        }
        return falseMap;
    }
    
    /** See super method    */
    public BitSet getRidListForQuery( int operator, Object value ) {
        switch ( operator ) {
            case SQLSelect.OPERATION_TYPE_EQUALS: {
                IntArrayList m;
                boolean v = ((Boolean) value).booleanValue();
                m = falseMap;
                if ( v ) {
                    m = trueMap;
                } 
                BitSet result = PrimitiveArray.toBitSet(m, m.size());
                return result;
            }
            default: {
                General.showCodeBug("Operation not supported in getRidListForQuery: " + SQLSelect.operation_type_names[ operator ]);
                return null;
            }
        }
    }  
            
    public String toString() {
        return toString( true, true );
    }
        
    public String toString(boolean show_elements, boolean show_keys ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "Number of keys is: 2 (true;false)\n");
        if ( show_elements ) {
            sb.append( "true : " + PrimitiveArray.toString(trueMap) + General.eol);
            sb.append( "false: " + PrimitiveArray.toString(falseMap)+ General.eol);
        }
        return sb.toString();
    }
        
        
    public boolean updateIndex(Relation relation, String columnLabel) { 
        init();
        // Estimate size as to prevent resizing.
        trueMap.ensureCapacity(relation.used.size());
        falseMap.ensureCapacity(relation.used.size());
        Object object = relation.getColumn( columnLabel );
        if ( ! (object instanceof BitSet) ) {
            General.showError( "Object should be of type BitSet but found: " + object.getClass().getName());
            return false;
        }        
        BitSet column = (BitSet) object;
        for (int r=relation.used.nextSetBit(0); r>=0; r=relation.used.nextSetBit(r+1)) {
            if ( column.get(r) ) {
                trueMap.add(r);
            } else {
                falseMap.add(r);
            }
        }
        return true;
    }        
}
