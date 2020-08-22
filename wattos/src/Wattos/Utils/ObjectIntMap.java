package Wattos.Utils;

import Wattos.Database.Defs;
import java.util.HashMap;
/**
 * Assumes the values are of type Integer
 * @author Jurgen F. Doreleijers
 */
public class ObjectIntMap extends HashMap {
    private static final long serialVersionUID = 8660338984646403252L;

    /** Returns a int value.
     * @param key The key for the object to get.
     * @return The integer value referenced by key.
     */    
    public int getValueInt( Object key ) {
        if( key == null ) {           
            return Defs.NULL_INT;
        }
        Integer It = (Integer) get( key );
        if ( It == null ) {
            return Defs.NULL_INT;
        }
        return It.intValue();
    }
    
    public Integer put( Object key, int value ) {
        return (Integer) put(key,new Integer(value));
    }
    
    /** Increments existing values or inserts new keys with initial value 1
     */
    public void increment(Object key ) {
        int c = getValueInt(key);
        if ( Defs.isNull(c)) {
            c = 0;
            put(key, c);
        }
        c++;
        put(key,c); // replaces
    }
}
