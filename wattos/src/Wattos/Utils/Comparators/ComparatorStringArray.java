package Wattos.Utils.Comparators;

import java.util.Comparator;

/**
 * @author Jurgen F. Doreleijers
 */
public class ComparatorStringArray implements Comparator {
    
    public ComparatorStringArray() {      
    }
    
    public int compare(Object o1, Object o2) {               
        String s1 = (String)  o1;
        String s2 = (String)  o2;
        return s1.compareTo(s2);
    }
}
