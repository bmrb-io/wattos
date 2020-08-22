/*
 * ObjectMap2D.java
 *
 * Created on August 20, 2003, 11:44 AM
 */

package Wattos.Utils;

import java.util.*;

/**
 * HashMap of HashMap of HashMap construction.
 * @author Jurgen F. Doreleijers
 */
public class HashOfHashesOfHashes extends HashMap {
    private static final long serialVersionUID = 9061406596024671913L;

    public Object put( Object key1, Object key2, Object key3, Object value ) {
        HashOfHashes m2 = getHashOfHashes(key1);
        if ( m2 == null ) {
            m2 = new HashOfHashes();
            put( key1, m2 );
        }
        // Now m2 is known
        return m2.put(key2, key3, value);
    }
    
    public Object get( Object key1, Object key2, Object key3 ) {
        HashOfHashes m2 = getHashOfHashes(key1);
        if ( m2 == null ) {
            return null;
        }
        // m2 was known
        return m2.get(key2, key3);
    }        

    public HashMap get( Object key1, Object key2  ) {
        HashOfHashes m2 = getHashOfHashes(key1);
        if ( m2 == null ) {
            return null;
        }
        // m2 was known
        return m2.getHash(key2);
    }        

    public HashOfHashes getHashOfHashes( Object key1  ) {
        HashOfHashes m2 = (HashOfHashes) get(key1);
        return m2;
    }        
    
    /** Write out the code for the different levels so the values are neatly alligned */
    public String toString() {
        int cardinality = 0;
        StringBuffer sb = new StringBuffer();
        try {
            Set keySet1 = keySet();
            ArrayList keyList1 = new ArrayList( keySet1 );
            Collections.sort( keyList1 ); // in case of strings it will be alphanumerically sorted
            // in case of mixed object types an exception will be thrown.
            for (int i=0;i<keyList1.size();i++) {
                Object key1 = keyList1.get(i);
                HashOfHashes m2 = (HashOfHashes) get(key1);
                Set keySet2 = m2.keySet();
                ArrayList keyList2 = new ArrayList( keySet2 );
                Collections.sort( keyList2 ); // in case of strings it will be alphanumerically sorted
                for (int j=0;j<keyList2.size();j++) {
                    Object key2 = keyList2.get(j);
                    HashMap m3 = (HashMap) m2.get(key2);
                    Set keySet3 = m3.keySet();
                    ArrayList keyList3 = new ArrayList( keySet3 );
                    Collections.sort( keyList3 ); // in case of strings it will be alphanumerically sorted
                    for (int k=0;k<keyList3.size();k++) {
                        Object key3 = keyList3.get(k);
                        sb.append( "Key 1: " );
                        sb.append( key1.toString() );
                        sb.append( " Key 2: " );
                        sb.append( key2.toString() );
                        sb.append( " Key 3: " );
                        sb.append( key3.toString() );
                        sb.append( " Value: " );
                        Object o = m3.get( key3);
                        if ((o instanceof float[] ) || 
                            (o instanceof int[] ) || 
                            (o instanceof short[] ) 
                             ) {
                            sb.append( PrimitiveArray.toString( o ) );
                        } else {
                            sb.append( m3.get( key3) );
                        }
                        sb.append( General.eol );
                        cardinality++;
                    }
                }
            }
            sb.append( "Number of key combinations in three-dimensional map is: " + cardinality);
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
                HashOfHashes m2 = getHashOfHashes(key1);
                count += m2.cardinality();
            }
        // There might be cast errors so catch them.
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return -1;
        }
        return count;
    }        
}
