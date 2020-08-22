package Wattos.Utils.Comparators;

import Wattos.Utils.ObjectIntPair;
import java.util.Comparator;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class ComparatorStringIntPair implements Comparator {
    
    public ComparatorStringIntPair() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showDebug("doing compare");
        ObjectIntPair oip1= (ObjectIntPair) o1;        
        ObjectIntPair oip2= (ObjectIntPair) o2;        
        String v1 = (String) oip1.o;
        String v2 = (String) oip2.o;
        return v1.compareTo(v2);
    }            
}
 
