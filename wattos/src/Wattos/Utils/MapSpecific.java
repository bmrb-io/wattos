/*
 * MapSpecific.java
 *
 * Created on August 29, 2003, 9:14 AM
 */

package Wattos.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import Wattos.Database.Defs;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * Contains some methods so we can return specific types of data based on the
 *string key.
 * @author Jurgen F. Doreleijers
 */
public class MapSpecific implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;    
    
    /** Main container of data of this class.
     */
    public HashMap m = new HashMap();

    /** Creates a new instance of MapSpecific */
    public MapSpecific() {
        loadMap();
    }

    /** For easy working with maps
     */
    public static HashMap toHashMap( Object[] list ) {
        HashMap result = new HashMap();
        for (int i=0;i<list.length;i++) {
            result.put(list[i], null);
        }
        return result;
    }
    
    /** Loads the global variables */
    public boolean loadMap() {
        General.showCodeBug("Method loadMap in MapSpecific needs to be overriden ");
        return false;
    }
        
    /** Show the content of the hash map m
     */
    public String toString( ) {
        Parameters p = new Parameters(); // for printf
        Object key;
        String value;
        int size_print_max = 100;
        
        List keys = new ArrayList(Arrays.asList( m.keySet().toArray() ));
        Collections.sort(keys);
        
        StringBuffer sb = new StringBuffer();
        sb.append( Format.sprintf( "There are %d options set:\n", p.add( keys.size() )) );
        Iterator i = keys.iterator();
        while ( i.hasNext() ) {            
            key = i.next();
            value = m.get( key ).toString();
            // Truncate values longer than specified size.
            if ( value.length() > size_print_max )
                value = value.substring(0,size_print_max);
            // Don't show more than 1 line
            if ( value.indexOf(General.eol) != -1 )
                value = value.substring(0,value.indexOf(General.eol)) + "[...]";            
            sb.append( Format.sprintf( "%-25s : %-s\n", 
                p.add( key ).add( value )) );
        }
        return sb.toString();
    }
    
    
    /** Generic version of get returning an object.
     * @param key The key for the object to get.
     * @return The object reference or null if the key is invalid.
     */    
    public Object getValue( Object key ) {
        if( key != null ) {
            return m.get( key );
        }
        General.showError("in Globals.getValue found:"); 
        General.showError("key was not valid reference key for any object in hashmap");
        return null;
    }
    
    
    /** Returns a boolean value.
     * @param key The key for the object to get.
     * @return <CODE>true</CODE> if key exists and the value of the referenced object is
     * <CODE>true</CODE>.
     */    
    public boolean getValueBoolean( Object key ) {
        if( key != null ) {           
            return  ((Boolean) m.get( key )).booleanValue();
        }
        General.showError("in Globals.getValueBoolean found:");
        General.showError("key was not valid reference key for any object in hashmap");
        return false;
    }

    /** Returns a int value.
     * @param key The key for the object to get.
     * @return The integer value referenced by key.
     */    
    public int getValueInt( Object key ) {
        if( key != null ) {           
            return  ((Integer) m.get( key )).intValue();
        }
        General.showError("in Globals.getValueInt found:");
        General.showError("key was not valid reference key for any object in hashmap");
        return Defs.NULL_INT;
    }
    
    /** Returns a string reference. If the key is null or doesn't
     *occur in the hash it will return the default string reference
     *indicating invalidity.
     * @param key The key for the object to get.
     * @return The reference to a string referenced by key.
     */    
    public String getValueString( Object key ) {
        if( key == null ) {           
            General.showError("in Globals.getValueString found:");
            General.showError("key was not valid reference key for any object in hashmap");
            return Defs.NULL_STRING_NULL;
        }
        
        Object o = m.get( key );
        if ( o == null ) {
            return Defs.NULL_STRING_NULL;
        }
        
        return o.toString();
    }
    
    /** Makes keys for values and vice versa deleting doubles that might arise.
     */    
    public static HashMap invertHashMap( HashMap in ) {
        HashMap out = new HashMap();
        for ( Iterator it = in.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = in.get(key);
            out.put(value, key);           
        }
        return out;
    }

    /** Self test; tests the function <CODE>showmap</CODE>.
     * @param args Ignored.
     */
    public static void main (String[] args) 
    {
        MapSpecific map = new MapSpecific();
        General.showOutput( map.toString() );
    }            
}
 
