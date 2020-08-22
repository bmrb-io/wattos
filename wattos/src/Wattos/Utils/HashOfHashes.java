/*
 * ObjectMap2D.java
 *
 * Created on August 20, 2003, 11:44 AM
 */

package Wattos.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import Wattos.Database.Defs;

/**
 * HashMap of HashMap construction. Behaves as close to 1D HashMap
 *as possible.
 * @author Jurgen F. Doreleijers
 */
public class HashOfHashes extends HashMap {
    private static final long serialVersionUID = -4668260275026530415L;

    public Object put( Object key1, Object key2, Object value ) {
        HashMap m2 = (HashMap) get(key1);
        if ( m2 == null ) {
            m2 = new HashMap();
            put( key1, m2 );
        }
        // Now m2 is known
        return m2.put(key2,value);
    }
    
    public Object get( Object key1, Object key2 ) {
        HashMap m2 = (HashMap) get(key1);
        if ( m2 == null ) {
            return null;
        }
        // m2 was known
        return m2.get(key2);
    }  

    /** Convenience method; make sure the value is an int */
    public int getInt( Object key1, Object key2 ) {
        Object v = get(key1,key2);
        if ( v == null ) {
            return Defs.NULL_INT;
        }
        return ((Integer)v).intValue();
    }  
    
    /** Use only for String arguments
     *
    public Object getIgnoreWhiteSpace( Object key1, Object key2 ) {
        String keyStr1 = Strings.chomp( key1.toString() );
        String keyStr2 = Strings.chomp( key2.toString() );
        HashMap m2 = (HashMap) get(keyStr1);
        if ( m2 == null ) {
            return null;
        }
        // m2 was known
        return m2.get(keyStr2);
    }        
     */

    public HashMap getHash( Object key1  ) {
        HashMap m2 = (HashMap) get(key1);
        return m2;
    }        

    public String toString() {
        StringBuffer sb = new StringBuffer();
        int cardinality = 0;
        try {
            Set keySet1 = keySet();
            ArrayList keyList1 = new ArrayList( keySet1 );
            Collections.sort( keyList1 ); // in case of strings it will be alphanumerically sorted
            // in case of mixed object types an exception will be thrown.
            for (int i=0;i<keyList1.size();i++) {
                Object key1 = keyList1.get(i);
                HashMap m2 = (HashMap) get(key1);
                Set keySet2 = m2.keySet();
                ArrayList keyList2 = new ArrayList( keySet2 );
                Collections.sort( keyList2 ); // in case of strings it will be alphanumerically sorted
                for (int j=0;j<keyList2.size();j++) {
                    Object key2 = keyList2.get(j);
                    sb.append( "Key 1: " );
                    sb.append( key1.toString() );
                    sb.append( " Key 2: " );
                    sb.append( key2.toString() );
                    sb.append( " Value: " );
                    sb.append( m2.get( key2) );
                    sb.append( General.eol );
                    cardinality++;
                }
                sb.append( "Number of values in two-dimensional map is: " + cardinality);
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        }
        return sb.toString();
    }
    
    /** Returns the count of the number of deepest objects or
     *-1 in case of error.
     */
    public int cardinality() {
        int count = 0;
        Set keySet1 = keySet();
        try {
            for (Iterator it1=keySet1.iterator();it1.hasNext();) {
                Object key1 = it1.next();
                HashMap m2 = (HashMap) get(key1);
                count += m2.size();
            }
        // There might be cast errors so catch them.
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return -1;
        }
        return count;
    }        
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.showOutput("Starting tests in ObjectMap2D" );
        HashOfHashes HoH = new HashOfHashes();
        if ( true ) {
            HoH.put( "aapje", "olifant", "glijbaan" );
            Object value = HoH.get( "aapje", "olifant" );
            General.showOutput("Value looks like: " + value.toString() );            
        }
        General.showOutput("Map looks like:\n" + HoH.toString() );
        General.showOutput("Done with tests in HashOfHashes" );        
    }    
}
