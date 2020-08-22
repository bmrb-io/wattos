/*
 * IndexStringToMany.java
 *
 * Created on June 18, 2003, 11:02 AM
 */

package Wattos.Database.Indices;


import java.io.*;
import cern.colt.list.*;
import cern.colt.map.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 * @author Jurgen F. Doreleijers
 */
public class IndexHashedIntToMany extends Index implements Serializable  {
    
    private static final long serialVersionUID = -1207795172754062330L;    
        
    OpenIntObjectHashMap hashMap = new OpenIntObjectHashMap();
    /** Creates a new instance of IndexStringToMany */
    public IndexHashedIntToMany() {
        init();
    }
    
    public void init() {
        hashMap.clear();
    }
    

    public IntArrayList getRidListForQuery( Object o, Object threshold, Object operation ) {
        return null;
    }
    
    /** Returns new empty list in case of no list found.
     */
    public IntArrayList getRidList( int i) {
        if ( hashMap.containsKey( i ) ) {
            return (IntArrayList) hashMap.get(i);
        }
        return new IntArrayList(0);
    }
            
    public String toString() {
        return toString( true, true );
    }
        
    public String toString(boolean show_elements, boolean show_keys ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "Number of keys is: ");
        sb.append( hashMap.size() );
        if ( show_keys ) {
            sb.append( General.eol);
            IntArrayList keys = hashMap.keys();
            for ( int i=0;i<keys.size();i++) {
                int key = keys.get(i);
                sb.append( "Key: " );
                sb.append( key );
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
        if ( ! (object instanceof int[]) ) {
            General.showError( "Object should be of type int[] but found: " + object.getClass().getName());
            return false;
        }
        int[] column = (int[]) object;
        
        for (int r=relation.used.nextSetBit(0); r>=0; r=relation.used.nextSetBit(r+1)) {
            int o = column[r];
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
