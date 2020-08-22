/*
 * ComparatorAtom.java
 *
 * Created on September 29, 2003, 2:15 PM
 */

package Wattos.Utils.Comparators;

import java.util.Comparator;

/**
 * @author Jurgen F. Doreleijers
 */
public class ComparatorIntArray implements Comparator {
    
    public ComparatorIntArray() {      
    }
    
    /** Do comparison on first int and so on. 
     *Fastest when only 1 int to compare.
     */
    public int compare(Object o1, Object o2) {       
        
        int[] a1 = (int[])  o1;
        int[] a2 = (int[])  o2;
        
        int i1    = a1[0];
        int i2    = a2[0];
        
        if ( i1 < i2 ) {
            return -1;
        }
        if ( i1 > i2 ) {
            return 1;
        }
        
        /** Speed things down after first comp.
         */
        int sizeMin = Math.min( a1.length, a2.length );
        for (int i=1;i<sizeMin;i++) {
            i1    = a1[i];
            i2    = a2[i];
            if ( i1 < i2 ) {
                return -1;
            }
            if ( i1 > i2 ) {
                return 1;
            }
        }                    
        return 0;
    }
}
