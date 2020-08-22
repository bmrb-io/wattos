/*
 *
 * Created on July 31, 2003, 9:42 AM
 */

package Wattos.Utils.Comparators;

import Wattos.Utils.ObjectIntPair;
import java.util.Comparator;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class ComparatorShortIntPair implements Comparator {
    
    /** Creates a new instance of IntIntPairComparator */
    public ComparatorShortIntPair() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showDebug("doing compare");
        ObjectIntPair oip1= (ObjectIntPair) o1;        
        ObjectIntPair oip2= (ObjectIntPair) o2;        
        return ((Short) oip1.o).compareTo((Short) oip2.o);
    }            
}
 
