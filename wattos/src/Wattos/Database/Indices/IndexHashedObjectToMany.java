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
import Wattos.Utils.PrimitiveArray;
import Wattos.Database.*;

/**
 * @author Jurgen F. Doreleijers
 */
public class IndexHashedObjectToMany extends Index implements Serializable {
 
    private static final long serialVersionUID = -1207795172754062330L;        
        
    HashMap hashMap = new HashMap();
    /** Creates a new instance of IndexStringToMany */
    public IndexHashedObjectToMany() {
    }

    public Object getRidList( Object o, int listType) {
        if ( hashMap.containsKey( o ) ) {
            IntArrayList list = (IntArrayList) hashMap.get(o);
            if ( listType == Index.LIST_TYPE_INT_ARRAY_LIST ) {
                return list;
            }
            BitSet result = PrimitiveArray.toBitSet(list, -1);
            return result;
        }
        return null;
    }
        
            
    public String toString(boolean show_elements, boolean show_keys ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "Number of keys is: ");
        sb.append( hashMap.size() );
        if ( show_keys ) {
            sb.append( General.eol);
            Set keys = hashMap.keySet();
            for ( Iterator it=keys.iterator();it.hasNext();) {
                Object key = it.next();
                sb.append( "Key: " );
                sb.append( key.toString() );
                sb.append( " Size: " );
                IntArrayList rids = (IntArrayList) hashMap.get( key );
                sb.append( rids.size() );
                if ( show_elements ) {
                    sb.append( " Elements: " );
                    sb.append( PrimitiveArray.toString( rids )); 
                }                                    
                sb.append( General.eol );
            }
            
        }
        return sb.toString();
    }
        
        
    public boolean updateIndex(Relation relation, String columnLabel) { 
        IntArrayList rids = null;
        Object object = relation.getColumn( columnLabel );
        Object[] column = null;
        if ( object instanceof String[] ) {
            column = (String[]) object;
        } else if ( object instanceof Object[] ) {
            column = (Object[]) object;
        } else {
            General.showError( "Object should be of type String[] or Object[] but found: " + object.getClass().getName());
            return false;
        }
        
        BitSet used = relation.used;
        for (int r=used.nextSetBit(0); r>=0; r=used.nextSetBit(r+1)) {
            Object o = column[r];
            if ( ! hashMap.containsKey(o) ) {
                rids = new IntArrayList();
                hashMap.put( o, rids );
            } else {
                rids = (IntArrayList) hashMap.get( o );
            }
            rids.add( r );
        }
        return true;
    }    
}
