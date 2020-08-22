/*
 * IntIntPairComparator.java
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
public class ComparatorIntIntPair implements Comparator {
    
    /** Creates a new instance of IntIntPairComparator */
    public ComparatorIntIntPair() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showDebug("doing compare");
        ObjectIntPair oip1= (ObjectIntPair) o1;        
        ObjectIntPair oip2= (ObjectIntPair) o2;        
        return ((Integer) oip1.o).compareTo((Integer) oip2.o);
    }            
}
 
