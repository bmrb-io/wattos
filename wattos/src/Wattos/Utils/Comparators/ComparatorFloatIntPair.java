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
public class ComparatorFloatIntPair implements Comparator {
    
    /** Creates a new instance of IntIntPairComparator */
    public ComparatorFloatIntPair() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showDebug("doing compare");
        ObjectIntPair oip1= (ObjectIntPair) o1;        
        ObjectIntPair oip2= (ObjectIntPair) o2;        
        Float v1 = (Float) oip1.o;
        Float v2 = (Float) oip2.o;
        return v1.compareTo(v2);
    }            
}
 
