/*
 * MyMap.java
 *
 * Created on February 25, 2003, 3:36 PM
 */

package Wattos.Utils;

import java.util.*;
import Wattos.Database.Defs;

/**
 * Map with String keys and int values.
 * @author Jurgen F. Doreleijers
 */
public class StringIntMap extends HashMap {
    private static final long serialVersionUID = 7207737317164240139L;

    /** Creates a new instance of MyMap */
    public StringIntMap( String[] input, int[] values ) {
        super();
        fill( input, values );
    }
        
    /** Creates a new instance of MyMap */
    public StringIntMap() {
        super();        
    }
                
    /** All the elements will be added with the parrallel values array.
     */
    public boolean fill(String[] input, int[] values) {
        for (int i=0;i<input.length;i++) {
            addString( input[i], i );
        }
        return true;
    }
    
    /** Returns the Defs.NULL_INT if the key doesn't exist in the map
     */
    public int getInt( String key ) {
        if ( ! containsKey(key) ) {
            return Defs.NULL_INT;
        }
        return ((Integer) get( key )).intValue(); // this is the slowest part!
    }
    

    public void addString( String key, int value ) {
        put( key, new Integer(value) );
    }
    
    public int sum() {
        int sum = 0;
        Collection l = values();
        for ( Iterator it=l.iterator();it.hasNext();) {
            sum += ((Integer) it.next()).intValue();
        }
        return sum;
    }
    
    /** Adds the increment to existing tuple for key or adds to zero
     *if tuple for that key didn't exist already.
     *Bit of a weird function.
     */
    public void increment( String key ) {
        if ( containsKey(key) ) {
//            Integer tmp = (Integer) get( key );
            put(key,new Integer( 1 + ((Integer) get( key )).intValue()));
            return;
        }
        put( key, new Integer( 1) );
    }    
}
