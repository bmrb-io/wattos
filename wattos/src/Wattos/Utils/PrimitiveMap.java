/*
 * PrimitiveMap.java
 *
 * Created on January 14, 2004, 9:13 AM
 */

package Wattos.Utils;

import cern.colt.list.*;
import cern.colt.map.*;

/**
 * Extensions of the cern.colt.map API for working with maps based on primitive
 * datatypes.
 * @author Jurgen F. Doreleijers
 */
public class PrimitiveMap {

    /**Watch out not to initialize the primitive hashmap with too few elements like 3. Don't know why this
 *causes a problem but it does seem to cause it to hang in an infinite loop.
     */
    public static final int MIN_SIZE_MAP = 11;
    
    /** Creates a new instance of PrimitiveMap */
    public PrimitiveMap() {
    }

    /** Returns null if even one element isn't in map.*/
    static public IntArrayList getMappedList( IntArrayList in, OpenIntIntHashMap map) {        
        IntArrayList result = new IntArrayList(in.size());
        result.setSize( in.size() );
        for (int i = in.size()-1; i>=0; i--) {            
            int key = in.getQuick(i);
            if ( ! map.containsKey(key)) {
                General.showError("Failed to find key: " + key + " in OpenIntIntHashMap: " + map );
                return null;
            }
            //General.showDebug("Getting association with key: " + i);
            result.setQuick(i, map.get(key));
        }        
        return result;
    }
    
    /** Creates a map with the values being the key to the index .*/
    static public OpenIntIntHashMap createMapValuesAreKeys( IntArrayList in ) {        
        OpenIntIntHashMap result;
        if ( in == null ) {
            General.showWarning("Null pointer as argument to createMapValuesAreKeys");
            return null;
        }
        if ( in.size() > PrimitiveMap.MIN_SIZE_MAP ) {
            result = new OpenIntIntHashMap(in.size());
        } else {
            result = new OpenIntIntHashMap();
        }
        if ( in.size() == 0 ) {
            General.showDebug("No elements to do createMapValuesAreKeys");
            return result;
        }
        for (int i = in.size()-1; i>=0; i--) {            
            //General.showDebug("Setting association between key: " + in.getQuick(i) + " and value: " + i);
            result.put( in.getQuick(i), i );
        }
        return result;
    }
    
    /** Creates a map with the indices being the key to the value.*/
    static public OpenIntIntHashMap createMapIndicesAreKeys( IntArrayList in ) {        
        OpenIntIntHashMap result;
        if ( in == null ) {
            General.showWarning("Null pointer as argument to createMapIndicesAreKeys");
            return null;
        }
        if ( in.size() > PrimitiveMap.MIN_SIZE_MAP ) {
            result = new OpenIntIntHashMap(in.size());
        } else {
            result = new OpenIntIntHashMap();
        }
        if ( in.size() == 0 ) {
            General.showDebug("No elements to do createMapValuesAreKeys");
            return result;
        }
        for (int i = in.size()-1; i>=0; i--) {            
            //General.showDebug("Setting association between key: " + i + " and value: " + in.getQuick(i));
            result.put( i, in.getQuick(i));
        }
        return result;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.showDebug("Starting test");
        OpenIntIntHashMap m = new OpenIntIntHashMap(3); // this setup causes it to fail sometimes
        m.put(24,2);
        m.put(23,1);
        m.put(21,0); // this setup with 3 as capacity fails...
        General.showDebug("Finished test");
    }    
}
