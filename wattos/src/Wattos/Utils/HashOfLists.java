/*
 * HashOfLists.java
 *
 * Created on February 12, 2004, 4:43 PM
 */

package Wattos.Utils;

import java.util.*;

/**
 * HashMap of ArrayList construction.
 * @author Jurgen F. Doreleijers
 */
public class HashOfLists extends HashMap {
    private static final long serialVersionUID = 3789129002331643111L;
    /** Creates a new instance of HashOfLists */
    public HashOfLists() {
    }
    
    public Object get( Object o_1, Object o_2 ) {
        ArrayList array = (ArrayList) get(o_1);
        if ( array == null ) {
            return null;
        }
        int idx = array.indexOf( o_2 ); // Expensive of course.
        if ( idx < 0 ) {
            return null;
        }
        return array.get( idx );
    }

    /** For convenience the idx can be given as -1 which will lead
     *to the element being put in at the end of the list.
     */
    public void put( Object o_1, int idx, Object o_2 ) {
        ArrayList array = (ArrayList) get(o_1);
        if ( array == null ) {
            array = new ArrayList();
            put( o_1, array );
        }
        if ( idx < 0 ) {
            idx = array.size();
        }
        array.add( idx, o_2 );        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
